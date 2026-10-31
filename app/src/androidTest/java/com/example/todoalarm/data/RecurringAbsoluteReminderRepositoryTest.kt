package com.example.todoalarm.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

/** In-memory fixtures only; never opens the application's user database. */
class RecurringAbsoluteReminderRepositoryTest {
    private val start = LocalDateTime.of(2030, 1, 7, 10, 0)
    private val absolute = start.minusDays(1).toEpochMillis()
    private val otherAbsolute = start.plusDays(2).minusHours(1).toEpochMillis()
    private val recurrence = RecurrenceConfig(true, RecurrenceType.DAILY, endDate = start.toLocalDate().plusDays(3))

    private fun todo(at: LocalDateTime = start, recurring: Boolean = true) = TodoDraft(
        "事项", "", at, at.minusMinutes(5), 0, false, false,
        recurrence = if (recurring) recurrence else RecurrenceConfig(), reminderOffsetsMinutes = listOf(5))
    private fun event(at: LocalDateTime = start, recurring: Boolean = true) = CalendarEventDraft(
        "课程", "", "教室", at, at.plusHours(1), false, "#3370FF",
        reminderMinutesBefore = 5, ringEnabled = false, vibrateEnabled = false,
        recurrence = if (recurring) recurrence else RecurrenceConfig())
    private fun withRepository(block: suspend (AppDatabase, TodoRepository) -> Unit) = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try { val repo = TodoRepository(db); repo.ensureDefaultGroups(); block(db, repo) } finally { db.close() }
    }
    private suspend fun seed(db: AppDatabase, items: List<TodoItem>): List<TodoItem> {
        val seeded = items.sortedBy { it.dueAtMillis }.mapIndexed { index, item ->
            item.copy(reminderAbsoluteTimesCsv = when (index) { 0 -> "$absolute"; 2 -> "$otherAbsolute"; else -> "" })
        }
        db.todoDao().updateAll(seeded)
        return seeded
    }
    private fun assertOwned(items: List<TodoItem>, shiftDays: Long = 0) {
        val sorted = items.sortedBy { it.dueAtMillis }
        assertEquals(4, sorted.size)
        assertEquals(listOf("$absolute", "", "$otherAbsolute", ""), sorted.map { it.reminderAbsoluteTimesCsv })
        assertEquals(start.plusDays(shiftDays).toEpochMillis(), sorted.first().dueAtMillis)
        assertTrue(sorted.first().reminderTriggerTimesMillis().contains(absolute))
        assertTrue(sorted[2].reminderTriggerTimesMillis().contains(otherAbsolute))
        assertTrue(sorted.all { it.reminderEnabled })
    }

    @Test fun convertSingleTodoKeepsAbsoluteOnlyOnOriginalId() = withRepository { _, repo ->
        val single = repo.createFromDraft(todo(recurring = false).copy(absoluteReminderTimesMillis = listOf(absolute))).single()
        val updated = repo.updateFromDraft(single, todo().copy(absoluteReminderTimesMillis = listOf(absolute)))
        assertEquals(single.id, updated.first().id)
        assertEquals("$absolute", updated.first().reminderAbsoluteTimesCsv)
        assertTrue(updated.drop(1).all { it.reminderAbsoluteTimesCsv.isEmpty() && !it.reminderTriggerTimesMillis().contains(absolute) })
    }
    @Test fun convertSingleEventKeepsAbsoluteOnlyOnOriginalId() = withRepository { _, repo ->
        val single = repo.createCalendarEventFromDraft(event(recurring = false).copy(absoluteReminderTimesMillis = listOf(absolute))).single()
        val updated = repo.updateCalendarEventFromDraft(single, event().copy(absoluteReminderTimesMillis = listOf(absolute)))
        assertEquals(single.id, updated.first().id)
        assertEquals("$absolute", updated.first().reminderAbsoluteTimesCsv)
        assertTrue(updated.drop(1).all { it.reminderAbsoluteTimesCsv.isEmpty() && !it.reminderTriggerTimesMillis().contains(absolute) })
    }
    @Test fun todoAllContentEditPreservesSeparateOwners() = withRepository { db, repo ->
        val items = seed(db, repo.createFromDraft(todo()))
        val updated = repo.updateFromDraft(items.first(), todo().copy(title = "新事项", absoluteReminderTimesMillis = listOf(absolute)), RecurrenceScope.ALL)
        assertOwned(updated); assertTrue(updated.all { it.title == "新事项" })
        assertOwned(repo.getAllTodos())
    }
    @Test fun eventAllContentEditPreservesSeparateOwners() = withRepository { db, repo ->
        val items = seed(db, repo.createCalendarEventFromDraft(event()))
        val updated = repo.updateCalendarEventFromDraft(items.first(), event().copy(title = "新课程", absoluteReminderTimesMillis = listOf(absolute)), RecurrenceScope.ALL)
        assertOwned(updated); assertTrue(updated.all { it.title == "新课程" })
        assertOwned(repo.getAllTodos())
    }
    @Test fun todoFutureContentEditKeepsPastAndFutureOwners() = withRepository { db, repo ->
        val items = seed(db, repo.createFromDraft(todo()))
        repo.updateFromDraft(items[1], todo(start.plusDays(1)).copy(title = "后续事项"), RecurrenceScope.CURRENT_AND_FUTURE)
        assertOwned(repo.getAllTodos()); assertEquals("事项", repo.getTodo(items.first().id)!!.title)
    }
    @Test fun eventFutureContentEditKeepsPastAndFutureOwners() = withRepository { db, repo ->
        val items = seed(db, repo.createCalendarEventFromDraft(event()))
        repo.updateCalendarEventFromDraft(items[1], event(start.plusDays(1)).copy(title = "后续课程"), RecurrenceScope.CURRENT_AND_FUTURE)
        assertOwned(repo.getAllTodos()); assertEquals("课程", repo.getTodo(items.first().id)!!.title)
    }
    @Test fun todoAllShiftMapsOwnersOnceDespiteNewIds() = withRepository { db, repo ->
        val items = seed(db, repo.createFromDraft(todo()))
        val updated = repo.updateFromDraft(items.first(), todo(start.plusDays(1)).copy(
            recurrence = recurrence.copy(endDate = recurrence.endDate!!.plusDays(1)), absoluteReminderTimesMillis = listOf(absolute)), RecurrenceScope.ALL)
        assertOwned(updated, 1); assertTrue(updated.none { row -> items.any { it.id == row.id } })
    }
    @Test fun eventAllShiftMapsOwnersOnceDespiteNewIds() = withRepository { db, repo ->
        val items = seed(db, repo.createCalendarEventFromDraft(event()))
        val updated = repo.updateCalendarEventFromDraft(items.first(), event(start.plusDays(1)).copy(
            recurrence = recurrence.copy(endDate = recurrence.endDate!!.plusDays(1)), absoluteReminderTimesMillis = listOf(absolute)), RecurrenceScope.ALL)
        assertOwned(updated, 1); assertTrue(updated.none { row -> items.any { it.id == row.id } })
    }
    @Test fun editingUnremindedEventDoesNotDisableAnotherAbsoluteOwner() = withRepository { db, repo ->
        val off = event().copy(reminderMinutesBefore = null, reminderOffsetsMinutes = emptyList())
        val items = repo.createCalendarEventFromDraft(off).sortedBy { it.dueAtMillis }
        db.todoDao().update(items[2].copy(reminderAbsoluteTimesCsv = "$otherAbsolute", reminderAtMillis = otherAbsolute, reminderEnabled = true))
        repo.updateCalendarEventFromDraft(items.first(), off.copy(title = "改名"), RecurrenceScope.ALL)
        val owner = repo.getAllTodos().single { it.reminderAbsoluteTimesCsv == "$otherAbsolute" }
        assertTrue(owner.reminderEnabled); assertEquals(otherAbsolute, owner.reminderAtMillis)
        assertEquals(listOf(otherAbsolute), owner.reminderTriggerTimesMillis())
        assertTrue(repo.getAllTodos().filter { it.id != owner.id }.all { !it.reminderEnabled })
    }
    @Test fun explicitEventDisableClearsAllSelectedOwners() = withRepository { db, repo ->
        val items = seed(db, repo.createCalendarEventFromDraft(event()))
        val updated = repo.updateCalendarEventFromDraft(items.first(), event().copy(
            reminderMinutesBefore = null, reminderOffsetsMinutes = emptyList()), RecurrenceScope.ALL)
        assertTrue(updated.all { it.reminderAbsoluteTimesCsv.isEmpty() && !it.reminderEnabled && it.reminderAtMillis == null })
    }

    @Test fun editingUnremindedTodoKeepsAnotherAbsoluteOwner() = withRepository { db, repo ->
        val off = todo().copy(reminderAt = null, reminderOffsetsMinutes = emptyList())
        val items = repo.createFromDraft(off).sortedBy { it.dueAtMillis }
        db.todoDao().update(items[2].copy(reminderAbsoluteTimesCsv = "$otherAbsolute", reminderAtMillis = otherAbsolute, reminderEnabled = true))
        repo.updateFromDraft(items.first(), off.copy(title = "改名"), RecurrenceScope.ALL)
        val owner = repo.getAllTodos().single { it.reminderAbsoluteTimesCsv == "$otherAbsolute" }
        assertTrue(owner.reminderEnabled); assertEquals(listOf(otherAbsolute), owner.reminderTriggerTimesMillis())
        assertTrue(repo.getAllTodos().filter { it.id != owner.id }.all { !it.reminderEnabled && it.reminderAtMillis == null })
    }
    @Test fun explicitTodoFutureDisableKeepsPastOwner() = withRepository { db, repo ->
        val items = seed(db, repo.createFromDraft(todo()))
        val updated = repo.updateFromDraft(items[1], todo(start.plusDays(1)).copy(
            reminderAt = null, reminderOffsetsMinutes = emptyList()), RecurrenceScope.CURRENT_AND_FUTURE)
        assertTrue(updated.all { it.reminderAbsoluteTimesCsv.isEmpty() && !it.reminderEnabled && it.reminderAtMillis == null })
        assertEquals("$absolute", repo.getTodo(items.first().id)!!.reminderAbsoluteTimesCsv)
    }
    @Test fun eventFutureClockChangeKeepsOwnersAndRecalculatesRelativeTimes() = withRepository { db, repo ->
        val items = seed(db, repo.createCalendarEventFromDraft(event()))
        val updated = repo.updateCalendarEventFromDraft(items[1], event(start.plusDays(1).plusHours(2)), RecurrenceScope.CURRENT_AND_FUTURE)
        assertEquals(listOf("", "$otherAbsolute", ""), updated.map { it.reminderAbsoluteTimesCsv })
        assertTrue(updated.all { it.reminderTriggerTimesMillis().contains(it.startAtMillis!! - 5 * 60_000L) })
        assertEquals(start.plusDays(1).plusHours(2).toEpochMillis(), updated.first().startAtMillis)
        assertEquals("$absolute", repo.getTodo(items.first().id)!!.reminderAbsoluteTimesCsv)
    }
    @Test fun todoFutureAbsoluteChangeTouchesOnlySelectedInstance() = withRepository { db, repo ->
        val items = seed(db, repo.createFromDraft(todo()))
        val newAbsolute = start.plusDays(1).minusHours(2).toEpochMillis()
        val updated = repo.updateFromDraft(items[1], todo(start.plusDays(1)).copy(
            absoluteReminderTimesMillis = listOf(newAbsolute, newAbsolute)), RecurrenceScope.CURRENT_AND_FUTURE)
        assertEquals(listOf("$newAbsolute", "$otherAbsolute", ""), updated.map { it.reminderAbsoluteTimesCsv })
        assertEquals("$absolute", repo.getTodo(items.first().id)!!.reminderAbsoluteTimesCsv)
    }
    @Test fun todoAllContentEditPreservesOwnerBeyondInitialWindow() = withRepository { db, repo ->
        val infinite = todo().copy(recurrence = recurrence.copy(endDate = null))
        val items = repo.createFromDraft(infinite)
        repo.ensureRecurringInstancesAhead(start.toEpochMillis(), 80)
        val owner = repo.getAllTodos().maxBy { it.dueAtMillis }
        db.todoDao().update(owner.copy(reminderAbsoluteTimesCsv = "$otherAbsolute"))
        val beforeCount = repo.getAllTodos().size
        repo.updateFromDraft(items.first(), infinite.copy(title = "远期事项"), RecurrenceScope.ALL)
        assertEquals(beforeCount, repo.getAllTodos().size)
        val updatedOwner = repo.getAllTodos().single { it.reminderAbsoluteTimesCsv == "$otherAbsolute" }
        assertEquals(owner.dueAtMillis, updatedOwner.dueAtMillis)
        assertTrue(updatedOwner.reminderTriggerTimesMillis().contains(otherAbsolute))
    }
    @Test fun eventAllClockChangePreservesOwnerBeyondInitialWindow() = withRepository { db, repo ->
        val infinite = event().copy(recurrence = recurrence.copy(endDate = null))
        val items = repo.createCalendarEventFromDraft(infinite)
        repo.ensureRecurringInstancesAhead(start.toEpochMillis(), 80)
        val owner = repo.getAllTodos().maxBy { it.dueAtMillis }
        db.todoDao().update(owner.copy(reminderAbsoluteTimesCsv = "$otherAbsolute"))
        val beforeCount = repo.getAllTodos().size
        repo.updateCalendarEventFromDraft(items.first(), infinite.copy(startAt = start.plusHours(2), endAt = start.plusHours(3)), RecurrenceScope.ALL)
        assertEquals(beforeCount, repo.getAllTodos().size)
        val updatedOwner = repo.getAllTodos().single { it.reminderAbsoluteTimesCsv == "$otherAbsolute" }
        assertEquals(owner.startAtMillis!! + 2 * 60 * 60_000L, updatedOwner.startAtMillis)
        assertTrue(updatedOwner.reminderTriggerTimesMillis().contains(otherAbsolute))
    }
    @Test fun bundleContentEditAndReplenishmentDoNotCopyAbsoluteTimes() = withRepository { db, repo ->
        val course = event().copy(multiSlotBundleId = "same-course", recurrence = recurrence.copy(endDate = null))
        val firstSlot = repo.createCalendarEventFromDraft(course)
        val secondSlot = repo.createCalendarEventFromDraft(course.copy(startAt = start.plusHours(4), endAt = start.plusHours(5)))
        db.todoDao().update(firstSlot.first().copy(reminderAbsoluteTimesCsv = "$absolute"))
        db.todoDao().update(secondSlot[2].copy(reminderAbsoluteTimesCsv = "$otherAbsolute"))
        val original = repo.getTodo(firstSlot.first().id)!!
        repo.updateCalendarEventMultiSlotBundleSharedFields(original, course.copy(title = "整课改名", absoluteReminderTimesMillis = listOf(absolute)))
        repo.ensureRecurringInstancesAhead(start.toEpochMillis(), 80)
        val all = repo.getAllTodos()
        assertEquals(1, all.count { it.reminderAbsoluteTimesCsv == "$absolute" })
        assertEquals(1, all.count { it.reminderAbsoluteTimesCsv == "$otherAbsolute" })
        assertTrue(all.all { it.title == "整课改名" })
        val archive = backupSnapshotFromJson(repo.exportSnapshot(AppSettings()).toJson())
        assertEquals(2, archive.tasks.count { it.reminderAbsoluteTimesCsv.isNotEmpty() })
    }
    @Test fun invalidTodoSeriesEditRollsBackBeforeRemovingOwners() = withRepository { db, repo ->
        val items = seed(db, repo.createFromDraft(todo()))
        try {
            repo.updateFromDraft(items.first(), todo().copy(recurrence = recurrence.copy(interval = 0)), RecurrenceScope.ALL)
            fail("Invalid recurrence must fail")
        } catch (_: IllegalArgumentException) { }
        assertEquals(items.map { it.id }.toSet(), repo.getAllTodos().map { it.id }.toSet())
        assertOwned(repo.getAllTodos()); assertEquals(1, db.todoDao().getAllRecurringTemplates().size)
    }
    @Test fun removingOccurrenceDoesNotReassignItsAbsoluteReminder() = withRepository { db, repo ->
        val items = seed(db, repo.createCalendarEventFromDraft(event()))
        val updated = repo.updateCalendarEventFromDraft(items.first(), event().copy(
            absoluteReminderTimesMillis = listOf(absolute), recurrence = recurrence.copy(endDate = start.toLocalDate().plusDays(1))), RecurrenceScope.ALL)
        assertEquals(2, updated.size)
        assertEquals(listOf("$absolute", ""), updated.map { it.reminderAbsoluteTimesCsv })
        assertTrue(updated.none { it.reminderTriggerTimesMillis().contains(otherAbsolute) })
    }
    @Test fun directNewRecurringAbsoluteDraftRejectsWithoutCreatingItems() = withRepository { db, repo ->
        val groupsBefore = db.todoDao().getAllGroups()
        try { repo.createFromDraft(todo().copy(absoluteReminderTimesMillis = listOf(absolute))); fail("No owner selected") }
        catch (_: IllegalArgumentException) { }
        try { repo.createCalendarEventFromDraft(event().copy(absoluteReminderTimesMillis = listOf(absolute))); fail("No owner selected") }
        catch (_: IllegalArgumentException) { }
        assertTrue(repo.getAllTodos().isEmpty()); assertTrue(db.todoDao().getAllRecurringTemplates().isEmpty())
        assertEquals(groupsBefore, db.todoDao().getAllGroups())
    }
}
