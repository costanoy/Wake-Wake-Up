package com.wakewakeup.testing

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

/**
 * Stands in for radio-browser.info so the radio screen can be tested without a network: every
 * https request in the test JVM is answered from here (nothing else in the tests uses https).
 */
object FakeRadio {
    @Volatile var body: String = SAMPLE
    @Volatile var offline: Boolean = false
    val requests: MutableList<URL> = java.util.Collections.synchronizedList(mutableListOf())

    private var installed = false

    fun install() {
        body = SAMPLE
        offline = false
        requests.clear()
        if (installed) return
        URL.setURLStreamHandlerFactory { protocol ->
            if (protocol == "https") {
                object : URLStreamHandler() {
                    override fun openConnection(u: URL): URLConnection = Connection(u)
                }
            } else {
                null
            }
        }
        installed = true
    }

    private class Connection(url: URL) : HttpURLConnection(url) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun getResponseCode() = if (offline) -1 else 200
        override fun getInputStream(): InputStream {
            requests += url
            if (offline) throw IOException("no network")
            return body.byteInputStream()
        }
    }

    const val SAMPLE = """[
      {"name": "Radio Um", "url_resolved": "https://um.example/stream", "url": "http://um.example/raw", "country": "Brazil", "tags": "pop, rock,mpb"},
      {"name": "  Radio Dois  ", "url_resolved": "", "url": "https://dois.example/live", "country": "Brazil", "tags": ""},
      {"name": "Radio Insegura", "url_resolved": "http://insegura.example/stream", "url": "http://insegura.example", "country": "Brazil", "tags": "news"},
      {"name": "Radio Um (copia)", "url_resolved": "https://um.example/stream", "url": "", "country": "Brazil", "tags": "pop"},
      {"name": "", "url_resolved": "https://semnome.example/stream", "url": "", "country": "", "tags": ""}
    ]"""
}
