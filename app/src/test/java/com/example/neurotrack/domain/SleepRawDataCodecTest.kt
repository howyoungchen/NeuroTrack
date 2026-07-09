package com.example.neurotrack.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SleepRawDataCodecTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun inferTargetDateForRange_whenRangeEndsBeforeNoonUsesEndDate() {
        assertEquals(
            LocalDate.of(2026, 7, 4),
            SleepRawDataCodec.inferTargetDateForRange(
                startMillis = millis("2026-07-03T20:00:00"),
                endMillis = millis("2026-07-04T12:00:00"),
                zoneId = zoneId,
            ),
        )
    }

    @Test
    fun inferTargetDateForRange_whenRangeEndsAfterNoonUsesNextDateFromStart() {
        assertEquals(
            LocalDate.of(2026, 7, 4),
            SleepRawDataCodec.inferTargetDateForRange(
                startMillis = millis("2026-07-03T20:00:00"),
                endMillis = millis("2026-07-03T23:30:00"),
                zoneId = zoneId,
            ),
        )
    }

    @Test
    fun decode_readsFixtureAndReplaysExpectedSleepRecord() {
        val fixtures = sleepRawFixtures()

        assertFalse("Expected at least one sleep raw fixture", fixtures.isEmpty())
        fixtures.forEach { fixture ->
            val export = SleepRawDataCodec.decode(fixture.readText())
            val expected = requireNotNull(export.expectedResult) {
                "Fixture ${fixture.name} is missing expected_result"
            }
            val actual = SleepRawDataReplay.replay(export)

            assertEquals("dateEpochDay in ${fixture.name}", expected.dateEpochDay, actual.dateEpochDay)
            assertEquals("sleepStartMillis in ${fixture.name}", expected.sleepStartMillis, actual.sleepStartMillis)
            assertEquals("sleepEndMillis in ${fixture.name}", expected.sleepEndMillis, actual.sleepEndMillis)
            assertEquals("durationMinutes in ${fixture.name}", expected.durationMinutes, actual.durationMinutes)
            assertEquals("wakeUpCount in ${fixture.name}", expected.wakeUpCount, actual.wakeUpCount)
            assertEquals("isMissing in ${fixture.name}", expected.isMissing, actual.isMissing)
        }
    }

    @Test
    fun encode_preservesReplayableFixtureFields() {
        val original = SleepRawDataCodec.decode(sleepRawFixtures().first().readText())
        val decoded = SleepRawDataCodec.decode(SleepRawDataCodec.encode(original))

        assertEquals(original.targetDate, decoded.targetDate)
        assertEquals(original.zoneId, decoded.zoneId)
        assertEquals(original.rangeStartMillis, decoded.rangeStartMillis)
        assertEquals(original.rangeEndMillis, decoded.rangeEndMillis)
        assertEquals(original.generatedAtMillis, decoded.generatedAtMillis)
        assertEquals(original.observations.screenEvents, decoded.observations.screenEvents)
        assertEquals(original.observations.interactionEvents, decoded.observations.interactionEvents)
        assertEquals(original.observations.locationSignals, decoded.observations.locationSignals)
        assertEquals(original.expectedResult, decoded.expectedResult)
    }

    private fun sleepRawFixtures(): List<File> {
        val resource = requireNotNull(javaClass.classLoader?.getResource("sleep-raw")) {
            "Missing sleep-raw test resources"
        }
        return File(resource.toURI())
            .walkTopDown()
            .filter { it.isFile && it.extension == "csv" }
            .sortedBy { it.name }
            .toList()
    }

    private fun millis(dateTime: String): Long =
        LocalDateTime.parse(dateTime)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
}
