package com.example.neurotrack.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class SleepAnalyzerTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")
    private val targetDate = LocalDate.of(2026, 7, 4)

    @Test
    fun windowFor_usesPreviousEveningToTargetNoon() {
        val window = SleepAnalyzer.windowFor(targetDate, zoneId)

        assertEquals(millis("2026-07-03", "20:00"), window.startMillis)
        assertEquals(millis("2026-07-04", "12:00"), window.endMillis)
    }

    @Test
    fun targetDateForAnalysis_beforeNoonUsesYesterday() {
        assertEquals(
            LocalDate.of(2026, 7, 8),
            SleepAnalyzer.targetDateForAnalysis(LocalDateTime.of(2026, 7, 9, 11, 37)),
        )
    }

    @Test
    fun targetDateForAnalysis_afterNoonUsesToday() {
        assertEquals(
            LocalDate.of(2026, 7, 9),
            SleepAnalyzer.targetDateForAnalysis(LocalDateTime.of(2026, 7, 9, 12, 0)),
        )
    }

    @Test
    fun analyze_createsRecordFromLongestScreenOffInterval() {
        val record = SleepAnalyzer.analyze(
            targetDate = targetDate,
            events = listOf(
                event("2026-07-03", "22:45", ScreenEventType.SCREEN_ON),
                event("2026-07-03", "23:10", ScreenEventType.SCREEN_OFF),
                event("2026-07-04", "07:20", ScreenEventType.SCREEN_ON),
                event("2026-07-04", "09:00", ScreenEventType.SCREEN_OFF),
                event("2026-07-04", "09:30", ScreenEventType.SCREEN_ON),
            ),
            zoneId = zoneId,
            nowMillis = millis("2026-07-04", "12:05"),
        )

        assertFalse(record.isMissing)
        assertEquals(targetDate.toEpochDay(), record.dateEpochDay)
        assertEquals(millis("2026-07-03", "23:10"), record.sleepStartMillis)
        assertEquals(millis("2026-07-04", "07:20"), record.sleepEndMillis)
        assertEquals(490, record.durationMinutes)
        assertEquals(0, record.wakeUpCount)
    }

    @Test
    fun analyze_mergesShortAwakeGapsIntoOneSleepInterval() {
        val record = SleepAnalyzer.analyze(
            targetDate = targetDate,
            events = listOf(
                event("2026-07-03", "23:00", ScreenEventType.SCREEN_OFF),
                event("2026-07-04", "02:00", ScreenEventType.SCREEN_ON),
                event("2026-07-04", "02:10", ScreenEventType.SCREEN_OFF),
                event("2026-07-04", "07:00", ScreenEventType.SCREEN_ON),
            ),
            zoneId = zoneId,
            nowMillis = millis("2026-07-04", "12:05"),
        )

        assertFalse(record.isMissing)
        assertEquals(millis("2026-07-03", "23:00"), record.sleepStartMillis)
        assertEquals(millis("2026-07-04", "07:00"), record.sleepEndMillis)
        assertEquals(480, record.durationMinutes)
        assertEquals(1, record.wakeUpCount)
    }

    @Test
    fun analyze_stopsAtFirstMorningScreenUseWhenShortWakeupsBecomeFrequent() {
        val record = SleepAnalyzer.analyze(
            targetDate = targetDate,
            events = listOf(
                event("2026-07-03", "23:00", ScreenEventType.SCREEN_OFF),
                event("2026-07-04", "08:50", ScreenEventType.SCREEN_ON),
                event("2026-07-04", "08:50:05", ScreenEventType.SCREEN_OFF),
                event("2026-07-04", "08:53", ScreenEventType.SCREEN_ON),
                event("2026-07-04", "08:53:10", ScreenEventType.SCREEN_OFF),
                event("2026-07-04", "08:55", ScreenEventType.SCREEN_ON),
                event("2026-07-04", "08:55:08", ScreenEventType.SCREEN_OFF),
                event("2026-07-04", "09:00", ScreenEventType.SCREEN_ON),
                event("2026-07-04", "09:00:10", ScreenEventType.SCREEN_OFF),
                event("2026-07-04", "11:53", ScreenEventType.SCREEN_ON),
            ),
            zoneId = zoneId,
            nowMillis = millis("2026-07-04", "12:05"),
        )

        assertFalse(record.isMissing)
        assertEquals(millis("2026-07-03", "23:00"), record.sleepStartMillis)
        assertEquals(millis("2026-07-04", "08:50"), record.sleepEndMillis)
        assertEquals(590, record.durationMinutes)
        assertEquals(0, record.wakeUpCount)
    }

    @Test
    fun analyze_stopsAtLongMorningScreenUse() {
        val record = SleepAnalyzer.analyze(
            targetDate = targetDate,
            events = listOf(
                event("2026-07-03", "23:00", ScreenEventType.SCREEN_OFF),
                event("2026-07-04", "07:00", ScreenEventType.SCREEN_ON),
                event("2026-07-04", "07:06", ScreenEventType.SCREEN_OFF),
                event("2026-07-04", "09:00", ScreenEventType.SCREEN_ON),
            ),
            zoneId = zoneId,
            nowMillis = millis("2026-07-04", "12:05"),
        )

        assertFalse(record.isMissing)
        assertEquals(millis("2026-07-03", "23:00"), record.sleepStartMillis)
        assertEquals(millis("2026-07-04", "07:00"), record.sleepEndMillis)
        assertEquals(480, record.durationMinutes)
        assertEquals(0, record.wakeUpCount)
    }

    @Test
    fun analyze_usesLocationWakeSignalInsideLongScreenOffInterval() {
        val record = SleepAnalyzer.analyze(
            targetDate = targetDate,
            observations = SleepObservations(
                screenEvents = listOf(
                    event("2026-07-03", "23:00", ScreenEventType.SCREEN_OFF),
                    event("2026-07-04", "10:00", ScreenEventType.SCREEN_ON),
                ),
                locationSignals = listOf(
                    LocationSleepSignal(
                        timestampMillis = millis("2026-07-04", "06:45"),
                        atSleepPlace = false,
                        stationary = false,
                        leftSleepPlace = true,
                    ),
                ),
            ),
            zoneId = zoneId,
            nowMillis = millis("2026-07-04", "12:05"),
        )

        assertFalse(record.isMissing)
        assertEquals(millis("2026-07-03", "23:00"), record.sleepStartMillis)
        assertEquals(millis("2026-07-04", "06:45"), record.sleepEndMillis)
        assertEquals(465, record.durationMinutes)
    }

    @Test
    fun analyze_marksMissingWhenLongestIntervalIsTooShort() {
        val record = SleepAnalyzer.analyze(
            targetDate = targetDate,
            events = listOf(
                event("2026-07-04", "09:00", ScreenEventType.SCREEN_OFF),
                event("2026-07-04", "10:30", ScreenEventType.SCREEN_ON),
            ),
            zoneId = zoneId,
            nowMillis = millis("2026-07-04", "12:05"),
        )

        assertTrue(record.isMissing)
        assertEquals(0, record.durationMinutes)
        assertEquals(0, record.sleepStartMillis)
        assertEquals(0, record.sleepEndMillis)
    }

    @Test
    fun analyze_marksMissingWhenThereAreNoScreenEvents() {
        val record = SleepAnalyzer.analyze(
            targetDate = targetDate,
            events = emptyList(),
            zoneId = zoneId,
            nowMillis = millis("2026-07-04", "12:05"),
        )

        assertTrue(record.isMissing)
        assertEquals(targetDate.toEpochDay(), record.dateEpochDay)
    }

    private fun event(date: String, time: String, type: ScreenEventType): ScreenEvent =
        ScreenEvent(
            timestampMillis = millis(date, time),
            type = type,
        )

    private fun millis(date: String, time: String): Long =
        LocalDate.parse(date)
            .atTime(LocalTime.parse(time))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
}
