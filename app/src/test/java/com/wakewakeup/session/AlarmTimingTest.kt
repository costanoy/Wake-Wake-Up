package com.wakewakeup.session

import com.wakewakeup.data.Alarm
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

class AlarmTimingTest {

    private lateinit var originalZone: TimeZone

    @Before
    fun useBrazilTime() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalZone)
    }

    private val monday = LocalDate.of(2026, 10, 12)
    private val friday = monday.plusDays(4)

    private fun alarm(hour: Int, minute: Int, vararg days: Int) = Alarm(hour = hour, minute = minute, label = "", days = days.toSet())
    private fun millis(at: LocalDateTime) = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun dayIndexesRunMondayToSunday() {
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        for (i in 0..6) assertEquals(i, isoDayIndexOf(monday.plusDays(i.toLong())))
    }

    @Test
    fun oneOffLaterTodayRingsToday() {
        assertEquals(millis(monday.atTime(7, 0)), nextTriggerMillis(alarm(7, 0), monday.atTime(6, 59)))
    }

    @Test
    fun oneOffAlreadyPastRingsTomorrow() {
        assertEquals(millis(monday.plusDays(1).atTime(7, 0)), nextTriggerMillis(alarm(7, 0), monday.atTime(7, 1)))
    }

    @Test
    fun theMinuteAnAlarmFiresIsNotScheduledAgain() {
        // AlarmReceiver reschedules a repeating alarm while it is ringing: it must move on to the
        // next day, not fire again for the same minute.
        val daily = alarm(7, 0, 0, 1, 2, 3, 4, 5, 6)
        val tomorrow = millis(monday.plusDays(1).atTime(7, 0))
        assertEquals(tomorrow, nextTriggerMillis(daily, monday.atTime(7, 0)))
        assertEquals(tomorrow, nextTriggerMillis(daily, monday.atTime(7, 0, 0, 500_000_000)))
    }

    @Test
    fun weekdayAlarmOnFridayNightRingsMonday() {
        val weekdays = alarm(6, 30, 0, 1, 2, 3, 4)
        assertEquals(millis(monday.plusDays(7).atTime(6, 30)), nextTriggerMillis(weekdays, friday.atTime(22, 0)))
        assertEquals(millis(friday.atTime(6, 30)), nextTriggerMillis(weekdays, friday.atTime(5, 0)))
    }

    @Test
    fun weeklyAlarmThatAlreadyRangWaitsAWeek() {
        val mondays = alarm(6, 30, 0)
        assertEquals(millis(monday.plusDays(7).atTime(6, 30)), nextTriggerMillis(mondays, monday.atTime(6, 31)))
    }

    @Test
    fun weekendAlarmFromMonday() {
        val weekend = alarm(9, 15, 5, 6)
        assertEquals(millis(monday.plusDays(5).atTime(9, 15)), nextTriggerMillis(weekend, monday.atTime(8, 0)))
    }

    @Test
    fun midnightAlarm() {
        assertEquals(millis(monday.plusDays(1).atTime(0, 0)), nextTriggerMillis(alarm(0, 0), monday.atTime(23, 59)))
    }

    @Test
    fun skippingADayRingsAtTheFollowingOccurrence() {
        val weekdays = alarm(6, 30, 0, 1, 2, 3, 4)
        // Switched off on Thursday night: Friday is skipped, it rings again on Monday.
        assertEquals(millis(monday.plusDays(7).atTime(6, 30)), nextTriggerMillisAfterSkip(weekdays, friday.toEpochDay(), monday.plusDays(3).atTime(22, 0)))
        assertEquals(monday.plusDays(7).toEpochDay(), nextTriggerEpochDayAfterSkip(weekdays, friday.toEpochDay(), monday.plusDays(3).atTime(22, 0)))

        val daily = alarm(7, 0, 0, 1, 2, 3, 4, 5, 6)
        assertEquals(millis(monday.plusDays(1).atTime(7, 0)), nextTriggerMillisAfterSkip(daily, monday.toEpochDay(), monday.atTime(6, 0)))

        val mondays = alarm(6, 30, 0)
        assertEquals(millis(monday.plusDays(7).atTime(6, 30)), nextTriggerMillisAfterSkip(mondays, monday.toEpochDay(), monday.atTime(5, 0)))
    }

    @Test
    fun aSkipThatAlreadyPassedNeverPointsBackInTime() {
        // The phone was off through the skipped day: the next ring is still in the future.
        val daily = alarm(7, 0, 0, 1, 2, 3, 4, 5, 6)
        val now = monday.plusDays(3).atTime(8, 0)
        assertEquals(millis(monday.plusDays(4).atTime(7, 0)), nextTriggerMillisAfterSkip(daily, monday.toEpochDay(), now))
    }

    @Test
    fun nextTriggerDayMatchesTheTrigger() {
        val weekdays = alarm(6, 30, 0, 1, 2, 3, 4)
        assertEquals(monday.plusDays(7).toEpochDay(), nextTriggerEpochDay(weekdays, friday.atTime(22, 0)))
        assertEquals(monday.plusDays(1).toEpochDay(), nextTriggerEpochDay(alarm(0, 0), monday.atTime(23, 59)))
    }
}
