package com.wakewakeup.session

import com.wakewakeup.data.Difficulty
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale
import kotlin.random.Random

data class MathQuestion(val question: String, val answer: String)

object MissionGenerator {

    // Written without accents on purpose: nobody should have to hunt for "ç" or "ã" half asleep.
    private val phrasesPt = listOf(
        "Levantar agora vale a pena.",
        "Hoje eu acordo de verdade.",
        "Abro os olhos e saio da cama.",
        "Cada minuto acordado conta.",
        "Nada de voltar a dormir agora.",
        "Desligo o alarme e levanto.",
        "A cama fica para a noite.",
        "Estou de olhos bem abertos.",
        "Saio da cama com vontade.",
        "Levanto, lavo o rosto e sigo em frente.",
        "O dia de hoje me espera.",
        "Bom dia para mim mesmo.",
    )
    private val phrasesEn = listOf(
        "Getting up now is worth it.",
        "Today I really wake up.",
        "I open my eyes and get up.",
        "Every awake minute counts.",
        "No going back to sleep now.",
        "I turn off the alarm and get up.",
        "The bed can wait for tonight.",
        "My eyes are wide open.",
        "Today starts out of bed.",
        "I get up, wash my face and move on.",
        "The sun is already up.",
        "Good morning to me.",
    )

    fun mathQuestion(difficulty: Difficulty): MathQuestion {
        fun r(min: Int, max: Int) = Random.nextInt(min, max + 1)
        return when (difficulty) {
            Difficulty.EASY -> {
                val a = r(2, 9); val b = r(2, 9)
                MathQuestion("$a + $b", (a + b).toString())
            }
            Difficulty.HARD -> {
                val a = r(12, 49); val b = r(6, 19)
                MathQuestion("$a × $b", (a * b).toString())
            }
            Difficulty.MEDIUM -> {
                val a = r(21, 89); val b = r(14, 79)
                MathQuestion("$a + $b", (a + b).toString())
            }
        }
    }

    /** A different phrase each day (the same one all day, so a retry isn't a new phrase). */
    fun phraseOfTheDay(date: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): String {
        val pool = if (locale.language == "pt") phrasesPt else phrasesEn
        return pool[Math.floorMod(date.toEpochDay(), pool.size.toLong()).toInt()]
    }

    /** Case, accents and punctuation don't count; runs of spaces count as one. */
    fun normalizePhrase(text: String): String = buildString {
        for (ch in text) {
            val n = normalizeChar(ch) ?: continue
            if (n == ' ' && (isEmpty() || last() == ' ')) continue
            append(n)
        }
    }.trimEnd()

    fun phraseMatches(typed: String, phrase: String): Boolean =
        normalizePhrase(typed).isNotEmpty() && normalizePhrase(typed) == normalizePhrase(phrase)

    /**
     * How many characters of [phrase] (as displayed, punctuation included) the user has typed
     * correctly so far, from the start — drives the lit/unlit split of the phrase display.
     */
    fun matchedPrefixLength(phrase: String, typed: String): Int {
        val t = buildString {
            for (ch in typed) {
                val n = normalizeChar(ch) ?: continue
                if (n == ' ' && (isEmpty() || last() == ' ')) continue
                append(n)
            }
        }
        var j = 0
        var matched = 0
        for (i in phrase.indices) {
            val n = normalizeChar(phrase[i])
            if (n == null) {
                if (matched == i && j > 0) matched = i + 1 else if (j == 0) break
                continue
            }
            if (j < t.length && t[j] == n) {
                j++
                matched = i + 1
            } else {
                break
            }
        }
        return matched
    }

    private fun normalizeChar(ch: Char): Char? {
        if (ch.isWhitespace()) return ' '
        if (!ch.isLetterOrDigit()) return null
        val stripped = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD)
            .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
        return stripped.firstOrNull()?.lowercaseChar()
    }
}
