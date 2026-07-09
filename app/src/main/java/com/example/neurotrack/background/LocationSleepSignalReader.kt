package com.example.neurotrack.background

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.neurotrack.domain.LocationSample
import com.example.neurotrack.domain.LocationSleepSignal
import com.example.neurotrack.domain.LocationSleepSignalDeriver
import java.time.ZoneId

object LocationSleepSignalReader {
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

        return LocationSleepSignalDeriver.derive(
            samples = locations.map { it.toLocationSample() },
            zoneId = zoneId,
        )
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocations(locationManager: LocationManager): List<Location> =
        runCatching {
            locationManager.getProviders(true)
                .mapNotNull { provider ->
                    runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
                }
        }.getOrDefault(emptyList())

    private fun Location.toLocationSample(): LocationSample =
        LocationSample(
            timestampMillis = time,
            latitude = latitude,
            longitude = longitude,
            speedMetersPerSecond = if (hasSpeed()) speed else null,
        )
}
