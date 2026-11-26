package com.wakewakeup.ui

import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.MainActivity
import com.wakewakeup.data.Alarm
import com.wakewakeup.session.ACTION_CONFIRM_SKIP
import com.wakewakeup.session.EXTRA_SKIP_ATS
import com.wakewakeup.session.EXTRA_SKIP_IDS
import com.wakewakeup.session.nextRingMillis
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.ui.permissions.AlarmPermissions
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalTime

/** TEC-10: a MainActivity exportada aceita CONFIRM_SKIP de qualquer app. */
@Ignore("TEC-10: intent CONFIRM_SKIP sem token abre a tela de confirmação")
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE)
class SkipTokenBugTest {

    @get:Rule(order = 0) val env = AppEnv()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun close() {
        scenario?.close()
    }

    @Test
    fun aForgedConfirmSkipIntentOpensTheListInstead() {
        val time = LocalTime.now().plusHours(2)
        val alarm = env.save(Alarm(hour = time.hour, minute = time.minute, label = "Trabalho", days = (0..6).toSet()))
        AlarmPermissions.markOnboarded(context)
        val forged = Intent(context, MainActivity::class.java)
            .setAction(ACTION_CONFIRM_SKIP)
            .putExtra(EXTRA_SKIP_IDS, longArrayOf(alarm.id))
            .putExtra(EXTRA_SKIP_ATS, longArrayOf(nextRingMillis(alarm)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        scenario = ActivityScenario.launch(forged)
        compose.waitForIdle()
        val confirmShown = compose.onAllNodesWithText(" =", substring = true).fetchSemanticsNodes().isNotEmpty()
        assertTrue("a tela de confirmação (conta de soma) abriu sem token", !confirmShown)
    }
}
