package com.example.todoalarm.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDateTime

class CalendarRepositoryTest {
    @Test fun calendarColorUpdatesInheritedEventsAndRulesButPreservesCustomColor() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = TodoRepository(db); val group = repo.ensureDefaultGroups().first().id
            repo.saveCalendar(PlannerCalendar("color-test", "课程", "#7955CC"))
            val start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)
            val draft = CalendarEventDraft("跟随日历", "", "", start, start.plusMinutes(30), false, "#7955CC", ringEnabled = false, vibrateEnabled = false,
                calendarId = "color-test", recurrence = RecurrenceConfig(true, RecurrenceType.DAILY, endDate = start.toLocalDate().plusDays(3)), groupId = group)
            repo.createCalendarEventFromDraft(draft)
            repo.createCalendarEventFromDraft(draft.copy(title = "自定义", accentColorHex = "#D94646", recurrence = RecurrenceConfig()))
            db.todoDao().insertScheduleTemplate(ScheduleTemplate(name = "课程周", templateType = "WEEKLY", payloadJson = """{"entries":[{"calendarId":"color-test","accentColorHex":"#7955CC","future":42}]}"""))
            repo.saveCalendar(PlannerCalendar("color-test", "新名称", "#16A369"))
            repo.ensureRecurringInstancesAhead()
            val snapshot = repo.exportSnapshot(AppSettings())
            assertTrue(snapshot.tasks.filter { it.title == "跟随日历" }.all { it.accentColorHex == "#16A369" })
            assertEquals("#D94646", snapshot.tasks.single { it.title == "自定义" }.accentColorHex)
            assertTrue(snapshot.templates.all { it.accentColorHex == "#16A369" })
            val entry = org.json.JSONObject(snapshot.scheduleTemplates.single().payloadJson).getJSONArray("entries").getJSONObject(0)
            assertEquals("#16A369", entry.getString("accentColorHex")); assertEquals(42, entry.getInt("future"))
        } finally { db.close() }
    }
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
            if (imported.tasks.any { it.title.startsWith("跨午夜循环") }) {
                assertEquals(4, imported.tasks.count { it.title.startsWith("跨午夜循环") })
                repo.ensureRecurringInstancesAhead()
                assertEquals(4, repo.exportSnapshot(snapshot.settings).tasks.count { it.title.startsWith("跨午夜循环") })
                assertEquals(2, imported.tasks.count { it.title == "多时段跨端" })
                assertTrue(imported.tasks.single { it.title == "全天跨端" }.allDay)
            }
            repo.updateTodo(event.copy(title = "手机修改课程"))
            repo.saveCalendar(PlannerCalendar("cross-courses", "手机修改课程表", "#DB5298"))
            val output = repo.exportSnapshot(snapshot.settings)
            File(directory, "calendar-android.paykibak").writeBytes(BackupArchiveCodec.encode(output))
        } finally { db.close() }
    }
}
