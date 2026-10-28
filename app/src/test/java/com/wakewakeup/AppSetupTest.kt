package com.wakewakeup

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.data.Alarm
import com.wakewakeup.session.AlarmReceiver
import com.wakewakeup.session.AlarmRingService
import com.wakewakeup.session.BootReceiver
import com.wakewakeup.session.PRE_ALARM_CHANNEL_ID
import com.wakewakeup.session.PreAlarmReceiver
import com.wakewakeup.session.RingActivity
import com.wakewakeup.session.nextTriggerMillis
import com.wakewakeup.testing.AppEnv
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class AppSetupTest {

    @get:Rule val env = AppEnv()

    private val originalZone: TimeZone = TimeZone.getDefault()

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalZone)
    }

    private val pm get() = env.app.packageManager

    @Test
    fun theAlarmNotificationChannelRingsSilently() {
        val channel = env.app.getSystemService(NotificationManager::class.java).getNotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        // The service plays the sound itself (with its volume ramp), never the notification.
        assertNull(channel.sound)
    }

    @Test
    fun thePreAlarmChannelPopsUpWithoutASound() {
        val channel = env.app.getSystemService(NotificationManager::class.java).getNotificationChannel(PRE_ALARM_CHANNEL_ID)
        assertNotNull(channel)
        // Seen by someone already looking at the screen, never heard by someone still asleep.
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertNull(channel.sound)
        assertFalse(channel.shouldVibrate())
    }

    @Test
    fun everyPermissionAnAlarmNeedsIsDeclared() {
        val requested = pm.getPackageInfo(env.app.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions!!.toSet()
        // This is Android 15: SCHEDULE_EXACT_ALARM only applies up to 12L, USE_EXACT_ALARM after.
        for (permission in listOf(
            Manifest.permission.USE_EXACT_ALARM, Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.WAKE_LOCK, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED, Manifest.permission.USE_FULL_SCREEN_INTENT,
            Manifest.permission.VIBRATE, Manifest.permission.INTERNET,
        )) {
            assertTrue("missing $permission", permission in requested)
        }
        // Google Play policy: nothing an alarm clock doesn't need.
        for (permission in listOf(
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK,
            Manifest.permission.DISABLE_KEYGUARD, Manifest.permission.SCHEDULE_EXACT_ALARM,
        )) {
            assertFalse("$permission is not needed on this Android version", permission in requested)
        }
    }

    @Test
    fun componentsAreWiredUp() {
        val pkg = env.app.packageName
        val alarmReceiver = pm.getReceiverInfo(ComponentName(pkg, AlarmReceiver::class.java.name), 0)
        assertFalse("other apps must not be able to fire alarms", alarmReceiver.exported)

        val service = pm.getServiceInfo(ComponentName(pkg, AlarmRingService::class.java.name), 0)
        assertFalse(service.exported)
        // The type Android sets aside for alarm-clock apps holding the exact-alarm permission.
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED, service.foregroundServiceType)

        val ring = pm.getActivityInfo(ComponentName(pkg, RingActivity::class.java.name), 0)
        assertFalse(ring.exported)

        val preAlarm = pm.getReceiverInfo(ComponentName(pkg, PreAlarmReceiver::class.java.name), 0)
        assertFalse(preAlarm.exported)

        for (action in listOf(
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )) {
            val receivers = pm.queryBroadcastReceivers(Intent(action).setPackage(pkg), 0)
            assertTrue("nothing reschedules on $action", receivers.any { it.activityInfo.name == BootReceiver::class.java.name })
        }
    }

    @Test
    fun aNewTimeZoneMovesTheAlarmsToTheNewLocalTime() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "Viagem", days = (0..6).toSet()))
        val inBrazil = env.scheduledFor(alarm)!!.triggerAtMs

        // Landed in Lisbon: 07:00 there is a different moment than 07:00 in São Paulo.
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Lisbon"))
        env.deliverBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))

        val inLisbon = env.scheduledFor(alarm)!!.triggerAtMs
        assertNotEquals(inBrazil, inLisbon)
        assertEquals(nextTriggerMillis(alarm), inLisbon)
        val local = java.time.Instant.ofEpochMilli(inLisbon).atZone(ZoneId.of("Europe/Lisbon")).toLocalTime()
        assertEquals(java.time.LocalTime.of(7, 0), local)
        assertTrue(java.time.Instant.ofEpochMilli(inLisbon).atZone(ZoneId.of("Europe/Lisbon")).toLocalDate() >= LocalDate.now(ZoneId.of("Europe/Lisbon")))
    }

    @Test
    fun anAppUpdateReschedulesEverything() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = setOf(1)))
        env.app.getSystemService(android.app.AlarmManager::class.java).cancelAll()
        env.deliverBroadcast(Intent(Intent.ACTION_MY_PACKAGE_REPLACED).setPackage(env.app.packageName))
        assertEquals(nextTriggerMillis(alarm), env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun otherBroadcastsDontTouchTheAlarms() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = setOf(1)))
        env.app.getSystemService(android.app.AlarmManager::class.java).cancelAll()
        BootReceiver().onReceive(env.app, Intent(Intent.ACTION_SCREEN_ON))
        assertNull(env.scheduledFor(alarm))
    }
}
