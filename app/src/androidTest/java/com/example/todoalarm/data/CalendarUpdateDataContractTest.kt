package com.example.todoalarm.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/** Synthetic databases only. These tests never use TodoApplication.database or network accounts. */
@RunWith(AndroidJUnit4::class)
class CalendarUpdateDataContractTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun migration32To33PreservesLegacyRemindersFiniteSeriesAndSkippedDates() = runBlocking {
        val name = "calendar-contract-32-33-${UUID.randomUUID()}.db"
        val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)
        val date = LocalDate.of(2030, 1, 31)
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item = event(id = 51, start = start, calendarId = "legacy").copy(allDay = true,
            title = "旧日程", notes = "原始备注", reminderOffsetsCsv = "1440,15", reminderEnabled = true,
            reminderAtMillis = date.atTime(9, 0).toEpochMillis() - 1440 * 60_000L,
            recurringSeriesId = "legacy-series", recurrenceType = "MONTHLY_DAY", recurrenceMonthlyDay = 31,
            recurrenceAnchorDueAtMillis = start, recurrenceEndEpochDay = date.plusMonths(3).toEpochDay())
        val master = template("legacy-series", "legacy", date).copy(id = 61, allDay = true,
            title = "旧日程", notes = "原始备注", recurrenceType = "MONTHLY_DAY", recurrenceMonthlyDay = 31,
            reminderOffsetsCsv = "1440,15", endEpochDay = date.plusMonths(3).toEpochDay())
        val snapshot = BackupSnapshot(0, listOf(TaskGroup(41, "旧分组", "#3370FF", 0, true)),
            listOf(master), listOf(item), calendars = listOf(PlannerCalendar("legacy", "旧日历", "#3370FF")), settings = AppSettings())
        val json = snapshot.toJson()
        val added33 = setOf("eventTimeZoneId", "allDayReminderRulesJson", "eventBusyStatus", "eventVisibility",
            "eventParticipantsJson", "recurrenceInterval", "recurrenceInvalidDatePolicy", "recurrenceNeverEnds", "sharedEventId")
        val originalTriggers = item.reminderTriggerTimesMillis()
        helper.createDatabase(name, 32).use { db ->
            db.insert("task_groups", SQLiteDatabase.CONFLICT_ABORT, values(json.getJSONArray("groups").getJSONObject(0)))
            db.insert("todo_items", SQLiteDatabase.CONFLICT_ABORT, values(json.getJSONArray("tasks").getJSONObject(0), added33))
            db.insert("recurring_task_templates", SQLiteDatabase.CONFLICT_ABORT, values(json.getJSONArray("templates").getJSONObject(0), added33))
            db.insert("planner_calendars", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("id", "legacy"); put("name", "旧日历"); put("colorHex", "#3370FF"); put("sortOrder", 0)
            })
            db.insert("recurring_instance_skips", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("id", 77L); put("seriesId", "legacy-series"); put("instanceEpochDay", LocalDate.of(2030, 2, 28).toEpochDay()); put("createdAtMillis", 1L)
            })
        }
        helper.runMigrationsAndValidate(name, 33, true, DatabaseMigrations.MIGRATION_32_33).close()
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(DatabaseMigrations.MIGRATION_32_33).build()
        try {
            val repo = TodoRepository(db)
            val restored = repo.getTodo(51)!!
            assertEquals("原始备注", restored.notes)
            assertEquals(originalTriggers, restored.reminderTriggerTimesMillis())
            assertEquals("旧日历", db.todoDao().getCalendars().single().name)
            repo.ensureRecurringInstancesAhead(date.atTime(12, 0).toEpochMillis(), lookAheadDays = 200)
            val dates = repo.getAllTodos().filter { it.recurringSeriesId == "legacy-series" }.map { it.eventStartDate().toString() }.sorted()
            assertEquals(listOf("2030-01-31", "2030-03-31", "2030-04-30"), dates)
            assertEquals(51L, repo.getTodo(51)!!.id)
            assertEquals(listOf(LocalDate.of(2030, 2, 28).toEpochDay()), db.todoDao().getRecurringSkipEpochDays("legacy-series"))
        } finally { db.close() }
    }

    @Test fun zonedAllDayIntervalAndClockRulesSurviveArchiveAndRoomRestore() = withRepository { db, repo, group ->
        repo.saveCalendar(PlannerCalendar("month-end", "每两月", "#3370FF"))
        val date = LocalDate.of(2030, 1, 31)
        val draft = draft(date.atStartOfDay(), group, "month-end").copy(allDay = true, endAt = date.atStartOfDay(),
            eventTimeZoneId = "America/New_York",
            allDayReminderRulesJson = "[{\"daysBefore\":1,\"minuteOfDay\":480},{\"daysBefore\":0,\"minuteOfDay\":540}]",
            recurrence = RecurrenceConfig(true, RecurrenceType.MONTHLY_DAY, endDate = LocalDate.of(2030, 7, 31), interval = 2, invalidDatePolicy = "SKIP"))
        repo.createCalendarEventFromDraft(draft)
        val archive = BackupArchiveCodec.encode(repo.exportSnapshot(AppSettings()))
        repo.importSnapshot(BackupArchiveCodec.decode(archive))
        repo.ensureRecurringInstancesAhead(date.atTime(12, 0).toEpochMillis(), 200)
        val events = repo.getAllTodos().sortedBy { it.startAtMillis }
        assertEquals(listOf("2030-01-31", "2030-03-31", "2030-05-31", "2030-07-31"), events.map { it.eventStartDate().toString() })
        assertEquals(listOf("2030-01-30T13:00:00Z", "2030-01-31T14:00:00Z"), events.first().reminderTriggerTimesMillis().map { Instant.ofEpochMilli(it).toString() })
        assertEquals(listOf("2030-03-30T12:00:00Z", "2030-03-31T13:00:00Z"), events[1].reminderTriggerTimesMillis().map { Instant.ofEpochMilli(it).toString() })
        assertTrue(events.all { it.calendarEndDateTime().toLocalDate() == it.eventStartDate()!!.plusDays(1) })
        assertEquals(2, db.todoDao().getAllRecurringTemplates().single().recurrenceInterval)
    }

    @Test fun infiniteSeriesReplenishesPastInitialLimitWithoutLosingIntervalPhase() = withRepository { db, repo, group ->
        val start = LocalDate.of(2030, 10, 24).atTime(10, 0)
        val created = repo.createCalendarEventFromDraft(draft(start, group).copy(eventTimeZoneId = "Asia/Shanghai",
            recurrence = RecurrenceConfig(true, RecurrenceType.DAILY, interval = 3)))
        assertTrue(created.size < 60)
        repo.ensureRecurringInstancesAhead(start.toLocalDate().atTime(12, 0).toEpochMillis(), 180)
        val dates = repo.getAllTodos().mapNotNull { it.eventStartDate() }.sorted()
        assertEquals(60, dates.size)
        assertEquals(start.toLocalDate().plusDays(177), dates.last())
        assertTrue(dates.zipWithNext().all { (a, b) -> b == a.plusDays(3) })
        assertTrue(db.todoDao().getAllRecurringTemplates().single().recurrenceNeverEnds)
    }

    @Test fun lunarWinterRecurrenceIncludesAnchorAndKeepsTwoYearPhase() = withRepository { _, repo, group ->
        val anchor = generateSequence(LocalDate.of(2030, 11, 1)) { it.plusDays(1) }.take(61).first { date ->
            LunarCalendar.labelFor(date).let { it.month == 11 && it.day == 1 && !it.isLeapMonth }
        }
        repo.createCalendarEventFromDraft(draft(anchor.atTime(10, 0), group).copy(
            recurrence = RecurrenceConfig(true, RecurrenceType.YEARLY_LUNAR_DATE, endDate = LocalDate.of(2034, 12, 31), interval = 2)))
        val dates = repo.getAllTodos().mapNotNull { it.eventStartDate() }.sorted()
        assertEquals(listOf(2030, 2032, 2034), dates.map { it.year })
        assertEquals(anchor, dates.first())
        assertTrue(dates.all { date -> LunarCalendar.labelFor(date).let { it.month == 11 && it.day == 1 && !it.isLeapMonth } })
    }

    @Test fun lunarLookupKeepsLateWinterDatesInTheirLunarYear() {
        val lunarNewYear = LocalDate.of(2026, 2, 17)
        val eve = lunarNewYear.minusDays(1)
        val label = LunarCalendar.labelFor(eve)
        assertEquals(12, label.month)
        assertEquals(eve, LunarCalendar.findDate(2025, label.month, label.day))
        assertEquals(lunarNewYear, LunarCalendar.findDate(2026, 1, 1))
        assertEquals(eve, LunarCalendar.sameLunarDateInYear(eve, 2026))
        assertEquals(listOf(eve), calendarRecurrenceDates(eve,
            RecurrenceConfig(true, RecurrenceType.YEARLY_LUNAR_DATE), rangeEnd = eve))
    }

    @Test fun contentEditOfEntireSeriesPreservesMovedExceptionAndSharedIdentity() = withRepository { db, repo, group ->
        val calendarId = "shared:owned-fixture"
        db.todoDao().insertCalendars(listOf(PlannerCalendar(id = calendarId, name = "共享", shareMode = "SHARED", sharedRole = "OWNER")))
        val base = draft(LocalDate.of(2030, 10, 24).atTime(10, 0), group, calendarId).copy(eventTimeZoneId = "America/New_York",
            recurrence = RecurrenceConfig(true, RecurrenceType.DAILY, endDate = LocalDate.of(2030, 12, 3), interval = 2))
        val created = repo.createCalendarEventFromDraft(base).sortedBy { it.startAtMillis }
        val exception = created[3]
        val moved = repo.updateCalendarEventFromDraft(exception, base.copy(startAt = exception.calendarStartDateTime().plusDays(1).plusHours(2),
            endAt = exception.calendarEndDateTime().plusDays(1).plusHours(2)), RecurrenceScope.CURRENT).single()
        assertEquals(exception.id, moved.id)
        assertEquals(exception.sharedEventId, moved.sharedEventId)
        assertEquals(exception.recurrenceAnchorDueAtMillis, moved.recurrenceAnchorDueAtMillis)
        val before = repo.getAllTodos().associateBy { it.id }
        repo.updateCalendarEventFromDraft(created.first(), base.copy(title = "统一新标题", notes = "新的详情"), RecurrenceScope.ALL)
        val after = repo.getAllTodos().associateBy { it.id }
        assertEquals(before.keys, after.keys)
        before.forEach { (id, item) ->
            assertEquals(item.startAtMillis, after[id]!!.startAtMillis)
            assertEquals(item.endAtMillis, after[id]!!.endAtMillis)
            assertEquals(item.sharedEventId, after[id]!!.sharedEventId)
            assertEquals("统一新标题", after[id]!!.title)
        }
        repo.ensureRecurringInstancesAhead(base.startAt.toLocalDate().atTime(12, 0).toEpochMillis(), 90)
        assertTrue(repo.getAllTodos().all { it.title == "统一新标题" })
        assertEquals(moved.startAtMillis, repo.getTodo(moved.id)!!.startAtMillis)
    }

    @Test fun futureContentBranchKeepsSkippedDatesWhenWindowExpands() = withRepository { db, repo, group ->
        val base = draft(LocalDate.of(2030, 10, 24).atTime(10, 0), group).copy(recurrence = RecurrenceConfig(true, RecurrenceType.DAILY))
        val created = repo.createCalendarEventFromDraft(base).sortedBy { it.startAtMillis }
        val seriesId = created.first().recurringSeriesId!!
        repo.deleteCalendarEvent(created[1], RecurrenceScope.CURRENT)
        val skippedFuture = base.startAt.toLocalDate().plusDays(20).toEpochDay()
        db.todoDao().insertRecurringSkip(RecurringInstanceSkip(seriesId = seriesId, instanceEpochDay = skippedFuture))
        val pivot = created[3]
        val updated = repo.updateCalendarEventFromDraft(pivot, base.copy(startAt = pivot.calendarStartDateTime(),
            endAt = pivot.calendarEndDateTime(), title = "分支新标题"), RecurrenceScope.CURRENT_AND_FUTURE)
        val branch = updated.first().recurringSeriesId!!
        assertNotEquals(seriesId, branch)
        assertTrue(db.todoDao().getRecurringSkipEpochDays(branch).contains(skippedFuture))
        assertTrue(db.todoDao().getRecurringSkipEpochDays(seriesId).contains(base.startAt.toLocalDate().plusDays(1).toEpochDay()))
        repo.ensureRecurringInstancesAhead(base.startAt.toLocalDate().atTime(12, 0).toEpochMillis(), 40)
        assertFalse(repo.getAllTodos().any { it.isActive && it.eventStartDate()?.toEpochDay() == skippedFuture })
        assertEquals(38, repo.getAllTodos().size)
    }

    @Test fun invalidDstInstanceDuringSeriesReplacementLeavesOriginalRowsUntouched() = withRepository { _, repo, group ->
        val base = draft(LocalDate.of(2030, 3, 8).atTime(10, 0), group).copy(eventTimeZoneId = "America/New_York",
            recurrence = RecurrenceConfig(true, RecurrenceType.DAILY, endDate = LocalDate.of(2030, 3, 12)))
        val created = repo.createCalendarEventFromDraft(base).sortedBy { it.startAtMillis }
        val before = repo.exportSnapshot(AppSettings())
        rejected {
            repo.updateCalendarEventFromDraft(created.first(), base.copy(startAt = base.startAt.withHour(2).withMinute(30),
                endAt = base.endAt.withHour(3).withMinute(30)), RecurrenceScope.ALL)
        }
        val after = repo.exportSnapshot(AppSettings())
        assertEquals(before.tasks.sortedBy { it.id }, after.tasks.sortedBy { it.id })
        assertEquals(before.templates, after.templates)
        assertEquals(before.recurringInstanceSkips, after.recurringInstanceSkips)
    }

    @Test fun viewerCannotMutateSharedEventsOrGenerateRecurringInstances() = withRepository { db, repo, group ->
        val calendarId = "shared:viewer-fixture"
        db.todoDao().insertCalendars(listOf(PlannerCalendar(id = calendarId, name = "只读", shareMode = "SHARED", sharedRole = "VIEWER")))
        val start = LocalDate.of(2030, 10, 24).atTime(10, 0)
        val stored = event(id = 700, start = start.toEpochMillis(), calendarId = calendarId).copy(sharedEventId = "viewer-event",
            recurringSeriesId = "viewer-series", recurrenceType = "DAILY", recurrenceAnchorDueAtMillis = start.toEpochMillis())
        db.todoDao().insert(stored)
        db.todoDao().insertTemplate(template("viewer-series", calendarId, start.toLocalDate()).copy(groupId = group, recurrenceNeverEnds = true))
        rejected { repo.createCalendarEventFromDraft(draft(start.plusDays(1), group, calendarId)) }
        rejected { repo.updateCalendarEventFromDraft(stored, draft(start, group, calendarId).copy(title = "非法改名")) }
        rejected { repo.updateTodo(stored.copy(title = "绕过编辑器")) }
        rejected { repo.cancelCalendarEvent(stored) }
        rejected { repo.deleteCalendarEvent(stored) }
        rejected { repo.saveCalendar(PlannerCalendar(id = calendarId, name = "绕过协作管理")) }
        assertTrue(repo.ensureRecurringInstancesAhead(start.toEpochMillis(), 90).isEmpty())
        assertEquals(listOf(stored), repo.getAllTodos())
    }

    @Test fun personalArchiveExcludesSharedRowsAndCloudRestorePreservesCacheAcrossIdCollision() = withRepository { _, repo, _ ->
        val date = LocalDate.of(2030, 10, 24)
        val sharedId = "shared:cache-fixture"
        val shared = event(id = 7, start = date.atTime(10, 0).toEpochMillis(), calendarId = sharedId).copy(title = "SHARED_SECRET",
            sharedEventId = "stable-cache-event", recurringSeriesId = "cache-series", recurrenceType = "DAILY")
        val sharedMaster = template("cache-series", sharedId, date).copy(id = 3, title = "SHARED_SECRET", eventTimeZoneId = "Asia/Shanghai")
        val local = BackupSnapshot(0, listOf(TaskGroup(41, "本机默认", "#3370FF", 0, true)), listOf(sharedMaster), listOf(shared),
            calendars = listOf(PlannerCalendar(), PlannerCalendar(id = sharedId, name = "SHARED_SECRET", shareMode = "SHARED")),
            eventCheckIns = listOf(EventCheckIn(8, shared.id, 1000, 2000, 10)),
            todoGroupTags = listOf(TodoGroupTag(shared.id, 41)),
            recurringInstanceSkips = listOf(RecurringInstanceSkip(9, "cache-series", date.plusDays(1).toEpochDay(), 1)),
            reminderChainLogs = listOf(ReminderChainLog(4, shared.id, "shared-chain", "test", "test", "OK", "SHARED_SECRET", createdAtMillis = 1)), settings = AppSettings())
        val personal = BackupArchiveCodec.decode(BackupArchiveCodec.encode(local.personalSyncSnapshot()))
        assertFalse(personal.toJson().toString().contains("SHARED_SECRET"))
        assertTrue(personal.eventCheckIns.isEmpty()); assertTrue(personal.todoGroupTags.isEmpty()); assertTrue(personal.recurringInstanceSkips.isEmpty())
        val remote = BackupSnapshot(10, listOf(TaskGroup(11, "远端默认", "#3370FF", 0, true)), emptyList(),
            listOf(event(id = 7, start = date.atTime(11, 0).toEpochMillis()).copy(title = "PERSONAL", groupId = 11)), settings = AppSettings())
        val merged = BackupArchiveCodec.decode(BackupArchiveCodec.encode(remote.personalSyncSnapshot())).withSharedCacheFrom(local)
        repo.importSnapshot(merged)
        val restored = repo.exportSnapshot(AppSettings())
        val personalEvent = restored.tasks.single { it.title == "PERSONAL" }
        val sharedEvent = restored.tasks.single { it.title == "SHARED_SECRET" }
        assertEquals(7L, personalEvent.id)
        assertNotEquals(personalEvent.id, sharedEvent.id)
        assertEquals("stable-cache-event", sharedEvent.sharedEventId)
        assertEquals(sharedEvent.id, restored.eventCheckIns.single().eventId)
        assertEquals("cache-series", restored.recurringInstanceSkips.single().seriesId)
        assertEquals("Asia/Shanghai", restored.templates.single().eventTimeZoneId)
        assertFalse(restored.personalSyncSnapshot().toJson().toString().contains("SHARED_SECRET"))
    }

    @Test fun personalSnapshotDetachesSharedLinksAndRejectsSeriesCollision() {
        val date = LocalDate.of(2030, 10, 24)
        val shared = event(7, date.atTime(10, 0).toEpochMillis(), "shared:fixture").copy(recurringSeriesId = "same-series")
        val local = BackupSnapshot(0, emptyList(), listOf(template("same-series", shared.calendarId, date)), listOf(shared),
            planningNodes = listOf(PlanningNode(id = 1, noteId = 1, sortOrder = 0, text = "本机记录", linkedTodoId = 7, linkedEndTodoId = 7)),
            planningLineMappings = listOf(PlanningLineMapping(id = 1, noteId = 1, contentFingerprint = "fixture",
                originalLineText = "本机记录", eventId = 7, batchId = "fixture", createdAtMillis = 0, lastRefreshedAtMillis = 0)), settings = AppSettings())
        val personal = local.personalSyncSnapshot()
        assertNull(personal.planningNodes.single().linkedTodoId)
        assertNull(personal.planningNodes.single().linkedEndTodoId)
        assertNull(personal.planningLineMappings.single().eventId)
        assertEquals(MappingStatus.ORPHANED, personal.planningLineMappings.single().status)
        val remote = local.copy(tasks = listOf(shared.copy(calendarId = "default")), templates = emptyList())
        try {
            remote.withSharedCacheFrom(local)
            fail("Series collision must preserve both sources without merging them")
        } catch (_: IllegalArgumentException) { }
        assertEquals(shared, local.tasks.single())
    }

    @Test fun sharedRecurrenceKeepsOccurrenceResponsesAndNewInstancesHaveIndependentIdentity() = withRepository { db, repo, group ->
        val calendarId = "shared:participants-fixture"
        db.todoDao().insertCalendars(listOf(PlannerCalendar(id = calendarId, name = "共享", shareMode = "SHARED", sharedRole = "OWNER")))
        val start = LocalDate.of(2030, 10, 24).atTime(10, 0)
        val base = draft(start, group, calendarId).copy(eventParticipantsJson = "[{\"username\":\"alice\",\"response\":\"ACCEPTED\"}]",
            recurrence = RecurrenceConfig(true, RecurrenceType.DAILY))
        val created = repo.createCalendarEventFromDraft(base).sortedBy { it.startAtMillis }
        assertEquals(created.size, created.map { it.sharedEventId }.toSet().size)
        assertTrue(created.all { JSONObject(org.json.JSONArray(it.eventParticipantsJson).getJSONObject(0).toString()).getString("response") == "NEEDS_ACTION" })
        val responded = created.first().copy(eventParticipantsJson = "[{\"username\":\"alice\",\"response\":\"ACCEPTED\"}]")
        db.todoDao().update(responded)
        repo.updateCalendarEventFromDraft(responded, base.copy(notes = "只修改详情",
            eventParticipantsJson = "[{\"username\":\"alice\",\"response\":\"DECLINED\"}]"), RecurrenceScope.ALL)
        assertEquals("ACCEPTED", org.json.JSONArray(repo.getTodo(responded.id)!!.eventParticipantsJson).getJSONObject(0).getString("response"))
        val stale = repo.getTodo(created[1].id)!!
        db.todoDao().update(stale.copy(eventParticipantsJson = "[{\"username\":\"alice\",\"response\":\"TENTATIVE\"}]"))
        repo.updateCalendarEventFromDraft(stale, base.copy(startAt = stale.calendarStartDateTime(),
            endAt = stale.calendarEndDateTime(), notes = "过期草稿仍保留当前回应"), RecurrenceScope.CURRENT)
        assertEquals("TENTATIVE", org.json.JSONArray(repo.getTodo(stale.id)!!.eventParticipantsJson).getJSONObject(0).getString("response"))
        repo.ensureRecurringInstancesAhead(start.toLocalDate().atTime(12, 0).toEpochMillis(), 45)
        val all = repo.getAllTodos()
        assertEquals(all.size, all.map { it.sharedEventId }.toSet().size)
        assertTrue(all.filter { it.id != responded.id && it.id != stale.id }.all {
            org.json.JSONArray(it.eventParticipantsJson).getJSONObject(0).getString("response") == "NEEDS_ACTION"
        })
    }

    private fun withRepository(block: suspend (AppDatabase, TodoRepository, Long) -> Unit) = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = TodoRepository(db)
            val group = repo.ensureDefaultGroups().first().id
            block(db, repo, group)
        } finally { db.close() }
    }

    private fun draft(start: LocalDateTime, group: Long, calendarId: String = "default") = CalendarEventDraft(
        title = "原日程", notes = "原详情", location = "原地点", startAt = start, endAt = start.plusMinutes(45),
        allDay = false, accentColorHex = "#3370FF", ringEnabled = false, vibrateEnabled = false,
        calendarId = calendarId, groupId = group)

    private fun event(id: Long, start: Long, calendarId: String = "default") = TodoItem(id = id, itemType = "EVENT", title = "原日程",
        dueAtMillis = start, startAtMillis = start, endAtMillis = start + 45 * 60_000L,
        reminderAtMillis = null, reminderEnabled = false, ringEnabled = false, vibrateEnabled = false, calendarId = calendarId, groupId = 41, createdAtMillis = 1)

    private fun template(seriesId: String, calendarId: String, date: LocalDate) = RecurringTaskTemplate(
        seriesId = seriesId, itemType = "EVENT", title = "原日程", groupId = 41, dueHour = 0, dueMinute = 0,
        eventDurationMinutes = 45, ringEnabled = false, vibrateEnabled = false, recurrenceType = "DAILY", calendarId = calendarId,
        startEpochDay = date.toEpochDay(), endEpochDay = date.plusDays(30).toEpochDay(), createdAtMillis = 1)

    private suspend fun rejected(block: suspend () -> Unit) {
        try { block(); fail("操作应拒绝并保留原数据") } catch (_: IllegalArgumentException) { }
    }

    private fun values(json: JSONObject, excluded: Set<String> = emptySet()) = ContentValues().apply {
        json.keys().forEach { key -> if (key !in excluded) when (val value = json.get(key)) {
            JSONObject.NULL -> putNull(key)
            is Boolean -> put(key, if (value) 1 else 0)
            is Number -> put(key, value.toLong())
            else -> put(key, value.toString())
        } }
    }
}
