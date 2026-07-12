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
    fun windowFor_usesEveningToEveningSleepCycle() {
        val window = SleepAnalyzer.windowFor(targetDate, zoneId)

        assertEquals(millis("2026-07-03", "18:00"), window.startMillis)
        assertEquals(millis("2026-07-04", "18:00"), window.endMillis)
    }

    @Test
    fun windowForCurrentAnalysis_endsAtCurrentTime() {
        val nowMillis = millis("2026-07-04", "08:15")
        val window = SleepAnalyzer.windowForCurrentAnalysis(nowMillis, zoneId)

        assertEquals(millis("2026-07-03", "18:00"), window.startMillis)
        assertEquals(nowMillis, window.endMillis)
    }

    @Test
    fun windowForCurrentAnalysis_doesNotCutOffAfterEveningBoundary() {
        val nowMillis = millis("2026-07-04", "20:30")
        val window = SleepAnalyzer.windowForCurrentAnalysis(nowMillis, zoneId)

        assertEquals(millis("2026-07-03", "18:00"), window.startMillis)
        assertEquals(nowMillis, window.endMillis)
    }

    @Test
    fun targetDateForAnalysis_morningUsesToday() {
        assertEquals(
            LocalDate.of(2026, 7, 9),
            SleepAnalyzer.targetDateForAnalysis(LocalDateTime.of(2026, 7, 9, 11, 37)),
        )
    }

    @Test
    fun targetDateForAnalysis_eveningStillUsesCurrentWakeDate() {
        assertEquals(
            LocalDate.of(2026, 7, 9),
            SleepAnalyzer.targetDateForAnalysis(LocalDateTime.of(2026, 7, 9, 18, 0)),
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
    fun analyze_acceptsLateSleepThatEndsAfterNoon() {
        val record = SleepAnalyzer.analyze(
            targetDate = targetDate,
            events = listOf(
                event("2026-07-04", "04:30", ScreenEventType.SCREEN_OFF),
                event("2026-07-04", "13:15", ScreenEventType.SCREEN_ON),
            ),
            zoneId = zoneId,
            nowMillis = millis("2026-07-04", "13:20"),
            window = SleepAnalyzer.windowForCurrentAnalysis(
                nowMillis = millis("2026-07-04", "13:20"),
                zoneId = zoneId,
            ),
        )

        assertFalse(record.isMissing)
        assertEquals(millis("2026-07-04", "04:30"), record.sleepStartMillis)
        assertEquals(millis("2026-07-04", "13:15"), record.sleepEndMillis)
        assertEquals(525, record.durationMinutes)
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
    fun analyze_mergesFrequentBriefMorningScreenUse() {
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
        assertEquals(millis("2026-07-04", "11:53"), record.sleepEndMillis)
        assertEquals(773, record.durationMinutes)
        assertEquals(4, record.wakeUpCount)
    }

    @Test
    fun analyze_keepsSleepingThroughBriefMorningScreenEventsUntilSustainedUse() {
        val record = SleepAnalyzer.analyze(
            targetDate = LocalDate.of(2026, 7, 12),
            events = listOf(
                event("2026-07-11", "23:30", ScreenEventType.SCREEN_OFF),
                event("2026-07-12", "10:00:00", ScreenEventType.SCREEN_ON),
                event("2026-07-12", "10:00:10", ScreenEventType.SCREEN_OFF),
                event("2026-07-12", "10:00:12", ScreenEventType.SCREEN_ON),
                event("2026-07-12", "10:00:45", ScreenEventType.SCREEN_OFF),
                event("2026-07-12", "10:10:17", ScreenEventType.SCREEN_ON),
                event("2026-07-12", "10:10:23", ScreenEventType.SCREEN_OFF),
                event("2026-07-12", "10:10:29", ScreenEventType.SCREEN_ON),
                event("2026-07-12", "10:10:38", ScreenEventType.SCREEN_OFF),
                event("2026-07-12", "10:30:01", ScreenEventType.SCREEN_ON),
                event("2026-07-12", "10:30:16", ScreenEventType.SCREEN_OFF),
                event("2026-07-12", "11:01:47", ScreenEventType.SCREEN_ON),
                event("2026-07-12", "11:02:51", ScreenEventType.SCREEN_OFF),
                event("2026-07-12", "11:32:48", ScreenEventType.SCREEN_ON),
                event("2026-07-12", "11:32:54", ScreenEventType.SCREEN_OFF),
                event("2026-07-12", "12:09:23", ScreenEventType.SCREEN_ON),
                event("2026-07-12", "12:12:30", ScreenEventType.SCREEN_OFF),
            ),
            zoneId = zoneId,
            nowMillis = millis("2026-07-12", "12:15"),
            window = SleepAnalyzer.windowForCurrentAnalysis(
                nowMillis = millis("2026-07-12", "12:15"),
                zoneId = zoneId,
            ),
        )

        assertFalse(record.isMissing)
        assertEquals(millis("2026-07-11", "23:30"), record.sleepStartMillis)
        assertEquals(millis("2026-07-12", "12:09:23"), record.sleepEndMillis)
        assertEquals(759, record.durationMinutes)
        assertEquals(7, record.wakeUpCount)
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
