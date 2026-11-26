package com.wakewakeup.session

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.Difficulty
import com.wakewakeup.data.TaskType
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.testing.advance
import com.wakewakeup.testing.awaitUntil
import com.wakewakeup.testing.idleMain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.ShadowMediaPlayer
import java.time.Duration

/**
 * Casos de borda do ritual do toque: toques fora de hora (Espere durante a missão, tecla no meio
 * do toque), sessões velhas no vigia e alarme apagado enquanto toca.
 */
@RunWith(AndroidJUnit4::class)
class AlarmRingServiceEdgeTest {

    @get:Rule val env = AppEnv()

    private var controller: ServiceController<AlarmRingService>? = null
    private var startId = 0
    private val players = mutableListOf<ShadowMediaPlayer>()

    @Before
    fun recordPlayers() {
        ShadowMediaPlayer.setCreateListener { _, shadow -> players += shadow }
    }

    @After
    fun stopService() {
        controller?.destroy()
    }

    private val session get() = AlarmSessionState.session.value!!
    private val mission get() = session.mission!!
    private fun playing() = players.lastOrNull { it.state == ShadowMediaPlayer.State.STARTED }

    private fun alarm(taskCount: Int = 2, taskType: TaskType = TaskType.MATH, days: Set<Int> = setOf(0, 1, 2, 3, 4)) = env.save(
        Alarm(
            hour = 6, minute = 30, label = "Trabalho", days = days, taskCount = taskCount, taskType = taskType,
            difficulty = Difficulty.EASY, soundUri = "android.resource://${env.app.packageName}/raw/campainha_mecanica",
        ),
    )

    private fun ring(alarm: Alarm): AlarmRingService {
        val intent = Intent(env.app, AlarmRingService::class.java).putExtra(EXTRA_ALARM_ID, alarm.id)
        val c = controller ?: Robolectric.buildService(AlarmRingService::class.java, intent).create().also { controller = it }
        c.get().onStartCommand(intent, 0, ++startId)
        awaitUntil(message = "alarm ${alarm.id} ringing") { AlarmSessionState.session.value?.alarmId == alarm.id && playing() != null }
        return c.get()
    }

    private fun send(action: String, key: String? = null) {
        AlarmRingService.sendAction(env.app, action, key)
        controller!!.get().onStartCommand(shadowOf(env.app).nextStartedService, 0, ++startId)
        idleMain()
    }

    private fun type(text: String) = text.forEach { send(AlarmRingService.ACTION_MISSION_KEY, it.toString()) }

    private fun solveAll() {
        repeat(mission.count) {
            type(mission.answer)
            send(AlarmRingService.ACTION_MISSION_CONFIRM)
        }
    }

    // ---- Espere e missão fora de ordem -------------------------------------------------------

    @Test
    fun startingTheMissionDuringEspereEndsTheEspereForGood() {
        ring(alarm())
        send(AlarmRingService.ACTION_HOLD_ON)
        assertNotNull(session.holdUntilMillis)
        send(AlarmRingService.ACTION_START_MISSION)
        assertEquals(SessionScreen.MISSION, session.screen)
        assertNull(session.holdUntilMillis)
        // A minute later the mission is still the thing on screen, with no sound under it.
        repeat(60) { advance(Duration.ofSeconds(1)) }
        assertEquals(SessionScreen.MISSION, session.screen)
        assertNull(playing())
    }

    @Test
    fun doubleTappingStartMissionKeepsTheSameProblem() {
        ring(alarm())
        send(AlarmRingService.ACTION_START_MISSION)
        val first = mission.question
        send(AlarmRingService.ACTION_START_MISSION)
        assertEquals(first, mission.question)
        assertEquals(MISSION_TOTAL_SECONDS, mission.secondsLeft)
    }

    @Test
    fun aKeyPressedBeforeTheMissionStartsChangesNothing() {
        ring(alarm())
        send(AlarmRingService.ACTION_MISSION_KEY, "5")
        assertEquals(SessionScreen.RINGING, session.screen)
        send(AlarmRingService.ACTION_MISSION_CONFIRM)
        assertEquals(SessionScreen.RINGING, session.screen)
    }

    @Test
    fun theCalculatorTakesAtMostFourDigits() {
        ring(alarm())
        send(AlarmRingService.ACTION_START_MISSION)
        type("123456")
        assertEquals("1234", mission.typed)
    }

    @Test
    fun theSignKeyTogglesAMinus() {
        ring(alarm())
        send(AlarmRingService.ACTION_START_MISSION)
        type("5")
        send(AlarmRingService.ACTION_MISSION_KEY, AlarmRingService.KEY_SIGN)
        assertEquals("-5", mission.typed)
        send(AlarmRingService.ACTION_MISSION_KEY, AlarmRingService.KEY_SIGN)
        assertEquals("5", mission.typed)
    }

