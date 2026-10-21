# Validation plan · DALUYAN 0.2.0

Status: tests executed in GitHub CI only after its build run completes; device-level tests are not yet confirmed.

| Area | Acceptance check |
|---|---|
| Form | Missing coordinates rejected, three steps usable by touch, draft survives rotation |
| GPS | Permission denied and no fix handled, accuracy disclosed |
| Reporting | Save/edit/delete persist across restarts, photos remain on-device |
| Migration | Install 0.1.0, save report, install version 0.2.0 signed with same key, verify preserved records |
| GIS | Waterway GeoJSON imported; line-to-point distance checked against QGIS; missing geometry remains unknown |
| Offline | Existing reports and imported geometry usable with airplane mode; basemap not guaranteed offline |
| Action | Completed requires descriptive results, remains self-reported |
| Export | CSV opens without formula execution, no evidence is uploaded |
| Security | No claim of official government verification; local-only records |
| Updates | Consistent signing required for seamless updates; debug artifacts have CI signing limitations |

For actual competition claims, record device model, Android version, expected/actual results, screenshots and tester name.
