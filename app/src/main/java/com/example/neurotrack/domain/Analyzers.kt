package com.example.neurotrack.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.pow
import kotlin.math.sqrt

object SleepRules {
    const val SLEEP_WINDOW_BOUNDARY_HOUR = 18
    const val CORE_SLEEP_START_HOUR = 0
    const val CORE_SLEEP_END_HOUR = 10
    const val LATEST_SLEEP_START_HOUR = 8
    const val MORNING_WAKE_HOUR = 5
    const val MIN_SLEEP_MINUTES = 120
    const val MAX_SLEEP_MINUTES = 13 * 60
    const val MIN_CORE_SLEEP_OVERLAP_MINUTES = 120
    const val MICRO_AWAKE_MERGE_SECONDS = 90
    const val MAX_AWAKE_MERGE_MINUTES = 15
    const val MORNING_LONG_SCREEN_ON_WAKE_MINUTES = 5
    const val NIGHT_LONG_SCREEN_ON_WAKE_MINUTES = 10
    const val MORNING_ACTIVE_CLUSTER_WINDOW_MINUTES = 30
    const val MORNING_ACTIVE_CLUSTER_TOTAL_SECONDS = 3 * 60
    const val NIGHT_ACTIVE_CLUSTER_COUNT = 4
    const val NIGHT_ACTIVE_CLUSTER_TOTAL_SECONDS = 6 * 60
}

data class SleepWindow(
    val startMillis: Long,
    val endMillis: Long,
)

enum class DeviceInteractionType {
    KEYGUARD_UNLOCKED,
    USER_INTERACTION,
    FOREGROUND_APP,
}

data class DeviceInteractionEvent(
    val timestampMillis: Long,
    val type: DeviceInteractionType,
)

data class LocationSleepSignal(
    val timestampMillis: Long,
    val atSleepPlace: Boolean? = null,
    val stationary: Boolean? = null,
    val leftSleepPlace: Boolean = false,
)

data class SleepObservations(
    val screenEvents: List<ScreenEvent>,
    val interactionEvents: List<DeviceInteractionEvent> = emptyList(),
    val locationSignals: List<LocationSleepSignal> = emptyList(),
)

object SleepAnalyzer {
    fun windowFor(targetDate: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): SleepWindow {
        val start = targetDate.minusDays(1)
            .atTime(LocalTime.of(SleepRules.SLEEP_WINDOW_BOUNDARY_HOUR, 0))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val end = targetDate
            .atTime(LocalTime.of(SleepRules.SLEEP_WINDOW_BOUNDARY_HOUR, 0))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        return SleepWindow(start, end)
    }

    fun windowForCurrentAnalysis(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): SleepWindow {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDateTime()
        val standardWindow = windowFor(targetDateForAnalysis(now), zoneId)
        return SleepWindow(
            startMillis = standardWindow.startMillis,
            endMillis = nowMillis,
        )
    }

    fun targetDateForAnalysis(
        now: LocalDateTime = LocalDateTime.now(),
    ): LocalDate = now.toLocalDate()

    fun analyze(
        targetDate: LocalDate,
        events: List<ScreenEvent>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowMillis: Long = System.currentTimeMillis(),
        window: SleepWindow = windowFor(targetDate, zoneId),
    ): SleepRecord =
        analyze(
            targetDate = targetDate,
            observations = SleepObservations(screenEvents = events),
            zoneId = zoneId,
            nowMillis = nowMillis,
            window = window,
        )

    fun analyze(
        targetDate: LocalDate,
        observations: SleepObservations,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowMillis: Long = System.currentTimeMillis(),
        window: SleepWindow = windowFor(targetDate, zoneId),
    ): SleepRecord {
        val sortedScreenEvents = observations.screenEvents
            .filter { it.timestampMillis in window.startMillis..window.endMillis }
            .sortedBy { it.timestampMillis }
        val awakeSessions = screenOnSessions(sortedScreenEvents, window)
        val locationSignals = observations.locationSignals
            .filter { it.timestampMillis in window.startMillis..window.endMillis }
            .sortedBy { it.timestampMillis }
        val offIntervals = applyLocationWakeBoundaries(
            intervals = completeOffIntervals(sortedScreenEvents, window),
            locationSignals = locationSignals,
            targetDate = targetDate,
            zoneId = zoneId,
        )
        val merged = mergeSleepCompatibleIntervals(
            intervals = offIntervals,
            awakeSessions = awakeSessions,
            locationSignals = locationSignals,
            targetDate = targetDate,
            zoneId = zoneId,
        )
        val candidates = merged.filter { isPlausibleSleepCandidate(it, targetDate, zoneId) }
        val selected = candidates.maxWithOrNull(
            compareBy<MergedSleepInterval> {
                candidateScore(it, locationSignals, targetDate, zoneId)
            }.thenBy { it.durationMinutes() },
        )
        val durationMinutes = selected?.durationMinutes() ?: 0

        return if (selected == null || durationMinutes < SleepRules.MIN_SLEEP_MINUTES) {
            missingRecord(targetDate, nowMillis)
        } else {
            SleepRecord(
                dateEpochDay = targetDate.toEpochDay(),
                sleepStartMillis = selected.startMillis,
                sleepEndMillis = selected.endMillis,
                durationMinutes = durationMinutes,
                wakeUpCount = selected.wakeUps,
                isMissing = false,
                createdAtMillis = nowMillis,
            )
        }
    }

