package com.example.neurotrack.domain

import com.example.neurotrack.data.AssessmentRecordEntity
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

    private fun assessment(
        date: String,
        time: String,
        totalScore: Int,
    ): AssessmentRecordEntity =
        AssessmentRecordEntity(
            createdAtMillis = millis(date, time),
            answersCsv = "",
            totalScore = totalScore,
        )

    private fun millis(date: String, time: String): Long =
        LocalDate.parse(date)
            .atTime(LocalTime.parse(time))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
}
