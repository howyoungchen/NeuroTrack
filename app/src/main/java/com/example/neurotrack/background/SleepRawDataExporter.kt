package com.example.neurotrack.background

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.neurotrack.R
import com.example.neurotrack.domain.SleepAnalyzer
import com.example.neurotrack.domain.SleepRawDataCodec
import com.example.neurotrack.domain.SleepRawDataExport
import java.io.File
import java.time.ZoneId

object SleepRawDataExporter {
    private const val EXPORT_FILE_NAME = "neurotrack-sleep-raw.csv"

    suspend fun createShareIntent(
        context: Context,
        startMillis: Long,
        endMillis: Long,
    ): Intent {
        require(endMillis > startMillis) { "endMillis must be greater than startMillis" }

        val zoneId = ZoneId.systemDefault()
        val targetDate = SleepRawDataCodec.inferTargetDateForRange(
            startMillis = startMillis,
            endMillis = endMillis,
            zoneId = zoneId,
        )
        val usageObservations = UsageScreenEventReader.readSleepObservations(
            context = context,
            startMillis = startMillis,
            endMillis = endMillis,
        )
        val locationSignals = LocationSleepSignalReader.readSignals(
            context = context,
            startMillis = startMillis,
            endMillis = endMillis,
            zoneId = zoneId,
        )
        val observations = usageObservations.copy(locationSignals = locationSignals)
        val generatedAtMillis = System.currentTimeMillis()
        val expectedResult = SleepAnalyzer.analyze(
            targetDate = targetDate,
            observations = observations,
            zoneId = zoneId,
            nowMillis = generatedAtMillis,
        )
        val body = SleepRawDataCodec.encode(
            SleepRawDataExport(
                targetDate = targetDate,
                zoneId = zoneId,
                rangeStartMillis = startMillis,
                rangeEndMillis = endMillis,
                generatedAtMillis = generatedAtMillis,
                observations = observations,
                expectedResult = expectedResult,
            ),
        )

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, EXPORT_FILE_NAME)
        file.writeText(body)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, context.getString(R.string.share_sleep_raw_data_title))
    }
}
