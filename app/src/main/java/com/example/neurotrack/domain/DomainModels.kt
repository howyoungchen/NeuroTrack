package com.example.neurotrack.domain

import java.time.LocalDate

data class WeeklyAssessmentRecord(
    val createdAtMillis: Long,
    val totalScore: Int,
)

enum class MindfulnessSessionStatus {
    IN_PROGRESS,
    COMPLETED,
    INTERRUPTED,
    ABANDONED,
}

data class MindfulnessSessionRecord(
    val id: Long = 0,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val plannedDurationMinutes: Int,
    val status: MindfulnessSessionStatus,
)

enum class StressBand {
    LOW,
    MEDIUM,
    HIGH,
}

data class WeeklyStressResult(
    val weekStart: LocalDate,
    val score: Double?,
    val assessmentScore: Double?,
    val mindfulnessCompletionRate: Double,
    val completedPractices: Int,
    val scheduledPractices: Int,
    val band: StressBand?,
)

data class WeeklyStressPoint(
    val weekStart: LocalDate,
    val score: Double?,
    val band: StressBand?,
)
