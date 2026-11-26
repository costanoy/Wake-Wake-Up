package com.wakewakeup.session

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.Difficulty
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.testing.awaitUntil
import com.wakewakeup.testing.idleMain
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.ShadowMediaPlayer

/**
 * Bugs reais achados pela auditoria. Cada teste descreve o comportamento CORRETO e está ignorado
 * até a correção (a Fase 3 tira o @Ignore ao corrigir).
 */
@RunWith(AndroidJUnit4::class)
class SessionBugTest {

    @get:Rule val env = AppEnv()

    private var controller: ServiceController<AlarmRingService>? = null
    private var startId = 0
    private val players = mutableListOf<ShadowMediaPlayer>()

    @After
    fun stopService() {
        controller?.destroy()
    }

    private fun send(action: String) {
        AlarmRingService.sendAction(env.app, action)
        controller!!.get().onStartCommand(shadowOf(env.app).nextStartedService, 0, ++startId)
        idleMain()
    }

    @Ignore("BUG-1: Espere recebido durante a missão conta uma espera a mais e deixa o Espere preso (a tela da missão segue, mas o histórico sai com espera falsa)")
    @Test
    fun anEspereTapArrivingDuringTheMissionIsIgnored() {
        ShadowMediaPlayer.setCreateListener { _, shadow -> players += shadow }
        val alarm = env.save(
            Alarm(
                hour = 6, minute = 30, label = "", days = setOf(0), difficulty = Difficulty.EASY,
                soundUri = "android.resource://${env.app.packageName}/raw/campainha_mecanica",
            ),
        )
        val intent = Intent(env.app, AlarmRingService::class.java).putExtra(EXTRA_ALARM_ID, alarm.id)
        controller = Robolectric.buildService(AlarmRingService::class.java, intent).create()
        controller!!.get().onStartCommand(intent, 0, ++startId)
        awaitUntil(message = "ringing") { AlarmSessionState.session.value != null && players.isNotEmpty() }

        send(AlarmRingService.ACTION_START_MISSION)
        send(AlarmRingService.ACTION_HOLD_ON) // a second tap that was already on its way
        val session = AlarmSessionState.session.value!!
        assertEquals(SessionScreen.MISSION, session.screen)
        assertEquals(0, session.waits)
        assertNull(session.holdUntilMillis)
    }

    @Ignore("BUG-2: mudar a hora do celular à mão (ACTION_TIME_CHANGED) não reagenda os alarmes, que ficam no instante antigo")
    @Test
    fun changingTheClockByHandRearmsTheAlarms() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = (0..6).toSet()))
        env.alarmManager.scheduledAlarms.forEach { /* snapshot only */ }
        com.wakewakeup.session.AlarmScheduler(env.app).cancel(alarm)
        assertNull(env.scheduledFor(alarm))
        env.deliverBroadcast(Intent(Intent.ACTION_TIME_CHANGED).setClass(env.app, BootReceiver::class.java))
        awaitUntil(message = "alarm armed again") { env.scheduledFor(alarm) != null }
        assertNotNull(env.scheduledFor(alarm))
        runBlocking { }
    }
}
