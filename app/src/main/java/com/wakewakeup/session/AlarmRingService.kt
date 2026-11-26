package com.wakewakeup.session

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.UserManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.wakewakeup.ALARM_NOTIFICATION_CHANNEL_ID
import com.wakewakeup.R
import com.wakewakeup.WakeWakeUpApplication
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.TaskType
import com.wakewakeup.data.WakeHistoryEntry
import com.wakewakeup.ui.components.formatClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.ceil

private const val WAKE_LOCK_TAG = "wakewakeup:ring"
private const val WAKE_LOCK_TIMEOUT_MS = 15 * 60 * 1000L
private const val STREAM_TIMEOUT_MS = 8_000L

/** A sound chosen from the phone has this long to start before the next one in the chain takes over. */
private const val LOCAL_PREPARE_TIMEOUT_MS = 3_000L

/** While ringing, the alarm stream is kept at least this fraction of its maximum. */
private const val AUDIBLE_FLOOR = 0.6f

private const val TAG = "AlarmRingService"
private const val ALARM_COLOR = 0xFFE8663C.toInt()
private const val RADIO_GRACE_MS = 3 * 60 * 1000L

/** Bom dia closes by itself after this long, so the screen and the session don't stay up all day. */
private const val GOOD_MORNING_MS = 10 * 60 * 1000L

/** On 0.8 s, off 0.6 s, for as long as it rings. */
private val VIBRATION_PATTERN = longArrayOf(0, 800, 600)

class AlarmRingService : LifecycleService() {

    private val container get() = (application as WakeWakeUpApplication).container

