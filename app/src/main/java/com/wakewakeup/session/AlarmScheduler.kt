package com.wakewakeup.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wakewakeup.data.Alarm
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

const val EXTRA_ALARM_ID = "extra_alarm_id"

/**
 * Wraps AlarmManager.setAlarmClock, which the platform treats as a genuine alarm-clock trigger:
 * it fires on time even in Doze, may start the ringing foreground service from the background,
 * and feeds the "next alarm" shown in the status bar and on the lock screen. It is an exact-alarm
 * API: on Android 12+ it needs USE_EXACT_ALARM (13+) or the "Alarms & reminders" access
 * (SCHEDULE_EXACT_ALARM, which the user can revoke on 12/12L).
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** False when Android currently refuses exact alarms, so nothing can be armed. */
    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * Arms the alarm's next ring, or clears it when the alarm is off. Returns false when an enabled
     * alarm could not be armed (exact alarms not allowed); the list warns about that.
     */
    fun schedule(alarm: Alarm): Boolean {
        if (!alarm.enabled) {
            cancel(alarm)
            return true
        }
        return scheduleExact(alarm, nextRingMillis(alarm))
    }

    private fun scheduleExact(alarm: Alarm, triggerAt: Long): Boolean {
        if (!canScheduleExact()) return false
        val showIntent = PendingIntent.getActivity(
            context, alarm.id.toInt(),
            Intent(context, com.wakewakeup.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return try {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), ringIntent(alarm))
            true
        } catch (e: SecurityException) {
            // Exact-alarm access was revoked between the check and the call.
            false
        }
    }

    private fun ringIntent(alarm: Alarm): PendingIntent = PendingIntent.getBroadcast(
        context, alarm.id.toInt(),
        Intent(context, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarm.id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun cancel(alarm: Alarm) {
        alarmManager.cancel(ringIntent(alarm))
    }
}

/**
 * Earliest upcoming millis matching [Alarm.days] (0 = Monday .. 6 = Sunday), ignoring any skip.
 * An alarm with no selected days is treated as a one-off for the next occurrence of that time of
 * day. Pure function so the scheduler and the UI share one source of truth.
 */
fun nextTriggerMillis(alarm: Alarm, from: LocalDateTime = LocalDateTime.now()): Long {
    val zone = ZoneId.systemDefault()
    for (offset in 0..7) {
        val candidateDate = from.toLocalDate().plusDays(offset.toLong())
        val candidate = candidateDate.atTime(alarm.hour, alarm.minute)
        if (candidate.isBefore(from) || candidate.isEqual(from)) continue
        val isoDayIndex = isoDayIndexOf(candidateDate)
        if (alarm.days.isEmpty() || alarm.days.contains(isoDayIndex)) {
            return candidate.atZone(zone).toInstant().toEpochMilli()
        }
    }
    return from.plusDays(1).atZone(zone).toInstant().toEpochMilli()
}

/**
 * When the alarm really rings next: [nextTriggerMillis], unless that occurrence falls on the day
 * the alarm skips ([Alarm.skipDate]), in which case the occurrence after it.
 */
fun nextRingMillis(alarm: Alarm, from: LocalDateTime = LocalDateTime.now()): Long {
    val next = nextTriggerMillis(alarm, from)
    val skip = alarm.skipDate ?: return next
    if (alarm.days.isEmpty() || epochDayOf(next) != skip) return next
    return nextTriggerMillis(alarm, LocalDateTime.ofInstant(Instant.ofEpochMilli(next), ZoneId.systemDefault()))
}

/** True when the alarm's next occurrence is the day it skips. */
fun isSkippingNext(alarm: Alarm, from: LocalDateTime = LocalDateTime.now()): Boolean {
    val skip = alarm.skipDate ?: return false
    return alarm.enabled && alarm.days.isNotEmpty() && epochDayOf(nextTriggerMillis(alarm, from)) == skip
}

/** 0 = Monday .. 6 = Sunday, matching [Alarm.days]. */
fun isoDayIndexOf(date: LocalDate): Int =
    date.dayOfWeek.let { if (it == DayOfWeek.SUNDAY) 6 else it.value - 1 }

/** Calendar date (epoch day) of an instant in the phone's time zone. */
fun epochDayOf(millis: Long): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

/** Calendar date (epoch day) of the alarm's next occurrence, ignoring any skip. */
fun nextTriggerEpochDay(alarm: Alarm, from: LocalDateTime = LocalDateTime.now()): Long =
    epochDayOf(nextTriggerMillis(alarm, from))
