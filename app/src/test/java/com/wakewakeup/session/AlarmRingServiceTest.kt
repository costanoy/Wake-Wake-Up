package com.wakewakeup.session

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.Difficulty
import com.wakewakeup.data.TaskType
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.testing.FAKE_SOUND_MS
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
import org.robolectric.shadows.ShadowPowerManager
import org.robolectric.shadows.util.DataSource
import java.io.IOException
import java.time.Duration
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AlarmRingServiceTest {

    @get:Rule val env = AppEnv()

    private var controller: ServiceController<AlarmRingService>? = null
    private var startId = 0
    private val players = mutableListOf<ShadowMediaPlayer>()

    private val bellUri get() = "android.resource://${env.app.packageName}/raw/campainha_mecanica"
    private val bundledFallback get() = DataSource.toDataSource(env.app, Uri.parse("android.resource://${env.app.packageName}/${R.raw.campainha_mecanica}"))

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

    /** The player currently making sound, if any. */
    private fun playing(): ShadowMediaPlayer? = players.lastOrNull { it.state == ShadowMediaPlayer.State.STARTED }

    private fun alarm(
        days: Set<Int> = setOf(0, 1, 2, 3, 4),
        rampUp: Boolean = true,
        soundUri: String? = bellUri,
        taskCount: Int = 2,
        taskType: TaskType = TaskType.MATH,
    ) = env.save(
        Alarm(
            hour = 6, minute = 30, label = "Trabalho", days = days, rampUp = rampUp, soundUri = soundUri, soundName = "Campainha",
            taskCount = taskCount, taskType = taskType, difficulty = Difficulty.EASY,
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
        val c = controller!!
        AlarmRingService.sendAction(env.app, action, key)
        c.get().onStartCommand(shadowOf(env.app).nextStartedService, 0, ++startId)
        idleMain()
    }

    private fun type(text: String) = text.forEach { send(AlarmRingService.ACTION_MISSION_KEY, it.toString()) }

    private fun setTyped(text: String) {
        AlarmRingService.sendTypedText(env.app, text)
        controller!!.get().onStartCommand(shadowOf(env.app).nextStartedService, 0, ++startId)
        idleMain()
    }

    private fun wrongAnswer() = if (mission.answer == "99") "98" else "99"

    @Suppress("UNCHECKED_CAST")
    private fun setHoldExpired(service: AlarmRingService) {
        // Espere is timed on the wall clock, which Robolectric doesn't fast-forward: pretend the
        // five minutes are over.
        AlarmRingService::class.java.getDeclaredField("holdUntilMillis").apply { isAccessible = true }.set(service, System.currentTimeMillis() - 1)
    }

    private fun wakeLockHeld() = ShadowPowerManager.getLatestWakeLock()?.isHeld == true

    // ---------------------------------------------------------------------------------------
    // Ringing
    // ---------------------------------------------------------------------------------------

    @Test
    fun ringsTheChosenSoundOnLoopStartingLow() {
        val service = ring(alarm())
        assertEquals(SessionScreen.RINGING, session.screen)
        assertEquals("Trabalho", session.alarmLabel)
        assertEquals(RAMP_START_VOLUME, session.volume, 0.001f)

        val player = playing()!!
        assertEquals(DataSource.toDataSource(env.app, Uri.parse(bellUri)), player.dataSource)
        assertEquals(RAMP_START_VOLUME, player.leftVolume, 0.001f)

        // Foreground notification with a full-screen intent, the ring screen, and a wake lock.
        val notification = shadowOf(service).lastForegroundNotification
        assertNotNull(notification)
        assertNotNull(notification!!.fullScreenIntent)
        assertTrue(shadowOf(notification).contentTitle.toString().contains("Trabalho"))
        assertEquals(RingActivity::class.java.name, shadowOf(env.app).nextStartedActivity.component?.className)
        assertTrue(wakeLockHeld())
    }

    @Test
    fun volumeClimbsToFullInAMinute() {
        ring(alarm())
        advance(Duration.ofSeconds(30))
        assertEquals(0.15f + 30 * (0.85f / 60), session.volume, 0.01f)
        assertEquals(session.volume, playing()!!.leftVolume, 0.001f)
        advance(Duration.ofSeconds(31))
        assertEquals(1f, session.volume, 0f)
        assertEquals(1f, playing()!!.leftVolume, 0f)
    }

    @Test
    fun withoutTheRampItRingsAtFullVolumeAtOnce() {
        ring(alarm(rampUp = false))
        assertEquals(1f, session.volume, 0f)
        assertEquals(1f, playing()!!.leftVolume, 0f)
    }

    @Test
    fun aOneOffAlarmTurnsItselfOffAsItRings() {
        val oneOff = alarm(days = emptySet())
        ring(oneOff)
        awaitUntil(message = "one-off disabled") { env.reload(oneOff)?.enabled == false }
        assertNull(env.scheduledFor(oneOff))
    }

    @Test
    fun aRepeatingAlarmStaysOnWhenItRings() {
        val repeating = alarm()
        ring(repeating)
        assertTrue(env.reload(repeating)!!.enabled)
    }

    // ---------------------------------------------------------------------------------------
    // Sound fallbacks: an alarm must never stay silent (or crash)
    // ---------------------------------------------------------------------------------------

    @Test
    fun aSoundThatCantPlayFallsBackToTheSystemAlarm() {
        val systemAlarm = Uri.parse("content://media/internal/audio/media/7")
        Settings.System.putString(env.app.contentResolver, Settings.System.ALARM_ALERT, systemAlarm.toString())
        val broken = "content://com.android.providers.downloads/document/42"
        ShadowMediaPlayer.addException(DataSource.toDataSource(env.app, Uri.parse(broken)), IOException("gone"))

        ring(alarm(soundUri = broken))
        assertEquals(DataSource.toDataSource(env.app, systemAlarm), playing()!!.dataSource)
    }

    @Test
    fun withNoSystemAlarmSoundTheBundledBellRings() {
        // Robolectric's phone has no system ringtones at all — the worst case.
        ring(alarm(soundUri = null))
        assertEquals(bundledFallback, playing()!!.dataSource)
    }

    @Test
    fun aBrokenSystemAlarmSoundFallsBackToTheBundledBell() {
        val systemAlarm = Uri.parse("content://media/external/audio/media/123") // file deleted since
        Settings.System.putString(env.app.contentResolver, Settings.System.ALARM_ALERT, systemAlarm.toString())
        ShadowMediaPlayer.addException(DataSource.toDataSource(env.app, systemAlarm), IOException("gone"))

        ring(alarm(soundUri = null))
        assertEquals(bundledFallback, playing()!!.dataSource)
    }

    @Test
    fun aRadioThatDoesntStartIsReplacedAfterEightSeconds() {
        val stream = Uri.parse("https://radio.example/stream.mp3")
        val streamSource = DataSource.toDataSource(env.app, stream)
        ShadowMediaPlayer.setMediaInfoProvider { ds ->
            if (ds == streamSource) ShadowMediaPlayer.MediaInfo(FAKE_SOUND_MS, 60_000) else ShadowMediaPlayer.MediaInfo(FAKE_SOUND_MS, 0)
        }
        val radio = alarm(soundUri = stream.toString())
        val intent = Intent(env.app, AlarmRingService::class.java).putExtra(EXTRA_ALARM_ID, radio.id)
        controller = Robolectric.buildService(AlarmRingService::class.java, intent).create()
        controller!!.startCommand(0, ++startId)
        awaitUntil(message = "session") { AlarmSessionState.session.value != null }
        assertNull("stream still connecting", playing())

        advance(Duration.ofSeconds(9))
        assertEquals(bundledFallback, playing()!!.dataSource)
    }

    @Test
    fun aRadioNobodyReactsToGivesWayToTheAlarmAtFullVolume() {
        val stream = Uri.parse("https://radio.example/stream.mp3")
        ring(alarm(soundUri = stream.toString()))
        assertEquals(DataSource.toDataSource(env.app, stream), playing()!!.dataSource)

        advance(Duration.ofMinutes(3).plusSeconds(1))
        assertEquals(bundledFallback, playing()!!.dataSource)
        assertEquals(1f, session.volume, 0f)
    }

    // ---------------------------------------------------------------------------------------
    // Espere (snooze)
    // ---------------------------------------------------------------------------------------

    @Test
    fun espereSilencesForFiveMinutesAndCountsOnce() {
        ring(alarm())
        val before = System.currentTimeMillis()
        send(AlarmRingService.ACTION_HOLD_ON)
        assertNull("still ringing during Espere", playing())
        assertEquals(1, session.waits)
        val until = session.holdUntilMillis!!
        assertTrue(until - before in (HOLD_ON_SECONDS * 1000L - 2_000)..(HOLD_ON_SECONDS * 1000L + 2_000))

        // A second press while it's already active changes nothing.
        send(AlarmRingService.ACTION_HOLD_ON)
        assertEquals(1, session.waits)
        assertEquals(until, session.holdUntilMillis)
    }

    @Test
    fun afterEspereItRingsAgainFromLow() {
        val service = ring(alarm())
        advance(Duration.ofSeconds(61))
        send(AlarmRingService.ACTION_HOLD_ON)
        setHoldExpired(service)
        advance(Duration.ofSeconds(1))

        assertNull(session.holdUntilMillis)
        assertEquals(SessionScreen.RINGING, session.screen)
        assertNotNull("rings again", playing())
        assertEquals(RAMP_START_VOLUME, session.volume, 0.001f)
        // Espere can be used again, as many times as needed.
        send(AlarmRingService.ACTION_HOLD_ON)
        assertEquals(2, session.waits)
    }

    @Test
    fun espereKeepsThePhoneAwakeEvenAfterAQuarterOfAnHour() {
        // Without a CPU wake lock the 5-minute timer stalls once the screen goes off, and the
        // alarm never comes back. The first lock only lasts 15 minutes.
        val service = ring(alarm())
        advance(Duration.ofMinutes(16))
        send(AlarmRingService.ACTION_HOLD_ON)
        assertTrue("no wake lock held during Espere", wakeLockHeld())
        advance(Duration.ofMinutes(4))
        assertTrue(wakeLockHeld())
        setHoldExpired(service)
        advance(Duration.ofSeconds(1))
        assertNotNull(playing())
    }

    @Test
    fun theMissionTimerKeepsThePhoneAwakeToo() {
        ring(alarm())
        advance(Duration.ofMinutes(16))
        send(AlarmRingService.ACTION_START_MISSION)
        assertTrue("no wake lock held during the mission", wakeLockHeld())
    }

    @Test
    fun aSecondAlarmDuringEspereCanBeSnoozedToo() {
        val first = alarm()
        ring(first)
        send(AlarmRingService.ACTION_HOLD_ON)

        val second = env.save(first.copy(id = 0, minute = 35, label = "Segundo"))
        ring(second)
        assertEquals("Segundo", session.alarmLabel)
        assertEquals(0, session.waits)

        send(AlarmRingService.ACTION_HOLD_ON)
        assertEquals("Espere ignored on the second alarm", 1, session.waits)
        assertNotNull(session.holdUntilMillis)
        assertNull(playing())
    }

    // ---------------------------------------------------------------------------------------
    // Mission
    // ---------------------------------------------------------------------------------------

    @Test
    fun startingTheMissionStopsTheSoundAndShowsTheFirstProblem() {
        ring(alarm(taskCount = 3))
        send(AlarmRingService.ACTION_START_MISSION)
        assertEquals(SessionScreen.MISSION, session.screen)
        assertNull(playing())
        assertEquals(TaskType.MATH, mission.type)
        assertEquals(0, mission.index)
        assertEquals(3, mission.count)
        assertEquals(MISSION_TOTAL_SECONDS, mission.secondsLeft)
        assertTrue(mission.question.isNotBlank())
    }

    @Test
    fun calculatorKeys() {
        ring(alarm())
        send(AlarmRingService.ACTION_START_MISSION)
        type("12")
        assertEquals("12", mission.typed)
        send(AlarmRingService.ACTION_MISSION_KEY, AlarmRingService.KEY_BACKSPACE)
        assertEquals("1", mission.typed)
        send(AlarmRingService.ACTION_MISSION_KEY, AlarmRingService.KEY_SIGN)
        assertEquals("-1", mission.typed)
        send(AlarmRingService.ACTION_MISSION_KEY, AlarmRingService.KEY_SIGN)
        assertEquals("1", mission.typed)
        type("23456")
        assertEquals("at most 4 digits", "1234", mission.typed)
        send(AlarmRingService.ACTION_MISSION_KEY, AlarmRingService.KEY_CLEAR)
        assertEquals("", mission.typed)
    }

    @Test
    fun confirmingNothingDoesNothing() {
        ring(alarm())
        send(AlarmRingService.ACTION_START_MISSION)
        send(AlarmRingService.ACTION_MISSION_CONFIRM)
        assertFalse(mission.wrong)
        send(AlarmRingService.ACTION_MISSION_KEY, AlarmRingService.KEY_SIGN)
        send(AlarmRingService.ACTION_MISSION_CONFIRM)
        assertFalse(mission.wrong)
        assertEquals(SessionScreen.MISSION, session.screen)
    }

    @Test
    fun aWrongAnswerIsFlaggedAndTheNextKeyStartsOver() {
        ring(alarm())
        send(AlarmRingService.ACTION_START_MISSION)
        val question = mission.question
        type(wrongAnswer())
        send(AlarmRingService.ACTION_MISSION_CONFIRM)
        assertTrue(mission.wrong)
        assertEquals(question, mission.question)
        type("5")
        assertFalse(mission.wrong)
        assertEquals("5", mission.typed)
    }

    @Test
    fun solvingEveryProblemEndsInGoodMorning() {
        ring(alarm(taskCount = 3))
        send(AlarmRingService.ACTION_HOLD_ON)
        send(AlarmRingService.ACTION_START_MISSION)
        repeat(3) { i ->
            assertEquals(i, mission.index)
            type(mission.answer)
            send(AlarmRingService.ACTION_MISSION_CONFIRM)
            if (i < 2) {
                assertTrue(mission.justCorrect)
                assertEquals("", mission.typed)
            }
        }
        assertEquals(SessionScreen.GOOD_MORNING, session.screen)
        val finished = session.finished!!
        assertEquals(1, finished.waits)
        assertEquals(6, finished.alarmHour)
        assertEquals(30, finished.alarmMinute)
        assertTrue(finished.durationSec >= 0)
        assertNull(playing())
    }

    @Test
    fun runningOutOfTimeRingsAgainLouder() {
        ring(alarm())
        send(AlarmRingService.ACTION_START_MISSION)
        type("1")
        advance(Duration.ofSeconds(MISSION_TOTAL_SECONDS.toLong()))
        assertEquals(SessionScreen.RINGING, session.screen)
        assertTrue(session.volume >= 0.8f)
        assertEquals(session.volume, playing()!!.leftVolume, 0.001f)
        // The next attempt starts with a fresh problem and the full time.
        send(AlarmRingService.ACTION_START_MISSION)
        assertEquals(MISSION_TOTAL_SECONDS, mission.secondsLeft)
        assertEquals("", mission.typed)
    }

    @Test
    fun theTimerCountsDownOncePerSecond() {
        ring(alarm())
        send(AlarmRingService.ACTION_START_MISSION)
        advance(Duration.ofSeconds(10))
        assertEquals(MISSION_TOTAL_SECONDS - 10, mission.secondsLeft)
    }

    @Test
    fun givingUpSwitchesToThePhraseAndKeepsTheClockRunning() {
        ring(alarm())
        send(AlarmRingService.ACTION_START_MISSION)
        advance(Duration.ofSeconds(20))
        send(AlarmRingService.ACTION_GIVE_UP)
        assertEquals(TaskType.PHRASE, mission.type)
        assertEquals(MISSION_TOTAL_SECONDS - 20, mission.secondsLeft)
        assertEquals(MissionGenerator.phraseOfTheDay(), mission.phrase)

        setTyped("isso nao e a frase")
        send(AlarmRingService.ACTION_MISSION_CONFIRM)
        assertTrue(mission.wrong)
        setTyped(mission.phrase.lowercase().filter { it.isLetter() || it == ' ' })
        assertFalse(mission.wrong)
        send(AlarmRingService.ACTION_MISSION_CONFIRM)
        assertEquals(SessionScreen.GOOD_MORNING, session.screen)
    }

    @Test
    fun aPhraseAlarmGoesStraightToThePhrase() {
        ring(alarm(taskType = TaskType.PHRASE))
        send(AlarmRingService.ACTION_START_MISSION)
        assertEquals(TaskType.PHRASE, mission.type)
        setTyped("x".repeat(200))
        assertEquals(80, mission.typed.length)
    }

    @Test
    fun finishingRecordsTheWakeUpAndStopsEverything() {
        val service = ring(alarm(taskCount = 1))
        send(AlarmRingService.ACTION_HOLD_ON)
        send(AlarmRingService.ACTION_START_MISSION)
        type(mission.answer)
        send(AlarmRingService.ACTION_MISSION_CONFIRM)
        send(AlarmRingService.ACTION_FINISH)
        awaitUntil(message = "session cleared") { AlarmSessionState.session.value == null }

        val recorded = runBlocking { env.history.observeLastDays(7).first() }
        assertEquals(1, recorded.size)
        val entry = recorded.single()
        assertEquals(LocalDate.now(), entry.date)
        assertEquals(6, entry.alarmHour)
        assertEquals(30, entry.alarmMinute)
        assertEquals(1, entry.waits)

        assertTrue(shadowOf(service).isStoppedBySelf)
        assertTrue(shadowOf(service).isForegroundStopped)
        assertFalse(wakeLockHeld())
        assertNull(playing())
    }

    @Test
    fun aRepeatingAlarmKeepsItsScheduleThroughTheWholeRitual() {
        val repeating = alarm(taskCount = 1)
        val before = env.scheduledFor(repeating)!!.triggerAtMs
        ring(repeating)
        send(AlarmRingService.ACTION_START_MISSION)
        type(mission.answer)
        send(AlarmRingService.ACTION_MISSION_CONFIRM)
        send(AlarmRingService.ACTION_FINISH)
        awaitUntil(message = "session cleared") { AlarmSessionState.session.value == null }
        assertEquals(before, env.scheduledFor(repeating)!!.triggerAtMs)
    }
}
