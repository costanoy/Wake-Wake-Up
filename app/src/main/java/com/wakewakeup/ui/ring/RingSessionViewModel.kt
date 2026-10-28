package com.wakewakeup.ui.ring

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wakewakeup.WakeWakeUpApplication
import com.wakewakeup.session.AlarmRingService
import com.wakewakeup.session.AlarmSessionState
import com.wakewakeup.session.NotificationIds
import com.wakewakeup.session.Occurrence
import com.wakewakeup.session.PreAlarmPlanner
import com.wakewakeup.session.RingSession
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RingSessionViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as WakeWakeUpApplication).container

    val session: StateFlow<RingSession?> = AlarmSessionState.session

    /** Bom dia: the other morning alarms due within the hour (backups), offered to skip too. */
    var companions by mutableStateOf<List<Occurrence>>(emptyList())
        private set
    var companionsSkipped by mutableStateOf<List<Occurrence>>(emptyList())
        private set
    private var companionsFor: Long? = null

    fun startMission() = send(AlarmRingService.ACTION_START_MISSION)
    fun holdOn() = send(AlarmRingService.ACTION_HOLD_ON)
    fun pressKey(key: String) = send(AlarmRingService.ACTION_MISSION_KEY, key)
    fun setTypedText(text: String) = AlarmRingService.sendTypedText(getApplication(), text)
    fun confirm(typed: String? = null) = AlarmRingService.sendConfirm(getApplication(), typed)
    fun giveUp() = send(AlarmRingService.ACTION_GIVE_UP)
    fun finishSession() = send(AlarmRingService.ACTION_FINISH)

    /** Looks once per session (keyed by when it started) for alarms worth skipping along. */
    fun loadCompanions(session: RingSession) {
        if (companionsFor == session.startedAtMillis) return
        companionsFor = session.startedAtMillis
        companionsSkipped = emptyList()
        viewModelScope.launch {
            companions = PreAlarmPlanner.companions(container.alarmRepository.getAll(), System.currentTimeMillis())
        }
    }

    /** No sum to solve here: the mission that was just done proved the user is up. */
    fun skipCompanions() {
        val offered = companions
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val skipped = offered.filter { container.alarmRepository.skipOccurrence(it.alarmId, it.at, now) }
            companionsSkipped = skipped
            container.preAlarm.postReceipt(skipped, now)
        }
    }

    fun undoCompanions() {
        val skipped = companionsSkipped
        viewModelScope.launch {
            skipped.forEach { container.alarmRepository.undoSkipOccurrence(it.alarmId, it.at) }
            companionsSkipped = emptyList()
            NotificationManagerCompat.from(getApplication()).cancel(NotificationIds.SKIP_RECEIPT)
        }
    }

    private fun send(action: String, key: String? = null) {
        AlarmRingService.sendAction(getApplication(), action, key)
    }
}
