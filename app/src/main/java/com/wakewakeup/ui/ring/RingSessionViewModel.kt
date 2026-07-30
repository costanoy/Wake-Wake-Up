package com.wakewakeup.ui.ring

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.wakewakeup.session.AlarmRingService
import com.wakewakeup.session.AlarmSessionState
import com.wakewakeup.session.RingSession
import kotlinx.coroutines.flow.StateFlow

class RingSessionViewModel(application: Application) : AndroidViewModel(application) {

    val session: StateFlow<RingSession?> = AlarmSessionState.session

    fun startMission() = send(AlarmRingService.ACTION_START_MISSION)
    fun holdOn() = send(AlarmRingService.ACTION_HOLD_ON)
    fun pressKey(key: String) = send(AlarmRingService.ACTION_MISSION_KEY, key)
    fun confirm() = send(AlarmRingService.ACTION_MISSION_CONFIRM)
    fun giveUp() = send(AlarmRingService.ACTION_GIVE_UP)
    fun finishSession() = send(AlarmRingService.ACTION_FINISH)

    private fun send(action: String, key: String? = null) {
        AlarmRingService.sendAction(getApplication(), action, key)
    }
}
