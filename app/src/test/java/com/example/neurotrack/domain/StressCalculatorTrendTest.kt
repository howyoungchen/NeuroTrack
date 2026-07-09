package com.example.neurotrack.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class StressCalculatorTrendTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun trendPoints_returnsDailyPointsForRequestedWindow() {
        val endDate = LocalDate.of(2026, 7, 4)

        val points = StressCalculator.trendPoints(
            assessments = emptyList(),
            sleepRecords = emptyList(),
            endDate = endDate,
            days = 30,
            zoneId = zoneId,
        )

        assertEquals(30, points.size)
        assertEquals(LocalDate.of(2026, 6, 5), points.first().date)
        assertEquals(endDate, points.last().date)
        assertNull(points.last().score)
    }

    @Test
    fun trendPoints_usesOnlyRecordsAvailableAtEachDate() {
        val endDate = LocalDate.of(2026, 7, 4)
        val assessments = listOf(
            assessment("2026-07-03", "10:00", totalScore = 6),
            assessment("2026-07-05", "10:00", totalScore = 30),
        )

        val points = StressCalculator.trendPoints(
            assessments = assessments,
            sleepRecords = emptyList(),
            endDate = endDate,
            days = 3,
            zoneId = zoneId,
        )

        assertNull(points[0].score)
        assertEquals(2.0, points[1].score ?: -1.0, 0.0001)
        assertEquals(2.0, points[2].score ?: -1.0, 0.0001)
    }

    @Test
    fun trendPoints_reflectsPressureChangeInsideWindow() {
        val endDate = LocalDate.of(2026, 7, 4)
        val assessments = listOf(
            assessment("2026-07-01", "10:00", totalScore = 6),
            assessment("2026-07-04", "10:00", totalScore = 24),
        )

        val points = StressCalculator.trendPoints(
            assessments = assessments,
            sleepRecords = emptyList(),
            endDate = endDate,
            days = 4,
            zoneId = zoneId,
        )

        assertEquals(2.0, points.first().score ?: -1.0, 0.0001)
        assertTrue((points.last().score ?: 0.0) > (points.first().score ?: 0.0))
    }

    @Test
    fun bandForScore_usesSharedThresholds() {
        assertNull(StressCalculator.bandForScore(null))
        assertEquals(StressBand.LOW, StressCalculator.bandForScore(3.99))
        assertEquals(StressBand.MEDIUM, StressCalculator.bandForScore(4.0))
        assertEquals(StressBand.HIGH, StressCalculator.bandForScore(7.0))
    }

    @Test
    fun calculate_ignoresFutureSleepRecordsInSleepMetrics() {
        val result = StressCalculator.calculate(
            assessments = listOf(assessment("2026-07-04", "10:00", totalScore = 15)),
            sleepRecords = listOf(
                sleepRecord(LocalDate.of(2026, 7, 4), durationMinutes = 480),
                sleepRecord(LocalDate.of(2026, 7, 5), durationMinutes = 60),
            ),
            nowMillis = millis("2026-07-04", "12:00"),
            zoneId = zoneId,
        )

        assertEquals(480.0, result.metrics.averageDurationMinutes, 0.0001)
    }

    private fun assessment(
        date: String,
        time: String,
        totalScore: Int,
    ): AssessmentScoreRecord =
        AssessmentScoreRecord(
            createdAtMillis = millis(date, time),
            totalScore = totalScore,
        )

    private fun sleepRecord(date: LocalDate, durationMinutes: Int): SleepRecord =
        SleepRecord(
            dateEpochDay = date.toEpochDay(),
            sleepStartMillis = date.minusDays(1)
                .atTime(LocalTime.of(23, 0))
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli(),
            sleepEndMillis = date
                .atTime(LocalTime.of(7, 0))
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli(),
            durationMinutes = durationMinutes,
            wakeUpCount = 0,
            isMissing = false,
            createdAtMillis = millis(date.toString(), "12:00"),
        )

    private fun millis(date: String, time: String): Long =
        LocalDate.parse(date)
            .atTime(LocalTime.parse(time))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
}
