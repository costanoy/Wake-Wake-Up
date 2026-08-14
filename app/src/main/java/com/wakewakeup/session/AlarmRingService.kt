package com.wakewakeup.session

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.wakewakeup.ALARM_NOTIFICATION_CHANNEL_ID
import com.wakewakeup.R
import com.wakewakeup.WakeWakeUpApplication
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.Difficulty
import com.wakewakeup.data.TaskType
import com.wakewakeup.data.WakeHistoryEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

private const val NOTIFICATION_ID = 42
private const val WAKE_LOCK_TAG = "wakewakeup:ring"
private const val WAKE_LOCK_TIMEOUT_MS = 15 * 60 * 1000L

class AlarmRingService : LifecycleService() {

    private val container get() = (application as WakeWakeUpApplication).container

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentAlarm: Alarm? = null
    private var tickAtMillis: Long = 0L
    private var holdUntilMillis: Long? = null
    private var tickerStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START_MISSION -> onStartMission()
            ACTION_HOLD_ON -> onHoldOn()
            ACTION_MISSION_KEY -> onMissionKey(intent.getStringExtra(EXTRA_KEY).orEmpty())
            ACTION_MISSION_SET_TYPED -> onMissionSetTyped(intent.getStringExtra(EXTRA_TYPED_VALUE).orEmpty())
            ACTION_MISSION_CONFIRM -> onMissionConfirm()
            ACTION_GIVE_UP -> onGiveUp()
            ACTION_FINISH -> onFinish()
            else -> onAlarmFired(intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L)
        }
        return START_STICKY
    }

    private fun onAlarmFired(alarmId: Long) {
        if (alarmId < 0) return
        lifecycleScope.launch {
            val alarm = container.alarmRepository.getById(alarmId) ?: return@launch
            currentAlarm = alarm
            // A non-repeating alarm only ever fires once — disable it the moment it rings
            // (not just when the ritual finishes), so a reboot before the user completes it
            // can't reschedule and fire it again.
            if (alarm.days.isEmpty()) {
                container.alarmRepository.setEnabled(alarm, false)
            }
            AlarmSessionState.start(alarm)
            acquireWakeLock()
            startForeground(NOTIFICATION_ID, buildNotification(alarm))
            startRingActivity()
            startAudio(level = 1)
            startTicker()
        }
    }

    private fun startRingActivity() {
        startActivity(
            Intent(this, RingActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            ),
        )
    }

    private fun buildNotification(alarm: Alarm): Notification {
        val fullScreenIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, ALARM_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_sun)
            .setContentTitle(getString(R.string.alarm_notification_title, alarm.label))
            .setContentText(getString(R.string.alarm_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .build()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun startAudio(level: Int) {
        stopAudio()
        val chosenUri = currentAlarm?.soundUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val defaultUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getValidRingtoneUri(this)
        // The saved sound might no longer be accessible (uninstalled app, revoked
        // permission) — fall back to the system default rather than staying silent.
        mediaPlayer = chosenUri?.let { buildPlayer(it, level) } ?: buildPlayer(defaultUri, level)
    }

    private fun buildPlayer(uri: Uri, level: Int): MediaPlayer? {
        val player = MediaPlayer()
        return try {
            player.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                isLooping = true
                setVolume(volumeForLevel(level), volumeForLevel(level))
                setDataSource(this@AlarmRingService, uri)
                prepare()
                start()
            }
        } catch (e: Exception) {
            player.release()
            null
        }
    }

    private fun stopAudio() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    private fun volumeForLevel(level: Int): Float =
        (0.40f + level.coerceIn(1, 4) * 0.20f).coerceAtMost(1f)

    private fun startTicker() {
        if (tickerStarted) return
        tickerStarted = true
        tickAtMillis = System.currentTimeMillis()
        lifecycleScope.launch {
            while (true) {
                delay(1000)
                val session = AlarmSessionState.session.value ?: break
                when (session.screen) {
                    SessionScreen.RINGING -> tickRinging(session)
                    SessionScreen.MISSION -> tickMission(session)
                    SessionScreen.GOOD_MORNING -> Unit
                }
            }
        }
    }

    private fun tickRinging(session: RingSession) {
        val holdUntil = holdUntilMillis
        val now = System.currentTimeMillis()
        if (holdUntil != null) {
            if (now >= holdUntil) {
                holdUntilMillis = null
                tickAtMillis = now
                AlarmSessionState.update { it.copy(level = 1, holdUntilMillis = null) }
                startAudio(level = 1)
            }
            return
        }
        if (session.level < 4 && now - tickAtMillis >= 4000) {
            val nextLevel = session.level + 1
            tickAtMillis = now
            AlarmSessionState.update { it.copy(level = nextLevel) }
            mediaPlayer?.setVolume(volumeForLevel(nextLevel), volumeForLevel(nextLevel))
        }
    }

    private fun tickMission(session: RingSession) {
        val mission = session.mission ?: return
        val secondsLeft = mission.secondsLeft - 1
        if (secondsLeft <= 0) {
            val alarm = currentAlarm ?: return
            tickAtMillis = System.currentTimeMillis()
            AlarmSessionState.update {
                it.copy(screen = SessionScreen.RINGING, level = 3, mission = freshMission(alarm))
            }
            startAudio(level = 3)
        } else {
            AlarmSessionState.update { it.copy(mission = mission.copy(secondsLeft = secondsLeft)) }
        }
    }

    private fun freshMission(alarm: Alarm, type: TaskType = alarm.taskType): MissionState {
        val q = MissionGenerator.mathQuestion(alarm.difficulty)
        return MissionState(
            type = type,
            index = 0,
            count = alarm.taskCount,
            question = q.question,
            answer = q.answer,
            phrase = MissionGenerator.randomPhrase(),
        )
    }

    private fun onStartMission() {
        val alarm = currentAlarm ?: return
        holdUntilMillis = null
        stopAudio()
        AlarmSessionState.update { session ->
            session.copy(
                screen = SessionScreen.MISSION,
                mission = session.mission ?: freshMission(alarm),
            )
        }
    }

    private fun onHoldOn() {
        if (holdUntilMillis != null) return
        val until = System.currentTimeMillis() + HOLD_ON_SECONDS * 1000L
        holdUntilMillis = until
        stopAudio()
        AlarmSessionState.update { it.copy(waits = it.waits + 1, holdUntilMillis = until) }
    }

    private fun onMissionKey(key: String) {
        AlarmSessionState.update { session ->
            val mission = session.mission ?: return@update session
            val maxLen = if (mission.type == TaskType.PHRASE) 40 else 6
            val typed = when (key) {
                "⌫" -> mission.typed.dropLast(1)
                "C" -> ""
                else -> if (mission.typed.length < maxLen) {
                    mission.typed + if (mission.type == TaskType.PHRASE) key.lowercase(Locale.getDefault()) else key
                } else mission.typed
            }
            session.copy(mission = mission.copy(typed = typed, wrong = false))
        }
    }

    private fun onMissionSetTyped(text: String) {
        AlarmSessionState.update { session ->
            val mission = session.mission ?: return@update session
            val maxLen = if (mission.type == TaskType.PHRASE) 40 else 6
            session.copy(mission = mission.copy(typed = text.take(maxLen), wrong = false))
        }
    }

    private fun onMissionConfirm() {
        val alarm = currentAlarm ?: return
        val session = AlarmSessionState.session.value ?: return
        val mission = session.mission ?: return
        val correct = if (mission.type == TaskType.PHRASE) {
            mission.typed.trim().lowercase(Locale.getDefault()) == mission.phrase.lowercase(Locale.getDefault())
        } else {
            mission.typed == mission.answer
        }
        if (!correct) {
            AlarmSessionState.update { it.copy(mission = mission.copy(wrong = true)) }
            return
        }
        if (mission.type == TaskType.MATH && mission.index + 1 < mission.count) {
            val q = MissionGenerator.mathQuestion(alarm.difficulty)
            AlarmSessionState.update {
                it.copy(mission = mission.copy(index = mission.index + 1, question = q.question, answer = q.answer, typed = "", wrong = false))
            }
            return
        }
        finishSession(session)
    }

    private fun onGiveUp() {
        AlarmSessionState.update { session ->
            val secondsLeft = session.mission?.secondsLeft ?: MISSION_TOTAL_SECONDS
            session.copy(mission = MissionState(type = TaskType.PHRASE, index = 0, count = 1, secondsLeft = secondsLeft, phrase = MissionGenerator.randomPhrase()))
        }
    }

    private fun finishSession(session: RingSession) {
        val now = System.currentTimeMillis()
        // Real wall-clock time already includes however long any "hold on" pauses lasted —
        // don't add HOLD_ON_SECONDS per wait on top of that, or it double-counts.
        val elapsedSec = ((now - session.startedAtMillis) / 1000).toInt()
        val nowTime = LocalTime.now()
        val took = getString(R.string.duration_min_sec, elapsedSec / 60, elapsedSec % 60)
        AlarmSessionState.update {
            it.copy(
                screen = SessionScreen.GOOD_MORNING,
                finished = FinishedInfo(
                    tookText = took,
                    waits = it.waits,
                    alarmHour = it.alarmHour,
                    alarmMinute = it.alarmMinute,
                    wokeHour = nowTime.hour,
                    wokeMinute = nowTime.minute,
                    durationSec = elapsedSec,
                ),
            )
        }
        stopAudio()
        holdUntilMillis = null
    }

    private fun onFinish() {
        val finished = AlarmSessionState.session.value?.finished
        lifecycleScope.launch {
            if (finished != null) {
                container.wakeHistoryRepository.record(
                    WakeHistoryEntry(
                        date = LocalDate.now(),
                        alarmHour = finished.alarmHour,
                        alarmMinute = finished.alarmMinute,
                        wokeHour = finished.wokeHour,
                        wokeMinute = finished.wokeMinute,
                        waits = finished.waits,
                        durationSec = finished.durationSec,
                    ),
                )
            }
            AlarmSessionState.clear()
            stopSelfSafely()
        }
    }

    private fun stopSelfSafely() {
        stopAudio()
        wakeLock?.let { if (it.isHeld) it.release() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopAudio()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_MISSION = "com.wakewakeup.action.START_MISSION"
        const val ACTION_HOLD_ON = "com.wakewakeup.action.HOLD_ON"
        const val ACTION_MISSION_KEY = "com.wakewakeup.action.MISSION_KEY"
        const val ACTION_MISSION_SET_TYPED = "com.wakewakeup.action.MISSION_SET_TYPED"
        const val ACTION_MISSION_CONFIRM = "com.wakewakeup.action.MISSION_CONFIRM"
        const val ACTION_GIVE_UP = "com.wakewakeup.action.GIVE_UP"
        const val ACTION_FINISH = "com.wakewakeup.action.FINISH"
        const val EXTRA_KEY = "extra_key"
        const val EXTRA_TYPED_VALUE = "extra_typed_value"

        fun sendAction(context: Context, action: String, key: String? = null) {
            val intent = Intent(context, AlarmRingService::class.java).setAction(action)
            if (key != null) intent.putExtra(EXTRA_KEY, key)
            context.startService(intent)
        }

        fun sendTypedText(context: Context, text: String) {
            val intent = Intent(context, AlarmRingService::class.java)
                .setAction(ACTION_MISSION_SET_TYPED)
                .putExtra(EXTRA_TYPED_VALUE, text)
            context.startService(intent)
        }
    }
}
