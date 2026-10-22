# DALUYAN v0.5 — role-adaptive Kotlin environmental case workflow

Native Kotlin Android (Jetpack Compose, MapLibre, SQLite), six demonstrable local role profiles, actual HOT/OpenStreetMap-derived Calapan waterways, official historical DENR data and an explainable local case lifecycle.

**Important: organizational profiles are locally simulated and NOT real accounts or official LGU verification.** No multi-user backend is connected. Private reports stay on the phone. Refer to [ROLE_DEMO_GUIDE.md](ROLE_DEMO_GUIDE.md) for the actual end-to-end demonstration and limitations.

## Role interfaces

- Guest: Explore, Learn, About; no access to locally submitted private map markers.
- Community reporter: Explore, My reports, Updates; real map-based report form, GPS, evidence photo, local case history and CSV export.
- Barangay reviewer: Review queue, Map, Cases; verify, request clarification or reject with a mandatory justification.
- City coordinator: Overview, Cases, Planning; action planning, field-team assignment, outcome review and reopening.
- Field response member: My tasks, Map, History; work on assigned cases and report outcomes.
- System administrator: Accounts, GIS data, Audit; inspect local demonstration profiles, evidence provenance and case events. Cannot pretend to provision real identities.

## GIS and builds

GitHub Actions prepares real Calapan HOT/HDX waterways into GeoJSON and GeoPackage, adds data to the APK and runs unit tests. Runtime distance calculations use bundled GeoJSON. Historical DENR fecal-coliform records are separately labeled and never treated as solid waste measurements. Map basemap requires internet.

Actions → latest successful Android workflow → artifact `DALUYAN-v0.5-role-workspaces-debug-apk` → unzip `app-debug.apk`.

## Safety, security and privacy

This is a demonstration, not a government deployment. No real LGU authorization, registration, cross-device synchronization, user verification or secure server audit is implemented. Local reports and photos are not automatically uploaded. Case role switching is intentionally open for judges and **must not be reused as authorization logic for production**. Never encourage unsafe field access. GitHub's per-run debug signing may prevent in-place upgrades.
