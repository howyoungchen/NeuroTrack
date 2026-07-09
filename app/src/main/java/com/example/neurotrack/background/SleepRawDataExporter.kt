package com.example.neurotrack.background

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.neurotrack.R
import com.example.neurotrack.domain.SleepRawDataExport
import com.example.neurotrack.domain.SleepRawDataCodec
import java.io.File

object SleepRawDataExporter {
    private const val EXPORT_FILE_NAME = "neurotrack-sleep-raw.csv"

    suspend fun createShareIntent(
        context: Context,
        startMillis: Long,
        endMillis: Long,
    ): Intent {
        require(endMillis > startMillis) { "endMillis must be greater than startMillis" }

        val analysis = SleepAnalysisRunner.android(context).runForRange(
            startMillis = startMillis,
            endMillis = endMillis,
        )
        val body = SleepRawDataCodec.encode(
            SleepRawDataExport(
                targetDate = analysis.targetDate,
                zoneId = analysis.zoneId,
                rangeStartMillis = startMillis,
                rangeEndMillis = endMillis,
                generatedAtMillis = analysis.record.createdAtMillis,
                observations = analysis.observations,
                expectedResult = analysis.record,
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