    private fun completeOffIntervals(
        events: List<ScreenEvent>,
        window: SleepWindow,
    ): List<SleepInterval> {
        if (events.isEmpty()) return emptyList()

        val offIntervals = mutableListOf<SleepInterval>()
        if (events.first().type == ScreenEventType.SCREEN_ON && events.first().timestampMillis > window.startMillis) {
            offIntervals += SleepInterval(window.startMillis, events.first().timestampMillis)
        }

        var offStart: Long? = null
        events.forEach { event ->
            when (event.type) {
                ScreenEventType.SCREEN_OFF -> if (offStart == null) offStart = event.timestampMillis
                ScreenEventType.SCREEN_ON -> {
                    val start = offStart
                    if (start != null && event.timestampMillis > start) {
                        offIntervals += SleepInterval(start, event.timestampMillis)
                    }
                    offStart = null
                }
            }
        }
        offStart?.let { start ->
            if (window.endMillis > start) offIntervals += SleepInterval(start, window.endMillis)
        }
        return offIntervals.filter { it.endMillis > it.startMillis }
    }

    private fun screenOnSessions(
        events: List<ScreenEvent>,
        window: SleepWindow,
    ): List<AwakeSession> {
        if (events.isEmpty()) return emptyList()

        val sessions = mutableListOf<AwakeSession>()
        var onStart: Long? = if (events.first().type == ScreenEventType.SCREEN_OFF) window.startMillis else null
        events.forEach { event ->
            when (event.type) {
                ScreenEventType.SCREEN_ON -> if (onStart == null) onStart = event.timestampMillis
                ScreenEventType.SCREEN_OFF -> {
                    val start = onStart
                    if (start != null && event.timestampMillis > start) {
                        sessions += AwakeSession(start, event.timestampMillis)
                    }
                    onStart = null
                }
            }
        }
        onStart?.let { start ->
            if (window.endMillis > start) sessions += AwakeSession(start, window.endMillis)
        }
        return sessions
    }

    private fun applyLocationWakeBoundaries(
        intervals: List<SleepInterval>,
        locationSignals: List<LocationSleepSignal>,
        targetDate: LocalDate,
        zoneId: ZoneId,
    ): List<SleepInterval> {
        if (intervals.isEmpty() || locationSignals.isEmpty()) return intervals
        val wakeSignals = locationSignals.filter { isStrongLocationWake(it, targetDate, zoneId) }
        if (wakeSignals.isEmpty()) return intervals

        return intervals.mapNotNull { interval ->
            val boundary = wakeSignals
                .filter { it.timestampMillis in (interval.startMillis + 1) until interval.endMillis }
                .minOfOrNull { it.timestampMillis }
            val end = boundary ?: interval.endMillis
            if (end > interval.startMillis) {
                SleepInterval(interval.startMillis, end)
            } else {
                null
            }
        }
    }

    private fun mergeSleepCompatibleIntervals(
        intervals: List<SleepInterval>,
        awakeSessions: List<AwakeSession>,
        locationSignals: List<LocationSleepSignal>,
        targetDate: LocalDate,
        zoneId: ZoneId,
    ): List<MergedSleepInterval> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals
            .filter { it.endMillis > it.startMillis }
            .sortedBy { it.startMillis }
        if (sorted.isEmpty()) return emptyList()

