package app.glasswidgets

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
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
    private val cb = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = push()
        override fun onPlaybackStateChanged(state: PlaybackState?) = push()
    }

    override fun onListenerConnected() {
        val msm = getSystemService(MediaSessionManager::class.java)
        val me = ComponentName(this, MediaListener::class.java)
        msm.addOnActiveSessionsChangedListener({ bind(it?.firstOrNull()) }, me)
        bind(msm.getActiveSessions(me).firstOrNull())
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
        if (!playing && NowPlaying.playing) NowPlaying.pausedAt = System.currentTimeMillis()
        NowPlaying.playing = playing
        NowPlaying.title = m?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        NowPlaying.artist = (m?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: m?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)).orEmpty()
        val raw = m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        NowPlaying.art = raw?.let { Bitmap.createScaledBitmap(it, 600, 600, true) }
        scope.launch { MusicWidget().updateAll(applicationContext) }
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
    val w = minOf((size.width.value * d).toInt(), 700).coerceAtLeast(1)
    val h = minOf((size.height.value * d).toInt(), 700).coerceAtLeast(1)
    val bg = GlassRenderer.musicCard(NowPlaying.art, s, w, h, d)
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
