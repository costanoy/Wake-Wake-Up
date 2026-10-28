package com.wakewakeup.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.MainActivity
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.session.AlarmRingService
import com.wakewakeup.session.AlarmSessionState
import com.wakewakeup.session.EXTRA_ALARM_ID
import com.wakewakeup.session.RingActivity
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.ui.permissions.AlarmPermissions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/** A phone set to Portuguese gets the whole app in Portuguese. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-$PHONE")
class PortugueseUiTest {

    @get:Rule(order = 0) val env = AppEnv()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    private var main: ActivityScenario<MainActivity>? = null
    private var ring: ActivityScenario<RingActivity>? = null
    private var service: ServiceController<AlarmRingService>? = null

    @After
    fun close() {
        ring?.close()
        main?.close()
        service?.destroy()
    }

    private fun launch() {
        AlarmPermissions.markOnboarded(context)
        main = ActivityScenario.launch(MainActivity::class.java)
    }

    @Test
    fun theStringsArePortuguese() {
        assertEquals("Nenhum alarme ainda", str(R.string.empty_title))
        assertEquals("segunda", context.resources.getStringArray(R.array.day_full)[0])
    }

    @Test
    fun listAndEditor() {
        launch()
        compose.waitForText("Nenhum alarme ainda")
        compose.onNodeWithText("PRÓXIMO").assertIsDisplayed()
        compose.onNodeWithText("NENHUM").assertIsDisplayed()
        compose.onNodeWithContentDescription("Criar alarme").performClick()

        compose.waitForText("Novo alarme")
        compose.onNodeWithText("Matemática, 3 contas").assertIsDisplayed()
        compose.onNodeWithText("Padrão do sistema").assertIsDisplayed()
        compose.onNodeWithContentDescription("segunda").performClick()
        compose.onNodeWithContentDescription("Salvar").performClick()

        compose.waitForDescription("Alarme das 07:00")
        compose.onNodeWithText("MATEMÁTICA").assertIsDisplayed()
        compose.onNodeWithText("SEG").assertIsDisplayed()
    }

    @Test
    fun skippingADayInPortuguese() {
        val time = LocalTime.now().plusHours(1)
        env.save(Alarm(hour = time.hour, minute = time.minute, label = "Diario", days = (0..6).toSet()))
        launch()
        compose.waitForText("Diario")
        compose.onNodeWithContentDescription("Alarme das %02d:%02d".format(time.hour, time.minute)).performClick()
        compose.waitForText("Pular só", substring = true)
        val prompt = compose.onNodeWithText("Pular só", substring = true).fetchSemanticsNode()
        val text = prompt.config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString()
        // "Pular só <dia>? Volta a tocar <dia seguinte>." with plain weekday names.
        val skipped = LocalDate.now().plusDays(if (time.isAfter(LocalTime.now())) 0 else 1)
        val days = context.resources.getStringArray(R.array.day_full)
        val skippedName = days[com.wakewakeup.session.isoDayIndexOf(skipped)]
        val backName = days[com.wakewakeup.session.isoDayIndexOf(skipped.plusDays(1))]
        assertEquals("Pular só $skippedName? Volta a tocar $backName.", text)
        compose.onNodeWithText("PULAR").performClick()
        compose.waitForText("Pulando $skippedName · volta $backName")
        compose.onNodeWithText("DESFAZER").assertIsDisplayed()
    }

    @Test
    fun theRingScreen() {
        val alarm = env.save(Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0), soundUri = "android.resource://com.wakewakeup/raw/passaros"))
        val intent = Intent(context, AlarmRingService::class.java).putExtra(EXTRA_ALARM_ID, alarm.id)
        service = Robolectric.buildService(AlarmRingService::class.java, intent).create()
        service!!.startCommand(0, 1)
        compose.waitFor("ringing") { AlarmSessionState.session.value != null }
        ring = ActivityScenario.launch(RingActivity::class.java)

        compose.waitForText("ESPERE 5 MIN")
        compose.onNodeWithText("SEM LIMITE").assertIsDisplayed()
        compose.onNodeWithContentDescription("Começar missão").assertIsDisplayed()
        compose.onNodeWithText("SUBINDO").assertIsDisplayed()
        // "TRABALHO · TERÇA, 13 OUT"
        val date = LocalDate.now()
        val weekday = context.resources.getStringArray(R.array.day_full)[com.wakewakeup.session.isoDayIndexOf(date)].uppercase()
        compose.onNodeWithText("TRABALHO · $weekday, ", substring = true).assertIsDisplayed()
    }
}
