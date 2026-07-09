package com.example.neurotrack.data

import com.example.neurotrack.domain.AssessmentScoreRecord
import com.example.neurotrack.domain.SleepRecord

fun AssessmentRecordEntity.toDomainAssessment(): AssessmentScoreRecord =
    AssessmentScoreRecord(
        createdAtMillis = createdAtMillis,
        totalScore = totalScore,
    )

fun SleepRecordEntity.toDomainSleepRecord(): SleepRecord =
    SleepRecord(
        dateEpochDay = dateEpochDay,
        sleepStartMillis = sleepStartMillis,
        sleepEndMillis = sleepEndMillis,
        durationMinutes = durationMinutes,
        wakeUpCount = wakeUpCount,
        isMissing = isMissing,
        createdAtMillis = createdAtMillis,
    )

fun SleepRecord.toEntity(): SleepRecordEntity =
    SleepRecordEntity(
        dateEpochDay = dateEpochDay,
        sleepStartMillis = sleepStartMillis,
        sleepEndMillis = sleepEndMillis,
        durationMinutes = durationMinutes,
        wakeUpCount = wakeUpCount,
        isMissing = isMissing,
        createdAtMillis = createdAtMillis,
    )
