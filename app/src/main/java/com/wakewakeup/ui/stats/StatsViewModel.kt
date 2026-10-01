package com.wakewakeup.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wakewakeup.WakeWakeUpApplication
import com.wakewakeup.data.WakeHistoryEntry
import com.wakewakeup.session.isoDayIndexOf
import com.wakewakeup.ui.components.formatClock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlin.math.roundToInt

/** One column of the 7-day LED chart; [minutes] is null for a day with no wake-up recorded. */
data class DayColumn(val dayIndex: Int, val isToday: Boolean, val minutes: Int?)

data class VsRow(val dayIndex: Int, val isToday: Boolean, val alarm: String, val woke: String, val deltaMinutes: Int)

/** The last seven calendar days ending today, with nothing recorded yet. */
fun emptyWeek(today: LocalDate = LocalDate.now()): List<DayColumn> =
    (6 downTo 0).map { back -> DayColumn(isoDayIndexOf(today.minusDays(back.toLong())), back == 0, null) }

data class StatsUiState(
    val loaded: Boolean = false,
    val hasData: Boolean = false,
    val days: List<DayColumn> = emptyWeek(),
    val todayMinutes: Int? = null,
    val todayWoke: String? = null,
    val avgMinutes: Int? = null,
    val avgWaits: Double? = null,
    val vsRows: List<VsRow> = emptyList(),
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as WakeWakeUpApplication).container.wakeHistoryRepository

    val uiState: StateFlow<StatsUiState> = repository.observeLastDays(7)
        .map { toUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    private fun toUiState(entries: List<WakeHistoryEntry>): StatsUiState {
        val today = LocalDate.now()
        val days = (6 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            val onDay = entries.filter { it.date == date }
            DayColumn(
                dayIndex = isoDayIndexOf(date),
                isToday = back == 0,
                minutes = if (onDay.isEmpty()) null else toMinutes(onDay.map { it.durationSec }.average()),
            )
        }
        val todayEntry = entries.lastOrNull { it.date == today }
        val vsRows = entries.takeLast(4).reversed().map { entry ->
            VsRow(
                dayIndex = isoDayIndexOf(entry.date),
                isToday = entry.date == today,
                alarm = formatClock(entry.alarmHour, entry.alarmMinute),
                woke = formatClock(entry.wokeHour, entry.wokeMinute),
                deltaMinutes = wokeDeltaMinutes(entry),
            )
        }
        return StatsUiState(
            loaded = true,
            hasData = entries.isNotEmpty(),
            days = days,
            todayMinutes = todayEntry?.let { toMinutes(it.durationSec.toDouble()) },
            todayWoke = todayEntry?.let { formatClock(it.wokeHour, it.wokeMinute) },
            avgMinutes = if (entries.isEmpty()) null else toMinutes(entries.map { it.durationSec }.average()),
            avgWaits = if (entries.isEmpty()) null else entries.map { it.waits }.average(),
            vsRows = vsRows,
        )
    }

    private fun toMinutes(seconds: Double): Int = (seconds / 60.0).roundToInt()

    private fun wokeDeltaMinutes(entry: WakeHistoryEntry): Int {
        val alarmMinutes = entry.alarmHour * 60 + entry.alarmMinute
        val wokeMinutes = entry.wokeHour * 60 + entry.wokeMinute
        return (wokeMinutes - alarmMinutes).let { if (it < 0) it + 24 * 60 else it }
    }
}
