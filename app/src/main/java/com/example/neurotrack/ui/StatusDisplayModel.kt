package com.example.neurotrack.ui

import com.example.neurotrack.domain.MindfulnessSchedule
import com.example.neurotrack.domain.MindfulnessSessionRecord
import com.example.neurotrack.domain.WeeklyAssessmentRecord
import com.example.neurotrack.domain.WeeklyStressCalculator
import com.example.neurotrack.domain.WeeklyStressPoint
import com.example.neurotrack.domain.WeeklyStressResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class StatusDisplayModel(
    val current: WeeklyStressResult,
    val trend: List<WeeklyStressPoint>,
) {
    companion object {
        fun empty(): StatusDisplayModel = buildStatusDisplayModel(emptyList(), emptyList())
    }
}

fun buildStatusDisplayModel(
    assessments: List<WeeklyAssessmentRecord>,
    sessions: List<MindfulnessSessionRecord>,
    now: LocalDateTime = LocalDateTime.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
    refreshDay: DayOfWeek = DayOfWeek.MONDAY,
): StatusDisplayModel {
    val weekStart = MindfulnessSchedule.lastCompletedWeekStart(now, refreshDay)
    return StatusDisplayModel(
        current = WeeklyStressCalculator.calculate(
            weekStart = weekStart,
            assessments = assessments,
            sessions = sessions,
            zoneId = zoneId,
            refreshDay = refreshDay,
        ),
        trend = WeeklyStressCalculator.trend(
            assessments = assessments,
            sessions = sessions,
            endWeekStart = weekStart,
            weeks = 8,
            zoneId = zoneId,
            refreshDay = refreshDay,
        ),
    )
}

fun formatWeekLabel(date: LocalDate): String = "${date.monthValue}/${date.dayOfMonth}"
