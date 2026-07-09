package com.example.neurotrack.ui

import com.example.neurotrack.domain.SleepAnalyzer
import java.time.ZoneId

data class SleepRawExportRange(
    val startMillis: Long,
    val endMillis: Long,
)

object SleepRawExportRangeModel {
    fun defaultRange(zoneId: ZoneId = ZoneId.systemDefault()): SleepRawExportRange {
        val window = SleepAnalyzer.windowFor(
            targetDate = SleepAnalyzer.targetDateForAnalysis(),
            zoneId = zoneId,
        )
        return SleepRawExportRange(
            startMillis = window.startMillis,
            endMillis = window.endMillis,
        )
    }

    fun format(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        formatDisplayDateTime(millis, zoneId)

    fun parse(text: String, zoneId: ZoneId = ZoneId.systemDefault()): Long? =
        parseDisplayDateTime(text, zoneId)
}
