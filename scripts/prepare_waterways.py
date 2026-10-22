#!/usr/bin/env python3
"""Prepare real HOT/OSM waterway lines for DALUYAN's Calapan pilot.
GeoJSON is packaged in the app; GeoPackage is supplied for GIS review.
Data is volunteer-mapped and may be incomplete. No synthetic features.
"""
import hashlib, io, json, os, shutil, sys, time, urllib.request, zipfile
from pathlib import Path
from shapely.geometry import shape, box
import fiona

HDX = "https://data.humdata.org/dataset/hotosm_phl_waterways"
API = "https://data.humdata.org/api/3/action/package_show?id=hotosm_phl_waterways"
MIRROR = "https://raw.githubusercontent.com/yowmamasita/philippines-map-data/main/data/infrastructure/hdx-osm-waterways/hotosm_phl_waterways_lines_geojson.zip"
BBOX = (121.03, 13.25, 121.38, 13.52)  # pilot rectangle, NOT official Calapan boundary
VALID = {"river", "stream", "canal", "drain", "ditch", "tidal_channel"}

def read_url(url, outfile):
    outfile.parent.mkdir(parents=True, exist_ok=True)
    for attempt in range(3):
        try:
            req = urllib.request.Request(url, headers={"User-Agent":"DALUYAN-GIS/0.3", "Accept":"*/*"})
            with urllib.request.urlopen(req, timeout=100) as response, outfile.open("wb") as dest:
                size = 0
                while True:
                    chunk = response.read(1024*1024)
                    if not chunk: break
                    dest.write(chunk)
                    size += len(chunk)
                    if size > 250*1024*1024: raise ValueError("Input exceeds 250 MB safety limit")
            if size < 1024: raise ValueError("Download too small")
            return
        except Exception as err:
            outfile.unlink(missing_ok=True)
            print("Download failed:",url, str(err)[:180],flush=True)
            if attempt<2: time.sleep(3*(attempt+1))
    raise RuntimeError("Could not download: "+url)

def available_resources():
    try:
        req = urllib.request.Request(API, headers={"User-Agent":"DALUYAN-GIS/0.3"})
        with urllib.request.urlopen(req,timeout=25) as response: result=json.load(response)
        if not result.get("success"): raise ValueError("HDX metadata unsuccessful")
        found={}
        for res in result["result"]["resources"]:
            description=((res.get("name") or "")+" "+(res.get("description") or "")).lower()
            fmt=str(res.get("format") or "").lower()
            url=res.get("download_url") or res.get("url")
            if not url or not url.startswith("https://"): continue
            if "geojson" in fmt or "geojson" in description:
                if "json" not in found or "line" in description: found["json"]=url
            if "geopackage" in fmt or "gpkg" in fmt or "geopackage" in description:
                if "gpkg" not in found or "line" in description: found["gpkg"]=url
        print("HDX distributions discovered:",list(found),flush=True)
        return found
    except Exception as err:
        print("HDX metadata unavailable; using public archived HDX/HOT GeoJSON source:",str(err)[:150],flush=True)
        return {}

def load_geojson(path):
    if zipfile.is_zipfile(path):
        with zipfile.ZipFile(path) as z:
            all_names=z.namelist()
            print("HDX ZIP entries:",[(n,z.getinfo(n).file_size) for n in all_names][:25],flush=True)
            names=[n for n in all_names if n.lower().endswith(".geojson") and not n.startswith("__MACOSX")]
            if not names:
                names=[n for n in all_names if n.lower().endswith(".json") and "metadata" not in n.lower() and not n.startswith("__MACOSX")]
            if not names: raise ValueError("Archive contains no geographic GeoJSON (metadata-only archive)")
            names.sort(key=lambda n:("line" not in n.lower(),-z.getinfo(n).file_size))
            print("Reading geographic features:",names[0],flush=True)
            with z.open(names[0]) as stream: return json.load(stream)
    with path.open(encoding="utf-8") as stream: return json.load(stream)

