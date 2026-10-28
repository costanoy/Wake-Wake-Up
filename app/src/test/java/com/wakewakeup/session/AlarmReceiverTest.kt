package com.wakewakeup.session

import android.app.AlarmManager
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.AppDatabase
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
    private val everyDay = (0..6).toSet()

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

    private fun pinSkip(alarm: Alarm, day: LocalDate) = runBlocking {
        // Straight to the database: the day a test needs isn't always the alarm's next one.
        AppDatabase.get(env.app).alarmDao().setSkipDate(alarm.id, day.toEpochDay())
        env.reload(alarm)!!
    }

    @Test
    fun aRingOnTheSkippedDayStaysSilentAndArmsTheNextOne() {
        // A ring armed before the skip was set (or moved by a time zone change) still fires.
        val daily = env.save(Alarm(hour = 7, minute = 0, label = "", days = everyDay))
        pinSkip(daily, LocalDate.now())
        env.deliverBroadcast(ringIntent(daily))

        assertTrue("must not ring on the skipped day", startedServices().isEmpty())
        val after = env.reload(daily)!!
        assertTrue(after.enabled)
        assertNull("the skip is spent", after.skipDate)
        assertEquals(nextTriggerMillis(after), env.scheduledFor(daily)!!.triggerAtMs)
    }

    @Test
    fun aRingOnAnotherDayKeepsALaterSkip() {
        val daily = env.save(Alarm(hour = 7, minute = 0, label = "", days = everyDay))
        val inTwoDays = LocalDate.now().plusDays(2)
        pinSkip(daily, inTwoDays)
        env.deliverBroadcast(ringIntent(daily))

        assertEquals(1, startedServices().size)
        assertEquals(inTwoDays.toEpochDay(), env.reload(daily)!!.skipDate)
    }

    @Test
    fun aRingAfterTheSkippedDayClearsTheSkip() {
        val daily = env.save(Alarm(hour = 7, minute = 0, label = "", days = everyDay))
        pinSkip(daily, LocalDate.now().minusDays(1))
        env.deliverBroadcast(ringIntent(daily))

        assertEquals(1, startedServices().size)
        assertNull(env.reload(daily)!!.skipDate)
    }

    @Test
    fun afterARebootEveryAlarmThatIsOnIsArmedAtItsRealNextRing() {
        val enabled = env.save(Alarm(hour = 6, minute = 0, label = "on", days = weekdays))
        val disabled = env.save(Alarm(hour = 6, minute = 10, label = "off", days = weekdays, enabled = false))
        val skipping = env.save(Alarm(hour = 6, minute = 20, label = "skipping", days = weekdays))
        runBlocking { env.repository.skipNext(skipping) }
        env.app.getSystemService(AlarmManager::class.java).cancelAll()

        env.deliverBroadcast(Intent(Intent.ACTION_BOOT_COMPLETED))

        assertEquals(nextTriggerMillis(enabled), env.scheduledFor(enabled)!!.triggerAtMs)
        assertEquals(nextRingMillis(env.reload(skipping)!!), env.scheduledFor(skipping)!!.triggerAtMs)
        assertNull(env.scheduledFor(disabled))
    }
}
