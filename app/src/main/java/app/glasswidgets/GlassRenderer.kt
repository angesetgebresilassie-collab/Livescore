package app.glasswidgets

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.File

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

    /**
     * Glass clock. The digits come from either code-drawn strokes (StrokeDigits) or a font file the
     * user imported. Either way the shape is turned into an outline, filled with a vertical glass
     * gradient, given a diagonal sheen, and edged with a rim.
     */
    fun clock(text: String, s: ClockStyle, wPx: Int, hPx: Int, d: Float): Bitmap {
        val out = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val dw = s.scaleX.coerceIn(0.3f, 1.4f)
        val m = Matrix()

        val tf = if (s.fontPath.isNotEmpty()) FontCache.get(s.fontPath) else null
        val outline = if (tf != null) fontOutline(text, tf, dw, m) else drawnOutline(text, s, dw, m)

        val b = RectF()
        outline.computeBounds(b, true)
        if (b.width() <= 0f || b.height() <= 0f) return out
        val k = minOf(wPx * 0.98f / b.width(), hPx * s.sizeFraction.coerceIn(0.2f, 1f) / b.height())
        m.reset()
        m.postTranslate(-b.centerX(), -b.centerY())
        m.postScale(k, k)
        m.postTranslate(wPx / 2f, hPx / 2f)
        outline.transform(m)

        val tb = RectF()
        outline.computeBounds(tb, true)
        c.drawPath(outline, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(0f, tb.top, 0f, tb.bottom, s.topColor, s.bottomColor, Shader.TileMode.CLAMP)
        })
        c.save()
        c.clipPath(outline)
        c.drawRect(tb, Paint().apply {
            shader = LinearGradient(tb.left, tb.top, tb.right, tb.bottom, 0x33FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        })
        c.restore()
        c.drawPath(outline, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s.strokeDp * d
            strokeJoin = Paint.Join.ROUND
            color = s.strokeColor
        })
        return out
    }

    /** Outline of the text in the imported font, squeezed horizontally by the width setting. */
    private fun fontOutline(text: String, tf: Typeface, dw: Float, m: Matrix): Path {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textSize = 200f
        }
        val raw = Path()
        p.getTextPath(text, 0, text.length, 0f, 0f, raw)
        m.setScale(dw, 1f)
        raw.transform(m)
        return raw
    }

    /** Outline of the code-drawn rounded digits (strokes converted to filled geometry). */
    private fun drawnOutline(text: String, s: ClockStyle, dw: Float, m: Matrix): Path {
        val sw = s.weight.coerceIn(0.12f, 0.6f)
        val gap = sw * 0.6f
        val strokes = Path()
        val dots = ArrayList<Float>()
        var x = 0f
        for (ch in text) {
            if (ch == ':') {
                val cx = x + sw * 0.4f
                dots.add(cx); dots.add(0.62f); dots.add(cx); dots.add(1.38f)
                x += sw * 0.8f + gap
            } else if (ch in '0'..'9') {
                val g = Path(StrokeDigits.glyph(ch))
                m.setScale(dw, 1f)
                g.transform(m)
                strokes.addPath(g, x, 0f)
                x += dw + sw + gap
            }
        }
        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = sw
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val outline = Path()
        unitPaint.getFillPath(strokes, outline)
        for (i in dots.indices step 2) outline.addCircle(dots[i], dots[i + 1], sw * 0.4f, Path.Direction.CW)
        return outline
    }

    private fun drawCrop(c: Canvas, b: Bitmap, dst: RectF) {
        val scale = maxOf(dst.width() / b.width, dst.height() / b.height)
        val sw = dst.width() / scale; val sh = dst.height() / scale
        val src = Rect(((b.width - sw) / 2).toInt(), ((b.height - sh) / 2).toInt(),
            ((b.width + sw) / 2).toInt(), ((b.height + sh) / 2).toInt())
        c.drawBitmap(b, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))
    }
}

/** Loads imported font files once. Each import gets a new file name, so a path is never stale. */
object FontCache {
    private val cache = HashMap<String, Typeface>()
    fun get(path: String): Typeface? {
        cache[path]?.let { return it }
        return try {
            Typeface.createFromFile(path).also { cache[path] = it }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Lets the user bring their own font file for the clock (for example one they downloaded themselves).
 * Nothing is bundled in the app.
 */
object ClockFontStore {
    /** Copies the picked file into app storage and checks that it loads. Returns its path, or null. */
    fun import(c: Context, uri: Uri): String? = try {
        c.filesDir.listFiles { f -> f.name.startsWith("clock_font_") }?.forEach { it.delete() }
        val dest = File(c.filesDir, "clock_font_${System.currentTimeMillis()}")
        c.contentResolver.openInputStream(uri)?.use { input -> dest.outputStream().use { input.copyTo(it) } }
        Typeface.createFromFile(dest)
        dest.path
    } catch (e: Exception) {
        null
    }
}

/**
 * Digit centerlines on a 1 x 2 grid (y points down). The renderer strokes them with round
 * caps and joins, which gives the tall, even-weight, rounded-end look of the iOS lock-screen clock.
 */
object StrokeDigits {
    fun glyph(ch: Char): Path = Path().apply {
        when (ch) {
            '0' -> addRoundRect(RectF(0f, 0f, 1f, 2f), 0.5f, 0.5f, Path.Direction.CW)
            '1' -> { moveTo(0.1f, 0.45f); lineTo(0.55f, 0f); lineTo(0.55f, 2f) }
            '2' -> {
                moveTo(0f, 0.5f)
                arcTo(RectF(0f, 0f, 1f, 1f), 180f, 180f)
                cubicTo(1f, 1.15f, 0f, 1.15f, 0f, 2f)
                lineTo(1f, 2f)
            }
            '3' -> {
                moveTo(0f, 0.5f)
                arcTo(RectF(0f, 0f, 1f, 1f), 180f, 270f)
                arcTo(RectF(0f, 1f, 1f, 2f), 270f, 270f)
            }
            '4' -> { moveTo(0.75f, 2f); lineTo(0.75f, 0f); lineTo(0f, 1.45f); lineTo(1f, 1.45f) }
            '5' -> {
                moveTo(0.95f, 0f); lineTo(0.08f, 0f); lineTo(0.03f, 0.92f)
                arcTo(RectF(0f, 0.9f, 1f, 2f), 250f, 280f)
            }
            '6' -> {
                moveTo(0f, 1.5f); lineTo(0f, 0.5f)
                arcTo(RectF(0f, 0f, 1f, 1f), 180f, 130f)
                addOval(RectF(0f, 0.95f, 1f, 2f), Path.Direction.CW)
            }
            '7' -> { moveTo(0f, 0f); lineTo(1f, 0f); lineTo(0.3f, 2f) }
            '8' -> {
                addOval(RectF(0.06f, 0f, 0.94f, 1f), Path.Direction.CW)
                addOval(RectF(0f, 0.95f, 1f, 2f), Path.Direction.CW)
            }
            '9' -> {
                moveTo(1f, 0.5f); lineTo(1f, 1.5f)
                arcTo(RectF(0f, 1f, 1f, 2f), 0f, 130f)
                addOval(RectF(0f, 0f, 1f, 1.05f), Path.Direction.CW)
            }
        }
    }
}
