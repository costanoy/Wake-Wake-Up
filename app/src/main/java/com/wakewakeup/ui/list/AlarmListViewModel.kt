package com.wakewakeup.ui.list

import android.app.Application
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wakewakeup.WakeWakeUpApplication
import com.wakewakeup.data.Alarm
import com.wakewakeup.session.nextRingMillis
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt

/** The phone's alarm volume on the system's own scale: "2 of 7". */
data class AlarmVolume(val level: Int, val max: Int) {
    /** Under half: easy to sleep through. */
    val isLow: Boolean get() = level * 2 < max
}

class AlarmListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as WakeWakeUpApplication).container.alarmRepository
    private val audio: AudioManager? = application.getSystemService(AudioManager::class.java)

    /**
     * In time-of-day order, on or off, so switching an alarm never moves its card from under the
     * finger. Null until the first read from the database, so the empty state never flashes.
     */
    val alarms: StateFlow<List<Alarm>?> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleEnabled(alarm: Alarm) {
        viewModelScope.launch {
            repository.setEnabled(alarm, !alarm.enabled)
        }
    }

    fun skipNext(alarm: Alarm) {
        viewModelScope.launch {
            repository.skipNext(alarm)
        }
    }

    fun clearSkip(alarm: Alarm) {
        viewModelScope.launch {
            repository.clearSkip(alarm)
        }
    }

    /** The next time any alarm really rings (a skipped day doesn't count). */
    fun nextAlarmMillis(alarms: List<Alarm>, now: Long = System.currentTimeMillis()): Long? {
        val from = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
        return alarms.filter { it.enabled }.minOfOrNull { nextRingMillis(it, from) }
    }

    /** The alarm stream's volume right now (the volume keys change it behind the app's back). */
    fun alarmVolume(): AlarmVolume? = audio?.let {
        AlarmVolume(it.getStreamVolume(AudioManager.STREAM_ALARM), it.getStreamMaxVolume(AudioManager.STREAM_ALARM))
    }

    /**
     * Puts the alarm volume at 80 % with the system's volume panel showing, so the user sees what
     * changed and can still fine-tune it. A phone that refuses is left as it was.
     */
    fun turnUpAlarmVolume() {
        val manager = audio ?: return
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        runCatching { manager.setStreamVolume(AudioManager.STREAM_ALARM, (max * 0.8f).roundToInt(), AudioManager.FLAG_SHOW_UI) }
    }
}
