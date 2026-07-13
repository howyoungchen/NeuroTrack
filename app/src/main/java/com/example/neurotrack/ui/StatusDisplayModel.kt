package com.example.neurotrack.ui

import com.example.neurotrack.domain.MindfulnessSchedule
import com.example.neurotrack.domain.MindfulnessSessionRecord
import com.example.neurotrack.domain.WeeklyAssessmentRecord
import com.example.neurotrack.domain.WeeklyStressCalculator
import com.example.neurotrack.domain.WeeklyStressPoint
import com.example.neurotrack.domain.WeeklyStressResult
import java.time.LocalDate
import java.time.ZoneId

data class AssessmentHistoryItem(
    val id: Long,
    val createdAtMillis: Long,
    val totalScore: Int,
)

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
    today: LocalDate = LocalDate.now(),
    dueThroughDate: LocalDate = today,
    zoneId: ZoneId = ZoneId.systemDefault(),
): StatusDisplayModel {
    val weekStart = MindfulnessSchedule.weekStart(today)
    return StatusDisplayModel(
        current = WeeklyStressCalculator.calculate(
            weekStart = weekStart,
            assessments = assessments,
            sessions = sessions,
            asOfDate = today,
            practiceDueThroughDate = dueThroughDate,
            zoneId = zoneId,
        ),
        trend = WeeklyStressCalculator.trend(
            assessments = assessments,
            sessions = sessions,
            endWeekStart = weekStart,
            weeks = 8,
            asOfDate = today,
            practiceDueThroughDate = dueThroughDate,
            zoneId = zoneId,
        ),
    )
}

fun formatWeekLabel(date: LocalDate): String = "${date.monthValue}/${date.dayOfMonth}"
