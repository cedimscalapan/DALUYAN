# DALUYAN · Environmental action planner

**Version 0.2.0** · Native Android / Kotlin / Jetpack Compose · Oriental Mindoro pilot

## What is functional

- **Map:** MapLibre with an OSM-derived online basemap, GPS selection, field report markers.
- **Report:** Short three-stage form, evidence photo, local-only storage, edit/delete, draft retention across rotation.
- **Actions:** Separate environmental concern, evidence confidence and site-access rules; location/time-based recurrence; self-reported action tracking and documented completion.
- **GIS:** Import verified, waterway-only GeoJSON (`FeatureCollection`, `LineString` / `MultiLineString`, max 3 MB). Calculate nearest straight-line distance offline. No invented waterways; proximity reads as unknown until a valid layer is imported. These distances do not establish drainage connectivity.
- **Evidence:** Historical 2025 DENR-EMB MIMAROPA data, with units and source link. Fecal coliform is not a measurement of solid litter.
- **Privacy:** No accounts, uploads, remote administration, or alleged official verification. Observations and photos remain on device until the user exports a CSV. Personal data in photographs should be avoided.
- **Upgrade:** Database v1->v2 migration preserves earlier reports, with new action fields populated as unverified defaults.

## Installing from GitHub Actions

Navigate to **Actions → DALUYAN · Android CI / APK → latest successful run → Artifacts → DALUYAN-v0.2-debug-apk**. Unzip the artifact and install `app-debug.apk` on an Android 8+ phone. Debug builds on rotating CI runners may have changing signing keys; if Android rejects an upgrade, **export your records first**, then make a deliberate installation decision. A stable signed release is still required for distribution.

## Real waterway data — manual preparation

Use https://overpass-turbo.eu/ with this query and review the extracted network in QGIS before importing:

```overpass
[out:json][timeout:45];
(
  way["waterway"~"^(river|stream|canal|drain)$"](13.25,121.03,13.52,121.38);
);
out geom;
```

Export as GeoJSON. Filter and confirm features are waterway lines only, check the extent and data license, and import from **Evidence → Import waterway GeoJSON**. The app does not automatically download or assert official river coordinates. Maps require a connection; imported waterway geometry and saved reports work offline. OSM attribution must be retained.

## What remains out of scope

Official reviewer authorization, server synchronization, validated ecological risk prediction, real-time water quality, reliable packaged offline basemap, and a production keystore. These are not implied by the current build.

## Testing

Run `gradle :app:testDebugUnitTest :app:assembleDebug` with JDK 17, Android 35 SDK and Gradle 8.11.1. CI performs this on every push. Real phone testing is still required for GPS, camera, rotation, SQLite migration and MapLibre lifecycle.
