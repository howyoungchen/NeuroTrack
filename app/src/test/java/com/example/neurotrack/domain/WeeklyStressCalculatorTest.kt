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
    fun calculate_usesWeeklyAssessmentAndSixCompletedCourses() {
        val result = WeeklyStressCalculator.calculate(
            weekStart = weekStart,
            assessments = listOf(assessment(score = 24)),
            sessions = (1..6).map(::completedSession),
            zoneId = zoneId,
        )

        assertEquals(8.0, result.score!!, 0.001)
        assertEquals(1.0, result.mindfulnessCompletionRate, 0.001)
        assertEquals(6, result.completedPractices)
        assertEquals(6, result.scheduledPractices)
        assertEquals(StressBand.HIGH, result.band)
    }

    @Test
    fun calculate_missingCoursesRaiseWeeklyPressure() {
        val result = WeeklyStressCalculator.calculate(
            weekStart = weekStart,
            assessments = listOf(assessment(score = 15)),
            sessions = emptyList(),
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
            sessions = listOf(completedSession(1)),
            zoneId = zoneId,
        )

        assertNull(result.score)
        assertNull(result.band)
        assertEquals(1, result.completedPractices)
    }

    @Test
    fun calculate_duplicateCourseOnlyCountsOnce() {
        val result = WeeklyStressCalculator.calculate(
            weekStart = weekStart,
            assessments = listOf(assessment(score = 15)),
            sessions = listOf(completedSession(1), completedSession(1)),
            zoneId = zoneId,
        )

        assertEquals(1, result.completedPractices)
        assertEquals(1.0 / 6.0, result.mindfulnessCompletionRate, 0.001)
    }

    @Test
    fun trend_returnsOnePointPerCompletedWeek() {
        val previousWeek = weekStart.minusWeeks(1)
        val points = WeeklyStressCalculator.trend(
            assessments = listOf(
                WeeklyAssessmentRecord(
                    weekStart = previousWeek,
                    createdAtMillis = millis(previousWeek.plusDays(6)),
                    totalScore = 9,
                ),
                assessment(score = 21),
            ),
            sessions = emptyList(),
            endWeekStart = weekStart,
            weeks = 2,
            zoneId = zoneId,
        )

        assertEquals(listOf(previousWeek, weekStart), points.map { it.weekStart })
        assertEquals(2, points.size)
    }

    private fun assessment(score: Int): WeeklyAssessmentRecord =
        WeeklyAssessmentRecord(
            weekStart = weekStart,
            createdAtMillis = millis(weekStart.plusDays(6)),
            totalScore = score,
        )

    private fun completedSession(lessonId: Int): MindfulnessSessionRecord =
        MindfulnessSessionRecord(
            startedAtMillis = millis(weekStart.plusDays(lessonId.toLong() - 1)),
            status = MindfulnessSessionStatus.COMPLETED,
            lessonId = lessonId,
        )

    private fun millis(date: LocalDate): Long =
        date.atTime(20, 0).atZone(zoneId).toInstant().toEpochMilli()
}
