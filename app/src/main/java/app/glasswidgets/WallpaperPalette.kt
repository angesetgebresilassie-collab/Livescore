package app.glasswidgets

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color

/**
 * Reads the system wallpaper's dominant color once and turns it into a translucent glass tint for the clock.
 * The tint keeps the wallpaper's hue but is lighter, so the digits read as frosted glass over the wallpaper
 * (like the iOS lock screen). Nothing is live: the result is written into the clock style and stays until
 * you press the button again.
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
        val base = if (secondary != null && saturation(secondary) > saturation(primary) + 0.2f) secondary else primary

        val hsv = FloatArray(3)
        Color.colorToHSV(base, hsv)
        val h = hsv[0]
        val s = hsv[1]
        val v = hsv[2]

        // Same hue as the wallpaper, lighter, still translucent so the wallpaper shows through.
        val sat = (s * 0.75f).coerceIn(0.05f, 0.65f)
        val topV = (v + 0.30f).coerceIn(0.62f, 0.92f)
        val bottomV = (topV + 0.10f).coerceAtMost(1f)
        val top = Color.HSVToColor(0xA8, floatArrayOf(h, sat, topV))
        val bottom = Color.HSVToColor(0xBC, floatArrayOf(h, sat * 0.8f, bottomV))
        val edge = Color.HSVToColor(0x88, floatArrayOf(h, (s * 0.25f).coerceIn(0.02f, 0.3f), 1f))
        return ClockColors(top, bottom, edge)
    }

    private fun saturation(argb: Int): Float {
        val hsv = FloatArray(3)
        Color.colorToHSV(argb, hsv)
        return hsv[1]
    }
}
