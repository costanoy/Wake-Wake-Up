package com.wakewakeup.session

import com.wakewakeup.data.Alarm
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        // Skipping Friday on Thursday night: it rings again on Monday.
        val weekdays = alarm(6, 30, 0, 1, 2, 3, 4).copy(skipDate = friday.toEpochDay())
        val thursdayNight = monday.plusDays(3).atTime(22, 0)
        assertEquals(millis(monday.plusDays(7).atTime(6, 30)), nextRingMillis(weekdays, thursdayNight))
        assertTrue(isSkippingNext(weekdays, thursdayNight))

        val daily = alarm(7, 0, 0, 1, 2, 3, 4, 5, 6).copy(skipDate = monday.toEpochDay())
        assertEquals(millis(monday.plusDays(1).atTime(7, 0)), nextRingMillis(daily, monday.atTime(6, 0)))

        val mondays = alarm(6, 30, 0).copy(skipDate = monday.toEpochDay())
        assertEquals(millis(monday.plusDays(7).atTime(6, 30)), nextRingMillis(mondays, monday.atTime(5, 0)))
    }

    @Test
    fun aSkipOnlyCountsWhenItsDayIsTheNextOccurrence() {
        val daily = alarm(7, 0, 0, 1, 2, 3, 4, 5, 6).copy(skipDate = monday.plusDays(2).toEpochDay())
        // Wednesday is skipped, but Tuesday comes first and still rings.
        assertEquals(millis(monday.plusDays(1).atTime(7, 0)), nextRingMillis(daily, monday.atTime(8, 0)))
        assertFalse(isSkippingNext(daily, monday.atTime(8, 0)))
        // Once that day has passed, the skip means nothing.
        assertEquals(millis(monday.plusDays(4).atTime(7, 0)), nextRingMillis(daily, monday.plusDays(3).atTime(8, 0)))
        assertFalse(isSkippingNext(daily, monday.plusDays(3).atTime(8, 0)))
    }

    @Test
    fun aSkipAfterMidnightStillNamesTheMorningAhead() {
        // At 00:30 on Tuesday, the next 06:30 is Tuesday's: that is the day a skip leaves out.
        val weekdays = alarm(6, 30, 0, 1, 2, 3, 4)
        val tuesday = monday.plusDays(1)
        assertEquals(tuesday.toEpochDay(), nextTriggerEpochDay(weekdays, tuesday.atTime(0, 30)))
        val skipping = weekdays.copy(skipDate = tuesday.toEpochDay())
        assertEquals(millis(monday.plusDays(2).atTime(6, 30)), nextRingMillis(skipping, tuesday.atTime(0, 30)))
    }

    @Test
    fun oneTimeAndSwitchedOffAlarmsIgnoreASkip() {
        val oneOff = alarm(7, 0).copy(skipDate = monday.toEpochDay())
        assertEquals(millis(monday.atTime(7, 0)), nextRingMillis(oneOff, monday.atTime(6, 0)))
        assertFalse(isSkippingNext(oneOff, monday.atTime(6, 0)))

        val off = alarm(7, 0, 0, 1, 2, 3, 4, 5, 6).copy(enabled = false, skipDate = monday.toEpochDay())
        assertFalse(isSkippingNext(off, monday.atTime(6, 0)))
    }

    @Test
    fun nextTriggerDayMatchesTheTrigger() {
        val weekdays = alarm(6, 30, 0, 1, 2, 3, 4)
        assertEquals(monday.plusDays(7).toEpochDay(), nextTriggerEpochDay(weekdays, friday.atTime(22, 0)))
        assertEquals(monday.plusDays(1).toEpochDay(), nextTriggerEpochDay(alarm(0, 0), monday.atTime(23, 59)))
    }
}
