package com.example.neurotrack.domain

import java.time.LocalDate

enum class ScreenEventType {
    SCREEN_ON,
    SCREEN_OFF,
}

data class ScreenEvent(
    val timestampMillis: Long,
    val type: ScreenEventType,
)

data class AssessmentScoreRecord(
    val createdAtMillis: Long,
    val totalScore: Int,
)

data class SleepRecord(
    val dateEpochDay: Long,
    val sleepStartMillis: Long,
    val sleepEndMillis: Long,
    val durationMinutes: Int,
    val wakeUpCount: Int,
    val isMissing: Boolean,
    val createdAtMillis: Long,
)

object SleepRecordSelection {
    fun isUsable(record: SleepRecord, todayEpochDay: Long? = null): Boolean =
        !record.isMissing &&
            record.durationMinutes > 0 &&
            (todayEpochDay == null || record.dateEpochDay <= todayEpochDay)

    fun latestForDisplay(
        records: List<SleepRecord>,
        todayEpochDay: Long = LocalDate.now().toEpochDay(),
    ): SleepRecord? =
        records
            .asSequence()
            .filter { isUsable(it, todayEpochDay) }
            .maxByOrNull { it.dateEpochDay }

    fun recordsForPeriod(
        records: List<SleepRecord>,
        today: LocalDate = LocalDate.now(),
        days: Int,
    ): List<SleepRecord> {
        require(days > 0) { "days must be positive" }
        val endEpochDay = today.toEpochDay()
        val startEpochDay = today.minusDays((days - 1).toLong()).toEpochDay()
        return records
            .filter { isUsable(it) }
            .filter { it.dateEpochDay in startEpochDay..endEpochDay }
            .sortedBy { it.dateEpochDay }
    }
}
