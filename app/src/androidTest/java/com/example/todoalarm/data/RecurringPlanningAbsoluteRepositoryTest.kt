package com.example.todoalarm.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

/** Planning fixtures use isolated, in-memory Room databases only. */
class RecurringPlanningAbsoluteRepositoryTest {
    private val now = LocalDateTime.of(2030, 1, 6, 12, 0)
    private val weeklyStarts = listOf(
        "2030-01-07T08:00", "2030-01-09T14:00", "2030-01-14T08:00",
        "2030-01-16T14:00", "2030-01-21T08:00", "2030-01-23T14:00"
    )

    private fun withRepository(block: suspend (AppDatabase, TodoRepository) -> Unit) = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = TodoRepository(db)
            repository.ensureDefaultGroups()
            block(db, repository)
        } finally {
            db.close()
        }
    }

    private fun candidates(text: String): List<TransientPlanningCandidate> {
        val parsed = TransientPlanningParser.parse(text, now)
        assertFalse(parsed.diagnostics.toString(), parsed.hasErrors)
        assertTrue(parsed.candidates.isNotEmpty())
        return parsed.candidates
    }

    private fun millis(text: String) = LocalDateTime.parse(text).toEpochMillis()
    private fun starts(items: List<TodoItem>) = items.map { it.calendarStartDateTime().toString() }.sorted()

    private fun assertAbsoluteOwners(items: List<TodoItem>, expected: Map<String, List<String>>) {
        val actual = items.filter { it.configuredAbsoluteReminderTimesMillis.isNotEmpty() }
            .associate { item ->
                item.calendarStartDateTime().toString() to item.configuredAbsoluteReminderTimesMillis
            }
        assertEquals(expected.mapValues { (_, times) -> times.map(::millis).distinct().sorted() }, actual)
        val expectedTimes = expected.values.flatten().map(::millis).distinct()
        assertEquals(expectedTimes.size, items.sumOf { it.configuredAbsoluteReminderTimesMillis.size })
        expectedTimes.forEach { absolute ->
            assertEquals("Absolute trigger $absolute must have one owner", 1,
                items.count { absolute in it.configuredAbsoluteReminderTimesMillis })
        }
    }

    private fun assertRelativeTriggers(items: List<TodoItem>, offsets: List<Int>) {
        items.forEach { item ->
            assertEquals(offsets, item.configuredReminderOffsetsMinutes)
            val relative = offsets.map { item.startAtMillis!! - it * 60_000L }
            val expected = (relative + item.configuredAbsoluteReminderTimesMillis).distinct().sorted()
            assertEquals("${item.title} at ${item.calendarStartDateTime()}", expected, item.reminderTriggerTimesMillis())
            assertEquals(expected.minOrNull(), item.reminderAtMillis)
            assertEquals(expected.isNotEmpty(), item.reminderEnabled)
        }
    }

    @Test fun weeklyCourseAssignsAcrossAllSlotsOnceAndKeepsEveryRelativeTrigger() = withRepository { db, repo ->
        val preview = candidates("数学,每周[周一 08:00>09:40,周三 14:00>15:40],(5,0,2030-01-06 07:00,2030-01-07 08:00,2030-01-07 08:01,2030-01-16 15:00),自2030-01-07至2030-01-23;")
        val items = repo.importTransientPlanning(preview, "weekly-mixed")
        assertEquals(weeklyStarts, starts(items))
        assertEquals(1, items.map { it.multiSlotBundleId }.distinct().size)
        assertNotNull(items.first().multiSlotBundleId)
        assertEquals(2, items.map { it.recurringSeriesId }.distinct().size)
        assertAbsoluteOwners(items, mapOf(
            "2030-01-07T08:00" to listOf("2030-01-06T07:00", "2030-01-07T08:00"),
            "2030-01-09T14:00" to listOf("2030-01-07T08:01"),
            "2030-01-21T08:00" to listOf("2030-01-16T15:00")
        ))
        assertRelativeTriggers(items, listOf(5, 0))
        assertEquals(items.map { it.id }.toSet(), db.todoDao().getActiveReminderItems().map { it.id }.toSet())
        assertTrue(repo.getAllTodos().all { it.isEvent })
        assertTrue(db.todoDao().getAllPlanningNotes().isEmpty())
    }

    @Test fun pureAbsoluteCourseDisablesNonOwnersAndSchedulesEachTimestampOnce() = withRepository { db, repo ->
        val items = repo.importTransientPlanning(candidates(
            "数学,每周[周一 08:00>09:40,周三 14:00>15:40],(2030-01-07 08:01,2030-01-07 08:01),自2030-01-07至2030-01-23;"
        ), "weekly-pure")
        assertEquals(weeklyStarts, starts(items))
        assertAbsoluteOwners(items, mapOf("2030-01-09T14:00" to listOf("2030-01-07T08:01")))
        assertRelativeTriggers(items, emptyList())
        val owner = items.single { it.reminderEnabled }
        assertEquals(listOf(millis("2030-01-07T08:01")), owner.reminderTriggerTimesMillis())
        assertTrue(items.filter { it.id != owner.id }.all { !it.reminderEnabled && it.reminderAtMillis == null })
        assertEquals(listOf(owner.id), db.todoDao().getActiveReminderItems().map { it.id })
        assertEquals(listOf(owner.id), repo.dueReminderItems(millis("2030-01-07T08:01"), 0).map { it.id })
    }

    @Test fun equalStartsChooseFirstInputSlotIncludingReversedDurationOrder() = withRepository { _, repo ->
        suspend fun check(source: String, session: String, expectedEnd: String) {
            val items = repo.importTransientPlanning(candidates(source), session)
            assertEquals(4, items.size)
            val owner = items.single { it.configuredAbsoluteReminderTimesMillis.isNotEmpty() }
            assertEquals("2030-01-07T08:00", owner.calendarStartDateTime().toString())
            assertEquals(expectedEnd, owner.calendarEndDateTime().toString())
            assertEquals(listOf(millis("2030-01-07T08:00")), owner.reminderTriggerTimesMillis())
            assertTrue(items.filter { it.id != owner.id }.all { !it.reminderEnabled && it.reminderAtMillis == null })
        }
        check("并列甲,每周[周一 08:00>10:00,周一 08:00>09:00],(2030-01-07 08:00),自2030-01-07至2030-01-14;",
            "tie-long-first", "2030-01-07T10:00")
        check("并列乙,每周[周一 08:00>09:00,周一 08:00>10:00],(2030-01-07 08:00),自2030-01-07至2030-01-14;",
            "tie-short-first", "2030-01-07T09:00")
    }

    @Test fun farFutureOwnerIsMaterializedAndReplenishmentFillsAllIntermediateWeeks() = withRepository { _, repo ->
        val items = repo.importTransientPlanning(candidates(
            "远期数学,每周[周一 08:00>09:40,周三 14:00>15:40],(5,0,2030-04-30 12:00),自2030-01-07至2031-01-07;"
        ), "far-finite")
        assertAbsoluteOwners(items, mapOf("2030-05-01T14:00" to listOf("2030-04-30T12:00")))
        val ownerId = items.single { it.configuredAbsoluteReminderTimesMillis.isNotEmpty() }.id
        repo.ensureRecurringInstancesAhead(millis("2030-01-07T00:00"), 122)
        val all = repo.getAllTodos()
        val expected = listOf(
            "2030-01-07T08:00", "2030-01-09T14:00", "2030-01-14T08:00", "2030-01-16T14:00",
            "2030-01-21T08:00", "2030-01-23T14:00", "2030-01-28T08:00", "2030-01-30T14:00",
            "2030-02-04T08:00", "2030-02-06T14:00", "2030-02-11T08:00", "2030-02-13T14:00",
            "2030-02-18T08:00", "2030-02-20T14:00", "2030-02-25T08:00", "2030-02-27T14:00",
            "2030-03-04T08:00", "2030-03-06T14:00", "2030-03-11T08:00", "2030-03-13T14:00",
            "2030-03-18T08:00", "2030-03-20T14:00", "2030-03-25T08:00", "2030-03-27T14:00",
            "2030-04-01T08:00", "2030-04-03T14:00", "2030-04-08T08:00", "2030-04-10T14:00",
            "2030-04-15T08:00", "2030-04-17T14:00", "2030-04-22T08:00", "2030-04-24T14:00",
            "2030-04-29T08:00", "2030-05-01T14:00", "2030-05-06T08:00", "2030-05-08T14:00"
        )
        assertEquals(expected, starts(all.filter { it.startAtMillis!! <= millis("2030-05-08T23:59") }))
        assertEquals(all.size, all.map { it.recurringSeriesId to it.startAtMillis }.distinct().size)
        assertEquals(ownerId, all.single { it.configuredAbsoluteReminderTimesMillis.isNotEmpty() }.id)
        assertAbsoluteOwners(all, mapOf("2030-05-01T14:00" to listOf("2030-04-30T12:00")))
        assertRelativeTriggers(all, listOf(5, 0))
        val beforeRepeat = all.associateBy { it.id }
        assertTrue(repo.ensureRecurringInstancesAhead(millis("2030-01-07T00:00"), 122).isEmpty())
        assertEquals(beforeRepeat, repo.getAllTodos().associateBy { it.id })
    }

    @Test fun indefinitePureAbsoluteOwnerSurvivesLaterWindowWithoutCopying() = withRepository { _, repo ->
        val items = repo.importTransientPlanning(candidates(
            "无限课程,每周[周一 08:00>09:40,周三 14:00>15:40],(2030-01-31 12:00);"
        ), "far-infinite")
        assertAbsoluteOwners(items, mapOf("2030-02-04T08:00" to listOf("2030-01-31T12:00")))
        assertTrue(items.all { it.recurrenceEndEpochDay == null })
        repo.ensureRecurringInstancesAhead(millis("2030-01-07T00:00"), 38)
        val all = repo.getAllTodos()
        assertEquals(listOf(
            "2030-01-07T08:00", "2030-01-09T14:00", "2030-01-14T08:00", "2030-01-16T14:00",
            "2030-01-21T08:00", "2030-01-23T14:00", "2030-01-28T08:00", "2030-01-30T14:00",
            "2030-02-04T08:00", "2030-02-06T14:00", "2030-02-11T08:00", "2030-02-13T14:00"
        ), starts(all))
        assertAbsoluteOwners(all, mapOf("2030-02-04T08:00" to listOf("2030-01-31T12:00")))
        assertRelativeTriggers(all, emptyList())
    }

    @Test fun finiteCourseWithoutNextOccurrenceRejectsWholeHierarchyWithLineLocation() = withRepository { db, repo ->
        val preview = candidates("- 父事项\n  - 子事项;\n- 数学,每周[周一 08:00>09:40,周三 14:00>15:40],自2030-01-07至2030-01-23;")
        val invalid = preview.map { if (it.title == "数学") it.copy(
            reminderMode = PlanningReminderMode.EXPLICIT, reminderOffsetsMinutes = emptyList(),
            absoluteReminderTimes = listOf(LocalDateTime.parse("2030-01-23T14:01"))) else it }
        val groupsBefore = db.todoDao().getAllGroups()
        try {
            repo.importTransientPlanning(invalid, "finite-invalid")
            fail("No occurrence at or after the absolute time must reject the preview")
        } catch (error: IllegalStateException) {
            assertTrue(error.message.orEmpty(), error.message.orEmpty().contains("第3行"))
            assertTrue(error.message.orEmpty(), error.message.orEmpty().contains("绝对提醒"))
        }
        assertTrue(repo.getAllTodos().isEmpty())
        assertTrue(db.todoDao().getAllRecurringTemplates().isEmpty())
        assertTrue(repo.getAllQuickChecklistGroups().isEmpty())
        assertTrue(db.todoDao().getAllPlanningNotes().isEmpty())
        assertEquals(groupsBefore, db.todoDao().getAllGroups())
        assertTrue(repo.importTransientPlanning(preview, "finite-invalid").isNotEmpty())
    }

    @Test fun everyTwoWeeksKeepsPhaseWhenAbsoluteFallsInInactiveWeek() = withRepository { _, repo ->
        val items = repo.importTransientPlanning(candidates(
            "隔周课程,每2周[周一 08:00>09:40,周三 14:00>15:40],(0,2030-01-10 12:00,2030-01-21 08:00,2030-01-23 14:01),自2030-01-07至2030-02-06;"
        ), "biweekly")
        assertEquals(listOf("2030-01-07T08:00", "2030-01-09T14:00", "2030-01-21T08:00",
            "2030-01-23T14:00", "2030-02-04T08:00", "2030-02-06T14:00"), starts(items))
        assertAbsoluteOwners(items, mapOf(
            "2030-01-21T08:00" to listOf("2030-01-10T12:00", "2030-01-21T08:00"),
            "2030-02-04T08:00" to listOf("2030-01-23T14:01")
        ))
        assertRelativeTriggers(items, listOf(0))
    }

    @Test fun month31ClampsFebruaryThenReturnsTo31ForAbsoluteOwnership() = withRepository { _, repo ->
        val items = repo.importTransientPlanning(candidates(
            "月底课程,每月[31日 08:00>09:00],(2030-02-28 08:00,2030-02-28 08:01),自2030-01-01至2030-04-30;"
        ), "month31")
        assertEquals(listOf("2030-01-31T08:00", "2030-02-28T08:00", "2030-03-31T08:00", "2030-04-30T08:00"), starts(items))
        assertAbsoluteOwners(items, mapOf(
            "2030-02-28T08:00" to listOf("2030-02-28T08:00"),
            "2030-03-31T08:00" to listOf("2030-02-28T08:01")
        ))
        assertRelativeTriggers(items, emptyList())
    }

    @Test fun leapDayClampsNonLeapYearsAndAssignsExactLeapDayToThatOccurrence() = withRepository { _, repo ->
        val items = repo.importTransientPlanning(candidates(
            "闰日课程,每年[02-29 08:00>09:00],(2029-02-28 08:00,2029-03-01 12:00,2032-02-29 08:00),自2028-02-01至2032-03-01;"
        ), "leap-day")
        assertEquals(listOf("2028-02-29T08:00", "2029-02-28T08:00", "2030-02-28T08:00",
            "2031-02-28T08:00", "2032-02-29T08:00"), starts(items))
        assertAbsoluteOwners(items, mapOf(
            "2029-02-28T08:00" to listOf("2029-02-28T08:00"),
            "2030-02-28T08:00" to listOf("2029-03-01T12:00"),
            "2032-02-29T08:00" to listOf("2032-02-29T08:00")
        ))
        assertRelativeTriggers(items, emptyList())
    }

    @Test fun repeatedConcurrentConfirmationDoesNotDuplicateAbsoluteOwnersOrTemplates() = withRepository { db, repo ->
        val preview = candidates("数学,每周[周一 08:00>09:40,周三 14:00>15:40],(2030-01-07 08:01),自2030-01-07至2030-01-23;")
        val both = coroutineScope {
            val first = async { repo.importTransientPlanning(preview, "same-preview") }
            val second = async { repo.importTransientPlanning(preview, "same-preview") }
            first.await() to second.await()
        }
        assertEquals(both.first, both.second)
        assertEquals(both.first, repo.importTransientPlanning(preview, "same-preview"))
        assertEquals(6, repo.getAllTodos().size)
        assertEquals(2, db.todoDao().getAllRecurringTemplates().size)
        assertAbsoluteOwners(repo.getAllTodos(), mapOf("2030-01-09T14:00" to listOf("2030-01-07T08:01")))
    }

    @Test fun accountSwitchAfterCourseWritesRollsBackAndAllowsRetryOfSameSession() = withRepository { db, repo ->
        val preview = candidates("- 父事项\n  - 子事项;\n- 数学,每周[周一 08:00>09:40,周三 14:00>15:40],(2030-01-07 08:01),自2030-01-07至2030-01-23;")
        val groupsBefore = db.todoDao().getAllGroups()
        var checks = 0
        try {
            repo.importTransientPlanning(preview, "account-switch") { ++checks < 5 }
            fail("Changing account before commit must abort")
        } catch (_: IllegalStateException) { }
        assertTrue(repo.getAllTodos().isEmpty())
        assertTrue(db.todoDao().getAllRecurringTemplates().isEmpty())
        assertTrue(repo.getAllQuickChecklistGroups().isEmpty())
        assertTrue(db.todoDao().getAllPlanningNotes().isEmpty())
        assertEquals(groupsBefore, db.todoDao().getAllGroups())
        val retried = repo.importTransientPlanning(preview, "account-switch")
        assertEquals(8, retried.size)
        assertAbsoluteOwners(retried.filter { it.isEvent }, mapOf("2030-01-09T14:00" to listOf("2030-01-07T08:01")))
        assertEquals(retried, repo.importTransientPlanning(preview, "account-switch"))
    }

    @Test fun archiveRestoreAndSeriesContentEditsPreserveOwnersWithoutSpreading() = withRepository { _, repo ->
        val preview = candidates("数学,每周[周一 08:00>09:40,周三 14:00>15:40],(2030-01-07 08:01,2030-01-09 14:01),自2030-01-07至2030-02-06;")
        repo.importTransientPlanning(preview, "archive-edit")
        val snapshot = BackupArchiveCodec.decode(BackupArchiveCodec.encode(repo.exportSnapshot(AppSettings())))
        val restoredDb = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java
        ).build()
        try {
            val restoredRepo = TodoRepository(restoredDb)
            restoredRepo.importSnapshot(snapshot)
            val expectedOwners = mapOf(
                "2030-01-09T14:00" to listOf("2030-01-07T08:01"),
                "2030-01-14T08:00" to listOf("2030-01-09T14:01")
            )
            val before = restoredRepo.getAllTodos()
            assertAbsoluteOwners(before, expectedOwners)
            assertEquals(repo.getAllTodos().associate { it.id to it.reminderTriggerTimesMillis() },
                before.associate { it.id to it.reminderTriggerTimesMillis() })
            val unreminded = before.single { it.calendarStartDateTime().toString() == "2030-01-07T08:00" }
            val draft = CalendarEventDraft("系列改名", "修改备注", "新教室", unreminded.calendarStartDateTime(),
                unreminded.calendarEndDateTime(), false, "#467DAB", ringEnabled = true, vibrateEnabled = true,
                recurrence = preview.single().slots.first().recurrence, groupId = unreminded.groupId,
                multiSlotBundleId = unreminded.multiSlotBundleId)
            restoredRepo.updateCalendarEventFromDraft(unreminded, draft, RecurrenceScope.ALL)
            assertAbsoluteOwners(restoredRepo.getAllTodos(), expectedOwners)
            val renamed = restoredRepo.getTodo(unreminded.id)!!
            restoredRepo.updateCalendarEventMultiSlotBundleSharedFields(renamed, draft.copy(title = "整课改名"))
            restoredRepo.ensureRecurringInstancesAhead(millis("2030-01-07T00:00"), 31)
            val after = restoredRepo.getAllTodos()
            assertAbsoluteOwners(after, expectedOwners)
            assertRelativeTriggers(after, emptyList())
            assertTrue(after.all { it.title == "整课改名" && it.notes == "修改备注" && it.location == "新教室" })
            assertEquals(before.associate { it.id to (it.startAtMillis to it.endAtMillis) },
                after.associate { it.id to (it.startAtMillis to it.endAtMillis) })
        } finally {
            restoredDb.close()
        }
    }
}
