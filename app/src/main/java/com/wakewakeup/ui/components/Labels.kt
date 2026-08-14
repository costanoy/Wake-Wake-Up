package com.wakewakeup.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wakewakeup.R
import com.wakewakeup.data.Difficulty
import com.wakewakeup.data.TaskType

@Composable
fun taskTypeName(type: TaskType): String = stringResource(
    when (type) {
        TaskType.MATH -> R.string.task_math_name
        TaskType.PHRASE -> R.string.task_phrase_name
    },
)

@Composable
fun taskTypeDesc(type: TaskType): String = stringResource(
    when (type) {
        TaskType.MATH -> R.string.task_math_desc
        TaskType.PHRASE -> R.string.task_phrase_desc
    },
)

@Composable
fun difficultyName(difficulty: Difficulty): String = stringResource(
    when (difficulty) {
        Difficulty.EASY -> R.string.easy
        Difficulty.MEDIUM -> R.string.medium
        Difficulty.HARD -> R.string.hard
    },
)

/** e.g. "Math · Medium" for a math alarm card chip, or just the task name otherwise. */
@Composable
fun taskCardLabel(type: TaskType, difficulty: Difficulty): String {
    val name = taskTypeName(type)
    return if (type == TaskType.MATH) "$name · ${difficultyName(difficulty)}" else name
}

fun formatClock(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

@Composable
fun nextAlarmInText(nowMillis: Long, triggerMillis: Long): String {
    val totalMinutes = ((triggerMillis - nowMillis) / 60000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val duration = if (hours > 0) {
        stringResource(R.string.duration_h_min, hours.toInt(), minutes.toInt())
    } else {
        stringResource(R.string.duration_min_only, minutes.toInt())
    }
    return stringResource(R.string.next_alarm_in, duration)
}
