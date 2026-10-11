package app.glasswidgets

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BgMode { SOLID, GRADIENT, IMAGE }

/** The app's own background (not the widgets). The user picks it; nothing animated. */
data class AppBackground(
    val mode: BgMode = BgMode.GRADIENT,
    val color1: Int = 0xFF172554.toInt(),
    val color2: Int = 0xFF9333EA.toInt(),
    val dim: Float = 0f,          // black overlay, helps text stay readable over photos
    val imageStamp: Long = 0L,    // changes when a new photo is picked so it reloads
)

object BackgroundStore {
    private fun sp(c: Context) = c.getSharedPreferences("glass_styles", Context.MODE_PRIVATE)
    fun file(c: Context) = File(c.filesDir, "background.jpg")

    fun load(c: Context): AppBackground {
        val d = AppBackground(); val p = sp(c)
        return AppBackground(
            BgMode.valueOf(p.getString("b.mode", d.mode.name)!!),
            p.getInt("b.c1", d.color1), p.getInt("b.c2", d.color2),
            p.getFloat("b.dim", d.dim), p.getLong("b.stamp", d.imageStamp),
        )
    }

    fun save(c: Context, b: AppBackground) = sp(c).edit()
        .putString("b.mode", b.mode.name).putInt("b.c1", b.color1).putInt("b.c2", b.color2)
        .putFloat("b.dim", b.dim).putLong("b.stamp", b.imageStamp).apply()

    /** Copies the picked photo into app storage (downscaled, rotation applied) so it survives restarts. */
    fun importImage(c: Context, uri: Uri): Boolean = try {
        val src = ImageDecoder.createSource(c.contentResolver, uri)
        val bmp = ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            val m = maxOf(info.size.width, info.size.height)
            if (m > 2000) {
                val k = 2000f / m
                decoder.setTargetSize((info.size.width * k).toInt(), (info.size.height * k).toInt())
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        file(c).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Draws the user's background and captures it as the Liquid Glass backdrop. Panels are drawn
 * above the captured layer (not inside it), so they refract the background without sampling themselves.
 */
@Composable
fun AppBackdrop(bg: AppBackground, content: @Composable BoxScope.() -> Unit) {
    val ctx = LocalContext.current
    val backdrop = rememberLayerBackdrop()
    val image by produceState<ImageBitmap?>(null, bg.imageStamp, bg.mode) {
        value = if (bg.mode == BgMode.IMAGE) withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(BackgroundStore.file(ctx).path)?.asImageBitmap()
        } else null
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            val second = if (bg.mode == BgMode.SOLID) bg.color1 else bg.color2
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(bg.color1), Color(second)))))
            image?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            if (bg.dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = bg.dim)))
        }
        CompositionLocalProvider(LocalBackdrop provides backdrop) { content() }
    }
}

/** Small button shown on the home screen that opens the background settings. */
@Composable
fun BoxScope.BackgroundPill(onClick: () -> Unit) {
    Text(
        "Background",
        color = Color.White,
        fontSize = 13.sp,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(top = 20.dp, end = 20.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
fun BackgroundEditor(bg: AppBackground, onChange: (AppBackground) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { BackgroundStore.importImage(ctx, uri) }
            if (ok) onChange(bg.copy(mode = BgMode.IMAGE, imageStamp = System.currentTimeMillis(), dim = maxOf(bg.dim, 0.3f)))
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Back", color = Color.White, fontSize = 16.sp) }
            Spacer(Modifier.weight(1f))
            Text("Background", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(72.dp))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GlassPanel {
                PanelTitle("Type")
                Segmented(listOf("Color", "Gradient", "Photo"), bg.mode.ordinal) {
                    onChange(bg.copy(mode = BgMode.values()[it]))
                }
            }
            when (bg.mode) {
                BgMode.SOLID -> GlassPanel {
                    PanelTitle("Color")
                    ColorField("Background color", bg.color1) { onChange(bg.copy(color1 = it or 0xFF000000.toInt())) }
                }
                BgMode.GRADIENT -> GlassPanel {
                    PanelTitle("Gradient")
                    ColorField("Top color", bg.color1) { onChange(bg.copy(color1 = it or 0xFF000000.toInt())) }
                    ColorField("Bottom color", bg.color2) { onChange(bg.copy(color2 = it or 0xFF000000.toInt())) }
                }
                BgMode.IMAGE -> GlassPanel {
                    PanelTitle("Photo")
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text("Choose photo") }
                        TextButton(onClick = {
                            BackgroundStore.file(ctx).delete()
                            onChange(bg.copy(mode = BgMode.GRADIENT, imageStamp = 0L))
                        }) { Text("Remove", color = Color.White) }
                    }
                }
            }
            GlassPanel {
                PanelTitle("Readability")
                SliderRow("Darken", bg.dim, 0f..0.8f, { onChange(bg.copy(dim = it)) }) { "${(it * 100).toInt()}%" }
            }
        }
    }
}
