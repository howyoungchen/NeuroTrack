package com.example.neurotrack.domain
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SleepRawDataExport(
    val targetDate: LocalDate,
    val zoneId: ZoneId,
    val rangeStartMillis: Long,
    val rangeEndMillis: Long,
    val generatedAtMillis: Long,
    val observations: SleepObservations,
    val expectedResult: SleepRecord? = null,
)

object SleepRawDataCodec {
    const val FORMAT_VERSION = "neurotrack-sleep-raw-v1"
    private const val NULL_VALUE = "null"

    fun inferTargetDateForRange(
        startMillis: Long,
        endMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LocalDate {
        require(endMillis > startMillis) { "endMillis must be greater than startMillis" }
        val startDate = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()
        val endDateTime = Instant.ofEpochMilli(endMillis).atZone(zoneId).toLocalDateTime()
        val cycleBoundary = LocalTime.of(SleepRules.SLEEP_WINDOW_BOUNDARY_HOUR, 0)
        return if (!endDateTime.toLocalTime().isAfter(cycleBoundary)) {
            endDateTime.toLocalDate()
        } else {
            startDate.plusDays(1)
        }
    }

    fun encode(export: SleepRawDataExport): String {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        return buildString {
            appendLine("# NeuroTrack sleep raw export")
            appendLine("# Keep this file unchanged when using it as a JVM regression fixture.")
            appendLine("format,$FORMAT_VERSION")
            appendLine("generated_at_millis,${export.generatedAtMillis}")
            appendLine("zone_id,${export.zoneId.id}")
            appendLine("target_date,${export.targetDate}")
            appendLine(
                listOf(
                    "range",
                    export.rangeStartMillis,
                    formatMillis(export.rangeStartMillis, export.zoneId, formatter),
                    export.rangeEndMillis,
                    formatMillis(export.rangeEndMillis, export.zoneId, formatter),
                ).joinToString(","),
            )
            appendLine("section,screen_event,timestampMillis,localDateTime,eventType")
            export.observations.screenEvents.sortedBy { it.timestampMillis }.forEach { event ->
                appendLine(
                    listOf(
                        "screen_event",
                        event.timestampMillis,
                        formatMillis(event.timestampMillis, export.zoneId, formatter),
                        event.type.name,
                    ).joinToString(","),
                )
            }
            appendLine("section,interaction_event,timestampMillis,localDateTime,type")
            export.observations.interactionEvents.sortedBy { it.timestampMillis }.forEach { event ->
                appendLine(
                    listOf(
                        "interaction_event",
                        event.timestampMillis,
                        formatMillis(event.timestampMillis, export.zoneId, formatter),
                        event.type.name,
                    ).joinToString(","),
                )
            }
            appendLine("section,location_signal,timestampMillis,localDateTime,atSleepPlace,stationary,leftSleepPlace")
            export.observations.locationSignals.sortedBy { it.timestampMillis }.forEach { signal ->
                appendLine(
                    listOf(
                        "location_signal",
                        signal.timestampMillis,
                        formatMillis(signal.timestampMillis, export.zoneId, formatter),
                        signal.atSleepPlace?.toString() ?: NULL_VALUE,
                        signal.stationary?.toString() ?: NULL_VALUE,
                        signal.leftSleepPlace,
                    ).joinToString(","),
                )
            }
            export.expectedResult?.let { record ->
                appendLine("section,expected_result,dateEpochDay,sleepStartMillis,sleepEndMillis,durationMinutes,wakeUpCount,isMissing,createdAtMillis")
                appendLine(
                    listOf(
                        "expected_result",
                        record.dateEpochDay,
                        record.sleepStartMillis,
                        record.sleepEndMillis,
                        record.durationMinutes,
                        record.wakeUpCount,
                        record.isMissing,
                        record.createdAtMillis,
                    ).joinToString(","),
                )
            }
        }
    }

    fun decode(text: String): SleepRawDataExport {
        var formatSeen = false
        var generatedAtMillis: Long? = null
        var zoneId: ZoneId? = null
        var targetDate: LocalDate? = null
        var rangeStartMillis: Long? = null
        var rangeEndMillis: Long? = null
        var expectedResult: SleepRecord? = null
        val screenEvents = mutableListOf<ScreenEvent>()
        val interactionEvents = mutableListOf<DeviceInteractionEvent>()
        val locationSignals = mutableListOf<LocationSleepSignal>()

        text.lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
            val columns = line.split(",")
            when (columns.first()) {
                "format" -> {
                    requireColumnCount(columns, 2, index)
                    require(columns[1] == FORMAT_VERSION) {
                        "Unsupported sleep raw format '${columns[1]}' at line ${index + 1}"
                    }
                    formatSeen = true
                }
                "generated_at_millis" -> {
                    requireColumnCount(columns, 2, index)
                    generatedAtMillis = columns[1].toLong()
                }
                "zone_id" -> {
                    requireColumnCount(columns, 2, index)
                    zoneId = ZoneId.of(columns[1])
                }
                "target_date" -> {
                    requireColumnCount(columns, 2, index)
                    targetDate = LocalDate.parse(columns[1])
                }
                "range" -> {
                    requireColumnCount(columns, 5, index)
                    rangeStartMillis = columns[1].toLong()
                    rangeEndMillis = columns[3].toLong()
                }
                "screen_event" -> {
                    requireColumnCount(columns, 4, index)
                    screenEvents += ScreenEvent(
                        timestampMillis = columns[1].toLong(),
                        type = ScreenEventType.valueOf(columns[3]),
                    )
                }
                "interaction_event" -> {
                    requireColumnCount(columns, 4, index)
                    interactionEvents += DeviceInteractionEvent(
                        timestampMillis = columns[1].toLong(),
                        type = DeviceInteractionType.valueOf(columns[3]),
                    )
                }
                "location_signal" -> {
                    requireColumnCount(columns, 6, index)
                    locationSignals += LocationSleepSignal(
                        timestampMillis = columns[1].toLong(),
                        atSleepPlace = parseNullableBoolean(columns[3], index),
                        stationary = parseNullableBoolean(columns[4], index),
                        leftSleepPlace = columns[5].toBooleanStrict(),
                    )
                }
                "expected_result" -> {
                    requireColumnCount(columns, 8, index)
                    expectedResult = SleepRecord(
                        dateEpochDay = columns[1].toLong(),
                        sleepStartMillis = columns[2].toLong(),
                        sleepEndMillis = columns[3].toLong(),
                        durationMinutes = columns[4].toInt(),
                        wakeUpCount = columns[5].toInt(),
                        isMissing = columns[6].toBooleanStrict(),
                        createdAtMillis = columns[7].toLong(),
                    )
                }
                "section" -> Unit
                else -> error("Unknown sleep raw row '${columns.first()}' at line ${index + 1}")
            }
        }

        require(formatSeen) { "Missing sleep raw format row" }
        val parsedZoneId = requireNotNull(zoneId) { "Missing zone_id row" }
        return SleepRawDataExport(
            targetDate = requireNotNull(targetDate) { "Missing target_date row" },
            zoneId = parsedZoneId,
            rangeStartMillis = requireNotNull(rangeStartMillis) { "Missing range row" },
            rangeEndMillis = requireNotNull(rangeEndMillis) { "Missing range row" },
            generatedAtMillis = requireNotNull(generatedAtMillis) { "Missing generated_at_millis row" },
            observations = SleepObservations(
                screenEvents = screenEvents.sortedBy { it.timestampMillis },
                interactionEvents = interactionEvents.sortedBy { it.timestampMillis },
                locationSignals = locationSignals.sortedBy { it.timestampMillis },
            ),
            expectedResult = expectedResult,
        )
    }

    private fun formatMillis(
        millis: Long,
        zoneId: ZoneId,
        formatter: DateTimeFormatter,
    ): String =
        Instant.ofEpochMilli(millis).atZone(zoneId).format(formatter)

    private fun parseNullableBoolean(value: String, lineIndex: Int): Boolean? =
        when (value) {
            NULL_VALUE -> null
            "true" -> true
            "false" -> false
            else -> error("Invalid nullable boolean '$value' at line ${lineIndex + 1}")
        }

    private fun requireColumnCount(columns: List<String>, count: Int, lineIndex: Int) {
        require(columns.size == count) {
            "Expected $count columns at line ${lineIndex + 1}, got ${columns.size}"
        }
    }
}
