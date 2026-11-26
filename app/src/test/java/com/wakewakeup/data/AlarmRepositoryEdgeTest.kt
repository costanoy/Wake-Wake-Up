package com.wakewakeup.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.session.nextRingMillis
import com.wakewakeup.session.nextTriggerMillis
import com.wakewakeup.testing.AppEnv
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class AlarmRepositoryEdgeTest {

    @get:Rule val env = AppEnv()

    private val everyDay = (0..6).toSet()
    private val minute = 60_000L

    private fun daily(hour: Int = 7, enabled: Boolean = true) =
        env.save(Alarm(hour = hour, minute = 0, label = "", days = everyDay, enabled = enabled))

    @Test
    fun aRingLessThanAMinuteAwayCannotBeSkipped() = runBlocking {
        val alarm = daily()
        val at = nextRingMillis(alarm)
        assertFalse(env.repository.skipOccurrence(alarm.id, at, now = at - 59_000))
        assertNull(env.reload(alarm)!!.skipDate)
        assertTrue(env.repository.skipOccurrence(alarm.id, at, now = at - 61_000))
        assertNotNull(env.reload(alarm)!!.skipDate)
    }

    @Test
    fun onlyTheNextRingCanBeSkippedNotALaterOne() = runBlocking {
        val alarm = daily()
        val later = nextRingMillis(alarm) + 24 * 60 * minute
        assertFalse(env.repository.skipOccurrence(alarm.id, later, now = System.currentTimeMillis()))
        assertNull(env.reload(alarm)!!.skipDate)
    }

    @Test
    fun aSwitchedOffAlarmCannotBeSkipped() = runBlocking {
        val alarm = daily(enabled = false)
        val at = nextTriggerMillis(alarm)
        assertFalse(env.repository.skipOccurrence(alarm.id, at, now = at - 600 * minute))
    }

    @Test
    fun skippingAnUnknownAlarmIsRefused() = runBlocking {
        assertFalse(env.repository.skipOccurrence(9_999, System.currentTimeMillis() + 600 * minute))
        assertFalse(env.repository.undoSkipOccurrence(9_999, System.currentTimeMillis() + 600 * minute))
    }

    @Test
    fun skippingThenUndoingAOneOffRingBringsItBack() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = emptySet()))
        val at = nextTriggerMillis(alarm)
        val now = at - 600 * minute
        assertTrue(env.repository.skipOccurrence(alarm.id, at, now))
        assertFalse(env.reload(alarm)!!.enabled)
        assertNull(env.scheduledFor(alarm))
        assertTrue(env.repository.undoSkipOccurrence(alarm.id, at, now))
        assertTrue(env.reload(alarm)!!.enabled)
        assertEquals(at, env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun undoingASkipIsAcceptedUntilTheRingInstantAndRefusedAfterIt() = runBlocking {
        val alarm = daily()
        val at = nextRingMillis(alarm)
        assertTrue(env.repository.skipOccurrence(alarm.id, at, now = at - 600 * minute))
        // The ring has already come: too late to bring it back.
        assertFalse(env.repository.undoSkipOccurrence(alarm.id, at, now = at + 1_000))
        assertNotNull(env.reload(alarm)!!.skipDate)
        // Thirty seconds before it, the undo still works and arms the ring again.
        assertTrue(env.repository.undoSkipOccurrence(alarm.id, at, now = at - 30_000))
        assertNull(env.reload(alarm)!!.skipDate)
        assertEquals(at, env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun cancellingRightAfterClearSkipNeverLeavesTheDatabaseAndAlarmManagerDisagreeing() = runBlocking {
        val alarm = daily()
        repeat(15) { round ->
            val skipped = env.reload(alarm)!!.let { env.repository.skipNext(it); env.reload(alarm)!! }
            val job = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { env.repository.clearSkip(skipped) }
            if (round % 3 != 0) kotlinx.coroutines.delay(round % 3L)
            job.cancel()
            job.join()
            val now = env.reload(alarm)!!
            val armedAt = env.scheduledFor(alarm)!!.triggerAtMs
            // Either the skip is still there and the ring was not moved, or it is gone and the ring is the next day's.
            assertEquals("round $round", nextRingMillis(now), armedAt)
        }
    }

    @Test
    fun undoingASkipOfAnotherDayChangesNothing() = runBlocking {
        val alarm = daily()
        val at = nextRingMillis(alarm)
        assertTrue(env.repository.skipOccurrence(alarm.id, at, now = at - 600 * minute))
        assertFalse(env.repository.undoSkipOccurrence(alarm.id, at + 24 * 60 * minute, now = at - 600 * minute))
        assertNotNull(env.reload(alarm)!!.skipDate)
    }

    @Test
    fun savingASwitchedOffAlarmLeavesNothingInAlarmManager() {
        val alarm = daily()
        assertNotNull(env.scheduledFor(alarm))
        env.save(alarm.copy(enabled = false))
        assertNull(env.scheduledFor(alarm))
    }

    @Test
    fun editingTheTimeMovesTheRingInsteadOfAddingAnotherOne() {
        val alarm = daily(hour = 7)
        val edited = env.save(alarm.copy(hour = 9))
        assertEquals(1, env.scheduledAlarms().size)
        assertEquals(nextTriggerMillis(edited), env.scheduledFor(edited)!!.triggerAtMs)
    }

    @Test
    fun twoAlarmsAtTheSameTimeBothStayArmed() {
        val a = daily(hour = 7)
        val b = daily(hour = 7)
        assertEquals(2, env.scheduledAlarms().size)
        assertNotNull(env.scheduledFor(a))
        assertNotNull(env.scheduledFor(b))
    }

    @Test
    fun deletingOneAlarmKeepsTheOthersArmed() = runBlocking {
        val a = daily(hour = 6)
        val b = daily(hour = 8)
        env.repository.delete(a)
        assertNull(env.scheduledFor(a))
        assertNotNull(env.scheduledFor(b))
        assertEquals(listOf(b.id), env.repository.getAll().map { it.id })
    }

    @Test
    fun rescheduleAllNeverArmsAnAlarmThatIsOff() = runBlocking {
        val off = daily(hour = 6, enabled = false)
        val on = daily(hour = 8)
        assertTrue(env.repository.rescheduleAll())
        assertNull(env.scheduledFor(off))
        assertNotNull(env.scheduledFor(on))
    }

    @Test
    fun rescheduleAllWithSparingLeavesAJustDueRingInFlightAlone() = runBlocking {
        val now = System.currentTimeMillis()
        val due = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault()).minusSeconds(30)
        val alarm = env.save(Alarm(hour = due.hour, minute = due.minute, label = "", days = everyDay))
        com.wakewakeup.session.AlarmScheduler(env.app).cancel(alarm)
        assertNull(env.scheduledFor(alarm))
        env.repository.rescheduleAll(sparingDue = true, now = now)
        assertNull("a ring that just came due must not be replaced by tomorrow's", env.scheduledFor(alarm))
        env.repository.rescheduleAll(sparingDue = false, now = now)
        assertNotNull(env.scheduledFor(alarm))
    }

    @Test
    fun armingTheNextRingForgetsASkipWhoseDayHasCome() = runBlocking {
        val today = LocalDate.now().toEpochDay()
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = everyDay))
        env.repository.armNextRing(alarm.copy(skipDate = today))
        assertNull(env.reload(alarm)!!.skipDate)
        assertEquals(nextTriggerMillis(alarm), env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun theRingAfterASkippedOneIsStillArmedWhenTheSkipIsSet() = runBlocking {
        val alarm = daily()
        env.repository.skipNext(alarm)
        val saved = env.reload(alarm)!!
        assertEquals(nextRingMillis(saved), env.scheduledFor(alarm)!!.triggerAtMs)
        assertTrue(env.scheduledFor(alarm)!!.triggerAtMs > nextTriggerMillis(saved))
    }
}
