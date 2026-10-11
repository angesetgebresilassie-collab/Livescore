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

data class ClockStyle(
    val use24h: Boolean = false,
    val scaleX: Float = 0.62f,        // digit width (digit height is fixed at 2 units)
    val sizeFraction: Float = 1.0f,   // how much of the widget height the digits fill
    val topColor: Int = 0x88A5ADB5.toInt(),
    val bottomColor: Int = 0xEEF2F6FA.toInt(),
    val strokeColor: Int = 0x55FFFFFF,
    val strokeDp: Float = 1.5f,       // rim thickness
    val showDate: Boolean = true,
    val textColor: Int = Color.WHITE,
    val weight: Float = 0.30f,        // stroke thickness of the drawn digits (in digit units)
    val fontPath: String = "",        // empty = drawn digits; otherwise a font file the user imported
    val depth: Float = 0.6f,          // 0 = flat, 1 = deep (drop shadow + inner bevel on the digits)
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

    fun clock(c: Context): ClockStyle {
        val d = ClockStyle(); val p = sp(c)
        return ClockStyle(
            p.getBoolean("c.24h", d.use24h), p.getFloat("c.scaleX", d.scaleX), p.getFloat("c.size", d.sizeFraction),
            p.getInt("c.top", d.topColor), p.getInt("c.bottom", d.bottomColor), p.getInt("c.stroke", d.strokeColor),
            p.getFloat("c.strokeDp", d.strokeDp), p.getBoolean("c.date", d.showDate), p.getInt("c.text", d.textColor),
            p.getFloat("c.weight", d.weight), p.getString("c.font", d.fontPath) ?: "",
            p.getFloat("c.depth", d.depth),
        )
    }

    fun saveClock(c: Context, s: ClockStyle) = sp(c).edit()
        .putBoolean("c.24h", s.use24h).putFloat("c.scaleX", s.scaleX).putFloat("c.size", s.sizeFraction)
        .putInt("c.top", s.topColor).putInt("c.bottom", s.bottomColor).putInt("c.stroke", s.strokeColor)
        .putFloat("c.strokeDp", s.strokeDp).putBoolean("c.date", s.showDate).putInt("c.text", s.textColor)
        .putFloat("c.weight", s.weight).putString("c.font", s.fontPath).putFloat("c.depth", s.depth).apply()

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
