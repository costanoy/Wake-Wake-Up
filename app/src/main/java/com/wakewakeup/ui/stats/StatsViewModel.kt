package com.wakewakeup.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wakewakeup.WakeWakeUpApplication
import com.wakewakeup.data.WakeHistoryEntry
import com.wakewakeup.ui.components.formatClock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class BarPoint(val dayIndex: Int, val label: String, val minutesValue: Double, val isToday: Boolean)
data class VsRow(val dayIndex: Int, val alarm: String, val woke: String, val deltaMinutes: Int)
data class StatsUiState(
    val bars: List<BarPoint> = emptyList(),
    val todayText: String = "—",
    val waitsToday: Int = 0,
    val avg7Text: String = "—",
    val vsRows: List<VsRow> = emptyList(),
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as WakeWakeUpApplication).container.wakeHistoryRepository

    val uiState: StateFlow<StatsUiState> = repository.observeRecent(7)
        .map { toUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    private fun toUiState(entries: List<WakeHistoryEntry>): StatsUiState {
        val today = LocalDate.now()
        val bars = entries.map { entry ->
            BarPoint(
                dayIndex = entry.date.dayOfWeek.value - 1,
                label = "%dm".format(entry.durationSec / 60),
                minutesValue = entry.durationSec / 60.0,
                isToday = entry.date == today,
            )
        }
        val todayEntry = entries.lastOrNull { it.date == today }
        val avgSec = if (entries.isNotEmpty()) entries.map { it.durationSec }.average().toInt() else 0
        val vsRows = entries.takeLast(4).reversed().map { entry ->
            VsRow(
                dayIndex = entry.date.dayOfWeek.value - 1,
                alarm = formatClock(entry.alarmHour, entry.alarmMinute),
                woke = formatClock(entry.wokeHour, entry.wokeMinute),
                deltaMinutes = wokeDeltaMinutes(entry),
            )
        }
        return StatsUiState(
            bars = bars,
            todayText = todayEntry?.let { compactDuration(it.durationSec) } ?: "—",
            waitsToday = todayEntry?.waits ?: 0,
            avg7Text = if (entries.isNotEmpty()) compactDuration(avgSec) else "—",
            vsRows = vsRows,
        )
    }

    private fun wokeDeltaMinutes(entry: WakeHistoryEntry): Int {
        val alarmMinutes = entry.alarmHour * 60 + entry.alarmMinute
        val wokeMinutes = entry.wokeHour * 60 + entry.wokeMinute
        return (wokeMinutes - alarmMinutes).let { if (it < 0) it + 24 * 60 else it }
    }

    private fun compactDuration(totalSec: Int): String = "%dm%02d".format(totalSec / 60, totalSec % 60)
}
