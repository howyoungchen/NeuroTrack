package com.example.neurotrack.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LocationSample(
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val speedMetersPerSecond: Float? = null,
)

object LocationSleepSignalDeriver {
    private const val SLEEP_PLACE_RADIUS_METERS = 200.0
    private const val LEFT_SLEEP_PLACE_METERS = 350.0
    private const val MOVING_SPEED_METERS_PER_SECOND = 1.2f
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun derive(
        samples: List<LocationSample>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<LocationSleepSignal> {
        val sorted = samples.sortedBy { it.timestampMillis }
        if (sorted.isEmpty()) return emptyList()

        val sleepAnchor = sorted.firstOrNull { it.isInCoreSleepTime(zoneId) }
        return sorted.map { sample ->
            val distanceFromSleepAnchor = sleepAnchor?.distanceTo(sample)
            LocationSleepSignal(
                timestampMillis = sample.timestampMillis,
                atSleepPlace = distanceFromSleepAnchor?.let { it <= SLEEP_PLACE_RADIUS_METERS },
                stationary = sample.speedMetersPerSecond?.let { it < MOVING_SPEED_METERS_PER_SECOND },
                leftSleepPlace = distanceFromSleepAnchor?.let { it >= LEFT_SLEEP_PLACE_METERS } == true,
            )
        }
    }

    private fun LocationSample.isInCoreSleepTime(zoneId: ZoneId): Boolean {
        val time = Instant.ofEpochMilli(timestampMillis).atZone(zoneId).toLocalTime()
        return !time.isBefore(LocalTime.MIDNIGHT) && time.isBefore(LocalTime.of(5, 0))
    }

    private fun LocationSample.distanceTo(other: LocationSample): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }
}
