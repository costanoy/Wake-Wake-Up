package com.wakewakeup

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.data.Alarm
import com.wakewakeup.session.AlarmReceiver
import com.wakewakeup.session.AlarmRingService
import com.wakewakeup.session.AlarmSessionState
import com.wakewakeup.session.BootReceiver
import com.wakewakeup.session.EXTRA_RESUMED_SESSION
import com.wakewakeup.session.RingActivity
import com.wakewakeup.session.RingWatchdog
import com.wakewakeup.session.nextRingMillis
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.testing.awaitUntil
import com.wakewakeup.ui.edit.EditAlarmViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * Bugs de logica (app, dados, manifesto) confirmados pela Fase 3 (auditoria-logica.md). Cada teste
 * descreve o comportamento CORRETO e esta ignorado ate a correcao.
 */
@RunWith(AndroidJUnit4::class)
class AppLogicBugTest {

    @get:Rule val env = AppEnv()

    private val everyDay = (0..6).toSet()

    @Test
    fun theRingComponentsAreDirectBootAware() {
        val pm = env.app.packageManager
        fun name(c: Class<*>) = android.content.ComponentName(env.app, c)
        assertTrue("AlarmReceiver", pm.getReceiverInfo(name(AlarmReceiver::class.java), 0).directBootAware)
        assertTrue("BootReceiver", pm.getReceiverInfo(name(BootReceiver::class.java), 0).directBootAware)
        assertTrue("AlarmRingService", pm.getServiceInfo(name(AlarmRingService::class.java), 0).directBootAware)
        assertTrue("RingActivity", pm.getActivityInfo(name(RingActivity::class.java), 0).directBootAware)
    }

    @Test
    fun theBootReceiverListensToLockedBootCompleted() {
        val receivers = env.app.packageManager.queryBroadcastReceivers(Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED), PackageManager.MATCH_DIRECT_BOOT_AWARE or PackageManager.MATCH_DIRECT_BOOT_UNAWARE)
        assertTrue(receivers.any { it.activityInfo.name == BootReceiver::class.java.name })
    }

    @Test
    fun openingTheAppBringsBackAnInterruptedRing() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = everyDay))
        AlarmSessionState.start(alarm, startedAtMillis = System.currentTimeMillis() - 60_000, waits = 0)
        RingWatchdog.save(env.app, AlarmSessionState.session.value!!)
        AlarmSessionState.clear() // the process died: memory is empty, the saved session is not
        shadowOf(env.app).clearStartedServices()
        ActivityScenario.launch(MainActivity::class.java).use {
            // The resume runs in the app scope, off the main thread.
            awaitUntil(message = "ring service started") { shadowOf(env.app).peekNextStartedService() != null }
            val started = shadowOf(env.app).peekNextStartedService()
            assertNotNull("no service started for the saved session", started)
            assertTrue(started.getBooleanExtra(EXTRA_RESUMED_SESSION, false))
        }
    }

    @Test
    fun editingTheTimeOfASkippingAlarmDropsTheSkip() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = everyDay))
        val skipped = runBlocking { env.repository.skipNext(alarm) }
        assertNotNull(skipped)
        val vm = EditAlarmViewModel(env.app)
        vm.startEdit(env.reload(alarm)!!)
        vm.setHour(21)
        var saved = false
        vm.save { saved = true }
        awaitUntil(message = "saved") { saved }
        assertNull("the skip survived a new time", env.reload(alarm)!!.skipDate)
    }

    @Test
    fun editingOnlyTheNameOfASkippingAlarmKeepsTheSkip() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = everyDay))
        val skipped = runBlocking { env.repository.skipNext(alarm) }
        val vm = EditAlarmViewModel(env.app)
        vm.startEdit(env.reload(alarm)!!)
        vm.setLabel("Trabalho")
        var saved = false
        vm.save { saved = true }
        awaitUntil(message = "saved") { saved }
        assertEquals(skipped, env.reload(alarm)!!.skipDate)
    }

    @Test
    fun undoingASkipJustBeforeTheRingIsAccepted() = runBlocking {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = everyDay))
        val at = nextRingMillis(alarm)
        assertTrue(env.repository.skipOccurrence(alarm.id, at, now = at - 600 * 60_000L))
        assertTrue(env.repository.undoSkipOccurrence(alarm.id, at, now = at - 30_000))
        assertNull(env.reload(alarm)!!.skipDate)
        assertEquals(at, env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun anAlarmSparedOnOpeningIsArmedByTheSecondPass() {
        val due = java.time.LocalDateTime.now().minusSeconds(30)
        val alarm = env.save(Alarm(hour = due.hour, minute = due.minute, label = "", days = everyDay))
        env.app.getSystemService(android.app.AlarmManager::class.java).cancelAll()
        ActivityScenario.launch(MainActivity::class.java).use {
            joinAppScope()
            assertNull("its ring may be in flight: spared on opening", env.scheduledFor(alarm))
            shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(130))
            joinAppScope()
            val armed = env.scheduledFor(alarm)
            assertNotNull("the second pass never armed it", armed)
            assertTrue("armed for the next occurrence", armed!!.triggerAtMs > System.currentTimeMillis() + 60 * 60_000L)
        }
    }

    private fun joinAppScope() = runBlocking {
        env.app.container.appScope.coroutineContext[kotlinx.coroutines.Job]?.children?.forEach { it.join() }
    }
}
