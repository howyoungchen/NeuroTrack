package com.example.neurotrack.ui

import com.example.neurotrack.domain.AssessmentScoreRecord
import com.example.neurotrack.domain.SleepPenaltyMetrics
import com.example.neurotrack.domain.SleepRecord
import com.example.neurotrack.domain.SleepRecordSelection
import com.example.neurotrack.domain.StressCalculator
import com.example.neurotrack.domain.StressResult
import com.example.neurotrack.domain.StressTrendPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class AssessmentHistoryItem(
    val id: Long,
    val createdAtMillis: Long,
    val answersCsv: String,
    val totalScore: Int,
)

data class AssessmentSubmissionDisplay(
    val id: Long,
    val totalScore: Int,
)

data class StatusDisplayModel(
    val stress: StressResult,
    val pressureTrend: PressureTrendDisplay,
    val yesterdaySleep: SleepSummaryDisplay,
    val weekSleep: SleepPeriodDisplay,
    val monthSleep: SleepPeriodDisplay,
    val insights: List<StatusInsight>,
) {
    companion object {
        fun empty(): StatusDisplayModel =
            buildStatusDisplayModel(
                assessments = emptyList(),
                sleepRecords = emptyList(),
            )
    }
}

data class PressureTrendDisplay(
    val points: List<StressTrendPoint>,
    val latestScore: Double?,
    val chartLabels: List<String>,
) {
    val latestBand = StressCalculator.bandForScore(latestScore)
}

data class SleepSummaryDisplay(
    val durationHours: Double?,
    val bedtimeText: String?,
    val wakeTimeText: String?,
) {
    val hasData: Boolean = durationHours != null
}

data class SleepPeriodDisplay(
    val durationHours: Double?,
    val bedtimeText: String?,
    val wakeTimeText: String?,
    val points: List<SleepDurationPoint>,
    val chartLabels: List<SleepChartLabel>,
) {
    val hasData: Boolean = durationHours != null
}

data class SleepDurationPoint(
    val date: LocalDate,
    val durationMinutes: Float?,
)

sealed interface SleepChartLabel {
    data class Weekday(val dayOfWeekValue: Int) : SleepChartLabel
    data class Text(val value: String) : SleepChartLabel
}

enum class StatusInsight {
    NO_SLEEP,
    LATER,
    IRREGULAR,
    SHORT,
    DROP,
    LATE_AVERAGE,
    STABLE,
}

fun buildStatusDisplayModel(
    assessments: List<AssessmentScoreRecord>,
    sleepRecords: List<SleepRecord>,
    today: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): StatusDisplayModel {
    val todayEndMillis = today.plusDays(1)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli() - 1
    val stress = StressCalculator.calculate(
        assessments = assessments,
        sleepRecords = sleepRecords,
        nowMillis = todayEndMillis,
        zoneId = zoneId,
    )
    val trendPoints = StressCalculator.trendPoints(
        assessments = assessments,
        sleepRecords = sleepRecords,
        endDate = today,
        days = 30,
        zoneId = zoneId,
    )
    return StatusDisplayModel(
        stress = stress,
        pressureTrend = PressureTrendDisplay(
            points = trendPoints,
            latestScore = trendPoints.lastOrNull { it.score != null }?.score,
            chartLabels = trendLabels(trendPoints),
        ),
        yesterdaySleep = summaryFor(
            SleepRecordSelection.latestForDisplay(
                records = sleepRecords,
                todayEpochDay = today.toEpochDay(),
            ),
            zoneId = zoneId,
        ),
        weekSleep = periodFor(
            records = sleepRecords,
            today = today,
            days = 7,
            zoneId = zoneId,
        ),
        monthSleep = periodFor(
            records = sleepRecords,
            today = today,
            days = 30,
            zoneId = zoneId,
        ),
        insights = insightsFor(stress.metrics),
    )
}

fun formatDisplayDateTime(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return Instant.ofEpochMilli(millis)
        .atZone(zoneId)
        .toLocalDateTime()
        .format(formatter)
}

fun formatMonthDay(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("M/d", Locale.getDefault()))

private fun trendLabels(points: List<StressTrendPoint>): List<String> =
    if (points.isEmpty()) {
        emptyList()
    } else {
        listOf(
            formatMonthDay(points.first().date),
            formatMonthDay(points[points.size / 2].date),
            formatMonthDay(points.last().date),
        )
    }

