# DALUYAN — Kotlin environmental action planner

**Version 0.4.1 — Premium map-first Android prototype**. Jetpack Compose, MapLibre, SQLite and HOT/OSM waterway geometry. Requires Android 8.0 or newer.

## Main experience

- **Map:** Tap a real location, use device GPS, and record an environmental observation. The redesigned interface respects the system status bar and focuses on a single reporting action. A MapLibre basemap (OpenFreeMap / OpenStreetMap derived) requires internet, while the loaded HOT waterway geometry is drawn as a separate layer.
- **Reporting:** Three short screens (where, what, review), photo evidence, offline local records, editing and validation. Bottom-sheet navigation buttons stay in a separate keyboard- and system-inset-aware footer.
- **Actions:** Record, revise, remove, rank, document a follow-up action, and export observations as CSV. Scores are heuristic decision support—not certified risk ratings.
- **Evidence:** 2025 historical DENR-EMB MIMAROPA fecal-coliform readings. These values are **not** solid-waste or plastic counts. A compact list expands to all readings.
- **GIS:** On each GitHub Actions build, the real HOT/HDX Philippine waterway distribution is reduced to a Calapan *pilot rectangle*. The GeoJSON is bundled in the APK and loaded automatically to compute approximate straight-line distances offline. The equivalent GeoPackage and metadata are also bundled and exportable from Evidence for QGIS. Importing an alternative waterway-only GeoJSON layer is optional.

## Actual geographic data

Source: [HOT / HDX Waterways of Philippines](https://data.humdata.org/dataset/hotosm_phl_waterways), **© OpenStreetMap contributors, ODbL**. The reproducible preparation script is `scripts/prepare_waterways.py`, including direct HDX and a documented public archive fallback.

Pilot bounding rectangle (WGS84): `[121.03, 13.25, 121.38, 13.52]`. **This is not the official Calapan city boundary.** The processed reference layer contains **281 mapped lines and 8,704 segments** as of the supplied snapshot. Coverage can omit unmapped streams. Straight-line proximity does not prove drainage connectivity, environmental harm, or flood risk.

On every CI build, regional `calapan_waterways.geojson`, `calapan_waterways.gpkg`, and provenance metadata are generated; both geographic formats are included as assets and separately uploaded as GIS artifacts. Android uses **GeoJSON**, not the GeoPackage database, for runtime calculation.

## Install the APK

Go to [GitHub Actions](https://github.com/cedimscalapan/DALUYAN/actions) → latest **successful** `DALUYAN · Android CI / APK` run → artifact **DALUYAN-v0.4.1-premium-GIS-debug-apk** → unzip and install the APK.

GitHub Actions runs Android compilation and Kotlin unit tests. It also validates GIS geometry before packaging the assets. A successful workflow does **not** establish physical-device, GPS or camera testing.

**Important:** The build is a debug APK; each runner may use a different signing identity. Android may reject updating an earlier DALUYAN build. **Export important local records before uninstalling:** uninstalling an app generally removes its internal SQLite data and photos. A stable signed release build is not yet configured.

## Boundaries and privacy

Reports and photos remain on the user's device unless explicitly exported. No LGU role or official verification is claimed. Offline observations, evidence details and geographic distances work without internet once the bundled layer is loaded. Live basemap tiles still need connectivity. Avoid entering unsafe or private locations.
