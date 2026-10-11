package app.glasswidgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Screen { HOME, MUSIC, CLOCK, WEATHER }

/** Slowly drifting, heavily blurred color orbs on a near-black base. Gives the glass cards something to sit on. */
@Composable
fun AuroraBackground(content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "aurora")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(1f, 0f, infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    Box(Modifier.fillMaxSize().background(Color(0xFF04050D))) {
        Box(
            Modifier
                .fillMaxSize()
                .blur(90.dp)
                .drawBehind {
                    fun orb(color: Color, fx: Float, fy: Float, r: Float) =
                        drawCircle(color, radius = size.minDimension * r, center = Offset(size.width * fx, size.height * fy))
                    orb(Color(0xFF7C3AED), 0.15f + 0.5f * a, 0.12f + 0.2f * b, 0.55f)
                    orb(Color(0xFF06B6D4), 0.9f - 0.5f * b, 0.40f + 0.2f * a, 0.50f)
                    orb(Color(0xFFEC4899), 0.2f + 0.4f * b, 0.90f - 0.25f * a, 0.55f)
                    orb(Color(0xFF2563EB), 0.8f - 0.3f * a, 0.85f, 0.40f)
                }
        )
        content()
    }
}

@Composable
fun GlassPanel(modifier: Modifier = Modifier, corner: Dp = 28.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))))
            .border(1.dp, Brush.linearGradient(listOf(Color(0xAAFFFFFF), Color(0x11FFFFFF), Color(0x55FFFFFF))), shape)
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun PanelTitle(text: String) = Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

@Composable
fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    display: (Float) -> String = { "%.2f".format(it) },
) {
    Column(Modifier.padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Text(display(value), color = Color(0x99FFFFFF), fontSize = 14.sp)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color(0x33FFFFFF),
            ),
        )
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 15.sp)
        Switch(checked, onChange)
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(50)).background(Color(0x22FFFFFF)).padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (i == selected) Color(0x55FFFFFF) else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = Color.White, fontSize = 14.sp, fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

/** Tap the swatch to open hue / saturation / brightness / opacity sliders. */
@Composable
fun ColorField(label: String, argb: Int, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(argb, it) }
    val alpha = android.graphics.Color.alpha(argb) / 255f
    fun emit(h: Float = hsv[0], sat: Float = hsv[1], v: Float = hsv[2], a: Float = alpha) =
        onChange(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, sat, v)))

    Column(Modifier.padding(top = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Box(Modifier.size(32.dp).clip(CircleShape).background(Color(argb)).border(1.dp, Color(0x88FFFFFF), CircleShape))
        }
        if (open) {
            SliderRow("Hue", hsv[0], 0f..360f, { emit(h = it) }) { "${it.toInt()}°" }
            SliderRow("Saturation", hsv[1], 0f..1f, { emit(sat = it) })
            SliderRow("Brightness", hsv[2], 0f..1f, { emit(v = it) })
            SliderRow("Opacity", alpha, 0f..1f, { emit(a = it) })
        }
    }
}
