package com.wakewakeup.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.wakewakeup.data.Alarm
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

const val EXTRA_ALARM_ID = "extra_alarm_id"

/**
 * Wraps AlarmManager.setAlarmClock, which the platform treats as a genuine
 * alarm-clock trigger: it is exempt from the SCHEDULE_EXACT_ALARM runtime
 * check other exact alarms need, and it surfaces the "next alarm" glyph in
 * the status bar.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        if (!alarm.enabled) {
            cancel(alarm)
            return
        }
        val triggerAt = nextTriggerMillis(alarm)
        val showIntent = PendingIntent.getActivity(
            context, alarm.id.toInt(),
            Intent(context, com.wakewakeup.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val operation = PendingIntent.getBroadcast(
            context, alarm.id.toInt(),
            Intent(context, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarm.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), operation)
    }

    fun cancel(alarm: Alarm) {
        val operation = PendingIntent.getBroadcast(
            context, alarm.id.toInt(),
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarmManager.cancel(operation)
    }
}

/**
 * Earliest upcoming millis matching [Alarm.days] (0 = Monday .. 6 = Sunday).
 * An alarm with no selected days is treated as a one-off for the next
 * occurrence of that time of day. Pure function so both the scheduler and
 * the "next alarm in ..." UI copy share one source of truth.
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

/** 0 = Monday .. 6 = Sunday, matching [Alarm.days]. */
fun isoDayIndexOf(date: LocalDate): Int =
    date.dayOfWeek.let { if (it == DayOfWeek.SUNDAY) 6 else it.value - 1 }

/** Calendar date (epoch day) of the alarm's next occurrence. */
fun nextTriggerEpochDay(alarm: Alarm, from: LocalDateTime = LocalDateTime.now()): Long =
    Instant.ofEpochMilli(nextTriggerMillis(alarm, from)).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
