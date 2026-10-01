package com.wakewakeup.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Short previews on the alarm stream, so what you hear is how loud the alarm will be.
 * The preview window is counted from when audio actually starts (a radio stream needs a
 * moment to connect), and [previewing] says which sound, if any, is on right now.
 */
class SoundPreview(private val context: Context, private val scope: CoroutineScope) {
    private var player: MediaPlayer? = null
    private var stopJob: Job? = null

    /** The uri being previewed ([DEFAULT] for the system default), or null when silent. */
    var previewing by mutableStateOf<String?>(null)
        private set

    fun isPreviewing(uri: String?) = previewing == (uri ?: DEFAULT)

    fun play(uri: String?, durationMs: Long) {
        stop()
        val target = uri?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val mp = MediaPlayer()
        player = mp
        previewing = uri ?: DEFAULT
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
                it.start()
                stopJob = scope.launch {
                    delay(durationMs)
                    stop()
                }
            }
            mp.setOnErrorListener { _, _, _ -> if (player === mp) stop(); true }
            mp.setOnCompletionListener { if (player === mp) stop() }
            mp.prepareAsync()
        }.onFailure { stop() }
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
    }

    companion object {
        private const val DEFAULT = "default"
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