    @Test
    fun aLoneMinusSignIsNeverAnAnswer() {
        ring(alarm())
        send(AlarmRingService.ACTION_START_MISSION)
        send(AlarmRingService.ACTION_MISSION_KEY, AlarmRingService.KEY_SIGN)
        send(AlarmRingService.ACTION_MISSION_CONFIRM)
        assertFalse(mission.wrong)
        assertEquals(SessionScreen.MISSION, session.screen)
    }

    @Test
    fun aTimedOutMissionStartsOverFromTheFirstProblem() {
        ring(alarm(taskCount = 3))
        send(AlarmRingService.ACTION_START_MISSION)
        type(mission.answer)
        send(AlarmRingService.ACTION_MISSION_CONFIRM)
        assertEquals(1, mission.index)
        repeat(MISSION_TOTAL_SECONDS + 1) { advance(Duration.ofSeconds(1)) }
        assertEquals(SessionScreen.RINGING, session.screen)
        assertEquals(0, session.mission?.index)
        assertTrue(session.volume >= 0.8f)
        assertNotNull(playing())
    }

    @Test
    fun aPhraseTypedWithCapsAndNoAccentsIsAccepted() {
        ring(alarm(taskType = TaskType.PHRASE))
        send(AlarmRingService.ACTION_START_MISSION)
        AlarmRingService.sendConfirm(env.app, mission.phrase.uppercase())
        controller!!.get().onStartCommand(shadowOf(env.app).nextStartedService, 0, ++startId)
        idleMain()
        assertEquals(SessionScreen.GOOD_MORNING, session.screen)
    }

    @Test
    fun aPhraseLongerThanEightyCharactersIsCutNotRejectedOutright() {
        ring(alarm(taskType = TaskType.PHRASE))
        send(AlarmRingService.ACTION_START_MISSION)
        AlarmRingService.sendTypedText(env.app, "a".repeat(200))
        controller!!.get().onStartCommand(shadowOf(env.app).nextStartedService, 0, ++startId)
        idleMain()
        assertEquals(80, mission.typed.length)
    }

    // ---- O despertar gravado -----------------------------------------------------------------

    @Test
    fun theRecordedWakeUpCountsEveryEspere() {
        ring(alarm(taskCount = 1))
        send(AlarmRingService.ACTION_HOLD_ON)
        // Espere is timed on the wall clock: pretend the five minutes are over, then ring and snooze again.
        AlarmRingService::class.java.getDeclaredField("holdUntilMillis").apply { isAccessible = true }
            .set(controller!!.get(), System.currentTimeMillis() - 1)
        advance(Duration.ofSeconds(2))
        awaitUntil(message = "ringing again") { playing() != null && session.holdUntilMillis == null }
        send(AlarmRingService.ACTION_HOLD_ON)
        assertEquals(2, session.waits)
        send(AlarmRingService.ACTION_START_MISSION)
        solveAll()
        assertEquals(SessionScreen.GOOD_MORNING, session.screen)
        awaitUntil(message = "history saved") { runBlocking { env.history.observeLastDays(7).first() }.isNotEmpty() }
        val saved = runBlocking { env.history.observeLastDays(7).first() }.single()
        assertEquals(2, saved.waits)
        assertEquals(6, saved.alarmHour)
        assertEquals(30, saved.alarmMinute)
        assertTrue(saved.durationSec >= 0)
    }

    @Test
    fun aSecondFinishAfterConcluirIsHarmless() {
        ring(alarm(taskCount = 1))
        send(AlarmRingService.ACTION_START_MISSION)
        solveAll()
        send(AlarmRingService.ACTION_FINISH)
        assertNull(AlarmSessionState.session.value)
        send(AlarmRingService.ACTION_FINISH)
        assertNull(AlarmSessionState.session.value)
    }

    // ---- Alarme apagado, sessão velha, vigia -------------------------------------------------

    @Test
    fun anAlarmDeletedBeforeTheServiceStartsRingsNothing() {
        val gone = alarm()
        runBlocking { env.repository.delete(gone) }
        val intent = Intent(env.app, AlarmRingService::class.java).putExtra(EXTRA_ALARM_ID, gone.id)
        val c = Robolectric.buildService(AlarmRingService::class.java, intent).create().also { controller = it }
        c.get().onStartCommand(intent, 0, ++startId)
        awaitUntil(message = "service stopped") { shadowOf(c.get()).isStoppedBySelf }
        assertNull(AlarmSessionState.session.value)
        assertNull(playing())
        assertNull(RingWatchdog.saved(env.app))
    }

