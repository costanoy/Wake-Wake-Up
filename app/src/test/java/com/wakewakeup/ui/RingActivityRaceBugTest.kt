package com.wakewakeup.ui

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.MainActivity
import com.wakewakeup.data.Alarm
import com.wakewakeup.session.AlarmSessionState
import com.wakewakeup.session.RingActivity
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.testing.advance
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** TEC-05: a tela de toque aberta antes de a sessão existir se fecha sozinha. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE)
class RingActivityRaceBugTest {

    @get:Rule(order = 0) val env = AppEnv()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<RingActivity>? = null

    @After
    fun close() {
        runCatching { scenario?.close() }
    }

    @Test
    fun theRingScreenWaitsForASessionThatArrivesALittleLater() {
        AlarmSessionState.clear()
        scenario = ActivityScenario.launch(RingActivity::class.java)
        advance(Duration.ofMillis(500))
        compose.waitForIdle()
        AlarmSessionState.start(env.save(Alarm(hour = 6, minute = 30, label = "", days = setOf(0))))
        advance(Duration.ofMillis(100))
        compose.waitForIdle()
        val opened = generateSequence { shadowOf(env.app).nextStartedActivity }.map { it.component?.className }.toList()
        assertFalse("a MainActivity foi aberta antes de a sessão chegar", MainActivity::class.java.name in opened)
        assertNotEquals(Lifecycle.State.DESTROYED, scenario!!.state)
    }
}
