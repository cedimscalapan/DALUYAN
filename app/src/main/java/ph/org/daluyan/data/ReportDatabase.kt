package ph.org.daluyan.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import ph.org.daluyan.domain.*

/** Unverified, private device-local reports. No administrative permissions or sync. */
class ReportDatabase(context: Context): SQLiteOpenHelper(context, "daluyan.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE reports (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL, lat REAL NOT NULL, lon REAL NOT NULL,
            severity INTEGER NOT NULL, waterway TEXT NOT NULL,
            repeats INTEGER NOT NULL, access TEXT NOT NULL,
            notes TEXT NOT NULL, photo_uri TEXT, created_at INTEGER NOT NULL
        )""".trimIndent())
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Database migration must be implemented before changing version.")
    }
    fun insert(r: SiteReport): Long {
        require(ReportValidation.errors(r).isEmpty()) { "Invalid observation" }
        val row = ContentValues().apply {
            put("title", r.title.trim())
            put("lat", r.latitude)
            put("lon", r.longitude)
            put("severity", r.severity)
            put("waterway", r.waterwayDistance.name)
            put("repeats", r.repeatCount)
            put("access", r.access.name)
            put("notes", r.notes)
            put("photo_uri", r.photoUri)
            put("created_at", r.createdAt)
        }
        return writableDatabase.insertOrThrow("reports", null, row)
    }
    fun all(): List<SiteReport> = buildList {
        readableDatabase.query("reports", null, null, null, null, null, "created_at DESC, id DESC").use { c ->
            fun idx(name: String) = c.getColumnIndexOrThrow(name)
            while (c.moveToNext()) {
                add(SiteReport(
                    id = c.getLong(idx("id")), title = c.getString(idx("title")),
                    latitude = c.getDouble(idx("lat")), longitude = c.getDouble(idx("lon")),
                    severity = c.getInt(idx("severity")),
                    waterwayDistance = WaterwayDistance.valueOf(c.getString(idx("waterway"))),
                    repeatCount = c.getInt(idx("repeats")),
                    access = SiteAccess.valueOf(c.getString(idx("access"))),
                    notes = c.getString(idx("notes")),
                    photoUri = if (c.isNull(idx("photo_uri"))) null else c.getString(idx("photo_uri")),
                    createdAt = c.getLong(idx("created_at"))
                ))
            }
        }
    }
    fun delete(id: Long): Boolean = writableDatabase.delete("reports","id=?",arrayOf(id.toString())) == 1
}
