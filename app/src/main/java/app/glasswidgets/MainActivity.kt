package app.glasswidgets

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Home() } }
    }
}

@Composable
private fun Home() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var clock by remember { mutableStateOf(StyleStore.clock(ctx)) }
    var lat by remember { mutableStateOf(StyleStore.weather(ctx).lat.toString()) }
    var lon by remember { mutableStateOf(StyleStore.weather(ctx).lon.toString()) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B1A))
            .drawBehind {
                drawRect(Brush.radialGradient(listOf(Color(0xCC6D3BFF), Color.Transparent), Offset(size.width * 0.1f, size.height * 0.05f), size.width))
                drawRect(Brush.radialGradient(listOf(Color(0xAA00BFA5), Color.Transparent), Offset(size.width * 0.95f, size.height * 0.45f), size.width * 0.9f))
                drawRect(Brush.radialGradient(listOf(Color(0x99FF3D9A), Color.Transparent), Offset(size.width * 0.2f, size.height * 0.95f), size.width))
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Glass Widgets", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)

            GlassCard {
                Text("Music widget style", color = Color.White, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Dark" to MusicPreset.DARK_GLASS, "Frosted" to MusicPreset.FROSTED_LIGHT, "Art" to MusicPreset.ART_FULL)
                        .forEach { (label, p) ->
                            FilledTonalButton(
                                onClick = {
                                    StyleStore.saveMusic(ctx, MusicStyle.preset(p))
                                    scope.launch { MusicWidget().updateAll(ctx) }
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text(label) }
                        }
                }
            }

            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("24-hour clock", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Switch(checked = clock.use24h, onCheckedChange = {
                        clock = clock.copy(use24h = it)
                        StyleStore.saveClock(ctx, clock)
                        scope.launch { ClockWidget().updateAll(ctx) }
                    })
                }
            }

            GlassCard {
                Text("Weather location", color = Color.White, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(lat, { lat = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Latitude") }, singleLine = true)
                OutlinedTextField(lon, { lon = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Longitude") }, singleLine = true)
                Button(
                    onClick = {
                        StyleStore.saveWeather(ctx, StyleStore.weather(ctx).copy(lat = lat.toDoubleOrNull() ?: 0.0, lon = lon.toDoubleOrNull() ?: 0.0))
                        WorkManager.getInstance(ctx).enqueue(OneTimeWorkRequestBuilder<WeatherWorker>().build())
                    },
                    modifier = Modifier.padding(top = 12.dp),
                ) { Text("Save and refresh") }
            }

            GlassCard {
                Text("Now playing", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    "Allow notification access so the music widget can read the current song and album art.",
                    color = Color(0xB3FFFFFF), modifier = Modifier.padding(top = 4.dp),
                )
                Button(
                    onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                    modifier = Modifier.padding(top = 12.dp),
                ) { Text("Open notification access") }
            }
        }
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x0DFFFFFF))))
            .border(1.dp, Brush.linearGradient(listOf(Color(0x99FFFFFF), Color(0x1AFFFFFF))), shape)
            .padding(18.dp),
        content = content,
    )
}