def extract(raw):
    if raw.get("type")!="FeatureCollection": raise ValueError("Source must be GeoJSON FeatureCollection")
    extent=box(*BBOX)
    features=[]
    scanned=0
    for feature in raw.get("features",[]):
        scanned+=1
        geometry=feature.get("geometry") or {}
        if geometry.get("type") not in ("LineString","MultiLineString"): continue
        props=feature.get("properties") or {}
        kind=str(props.get("waterway") or props.get("waterway_type") or "").lower()
        if "highway" in props or (kind and kind not in VALID): continue
        try: geom=shape(geometry)
        except Exception: continue
        if geom.is_empty or not geom.is_valid or not geom.intersects(extent): continue
        clipped=geom.intersection(extent)
        stack=[clipped]
        while stack:
            g=stack.pop()
            if g.geom_type=="LineString":
                if len(g.coords)<2 or g.length==0: continue
                simpler=g.simplify(.000005,preserve_topology=True)
                coords=[[round(float(x),7),round(float(y),7)] for x,y in simpler.coords]
                if len(coords)<2: continue
                features.append({"type":"Feature","properties":{"waterway":kind or "unspecified",
                    "osm_id":str(props.get("osm_id") or props.get("@id") or "")},
                    "geometry":{"type":"LineString","coordinates":coords}})
            elif hasattr(g,"geoms"):stack.extend(g.geoms)
    if not features: raise ValueError("No verified source waterways within pilot bounding rectangle")
    features.sort(key=lambda f:(f["properties"]["waterway"],f["geometry"]["coordinates"][0]))
    return features,scanned

def main():
    output=Path("build/waterways")
    raw=output/"national_source"
    raw.mkdir(parents=True,exist_ok=True)
    distributions=available_resources()
    national=raw/"hotosm_phl_waterways_lines_geojson.zip"
    url=distributions.get("json") or MIRROR
    try:
        read_url(url,national)
        origin="HDX direct" if url!=MIRROR else "HDX-derived archival GitHub mirror"
    except Exception:
        if url==MIRROR: raise
        read_url(MIRROR,national)
        origin="HDX-derived archival GitHub mirror"
    gpkg_source=None
    if distributions.get("gpkg"):
        try:
            gpkg_source=raw/"hotosm_original_geopackage"
            read_url(distributions["gpkg"],gpkg_source)
            print("Original GeoPackage distribution downloaded",gpkg_source.stat().st_size,flush=True)
        except Exception:
            gpkg_source=None
            print("Original national GeoPackage unavailable; regional GeoPackage will be derived from GeoJSON.",flush=True)
    raw_geojson=load_geojson(national)
    features,scanned=extract(raw_geojson)
    geojson=output/"calapan_waterways.geojson"
    data={"type":"FeatureCollection","bbox":list(BBOX),"name":"Calapan pilot rectangle, HOT/OSM waterways",
          "features":features}
    geojson.write_text(json.dumps(data,separators=(",",":"),ensure_ascii=False),encoding="utf-8")
    segments=sum(len(f["geometry"]["coordinates"])-1 for f in features)
    if geojson.stat().st_size>3*1024*1024 or len(features)>5000 or segments>39999:
        raise ValueError(f"Layer exceeds Android limits: {len(features)} features, {segments} segments, {geojson.stat().st_size} bytes")
    gpkg=output/"calapan_waterways.gpkg"
    gpkg.unlink(missing_ok=True)
    schema={"geometry":"LineString","properties":{"waterway":"str:48","osm_id":"str:100"}}
    with fiona.open(gpkg,"w",driver="GPKG",layer="calapan_waterways",schema=schema,crs="EPSG:4326") as sink:
        for f in features: sink.write(f)
    meta={"source_dataset":HDX,"provider":"HOT / © OpenStreetMap contributors","license":"ODbL",
          "original_geojson_source":origin,"original_geopackage_downloaded":gpkg_source is not None,
          "original_geojson_sha256":hashlib.sha256(national.read_bytes()).hexdigest(),
          "filtered_geojson_sha256":hashlib.sha256(geojson.read_bytes()).hexdigest(),
          "pilot_extent_wgs84":list(BBOX),"pilot_extent_is_administrative_boundary":False,
          "scanned_source_features":scanned,"clipped_features":len(features),"segments":segments,
          "geojson_bytes":geojson.stat().st_size,"geopackage_bytes":gpkg.stat().st_size,
          "note":"Both regional formats represent the same verified GeoJSON source lines. GeoPackage is derived locally. Geographic coverage is not guaranteed; distance is straight-line only."}
    metadata=output/"calapan_waterways_metadata.json"
    metadata.write_text(json.dumps(meta,indent=2),encoding="utf-8")
    assets=Path("app/src/main/assets")
    assets.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(geojson,assets/"calapan_waterways.geojson")
    shutil.copyfile(metadata,assets/"calapan_waterways_metadata.json")
    print("CALAPAN DATA READY",json.dumps(meta),flush=True)

if __name__=="__main__": main()
