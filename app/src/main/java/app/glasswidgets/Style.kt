package app.glasswidgets

import android.content.Context
import android.graphics.Color

/** Every visual value lives here, so the in-app editor can expose all of it. */
enum class MusicPreset { DARK_GLASS, FROSTED_LIGHT, ART_FULL }

data class MusicStyle(
    val preset: MusicPreset = MusicPreset.ART_FULL,
    val cornerDp: Float = 28f,
    val tint: Int = 0x00000000,
    val border: Int = 0x66FFFFFF,
    val blurFade: Float = 0.45f,   // ART_FULL: how much of the card fades into blurred art
    val artInset: Float = 0.07f,   // glass presets: art position (fraction of width)
    val artSize: Float = 0.55f,    // glass presets: art size (fraction of width)
    val textColor: Int = Color.WHITE,
    val showArtist: Boolean = true,
    val showStatusChip: Boolean = true,
) {
    companion object {
        fun preset(p: MusicPreset) = when (p) {
            MusicPreset.DARK_GLASS -> MusicStyle(p, tint = 0xAA1C1C1E.toInt())
            MusicPreset.FROSTED_LIGHT -> MusicStyle(p, tint = 0x66FFFFFF, border = 0x99FFFFFF.toInt())
            MusicPreset.ART_FULL -> MusicStyle(p)
        }
    }
}

/** Defaults match the iOS lock-screen clock: tall, narrow, even-weight digits in a translucent wallpaper tint. */
data class ClockStyle(
    val use24h: Boolean = false,
    val scaleX: Float = 0.46f,        // digit width (digit height is fixed at 2 units)
    val sizeFraction: Float = 1.0f,   // how much of the widget height the digits fill
    val topColor: Int = 0xA8A5B8D0.toInt(),
    val bottomColor: Int = 0xBCD8E4F2.toInt(),
    val strokeColor: Int = 0x88FFFFFF.toInt(),
    val strokeDp: Float = 1.2f,       // rim thickness
    val showDate: Boolean = true,
    val textColor: Int = Color.WHITE,
    val weight: Float = 0.20f,        // stroke thickness of the drawn digits (in digit units)
    val fontPath: String = "",        // empty = drawn digits; otherwise a font file the user imported
    val depth: Float = 0f,            // optional drop shadow + bevel; off by default
)

data class WeatherStyle(
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val celsius: Boolean = true,
    val textColor: Int = Color.WHITE,
)

object StyleStore {
    private fun sp(c: Context) = c.getSharedPreferences("glass_styles", Context.MODE_PRIVATE)

    fun music(c: Context): MusicStyle {
        val d = MusicStyle(); val p = sp(c)
        return MusicStyle(
            MusicPreset.valueOf(p.getString("m.preset", d.preset.name)!!),
            p.getFloat("m.corner", d.cornerDp), p.getInt("m.tint", d.tint), p.getInt("m.border", d.border),
            p.getFloat("m.fade", d.blurFade), p.getFloat("m.inset", d.artInset), p.getFloat("m.artSize", d.artSize),
            p.getInt("m.text", d.textColor), p.getBoolean("m.artist", d.showArtist), p.getBoolean("m.chip", d.showStatusChip),
        )
    }

    fun saveMusic(c: Context, s: MusicStyle) = sp(c).edit()
        .putString("m.preset", s.preset.name).putFloat("m.corner", s.cornerDp).putInt("m.tint", s.tint)
        .putInt("m.border", s.border).putFloat("m.fade", s.blurFade).putFloat("m.inset", s.artInset)
        .putFloat("m.artSize", s.artSize).putInt("m.text", s.textColor).putBoolean("m.artist", s.showArtist)
        .putBoolean("m.chip", s.showStatusChip).apply()

    // Clock keys carry a "2" suffix so older saved looks do not override the new lock-screen defaults.
    fun clock(c: Context): ClockStyle {
        val d = ClockStyle(); val p = sp(c)
        // First run: pick the glass tint from the wallpaper once and keep it.
        if (!p.contains("c.top2")) {
            WallpaperPalette.clockColors(c)?.let {
                p.edit().putInt("c.top2", it.top).putInt("c.bottom2", it.bottom).putInt("c.stroke2", it.edge).apply()
            }
        }
        return ClockStyle(
            p.getBoolean("c.24h", d.use24h), p.getFloat("c.scaleX2", d.scaleX), p.getFloat("c.size", d.sizeFraction),
            p.getInt("c.top2", d.topColor), p.getInt("c.bottom2", d.bottomColor), p.getInt("c.stroke2", d.strokeColor),
            p.getFloat("c.strokeDp2", d.strokeDp), p.getBoolean("c.date", d.showDate), p.getInt("c.text", d.textColor),
            p.getFloat("c.weight2", d.weight), p.getString("c.font", d.fontPath) ?: "",
            p.getFloat("c.depth2", d.depth),
        )
    }

    fun saveClock(c: Context, s: ClockStyle) = sp(c).edit()
        .putBoolean("c.24h", s.use24h).putFloat("c.scaleX2", s.scaleX).putFloat("c.size", s.sizeFraction)
        .putInt("c.top2", s.topColor).putInt("c.bottom2", s.bottomColor).putInt("c.stroke2", s.strokeColor)
        .putFloat("c.strokeDp2", s.strokeDp).putBoolean("c.date", s.showDate).putInt("c.text", s.textColor)
        .putFloat("c.weight2", s.weight).putString("c.font", s.fontPath).putFloat("c.depth2", s.depth).apply()

    fun weather(c: Context): WeatherStyle {
        val d = WeatherStyle(); val p = sp(c)
        return WeatherStyle(
            Double.fromBits(p.getLong("w.lat", d.lat.toRawBits())), Double.fromBits(p.getLong("w.lon", d.lon.toRawBits())),
            p.getBoolean("w.c", d.celsius), p.getInt("w.text", d.textColor),
        )
    }

    fun saveWeather(c: Context, s: WeatherStyle) = sp(c).edit()
        .putLong("w.lat", s.lat.toRawBits()).putLong("w.lon", s.lon.toRawBits())
        .putBoolean("w.c", s.celsius).putInt("w.text", s.textColor).apply()
}
