package ph.org.daluyan.domain

import org.junit.Assert.*
import org.junit.Test

class PriorityTest {
    private fun report(
        severity: Int = 3, distance: WaterwayDistance = WaterwayDistance.NEAR,
        count: Int = 5, access: SiteAccess = SiteAccess.PUBLIC,
        lat: Double = 13.4, lon: Double = 121.2, name: String = "Public site"
    ) = SiteReport(title=name,latitude=lat,longitude=lon,severity=severity,
        waterwayDistance=distance,repeatCount=count,access=access,notes="")
    @Test fun maximumIs100() { assertEquals(100, PriorityEngine.score(report()).total) }
    @Test fun nearOutranksFar() { assertTrue(PriorityEngine.score(report()).total > PriorityEngine.score(report(distance=WaterwayDistance.UNKNOWN)).total) }
    @Test fun restrictedSiteIsInaccessible() { assertTrue(PriorityEngine.nextStep(report(access=SiteAccess.UNSAFE)).contains("Do not enter")) }
    @Test fun invalidCoordinatesRejected() { assertTrue(ReportValidation.errors(report(lat=Double.NaN)).isNotEmpty()) }
    @Test fun shortNameRejected() { assertTrue(ReportValidation.errors(report(name="A")).isNotEmpty()) }
    @Test fun repeatCountRejected() { assertTrue(ReportValidation.errors(report(count=8)).isNotEmpty()) }
    @Test fun csvEscapesSpreadsheetFormulas() { assertTrue(CsvExport.generate(listOf(report(name="=1+1"))).contains("'=1+1")) }
    @Test fun csvEscapesQuotes() { assertTrue(CsvExport.generate(listOf(report(name="A \"river\""))).contains("\"\"river\"\"")) }
}
