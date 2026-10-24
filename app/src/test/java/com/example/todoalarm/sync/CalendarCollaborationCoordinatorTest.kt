package com.example.todoalarm.sync

import com.example.todoalarm.data.RecurringTaskTemplate
import com.example.todoalarm.data.RecurringInstanceSkip
import com.example.todoalarm.data.TodoItem
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CalendarCollaborationCoordinatorTest {
    @Test fun simultaneousEditsAndViewerEditsRemainConflicts() {
        val policy = SharedCalendarSyncPolicy
        assertEquals(SharedCalendarSyncPolicy.Decision.CONFLICT,
            policy.decide("before", 5, "local-change", 6, false, false, true))
        assertEquals(SharedCalendarSyncPolicy.Decision.CONFLICT,
            policy.decide("before", 5, "local-change", 5, false, false, false))
        assertEquals(SharedCalendarSyncPolicy.Decision.CONFLICT,
            policy.decide(null, -1, "local", 3, false, false, true))
    }

    @Test fun cleanRemoteChangesImportAndLocalEditsUploadOnlyAtKnownRevision() {
        val policy = SharedCalendarSyncPolicy
        assertEquals(SharedCalendarSyncPolicy.Decision.IMPORT,
            policy.decide("before", 5, "before", 6, false, false, true))
        assertEquals(SharedCalendarSyncPolicy.Decision.UPLOAD,
            policy.decide("before", 5, "local-change", 5, false, false, true))
        assertEquals(SharedCalendarSyncPolicy.Decision.CLEAN,
            policy.decide("before", 5, "before", 5, false, false, true))
        // A newer local fingerprint after an upload stays dirty against the uploaded baseline.
        assertEquals(SharedCalendarSyncPolicy.Decision.UPLOAD,
            policy.decide("uploaded", 6, "edited-during-upload", 6, false, false, true))
    }

    @Test fun firstSyncImportsAnEmptyLocalCopyAndPreservesAnUnmatchedNonemptyCopy() {
        val policy = SharedCalendarSyncPolicy
        assertEquals(SharedCalendarSyncPolicy.Decision.IMPORT,
            policy.decide(null, -1, "empty", 0, true, true, true))
        assertEquals(SharedCalendarSyncPolicy.Decision.UPLOAD,
            policy.decide(null, -1, "created", 0, false, true, true))
        assertEquals(SharedCalendarSyncPolicy.Decision.CONFLICT,
            policy.decide(null, -1, "created", 0, false, true, false))
    }

    @Test fun rejoiningNeverPublishesTheLocallyDisabledRevokedCopy() {
        assertEquals(SharedCalendarSyncPolicy.Decision.IMPORT,
            SharedCalendarSyncPolicy.decide("active-before", 5, "disabled-after-revocation", 5,
                false, false, true, wasRevoked = true))
    }

    @Test fun roleDowngradeReloadsPrivateProjectionEvenWhenEventRevisionIsUnchanged() {
        val oldWire = SharedCalendarWireCodec.encode(listOf(event().copy(notes = "private-detail", eventVisibility = "PRIVATE")), emptyList())
        val fingerprint = SharedCalendarWireCodec.fingerprint(oldWire)
        val permissionChanged = SharedCalendarSyncPolicy.requiresPermissionReload("EDITOR", "viewer")
        assertEquals(SharedCalendarSyncPolicy.Decision.IMPORT,
            SharedCalendarSyncPolicy.decide(fingerprint, 5, fingerprint, 5, false, false, false, wasRevoked = permissionChanged))
        val projection = JSONArray().put(JSONObject().put("id", "wire-event").put("title", "忙碌")
            .put("startAtMillis", 1000).put("endAtMillis", 2000).put("redacted", true))
        assertEquals("", SharedCalendarWireCodec.decode(projection, "shared:source", 1).tasks.single().notes)
        assertFalse(SharedCalendarSyncPolicy.requiresPermissionReload("OWNER", "owner"))
    }

    @Test fun recurringIdentitySurvivesReschedulingAndDiffersBetweenOccurrences() {
        val first = event().copy(sharedEventId = "", recurringSeriesId = "series", recurrenceAnchorDueAtMillis = 1000)
        assertEquals("series:1000", SharedCalendarWireCodec.eventId(first))
        assertEquals(SharedCalendarWireCodec.eventId(first), SharedCalendarWireCodec.eventId(first.copy(dueAtMillis = 9000)))
        assertNotEquals(SharedCalendarWireCodec.eventId(first), SharedCalendarWireCodec.eventId(first.copy(recurrenceAnchorDueAtMillis = 2000)))
    }

    @Test fun wireRoundtripPreservesRecurrenceTimezoneParticipantsAndExtensionFields() {
        val task = event().copy(recurringSeriesId = "series", recurrenceType = "WEEKLY", recurrenceInterval = 2,
            eventTimeZoneId = "America/New_York", allDayReminderRulesJson = "[{\"daysBefore\":1,\"minuteOfDay\":540}]",
            eventParticipantsJson = "[{\"username\":\"viewer\",\"response\":\"ACCEPTED\"}]")
        val template = RecurringTaskTemplate(id = 52, seriesId = "series", title = "循环",
            groupId = 17, dueHour = 9, dueMinute = 0, ringEnabled = true, vibrateEnabled = true,
            recurrenceType = "WEEKLY", startEpochDay = 20000, endEpochDay = 21000,
            calendarId = "shared:source", recurrenceInterval = 2, eventTimeZoneId = "America/New_York", recurrenceNeverEnds = true)
        val cached = JSONArray().put(JSONObject().put("id", "wire-event").put("customExtension", JSONObject().put("v", 2))
            .put("template", JSONObject().put("customMaster", "keep")))
        val wire = SharedCalendarWireCodec.encode(listOf(task), listOf(template), cached,
            listOf(RecurringInstanceSkip(seriesId = "series", instanceEpochDay = 20001),
                RecurringInstanceSkip(seriesId = "series", instanceEpochDay = 20008)))
        val encoded = wire.getJSONObject(0)
        assertFalse(encoded.has("calendarId")); assertFalse(encoded.has("groupId")); assertFalse(encoded.has("sharedEventId"))
        assertEquals(2, encoded.getJSONObject("customExtension").getInt("v"))
        assertEquals("keep", encoded.getJSONObject("template").getString("customMaster"))
        assertFalse(encoded.getJSONObject("template").has("id"))
        val restored = SharedCalendarWireCodec.decode(wire, "shared:destination", 25)
        assertEquals("wire-event", restored.tasks.single().sharedEventId)
        assertEquals(0L, restored.tasks.single().id)
        assertEquals(25L, restored.tasks.single().groupId)
        assertEquals("America/New_York", restored.tasks.single().eventTimeZoneId)
        val participant = JSONArray(restored.tasks.single().eventParticipantsJson).getJSONObject(0)
        assertEquals("viewer", participant.getString("username"))
        assertEquals("ACCEPTED", participant.getString("response"))
        assertEquals(2, restored.templates.single().recurrenceInterval)
        assertTrue(restored.templates.single().recurrenceNeverEnds)
        assertEquals("shared:destination", restored.templates.single().calendarId)
        assertEquals(listOf(20001L, 20008L), restored.skips.map { it.instanceEpochDay })
    }

    @Test fun redactedBusyBlocksNeverCreateReminderOrRecurrence() {
        val redacted = JSONObject().put("id", "private").put("title", "忙碌").put("startAtMillis", 1000)
            .put("endAtMillis", 2000).put("redacted", true).put("notes", "stale-private")
            .put("location", "stale-private").put("reminderEnabled", true)
            .put("template", JSONObject().put("seriesId", "hidden"))
        val decoded = SharedCalendarWireCodec.decode(JSONArray().put(redacted), "shared:source", 1)
        assertEquals("", decoded.tasks.single().notes)
        assertEquals("", decoded.tasks.single().location)
        assertFalse(decoded.tasks.single().reminderEnabled)
        assertNull(decoded.tasks.single().recurringSeriesId)
        assertTrue(decoded.templates.isEmpty())
    }

    @Test fun duplicatedWireIdsRejectBeforeProducingLocalRows() {
        val event = JSONObject().put("id", "duplicate").put("title", "事件")
        assertThrows(IllegalArgumentException::class.java) {
            SharedCalendarWireCodec.decode(JSONArray().put(event).put(JSONObject(event.toString())), "shared:source", 1)
        }
    }

    @Test fun invalidRemoteCalendarFieldsRejectBeforeProducingAnUnrenderableCache() {
        val event = JSONObject().put("id", "invalid").put("title", "事件").put("startAtMillis", 1000).put("endAtMillis", 2000)
        assertTrue(runCatching { SharedCalendarWireCodec.decode(JSONArray().put(JSONObject(event.toString())
            .put("eventTimeZoneId", "Mars/Base")), "shared:source", 1) }.isFailure)
        assertTrue(runCatching { SharedCalendarWireCodec.decode(JSONArray().put(JSONObject(event.toString())
            .put("allDay", true).put("allDayReminderRulesJson", "[{\"daysBefore\":0,\"minuteOfDay\":540.5}]")), "shared:source", 1) }.isFailure)
    }

    @Test fun fingerprintIgnoresJsonKeyAndLocalRowOrderButTracksEventChanges() {
        val first = event()
        val second = event().copy(id = 19, sharedEventId = "wire-second", title = "另一个")
        val a = SharedCalendarWireCodec.encode(listOf(first, second), emptyList())
        val b = SharedCalendarWireCodec.encode(listOf(second, first.copy(id = 99)), emptyList())
        assertEquals(SharedCalendarWireCodec.fingerprint(a), SharedCalendarWireCodec.fingerprint(b))
        val changed = SharedCalendarWireCodec.encode(listOf(first.copy(title = "改动"), second), emptyList())
        assertNotEquals(SharedCalendarWireCodec.fingerprint(a), SharedCalendarWireCodec.fingerprint(changed))
        assertEquals(SharedCalendarWireCodec.fingerprint(JSONArray().put(JSONObject("{\"a\":1,\"b\":2}"))),
            SharedCalendarWireCodec.fingerprint(JSONArray().put(JSONObject("{\"b\":2,\"a\":1}"))))
    }

    private fun event() = TodoItem(id = 11, itemType = "EVENT", title = "事件", dueAtMillis = 1000,
        startAtMillis = 1000, endAtMillis = 2000, reminderAtMillis = null, reminderEnabled = false,
        ringEnabled = true, vibrateEnabled = true, sharedEventId = "wire-event", calendarId = "shared:source", createdAtMillis = 200)
}
