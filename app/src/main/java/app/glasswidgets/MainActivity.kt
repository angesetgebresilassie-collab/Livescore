package app.glasswidgets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AuroraBackground {
                    var screen by remember { mutableStateOf(Screen.HOME) }
                    when (screen) {
                        Screen.HOME -> HomeScreen { screen = it }
                        Screen.MUSIC -> MusicEditor { screen = Screen.HOME }
                        Screen.CLOCK -> ClockEditor { screen = Screen.HOME }
                        Screen.WEATHER -> WeatherEditor { screen = Screen.HOME }
                    }
                }
            }
        }
    }
}
