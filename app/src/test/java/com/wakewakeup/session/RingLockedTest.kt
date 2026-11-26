package com.wakewakeup.session

import android.content.Intent
import android.os.UserManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.data.Alarm
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.testing.awaitUntil
import com.wakewakeup.testing.idleMain
import com.wakewakeup.testing.resetDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.ShadowMediaPlayer

/**
 * TEC-01 / LOG-01: a phone that restarted and was not unlocked yet has its database shut. The
 * alarm still has to ring (a minimal alarm with the app's bell) and the ritual has to end cleanly.
 */
@RunWith(AndroidJUnit4::class)
class RingLockedTest {

    @get:Rule val env = AppEnv()

    private var controller: ServiceController<AlarmRingService>? = null
    private var startId = 0
    private val players = mutableListOf<ShadowMediaPlayer>()

    @Before
    fun lockTheUser() {
        ShadowMediaPlayer.setCreateListener { _, shadow -> players += shadow }
        shadowOf(env.app.getSystemService(UserManager::class.java)).setUserUnlocked(false)
    }

    @After
    fun stopService() {
        controller?.destroy()
        shadowOf(env.app.getSystemService(UserManager::class.java)).setUserUnlocked(true)
    }

    private fun playing() = players.lastOrNull { it.state == ShadowMediaPlayer.State.STARTED }

    private fun send(action: String, key: String? = null) {
        AlarmRingService.sendAction(env.app, action, key)
        controller!!.get().onStartCommand(shadowOf(env.app).nextStartedService, 0, ++startId)
        idleMain()
    }

    @Test
    fun aLockedPhoneStillRingsAndTheRitualEndsCleanly() {
        val alarm = env.save(Alarm(hour = 6, minute = 30, label = "Trabalho", days = (0..6).toSet()))
        resetDatabase() // the database is out of reach, as before the first unlock

        env.deliverBroadcast(Intent(env.app, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarm.id))
        val started = shadowOf(env.app).nextStartedService
        assertNotNull("the ringing service is started even though the alarm can't be read", started)
        assertEquals(alarm.id, started.getLongExtra(EXTRA_ALARM_ID, -1))

        controller = Robolectric.buildService(AlarmRingService::class.java, started).create()
        controller!!.get().onStartCommand(started, 0, ++startId)
        awaitUntil(message = "ringing while locked") { AlarmSessionState.session.value != null && playing() != null }
        assertNotNull(RingWatchdog.saved(env.app))

        send(AlarmRingService.ACTION_START_MISSION)
        repeat(AlarmSessionState.session.value!!.mission!!.count) {
            AlarmSessionState.session.value!!.mission!!.answer.forEach { send(AlarmRingService.ACTION_MISSION_KEY, it.toString()) }
            send(AlarmRingService.ACTION_MISSION_CONFIRM)
        }
        assertEquals(SessionScreen.GOOD_MORNING, AlarmSessionState.session.value!!.screen)
        assertNull(RingWatchdog.saved(env.app))
    }
}
