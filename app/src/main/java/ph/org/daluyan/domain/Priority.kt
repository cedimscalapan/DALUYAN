package ph.org.daluyan.domain

import java.time.Instant
import java.util.Locale
import kotlin.math.*

enum class SiteAccess(val title: String) {
    PUBLIC("Public viewing area"), PERMISSION("Permission required"), UNSAFE("Unsafe / restricted")
}
enum class ActionStatus(val title: String) {
    REPORTED("Reported"), REVIEW("Needs review"), PLANNED("Action planned"), COMPLETED("Action recorded"), FOLLOW_UP("Follow-up needed")
}
enum class LocationSource { MAP, GPS, MANUAL }

data class SiteReport(
    val id: Long = 0,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val severity: Int,
    val access: SiteAccess,
    val notes: String,
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val locationSource: LocationSource = LocationSource.MAP,
    val accuracyMeters: Float? = null,
    val status: ActionStatus = ActionStatus.REPORTED,
    val actionNote: String = "",
    val actionAt: Long? = null
)

data class Assessment(
    val concern: Int,
    val confidence: Int,
    val concernLabel: String,
    val confidenceLabel: String,
    val distanceMeters: Double?,
    val recurrence: Int,
    val ready: Boolean,
    val nextStep: String
)

object ReportValidation {
    fun problems(r: SiteReport): List<String> = buildList {
        if (r.title.trim().length !in 4..80) add("Give the location a name (4–80 characters).")
        if (!r.latitude.isFinite() || r.latitude !in -90.0..90.0) add("Latitude must be between −90 and 90.")
        if (!r.longitude.isFinite() || r.longitude !in -180.0..180.0) add("Longitude must be between −180 and 180.")
        if (r.severity !in 1..3) add("Select the visible litter level.")
        if (r.notes.length > 500) add("Notes must be 500 characters or fewer.")
        if (r.actionNote.length > 800) add("Action notes must be 800 characters or fewer.")
        if (r.accuracyMeters != null && (!r.accuracyMeters.isFinite() || r.accuracyMeters < 0)) add("Invalid GPS accuracy.")
        if (r.status == ActionStatus.COMPLETED && r.actionNote.trim().length < 12) add("Describe the action result (at least 12 characters).")
        if (r.status == ActionStatus.COMPLETED && r.actionAt == null) add("An action completion date is required.")
    }
}

object GeoMath {
    private const val R = 6371000.0
    fun distanceMeters(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
        val dLat = Math.toRadians(bLat - aLat)
        val dLon = Math.toRadians(bLon - aLon)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(aLat)) * cos(Math.toRadians(bLat)) * sin(dLon / 2).pow(2)
        return 2 * R * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
    /** Local metric projection for short river segments. Returns straight-line distance, not flow connectivity. */
    fun pointSegmentMeters(lat: Double, lon: Double, aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
        val x1 = Math.toRadians(aLon - lon) * R * cos(Math.toRadians(lat))
        val y1 = Math.toRadians(aLat - lat) * R
        val x2 = Math.toRadians(bLon - lon) * R * cos(Math.toRadians(lat))
        val y2 = Math.toRadians(bLat - lat) * R
        val dx = x2 - x1; val dy = y2 - y1
        val fraction = if (dx * dx + dy * dy < 1e-10) 0.0 else (-x1 * dx - y1 * dy) / (dx * dx + dy * dy)
        val t = fraction.coerceIn(0.0, 1.0)
        return hypot(x1 + t * dx, y1 + t * dy)
    }
}

/** On-device method: method & thresholds are provisional and not official environmental risk ratings. */
object AssessmentEngine {
    fun assess(r: SiteReport, reports: List<SiteReport>, distanceMeters: Double?): Assessment {
        // Count unique observation dates from other records within 50 m. Repeated taps on one day count once.
        val dates = reports.asSequence().filter { it.id != r.id && it.createdAt != r.createdAt &&
            GeoMath.distanceMeters(it.latitude, it.longitude, r.latitude, r.longitude) <= 50.0 }
            .map { java.time.Instant.ofEpochMilli(it.createdAt).atZone(java.time.ZoneOffset.UTC).toLocalDate() }
            .filter { it != java.time.Instant.ofEpochMilli(r.createdAt).atZone(java.time.ZoneOffset.UTC).toLocalDate() }
            .toSet()
        val recurrence = (dates.size + 1).coerceAtMost(5)
        val severityScore = (r.severity * 20) // 20, 40, 60
        val repeatScore = (recurrence - 1) * 5 // 0..20
        val spatialScore = when {
            distanceMeters == null -> 0
            distanceMeters <= 50 -> 20
            distanceMeters <= 200 -> 11
            else -> 0
        }
        val score = (severityScore + repeatScore + spatialScore).coerceIn(0,100)
        val confidence = ((if (r.photoUri != null) 35 else 10) +
            (if (r.locationSource == LocationSource.GPS && (r.accuracyMeters ?: 9999f) <= 50f) 35 else 15) +
            (if (distanceMeters != null) 30 else 0)).coerceIn(0,100)
        val next = when {
            r.access == SiteAccess.UNSAFE -> "Do not enter. Request assessment by authorized personnel."
            r.access == SiteAccess.PERMISSION -> "Obtain property and barangay authorization before any visit."
            distanceMeters == null -> "Check the site and import a verified local waterway layer for GIS context."
            r.severity == 3 -> "Request review of the documented litter and safe prevention options."
            else -> "Review the report and plan a suitable prevention or collection action."
        }
        return Assessment(score,confidence,
            if (distanceMeters == null) "Awaiting geographic data" else if(score >= 70) "Higher concern" else if(score >= 40) "Moderate concern" else "Lower concern",
            if(confidence >= 75) "Stronger evidence" else "Needs confirmation",
            distanceMeters,recurrence,r.access==SiteAccess.PUBLIC,next)
    }
}

object CsvExport {
    private fun safe(value: String): String {
        val normalized = value.replace("\r", " ").replace("\n", " ")
        val risk = normalized.trimStart().firstOrNull() in listOf('=', '+', '-', '@', '\t')
        return "\"" + (if (risk) "'" else "") + normalized.replace("\"", "\"\"") + "\""
    }
    fun generate(reports: List<SiteReport>, assessment: (SiteReport) -> Assessment): String = buildString {
        appendLine("id,title,latitude,longitude,severity,access,location_source,gps_accuracy_m,concern_score,evidence_score,gis_distance_m,verification,status,action_note,created_utc,updated_utc,notes")
        reports.forEach { r ->
            val a = assessment(r)
            appendLine(listOf(r.id.toString(),safe(r.title),r.latitude.toString(),r.longitude.toString(),
                r.severity.toString(),safe(r.access.title),r.locationSource.name,r.accuracyMeters?.toString() ?: "",
                a.concern.toString(),a.confidence.toString(),a.distanceMeters?.let { String.format(Locale.US,"%.1f",it) } ?: "",
                "UNVERIFIED",safe(r.status.title),safe(r.actionNote),Instant.ofEpochMilli(r.createdAt),
                Instant.ofEpochMilli(r.updatedAt),safe(r.notes)).joinToString(","))
        }
    }
}
