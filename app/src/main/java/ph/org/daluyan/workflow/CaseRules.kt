package ph.org.daluyan.workflow

/** All roles are LOCAL DEMONSTRATION PROFILES, not authenticated LGU identities. */
enum class DemoRole(val label: String, val description: String) {
    GUEST("Guest", "Explore published reference information"),
    COMMUNITY("Community reporter", "Document and follow up on an observation"),
    BARANGAY("Barangay reviewer", "Review environmental evidence"),
    COORDINATOR("City coordinator", "Plan, assign, and validate interventions"),
    FIELD("Field response member", "Document assigned environmental actions"),
    ADMIN("System administrator", "Inspect the demo setup, GIS and audit history")
}

enum class CaseStage(val title: String, val publicLabel: String) {
    SUBMITTED("Submitted", "Submitted"),
    CLARIFICATION("Needs clarification", "More information requested"),
    VERIFIED("Reviewed and accepted", "Reviewing"),
    REJECTED("Not accepted", "Review finished"),
    PLANNED("Intervention planned", "Action in progress"),
    ASSIGNED("Assigned to field team", "Action in progress"),
    IN_PROGRESS("Work in progress", "Action in progress"),
    AWAITING_REVIEW("Outcome awaiting review", "Action in progress"),
    CLOSED("Outcome reviewed", "Resolved"),
    FOLLOW_UP("Follow-up needed", "Reviewing")
}

data class CaseItem(
    val reportId: Long,
    val stage: CaseStage = CaseStage.SUBMITTED,
    val owner: String = "community-demo",
    val assignee: String = "",
    val dueAt: Long? = null,
    val lastNote: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

data class CaseEvent(
    val id: Long = 0,
    val reportId: Long,
    val actor: DemoRole,
    val from: CaseStage?,
    val to: CaseStage,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

object CaseRules {
    private val steps: Map<CaseStage, List<Pair<DemoRole, CaseStage>>> = mapOf(
        CaseStage.SUBMITTED to listOf(DemoRole.BARANGAY to CaseStage.VERIFIED, DemoRole.BARANGAY to CaseStage.CLARIFICATION, DemoRole.BARANGAY to CaseStage.REJECTED),
        CaseStage.CLARIFICATION to listOf(DemoRole.COMMUNITY to CaseStage.SUBMITTED, DemoRole.BARANGAY to CaseStage.REJECTED),
        CaseStage.VERIFIED to listOf(DemoRole.COORDINATOR to CaseStage.PLANNED, DemoRole.COORDINATOR to CaseStage.FOLLOW_UP),
        CaseStage.PLANNED to listOf(DemoRole.COORDINATOR to CaseStage.ASSIGNED),
        CaseStage.ASSIGNED to listOf(DemoRole.FIELD to CaseStage.IN_PROGRESS, DemoRole.COORDINATOR to CaseStage.PLANNED),
        CaseStage.IN_PROGRESS to listOf(DemoRole.FIELD to CaseStage.AWAITING_REVIEW),
        CaseStage.AWAITING_REVIEW to listOf(DemoRole.COORDINATOR to CaseStage.CLOSED, DemoRole.COORDINATOR to CaseStage.FOLLOW_UP),
        CaseStage.FOLLOW_UP to listOf(DemoRole.COORDINATOR to CaseStage.PLANNED),
        CaseStage.CLOSED to listOf(DemoRole.COORDINATOR to CaseStage.FOLLOW_UP)
    )

    fun next(role: DemoRole, stage: CaseStage): List<CaseStage> =
        steps[stage].orEmpty().filter { it.first == role }.map { it.second }

    fun error(role: DemoRole, item: CaseItem, to: CaseStage, note: String, assignee: String = item.assignee): String? {
        if (to !in next(role, item.stage)) return "This role cannot make that change from ${item.stage.title}."
        if (note.trim().length !in 12..800) return "Give a clear reason or outcome (12–800 characters)."
        if (to == CaseStage.ASSIGNED && assignee.isBlank()) return "Choose a responsible field team."
        if (role == DemoRole.FIELD && item.assignee.isBlank()) return "No team has been assigned to this case."
        if (to == CaseStage.CLOSED && item.stage != CaseStage.AWAITING_REVIEW) return "The work must be reviewed first."
        return null
    }
}
