package com.example.neurotrack.background

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.example.neurotrack.domain.DeviceInteractionEvent
import com.example.neurotrack.domain.DeviceInteractionType
import com.example.neurotrack.domain.ScreenEvent
import com.example.neurotrack.domain.ScreenEventType
import com.example.neurotrack.domain.SleepObservations

enum class UsageObservationStatus {
    AVAILABLE,
    UNSUPPORTED_SDK,
    MISSING_USAGE_ACCESS,
    USAGE_STATS_UNAVAILABLE,
}

data class UsageObservationRead(
    val observations: SleepObservations,
    val status: UsageObservationStatus,
)

object UsageScreenEventReader {
    @Suppress("DEPRECATION")
    fun hasUsageStatsAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun readScreenEvents(
        context: Context,
        startMillis: Long,
        endMillis: Long,
    ): List<ScreenEvent> =
        readSleepObservations(context, startMillis, endMillis).screenEvents

    fun readSleepObservations(
        context: Context,
        startMillis: Long,
        endMillis: Long,
    ): SleepObservations =
        readSleepObservationResult(context, startMillis, endMillis).observations

    fun readSleepObservationResult(
        context: Context,
        startMillis: Long,
        endMillis: Long,
    ): UsageObservationRead {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return UsageObservationRead(emptyObservations(), UsageObservationStatus.UNSUPPORTED_SDK)
        }
        if (!hasUsageStatsAccess(context)) {
            return UsageObservationRead(emptyObservations(), UsageObservationStatus.MISSING_USAGE_ACCESS)
        }

        val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
            ?: return UsageObservationRead(emptyObservations(), UsageObservationStatus.USAGE_STATS_UNAVAILABLE)
        val usageEvents = usageStatsManager.queryEvents(startMillis, endMillis)
        val event = UsageEvents.Event()
        val screenEvents = mutableListOf<ScreenEvent>()
        val interactionEvents = mutableListOf<DeviceInteractionEvent>()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            screenEventTypeForUsageEventType(event.eventType)?.let { type ->
                screenEvents += ScreenEvent(
                    timestampMillis = event.timeStamp,
                    type = type,
                )
            }
            interactionTypeForUsageEventType(event.eventType)?.let { type ->
                interactionEvents += DeviceInteractionEvent(
                    timestampMillis = event.timeStamp,
                    type = type,
                )
            }
        }

        return UsageObservationRead(
            observations = SleepObservations(
                screenEvents = screenEvents.sortedBy { it.timestampMillis },
                interactionEvents = interactionEvents.sortedBy { it.timestampMillis },
            ),
            status = UsageObservationStatus.AVAILABLE,
        )
    }

    internal fun screenEventTypeForUsageEventType(eventType: Int): ScreenEventType? =
        when (eventType) {
            UsageEvents.Event.SCREEN_INTERACTIVE -> ScreenEventType.SCREEN_ON
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> ScreenEventType.SCREEN_OFF
            else -> null
        }

    @Suppress("DEPRECATION")
    internal fun interactionTypeForUsageEventType(eventType: Int): DeviceInteractionType? =
        when (eventType) {
            UsageEvents.Event.KEYGUARD_HIDDEN -> DeviceInteractionType.KEYGUARD_UNLOCKED
            UsageEvents.Event.USER_INTERACTION -> DeviceInteractionType.USER_INTERACTION
            UsageEvents.Event.ACTIVITY_RESUMED,
            UsageEvents.Event.MOVE_TO_FOREGROUND,
            -> DeviceInteractionType.FOREGROUND_APP
            else -> null
        }

    private fun emptyObservations(): SleepObservations = SleepObservations(screenEvents = emptyList())
}
