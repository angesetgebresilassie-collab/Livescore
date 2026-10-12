package app.glasswidgets

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.*
import java.net.URL

object NowPlaying {
    @Volatile var title = ""
    @Volatile var artist = ""
    @Volatile var art: Bitmap? = null
    @Volatile var playing = false
    @Volatile var pausedAt = 0L
    @Volatile var controller: MediaController? = null
}

/** User must enable "Notification access" for the app once; that is what allows reading media sessions. */
class MediaListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var current: MediaController? = null
    private var lastArtUri: String? = null
    private var lastTrack = ""
    private val me by lazy { ComponentName(this, MediaListener::class.java) }
    private val msm by lazy { getSystemService(MediaSessionManager::class.java) }

    private val cb = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = push()
        override fun onPlaybackStateChanged(state: PlaybackState?) = repick()
        override fun onSessionDestroyed() = repick()
    }

    override fun onListenerConnected() {
        msm.addOnActiveSessionsChangedListener({ bind(pick(it)) }, me)
        repick()
    }

    override fun onListenerDisconnected() {
        NotificationListenerService.requestRebind(me)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Prefer whichever app is actually playing right now, otherwise the first active session. */
    private fun pick(list: List<MediaController>?): MediaController? =
        list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()

    private fun repick() {
        val list = try { msm.getActiveSessions(me) } catch (e: SecurityException) { null }
        val best = pick(list)
        if (best?.sessionToken != current?.sessionToken) bind(best) else push()
    }

    private fun bind(c: MediaController?) {
        current?.unregisterCallback(cb)
        current = c; NowPlaying.controller = c
        c?.registerCallback(cb)
        push()
    }

    private fun push() {
        val m = current?.metadata
        val playing = current?.playbackState?.state == PlaybackState.STATE_PLAYING
        val now = System.currentTimeMillis()
        // Start the paused timer at the moment playback stops (never leave it at 0, which showed a huge number).
        if (playing) NowPlaying.pausedAt = 0L
        else if (NowPlaying.playing || NowPlaying.pausedAt == 0L) NowPlaying.pausedAt = now
        NowPlaying.playing = playing

        NowPlaying.title = m?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        NowPlaying.artist = (m?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: m?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)).orEmpty()
        val track = "${NowPlaying.title}|${NowPlaying.artist}"

        // Many players only give a picture link, or put the cover under a different key, so try all of them.
        val bmp = m?.let {
            it.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: it.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: it.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        }
        val uri = m?.let {
            it.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: it.getString(MediaMetadata.METADATA_KEY_ART_URI)
                ?: it.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
        }
        when {
            bmp != null -> { NowPlaying.art = fit(bmp); lastArtUri = null }
            uri != null && uri != lastArtUri -> {
                lastArtUri = uri
                if (track != lastTrack) NowPlaying.art = null
                scope.launch {
                    val b = withContext(Dispatchers.IO) { loadUri(uri) }
                    if (b != null && lastArtUri == uri) {
                        NowPlaying.art = fit(b)
                        MusicWidget().updateAll(applicationContext)
                    }
                }
            }
            uri != null -> Unit // same picture link as before: keep the art we already have
            track != lastTrack -> NowPlaying.art = null
        }
        lastTrack = track
        scope.launch { MusicWidget().updateAll(applicationContext) }
    }

    private fun loadUri(u: String): Bitmap? = try {
        val uri = Uri.parse(u)
        val stream = if (uri.scheme == "http" || uri.scheme == "https") URL(u).openStream() else contentResolver.openInputStream(uri)
        stream?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        null
    }

    /** Keeps covers sharp (up to 1200px) but never bigger, and keeps their proportions (the card crops to fit). */
    private fun fit(b: Bitmap, max: Int = 1200): Bitmap {
        val big = maxOf(b.width, b.height)
        if (big <= max) return b
        val k = max / big.toFloat()
        return Bitmap.createScaledBitmap(b, (b.width * k).toInt().coerceAtLeast(1), (b.height * k).toInt().coerceAtLeast(1), true)
    }
}

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val t = NowPlaying.controller?.transportControls ?: return
        if (NowPlaying.playing) t.pause() else t.play()
    }
}

class MusicWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val style = StyleStore.music(context)
        provideContent { MusicCard(context, style) }
    }
}

class MusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MusicWidget()
}

@Composable
private fun MusicCard(ctx: Context, s: MusicStyle) {
    val size = LocalSize.current
    val d = ctx.resources.displayMetrics.density
    val w = (size.width.value * d).toInt().coerceAtLeast(1)
    val h = (size.height.value * d).toInt().coerceAtLeast(1)
    // Render above the on-screen size so the card and cover stay sharp; the launcher scales it down.
    val k = HiRes.boost(ctx, w, h)
    val bg = GlassRenderer.musicCard(NowPlaying.art, s, (w * k).toInt(), (h * k).toInt(), d * k)
    val text = ColorProvider(Color(s.textColor))

    Box(GlanceModifier.fillMaxSize().clickable(actionRunCallback<PlayPauseAction>())) {
        Image(ImageProvider(bg), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        Column(GlanceModifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.Bottom) {
            if (s.showStatusChip && !NowPlaying.playing && NowPlaying.title.isNotEmpty()) {
                val mins = ((System.currentTimeMillis() - NowPlaying.pausedAt) / 60000).coerceAtLeast(0)
                Box(
                    GlanceModifier.background(ColorProvider(Color(0x44FFFFFF))).cornerRadius(12.dp)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) { Text("⏸ ${mins}m", style = TextStyle(color = text, fontSize = 13.sp, fontWeight = FontWeight.Bold)) }
                Spacer(GlanceModifier.height(4.dp))
            }
            Text(
                NowPlaying.title.ifEmpty { "Not playing" },
                style = TextStyle(color = text, fontSize = 15.sp, fontWeight = FontWeight.Bold), maxLines = 1,
            )
            if (s.showArtist && NowPlaying.artist.isNotEmpty()) {
                Text(NowPlaying.artist, style = TextStyle(color = ColorProvider(Color(s.textColor).copy(alpha = 0.7f)), fontSize = 13.sp), maxLines = 1)
            }
        }
    }
}
