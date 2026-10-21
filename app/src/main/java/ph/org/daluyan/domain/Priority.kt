package ph.org.daluyan.domain

import kotlin.math.roundToInt
import java.util.Locale
import java.time.Instant

enum class WaterwayDistance(val label: String, val weight: Double) {
    NEAR("Within about 50m (estimated)", 1.0),
    MID("About 50–200m (estimated)", 0.55),
    UNKNOWN("Farther away / unknown", 0.0)
}
enum class SiteAccess(val label: String, val weight: Double) {
    PUBLIC("Publicly accessible", 1.0),
    PERMISSION("Permission required", 0.4),
    UNSAFE("Unsafe or inaccessible", 0.0)
}
data class SiteReport(
    val id: Long = 0L,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val severity: Int,
    val waterwayDistance: WaterwayDistance,
    val repeatCount: Int,
    val access: SiteAccess,
    val notes: String,
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
data class Score(
    val severity: Int, val waterway: Int, val recurrence: Int, val access: Int
) {
    val total: Int get() = severity + waterway + recurrence + access
    val band: String get() = if (total >= 70) "High priority" else if (total >= 40) "Moderate priority" else "Lower priority"
}
object PriorityEngine {
    /** Provisional prioritization weights for human review, not scientific risk ratings. */
    fun score(r: SiteReport): Score = Score(
        ((r.severity.coerceIn(1, 3) / 3.0) * 40).roundToInt(),
        (r.waterwayDistance.weight * 25).roundToInt(),
        ((r.repeatCount.coerceIn(1, 5) / 5.0) * 20).roundToInt(),
        (r.access.weight * 15).roundToInt()
    )
    fun nextStep(r: SiteReport): String = when {
        r.access == SiteAccess.UNSAFE -> "Do not enter this site. Ask authorized local environmental personnel to assess the report."
        r.access == SiteAccess.PERMISSION -> "Seek permission and barangay review before any field visit or cleanup."
        r.waterwayDistance == WaterwayDistance.NEAR && r.severity >= 2 -> "Request barangay review and discuss a supervised, safe cleanup and source prevention."
        r.repeatCount >= 3 -> "Request assessment of repeated litter and possible improvements to collection or prevention."
        else -> "Verify the observation with local personnel and choose an appropriate prevention or collection action."
    }
}
object ReportValidation {
    fun errors(r: SiteReport): List<String> = buildList {
        if (r.title.trim().length !in 4..80) add("Site name must be 4–80 characters.")
        if (!r.latitude.isFinite() || r.latitude !in -90.0..90.0) add("Latitude must be between -90 and 90.")
        if (!r.longitude.isFinite() || r.longitude !in -180.0..180.0) add("Longitude must be between -180 and 180.")
        if (r.severity !in 1..3) add("Choose a severity level.")
        if (r.repeatCount !in 1..5) add("Repeat observations must be between 1 and 5.")
        if (r.notes.length > 500) add("Notes cannot exceed 500 characters.")
    }
}
object CsvExport {
    private fun safe(text: String): String {
        val protected = if (text.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) "'" + text else text
        return "\"" + protected.replace("\"", "\"\"") + "\""
    }
    fun generate(reports: List<SiteReport>): String = buildString {
        appendLine("id,title,latitude,longitude,severity,waterway_estimate,repeats,access,score,notes,time_utc,next_step")
        reports.forEach { r ->
            val data = listOf(
                r.id.toString(), r.title, String.format(Locale.US, "%.6f", r.latitude),
                String.format(Locale.US, "%.6f", r.longitude), r.severity.toString(),
                r.waterwayDistance.name, r.repeatCount.toString(), r.access.name,
                PriorityEngine.score(r).total.toString(), r.notes,
                Instant.ofEpochMilli(r.createdAt).toString(), PriorityEngine.nextStep(r)
            )
            appendLine(data.joinToString(",") { safe(it) })
        }
    }
}
