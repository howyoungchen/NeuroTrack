package com.example.neurotrack.background

import android.content.Context
import com.example.neurotrack.domain.LocationSleepSignal
import com.example.neurotrack.domain.SleepAnalyzer
import com.example.neurotrack.domain.SleepObservations
import com.example.neurotrack.domain.SleepRawDataCodec
import com.example.neurotrack.domain.SleepRecord
import com.example.neurotrack.domain.SleepWindow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun interface UsageObservationSource {
    fun read(startMillis: Long, endMillis: Long): UsageObservationRead
}

fun interface LocationSignalSource {
    fun read(startMillis: Long, endMillis: Long, zoneId: ZoneId): List<LocationSleepSignal>
}

data class SleepAnalysisRun(
    val targetDate: LocalDate,
    val zoneId: ZoneId,
    val window: SleepWindow,
    val observations: SleepObservations,
    val record: SleepRecord,
    val usageStatus: UsageObservationStatus,
)

class SleepAnalysisRunner(
    private val usageObservationSource: UsageObservationSource,
    private val locationSignalSource: LocationSignalSource,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() },
) {
    fun runForCurrentTime(
        zoneId: ZoneId = zoneIdProvider(),
    ): SleepAnalysisRun {
        val currentTimeMillis = nowMillis()
        val targetDate = SleepAnalyzer.targetDateForAnalysis(
            Instant.ofEpochMilli(currentTimeMillis).atZone(zoneId).toLocalDateTime(),
        )
        return run(
            targetDate = targetDate,
            zoneId = zoneId,
            window = SleepAnalyzer.windowForCurrentAnalysis(currentTimeMillis, zoneId),
            createdAtMillis = currentTimeMillis,
        )
    }

    fun runForTargetDate(
        targetDate: LocalDate = SleepAnalyzer.targetDateForAnalysis(),
        zoneId: ZoneId = zoneIdProvider(),
    ): SleepAnalysisRun {
        val window = SleepAnalyzer.windowFor(targetDate, zoneId)
        return run(
            targetDate = targetDate,
            zoneId = zoneId,
            window = window,
            createdAtMillis = nowMillis(),
        )
    }

    fun runForRange(
        startMillis: Long,
        endMillis: Long,
        zoneId: ZoneId = zoneIdProvider(),
    ): SleepAnalysisRun {
        require(endMillis > startMillis) { "endMillis must be greater than startMillis" }
        return run(
            targetDate = SleepRawDataCodec.inferTargetDateForRange(
                startMillis = startMillis,
                endMillis = endMillis,
                zoneId = zoneId,
            ),
            zoneId = zoneId,
            window = SleepWindow(startMillis, endMillis),
            createdAtMillis = nowMillis(),
        )
    }

    private fun run(
        targetDate: LocalDate,
        zoneId: ZoneId,
        window: SleepWindow,
        createdAtMillis: Long,
    ): SleepAnalysisRun {
        val usageRead = usageObservationSource.read(window.startMillis, window.endMillis)
        val locationSignals = locationSignalSource.read(window.startMillis, window.endMillis, zoneId)
        val observations = usageRead.observations.copy(locationSignals = locationSignals)
        val record = SleepAnalyzer.analyze(
            targetDate = targetDate,
            observations = observations,
            zoneId = zoneId,
            nowMillis = createdAtMillis,
            window = window,
        )
        return SleepAnalysisRun(
            targetDate = targetDate,
            zoneId = zoneId,
            window = window,
            observations = observations,
            record = record,
            usageStatus = usageRead.status,
        )
    }

    companion object {
        fun android(context: Context): SleepAnalysisRunner {
            val appContext = context.applicationContext
            return SleepAnalysisRunner(
                usageObservationSource = UsageObservationSource { startMillis, endMillis ->
                    UsageScreenEventReader.readSleepObservationResult(
                        context = appContext,
                        startMillis = startMillis,
                        endMillis = endMillis,
                    )
                },
                locationSignalSource = LocationSignalSource { startMillis, endMillis, zoneId ->
                    LocationSleepSignalReader.readSignals(
                        context = appContext,
                        startMillis = startMillis,
                        endMillis = endMillis,
                        zoneId = zoneId,
                    )
                },
            )
        }
    }
}