        val merged = mutableListOf<MergedSleepInterval>()
        var start = sorted.first().startMillis
        var end = sorted.first().endMillis
        var wakeUps = 0
        sorted.drop(1).forEach { next ->
            if (isSleepCompatibleAwakeGap(
                    gapStartMillis = end,
                    gapEndMillis = next.startMillis,
                    awakeSessions = awakeSessions,
                    locationSignals = locationSignals,
                    targetDate = targetDate,
                    zoneId = zoneId,
                )
            ) {
                wakeUps += 1
                end = maxOf(end, next.endMillis)
            } else {
                merged += MergedSleepInterval(start, end, wakeUps)
                start = next.startMillis
                end = next.endMillis
                wakeUps = 0
            }
        }
        merged += MergedSleepInterval(start, end, wakeUps)
        return merged
    }

    private fun isSleepCompatibleAwakeGap(
        gapStartMillis: Long,
        gapEndMillis: Long,
        awakeSessions: List<AwakeSession>,
        locationSignals: List<LocationSleepSignal>,
        targetDate: LocalDate,
        zoneId: ZoneId,
    ): Boolean {
        if (gapEndMillis < gapStartMillis) return false

        val gapSeconds = (gapEndMillis - gapStartMillis) / 1_000L
        val gapMinutes = gapSeconds / 60L
        if (gapMinutes >= SleepRules.MAX_AWAKE_MERGE_MINUTES) return false

        if (locationSignals.any {
                it.timestampMillis in gapStartMillis..gapEndMillis &&
                    isStrongLocationWake(it, targetDate, zoneId)
            }
        ) {
            return false
        }

        val morning = gapStartMillis >= morningWakeStartMillis(targetDate, zoneId)
        val cluster = awakeSessions.filter {
            it.startMillis in
                (gapStartMillis - minutesToMillis(15))..(gapStartMillis + minutesToMillis(
                    SleepRules.MORNING_ACTIVE_CLUSTER_WINDOW_MINUTES,
                ))
        }
        val clusterSeconds = cluster.sumOf { it.durationSeconds() }

        return if (morning) {
            if (gapMinutes >= SleepRules.MORNING_LONG_SCREEN_ON_WAKE_MINUTES) return false
            if (clusterSeconds >= SleepRules.MORNING_ACTIVE_CLUSTER_TOTAL_SECONDS) return false
            gapSeconds <= SleepRules.MICRO_AWAKE_MERGE_SECONDS
        } else {
            if (gapMinutes > SleepRules.NIGHT_LONG_SCREEN_ON_WAKE_MINUTES) return false
            if (
                cluster.size >= SleepRules.NIGHT_ACTIVE_CLUSTER_COUNT &&
                clusterSeconds >= SleepRules.NIGHT_ACTIVE_CLUSTER_TOTAL_SECONDS
            ) {
                return false
            }
            true
        }
    }

    private fun isPlausibleSleepCandidate(
        interval: MergedSleepInterval,
        targetDate: LocalDate,
        zoneId: ZoneId,
    ): Boolean {
        val durationMinutes = interval.durationMinutes()
        if (durationMinutes !in SleepRules.MIN_SLEEP_MINUTES..SleepRules.MAX_SLEEP_MINUTES) return false
        if (interval.startMillis > latestSleepStartMillis(targetDate, zoneId)) return false
        return coreSleepOverlapMinutes(interval, targetDate, zoneId) >=
            SleepRules.MIN_CORE_SLEEP_OVERLAP_MINUTES
    }

    private fun candidateScore(
        interval: MergedSleepInterval,
        locationSignals: List<LocationSleepSignal>,
        targetDate: LocalDate,
        zoneId: ZoneId,
    ): Int {
        val duration = interval.durationMinutes()
        val durationScore = when {
            duration in 7 * 60..9 * 60 -> 240
            duration in 6 * 60 until 7 * 60 -> 180
            duration in 9 * 60..10 * 60 -> 180
            duration in 5 * 60 until 6 * 60 -> 120
            else -> 60
        }
        val coreScore = coreSleepOverlapMinutes(interval, targetDate, zoneId).coerceAtMost(8 * 60)
        val locationScore = locationSignals.count {
            it.timestampMillis in interval.startMillis..interval.endMillis &&
                (it.atSleepPlace == true || it.stationary == true)
        } * 20
        return durationScore + coreScore + locationScore - (interval.wakeUps * 12)
    }

    private fun coreSleepOverlapMinutes(
        interval: MergedSleepInterval,
        targetDate: LocalDate,
        zoneId: ZoneId,
    ): Int {
        val coreStart = targetDate
            .atTime(LocalTime.of(SleepRules.CORE_SLEEP_START_HOUR, 0))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val coreEnd = targetDate
            .atTime(LocalTime.of(SleepRules.CORE_SLEEP_END_HOUR, 0))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val overlapStart = maxOf(interval.startMillis, coreStart)
        val overlapEnd = minOf(interval.endMillis, coreEnd)
        return ((overlapEnd - overlapStart).coerceAtLeast(0) / 60_000L).toInt()
    }

    private fun isStrongLocationWake(
        signal: LocationSleepSignal,
        targetDate: LocalDate,
        zoneId: ZoneId,
    ): Boolean {
        if (signal.leftSleepPlace) return true
        val isMorning = signal.timestampMillis >= morningWakeStartMillis(targetDate, zoneId)
        return isMorning && (signal.atSleepPlace == false || signal.stationary == false)
    }

    private fun morningWakeStartMillis(targetDate: LocalDate, zoneId: ZoneId): Long =
        targetDate
            .atTime(LocalTime.of(SleepRules.MORNING_WAKE_HOUR, 0))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

    private fun latestSleepStartMillis(targetDate: LocalDate, zoneId: ZoneId): Long =
        targetDate
            .atTime(LocalTime.of(SleepRules.LATEST_SLEEP_START_HOUR, 0))
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

    private fun minutesToMillis(minutes: Int): Long = minutes * 60_000L

    private fun missingRecord(targetDate: LocalDate, nowMillis: Long): SleepRecord =
        SleepRecord(
            dateEpochDay = targetDate.toEpochDay(),
            sleepStartMillis = 0,
            sleepEndMillis = 0,
            durationMinutes = 0,
            wakeUpCount = 0,
            isMissing = true,
            createdAtMillis = nowMillis,
        )

    private data class SleepInterval(
        val startMillis: Long,
        val endMillis: Long,
    )

    private data class AwakeSession(
        val startMillis: Long,
        val endMillis: Long,
    ) {
        fun durationSeconds(): Long = ((endMillis - startMillis) / 1_000L).coerceAtLeast(0)
    }

    private data class MergedSleepInterval(
        val startMillis: Long,
        val endMillis: Long,
        val wakeUps: Int,
    ) {
        fun durationMinutes(): Int = ((endMillis - startMillis) / 60_000L).toInt()
    }
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
    val band: StressBand? = StressCalculator.bandForScore(score),
)

