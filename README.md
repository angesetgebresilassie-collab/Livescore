# Glass Widgets

Android home-screen widgets drawn from code (Kotlin, Jetpack Glance), with a Liquid Glass style editor app.

- Music widget (2x2) with three presets: dark glass, frosted, full art
- Big glass clock widget
- Weather widget (Open-Meteo, set your coordinates in the app)

Everything visual is a field in `app/src/main/java/app/glasswidgets/Style.kt`.

## First run
1. Install the debug APK (built by GitHub Actions, see the Actions tab, artifact `glasswidgets-debug-apk`).
2. Open the app, tap "Open notification access" and enable it (needed for the playing song and album art).
3. Enter your coordinates and tap "Save and refresh".
4. Add the widgets from the home-screen widget picker.

## Known limits
- Glass inside widgets is drawn (tint, gradient, rim), not live blur. Real blur only works inside the app.
- The clock ticks every minute while the app process is alive.
- `com/livescore/**` (old Livescore files) is excluded from the build in `app/build.gradle.kts`. Delete those files, then remove the exclude.
