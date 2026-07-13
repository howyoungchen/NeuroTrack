package com.example.neurotrack.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object MindfulnessSchedule {
    const val LESSON_COUNT = 6
    val lessonIds: IntRange = 1..LESSON_COUNT
    val refreshTime: LocalTime = LocalTime.of(5, 0)
    val assessmentReminderTime: LocalTime = LocalTime.of(22, 0)

    fun weekStart(
        date: LocalDate,
        refreshDay: DayOfWeek = DayOfWeek.MONDAY,
    ): LocalDate = date.with(TemporalAdjusters.previousOrSame(refreshDay))

    fun weekStart(
        now: LocalDateTime,
        refreshDay: DayOfWeek = DayOfWeek.MONDAY,
    ): LocalDate {
        val start = weekStart(now.toLocalDate(), refreshDay)
        return if (now.toLocalDate() == start && now.toLocalTime().isBefore(refreshTime)) {
            start.minusWeeks(1)
        } else {
            start
        }
    }

    fun weekStart(
        millis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        refreshDay: DayOfWeek = DayOfWeek.MONDAY,
    ): LocalDate = weekStart(
        now = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDateTime(),
        refreshDay = refreshDay,
    )

    fun lastCompletedWeekStart(
        now: LocalDateTime,
        refreshDay: DayOfWeek = DayOfWeek.MONDAY,
    ): LocalDate = weekStart(now, refreshDay).minusWeeks(1)

    fun completedLessonIds(
        weekStart: LocalDate,
        sessions: List<MindfulnessSessionRecord>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        refreshDay: DayOfWeek = DayOfWeek.MONDAY,
    ): Set<Int> {
        val start = weekStart(weekStart, refreshDay)
        return sessions
            .asSequence()
            .filter { it.status == MindfulnessSessionStatus.COMPLETED }
            .filter { it.lessonId in lessonIds }
            .filter { weekStart(it.startedAtMillis, zoneId, refreshDay) == start }
            .map(MindfulnessSessionRecord::lessonId)
            .toSet()
    }

    fun assessmentDay(refreshDay: DayOfWeek): DayOfWeek = refreshDay.minus(1)

    fun isAssessmentDay(
        date: LocalDate,
        refreshDay: DayOfWeek = DayOfWeek.MONDAY,
    ): Boolean = date.dayOfWeek == assessmentDay(refreshDay)

    fun isAssessmentReminderWindow(
        now: LocalDateTime,
        refreshDay: DayOfWeek = DayOfWeek.MONDAY,
    ): Boolean = isAssessmentDay(now.toLocalDate(), refreshDay) &&
        !now.toLocalTime().isBefore(assessmentReminderTime)

    fun nextAssessmentReminder(
        now: LocalDateTime,
        refreshDay: DayOfWeek = DayOfWeek.MONDAY,
    ): LocalDateTime =
        (0L..7L)
            .asSequence()
            .map { now.toLocalDate().plusDays(it) }
            .filter { it.dayOfWeek == assessmentDay(refreshDay) }
            .map { it.atTime(assessmentReminderTime) }
            .first { it.isAfter(now) }
}

object WeeklyStressCalculator {
    private const val MAX_RECOVERY_GAP_PENALTY = 2.0
    private const val MAX_ASSESSMENT_SCORE = 30.0

    fun calculate(
        weekStart: LocalDate,
        assessments: List<WeeklyAssessmentRecord>,
        sessions: List<MindfulnessSessionRecord>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        refreshDay: DayOfWeek = DayOfWeek.MONDAY,
    ): WeeklyStressResult {
        val start = MindfulnessSchedule.weekStart(weekStart, refreshDay)
        val latestAssessment = assessments
            .filter { it.weekStart == start }
            .maxByOrNull { it.createdAtMillis }
        val assessmentScore = latestAssessment?.totalScore
            ?.div(MAX_ASSESSMENT_SCORE)
            ?.times(10.0)
            ?.coerceIn(0.0, 10.0)
        val completed = MindfulnessSchedule.completedLessonIds(
            weekStart = start,
            sessions = sessions,
            zoneId = zoneId,
            refreshDay = refreshDay,
        ).size
        val completionRate = completed.toDouble() / MindfulnessSchedule.LESSON_COUNT
        val score = assessmentScore?.let {
            it + ((1.0 - completionRate) * MAX_RECOVERY_GAP_PENALTY)
        }?.coerceIn(0.0, 10.0)

        return WeeklyStressResult(
            weekStart = start,
            score = score,
            assessmentScore = assessmentScore,
            mindfulnessCompletionRate = completionRate,
            completedPractices = completed,
            scheduledPractices = MindfulnessSchedule.LESSON_COUNT,
            band = bandForScore(score),
        )
    }

    fun trend(
        assessments: List<WeeklyAssessmentRecord>,
        sessions: List<MindfulnessSessionRecord>,
        refreshDay: DayOfWeek = DayOfWeek.MONDAY,
        endWeekStart: LocalDate = MindfulnessSchedule.lastCompletedWeekStart(
            LocalDateTime.now(),
            refreshDay,
        ),
        weeks: Int = 8,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<WeeklyStressPoint> {
        require(weeks > 0) { "weeks must be positive" }
        val lastWeekStart = MindfulnessSchedule.weekStart(endWeekStart, refreshDay)
        return (weeks - 1 downTo 0).map { weeksAgo ->
            val start = lastWeekStart.minusWeeks(weeksAgo.toLong())
            val result = calculate(
                weekStart = start,
                assessments = assessments,
                sessions = sessions,
                zoneId = zoneId,
                refreshDay = refreshDay,
            )
            WeeklyStressPoint(start, result.score, result.band)
        }
    }

    fun bandForScore(score: Double?): StressBand? = when {
        score == null -> null
        score < 4.0 -> StressBand.LOW
        score < 7.0 -> StressBand.MEDIUM
        else -> StressBand.HIGH
    }

}
