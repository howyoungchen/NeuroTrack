package com.example.neurotrack.ui

import com.example.neurotrack.domain.MindfulnessSessionRecord
import com.example.neurotrack.domain.MindfulnessSessionStatus
import com.example.neurotrack.domain.WeeklyAssessmentRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StatusDisplayModelTest {
    private val zoneId = ZoneId.of("UTC")
    private val completedWeek = LocalDate.of(2026, 7, 13)

    @Test
    fun buildStatusDisplayModel_exposesLastCompletedWeekAndTrend() {
        val model = buildStatusDisplayModel(
            assessments = listOf(
                WeeklyAssessmentRecord(completedWeek, millis(completedWeek.plusDays(6)), 15),
            ),
            sessions = listOf(completedSession(completedWeek)),
            now = LocalDateTime.of(2026, 7, 20, 5, 0),
            zoneId = zoneId,
        )

        assertEquals(completedWeek, model.current.weekStart)
        assertEquals(1, model.current.completedPractices)
        assertEquals(8, model.trend.size)
        assertEquals(completedWeek, model.trend.last().weekStart)
    }

    @Test
    fun buildStatusDisplayModel_beforeMondayRefreshKeepsEarlierCompletedWeek() {
        val model = buildStatusDisplayModel(
            assessments = emptyList(),
            sessions = emptyList(),
            now = LocalDateTime.of(2026, 7, 20, 4, 59),
            zoneId = zoneId,
        )

        assertEquals(LocalDate.of(2026, 7, 6), model.current.weekStart)
        assertNull(model.current.score)
    }

    @Test
    fun buildStatusDisplayModel_usesConfiguredRefreshDay() {
        val model = buildStatusDisplayModel(
            assessments = emptyList(),
            sessions = emptyList(),
            now = LocalDateTime.of(2026, 7, 16, 5, 0),
            zoneId = zoneId,
            refreshDay = DayOfWeek.THURSDAY,
        )

        assertEquals(LocalDate.of(2026, 7, 9), model.current.weekStart)
    }

    @Test
    fun formatWeekLabel_isLocaleNeutral() {
        assertEquals("7/13", formatWeekLabel(LocalDate.of(2026, 7, 13)))
    }

    private fun completedSession(date: LocalDate) = MindfulnessSessionRecord(
        startedAtMillis = millis(date),
        status = MindfulnessSessionStatus.COMPLETED,
        lessonId = 1,
    )

    private fun millis(date: LocalDate): Long =
        date.atTime(20, 0).atZone(zoneId).toInstant().toEpochMilli()
}
