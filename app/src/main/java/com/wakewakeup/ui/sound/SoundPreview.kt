package com.wakewakeup.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Short previews on the alarm stream, so what you hear is how loud the alarm will be.
 * The preview window is counted from when audio actually starts (a radio stream needs a
 * moment to connect), a preview that never starts gives up after a while, and [previewing]
 * says which sound, if any, is on right now.
 */
class SoundPreview(private val context: Context, private val scope: CoroutineScope) {
    private var player: MediaPlayer? = null
    private var stopJob: Job? = null

    /** The uri being previewed ([DEFAULT] for the system default), or null when silent. */
    var previewing by mutableStateOf<String?>(null)
        private set

    /** True from the moment a preview is asked for until the player is ready to start. */
    var loading by mutableStateOf(false)
        private set

    /** The uri whose preview could not start (error, or never connected), until the next [play]. */
    private var failed by mutableStateOf<String?>(null)

    fun isPreviewing(uri: String?) = previewing == (uri ?: DEFAULT)

    fun isFailed(uri: String?) = failed == (uri ?: DEFAULT)

    /** The preview key: stops what is playing for [uri], or plays it. */
    fun toggle(uri: String?, durationMs: Long) {
        if (isPreviewing(uri)) stop() else play(uri, durationMs)
    }

    private fun fail() {
        val key = previewing
        stop()
        failed = key
    }

    fun play(uri: String?, durationMs: Long) {
        stop()
        val target = uri?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val mp = MediaPlayer()
        player = mp
        previewing = uri ?: DEFAULT
        failed = null
        loading = true
        // A station can take the connection and never send audio: don't stay lit waiting for it.
        stopJob = scope.launch {
            delay(START_TIMEOUT_MS)
            fail()
        }
        runCatching {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            mp.setDataSource(context, target)
            mp.setOnPreparedListener {
                if (player !== mp) return@setOnPreparedListener
                loading = false
                it.start()
                stopJob?.cancel()
                stopJob = scope.launch {
                    delay(durationMs)
                    stop()
                }
            }
            mp.setOnErrorListener { _, _, _ -> if (player === mp) fail(); true }
            mp.setOnCompletionListener { if (player === mp) stop() }
            mp.prepareAsync()
        }.onFailure { fail() }
    }

    fun stop() {
        stopJob?.cancel()
        stopJob = null
        player?.let { mp ->
            runCatching { if (mp.isPlaying) mp.stop() }
            mp.release()
        }
        player = null
        previewing = null
        loading = false
    }

    companion object {
        private const val DEFAULT = "default"
        private const val START_TIMEOUT_MS = 10_000L
    }
}

/** A [SoundPreview] that goes quiet when the screen leaves composition. */
@Composable
fun rememberSoundPreview(): SoundPreview {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preview = remember { SoundPreview(context.applicationContext, scope) }
    DisposableEffect(Unit) { onDispose { preview.stop() } }
    return preview
}

/** The stop key's square: what the play triangle turns into while a preview is on. */
@Composable
internal fun StopGlyph(size: Dp, color: Color) =
    Canvas(Modifier.size(size)) { drawRoundRect(color, cornerRadius = CornerRadius(1.5.dp.toPx())) }
