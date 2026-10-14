package com.wakewakeup.session

import android.app.AlarmManager
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.data.Alarm
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
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AlarmReceiverTest {

    @get:Rule val env = AppEnv()

    private val weekdays = setOf(0, 1, 2, 3, 4)

    private fun startedServices() = shadowOf(env.app).allStartedServices

    private fun ringIntent(alarm: Alarm) =
        Intent(env.app, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarm.id)

    @Test
    fun anEnabledAlarmStartsTheRingingServiceAndSchedulesItsNextOccurrence() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        env.fire(env.scheduledFor(alarm)!!)

        val service = startedServices().single()
        assertEquals(AlarmRingService::class.java.name, service.component?.className)
        assertEquals(alarm.id, service.getLongExtra(EXTRA_ALARM_ID, -1))
        assertEquals(nextTriggerMillis(alarm), env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun aOneOffAlarmRingsAndIsNotScheduledAgain() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = emptySet()))
        env.fire(env.scheduledFor(alarm)!!)
        assertEquals(1, startedServices().size)
        assertNull(env.scheduledFor(alarm))
    }

    @Test
    fun aDisabledAlarmNeverRings() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        env.repository.setEnabled(alarm, false)
        env.deliverBroadcast(ringIntent(alarm)) // a stale wake that slipped through
        assertTrue(startedServices().isEmpty())
        assertFalse(env.reload(alarm)!!.enabled)
    }

    @Test
    fun aDeletedAlarmOrABrokenIntentIsIgnored() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        env.repository.delete(alarm)
        env.deliverBroadcast(ringIntent(alarm))
        env.deliverBroadcast(Intent(env.app, AlarmReceiver::class.java))
        assertTrue(startedServices().isEmpty())
    }

    private fun pause(alarm: Alarm, resumeDay: LocalDate) = runBlocking {
        env.repository.setEnabled(alarm, false)
        env.repository.scheduleResume(env.reload(alarm)!!)
        // Pin the resume to the day the test needs (the skipped occurrence).
        val paused = env.reload(alarm)!!.copy(resumeDate = resumeDay.toEpochDay())
        env.repository.save(paused)
        env.reload(alarm)!!
    }

    @Test
    fun theResumeWakeTurnsThePausedAlarmBackOnWithoutRinging() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        val paused = pause(alarm, LocalDate.now())
        env.deliverBroadcast(ringIntent(paused))

        assertTrue("must not ring on the skipped day", startedServices().isEmpty())
        val resumed = env.reload(alarm)!!
        assertTrue(resumed.enabled)
        assertNull(resumed.resumeDate)
        assertEquals(nextTriggerMillis(resumed), env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun aResumeWakeThatArrivesLateStillTurnsTheAlarmBackOn() {
        // The phone was off (or the wake was delayed past midnight) when the skipped occurrence
        // came around: the wake shows up on a later day. The alarm must not stay off forever.
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        val paused = pause(alarm, LocalDate.now().minusDays(1))
        env.deliverBroadcast(ringIntent(paused))

        assertTrue(startedServices().isEmpty())
        val resumed = env.reload(alarm)!!
        assertTrue("alarm stayed off after a late resume wake", resumed.enabled)
        assertNull(resumed.resumeDate)
        assertNotNull(env.scheduledFor(alarm))
    }

    @Test
    fun aResumeWakeBeforeTheSkippedDayDoesNothing() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        val paused = pause(alarm, LocalDate.now().plusDays(2))
        env.deliverBroadcast(ringIntent(paused))
        assertTrue(startedServices().isEmpty())
        assertFalse(env.reload(alarm)!!.enabled)
        assertEquals(paused.resumeDate, env.reload(alarm)!!.resumeDate)
    }

    @Test
    fun afterARebootEnabledAndPausedAlarmsAreScheduledAgain() {
        val enabled = env.save(Alarm(hour = 6, minute = 0, label = "on", days = weekdays))
        val disabled = env.save(Alarm(hour = 6, minute = 10, label = "off", days = weekdays, enabled = false))
        val paused = pause(env.save(Alarm(hour = 6, minute = 20, label = "paused", days = weekdays)), LocalDate.now().plusDays(1))
        env.app.getSystemService(AlarmManager::class.java).cancelAll()

        env.deliverBroadcast(Intent(Intent.ACTION_BOOT_COMPLETED))

        assertEquals(nextTriggerMillis(enabled), env.scheduledFor(enabled)!!.triggerAtMs)
        assertNotNull(env.scheduledFor(paused))
        assertNull(env.scheduledFor(disabled))
    }

    @Test
    fun aPhoneThatWasOffDuringTheSkippedTimeStillResumesTheAlarm() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = weekdays))
        pause(alarm, LocalDate.now().minusDays(1))
        env.app.getSystemService(AlarmManager::class.java).cancelAll()

        // Boot: the resume time already passed, so AlarmManager delivers that wake right away.
        env.deliverBroadcast(Intent(Intent.ACTION_BOOT_COMPLETED))
        env.fire(env.scheduledFor(alarm)!!)

        assertTrue(startedServices().isEmpty())
        assertTrue("alarm stayed off after the reboot", env.reload(alarm)!!.enabled)
    }
}
