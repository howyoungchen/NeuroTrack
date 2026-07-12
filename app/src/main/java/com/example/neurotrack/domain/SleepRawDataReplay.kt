package com.example.neurotrack.domain

object SleepRawDataReplay {
    fun replay(export: SleepRawDataExport): SleepRecord =
        SleepAnalyzer.analyze(
            targetDate = export.targetDate,
            observations = export.observations,
            zoneId = export.zoneId,
            nowMillis = export.expectedResult?.createdAtMillis ?: export.generatedAtMillis,
            window = SleepWindow(export.rangeStartMillis, export.rangeEndMillis),
        )
}
