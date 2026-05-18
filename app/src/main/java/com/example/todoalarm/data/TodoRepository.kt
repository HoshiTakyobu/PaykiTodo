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
import java.time.format.DateTimeFormatter
import java.util.UUID

class TodoRepository(
    private val todoDao: TodoDao,
    private val onItemsChanged: (() -> Unit)? = null
) {
    fun observeTodos(): Flow<List<TodoItem>> = todoDao.observeTodos()
    fun observeActiveTodoItems(groupId: Long?): Flow<List<TodoItem>> = todoDao.observeActiveTodoItems(groupId)
    fun observeHistoryTodoItems(groupId: Long?): Flow<List<TodoItem>> = todoDao.observeHistoryTodoItems(groupId)
    fun observeActiveCalendarEvents(): Flow<List<TodoItem>> = todoDao.observeActiveCalendarEvents()
    fun observeActiveCalendarEventsInRange(rangeStartMillis: Long, rangeEndMillis: Long): Flow<List<TodoItem>> {
        return todoDao.observeActiveCalendarEventsInRange(rangeStartMillis, rangeEndMillis)
    }
    fun observeActiveCountdownItems(minTargetMillis: Long): Flow<List<TodoItem>> {
        return todoDao.observeActiveCountdownItems(NO_DUE_DATE_MILLIS, minTargetMillis)
    }
    fun observeGroups(): Flow<List<TaskGroup>> = todoDao.observeGroups()
    fun observePlanningNotes(): Flow<List<PlanningNote>> = todoDao.observePlanningNotes()
    fun observePlanningNotesWithAnnouncementHints(): Flow<List<PlanningNote>> = todoDao.observePlanningNotesWithAnnouncementHints()
    fun observeFocusSessions(): Flow<List<FocusSession>> = todoDao.observeFocusSessions()
    fun observeAiReports(limit: Int): Flow<List<AiReport>> = todoDao.observeAiReports(limit)
    fun observeAiReportsByType(type: AiReportType, limit: Int): Flow<List<AiReport>> {
        return todoDao.observeAiReportsByType(type, limit)
    }
    fun observeAiReportsFiltered(
        type: AiReportType?,
        query: String,
        startMillis: Long,
        endMillis: Long,
        limit: Int
    ): Flow<List<AiReport>> {
        val safeQuery = query.trim()
        return if (type == null) {
            todoDao.observeAiReportsFiltered(
                query = safeQuery,
                startMillis = startMillis,
                endMillis = endMillis,
                limit = limit
            )
        } else {
            todoDao.observeAiReportsFilteredByType(
                type = type,
                query = safeQuery,
                startMillis = startMillis,
                endMillis = endMillis,
                limit = limit
            )
        }
    }
    fun observeRecentReminderChainLogs(limit: Int = 80): Flow<List<ReminderChainLog>> {
        return todoDao.observeRecentReminderChainLogs(limit)
    }
    fun observeScheduleTemplates(): Flow<List<ScheduleTemplate>> = todoDao.observeScheduleTemplates()
    fun observeFocusSessionStatsInRange(startMillis: Long, endMillis: Long): Flow<FocusSessionStats> {
        return todoDao.observeFocusSessionStatsInRange(startMillis, endMillis)
    }

    suspend fun addTodo(item: TodoItem): TodoItem {
        val id = todoDao.insert(item)
        notifyItemsChanged()
        return item.copy(id = id)
    }

    suspend fun getTodo(id: Long): TodoItem? = todoDao.getById(id)
    suspend fun getGroup(groupId: Long): TaskGroup? = todoDao.getGroupById(groupId)
    suspend fun getAllTodos(): List<TodoItem> = todoDao.getAllTodos()
    suspend fun getDesktopTodoItems(): List<TodoItem> = todoDao.getDesktopTodoItems()
    suspend fun getDesktopTodoItemsPaged(
        query: String,
        limit: Int,
        offset: Int,
        today: LocalDate = LocalDate.now()
    ): List<TodoItem> {
        val zone = ZoneId.systemDefault()
        val todayStartMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEndMillisExclusive = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return todoDao.getDesktopTodoItemsPaged(
            query = query.trim(),
            todayStartMillis = todayStartMillis,
            todayEndMillisExclusive = todayEndMillisExclusive,
            noDueDateMillis = NO_DUE_DATE_MILLIS,
            limit = limit,
            offset = offset
        )
    }
    suspend fun countDesktopTodoItems(query: String): Int = todoDao.countDesktopTodoItems(query.trim())
    suspend fun getActiveCalendarEventsInRangeOnce(rangeStartMillis: Long, rangeEndMillis: Long): List<TodoItem> {
        return todoDao.getActiveCalendarEventsInRangeOnce(rangeStartMillis, rangeEndMillis)
    }
    suspend fun saveFocusSession(session: FocusSession): Long {
        val id = todoDao.insertFocusSession(session)
        notifyItemsChanged()
        return id
    }

    suspend fun getTodayFocusMinutes(): Int {
        val (startMillis, endMillis) = todayRangeMillis()
        return todoDao.getCompletedFocusMinutesInRange(startMillis, endMillis)
    }

    suspend fun getTodayFocusSessions(): List<FocusSession> {
        val (startMillis, endMillis) = todayRangeMillis()
        return todoDao.getFocusSessionsInRange(startMillis, endMillis)
    }

    suspend fun getFocusSessionsInRange(startMillis: Long, endMillis: Long): List<FocusSession> {
        return todoDao.getFocusSessionsInRange(startMillis, endMillis)
    }

    suspend fun getFocusSessionStatsInRange(startMillis: Long, endMillis: Long): FocusSessionStats {
        return todoDao.getFocusSessionStatsInRange(startMillis, endMillis)
    }

    suspend fun getTodayFocusSessionStats(date: LocalDate = LocalDate.now()): FocusSessionStats {
        val (startMillis, endMillis) = todayRangeMillis(date)
        return todoDao.getFocusSessionStatsInRange(startMillis, endMillis)
    }

    suspend fun getActiveItemsForBoardRange(now: LocalDate = LocalDate.now()): List<TodoItem> {
        val zone = ZoneId.systemDefault()
        val boardStart = now.atStartOfDay(zone).toInstant().toEpochMilli()
        val boardEnd = now.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        val todoStart = now.minusDays(30).atStartOfDay(zone).toInstant().toEpochMilli()
        return todoDao.getActiveItemsForBoardRange(
            todoStartMillis = todoStart,
            boardStartMillis = boardStart,
            boardEndMillis = boardEnd,
            noDueDateMillis = NO_DUE_DATE_MILLIS
        )
    }

    suspend fun getAllPlanningNotes(): List<PlanningNote> = todoDao.getAllPlanningNotes()
    suspend fun getActivePlanningNotes(): List<PlanningNote> = todoDao.getActivePlanningNotes()
    suspend fun getPlanningNotesWithAnnouncementHints(): List<PlanningNote> = todoDao.getPlanningNotesWithAnnouncementHints()
    suspend fun getPlanningMappingsForNote(noteId: Long): List<PlanningLineMapping> = todoDao.getMappingsForNote(noteId)

    suspend fun insertPlanningMappings(mappings: List<PlanningLineMapping>) {
        if (mappings.isNotEmpty()) todoDao.insertPlanningMappings(mappings)
    }

    suspend fun ensureDefaultPlanningNote(): PlanningNote {
        val existing = todoDao.getAllPlanningNotes().firstOrNull { !it.archived }
        if (existing != null) return existing
        val now = System.currentTimeMillis()
        val note = PlanningNote(
            title = "我的规划",
            contentMarkdown = "",
            createdAtMillis = now,
            updatedAtMillis = now,
            archived = false
        )
        val id = todoDao.insertPlanningNote(note)
        return note.copy(id = id)
    }

    suspend fun createPlanningNote(title: String = "新的规划"): PlanningNote {
        val now = System.currentTimeMillis()
        val note = PlanningNote(
            title = title.trim().ifBlank { "新的规划" },
            contentMarkdown = "",
            createdAtMillis = now,
            updatedAtMillis = now,
            archived = false
        )
        val id = todoDao.insertPlanningNote(note)
        return note.copy(id = id)
    }

    suspend fun updatePlanningNoteContent(noteId: Long, contentMarkdown: String): PlanningNote? {
        val note = todoDao.getPlanningNote(noteId) ?: return null
        val updated = note.copy(
            contentMarkdown = contentMarkdown,
            updatedAtMillis = System.currentTimeMillis(),
            hasAnnouncementHint = PlanningAnnouncementParser.mightContainAnnouncement(contentMarkdown)
        )
        todoDao.updatePlanningNote(updated)
        notifyItemsChanged()
        return updated
    }

    suspend fun renamePlanningNote(noteId: Long, title: String): PlanningNote? {
        val note = todoDao.getPlanningNote(noteId) ?: return null
        val updated = note.copy(title = title.trim().ifBlank { note.title }, updatedAtMillis = System.currentTimeMillis())
        todoDao.updatePlanningNote(updated)
        notifyItemsChanged()
        return updated
    }

    suspend fun archivePlanningNote(noteId: Long): PlanningNote? {
        val note = todoDao.getPlanningNote(noteId) ?: return null
        val updated = note.copy(archived = true, updatedAtMillis = System.currentTimeMillis())
        todoDao.updatePlanningNote(updated)
        notifyItemsChanged()
        return updated
    }

    suspend fun deletePlanningNote(noteId: Long) {
        todoDao.deletePlanningMappingsForNote(noteId)
        todoDao.deletePlanningNote(noteId)
        notifyItemsChanged()
    }

    suspend fun syncPlanningMappingStatuses(noteId: Long, markdown: String): PlanningMappingStatusSnapshot {
        val mappings = todoDao.getMappingsForNote(noteId)
        if (mappings.isEmpty()) return PlanningMappingStatusSnapshot(emptyList(), 0)
        val lines = planningDocumentLines(markdown)
        val relocated = PlanningLineMatcher.relocateMappings(lines, mappings)
        val updated = mappings.map { mapping ->
            val item = mapping.itemId?.let { todoDao.getById(it) }
            val nextStatus = when {
                item == null -> MappingStatus.ORPHANED
                item.completed -> MappingStatus.COMPLETED
                item.canceled -> MappingStatus.CANCELED
                mapping.status == MappingStatus.CONFLICT -> MappingStatus.CONFLICT
                relocated[mapping.id] == null -> MappingStatus.ORPHANED
                else -> MappingStatus.ACTIVE
            }
            val lineIndex = relocated[mapping.id]
            val nextLineText = lineIndex?.let { lines.getOrNull(it) }.orEmpty()
            mapping.copy(
                status = nextStatus,
                currentLineText = nextLineText.ifBlank { mapping.currentLineText },
                lastKnownLineNumber = lineIndex?.plus(1) ?: mapping.lastKnownLineNumber
            )
        }
        val changed = updated.filterIndexed { index, next -> next != mappings[index] }
        if (changed.isNotEmpty()) todoDao.updatePlanningMappings(changed)
        return PlanningMappingStatusSnapshot(todoDao.getMappingsForNote(noteId), changed.size)
    }

    suspend fun refreshPlanningImportedItems(
        noteId: Long,
        markdown: String,
        wholeDocument: Boolean = true,
        cursorLineNumber: Int? = null
    ): PlanningOperationResult {
        val sync = syncPlanningMappingStatuses(noteId, markdown)
        val activeMappings = sync.mappings.filter { it.status == MappingStatus.ACTIVE }
        if (activeMappings.isEmpty()) {
            return PlanningOperationResult(message = "没有可刷新的已导入项")
        }
        val lines = planningDocumentLines(markdown)
        val allowedLineRange = if (wholeDocument) lines.indices else planningSectionRange(lines, cursorLineNumber)
        val relocated = PlanningLineMatcher.relocateMappings(lines, activeMappings)
        val parseByLine = PlanningMarkdownParser.parse(markdownWithoutImportedMarkers(markdown)).candidates
            .filter { it.importable }
            .associateBy { it.lineNumber }
        val groups = getAllGroups().ifEmpty { ensureDefaultGroups() }
        val batchId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val updatedMappings = mutableListOf<PlanningLineMapping>()
        val beforeItems = mutableListOf<TodoItem>()
        val afterItems = mutableListOf<TodoItem>()
        var skipped = sync.mappings.count { it.status == MappingStatus.COMPLETED || it.status == MappingStatus.CANCELED }
        var orphaned = 0
        var conflicts = 0

        for (mapping in activeMappings) {
            val lineIndex = relocated[mapping.id]
            if (lineIndex == null) {
                orphaned += 1
                updatedMappings += mapping.copy(status = MappingStatus.ORPHANED)
                continue
            }
            if (lineIndex !in allowedLineRange) {
                skipped += 1
                continue
            }
            val candidate = parseByLine[lineIndex + 1]
            if (candidate == null) {
                skipped += 1
                continue
            }
            val item = mapping.itemId?.let { todoDao.getById(it) }
            if (item == null) {
                orphaned += 1
                updatedMappings += mapping.copy(status = MappingStatus.ORPHANED)
                continue
            }
            if (item.completed || item.canceled) {
                skipped += 1
                updatedMappings += mapping.copy(status = if (item.completed) MappingStatus.COMPLETED else MappingStatus.CANCELED)
                continue
            }
            if (isPlanningItemConflict(item, mapping)) {
                conflicts += 1
                updatedMappings += mapping.copy(status = MappingStatus.CONFLICT)
                continue
            }
            val updated = updateItemFromPlanningCandidate(item, candidate, groups)
            if (updated == null) {
                skipped += 1
                continue
            }
            beforeItems += item
            afterItems += updated
            updatedMappings += mapping.copy(
                currentLineText = lines[lineIndex],
                contentFingerprint = PlanningLineMatcher.fingerprint(lines[lineIndex]),
                batchId = batchId,
                operationType = "REFRESH",
                lastRefreshedAtMillis = now,
                lastKnownLineNumber = lineIndex + 1,
                status = MappingStatus.ACTIVE
            )
        }

        if (afterItems.isNotEmpty()) {
            todoDao.updateAll(afterItems)
            notifyItemsChanged()
        }
        if (updatedMappings.isNotEmpty()) todoDao.updatePlanningMappings(updatedMappings)
        val message = "已刷新 ${afterItems.size} 条，跳过 $skipped 条已完成/不在范围项，冲突 $conflicts 条，丢失 $orphaned 条"
        return PlanningOperationResult(
            message = message,
            affectedBeforeItems = beforeItems,
            affectedAfterItems = afterItems,
            refreshedCount = afterItems.size,
            skippedCount = skipped,
            orphanedCount = orphaned,
            conflictCount = conflicts,
            batchId = batchId.takeIf { afterItems.isNotEmpty() || updatedMappings.isNotEmpty() }
        )
    }

    suspend fun postponePlanningImportedItems(
        noteId: Long,
        markdown: String,
        startMappingId: Long?,
        offsetMinutes: Int,
        scope: PlanningPostponeScope
    ): PlanningOperationResult {
        val offset = offsetMinutes.coerceIn(-24 * 60, 24 * 60)
        if (offset == 0) return PlanningOperationResult(message = "顺延分钟数不能为 0")
        val sync = syncPlanningMappingStatuses(noteId, markdown)
        val activeMappings = sync.mappings.filter { it.status == MappingStatus.ACTIVE }
        if (activeMappings.isEmpty()) return PlanningOperationResult(message = "没有可顺延的已导入项")
        val lines = planningDocumentLines(markdown)
        val relocated = PlanningLineMatcher.relocateMappings(lines, activeMappings)
        val startLine = startMappingId?.let { id -> relocated[id] } ?: activeMappings.mapNotNull { relocated[it.id] }.minOrNull()
        if (startLine == null) return PlanningOperationResult(message = "找不到起始条目")
        val targetRange = when (scope) {
            PlanningPostponeScope.FROM_ITEM_TO_SECTION_END -> planningSectionRange(lines, startLine + 1).let { startLine..it.last }
            PlanningPostponeScope.FROM_ITEM_TO_DOCUMENT_END -> startLine..lines.lastIndex
            PlanningPostponeScope.CURRENT_SECTION_ALL -> planningSectionRange(lines, startLine + 1)
        }
        val targets = activeMappings
            .mapNotNull { mapping -> relocated[mapping.id]?.let { lineIndex -> mapping to lineIndex } }
            .filter { (_, lineIndex) -> lineIndex in targetRange }
            .sortedBy { it.second }
        if (targets.isEmpty()) return PlanningOperationResult(message = "当前范围没有可顺延条目")

        val batchId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val mutableLines = lines.toMutableList()
        val beforeItems = mutableListOf<TodoItem>()
        val afterItems = mutableListOf<TodoItem>()
        val updatedMappings = mutableListOf<PlanningLineMapping>()

        for ((mapping, lineIndex) in targets) {
            val item = mapping.itemId?.let { todoDao.getById(it) } ?: continue
            if (item.completed || item.canceled) continue
            if (isPlanningItemConflict(item, mapping)) {
                updatedMappings += mapping.copy(status = MappingStatus.CONFLICT)
                continue
            }
            val updated = postponeItem(item, offset)
            beforeItems += item
            afterItems += updated
            val updatedLine = shiftPlanningLineTimeText(mutableLines[lineIndex], offset)
            mutableLines[lineIndex] = updatedLine
            updatedMappings += mapping.copy(
                currentLineText = updatedLine,
                contentFingerprint = PlanningLineMatcher.fingerprint(updatedLine),
                batchId = batchId,
                operationType = "POSTPONE",
                postponeOffsetMinutes = offset,
                lastRefreshedAtMillis = now,
                lastKnownLineNumber = lineIndex + 1,
                status = MappingStatus.ACTIVE
            )
        }

        if (afterItems.isNotEmpty()) {
            todoDao.updateAll(afterItems)
            notifyItemsChanged()
        }
        if (updatedMappings.isNotEmpty()) todoDao.updatePlanningMappings(updatedMappings)
        val updatedMarkdown = joinPlanningDocumentLines(markdown, mutableLines)
        updatePlanningNoteContent(noteId, updatedMarkdown)
        return PlanningOperationResult(
            message = "已顺延 ${afterItems.size} 条，偏移 ${offset} 分钟",
            updatedMarkdown = updatedMarkdown,
            affectedBeforeItems = beforeItems,
            affectedAfterItems = afterItems,
            refreshedCount = afterItems.size,
            conflictCount = updatedMappings.count { it.status == MappingStatus.CONFLICT },
            batchId = batchId
        )
    }

    suspend fun undoLastPlanningOperation(noteId: Long, markdown: String): PlanningOperationResult {
        val latest = todoDao.getLatestPlanningMappingForNote(noteId) ?: return PlanningOperationResult(message = "没有可撤销的规划台操作")
        val batch = todoDao.getMappingsByBatch(latest.batchId).filter { it.noteId == noteId }
        if (batch.isEmpty()) return PlanningOperationResult(message = "没有可撤销的规划台操作")
        return when (latest.operationType) {
            "IMPORT" -> undoPlanningImport(noteId, markdown, latest.batchId, batch)
            "REFRESH" -> undoPlanningRefresh(batch)
            "POSTPONE" -> undoPlanningPostpone(noteId, markdown, batch)
            else -> PlanningOperationResult(message = "暂不支持撤销 ${latest.operationType}")
        }
    }

    suspend fun resolvePlanningConflictWithDocument(
        noteId: Long,
        markdown: String,
        mappingId: Long
    ): PlanningOperationResult {
        val mapping = todoDao.getMappingsForNote(noteId).firstOrNull { it.id == mappingId } ?: return PlanningOperationResult(message = "映射不存在")
        val item = mapping.itemId?.let { todoDao.getById(it) } ?: return PlanningOperationResult(message = "事项不存在")
        val lines = planningDocumentLines(markdown)
        val lineIndex = PlanningLineMatcher.relocateMappings(lines, listOf(mapping))[mapping.id]
            ?: return PlanningOperationResult(message = "文档中找不到对应行")
        val candidate = PlanningMarkdownParser.parse(markdownWithoutImportedMarkers(markdown)).candidates
            .firstOrNull { it.lineNumber == lineIndex + 1 && it.importable }
            ?: return PlanningOperationResult(message = "当前行无法解析为可导入事项")
        val groups = getAllGroups().ifEmpty { ensureDefaultGroups() }
        val updated = updateItemFromPlanningCandidate(item, candidate, groups) ?: return PlanningOperationResult(message = "事项类型与文档行不匹配")
        val now = System.currentTimeMillis()
        val batchId = UUID.randomUUID().toString()
        todoDao.update(updated)
        notifyItemsChanged()
        todoDao.updatePlanningMappings(
            listOf(
                mapping.copy(
                    status = MappingStatus.ACTIVE,
                    currentLineText = lines[lineIndex],
                    contentFingerprint = PlanningLineMatcher.fingerprint(lines[lineIndex]),
                    lastKnownLineNumber = lineIndex + 1,
                    lastRefreshedAtMillis = now,
                    operationType = "REFRESH",
                    batchId = batchId
                )
            )
        )
        return PlanningOperationResult(
            message = "已按文档内容覆盖事项",
            affectedBeforeItems = listOf(item),
            affectedAfterItems = listOf(updated),
            batchId = batchId
        )
    }

    suspend fun resolvePlanningConflictWithItem(
        noteId: Long,
        markdown: String,
        mappingId: Long
    ): PlanningOperationResult {
        val mapping = todoDao.getMappingsForNote(noteId).firstOrNull { it.id == mappingId } ?: return PlanningOperationResult(message = "映射不存在")
        val item = mapping.itemId?.let { todoDao.getById(it) } ?: return PlanningOperationResult(message = "事项不存在")
        val lines = planningDocumentLines(markdown).toMutableList()
        val lineIndex = PlanningLineMatcher.relocateMappings(lines, listOf(mapping))[mapping.id] ?: ((mapping.lastKnownLineNumber - 1).coerceAtLeast(0).coerceAtMost(lines.size))
        val groupName = item.groupId.takeIf { it > 0 }?.let { todoDao.getGroupById(it)?.name }.orEmpty()
        val rewrittenLine = planningLineFromItem(item, groupName)
        if (lineIndex in lines.indices) {
            lines[lineIndex] = rewrittenLine
        } else {
            lines += rewrittenLine
        }
        val updatedMarkdown = joinPlanningDocumentLines(markdown, lines)
        updatePlanningNoteContent(noteId, updatedMarkdown)
        val now = System.currentTimeMillis()
        val batchId = UUID.randomUUID().toString()
        todoDao.updatePlanningMappings(
            listOf(
                mapping.copy(
                    status = MappingStatus.ACTIVE,
                    currentLineText = rewrittenLine,
                    contentFingerprint = PlanningLineMatcher.fingerprint(rewrittenLine),
                    lastKnownLineNumber = (if (lineIndex in lines.indices) lineIndex else lines.lastIndex) + 1,
                    lastRefreshedAtMillis = now,
                    operationType = "REFRESH",
                    batchId = batchId
                )
            )
        )
        return PlanningOperationResult(
            message = "已按事项内容回写原文",
            updatedMarkdown = updatedMarkdown,
            batchId = batchId
        )
    }

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
        notifyItemsChanged()
        return item
    }

    suspend fun deleteTodo(id: Long) {
        todoDao.deleteById(id)
        notifyItemsChanged()
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
        notifyItemsChanged()
        return created
    }

    suspend fun createCalendarEventFromDraft(draft: CalendarEventDraft): List<TodoItem> {
        val now = System.currentTimeMillis()
        val resolvedGroupId = resolveCalendarGroupId(draft)
        val resolvedDraft = draft.copy(groupId = resolvedGroupId)
        val generated = if (draft.recurrence.isRecurring) {
            generateRecurringEventItems(
                draft = resolvedDraft,
                seriesId = UUID.randomUUID().toString(),
                now = now
            )
        } else {
            listOf(buildCalendarEventItem(resolvedDraft, now = now))
        }
        val ids = todoDao.insertAll(generated)
        val created = generated.zip(ids) { item, id -> item.copy(id = id) }
        if (resolvedDraft.recurrence.isRecurring) {
            todoDao.insertTemplate(buildCalendarTemplate(created.first(), resolvedDraft))
        }
        notifyItemsChanged()
        return created
    }

    suspend fun updateFromDraft(
        original: TodoItem,
        draft: TodoDraft,
        scope: RecurrenceScope = RecurrenceScope.CURRENT
    ): List<TodoItem> {
        val resolvedDraft = draft.copy(groupId = draft.groupId.takeIf { it > 0 } ?: original.groupId)
        val updatedItems = when {
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
        notifyItemsChanged()
        return updatedItems
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
        notifyItemsChanged()
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
                item.reminderTriggerTimesMillis().any { it > now }
            }
        )
        todoDao.update(updated)
        notifyItemsChanged()
        return updated
    }

    suspend fun snoozeTodo(id: Long, nextReminderMillis: Long): TodoItem? {
        val item = todoDao.getById(id) ?: return null
        if (item.isHistory) return null
        val updated = item.copy(
            dueAtMillis = item.dueAtMillis,
            reminderAtMillis = nextReminderMillis,
            reminderOffsetsCsv = if (item.isTodo) "" else item.reminderOffsetsCsv,
            reminderEnabled = true,
            reminderOffsetMinutes = if (item.isTodo) null else item.reminderOffsetMinutes,
            missed = false,
            missedAtMillis = null
        )
        todoDao.update(updated)
        notifyItemsChanged()
        return updated
    }

    suspend fun postponeTodoDueAt(id: Long, newDueAtMillis: Long): TodoItem? {
        val item = todoDao.getById(id) ?: return null
        if (!item.isTodo || item.isHistory || !item.hasDueDate || newDueAtMillis <= item.dueAtMillis) return null
        val now = System.currentTimeMillis()
        val preservedOffsets = item.configuredReminderOffsetsMinutes.ifEmpty { listOf(0) }
        val futureOffsets = preservedOffsets
            .filter { offset -> newDueAtMillis - offset * 60_000L > now }
            .ifEmpty { listOf(0) }
        val nextReminderMillis = futureOffsets
            .map { offset -> newDueAtMillis - offset * 60_000L }
            .filter { it > now }
            .minOrNull()
        val updated = item.copy(
            dueAtMillis = newDueAtMillis,
            reminderAtMillis = nextReminderMillis,
            reminderOffsetsCsv = encodeReminderOffsets(futureOffsets),
            reminderEnabled = nextReminderMillis != null,
            reminderOffsetMinutes = futureOffsets.firstOrNull(),
            missed = false,
            missedAtMillis = null
        )
        todoDao.update(updated)
        notifyItemsChanged()
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
        notifyItemsChanged()
        return updated.size
    }

    suspend fun futureReminderItems(now: Long): List<TodoItem> {
        return todoDao.getActiveReminderItems().filter { item ->
            item.reminderTriggerTimesMillis().any { it >= now }
        }
    }

    suspend fun dueReminderItems(now: Long, graceWindowMillis: Long = 2 * 60_000L): List<TodoItem> {
        val earliestAllowed = now - graceWindowMillis
        return todoDao.getActiveReminderItems().filter { item ->
            item.reminderTriggerTimesMillis().any { it in earliestAllowed..now }
        }
    }

    suspend fun nextReminderItem(): TodoItem? {
        return todoDao.getActiveReminderItems()
            .mapNotNull { item -> item.reminderTriggerTimesMillis().filter { it >= System.currentTimeMillis() }.minOrNull()?.let { trigger -> item to trigger } }
            .minByOrNull { it.second }
            ?.first
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
        val resolvedGroupId = when {
            draft.groupId > 0 -> draft.groupId
            draft.groupName.isNotBlank() -> resolveGroupIdByNameOrCreate(draft.groupName)
            else -> original.groupId
        }
        val resolvedDraft = draft.copy(groupId = resolvedGroupId)
        val updatedItems = when {
            !original.isRecurring && resolvedDraft.recurrence.isRecurring -> {
                convertSingleEventToRecurring(original, resolvedDraft)
            }
            original.isRecurring && scope != RecurrenceScope.CURRENT -> {
                updateRecurringCalendarSeries(original, resolvedDraft, scope)
            }
            else -> {
                val updated = buildCalendarEventItem(
                    draft = resolvedDraft,
                    now = original.createdAtMillis,
                    existing = original,
                    keepSeries = original.isRecurring
                )
                todoDao.update(updated)
                listOf(updated)
            }
        }
        notifyItemsChanged()
        return updatedItems
    }

    suspend fun deleteCalendarEvent(
        item: TodoItem,
        scope: RecurrenceScope = RecurrenceScope.CURRENT
    ): List<TodoItem> {
        if (!item.isEvent) return emptyList()
        val seriesId = item.recurringSeriesId
        val deletedItems = when {
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
        if (deletedItems.isNotEmpty()) notifyItemsChanged()
        return deletedItems
    }

    suspend fun getAllGroups(): List<TaskGroup> = todoDao.getAllGroups()

    suspend fun getRecentReminderChainLogs(limit: Int = 80): List<ReminderChainLog> {
        return todoDao.getRecentReminderChainLogs(limit)
    }

    suspend fun addReminderChainLog(log: ReminderChainLog) {
        todoDao.insertReminderChainLog(log)
        todoDao.trimReminderChainLogs(400)
    }

    suspend fun clearReminderChainLogs() {
        todoDao.clearReminderChainLogs()
    }

    suspend fun getScheduleTemplates(): List<ScheduleTemplate> = todoDao.getScheduleTemplates()

    suspend fun upsertScheduleTemplate(template: ScheduleTemplate): ScheduleTemplate {
        val id = todoDao.insertScheduleTemplate(template.copy(updatedAtMillis = System.currentTimeMillis()))
        return template.copy(id = id)
    }

    suspend fun deleteScheduleTemplate(templateId: Long) {
        todoDao.deleteScheduleTemplate(templateId)
    }

    suspend fun saveAiReport(report: AiReport): Long {
        return todoDao.insertAiReport(report)
    }

    suspend fun saveAiReports(reports: List<AiReport>): List<Long> {
        if (reports.isEmpty()) return emptyList()
        return todoDao.insertAiReports(reports)
    }

    suspend fun getAllAiReports(): List<AiReport> = todoDao.getAllAiReports()

    suspend fun getAiReportsByType(type: AiReportType): List<AiReport> = todoDao.getAiReportsByType(type)

    suspend fun getAiReportById(id: Long): AiReport? = todoDao.getAiReportById(id)

    suspend fun deleteAiReport(reportId: Long) {
        todoDao.deleteAiReport(reportId)
    }

    suspend fun exportSnapshot(settings: AppSettings): BackupSnapshot {
        val planningNotes = todoDao.getAllPlanningNotes()
        val planningMappings = mutableListOf<PlanningLineMapping>()
        for (note in planningNotes) {
            planningMappings += todoDao.getMappingsForNote(note.id)
        }
        return BackupSnapshot(
            exportedAtMillis = System.currentTimeMillis(),
            groups = todoDao.getAllGroups(),
            templates = todoDao.getAllRecurringTemplates(),
            tasks = todoDao.getAllTodos(),
            focusSessions = todoDao.getAllFocusSessions(),
            reminderChainLogs = todoDao.getRecentReminderChainLogs(400),
            scheduleTemplates = todoDao.getScheduleTemplates(),
            planningNotes = planningNotes,
            planningLineMappings = planningMappings,
            aiReports = todoDao.getAllAiReports(),
            settings = settings
        )
    }

    suspend fun importSnapshot(snapshot: BackupSnapshot) {
        todoDao.clearTodos()
        todoDao.clearTemplates()
        todoDao.clearGroups()
        todoDao.clearReminderChainLogs()
        todoDao.clearScheduleTemplates()
        todoDao.clearPlanningNotes()
        todoDao.clearPlanningMappings()
        todoDao.clearFocusSessions()
        todoDao.clearAiReports()
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
        if (snapshot.focusSessions.isNotEmpty()) {
            todoDao.insertFocusSessions(snapshot.focusSessions)
        }
        if (snapshot.reminderChainLogs.isNotEmpty()) {
            todoDao.insertReminderChainLogs(snapshot.reminderChainLogs)
        }
        snapshot.scheduleTemplates.forEach { template ->
            todoDao.insertScheduleTemplate(template)
        }
        if (snapshot.planningNotes.isNotEmpty()) {
            todoDao.insertPlanningNotes(snapshot.planningNotes)
        }
        if (snapshot.planningLineMappings.isNotEmpty()) {
            todoDao.insertPlanningMappings(snapshot.planningLineMappings)
        }
        if (snapshot.aiReports.isNotEmpty()) {
            todoDao.insertAiReports(snapshot.aiReports)
        }
    }

    private suspend fun updateItemFromPlanningCandidate(
        item: TodoItem,
        candidate: PlanningParsedCandidate,
        groups: List<TaskGroup>
    ): TodoItem? {
        val groupId = resolvePlanningGroupId(candidate.groupName, groups, item.groupId)
        val offsets = candidate.reminderOffsetsMinutes
            .map { it.coerceAtLeast(0) }
            .distinct()
            .sortedDescending()
        return when {
            item.isTodo && candidate.type == PlanningParsedType.TODO -> {
                val dueAtMillis = candidate.dueAt?.toEpochMillis() ?: NO_DUE_DATE_MILLIS
                val reminderOffsets = if (candidate.dueAt == null) emptyList() else offsets
                val updated = item.copy(
                    title = candidate.title.trim(),
                    notes = candidate.notes.trim(),
                    dueAtMillis = dueAtMillis,
                    groupId = groupId,
                    reminderOffsetsCsv = encodeReminderOffsets(reminderOffsets),
                    reminderOffsetMinutes = reminderOffsets.firstOrNull(),
                    reminderAtMillis = candidate.dueAt?.let { due -> reminderOffsets.minOrNull()?.let { due.minusMinutes(it.toLong()).toEpochMillis() } },
                    reminderEnabled = reminderOffsets.isNotEmpty(),
                    missed = false,
                    missedAtMillis = null
                )
                updated
            }
            item.isEvent && candidate.type == PlanningParsedType.EVENT && candidate.startAt != null && candidate.endAt != null -> {
                val startMillis = candidate.startAt.toEpochMillis()
                val endMillis = candidate.endAt.toEpochMillis()
                val updated = item.copy(
                    title = candidate.title.trim(),
                    notes = candidate.notes.trim(),
                    dueAtMillis = startMillis,
                    startAtMillis = startMillis,
                    endAtMillis = endMillis,
                    groupId = groupId,
                    reminderOffsetsCsv = encodeReminderOffsets(offsets, DEFAULT_PLANNING_REMINDER_MINUTES),
                    reminderOffsetMinutes = offsets.firstOrNull(),
                    reminderAtMillis = offsets.minOrNull()?.let { candidate.startAt.minusMinutes(it.toLong()).toEpochMillis() },
                    reminderEnabled = offsets.isNotEmpty(),
                    missed = false,
                    missedAtMillis = null
                )
                updated
            }
            else -> null
        }
    }

    private fun postponeItem(item: TodoItem, offsetMinutes: Int): TodoItem {
        val delta = offsetMinutes * 60_000L
        return if (item.isEvent) {
            item.copy(
                dueAtMillis = item.dueAtMillis + delta,
                startAtMillis = item.startAtMillis?.plus(delta),
                endAtMillis = item.endAtMillis?.plus(delta),
                reminderAtMillis = item.reminderAtMillis?.plus(delta),
                missed = false,
                missedAtMillis = null
            )
        } else {
            val dueAtMillis = if (item.hasDueDate) item.dueAtMillis + delta else item.dueAtMillis
            item.copy(
                dueAtMillis = dueAtMillis,
                reminderAtMillis = item.reminderAtMillis?.plus(delta),
                missed = false,
                missedAtMillis = null
            )
        }
    }

    private fun isPlanningItemConflict(item: TodoItem, mapping: PlanningLineMapping): Boolean {
        val expected = PlanningMarkdownParser.parse(stripImportedMarker(mapping.trackedLineText.ifBlank { mapping.originalLineText })).candidates.firstOrNull { it.importable }
            ?: return false
        val titleChanged = item.title.trim() != expected.title.trim()
        val timeChanged = when {
            item.isTodo && expected.type == PlanningParsedType.TODO -> expected.dueAt?.toEpochMillis() != item.dueAtMillis.takeIf { item.hasDueDate }
            item.isEvent && expected.type == PlanningParsedType.EVENT -> expected.startAt?.toEpochMillis() != item.startAtMillis ||
                expected.endAt?.toEpochMillis() != item.endAtMillis
            else -> true
        }
        return titleChanged || timeChanged
    }

    private suspend fun undoPlanningImport(
        noteId: Long,
        markdown: String,
        batchId: String,
        batch: List<PlanningLineMapping>
    ): PlanningOperationResult {
        val affectedItems = batch.mapNotNull { it.itemId?.let { id -> todoDao.getById(id) } }
        val todoIds = affectedItems.filter { it.isTodo }.map { it.id }
        val eventItems = affectedItems.filter { it.isEvent }
        if (todoIds.isNotEmpty()) todoDao.deleteByIds(todoIds)
        eventItems.forEach { todoDao.deleteById(it.id) }
        if (affectedItems.isNotEmpty()) notifyItemsChanged()
        todoDao.deletePlanningMappingBatch(batchId)
        val updatedMarkdown = removeImportedMarkersForMappings(markdown, batch)
        updatePlanningNoteContent(noteId, updatedMarkdown)
        return PlanningOperationResult(
            message = "已撤销导入，删除 ${affectedItems.size} 条事项",
            updatedMarkdown = updatedMarkdown,
            affectedBeforeItems = affectedItems
        )
    }

    private suspend fun undoPlanningRefresh(batch: List<PlanningLineMapping>): PlanningOperationResult {
        val groups = getAllGroups().ifEmpty { ensureDefaultGroups() }
        val beforeItems = mutableListOf<TodoItem>()
        val afterItems = mutableListOf<TodoItem>()
        val updatedMappings = mutableListOf<PlanningLineMapping>()
        val batchId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        batch.forEach { mapping ->
            val item = mapping.itemId?.let { todoDao.getById(it) } ?: return@forEach
            val candidate = PlanningMarkdownParser.parse(mapping.originalLineText).candidates.firstOrNull { it.importable } ?: return@forEach
            val restored = updateItemFromPlanningCandidate(item, candidate, groups) ?: return@forEach
            beforeItems += item
            afterItems += restored
            val trackedLine = mapping.currentLineText.ifBlank { mapping.originalLineText }
            val staysAligned = PlanningLineMatcher.normalizeLine(trackedLine) == PlanningLineMatcher.normalizeLine(mapping.originalLineText)
            updatedMappings += mapping.copy(
                status = if (staysAligned) MappingStatus.ACTIVE else MappingStatus.CONFLICT,
                contentFingerprint = PlanningLineMatcher.fingerprint(trackedLine),
                batchId = batchId,
                operationType = "UNDO_REFRESH",
                postponeOffsetMinutes = 0,
                lastRefreshedAtMillis = now
            )
        }
        if (afterItems.isNotEmpty()) {
            todoDao.updateAll(afterItems)
            notifyItemsChanged()
        }
        if (updatedMappings.isNotEmpty()) todoDao.updatePlanningMappings(updatedMappings)
        val conflictCount = updatedMappings.count { it.status == MappingStatus.CONFLICT }
        return PlanningOperationResult(
            message = buildString {
                append("已撤销刷新，恢复 ${afterItems.size} 条事项")
                if (conflictCount > 0) append("，${conflictCount} 条因原文仍不同步而标记为冲突")
            },
            affectedBeforeItems = beforeItems,
            affectedAfterItems = afterItems,
            conflictCount = conflictCount,
            batchId = batchId.takeIf { updatedMappings.isNotEmpty() }
        )
    }

    private suspend fun undoPlanningPostpone(
        noteId: Long,
        markdown: String,
        batch: List<PlanningLineMapping>
    ): PlanningOperationResult {
        val lines = planningDocumentLines(markdown)
        val relocated = PlanningLineMatcher.relocateMappings(lines, batch)
        val mutableLines = lines.toMutableList()
        val beforeItems = mutableListOf<TodoItem>()
        val afterItems = mutableListOf<TodoItem>()
        val updatedMappings = mutableListOf<PlanningLineMapping>()
        val batchId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        batch.forEach { mapping ->
            val item = mapping.itemId?.let { todoDao.getById(it) } ?: return@forEach
            val offset = mapping.postponeOffsetMinutes
            if (offset == 0) return@forEach
            val restored = postponeItem(item, -offset)
            relocated[mapping.id]?.let { lineIndex ->
                if (lineIndex in mutableLines.indices) {
                    mutableLines[lineIndex] = shiftPlanningLineTimeText(mutableLines[lineIndex], -offset)
                }
            }
            beforeItems += item
            afterItems += restored
            val resolvedLineIndex = relocated[mapping.id]
            val restoredLine = resolvedLineIndex?.let { mutableLines.getOrNull(it) }.orEmpty()
            updatedMappings += mapping.copy(
                currentLineText = restoredLine.ifBlank { mapping.currentLineText },
                contentFingerprint = PlanningLineMatcher.fingerprint(restoredLine.ifBlank { mapping.currentLineText }),
                batchId = batchId,
                postponeOffsetMinutes = 0,
                operationType = "UNDO_POSTPONE",
                lastKnownLineNumber = resolvedLineIndex?.plus(1) ?: mapping.lastKnownLineNumber,
                lastRefreshedAtMillis = now,
                status = if (resolvedLineIndex == null) MappingStatus.ORPHANED else MappingStatus.ACTIVE
            )
        }
        if (afterItems.isNotEmpty()) {
            todoDao.updateAll(afterItems)
            notifyItemsChanged()
        }
        if (updatedMappings.isNotEmpty()) todoDao.updatePlanningMappings(updatedMappings)
        val updatedMarkdown = joinPlanningDocumentLines(markdown, mutableLines)
        if (afterItems.isNotEmpty()) updatePlanningNoteContent(noteId, updatedMarkdown)
        return PlanningOperationResult(
            message = "已撤销顺延，恢复 ${afterItems.size} 条事项",
            updatedMarkdown = updatedMarkdown.takeIf { afterItems.isNotEmpty() },
            affectedBeforeItems = beforeItems,
            affectedAfterItems = afterItems,
            batchId = batchId.takeIf { updatedMappings.isNotEmpty() }
        )
    }

    private suspend fun resolvePlanningGroupId(groupName: String, groups: List<TaskGroup>, fallbackGroupId: Long): Long {
        if (groupName.isNotBlank()) {
            groups.firstOrNull { it.name.equals(groupName.trim(), ignoreCase = true) }?.let { return it.id }
            return createGroup(groupName.trim(), "#4E87E1").id
        }
        return fallbackGroupId.takeIf { it > 0 } ?: groups.firstOrNull { it.name == "例行" }?.id ?: groups.firstOrNull()?.id ?: 0L
    }

    private fun planningDocumentLines(markdown: String): List<String> {
        return markdown.replace("\r\n", "\n").replace('\r', '\n').lines().let { lines ->
            if (lines.size > 1 && lines.last().isEmpty() && markdown.endsWith("\n")) lines.dropLast(1) else lines
        }
    }

    private fun joinPlanningDocumentLines(originalMarkdown: String, lines: List<String>): String {
        val joined = lines.joinToString("\n")
        return if ((originalMarkdown.endsWith("\n") || originalMarkdown.endsWith("\r")) && !joined.endsWith("\n")) "$joined\n" else joined
    }

    private fun planningSectionRange(lines: List<String>, cursorLineNumber: Int?): IntRange {
        if (lines.isEmpty()) return IntRange.EMPTY
        val cursor = ((cursorLineNumber ?: 1) - 1).coerceIn(lines.indices)
        val start = (cursor downTo 0).firstOrNull { lines[it].trimStart().startsWith("#") } ?: 0
        val end = ((start + 1)..lines.lastIndex).firstOrNull { lines[it].trimStart().startsWith("#") }?.minus(1) ?: lines.lastIndex
        return start..end
    }

    private fun shiftPlanningLineTimeText(line: String, offsetMinutes: Int): String {
        val timeRegex = Regex("(凌晨|早上|上午|中午|下午|晚上)?\\s*(\\d{1,2})[:：](\\d{2})")
        return timeRegex.replace(line) { match ->
            val period = match.groupValues[1]
            val hour = match.groupValues[2].toIntOrNull() ?: return@replace match.value
            val minute = match.groupValues[3].toIntOrNull() ?: return@replace match.value
            val base = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)).plusMinutes(offsetMinutes.toLong())
            if (period.isBlank()) "%02d:%02d".format(base.hour, base.minute) else "$period %02d:%02d".format(base.hour, base.minute)
        }
    }

    private fun removeImportedMarkersByLine(markdown: String, lineNumbers: Set<Int>): String {
        if (lineNumbers.isEmpty()) return markdown
        val hasTrailingNewline = markdown.endsWith("\n") || markdown.endsWith("\r")
        val updated = planningDocumentLines(markdown).mapIndexed { index, line ->
            if (index + 1 in lineNumbers) {
                line.replace(Regex("\\s+#imported(?=\\s|$)"), "")
            } else {
                line
            }
        }.joinToString("\n")
        return if (hasTrailingNewline && !updated.endsWith("\n")) "$updated\n" else updated
    }

    private fun removeImportedMarkersForMappings(markdown: String, mappings: List<PlanningLineMapping>): String {
        if (mappings.isEmpty()) return markdown
        val lines = planningDocumentLines(markdown)
        if (lines.isEmpty()) return markdown
        val relocated = PlanningLineMatcher.relocateMappings(lines, mappings)
        val targetLineNumbers = mappings.mapNotNull { mapping ->
            relocated[mapping.id]?.plus(1)
                ?: mapping.lastKnownLineNumber.takeIf { it > 0 && it <= lines.size }
        }.toSet()
        return removeImportedMarkersByLine(markdown, targetLineNumbers)
    }

    private fun markdownWithoutImportedMarkers(markdown: String): String {
        return planningDocumentLines(markdown).joinToString("\n") { stripImportedMarker(it) }
    }

    private fun stripImportedMarker(line: String): String {
        return line.replace(Regex("\\s+#imported(?=\\s|$)"), "")
    }

    private fun planningLineFromItem(item: TodoItem, groupName: String): String {
        val groupPart = groupName.takeIf { it.isNotBlank() }?.let { " #group $it" }.orEmpty()
        return if (item.isEvent) {
            val start = item.startAtMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
            val end = item.endAtMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
            val startLabel = start?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) ?: ""
            val endLabel = end?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""
            "- [ ] $startLabel-$endLabel ${item.title}$groupPart #imported".trim()
        } else {
            val ddl = item.dueDateTimeOrNull()?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            val ddlPart = ddl?.let { " #ddl $it" }.orEmpty()
            "- [ ] ${item.title}$ddlPart$groupPart #imported".trim()
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
            RecurrenceScope.CURRENT -> {
                val current = targets.first()
                val updated = buildTaskItem(
                    draft = draft.copy(groupId = draft.groupId.takeIf { it > 0 } ?: current.groupId),
                    now = current.createdAtMillis,
                    existing = current,
                    keepSeries = false
                )
                todoDao.update(updated)
                listOf(updated)
            }
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
            RecurrenceScope.CURRENT -> {
                val current = targets.first()
                val updated = buildCalendarEventItem(
                    draft = draft,
                    now = current.createdAtMillis,
                    existing = current,
                    keepSeries = false
                )
                todoDao.update(updated)
                listOf(updated)
            }
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
        requireNotNull(dueAt) { "Recurring todo requires DDL" }
        val offsetMinutes = normalizedReminderOffsetsMinutes.firstOrNull()
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
        val normalizedOffsets = if (draft.dueAt == null) emptyList() else draft.normalizedReminderOffsetsMinutes
        val reminderAtMillis = if (draft.dueAt == null) null else normalizedOffsets
            .map { dueAtMillis - it * 60_000L }
            .minOrNull()
        val offsetMinutes = normalizedOffsets.firstOrNull()
        val reminderOffsetsCsv = encodeReminderOffsets(normalizedOffsets)
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
            countdownEnabled = draft.countdownEnabled && draft.dueAt != null,
            reminderAtMillis = reminderAtMillis,
            reminderOffsetsCsv = reminderOffsetsCsv,
            reminderEnabled = normalizedOffsets.isNotEmpty(),
            ringEnabled = draft.ringEnabled,
            vibrateEnabled = draft.vibrateEnabled,
            voiceEnabled = false,
            reminderDeliveryMode = draft.reminderDeliveryMode.name,
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
        val normalizedOffsets = draft.normalizedReminderOffsetsMinutes
        val reminderAtMillis = draft.reminderTriggerTimesMillis().minOrNull()
        val offsetMinutes = reminderAtMillis?.let { ((eventStartAtMillis - it) / 60_000L).toInt() }
        val reminderOffsetsCsv = encodeReminderOffsets(normalizedOffsets, draft.reminderMinutesBefore)
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
            countdownEnabled = draft.countdownEnabled,
            reminderAtMillis = reminderAtMillis,
            reminderOffsetsCsv = reminderOffsetsCsv,
            reminderEnabled = normalizedOffsets.isNotEmpty(),
            ringEnabled = draft.ringEnabled,
            vibrateEnabled = draft.vibrateEnabled,
            voiceEnabled = false,
            reminderDeliveryMode = draft.reminderDeliveryMode.name,
            groupId = draft.groupId,
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
        val normalizedOffsets = draft.normalizedReminderOffsetsMinutes
        val lunarDate = LunarCalendar.labelFor(dueDate)
        return RecurringTaskTemplate(
            id = 0,
            seriesId = item.recurringSeriesId ?: UUID.randomUUID().toString(),
            itemType = PlannerItemType.TODO.name,
            title = draft.title.trim(),
            notes = draft.notes.trim(),
            location = "",
            accentColorHex = null,
            countdownEnabled = draft.countdownEnabled,
            allDay = false,
            groupId = draft.groupId,
            dueHour = dueAt.hour,
            dueMinute = dueAt.minute,
            eventDurationMinutes = null,
            reminderOffsetMinutes = normalizedOffsets.firstOrNull(),
            reminderOffsetsCsv = encodeReminderOffsets(normalizedOffsets),
            ringEnabled = draft.ringEnabled,
            vibrateEnabled = draft.vibrateEnabled,
            reminderDeliveryMode = draft.reminderDeliveryMode.name,
            recurrenceType = draft.recurrence.type.name,
            recurrenceWeekdays = draft.recurrence.weeklyDays.toStorageString(),
            recurrenceMonthlyOrdinal = if (draft.recurrence.type == RecurrenceType.MONTHLY_NTH_WEEKDAY) dueDate.nthWeekOrdinal() else null,
            recurrenceMonthlyWeekday = if (draft.recurrence.type == RecurrenceType.MONTHLY_NTH_WEEKDAY) dueDate.dayOfWeek.value else null,
            recurrenceMonthlyDay = if (draft.recurrence.type == RecurrenceType.MONTHLY_DAY) dueDate.dayOfMonth else null,
            recurrenceYearlyMonth = when (draft.recurrence.type) {
                RecurrenceType.YEARLY_DATE -> dueDate.monthValue
                RecurrenceType.YEARLY_LUNAR_DATE -> lunarDate.month
                else -> null
            },
            recurrenceYearlyDay = when (draft.recurrence.type) {
                RecurrenceType.YEARLY_DATE -> dueDate.dayOfMonth
                RecurrenceType.YEARLY_LUNAR_DATE -> lunarDate.day
                else -> null
            },
            startEpochDay = dueDate.toEpochDay(),
            endEpochDay = draft.recurrence.endDate?.toEpochDay() ?: dueDate.toEpochDay()
        )
    }

    private fun buildCalendarTemplate(item: TodoItem, draft: CalendarEventDraft): RecurringTaskTemplate {
        val startDate = draft.startAt.toLocalDate()
        val lunarDate = LunarCalendar.labelFor(startDate)
        return RecurringTaskTemplate(
            id = 0,
            seriesId = item.recurringSeriesId ?: UUID.randomUUID().toString(),
            itemType = PlannerItemType.EVENT.name,
            title = draft.title.trim(),
            notes = draft.notes.trim(),
            location = draft.location.trim(),
            accentColorHex = draft.accentColorHex,
            countdownEnabled = draft.countdownEnabled,
            allDay = draft.allDay,
            groupId = draft.groupId,
            dueHour = draft.startAt.hour,
            dueMinute = draft.startAt.minute,
            eventDurationMinutes = Duration.between(draft.startAt, draft.endAt).toMinutes().coerceAtLeast(30).toInt(),
            reminderOffsetMinutes = draft.reminderMinutesBefore,
            reminderOffsetsCsv = encodeReminderOffsets(draft.normalizedReminderOffsetsMinutes, draft.reminderMinutesBefore),
            ringEnabled = draft.ringEnabled,
            vibrateEnabled = draft.vibrateEnabled,
            reminderDeliveryMode = draft.reminderDeliveryMode.name,
            recurrenceType = draft.recurrence.type.name,
            recurrenceWeekdays = draft.recurrence.weeklyDays.toStorageString(),
            recurrenceMonthlyOrdinal = if (draft.recurrence.type == RecurrenceType.MONTHLY_NTH_WEEKDAY) startDate.nthWeekOrdinal() else null,
            recurrenceMonthlyWeekday = if (draft.recurrence.type == RecurrenceType.MONTHLY_NTH_WEEKDAY) startDate.dayOfWeek.value else null,
            recurrenceMonthlyDay = if (draft.recurrence.type == RecurrenceType.MONTHLY_DAY) startDate.dayOfMonth else null,
            recurrenceYearlyMonth = when (draft.recurrence.type) {
                RecurrenceType.YEARLY_DATE -> startDate.monthValue
                RecurrenceType.YEARLY_LUNAR_DATE -> lunarDate.month
                else -> null
            },
            recurrenceYearlyDay = when (draft.recurrence.type) {
                RecurrenceType.YEARLY_DATE -> startDate.dayOfMonth
                RecurrenceType.YEARLY_LUNAR_DATE -> lunarDate.day
                else -> null
            },
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
        val offsetMinutes = draft.normalizedReminderOffsetsMinutes.firstOrNull()

        val dates = when (config.type) {
            RecurrenceType.NONE -> listOf(dueDate)
            RecurrenceType.DAILY -> generateDailyDates(dueDate, endDate)
            RecurrenceType.WEEKLY -> generateWeeklyDates(dueDate, endDate, config.weeklyDays.ifEmpty { setOf(dueDate.dayOfWeek) })
            RecurrenceType.MONTHLY_NTH_WEEKDAY -> generateNthWeekdayDates(dueDate, endDate)
            RecurrenceType.MONTHLY_DAY -> generateMonthlyDayDates(dueDate, endDate)
            RecurrenceType.YEARLY_DATE -> generateYearlyDates(dueDate, endDate)
            RecurrenceType.YEARLY_LUNAR_DATE -> generateYearlyLunarDates(dueDate, endDate)
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
            RecurrenceType.YEARLY_LUNAR_DATE -> generateYearlyLunarDates(startDate, endDate)
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

    private fun generateYearlyLunarDates(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = linkedSetOf<LocalDate>()
        var year = start.year
        while (year <= end.year + 1) {
            val candidate = LunarCalendar.sameLunarDateInYear(start, year)
            if (candidate != null && !candidate.isBefore(start) && !candidate.isAfter(end)) {
                dates += candidate
            }
            year += 1
        }
        return dates.sorted()
    }

    companion object {
        private const val MISSED_GRACE_PERIOD_MILLIS = 60_000L
    }

    private suspend fun defaultGroupId(): Long {
        val groups = ensureDefaultGroups()
        return groups.firstOrNull { it.name == "例行" }?.id ?: groups.firstOrNull()?.id ?: 0L
    }

    private suspend fun resolveCalendarGroupId(draft: CalendarEventDraft): Long {
        return when {
            draft.groupId > 0 -> draft.groupId
            draft.groupName.isNotBlank() -> resolveGroupIdByNameOrCreate(draft.groupName)
            else -> defaultGroupId()
        }
    }

    private suspend fun resolveGroupIdByNameOrCreate(groupName: String): Long {
        val normalized = groupName.trim()
        if (normalized.isBlank()) return defaultGroupId()
        val groups = ensureDefaultGroups()
        val existing = groups.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
        if (existing != null) return existing.id
        return createGroup(normalized, "#4E87E1").id
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

    private fun notifyItemsChanged() {
        onItemsChanged?.invoke()
    }

    private fun todayRangeMillis(date: LocalDate = LocalDate.now()): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }
}
