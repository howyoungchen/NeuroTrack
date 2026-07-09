package com.example.neurotrack.background

import com.example.neurotrack.domain.LocationSleepSignal
import com.example.neurotrack.domain.ScreenEvent
import com.example.neurotrack.domain.ScreenEventType
import com.example.neurotrack.domain.SleepObservations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SleepAnalysisRunnerTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")
    private val targetDate = LocalDate.of(2026, 7, 4)

    @Test
    fun runForTargetDate_collectsObservationsAndAnalyzesSleep() {
        val runner = SleepAnalysisRunner(
            usageObservationSource = UsageObservationSource { _, _ ->
                UsageObservationRead(
                    observations = SleepObservations(
                        screenEvents = listOf(
                            event("2026-07-03", "23:00", ScreenEventType.SCREEN_OFF),
                            event("2026-07-04", "07:00", ScreenEventType.SCREEN_ON),
                        ),
                    ),
                    status = UsageObservationStatus.AVAILABLE,
                )
            },
            locationSignalSource = LocationSignalSource { _, _, _ ->
                listOf(
                    LocationSleepSignal(
                        timestampMillis = millis("2026-07-04", "02:00"),
                        atSleepPlace = true,
                        stationary = true,
                    ),
                )
            },
            nowMillis = { millis("2026-07-04", "12:05") },
            zoneIdProvider = { zoneId },
        )

        val result = runner.runForTargetDate(targetDate = targetDate)

        assertEquals(UsageObservationStatus.AVAILABLE, result.usageStatus)
        assertEquals(1, result.observations.locationSignals.size)
        assertFalse(result.record.isMissing)
        assertEquals(millis("2026-07-03", "23:00"), result.record.sleepStartMillis)
        assertEquals(millis("2026-07-04", "07:00"), result.record.sleepEndMillis)
    }

    @Test
    fun runForRange_infersTargetDateAndPreservesUsageStatus() {
        val runner = SleepAnalysisRunner(
            usageObservationSource = UsageObservationSource { _, _ ->
                UsageObservationRead(
                    observations = SleepObservations(screenEvents = emptyList()),
                    status = UsageObservationStatus.MISSING_USAGE_ACCESS,
                )
            },
            locationSignalSource = LocationSignalSource { _, _, _ -> emptyList() },
            nowMillis = { millis("2026-07-04", "12:05") },
            zoneIdProvider = { zoneId },
        )

        val result = runner.runForRange(
            startMillis = millis("2026-07-03", "20:00"),
            endMillis = millis("2026-07-04", "12:00"),
        )

        assertEquals(targetDate, result.targetDate)
        assertEquals(UsageObservationStatus.MISSING_USAGE_ACCESS, result.usageStatus)
        assertEquals(targetDate.toEpochDay(), result.record.dateEpochDay)
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
