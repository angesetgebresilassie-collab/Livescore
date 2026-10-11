package app.glasswidgets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val ctx = LocalContext.current
                var bg by remember { mutableStateOf(BackgroundStore.load(ctx)) }
                AppBackdrop(bg) {
                    var screen by remember { mutableStateOf(Screen.HOME) }
                    when (screen) {
                        Screen.HOME -> Box(Modifier.fillMaxSize()) {
                            HomeScreen { screen = it }
                            BackgroundPill { screen = Screen.BACKGROUND }
                        }
                        Screen.MUSIC -> MusicEditor { screen = Screen.HOME }
                        Screen.CLOCK -> ClockEditor { screen = Screen.HOME }
                        Screen.WEATHER -> WeatherEditor { screen = Screen.HOME }
                        Screen.BACKGROUND -> BackgroundEditor(
                            bg = bg,
                            onChange = { bg = it; BackgroundStore.save(ctx, it) },
                            onBack = { screen = Screen.HOME },
                        )
                    }
                }
            }
        }
    }
}
