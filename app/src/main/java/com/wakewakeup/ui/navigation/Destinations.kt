package com.wakewakeup.ui.navigation

object Destinations {
    const val PERMISSIONS = "permissions"
    const val LIST = "list"
    const val EDIT = "edit/{alarmId}"
    const val TASK = "task"
    const val SOUND = "sound"
    const val RADIO = "radio"
    const val STATS = "stats"

    const val ARG_ALARM_ID = "alarmId"
    const val NEW_ALARM_ID = -1L

    fun edit(alarmId: Long) = "edit/$alarmId"
}
