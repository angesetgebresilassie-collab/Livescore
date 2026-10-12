package app.glasswidgets

import android.content.Context

/**
 * Widgets show a bitmap, so sharpness depends on how many pixels it has. This picks a render scale
 * above the widget's on-screen size: as sharp as possible while staying inside the memory the home
 * screen allows for a widget (about 1.5x the screen's bytes in total; one bitmap here stays under 1x).
 */
object HiRes {
    fun boost(ctx: Context, wPx: Int, hPx: Int, max: Float = 2.5f): Float {
        val dm = ctx.resources.displayMetrics
        val budget = dm.widthPixels.toDouble() * dm.heightPixels * 0.9
        val byMemory = Math.sqrt(budget / (wPx.toDouble() * hPx)).toFloat()
        val byTexture = 4096f / maxOf(wPx, hPx)
        return minOf(max, byMemory, byTexture).coerceAtLeast(1f)
    }
}
