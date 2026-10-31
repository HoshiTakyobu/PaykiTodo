package com.example.todoalarm.sync

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.todoalarm.data.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class TransientPlanningWebContractTest {
    private val now = LocalDateTime.of(2026, 11, 1, 12, 0)

    @Test fun editedPreviewUsesFrozenDateAndCommitsOnceWithoutNotes() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            val repo = TodoRepository(db)
            val parsed = TransientPlanningParser.parse("- 出行\n  - 买车票,16:30,(5,0,09:00);\n- 数学,每2周[周一 08:00>09:40,周三 14:00>15:40];", now)
            val json = TransientPlanningWeb.toJson(parsed)
            assertTrue(repo.getAllTodos().isEmpty())
            assertTrue(repo.getAllPlanningNotes().isEmpty())
            val edits = json.getJSONArray("candidates")
            edits.getJSONObject(1).put("title", "车票,与保险; \"核对\"").put("notes", "改后的备注; #原样保留")
            val candidates = TransientPlanningWeb.readEdits(edits, parsed.candidates, now)
            assertEquals(now.toLocalDate(), candidates[1].dueAt!!.toLocalDate())
            val created = repo.importTransientPlanning(candidates, "web-once")
            val repeated = repo.importTransientPlanning(candidates, "web-once")
            assertEquals(created.map { it.id }, repeated.map { it.id })
            val child = created.single { it.title.startsWith("车票,") }
            assertEquals(created.single { it.title == "出行" }.id, child.parentTodoId)
            assertEquals(3, child.reminderTriggerTimesMillis().size)
            assertTrue(repo.getAllPlanningNotes().isEmpty())
        } finally { db.close() }
    }

    @Test fun injectedExtraItemsAndUnknownIdentityAreRejected() {
        val parsed = TransientPlanningParser.parse("阅读,16:30;", now)
        val edits = TransientPlanningWeb.toJson(parsed).getJSONArray("candidates")
        edits.getJSONObject(0).put("time", "16:30;隐藏事项")
        assertThrows(IllegalArgumentException::class.java) { TransientPlanningWeb.readEdits(edits, parsed.candidates, now) }
        edits.getJSONObject(0).put("time", "16:30").put("id", "unknown")
        assertThrows(IllegalStateException::class.java) { TransientPlanningWeb.readEdits(edits, parsed.candidates, now) }
        assertThrows(IllegalArgumentException::class.java) { TransientPlanningWeb.readEdits(JSONArray(), parsed.candidates, now) }
    }
}
