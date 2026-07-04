package com.example.neurotrack.background

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.example.neurotrack.data.SCREEN_OFF
import com.example.neurotrack.data.SCREEN_ON
import com.example.neurotrack.data.ScreenEventEntity

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
    ): List<ScreenEventEntity> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return emptyList()
        if (!hasUsageStatsAccess(context)) return emptyList()

        val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
            ?: return emptyList()
        val usageEvents = usageStatsManager.queryEvents(startMillis, endMillis)
        val event = UsageEvents.Event()
        val screenEvents = mutableListOf<ScreenEventEntity>()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val type = screenEventTypeForUsageEventType(event.eventType) ?: continue
            screenEvents += ScreenEventEntity(
                timestampMillis = event.timeStamp,
                eventType = type,
            )
        }

        return screenEvents.sortedBy { it.timestampMillis }
    }

    internal fun screenEventTypeForUsageEventType(eventType: Int): String? =
        when (eventType) {
            UsageEvents.Event.SCREEN_INTERACTIVE -> SCREEN_ON
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> SCREEN_OFF
            else -> null
        }
}
