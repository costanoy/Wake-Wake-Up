package com.wakewakeup.data

import android.app.AlarmManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.session.AlarmReceiver
import com.wakewakeup.session.EXTRA_ALARM_ID
import com.wakewakeup.session.epochDayOf
import com.wakewakeup.session.isSkippingNext
import com.wakewakeup.session.isoDayIndexOf
import com.wakewakeup.session.nextRingMillis
import com.wakewakeup.session.nextTriggerEpochDay
import com.wakewakeup.session.nextTriggerMillis
import com.wakewakeup.testing.AppEnv
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
class AlarmRepositoryTest {

    @get:Rule val env = AppEnv()

    private val weekdays = setOf(0, 1, 2, 3, 4)
    private val everyDay = (0..6).toSet()
    private val hour = 3_600_000L

    private fun dao() = AppDatabase.get(env.app).alarmDao()

    @Test
    fun savingANewAlarmSchedulesItsNextOccurrenceAsAnAlarmClock() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "Trabalho", days = weekdays))
        assertTrue(alarm.id > 0)
        val scheduled = env.scheduledFor(alarm)
        assertNotNull("not scheduled", scheduled)
        assertEquals(nextTriggerMillis(alarm), scheduled!!.triggerAtMs)
        // setAlarmClock: exact, allowed in Doze, shows the alarm icon in the status bar.
        assertNotNull(scheduled.alarmClockInfo)
        val intent = env.broadcastIntentOf(scheduled)
        assertEquals(AlarmReceiver::class.java.name, intent.component?.className)
        assertEquals(alarm.id, intent.getLongExtra(EXTRA_ALARM_ID, -1))
    }

    @Test
    fun editingAnAlarmReplacesItsSchedule() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        val edited = env.save(alarm.copy(hour = 8, minute = 45))
        assertEquals(alarm.id, edited.id)
        assertEquals(1, env.scheduledAlarms().size)
        assertEquals(nextTriggerMillis(edited), env.scheduledFor(edited)!!.triggerAtMs)
    }

    @Test
    fun eachAlarmGetsItsOwnSchedule() {
        val a = env.save(Alarm(hour = 6, minute = 0, label = "A", days = weekdays))
        val b = env.save(Alarm(hour = 6, minute = 5, label = "B", days = weekdays))
        assertEquals(2, env.scheduledAlarms().size)
        assertEquals(nextTriggerMillis(a), env.scheduledFor(a)!!.triggerAtMs)
        assertEquals(nextTriggerMillis(b), env.scheduledFor(b)!!.triggerAtMs)
    }

    @Test
    fun turningOffCancelsAndTurningOnReschedules() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        env.repository.setEnabled(alarm, false)
        assertFalse(env.reload(alarm)!!.enabled)
        assertNull(env.scheduledFor(alarm))

        env.repository.setEnabled(env.reload(alarm)!!, true)
        assertTrue(env.reload(alarm)!!.enabled)
        assertEquals(nextTriggerMillis(alarm), env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun deletingCancels() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        env.repository.delete(alarm)
        assertNull(env.reload(alarm))
        assertNull(env.scheduledFor(alarm))
    }

    @Test
    fun skippingTheNextDayKeepsTheAlarmOnAndArmsTheRingAfterIt() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        // The list's switch, then PULAR in the drawer that slides out.
        env.repository.setEnabled(alarm, false)
        assertEquals(nextTriggerEpochDay(alarm), env.repository.skipNext(env.reload(alarm)!!))

        val skipping = env.reload(alarm)!!
        assertTrue("a skipping alarm stays on", skipping.enabled)
        assertEquals(nextTriggerEpochDay(alarm), skipping.skipDate)
        assertTrue(isSkippingNext(skipping))
        // The phone's own "next alarm" (status bar, lock screen, watch) is the real next ring.
        val ring = env.scheduledFor(alarm)!!
        assertEquals(nextRingMillis(skipping), ring.triggerAtMs)
        assertEquals(nextRingMillis(skipping), ring.alarmClockInfo!!.triggerTime)
        assertNotEquals(nextTriggerMillis(alarm), ring.triggerAtMs)
    }

    @Test
    fun undoingTheSkipRingsOnTheSkippedDayAgain() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        env.repository.skipNext(alarm)
        env.repository.clearSkip(env.reload(alarm)!!)
        val on = env.reload(alarm)!!
        assertTrue(on.enabled)
        assertNull(on.skipDate)
        assertEquals(nextTriggerMillis(alarm), env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun switchingByHandDropsASkip() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        env.repository.skipNext(alarm)
        env.repository.setEnabled(env.reload(alarm)!!, false)
        assertNull(env.reload(alarm)!!.skipDate)
        assertNull(env.scheduledFor(alarm))

        env.repository.setEnabled(env.reload(alarm)!!, true)
        assertNull(env.reload(alarm)!!.skipDate)
        assertEquals(nextTriggerMillis(alarm), env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun skippingAOneTimeAlarmSwitchesItOff() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = emptySet()))
        assertNull(env.repository.skipNext(alarm))
        assertFalse(env.reload(alarm)!!.enabled)
        assertNull(env.scheduledFor(alarm))
    }

    @Test
    fun savingKeepsASkipOnlyWhileItStillFits() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = everyDay))
        env.repository.skipNext(alarm)
        val skipping = env.reload(alarm)!!

        // Editing something else keeps it.
        env.save(skipping.copy(label = "Trabalho"))
        assertEquals(skipping.skipDate, env.reload(alarm)!!.skipDate)

        // Taking that weekday out of the alarm drops it: a stale date must never come back later.
        val skipWeekday = isoDayIndexOf(LocalDate.ofEpochDay(skipping.skipDate!!))
        env.save(env.reload(alarm)!!.copy(days = everyDay - skipWeekday))
        assertNull(env.reload(alarm)!!.skipDate)

        // A day that already passed, or any skip on a one-time alarm, is never stored.
        env.save(env.reload(alarm)!!.copy(days = everyDay, skipDate = LocalDate.now().minusDays(1).toEpochDay()))
        assertNull(env.reload(alarm)!!.skipDate)
        env.save(env.reload(alarm)!!.copy(days = emptySet(), skipDate = LocalDate.now().plusDays(1).toEpochDay()))
        assertNull(env.reload(alarm)!!.skipDate)
    }

    @Test
    fun skippingOneRingOnlyWorksWhileThatRingIsNextAndAhead() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = everyDay))
        val at = nextTriggerMillis(alarm)
        val now = at - hour

        // Not the next ring (the one a day later), or less than a minute before it: refused.
        assertFalse(env.repository.skipOccurrence(alarm.id, at + 24 * hour, now))
        assertFalse(env.repository.skipOccurrence(alarm.id, at, at - 30_000))
        assertNull(env.reload(alarm)!!.skipDate)
        assertEquals(at, env.scheduledFor(alarm)!!.triggerAtMs)

        assertTrue(env.repository.skipOccurrence(alarm.id, at, now))
        val skipping = env.reload(alarm)!!
        assertTrue(skipping.enabled)
        assertEquals(epochDayOf(at), skipping.skipDate)
        assertEquals(nextTriggerMillis(alarm, LocalDateTime.ofInstant(Instant.ofEpochMilli(at), ZoneId.systemDefault())), env.scheduledFor(alarm)!!.triggerAtMs)

        // Undo puts that ring back, but only before it.
        assertFalse(env.repository.undoSkipOccurrence(alarm.id, at, at + 1_000))
        assertTrue(env.repository.undoSkipOccurrence(alarm.id, at, now))
        assertNull(env.reload(alarm)!!.skipDate)
        assertEquals(at, env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun skippingOneRingOfAOneTimeAlarmSwitchesItOffAndUndoBringsItBack() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = emptySet()))
        val at = nextTriggerMillis(alarm)
        val now = at - hour

        assertTrue(env.repository.skipOccurrence(alarm.id, at, now))
        assertFalse(env.reload(alarm)!!.enabled)
        assertNull(env.scheduledFor(alarm))

        assertTrue(env.repository.undoSkipOccurrence(alarm.id, at, now))
        assertTrue(env.reload(alarm)!!.enabled)
        assertEquals(at, env.scheduledFor(alarm)!!.triggerAtMs)

        // Too late to undo once the time came: it must not come back on for tomorrow.
        assertTrue(env.repository.skipOccurrence(alarm.id, at, now))
        assertFalse(env.repository.undoSkipOccurrence(alarm.id, at, at + 1_000))
        assertFalse(env.reload(alarm)!!.enabled)
        assertNull(env.scheduledFor(alarm))
    }

    @Test
    fun rescheduleAllArmsEveryAlarmThatIsOnAtItsRealNextRing() = runBlocking {
        val enabled = env.save(Alarm(hour = 6, minute = 0, label = "on", days = weekdays))
        val disabled = env.save(Alarm(hour = 6, minute = 10, label = "off", days = weekdays, enabled = false))
        val skipping = env.save(Alarm(hour = 6, minute = 20, label = "skipping", days = weekdays))
        env.repository.skipNext(skipping)

        // A reboot, a force stop or a restored backup: AlarmManager holds nothing of the app's.
        env.app.getSystemService(AlarmManager::class.java).cancelAll()
        assertTrue(env.scheduledAlarms().isEmpty())

        assertTrue(env.repository.rescheduleAll())
        assertEquals(nextTriggerMillis(enabled), env.scheduledFor(enabled)!!.triggerAtMs)
        assertEquals(nextRingMillis(env.reload(skipping)!!), env.scheduledFor(skipping)!!.triggerAtMs)
        assertNull(env.scheduledFor(disabled))
    }

    @Test
    fun rescheduleAllForgetsSkipsWhoseDayIsOver() = runBlocking {
        val alarm = env.save(Alarm(hour = 6, minute = 0, label = "", days = everyDay))
        // Straight to the database: save() would never store a past day.
        dao().setSkipDate(alarm.id, LocalDate.now().minusDays(1).toEpochDay())
        env.repository.rescheduleAll()
        assertNull(env.reload(alarm)!!.skipDate)
        assertEquals(nextTriggerMillis(alarm), env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun theStartupRescheduleLeavesARingThatMayStillBeOnItsWayAlone() = runBlocking {
        val now = System.currentTimeMillis()
        val dueAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(now - 30_000), ZoneId.systemDefault())
        val due = env.save(Alarm(hour = dueAt.hour, minute = dueAt.minute, label = "due", days = everyDay))
        val later = env.save(Alarm(hour = (dueAt.hour + 3) % 24, minute = dueAt.minute, label = "later", days = everyDay))
        env.app.getSystemService(AlarmManager::class.java).cancelAll()

        // Re-arming an alarm whose time came 30 s ago would replace a ring that may not have been
        // delivered yet with tomorrow's.
        env.repository.rescheduleAll(sparingDue = true, now = now)
        assertNull(env.scheduledFor(due))
        assertNotNull(env.scheduledFor(later))

        env.repository.rescheduleAll()
        assertNotNull(env.scheduledFor(due))
    }
}
