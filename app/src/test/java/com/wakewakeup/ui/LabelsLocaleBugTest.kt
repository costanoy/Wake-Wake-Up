package com.wakewakeup.ui

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.wakewakeup.MainActivity
import com.wakewakeup.testing.AppEnv
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.activity.compose.setContent
import com.wakewakeup.ui.components.panelDate
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.Locale

/** TEC-09: a data da tela de toque mistura o idioma dos recursos com o Locale do sistema. */
@Ignore("TEC-09: panelDate usa Locale.getDefault() (idioma misturado e lint NonObservableLocale)")
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "de")
class LabelsLocaleBugTest {

    @get:Rule(order = 0) val env = AppEnv()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun theDateStaysInOneLanguage() {
        val saved = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
        try {
            var text = ""
            compose.activity.runOnUiThread { compose.activity.setContent { text = panelDate(LocalDate.of(2026, 3, 4)) } }
            compose.waitForIdle()
            assertEquals("WEDNESDAY, MAR 4", text)
        } finally {
            Locale.setDefault(saved)
        }
    }
}
