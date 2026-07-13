package com.example.neurotrack.data

import com.example.neurotrack.domain.MindfulnessSessionRecord
import com.example.neurotrack.domain.MindfulnessSessionStatus
import com.example.neurotrack.domain.WeeklyAssessmentRecord
import java.time.LocalDate

fun AssessmentRecordEntity.toDomain(): WeeklyAssessmentRecord =
    WeeklyAssessmentRecord(
        weekStart = LocalDate.ofEpochDay(weekStartEpochDay),
        createdAtMillis = createdAtMillis,
        totalScore = totalScore,
    )

fun MindfulnessSessionEntity.toDomain(): MindfulnessSessionRecord =
    MindfulnessSessionRecord(
        startedAtMillis = startedAtMillis,
        status = MindfulnessSessionStatus.valueOf(status),
        lessonId = lessonId,
    )
