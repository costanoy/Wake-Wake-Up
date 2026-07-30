package com.wakewakeup.data

import java.time.LocalDate

data class WakeHistoryEntry(
    val id: Long = 0,
    val date: LocalDate,
    val alarmHour: Int,
    val alarmMinute: Int,
    val wokeHour: Int,
    val wokeMinute: Int,
    val waits: Int,
    val durationSec: Int,
)
