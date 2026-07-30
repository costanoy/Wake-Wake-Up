package com.wakewakeup.ui.navigation

object Destinations {
    const val LIST = "list"
    const val EDIT = "edit/{alarmId}"
    const val TASK = "task"
    const val STATS = "stats"

    const val ARG_ALARM_ID = "alarmId"
    const val NEW_ALARM_ID = -1L

    fun edit(alarmId: Long) = "edit/$alarmId"
}