    private var mediaPlayer: MediaPlayer? = null
    private var streamTimeout: Job? = null
    private var bufferingTimeout: Job? = null
    private var prepareTimeout: Job? = null
    private var originalAlarmVolume: Int? = null
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
            val alarm = try {
                container.alarmRepository.getById(alarmId)
            } catch (e: Exception) {
                // A closed database reports itself as a cancellation: only this coroutine's own
                // cancellation (the service going away) is passed on.
                currentCoroutineContext().ensureActive()
                // The alarm can't be read (full disk, damaged database, phone still locked): an
                // alarm that rings with the default sound beats one that stays silent.
                Log.e(TAG, "Could not read alarm $alarmId; ringing a minimal one", e)
                val time = LocalTime.now()
                Alarm(id = alarmId, hour = time.hour, minute = time.minute, label = "", days = emptySet(), enabled = false)
            }
            if (alarm == null) {
                // Deleted in the meantime. Another alarm may still be ringing, though.
                if (AlarmSessionState.session.value == null) {
                    RingWatchdog.clear(this@AlarmRingService)
                    stopSelfSafely()
                }
                return@launch
            }
            // From here to the sound there is no suspension: two alarms due in the same minute
            // can't interleave, and nothing slow (the database) comes before the sound.
            val previous = AlarmSessionState.session.value?.takeIf { resumedFrom == null && it.alarmId != alarm.id }
            if (previous?.screen == SessionScreen.MISSION) {
                // The ritual of the first alarm is under way: leave it alone.
                cleanUpAfterRing(alarm)
                return@launch
            }
            currentAlarm = alarm
            // Another alarm may still be in its Espere: this one starts its own session, which
            // keeps the waits and the clock of the first (the ritual is one, however many ring).
            holdUntilMillis = null
            goodMorningTimeout?.cancel()
            val now = System.currentTimeMillis()
            if (previous?.screen == SessionScreen.RINGING) {
                AlarmSessionState.start(alarm, startedAtMillis = previous.startedAtMillis, waits = previous.waits)
                AlarmSessionState.update { it.copy(alarmHour = previous.alarmHour, alarmMinute = previous.alarmMinute) }
            } else {
                AlarmSessionState.start(alarm, startedAtMillis = resumedFrom?.startedAtMillis ?: now, waits = resumedFrom?.waits ?: 0)
            }
            acquireWakeLock()
            val hold = resumedFrom?.holdUntilMillis?.takeIf { it > now }
            if (hold != null) {
                // The process died during an Espere: let the rest of it run out first.
                holdUntilMillis = hold
                AlarmSessionState.update { it.copy(holdUntilMillis = hold) }
            } else {
                startAudio(AlarmSessionState.session.value?.volume ?: 1f)
                startRingActivity()
            }
            refreshNotification()
            AlarmSessionState.session.value?.let { RingWatchdog.save(this@AlarmRingService, it) }
            startTicker()
            cleanUpAfterRing(alarm)
        }
    }

    /**
     * What an alarm ringing leaves to do, once it rings: nothing here can delay the sound, and
     * each step is on its own so one failing doesn't cost the others.
     */
    private fun cleanUpAfterRing(alarm: Alarm) {
        if (!UserManagerCompat.isUserUnlocked(this)) {
            Log.i(TAG, "Phone still locked: the clean-up after the ring is left for later")
            return
        }
        container.appScope.launch {
            // Whatever the pre-alarm notification asked about, an alarm is ringing now.
            runCatching { container.preAlarm.cancelOffer() }
            // A non-repeating alarm only ever fires once: disable it the moment it rings (not
            // just when the ritual finishes), so a reboot before the user completes it can't
            // reschedule and fire it again.
            if (alarm.days.isEmpty() && alarm.enabled) {
                try {
                    container.alarmRepository.setEnabled(alarm, false)
                } catch (e: Exception) {
                    currentCoroutineContext().ensureActive()
                    Log.w(TAG, "Could not switch off the one-time alarm ${alarm.id}", e)
                }
            }
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
            .setColor(ALARM_COLOR)
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
            .setColor(ALARM_COLOR)
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
        ensureAudible()
        val chosenUri = currentAlarm?.soundUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        when {
            chosenUri == null -> playDefault(volume)
            chosenUri.isStream() -> startStream(chosenUri, volume)
            // The saved sound might no longer be accessible (uninstalled app, revoked
            // permission): the chain goes on to the system default, then the bell of the app.
            else -> startSound(chosenUri, volume) { playDefault(sessionVolume(volume)) }
        }
    }

    private fun sessionVolume(fallback: Float) = AlarmSessionState.session.value?.volume ?: fallback

    /**
     * The system's default alarm sound, or — when there's none (set to silent, a file that was
     * deleted, a phone without ringtones) — the bell bundled with the app, which always plays.
     */
    private fun playDefault(volume: Float) {
        val uri = runCatching { defaultAlarmUri() }.getOrNull()
        if (uri == null) {
            playBell(volume)
        } else {
            startSound(uri, volume) { playBell(sessionVolume(volume)) }
        }
    }

    /** The end of the chain: nowhere left to fall to, so a failure here is only logged. */
    private fun playBell(volume: Float) {
        val bell = Uri.parse("android.resource://$packageName/${R.raw.campainha_mecanica}")
        startBundled(bell, volume) { Log.e(TAG, "Even the bundled bell failed to play") }
    }

    private fun defaultAlarmUri(): Uri? =
        RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getValidRingtoneUri(this)

    private fun Uri.isStream() = scheme == "http" || scheme == "https"

    private fun Uri.isBundled() = scheme == "android.resource"

    /**
     * Internet radio as the alarm sound. A stream can't be trusted to be there at wake-up time
     * (no connection, station offline, stream drops), so anything short of audio actually
     * playing within [STREAM_TIMEOUT_MS] — or a stream that later errors, ends or stalls for
     * that long — falls back to the system's default alarm sound. An alarm must never stay silent.
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
            bufferingTimeout?.cancel()
            runCatching { player.release() }
            mediaPlayer = null
            if (fullVolume) AlarmSessionState.update { it.copy(volume = 1f) }
            playDefault(sessionVolume(volume))
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
                val current = sessionVolume(volume)
                it.setVolume(current, current)
                it.start()
                streamTimeout = lifecycleScope.launch {
                    delay(RADIO_GRACE_MS)
                    fallback(fullVolume = true)
                }
            }
            player.setOnErrorListener { _, _, _ -> fallback(); true }
            player.setOnCompletionListener { fallback() }
            // A stream that stops at "loading" without an error stays silent: it gets the same
            // time as the first connection.
            player.setOnInfoListener { _, what, _ ->
                when (what) {
                    MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                        bufferingTimeout?.cancel()
                        bufferingTimeout = lifecycleScope.launch {
                            delay(STREAM_TIMEOUT_MS)
                            fallback()
                        }
                    }
                    MediaPlayer.MEDIA_INFO_BUFFERING_END -> bufferingTimeout?.cancel()
                }
                false
            }
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

    private fun newLoopingPlayer(volume: Float): MediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        isLooping = true
        setVolume(volume, volume)
    }

    /** A sound of the app itself (inside the APK), or a file or provider of the phone. */
    private fun startSound(uri: Uri, volume: Float, onFail: () -> Unit) {
        if (uri.isBundled()) startBundled(uri, volume, onFail) else startLocal(uri, volume, onFail)
    }

    /**
     * A sound from inside the APK: quick and certain, so it is prepared right here. The only
     * path that still prepares synchronously.
     */
    private fun startBundled(uri: Uri, volume: Float, onFail: () -> Unit) {
        val player = newLoopingPlayer(volume)
        try {
            player.setOnErrorListener { failed, _, _ ->
                if (mediaPlayer === failed) {
                    runCatching { failed.release() }
                    mediaPlayer = null
                    onFail()
                }
                true
            }
            player.setDataSource(this, uri)
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (e: Exception) {
            runCatching { player.release() }
            onFail()
        }
    }

    /**
     * A file or provider of the phone (a song from the picker, the system's default sound): it
     * may live in the cloud or on a card that went away, so nothing here blocks the main thread
     * and [LOCAL_PREPARE_TIMEOUT_MS] is all it gets to start before [onFail] takes over.
     */
    private fun startLocal(uri: Uri, volume: Float, onFail: () -> Unit) {
        val player = newLoopingPlayer(volume)
        mediaPlayer = player
        fun fail() {
            if (mediaPlayer !== player) return
            prepareTimeout?.cancel()
            runCatching { player.release() }
            mediaPlayer = null
            onFail()
        }
        player.setOnErrorListener { _, _, _ -> fail(); true }
        player.setOnPreparedListener {
            if (mediaPlayer !== player) return@setOnPreparedListener
            prepareTimeout?.cancel()
            val current = sessionVolume(volume)
            it.setVolume(current, current)
            it.start()
        }
        prepareTimeout = lifecycleScope.launch {
            delay(LOCAL_PREPARE_TIMEOUT_MS)
            fail()
        }
        lifecycleScope.launch {
            val opened = try {
                withContext(Dispatchers.IO) { player.setDataSource(this@AlarmRingService, uri) }
                true
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                false
            }
            if (mediaPlayer !== player) return@launch
            if (!opened) {
                fail()
                return@launch
            }
            try {
                player.prepareAsync()
            } catch (e: Exception) {
                fail()
            }
        }
    }

    private fun stopAudio() {
        stopVibration()
        streamTimeout?.cancel()
        streamTimeout = null
        bufferingTimeout?.cancel()
        bufferingTimeout = null
        prepareTimeout?.cancel()
        prepareTimeout = null
        mediaPlayer?.apply {
            // A stream may still be preparing, where stop() isn't a valid call.
            runCatching { if (isPlaying) stop() }
            runCatching { release() }
        }
        mediaPlayer = null
    }

    /**
     * An alarm nobody can hear isn't one: while it rings, the alarm stream is lifted to a floor
     * of [AUDIBLE_FLOOR] of its maximum, remembering the level the person had so it can be put
     * back when the ringing ends.
     */
    private fun ensureAudible() {
        runCatching {
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val floor = ceil(audio.getStreamMaxVolume(AudioManager.STREAM_ALARM) * AUDIBLE_FLOOR).toInt()
            val now = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            // A service that took over from one the process lost finds the volume already lifted:
            // the level the person had was written down by the first one.
            if (originalAlarmVolume == null) originalAlarmVolume = RingWatchdog.originalVolume(this)
            if (now < floor) {
                if (originalAlarmVolume == null) {
                    originalAlarmVolume = now
                    RingWatchdog.saveOriginalVolume(this, now)
                }
                audio.setStreamVolume(AudioManager.STREAM_ALARM, floor, 0)
            }
        }
    }

    private fun restoreAlarmVolume() {
        val original = originalAlarmVolume ?: RingWatchdog.originalVolume(this) ?: return
        originalAlarmVolume = null
        RingWatchdog.forgetOriginalVolume(this)
        runCatching {
            (getSystemService(Context.AUDIO_SERVICE) as AudioManager).setStreamVolume(AudioManager.STREAM_ALARM, original, 0)
        }
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
        ensureAudible()
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
        if (AlarmSessionState.session.value?.screen != SessionScreen.RINGING) return
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
        if (AlarmSessionState.session.value?.screen != SessionScreen.RINGING) return
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
            if (session.screen != SessionScreen.MISSION) return@update session
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
            if (session.screen != SessionScreen.MISSION) return@update session
            val mission = session.mission ?: return@update session
            session.copy(mission = mission.copy(typed = text.take(MAX_PHRASE_LENGTH), wrong = false))
        }
    }

    /**
     * [typedNow] is the phrase as the screen has it: sent along with the confirmation, the check
     * never races a keystroke still on its way here.
     */
    private fun onMissionConfirm(typedNow: String? = null) {
        if (AlarmSessionState.session.value?.screen != SessionScreen.MISSION) return
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
                it.copy(mission = mission.copy(index = mission.index + 1, question = q.question, answer = q.answer, typed = "", wrong = false, justCorrect = true, secondsLeft = MISSION_TOTAL_SECONDS))
            }
            return
        }
        finishSession(session)
    }

    private fun onGiveUp() {
        AlarmSessionState.update { session ->
            if (session.screen != SessionScreen.MISSION) return@update session
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
        restoreAlarmVolume()
        holdUntilMillis = null
        RingWatchdog.clear(this)
        dismissResurface()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (UserManagerCompat.isUserUnlocked(this)) {
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
        } else {
            // The database is shut until the first unlock: this wake-up can't be written.
            Log.i(TAG, "Phone still locked: the wake-up is not recorded")
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
        if (UserManagerCompat.isUserUnlocked(this)) container.appScope.launch { container.preAlarm.reschedule() }
        stopSelfSafely()
    }

    private fun stopSelfSafely() {
        stopAudio()
        restoreAlarmVolume()
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
