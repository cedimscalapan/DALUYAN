# Quality gate (DALUYAN Android MVP)

Automated CI must pass :app:testDebugUnitTest and :app:assembleDebug. Only a successful run with a nonempty artifact is considered a confirmed build. Site scores are transparent heuristic decision support, not verified environmental risk ratings.

## Phone smoke tests before pitching
- Map loads over internet with attribution.
- GPS permission granted/denied flows and map coordinate selection.
- Report name, latitude/longitude, notes, severity, repetitions, accessibility validation.
- Camera capture, cancellation, report deletion, and photo cleanup.
- SQLite records persist across reopening the app.
- Priority ranking and explanation match inputs.
- Airplane mode: saved reports, score, evidence and CSV export still work.
- Offline map *not* advertised as available; tiles need a network connection.
- Official 2025 water-quality records are distinguished from community litter observations.
- CSV export opens cleanly without spreadsheet-formula execution.
- All field work stays on safely accessible and permitted locations.
