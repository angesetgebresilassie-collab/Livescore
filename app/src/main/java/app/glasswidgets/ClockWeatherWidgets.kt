package app.glasswidgets

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

// ───────────── Clock (big glass digits) ─────────────

class ClockWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = StyleStore.clock(context)
        provideContent {
            val size = LocalSize.current
            val d = context.resources.displayMetrics.density
            val w = minOf((size.width.value * d).toInt(), 1100).coerceAtLeast(1)
            val dateH = if (s.showDate) 28f else 0f
            val h = minOf(((size.height.value - dateH) * d).toInt(), 1100).coerceAtLeast(1)
            val now = LocalDateTime.now()
            val time = now.format(DateTimeFormatter.ofPattern(if (s.use24h) "H:mm" else "h:mm"))
            Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (s.showDate) Text(
                    now.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                    style = TextStyle(color = ColorProvider(Color(s.textColor)), fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                )
                Image(ImageProvider(GlassRenderer.clock(time, s, w, h, d)), null,
                    GlanceModifier.fillMaxWidth().defaultWeight(), contentScale = ContentScale.Fit)
            }
        }
    }
}

class ClockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ClockWidget()
}

/** Minute ticks. ACTION_TIME_TICK can only be registered at runtime, see App below. */
class TickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            ClockWidget().updateAll(context)
            pending.finish()
        }
    }
}

// ───────────── Weather ─────────────

object WeatherState {
    @Volatile var temp = 0
    @Volatile var cond = "—"
    @Volatile var hi = 0
    @Volatile var lo = 0
    @Volatile var night = false
}

class WeatherWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result = try {
        val s = StyleStore.weather(applicationContext)
        val unit = if (s.celsius) "celsius" else "fahrenheit"
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${s.lat}&longitude=${s.lon}" +
            "&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min" +
            "&temperature_unit=$unit&timezone=auto"
        val j = JSONObject(withContext(Dispatchers.IO) { URL(url).readText() })
        val cur = j.getJSONObject("current"); val day = j.getJSONObject("daily")
        WeatherState.temp = Math.round(cur.getDouble("temperature_2m")).toInt()
        WeatherState.cond = wmo(cur.getInt("weather_code"))
        WeatherState.night = cur.getInt("is_day") == 0
        WeatherState.hi = Math.round(day.getJSONArray("temperature_2m_max").getDouble(0)).toInt()
        WeatherState.lo = Math.round(day.getJSONArray("temperature_2m_min").getDouble(0)).toInt()
        WeatherWidget().updateAll(applicationContext)
        Result.success()
    } catch (e: Exception) { Result.retry() }

    private fun wmo(c: Int) = when (c) {
        0 -> "Clear"; 1, 2 -> "Mostly clear"; 3 -> "Cloudy"; 45, 48 -> "Fog"
        in 51..57 -> "Drizzle"; in 61..67, in 80..82 -> "Rain"; in 71..77, 85, 86 -> "Snow"
        in 95..99 -> "Thunderstorm"; else -> "—"
    }
}

class WeatherWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = StyleStore.weather(context)
        provideContent {
            val main = ColorProvider(Color(s.textColor))
            val dim = ColorProvider(Color(s.textColor).copy(alpha = 0.6f))
            Column(GlanceModifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${if (WeatherState.night) "☾" else "☀"} ${WeatherState.temp}°",
                    style = TextStyle(color = main, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                Text(WeatherState.cond, style = TextStyle(color = main, fontSize = 16.sp))
                Text("H:${WeatherState.hi}° L:${WeatherState.lo}°", style = TextStyle(color = dim, fontSize = 14.sp))
            }
        }
    }
}

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = WeatherWidget()
}

// ───────────── App ─────────────

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(this, TickReceiver(), IntentFilter(Intent.ACTION_TIME_TICK),
            ContextCompat.RECEIVER_NOT_EXPORTED)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weather", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WeatherWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build(),
        )
    }
}
