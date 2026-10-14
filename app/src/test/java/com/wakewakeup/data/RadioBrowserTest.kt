package com.wakewakeup.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.testing.FakeRadio
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RadioBrowserTest {

    @Before
    fun fakeNetwork() = FakeRadio.install()

    @Test
    fun keepsOnlyNamedHttpsStationsOncePerStream() = runBlocking {
        val stations = RadioBrowser.search("", "BR")
        assertEquals(
            listOf(
                RadioStation("Radio Um", "https://um.example/stream", "Brazil", "pop, rock"),
                RadioStation("Radio Dois", "https://dois.example/live", "Brazil", ""),
            ),
            stations,
        )
    }

    @Test
    fun aBlankQueryAsksForTheMostListenedInTheCountry() = runBlocking {
        RadioBrowser.search("  ", "BR")
        val url = FakeRadio.requests.single().toString()
        assertTrue(url, url.startsWith("https://all.api.radio-browser.info/json/stations/search?countrycode=BR&"))
        assertTrue(url, url.contains("is_https=true") && url.contains("hidebroken=true") && url.contains("order=clickcount"))
    }

    @Test
    fun aQuerySearchesByName() = runBlocking {
        RadioBrowser.search(" Rádio Jovem Pan ", "BR")
        val url = FakeRadio.requests.single().toString()
        assertTrue(url, url.contains("?name=R%C3%A1dio+Jovem+Pan&"))
    }

    @Test
    fun noNetworkIsAnError() = runBlocking {
        FakeRadio.offline = true
        try {
            RadioBrowser.search("", "BR")
            fail("expected an error")
        } catch (e: IOException) {
            // The radio screen shows "No signal" and a retry.
        }
    }

    @Test
    fun anUnexpectedAnswerIsAnErrorToo() = runBlocking {
        FakeRadio.body = "<html>maintenance</html>"
        try {
            RadioBrowser.search("", "BR")
            fail("expected an error")
        } catch (e: Exception) {
            // Caught by the radio screen like a network error.
        }
    }
}
