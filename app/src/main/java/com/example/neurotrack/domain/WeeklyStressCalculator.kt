package com.example.neurotrack.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object MindfulnessSchedule {
    val practiceDays: Set<DayOfWeek> = linkedSetOf(
        DayOfWeek.MONDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SUNDAY,
    )

    fun isPracticeDay(date: LocalDate): Boolean = date.dayOfWeek in practiceDays

    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun practiceDates(weekStart: LocalDate): List<LocalDate> {
        val monday = weekStart(weekStart)
        return listOf(0L, 2L, 4L, 6L).map(monday::plusDays)
    }

    fun completedPracticeDates(
        weekStart: LocalDate,
        sessions: List<MindfulnessSessionRecord>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Set<LocalDate> {
        val plannedDates = practiceDates(weekStart).toSet()
        return sessions
            .asSequence()
            .filter { it.status == MindfulnessSessionStatus.COMPLETED }
            .map { Instant.ofEpochMilli(it.startedAtMillis).atZone(zoneId).toLocalDate() }
            .filter { it in plannedDates }
            .toSet()
    }

    fun nextReminder(now: LocalDateTime, hour: Int, minute: Int): LocalDateTime {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        return (0L..7L)
            .asSequence()
            .map { now.toLocalDate().plusDays(it) }
            .filter(::isPracticeDay)
            .map { it.atTime(safeHour, safeMinute) }
            .first { it.isAfter(now) }
    }
}

object WeeklyStressCalculator {
    private const val MAX_RECOVERY_GAP_PENALTY = 2.0
    private const val MAX_ASSESSMENT_SCORE = 30.0

    fun calculate(
        weekStart: LocalDate,
        assessments: List<WeeklyAssessmentRecord>,
        sessions: List<MindfulnessSessionRecord>,
        asOfDate: LocalDate = LocalDate.now(),
        practiceDueThroughDate: LocalDate = asOfDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): WeeklyStressResult {
        val monday = MindfulnessSchedule.weekStart(weekStart)
        val weekEndExclusive = monday.plusWeeks(1)
        val effectiveAsOfDate = minOf(asOfDate, weekEndExclusive.minusDays(1))
        val effectivePracticeDueDate = minOf(practiceDueThroughDate, weekEndExclusive.minusDays(1))
        val latestAssessment = assessments
            .filter { millisToDate(it.createdAtMillis, zoneId) in monday..effectiveAsOfDate }
            .maxByOrNull { it.createdAtMillis }
        val assessmentScore = latestAssessment?.totalScore
            ?.div(MAX_ASSESSMENT_SCORE)
            ?.times(10.0)
            ?.coerceIn(0.0, 10.0)
        val plannedDates = MindfulnessSchedule.practiceDates(monday)
            .filter { !it.isAfter(effectivePracticeDueDate) }
            .toSet()
        val completed = MindfulnessSchedule.completedPracticeDates(monday, sessions, zoneId)
            .count { it in plannedDates }
        val completionRate = if (plannedDates.isEmpty()) 1.0 else completed.toDouble() / plannedDates.size
        val score = assessmentScore?.let {
            it + ((1.0 - completionRate) * MAX_RECOVERY_GAP_PENALTY)
        }?.coerceIn(0.0, 10.0)

        return WeeklyStressResult(
            weekStart = monday,
            score = score,
            assessmentScore = assessmentScore,
            mindfulnessCompletionRate = completionRate,
            completedPractices = completed,
            scheduledPractices = plannedDates.size,
            band = bandForScore(score),
        )
    }

    fun trend(
        assessments: List<WeeklyAssessmentRecord>,
        sessions: List<MindfulnessSessionRecord>,
        endWeekStart: LocalDate = MindfulnessSchedule.weekStart(LocalDate.now()),
        weeks: Int = 8,
        asOfDate: LocalDate = LocalDate.now(),
        practiceDueThroughDate: LocalDate = asOfDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<WeeklyStressPoint> {
        require(weeks > 0) { "weeks must be positive" }
        val lastMonday = MindfulnessSchedule.weekStart(endWeekStart)
        return (weeks - 1 downTo 0).map { weeksAgo ->
            val monday = lastMonday.minusWeeks(weeksAgo.toLong())
            val result = calculate(
                weekStart = monday,
                assessments = assessments,
                sessions = sessions,
                asOfDate = minOf(asOfDate, monday.plusDays(6)),
                practiceDueThroughDate = minOf(practiceDueThroughDate, monday.plusDays(6)),
                zoneId = zoneId,
            )
            WeeklyStressPoint(monday, result.score, result.band)
        }
    }

    fun bandForScore(score: Double?): StressBand? = when {
        score == null -> null
        score < 4.0 -> StressBand.LOW
        score < 7.0 -> StressBand.MEDIUM
        else -> StressBand.HIGH
    }

    private fun millisToDate(millis: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
}
