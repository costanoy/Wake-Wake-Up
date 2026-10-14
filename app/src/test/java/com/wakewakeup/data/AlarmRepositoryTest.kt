package com.wakewakeup.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.session.AlarmReceiver
import com.wakewakeup.session.EXTRA_ALARM_ID
import com.wakewakeup.session.nextTriggerEpochDay
import com.wakewakeup.session.nextTriggerMillis
import com.wakewakeup.testing.AppEnv
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class AlarmRepositoryTest {

    @get:Rule val env = AppEnv()

    private val weekdays = setOf(0, 1, 2, 3, 4)

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
    fun skippingOneDayKeepsItOffAndArmsTheResumeAtTheSkippedTime() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        env.repository.setEnabled(alarm, false)
        val off = env.reload(alarm)!!
        env.repository.scheduleResume(off)

        val paused = env.reload(alarm)!!
        assertFalse(paused.enabled)
        val skippedDay = nextTriggerEpochDay(alarm)
        assertEquals(skippedDay, paused.resumeDate)
        val resumeAt = LocalDate.ofEpochDay(skippedDay).atTime(7, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(resumeAt, env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun cancellingTheSkipLeavesItSimplyOff() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        env.repository.setEnabled(alarm, false)
        env.repository.scheduleResume(env.reload(alarm)!!)
        env.repository.cancelResume(env.reload(alarm)!!)
        val off = env.reload(alarm)!!
        assertFalse(off.enabled)
        assertNull(off.resumeDate)
        assertNull(env.scheduledFor(alarm))
    }

    @Test
    fun turningOnByHandDropsAPendingSkip() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        env.repository.setEnabled(alarm, false)
        env.repository.scheduleResume(env.reload(alarm)!!)
        env.repository.setEnabled(env.reload(alarm)!!, true)
        val on = env.reload(alarm)!!
        assertTrue(on.enabled)
        assertNull(on.resumeDate)
        assertEquals(nextTriggerMillis(alarm), env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun rescheduleAllCoversEnabledAndPausedAlarmsOnly() = runBlocking {
        val enabled = env.save(Alarm(hour = 6, minute = 0, label = "on", days = weekdays))
        val disabled = env.save(Alarm(hour = 6, minute = 10, label = "off", days = weekdays, enabled = false))
        val paused = env.save(Alarm(hour = 6, minute = 20, label = "paused", days = weekdays))
        env.repository.setEnabled(paused, false)
        env.repository.scheduleResume(env.reload(paused)!!)

        // A reboot wipes everything the app had in AlarmManager.
        env.app.getSystemService(android.app.AlarmManager::class.java).cancelAll()
        assertTrue(env.scheduledAlarms().isEmpty())

        env.repository.rescheduleAll()
        assertNotNull(env.scheduledFor(enabled))
        assertNotNull(env.scheduledFor(paused))
        assertNull(env.scheduledFor(disabled))
    }
}
