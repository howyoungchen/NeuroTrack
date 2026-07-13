package com.example.neurotrack

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.neurotrack.data.NeuroTrackDatabase
import com.example.neurotrack.background.NotificationHelper
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
class PrivacyAndStorageInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun manifestRequestsNoSensitiveObservationPermissions() {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        }

        val requested = packageInfo.requestedPermissions.orEmpty().toSet()
        assertTrue(Manifest.permission.POST_NOTIFICATIONS in requested)
        assertTrue(Manifest.permission.RECEIVE_BOOT_COMPLETED in requested)
        assertTrue(
            requested.intersect(
                setOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.PACKAGE_USAGE_STATS,
                    Manifest.permission.SCHEDULE_EXACT_ALARM,
                    Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                ),
            ).isEmpty(),
        )
    }

    @Test
    fun databaseContainsOnlyWeeklyReflectionAndMindfulnessData() {
        val database = NeuroTrackDatabase.getInstance(context).openHelper.readableDatabase
        val tables = buildSet {
            database.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }

        assertTrue("weekly_assessments" in tables)
        assertTrue("mindfulness_sessions" in tables)
        assertFalse(tables.any { it.contains("sleep", ignoreCase = true) })
    }

    @Test
    fun mindfulnessNotificationChannelIsCreated() {
        NotificationHelper.createChannel(context, DayOfWeek.MONDAY)
        val manager = context.getSystemService(NotificationManager::class.java)

        assertNotNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_ID))
    }
}
