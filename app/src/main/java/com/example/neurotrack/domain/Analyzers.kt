package com.example.neurotrack.domain

import com.example.neurotrack.data.AssessmentRecordEntity
import com.example.neurotrack.data.SCREEN_OFF
import com.example.neurotrack.data.SCREEN_ON
import com.example.neurotrack.data.ScreenEventEntity
import com.example.neurotrack.data.SleepRecordEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.pow
import kotlin.math.sqrt

object SleepRules {
    const val NIGHT_WINDOW_START_HOUR = 20
    const val NIGHT_WINDOW_END_HOUR = 12
    const val SHORT_AWAKE_MERGE_MINUTES = 15
    const val MIN_SLEEP_MINUTES = 120
}

data class SleepWindow(
    val startMillis: Long,
    val endMillis: Long,
)

object SleepAnalyzer {
    fun windowFor(targetDate: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): SleepWindow {
        val start = targetDate.minusDays(1)
            .atTime(LocalTime.of(SleepRules.NIGHT_WINDOW_START_HOUR, 0))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val end = targetDate
            .atTime(LocalTime.of(SleepRules.NIGHT_WINDOW_END_HOUR, 0))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        return SleepWindow(start, end)
    }

    fun analyze(
        targetDate: LocalDate,
        events: List<ScreenEventEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowMillis: Long = System.currentTimeMillis(),
    ): SleepRecordEntity {
        val window = windowFor(targetDate, zoneId)
        val sorted = events
            .filter { it.timestampMillis in window.startMillis..window.endMillis }
            .sortedBy { it.timestampMillis }

        val offIntervals = mutableListOf<Pair<Long, Long>>()
        val first = sorted.firstOrNull()
        if (first?.eventType == SCREEN_ON && first.timestampMillis > window.startMillis) {
            offIntervals += window.startMillis to first.timestampMillis
        }

        var offStart: Long? = null
        sorted.forEach { event ->
            when (event.eventType) {
                SCREEN_OFF -> if (offStart == null) offStart = event.timestampMillis
                SCREEN_ON -> {
                    val start = offStart
                    if (start != null && event.timestampMillis > start) {
                        offIntervals += start to event.timestampMillis
                    }
                    offStart = null
                }
            }
        }
        offStart?.let { start ->
            if (window.endMillis > start) offIntervals += start to window.endMillis
        }

        val merged = mergeShortAwakeGaps(offIntervals)
        val longest = merged.maxByOrNull { it.endMillis - it.startMillis }
        val durationMinutes = longest?.let { ((it.endMillis - it.startMillis) / 60_000L).toInt() } ?: 0

        return if (longest == null || durationMinutes < SleepRules.MIN_SLEEP_MINUTES) {
            SleepRecordEntity(
                dateEpochDay = targetDate.toEpochDay(),
                sleepStartMillis = 0,
                sleepEndMillis = 0,
                durationMinutes = 0,
                wakeUpCount = 0,
                isMissing = true,
                createdAtMillis = nowMillis,
            )
        } else {
            SleepRecordEntity(
                dateEpochDay = targetDate.toEpochDay(),
                sleepStartMillis = longest.startMillis,
                sleepEndMillis = longest.endMillis,
                durationMinutes = durationMinutes,
                wakeUpCount = longest.wakeUps,
                isMissing = false,
                createdAtMillis = nowMillis,
            )
        }
    }

    private fun mergeShortAwakeGaps(intervals: List<Pair<Long, Long>>): List<MergedSleepInterval> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals
            .filter { it.second > it.first }
            .sortedBy { it.first }
        if (sorted.isEmpty()) return emptyList()

        val merged = mutableListOf<MergedSleepInterval>()
        var start = sorted.first().first
        var end = sorted.first().second
        var wakeUps = 0
        sorted.drop(1).forEach { (nextStart, nextEnd) ->
            val gapMinutes = (nextStart - end) / 60_000L
            if (gapMinutes >= 0 && gapMinutes < SleepRules.SHORT_AWAKE_MERGE_MINUTES) {
                wakeUps += 1
                end = maxOf(end, nextEnd)
            } else {
                merged += MergedSleepInterval(start, end, wakeUps)
                start = nextStart
                end = nextEnd
                wakeUps = 0
            }
        }
        merged += MergedSleepInterval(start, end, wakeUps)
        return merged
    }

    private data class MergedSleepInterval(
        val startMillis: Long,
        val endMillis: Long,
        val wakeUps: Int,
    )
}

