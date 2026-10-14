package com.wakewakeup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** The Portuguese and English string files must stay in step: same keys, same placeholders. */
class StringsTest {

    private data class Res(val kind: String, val name: String, val values: List<String>)

    private fun load(folder: String): Map<String, Res> {
        val file = File("src/main/res/$folder/strings.xml")
        assertTrue("missing $file", file.exists())
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = doc.documentElement
        val out = mutableMapOf<String, Res>()
        val nodes = root.childNodes
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            val name = el.getAttribute("name")
            val values = when (el.tagName) {
                "string" -> listOf(el.textContent)
                else -> el.getElementsByTagName("item").let { items -> (0 until items.length).map { items.item(it).textContent } }
            }
            out["${el.tagName}/$name"] = Res(el.tagName, name, values)
        }
        return out
    }

    private val placeholder = Regex("""%(\d+\$)?[sd]""")
    private fun placeholders(text: String) = placeholder.findAll(text).map { it.value }.toSortedSet()

    @Test
    fun everyStringExistsInBothLanguages() {
        val en = load("values")
        val pt = load("values-pt")
        assertEquals("only in English", emptySet<String>(), en.keys - pt.keys)
        assertEquals("only in Portuguese", emptySet<String>(), pt.keys - en.keys)
    }

    @Test
    fun placeholdersMatch() {
        val en = load("values")
        val pt = load("values-pt")
        for ((key, res) in en) {
            val other = pt[key] ?: continue
            val expected = res.values.flatMap { placeholders(it) }.toSortedSet()
            other.values.forEach { value ->
                assertEquals("$key: \"$value\"", expected, placeholders(value))
            }
        }
    }

    @Test
    fun dayArraysHaveSevenEntries() {
        for (folder in listOf("values", "values-pt")) {
            val res = load(folder)
            for (array in listOf("day_letters", "day_short", "day_full")) {
                assertEquals("$folder/$array", 7, res.getValue("string-array/$array").values.size)
            }
        }
    }
}
