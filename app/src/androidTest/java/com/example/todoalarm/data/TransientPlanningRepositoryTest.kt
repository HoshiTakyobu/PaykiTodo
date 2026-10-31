package com.example.todoalarm.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.UUID

class TransientPlanningRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = LocalDateTime.of(2026,11,1,12,0)
    @Test fun atomicHierarchyCourseAndReminderArchiveRoundtrip() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try {
            val repository = TodoRepository(db)
            val result = TransientPlanningParser.parse("- 出行\n  - 车票,16:30,(5,0,09:00,2027-07-03 15:00); #原始备注\n  - 行李;\n- 数学,每2周[周一 08:00>09:40,周三 14:00>15:40];",now)
            assertFalse(result.diagnostics.toString(),result.hasErrors)
            val created = repository.importTransientPlanning(result.candidates,"roundtrip")
            val root=created.single { it.title=="出行" };val child=created.single { it.title=="车票" }
            assertEquals(root.id,child.parentTodoId);assertEquals("原始备注",child.notes)
            assertEquals(4,child.reminderTriggerTimesMillis().size)
            assertFalse(root.reminderEnabled)
            val course=created.filter { it.title=="数学" }
            assertTrue(course.size>2);assertEquals(1,course.map { it.multiSlotBundleId }.distinct().size)
            assertEquals(2,course.map { it.recurringSeriesId }.distinct().size)
            assertTrue(course.all { it.recurrenceEndEpochDay==null && it.recurrenceInterval==2 })
            assertTrue(db.todoDao().getAllPlanningNotes().isEmpty())
            val restored = backupSnapshotFromJson(repository.exportSnapshot(AppSettings()).toJson())
            val recovered=restored.tasks.single { it.id==child.id }
            assertEquals(child.reminderAbsoluteTimesCsv,recovered.reminderAbsoluteTimesCsv)
            assertEquals(child.reminderTriggerTimesMillis(),recovered.reminderTriggerTimesMillis())
            repository.ensureRecurringInstancesAhead(LocalDateTime.of(2027,5,1,12,0).toEpochMillis(),120)
            assertTrue(repository.getAllTodos().any { it.title=="数学" && it.eventStartDate()!!>=java.time.LocalDate.of(2027,5,1) })
        } finally { db.close() }
    }
    @Test fun concurrentConfirmationIsIdempotentButNewSessionCanRepeatTitle() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try {
            val repo=TodoRepository(db);val candidates=TransientPlanningParser.parse("同名事项;",now).candidates
            val first=coroutineScope { val a=async { repo.importTransientPlanning(candidates,"one") };val b=async { repo.importTransientPlanning(candidates,"one") };a.await() to b.await() }
            assertEquals(first.first,first.second);assertEquals(1,repo.getAllTodos().size)
            repo.importTransientPlanning(candidates,"two");assertEquals(2,repo.getAllTodos().size)
        } finally { db.close() }
    }
    @Test fun accountChangeDuringImportRollsBackAllItemsAndGroups() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try {
            val repo=TodoRepository(db);val candidates=TransientPlanningParser.parse("- 父事项\n  - 子事项;\n- 日程,14:00>15:00;",now).candidates
            var checks=0
            try { repo.importTransientPlanning(candidates,"changed") { ++checks<3 };fail("account change must abort") } catch (_:IllegalStateException) { }
            assertTrue(repo.getAllTodos().isEmpty());assertTrue(repo.getAllQuickChecklistGroups().isEmpty())
            assertTrue(db.todoDao().getAllPlanningNotes().isEmpty())
        } finally { db.close() }
    }
    @Test fun migration33To34KeepsOldReminderAndAddsEmptyAbsoluteField() = runBlocking {
        val name="transient-migration-${UUID.randomUUID()}.db"
        val helper=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),AppDatabase::class.java)
        val item=TodoItem(id=51,title="旧待办",notes="原文保留",dueAtMillis=now.toEpochMillis(),reminderAtMillis=now.minusMinutes(5).toEpochMillis(),reminderOffsetsCsv="5,0",reminderEnabled=true,ringEnabled=true,vibrateEnabled=true)
        val snapshot=BackupSnapshot(0,emptyList(),emptyList(),listOf(item),settings=AppSettings())
        helper.createDatabase(name,33).use { database -> database.insert("todo_items",SQLiteDatabase.CONFLICT_ABORT,values(snapshot.toJson().getJSONArray("tasks").getJSONObject(0))) }
        helper.runMigrationsAndValidate(name,34,true,DatabaseMigrations.MIGRATION_33_34).close()
        val db=Room.databaseBuilder(context,AppDatabase::class.java,name).build()
        try { val recovered=db.todoDao().getById(51)!!;assertEquals(item.notes,recovered.notes);assertEquals("",recovered.reminderAbsoluteTimesCsv);assertEquals(item.reminderTriggerTimesMillis(),recovered.reminderTriggerTimesMillis()) }
        finally { db.close() }
    }
    @Test fun invalidCalendarRollsBackTheEntirePreview() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = TodoRepository(db)
            val candidates = TransientPlanningParser.parse("父事项; 子事项;", now).candidates
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repo.importTransientPlanning(listOf(candidates[0], candidates[1].copy(calendarId = "missing")), "invalid-calendar") }
            }
            assertTrue(repo.getAllTodos().isEmpty())
            assertTrue(repo.getAllPlanningNotes().isEmpty())
        } finally { db.close() }
    }
    @Test fun pureAbsoluteEventHasOneTriggerAndCreatesNoLinkedTodo() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = TodoRepository(db)
            val candidate = TransientPlanningParser.parse("讨论,14:00>15:00,(11-02+12:00);", now).candidates.single()
            val item = repo.importTransientPlanning(listOf(candidate), "absolute-event").single()
            assertTrue(item.isEvent)
            assertTrue(item.reminderEnabled)
            assertTrue(item.configuredReminderOffsetsMinutes.isEmpty())
            assertEquals(listOf(now.toLocalDate().plusDays(1).atTime(12, 0).toEpochMillis()), item.reminderTriggerTimesMillis())
            assertEquals(listOf(item.id), db.todoDao().getActiveReminderItems().map { it.id })
            val recovered = backupSnapshotFromJson(repo.exportSnapshot(AppSettings()).toJson()).tasks.single()
            assertEquals(item.reminderTriggerTimesMillis(), recovered.reminderTriggerTimesMillis())
            assertTrue(repo.getAllTodos().none { it.isTodo })
        } finally { db.close() }
    }
    @Test fun courseScopesKeepOtherSlotTimesAndPastReminderConfiguration() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = TodoRepository(db)
            val candidates = TransientPlanningParser.parse("数学,每周[周一 08:00>09:40,周三 14:00>15:40],自2026-10-26至2026-11-20;", now).candidates
            val imported = repo.importTransientPlanning(candidates, "course-scopes")
            val first = imported.first()
            val base = CalendarEventDraft(first.title, first.notes, first.location,
                first.calendarStartDateTime(), first.calendarEndDateTime(), false, first.accentColorHex ?: "#467DAB",
                reminderOffsetsMinutes = listOf(10, 0), ringEnabled = true, vibrateEnabled = true,
                groupId = first.groupId, recurrence = candidates.single().slots.first().recurrence)
            val exception = repo.updateCalendarEventFromDraft(first, base.copy(title = "一次改动", startAt = base.startAt.plusHours(1), endAt = base.endAt.plusHours(1)), RecurrenceScope.CURRENT).single()
            assertEquals(first.startAtMillis!! + 3_600_000, exception.startAtMillis)
            val otherSeries = imported.first { it.recurringSeriesId != first.recurringSeriesId }.recurringSeriesId
            val otherBefore = repo.getAllTodos().filter { it.recurringSeriesId == otherSeries }.associate { it.id to it.startAtMillis }
            val sameSeriesFuture = imported.first { it.recurringSeriesId == first.recurringSeriesId && it.id != first.id }
            repo.updateCalendarEventFromDraft(sameSeriesFuture, base.copy(title = "时段后续", startAt = sameSeriesFuture.calendarStartDateTime(), endAt = sameSeriesFuture.calendarEndDateTime()), RecurrenceScope.CURRENT_AND_FUTURE)
            assertEquals(otherBefore, repo.getAllTodos().filter { it.recurringSeriesId == otherSeries }.associate { it.id to it.startAtMillis })
            val beforeShared = repo.getActiveEventsForMultiSlotBundle(exception).associate { it.id to (it.startAtMillis to it.endAtMillis) }
            val shared = repo.updateCalendarEventMultiSlotBundleSharedFields(exception, base.copy(title = "整门课", location = "新教室"))
            assertEquals(beforeShared, shared.associate { it.id to (it.startAtMillis to it.endAtMillis) })
            assertTrue(shared.all { it.title == "整门课" && it.location == "新教室" && it.configuredReminderOffsetsMinutes == listOf(10, 0) })
            assertTrue(shared.any { (it.startAtMillis ?: Long.MAX_VALUE) < System.currentTimeMillis() })
        } finally { db.close() }
    }
    private fun values(json:JSONObject)=ContentValues().apply {
        json.keys().forEach { key -> if(key!="reminderAbsoluteTimesCsv") {
            when(val value=json.opt(key)) { JSONObject.NULL,null -> putNull(key);is Boolean->put(key,if(value)1 else 0);is Number->put(key,value.toLong());else->put(key,value.toString()) }
        } }
    }
}