enum class StressBand {
    LOW,
    MEDIUM,
    HIGH,
}

data class SleepPenaltyMetrics(
    val averageDurationMinutes: Double = 0.0,
    val bedtimeStdDevMinutes: Double = 0.0,
    val bedtimeSlopeMinutesPerDay: Double = 0.0,
    val averageBedtimeMinute: Double = 0.0,
    val shortSleep: Boolean = false,
    val irregularBedtime: Boolean = false,
    val bedtimeGettingLater: Boolean = false,
    val lateAverageBedtime: Boolean = false,
    val suddenDurationDrop: Boolean = false,
    val hasSleepData: Boolean = false,
)

data class StressResult(
    val score: Double?,
    val latestAssessmentScore: Double?,
    val trendAssessmentScore: Double?,
    val sleepPenaltyScore: Double?,
    val band: StressBand?,
    val metrics: SleepPenaltyMetrics,
)

data class StressTrendPoint(
    val date: LocalDate,
    val score: Double?,
)

object StressCalculator {
    const val LATEST_WEIGHT = 0.5
    const val TREND_WEIGHT = 0.2
    const val SLEEP_WEIGHT = 0.3

    fun calculate(
        assessments: List<AssessmentRecordEntity>,
        sleepRecords: List<SleepRecordEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): StressResult {
        val effectiveAssessments = effectiveDailyAssessments(assessments, zoneId)
        val latest = effectiveAssessments.maxByOrNull { it.createdAtMillis }
        val latestScore = latest?.let { normalizeAssessmentScore(it.totalScore) }
        val trendScore = assessmentTrendScore(effectiveAssessments, nowMillis, zoneId) ?: latestScore
        val metrics = sleepMetrics(sleepRecords, zoneId)
        val sleepPenalty = if (metrics.hasSleepData) sleepPenaltyScore(metrics) else null

        if (latestScore == null) {
            return StressResult(
                score = null,
                latestAssessmentScore = null,
                trendAssessmentScore = null,
                sleepPenaltyScore = sleepPenalty,
                band = null,
                metrics = metrics,
            )
        }

        val score = if (sleepPenalty == null) {
            ((LATEST_WEIGHT * latestScore) + (TREND_WEIGHT * (trendScore ?: latestScore))) /
                (LATEST_WEIGHT + TREND_WEIGHT)
        } else {
            (LATEST_WEIGHT * latestScore) +
                (TREND_WEIGHT * (trendScore ?: latestScore)) +
                (SLEEP_WEIGHT * sleepPenalty)
        }.coerceIn(0.0, 10.0)

        return StressResult(
            score = score,
            latestAssessmentScore = latestScore,
            trendAssessmentScore = trendScore,
            sleepPenaltyScore = sleepPenalty,
            band = when {
                score < 4.0 -> StressBand.LOW
                score < 7.0 -> StressBand.MEDIUM
                else -> StressBand.HIGH
            },
            metrics = metrics,
        )
    }

    fun trendPoints(
        assessments: List<AssessmentRecordEntity>,
        sleepRecords: List<SleepRecordEntity>,
        endDate: LocalDate = LocalDate.now(),
        days: Int = 30,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<StressTrendPoint> {
        require(days > 0) { "days must be positive" }

        val startDate = endDate.minusDays((days - 1).toLong())
        return (0 until days).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val endMillis = date.plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli() - 1
            val endEpochDay = date.toEpochDay()

            StressTrendPoint(
                date = date,
                score = calculate(
                    assessments = assessments.filter { it.createdAtMillis <= endMillis },
                    sleepRecords = sleepRecords.filter { it.dateEpochDay <= endEpochDay },
                    nowMillis = endMillis,
                    zoneId = zoneId,
                ).score,
            )
        }
    }

    private fun normalizeAssessmentScore(totalScore: Int): Double =
        (totalScore / 30.0 * 10.0).coerceIn(0.0, 10.0)

