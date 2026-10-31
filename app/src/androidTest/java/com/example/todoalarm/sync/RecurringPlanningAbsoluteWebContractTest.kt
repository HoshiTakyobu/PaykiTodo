package com.example.todoalarm.sync

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.todoalarm.data.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

/** LAN expressions and derived ownership are checked against isolated, in-memory Room fixtures. */
class RecurringPlanningAbsoluteWebContractTest {
    private val now = LocalDateTime.of(2030, 1, 6, 12, 0)
    private val source = "数学,每周[周一 08:00>09:40,周三 14:00>15:40],(2030-01-07 08:01),自2030-01-07至2030-01-23;"

    private fun withRepository(block: suspend (AppDatabase, TodoRepository) -> Unit) = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java
        ).build()
        try {
            val repository = TodoRepository(db)
            repository.ensureDefaultGroups()
            block(db, repository)
        } finally {
            db.close()
        }
    }

    private fun parse(text: String = source): TransientPlanningResult {
        val parsed = TransientPlanningParser.parse(text, now)
        assertFalse(parsed.diagnostics.toString(), parsed.hasErrors)
        return parsed
    }

    private fun assertJsonOwners(candidate: JSONObject, expected: Map<Pair<Int, String>, List<String>>) {
        val owners = candidate.getJSONArray("absoluteAssignments")
        val actual = (0 until owners.length()).associate { index ->
            val owner = owners.getJSONObject(index)
            val reminders = owner.getJSONArray("reminders")
            (owner.getInt("slotIndex") to owner.getString("start")) to
                (0 until reminders.length()).map(reminders::getString)
        }
        assertEquals(expected, actual)
        assertEquals(expected.size, owners.length())
        val actualTimes = actual.values.flatten()
        assertEquals(expected.values.flatten().size, actualTimes.size)
        assertEquals(actualTimes.size, actualTimes.distinct().size)
    }

    @Test fun serializedCourseContainsUniqueOwnersAcrossAllSlotsBeforeAnyWrite() = withRepository { db, repo ->
        val parsed = parse("数学,每周[周一 08:00>09:40,周三 14:00>15:40],(5,0,2030-01-06 07:00,2030-01-07 08:01,2030-01-07 08:01,2030-01-09 13:00),自2030-01-07至2030-01-23;")
        val json = TransientPlanningWeb.toJson(parsed)
        assertEquals(0, json.getJSONArray("diagnostics").length())
        val serialized = json.getJSONArray("candidates").getJSONObject(0)
        assertJsonOwners(serialized, mapOf(
            (0 to "2030-01-07T08:00") to listOf("2030-01-06T07:00"),
            (1 to "2030-01-09T14:00") to listOf("2030-01-07T08:01", "2030-01-09T13:00")
        ))
        assertEquals(2, serialized.getJSONArray("slots").length())
        assertTrue(repo.getAllTodos().isEmpty())
        assertTrue(db.todoDao().getAllRecurringTemplates().isEmpty())
        assertTrue(db.todoDao().getAllPlanningNotes().isEmpty())

        val created = repo.importTransientPlanning(
            TransientPlanningWeb.readEdits(json.getJSONArray("candidates"), parsed.candidates, now), "web-initial-owners"
        )
        assertEquals(6, created.size)
        val firstOwner = created.single { it.calendarStartDateTime().toString() == "2030-01-07T08:00" }
        val secondOwner = created.single { it.calendarStartDateTime().toString() == "2030-01-09T14:00" }
        assertEquals(listOf(LocalDateTime.parse("2030-01-06T07:00").toEpochMillis()), firstOwner.configuredAbsoluteReminderTimesMillis)
        assertEquals(listOf("2030-01-07T08:01", "2030-01-09T13:00").map { LocalDateTime.parse(it).toEpochMillis() },
            secondOwner.configuredAbsoluteReminderTimesMillis)
        assertEquals(3, created.sumOf { it.configuredAbsoluteReminderTimesMillis.size })
        assertTrue(created.all { it.configuredReminderOffsetsMinutes == listOf(5, 0) })
    }

    @Test fun reminderEditRecomputesOwnershipUsingFrozenYearAndKeepsOriginalCandidate() = withRepository { db, repo ->
        val parsed = parse()
        val original = parsed.candidates.single()
        val originalJson = TransientPlanningWeb.toJson(parsed)
        assertJsonOwners(originalJson.getJSONArray("candidates").getJSONObject(0), mapOf(
            (1 to "2030-01-09T14:00") to listOf("2030-01-07T08:01")
        ))
        val edits = originalJson.getJSONArray("candidates")
        edits.getJSONObject(0).put("reminders", "(01-09+14:01)")
        val edited = TransientPlanningWeb.readEdits(edits, parsed.candidates, now)
        assertTrue(repo.getAllTodos().isEmpty())
        assertTrue(db.todoDao().getAllRecurringTemplates().isEmpty())
        assertEquals(listOf(LocalDateTime.parse("2030-01-09T14:01")), edited.single().absoluteReminderTimes)
        assertEquals(original.id, edited.single().id)
        assertEquals(original, parsed.candidates.single())
        assertEquals("(2030-01-07 08:01)", original.reminderExpression)
        assertEquals(listOf(LocalDateTime.parse("2030-01-07T08:01")), original.absoluteReminderTimes)
        val refreshed = TransientPlanningWeb.toJson(TransientPlanningResult(edited, emptyList()))
        assertJsonOwners(refreshed.getJSONArray("candidates").getJSONObject(0), mapOf(
            (0 to "2030-01-14T08:00") to listOf("2030-01-09T14:01")
        ))
        assertJsonOwners(TransientPlanningWeb.toJson(parsed).getJSONArray("candidates").getJSONObject(0), mapOf(
            (1 to "2030-01-09T14:00") to listOf("2030-01-07T08:01")
        ))

        val created = repo.importTransientPlanning(edited, "web-edited-owner")
        val owner = created.single { it.configuredAbsoluteReminderTimesMillis.isNotEmpty() }
        assertEquals("2030-01-14T08:00", owner.calendarStartDateTime().toString())
        assertEquals(listOf(LocalDateTime.parse("2030-01-09T14:01").toEpochMillis()), owner.reminderTriggerTimesMillis())
        assertTrue(created.filter { it.id != owner.id }.all { !it.reminderEnabled && it.reminderAtMillis == null })
    }

    @Test fun forgedDerivedOwnershipCannotBypassFiniteRangeAndIsIgnoredForValidEdits() = withRepository { db, repo ->
        val parsed = parse("父事项;\n$source")
        val original = parsed.candidates.toList()
        val edits = TransientPlanningWeb.toJson(parsed).getJSONArray("candidates")
        val course = edits.getJSONObject(1)
        val forged = JSONArray().put(JSONObject()
            .put("slotIndex", 1).put("start", "2030-01-23T14:00")
            .put("reminders", JSONArray().put("2030-01-23T14:01")))
        course.put("reminders", "(2030-01-23 14:01)").put("absoluteAssignments", forged)
        val groupsBefore = db.todoDao().getAllGroups()
        val error = assertThrows(IllegalArgumentException::class.java) {
            TransientPlanningWeb.readEdits(edits, parsed.candidates, now)
        }
        assertTrue(error.message.orEmpty(), error.message.orEmpty().contains("第2行"))
        assertTrue(error.message.orEmpty(), error.message.orEmpty().contains("绝对提醒"))
        assertTrue(error.message.orEmpty(), error.message.orEmpty().contains("2030-01-23 14:01"))
        assertEquals(original, parsed.candidates)
        assertTrue(repo.getAllTodos().isEmpty())
        assertTrue(db.todoDao().getAllRecurringTemplates().isEmpty())
        assertTrue(db.todoDao().getAllPlanningNotes().isEmpty())
        assertEquals(groupsBefore, db.todoDao().getAllGroups())

        course.put("reminders", "(2030-01-09 14:01)")
        val accepted = TransientPlanningWeb.readEdits(edits, parsed.candidates, now)
        assertEquals(listOf(LocalDateTime.parse("2030-01-09T14:01")), accepted[1].absoluteReminderTimes)
        val refreshed = TransientPlanningWeb.toJson(TransientPlanningResult(accepted, emptyList()))
        assertJsonOwners(refreshed.getJSONArray("candidates").getJSONObject(1), mapOf(
            (0 to "2030-01-14T08:00") to listOf("2030-01-09T14:01")
        ))
        assertEquals("2030-01-23T14:00", course.getJSONArray("absoluteAssignments").getJSONObject(0).getString("start"))
        assertEquals(original, parsed.candidates)
        assertTrue(repo.getAllTodos().isEmpty())
    }
}
