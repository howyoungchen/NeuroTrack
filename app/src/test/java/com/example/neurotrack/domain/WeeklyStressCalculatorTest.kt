package com.example.neurotrack.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WeeklyStressCalculatorTest {
    private val zoneId = ZoneId.of("UTC")
    private val weekStart = LocalDate.of(2026, 7, 13)

    @Test
    fun calculate_usesWeeklyAssessmentAndFourCompletedPractices() {
        val result = WeeklyStressCalculator.calculate(
            weekStart = weekStart,
            assessments = listOf(assessment(day = 6, score = 24)),
            sessions = listOf(0L, 2L, 4L, 6L).map { completedSession(it) },
            asOfDate = weekStart.plusDays(6),
            zoneId = zoneId,
        )

        assertEquals(8.0, result.score!!, 0.001)
        assertEquals(1.0, result.mindfulnessCompletionRate, 0.001)
        assertEquals(4, result.completedPractices)
        assertEquals(StressBand.HIGH, result.band)
    }

    @Test
    fun calculate_missingPracticesRaiseWeeklyPressure() {
        val result = WeeklyStressCalculator.calculate(
            weekStart = weekStart,
            assessments = listOf(assessment(day = 6, score = 15)),
            sessions = emptyList(),
            asOfDate = weekStart.plusDays(6),
            zoneId = zoneId,
        )

        assertEquals(7.0, result.score!!, 0.001)
        assertEquals(0.0, result.mindfulnessCompletionRate, 0.001)
        assertEquals(StressBand.HIGH, result.band)
    }

    @Test
    fun calculate_withoutWeeklyAssessmentDoesNotInventPressureScore() {
        val result = WeeklyStressCalculator.calculate(
            weekStart = weekStart,
            assessments = emptyList(),
            sessions = listOf(completedSession(0L)),
            asOfDate = weekStart.plusDays(6),
            zoneId = zoneId,
        )

        assertNull(result.score)
        assertNull(result.band)
        assertEquals(1, result.completedPractices)
    }

    @Test
    fun calculate_doesNotPenalizeFuturePracticeDays() {
        val result = WeeklyStressCalculator.calculate(
            weekStart = weekStart,
            assessments = listOf(assessment(day = 0, score = 15)),
            sessions = listOf(completedSession(0L)),
            asOfDate = weekStart,
            zoneId = zoneId,
        )

        assertEquals(5.0, result.score!!, 0.001)
        assertEquals(1, result.scheduledPractices)
        assertEquals(1.0, result.mindfulnessCompletionRate, 0.001)
    }

    @Test
    fun calculate_keepsTodaysReflectionBeforePracticeReminder() {
        val result = WeeklyStressCalculator.calculate(
            weekStart = weekStart,
            assessments = listOf(assessment(day = 0, score = 15)),
            sessions = emptyList(),
            asOfDate = weekStart,
            practiceDueThroughDate = weekStart.minusDays(1),
            zoneId = zoneId,
        )

        assertEquals(5.0, result.score!!, 0.001)
        assertEquals(0, result.scheduledPractices)
    }

    @Test
    fun trend_returnsOnePointPerWeek() {
        val previousWeek = weekStart.minusWeeks(1)
        val points = WeeklyStressCalculator.trend(
            assessments = listOf(
                WeeklyAssessmentRecord(previousWeek.plusDays(6).atStartOfDay(zoneId).toInstant().toEpochMilli(), 9),
                assessment(day = 6, score = 21),
            ),
            sessions = emptyList(),
            endWeekStart = weekStart,
            weeks = 2,
            zoneId = zoneId,
        )

        assertEquals(listOf(previousWeek, weekStart), points.map { it.weekStart })
        assertEquals(2, points.size)
    }

    private fun assessment(day: Long, score: Int): WeeklyAssessmentRecord =
        WeeklyAssessmentRecord(
            createdAtMillis = weekStart.plusDays(day).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            totalScore = score,
        )

    private fun completedSession(day: Long): MindfulnessSessionRecord =
        MindfulnessSessionRecord(
            startedAtMillis = weekStart.plusDays(day).atTime(20, 0).atZone(zoneId).toInstant().toEpochMilli(),
            endedAtMillis = weekStart.plusDays(day).atTime(20, 10).atZone(zoneId).toInstant().toEpochMilli(),
            plannedDurationMinutes = 10,
            status = MindfulnessSessionStatus.COMPLETED,
        )
}
