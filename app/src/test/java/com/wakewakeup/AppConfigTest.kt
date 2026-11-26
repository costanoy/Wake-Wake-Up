package com.wakewakeup

import androidx.compose.ui.graphics.luminance
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** F75 (backup) e F77 (tema escuro fixo). */
@RunWith(AndroidJUnit4::class)
class AppConfigTest {

    private fun domains(xmlRes: Int, tagPath: String): List<String> {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val parser = app.resources.getXml(xmlRes)
        val found = mutableListOf<String>()
        val stack = ArrayDeque<String>()
        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> {
                    stack.addLast(parser.name)
                    if (parser.name == "include" || parser.name == "exclude") {
                        found += "${stack.joinToString("/")}:${parser.getAttributeValue(null, "domain")}"
                    }
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> stack.removeLast()
            }
            event = parser.next()
        }
        return found.filter { it.startsWith(tagPath) }
    }

    @Test
    fun theOldBackupRulesTakeOnlyTheDatabase() {
        val rules = domains(R.xml.backup_rules, "full-backup-content")
        assertEquals(listOf("full-backup-content/include:database"), rules)
    }

    @Test
    fun theNewBackupRulesTakeOnlyTheDatabaseForCloudAndPhoneToPhone() {
        val rules = domains(R.xml.data_extraction_rules, "data-extraction-rules")
        assertEquals(
            listOf(
                "data-extraction-rules/cloud-backup/include:database",
                "data-extraction-rules/device-transfer/include:database",
            ),
            rules,
        )
    }

    @Test
    fun theManifestPointsAtBothBackupRuleFiles() {
        val manifest = File("src/main/AndroidManifest.xml").takeIf { it.isFile } ?: File("app/src/main/AndroidManifest.xml")
        val text = manifest.readText(Charsets.UTF_8)
        assertTrue(text.contains("@xml/backup_rules"))
        assertTrue(text.contains("@xml/data_extraction_rules"))
    }

    private fun source(path: String): String =
        (File("src/main/java/com/wakewakeup/$path").takeIf { it.isFile } ?: File("app/src/main/java/com/wakewakeup/$path")).readText(Charsets.UTF_8)

    @Test
    fun theThemeHasOnlyADarkSchemeAndNeverAsksTheSystem() {
        val theme = source("ui/theme/Theme.kt")
        assertTrue(theme.contains("darkColorScheme("))
        assertFalse(theme.contains("lightColorScheme"))
        assertFalse(theme.contains("isSystemInDarkTheme"))
        assertFalse(theme.contains("dynamicDarkColorScheme") || theme.contains("dynamicLightColorScheme"))
    }

    @Test
    fun theBackgroundAndSurfacesAreDark() {
        val bg = com.wakewakeup.ui.theme.Housing
        val surface = com.wakewakeup.ui.theme.PlateBottom
        val text = com.wakewakeup.ui.theme.Ink
        assertTrue("background $bg", bg.luminance() < 0.1f)
        assertTrue("surface $surface", surface.luminance() < 0.2f)
        assertTrue("text $text", text.luminance() > 0.5f)
    }

    @Test
    fun theSystemBarsAreDarkOnBothScreens() {
        assertTrue(source("MainActivity.kt").contains("enableEdgeToEdge"))
        assertTrue(source("session/RingActivity.kt").contains("SystemBarStyle.dark"))
    }
}
