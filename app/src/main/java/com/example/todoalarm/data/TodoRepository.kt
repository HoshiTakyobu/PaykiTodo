package com.example.todoalarm.data

import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class TodoRepository(
    private val todoDao: TodoDao
) {
    fun observeTodos(): Flow<List<TodoItem>> = todoDao.observeTodos()
    fun observeGroups(): Flow<List<TaskGroup>> = todoDao.observeGroups()

    suspend fun addTodo(item: TodoItem): TodoItem {
        val id = todoDao.insert(item)
        return item.copy(id = id)
    }

    suspend fun getTodo(id: Long): TodoItem? = todoDao.getById(id)
    suspend fun getGroup(groupId: Long): TaskGroup? = todoDao.getGroupById(groupId)
    suspend fun getAllTodos(): List<TodoItem> = todoDao.getAllTodos()

    suspend fun getActiveItemsForScope(
        item: TodoItem,
        scope: RecurrenceScope
    ): List<TodoItem> {
        if (!item.isRecurring || scope == RecurrenceScope.CURRENT) {
            return listOfNotNull(todoDao.getById(item.id)).filter { it.isActive }
        }
        val seriesId = item.recurringSeriesId ?: return emptyList()
        return todoDao.getActiveBySeriesId(seriesId).filter { candidate ->
            when (scope) {
                RecurrenceScope.CURRENT -> candidate.id == item.id
                RecurrenceScope.CURRENT_AND_FUTURE -> candidate.dueAtMillis >= item.dueAtMillis
                RecurrenceScope.ALL -> true
            }
        }
    }

    suspend fun updateTodo(item: TodoItem): TodoItem {
        todoDao.update(item)
        return item
    }

    suspend fun deleteTodo(id: Long) {
        todoDao.deleteById(id)
    }

    suspend fun acknowledgeCalendarEvent(id: Long): TodoItem? {
        val item = todoDao.getById(id) ?: return null
        if (!item.isEvent) return item
        val updated = item.copy(reminderEnabled = false)
        todoDao.update(updated)
        return updated
    }

    suspend fun createFromDraft(draft: TodoDraft): List<TodoItem> {
        val now = System.currentTimeMillis()
        val groupId = draft.groupId.takeIf { it > 0 } ?: defaultGroupId()
        val generated = if (draft.recurrence.isRecurring) {
            generateRecurringItems(
                draft = draft.copy(groupId = groupId),
                seriesId = UUID.randomUUID().toString(),
                now = now
            )
        } else {
            listOf(buildTaskItem(draft.copy(groupId = groupId), now = now))
        }
        val ids = todoDao.insertAll(generated)
        val created = generated.zip(ids) { item, id -> item.copy(id = id) }
        if (draft.recurrence.isRecurring) {
            val template = buildTemplate(created.first(), draft.copy(groupId = groupId))
            todoDao.insertTemplate(template)
        }
        return created
    }

    suspend fun createCalendarEventFromDraft(draft: CalendarEventDraft): List<TodoItem> {
        val now = System.currentTimeMillis()
        val generated = if (draft.recurrence.isRecurring) {
            generateRecurringEventItems(
                draft = draft,
                seriesId = UUID.randomUUID().toString(),
                now = now
            )
        } else {
            listOf(buildCalendarEventItem(draft, now = now))
        }
        val ids = todoDao.insertAll(generated)
        val created = generated.zip(ids) { item, id -> item.copy(id = id) }
        if (draft.recurrence.isRecurring) {
            todoDao.insertTemplate(buildCalendarTemplate(created.first(), draft))
        }
        return created
    }

    suspend fun updateFromDraft(
        original: TodoItem,
        draft: TodoDraft,
        scope: RecurrenceScope = RecurrenceScope.CURRENT
    ): List<TodoItem> {
        val resolvedDraft = draft.copy(groupId = draft.groupId.takeIf { it > 0 } ?: original.groupId)
        return when {
            !original.isRecurring && resolvedDraft.recurrence.isRecurring -> {
                convertSingleTodoToRecurring(original, resolvedDraft)
            }
            original.isRecurring && scope != RecurrenceScope.CURRENT -> {
                updateRecurringSeries(original, resolvedDraft, scope)
            }
            else -> {
                val updated = buildTaskItem(
                    draft = resolvedDraft,
                    now = original.createdAtMillis,
                    existing = original,
                    keepSeries = original.isRecurring
                )
                todoDao.update(updated)
                listOf(updated)
            }
        }
    }

    suspend fun cancelTodo(
        item: TodoItem,
        scope: RecurrenceScope = RecurrenceScope.CURRENT
    ): List<TodoItem> {
        val now = System.currentTimeMillis()
        val activeSeries = if (item.isRecurring) {
            val seriesId = item.recurringSeriesId ?: return emptyList()
            todoDao.getActiveBySeriesId(seriesId)
        } else {
            emptyList()
        }
        val targets = if (item.isRecurring && scope != RecurrenceScope.CURRENT) {
            activeSeries.filter { candidate ->
                when (scope) {
                    RecurrenceScope.CURRENT -> candidate.id == item.id
                    RecurrenceScope.CURRENT_AND_FUTURE -> candidate.dueAtMillis >= item.dueAtMillis
                    RecurrenceScope.ALL -> true
                }
            }
        } else {
            listOfNotNull(todoDao.getById(item.id)).filter { it.isActive }
        }

        if (targets.isEmpty()) return emptyList()
        val canceledItems = targets.map { target ->
            target.copy(
                canceled = true,
                canceledAtMillis = now,
                completed = false,
                completedAtMillis = null,
                missed = false,
                missedAtMillis = null,
                reminderEnabled = false
            )
        }
        todoDao.updateAll(canceledItems)
        if (item.isRecurring) {
            item.recurringSeriesId?.let { seriesId ->
                when (scope) {
                    RecurrenceScope.CURRENT -> Unit
                    RecurrenceScope.CURRENT_AND_FUTURE -> {
                        truncateTemplateBefore(
                            seriesId = seriesId,
                            template = todoDao.getTemplateBySeriesId(seriesId),
                            activeSeries = activeSeries,
                            splitDate = item.dueDate()
                        )
                    }
                    RecurrenceScope.ALL -> {
                        todoDao.deleteTemplateBySeriesId(seriesId)
                    }
                }
            }
        }
        return canceledItems
    }

    suspend fun setCompleted(id: Long, completed: Boolean): TodoItem? {
        val item = todoDao.getById(id) ?: return null
        val now = System.currentTimeMillis()
        val updated = item.copy(
            completed = completed,
            completedAtMillis = if (completed) now else null,
            canceled = false,
            canceledAtMillis = null,
            missed = if (completed) false else item.missed,
            missedAtMillis = if (completed) null else item.missedAtMillis,
            reminderEnabled = if (completed) {
                false
            } else {
                item.reminderAtMillis?.let { it > now } == true
            }
        )
        todoDao.update(updated)
        return updated
    }

    suspend fun snoozeTodo(id: Long, nextReminderMillis: Long): TodoItem? {
        val item = todoDao.getById(id) ?: return null
        if (item.isHistory) return null
        val updated = item.copy(
            reminderAtMillis = nextReminderMillis,
            reminderEnabled = true
        )
        todoDao.update(updated)
        return updated
    }

    suspend fun markMissedTasks(now: Long = System.currentTimeMillis()): Int {
        val candidates = todoDao.getPendingMissCandidates(now - MISSED_GRACE_PERIOD_MILLIS)
        if (candidates.isEmpty()) return 0
        val updated = candidates.map { item ->
            item.copy(
                missed = true,
                missedAtMillis = now,
                reminderEnabled = false
            )
        }
        todoDao.updateAll(updated)
        return updated.size
    }

    suspend fun futureReminderItems(now: Long): List<TodoItem> {
        return todoDao.getFutureReminderItems(now)
    }

    suspend fun ensureDefaultGroups(): List<TaskGroup> {
        val existing = todoDao.getAllGroups()
        if (existing.isNotEmpty()) return existing
        val ids = todoDao.insertGroups(DefaultTaskGroups.seed)
        return DefaultTaskGroups.seed.zip(ids) { item, id -> item.copy(id = id) }
    }

    suspend fun createGroup(name: String, colorHex: String): TaskGroup {
        val current = todoDao.getAllGroups()
        val group = TaskGroup(
            name = name.trim(),
            colorHex = colorHex,
            sortOrder = (current.maxOfOrNull { it.sortOrder } ?: -1) + 1,
            isDefault = false
        )
        val id = todoDao.insertGroup(group)
        return group.copy(id = id)
    }

    suspend fun updateGroup(group: TaskGroup) {
        todoDao.updateGroup(group)
    }

    suspend fun deleteGroup(groupId: Long): Boolean {
        if (todoDao.countTasksInGroup(groupId) > 0) return false
        todoDao.deleteGroup(groupId)
        return true
    }

    suspend fun updateCalendarEventFromDraft(
        original: TodoItem,
        draft: CalendarEventDraft,
        scope: RecurrenceScope = RecurrenceScope.CURRENT
    ): List<TodoItem> {
        return when {
            !original.isRecurring && draft.recurrence.isRecurring -> {
                convertSingleEventToRecurring(original, draft)
            }
            original.isRecurring && scope != RecurrenceScope.CURRENT -> {
                updateRecurringCalendarSeries(original, draft, scope)
            }
            else -> {
                val updated = buildCalendarEventItem(
                    draft = draft,
                    now = original.createdAtMillis,
                    existing = original,
                    keepSeries = original.isRecurring
                )
                todoDao.update(updated)
                listOf(updated)
            }
        }
    }

    suspend fun deleteCalendarEvent(
        item: TodoItem,
        scope: RecurrenceScope = RecurrenceScope.CURRENT
    ): List<TodoItem> {
        if (!item.isEvent) return emptyList()
        val seriesId = item.recurringSeriesId
        return when {
            !item.isRecurring || scope == RecurrenceScope.CURRENT -> {
                val target = todoDao.getById(item.id) ?: return emptyList()
                todoDao.deleteById(item.id)
                listOf(target)
            }
            seriesId == null -> emptyList()
            else -> {
                val seriesItems = todoDao.getBySeriesId(seriesId).filter { it.isEvent && !it.canceled }
                val targets = seriesItems.filter { candidate ->
                    when (scope) {
                        RecurrenceScope.CURRENT -> candidate.id == item.id
                        RecurrenceScope.CURRENT_AND_FUTURE -> candidate.dueAtMillis >= item.dueAtMillis
                        RecurrenceScope.ALL -> true
                    }
                }
                if (targets.isEmpty()) return emptyList()
                todoDao.deleteByIds(targets.map { it.id })
                when (scope) {
                    RecurrenceScope.CURRENT -> Unit
                    RecurrenceScope.CURRENT_AND_FUTURE -> truncateTemplateBefore(
                        seriesId = seriesId,
                        template = todoDao.getTemplateBySeriesId(seriesId),
                        activeSeries = seriesItems,
                        splitDate = item.dueDate()
                    )
                    RecurrenceScope.ALL -> todoDao.deleteTemplateBySeriesId(seriesId)
                }
                targets
            }
        }
    }

    suspend fun getAllGroups(): List<TaskGroup> = todoDao.getAllGroups()

    suspend fun exportSnapshot(settings: AppSettings): BackupSnapshot {
        return BackupSnapshot(
            exportedAtMillis = System.currentTimeMillis(),
            groups = todoDao.getAllGroups(),
            templates = todoDao.getAllRecurringTemplates(),
            tasks = todoDao.getAllTodos(),
            settings = settings
        )
    }

    suspend fun importSnapshot(snapshot: BackupSnapshot) {
        todoDao.clearTodos()
        todoDao.clearTemplates()
        todoDao.clearGroups()
        if (snapshot.groups.isNotEmpty()) {
            todoDao.insertGroups(snapshot.groups)
        } else {
            todoDao.insertGroups(DefaultTaskGroups.seed)
        }
        if (snapshot.templates.isNotEmpty()) {
            todoDao.insertTemplates(snapshot.templates)
        }
        if (snapshot.tasks.isNotEmpty()) {
            todoDao.insertAll(snapshot.tasks)
        }
    }

    private suspend fun updateRecurringSeries(
        original: TodoItem,
        draft: TodoDraft,
        scope: RecurrenceScope
    ): List<TodoItem> {
        val seriesId = original.recurringSeriesId ?: UUID.randomUUID().toString()
        val template = todoDao.getTemplateBySeriesId(seriesId)
        val activeSeries = todoDao.getActiveBySeriesId(seriesId)
        val targets = activeSeries.filter { candidate ->
            when (scope) {
                RecurrenceScope.CURRENT -> candidate.id == original.id
                RecurrenceScope.CURRENT_AND_FUTURE -> candidate.dueAtMillis >= original.dueAtMillis
                RecurrenceScope.ALL -> true
            }
        }
        if (targets.isEmpty()) return emptyList()

        return when (scope) {
            RecurrenceScope.CURRENT -> emptyList()
            RecurrenceScope.ALL -> replaceRecurringTargets(
                original = original,
                draft = alignRecurringDraftForAll(draft, activeSeries, original),
                targets = targets,
                targetSeriesId = seriesId,
                deleteTemplateSeriesId = if (draft.recurrence.isRecurring) null else seriesId
            ).also { updated ->
                if (draft.recurrence.isRecurring && updated.isNotEmpty()) {
                    todoDao.insertTemplate(buildTemplate(updated.first(), alignRecurringDraftForAll(draft, activeSeries, original)))
                } else if (draft.recurrence.isRecurring && updated.isEmpty()) {
                    todoDao.deleteTemplateBySeriesId(seriesId)
                } else {
                    todoDao.deleteTemplateBySeriesId(seriesId)
                }
            }

            RecurrenceScope.CURRENT_AND_FUTURE -> {
                todoDao.deleteByIds(targets.map { it.id })
                truncateTemplateBefore(seriesId, template, activeSeries, original.dueDate())

                if (draft.recurrence.isRecurring) {
                    val branchSeriesId = UUID.randomUUID().toString()
                    val replacement = generateRecurringItems(
                        draft = draft.copy(groupId = draft.groupId.takeIf { it > 0 } ?: original.groupId),
                        seriesId = branchSeriesId,
                        now = System.currentTimeMillis()
                    )
                    if (replacement.isEmpty()) {
                        return emptyList()
                    }
                    val ids = todoDao.insertAll(replacement)
                    val updated = replacement.zip(ids) { item, id -> item.copy(id = id) }
                    todoDao.insertTemplate(buildTemplate(updated.first(), draft.copy(groupId = draft.groupId.takeIf { it > 0 } ?: original.groupId)))
                    updated
                } else {
                    val singleReplacement = listOf(
                        buildTaskItem(
                            draft = draft.copy(groupId = draft.groupId.takeIf { it > 0 } ?: original.groupId),
                            now = original.createdAtMillis,
                            existing = original.copy(id = 0),
                            keepSeries = false
                        )
                    )
                    val ids = todoDao.insertAll(singleReplacement)
                    singleReplacement.zip(ids) { item, id -> item.copy(id = id) }
                }
            }
        }
    }

    private suspend fun updateRecurringCalendarSeries(
        original: TodoItem,
        draft: CalendarEventDraft,
        scope: RecurrenceScope
    ): List<TodoItem> {
        val seriesId = original.recurringSeriesId ?: UUID.randomUUID().toString()
        val template = todoDao.getTemplateBySeriesId(seriesId)
        val seriesItems = todoDao.getBySeriesId(seriesId).filter { it.isEvent && !it.canceled }
        val targets = seriesItems.filter { candidate ->
            when (scope) {
                RecurrenceScope.CURRENT -> candidate.id == original.id
                RecurrenceScope.CURRENT_AND_FUTURE -> candidate.dueAtMillis >= original.dueAtMillis
                RecurrenceScope.ALL -> true
            }
        }
        if (targets.isEmpty()) return emptyList()

        return when (scope) {
            RecurrenceScope.CURRENT -> emptyList()
            RecurrenceScope.ALL -> replaceRecurringCalendarTargets(
                original = original,
                draft = alignRecurringCalendarDraftForAll(draft, seriesItems, original),
                targets = targets,
                targetSeriesId = seriesId,
                deleteTemplateSeriesId = seriesId
            ).also { updated ->
                if (updated.isEmpty()) {
                    todoDao.deleteTemplateBySeriesId(seriesId)
                } else {
                    todoDao.insertTemplate(
                        buildCalendarTemplate(
                            updated.first(),
                            alignRecurringCalendarDraftForAll(draft, seriesItems, original)
                        )
                    )
                }
            }

            RecurrenceScope.CURRENT_AND_FUTURE -> {
                todoDao.deleteByIds(targets.map { it.id })
                truncateTemplateBefore(seriesId, template, seriesItems, original.dueDate())
                if (draft.recurrence.isRecurring) {
                    val branchSeriesId = UUID.randomUUID().toString()
                    val replacement = generateRecurringEventItems(
                        draft = draft,
                        seriesId = branchSeriesId,
                        now = System.currentTimeMillis()
                    )
                    if (replacement.isEmpty()) {
                        return emptyList()
                    }
                    val ids = todoDao.insertAll(replacement)
                    val updated = replacement.zip(ids) { item, id -> item.copy(id = id) }
                    todoDao.insertTemplate(buildCalendarTemplate(updated.first(), draft))
                    updated
                } else {
                    val singleReplacement = listOf(
                        buildCalendarEventItem(
                            draft = draft,
                            now = original.createdAtMillis,
                            existing = original.copy(id = 0),
                            keepSeries = false
                        )
                    )
                    val ids = todoDao.insertAll(singleReplacement)
                    singleReplacement.zip(ids) { item, id -> item.copy(id = id) }
                }
            }
        }
    }

    private suspend fun convertSingleTodoToRecurring(
        original: TodoItem,
        draft: TodoDraft
    ): List<TodoItem> {
        val seriesId = UUID.randomUUID().toString()
        val generated = generateRecurringItems(
            draft = draft,
            seriesId = seriesId,
            now = original.createdAtMillis
        )
        if (generated.isEmpty()) return emptyList()

        val first = generated.first().copy(id = original.id, createdAtMillis = original.createdAtMillis)
        todoDao.update(first)
        val rest = generated.drop(1)
        val createdRest = if (rest.isEmpty()) {
            emptyList()
        } else {
            val ids = todoDao.insertAll(rest)
            rest.zip(ids) { item, id -> item.copy(id = id) }
        }
        todoDao.insertTemplate(buildTemplate(first, draft))
        return listOf(first) + createdRest
    }

    private suspend fun convertSingleEventToRecurring(
        original: TodoItem,
        draft: CalendarEventDraft
    ): List<TodoItem> {
        val seriesId = UUID.randomUUID().toString()
        val generated = generateRecurringEventItems(
            draft = draft,
            seriesId = seriesId,
            now = original.createdAtMillis
        )
        if (generated.isEmpty()) return emptyList()

        val first = generated.first().copy(id = original.id, createdAtMillis = original.createdAtMillis)
        todoDao.update(first)
        val rest = generated.drop(1)
        val createdRest = if (rest.isEmpty()) {
            emptyList()
        } else {
            val ids = todoDao.insertAll(rest)
            rest.zip(ids) { item, id -> item.copy(id = id) }
        }
        todoDao.insertTemplate(buildCalendarTemplate(first, draft))
        return listOf(first) + createdRest
    }

    private fun alignRecurringDraftForAll(
        draft: TodoDraft,
        activeSeries: List<TodoItem>,
        original: TodoItem
    ): TodoDraft {
        val dueAt = requireNotNull(draft.dueAt) { "Recurring todo requires DDL" }
        val baseDate = activeSeries.minByOrNull { it.dueAtMillis }?.dueDate()
            ?: original.dueDate()
        val alignedDueAt = LocalDateTime.of(baseDate, dueAt.toLocalTime())
        return draft.withAlignedDateTime(alignedDueAt)
    }

    private fun alignRecurringCalendarDraftForAll(
        draft: CalendarEventDraft,
        activeSeries: List<TodoItem>,
        original: TodoItem
    ): CalendarEventDraft {
        val baseDate = activeSeries.minByOrNull { it.dueAtMillis }?.dueDate() ?: original.dueDate()
        val newStart = LocalDateTime.of(baseDate, draft.startAt.toLocalTime())
        val durationMinutes = Duration.between(draft.startAt, draft.endAt).toMinutes().coerceAtLeast(30)
        return draft.copy(
            startAt = newStart,
            endAt = newStart.plusMinutes(durationMinutes)
        )
    }

    private suspend fun truncateTemplateBefore(
        seriesId: String,
        template: RecurringTaskTemplate?,
        activeSeries: List<TodoItem>,
        splitDate: LocalDate
    ) {
        val remainingActive = activeSeries.filter { it.dueDate().isBefore(splitDate) }
        if (remainingActive.isEmpty()) {
            todoDao.deleteTemplateBySeriesId(seriesId)
            return
        }

        val existingTemplate = template ?: return
        val newEndEpochDay = splitDate.minusDays(1).toEpochDay()
        if (existingTemplate.startEpochDay > newEndEpochDay) {
            todoDao.deleteTemplateBySeriesId(seriesId)
            return
        }
        todoDao.updateTemplate(existingTemplate.copy(endEpochDay = newEndEpochDay))
    }

    private suspend fun replaceRecurringTargets(
        original: TodoItem,
        draft: TodoDraft,
        targets: List<TodoItem>,
        targetSeriesId: String,
        deleteTemplateSeriesId: String?
    ): List<TodoItem> {
        todoDao.deleteByIds(targets.map { it.id })

        val replacement = if (draft.recurrence.isRecurring) {
            generateRecurringItems(
                draft = draft,
                seriesId = targetSeriesId,
                now = System.currentTimeMillis()
            )
        } else {
            listOf(
                buildTaskItem(
                    draft = draft.copy(groupId = draft.groupId.takeIf { it > 0 } ?: original.groupId),
                    now = original.createdAtMillis,
                    existing = original.copy(id = 0),
                    keepSeries = false
                )
            )
        }

        if (replacement.isEmpty()) {
            deleteTemplateSeriesId?.let { todoDao.deleteTemplateBySeriesId(it) }
            return emptyList()
        }

        val ids = todoDao.insertAll(replacement)
        return replacement.zip(ids) { item, id -> item.copy(id = id) }
    }

    private suspend fun replaceRecurringCalendarTargets(
        original: TodoItem,
        draft: CalendarEventDraft,
        targets: List<TodoItem>,
        targetSeriesId: String,
        deleteTemplateSeriesId: String?
    ): List<TodoItem> {
        todoDao.deleteByIds(targets.map { it.id })
        val replacement = if (draft.recurrence.isRecurring) {
            generateRecurringEventItems(
                draft = draft,
                seriesId = targetSeriesId,
                now = System.currentTimeMillis()
            )
        } else {
            listOf(
                buildCalendarEventItem(
                    draft = draft,
                    now = original.createdAtMillis,
                    existing = original.copy(id = 0),
                    keepSeries = false
                )
            )
        }
        if (replacement.isEmpty()) {
            deleteTemplateSeriesId?.let { todoDao.deleteTemplateBySeriesId(it) }
            return emptyList()
        }
        val ids = todoDao.insertAll(replacement)
        return replacement.zip(ids) { item, id -> item.copy(id = id) }
    }

    private fun TodoDraft.withAlignedDateTime(newDueAt: LocalDateTime): TodoDraft {
        val currentDueAt = requireNotNull(dueAt) { "Recurring todo requires DDL" }
        val offsetMinutes = reminderAt?.let {
            ((currentDueAt.toEpochMillis() - it.toEpochMillis()) / 60_000L).toInt()
        }
        val newReminderAt = offsetMinutes?.let { newDueAt.minusMinutes(it.toLong()) }
        return copy(dueAt = newDueAt, reminderAt = newReminderAt)
    }

    private fun buildTaskItem(
        draft: TodoDraft,
        now: Long,
        existing: TodoItem? = null,
        keepSeries: Boolean = false,
        seriesId: String? = null
    ): TodoItem {
        val dueAtMillis = draft.dueAt?.toEpochMillis() ?: NO_DUE_DATE_MILLIS
        val reminderAtMillis = if (draft.dueAt == null) null else draft.reminderAt?.toEpochMillis()
        val offsetMinutes = reminderAtMillis?.let { ((dueAtMillis - it) / 60_000L).toInt() }
        val dueDate = draft.dueAt?.toLocalDate()
        val recurrence = draft.recurrence

        return TodoItem(
            id = existing?.id ?: 0,
            itemType = PlannerItemType.TODO.name,
            title = draft.title.trim(),
            notes = draft.notes.trim(),
            dueAtMillis = dueAtMillis,
            startAtMillis = null,
            endAtMillis = null,
            allDay = false,
            location = "",
            accentColorHex = null,
            reminderAtMillis = reminderAtMillis,
            reminderEnabled = reminderAtMillis != null,
            ringEnabled = draft.ringEnabled,
            vibrateEnabled = draft.vibrateEnabled,
            voiceEnabled = false,
            groupId = draft.groupId,
            categoryKey = existing?.categoryKey ?: TodoCategory.ROUTINE.key,
            completed = existing?.completed == true,
            completedAtMillis = existing?.completedAtMillis,
            canceled = existing?.canceled == true,
            canceledAtMillis = existing?.canceledAtMillis,
            missed = false,
            missedAtMillis = null,
            recurringSeriesId = if (keepSeries) existing?.recurringSeriesId else seriesId,
            recurrenceType = if (recurrence.isRecurring) recurrence.type.name else if (keepSeries) {
                existing?.recurrenceType ?: RecurrenceType.NONE.name
            } else {
                RecurrenceType.NONE.name
            },
            recurrenceWeekdays = if (recurrence.isRecurring) recurrence.weeklyDays.toStorageString() else if (keepSeries) {
                existing?.recurrenceWeekdays.orEmpty()
            } else {
                ""
            },
            recurrenceMonthlyOrdinal = if (recurrence.isRecurring && recurrence.type == RecurrenceType.MONTHLY_NTH_WEEKDAY && dueDate != null) {
                dueDate.nthWeekOrdinal()
            } else if (keepSeries) {
                existing?.recurrenceMonthlyOrdinal
            } else {
                null
            },
            recurrenceMonthlyWeekday = if (recurrence.isRecurring && recurrence.type == RecurrenceType.MONTHLY_NTH_WEEKDAY && dueDate != null) {
                dueDate.dayOfWeek.value
            } else if (keepSeries) {
                existing?.recurrenceMonthlyWeekday
            } else {
                null
            },
            recurrenceMonthlyDay = if (recurrence.isRecurring && recurrence.type == RecurrenceType.MONTHLY_DAY && dueDate != null) {
                dueDate.dayOfMonth
            } else if (keepSeries) {
                existing?.recurrenceMonthlyDay
            } else {
                null
            },
            recurrenceEndEpochDay = if (recurrence.isRecurring) recurrence.endDate?.toEpochDay() else if (keepSeries) {
                existing?.recurrenceEndEpochDay
            } else {
                null
            },
            recurrenceAnchorDueAtMillis = if (recurrence.isRecurring) dueAtMillis else if (keepSeries) {
                existing?.recurrenceAnchorDueAtMillis
            } else {
                null
            },
            reminderOffsetMinutes = offsetMinutes,
            createdAtMillis = existing?.createdAtMillis ?: now
        )
    }

    private fun buildCalendarEventItem(
        draft: CalendarEventDraft,
        now: Long,
        existing: TodoItem? = null,
        keepSeries: Boolean = false,
        seriesId: String? = null
    ): TodoItem {
        val eventStartAtMillis = eventDisplayStartMillis(draft)
        val eventEndAtMillis = eventDisplayEndMillis(draft)
        val reminderAtMillis = draft.reminderMinutesBefore?.let { minutes ->
            reminderAnchorMillis(draft) - minutes * 60_000L
        }
        val offsetMinutes = reminderAtMillis?.let { ((eventStartAtMillis - it) / 60_000L).toInt() }
        val recurrence = draft.recurrence
        val startDate = draft.startAt.toLocalDate()

        return TodoItem(
            id = existing?.id ?: 0,
            itemType = PlannerItemType.EVENT.name,
            title = draft.title.trim(),
            notes = draft.notes.trim(),
            dueAtMillis = eventStartAtMillis,
            startAtMillis = eventStartAtMillis,
            endAtMillis = eventEndAtMillis,
            allDay = draft.allDay,
            location = draft.location.trim(),
            accentColorHex = draft.accentColorHex,
            reminderAtMillis = reminderAtMillis,
            reminderEnabled = reminderAtMillis != null,
            ringEnabled = draft.ringEnabled,
            vibrateEnabled = draft.vibrateEnabled,
            voiceEnabled = false,
            groupId = 0L,
            categoryKey = TodoCategory.ROUTINE.key,
            completed = false,
            completedAtMillis = null,
            canceled = false,
            canceledAtMillis = null,
            missed = false,
            missedAtMillis = null,
            recurringSeriesId = if (keepSeries) existing?.recurringSeriesId else seriesId,
            recurrenceType = if (recurrence.isRecurring) recurrence.type.name else if (keepSeries) {
                existing?.recurrenceType ?: RecurrenceType.NONE.name
            } else {
                RecurrenceType.NONE.name
            },
            recurrenceWeekdays = if (recurrence.isRecurring) recurrence.weeklyDays.toStorageString() else if (keepSeries) {
                existing?.recurrenceWeekdays.orEmpty()
            } else {
                ""
            },
            recurrenceMonthlyOrdinal = if (recurrence.isRecurring && recurrence.type == RecurrenceType.MONTHLY_NTH_WEEKDAY) {
                startDate.nthWeekOrdinal()
            } else if (keepSeries) {
                existing?.recurrenceMonthlyOrdinal
            } else {
                null
            },
            recurrenceMonthlyWeekday = if (recurrence.isRecurring && recurrence.type == RecurrenceType.MONTHLY_NTH_WEEKDAY) {
                startDate.dayOfWeek.value
            } else if (keepSeries) {
                existing?.recurrenceMonthlyWeekday
            } else {
                null
            },
            recurrenceMonthlyDay = if (recurrence.isRecurring && recurrence.type == RecurrenceType.MONTHLY_DAY) {
                startDate.dayOfMonth
            } else if (keepSeries) {
                existing?.recurrenceMonthlyDay
            } else {
                null
            },
            recurrenceEndEpochDay = if (recurrence.isRecurring) recurrence.endDate?.toEpochDay() else if (keepSeries) {
                existing?.recurrenceEndEpochDay
            } else {
                null
            },
            recurrenceAnchorDueAtMillis = if (recurrence.isRecurring) eventStartAtMillis else if (keepSeries) {
                existing?.recurrenceAnchorDueAtMillis
            } else {
                null
            },
            reminderOffsetMinutes = offsetMinutes,
            createdAtMillis = existing?.createdAtMillis ?: now
        )
    }

    private fun buildTemplate(item: TodoItem, draft: TodoDraft): RecurringTaskTemplate {
        val dueAt = requireNotNull(draft.dueAt) { "Recurring todo requires DDL" }
        val dueDate = dueAt.toLocalDate()
        return RecurringTaskTemplate(
            id = 0,
            seriesId = item.recurringSeriesId ?: UUID.randomUUID().toString(),
            itemType = PlannerItemType.TODO.name,
            title = draft.title.trim(),
            notes = draft.notes.trim(),
            location = "",
            accentColorHex = null,
            allDay = false,
            groupId = draft.groupId,
            dueHour = dueAt.hour,
            dueMinute = dueAt.minute,
            eventDurationMinutes = null,
            reminderOffsetMinutes = draft.reminderAt?.let {
                ((dueAt.toEpochMillis() - it.toEpochMillis()) / 60_000L).toInt()
            },
            ringEnabled = draft.ringEnabled,
            vibrateEnabled = draft.vibrateEnabled,
            recurrenceType = draft.recurrence.type.name,
            recurrenceWeekdays = draft.recurrence.weeklyDays.toStorageString(),
            recurrenceMonthlyOrdinal = if (draft.recurrence.type == RecurrenceType.MONTHLY_NTH_WEEKDAY) dueDate.nthWeekOrdinal() else null,
            recurrenceMonthlyWeekday = if (draft.recurrence.type == RecurrenceType.MONTHLY_NTH_WEEKDAY) dueDate.dayOfWeek.value else null,
            recurrenceMonthlyDay = if (draft.recurrence.type == RecurrenceType.MONTHLY_DAY) dueDate.dayOfMonth else null,
            recurrenceYearlyMonth = if (draft.recurrence.type == RecurrenceType.YEARLY_DATE) dueDate.monthValue else null,
            recurrenceYearlyDay = if (draft.recurrence.type == RecurrenceType.YEARLY_DATE) dueDate.dayOfMonth else null,
            startEpochDay = dueDate.toEpochDay(),
            endEpochDay = draft.recurrence.endDate?.toEpochDay() ?: dueDate.toEpochDay()
        )
    }

    private fun buildCalendarTemplate(item: TodoItem, draft: CalendarEventDraft): RecurringTaskTemplate {
        val startDate = draft.startAt.toLocalDate()
        return RecurringTaskTemplate(
            id = 0,
            seriesId = item.recurringSeriesId ?: UUID.randomUUID().toString(),
            itemType = PlannerItemType.EVENT.name,
            title = draft.title.trim(),
            notes = draft.notes.trim(),
            location = draft.location.trim(),
            accentColorHex = draft.accentColorHex,
            allDay = draft.allDay,
            groupId = 0L,
            dueHour = draft.startAt.hour,
            dueMinute = draft.startAt.minute,
            eventDurationMinutes = Duration.between(draft.startAt, draft.endAt).toMinutes().coerceAtLeast(30).toInt(),
            reminderOffsetMinutes = draft.reminderMinutesBefore,
            ringEnabled = draft.ringEnabled,
            vibrateEnabled = draft.vibrateEnabled,
            recurrenceType = draft.recurrence.type.name,
            recurrenceWeekdays = draft.recurrence.weeklyDays.toStorageString(),
            recurrenceMonthlyOrdinal = if (draft.recurrence.type == RecurrenceType.MONTHLY_NTH_WEEKDAY) startDate.nthWeekOrdinal() else null,
            recurrenceMonthlyWeekday = if (draft.recurrence.type == RecurrenceType.MONTHLY_NTH_WEEKDAY) startDate.dayOfWeek.value else null,
            recurrenceMonthlyDay = if (draft.recurrence.type == RecurrenceType.MONTHLY_DAY) startDate.dayOfMonth else null,
            recurrenceYearlyMonth = if (draft.recurrence.type == RecurrenceType.YEARLY_DATE) startDate.monthValue else null,
            recurrenceYearlyDay = if (draft.recurrence.type == RecurrenceType.YEARLY_DATE) startDate.dayOfMonth else null,
            startEpochDay = startDate.toEpochDay(),
            endEpochDay = draft.recurrence.endDate?.toEpochDay() ?: startDate.toEpochDay()
        )
    }

    private fun generateRecurringItems(
        draft: TodoDraft,
        seriesId: String,
        now: Long
    ): List<TodoItem> {
        val config = draft.recurrence
        if (!config.isRecurring) {
            return listOf(buildTaskItem(draft, now = now, seriesId = null))
        }

        val dueDateTime = requireNotNull(draft.dueAt) { "Recurring todo requires DDL" }
        val dueDate = dueDateTime.toLocalDate()
        val endDate = config.endDate ?: return listOf(buildTaskItem(draft, now = now, seriesId = seriesId))
        val dueTime = dueDateTime.toLocalTime()
        val offsetMinutes = draft.reminderAt?.let { ((dueDateTime.toEpochMillis() - it.toEpochMillis()) / 60_000L).toInt() }

        val dates = when (config.type) {
            RecurrenceType.NONE -> listOf(dueDate)
            RecurrenceType.DAILY -> generateDailyDates(dueDate, endDate)
            RecurrenceType.WEEKLY -> generateWeeklyDates(dueDate, endDate, config.weeklyDays.ifEmpty { setOf(dueDate.dayOfWeek) })
            RecurrenceType.MONTHLY_NTH_WEEKDAY -> generateNthWeekdayDates(dueDate, endDate)
            RecurrenceType.MONTHLY_DAY -> generateMonthlyDayDates(dueDate, endDate)
            RecurrenceType.YEARLY_DATE -> generateYearlyDates(dueDate, endDate)
        }

        return dates.map { instanceDate ->
            val instanceDue = LocalDateTime.of(instanceDate, dueTime)
            val instanceReminder = offsetMinutes?.let { instanceDue.minusMinutes(it.toLong()) }
            buildTaskItem(
                draft = draft.copy(dueAt = instanceDue, reminderAt = instanceReminder),
                now = now,
                seriesId = seriesId
            )
        }
    }

    private fun generateRecurringEventItems(
        draft: CalendarEventDraft,
        seriesId: String,
        now: Long
    ): List<TodoItem> {
        val config = draft.recurrence
        if (!config.isRecurring) {
            return listOf(buildCalendarEventItem(draft, now = now, seriesId = null))
        }

        val startDateTime = draft.startAt
        val startDate = startDateTime.toLocalDate()
        val endDate = config.endDate ?: return listOf(buildCalendarEventItem(draft, now = now, seriesId = seriesId))
        val durationMinutes = Duration.between(draft.startAt, draft.endAt).toMinutes().coerceAtLeast(30)

        val dates = when (config.type) {
            RecurrenceType.NONE -> listOf(startDate)
            RecurrenceType.DAILY -> generateDailyDates(startDate, endDate)
            RecurrenceType.WEEKLY -> generateWeeklyDates(startDate, endDate, config.weeklyDays.ifEmpty { setOf(startDate.dayOfWeek) })
            RecurrenceType.MONTHLY_NTH_WEEKDAY -> generateNthWeekdayDates(startDate, endDate)
            RecurrenceType.MONTHLY_DAY -> generateMonthlyDayDates(startDate, endDate)
            RecurrenceType.YEARLY_DATE -> generateYearlyDates(startDate, endDate)
        }

        return dates.map { instanceDate ->
            val instanceStart = LocalDateTime.of(instanceDate, draft.startAt.toLocalTime())
            val instanceEnd = instanceStart.plusMinutes(durationMinutes)
            buildCalendarEventItem(
                draft = draft.copy(startAt = instanceStart, endAt = instanceEnd),
                now = now,
                seriesId = seriesId
            )
        }
    }

    private fun generateDailyDates(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var cursor = start
        while (!cursor.isAfter(end)) {
            dates += cursor
            cursor = cursor.plusDays(1)
        }
        return dates
    }

    private fun generateWeeklyDates(
        start: LocalDate,
        end: LocalDate,
        weekdays: Set<DayOfWeek>
    ): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var cursor = start
        while (!cursor.isAfter(end)) {
            if (cursor.dayOfWeek in weekdays) {
                dates += cursor
            }
            cursor = cursor.plusDays(1)
        }
        return dates
    }

    private fun generateNthWeekdayDates(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        val ordinal = start.nthWeekOrdinal()
        val weekday = start.dayOfWeek
        var cursor = YearMonth.from(start)
        val endMonth = YearMonth.from(end)
        while (!cursor.isAfter(endMonth)) {
            val occurrence = resolveNthWeekdayDate(cursor, ordinal, weekday)
            if (occurrence != null && !occurrence.isBefore(start) && !occurrence.isAfter(end)) {
                dates += occurrence
            }
            cursor = cursor.plusMonths(1)
        }
        return dates
    }

    private fun generateMonthlyDayDates(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        val targetDay = start.dayOfMonth
        var cursor = YearMonth.from(start)
        val endMonth = YearMonth.from(end)
        while (!cursor.isAfter(endMonth)) {
            val occurrence = resolveMonthlyDate(cursor, targetDay)
            if (!occurrence.isBefore(start) && !occurrence.isAfter(end)) {
                dates += occurrence
            }
            cursor = cursor.plusMonths(1)
        }
        return dates
    }

    private fun generateYearlyDates(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var year = start.year
        while (year <= end.year) {
            val month = start.monthValue
            val day = start.dayOfMonth
            val candidate = resolveMonthlyDate(YearMonth.of(year, month), day)
            if (!candidate.isBefore(start) && !candidate.isAfter(end)) {
                dates += candidate
            }
            year += 1
        }
        return dates
    }

    companion object {
        private const val MISSED_GRACE_PERIOD_MILLIS = 60_000L
    }

    private suspend fun defaultGroupId(): Long {
        val groups = ensureDefaultGroups()
        return groups.firstOrNull { it.name == "例行" }?.id ?: groups.firstOrNull()?.id ?: 0L
    }

    private fun reminderAnchorMillis(draft: CalendarEventDraft): Long {
        return draft.reminderAnchorAt.toEpochMillis()
    }

    private fun eventDisplayStartMillis(draft: CalendarEventDraft): Long {
        val start = if (draft.allDay) {
            LocalDateTime.of(draft.startAt.toLocalDate(), LocalTime.MIN)
        } else {
            draft.startAt
        }
        return start.toEpochMillis()
    }

    private fun eventDisplayEndMillis(draft: CalendarEventDraft): Long {
        val end = if (draft.allDay) {
            LocalDateTime.of(draft.endAt.toLocalDate().plusDays(1), LocalTime.MIN)
        } else {
            draft.endAt
        }
        return end.toEpochMillis()
    }
}