    private fun effectiveDailyAssessments(
        records: List<AssessmentRecordEntity>,
        zoneId: ZoneId,
    ): List<AssessmentRecordEntity> {
        return records
            .groupBy { millisToDate(it.createdAtMillis, zoneId) }
            .values
            .mapNotNull { daily -> daily.maxByOrNull { it.createdAtMillis } }
    }

    private fun assessmentTrendScore(
        records: List<AssessmentRecordEntity>,
        nowMillis: Long,
        zoneId: ZoneId,
    ): Double? {
        val today = millisToDate(nowMillis, zoneId)
        val scoresByRecentWeek = records.mapNotNull { record ->
            val date = millisToDate(record.createdAtMillis, zoneId)
            val daysAgo = ChronoUnit.DAYS.between(date, today).toInt()
            if (daysAgo !in 0..27) return@mapNotNull null
            (daysAgo / 7) to normalizeAssessmentScore(record.totalScore)
        }.groupBy({ it.first }, { it.second })

        val weights = listOf(4.0, 3.0, 2.0, 1.0)
        var weightedTotal = 0.0
        var weightTotal = 0.0
        weights.forEachIndexed { weekIndex, weight ->
            val values = scoresByRecentWeek[weekIndex].orEmpty()
            if (values.isNotEmpty()) {
                weightedTotal += values.average() * weight
                weightTotal += weight
            }
        }
        return if (weightTotal > 0.0) weightedTotal / weightTotal else null
    }

    fun sleepMetrics(
        records: List<SleepRecordEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): SleepPenaltyMetrics {
        val recent = records
            .filter { !it.isMissing && it.durationMinutes > 0 }
            .sortedByDescending { it.dateEpochDay }
            .take(7)
            .sortedBy { it.dateEpochDay }

        if (recent.isEmpty()) return SleepPenaltyMetrics(hasSleepData = false)

        val durations = recent.map { it.durationMinutes.toDouble() }
        val bedtimes = recent.map { bedtimeMinute(it.sleepStartMillis, zoneId) }
        val averageDuration = durations.average()
        val stdDev = standardDeviation(bedtimes)
        val slope = regressionSlope(bedtimes)
        val averageBedtime = bedtimes.average()
        val suddenDrop = recent.size >= 4 &&
            durations.last() < durations.dropLast(1).average() - 90.0

        return SleepPenaltyMetrics(
            averageDurationMinutes = averageDuration,
            bedtimeStdDevMinutes = stdDev,
            bedtimeSlopeMinutesPerDay = slope,
            averageBedtimeMinute = averageBedtime,
            shortSleep = averageDuration < 7 * 60,
            irregularBedtime = stdDev > 60,
            bedtimeGettingLater = slope > 15.0,
            lateAverageBedtime = averageBedtime > 25 * 60,
            suddenDurationDrop = suddenDrop,
            hasSleepData = true,
        )
    }

    private fun sleepPenaltyScore(metrics: SleepPenaltyMetrics): Double {
        var score = 0.0
        score += when {
            metrics.averageDurationMinutes < 6 * 60 -> 3.0
            metrics.averageDurationMinutes < 7 * 60 -> 1.5
            else -> 0.0
        }
        score += when {
            metrics.bedtimeStdDevMinutes > 90 -> 3.0
            metrics.bedtimeStdDevMinutes > 60 -> 1.5
            else -> 0.0
        }
        if (metrics.bedtimeGettingLater) score += 2.0
        if (metrics.lateAverageBedtime) score += 2.0
        return score.coerceIn(0.0, 10.0)
    }

    private fun bedtimeMinute(millis: Long, zoneId: ZoneId): Double {
        val time = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalTime()
        val minute = time.hour * 60 + time.minute
        return if (minute < 12 * 60) (minute + 24 * 60).toDouble() else minute.toDouble()
    }

    private fun standardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean).pow(2) } / values.size)
    }

    private fun regressionSlope(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val xs = values.indices.map { it.toDouble() }
        val xMean = xs.average()
        val yMean = values.average()
        val numerator = xs.indices.sumOf { index -> (xs[index] - xMean) * (values[index] - yMean) }
        val denominator = xs.sumOf { (it - xMean).pow(2) }
        return if (denominator == 0.0) 0.0 else numerator / denominator
    }

    private fun millisToDate(millis: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
}
