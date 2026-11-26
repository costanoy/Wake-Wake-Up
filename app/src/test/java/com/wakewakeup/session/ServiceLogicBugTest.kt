package com.wakewakeup.session

import android.content.Intent
import android.media.AudioManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.Difficulty
import com.wakewakeup.data.TaskType
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.testing.advance
import com.wakewakeup.testing.awaitUntil
import com.wakewakeup.testing.idleMain
import com.wakewakeup.testing.resetDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.ShadowMediaPlayer
import java.time.Duration

/**
 * Bugs de logica do servico de toque confirmados pela Fase 3 (auditoria-logica.md). Cada teste
 * descreve o comportamento CORRETO e esta ignorado ate a correcao.
 */
@RunWith(AndroidJUnit4::class)
class ServiceLogicBugTest {

    @get:Rule val env = AppEnv()

    private var controller: ServiceController<AlarmRingService>? = null
    private var startId = 0
    private val players = mutableListOf<ShadowMediaPlayer>()
    private val bellUri get() = "android.resource://${env.app.packageName}/raw/campainha_mecanica"

    @Before
    fun recordPlayers() {
        ShadowMediaPlayer.setCreateListener { _, shadow -> players += shadow }
    }

    @After
    fun stopService() {
        controller?.destroy()
    }

    private fun playing() = players.lastOrNull { it.state == ShadowMediaPlayer.State.STARTED }

    private fun start(alarm: Alarm) {
        val intent = Intent(env.app, AlarmRingService::class.java).putExtra(EXTRA_ALARM_ID, alarm.id)
        val c = controller ?: Robolectric.buildService(AlarmRingService::class.java, intent).create().also { controller = it }
        c.get().onStartCommand(intent, 0, ++startId)
    }

    private fun ring(alarm: Alarm) {
        start(alarm)
        awaitUntil(message = "alarm ${alarm.id} ringing") { AlarmSessionState.session.value?.alarmId == alarm.id && playing() != null }
    }

    private fun send(action: String, key: String? = null) {
        AlarmRingService.sendAction(env.app, action, key)
        controller!!.get().onStartCommand(shadowOf(env.app).nextStartedService, 0, ++startId)
        idleMain()
    }

    private fun alarm(
        label: String = "A", hour: Int = 6, minute: Int = 30, days: Set<Int> = setOf(0, 1, 2, 3, 4),
        taskType: TaskType = TaskType.MATH, taskCount: Int = 1, difficulty: Difficulty = Difficulty.EASY,
    ) = env.save(
        Alarm(hour = hour, minute = minute, label = label, days = days, soundUri = bellUri, taskType = taskType, taskCount = taskCount, difficulty = difficulty),
    )

    private fun history() = runBlocking { env.history.observeLastDays(7).first() }

    @Ignore("LOG-06: confirmar duas vezes a ultima tarefa grava dois despertares no historico")
    @Test
    fun confirmingTwiceRecordsOneWakeUp() {
        ring(alarm())
        send(AlarmRingService.ACTION_START_MISSION)
        AlarmSessionState.session.value!!.mission!!.answer.forEach { send(AlarmRingService.ACTION_MISSION_KEY, it.toString()) }
        send(AlarmRingService.ACTION_MISSION_CONFIRM)
        send(AlarmRingService.ACTION_MISSION_CONFIRM)
        awaitUntil(message = "history written") { history().isNotEmpty() }
        Thread.sleep(300)
        idleMain()
        assertEquals(1, history().size)
    }

    @Ignore("LOG-06: Espere na tela Bom dia regrava a sessao no vigia e o alarme tocaria de novo")
    @Test
    fun anEspereTapOnGoodMorningDoesNotRearmTheWatchdog() {
        ring(alarm())
        send(AlarmRingService.ACTION_START_MISSION)
        AlarmSessionState.session.value!!.mission!!.answer.forEach { send(AlarmRingService.ACTION_MISSION_KEY, it.toString()) }
        send(AlarmRingService.ACTION_MISSION_CONFIRM)
        assertEquals(SessionScreen.GOOD_MORNING, AlarmSessionState.session.value!!.screen)
        assertNull(RingWatchdog.saved(env.app))
        send(AlarmRingService.ACTION_HOLD_ON)
        assertNull("the watchdog would ring the finished alarm again", RingWatchdog.saved(env.app))
        assertEquals(0, AlarmSessionState.session.value!!.waits)
    }

