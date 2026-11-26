package com.wakewakeup.session

import com.wakewakeup.data.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class MissionGeneratorEdgeTest {

    @Test
    fun everyQuestionHasTheRightAnswerAndAPositiveOne() {
        for (difficulty in Difficulty.entries) {
            repeat(1_000) {
                val q = MissionGenerator.mathQuestion(difficulty)
                val parts = q.question.split(" ")
                val a = parts[0].toInt()
                val b = parts[2].toInt()
                val expected = if (parts[1] == "×") a * b else a + b
                assertEquals(q.question, expected.toString(), q.answer)
                assertTrue(expected > 0)
            }
        }
    }

    @Test
    fun harderLevelsGiveBiggerAnswersOnAverage() {
        fun avg(d: Difficulty) = (1..500).map { MissionGenerator.mathQuestion(d).answer.toInt() }.average()
        assertTrue(avg(Difficulty.EASY) < avg(Difficulty.MEDIUM))
        assertTrue(avg(Difficulty.MEDIUM) < avg(Difficulty.HARD))
    }

    @Test
    fun theBiggestAnswerStillFitsTheCalculatorDisplay() {
        // 49 x 19 = 931 and 89 + 79 = 168: never more than four digits.
        repeat(2_000) {
            for (d in Difficulty.entries) assertTrue(MissionGenerator.mathQuestion(d).answer.length <= 4)
        }
    }

    @Test
    fun noPhraseRepeatsInsideATwelveDayCycleInEitherLanguage() {
        val start = LocalDate.of(2026, 11, 1)
        for (locale in listOf(Locale("pt", "BR"), Locale.US)) {
            val phrases = (0L until 12L).map { MissionGenerator.phraseOfTheDay(start.plusDays(it), locale) }
            assertEquals(12, phrases.toSet().size)
            // And the cycle comes back around.
            assertEquals(phrases[0], MissionGenerator.phraseOfTheDay(start.plusDays(12), locale))
        }
    }

    @Test
    fun theNormalizedPhraseOfADateBeforeTheEpochIsStillValid() {
        val phrase = MissionGenerator.phraseOfTheDay(LocalDate.of(1960, 1, 1), Locale("pt", "BR"))
        assertTrue(phrase.isNotBlank())
    }

    @Test
    fun anUnknownLanguageFallsBackToEnglish() {
        val d = LocalDate.of(2026, 11, 26)
        assertEquals(MissionGenerator.phraseOfTheDay(d, Locale.US), MissionGenerator.phraseOfTheDay(d, Locale("fr")))
    }

    @Test
    fun typingOnlyPunctuationOrSpacesNeverCountsAsAnAnswer() {
        val phrase = MissionGenerator.phraseOfTheDay()
        assertFalse(MissionGenerator.phraseMatches("", phrase))
        assertFalse(MissionGenerator.phraseMatches("   ", phrase))
        assertFalse(MissionGenerator.phraseMatches("...!!!", phrase))
    }

    @Test
    fun aPhraseTypedWithAnExtraWordIsRejected() {
        val phrase = "Hoje eu acordo de verdade."
        assertTrue(MissionGenerator.phraseMatches("hoje eu acordo de verdade", phrase))
        assertFalse(MissionGenerator.phraseMatches("hoje eu acordo de verdade mesmo", phrase))
        assertFalse(MissionGenerator.phraseMatches("hoje eu acordo de", phrase))
    }

    @Test
    fun theAccentedLettersAreTypeableWithoutTheAccent() {
        assertTrue(MissionGenerator.phraseMatches("coracao", "Coração"))
        assertTrue(MissionGenerator.phraseMatches("CORAÇÃO", "coracao"))
    }

    @Test
    fun theLitPartNeverShrinksAsMoreOfThePhraseIsTyped() {
        for (phrase in listOf("Levanto, lavo o rosto e sigo em frente.", "Bom dia para mim mesmo.")) {
            var last = 0
            val typed = StringBuilder()
            for (ch in phrase) {
                typed.append(ch)
                val now = MissionGenerator.matchedPrefixLength(phrase, typed.toString())
                assertTrue("$phrase at '$typed': $now < $last", now >= last)
                last = now
            }
            assertEquals(phrase.length, last)
        }
    }

    @Test
    fun theLitPartStopsAtTheFirstMistake() {
        val phrase = "Bom dia para mim mesmo."
        assertEquals(6, MissionGenerator.matchedPrefixLength(phrase, "bom dix"))
        assertEquals(0, MissionGenerator.matchedPrefixLength(phrase, "x"))
    }

    @Test
    fun typingMoreThanThePhraseNeverLightsMoreThanThePhrase() {
        val phrase = "Bom dia para mim mesmo."
        assertEquals(phrase.length, MissionGenerator.matchedPrefixLength(phrase, "bom dia para mim mesmo e mais um pouco"))
    }
}
