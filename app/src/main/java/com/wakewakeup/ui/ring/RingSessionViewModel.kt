package com.wakewakeup.ui.ring

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.UserManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wakewakeup.WakeWakeUpApplication
import com.wakewakeup.session.AlarmRingService
import com.wakewakeup.session.AlarmSessionState
import com.wakewakeup.session.NotificationIds
import com.wakewakeup.session.Occurrence
import com.wakewakeup.session.PreAlarmPlanner
import com.wakewakeup.session.RingSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class RingSessionViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as WakeWakeUpApplication).container

    val session: StateFlow<RingSession?> = AlarmSessionState.session

    /** Bom dia: the other morning alarms due within the hour (backups), offered to skip too. */
    var companions by mutableStateOf<List<Occurrence>>(emptyList())
        private set
    var companionsSkipped by mutableStateOf<List<Occurrence>>(emptyList())
        private set
    private var companionsFor: Long? = null

    /** Bom dia: the average time to wake up over the last week, in minutes (null: no earlier wake-up). */
    var weekAverageMinutes by mutableStateOf<Int?>(null)
        private set

    /** The history was read, so Bom dia can compare (or say it is the first). */
    var comparisonReady by mutableStateOf(false)
        private set

    fun startMission() = send(AlarmRingService.ACTION_START_MISSION)
    fun holdOn() = send(AlarmRingService.ACTION_HOLD_ON)
    fun pressKey(key: String) = send(AlarmRingService.ACTION_MISSION_KEY, key)
    fun setTypedText(text: String) = AlarmRingService.sendTypedText(getApplication(), text)
    fun confirm(typed: String? = null) = AlarmRingService.sendConfirm(getApplication(), typed)
    fun giveUp() = send(AlarmRingService.ACTION_GIVE_UP)
    fun finishSession() = send(AlarmRingService.ACTION_FINISH)

    /** Looks once per session (keyed by when it started) for alarms worth skipping along. */
    fun loadCompanions(session: RingSession) {
        // Before the first unlock after boot the database can't be opened: no backups to offer.
        if (!isUnlocked()) return
        if (companionsFor == session.startedAtMillis) return
        companionsFor = session.startedAtMillis
        companionsSkipped = emptyList()
        loadWeekAverage(session)
        viewModelScope.launch {
            try {
                companions = PreAlarmPlanner.companions(container.alarmRepository.getAll(), System.currentTimeMillis())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                companions = emptyList()
            }
        }
    }

    /** Reads the last week once per session, off the main thread (Room), without today's own record. */
    private fun loadWeekAverage(session: RingSession) {
        viewModelScope.launch {
            try {
                val finished = session.finished
                val today = LocalDate.now()
                val past = container.wakeHistoryRepository.observeLastDays(7).first().toMutableList()
                // The wake-up that just ended is recorded on Concluir; if it is already there, leave it out.
                if (finished != null) {
                    past.indexOfLast {
                        it.date == today && it.durationSec == finished.durationSec &&
                            it.wokeHour == finished.wokeHour && it.wokeMinute == finished.wokeMinute
                    }.takeIf { it >= 0 }?.let { past.removeAt(it) }
                }
                weekAverageMinutes = if (past.isEmpty()) null else (past.sumOf { it.durationSec } / past.size + 30) / 60
                comparisonReady = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                comparisonReady = false
            }
        }
    }

    /** No sum to solve here: the mission that was just done proved the user is up. */
    fun skipCompanions() {
        if (!isUnlocked()) return
        val offered = companions
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val skipped = offered.filter { container.alarmRepository.skipOccurrence(it.alarmId, it.at, now) }
                companionsSkipped = skipped
                container.preAlarm.postReceipt(skipped, now)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The database is out of reach: nothing was skipped, and the offer stays.
            }
        }
    }

    fun undoCompanions() {
        if (!isUnlocked()) return
        val skipped = companionsSkipped
        viewModelScope.launch {
            try {
                skipped.forEach { container.alarmRepository.undoSkipOccurrence(it.alarmId, it.at) }
                companionsSkipped = emptyList()
                NotificationManagerCompat.from(getApplication()).cancel(NotificationIds.SKIP_RECEIPT)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Same as skipping: with the database out of reach the skip stays as it was.
            }
        }
    }

    private fun isUnlocked() = UserManagerCompat.isUserUnlocked(getApplication<Application>())

    private fun send(action: String, key: String? = null) {
        AlarmRingService.sendAction(getApplication(), action, key)
    }
}
