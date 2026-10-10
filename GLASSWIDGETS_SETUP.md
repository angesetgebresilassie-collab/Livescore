# Glass Widgets setup

Not compiled or run yet. Expect a few small fixes on first build.

## Gradle (app module)
- `minSdk 31` (rounded corners in Glance need it)
- Compose enabled (Glance needs the Compose compiler plugin)

```kotlin
implementation("androidx.glance:glance-appwidget:1.1.1")
implementation("androidx.work:work-runtime-ktx:2.9.1")
implementation("androidx.core:core-ktx:1.13.1")
```

## AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.INTERNET" />

<application android:name="app.glasswidgets.App" ...>

  <receiver android:name="app.glasswidgets.MusicWidgetReceiver" android:exported="true">
    <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE" /></intent-filter>
    <meta-data android:name="android.appwidget.provider" android:resource="@xml/music_widget_info" />
  </receiver>
  <receiver android:name="app.glasswidgets.ClockWidgetReceiver" android:exported="true">
    <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE" /></intent-filter>
    <meta-data android:name="android.appwidget.provider" android:resource="@xml/clock_widget_info" />
  </receiver>
  <receiver android:name="app.glasswidgets.WeatherWidgetReceiver" android:exported="true">
    <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE" /></intent-filter>
    <meta-data android:name="android.appwidget.provider" android:resource="@xml/weather_widget_info" />
  </receiver>

  <service android:name="app.glasswidgets.MediaListener" android:exported="true"
      android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
    <intent-filter><action android:name="android.service.notification.NotificationListenerService" /></intent-filter>
  </service>
</application>
```

## res/xml (three files)
`music_widget_info.xml`
```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="110dp" android:minHeight="110dp"
    android:targetCellWidth="2" android:targetCellHeight="2"
    android:resizeMode="horizontal|vertical" android:widgetCategory="home_screen"
    android:updatePeriodMillis="0" android:initialLayout="@android:layout/list_content" />
```
`clock_widget_info.xml`: same, with `minWidth="250dp" minHeight="110dp" targetCellWidth="4" targetCellHeight="2"`.
`weather_widget_info.xml`: same, with `minWidth="110dp" minHeight="50dp" targetCellWidth="2" targetCellHeight="1"`.

## First run
1. Open Settings > Notification access and enable the app (needed to read the playing song and album art).
2. Call `StyleStore.saveWeather(ctx, WeatherStyle(lat = ..., lon = ...))` once with your coordinates (the editor will do this later).
3. Add the three widgets from the home-screen widget picker.

## Customizing
Everything visual is a field in `Style.kt`. Save a changed style with `StyleStore.saveMusic/saveClock/saveWeather`, then call `MusicWidget().updateAll(ctx)` (or the clock/weather one) and it redraws.

Music presets: `MusicStyle.preset(MusicPreset.DARK_GLASS | FROSTED_LIGHT | ART_FULL)`.
The 4th look in the reference picture (pink card) is `ART_FULL` with a different album, since the color comes from the art.

## Known limits
- Glass in widgets is drawn (tint, gradient, rim), not live blur. Real blur only works inside the app.
- The clock ticks every minute while the app process is alive. For guaranteed ticks after the process is killed, add a foreground service or accept a few minutes of lag.
- The old Livescore files (`local.properties.example`, `PermissionGate.kt`, `PermissionScreen.kt`) are still in the repo and need to be deleted by hand.
