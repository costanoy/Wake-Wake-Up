package com.wakewakeup.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.MainActivity
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.ui.permissions.AlarmPermissions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** F08: o nome do alarme (rótulo) pode ser escrito, trocado e apagado, e aparece na lista. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE)
class AlarmLabelUiTest {

    @get:Rule(order = 0) val env = AppEnv()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun close() {
        scenario?.close()
    }

    private fun launch() {
        AlarmPermissions.markOnboarded(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
    }

    private fun openEditor(label: String) {
        launch()
        compose.waitForText(label)
        compose.onNodeWithText(label).performClick()
        compose.waitForText(str(R.string.edit_alarm))
        compose.waitForDescription(str(R.string.hour))
    }

    @Test
    fun theNameIsReplacedAndShownOnTheList() {
        val alarm = env.save(Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4)))
        openEditor("Trabalho")
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("Academia")
        compose.onNodeWithContentDescription(str(R.string.save)).performClick()
        compose.waitForText("Academia")
        assertEquals("Academia", env.reload(alarm)!!.label)
    }

    @Test
    fun anEmptyNameShowsTheUntitledPlaceholderOnTheList() {
        val alarm = env.save(Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4)))
        openEditor("Trabalho")
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNodeWithContentDescription(str(R.string.save)).performClick()
        compose.waitForText(str(R.string.untitled_alarm))
        assertEquals("", env.reload(alarm)!!.label)
    }

    @Test
    fun accentsAndEmojiInTheNameSurviveTheDatabase() {
        val alarm = env.save(Alarm(hour = 6, minute = 30, label = "Café ☕ da manhã", days = setOf(0)))
        launch()
        compose.waitForText("Café ☕ da manhã")
        assertEquals("Café ☕ da manhã", env.reload(alarm)!!.label)
    }
}
