package com.example.neurotrack.ui

import com.example.neurotrack.domain.SleepRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class StatusDisplayModelTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 7, 4)

    @Test
    fun buildStatusDisplayModel_usesMostRecentUsableSleepRecordForYesterdayCard() {
        val result = buildStatusDisplayModel(
            assessments = emptyList(),
            sleepRecords = listOf(
                sleepRecord(dateEpochDay = today.minusDays(1).toEpochDay(), durationMinutes = 480),
                sleepRecord(dateEpochDay = today.toEpochDay(), durationMinutes = 450),
            ),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(7.5, result.yesterdaySleep.durationHours ?: -1.0, 0.0001)
        assertEquals("23:00", result.yesterdaySleep.bedtimeText)
        assertEquals("06:30", result.yesterdaySleep.wakeTimeText)
    }

    @Test
    fun buildStatusDisplayModel_ignoresMissingZeroDurationAndFutureSleepRecords() {
        val result = buildStatusDisplayModel(
            assessments = emptyList(),
            sleepRecords = listOf(
                sleepRecord(dateEpochDay = today.plusDays(1).toEpochDay(), durationMinutes = 480),
                sleepRecord(dateEpochDay = today.toEpochDay(), durationMinutes = 0),
                sleepRecord(dateEpochDay = today.minusDays(1).toEpochDay(), durationMinutes = 460, isMissing = true),
            ),
            today = today,
            zoneId = zoneId,
        )

        assertFalse(result.yesterdaySleep.hasData)
        assertNull(result.yesterdaySleep.durationHours)
    }

    @Test
    fun buildStatusDisplayModel_buildsWeekSleepPointsWithGaps() {
        val result = buildStatusDisplayModel(
            assessments = emptyList(),
            sleepRecords = listOf(
                sleepRecord(dateEpochDay = today.minusDays(6).toEpochDay(), durationMinutes = 420),
                sleepRecord(dateEpochDay = today.toEpochDay(), durationMinutes = 480),
            ),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(7, result.weekSleep.points.size)
        assertEquals(420f, result.weekSleep.points.first().durationMinutes)
        assertEquals(480f, result.weekSleep.points.last().durationMinutes)
        assertTrue(result.weekSleep.points.drop(1).dropLast(1).all { it.durationMinutes == null })
        assertTrue(result.weekSleep.chartLabels.first() is SleepChartLabel.Weekday)
    }

    @Test
    fun buildStatusDisplayModel_mapsNoSleepInsight() {
        val result = buildStatusDisplayModel(
            assessments = emptyList(),
            sleepRecords = emptyList(),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(listOf(StatusInsight.NO_SLEEP), result.insights)
    }

    @Test
    fun buildStatusDisplayModel_buildsPressureAndMonthChartLabels() {
        val result = buildStatusDisplayModel(
            assessments = emptyList(),
            sleepRecords = emptyList(),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(listOf("6/5", "6/20", "7/4"), result.pressureTrend.chartLabels)
        assertEquals(
            listOf("6/5", "6/12", "6/19", "6/26", "7/3"),
            result.monthSleep.chartLabels.map { (it as SleepChartLabel.Text).value },
        )
    }

    private fun sleepRecord(
        dateEpochDay: Long,
        durationMinutes: Int,
        isMissing: Boolean = false,
    ): SleepRecord {
        val date = LocalDate.ofEpochDay(dateEpochDay)
        return SleepRecord(
            dateEpochDay = dateEpochDay,
            sleepStartMillis = millis(date, LocalTime.of(23, 0)),
            sleepEndMillis = millis(date.plusDays(1), LocalTime.of(6, 30)),
            durationMinutes = durationMinutes,
            wakeUpCount = 0,
            isMissing = isMissing,
            createdAtMillis = millis(date.plusDays(1), LocalTime.NOON),
        )
    }

    private fun millis(date: LocalDate, time: LocalTime): Long =
        date.atTime(time)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
}
