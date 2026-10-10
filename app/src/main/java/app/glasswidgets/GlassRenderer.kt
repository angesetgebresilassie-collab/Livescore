package app.glasswidgets

import android.graphics.*

/**
 * Draws the "glass" layers from code. Glance can't do live blur, so each backdrop is
 * rendered from the style config every time (nothing is a stored picture).
 */
object GlassRenderer {

    fun musicCard(art: Bitmap?, s: MusicStyle, wPx: Int, hPx: Int, d: Float): Bitmap {
        val out = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val w = wPx.toFloat(); val h = hPx.toFloat()
        val rect = RectF(0f, 0f, w, h)
        val r = s.cornerDp * d
        c.clipPath(Path().apply { addRoundRect(rect, r, r, Path.Direction.CW) })

        if (s.preset == MusicPreset.ART_FULL) {
            if (art != null) drawCrop(c, art, rect) else c.drawColor(0xFF2A2A2E.toInt())
            if (art != null) {
                // blurred copy (tiny downscale, smooth upscale) faded in from the bottom
                val small = Bitmap.createScaledBitmap(art, 12, 12, true)
                val blur = Bitmap.createScaledBitmap(small, wPx, hPx, true)
                val layer = c.saveLayer(rect, null)
                c.drawBitmap(blur, null, rect, null)
                val mask = Paint().apply {
                    shader = LinearGradient(0f, h * (1 - s.blurFade), 0f, h * (1 - s.blurFade * 0.6f),
                        0x00000000, 0xFF000000.toInt(), Shader.TileMode.CLAMP)
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                }
                c.drawRect(rect, mask)
                c.restoreToCount(layer)
            }
            // light scrim so white text stays readable
            c.drawRect(rect, Paint().apply {
                shader = LinearGradient(0f, h * 0.6f, 0f, h, 0x00000000, 0x33000000, Shader.TileMode.CLAMP)
            })
        } else {
            c.drawColor(s.tint)
            c.drawRect(rect, Paint().apply {
                shader = LinearGradient(0f, 0f, w, h, 0x33FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
            })
            val x = w * s.artInset; val side = w * s.artSize
            val artRect = RectF(x, x, x + side, x + side)
            val ar = r * 0.55f
            c.save()
            c.clipPath(Path().apply { addRoundRect(artRect, ar, ar, Path.Direction.CW) })
            if (art != null) drawCrop(c, art, artRect)
            else c.drawRect(artRect, Paint().apply { color = 0xFF555555.toInt() })
            c.restore()
            c.drawRoundRect(artRect, ar, ar, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 1.5f * d; color = 0x66FFFFFF
            })
        }

        // glass rim
        val inset = 0.75f * d
        c.drawRoundRect(RectF(inset, inset, w - inset, h - inset), r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.5f * d; color = s.border
        })
        return out
    }

    fun clock(text: String, s: ClockStyle, wPx: Int, hPx: Int, d: Float): Bitmap {
        val out = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textScaleX = s.scaleX
            textSize = hPx * s.sizeFraction
        }
        val tw = p.measureText(text)
        if (tw > wPx) p.textSize *= wPx / tw
        val b = Rect(); p.getTextBounds(text, 0, text.length, b)
        val x = (wPx - b.width()) / 2f - b.left
        val y = (hPx - b.height()) / 2f - b.top

        p.shader = LinearGradient(0f, y + b.top, 0f, y + b.bottom, s.topColor, s.bottomColor, Shader.TileMode.CLAMP)
        c.drawText(text, x, y, p)

        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = s.strokeDp * d
        p.color = s.strokeColor
        c.drawText(text, x, y, p)
        return out
    }

    private fun drawCrop(c: Canvas, b: Bitmap, dst: RectF) {
        val scale = maxOf(dst.width() / b.width, dst.height() / b.height)
        val sw = dst.width() / scale; val sh = dst.height() / scale
        val src = Rect(((b.width - sw) / 2).toInt(), ((b.height - sh) / 2).toInt(),
            ((b.width + sw) / 2).toInt(), ((b.height + sh) / 2).toInt())
        c.drawBitmap(b, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))
    }
}
