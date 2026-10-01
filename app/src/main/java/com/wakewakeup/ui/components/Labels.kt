package com.wakewakeup.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.wakewakeup.R
import com.wakewakeup.data.Difficulty
import com.wakewakeup.data.TaskType
import com.wakewakeup.session.isoDayIndexOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun difficultyName(difficulty: Difficulty): String = stringResource(
    when (difficulty) {
        Difficulty.EASY -> R.string.easy
        Difficulty.MEDIUM -> R.string.medium
        Difficulty.HARD -> R.string.hard
    },
)

/** Short screen-print task label on an alarm card (MATEMÁTICA / FRASE). */
@Composable
fun taskCardLabel(type: TaskType): String = stringResource(
    when (type) {
        TaskType.MATH -> R.string.task_label_math
        TaskType.PHRASE -> R.string.task_label_phrase
    },
)

/** "Matemática, 3 contas" / "Digitar uma frase" on the edit screen's TAREFA row. */
@Composable
fun taskSummary(type: TaskType, count: Int): String = when (type) {
    TaskType.MATH -> pluralStringResource(R.plurals.task_summary_math, count, count)
    TaskType.PHRASE -> stringResource(R.string.task_summary_phrase)
}

fun formatClock(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

/** "EM 8 H 12 MIN" for the list's PRÓXIMO display. */
@Composable
fun nextAlarmInText(nowMillis: Long, triggerMillis: Long): String {
    val totalMinutes = ((triggerMillis - nowMillis + 59_999L) / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val duration = if (hours > 0) {
        stringResource(R.string.duration_h_min, hours.toInt(), minutes.toInt())
    } else {
        stringResource(R.string.duration_min_only, minutes.toInt())
    }
    return stringResource(R.string.next_in, duration)
}

/** "hoje", "amanhã, quarta" or "sábado" — how the drawer names the day an alarm will ring. */
@Composable
fun relativeDayName(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val full = stringArrayResource(R.array.day_full)
    val name = full.getOrElse(isoDayIndexOf(date)) { "" }
    return when (date) {
        today -> stringResource(R.string.day_today)
        today.plusDays(1) -> stringResource(R.string.day_tomorrow, name)
        else -> name
    }
}

/** "QUARTA, 30 SET" / "WEDNESDAY, SEP 30" date line on the ringing and good-morning screens. */
@Composable
fun panelDate(date: LocalDate = LocalDate.now()): String {
    val locale = Locale.getDefault()
    val dayName = stringArrayResource(R.array.day_full).getOrElse(isoDayIndexOf(date)) { "" }
    val pattern = if (locale.language == "pt") "d MMM" else "MMM d"
    val rest = date.format(DateTimeFormatter.ofPattern(pattern, locale)).replace(".", "")
    return "$dayName, $rest".uppercase(locale)
}
