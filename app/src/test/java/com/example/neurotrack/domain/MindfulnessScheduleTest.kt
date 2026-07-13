package com.example.neurotrack.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class MindfulnessScheduleTest {
    @Test
    fun practiceDays_areMondayWednesdayFridayAndSunday() {
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
            MindfulnessSchedule.practiceDays,
        )
        assertTrue(MindfulnessSchedule.isPracticeDay(LocalDate.of(2026, 7, 13)))
        assertFalse(MindfulnessSchedule.isPracticeDay(LocalDate.of(2026, 7, 14)))
    }

    @Test
    fun nextReminder_movesToNextConfiguredPracticeDay() {
        val next = MindfulnessSchedule.nextReminder(
            now = LocalDateTime.of(2026, 7, 13, 21, 0),
            hour = 20,
            minute = 30,
        )

        assertEquals(LocalDateTime.of(2026, 7, 15, 20, 30), next)
    }
}
