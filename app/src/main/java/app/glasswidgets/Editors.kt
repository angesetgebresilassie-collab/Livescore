package app.glasswidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ───────────── Live previews (same renderer the widgets use) ─────────────

private fun sampleArt(): Bitmap {
    val b = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    c.drawRect(0f, 0f, 400f, 400f, Paint().apply {
        shader = LinearGradient(0f, 0f, 400f, 400f, 0xFFFFE600.toInt(), 0xFFE91E63.toInt(), Shader.TileMode.CLAMP)
    })
    c.drawCircle(200f, 170f, 90f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66000000 })
    return b
}

@Composable
fun MusicPreview(s: MusicStyle, size: Dp = 200.dp) {
    val d = LocalDensity.current.density
    val px = (size.value * d).toInt()
    val art = remember { NowPlaying.art ?: sampleArt() }
    val bmp = remember(s, px) { GlassRenderer.musicCard(art, s, px, px, d).asImageBitmap() }
    val text = Color(s.textColor)
    Box(Modifier.size(size)) {
        Image(bmp, null, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Bottom) {
            Text(NowPlaying.title.ifEmpty { "See You Again" }, color = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (s.showArtist) {
                Text(NowPlaying.artist.ifEmpty { "Charlie Puth" }, color = text.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun ClockPreview(s: ClockStyle, width: Dp = 300.dp, height: Dp = 150.dp) {
    val d = LocalDensity.current.density
    val w = (width.value * d).toInt()
    val h = (height.value * d).toInt()
    val now = LocalDateTime.now()
    val time = now.format(DateTimeFormatter.ofPattern(if (s.use24h) "H:mm" else "h:mm"))
    val bmp = remember(s, w, h, time) { GlassRenderer.clock(time, s, w, h, d).asImageBitmap() }
    Column(
        Modifier.width(width).clip(RoundedCornerShape(24.dp)).background(Color(0x66000000)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (s.showDate) {
            Text(now.format(DateTimeFormatter.ofPattern("EEE d MMM")), color = Color(s.textColor), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Image(bmp, null, Modifier.fillMaxWidth().height(height))
    }
}

@Composable
fun WeatherPreview(s: WeatherStyle) {
    val has = WeatherState.cond != "—"
    val main = Color(s.textColor)
    Column(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0x66000000)).padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text("${if (has && WeatherState.night) "☾" else "☀"} ${if (has) WeatherState.temp else 27}°", color = main, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(if (has) WeatherState.cond else "Clear", color = main, fontSize = 16.sp)
        Text("H:${if (has) WeatherState.hi else 33}° L:${if (has) WeatherState.lo else 24}°", color = main.copy(alpha = 0.6f), fontSize = 14.sp)
    }
}

// ───────────── Home ─────────────

@Composable
fun HomeScreen(open: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val music = remember { StyleStore.music(ctx) }
    val clock = remember { StyleStore.clock(ctx) }
    val weather = remember { StyleStore.weather(ctx) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column {
            Text("Glass Widgets", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("Tap a widget to customize it", color = Color(0x99FFFFFF), fontSize = 15.sp)
        }
        WidgetCard("Music", "Now playing card", { open(Screen.MUSIC) }) { MusicPreview(music, 150.dp) }
        WidgetCard("Clock", "Big glass digits", { open(Screen.CLOCK) }) { ClockPreview(clock, 260.dp, 110.dp) }
        WidgetCard("Weather", "Temperature, condition, high and low", { open(Screen.WEATHER) }) { WeatherPreview(weather) }
        GlassPanel {
            PanelTitle("Now playing access")
            Text(
                "Allow notification access so the music widget can read the current song and album art.",
                color = Color(0xB3FFFFFF), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp),
            )
            Button(
                onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                modifier = Modifier.padding(top = 12.dp),
            ) { Text("Open notification access") }
        }
    }
}

@Composable
private fun WidgetCard(title: String, subtitle: String, onClick: () -> Unit, preview: @Composable () -> Unit) {
    GlassPanel(Modifier.clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                PanelTitle(title)
                Text(subtitle, color = Color(0x99FFFFFF), fontSize = 13.sp)
            }
            Text("Customize ›", color = Color.White, fontSize = 14.sp)
        }
        Box(Modifier.fillMaxWidth().padding(top = 14.dp), contentAlignment = Alignment.Center) { preview() }
    }
}

// ───────────── Editor scaffolding ─────────────

private fun pinWidget(ctx: Context, receiver: Class<*>) {
    val m = AppWidgetManager.getInstance(ctx)
    if (m.isRequestPinAppWidgetSupported) m.requestPinAppWidget(ComponentName(ctx, receiver), null, null)
}

/** Waits a moment after the last change, then saves and redraws the real widget. */
@Composable
private fun <T> PersistEffect(key: T, save: suspend () -> Unit) {
    LaunchedEffect(key) {
        delay(350)
        save()
    }
}

@Composable
private fun EditorScaffold(
    title: String,
    onBack: () -> Unit,
    receiver: Class<*>,
    preview: @Composable () -> Unit,
    controls: @Composable ColumnScope.() -> Unit,
) {
    val ctx = LocalContext.current
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Back", color = Color.White, fontSize = 16.sp) }
            Spacer(Modifier.weight(1f))
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { pinWidget(ctx, receiver) }) { Text("Add", color = Color.White, fontSize = 16.sp) }
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) { preview() }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = controls,
        )
    }
}

// ───────────── Music ─────────────

@Composable
fun MusicEditor(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var s by remember { mutableStateOf(StyleStore.music(ctx)) }
    PersistEffect(s) { StyleStore.saveMusic(ctx, s); MusicWidget().updateAll(ctx) }

    EditorScaffold("Music widget", onBack, MusicWidgetReceiver::class.java, { MusicPreview(s, 200.dp) }) {
        GlassPanel {
            PanelTitle("Style")
            Segmented(listOf("Dark glass", "Frosted", "Full art"), s.preset.ordinal) {
                s = MusicStyle.preset(MusicPreset.values()[it])
            }
        }
        GlassPanel {
            PanelTitle("Shape")
            SliderRow("Corner radius", s.cornerDp, 0f..48f, { s = s.copy(cornerDp = it) }) { "${it.toInt()} dp" }
            if (s.preset == MusicPreset.ART_FULL) {
                SliderRow("Blur fade", s.blurFade, 0.1f..0.9f, { s = s.copy(blurFade = it) })
            } else {
                SliderRow("Art size", s.artSize, 0.3f..0.8f, { s = s.copy(artSize = it) })
                SliderRow("Art position", s.artInset, 0f..0.2f, { s = s.copy(artInset = it) })
            }
        }
        GlassPanel {
            PanelTitle("Colors")
            if (s.preset != MusicPreset.ART_FULL) ColorField("Glass tint", s.tint) { s = s.copy(tint = it) }
            ColorField("Border", s.border) { s = s.copy(border = it) }
            ColorField("Text", s.textColor) { s = s.copy(textColor = it) }
        }
        GlassPanel {
            PanelTitle("Details")
            ToggleRow("Show artist", s.showArtist) { s = s.copy(showArtist = it) }
            ToggleRow("Show paused chip", s.showStatusChip) { s = s.copy(showStatusChip = it) }
        }
    }
}

// ───────────── Clock ─────────────

@Composable
fun ClockEditor(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var s by remember { mutableStateOf(StyleStore.clock(ctx)) }
    var wallpaperNote by remember { mutableStateOf("") }
    PersistEffect(s) { StyleStore.saveClock(ctx, s); ClockWidget().updateAll(ctx) }

    // The user picks a font file themselves (for example one they downloaded); it is copied into app storage.
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ClockFontStore.import(ctx, uri) }
            if (path != null) s = s.copy(fontPath = path)
        }
    }

    EditorScaffold("Clock widget", onBack, ClockWidgetReceiver::class.java, { ClockPreview(s, 320.dp, 170.dp) }) {
        GlassPanel {
            PanelTitle("Font")
            Segmented(listOf("Drawn digits", "My font"), if (s.fontPath.isEmpty()) 0 else 1) {
                if (it == 0) s = s.copy(fontPath = "") else fontPicker.launch(arrayOf("*/*"))
            }
            if (s.fontPath.isNotEmpty()) {
                Text(
                    "Using the font file you imported. Tap \"My font\" again to pick a different one.",
                    color = Color(0x99FFFFFF), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        GlassPanel {
            PanelTitle("Digits")
            SliderRow("Width", s.scaleX, 0.35f..1.1f, { s = s.copy(scaleX = it) })
            if (s.fontPath.isEmpty()) SliderRow("Thickness", s.weight, 0.15f..0.5f, { s = s.copy(weight = it) })
            SliderRow("Size", s.sizeFraction, 0.4f..1f, { s = s.copy(sizeFraction = it) })
            SliderRow("Depth", s.depth, 0f..1f, { s = s.copy(depth = it) })
            ToggleRow("24-hour time", s.use24h) { s = s.copy(use24h = it) }
        }
        GlassPanel {
            PanelTitle("Glass")
            Button(
                onClick = {
                    val p = WallpaperPalette.clockColors(ctx)
                    if (p != null) {
                        s = s.copy(topColor = p.top, bottomColor = p.bottom, strokeColor = p.edge)
                        wallpaperNote = "Colors set from your current wallpaper."
                    } else {
                        wallpaperNote = "Couldn't read the wallpaper colors."
                    }
                },
            ) { Text("Match wallpaper") }
            Text(
                if (wallpaperNote.isEmpty()) "Picks a soft tint once and keeps it. Press again after you change wallpaper." else wallpaperNote,
                color = Color(0x99FFFFFF), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp),
            )
            ColorField("Top color", s.topColor) { s = s.copy(topColor = it) }
            ColorField("Bottom color", s.bottomColor) { s = s.copy(bottomColor = it) }
            ColorField("Edge color", s.strokeColor) { s = s.copy(strokeColor = it) }
            SliderRow("Edge thickness", s.strokeDp, 0f..4f, { s = s.copy(strokeDp = it) }) { "%.1f dp".format(it) }
        }
        GlassPanel {
            PanelTitle("Date")
            ToggleRow("Show date", s.showDate) { s = s.copy(showDate = it) }
            ColorField("Date color", s.textColor) { s = s.copy(textColor = it) }
        }
    }
}

// ───────────── Weather ─────────────

@Composable
fun WeatherEditor(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var s by remember { mutableStateOf(StyleStore.weather(ctx)) }
    var lat by remember { mutableStateOf(s.lat.toString()) }
    var lon by remember { mutableStateOf(s.lon.toString()) }
    fun refresh() = WorkManager.getInstance(ctx).enqueue(OneTimeWorkRequestBuilder<WeatherWorker>().build())
    PersistEffect(s) { StyleStore.saveWeather(ctx, s); WeatherWidget().updateAll(ctx) }

    EditorScaffold("Weather widget", onBack, WeatherWidgetReceiver::class.java, { WeatherPreview(s) }) {
        GlassPanel {
            PanelTitle("Location")
            OutlinedTextField(lat, { lat = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Latitude") }, singleLine = true)
            OutlinedTextField(lon, { lon = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Longitude") }, singleLine = true)
            Button(
                onClick = {
                    s = s.copy(lat = lat.toDoubleOrNull() ?: 0.0, lon = lon.toDoubleOrNull() ?: 0.0)
                    StyleStore.saveWeather(ctx, s)
                    refresh()
                },
                modifier = Modifier.padding(top = 12.dp),
            ) { Text("Save and refresh") }
        }
        GlassPanel {
            PanelTitle("Look")
            ToggleRow("Celsius", s.celsius) { s = s.copy(celsius = it); StyleStore.saveWeather(ctx, s); refresh() }
            ColorField("Text color", s.textColor) { s = s.copy(textColor = it) }
        }
    }
}
