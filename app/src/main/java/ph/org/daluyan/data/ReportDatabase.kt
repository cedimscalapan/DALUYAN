package ph.org.daluyan.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import ph.org.daluyan.domain.*

/** Version 2 safely migrates observations created in v0.1.0. All records stay device-local. */
class ReportDatabase(context: Context) : SQLiteOpenHelper(context, "daluyan.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE reports (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL, lat REAL NOT NULL, lon REAL NOT NULL,
            severity INTEGER NOT NULL, waterway TEXT NOT NULL DEFAULT 'UNKNOWN',
            repeats INTEGER NOT NULL DEFAULT 1, access TEXT NOT NULL,
            notes TEXT NOT NULL, photo_uri TEXT, created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL, location_source TEXT NOT NULL DEFAULT 'MAP',
            accuracy REAL, action_status TEXT NOT NULL DEFAULT 'REPORTED',
            action_note TEXT NOT NULL DEFAULT '', action_at INTEGER
        )""".trimIndent())
        db.execSQL("CREATE INDEX idx_reports_created ON reports(created_at)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE reports ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE reports SET updated_at=created_at WHERE updated_at=0")
            db.execSQL("ALTER TABLE reports ADD COLUMN location_source TEXT NOT NULL DEFAULT 'MAP'")
            db.execSQL("ALTER TABLE reports ADD COLUMN accuracy REAL")
            db.execSQL("ALTER TABLE reports ADD COLUMN action_status TEXT NOT NULL DEFAULT 'REPORTED'")
            db.execSQL("ALTER TABLE reports ADD COLUMN action_note TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE reports ADD COLUMN action_at INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_reports_created ON reports(created_at)")
        }
    }
    private fun content(r: SiteReport) = ContentValues().apply {
        put("title", r.title.trim()); put("lat", r.latitude); put("lon", r.longitude)
        put("severity", r.severity); put("waterway","UNKNOWN"); put("repeats",1); put("access", r.access.name); put("notes", r.notes.trim())
        put("photo_uri", r.photoUri); put("created_at", r.createdAt); put("updated_at", r.updatedAt)
        put("location_source", r.locationSource.name); put("accuracy", r.accuracyMeters)
        put("action_status", r.status.name); put("action_note", r.actionNote.trim()); put("action_at",r.actionAt)
    }
    fun insert(r: SiteReport): Long {
        require(ReportValidation.problems(r).isEmpty()) { "Invalid report" }
        return writableDatabase.insertOrThrow("reports", null, content(r))
    }
    fun update(r: SiteReport): Boolean {
        require(r.id > 0 && ReportValidation.problems(r).isEmpty()) { "Invalid report" }
        return writableDatabase.update("reports",content(r),"id=?",arrayOf(r.id.toString())) == 1
    }
    fun all(): List<SiteReport> = buildList {
        readableDatabase.query("reports", null, null, null, null, null, "created_at DESC, id DESC").use { c ->
            fun idx(s:String)=c.getColumnIndexOrThrow(s)
            while(c.moveToNext()) {
                fun str(s:String)=c.getString(idx(s))
                fun lng(s:String)=c.getLong(idx(s))
                fun nullable(s:String)=if(c.isNull(idx(s))) null else str(s)
                fun nullableLong(s:String)=if(c.isNull(idx(s))) null else lng(s)
                add(SiteReport(
                    id=lng("id"),title=str("title"),latitude=c.getDouble(idx("lat")), longitude=c.getDouble(idx("lon")),
                    severity=c.getInt(idx("severity")), access=runCatching { SiteAccess.valueOf(str("access")) }.getOrDefault(SiteAccess.PERMISSION),
                    notes=str("notes"),photoUri=nullable("photo_uri"),createdAt=lng("created_at"),updatedAt=lng("updated_at"),
                    locationSource=runCatching{LocationSource.valueOf(str("location_source"))}.getOrDefault(LocationSource.MAP),
                    accuracyMeters=if(c.isNull(idx("accuracy"))) null else c.getFloat(idx("accuracy")),
                    status=runCatching {ActionStatus.valueOf(str("action_status"))}.getOrDefault(ActionStatus.REPORTED),
                    actionNote=str("action_note"),actionAt=nullableLong("action_at")
                ))
            }
        }
    }
    fun delete(id:Long):Boolean = writableDatabase.delete("reports","id=?",arrayOf(id.toString()))==1
}
