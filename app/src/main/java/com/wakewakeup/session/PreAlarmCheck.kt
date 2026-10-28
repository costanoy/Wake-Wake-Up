package com.wakewakeup.session

import android.Manifest
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wakewakeup.MainActivity
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.AlarmDao
import com.wakewakeup.data.toDomain
import com.wakewakeup.ui.components.formatClock
import kotlinx.coroutines.delay

const val PRE_ALARM_CHANNEL_ID = "pre_alarm"
const val ACTION_PRE_ALARM_CHECK = "com.wakewakeup.action.PRE_ALARM_CHECK"
const val ACTION_CONFIRM_SKIP = "com.wakewakeup.action.CONFIRM_SKIP"
const val ACTION_UNDO_SKIP = "com.wakewakeup.action.UNDO_SKIP"
const val EXTRA_SKIP_IDS = "extra_skip_ids"
const val EXTRA_SKIP_ATS = "extra_skip_ats"

/** Every notification the app posts, in one place so no two ever share an id by accident. */
object NotificationIds {
    const val RINGING = 42
    const val PRE_ALARM = 43
    const val RE_RING = 44
    const val SKIP_RECEIPT = 45
}

// Request codes for the PendingIntents that aren't an alarm's own (those use the alarm id, > 0).
private const val CHECK_REQUEST_CODE = -1001
private const val CONFIRM_REQUEST_CODE = -1002
private const val UNDO_REQUEST_CODE = -1003

private const val FIRST_CHECK_WINDOW_MS = 10 * 60_000L
private const val SESSION_RETRY_MS = 5 * 60_000L
private const val UNLOCK_POLL_MS = 250L
private const val HANDLED_KEPT_MS = 24 * 60 * 60_000L

private const val PREFS = "pre_alarm"
private const val KEY_HANDLED = "handled"
private const val KEY_POSTED = "posted"
private const val KEY_APP_SEEN = "app_seen_at"

/**
 * "Você quer desativar o próximo alarme?": when the phone is already in use (screen on and
 * unlocked) in the hour before a morning alarm, offer once to turn that morning's ring(s) off.
 *
 * It never wakes the phone to find out: there is one device-wide, non-waking check in
 * AlarmManager. While the screen is off it simply waits, and Android delivers it the moment the
 * screen comes on (which also ends Doze). Confirming takes the user into the app and a quick sum
 * ([com.wakewakeup.ui.skip.SkipConfirmScreen]), so a reflexive tap can't silence an alarm.
 * The rules themselves live in [PreAlarmPlanner].
 */
class PreAlarmCheck(private val context: Context, private val dao: AlarmDao) {

    /** How long a check waits for a phone whose screen just came on to be unlocked. */
    var unlockWaitMs = 5_000L

    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Arms (or cancels) the next check. Runs after every change to the alarms and app start. */
    suspend fun reschedule(now: Long = System.currentTimeMillis()) {
        val alarms = enabledAlarms()
        dropStaleOffer(alarms, now)
        if (!notificationsAllowed()) {
            cancelCheck()
            return
        }
        val plan = PreAlarmPlanner.plan(alarms, now, handled(), appSeenAt())
        if (plan.quietlyHandled.isNotEmpty()) markHandled(plan.quietlyHandled, now)
        when {
            plan.offer.isNotEmpty() -> armAt(now + 1_000L)
            plan.nextCheckAt != null -> armFirstCheck(plan.nextCheckAt, now)
            else -> cancelCheck()
        }
    }

    /** The check itself, delivered to [PreAlarmReceiver]. */
    suspend fun check(now: Long = System.currentTimeMillis()) {
        if (!notificationsAllowed()) {
            cancelCheck()
            return
        }
        // Ringing, Espere, mission or Bom dia: never ask about turning alarms off over that.
        if (AlarmSessionState.session.value != null) {
            armAt(now + SESSION_RETRY_MS)
            return
        }
        val alarms = enabledAlarms()
        val plan = PreAlarmPlanner.plan(alarms, now, handled(), appSeenAt())
        if (plan.quietlyHandled.isNotEmpty()) markHandled(plan.quietlyHandled, now)
        if (plan.offer.isEmpty()) {
            if (plan.nextCheckAt != null) armFirstCheck(plan.nextCheckAt, now) else cancelCheck()
            return
        }
        if (waitUntilInUse()) {
            markHandled(plan.offer, now)
            postOffer(plan.offer, now)
            reschedule(now)
        } else if (now + PreAlarmPlanner.RECHECK_MS <= plan.offer.first().at - PreAlarmPlanner.CUTOFF_MS) {
            // Screen on but still locked, or off again: look again in a minute. A non-waking
            // alarm, so with the screen off this waits for the next time it comes on.
            armAt(now + PreAlarmPlanner.RECHECK_MS)
        } else {
            reschedule(now)
        }
    }

