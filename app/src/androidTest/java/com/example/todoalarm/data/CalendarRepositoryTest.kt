package com.example.todoalarm.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDateTime

class CalendarRepositoryTest {
    @Test fun ownershipRecurrenceMemorialAndTemplateDeletionSurviveArchiveRestore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = TodoRepository(db)
            val groups = repo.ensureDefaultGroups()
            repo.saveCalendar(PlannerCalendar("courses", "课程表", "#7955CC"))
            val start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(15).withSecond(0).withNano(0)
            repo.createCalendarEventFromDraft(CalendarEventDraft("数学", "", "", start, start.plusMinutes(30), false, "#7955CC", ringEnabled = false, vibrateEnabled = false,
                calendarId = "courses", recurrence = RecurrenceConfig(true, RecurrenceType.DAILY, endDate = start.toLocalDate().plusDays(4)), groupId = groups.first().id))
            repo.ensureRecurringInstancesAhead()
            val tasks = repo.exportSnapshot(AppSettings()).tasks
            assertTrue(tasks.size > 1)
            assertTrue(tasks.all { it.calendarId == "courses" })
            val todo = repo.createFromDraft(TodoDraft("毕业纪念", "", start, null, groups.first().id, false, false, countdownEnabled = true)).first()
            repo.setCompleted(todo.id, true)
            assertTrue(CountdownVisibility.needsDecision(repo.getTodo(todo.id)!!, System.currentTimeMillis()))
            repo.setCountdownPastMode(todo.id, "KEEP")
            val template = ScheduleTemplate(name = "本周", templateType = "WEEKLY", payloadJson = """{"entries":[{"calendarId":"courses","futureField":42}]}""")
            db.todoDao().insertScheduleTemplate(template)
            val exported = BackupArchiveCodec.decode(BackupArchiveCodec.encode(repo.exportSnapshot(AppSettings())))
            repo.importSnapshot(exported)
            assertEquals("KEEP", repo.getTodo(todo.id)!!.countdownPastMode)
            assertTrue(repo.getTodo(todo.id)!!.completed)
            repo.deleteCalendar("courses")
            val restored = repo.exportSnapshot(AppSettings())
            assertTrue(restored.tasks.all { it.calendarId == DEFAULT_CALENDAR_ID })
            assertTrue(restored.templates.all { it.calendarId == DEFAULT_CALENDAR_ID })
            val entry = org.json.JSONObject(restored.scheduleTemplates.single().payloadJson).getJSONArray("entries").getJSONObject(0)
            assertEquals("default", entry.getString("calendarId")); assertEquals(42, entry.getInt("futureField"))
            repo.importSnapshot(restored.copy(tasks = restored.tasks.map { it.copy(calendarId = "missing") }))
            assertTrue(repo.exportSnapshot(AppSettings()).tasks.all { it.calendarId == DEFAULT_CALENDAR_ID })
        } finally { db.close() }
    }

    @Test fun actualWindowsArchiveCanBeEditedAndExportedForWindows() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = context.getExternalFilesDir(null)!!
        val incoming = File(directory, "calendar-desktop.paykibak")
        org.junit.Assume.assumeTrue("Supply Windows-generated calendar-desktop.paykibak for cross-client acceptance", incoming.exists())
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = TodoRepository(db)
            val snapshot = BackupArchiveCodec.decode(incoming.readBytes())
            repo.importSnapshot(snapshot)
            val imported = repo.exportSnapshot(snapshot.settings)
            assertTrue(imported.calendars.any { it.id == "cross-courses" })
            val event = imported.tasks.single { it.title == "桌面跨端课程" }
            assertEquals("cross-courses", event.calendarId)
            val memorial = imported.tasks.single { it.title == "跨端纪念" }
            assertTrue(memorial.completed); assertEquals("KEEP", memorial.countdownPastMode)
            repo.updateTodo(event.copy(title = "手机修改课程"))
            repo.saveCalendar(PlannerCalendar("cross-courses", "手机修改课程表", "#DB5298"))
            val output = repo.exportSnapshot(snapshot.settings)
            File(directory, "calendar-android.paykibak").writeBytes(BackupArchiveCodec.encode(output))
        } finally { db.close() }
    }
}
