package com.wakewakeup

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** TEC-08 (parte do app): nenhum bitmap PNG em res/drawable*; o ícone usa WebP sem perda. */
class AppBudgetTest {

    private fun dir(vararg c: String) = c.map { File(it) }.first { it.isDirectory }

    @Test
    fun noBitmapInDrawablesIsAPng() {
        val res = dir("src/main/res", "app/src/main/res")
        val pngs = res.walkTopDown().filter { it.isFile && it.extension == "png" && it.parentFile.name.startsWith("drawable") }.toList()
        assertTrue("PNGs: $pngs", pngs.isEmpty())
    }
}
