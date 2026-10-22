# DALUYAN v0.4.1 validation

## Automated (GitHub Actions)

- [ ] HOT/HDX national waterways are prepared to a real Calapan pilot rectangle and GeoJSON data passes geometry, size, feature-count and provenance checks.
- [ ] The derived GeoPackage is readable and attached to the build artifacts.
- [ ] Both GeoJSON and GeoPackage are included in the APK assets.
- [ ] Kotlin unit tests pass.
- [ ] Gradle compiles an Android debug APK and uploads it as a workflow artifact.

## Manual checks on an Android device

- [ ] Top-bar content never overlaps status icons or display cutouts.
- [ ] Map tiles load over internet; mapped waterways show as colored lines; report markers remain visible.
- [ ] Open Evidence and confirm waterway data has loaded automatically, without selecting any file.
- [ ] Export GeoJSON and GeoPackage from Evidence; verify GeoJSON and open GeoPackage in QGIS.
- [ ] Tap a map point and successfully save, edit, delete and export an observation.
- [ ] Test GPS with permission allowed, denied and poor accuracy.
- [ ] Open the reporting bottom sheet and keyboard, then rotate the device. Back and Continue remain fully reachable above navigation.
- [ ] Disable internet and confirm that saved data and distance calculations still work.
- [ ] Expand and collapse government measurements, retaining source labels and disclaimers.
- [ ] Verify migration and data survival across an in-place upgrade when APK signatures match.

**Only GitHub Actions can establish successful compilation. This checklist does not claim physical-device tests were performed.**
