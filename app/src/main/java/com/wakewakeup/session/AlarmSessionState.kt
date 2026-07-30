package com.wakewakeup.session

import com.wakewakeup.data.Alarm
import com.wakewakeup.data.Difficulty
import com.wakewakeup.data.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val MISSION_TOTAL_SECONDS = 120
const val HOLD_ON_SECONDS = 300

enum class SessionScreen { RINGING, MISSION, GOOD_MORNING }

data class MissionState(
    val type: TaskType,
    val index: Int,
    val count: Int,
    val secondsLeft: Int = MISSION_TOTAL_SECONDS,
    val question: String = "",
    val answer: String = "",
    val typed: String = "",
    val phrase: String = "",
    val wrong: Boolean = false,
)

data class FinishedInfo(
    val tookText: String,
    val waits: Int,
    val alarmHour: Int,
    val alarmMinute: Int,
    val wokeHour: Int,
    val wokeMinute: Int,
    val durationSec: Int,
)

data class RingSession(
    val alarmId: Long,
    val alarmLabel: String,
    val alarmHour: Int,
    val alarmMinute: Int,
    val difficulty: Difficulty,
    val screen: SessionScreen = SessionScreen.RINGING,
    val level: Int = 1,
    val waits: Int = 0,
    val startedAtMillis: Long = System.currentTimeMillis(),
    val mission: MissionState? = null,
    val finished: FinishedInfo? = null,
)

/**
 * Single in-memory holder for the currently ringing alarm session. It is
 * written by [com.wakewakeup.session.AlarmRingService] (the only component
 * guaranteed to keep running) and observed by the ring/mission/good-morning
 * Compose screens hosted in [RingActivity].
 */
object AlarmSessionState {
    private val _session = MutableStateFlow<RingSession?>(null)
    val session: StateFlow<RingSession?> = _session.asStateFlow()

    fun start(alarm: Alarm) {
        _session.value = RingSession(
            alarmId = alarm.id,
            alarmLabel = alarm.label,
            alarmHour = alarm.hour,
            alarmMinute = alarm.minute,
            difficulty = alarm.difficulty,
        )
    }

    fun update(transform: (RingSession) -> RingSession) {
        _session.value = _session.value?.let(transform)
    }

    fun clear() {
        _session.value = null
    }
}
