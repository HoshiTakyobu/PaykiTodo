package com.example.todoalarm.data

import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import org.json.JSONArray
import org.json.JSONObject

class CalendarSemanticsTest {
    @Test fun monthEndPoliciesPreserveLegacyAndSkipInvalidNewDates() {
        val anchor = LocalDate.of(2026, 10, 31)
        val legacy = RecurrenceConfig(true, RecurrenceType.MONTHLY_DAY, endDate = LocalDate.of(2027, 1, 31))
        assertEquals(listOf("2026-10-31", "2026-11-30", "2026-12-31", "2027-01-31"), calendarRecurrenceDates(anchor, legacy).map { it.toString() })
        assertEquals(listOf("2026-10-31", "2026-12-31", "2027-01-31"), calendarRecurrenceDates(anchor, legacy.copy(invalidDatePolicy = "SKIP")).map { it.toString() })
    }
    @Test fun leapDayOnlyReturnsLeapYearsUnderSkipPolicy() {
        val config = RecurrenceConfig(true, RecurrenceType.YEARLY_DATE, endDate = LocalDate.of(2032, 2, 29), invalidDatePolicy = "SKIP")
        assertEquals(listOf("2028-02-29", "2032-02-29"), calendarRecurrenceDates(LocalDate.of(2028, 2, 29), config).map { it.toString() })
    }
    @Test fun weeklyIntervalKeepsOriginalPhaseWhenRangeMoves() {
        val config = RecurrenceConfig(true, RecurrenceType.WEEKLY, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY), LocalDate.of(2026, 11, 18), interval = 2)
        assertEquals(listOf("2026-11-02", "2026-11-04", "2026-11-07", "2026-11-16", "2026-11-18"), calendarRecurrenceDates(LocalDate.of(2026, 10, 24), config, LocalDate.of(2026, 10, 26), config.endDate!!).map { it.toString() })
    }
    @Test fun lastWeekdayIsDistinctFromFifth() {
        val anchor = LocalDate.of(2026, 10, 31)
        val config = RecurrenceConfig(true, RecurrenceType.MONTHLY_NTH_WEEKDAY, endDate = LocalDate.of(2027, 1, 31), monthlyOrdinal = -1)
        assertEquals(listOf("2026-10-31", "2026-11-28", "2026-12-26", "2027-01-30"), calendarRecurrenceDates(anchor, config).map { it.toString() })
        assertEquals(listOf("2026-10-31", "2027-01-30"), calendarRecurrenceDates(anchor, config.copy(monthlyOrdinal = 5)).map { it.toString() })
    }
    @Test fun infiniteSeriesUsesBoundedWindowAndInclusiveCutoff() {
        val anchor = LocalDate.of(2026, 10, 24)
        val config = RecurrenceConfig(true, RecurrenceType.DAILY, interval = 30)
        assertTrue(config.isRecurring)
        assertEquals(listOf(anchor, anchor.plusDays(30), anchor.plusDays(60), anchor.plusDays(90)), calendarRecurrenceDates(anchor, config))
        assertEquals(3, calendarRecurrenceDates(anchor, config.copy(endDate = anchor.plusDays(60))).size)
    }
    @Test fun independentAllDayClocksAreDeduplicatedAndZoned() {
        val json = "[{\"daysBefore\":1,\"minuteOfDay\":540},{\"daysBefore\":0,\"minuteOfDay\":480},{\"daysBefore\":0,\"minuteOfDay\":480}]"
        val triggers = allDayReminderTimes(LocalDate.of(2026, 10, 24), "Asia/Shanghai", json)
        assertEquals(listOf("2026-10-23T01:00:00Z", "2026-10-24T00:00:00Z"), triggers.map { Instant.ofEpochMilli(it).toString() })
        assertTrue(allDayReminderTimes(LocalDate.of(2026, 10, 24), "Asia/Shanghai", "[]").isEmpty())
    }
    @Test fun explicitEmptyRulesDoNotFallBackToLegacyOffsets() {
        val draft = CalendarEventDraft("sample", "", "", LocalDateTime.of(2026, 10, 24, 0, 0), LocalDateTime.of(2026, 10, 24, 0, 0), true,
            eventTimeZoneId = "Asia/Shanghai", allDayReminderRulesJson = "[]", accentColorHex = "#3370FF", reminderOffsetsMinutes = listOf(15), ringEnabled = false, vibrateEnabled = false)
        assertTrue(draft.reminderTriggerTimesMillis().isEmpty())
    }
    @Test fun daylightOverlapUsesLaterOffsetAndGapIsRejected() {
        assertEquals("2026-11-01T06:30:00Z", Instant.ofEpochMilli(calendarEpochMillis(LocalDateTime.of(2026, 11, 1, 1, 30), "America/New_York")).toString())
        try {
            calendarEpochMillis(LocalDateTime.of(2026, 3, 8, 2, 30), "America/New_York")
            fail("nonexistent local time must be rejected")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun legacyBackupArchiveKeepsOriginalAllDayOffsetTriggers() {
        val date = LocalDate.of(2030, 10, 24)
        val start = date.atStartOfDay().toEpochMillis()
        val legacy = JSONObject().put("snapshotVersion", 1).put("tasks", JSONArray().put(JSONObject()
            .put("id", 51).put("itemType", "EVENT").put("title", "旧全天日程").put("notes", "旧备注")
            .put("dueAtMillis", start).put("startAtMillis", start).put("endAtMillis", date.plusDays(1).atStartOfDay().toEpochMillis())
            .put("allDay", true).put("reminderEnabled", true).put("reminderOffsetsCsv", "1440,15")
            .put("ringEnabled", false).put("vibrateEnabled", false)))
        val restored = backupSnapshotFromJson(legacy)
        val archiveRestored = BackupArchiveCodec.decode(BackupArchiveCodec.encode(restored))
        val expected = listOf(date.minusDays(1).atTime(9, 0).toEpochMillis(), date.atTime(8, 45).toEpochMillis())
        assertEquals(expected, restored.tasks.single().reminderTriggerTimesMillis())
        assertEquals(expected, archiveRestored.tasks.single().reminderTriggerTimesMillis())
        assertEquals("旧备注", archiveRestored.tasks.single().notes)
    }

    @Test fun allDayReminderClocksAcrossDstAdvanceBy23Hours() {
        val rules = "[{\"daysBefore\":1,\"minuteOfDay\":540},{\"daysBefore\":0,\"minuteOfDay\":540}]"
        val triggers = allDayReminderTimes(LocalDate.of(2030, 3, 10), "America/New_York", rules)
        assertEquals(listOf("2030-03-09T14:00:00Z", "2030-03-10T13:00:00Z"), triggers.map { Instant.ofEpochMilli(it).toString() })
        assertEquals(23 * 60 * 60_000L, triggers[1] - triggers[0])
    }

    @Test fun allDayReminderRulesRejectNumbersThatWouldBeSilentlyTruncatedOrCoerced() {
        listOf("[{\"daysBefore\":1.5,\"minuteOfDay\":540}]", "[{\"daysBefore\":1,\"minuteOfDay\":540.5}]",
            "[{\"daysBefore\":\"1\",\"minuteOfDay\":540}]", "[{\"daysBefore\":true,\"minuteOfDay\":540}]").forEach { json ->
            assertTrue("Invalid clock rules must not silently change meaning: $json", runCatching { decodeAllDayReminderRules(json) }.isFailure)
        }
    }
}
