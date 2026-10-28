package com.wakewakeup.session

import com.wakewakeup.data.Alarm
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** One ring of one alarm: the alarm at the instant [at] (epoch millis). */
data class Occurrence(val alarmId: Long, val at: Long, val label: String, val hour: Int, val minute: Int) {
    val key: String get() = occurrenceKey(alarmId, at)
}

fun occurrenceKey(alarmId: Long, at: Long) = "$alarmId@$at"

/**
 * What the pre-alarm check should do now: [offer] the rings to ask about (empty for none), when
 * to look again, and rings to just mark as handled without asking ([quietlyHandled]).
 */
data class PreAlarmPlan(
    val offer: List<Occurrence>,
    val nextCheckAt: Long?,
    val quietlyHandled: List<Occurrence> = emptyList(),
)

/**
 * The rules behind "Você quer desativar o próximo alarme?", as a pure function of the alarms and
 * the clock so they can be tested without a phone:
 * - only morning alarms (set between 03:00 and 11:59) are offered, since being on the phone
 *   before one means being up already; a 22:00 medicine reminder is never questioned;
 * - the offer comes up to an hour before the first of them, never in the last 5 minutes before
 *   any ring, and covers every morning ring in the hour after the first (backup alarms);
 * - each ring is asked about once, and not at all when the app itself was open since an hour
 *   and a half before it (the user just looked at or changed their alarms).
 */
object PreAlarmPlanner {
    const val LEAD_MS = 60 * 60_000L
    const val CUTOFF_MS = 5 * 60_000L
    const val CLUSTER_MS = 60 * 60_000L
    const val APP_SEEN_MS = 90 * 60_000L
    const val RECHECK_MS = 60_000L

    fun isMorningAlarm(alarm: Alarm): Boolean = alarm.hour in 3..11

    fun plan(
        alarms: List<Alarm>,
        now: Long,
        handled: Set<String>,
        appSeenAt: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): PreAlarmPlan {
        val from = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
        val upcoming = alarms.filter { it.enabled }.map { it to nextRingMillis(it, from) }

        // Something rings in a moment: never ask over it, look again once it has rung.
        val imminent = upcoming.map { it.second }.filter { it - now < CUTOFF_MS }.minOrNull()
        if (imminent != null) return PreAlarmPlan(emptyList(), imminent + RECHECK_MS)

        val eligible = upcoming
            .filter { (alarm, _) -> isMorningAlarm(alarm) }
            .mapNotNull { (alarm, next) -> firstUnhandled(alarm, next, handled, zone) }
            .sortedBy { it.at }
        val first = eligible.firstOrNull() ?: return PreAlarmPlan(emptyList(), null)
        if (first.at - now > LEAD_MS) return PreAlarmPlan(emptyList(), first.at - LEAD_MS)

        val cluster = eligible.filter { it.at <= first.at + CLUSTER_MS }
        val later = nextCheckFor(eligible - cluster.toSet(), now)
        return if (appSeenAt >= first.at - APP_SEEN_MS) {
            PreAlarmPlan(emptyList(), later, quietlyHandled = cluster)
        } else {
            PreAlarmPlan(cluster, later)
        }
    }

    /**
     * Rings worth offering to skip along with the one that just finished its mission (the Bom
     * dia screen): the other morning alarms due within the hour, more than a minute ahead.
     */
    fun companions(alarms: List<Alarm>, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Occurrence> {
        val from = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
        return alarms.filter { it.enabled && isMorningAlarm(it) }
            .map { Occurrence(it.id, nextRingMillis(it, from), it.label, it.hour, it.minute) }
            .filter { it.at - now in RECHECK_MS..CLUSTER_MS }
            .sortedBy { it.at }
    }

    /**
     * The alarm's next ring, or, once that one was asked about, the one after it (so tomorrow's
     * check is armed without waiting for today's ring). A one-time alarm has no ring after it.
     */
    private fun firstUnhandled(alarm: Alarm, next: Long, handled: Set<String>, zone: ZoneId): Occurrence? {
        var at = next
        if (occurrenceKey(alarm.id, at) in handled) {
            if (alarm.days.isEmpty()) return null
            at = nextRingMillis(alarm, LocalDateTime.ofInstant(Instant.ofEpochMilli(at), zone))
            if (occurrenceKey(alarm.id, at) in handled) return null
        }
        return Occurrence(alarm.id, at, alarm.label, alarm.hour, alarm.minute)
    }

    private fun nextCheckFor(rest: List<Occurrence>, now: Long): Long? =
        rest.minOfOrNull { it.at }?.let { maxOf(it - LEAD_MS, now + RECHECK_MS) }
}