    /** MainActivity is showing: whatever is coming up, the user can see it there. */
    fun appSeen(now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_APP_SEEN, now).apply()
    }

    /** The receipt after skipping, with an undo that works without opening the app. */
    fun postReceipt(skipped: List<Occurrence>, now: Long = System.currentTimeMillis()) {
        if (skipped.isEmpty() || !notificationsAllowed()) return
        val first = skipped.first()
        val title = if (skipped.size == 1) {
            context.getString(R.string.skip_receipt_title_one, labelOf(first), formatClock(first.hour, first.minute))
        } else {
            context.getString(R.string.skip_receipt_title_many, clockList(skipped))
        }
        val undo = PendingIntent.getBroadcast(
            context, UNDO_REQUEST_CODE,
            Intent(context, PreAlarmReceiver::class.java)
                .setAction(ACTION_UNDO_SKIP)
                .putExtra(EXTRA_SKIP_IDS, skipped.map { it.alarmId }.toLongArray())
                .putExtra(EXTRA_SKIP_ATS, skipped.map { it.at }.toLongArray()),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, PRE_ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_sun)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.skip_receipt_text))
            .setSilent(true)
            .setLocalOnly(true)
            .setAutoCancel(true)
            .setTimeoutAfter((skipped.minOf { it.at } - now).coerceAtLeast(1_000L))
            .addAction(0, context.getString(R.string.undo), undo)
            .build()
        notify(NotificationIds.SKIP_RECEIPT, notification)
    }

    fun cancelOffer() {
        NotificationManagerCompat.from(context).cancel(NotificationIds.PRE_ALARM)
        prefs.edit().remove(KEY_POSTED).apply()
    }

    private fun postOffer(offer: List<Occurrence>, now: Long) {
        val first = offer.first()
        val title = context.getString(if (offer.size == 1) R.string.pre_alarm_title_one else R.string.pre_alarm_title_many)
        val text = if (offer.size == 1) {
            context.getString(R.string.pre_alarm_text_one, labelOf(first), formatClock(first.hour, first.minute))
        } else {
            context.getString(R.string.pre_alarm_text_many, clockList(offer))
        }
        // An activity, not a broadcast: confirming needs the phone unlocked and a sum solved.
        val confirm = PendingIntent.getActivity(
            context, CONFIRM_REQUEST_CODE, confirmSkipIntent(context, offer),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, PRE_ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_sun)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            // Never on a lock screen: whoever sees it is already in the phone.
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setLocalOnly(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setTimeoutAfter((first.at - now).coerceAtLeast(1_000L))
            .setContentIntent(confirm)
            .addAction(0, context.getString(R.string.pre_alarm_action), confirm)
            .build()
        notify(NotificationIds.PRE_ALARM, notification)
        prefs.edit().putStringSet(KEY_POSTED, offer.map { it.key }.toSet()).apply()
    }

    /** A posted offer whose rings changed (edited, switched off, skipped, rang) is withdrawn. */
    private fun dropStaleOffer(alarms: List<Alarm>, now: Long) {
        val posted = prefs.getStringSet(KEY_POSTED, null)?.toSet() ?: return
        val current = alarms.map { occurrenceKey(it.id, nextRingMillis(it)) }.toSet()
        val stillAhead = posted.all { key -> key in current && (key.substringAfter('@').toLongOrNull() ?: 0L) > now }
        if (!stillAhead) cancelOffer()
    }

    private suspend fun waitUntilInUse(): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        var waited = 0L
        while (true) {
            if (!power.isInteractive) return false
            // Still true while the ring screen or the camera covers the lock screen: right, here.
            if (!keyguard.isKeyguardLocked) return true
            if (waited >= unlockWaitMs) return false
            delay(UNLOCK_POLL_MS)
            waited += UNLOCK_POLL_MS
        }
    }

    private fun notificationsAllowed(): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val channel = manager.getNotificationChannelCompat(PRE_ALARM_CHANNEL_ID)
        return channel == null || channel.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }

    private fun notify(id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // The notification permission was taken away in the meantime.
        }
    }

    private suspend fun enabledAlarms(): List<Alarm> = dao.getAllEnabled().map { it.toDomain() }

    private fun handled(): Set<String> = prefs.getStringSet(KEY_HANDLED, null)?.toSet() ?: emptySet()

    private fun markHandled(occurrences: List<Occurrence>, now: Long) {
        val kept = handled().filter { (it.substringAfter('@').toLongOrNull() ?: 0L) >= now - HANDLED_KEPT_MS }
        prefs.edit().putStringSet(KEY_HANDLED, (kept + occurrences.map { it.key }).toSet()).apply()
    }

    private fun appSeenAt(): Long = prefs.getLong(KEY_APP_SEEN, 0L)

    private fun labelOf(occurrence: Occurrence) = occurrence.label.ifBlank { context.getString(R.string.untitled_alarm) }

    private fun clockList(occurrences: List<Occurrence>) = occurrences.joinToString(" · ") { formatClock(it.hour, it.minute) }

    /** Far ahead: a 10-minute window lets Android batch it. Never a waking or exact alarm. */
    private fun armFirstCheck(at: Long, now: Long) {
        if (at - now <= PreAlarmPlanner.RECHECK_MS) {
            armAt(at)
        } else {
            alarmManager().setWindow(AlarmManager.RTC, at, FIRST_CHECK_WINDOW_MS, checkIntent())
        }
    }

    private fun armAt(at: Long) {
        alarmManager().set(AlarmManager.RTC, at, checkIntent())
    }

    private fun cancelCheck() {
        alarmManager().cancel(checkIntent())
    }

    private fun checkIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, CHECK_REQUEST_CODE,
        Intent(context, PreAlarmReceiver::class.java).setAction(ACTION_PRE_ALARM_CHECK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun alarmManager() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        /** Opens the confirm screen for [occurrences]. */
        fun confirmSkipIntent(context: Context, occurrences: List<Occurrence>): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_CONFIRM_SKIP)
                .putExtra(EXTRA_SKIP_IDS, occurrences.map { it.alarmId }.toLongArray())
                .putExtra(EXTRA_SKIP_ATS, occurrences.map { it.at }.toLongArray())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
