package ph.org.daluyan.data

import android.content.Context

data class WaterResult(
    val waterbody: String, val station: String, val classification: String,
    val indicator: String, val value: Int, val guideline: Int,
    val unit: String, val year: Int, val period: String
)

object OfficialData {
    const val SOURCE_URL = "https://mimaropa.emb.gov.ph/water-quality-management/"
    const val SOURCE_NAME = "DENR-EMB MIMAROPA • CY 2025 Annual Monitoring Results"
    fun read(context: Context): List<WaterResult> = context.assets.open("calapan_2025.csv")
        .bufferedReader().useLines { lines ->
            lines.drop(1).filter { it.isNotBlank() }.map { line ->
                val c = line.split(',')
                require(c.size == 9) { "Official evidence CSV has an invalid row" }
                WaterResult(c[0], c[1], c[2], c[3], c[4].toInt(), c[5].toInt(), c[6], c[7].toInt(), c[8])
            }.toList()
        }
}