object StressCalculator {
    const val LATEST_WEIGHT = 0.5
    const val TREND_WEIGHT = 0.2
    const val SLEEP_WEIGHT = 0.3

    fun calculate(
        assessments: List<AssessmentScoreRecord>,
        sleepRecords: List<SleepRecord>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): StressResult {
        val effectiveAssessments = effectiveDailyAssessments(assessments, zoneId)
        val latest = effectiveAssessments.maxByOrNull { it.createdAtMillis }
        val latestScore = latest?.let { normalizeAssessmentScore(it.totalScore) }
        val trendScore = assessmentTrendScore(effectiveAssessments, nowMillis, zoneId) ?: latestScore
        val todayEpochDay = millisToDate(nowMillis, zoneId).toEpochDay()
        val metrics = sleepMetrics(
            records = sleepRecords,
            zoneId = zoneId,
            todayEpochDay = todayEpochDay,
        )
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
            band = bandForScore(score),
            metrics = metrics,
        )
    }

    fun bandForScore(score: Double?): StressBand? =
        when {
            score == null -> null
            score < 4.0 -> StressBand.LOW
            score < 7.0 -> StressBand.MEDIUM
            else -> StressBand.HIGH
        }

    fun trendPoints(
        assessments: List<AssessmentScoreRecord>,
        sleepRecords: List<SleepRecord>,
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
        records: List<AssessmentScoreRecord>,
        zoneId: ZoneId,
    ): List<AssessmentScoreRecord> {
        return records
            .groupBy { millisToDate(it.createdAtMillis, zoneId) }
            .values
            .mapNotNull { daily -> daily.maxByOrNull { it.createdAtMillis } }
    }

    private fun assessmentTrendScore(
        records: List<AssessmentScoreRecord>,
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
        records: List<SleepRecord>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        todayEpochDay: Long = LocalDate.now(zoneId).toEpochDay(),
    ): SleepPenaltyMetrics {
        val recent = records
            .filter { SleepRecordSelection.isUsable(it, todayEpochDay) }
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
