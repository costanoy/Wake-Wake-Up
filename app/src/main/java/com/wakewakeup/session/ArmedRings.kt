package com.wakewakeup.session

import android.content.Context
import android.content.SharedPreferences
import com.wakewakeup.data.Alarm
import org.json.JSONObject

/**
 * A minimal copy of what is armed in AlarmManager, kept in the device-protected storage so
 * [BootReceiver] can arm the alarms again after a restart while the phone is still locked
 * (the Room database lives in credential-protected storage, closed until the first unlock).
 * Only time, days and skip are kept: no label, no sound, nothing private.
 */
object ArmedRings {

    data class Entry(val alarm: Alarm, val armedAt: Long)

    private const val NAME = "armed_rings"
    private const val PREFIX = "a_"

    private fun prefs(context: Context): SharedPreferences =
        context.createDeviceProtectedStorageContext().getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun put(context: Context, alarm: Alarm, armedAt: Long) {
        val json = JSONObject()
            .put("hour", alarm.hour)
            .put("minute", alarm.minute)
            .put("days", alarm.days.sorted().joinToString(","))
            .put("at", armedAt)
        alarm.skipDate?.let { json.put("skipDate", it) }
        prefs(context).edit().putString(PREFIX + alarm.id, json.toString()).apply()
    }

    fun remove(context: Context, id: Long) {
        prefs(context).edit().remove(PREFIX + id).apply()
    }

    fun all(context: Context): List<Entry> = prefs(context).all.mapNotNull { (key, value) ->
        val id = key.removePrefix(PREFIX).toLongOrNull()
        if (!key.startsWith(PREFIX) || id == null || value !is String) null else parse(id, value)
    }

    private fun parse(id: Long, text: String): Entry? = try {
        val json = JSONObject(text)
        val days = json.getString("days").split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }.toSet()
        val skip = if (json.has("skipDate")) json.getLong("skipDate") else null
        Entry(
            Alarm(
                id = id, hour = json.getInt("hour"), minute = json.getInt("minute"), label = "",
                days = days, enabled = true, skipDate = skip,
            ),
            json.getLong("at"),
        )
    } catch (e: Exception) {
        null
    }
}
