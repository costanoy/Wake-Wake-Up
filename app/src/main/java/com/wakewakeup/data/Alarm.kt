package com.wakewakeup.data

enum class TaskType { MATH, PHRASE }

enum class Difficulty { EASY, MEDIUM, HARD }

/**
 * Days of week, index 0 = Monday .. 6 = Sunday, matching the design's day-chip order.
 */
data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val days: Set<Int>,
    val enabled: Boolean = true,
    val taskType: TaskType = TaskType.MATH,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val taskCount: Int = 3,
    /** Null means "use the system's default alarm sound". */
    val soundUri: String? = null,
    val soundName: String? = null,
)
