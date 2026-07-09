package com.example.neurotrack.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class LocationSleepSignalDeriverTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun derive_marksSamplesNearCoreSleepAnchorAsSleepPlace() {
        val signals = LocationSleepSignalDeriver.derive(
            samples = listOf(
                sample("2026-07-04", "02:00", latitude = 31.2304, longitude = 121.4737, speed = 0.2f),
                sample("2026-07-04", "06:30", latitude = 31.2308, longitude = 121.4741, speed = 0.1f),
            ),
            zoneId = zoneId,
        )

        assertEquals(true, signals[0].atSleepPlace)
        assertEquals(true, signals[1].atSleepPlace)
        assertEquals(true, signals[0].stationary)
        assertFalse(signals[1].leftSleepPlace)
    }

    @Test
    fun derive_marksFarMorningSampleAsLeftSleepPlace() {
        val signals = LocationSleepSignalDeriver.derive(
            samples = listOf(
                sample("2026-07-04", "02:00", latitude = 31.2304, longitude = 121.4737),
                sample("2026-07-04", "07:00", latitude = 31.2404, longitude = 121.4737, speed = 2.0f),
            ),
            zoneId = zoneId,
        )

        assertEquals(false, signals[1].atSleepPlace)
        assertEquals(false, signals[1].stationary)
        assertTrue(signals[1].leftSleepPlace)
    }

    @Test
    fun derive_leavesPlaceUnknownWhenThereIsNoCoreSleepAnchor() {
        val signals = LocationSleepSignalDeriver.derive(
            samples = listOf(
                sample("2026-07-04", "07:00", latitude = 31.2304, longitude = 121.4737),
            ),
            zoneId = zoneId,
        )

        assertNull(signals.single().atSleepPlace)
        assertFalse(signals.single().leftSleepPlace)
    }

    private fun sample(
        date: String,
        time: String,
        latitude: Double,
        longitude: Double,
        speed: Float? = null,
    ): LocationSample =
        LocationSample(
            timestampMillis = millis(date, time),
            latitude = latitude,
            longitude = longitude,
            speedMetersPerSecond = speed,
        )

    private fun millis(date: String, time: String): Long =
        LocalDate.parse(date)
            .atTime(LocalTime.parse(time))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
}
