package com.wakewakeup.session

import com.wakewakeup.data.Alarm
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone
import kotlin.random.Random

/** Bordas do cálculo do próximo toque: virada de mês e ano, 29 de fevereiro, horário de verão, pulos. */
class AlarmTimingEdgeTest {

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

    private fun alarm(hour: Int, minute: Int, vararg days: Int, skip: Long? = null) =
        Alarm(hour = hour, minute = minute, label = "", days = days.toSet(), skipDate = skip)

    private fun local(millis: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())

    private val everyDay = intArrayOf(0, 1, 2, 3, 4, 5, 6)

    @Test
    fun theLastNightOfTheYearRingsOnNewYearsDay() {
        val next = nextTriggerMillis(alarm(7, 0, *everyDay), LocalDateTime.of(2026, 12, 31, 23, 0))
        assertEquals(LocalDateTime.of(2027, 1, 1, 7, 0), local(next))
    }

    @Test
    fun theTwentyNinthOfFebruaryExistsOnlyInLeapYears() {
        val leap = nextTriggerMillis(alarm(7, 0, *everyDay), LocalDateTime.of(2028, 2, 28, 8, 0))
        assertEquals(LocalDateTime.of(2028, 2, 29, 7, 0), local(leap))
        val plain = nextTriggerMillis(alarm(7, 0, *everyDay), LocalDateTime.of(2027, 2, 28, 8, 0))
        assertEquals(LocalDateTime.of(2027, 3, 1, 7, 0), local(plain))
    }

    @Test
    fun aSingleWeekdayPassedTodayWaitsExactlyAWeek() {
        // 2026-10-12 is a Monday.
        val next = nextTriggerMillis(alarm(7, 0, 0), LocalDateTime.of(2026, 10, 12, 7, 1))
        assertEquals(LocalDateTime.of(2026, 10, 19, 7, 0), local(next))
    }

    @Test
    fun theSameMinuteAsTheAlarmIsAlreadyTomorrow() {
        val next = nextTriggerMillis(alarm(7, 0, *everyDay), LocalDateTime.of(2026, 10, 12, 7, 0))
        assertEquals(LocalDateTime.of(2026, 10, 13, 7, 0), local(next))
    }

    @Test
    fun oneSecondBeforeTheAlarmStillRingsToday() {
        val next = nextTriggerMillis(alarm(7, 0, *everyDay), LocalDateTime.of(2026, 10, 12, 6, 59, 59))
        assertEquals(LocalDateTime.of(2026, 10, 12, 7, 0), local(next))
    }

    @Test
    fun skippingTheOnlyWeekdayJumpsTwoWeeks() {
        val monday = LocalDate.of(2026, 10, 12)
        val skipped = alarm(7, 0, 0, skip = monday.plusDays(7).toEpochDay())
        val next = nextRingMillis(skipped, LocalDateTime.of(2026, 10, 12, 7, 1))
        assertEquals(LocalDateTime.of(2026, 10, 26, 7, 0), local(next))
    }

    @Test
    fun aSkipOnADayThatIsNotNextChangesNothing() {
        val monday = LocalDate.of(2026, 10, 12)
        val skipped = alarm(7, 0, *everyDay, skip = monday.plusDays(3).toEpochDay())
        val from = LocalDateTime.of(2026, 10, 12, 6, 0)
        assertEquals(nextTriggerMillis(skipped, from), nextRingMillis(skipped, from))
        assertFalse(isSkippingNext(skipped, from))
    }

    @Test
    fun skippingTodayWhenTodayStillComesRingsTomorrow() {
        val monday = LocalDate.of(2026, 10, 12)
        val skipped = alarm(7, 0, *everyDay, skip = monday.toEpochDay())
        val from = LocalDateTime.of(2026, 10, 12, 6, 0)
        assertTrue(isSkippingNext(skipped, from))
        assertEquals(LocalDateTime.of(2026, 10, 13, 7, 0), local(nextRingMillis(skipped, from)))
    }

    @Test
    fun theAlarmKeepsItsWallClockTimeAcrossTheSpringForwardChange() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        // Clocks jump on 2027-03-14 at 02:00. A 07:00 alarm must still ring at 07:00 local time.
        val next = nextTriggerMillis(alarm(7, 0, *everyDay), LocalDateTime.of(2027, 3, 13, 8, 0))
        assertEquals(LocalDateTime.of(2027, 3, 14, 7, 0), local(next))
    }

    @Test
    fun anAlarmInsideTheMissingHourStillRingsThatDay() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        val next = nextTriggerMillis(alarm(2, 30, *everyDay), LocalDateTime.of(2027, 3, 13, 23, 0))
        val ring = local(next)
        assertEquals(LocalDate.of(2027, 3, 14), ring.toLocalDate())
        assertTrue("rings after the clocks jump", ring.hour >= 3)
    }

    @Test
    fun theNextRingIsAlwaysAheadOnAMatchingDayAtTheRightTime() {
        val random = Random(42)
        repeat(2_000) {
            val days = (0..6).filter { random.nextInt(3) == 0 }.toSet()
            val a = Alarm(hour = random.nextInt(24), minute = random.nextInt(60), label = "", days = days)
            val from = LocalDateTime.of(2026, 1, 1, 0, 0)
                .plusMinutes(random.nextLong(0, 60L * 24 * 365 * 2))
                .plusSeconds(random.nextLong(0, 60))
            val next = local(nextTriggerMillis(a, from))
            assertTrue("$a from $from -> $next", next.isAfter(from))
            assertTrue("within a week: $a from $from -> $next", next.isBefore(from.plusDays(8)))
            assertEquals(a.hour, next.hour)
            assertEquals(a.minute, next.minute)
            if (days.isNotEmpty()) assertTrue("weekday of $next", isoDayIndexOf(next.toLocalDate()) in days)
        }
    }
}
