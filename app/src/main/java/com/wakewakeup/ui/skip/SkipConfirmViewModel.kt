package com.wakewakeup.ui.skip

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wakewakeup.WakeWakeUpApplication
import com.wakewakeup.data.Difficulty
import com.wakewakeup.session.MissionGenerator
import com.wakewakeup.session.NotificationIds
import com.wakewakeup.session.Occurrence
import com.wakewakeup.session.epochDayOf
import com.wakewakeup.session.nextRingMillis
import com.wakewakeup.session.nextTriggerMillis
import com.wakewakeup.ui.components.CalcKeys
import com.wakewakeup.ui.navigation.SkipRequest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** A ring the confirm screen offers to turn off, and the day the alarm rings again (null: never). */
data class SkipRow(val occurrence: Occurrence, val ringsAgainOn: LocalDate?)

enum class SkipStage { LOADING, CONFIRM, DONE, STALE }

private const val MAX_ANSWER_DIGITS = 4

class SkipConfirmViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as WakeWakeUpApplication).container

    var stage by mutableStateOf(SkipStage.LOADING)
        private set
    var rows by mutableStateOf<List<SkipRow>>(emptyList())
        private set

    /** The sum that stands between a half-asleep tap and a silenced alarm. */
    val question = MissionGenerator.mathQuestion(Difficulty.MEDIUM)
    var typed by mutableStateOf("")
        private set
    var wrong by mutableStateOf(false)
        private set

    private var loaded = false
    private var busy = false
    private var skipped: List<Occurrence> = emptyList()

    /** Keeps only the rings still worth turning off: unchanged, and more than a minute away. */
    fun load(request: SkipRequest?) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val from = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
            val valid = request?.alarmIds.orEmpty().zip(request?.ats.orEmpty()).mapNotNull { (id, at) ->
                val alarm = container.alarmRepository.getById(id) ?: return@mapNotNull null
                if (!alarm.enabled || at - now < 60_000L || nextRingMillis(alarm, from) != at) return@mapNotNull null
                val ringsAgain = if (alarm.days.isEmpty()) {
                    null
                } else {
                    LocalDate.ofEpochDay(epochDayOf(nextTriggerMillis(alarm, LocalDateTime.ofInstant(Instant.ofEpochMilli(at), zone))))
                }
                SkipRow(Occurrence(alarm.id, at, alarm.label, alarm.hour, alarm.minute), ringsAgain)
            }
            rows = valid
            stage = if (valid.isEmpty()) SkipStage.STALE else SkipStage.CONFIRM
        }
    }

    fun pressKey(key: String) {
        if (key == CalcKeys.OK) {
            confirm()
            return
        }
        // After a wrong answer the next key starts from an empty display, as in the mission.
        val current = if (wrong) "" else typed
        wrong = false
        typed = when (key) {
            CalcKeys.BACKSPACE -> current.dropLast(1)
            CalcKeys.CLEAR -> ""
            CalcKeys.SIGN -> if (current.startsWith("-")) current.drop(1) else "-$current"
            else -> if (key.all { it.isDigit() } && current.trimStart('-').length < MAX_ANSWER_DIGITS) current + key else current
        }
    }

    fun confirm() {
        if (stage != SkipStage.CONFIRM || busy || typed.isBlank() || typed == "-") return
        if (typed != question.answer) {
            wrong = true
            return
        }
        busy = true
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                skipped = rows.map { it.occurrence }.filter { container.alarmRepository.skipOccurrence(it.alarmId, it.at, now) }
                if (skipped.isEmpty()) {
                    stage = SkipStage.STALE
                } else {
                    container.preAlarm.postReceipt(skipped, now)
                    stage = SkipStage.DONE
                }
            } finally {
                busy = false
            }
        }
    }

    fun undo(onUndone: () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                skipped.forEach { container.alarmRepository.undoSkipOccurrence(it.alarmId, it.at) }
                skipped = emptyList()
                NotificationManagerCompat.from(getApplication()).cancel(NotificationIds.SKIP_RECEIPT)
                onUndone()
            } finally {
                busy = false
            }
        }
    }
}
