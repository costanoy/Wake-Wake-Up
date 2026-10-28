package com.wakewakeup.session

import android.Manifest
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.MainActivity
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.testing.AppEnv
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class PreAlarmCheckTest {

    @get:Rule val env = AppEnv()

    private val check get() = env.app.container.preAlarm
    private val notifications get() = shadowOf(env.app.getSystemService(NotificationManager::class.java))
    private val everyDay = (0..6).toSet()
    private val minute = 60_000L

    @Before
    fun phoneInUse() {
        shadowOf(env.app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        usingPhone(screenOn = true, unlocked = true)
        check.unlockWaitMs = 0
    }

    private fun usingPhone(screenOn: Boolean, unlocked: Boolean) {
        shadowOf(env.app.getSystemService(PowerManager::class.java)).setIsInteractive(screenOn)
        shadowOf(env.app.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(!unlocked)
    }

    /** A morning alarm, and a "now" 40 minutes before its next ring, whatever the real time is. */
    private fun morningAlarm(hour: Int = 6, minuteOfHour: Int = 30, label: String = "Trabalho"): Pair<Alarm, Long> {
        val alarm = env.save(Alarm(hour = hour, minute = minuteOfHour, label = label, days = everyDay))
        return alarm to nextRingMillis(alarm)
    }

    private fun offer(): Notification? = notifications.getNotification(NotificationIds.PRE_ALARM)

    private fun checkAt(now: Long) = runBlocking { check.check(now) }

    @Test
    fun aPhoneInUseGetsOneSilentOffer() {
        val (alarm, at) = morningAlarm()
        checkAt(at - 40 * minute)

        val notification = offer()
        assertNotNull("no offer while the phone was in use", notification)
        assertEquals(PRE_ALARM_CHANNEL_ID, notification!!.channelId)
        assertEquals(str(R.string.pre_alarm_title_one), shadowOf(notification).contentTitle.toString())
        assertEquals(str(R.string.pre_alarm_text_one, "Trabalho", "06:30"), shadowOf(notification).contentText.toString())
        // Never on a lock screen, never on a watch.
        assertEquals(NotificationCompat.VISIBILITY_SECRET, notification.visibility)
        assertTrue(notification.flags and Notification.FLAG_LOCAL_ONLY != 0)

        // Its one action opens the confirm screen in the app: no broadcast can switch anything off.
        assertEquals(1, notification.actions.size)
        val confirm = shadowOf(notification.contentIntent)
        assertTrue(confirm.isActivityIntent)
        val intent = confirm.savedIntent
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals(ACTION_CONFIRM_SKIP, intent.action)
        assertArrayEquals(longArrayOf(alarm.id), intent.getLongArrayExtra(EXTRA_SKIP_IDS))
        assertArrayEquals(longArrayOf(at), intent.getLongArrayExtra(EXTRA_SKIP_ATS))
        assertTrue("the alarm itself is untouched until confirmed", env.reload(alarm)!!.enabled)
    }

    @Test
    fun theSameRingIsNeverAskedAboutTwice() {
        val (_, at) = morningAlarm()
        checkAt(at - 40 * minute)
        env.app.getSystemService(NotificationManager::class.java).cancelAll()
        checkAt(at - 30 * minute)
        assertNull(offer())
    }

    @Test
    fun backupAlarmsShareOneOffer() {
        val (first, at) = morningAlarm(6, 0, "Um")
        val second = env.save(Alarm(hour = 6, minute = 5, label = "Dois", days = everyDay))
        checkAt(at - 30 * minute)

        val notification = offer()!!
        assertEquals(str(R.string.pre_alarm_title_many), shadowOf(notification).contentTitle.toString())
        assertEquals(str(R.string.pre_alarm_text_many, "06:00 · 06:05"), shadowOf(notification).contentText.toString())
        val intent = shadowOf(notification.contentIntent).savedIntent
        assertArrayEquals(longArrayOf(first.id, second.id), intent.getLongArrayExtra(EXTRA_SKIP_IDS))
    }

    @Test
    fun withTheScreenOffItWaitsWithoutWakingThePhone() {
        val (_, at) = morningAlarm()
        usingPhone(screenOn = false, unlocked = false)
        val now = at - 40 * minute
        checkAt(now)

        assertNull(offer())
        val next = env.scheduledWithAction(ACTION_PRE_ALARM_CHECK)!!
        assertEquals("a non-waking alarm", AlarmManager.RTC, next.type)
        assertEquals(now + minute, next.triggerAtMs)
    }

    @Test
    fun aLockedScreenIsNotInUse() {
        val (_, at) = morningAlarm()
        usingPhone(screenOn = true, unlocked = false)
        checkAt(at - 40 * minute)
        assertNull(offer())
        assertNotNull(env.scheduledWithAction(ACTION_PRE_ALARM_CHECK))

        // Unlocked a minute later: now it asks.
        usingPhone(screenOn = true, unlocked = true)
        checkAt(at - 39 * minute)
        assertNotNull(offer())
    }

    @Test
    fun anEveningAlarmIsNeverQuestioned() {
        val evening = env.save(Alarm(hour = 22, minute = 0, label = "Remédio", days = everyDay))
        checkAt(nextRingMillis(evening) - 40 * minute)
        assertNull(offer())
    }

    @Test
    fun nothingIsAskedWhileAnAlarmIsRinging() {
        val (alarm, at) = morningAlarm()
        AlarmSessionState.start(alarm)
        checkAt(at - 40 * minute)
        assertNull(offer())
    }

    @Test
    fun theFirstCheckIsArmedAnHourBeforeAndNeverWakesThePhone() {
        val (_, at) = morningAlarm()
        runBlocking { check.reschedule(now = at - 3 * 60 * minute) }
        val first = env.scheduledWithAction(ACTION_PRE_ALARM_CHECK)!!
        assertEquals(AlarmManager.RTC, first.type)
        assertEquals(at - 60 * minute, first.triggerAtMs)
    }

    @Test
    fun withoutTheNotificationPermissionThereAreNoChecks() {
        shadowOf(env.app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val (_, at) = morningAlarm()
        runBlocking { check.reschedule(now = at - 3 * 60 * minute) }
        assertNull(env.scheduledWithAction(ACTION_PRE_ALARM_CHECK))
        checkAt(at - 40 * minute)
        assertNull(offer())
    }

    @Test
    fun anOfferWhoseAlarmChangedIsWithdrawn() {
        val (alarm, at) = morningAlarm()
        checkAt(at - 40 * minute)
        assertNotNull(offer())
        // Moved to 06:45 in the list: the offer about 06:30 no longer holds.
        env.save(env.reload(alarm)!!.copy(minute = 45))
        assertNull(offer())
    }

    @Test
    fun theReceiptsUndoPutsTheRingBack() = runBlocking {
        // Any alarm a couple of hours ahead (undo doesn't care about the morning rule).
        val time = LocalTime.now().plusHours(2)
        val alarm = env.save(Alarm(hour = time.hour, minute = time.minute, label = "Trabalho", days = everyDay))
        val at = nextRingMillis(alarm)
        assertTrue(env.repository.skipOccurrence(alarm.id, at))
        val occurrence = Occurrence(alarm.id, at, alarm.label, alarm.hour, alarm.minute)
        check.postReceipt(listOf(occurrence))
        val receipt = notifications.getNotification(NotificationIds.SKIP_RECEIPT)!!
        val undo = shadowOf(receipt.actions.single().actionIntent)
        assertTrue("undo works from the shade, without opening the app", undo.isBroadcastIntent)

        env.deliverBroadcast(undo.savedIntent)
        assertNull(env.reload(alarm)!!.skipDate)
        assertEquals(at, env.scheduledFor(alarm)!!.triggerAtMs)
        assertNull(notifications.getNotification(NotificationIds.SKIP_RECEIPT))
    }


    private fun str(id: Int, vararg args: Any) = env.app.getString(id, *args)
}