    @Ignore("LOG-06: Desistir na tela do toque cria uma missao")
    @Test
    fun giveUpOnTheRingingScreenDoesNotCreateAMission() {
        ring(alarm())
        send(AlarmRingService.ACTION_GIVE_UP)
        assertNull(AlarmSessionState.session.value!!.mission)
    }

    @Ignore("LOG-03: dois alarmes no mesmo minuto misturam rotulo de um e missao do outro")
    @Test
    fun twoAlarmsInTheSameMinuteKeepLabelAndMissionTogether() {
        val once = alarm(label = "Uma vez", days = emptySet(), taskType = TaskType.PHRASE)
        val daily = alarm(label = "Diario", taskType = TaskType.MATH, difficulty = Difficulty.HARD)
        start(once)
        start(daily)
        awaitUntil(message = "ringing") { AlarmSessionState.session.value != null && playing() != null }
        Thread.sleep(300)
        idleMain()
        send(AlarmRingService.ACTION_START_MISSION)
        val session = AlarmSessionState.session.value!!
        val expected = if (session.alarmLabel == "Uma vez") TaskType.PHRASE else TaskType.MATH
        assertEquals("mission of the alarm shown on screen (${session.alarmLabel})", expected, session.mission!!.type)
    }

    @Ignore("LOG-03: falha ao ler o banco antes do som deixa o alarme mudo")
    @Test
    fun theAlarmStillRingsWhenTheDatabaseFailsAtRingTime() {
        val alarm = alarm()
        resetDatabase() // closes the instance the container's repository still points at
        runCatching {
            start(alarm)
            repeat(100) { idleMain(); Thread.sleep(5) }
        }
        assertNotNull("no session after a database failure", AlarmSessionState.session.value)
        assertNotNull("silent alarm", playing())
    }

    @Ignore("LOG-04: erro do MediaPlayer depois que comecou a tocar nao tem reserva")
    @Test
    fun aPlayerThatFailsWhileRingingIsReplaced() {
        ring(alarm())
        val failing = playing()!!
        failing.invokeErrorListener(1, -1004)
        idleMain()
        val now = playing()
        assertNotNull("sound stopped and nothing took over", now)
        assertTrue("same player kept", now !== failing)
    }

    @Ignore("LOG-10: segundo alarme durante a missao reinicia a sessao e derruba a tela da missao")
    @Test
    fun aSecondAlarmDuringTheMissionDoesNotResetIt() {
        val first = alarm()
        ring(first)
        send(AlarmRingService.ACTION_START_MISSION)
        val second = env.save(first.copy(id = 0, minute = 35, label = "Segundo"))
        start(second)
        Thread.sleep(300)
        idleMain()
        assertEquals(SessionScreen.MISSION, AlarmSessionState.session.value!!.screen)
    }

    @Ignore("LOG-10: segundo alarme durante o Espere zera as esperas")
    @Test
    fun aSecondAlarmKeepsTheWaitsOfTheFirst() {
        val first = alarm()
        ring(first)
        send(AlarmRingService.ACTION_HOLD_ON)
        val second = env.save(first.copy(id = 0, minute = 35, label = "Segundo"))
        ring(second)
        assertEquals(1, AlarmSessionState.session.value!!.waits)
    }

    @Ignore("LOG-02: o servico nao garante volume de alarme minimo")
    @Test
    fun theAlarmStreamIsRaisedToAnAudibleFloorWhileRinging() {
        val audio = env.app.getSystemService(AudioManager::class.java)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 1, 0)
        ring(alarm())
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val now = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        assertTrue("volume $now of $max", now >= Math.ceil(max * 0.6).toInt())
    }
}
