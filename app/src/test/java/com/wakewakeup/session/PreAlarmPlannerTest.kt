package com.wakewakeup.session

import com.wakewakeup.data.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** The rules of "Você quer desativar o próximo alarme?", on a fixed calendar and clock. */
class PreAlarmPlannerTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val monday = LocalDate.of(2026, 10, 12)
    private val everyDay = (0..6).toSet()
    private val minute = 60_000L

    private fun at(hour: Int, min: Int, date: LocalDate = monday) = date.atTime(hour, min).atZone(zone).toInstant().toEpochMilli()

    private fun alarm(id: Long, hour: Int, min: Int, days: Set<Int> = everyDay, enabled: Boolean = true, skipDate: Long? = null) =
        Alarm(id = id, hour = hour, minute = min, label = "A$id", days = days, enabled = enabled, skipDate = skipDate)

    private fun plan(alarms: List<Alarm>, now: Long, handled: Set<String> = emptySet(), appSeenAt: Long = 0L) =
        PreAlarmPlanner.plan(alarms, now, handled, appSeenAt, zone)

    @Test
    fun nothingIsAskedUntilAnHourBefore() {
        val p = plan(listOf(alarm(1, 6, 30)), now = at(4, 0))
        assertTrue(p.offer.isEmpty())
        assertEquals(at(5, 30), p.nextCheckAt)
    }

    @Test
    fun withinTheHourTheMorningAlarmIsOffered() {
        val p = plan(listOf(alarm(1, 6, 30)), now = at(5, 45))
        assertEquals(listOf(occurrenceKey(1, at(6, 30))), p.offer.map { it.key })
        assertEquals("A1", p.offer.single().label)
        assertNull("nothing else to look at later", p.nextCheckAt)
    }

    @Test
    fun backupAlarmsComeTogetherInOneOffer() {
        val alarms = listOf(alarm(1, 6, 0), alarm(2, 6, 5), alarm(3, 6, 10), alarm(4, 7, 30))
        val p = plan(alarms, now = at(5, 30))
        assertEquals(listOf(at(6, 0), at(6, 5), at(6, 10)), p.offer.map { it.at })
        // The 07:30 one is more than an hour after the first: asked about on its own, later.
        assertEquals(at(6, 30), p.nextCheckAt)
    }

    @Test
    fun neverInTheLastFiveMinutesBeforeARing() {
        val p = plan(listOf(alarm(1, 6, 30)), now = at(6, 26))
        assertTrue(p.offer.isEmpty())
        assertEquals("look again once it has rung", at(6, 31), p.nextCheckAt)
    }

    @Test
    fun anyAlarmAboutToRingHoldsTheOfferBack() {
        // A night alarm (never offered itself) rings in 3 minutes; the 03:40 one waits.
        val p = plan(listOf(alarm(1, 2, 58), alarm(2, 3, 40)), now = at(2, 55))
        assertTrue(p.offer.isEmpty())
        assertEquals(at(2, 59), p.nextCheckAt)
    }

    @Test
    fun onlyMorningAlarmsAreEverQuestioned() {
        // A 22:00 medicine reminder rings while its user is awake by definition.
        assertTrue(plan(listOf(alarm(1, 22, 0)), now = at(21, 30)).let { it.offer.isEmpty() && it.nextCheckAt == null })
        assertTrue(plan(listOf(alarm(1, 12, 0)), now = at(11, 30)).offer.isEmpty())
        assertTrue(plan(listOf(alarm(1, 2, 30)), now = at(2, 0)).offer.isEmpty())
        assertEquals(1, plan(listOf(alarm(1, 3, 0)), now = at(2, 20)).offer.size)
        assertEquals(1, plan(listOf(alarm(1, 11, 59)), now = at(11, 20)).offer.size)
    }

    @Test
    fun switchedOffAndSkippedRingsAreLeftAlone() {
        assertTrue(plan(listOf(alarm(1, 6, 30, enabled = false)), now = at(5, 45)).let { it.offer.isEmpty() && it.nextCheckAt == null })
        // Skipping today: the next ring is tomorrow's, so tomorrow is when to ask.
        val skipping = alarm(1, 6, 30, skipDate = monday.toEpochDay())
        val p = plan(listOf(skipping), now = at(5, 45))
        assertTrue(p.offer.isEmpty())
        assertEquals(at(5, 30, monday.plusDays(1)), p.nextCheckAt)
    }

    @Test
    fun eachRingIsAskedAboutOnce() {
        val alarms = listOf(alarm(1, 6, 0), alarm(2, 6, 5))
        val handled = setOf(occurrenceKey(1, at(6, 0)), occurrenceKey(2, at(6, 5)))
        val p = plan(alarms, now = at(5, 30), handled = handled)
        assertTrue(p.offer.isEmpty())
        assertEquals("tomorrow's rings are new ones", at(5, 0, monday.plusDays(1)), p.nextCheckAt)
    }

    @Test
    fun havingTheAppOpenSinceBeforeMeansNoNeedToAsk() {
        val alarms = listOf(alarm(1, 6, 30))
        val seen = plan(alarms, now = at(5, 45), appSeenAt = at(5, 10))
        assertTrue(seen.offer.isEmpty())
        assertEquals(listOf(occurrenceKey(1, at(6, 30))), seen.quietlyHandled.map { it.key })
        // Last night's look at the list doesn't count.
        assertEquals(1, plan(alarms, now = at(5, 45), appSeenAt = at(23, 0, monday.minusDays(1))).offer.size)
    }

    @Test
    fun anOfferNeverComesLaterThanAMinuteFromNow() {
        // The next cluster is less than an hour away already: look again soon, not in the past.
        val alarms = listOf(alarm(1, 6, 0), alarm(2, 7, 10))
        val p = plan(alarms, now = at(5, 50))
        assertEquals(listOf(at(6, 0)), p.offer.map { it.at })
        assertEquals(at(6, 10), p.nextCheckAt)
        val later = plan(alarms, now = at(6, 20), handled = setOf(occurrenceKey(1, at(6, 0))))
        assertEquals(listOf(at(7, 10)), later.offer.map { it.at })
    }

    @Test
    fun bomDiaOffersTheOtherMorningAlarmsDueWithinTheHour() {
        val alarms = listOf(
            alarm(1, 6, 15), alarm(2, 6, 40), alarm(3, 7, 30), alarm(4, 22, 0),
            alarm(5, 6, 35, enabled = false),
        )
        val now = at(6, 12)
        assertEquals(listOf(1L, 2L), PreAlarmPlanner.companions(alarms, now, zone).map { it.alarmId })
        // One that rings in less than a minute is too late to skip.
        assertTrue(PreAlarmPlanner.companions(listOf(alarm(1, 6, 13)), at(6, 12) + 30_000, zone).isEmpty())
        assertTrue(PreAlarmPlanner.companions(alarms, at(6, 12) + 61 * minute, zone).none { it.alarmId == 1L })
    }
}
