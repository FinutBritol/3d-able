# 3D map changes

Files touched:
- app/src/main/java/com/example/dbtest/map/Building.kt     (FLOOR_HEIGHT, baseZ/topZ, FLAT_IDS)
- app/src/main/java/com/example/dbtest/map/SiteMapView.kt  (projection, extrusion renderer, colour-ID picking, yaw/pitch)
- app/src/main/java/com/example/dbtest/CampusMapActivity.kt (3D/2D button + tilt bar wiring)
- app/src/main/res/layout/campus_map.xml                    (view3dButton, tiltSeek)

Tuning knobs: FLOOR_HEIGHT and FLAT_IDS in Building.kt, default pitchDeg and wallShade() in SiteMapView.kt.
Not compiled on the machine that generated it: open in Android Studio, sync Gradle, build.