    private fun savedSession(startedAt: Long, hold: Long? = null) = RingSession(
        alarmId = 7, alarmLabel = "", alarmHour = 6, alarmMinute = 30, difficulty = Difficulty.EASY,
        waits = 1, holdUntilMillis = hold, startedAtMillis = startedAt,
    )

    @Test
    fun aSessionOlderThanThreeHoursIsNotBroughtBack() {
        val now = System.currentTimeMillis()
        val threeHours = 3 * 60 * 60_000L
        RingWatchdog.save(env.app, savedSession(startedAt = now - threeHours - 60_000))
        assertNull(RingWatchdog.saved(env.app, now))
        assertFalse(RingWatchdog.resumeIfInterrupted(env.app))
        assertNull(shadowOf(env.app).nextStartedService)
    }

    @Test
    fun aSessionJustUnderThreeHoursIsStillBroughtBack() {
        val now = System.currentTimeMillis()
        RingWatchdog.save(env.app, savedSession(startedAt = now - 2 * 60 * 60_000L))
        assertNotNull(RingWatchdog.saved(env.app, now))
        assertTrue(RingWatchdog.resumeIfInterrupted(env.app))
        assertTrue(shadowOf(env.app).nextStartedService.getBooleanExtra(EXTRA_RESUMED_SESSION, false))
    }

    @Test
    fun aSessionFromTheFutureIsIgnoredInsteadOfRingingForever() {
        // The phone's clock was set back after the session was saved.
        val now = System.currentTimeMillis()
        RingWatchdog.save(env.app, savedSession(startedAt = now + 60 * 60_000L))
        assertNull(RingWatchdog.saved(env.app, now))
    }

    @Test
    fun theWatchdogWakingWithNothingSavedStopsWatching() {
        RingWatchdog.save(env.app, savedSession(startedAt = System.currentTimeMillis() - 4 * 60 * 60_000L))
        assertNotNull(env.scheduledWithAction(ACTION_RING_WATCHDOG))
        env.deliverBroadcast(Intent(env.app, AlarmReceiver::class.java).setAction(ACTION_RING_WATCHDOG))
        assertNull(shadowOf(env.app).nextStartedService)
        assertNull(env.scheduledWithAction(ACTION_RING_WATCHDOG))
        assertNull(RingWatchdog.saved(env.app))
    }

    @Test
    fun aLostSessionWhoseEspereAlreadyEndedRingsAtOnce() {
        val alarm = alarm()
        val now = System.currentTimeMillis()
        RingWatchdog.save(
            env.app,
            savedSession(startedAt = now - 10 * 60_000L, hold = now - 60_000L).copy(alarmId = alarm.id),
        )
        val restart = Intent(env.app, AlarmRingService::class.java)
            .putExtra(EXTRA_ALARM_ID, alarm.id).putExtra(EXTRA_RESUMED_SESSION, true)
        val c = Robolectric.buildService(AlarmRingService::class.java, restart).create().also { controller = it }
        c.get().onStartCommand(restart, 0, ++startId)
        awaitUntil(message = "ringing") { AlarmSessionState.session.value != null && playing() != null }
        assertNull(session.holdUntilMillis)
        assertEquals(1, session.waits)
    }

    @Test
    fun theOriginalAlarmVolumeSurvivesTheProcessDyingMidRing() {
        // REV-06: the first service lifted the volume and remembered the old level only in memory.
        val audio = env.app.getSystemService(android.media.AudioManager::class.java)
        audio.setStreamVolume(android.media.AudioManager.STREAM_ALARM, 1, 0)
        val alarm = alarm(taskCount = 1)
        ring(alarm)
        assertEquals(1, RingWatchdog.originalVolume(env.app))
        // The process dies: the session is gone from memory, the volume stays lifted.
        controller!!.destroy()
        controller = null
        AlarmSessionState.clear()
        players.clear()
        assertTrue(audio.getStreamVolume(android.media.AudioManager.STREAM_ALARM) > 1)

        val restart = Intent(env.app, AlarmRingService::class.java)
            .putExtra(EXTRA_ALARM_ID, alarm.id).putExtra(EXTRA_RESUMED_SESSION, true)
        val c = Robolectric.buildService(AlarmRingService::class.java, restart).create().also { controller = it }
        c.get().onStartCommand(restart, 0, ++startId)
        awaitUntil(message = "ringing again") { AlarmSessionState.session.value != null && playing() != null }
        send(AlarmRingService.ACTION_START_MISSION)
        solveAll()

        assertEquals(SessionScreen.GOOD_MORNING, session.screen)
        assertEquals(1, audio.getStreamVolume(android.media.AudioManager.STREAM_ALARM))
        assertNull(RingWatchdog.originalVolume(env.app))
    }
}
