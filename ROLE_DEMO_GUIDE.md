# DALUYAN v0.5 — Role-Adaptive Demonstration

## This is not a live government platform

The role selector contains six **local demonstration profiles**: Guest, Community reporter, Barangay reviewer, City coordinator, Field response, and System administrator. Switching profile on one phone changes which screens and local actions appear. It **does not authenticate an account**, assign a real LGU identity, transmit a report, or provide cross-device synchronization. All changes stay on the device; GitHub Actions can build a testable APK.

## Complete end-to-end demonstration

1. Select Community reporter. On Explore, pick a location on the actual Calapan map and choose **Record observation**; capture a photo if available, then save the report.
2. In My reports, open the case to inspect the original field observation and case history. It starts as **Submitted**.
3. Switch to the Barangay reviewer profile. Open Review, select the same case, choose **Reviewed and accepted**, write a meaningful justification, and confirm.
4. Switch to City coordinator → Overview. Open the verified case and choose **Intervention planned**; add a reason. Open it again to assign the **Calapan pilot field team** and document the assignment.
5. Switch to Field response member → My tasks. Open the assigned case, mark **Work in progress**, then **Outcome awaiting review** and add meaningful work details.
6. Return to City coordinator → Cases; inspect the history and **Outcome reviewed** or mark **Follow-up needed**. Reopening requires a new plan.
7. Switch to Community reporter → Updates to inspect the public-oriented status and events.
8. Switch to Administrator → Audit to inspect the local case history; Data to examine and export the bundled HOT/HDX GeoJSON and GeoPackage layers.

## Limits and acceptance conditions

- **No mock government accounts are valid official accounts.** For real production usage, add server-side authentication, verified organizational memberships, role- and jurisdiction-based authorization, protected photo uploads, a synchronization queue, and an immutable server-managed audit trail.
- **Case stages are locally enforceable only.** The transition rules block invalid steps for the selected demo role, but anyone with the phone may switch roles; this is not secure RBAC.
- Users must create actual observations themselves. No fake environmental incidents are bundled.
- The data bundle includes actual mapped waterways but straight-line proximity does not establish flood risk or pollution transport.
- The offline basemap is not packaged. Map backgrounds need internet; saved reports and GIS geometry can be used offline.
- Debug APK signatures may differ between CI runs. Export current reports before uninstalling older APKs.

## Proposed production backend architecture

Kotlin + Compose + Room/SQLite offline queue → authenticated API → PostgreSQL/PostGIS cases, memberships, case events, private evidence storage. Permissions must be verified by backend, not by hiding buttons. City and barangay authority requires a real partner's authorization.
