package ph.org.daluyan.workflow

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import ph.org.daluyan.domain.SiteReport

/** Separate local demo database. No local data can grant real server/organizational authority. */
class CaseStore(context: Context) : SQLiteOpenHelper(context, "daluyan_demo_cases.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE cases (report_id INTEGER PRIMARY KEY, stage TEXT NOT NULL,
            owner TEXT NOT NULL, assignee TEXT NOT NULL DEFAULT '', due_at INTEGER,
            last_note TEXT NOT NULL DEFAULT '', updated_at INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE case_events (id INTEGER PRIMARY KEY AUTOINCREMENT,
            report_id INTEGER NOT NULL, actor TEXT NOT NULL, from_stage TEXT,
            to_stage TEXT NOT NULL, note TEXT NOT NULL, event_at INTEGER NOT NULL)""")
        db.execSQL("CREATE INDEX idx_case_events_report ON case_events(report_id, id)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Case database migration missing")
    }
    private fun stage(s: String): CaseStage = CaseStage.entries.firstOrNull { it.name == s } ?: CaseStage.SUBMITTED
    private fun item(cursor: android.database.Cursor): CaseItem {
        fun col(name: String) = cursor.getColumnIndexOrThrow(name)
        return CaseItem(cursor.getLong(col("report_id")),stage(cursor.getString(col("stage"))),
            cursor.getString(col("owner")),cursor.getString(col("assignee")),
            if(cursor.isNull(col("due_at"))) null else cursor.getLong(col("due_at")),
            cursor.getString(col("last_note")),cursor.getLong(col("updated_at")))
    }
    fun synchronize(reports: List<SiteReport>) {
        val db=writableDatabase
        db.beginTransaction()
        try {
            reports.forEach { report ->
                val values = ContentValues().apply {
                    put("report_id", report.id); put("stage", CaseStage.SUBMITTED.name)
                    put("owner", "community-demo"); put("assignee", "")
                    put("last_note", "Imported local observation — not an official submission")
                    put("updated_at", report.createdAt)
                }
                val id = db.insertWithOnConflict("cases", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                if(id != -1L) {
                    val event = ContentValues().apply {
                        put("report_id", report.id); put("actor", DemoRole.COMMUNITY.name)
                        put("to_stage", CaseStage.SUBMITTED.name)
                        put("note", "Local case created for demonstration")
                        put("event_at",report.createdAt)
                    }
                    db.insertOrThrow("case_events",null,event)
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun all(): List<CaseItem> = buildList {
        readableDatabase.query("cases",null,null,null,null,null,"updated_at DESC").use { c ->
            while(c.moveToNext()) add(item(c))
        }
    }
    fun events(reportId: Long): List<CaseEvent> = buildList {
        readableDatabase.query("case_events",null,"report_id=?",arrayOf(reportId.toString()),null,null,"id DESC").use { c ->
            fun col(s:String)=c.getColumnIndexOrThrow(s)
            while(c.moveToNext()) add(CaseEvent(c.getLong(col("id")), reportId,
                DemoRole.entries.firstOrNull{it.name==c.getString(col("actor"))}?:DemoRole.COMMUNITY,
                if(c.isNull(col("from_stage"))) null else stage(c.getString(col("from_stage"))),
                stage(c.getString(col("to_stage"))),c.getString(col("note")),c.getLong(col("event_at"))))
        }
    }
    /** The caller-supplied current stage is validated AGAINST the stored stage inside one transaction. */
    fun transition(reportId: Long, role: DemoRole, to: CaseStage, note: String, assignee: String): String? {
        val db=writableDatabase
        db.beginTransaction()
        try {
            val existing=db.query("cases",null,"report_id=?",arrayOf(reportId.toString()),null,null,null).use{c->
                if(c.moveToFirst())item(c)else null
            } ?: return "Case is missing."
            val err=CaseRules.error(role,existing,to,note,assignee)
            if(err!=null)return err
            val now=System.currentTimeMillis()
            val values=ContentValues().apply{
                put("stage",to.name);put("last_note",note.trim());put("updated_at",now)
                if(to==CaseStage.ASSIGNED)put("assignee",assignee.trim())
            }
            check(db.update("cases",values,"report_id=?",arrayOf(reportId.toString()))==1)
            val event=ContentValues().apply{
                put("report_id",reportId);put("actor",role.name);put("from_stage",existing.stage.name)
                put("to_stage",to.name);put("note",note.trim());put("event_at",now)
            }
            db.insertOrThrow("case_events",null,event)
            db.setTransactionSuccessful()
            return null
        }finally{db.endTransaction()}
    }
    fun removeOrphanCase(reportId:Long) {
        val db=writableDatabase;db.beginTransaction()
        try {db.delete("case_events","report_id=?",arrayOf(reportId.toString()));db.delete("cases","report_id=?",arrayOf(reportId.toString()));db.setTransactionSuccessful()}
        finally {db.endTransaction()}
    }
}
