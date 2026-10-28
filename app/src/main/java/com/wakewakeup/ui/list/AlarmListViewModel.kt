package com.wakewakeup.ui.list

import android.app.Application
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

class AlarmListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as WakeWakeUpApplication).container.alarmRepository

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
}
