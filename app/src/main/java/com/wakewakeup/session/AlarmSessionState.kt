package com.wakewakeup.session

import com.wakewakeup.data.Alarm
import com.wakewakeup.data.Difficulty
import com.wakewakeup.data.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val MISSION_TOTAL_SECONDS = 120
const val HOLD_ON_SECONDS = 300

/** "Aumentar aos poucos": from [RAMP_START_VOLUME] to full over this many seconds. */
const val RAMP_SECONDS = 60
const val RAMP_START_VOLUME = 0.15f

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
    /** The previous problem was just answered correctly and this is the next one. */
    val justCorrect: Boolean = false,
)

data class FinishedInfo(
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
    val rampUp: Boolean = true,
    val screen: SessionScreen = SessionScreen.RINGING,
    /** Current playback volume, 0..1 (drives the LED volume meter). */
    val volume: Float = 1f,
    val waits: Int = 0,
    val holdUntilMillis: Long? = null,
    val startedAtMillis: Long = System.currentTimeMillis(),
    /** When the current stretch of ringing began (resets after an Espere or a timed-out mission). */
    val ringingSinceMillis: Long = System.currentTimeMillis(),
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
            rampUp = alarm.rampUp,
            volume = if (alarm.rampUp) RAMP_START_VOLUME else 1f,
        )
    }

    fun update(transform: (RingSession) -> RingSession) {
        _session.value = _session.value?.let(transform)
    }

    fun clear() {
        _session.value = null
    }
}
