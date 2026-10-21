# DALUYAN — Kotlin Android environmental observation MVP

A native Kotlin + Jetpack Compose app for a Calapan, Oriental Mindoro pilot. Uses an OpenStreetMap-derived MapLibre map, local SQLite site reports, a transparent prioritization heuristic, and a separately labeled selection of historical DENR-EMB MIMAROPA water quality measurements.

## Status
Development build. CI compilation and real-device functionality must be verified before public demonstration. Real map tiles require internet. Field reports and CSV export remain on the phone. No user accounts or official LGU verification in the MVP. Do not visit unsafe or restricted locations.

## APK
Open GitHub Actions > Android Build > latest **successful** run > Artifacts > DALUYAN-debug-apk; extract app-debug.apk and install on an Android device (Android 8.0+).

### Attribution and provenance
Map: OpenFreeMap Liberty style, based on OpenStreetMap data. Official water quality: https://mimaropa.emb.gov.ph/water-quality-management/ (2025, fecal coliform; not plastic quantities). Monitoring stations are not placed on the map without verified coordinates. Scores are illustrative decision-support, not model predictions or official environmental risk ratings.
