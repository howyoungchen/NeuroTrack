package com.example.neurotrack.data

import com.example.neurotrack.domain.MindfulnessSessionRecord
import com.example.neurotrack.domain.MindfulnessSessionStatus
import com.example.neurotrack.domain.WeeklyAssessmentRecord

fun AssessmentRecordEntity.toDomain(): WeeklyAssessmentRecord =
    WeeklyAssessmentRecord(createdAtMillis = createdAtMillis, totalScore = totalScore)

fun MindfulnessSessionEntity.toDomain(): MindfulnessSessionRecord =
    MindfulnessSessionRecord(
        id = id,
        startedAtMillis = startedAtMillis,
        endedAtMillis = endedAtMillis,
        plannedDurationMinutes = plannedDurationMinutes,
        status = runCatching { MindfulnessSessionStatus.valueOf(status) }
            .getOrDefault(MindfulnessSessionStatus.INTERRUPTED),
    )
