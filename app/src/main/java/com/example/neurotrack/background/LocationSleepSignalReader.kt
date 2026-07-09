package com.example.neurotrack.background

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.neurotrack.domain.LocationSleepSignal
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

object LocationSleepSignalReader {
    private const val SLEEP_PLACE_RADIUS_METERS = 200f
    private const val LEFT_SLEEP_PLACE_METERS = 350f
    private const val MOVING_SPEED_METERS_PER_SECOND = 1.2f

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    fun readSignals(
        context: Context,
        startMillis: Long,
        endMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<LocationSleepSignal> {
        if (!hasLocationPermission(context)) return emptyList()
        val locationManager = context.getSystemService(LocationManager::class.java)
            ?: return emptyList()

        val locations = lastKnownLocations(locationManager)
            .filter { it.time in startMillis..endMillis }
            .sortedBy { it.time }

        if (locations.isEmpty()) return emptyList()

        val sleepAnchor = locations.firstOrNull { it.isInCoreSleepTime(zoneId) }
        return locations.map { location ->
            val distanceFromSleepAnchor = sleepAnchor?.distanceTo(location)
            val atSleepPlace = distanceFromSleepAnchor?.let { it <= SLEEP_PLACE_RADIUS_METERS }
            LocationSleepSignal(
                timestampMillis = location.time,
                atSleepPlace = atSleepPlace,
                stationary = location.stationarySignal(),
                leftSleepPlace = distanceFromSleepAnchor?.let { it >= LEFT_SLEEP_PLACE_METERS } == true,
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocations(locationManager: LocationManager): List<Location> =
        runCatching {
            locationManager.getProviders(true)
                .mapNotNull { provider ->
                    runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
                }
        }.getOrDefault(emptyList())

    private fun Location.isInCoreSleepTime(zoneId: ZoneId): Boolean {
        val time = Instant.ofEpochMilli(time).atZone(zoneId).toLocalTime()
        return !time.isBefore(LocalTime.MIDNIGHT) && time.isBefore(LocalTime.of(5, 0))
    }

    private fun Location.stationarySignal(): Boolean? =
        if (hasSpeed()) speed < MOVING_SPEED_METERS_PER_SECOND else null
}
