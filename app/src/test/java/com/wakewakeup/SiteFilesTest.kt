package com.wakewakeup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * O site é HTML/JS puro e não roda na JVM, mas a estrutura dele dá para conferir: as duas páginas
 * têm os elementos que o main.js procura, os links locais existem e as âncoras apontam para algo.
 */
class SiteFilesTest {

    private val site: File = listOf(File("../site"), File("site")).first { it.isDirectory || it.path == "site" }

    private fun read(path: String): String = File(site, path).readText(Charsets.UTF_8)

    private val pages = listOf("index.html", "en/index.html", "privacidade.html", "en/privacy.html")

    @Test
    fun theSiteFolderIsThere() {
        assumeTrue(site.isDirectory)
        for (p in pages + listOf("main.js", "lang.js", "site-config.js", "styles.css", "sitemap.xml")) {
            assertTrue("missing $p", File(site, p).isFile)
        }
    }

    @Test
    fun everyElementTheScriptLooksUpExistsInBothLanguages() {
        assumeTrue(site.isDirectory)
        val wanted = Regex("""\$\('([\w-]+)'\)""").findAll(read("main.js")).map { it.groupValues[1] }.toSet()
        assertTrue(wanted.isNotEmpty())
        for (page in listOf("index.html", "en/index.html")) {
            val html = read(page)
            for (id in wanted) assertTrue("$page has no #$id", Regex("""\bid="$id"""").containsMatchIn(html))
        }
    }

    @Test
    fun bothLanguagesHaveTheSameDownloadHooks() {
        assumeTrue(site.isDirectory)
        for (attr in listOf("data-download", "data-apk-note", "data-desktop-note", "data-ios-note", "data-reveal")) {
            val pt = Regex(attr).findAll(read("index.html")).count()
            val en = Regex(attr).findAll(read("en/index.html")).count()
            assertTrue("$attr missing in pt", pt > 0)
            assertEquals("$attr count differs between pt and en", pt, en)
        }
    }

    @Test
    fun everyLocalLinkAndAssetResolvesToAFile() {
        assumeTrue(site.isDirectory)
        val ref = Regex("""(?:href|src|poster)="([^"#?]+)(?:[?#][^"]*)?"""")
        for (page in pages) {
            val dir = File(site, page).parentFile
            for (m in ref.findAll(read(page))) {
                val target = m.groupValues[1]
                if (target.startsWith("http") || target.startsWith("mailto:") || target.startsWith("tel:") || target.startsWith("data:")) continue
                if (target.endsWith(".apk")) continue // o APK não é versionado (gitignore); entra só no servidor
                val file = File(dir, target).canonicalFile
                assertTrue("$page points to $target, which does not exist", file.exists())
            }
        }
    }

    @Test
    fun everyAnchorPointsToAnElementOfTheSamePage() {
        assumeTrue(site.isDirectory)
        for (page in pages) {
            val html = read(page)
            val ids = Regex("""\bid="([^"]+)"""").findAll(html).map { it.groupValues[1] }.toSet()
            for (m in Regex("""href="#([\w-]+)"""").findAll(html)) {
                assertTrue("$page links to #${m.groupValues[1]}, which does not exist", m.groupValues[1] in ids)
            }
        }
    }

    @Test
    fun theLanguageAttributeMatchesTheFolder() {
        assumeTrue(site.isDirectory)
        assertTrue(Regex("""<html[^>]*lang="pt""").containsMatchIn(read("index.html")))
        assertTrue(Regex("""<html[^>]*lang="en""").containsMatchIn(read("en/index.html")))
    }

    @Test
    fun theSitemapOnlyListsPagesThatExist() {
        assumeTrue(site.isDirectory)
        val base = "https://wakewakeup.cyberhat.com.br/"
        for (m in Regex("""<loc>([^<]+)</loc>""").findAll(read("sitemap.xml"))) {
            val url = m.groupValues[1]
            assertTrue(url, url.startsWith(base))
            var path = url.removePrefix(base)
            if (path.isEmpty() || path.endsWith("/")) path += "index.html"
            assertTrue("sitemap lists $url but $path is missing", File(site, path).isFile)
        }
    }

    @Test
    fun theScriptVersionStampIsTheSameOnEveryPage() {
        assumeTrue(site.isDirectory)
        val stamps = pages.flatMap { Regex("""\?v=(\d+)""").findAll(read(it)).map { m -> m.groupValues[1] }.toList() }.toSet()
        assertEquals("pages use different cache stamps: $stamps", 1, stamps.size)
    }

    @Test
    fun theDownloadConfigNamesTheSameFileThePagesLinkTo() {
        assumeTrue(site.isDirectory)
        val config = read("site-config.js")
        val apk = Regex("""apkUrl:\s*"([^"]*)"""").find(config)?.groupValues?.get(1).orEmpty()
        assertTrue(apk.isNotEmpty())
        for (page in listOf("index.html", "en/index.html")) {
            assertTrue("$page does not link $apk", read(page).contains(apk))
        }
    }
}
