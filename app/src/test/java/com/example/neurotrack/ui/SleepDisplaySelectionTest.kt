package com.example.neurotrack.ui

import com.example.neurotrack.data.SleepRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SleepDisplaySelectionTest {
    @Test
    fun latestSleepRecordForDisplay_returnsMostRecentValidRecord() {
        val today = LocalDate.of(2026, 7, 4).toEpochDay()
        val yesterday = today - 1

        val result = latestSleepRecordForDisplay(
            records = listOf(
                sleepRecord(dateEpochDay = yesterday, durationMinutes = 480),
                sleepRecord(dateEpochDay = today, durationMinutes = 450),
            ),
            todayEpochDay = today,
        )

        assertEquals(today, result?.dateEpochDay)
    }

    @Test
    fun latestSleepRecordForDisplay_ignoresMissingAndZeroDurationRecords() {
        val today = LocalDate.of(2026, 7, 4).toEpochDay()
        val yesterday = today - 1

        val result = latestSleepRecordForDisplay(
            records = listOf(
                sleepRecord(dateEpochDay = today, durationMinutes = 0),
                sleepRecord(dateEpochDay = yesterday, durationMinutes = 460),
                sleepRecord(dateEpochDay = today - 2, durationMinutes = 470, isMissing = true),
            ),
            todayEpochDay = today,
        )

        assertEquals(yesterday, result?.dateEpochDay)
    }

    @Test
    fun latestSleepRecordForDisplay_ignoresFutureRecords() {
        val today = LocalDate.of(2026, 7, 4).toEpochDay()

        val result = latestSleepRecordForDisplay(
            records = listOf(
                sleepRecord(dateEpochDay = today + 1, durationMinutes = 480),
                sleepRecord(dateEpochDay = today - 1, durationMinutes = 450),
            ),
            todayEpochDay = today,
        )

        assertEquals(today - 1, result?.dateEpochDay)
    }

    @Test
    fun latestSleepRecordForDisplay_returnsNullWhenNoValidRecordsExist() {
        val today = LocalDate.of(2026, 7, 4).toEpochDay()

        val result = latestSleepRecordForDisplay(
            records = listOf(
                sleepRecord(dateEpochDay = today, durationMinutes = 0),
                sleepRecord(dateEpochDay = today - 1, durationMinutes = 480, isMissing = true),
            ),
            todayEpochDay = today,
        )

        assertNull(result)
    }

    private fun sleepRecord(
        dateEpochDay: Long,
        durationMinutes: Int,
        isMissing: Boolean = false,
    ): SleepRecordEntity =
        SleepRecordEntity(
            dateEpochDay = dateEpochDay,
            sleepStartMillis = 1L,
            sleepEndMillis = 2L,
            durationMinutes = durationMinutes,
            wakeUpCount = 0,
            isMissing = isMissing,
            createdAtMillis = 3L,
        )
}
