package ph.org.daluyan.domain

import org.junit.Assert.*
import org.junit.Test

class PriorityTest {
    private fun report(id:Long=1,lat:Double=13.39,lon:Double=121.18,severity:Int=2,photo:String?=null,
                       access:SiteAccess=SiteAccess.PUBLIC,title:String="Riverside site",stamp:Long=1700000000000L) =
        SiteReport(id=id,title=title,latitude=lat,longitude=lon,severity=severity,
            access=access,notes="",photoUri=photo,createdAt=stamp)

    @Test fun highConcernRequiresEvidenceToBeComplete() {
        val r=report(severity=3)
        val unknown=AssessmentEngine.assess(r,listOf(r),null)
        assertEquals("Awaiting geographic data",unknown.concernLabel)
        assertNull(unknown.distanceMeters)
        assertEquals(60,unknown.concern)
    }
    @Test fun nearWaterwayRaisesConcern() {
        val r=report()
        val near=AssessmentEngine.assess(r,listOf(r),20.0)
        val far=AssessmentEngine.assess(r,listOf(r),400.0)
        assertTrue(near.concern>far.concern)
    }
    @Test fun unsafeNeverReadyForVolunteerAction() {
        val r=report(access=SiteAccess.UNSAFE)
        val result=AssessmentEngine.assess(r,listOf(r),10.0)
        assertFalse(result.ready)
        assertTrue(result.nextStep.contains("Do not enter"))
    }
    @Test fun distinctDayReportsProduceRecurrence() {
        val a=report(id=1)
        val b=report(id=2,stamp=a.createdAt + 86400000)
        assertEquals(2,AssessmentEngine.assess(a,listOf(a,b),null).recurrence)
    }
    @Test fun sameDayRepeatedReportsDoNotInflateRecurrence() {
        val a=report(id=1)
        val b=report(id=2,stamp=a.createdAt + 3600000)
        assertEquals(1,AssessmentEngine.assess(a,listOf(a,b),null).recurrence)
    }
    @Test fun gpsWithoutAccuracyDoesNotEarnGpsConfidence() {
        val r=report()
        val gps=r.copy(locationSource=LocationSource.GPS,accuracyMeters=null)
        assertEquals(AssessmentEngine.assess(r,listOf(r),null).confidence,
            AssessmentEngine.assess(gps,listOf(gps),null).confidence)
    }
    @Test fun shortNamesAndInvalidCoordinatesRejected() {
        assertTrue(ReportValidation.problems(report(title="A")).isNotEmpty())
        assertTrue(ReportValidation.problems(report(lat=Double.NaN)).isNotEmpty())
    }
    @Test fun completionRequiresRecordedOutcome() {
        val invalid=report().copy(status=ActionStatus.COMPLETED,actionNote="done",actionAt=System.currentTimeMillis())
        assertTrue(ReportValidation.problems(invalid).isNotEmpty())
        val valid=invalid.copy(actionNote="Documented supervised collection completed.")
        assertTrue(ReportValidation.problems(valid).isEmpty())
    }
    @Test fun pointToSegmentWorks() {
        val d=GeoMath.pointSegmentMeters(13.39,121.18,13.39,121.17,13.39,121.19)
        assertEquals(0.0,d,0.2)
        val off=GeoMath.pointSegmentMeters(13.391,121.18,13.39,121.17,13.39,121.19)
        assertTrue(off>100.0 && off<120.0)
    }
    @Test fun csvEscapesFormulasAndQuotes() {
        val r=report(title="=SUM(1,2) \"test\"")
        val csv=CsvExport.generate(listOf(r)){AssessmentEngine.assess(it,listOf(it),null)}
        assertTrue(csv.contains("'=SUM"))
        assertTrue(csv.contains("\"\"test\"\""))
    }
}
