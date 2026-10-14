package com.wakewakeup.session

import com.wakewakeup.data.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class MissionGeneratorTest {

    private fun check(difficulty: Difficulty, operator: String, left: IntRange, right: IntRange, op: (Int, Int) -> Int) {
        repeat(3_000) {
            val q = MissionGenerator.mathQuestion(difficulty)
            val parts = q.question.split(" ")
            assertEquals(q.question, 3, parts.size)
            val (a, sign, b) = parts
            assertEquals(q.question, operator, sign)
            assertTrue(q.question, a.toInt() in left)
            assertTrue(q.question, b.toInt() in right)
            assertEquals(q.question, op(a.toInt(), b.toInt()).toString(), q.answer)
            // The calculator keypad takes at most 4 digits and the answer is never negative.
            assertTrue(q.question, q.answer.length <= 4 && q.answer.all { it.isDigit() })
        }
    }

    @Test
    fun easyIsASumOfTwoSingleDigits() = check(Difficulty.EASY, "+", 2..9, 2..9) { a, b -> a + b }

    @Test
    fun mediumIsASumOfTwoTwoDigitNumbers() = check(Difficulty.MEDIUM, "+", 21..89, 14..79) { a, b -> a + b }

    @Test
    fun hardIsAMultiplication() = check(Difficulty.HARD, "×", 12..49, 6..19) { a, b -> a * b }

    private val start = LocalDate.of(2026, 10, 12)
    private val pt = Locale.forLanguageTag("pt-BR")
    private val en = Locale.US

    private fun twelveDays(locale: Locale) = (0L until 12L).map { MissionGenerator.phraseOfTheDay(start.plusDays(it), locale) }

    @Test
    fun phraseIsTheSameAllDayAndChangesEveryDay() {
        for (locale in listOf(pt, en)) {
            assertEquals(MissionGenerator.phraseOfTheDay(start, locale), MissionGenerator.phraseOfTheDay(start, locale))
            assertEquals("12 different phrases over 12 days ($locale)", 12, twelveDays(locale).toSet().size)
            // The cycle repeats after the pool is used up.
            assertEquals(MissionGenerator.phraseOfTheDay(start, locale), MissionGenerator.phraseOfTheDay(start.plusDays(12), locale))
        }
    }

    @Test
    fun phraseFollowsTheLanguage() {
        val portuguese = twelveDays(pt).toSet()
        assertEquals(portuguese, twelveDays(Locale.forLanguageTag("pt-PT")).toSet())
        assertTrue(portuguese.contains("Hoje eu acordo de verdade."))
        val english = twelveDays(en).toSet()
        assertTrue(english.contains("Today I really wake up."))
        assertEquals(english, twelveDays(Locale.forLanguageTag("es-ES")).toSet())
    }

    @Test
    fun phrasesHaveNoAccentsAndAreTypeable() {
        for (phrase in twelveDays(pt) + twelveDays(en)) {
            assertTrue("accent in \"$phrase\"", phrase.all { it.code < 128 })
            assertTrue(phrase, phrase.length <= 80) // the mission field keeps 80 characters
            assertTrue(phrase, MissionGenerator.phraseMatches(phrase, phrase))
            assertTrue(phrase, MissionGenerator.phraseMatches(phrase.lowercase().filter { it.isLetter() || it == ' ' }, phrase))
            assertEquals(phrase, phrase.length, MissionGenerator.matchedPrefixLength(phrase, phrase))
        }
    }

    @Test
    fun normalizationIgnoresCaseAccentsPunctuationAndExtraSpaces() {
        assertEquals("ola mundo", MissionGenerator.normalizePhrase("  Olá,   MUNDO!! "))
        assertEquals("acao e coracao", MissionGenerator.normalizePhrase("Ação e coração."))
        assertEquals("", MissionGenerator.normalizePhrase("...!?"))
    }

    @Test
    fun phraseMatching() {
        val phrase = "Levanto, lavo o rosto e sigo em frente."
        assertTrue(MissionGenerator.phraseMatches("levanto lavo o rosto e sigo em frente", phrase))
        assertTrue(MissionGenerator.phraseMatches("LEVANTO, LAVO O ROSTO E SIGO EM FRENTE", phrase))
        assertTrue(MissionGenerator.phraseMatches("Levantó lavo o rosto e sígo em frente.", phrase))
        assertTrue(MissionGenerator.phraseMatches("levanto  lavo o rosto e sigo em frente ", phrase))
        assertFalse(MissionGenerator.phraseMatches("levanto lavo o rosto", phrase))
        assertFalse(MissionGenerator.phraseMatches("levanto lavo o rosto e sigo em frente agora", phrase))
        assertFalse(MissionGenerator.phraseMatches("", phrase))
        assertFalse(MissionGenerator.phraseMatches("   ", phrase))
        assertFalse(MissionGenerator.phraseMatches("...", "..."))
    }

    @Test
    fun matchedPrefixLightsTheDisplayAsYouType() {
        val phrase = "Levanto, lavo o rosto e sigo em frente."
        assertEquals(0, MissionGenerator.matchedPrefixLength(phrase, ""))
        assertEquals(3, MissionGenerator.matchedPrefixLength(phrase, "lev"))
        // The comma lights up together with the word before it.
        assertEquals(8, MissionGenerator.matchedPrefixLength(phrase, "levanto"))
        assertEquals(9, MissionGenerator.matchedPrefixLength(phrase, "levanto "))
        assertEquals(13, MissionGenerator.matchedPrefixLength(phrase, "Levanto, lavo"))
        // A typo stops the lit part where it happened.
        assertEquals(4, MissionGenerator.matchedPrefixLength(phrase, "levax"))
        // The final period lights up with the last word.
        assertEquals(phrase.length, MissionGenerator.matchedPrefixLength(phrase, "levanto lavo o rosto e sigo em frente"))
    }
}
