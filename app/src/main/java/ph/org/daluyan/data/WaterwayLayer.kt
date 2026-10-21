package ph.org.daluyan.data

import android.content.Context
import org.json.JSONObject
import ph.org.daluyan.domain.GeoMath
import java.io.File

data class GeoPoint(val lat: Double, val lon: Double)

/** Only line geometries from a waterway-specific GeoJSON export should be imported. */
class WaterwayLayer(val segments: List<Pair<GeoPoint, GeoPoint>>, val sourceName: String) {
    fun distanceMeters(latitude: Double, longitude: Double): Double? = segments.minOfOrNull { (a, b) ->
        GeoMath.pointSegmentMeters(latitude,longitude,a.lat,a.lon,b.lat,b.lon)
    }
    companion object {
        private const val MAX_SIZE = 3 * 1024 * 1024
        private const val MAX_SEGMENTS = 40000
        private const val NAME = "waterways.geojson"
        fun load(context: Context): WaterwayLayer? = runCatching {
            val f = File(context.filesDir, NAME)
            if (!f.isFile) return null
            parse(f.readText(), "Imported GeoJSON")
        }.getOrNull()
        fun save(context: Context, contents: String): WaterwayLayer {
            val layer = parse(contents, "Imported GeoJSON")
            val dest = File(context.filesDir, NAME)
            val temp = File(context.filesDir, "$NAME.tmp")
            temp.writeText(contents)
            if (!temp.renameTo(dest)) { temp.copyTo(dest,overwrite=true); temp.delete() }
            return layer
        }
        fun parse(json: String, sourceName: String): WaterwayLayer {
            require(json.toByteArray(Charsets.UTF_8).size <= MAX_SIZE) { "GeoJSON must be under 3 MB." }
            val root = JSONObject(json)
            require(root.optString("type") == "FeatureCollection") { "Choose a GeoJSON FeatureCollection." }
            val features = root.getJSONArray("features")
            require(features.length() in 1..5000) { "GeoJSON must contain 1–5000 features." }
            val segments = ArrayList<Pair<GeoPoint, GeoPoint>>()
            fun appendLine(coords: org.json.JSONArray) {
                var prev: GeoPoint? = null
                for (n in 0 until coords.length()) {
                    val p = coords.getJSONArray(n)
                    val lon = p.getDouble(0); val lat = p.getDouble(1)
                    require(lon.isFinite() && lon in -180.0..180.0 && lat.isFinite() && lat in -90.0..90.0) { "Invalid waterway coordinates." }
                    val point = GeoPoint(lat,lon)
                    prev?.let { if (segments.size < MAX_SEGMENTS) segments.add(it to point) }
                    prev = point
                }
            }
            for (i in 0 until features.length()) {
                val feat = features.getJSONObject(i)
                val properties = feat.optJSONObject("properties")
                // Explicitly reject known roads to prevent a misleading river-distance score.
                require(properties?.has("highway") != true) { "This layer includes roads. Export waterways only." }
                val geo = feat.optJSONObject("geometry") ?: continue
                when (geo.optString("type")) {
                    "LineString" -> appendLine(geo.getJSONArray("coordinates"))
                    "MultiLineString" -> {
                        val lines = geo.getJSONArray("coordinates")
                        for (j in 0 until lines.length()) appendLine(lines.getJSONArray(j))
                    }
                }
                require(segments.size < MAX_SEGMENTS) { "Too many geometry segments." }
            }
            require(segments.isNotEmpty()) { "No waterway lines found." }
            return WaterwayLayer(segments,sourceName)
        }
    }
}
