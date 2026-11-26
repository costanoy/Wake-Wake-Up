package com.wakewakeup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Confere, sem navegador, o que os lotes 3 e 8 mudaram no site: versão e tamanho do APK mostrados
 * sob o botão, passos de instalação, barra fixa de baixar, e-mail do cartão de teste, imagem de
 * compartilhamento em inglês, SEO e o limite de JavaScript.
 */
class SiteImprovementsTest {

    private val root: File = listOf(File(".."), File(".")).first { File(it, "site").isDirectory }
    private val site = File(root, "site")

    private fun site(path: String) = File(site, path).readText(Charsets.UTF_8)
    private fun count(text: String, token: String) = Regex(Regex.escape(token)).findAll(text).count()

    private val pt get() = site("index.html")
    private val en get() = site("en/index.html")

    @Test
    fun theSiteShowsTheSameVersionAsTheApp() {
        val gradle = File(root, "app/build.gradle.kts").readText()
        val appVersion = Regex("""versionName\s*=\s*"([^"]+)"""").find(gradle)!!.groupValues[1]
        val config = site("site-config.js")
        assertEquals(appVersion, Regex("""apkVersion:\s*"([^"]+)"""").find(config)!!.groupValues[1])
        for (page in listOf(pt, en)) {
            assertEquals(appVersion, Regex(""""softwareVersion":\s*"([^"]+)"""").find(page)!!.groupValues[1])
        }
        assertTrue(Regex("""apkSize:\s*"\d+(\.\d+)? MB"""").containsMatchIn(config))
    }

    @Test
    fun everyDownloadButtonHasAVersionAndSizeLineInBothLanguages() {
        val ptMeta = count(pt, "data-apk-meta")
        assertTrue(ptMeta >= 2)
        assertEquals(ptMeta, count(en, "data-apk-meta"))
        assertTrue(site("main.js").contains("[data-apk-meta]"))
    }

    @Test
    fun theInstallStepsExistAndTheTopLinkPointsAtThem() {
        assertTrue(pt.contains("id=\"instalar\""))
        assertTrue(en.contains("id=\"install\""))
        assertTrue(pt.contains("href=\"#instalar\""))
        assertTrue(en.contains("href=\"#install\""))
        assertTrue(site("lang.js").contains("'#instalar': '#install'"))
    }

    @Test
    fun theMobileDockIsInBothLanguagesAndDownloadsTheSameFile() {
        assertEquals(1, count(pt, "id=\"dock\""))
        assertEquals(1, count(en, "id=\"dock\""))
        assertTrue(pt.contains("class=\"dock__key\" href=\"wake-wake-up.apk\""))
        assertTrue(en.contains("class=\"dock__key\" href=\"../wake-wake-up.apk\""))
        // Hidden until the script decides, so without JavaScript nothing floats over the page.
        assertTrue(Regex("""id="dock" hidden""").containsMatchIn(pt))
        assertTrue(Regex("""id="dock" hidden""").containsMatchIn(en))
    }

    @Test
    fun theTestCardMailsTheConfiguredAddressInBothLanguages() {
        assertEquals(1, count(pt, "data-contact-link"))
        assertEquals(1, count(en, "data-contact-link"))
        assertTrue(site("site-config.js").contains("testContact:"))
        assertTrue(site("main.js").contains("config.testContact || config.contact"))
    }

    @Test
    fun theEnglishPagesHaveTheirOwnShareImage() {
        val img = File(site, "og-image-en.png")
        assertTrue(img.isFile)
        assertTrue(img.length() > 10_000)
        assertTrue(en.contains("og-image-en.png"))
        assertTrue(site("en/privacy.html").contains("og-image-en.png"))
        assertFalse(en.contains("/og-image.png"))
    }

    @Test
    fun titlesAndDescriptionsFitTheSearchResult() {
        for (page in listOf(pt, en)) {
            val title = Regex("<title>(.*?)</title>").find(page)!!.groupValues[1]
            val desc = Regex("""<meta name="description" content="([^"]*)"""").find(page)!!.groupValues[1]
            assertTrue("title ${title.length}", title.length <= 60)
            assertTrue("description ${desc.length}", desc.length <= 155)
            assertFalse(page.contains("aggregateRating"))
        }
    }

    @Test
    fun theSiteScriptsStayUnderTheBudget() {
        val total = listOf("main.js", "lang.js", "site-config.js").sumOf { File(site, it).length() }
        assertTrue("scripts $total B", total <= 30_000)
    }

    @Test
    fun theApkCheckerScriptIsThere() {
        val script = File(root, "ferramentas/conferir-apk-site.sh")
        assertTrue(script.isFile)
        val text = script.readText()
        assertTrue(text.contains("application-debuggable"))
        assertTrue(text.contains("apksigner"))
    }
}
