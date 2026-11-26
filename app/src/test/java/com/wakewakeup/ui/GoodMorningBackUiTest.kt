package com.wakewakeup.ui

import android.content.Intent
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.MainActivity
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.Difficulty
import com.wakewakeup.session.AlarmRingService
import com.wakewakeup.session.AlarmSessionState
import com.wakewakeup.session.EXTRA_ALARM_ID
import com.wakewakeup.session.RingActivity
import com.wakewakeup.session.SessionScreen
import com.wakewakeup.testing.AppEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

/** F59: na tela Bom dia, o botão Voltar do celular faz o mesmo que Concluir. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE)
class GoodMorningBackUiTest {

    @get:Rule(order = 0) val env = AppEnv()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    private var service: ServiceController<AlarmRingService>? = null
    private var screen: ActivityScenario<RingActivity>? = null
    private var startId = 0
    private val opened = mutableListOf<Intent>()

    @After
    fun close() {
        screen?.close()
        service?.destroy()
    }

    private val session get() = AlarmSessionState.session.value

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

    private fun startedActivities(): List<String?> {
        while (true) opened += shadowOf(env.app).nextStartedActivity ?: break
        return opened.map { it.component?.className }
    }

    private fun reachGoodMorning() {
        val alarm = env.save(
            Alarm(
                hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4), taskCount = 1,
                difficulty = Difficulty.EASY, soundUri = "android.resource://${env.app.packageName}/raw/sirene_1",
            ),
        )
        val intent = Intent(env.app, AlarmRingService::class.java).putExtra(EXTRA_ALARM_ID, alarm.id)
        service = Robolectric.buildService(AlarmRingService::class.java, intent).create()
        service!!.startCommand(0, ++startId)
        waitFor("ringing") { session != null }
        screen = ActivityScenario.launch(RingActivity::class.java)
        compose.waitForDescription(str(R.string.start_mission))
        compose.onNodeWithContentDescription(str(R.string.start_mission)).performClick()
        waitFor("mission") { session?.screen == SessionScreen.MISSION }
        for (d in session!!.mission!!.answer) compose.onNode(hasText(d.toString()) and hasClickAction()).performClick()
        waitFor("typed") { session?.mission?.typed == session?.mission?.answer }
        compose.onNodeWithContentDescription(str(R.string.confirm)).performClick()
        waitFor("good morning") { session?.screen == SessionScreen.GOOD_MORNING }
        compose.waitForText(str(R.string.good_morning))
        startedActivities()
        opened.clear()
    }

    @Test
    fun backOnGoodMorningClosesTheSessionAndReturnsToTheApp() {
        reachGoodMorning()
        screen!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor("session over") { session == null }
        waitFor("main screen opened") { MainActivity::class.java.name in startedActivities() }
        assertNull(session)
        // The wake-up was recorded once, with no snoozes.
        val history = runBlocking { env.history.observeLastDays(7).first() }
        assertEquals(1, history.size)
        assertEquals(0, history.single().waits)
        assertTrue(opened.any { it.component?.className == MainActivity::class.java.name })
    }
}
