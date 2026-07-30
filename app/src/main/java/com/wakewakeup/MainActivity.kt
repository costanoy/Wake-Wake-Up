package com.wakewakeup

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wakewakeup.ui.navigation.WwuNavGraph
import com.wakewakeup.ui.theme.WakeWakeUpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark-themed regardless of the system setting, so force
        // light system-bar icons rather than letting them follow system light/dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val openStats = intent?.getBooleanExtra(EXTRA_OPEN_STATS, false) ?: false
        setContent {
            WakeWakeUpTheme {
                WwuNavGraph(openStatsOnStart = openStats)
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_STATS = "extra_open_stats"
    }
}
