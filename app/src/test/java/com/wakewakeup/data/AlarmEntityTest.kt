package com.wakewakeup.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmEntityTest {

    @Test
    fun alarmSurvivesTheRoundTripToTheDatabase() {
        val alarm = Alarm(
            id = 7, hour = 6, minute = 5, label = "Trabalho", days = setOf(4, 0, 2), enabled = false,
            taskType = TaskType.PHRASE, difficulty = Difficulty.HARD, taskCount = 5,
            soundUri = "android.resource://com.wakewakeup/raw/sirene_1", soundName = "Sirene",
            rampUp = false, skipDate = 20_000,
        )
        assertEquals(alarm, alarm.toEntity().toDomain())
    }

    @Test
    fun daysAreStoredSortedAsText() {
        assertEquals("0,2,4", Alarm(hour = 7, minute = 0, label = "", days = setOf(4, 0, 2)).toEntity().daysCsv)
        assertEquals("", Alarm(hour = 7, minute = 0, label = "", days = emptySet()).toEntity().daysCsv)
        assertEquals(emptySet<Int>(), Alarm(hour = 7, minute = 0, label = "", days = emptySet()).toEntity().toDomain().days)
    }

    @Test
    fun unknownTaskValuesFallBackToDefaults() {
        val entity = AlarmEntity(hour = 7, minute = 0, label = "", daysCsv = "", enabled = true, taskType = "DANCE", difficulty = "IMPOSSIBLE", taskCount = 3)
        val alarm = entity.toDomain()
        assertEquals(TaskType.MATH, alarm.taskType)
        assertEquals(Difficulty.MEDIUM, alarm.difficulty)
    }
}
