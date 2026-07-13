package com.example.neurotrack.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class MindfulnessScheduleTest {
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun weekStart_changesAtFiveOnMondayMorning() {
        assertEquals(
            LocalDate.of(2026, 7, 6),
            MindfulnessSchedule.weekStart(LocalDateTime.of(2026, 7, 13, 4, 59)),
        )
        assertEquals(
            LocalDate.of(2026, 7, 13),
            MindfulnessSchedule.weekStart(LocalDateTime.of(2026, 7, 13, 5, 0)),
        )
    }

    @Test
    fun weekStart_usesConfiguredRefreshDayAtFive() {
        assertEquals(
            LocalDate.of(2026, 7, 9),
            MindfulnessSchedule.weekStart(
                LocalDateTime.of(2026, 7, 16, 4, 59),
                DayOfWeek.THURSDAY,
            ),
        )
        assertEquals(
            LocalDate.of(2026, 7, 16),
            MindfulnessSchedule.weekStart(
                LocalDateTime.of(2026, 7, 16, 5, 0),
                DayOfWeek.THURSDAY,
            ),
        )
    }

    @Test
    fun nextAssessmentReminder_isSundayAtTenAtNight() {
        assertEquals(
            LocalDateTime.of(2026, 7, 19, 22, 0),
            MindfulnessSchedule.nextAssessmentReminder(LocalDateTime.of(2026, 7, 13, 21, 0)),
        )
        assertEquals(
            LocalDateTime.of(2026, 7, 26, 22, 0),
            MindfulnessSchedule.nextAssessmentReminder(LocalDateTime.of(2026, 7, 19, 22, 0)),
        )
    }

    @Test
    fun assessmentReminderWindow_onlyOpensAfterTenOnSunday() {
        assertFalse(
            MindfulnessSchedule.isAssessmentReminderWindow(LocalDateTime.of(2026, 7, 19, 21, 59)),
        )
        assertTrue(
            MindfulnessSchedule.isAssessmentReminderWindow(LocalDateTime.of(2026, 7, 19, 22, 0)),
        )
        assertFalse(
            MindfulnessSchedule.isAssessmentReminderWindow(LocalDateTime.of(2026, 7, 20, 0, 1)),
        )
    }

    @Test
    fun assessmentDay_isSundayOnly() {
        assertFalse(MindfulnessSchedule.isAssessmentDay(LocalDate.of(2026, 7, 18)))
        assertTrue(MindfulnessSchedule.isAssessmentDay(LocalDate.of(2026, 7, 19)))
        assertFalse(MindfulnessSchedule.isAssessmentDay(LocalDate.of(2026, 7, 20)))
    }

    @Test
    fun assessmentDay_isDayBeforeConfiguredRefreshDay() {
        assertEquals(DayOfWeek.WEDNESDAY, MindfulnessSchedule.assessmentDay(DayOfWeek.THURSDAY))
        assertTrue(
            MindfulnessSchedule.isAssessmentDay(
                LocalDate.of(2026, 7, 15),
                DayOfWeek.THURSDAY,
            ),
        )
        assertFalse(
            MindfulnessSchedule.isAssessmentDay(
                LocalDate.of(2026, 7, 16),
                DayOfWeek.THURSDAY,
            ),
        )
    }

    @Test
    fun nextAssessmentReminder_tracksConfiguredRefreshDay() {
        assertEquals(
            LocalDateTime.of(2026, 7, 15, 22, 0),
            MindfulnessSchedule.nextAssessmentReminder(
                LocalDateTime.of(2026, 7, 13, 21, 0),
                DayOfWeek.THURSDAY,
            ),
        )
    }

    @Test
    fun completedLessonIds_countsEachCourseOncePerRound() {
        val weekStart = LocalDate.of(2026, 7, 13)
        val sessions = listOf(
            completedSession(weekStart, lessonId = 1),
            completedSession(weekStart.plusDays(1), lessonId = 1),
            completedSession(weekStart.plusDays(2), lessonId = 2),
        )

        assertEquals(
            setOf(1, 2),
            MindfulnessSchedule.completedLessonIds(weekStart, sessions, zoneId),
        )
    }

    private fun completedSession(date: LocalDate, lessonId: Int) = MindfulnessSessionRecord(
        startedAtMillis = date.atTime(20, 0).atZone(zoneId).toInstant().toEpochMilli(),
        status = MindfulnessSessionStatus.COMPLETED,
        lessonId = lessonId,
    )
}
