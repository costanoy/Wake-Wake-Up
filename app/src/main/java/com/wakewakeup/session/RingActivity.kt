package com.wakewakeup.session

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakewakeup.MainActivity
import com.wakewakeup.ui.goodmorning.GoodMorningScreen
import com.wakewakeup.ui.mission.MissionScreen
import com.wakewakeup.ui.ring.RingScreen
import com.wakewakeup.ui.ring.RingSessionViewModel
import com.wakewakeup.ui.theme.Housing
import com.wakewakeup.ui.theme.WakeWakeUpTheme
import kotlinx.coroutines.delay

/** How long a ring screen opened before its session exists waits for the service to create it. */
private const val SESSION_WAIT_MS = 3_000L

class RingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setShowsOverLockScreen()
        setContent {
            WakeWakeUpTheme {
                val viewModel: RingSessionViewModel = viewModel()
                val session by viewModel.session.collectAsStateWithLifecycle()
                var wantsStats by remember { mutableStateOf(false) }
                // The full-screen notification can open this screen before the service has created
                // the session: wait a moment for it unless one was already seen.
                var seenSession by rememberSaveable { mutableStateOf(false) }

                // No dismiss gesture: the ring/mission screens can only be left by actually
                // completing them (Start Mission -> Confirm, or Hold On), never by back. On Bom
                // dia, Back is Concluir.
                BackHandler(enabled = true) {
                    if (session?.screen == SessionScreen.GOOD_MORNING) viewModel.finishSession()
                }

                LaunchedEffect(session) {
                    if (session != null) {
                        seenSession = true
                        return@LaunchedEffect
                    }
                    if (!seenSession) delay(SESSION_WAIT_MS)
                    val intent = Intent(this@RingActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    if (wantsStats) intent.putExtra(MainActivity.EXTRA_OPEN_STATS, true)
                    // Before the first unlock the app can't start (its database is sealed): just close.
                    runCatching { startActivity(intent) }
                    finish()
                }

                val current = session
                when {
                    current == null -> Box(Modifier.fillMaxSize().background(Housing))
                    current.screen == SessionScreen.MISSION && current.mission != null -> MissionScreen(
                        mission = current.mission,
                        onGiveUp = viewModel::giveUp,
                        onKey = viewModel::pressKey,
                        onTypedChange = viewModel::setTypedText,
                        onConfirm = viewModel::confirm,
                    )
                    current.screen == SessionScreen.GOOD_MORNING && current.finished != null -> {
                        LaunchedEffect(current.startedAtMillis) { viewModel.loadCompanions(current) }
                        GoodMorningScreen(
                            finished = current.finished,
                            onStartDay = viewModel::finishSession,
                            onSeeStats = { wantsStats = true; viewModel.finishSession() },
                            companions = viewModel.companionsSkipped.ifEmpty { viewModel.companions },
                            companionsSkipped = viewModel.companionsSkipped.isNotEmpty(),
                            onSkipCompanions = viewModel::skipCompanions,
                            onUndoCompanions = viewModel::undoCompanions,
                        )
                    }
                    else -> RingScreen(
                        session = current,
                        onStartMission = viewModel::startMission,
                        onHoldOn = viewModel::holdOn,
                    )
                }
            }
        }
    }

    /**
     * Volume up still works, but lowering or muting the alarm is not a way out of the mission:
     * the keys are swallowed while it rings, until Bom dia.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val key = event.keyCode
        if (key == KeyEvent.KEYCODE_VOLUME_DOWN || key == KeyEvent.KEYCODE_VOLUME_MUTE) {
            val screen = AlarmSessionState.session.value?.screen
            if (screen != null && screen != SessionScreen.GOOD_MORNING) return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun setShowsOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
