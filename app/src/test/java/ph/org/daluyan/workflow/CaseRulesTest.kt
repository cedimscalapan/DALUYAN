package ph.org.daluyan.workflow

import org.junit.Assert.*
import org.junit.Test

class CaseRulesTest {
    private val submitted = CaseItem(42L)
    @Test fun reporterCannotApproveOwnCase() {
        assertNotNull(CaseRules.error(DemoRole.COMMUNITY,submitted,CaseStage.VERIFIED,"I approve this report"))
    }
    @Test fun barangayCanVerifyWithReason() {
        assertNull(CaseRules.error(DemoRole.BARANGAY,submitted,CaseStage.VERIFIED,"Evidence reviewed for pilot"))
    }
    @Test fun coordinatorCannotSkipReview() {
        assertNotNull(CaseRules.error(DemoRole.COORDINATOR,submitted,CaseStage.ASSIGNED,"Assign to pilot field team"))
    }
    @Test fun fieldCannotCloseUnreviewedCase() {
        val awaiting=CaseItem(42L,CaseStage.AWAITING_REVIEW, assignee="Pilot team")
        assertNotNull(CaseRules.error(DemoRole.FIELD,awaiting,CaseStage.CLOSED,"I completed the assigned task"))
    }
    @Test fun assignmentRequiresTeam() {
        val planned=CaseItem(42L,CaseStage.PLANNED)
        assertNotNull(CaseRules.error(DemoRole.COORDINATOR,planned,CaseStage.ASSIGNED,"Assign after site check",assignee=""))
        assertNull(CaseRules.error(DemoRole.COORDINATOR,planned,CaseStage.ASSIGNED,"Assign after site check",assignee="Pilot team"))
    }
    @Test fun closingRequiresCoordinatorReview() {
        val pending=CaseItem(42L,CaseStage.AWAITING_REVIEW,assignee="Pilot team")
        assertNull(CaseRules.error(DemoRole.COORDINATOR,pending,CaseStage.CLOSED,"Outcome documents inspected"))
    }
    @Test fun shortJustificationRejected() {
        assertNotNull(CaseRules.error(DemoRole.BARANGAY,submitted,CaseStage.VERIFIED,"ok"))
    }
}
