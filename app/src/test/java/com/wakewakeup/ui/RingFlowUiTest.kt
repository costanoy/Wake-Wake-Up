package com.wakewakeup.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.MainActivity
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.Difficulty
import com.wakewakeup.data.TaskType
import com.wakewakeup.session.AlarmRingService
import com.wakewakeup.session.AlarmSessionState
import com.wakewakeup.session.EXTRA_ALARM_ID
import com.wakewakeup.session.MissionGenerator
import com.wakewakeup.session.RingActivity
import com.wakewakeup.session.SessionScreen
import com.wakewakeup.testing.AppEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

/**
 * The whole wake-up, the way it happens on the phone: the service starts ringing, the ring
 * screen opens, and every tap travels back to the service as an intent.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE)
class RingFlowUiTest {

    @get:Rule(order = 0) val env = AppEnv()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    private var service: ServiceController<AlarmRingService>? = null
    private var screen: ActivityScenario<RingActivity>? = null
    private var startId = 0

    @After
    fun close() {
        screen?.close()
        service?.destroy()
    }

    private val session get() = AlarmSessionState.session.value

    /** Delivers the intents the ring screen sent to the service, as Android would. */
    private fun pump() {
        while (true) {
            val intent = shadowOf(env.app).nextStartedService ?: return
            service!!.get().onStartCommand(intent, 0, ++startId)
        }
    }

    private fun waitFor(message: String, condition: () -> Boolean) = compose.waitFor(message) {
        pump()
        condition()
    }

    private fun ring(taskType: TaskType = TaskType.MATH, taskCount: Int = 1) {
        val alarm = env.save(
            Alarm(
                hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4), taskType = taskType, taskCount = taskCount,
                difficulty = Difficulty.EASY, soundUri = "android.resource://${env.app.packageName}/raw/sirene_1",
            ),
        )
        val intent = Intent(env.app, AlarmRingService::class.java).putExtra(EXTRA_ALARM_ID, alarm.id)
        service = Robolectric.buildService(AlarmRingService::class.java, intent).create()
        service!!.startCommand(0, ++startId)
        waitFor("ringing") { session != null }
        assertTrue(RingActivity::class.java.name in openedActivities())
        screen = ActivityScenario.launch(RingActivity::class.java)
        compose.waitForDescription(str(R.string.start_mission))
        openedActivities()
        opened.clear()
    }

    private val opened = mutableListOf<Intent>()

    /** Activities started so far (the ring screen hands over to the app when it's done). */
    private fun openedActivities(): List<String?> {
        while (true) opened += shadowOf(env.app).nextStartedActivity ?: break
        return opened.map { it.component?.className }
    }

    private fun pressKeys(digits: String) {
        for (d in digits) {
            compose.onNode(hasText(d.toString()) and hasClickAction()).performClick()
        }
    }

    @Test
    fun theWholeRitualFromRingingToGoodMorning() {
        ring()
        compose.onNodeWithContentDescription("06:30").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.espere_title).uppercase()).assertIsDisplayed()

        // Espere: five quiet minutes, counted.
        compose.onNodeWithText(str(R.string.espere_title).uppercase()).performClick()
        waitFor("espere") { session?.holdUntilMillis != null }
        compose.waitForText(str(R.string.espere_returns_in))
        compose.onNodeWithText(str(R.string.times, 1)).assertIsDisplayed()

        // "I'm up": the mission.
        compose.onNodeWithContentDescription(str(R.string.awake_start_mission)).performClick()
        waitFor("mission") { session?.screen == SessionScreen.MISSION }
        compose.waitForText(str(R.string.problem_of, 1, 1).uppercase())
        val mission = session!!.mission!!
        compose.onNodeWithText("${mission.question} =").assertIsDisplayed()

        // A wrong answer first.
        val wrong = if (mission.answer == "99") "98" else "99"
        pressKeys(wrong)
        compose.onNodeWithContentDescription(str(R.string.confirm)).performClick()
        waitFor("wrong") { session?.mission?.wrong == true }
        compose.waitForText(str(R.string.wrong_try_again))

        // Then the right one.
        pressKeys(mission.answer)
        waitFor("typed") { session?.mission?.typed == mission.answer }
        compose.onNodeWithContentDescription(str(R.string.confirm)).performClick()
        waitFor("good morning") { session?.screen == SessionScreen.GOOD_MORNING }
        compose.waitForText(str(R.string.good_morning))

        // Done: recorded, and back to the app.
        compose.onNodeWithContentDescription(str(R.string.done)).performClick()
        waitFor("session over") { session == null }
        val history = runBlocking { env.history.observeLastDays(7).first() }
        assertEquals(1, history.single().waits)
        waitFor("main screen opened") { MainActivity::class.java.name in openedActivities() }
        val main = opened.last { it.component?.className == MainActivity::class.java.name }
        assertFalse(main.getBooleanExtra(MainActivity.EXTRA_OPEN_STATS, false))
    }

    @Test
    fun seeStatsOpensTheStatsScreen() {
        ring()
        compose.onNodeWithContentDescription(str(R.string.start_mission)).performClick()
        waitFor("mission") { session?.screen == SessionScreen.MISSION }
        pressKeys(session!!.mission!!.answer)
        waitFor("typed") { session?.mission?.typed == session?.mission?.answer }
        compose.onNodeWithContentDescription(str(R.string.confirm)).performClick()
        waitFor("good morning") { session?.screen == SessionScreen.GOOD_MORNING }
        compose.waitForText(str(R.string.see_stats))
        compose.onNodeWithText(str(R.string.see_stats)).performClick()
        waitFor("main screen opened") { MainActivity::class.java.name in openedActivities() }
        assertTrue(opened.last { it.component?.className == MainActivity::class.java.name }.getBooleanExtra(MainActivity.EXTRA_OPEN_STATS, false))
    }

    @Test
    fun cantSolveItSwitchesToTypingThePhrase() {
        ring()
        compose.onNodeWithContentDescription(str(R.string.start_mission)).performClick()
        waitFor("mission") { session?.screen == SessionScreen.MISSION }
        compose.onNodeWithText(str(R.string.cant_solve).uppercase()).performClick()
        waitFor("phrase") { session?.mission?.type == TaskType.PHRASE }
        val phrase = MissionGenerator.phraseOfTheDay()
        compose.waitForText(str(R.string.type_phrase_short).uppercase())
        compose.onNodeWithContentDescription(phrase).assertIsDisplayed()

        compose.onNode(hasSetTextAction()).performTextInput(phrase.lowercase().filter { it.isLetter() || it == ' ' })
        waitFor("typed") { session?.mission?.typed?.isNotEmpty() == true }
        compose.waitForText(str(R.string.chars_of, phrase.length, phrase.length))
        compose.onNodeWithContentDescription(str(R.string.confirm)).performClick()
        waitFor("good morning") { session?.screen == SessionScreen.GOOD_MORNING }
    }

    @Test
    fun aPhraseAlarm() {
        ring(taskType = TaskType.PHRASE)
        compose.onNodeWithContentDescription(str(R.string.start_mission)).performClick()
        waitFor("mission") { session?.screen == SessionScreen.MISSION }
        compose.waitForText(str(R.string.chars_of, 0, MissionGenerator.phraseOfTheDay().length))
        compose.onNode(hasSetTextAction()).performTextInput("nada a ver")
        waitFor("typed") { session?.mission?.typed == "nada a ver" }
        compose.onNodeWithContentDescription(str(R.string.confirm)).performClick()
        waitFor("wrong") { session?.mission?.wrong == true }
        compose.waitForText(str(R.string.wrong_try_again))
    }

    @Test
    fun aRingScreenOpenedAfterTheAlarmEndedGoesBackToTheApp() {
        // E.g. the notification tapped late: there's nothing ringing anymore.
        screen = ActivityScenario.launch(RingActivity::class.java)
        compose.waitFor("app opened") { MainActivity::class.java.name in openedActivities() }
        var finishing = false
        screen!!.onActivity { finishing = it.isFinishing }
        assertTrue("the ring screen should close itself", finishing)
    }

    @Test
    fun backDoesntDismissTheAlarm() {
        ring()
        screen!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        screen!!.onActivity { assertFalse(it.isFinishing) }
        compose.onNodeWithContentDescription(str(R.string.start_mission)).assertIsDisplayed()
        assertEquals(SessionScreen.RINGING, session?.screen)
    }

    @Test
    fun theVolumeMeterRisesWithTheRamp() {
        ring()
        compose.onNodeWithText(str(R.string.volume_rising).uppercase()).assertIsDisplayed()
        repeat(62) { com.wakewakeup.testing.advance(java.time.Duration.ofSeconds(1)) }
        compose.waitForText(str(R.string.volume_max).uppercase())
    }
}
