package com.wakewakeup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.io.File

/** TEC-08: o orçamento de performance já nasce estourado (fontes do site e ícone em PNG). */
@Ignore("TEC-08: 5 @font-face no site (máximo 4) e PNG em res/drawable-nodpi")
class BudgetBugTest {

    private fun dir(vararg c: String) = c.map { File(it) }.first { it.isDirectory }

    @Test
    fun theSiteLoadsAtMostFourFonts() {
        val css = File(dir("../site", "site"), "styles.css").readText()
        assertEquals(4, Regex("@font-face").findAll(css).count())
    }

    @Test
    fun noBitmapInDrawablesIsAPng() {
        val res = dir("src/main/res", "app/src/main/res")
        val pngs = res.walkTopDown().filter { it.isFile && it.extension == "png" && it.parentFile.name.startsWith("drawable") }.toList()
        assertTrue("PNGs: $pngs", pngs.isEmpty())
    }
}
