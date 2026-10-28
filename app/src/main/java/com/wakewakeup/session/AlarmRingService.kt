package com.wakewakeup.session

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.wakewakeup.ALARM_NOTIFICATION_CHANNEL_ID
import com.wakewakeup.R
import com.wakewakeup.WakeWakeUpApplication
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.TaskType
import com.wakewakeup.data.WakeHistoryEntry
import com.wakewakeup.ui.components.formatClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private const val WAKE_LOCK_TAG = "wakewakeup:ring"
private const val WAKE_LOCK_TIMEOUT_MS = 15 * 60 * 1000L
private const val STREAM_TIMEOUT_MS = 8_000L
private const val RADIO_GRACE_MS = 3 * 60 * 1000L

/** Bom dia closes by itself after this long, so the screen and the session don't stay up all day. */
private const val GOOD_MORNING_MS = 10 * 60 * 1000L

/** On 0.8 s, off 0.6 s, for as long as it rings. */
private val VIBRATION_PATTERN = longArrayOf(0, 800, 600)

class AlarmRingService : LifecycleService() {

    private val container get() = (application as WakeWakeUpApplication).container

    private var mediaPlayer: MediaPlayer? = null
    private var streamTimeout: Job? = null
    private var goodMorningTimeout: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentAlarm: Alarm? = null
    private var holdUntilMillis: Long? = null
    private var tickerStarted = false
    private var vibrating = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START_MISSION -> onStartMission()
            ACTION_HOLD_ON -> onHoldOn()
            ACTION_MISSION_KEY -> onMissionKey(intent.getStringExtra(EXTRA_KEY).orEmpty())
            ACTION_MISSION_SET_TYPED -> onMissionSetTyped(intent.getStringExtra(EXTRA_TYPED_VALUE).orEmpty())
            ACTION_MISSION_CONFIRM -> onMissionConfirm(intent.getStringExtra(EXTRA_TYPED_VALUE))
            ACTION_GIVE_UP -> onGiveUp()
            ACTION_FINISH -> onFinish()
            else -> onRingStart(intent)
        }
        return START_STICKY
    }

    /**
     * An alarm came due (AlarmReceiver), or a ringing session the app's process lost is picked up
     * again: by the watchdog, after a restart of the phone, or by Android restarting this sticky
     * service with no intent at all.
     */
    private fun onRingStart(intent: Intent?) {
        val saved = RingWatchdog.saved(this)
        val resumed = intent == null || intent.getBooleanExtra(EXTRA_RESUMED_SESSION, false)
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L)?.takeIf { it >= 0 } ?: saved?.alarmId ?: -1L
        // Into the foreground before anything that can take a while (the database): Android
        // gives a service started this way only a few seconds.
        val foreground = goForeground(buildNotification(AlarmSessionState.session.value))
        if (alarmId < 0 || (!foreground && intent == null)) {
            // Nothing to ring, or a restart that isn't allowed to ring from here: the watchdog
            // tries again in a few minutes.
            if (AlarmSessionState.session.value == null) stopSelfSafely()
            return
        }
        onAlarmFired(alarmId, if (resumed) saved?.takeIf { it.alarmId == alarmId } else null)
    }

    private fun onAlarmFired(alarmId: Long, resumedFrom: RingWatchdog.Saved?) {
        lifecycleScope.launch {
            val alarm = container.alarmRepository.getById(alarmId)
            if (alarm == null) {
                // Deleted in the meantime. Another alarm may still be ringing, though.
                if (AlarmSessionState.session.value == null) {
                    RingWatchdog.clear(this@AlarmRingService)
                    stopSelfSafely()
                }
                return@launch
            }
            currentAlarm = alarm
            // Another alarm may still be in its Espere: this one starts its own fresh session.
            holdUntilMillis = null
            goodMorningTimeout?.cancel()
            // Whatever the pre-alarm notification asked about, an alarm is ringing now.
            container.preAlarm.cancelOffer()
            // A non-repeating alarm only ever fires once: disable it the moment it rings (not
            // just when the ritual finishes), so a reboot before the user completes it can't
            // reschedule and fire it again.
            if (alarm.days.isEmpty() && alarm.enabled) {
                container.alarmRepository.setEnabled(alarm, false)
            }
            val now = System.currentTimeMillis()
            AlarmSessionState.start(alarm, startedAtMillis = resumedFrom?.startedAtMillis ?: now, waits = resumedFrom?.waits ?: 0)
            acquireWakeLock()
            val hold = resumedFrom?.holdUntilMillis?.takeIf { it > now }
            if (hold != null) {
                // The process died during an Espere: let the rest of it run out first.
                holdUntilMillis = hold
                AlarmSessionState.update { it.copy(holdUntilMillis = hold) }
            } else {
                startRingActivity()
                startAudio(AlarmSessionState.session.value?.volume ?: 1f)
            }
            refreshNotification()
            AlarmSessionState.session.value?.let { RingWatchdog.save(this@AlarmRingService, it) }
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

    private fun ringScreenIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** The ongoing notification, saying what the alarm is doing right now. */
    private fun buildNotification(session: RingSession?): Notification {
        val label = session?.alarmLabel?.ifBlank { null } ?: getString(R.string.untitled_alarm)
        val hold = session?.holdUntilMillis
        val screenIntent = ringScreenIntent()
        val builder = NotificationCompat.Builder(this, ALARM_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_sun)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(screenIntent)
        when {
            hold != null -> builder
                .setContentTitle(getString(R.string.espere_active))
                .setContentText(getString(R.string.espere_notification_text, clockOf(hold)))
                .setWhen(hold)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
            session?.screen == SessionScreen.MISSION -> builder
                .setContentTitle(getString(R.string.mission_notification_title))
                .setContentText(getString(R.string.mission_notification_text))
            else -> builder
                .setContentTitle(getString(R.string.alarm_notification_title, label))
                .setContentText(getString(R.string.alarm_notification_text))
                .setFullScreenIntent(screenIntent, true)
        }
        return builder.build()
    }

    private fun refreshNotification() {
        if (AlarmSessionState.session.value?.screen == SessionScreen.GOOD_MORNING) return
        goForeground(buildNotification(AlarmSessionState.session.value))
    }

    /**
     * Foreground as the kind of service Android sets aside for alarm-clock apps ("system
     * exempted", for apps holding the exact-alarm permission). Also updates the notification.
     */
    private fun goForeground(notification: Notification): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NotificationIds.RINGING, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
        } else {
            startForeground(NotificationIds.RINGING, notification)
        }
        true
    } catch (e: RuntimeException) {
        // Not allowed from here right now (a restart from the background on Android 12+).
        false
    }

    /**
     * Ringing again after an Espere or a mission that ran out of time, while the ring screen may
     * be gone (another app, the phone locked): bring it back like at the first ring.
     */
    private fun resurface() {
        startRingActivity()
        val notification = NotificationCompat.Builder(this, ALARM_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_sun)
            .setContentTitle(getString(R.string.alarm_notification_title, currentAlarm?.label?.ifBlank { null } ?: getString(R.string.untitled_alarm)))
            .setContentText(getString(R.string.alarm_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(ringScreenIntent(), true)
            .setContentIntent(ringScreenIntent())
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(NotificationIds.RE_RING, notification)
        } catch (e: SecurityException) {
            // No notification permission: starting the ring screen is all there is.
        }
    }

    private fun dismissResurface() {
        NotificationManagerCompat.from(this).cancel(NotificationIds.RE_RING)
    }

    /**
     * Keeps the CPU running so the ticker's timers (Espere, mission, volume ramp) fire even with
     * the screen off. Renewed whenever the sound stops for a while (Espere, mission), since the
     * timeout counts from the moment it's taken and nothing else keeps the phone awake then.
     */
    private fun acquireWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    private fun startAudio(volume: Float) {
        stopAudio()
        startVibration()
        val chosenUri = currentAlarm?.soundUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (chosenUri != null && chosenUri.isStream()) {
            startStream(chosenUri, volume)
            return
        }
        // The saved sound might no longer be accessible (uninstalled app, revoked
        // permission) — fall back to the system default rather than staying silent.
        mediaPlayer = chosenUri?.let { buildPlayer(it, volume) } ?: buildFallbackPlayer(volume)
    }

    /**
     * The system's default alarm sound, or — when there's none (set to silent, a file that was
     * deleted, a phone without ringtones) — the bell bundled with the app, which always plays.
     */
    private fun buildFallbackPlayer(volume: Float): MediaPlayer? =
        defaultAlarmUri()?.let { buildPlayer(it, volume) }
            ?: buildPlayer(Uri.parse("android.resource://$packageName/${R.raw.campainha_mecanica}"), volume)

    private fun defaultAlarmUri(): Uri? =
        RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getValidRingtoneUri(this)

    private fun Uri.isStream() = scheme == "http" || scheme == "https"

    /**
     * Internet radio as the alarm sound. A stream can't be trusted to be there at wake-up time
     * (no connection, station offline, stream drops), so anything short of audio actually
     * playing within [STREAM_TIMEOUT_MS] — or a stream that later errors or ends — falls back
     * to the system's default alarm sound. An alarm must never stay silent.
     *
     * A radio that does play is still easy to sleep through, so if it has been on for
     * [RADIO_GRACE_MS] with no reaction (no Espere, no mission started — either would have
     * stopped this player), the real alarm takes over at full volume.
     */
    private fun startStream(uri: Uri, volume: Float) {
        val player = MediaPlayer()
        mediaPlayer = player
        fun fallback(fullVolume: Boolean = false) {
            if (mediaPlayer !== player) return
            streamTimeout?.cancel()
            runCatching { player.release() }
            if (fullVolume) AlarmSessionState.update { it.copy(volume = 1f) }
            val current = AlarmSessionState.session.value?.volume ?: volume
            mediaPlayer = buildFallbackPlayer(current)
        }
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            player.setVolume(volume, volume)
            player.setDataSource(this, uri)
            player.setOnPreparedListener {
                if (mediaPlayer !== player) return@setOnPreparedListener
                streamTimeout?.cancel()
                val current = AlarmSessionState.session.value?.volume ?: volume
                it.setVolume(current, current)
                it.start()
                streamTimeout = lifecycleScope.launch {
                    delay(RADIO_GRACE_MS)
                    fallback(fullVolume = true)
                }
            }
            player.setOnErrorListener { _, _, _ -> fallback(); true }
            player.setOnCompletionListener { fallback() }
            player.prepareAsync()
        } catch (e: Exception) {
            fallback()
            return
        }
        streamTimeout = lifecycleScope.launch {
            delay(STREAM_TIMEOUT_MS)
            fallback()
        }
    }

    private fun buildPlayer(uri: Uri, volume: Float): MediaPlayer? {
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
                setVolume(volume, volume)
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
        stopVibration()
        streamTimeout?.cancel()
        streamTimeout = null
        mediaPlayer?.apply {
            // A stream may still be preparing, where stop() isn't a valid call.
            runCatching { if (isPlaying) stop() }
            release()
        }
        mediaPlayer = null
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    /** Vibrates along with the sound, as an alarm (so it goes through silent mode too). */
    private fun startVibration() {
        val vibrator = vibrator()?.takeIf { it.hasVibrator() } ?: return
        val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
        vibrating = true
    }

    private fun stopVibration() {
        if (!vibrating) return
        vibrating = false
        vibrator()?.cancel()
    }

    /** Volume at the start of a stretch of ringing: low when ramping up, full otherwise. */
    private fun startVolume(session: RingSession?, atLeast: Float = 0f): Float =
        if (session?.rampUp == false) 1f else maxOf(RAMP_START_VOLUME, atLeast)

    private fun startTicker() {
        if (tickerStarted) return
        tickerStarted = true
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
            tickerStarted = false
        }
    }

    private fun tickRinging(session: RingSession) {
        val holdUntil = holdUntilMillis
        val now = System.currentTimeMillis()
        if (holdUntil != null) {
            if (now >= holdUntil) {
                holdUntilMillis = null
                val volume = startVolume(session)
                AlarmSessionState.update { it.copy(volume = volume, holdUntilMillis = null, ringingSinceMillis = now) }
                startAudio(volume)
                refreshNotification()
                resurface()
                AlarmSessionState.session.value?.let { RingWatchdog.save(this, it) }
            }
            return
        }
        // "Aumentar aos poucos": climb linearly from the start volume to full over RAMP_SECONDS.
        if (session.volume < 1f) {
            val step = (1f - RAMP_START_VOLUME) / RAMP_SECONDS
            val next = (session.volume + step).coerceAtMost(1f)
            AlarmSessionState.update { it.copy(volume = next) }
            mediaPlayer?.setVolume(next, next)
        }
    }

    private fun tickMission(session: RingSession) {
        val mission = session.mission ?: return
        val secondsLeft = mission.secondsLeft - 1
        if (secondsLeft <= 0) {
            val alarm = currentAlarm ?: return
            // Ran out of time: back to ringing, already fairly loud this time.
            val volume = startVolume(session, atLeast = 0.8f)
            AlarmSessionState.update {
                it.copy(
                    screen = SessionScreen.RINGING,
                    volume = volume,
                    ringingSinceMillis = System.currentTimeMillis(),
                    mission = freshMission(alarm),
                )
            }
            startAudio(volume)
            refreshNotification()
            resurface()
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
            phrase = MissionGenerator.phraseOfTheDay(),
        )
    }

    private fun onStartMission() {
        val alarm = currentAlarm ?: return
        holdUntilMillis = null
        stopAudio()
        dismissResurface()
        acquireWakeLock()
        AlarmSessionState.update { session ->
            session.copy(
                screen = SessionScreen.MISSION,
                holdUntilMillis = null,
                mission = session.mission ?: freshMission(alarm),
            )
        }
        refreshNotification()
    }

    private fun onHoldOn() {
        if (holdUntilMillis != null || currentAlarm == null) return
        val until = System.currentTimeMillis() + HOLD_ON_SECONDS * 1000L
        holdUntilMillis = until
        stopAudio()
        dismissResurface()
        acquireWakeLock()
        AlarmSessionState.update { it.copy(waits = it.waits + 1, holdUntilMillis = until) }
        refreshNotification()
        AlarmSessionState.session.value?.let { RingWatchdog.save(this, it) }
    }

    /** Calculator keys. After a wrong answer, the next key starts from an empty display. */
    private fun onMissionKey(key: String) {
        AlarmSessionState.update { session ->
            val mission = session.mission ?: return@update session
            val current = if (mission.wrong) "" else mission.typed
            val typed = when (key) {
                KEY_BACKSPACE -> current.dropLast(1)
                KEY_CLEAR -> ""
                KEY_SIGN -> if (current.startsWith("-")) current.drop(1) else "-$current"
                else -> if (key.all { it.isDigit() } && current.trimStart('-').length < MAX_ANSWER_DIGITS) current + key else current
            }
            session.copy(mission = mission.copy(typed = typed, wrong = false, justCorrect = false))
        }
    }

    private fun onMissionSetTyped(text: String) {
        AlarmSessionState.update { session ->
            val mission = session.mission ?: return@update session
            session.copy(mission = mission.copy(typed = text.take(MAX_PHRASE_LENGTH), wrong = false))
        }
    }

    /**
     * [typedNow] is the phrase as the screen has it: sent along with the confirmation, the check
     * never races a keystroke still on its way here.
     */
    private fun onMissionConfirm(typedNow: String? = null) {
        if (typedNow != null) onMissionSetTyped(typedNow)
        val alarm = currentAlarm ?: return
        val session = AlarmSessionState.session.value ?: return
        val mission = session.mission ?: return
        if (mission.typed.isBlank() || mission.typed == "-") return
        val correct = if (mission.type == TaskType.PHRASE) {
            MissionGenerator.phraseMatches(mission.typed, mission.phrase)
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
                it.copy(mission = mission.copy(index = mission.index + 1, question = q.question, answer = q.answer, typed = "", wrong = false, justCorrect = true))
            }
            return
        }
        finishSession(session)
    }

    private fun onGiveUp() {
        AlarmSessionState.update { session ->
            val secondsLeft = session.mission?.secondsLeft ?: MISSION_TOTAL_SECONDS
            session.copy(mission = MissionState(type = TaskType.PHRASE, index = 0, count = 1, secondsLeft = secondsLeft, phrase = MissionGenerator.phraseOfTheDay()))
        }
    }

    /**
     * The mission is done: the alarm is over. The wake-up is saved right away and nothing keeps
     * the phone awake or says "está tocando" anymore; Bom dia stays up until Concluir, Back,
     * or [GOOD_MORNING_MS].
     */
    private fun finishSession(session: RingSession) {
        val now = System.currentTimeMillis()
        // Real wall-clock time already includes however long any "hold on" pauses lasted —
        // don't add HOLD_ON_SECONDS per wait on top of that, or it double-counts.
        val elapsedSec = ((now - session.startedAtMillis) / 1000).toInt()
        val nowTime = LocalTime.now()
        val finished = FinishedInfo(
            waits = session.waits,
            alarmHour = session.alarmHour,
            alarmMinute = session.alarmMinute,
            wokeHour = nowTime.hour,
            wokeMinute = nowTime.minute,
            durationSec = elapsedSec,
        )
        AlarmSessionState.update { it.copy(screen = SessionScreen.GOOD_MORNING, finished = finished) }
        stopAudio()
        holdUntilMillis = null
        RingWatchdog.clear(this)
        dismissResurface()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        // Not tied to this service: a quick Concluir stops it before the write would be done.
        container.appScope.launch {
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
        goodMorningTimeout?.cancel()
        goodMorningTimeout = lifecycleScope.launch {
            delay(GOOD_MORNING_MS)
            onFinish()
        }
    }

    /** Concluir (or Bom dia timing out): the session ends. Safe to call more than once. */
    private fun onFinish() {
        goodMorningTimeout?.cancel()
        goodMorningTimeout = null
        AlarmSessionState.clear()
        RingWatchdog.clear(this)
        dismissResurface()
        // The pre-alarm check waited while the session was open.
        container.appScope.launch { container.preAlarm.reschedule() }
        stopSelfSafely()
    }

    private fun stopSelfSafely() {
        stopAudio()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopAudio()
        releaseWakeLock()
        // A Bom dia left open dies with the service; a ringing session lives on in the watchdog.
        if (AlarmSessionState.session.value?.screen == SessionScreen.GOOD_MORNING) AlarmSessionState.clear()
        super.onDestroy()
    }

    private fun clockOf(millis: Long): String {
        val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
        return formatClock(time.hour, time.minute)
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
        const val KEY_CLEAR = "C"
        const val KEY_BACKSPACE = "⌫"
        const val KEY_SIGN = "−"
        private const val MAX_ANSWER_DIGITS = 4
        private const val MAX_PHRASE_LENGTH = 80
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

        /** Confirms the mission's answer; a phrase comes with the text as typed on screen. */
        fun sendConfirm(context: Context, typed: String? = null) {
            val intent = Intent(context, AlarmRingService::class.java).setAction(ACTION_MISSION_CONFIRM)
            if (typed != null) intent.putExtra(EXTRA_TYPED_VALUE, typed)
            context.startService(intent)
        }
    }
}
