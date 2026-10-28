package com.wakewakeup.data

import com.wakewakeup.session.AlarmScheduler
import com.wakewakeup.session.epochDayOf
import com.wakewakeup.session.isoDayIndexOf
import com.wakewakeup.session.nextRingMillis
import com.wakewakeup.session.nextTriggerEpochDay
import com.wakewakeup.session.nextTriggerMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Least time an action about a specific ring needs before it, so it never races the ring. */
private const val ACTION_MARGIN_MS = 60_000L

/** A ring that just came due may still be on its way to AlarmReceiver; don't move it. */
private const val DUE_GRACE_MS = 2 * 60_000L

class AlarmRepository(
    private val dao: AlarmDao,
    private val scheduler: AlarmScheduler,
    /** Runs after every change, so whatever depends on the alarms (the pre-alarm check) follows. */
    private val onChanged: suspend () -> Unit = {},
) {
    fun observeAll(): Flow<List<Alarm>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<Alarm> = dao.getAll().map { it.toDomain() }

    suspend fun getById(id: Long): Alarm? = dao.getById(id)?.toDomain()

    suspend fun save(alarm: Alarm): Long {
        // A skip survives an edit only while it still names one of this alarm's upcoming days.
        val clean = alarm.copy(skipDate = alarm.skipDate?.takeIf { alarm.enabled && isValidSkip(alarm, it) })
        val id = if (clean.id == 0L) dao.insert(clean.toEntity()) else {
            dao.update(clean.toEntity())
            clean.id
        }
        scheduler.schedule(clean.copy(id = id))
        onChanged()
        return id
    }

    suspend fun delete(alarm: Alarm) {
        dao.delete(alarm.toEntity())
        scheduler.cancel(alarm)
        onChanged()
    }

    /** Switching an alarm on or off by hand also drops any day it was skipping. */
    suspend fun setEnabled(alarm: Alarm, enabled: Boolean) {
        dao.setEnabled(alarm.id, enabled)
        scheduler.schedule(alarm.copy(enabled = enabled, skipDate = null))
        onChanged()
    }

    /**
     * Turns the alarm (back) on but skips its next occurrence, the list's "pular só quarta".
     * A one-time alarm has nothing after its next ring, so it is switched off instead.
     * Returns the skipped day, or null for a one-time alarm.
     */
    suspend fun skipNext(alarm: Alarm): Long? {
        if (alarm.days.isEmpty()) {
            setEnabled(alarm, false)
            return null
        }
        val day = nextTriggerEpochDay(alarm)
        dao.setSkipping(alarm.id, day)
        scheduler.schedule(alarm.copy(enabled = true, skipDate = day))
        onChanged()
        return day
    }

    /** Rings on the skipped day after all. */
    suspend fun clearSkip(alarm: Alarm) {
        dao.setSkipDate(alarm.id, null)
        scheduler.schedule(alarm.copy(skipDate = null))
        onChanged()
    }

    /**
     * Skips the ring at exactly [at] (epoch millis), if that is still the alarm's next ring and
     * more than a minute away. Recurring alarms skip that one day; one-time alarms switch off.
     */
    suspend fun skipOccurrence(id: Long, at: Long, now: Long = System.currentTimeMillis()): Boolean {
        val alarm = getById(id) ?: return false
        if (!alarm.enabled || at - now < ACTION_MARGIN_MS || nextRingMillis(alarm, localNow(now)) != at) return false
        if (alarm.days.isEmpty()) {
            setEnabled(alarm, false)
        } else {
            val day = epochDayOf(at)
            dao.setSkipDate(id, day)
            scheduler.schedule(alarm.copy(skipDate = day))
            onChanged()
        }
        return true
    }

    /** Undoes [skipOccurrence] for the ring at [at], if that ring is still more than a minute away. */
    suspend fun undoSkipOccurrence(id: Long, at: Long, now: Long = System.currentTimeMillis()): Boolean {
        val alarm = getById(id) ?: return false
        if (at - now < ACTION_MARGIN_MS) return false
        return if (alarm.days.isEmpty()) {
            if (alarm.enabled || nextTriggerMillis(alarm, localNow(now)) != at) return false
            setEnabled(alarm, true)
            true
        } else {
            if (!alarm.enabled || alarm.skipDate != epochDayOf(at)) return false
            clearSkip(alarm)
            true
        }
    }

    /**
     * Called as an alarm rings: a skip whose day has come is spent, and the next ring is armed
     * before anything else can go wrong.
     */
    suspend fun armNextRing(alarm: Alarm) {
        val today = LocalDate.now().toEpochDay()
        val skip = alarm.skipDate
        val current = if (skip != null && skip <= today) {
            dao.setSkipDate(alarm.id, null)
            alarm.copy(skipDate = null)
        } else {
            alarm
        }
        scheduler.schedule(current)
        onChanged()
    }

    /**
     * Arms every enabled alarm again (after a reboot, a time zone change, an update or a force
     * stop). With [sparingDue], an alarm whose time came up in the last two minutes is left alone:
     * its ring may be in flight, and re-arming it now would replace it with the next day's.
     * Returns false when some alarm couldn't be armed.
     */
    suspend fun rescheduleAll(sparingDue: Boolean = false, now: Long = System.currentTimeMillis()): Boolean {
        dao.clearSkipsBefore(LocalDate.now().toEpochDay())
        var allArmed = true
        for (alarm in dao.getAllEnabled().map { it.toDomain() }) {
            if (sparingDue && justCameDue(alarm, now)) continue
            if (!scheduler.schedule(alarm)) allArmed = false
        }
        onChanged()
        return allArmed
    }

    /** True when the alarm's time came up less than two minutes ago, on a day it rings. */
    private fun justCameDue(alarm: Alarm, now: Long): Boolean {
        val local = localNow(now)
        var last = local.toLocalDate().atTime(alarm.hour, alarm.minute)
        if (last.isAfter(local)) last = last.minusDays(1)
        val ringsThatDay = alarm.days.isEmpty() || isoDayIndexOf(last.toLocalDate()) in alarm.days
        return ringsThatDay && now - last.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() < DUE_GRACE_MS
    }

    private fun isValidSkip(alarm: Alarm, day: Long): Boolean =
        alarm.days.isNotEmpty() &&
            day >= LocalDate.now().toEpochDay() &&
            isoDayIndexOf(LocalDate.ofEpochDay(day)) in alarm.days

    private fun localNow(now: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
}
