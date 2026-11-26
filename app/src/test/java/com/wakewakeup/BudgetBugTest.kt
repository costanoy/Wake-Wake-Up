package com.wakewakeup

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** TEC-08: o orçamento de performance do site (no máximo 4 fontes). */
class BudgetBugTest {

    private fun dir(vararg c: String) = c.map { File(it) }.first { it.isDirectory }

    @Test
    fun theSiteLoadsAtMostFourFonts() {
        val css = File(dir("../site", "site"), "styles.css").readText()
        assertEquals(4, Regex("@font-face").findAll(css).count())
    }
}
