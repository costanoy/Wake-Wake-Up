package com.wakewakeup.session

import com.wakewakeup.data.Difficulty
import java.util.Locale
import kotlin.random.Random

data class MathQuestion(val question: String, val answer: String)

object MissionGenerator {

    private val phrasesPt = listOf(
        "o sol já está de pé",
        "levantar agora vale a pena",
        "hoje começa fora da cama",
    )
    private val phrasesEn = listOf(
        "the sun is already up",
        "getting up now is worth it",
        "today starts out of bed",
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

    fun randomPhrase(locale: Locale = Locale.getDefault()): String {
        val pool = if (locale.language == "pt") phrasesPt else phrasesEn
        return pool[Random.nextInt(pool.size)]
    }
}
