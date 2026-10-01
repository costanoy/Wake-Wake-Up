package com.wakewakeup.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class RadioStation(val name: String, val streamUrl: String, val country: String, val tags: String)

/**
 * Station search on Radio Browser (radio-browser.info), a free community directory of
 * internet radio streams. Only HTTPS streams are requested: the app doesn't allow
 * cleartext traffic, so an http:// stream couldn't be played anyway.
 */
object RadioBrowser {
    private const val BASE = "https://all.api.radio-browser.info/json/stations/search"
    private const val USER_AGENT = "WakeWakeUp/1.0 (Android alarm clock)"
    private const val LIMIT = 40

    /** Stations matching [query] by name; with a blank query, the most popular in [countryCode]. */
    suspend fun search(query: String, countryCode: String): List<RadioStation> = withContext(Dispatchers.IO) {
        val filter = if (query.isBlank()) {
            "countrycode=${encode(countryCode)}"
        } else {
            "name=${encode(query.trim())}"
        }
        val url = URL("$BASE?$filter&hidebroken=true&is_https=true&order=clickcount&reverse=true&limit=$LIMIT")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(body)
            (0 until array.length())
                .map { array.getJSONObject(it) }
                .mapNotNull { o ->
                    val name = o.optString("name").trim()
                    val stream = o.optString("url_resolved").ifBlank { o.optString("url") }
                    if (name.isEmpty() || !stream.startsWith("https://")) return@mapNotNull null
                    RadioStation(
                        name = name,
                        streamUrl = stream,
                        country = o.optString("country").trim(),
                        tags = o.optString("tags").split(',').map { it.trim() }.filter { it.isNotEmpty() }.take(2).joinToString(", "),
                    )
                }
                .distinctBy { it.streamUrl }
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
}
