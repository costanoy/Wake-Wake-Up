package com.wakewakeup.session

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakewakeup.MainActivity
import com.wakewakeup.ui.goodmorning.GoodMorningScreen
import com.wakewakeup.ui.mission.MissionScreen
import com.wakewakeup.ui.ring.RingScreen
import com.wakewakeup.ui.ring.RingSessionViewModel
import com.wakewakeup.ui.theme.WakeWakeUpTheme

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

                LaunchedEffect(session) {
                    if (session == null) {
                        val intent = Intent(this@RingActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        if (wantsStats) intent.putExtra(MainActivity.EXTRA_OPEN_STATS, true)
                        startActivity(intent)
                        finish()
                    }
                }

                val current = session
                when {
                    current == null -> Unit
                    current.screen == SessionScreen.MISSION && current.mission != null -> MissionScreen(
                        mission = current.mission,
                        onGiveUp = viewModel::giveUp,
                        onKey = viewModel::pressKey,
                        onConfirm = viewModel::confirm,
                    )
                    current.screen == SessionScreen.GOOD_MORNING && current.finished != null -> GoodMorningScreen(
                        finished = current.finished,
                        onStartDay = viewModel::finishSession,
                        onSeeStats = { wantsStats = true; viewModel.finishSession() },
                    )
                    else -> RingScreen(
                        session = current,
                        onStartMission = viewModel::startMission,
                        onHoldOn = viewModel::holdOn,
                    )
                }
            }
        }
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
