package com.wakewakeup

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.wakewakeup.session.ACTION_CONFIRM_SKIP
import com.wakewakeup.session.AlarmSessionState
import com.wakewakeup.session.EXTRA_SKIP_ATS
import com.wakewakeup.session.EXTRA_SKIP_IDS
import com.wakewakeup.session.RingActivity
import com.wakewakeup.session.SessionScreen
import com.wakewakeup.ui.navigation.SkipRequest
import com.wakewakeup.ui.navigation.WwuNavGraph
import com.wakewakeup.ui.permissions.AlarmPermissions
import com.wakewakeup.ui.theme.WakeWakeUpTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val app get() = application as WakeWakeUpApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark-themed regardless of the system setting, so force
        // light system-bar icons rather than letting them follow system light/dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        rearmAlarmsOnce()
        // What the launching intent asks for is acted on once: a recreated activity (rotation,
        // process death) gets its screens back from the saved navigation state instead.
        val fresh = savedInstanceState == null
        val openStats = fresh && intent?.getBooleanExtra(EXTRA_OPEN_STATS, false) == true
        val skipRequest = if (fresh) skipRequestOf(intent) else null
        if (skipRequest != null) app.container.preAlarm.cancelOffer()
        // First launch: walk through the permissions an alarm needs (notifications are asked
        // from there too) unless they're all already allowed.
        val showPermissions = !openStats && skipRequest == null && AlarmPermissions.shouldShowOnboarding(this)
        setContent {
            WakeWakeUpTheme {
                WwuNavGraph(openStatsOnStart = openStats, showPermissions = showPermissions, skipRequest = skipRequest)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        app.container.preAlarm.appSeen()
        // Picks up a notification permission or channel the user just changed in Settings.
        lifecycleScope.launch { app.container.preAlarm.reschedule() }
    }

    override fun onResume() {
        super.onResume()
        // While an alarm rings (or its Espere or mission runs), its screen is the way out.
        val session = AlarmSessionState.session.value
        if (session != null && session.screen != SessionScreen.GOOD_MORNING) {
            startActivity(Intent(this, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun onStop() {
        app.container.preAlarm.appSeen()
        super.onStop()
    }

    /**
     * AlarmManager forgets every alarm when the app is force-stopped or its data is restored
     * from a backup, while the list still shows them on. Arm them again whenever the app starts.
     */
    private fun rearmAlarmsOnce() {
        if (app.rearmedThisProcess) return
        app.rearmedThisProcess = true
        lifecycleScope.launch { app.container.alarmRepository.rescheduleAll(sparingDue = true) }
    }

    private fun skipRequestOf(intent: Intent?): SkipRequest? {
        if (intent?.action != ACTION_CONFIRM_SKIP) return null
        val ids = intent.getLongArrayExtra(EXTRA_SKIP_IDS) ?: return null
        val ats = intent.getLongArrayExtra(EXTRA_SKIP_ATS) ?: return null
        if (ids.isEmpty() || ids.size != ats.size) return null
        return SkipRequest(ids.toList(), ats.toList())
    }

    companion object {
        const val EXTRA_OPEN_STATS = "extra_open_stats"
    }
}
