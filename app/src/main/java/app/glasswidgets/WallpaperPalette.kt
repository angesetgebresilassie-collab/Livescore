package app.glasswidgets

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color

/**
 * Reads the system wallpaper's dominant color once and turns it into a soft glass tint for the clock.
 * Nothing is live: the result is written into the clock style and stays until you press the button again.
 */
object WallpaperPalette {

    data class ClockColors(val top: Int, val bottom: Int, val edge: Int)

    fun clockColors(c: Context): ClockColors? {
        val wc = try {
            WallpaperManager.getInstance(c).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        } catch (e: Exception) {
            null
        } ?: return null

        // Prefer the strongest hue the wallpaper offers; fall back to the primary color.
        val primary = wc.primaryColor.toArgb()
        val secondary = wc.secondaryColor?.toArgb()
        val base = if (secondary != null && saturation(secondary) > saturation(primary) + 0.15f) secondary else primary

        val hsv = FloatArray(3)
        Color.colorToHSV(base, hsv)
        val h = hsv[0]
        val s = hsv[1]

        // Soft: heavy white mix, only a hint of the wallpaper hue.
        val top = Color.HSVToColor(0x99, floatArrayOf(h, (s * 0.38f).coerceIn(0.04f, 0.45f), 0.80f))
        val bottom = Color.HSVToColor(0xEE, floatArrayOf(h, (s * 0.16f).coerceIn(0.02f, 0.22f), 0.97f))
        val edge = Color.HSVToColor(0x66, floatArrayOf(h, (s * 0.20f).coerceIn(0.02f, 0.25f), 1f))
        return ClockColors(top, bottom, edge)
    }

    private fun saturation(argb: Int): Float {
        val hsv = FloatArray(3)
        Color.colorToHSV(argb, hsv)
        return hsv[1]
    }
}
