package com.wakewakeup.ui.navigation

object Destinations {
    const val PERMISSIONS = "permissions"
    const val LIST = "list"
    const val EDIT = "edit/{alarmId}"
    const val TASK = "task"
    const val SOUND = "sound"
    const val RADIO = "radio"
    const val STATS = "stats"
    const val SKIP = "skip/{ids}/{ats}"

    const val ARG_ALARM_ID = "alarmId"
    const val ARG_SKIP_IDS = "ids"
    const val ARG_SKIP_ATS = "ats"
    const val NEW_ALARM_ID = -1L

    fun edit(alarmId: Long) = "edit/$alarmId"

    fun skip(request: SkipRequest) = "skip/${request.alarmIds.joinToString(",")}/${request.ats.joinToString(",")}"
}

/** The rings the pre-alarm notification offered to turn off: alarm ids and their ring times. */
data class SkipRequest(val alarmIds: List<Long>, val ats: List<Long>) {
    companion object {
        fun parse(ids: String?, ats: String?): SkipRequest? {
            val alarmIds = ids?.split(",")?.mapNotNull { it.toLongOrNull() } ?: return null
            val times = ats?.split(",")?.mapNotNull { it.toLongOrNull() } ?: return null
            return SkipRequest(alarmIds, times).takeIf { alarmIds.isNotEmpty() && alarmIds.size == times.size }
        }
    }
}