fun parseDisplayDateTime(text: String, zoneId: ZoneId = ZoneId.systemDefault()): Long? =
    runCatching {
        java.time.LocalDateTime
            .parse(text.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

private fun summaryFor(record: SleepRecord?, zoneId: ZoneId): SleepSummaryDisplay =
    SleepSummaryDisplay(
        durationHours = record?.durationMinutes?.let { it / 60.0 },
        bedtimeText = record?.let { formatClockTime(it.sleepStartMillis, zoneId) },
        wakeTimeText = record?.let { formatClockTime(it.sleepEndMillis, zoneId) },
    )

private fun periodFor(
    records: List<SleepRecord>,
    today: LocalDate,
    days: Int,
    zoneId: ZoneId,
): SleepPeriodDisplay {
    val data = SleepRecordSelection.recordsForPeriod(
        records = records,
        today = today,
        days = days,
    )
    val points = if (days <= 7) {
        dailySleepDurationPoints(data, today, days)
    } else {
        monthlySleepAveragePoints(data, today)
    }
    return SleepPeriodDisplay(
        durationHours = data.takeIf { it.isNotEmpty() }
            ?.map { it.durationMinutes }
            ?.average()
            ?.let { it / 60.0 },
        bedtimeText = averageClockText(data.map { nightMinute(it.sleepStartMillis, zoneId) }),
        wakeTimeText = averageClockText(data.map { minuteOfDay(it.sleepEndMillis, zoneId).toFloat() }),
        points = points,
        chartLabels = if (days <= 7) {
            points.map { SleepChartLabel.Weekday(it.date.dayOfWeek.value) }
        } else {
            points.map { SleepChartLabel.Text(formatMonthDay(it.date)) }
        },
    )
}

private fun dailySleepDurationPoints(
    records: List<SleepRecord>,
    today: LocalDate,
    days: Int,
): List<SleepDurationPoint> {
    val startDate = today.minusDays((days - 1).toLong())
    val recordsByDay = records.associateBy { it.dateEpochDay }
    return (0 until days).map { offset ->
        val date = startDate.plusDays(offset.toLong())
        SleepDurationPoint(
            date = date,
            durationMinutes = recordsByDay[date.toEpochDay()]?.durationMinutes?.toFloat(),
        )
    }
}

private fun monthlySleepAveragePoints(
    records: List<SleepRecord>,
    today: LocalDate,
): List<SleepDurationPoint> {
    val startDate = today.minusDays(29)
    return (0 until 5).map { bucketIndex ->
        val bucketStart = startDate.plusDays((bucketIndex * 7).toLong())
        val bucketEnd = bucketStart.plusDays(6).let { end ->
            if (end.isAfter(today)) today else end
        }
        val startEpochDay = bucketStart.toEpochDay()
        val endEpochDay = bucketEnd.toEpochDay()
        val durations = records
            .filter { it.dateEpochDay in startEpochDay..endEpochDay }
            .map { it.durationMinutes }
        SleepDurationPoint(
            date = bucketStart,
            durationMinutes = durations.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
        )
    }
}

private fun insightsFor(metrics: SleepPenaltyMetrics): List<StatusInsight> =
    when {
        !metrics.hasSleepData -> listOf(StatusInsight.NO_SLEEP)
        else -> buildList {
            if (metrics.bedtimeGettingLater) add(StatusInsight.LATER)
            if (metrics.irregularBedtime) add(StatusInsight.IRREGULAR)
            if (metrics.shortSleep) add(StatusInsight.SHORT)
            if (metrics.suddenDurationDrop) add(StatusInsight.DROP)
            if (metrics.lateAverageBedtime) add(StatusInsight.LATE_AVERAGE)
            if (isEmpty()) add(StatusInsight.STABLE)
        }
    }

private fun formatClockTime(millis: Long, zoneId: ZoneId): String {
    val time = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalTime()
    return "%02d:%02d".format(time.hour, time.minute)
}

private fun nightMinute(millis: Long, zoneId: ZoneId): Float {
    val time = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalTime()
    val minute = time.hour * 60 + time.minute
    return if (minute < 20 * 60) (minute + 24 * 60).toFloat() else minute.toFloat()
}

private fun minuteOfDay(millis: Long, zoneId: ZoneId): Int {
    val time = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalTime()
    return time.hour * 60 + time.minute
}

private fun averageClockText(minutes: List<Float>): String? {
    if (minutes.isEmpty()) return null
    val average = minutes.average().roundToInt()
    val normalized = ((average % (24 * 60)) + (24 * 60)) % (24 * 60)
    return "%02d:%02d".format(normalized / 60, normalized % 60)
}
