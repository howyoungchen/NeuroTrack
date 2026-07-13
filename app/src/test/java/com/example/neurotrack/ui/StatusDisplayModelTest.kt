package com.example.neurotrack.ui

import com.example.neurotrack.domain.MindfulnessSessionRecord
import com.example.neurotrack.domain.MindfulnessSessionStatus
import com.example.neurotrack.domain.WeeklyAssessmentRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StatusDisplayModelTest {
    private val zoneId = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 7, 15)
    private val weekStart = LocalDate.of(2026, 7, 13)

    @Test
    fun buildStatusDisplayModel_exposesOnlyWeeklyCurrentAndTrend() {
        val model = buildStatusDisplayModel(
            assessments = listOf(
                WeeklyAssessmentRecord(millis(weekStart.plusDays(1)), 15),
            ),
            sessions = listOf(completedSession(weekStart)),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(1, model.current.completedPractices)
        assertEquals(8, model.trend.size)
        assertEquals(weekStart, model.trend.last().weekStart)
    }

    @Test
    fun buildStatusDisplayModel_withoutReflectionKeepsScoreMissing() {
        val model = buildStatusDisplayModel(
            assessments = emptyList(),
            sessions = listOf(completedSession(weekStart)),
            today = today,
            zoneId = zoneId,
        )

        assertNull(model.current.score)
    }

    @Test
    fun dueThroughDate_waitsUntilConfiguredReminderTime() {
        val morning = LocalDateTime.of(2026, 7, 15, 8, 0)
        val evening = LocalDateTime.of(2026, 7, 15, 20, 30)

        assertEquals(LocalDate.of(2026, 7, 14), dueThroughDate(morning, 20, 0))
        assertEquals(LocalDate.of(2026, 7, 15), dueThroughDate(evening, 20, 0))
    }

    @Test
    fun formatWeekLabel_isLocaleNeutral() {
        assertEquals("7/13", formatWeekLabel(LocalDate.of(2026, 7, 13)))
    }

    private fun completedSession(date: LocalDate) = MindfulnessSessionRecord(
        startedAtMillis = millis(date),
        endedAtMillis = millis(date) + 300_000,
        plannedDurationMinutes = 5,
        status = MindfulnessSessionStatus.COMPLETED,
    )

    private fun millis(date: LocalDate): Long =
        date.atTime(20, 0).atZone(zoneId).toInstant().toEpochMilli()
}
