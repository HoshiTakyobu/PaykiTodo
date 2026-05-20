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
import kotlin.math.roundToInt

class TodoRepository(
    private val todoDao: TodoDao,
    private val onItemsChanged: (() -> Unit)? = null
) {
    enum class GroupFilterMode {
        INTERSECTION,
        UNION
    }

    fun observeTodos(): Flow<List<TodoItem>> = todoDao.observeTodos()
    fun observeActiveTodoItems(groupId: Long?): Flow<List<TodoItem>> = observeActiveTodoItems(groupId?.let(::setOf).orEmpty())
    fun observeActiveTodoItems(
        groupIds: Set<Long>,
        mode: GroupFilterMode = GroupFilterMode.INTERSECTION
    ): Flow<List<TodoItem>> {
        val ids = groupIds.filter { it > 0 }.distinct().sorted()
        return when (ids.size) {
            0 -> todoDao.observeActiveTodoItems(null)
            else -> when (mode) {
                GroupFilterMode.INTERSECTION -> todoDao.observeActiveTodoItemsByGroupIntersection(ids, ids.size)
                GroupFilterMode.UNION -> todoDao.observeActiveTodoItemsByGroupUnion(ids)
            }
        }
    }
    fun observeHistoryTodoItems(groupId: Long?): Flow<List<TodoItem>> = observeHistoryTodoItems(groupId?.let(::setOf).orEmpty())
    fun observeHistoryTodoItems(
        groupIds: Set<Long>,
        mode: GroupFilterMode = GroupFilterMode.INTERSECTION
    ): Flow<List<TodoItem>> {
        val ids = groupIds.filter { it > 0 }.distinct().sorted()
        return when (ids.size) {
            0 -> todoDao.observeHistoryTodoItems(null)
            else -> when (mode) {
                GroupFilterMode.INTERSECTION -> todoDao.observeHistoryTodoItemsByGroupIntersection(ids, ids.size)
                GroupFilterMode.UNION -> todoDao.observeHistoryTodoItemsByGroupUnion(ids)
            }
        }
    }
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

    suspend fun addTodo(item: TodoItem): TodoItem {
        val id = todoDao.insert(item)
        notifyItemsChanged()
        return item.copy(id = id)
    }

    suspend fun getTodo(id: Long): TodoItem? = todoDao.getById(id)
    suspend fun getGroup(groupId: Long): TaskGroup? = todoDao.getGroupById(groupId)
    suspend fun getGroupIdsForTodo(todoId: Long): List<Long> = todoDao.getGroupIdsForTodo(todoId)
    suspend fun getAllTodoGroupTags(): List<TodoGroupTag> = todoDao.getAllTodoGroupTags()
    suspend fun getCheckInsForEvent(eventId: Long): List<EventCheckIn> = todoDao.getCheckInsForEvent(eventId)
    suspend fun getActiveCheckIn(eventId: Long): EventCheckIn? = todoDao.getActiveCheckIn(eventId)
    suspend fun getAllActiveEventCheckIns(): List<EventCheckIn> = todoDao.getAllActiveEventCheckIns()
    suspend fun getActiveCheckInsForEvents(eventIds: List<Long>): List<EventCheckIn> {
        val ids = eventIds.filter { it > 0 }.distinct()
        return if (ids.isEmpty()) {
            emptyList()
        } else {
            todoDao.getActiveCheckInsForEvents(ids).distinctBy { it.eventId }
        }
    }
    suspend fun getTodayEventCheckInMinutes(date: LocalDate = LocalDate.now()): Int {
        val zone = ZoneId.systemDefault()
        val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return todoDao.getTotalCheckInMinutesInRange(startMillis, endMillis)
    }
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
    suspend fun getCompletedTodosInRange(startMillis: Long, endMillis: Long): List<TodoItem> {
        return todoDao.getCompletedTodosInRange(startMillis, endMillis)
    }
    suspend fun getMissedTodosDueInRange(startMillis: Long, endMillis: Long): List<TodoItem> {
        return todoDao.getMissedTodosDueInRange(startMillis, endMillis)
    }
    suspend fun getActiveTodosDueInRange(startMillis: Long, endMillis: Long): List<TodoItem> {
        return todoDao.getActiveTodosDueInRange(startMillis, endMillis, NO_DUE_DATE_MILLIS)
    }
    suspend fun getActiveEventsOverlappingRange(rangeStartMillis: Long, rangeEndMillis: Long): List<TodoItem> {
        return todoDao.getActiveEventsOverlappingRange(rangeStartMillis, rangeEndMillis)
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
    suspend fun getPlanningNote(noteId: Long): PlanningNote? = todoDao.getPlanningNote(noteId)
    suspend fun getPlanningNoteByDocumentDate(date: LocalDate): PlanningNote? = todoDao.getActivePlanningNoteByDocumentDate(date.toEpochDay())
    suspend fun getActivePlanningNotes(): List<PlanningNote> = todoDao.getActivePlanningNotes()
    suspend fun getPlanningNotesWithAnnouncementHints(): List<PlanningNote> = todoDao.getPlanningNotesWithAnnouncementHints()
    suspend fun getPlanningMappingsForNote(noteId: Long): List<PlanningLineMapping> = todoDao.getMappingsForNote(noteId)
    fun observePlanningNodesForNote(noteId: Long): Flow<List<PlanningNode>> = todoDao.observePlanningNodesForNote(noteId)
    suspend fun getPlanningNodesForNote(noteId: Long): List<PlanningNode> = todoDao.getPlanningNodesForNote(noteId)
    suspend fun getAllPlanningNodes(): List<PlanningNode> = todoDao.getAllPlanningNodes()
    suspend fun getPlanningNode(nodeId: Long): PlanningNode? = todoDao.getPlanningNode(nodeId)
    suspend fun getPlanningNodesByLinkedTodo(linkedTodoId: Long): List<PlanningNode> = todoDao.getPlanningNodesByLinkedTodo(linkedTodoId)

    suspend fun insertPlanningMappings(mappings: List<PlanningLineMapping>) {
        if (mappings.isNotEmpty()) todoDao.insertPlanningMappings(mappings)
    }

    suspend fun insertPlanningNodes(nodes: List<PlanningNode>) {
        if (nodes.isNotEmpty()) todoDao.insertPlanningNodes(nodes)
    }

    suspend fun ensurePlanningNodeLinkedItems(createEventEndTodo: Boolean = false): List<TodoItem> {
        val affected = mutableListOf<TodoItem>()
        val allNodes = todoDao.getAllPlanningNodes()
        val childrenByParent = allNodes.groupBy { it.parentNodeId }
        allNodes.forEach { node ->
            if (node.isDraft) return@forEach
            if (childrenByParent[node.id].orEmpty().isNotEmpty()) {
                val deleted = demotePlanningNodeToStructureIfNeeded(node, System.currentTimeMillis())
                deleteItemsByIds(deleted.map { it.id })
                return@forEach
            }
            if (!node.syncEnabled) return@forEach
            val linkedItemMissing = node.linkedTodoId?.let { todoDao.getById(it) } == null
            val endTodoMissing = createEventEndTodo &&
                node.startAtMillis != null &&
                node.endAtMillis != null &&
                node.linkedEndTodoId?.let { todoDao.getById(it) } == null
            if (!linkedItemMissing && !endTodoMissing) return@forEach
            val result = updatePlanningNode(
                nodeId = node.id,
                edit = PlanningNodeEdit(
                    text = node.text,
                    parentNodeId = node.parentNodeId,
                    sortOrder = node.sortOrder,
                    dueAt = node.dueAtMillis?.toPlanningLocalDateTime(),
                    startAt = node.startAtMillis?.toPlanningLocalDateTime(),
                    endAt = node.endAtMillis?.toPlanningLocalDateTime(),
                    location = node.location,
                    syncEnabled = node.syncEnabled,
                    collapsed = node.collapsed,
                    completed = node.completed
                ),
                createEventEndTodo = createEventEndTodo
            )
            affected += result?.affectedLinkedItems.orEmpty()
        }
        return affected.distinctBy { it.id }
    }

    suspend fun createPlanningNode(
        draft: PlanningNodeDraft,
        createEventEndTodo: Boolean = false
    ): PlanningNodeChangeResult? {
        val note = todoDao.getPlanningNote(draft.noteId) ?: return null
        val parentId = draft.parentNodeId?.takeIf { parentId ->
            todoDao.getPlanningNode(parentId)?.noteId == note.id
        }
        val resolved = resolvePlanningNodeDraft(
            note = note,
            text = draft.text,
            notes = draft.notes,
            groupId = draft.groupId,
            groupName = draft.groupName,
            dueAt = draft.dueAt,
            startAt = draft.startAt,
            endAt = draft.endAt,
            location = draft.location,
            reminderOffsetsMinutes = draft.reminderOffsetsMinutes,
            allDay = draft.allDay,
            countdownEnabled = draft.countdownEnabled,
            checkInEnabled = draft.checkInEnabled
        ) ?: return null
        val sortOrder = draft.sortOrder
            ?: (todoDao.getMaxPlanningNodeSortOrder(note.id, parentId) + 1)
        if (draft.sortOrder != null) {
            shiftPlanningNodeSiblingsForInsert(
                noteId = note.id,
                parentNodeId = parentId,
                fromSortOrder = sortOrder
            )
        }
        val now = System.currentTimeMillis()
        val effectiveSyncEnabled = draft.syncEnabled && !draft.isDraft
        val linked = if (effectiveSyncEnabled) {
            createLinkedItemForPlanningNode(resolved, completed = draft.completed, now = now)
        } else {
            null
        }
        val endTodo = createPlanningEventEndTodo(
            resolved = resolved,
            completed = draft.completed,
            now = now,
            enabled = effectiveSyncEnabled && createEventEndTodo
        )
        val node = PlanningNode(
            noteId = note.id,
            parentNodeId = parentId,
            sortOrder = sortOrder,
            text = resolved.title,
            createdAtMillis = now,
            updatedAtMillis = now,
            startAtMillis = resolved.startAt?.toEpochMillis(),
            endAtMillis = resolved.endAt?.toEpochMillis(),
            dueAtMillis = if (resolved.type == PlanningNodeResolvedType.TODO) resolved.dueAt?.toEpochMillis() else null,
            location = resolved.location,
            linkedTodoId = linked?.id,
            linkedEndTodoId = endTodo?.id,
            isDraft = draft.isDraft,
            syncEnabled = draft.syncEnabled,
            collapsed = draft.collapsed,
            completed = draft.completed,
            completedAtMillis = if (draft.completed) now else null
        )
        val id = todoDao.insertPlanningNode(node)
        val createdNode = node.copy(id = id)
        val deletedParentLinkedItems = demotePlanningParentIfNeeded(parentId, now)
        deleteItemsByIds(deletedParentLinkedItems.map { it.id })
        updatePlanningNoteNodeLegacyMarkdown(note.id)
        notifyItemsChanged()
        return PlanningNodeChangeResult(
            node = createdNode,
            linkedItem = linked,
            deletedLinkedItems = deletedParentLinkedItems,
            affectedLinkedItems = listOfNotNull(linked, endTodo)
        )
    }

    suspend fun updatePlanningNode(
        nodeId: Long,
        edit: PlanningNodeEdit,
        createEventEndTodo: Boolean = false
    ): PlanningNodeChangeResult? {
        val existing = todoDao.getPlanningNode(nodeId) ?: return null
        val note = todoDao.getPlanningNote(existing.noteId) ?: return null
        val parentId = edit.parentNodeId?.takeIf { parentId ->
            parentId != existing.id && todoDao.getPlanningNode(parentId)?.noteId == note.id
        }
        val allNodesBefore = todoDao.getPlanningNodesForNote(note.id)
        val hasChildren = allNodesBefore.any { it.parentNodeId == existing.id }
        val effectiveSyncEnabled = edit.syncEnabled && !hasChildren && !existing.isDraft
        val resolved = resolvePlanningNodeDraft(
            note = note,
            text = edit.text,
            dueAt = edit.dueAt,
            startAt = edit.startAt,
            endAt = edit.endAt,
            location = edit.location
        ) ?: return null
        val now = System.currentTimeMillis()
        val previousLinked = existing.linkedTodoId?.let { todoDao.getById(it) }
        val previousEndTodo = existing.linkedEndTodoId?.let { todoDao.getById(it) }
        val sync = if (effectiveSyncEnabled) {
            upsertLinkedItemForPlanningNode(previousLinked, resolved, edit.completed, now)
        } else {
            PlanningNodeLinkedSync(linkedItem = null, deletedLinkedItem = previousLinked)
        }
        val endTodoSync = upsertPlanningEventEndTodo(
            previous = previousEndTodo,
            resolved = resolved,
            completed = edit.completed,
            now = now,
            enabled = effectiveSyncEnabled && createEventEndTodo
        )
        val updated = existing.copy(
            parentNodeId = parentId,
            sortOrder = edit.sortOrder,
            text = resolved.title,
            updatedAtMillis = now,
            startAtMillis = resolved.startAt?.toEpochMillis(),
            endAtMillis = resolved.endAt?.toEpochMillis(),
            dueAtMillis = if (resolved.type == PlanningNodeResolvedType.TODO) resolved.dueAt?.toEpochMillis() else null,
            location = resolved.location,
            linkedTodoId = sync.linkedItem?.id,
            linkedEndTodoId = endTodoSync.linkedItem?.id,
            syncEnabled = effectiveSyncEnabled,
            collapsed = edit.collapsed,
            completed = edit.completed,
            completedAtMillis = when {
                edit.completed && !existing.completed -> now
                edit.completed -> existing.completedAtMillis ?: now
                else -> null
            }
        )
        todoDao.updatePlanningNode(updated)
        val deletedParentLinkedItems = demotePlanningParentIfNeeded(parentId, now)
        val restoredOldParent = if (existing.parentNodeId != parentId) {
            restorePlanningParentIfLeaf(existing.parentNodeId, createEventEndTodo, now)
        } else {
            null
        }
        val deletedItems = (listOfNotNull(sync.deletedLinkedItem, endTodoSync.deletedLinkedItem) + deletedParentLinkedItems)
            .distinctBy { it.id }
        deleteItemsByIds(deletedItems.map { it.id })
        updatePlanningNoteNodeLegacyMarkdown(note.id)
        notifyItemsChanged()
        return PlanningNodeChangeResult(
            node = updated,
            linkedItem = sync.linkedItem,
            deletedLinkedItem = sync.deletedLinkedItem,
            deletedLinkedItems = deletedItems,
            affectedLinkedItems = (listOfNotNull(sync.linkedItem, endTodoSync.linkedItem) + restoredOldParent.orEmpty())
                .distinctBy { it.id }
        )
    }

    suspend fun publishPlanningNode(
        nodeId: Long,
        createEventEndTodo: Boolean = false
    ): PlanningNodeChangeResult? {
        val existing = todoDao.getPlanningNode(nodeId) ?: return null
        if (!existing.isDraft) {
            val linked = existing.linkedTodoId?.let { todoDao.getById(it) }
            return PlanningNodeChangeResult(node = existing, linkedItem = linked)
        }
        val note = todoDao.getPlanningNote(existing.noteId) ?: return null
        val allNodes = todoDao.getPlanningNodesForNote(note.id)
        val hasChildren = allNodes.any { it.parentNodeId == existing.id }
        val resolved = resolvePlanningNodeDraft(
            note = note,
            text = existing.text,
            dueAt = existing.dueAtMillis?.toPlanningLocalDateTime(),
            startAt = existing.startAtMillis?.toPlanningLocalDateTime(),
            endAt = existing.endAtMillis?.toPlanningLocalDateTime(),
            location = existing.location
        ) ?: return null
        val now = System.currentTimeMillis()
        val effectiveSyncEnabled = existing.syncEnabled && !hasChildren
        val previousLinked = existing.linkedTodoId?.let { todoDao.getById(it) }
        val previousEndTodo = existing.linkedEndTodoId?.let { todoDao.getById(it) }
        val sync = if (effectiveSyncEnabled) {
            upsertLinkedItemForPlanningNode(previousLinked, resolved, existing.completed, now)
        } else {
            PlanningNodeLinkedSync(linkedItem = null, deletedLinkedItem = previousLinked)
        }
        val endTodoSync = upsertPlanningEventEndTodo(
            previous = previousEndTodo,
            resolved = resolved,
            completed = existing.completed,
            now = now,
            enabled = effectiveSyncEnabled && createEventEndTodo
        )
        val updated = existing.copy(
            text = resolved.title,
            updatedAtMillis = now,
            startAtMillis = resolved.startAt?.toEpochMillis(),
            endAtMillis = resolved.endAt?.toEpochMillis(),
            dueAtMillis = if (resolved.type == PlanningNodeResolvedType.TODO) resolved.dueAt?.toEpochMillis() else null,
            location = resolved.location,
            linkedTodoId = sync.linkedItem?.id,
            linkedEndTodoId = endTodoSync.linkedItem?.id,
            isDraft = false,
            syncEnabled = effectiveSyncEnabled
        )
        todoDao.updatePlanningNode(updated)
        val deletedItems = listOfNotNull(sync.deletedLinkedItem, endTodoSync.deletedLinkedItem).distinctBy { it.id }
        deleteItemsByIds(deletedItems.map { it.id })
        updatePlanningNoteNodeLegacyMarkdown(note.id)
        notifyItemsChanged()
        return PlanningNodeChangeResult(
            node = updated,
            linkedItem = sync.linkedItem,
            deletedLinkedItem = sync.deletedLinkedItem,
            deletedLinkedItems = deletedItems,
            affectedLinkedItems = listOfNotNull(sync.linkedItem, endTodoSync.linkedItem).distinctBy { it.id }
        )
    }

    suspend fun publishAllPlanningDrafts(
        noteId: Long,
        createEventEndTodo: Boolean = false
    ): PlanningNodePublishBatchResult {
        val draftNodes = todoDao.getPlanningNodesForNote(noteId).filter { it.isDraft }
        val published = mutableListOf<PlanningNodeChangeResult>()
        var failedCount = 0
        draftNodes.forEach { node ->
            val result = publishPlanningNode(node.id, createEventEndTodo)
            if (result != null && !result.node.isDraft) {
                published += result
            } else {
                failedCount += 1
            }
        }
        return PlanningNodePublishBatchResult(published = published, failedCount = failedCount)
    }

    suspend fun togglePlanningNodeCompleted(nodeId: Long, completed: Boolean? = null): PlanningNodeChangeResult? {
        val node = todoDao.getPlanningNode(nodeId) ?: return null
        val nextCompleted = completed ?: !node.completed
        val now = System.currentTimeMillis()
        val allNodes = todoDao.getPlanningNodesForNote(node.noteId)
        val targetNodes = if (nextCompleted) {
            planningNodeSubtree(node, allNodes)
        } else {
            listOf(node)
        }
        val updatedNodes = targetNodes.map { target ->
            target.copy(
                completed = nextCompleted,
                completedAtMillis = if (nextCompleted) now else null,
                updatedAtMillis = now
            )
        }
        todoDao.updatePlanningNodes(updatedNodes)
        val linked = updatedNodes
            .flatMap { target -> target.linkedItemIds() }
            .distinct()
            .mapNotNull { id -> updateLinkedItemCompletion(id, nextCompleted, now) }
            .toMutableList()
        linked += updateAncestorCompletionStates(
            changedNode = updatedNodes.firstOrNull { it.id == node.id } ?: node,
            allNodes = allNodes,
            now = now
        )
        updatePlanningNoteNodeLegacyMarkdown(node.noteId)
        notifyItemsChanged()
        return PlanningNodeChangeResult(
            node = updatedNodes.firstOrNull { it.id == node.id } ?: node.copy(
                completed = nextCompleted,
                completedAtMillis = if (nextCompleted) now else null,
                updatedAtMillis = now
            ),
            linkedItem = linked.firstOrNull { it.id == node.linkedTodoId },
            affectedLinkedItems = linked
        )
    }

    suspend fun deletePlanningNodeTree(
        nodeId: Long,
        createEventEndTodo: Boolean = false
    ): PlanningNodeDeleteResult {
        val root = todoDao.getPlanningNode(nodeId) ?: return PlanningNodeDeleteResult()
        val allNodes = todoDao.getPlanningNodesForNote(root.noteId)
        val childrenByParent = allNodes.groupBy { it.parentNodeId }
        val targetNodes = buildList {
            fun visit(node: PlanningNode) {
                add(node)
                childrenByParent[node.id].orEmpty().forEach(::visit)
            }
            visit(root)
        }
        val linkedItems = targetNodes.flatMap { it.linkedItemIds() }.distinct().mapNotNull { todoDao.getById(it) }
        val parentId = root.parentNodeId
        deleteItemsByIds(linkedItems.map { it.id })
        todoDao.deletePlanningNode(nodeId)
        val restoredParentItems = restorePlanningParentIfLeaf(
            parentId = parentId,
            createEventEndTodo = createEventEndTodo,
            now = System.currentTimeMillis()
        )
        updatePlanningNoteNodeLegacyMarkdown(root.noteId)
        notifyItemsChanged()
        return PlanningNodeDeleteResult(
            deletedLinkedItems = linkedItems.distinctBy { it.id },
            affectedLinkedItems = restoredParentItems.orEmpty().distinctBy { it.id }
        )
    }

    suspend fun reorderPlanningNodes(noteId: Long, parentNodeId: Long?, orderedNodeIds: List<Long>) {
        val nodes = todoDao.getPlanningNodesForNote(noteId).associateBy { it.id }
        val parentId = parentNodeId?.takeIf { nodes[it]?.noteId == noteId }
        val updates = orderedNodeIds.distinct().mapIndexedNotNull { index, nodeId ->
            nodes[nodeId]?.takeIf { it.noteId == noteId }?.copy(parentNodeId = parentId, sortOrder = index, updatedAtMillis = System.currentTimeMillis())
        }
        if (updates.isEmpty()) return
        todoDao.updatePlanningNodes(updates)
        updatePlanningNoteNodeLegacyMarkdown(noteId)
        notifyItemsChanged()
    }

    private suspend fun shiftPlanningNodeSiblingsForInsert(
        noteId: Long,
        parentNodeId: Long?,
        fromSortOrder: Int
    ) {
        val now = System.currentTimeMillis()
        val updates = todoDao.getPlanningNodesForNote(noteId)
            .filter { node -> node.parentNodeId == parentNodeId && node.sortOrder >= fromSortOrder }
            .map { node -> node.copy(sortOrder = node.sortOrder + 1, updatedAtMillis = now) }
        if (updates.isNotEmpty()) {
            todoDao.updatePlanningNodes(updates)
        }
    }

    private suspend fun demotePlanningParentIfNeeded(parentId: Long?, now: Long): List<TodoItem> {
        val parent = parentId?.let { todoDao.getPlanningNode(it) } ?: return emptyList()
        val hasChildren = todoDao.getPlanningNodesForNote(parent.noteId).any { it.parentNodeId == parent.id }
        if (!hasChildren) return emptyList()
        return demotePlanningNodeToStructureIfNeeded(parent, now)
    }

    private suspend fun demotePlanningNodeToStructureIfNeeded(node: PlanningNode, now: Long): List<TodoItem> {
        if (!node.syncEnabled && node.linkedItemIds().isEmpty()) return emptyList()
        val linkedItems = node.linkedItemIds().distinct().mapNotNull { todoDao.getById(it) }
        todoDao.updatePlanningNode(
            node.copy(
                linkedTodoId = null,
                linkedEndTodoId = null,
                syncEnabled = false,
                updatedAtMillis = now
            )
        )
        return linkedItems
    }

    private suspend fun restorePlanningParentIfLeaf(
        parentId: Long?,
        createEventEndTodo: Boolean,
        now: Long
    ): List<TodoItem> {
        val parent = parentId?.let { todoDao.getPlanningNode(it) } ?: return emptyList()
        val hasChildren = todoDao.getPlanningNodesForNote(parent.noteId).any { it.parentNodeId == parent.id }
        if (hasChildren || parent.syncEnabled || parent.linkedItemIds().isNotEmpty()) return emptyList()
        if (parent.isDraft) return emptyList()
        if (!shouldRestorePlanningStructureAsLeaf(parent)) return emptyList()
        val note = todoDao.getPlanningNote(parent.noteId) ?: return emptyList()
        val resolved = resolvePlanningNodeDraft(
            note = note,
            text = parent.text,
            dueAt = parent.dueAtMillis?.toPlanningLocalDateTime(),
            startAt = parent.startAtMillis?.toPlanningLocalDateTime(),
            endAt = parent.endAtMillis?.toPlanningLocalDateTime(),
            location = parent.location
        ) ?: return emptyList()
        val linked = createLinkedItemForPlanningNode(resolved, completed = parent.completed, now = now)
        val endTodo = createPlanningEventEndTodo(
            resolved = resolved,
            completed = parent.completed,
            now = now,
            enabled = createEventEndTodo
        )
        todoDao.updatePlanningNode(
            parent.copy(
                linkedTodoId = linked?.id,
                linkedEndTodoId = endTodo?.id,
                syncEnabled = true,
                updatedAtMillis = now
            )
        )
        return listOfNotNull(linked, endTodo)
    }

    private fun shouldRestorePlanningStructureAsLeaf(node: PlanningNode): Boolean {
        if (node.startAtMillis != null || node.endAtMillis != null || node.dueAtMillis != null) return true
        return node.text.trim() !in PlanningStructureTitleTexts
    }

    suspend fun exportPlanningNodesToMarkdown(noteId: Long): String {
        return planningNodesToMarkdown(todoDao.getPlanningNodesForNote(noteId))
    }

    suspend fun replacePlanningNodesFromMarkdown(noteId: Long, markdown: String): PlanningNodeMarkdownImportResult {
        val note = todoDao.getPlanningNote(noteId) ?: return PlanningNodeMarkdownImportResult(emptyList())
        val existingNodes = todoDao.getPlanningNodesForNote(noteId)
        val deletedLinkedItems = existingNodes.flatMap { it.linkedItemIds() }.distinct().mapNotNull { todoDao.getById(it) }
        deleteItemsByIds(deletedLinkedItems.map { it.id })
        todoDao.deletePlanningNodesForNote(noteId)

        val created = mutableListOf<PlanningNodeChangeResult>()
        val lastNodeByDepth = mutableMapOf<Int, Long>()
        val sortOrderByParent = mutableMapOf<Long, Int>()
        planningMarkdownImportLines(markdown).forEach { line ->
            val parentId = if (line.depth > 0) {
                ((line.depth - 1) downTo 0).firstNotNullOfOrNull { lastNodeByDepth[it] }
            } else {
                null
            }
            val parentKey = parentId ?: 0L
            val sortOrder = sortOrderByParent.getOrDefault(parentKey, 0)
            sortOrderByParent[parentKey] = sortOrder + 1
            val result = createPlanningNode(
                PlanningNodeDraft(
                    noteId = note.id,
                    parentNodeId = parentId,
                    text = line.text,
                    sortOrder = sortOrder,
                    syncEnabled = line.syncEnabled,
                    completed = line.completed
                )
            ) ?: return@forEach
            created += result
            lastNodeByDepth[line.depth] = result.node.id
            lastNodeByDepth.keys.filter { it > line.depth }.toList().forEach(lastNodeByDepth::remove)
        }
        updatePlanningNoteNodeLegacyMarkdown(noteId)
        notifyItemsChanged()
        return PlanningNodeMarkdownImportResult(created = created, deletedLinkedItems = deletedLinkedItems)
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

    suspend fun createPlanningNote(title: String, documentDate: LocalDate?): PlanningNote {
        val now = System.currentTimeMillis()
        val note = PlanningNote(
            title = title.trim().ifBlank { "新的规划" },
            contentMarkdown = "",
            createdAtMillis = now,
            updatedAtMillis = now,
            archived = false,
            documentDateEpochDay = documentDate?.toEpochDay()
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

    suspend fun deletePlanningNote(noteId: Long): List<TodoItem> {
        val linkedItems = todoDao.getPlanningNodesForNote(noteId)
            .flatMap { it.linkedItemIds() }
            .distinct()
            .mapNotNull { todoDao.getById(it) }
        deleteItemsByIds(linkedItems.map { it.id })
        todoDao.deletePlanningNodesForNote(noteId)
        todoDao.deletePlanningMappingsForNote(noteId)
        todoDao.deletePlanningNote(noteId)
        notifyItemsChanged()
        return linkedItems
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
        syncPlanningNodeCompletionFromItem(item)
        notifyItemsChanged()
        return item
    }

    suspend fun deleteTodo(id: Long): List<TodoItem> {
        val deletedItems = deleteItemsByIds(listOf(id))
        notifyItemsChanged()
        return deletedItems
    }

    suspend fun acknowledgeCalendarEvent(id: Long): TodoItem? {
        val item = todoDao.getById(id) ?: return null
        if (!item.isEvent) return item
        val updated = item.copy(reminderEnabled = false)
        todoDao.update(updated)
        return updated
    }

    suspend fun checkInEvent(eventId: Long, nowMillis: Long = System.currentTimeMillis()): EventCheckIn? {
        val item = todoDao.getById(eventId) ?: return null
        if (!item.isEvent || !item.checkInEnabled || item.completed || item.canceled) return null
        val active = todoDao.getActiveCheckIn(eventId)
        if (active != null) return active
        val checkIn = EventCheckIn(
            eventId = eventId,
            checkInAtMillis = nowMillis,
            checkOutAtMillis = null,
            durationMinutes = 0
        )
        val id = todoDao.insertCheckIn(checkIn)
        notifyItemsChanged()
        return checkIn.copy(id = id)
    }

    suspend fun checkOutEvent(eventId: Long, nowMillis: Long = System.currentTimeMillis()): EventCheckIn? {
        val active = todoDao.getActiveCheckIn(eventId) ?: return null
        val durationMinutes = ((nowMillis - active.checkInAtMillis).coerceAtLeast(0L) / 60_000L).toInt()
        todoDao.checkOutEvent(
            id = active.id,
            checkOutAtMillis = nowMillis,
            durationMinutes = durationMinutes
        )
        val totalMinutes = todoDao.getTotalCheckInMinutesForEvent(eventId)
        todoDao.updateTotalCheckInMinutes(eventId, totalMinutes)
        notifyItemsChanged()
        return active.copy(checkOutAtMillis = nowMillis, durationMinutes = durationMinutes)
    }

    suspend fun checkOutEventCheckIn(checkIn: EventCheckIn, checkOutMillis: Long): EventCheckIn {
        val durationMinutes = ((checkOutMillis - checkIn.checkInAtMillis).coerceAtLeast(0L) / 60_000L).toInt()
        todoDao.checkOutEvent(
            id = checkIn.id,
            checkOutAtMillis = checkOutMillis,
            durationMinutes = durationMinutes
        )
        val totalMinutes = todoDao.getTotalCheckInMinutesForEvent(checkIn.eventId)
        todoDao.updateTotalCheckInMinutes(checkIn.eventId, totalMinutes)
        notifyItemsChanged()
        return checkIn.copy(checkOutAtMillis = checkOutMillis, durationMinutes = durationMinutes)
    }

    suspend fun autoCheckOutEventIfNeeded(eventId: Long, nowMillis: Long = System.currentTimeMillis()): TodoItem? {
        checkOutEvent(eventId, nowMillis)
        return todoDao.getById(eventId)
    }

    suspend fun createFromDraft(draft: TodoDraft): List<TodoItem> {
        val now = System.currentTimeMillis()
        val groupIds = resolveTodoGroupIds(draft)
        val groupId = groupIds.first()
        val resolvedDraft = draft.copy(groupId = groupId, groupIds = groupIds)
        val generated = if (draft.recurrence.isRecurring) {
            generateRecurringItems(
                draft = resolvedDraft,
                seriesId = UUID.randomUUID().toString(),
                now = now
            )
        } else {
            listOf(buildTaskItem(resolvedDraft, now = now))
        }
        val ids = todoDao.insertAll(generated)
        val created = generated.zip(ids) { item, id -> item.copy(id = id) }
        replaceGroupTags(created, groupIds)
        if (draft.recurrence.isRecurring) {
            val template = buildTemplate(created.first(), resolvedDraft)
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
        val groupIds = resolveTodoGroupIds(draft, original)
        val resolvedDraft = draft.copy(groupId = groupIds.first(), groupIds = groupIds)
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
        replaceGroupTags(updatedItems.filter { it.isTodo }, groupIds)
        updatedItems.forEach { syncPlanningNodeFromItem(it) }
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
        val planningLinkedCanceledItems = syncPlanningNodeCancellationFromItems(canceledItems, now)
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
        return (canceledItems + planningLinkedCanceledItems).distinctBy { it.id }
    }

    suspend fun setCompleted(id: Long, completed: Boolean): TodoItem? {
        return setCompletedWithResult(id = id, completed = completed)?.item
    }

    suspend fun setCompletedWithResult(
        id: Long,
        completed: Boolean,
        autoCheckOutEventOnEnd: Boolean = false
    ): CompletedItemResult? {
        val item = todoDao.getById(id) ?: return null
        val now = System.currentTimeMillis()
        var autoCheckedOut = false
        if (completed && autoCheckOutEventOnEnd && item.isEvent && item.checkInEnabled) {
            val active = todoDao.getActiveCheckIn(item.id)
            if (active != null) {
                val durationMinutes = ((now - active.checkInAtMillis).coerceAtLeast(0L) / 60_000L).toInt()
                todoDao.checkOutEvent(
                    id = active.id,
                    checkOutAtMillis = now,
                    durationMinutes = durationMinutes
                )
                autoCheckedOut = true
            }
        }
        val totalCheckInMinutes = if (item.isEvent && item.checkInEnabled) {
            todoDao.getTotalCheckInMinutesForEvent(item.id)
        } else {
            item.totalCheckInMinutes
        }
        val updated = item.copy(
            completed = completed,
            completedAtMillis = if (completed) now else null,
            canceled = false,
            canceledAtMillis = null,
            missed = if (completed) false else item.missed,
            missedAtMillis = if (completed) null else item.missedAtMillis,
            totalCheckInMinutes = totalCheckInMinutes,
            reminderEnabled = if (completed) {
                false
            } else {
                item.reminderTriggerTimesMillis().any { it > now }
            }
        )
        todoDao.update(updated)
        val planningLinkedItems = syncPlanningNodeCompletionFromItem(updated, now)
        notifyItemsChanged()
        return CompletedItemResult(
            item = updated,
            affectedItems = (listOf(updated) + planningLinkedItems).distinctBy { it.id },
            eventCheckInSummary = if (completed && updated.isEvent && updated.checkInEnabled) {
                val checkIns = todoDao.getCheckInsForEvent(updated.id)
                updated.toEventCheckInCompletionSummary(
                    investedMinutes = totalCheckInMinutes,
                    checkInCount = checkIns.size,
                    autoCheckedOut = autoCheckedOut
                )
            } else {
                null
            }
        )
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
        updatedItems.forEach { syncPlanningNodeFromItem(it) }
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
                deleteItemsByIds(listOf(item.id))
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
                val deletedTargets = deleteItemsByIds(targets.map { it.id })
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
                deletedTargets
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

    suspend fun purgeAiReportsOlderThan(retention: AiReportRetention): Int {
        val days = retention.days ?: return 0
        val cutoffMillis = System.currentTimeMillis() - days.toLong() * 24L * 60L * 60L * 1000L
        return todoDao.purgeAiReportsBefore(cutoffMillis)
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
            reminderChainLogs = todoDao.getRecentReminderChainLogs(400),
            scheduleTemplates = todoDao.getScheduleTemplates(),
            planningNotes = planningNotes,
            planningLineMappings = planningMappings,
            planningNodes = todoDao.getAllPlanningNodes(),
            aiReports = todoDao.getAllAiReports(),
            todoGroupTags = todoDao.getAllTodoGroupTags(),
            eventCheckIns = todoDao.getAllEventCheckIns(),
            settings = settings
        )
    }

    suspend fun importSnapshot(snapshot: BackupSnapshot) {
        todoDao.clearPlanningNodes()
        todoDao.clearTodoGroupTags()
        todoDao.clearEventCheckIns()
        todoDao.clearTodos()
        todoDao.clearTemplates()
        todoDao.clearGroups()
        todoDao.clearReminderChainLogs()
        todoDao.clearScheduleTemplates()
        todoDao.clearPlanningNotes()
        todoDao.clearPlanningMappings()
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
        val restoredTags = restoredTodoGroupTags(snapshot)
        if (restoredTags.isNotEmpty()) {
            todoDao.insertTodoGroupTags(restoredTags)
        }
        val restoredCheckIns = restoredEventCheckIns(snapshot)
        if (restoredCheckIns.isNotEmpty()) {
            todoDao.insertCheckIns(restoredCheckIns)
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
        val restoredNodes = restoredPlanningNodes(snapshot)
        if (restoredNodes.isNotEmpty()) {
            todoDao.insertPlanningNodes(restoredNodes)
        }
        if (snapshot.aiReports.isNotEmpty()) {
            todoDao.insertAiReports(snapshot.aiReports)
        }
    }

    private fun restoredTodoGroupTags(snapshot: BackupSnapshot): List<TodoGroupTag> {
        val todoIds = snapshot.tasks.filter { it.isTodo }.map { it.id }.toSet()
        val groupIds = snapshot.groups.map { it.id }.toSet()
        val explicitTags = snapshot.todoGroupTags
            .filter { it.todoId in todoIds && it.groupId in groupIds }
            .distinct()
        if (explicitTags.isNotEmpty()) return explicitTags
        return snapshot.tasks
            .filter { it.isTodo && it.id > 0 && it.groupId > 0 && it.groupId in groupIds }
            .map { TodoGroupTag(todoId = it.id, groupId = it.groupId) }
            .distinct()
    }

    private fun restoredEventCheckIns(snapshot: BackupSnapshot): List<EventCheckIn> {
        val eventIds = snapshot.tasks.filter { it.isEvent }.map { it.id }.toSet()
        return snapshot.eventCheckIns
            .filter { it.eventId in eventIds && it.checkInAtMillis > 0 }
            .distinctBy { it.id.takeIf { id -> id > 0 } ?: "${it.eventId}-${it.checkInAtMillis}-${it.checkOutAtMillis}" }
    }

    private fun restoredPlanningNodes(snapshot: BackupSnapshot): List<PlanningNode> {
        val noteIds = snapshot.planningNotes.map { it.id }.toSet()
        val itemIds = snapshot.tasks.map { it.id }.toSet()
        val validNodes = snapshot.planningNodes
            .filter { it.id > 0 && it.noteId in noteIds && it.text.isNotBlank() }
            .distinctBy { it.id }
        val nodeIds = validNodes.map { it.id }.toSet()
        val sanitized = validNodes
            .map { node ->
                node.copy(
                    parentNodeId = node.parentNodeId?.takeIf { it in nodeIds },
                    linkedTodoId = node.linkedTodoId?.takeIf { !node.isDraft && it in itemIds },
                    linkedEndTodoId = node.linkedEndTodoId?.takeIf { !node.isDraft && it in itemIds }
                )
            }
        return topologicallySortedPlanningNodes(sanitized.withoutCyclicPlanningParents())
    }

    private fun List<PlanningNode>.withoutCyclicPlanningParents(): List<PlanningNode> {
        val byId = associateBy { it.id }
        return map { node ->
            val seen = mutableSetOf<Long>()
            var currentParent = node.parentNodeId
            var cyclic = false
            while (currentParent != null) {
                if (!seen.add(currentParent) || currentParent == node.id) {
                    cyclic = true
                    break
                }
                currentParent = byId[currentParent]?.parentNodeId
            }
            if (cyclic) node.copy(parentNodeId = null) else node
        }
    }

    private fun topologicallySortedPlanningNodes(nodes: List<PlanningNode>): List<PlanningNode> {
        if (nodes.isEmpty()) return emptyList()
        val childrenByParent = nodes
            .groupBy { it.parentNodeId }
            .mapValues { (_, children) ->
                children.sortedWith(compareBy<PlanningNode> { it.noteId }.thenBy { it.sortOrder }.thenBy { it.id })
            }
        val result = mutableListOf<PlanningNode>()
        val visited = mutableSetOf<Long>()

        fun visit(node: PlanningNode) {
            if (!visited.add(node.id)) return
            result += node
            childrenByParent[node.id].orEmpty().forEach(::visit)
        }

        childrenByParent[null]
            .orEmpty()
            .sortedWith(compareBy<PlanningNode> { it.noteId }.thenBy { it.sortOrder }.thenBy { it.id })
            .forEach(::visit)
        nodes.sortedWith(compareBy<PlanningNode> { it.noteId }.thenBy { it.sortOrder }.thenBy { it.id })
            .forEach(::visit)
        return result
    }

    private suspend fun resolvePlanningNodeDraft(
        note: PlanningNote,
        text: String,
        notes: String = "",
        groupId: Long = 0,
        groupName: String = "",
        dueAt: LocalDateTime?,
        startAt: LocalDateTime?,
        endAt: LocalDateTime?,
        location: String?,
        reminderOffsetsMinutes: List<Int>? = null,
        allDay: Boolean = false,
        countdownEnabled: Boolean = false,
        checkInEnabled: Boolean = false
    ): ResolvedPlanningNodeDraft? {
        val normalizedText = cleanPlanningNodeText(text)
        if (normalizedText.isBlank()) return null
        val documentDate = note.documentDateEpochDay?.let(LocalDate::ofEpochDay)
        val parsed = PlanningMarkdownParser.parse(
            markdown = "- [ ] $normalizedText",
            documentDate = documentDate
        ).candidates.firstOrNull { it.type == PlanningParsedType.TODO || it.type == PlanningParsedType.EVENT }
        val explicitLocation = location?.trim()?.takeIf { it.isNotBlank() }
        if (startAt != null && endAt != null && endAt.isAfter(startAt)) {
            return ResolvedPlanningNodeDraft(
                type = PlanningNodeResolvedType.EVENT,
                title = parsed?.title?.ifBlank { normalizedText } ?: normalizedText,
                notes = notes,
                groupId = groupId,
                groupName = groupName,
                dueAt = null,
                startAt = startAt,
                endAt = endAt,
                location = explicitLocation ?: parsed?.location?.takeIf { it.isNotBlank() },
                reminderOffsetsMinutes = reminderOffsetsMinutes,
                allDay = allDay,
                countdownEnabled = countdownEnabled,
                checkInEnabled = checkInEnabled
            )
        }
        if (dueAt != null) {
            return ResolvedPlanningNodeDraft(
                type = PlanningNodeResolvedType.TODO,
                title = parsed?.title?.ifBlank { normalizedText } ?: normalizedText,
                notes = notes,
                groupId = groupId,
                groupName = groupName,
                dueAt = dueAt,
                startAt = null,
                endAt = null,
                location = explicitLocation,
                reminderOffsetsMinutes = reminderOffsetsMinutes,
                allDay = allDay,
                countdownEnabled = countdownEnabled,
                checkInEnabled = checkInEnabled
            )
        }
        if (parsed?.type == PlanningParsedType.EVENT && parsed.startAt != null && parsed.endAt != null) {
            return ResolvedPlanningNodeDraft(
                type = PlanningNodeResolvedType.EVENT,
                title = parsed.title.ifBlank { normalizedText },
                notes = notes.ifBlank { parsed.notes },
                groupId = groupId,
                groupName = groupName.ifBlank { parsed.groupName },
                dueAt = null,
                startAt = parsed.startAt,
                endAt = parsed.endAt,
                location = explicitLocation ?: parsed.location.takeIf { it.isNotBlank() },
                reminderOffsetsMinutes = reminderOffsetsMinutes ?: parsed.reminderOffsetsMinutes,
                allDay = allDay || parsed.allDay,
                countdownEnabled = countdownEnabled || parsed.countdownEnabled,
                checkInEnabled = checkInEnabled || parsed.checkInEnabled
            )
        }
        return ResolvedPlanningNodeDraft(
            type = PlanningNodeResolvedType.TODO,
            title = parsed?.title?.ifBlank { normalizedText } ?: normalizedText,
            notes = notes.ifBlank { parsed?.notes.orEmpty() },
            groupId = groupId,
            groupName = groupName.ifBlank { parsed?.groupName.orEmpty() },
            dueAt = parsed?.dueAt,
            startAt = null,
            endAt = null,
            location = explicitLocation,
            reminderOffsetsMinutes = reminderOffsetsMinutes ?: parsed?.reminderOffsetsMinutes,
            allDay = allDay || (parsed?.allDay == true),
            countdownEnabled = countdownEnabled || (parsed?.countdownEnabled == true),
            checkInEnabled = checkInEnabled || (parsed?.checkInEnabled == true)
        )
    }

    private suspend fun createLinkedItemForPlanningNode(
        resolved: ResolvedPlanningNodeDraft,
        completed: Boolean,
        now: Long
    ): TodoItem? {
        val groupId = resolvePlanningNodeGroupId(resolved)
        val reminderOffsets = resolved.reminderOffsetsMinutes
            ?.map { it.coerceAtLeast(0) }
            ?.distinct()
            ?.sortedDescending()
        val item = when (resolved.type) {
            PlanningNodeResolvedType.TODO -> {
                val draft = TodoDraft(
                    title = resolved.title,
                    notes = resolved.notes,
                    dueAt = resolved.dueAt,
                    reminderAt = null,
                    groupId = groupId,
                    ringEnabled = true,
                    vibrateEnabled = true,
                    reminderDeliveryMode = ReminderDeliveryMode.FULLSCREEN,
                    countdownEnabled = resolved.countdownEnabled && resolved.dueAt != null,
                    reminderOffsetsMinutes = resolved.dueAt?.let { reminderOffsets ?: listOf(DEFAULT_PLANNING_REMINDER_MINUTES) }.orEmpty()
                )
                buildTaskItem(draft, now = now)
            }
            PlanningNodeResolvedType.EVENT -> {
                val start = resolved.startAt ?: return null
                val end = resolved.endAt ?: return null
                val group = todoDao.getGroupById(groupId)
                val draft = CalendarEventDraft(
                    title = resolved.title,
                    notes = resolved.notes,
                    location = resolved.location.orEmpty(),
                    startAt = start,
                    endAt = end,
                    allDay = resolved.allDay,
                    accentColorHex = group?.colorHex ?: "#4E87E1",
                    reminderMinutesBefore = reminderOffsets?.firstOrNull() ?: DEFAULT_PLANNING_REMINDER_MINUTES,
                    reminderOffsetsMinutes = reminderOffsets ?: listOf(DEFAULT_PLANNING_REMINDER_MINUTES),
                    ringEnabled = true,
                    vibrateEnabled = true,
                    reminderDeliveryMode = ReminderDeliveryMode.FULLSCREEN,
                    countdownEnabled = resolved.countdownEnabled,
                    checkInEnabled = resolved.checkInEnabled,
                    groupId = groupId
                )
                buildCalendarEventItem(draft, now = now)
            }
        }
        val id = todoDao.insert(item)
        var created = item.copy(id = id)
        if (created.isTodo) {
            replaceGroupTags(listOf(created), listOf(groupId))
        }
        if (completed) {
            created = created.copy(
                completed = true,
                completedAtMillis = now,
                reminderEnabled = false
            )
            todoDao.update(created)
        }
        return created
    }

    private suspend fun createPlanningEventEndTodo(
        resolved: ResolvedPlanningNodeDraft,
        completed: Boolean,
        now: Long,
        enabled: Boolean
    ): TodoItem? {
        if (!enabled || resolved.type != PlanningNodeResolvedType.EVENT) return null
        val end = resolved.endAt ?: return null
        val groupId = resolvePlanningNodeGroupId(resolved)
        val reminderOffsets = resolved.reminderOffsetsMinutes
            ?.map { it.coerceAtLeast(0) }
            ?.distinct()
            ?.sortedDescending()
            ?: listOf(DEFAULT_PLANNING_REMINDER_MINUTES)
        val draft = TodoDraft(
            title = resolved.title,
            notes = "",
            dueAt = end,
            reminderAt = null,
            groupId = groupId,
            ringEnabled = true,
            vibrateEnabled = true,
            reminderDeliveryMode = ReminderDeliveryMode.FULLSCREEN,
            countdownEnabled = false,
            reminderOffsetsMinutes = reminderOffsets,
            groupIds = listOf(groupId)
        )
        val item = buildTaskItem(draft, now = now)
        val id = todoDao.insert(item)
        var created = item.copy(id = id)
        replaceGroupTags(listOf(created), listOf(groupId))
        if (completed) {
            created = created.copy(
                completed = true,
                completedAtMillis = now,
                reminderEnabled = false
            )
            todoDao.update(created)
        }
        return created
    }

    private suspend fun upsertLinkedItemForPlanningNode(
        previous: TodoItem?,
        resolved: ResolvedPlanningNodeDraft,
        completed: Boolean,
        now: Long
    ): PlanningNodeLinkedSync {
        val expectedEvent = resolved.type == PlanningNodeResolvedType.EVENT
        if (previous == null || previous.isEvent != expectedEvent) {
            val created = createLinkedItemForPlanningNode(resolved, completed, now)
            return PlanningNodeLinkedSync(linkedItem = created, deletedLinkedItem = previous)
        }

        val updated = when (resolved.type) {
            PlanningNodeResolvedType.TODO -> {
                val groupIds = getGroupIdsForTodo(previous.id).ifEmpty { listOf(previous.groupId.takeIf { it > 0 } ?: defaultGroupId()) }
                val draft = TodoDraft(
                    title = resolved.title,
                    notes = previous.notes,
                    dueAt = resolved.dueAt,
                    reminderAt = null,
                    groupId = groupIds.first(),
                    ringEnabled = previous.ringEnabled,
                    vibrateEnabled = previous.vibrateEnabled,
                    reminderDeliveryMode = previous.reminderDeliveryModeEnum,
                    countdownEnabled = previous.countdownEnabled && resolved.dueAt != null,
                    reminderOffsetsMinutes = if (resolved.dueAt == null) emptyList() else previous.configuredReminderOffsetsMinutes.ifEmpty { listOf(DEFAULT_PLANNING_REMINDER_MINUTES) },
                    groupIds = groupIds
                )
                buildTaskItem(draft, now = previous.createdAtMillis, existing = previous).copy(
                    completed = completed,
                    completedAtMillis = if (completed) previous.completedAtMillis ?: now else null,
                    reminderEnabled = if (completed) false else buildTaskItem(draft, now = previous.createdAtMillis, existing = previous).reminderEnabled
                )
            }
            PlanningNodeResolvedType.EVENT -> {
                val start = resolved.startAt ?: return PlanningNodeLinkedSync(linkedItem = previous, deletedLinkedItem = null)
                val end = resolved.endAt ?: return PlanningNodeLinkedSync(linkedItem = previous, deletedLinkedItem = null)
                val draft = CalendarEventDraft(
                    title = resolved.title,
                    notes = previous.notes,
                    location = resolved.location ?: previous.location,
                    startAt = start,
                    endAt = end,
                    allDay = previous.allDay,
                    accentColorHex = previous.accentColorHex ?: "#4E87E1",
                    reminderMinutesBefore = previous.configuredReminderOffsetsMinutes.firstOrNull() ?: DEFAULT_PLANNING_REMINDER_MINUTES,
                    reminderOffsetsMinutes = previous.configuredReminderOffsetsMinutes.ifEmpty { listOf(DEFAULT_PLANNING_REMINDER_MINUTES) },
                    ringEnabled = previous.ringEnabled,
                    vibrateEnabled = previous.vibrateEnabled,
                    reminderDeliveryMode = previous.reminderDeliveryModeEnum,
                    countdownEnabled = previous.countdownEnabled,
                    checkInEnabled = previous.checkInEnabled,
                    groupId = previous.groupId
                )
                buildCalendarEventItem(draft, now = previous.createdAtMillis, existing = previous).copy(
                    completed = completed,
                    completedAtMillis = if (completed) previous.completedAtMillis ?: now else null,
                    reminderEnabled = if (completed) false else buildCalendarEventItem(draft, now = previous.createdAtMillis, existing = previous).reminderEnabled
                )
            }
        }
        todoDao.update(updated)
        if (updated.isTodo) {
            val ids = getGroupIdsForTodo(updated.id).ifEmpty { listOf(updated.groupId) }
            replaceGroupTags(listOf(updated), ids)
        }
        return PlanningNodeLinkedSync(linkedItem = updated, deletedLinkedItem = null)
    }

    private suspend fun upsertPlanningEventEndTodo(
        previous: TodoItem?,
        resolved: ResolvedPlanningNodeDraft,
        completed: Boolean,
        now: Long,
        enabled: Boolean
    ): PlanningNodeLinkedSync {
        val shouldExist = enabled && resolved.type == PlanningNodeResolvedType.EVENT && resolved.endAt != null
        if (!shouldExist) {
            return PlanningNodeLinkedSync(linkedItem = null, deletedLinkedItem = previous)
        }
        val end = resolved.endAt ?: return PlanningNodeLinkedSync(linkedItem = null, deletedLinkedItem = previous)
        if (previous == null || !previous.isTodo) {
            val created = createPlanningEventEndTodo(resolved, completed, now, enabled = true)
            return PlanningNodeLinkedSync(linkedItem = created, deletedLinkedItem = previous)
        }
        val groupIds = getGroupIdsForTodo(previous.id).ifEmpty {
            listOf(previous.groupId.takeIf { it > 0 } ?: resolvePlanningNodeGroupId(resolved))
        }
        val draft = TodoDraft(
            title = resolved.title,
            notes = previous.notes,
            dueAt = end,
            reminderAt = null,
            groupId = groupIds.first(),
            ringEnabled = previous.ringEnabled,
            vibrateEnabled = previous.vibrateEnabled,
            reminderDeliveryMode = previous.reminderDeliveryModeEnum,
            countdownEnabled = previous.countdownEnabled,
            reminderOffsetsMinutes = previous.configuredReminderOffsetsMinutes.ifEmpty { listOf(DEFAULT_PLANNING_REMINDER_MINUTES) },
            groupIds = groupIds
        )
        val built = buildTaskItem(draft, now = previous.createdAtMillis, existing = previous)
        val updated = built.copy(
            completed = completed,
            completedAtMillis = if (completed) previous.completedAtMillis ?: now else null,
            reminderEnabled = if (completed) false else built.reminderEnabled
        )
        todoDao.update(updated)
        replaceGroupTags(listOf(updated), groupIds)
        return PlanningNodeLinkedSync(linkedItem = updated, deletedLinkedItem = null)
    }

    private suspend fun syncPlanningNodeCompletionFromItem(item: TodoItem, now: Long = System.currentTimeMillis()): List<TodoItem> {
        val nodes = todoDao.getPlanningNodesByAnyLinkedTodo(item.id)
        if (nodes.isEmpty()) return emptyList()
        val allNodesByNote = nodes.map { it.noteId }.distinct().associateWith { noteId ->
            todoDao.getPlanningNodesForNote(noteId)
        }
        val directUpdates = nodes.map { node ->
            node.copy(
                completed = item.completed,
                completedAtMillis = if (item.completed) item.completedAtMillis ?: now else null,
                updatedAtMillis = now
            )
        }
        todoDao.updatePlanningNodes(directUpdates)
        val linkedUpdates = directUpdates
            .flatMap { it.linkedItemIds() }
            .distinct()
            .filter { it != item.id }
            .mapNotNull { linkedItemId ->
                updateLinkedItemCompletion(linkedItemId, item.completed, now)
            }
            .toMutableList()
        directUpdates.forEach { node ->
            allNodesByNote[node.noteId]?.let { allNodes ->
                linkedUpdates += updateAncestorCompletionStates(node, allNodes, now)
            }
        }
        nodes.map { it.noteId }.distinct().forEach { noteId -> updatePlanningNoteNodeLegacyMarkdown(noteId) }
        return linkedUpdates.distinctBy { it.id }
    }

    private suspend fun syncPlanningNodeCancellationFromItems(
        items: List<TodoItem>,
        now: Long = System.currentTimeMillis()
    ): List<TodoItem> {
        val canceledIds = items.map { it.id }.filter { it > 0 }.distinct()
        if (canceledIds.isEmpty()) return emptyList()
        val initialNodes = todoDao.getPlanningNodesByAnyLinkedTodos(canceledIds)
        if (initialNodes.isEmpty()) return emptyList()
        val primaryCanceledIds = canceledIds.toSet()
        val siblingIds = initialNodes
            .filter { node -> node.linkedTodoId in primaryCanceledIds }
            .flatMap { node -> node.linkedItemIds() }
            .filter { id -> id !in primaryCanceledIds }
            .distinct()
        val siblingCanceledItems = siblingIds
            .mapNotNull { id -> todoDao.getById(id) }
            .filter { item -> item.isActive }
            .map { item ->
                item.copy(
                    canceled = true,
                    canceledAtMillis = now,
                    completed = false,
                    completedAtMillis = null,
                    missed = false,
                    missedAtMillis = null,
                    reminderEnabled = false
                )
            }
        if (siblingCanceledItems.isNotEmpty()) {
            todoDao.updateAll(siblingCanceledItems)
        }
        detachPlanningNodesFromLinkedItems(
            linkedItemIds = canceledIds + siblingCanceledItems.map { it.id },
            now = now
        )
        return siblingCanceledItems
    }

    private suspend fun syncPlanningNodeFromItem(item: TodoItem, now: Long = System.currentTimeMillis()) {
        val nodes = todoDao.getPlanningNodesByLinkedTodo(item.id)
        if (nodes.isEmpty()) return
        val allNodesByNote = nodes.map { it.noteId }.distinct().associateWith { noteId ->
            todoDao.getPlanningNodesForNote(noteId)
        }
        val updatedNodes = nodes.map { node ->
            node.copy(
                text = item.title,
                updatedAtMillis = now,
                startAtMillis = item.startAtMillis.takeIf { item.isEvent },
                endAtMillis = item.endAtMillis.takeIf { item.isEvent },
                dueAtMillis = item.dueAtMillis.takeIf { item.isTodo && item.hasDueDate },
                location = item.location.takeIf { item.isEvent && it.isNotBlank() },
                completed = item.completed,
                completedAtMillis = if (item.completed) item.completedAtMillis ?: now else null
            )
        }
        todoDao.updatePlanningNodes(updatedNodes)
        updatedNodes.forEach { node ->
            allNodesByNote[node.noteId]?.let { allNodes ->
                updateAncestorCompletionStates(node, allNodes, now)
            }
        }
        nodes.map { it.noteId }.distinct().forEach { noteId -> updatePlanningNoteNodeLegacyMarkdown(noteId) }
    }

    private fun planningNodeSubtree(root: PlanningNode, allNodes: List<PlanningNode>): List<PlanningNode> {
        val childrenByParent = allNodes.groupBy { it.parentNodeId }
        return buildList {
            fun visit(node: PlanningNode) {
                add(node)
                childrenByParent[node.id].orEmpty().forEach(::visit)
            }
            visit(root)
        }
    }

    private suspend fun updateLinkedItemCompletion(
        itemId: Long,
        completed: Boolean,
        now: Long
    ): TodoItem? {
        val item = todoDao.getById(itemId) ?: return null
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
        return updated
    }

    private suspend fun updateAncestorCompletionStates(
        changedNode: PlanningNode,
        allNodes: List<PlanningNode>,
        now: Long
    ): List<TodoItem> {
        val nodesById = allNodes.associateBy { it.id }.toMutableMap()
        nodesById[changedNode.id] = changedNode
        val childrenByParent = allNodes.groupBy { it.parentNodeId }
        var parentId = changedNode.parentNodeId
        val updates = mutableListOf<PlanningNode>()
        val linkedUpdates = mutableListOf<TodoItem>()
        while (parentId != null) {
            val parent = nodesById[parentId] ?: break
            val children = childrenByParent[parent.id].orEmpty()
            if (children.isEmpty()) {
                parentId = parent.parentNodeId
                continue
            }
            val allChildrenCompleted = children.all { child ->
                nodesById[child.id]?.completed ?: child.completed
            }
            val nextParent = parent.copy(
                completed = allChildrenCompleted,
                completedAtMillis = if (allChildrenCompleted) parent.completedAtMillis ?: now else null,
                updatedAtMillis = now
            )
            if (nextParent.completed != parent.completed || nextParent.completedAtMillis != parent.completedAtMillis) {
                updates += nextParent
                nodesById[parent.id] = nextParent
                parent.linkedTodoId?.let { itemId ->
                    updateLinkedItemCompletion(itemId, allChildrenCompleted, now)?.let(linkedUpdates::add)
                }
                parent.linkedEndTodoId?.let { itemId ->
                    updateLinkedItemCompletion(itemId, allChildrenCompleted, now)?.let(linkedUpdates::add)
                }
            }
            parentId = parent.parentNodeId
        }
        if (updates.isNotEmpty()) {
            todoDao.updatePlanningNodes(updates)
        }
        return linkedUpdates
    }

    private suspend fun updatePlanningNoteNodeLegacyMarkdown(noteId: Long) {
        val note = todoDao.getPlanningNote(noteId) ?: return
        val markdown = planningNodesToMarkdown(todoDao.getPlanningNodesForNote(noteId))
        val updated = note.copy(
            contentMarkdown = markdown,
            updatedAtMillis = System.currentTimeMillis(),
            hasAnnouncementHint = PlanningAnnouncementParser.mightContainAnnouncement(markdown)
        )
        todoDao.updatePlanningNote(updated)
    }

    private fun planningNodesToMarkdown(nodes: List<PlanningNode>): String {
        if (nodes.isEmpty()) return ""
        val childrenByParent = nodes
            .groupBy { it.parentNodeId }
            .mapValues { (_, children) -> children.sortedWith(compareBy<PlanningNode> { it.sortOrder }.thenBy { it.id }) }
        val lines = mutableListOf<String>()
        fun appendNode(node: PlanningNode, depth: Int) {
            if (!node.syncEnabled) {
                lines += "#".repeat((depth + 1).coerceIn(1, 6)) + " " + node.text.trim()
            } else {
                val prefix = "  ".repeat(depth) + if (node.completed) "- [x] " else "- [ ] "
                lines += prefix + planningNodeMarkdownText(node)
            }
            childrenByParent[node.id].orEmpty().forEach { appendNode(it, depth + 1) }
        }
        childrenByParent[null].orEmpty().forEach { appendNode(it, 0) }
        return lines.joinToString("\n")
    }

    private fun planningNodeMarkdownText(node: PlanningNode): String {
        val title = node.text.trim()
        val location = node.location?.trim().orEmpty()
        return when {
            node.startAtMillis != null && node.endAtMillis != null -> {
                val start = node.startAtMillis.toPlanningLocalDateTime()
                val end = node.endAtMillis.toPlanningLocalDateTime()
                buildString {
                    append(start.format(PlanningNodeDateTimeFormatter))
                    append("-")
                    append(if (start.toLocalDate() == end.toLocalDate()) end.format(PlanningNodeTimeFormatter) else end.format(PlanningNodeDateTimeFormatter))
                    append(" ")
                    append(title)
                    if (location.isNotBlank()) append(" ").append(location)
                }
            }
            node.dueAtMillis != null -> "$title ddl ${node.dueAtMillis.toPlanningLocalDateTime().format(PlanningNodeDateTimeFormatter)}"
            else -> title
        }
    }

    private fun planningMarkdownImportLines(markdown: String): List<PlanningMarkdownImportLine> {
        return markdown.replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .mapNotNull { rawLine ->
                val leadingSpaces = rawLine.takeWhile { it == ' ' || it == '\t' }
                    .fold(0) { total, char -> total + if (char == '\t') 4 else 1 }
                val trimmed = rawLine.trim()
                if (trimmed.isBlank()) return@mapNotNull null
                val checkbox = Regex("^[-*+]\\s+\\[([ xX])\\]\\s+(.+)$").matchEntire(trimmed)
                if (checkbox != null) {
                    return@mapNotNull PlanningMarkdownImportLine(
                        depth = (leadingSpaces / 2).coerceAtLeast(0),
                        text = checkbox.groupValues[2].replace(Regex("\\s+#imported(?=\\s|$)"), "").trim(),
                        syncEnabled = true,
                        completed = checkbox.groupValues[1].equals("x", ignoreCase = true)
                    )
                }
                val heading = Regex("^#{1,6}\\s+(.+)$").matchEntire(trimmed)
                if (heading != null) {
                    return@mapNotNull PlanningMarkdownImportLine(
                        depth = (trimmed.takeWhile { it == '#' }.length - 1).coerceAtLeast(0),
                        text = heading.groupValues[1].replace(Regex("\\s+#imported(?=\\s|$)"), "").trim(),
                        syncEnabled = false,
                        completed = false
                    )
                }
                val bullet = Regex("^[-*+•]\\s+(.+)$").matchEntire(trimmed)
                PlanningMarkdownImportLine(
                    depth = (leadingSpaces / 2).coerceAtLeast(0),
                    text = (bullet?.groupValues?.getOrNull(1) ?: trimmed).replace(Regex("\\s+#imported(?=\\s|$)"), "").trim(),
                    syncEnabled = true,
                    completed = false
                )
            }
            .filter { it.text.isNotBlank() }
    }

    private fun cleanPlanningNodeText(text: String): String {
        return text.trim()
            .replace(Regex("^[-*+]\\s+\\[[ xX]]\\s+"), "")
            .replace(Regex("^[-*+•]\\s+"), "")
            .replace(Regex("\\s+#imported(?=\\s|$)"), "")
            .trim()
    }

    private fun Long.toPlanningLocalDateTime(): LocalDateTime {
        return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    private data class ResolvedPlanningNodeDraft(
        val type: PlanningNodeResolvedType,
        val title: String,
        val notes: String,
        val groupId: Long,
        val groupName: String,
        val dueAt: LocalDateTime?,
        val startAt: LocalDateTime?,
        val endAt: LocalDateTime?,
        val location: String?,
        val reminderOffsetsMinutes: List<Int>?,
        val allDay: Boolean,
        val countdownEnabled: Boolean,
        val checkInEnabled: Boolean
    )

    private data class PlanningNodeLinkedSync(
        val linkedItem: TodoItem?,
        val deletedLinkedItem: TodoItem?
    )

    private fun PlanningNode.linkedItemIds(): List<Long> {
        return listOfNotNull(linkedTodoId, linkedEndTodoId)
    }

    private data class PlanningMarkdownImportLine(
        val depth: Int,
        val text: String,
        val syncEnabled: Boolean,
        val completed: Boolean
    )

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
                    countdownEnabled = candidate.countdownEnabled && candidate.dueAt != null,
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
                    location = candidate.location.trim(),
                    dueAtMillis = startMillis,
                    startAtMillis = startMillis,
                    endAtMillis = endMillis,
                    allDay = candidate.allDay,
                    groupId = groupId,
                    reminderOffsetsCsv = encodeReminderOffsets(offsets, DEFAULT_PLANNING_REMINDER_MINUTES),
                    reminderOffsetMinutes = offsets.firstOrNull(),
                    reminderAtMillis = offsets.minOrNull()?.let { candidate.startAt.minusMinutes(it.toLong()).toEpochMillis() },
                    reminderEnabled = offsets.isNotEmpty(),
                    countdownEnabled = candidate.countdownEnabled,
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
        deleteItemsByIds(todoIds + eventItems.map { it.id })
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
                deleteItemsByIds(targets.map { it.id })
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
                deleteItemsByIds(targets.map { it.id })
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
        deleteItemsByIds(targets.map { it.id })

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
        val created = replacement.zip(ids) { item, id -> item.copy(id = id) }
        replaceGroupTags(created, draft.groupIds.ifEmpty { listOf(draft.groupId) })
        return created
    }

    private suspend fun replaceRecurringCalendarTargets(
        original: TodoItem,
        draft: CalendarEventDraft,
        targets: List<TodoItem>,
        targetSeriesId: String,
        deleteTemplateSeriesId: String?
    ): List<TodoItem> {
        deleteItemsByIds(targets.map { it.id })
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
            checkInEnabled = draft.checkInEnabled,
            totalCheckInMinutes = existing?.totalCheckInMinutes ?: 0,
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
        private val PlanningNodeDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        private val PlanningNodeTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val PlanningStructureTitleTexts = setOf(
            "今日计划",
            "今天计划",
            "明日计划",
            "明天计划",
            "后天计划",
            "收集箱",
            "今日",
            "今天",
            "明日",
            "明天",
            "后天"
        )
    }

    private suspend fun defaultGroupId(): Long {
        val groups = ensureDefaultGroups()
        return groups.firstOrNull { it.name == "例行" }?.id ?: groups.firstOrNull()?.id ?: 0L
    }

    private suspend fun resolveTodoGroupIds(draft: TodoDraft, original: TodoItem? = null): List<Long> {
        val groups = ensureDefaultGroups()
        val validGroupIds = groups.map { it.id }.toSet()
        val originalTagIds = original?.let { todoDao.getGroupIdsForTodo(it.id) }.orEmpty()
        val candidates = buildList {
            addAll(draft.groupIds)
            if (draft.groupId > 0) add(draft.groupId)
            if (isEmpty()) addAll(originalTagIds)
            if (original?.groupId != null && original.groupId > 0) add(original.groupId)
        }.filter { it > 0 && it in validGroupIds }.distinct()

        val primary = when {
            draft.groupId > 0 && draft.groupId in validGroupIds -> draft.groupId
            original?.groupId != null && original.groupId > 0 && original.groupId in validGroupIds -> original.groupId
            candidates.isNotEmpty() -> candidates.first()
            else -> defaultGroupId()
        }
        return (listOf(primary) + candidates.filter { it != primary })
            .filter { it > 0 && it in validGroupIds }
            .distinct()
            .ifEmpty { listOf(defaultGroupId()) }
    }

    private suspend fun replaceGroupTags(items: List<TodoItem>, groupIds: List<Long>) {
        val todoItems = items.filter { it.isTodo }
        if (todoItems.isEmpty()) return
        val distinctGroupIds = groupIds.filter { it > 0 }.distinct()
        todoDao.clearTodoGroupTagsForTodos(todoItems.map { it.id })
        if (distinctGroupIds.isEmpty()) return
        todoDao.insertTodoGroupTags(
            todoItems.flatMap { item ->
                distinctGroupIds.map { groupId -> TodoGroupTag(todoId = item.id, groupId = groupId) }
            }
        )
    }

    private suspend fun deleteItemsByIds(ids: List<Long>): List<TodoItem> {
        val distinctIds = ids.filter { it > 0 }.distinct()
        if (distinctIds.isEmpty()) return emptyList()
        val planningNodes = todoDao.getPlanningNodesByAnyLinkedTodos(distinctIds)
        val cascadeLinkedIds = planningNodes
            .filter { node -> node.linkedTodoId in distinctIds }
            .flatMap { node -> node.linkedItemIds() }
            .filter { id -> id !in distinctIds }
        val deleteIds = (distinctIds + cascadeLinkedIds).distinct()
        val deletedItems = deleteIds.mapNotNull { id -> todoDao.getById(id) }
        detachPlanningNodesFromLinkedItems(deleteIds)
        todoDao.clearTodoGroupTagsForTodos(deleteIds)
        todoDao.clearCheckInsForEvents(deleteIds)
        todoDao.deleteByIds(deleteIds)
        return deletedItems
    }

    private suspend fun detachPlanningNodesFromLinkedItems(
        linkedItemIds: List<Long>,
        now: Long = System.currentTimeMillis()
    ) {
        val ids = linkedItemIds.filter { it > 0 }.distinct()
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        val nodes = todoDao.getPlanningNodesByAnyLinkedTodos(ids)
        if (nodes.isEmpty()) return
        val updates = nodes.map { node ->
            val primaryRemoved = node.linkedTodoId in idSet
            val endTodoRemoved = node.linkedEndTodoId in idSet
            node.copy(
                linkedTodoId = node.linkedTodoId.takeUnless { primaryRemoved },
                linkedEndTodoId = node.linkedEndTodoId.takeUnless { primaryRemoved || endTodoRemoved },
                syncEnabled = if (primaryRemoved) false else node.syncEnabled,
                completed = if (primaryRemoved) false else node.completed,
                completedAtMillis = if (primaryRemoved) null else node.completedAtMillis,
                updatedAtMillis = now
            )
        }
        todoDao.updatePlanningNodes(updates)
        updates.map { it.noteId }.distinct().forEach { noteId ->
            updatePlanningNoteNodeLegacyMarkdown(noteId)
        }
    }

    private suspend fun resolveCalendarGroupId(draft: CalendarEventDraft): Long {
        return when {
            draft.groupId > 0 -> draft.groupId
            draft.groupName.isNotBlank() -> resolveGroupIdByNameOrCreate(draft.groupName)
            else -> defaultGroupId()
        }
    }

    private suspend fun resolvePlanningNodeGroupId(resolved: ResolvedPlanningNodeDraft): Long {
        return when {
            resolved.groupId > 0 -> resolved.groupId
            resolved.groupName.isNotBlank() -> resolveGroupIdByNameOrCreate(resolved.groupName)
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

    private fun TodoItem.toEventCheckInCompletionSummary(
        investedMinutes: Int,
        checkInCount: Int,
        autoCheckedOut: Boolean
    ): EventCheckInCompletionSummary {
        val start = startAtMillis ?: dueAtMillis
        val end = endAtMillis ?: start
        val plannedMinutes = ((end - start).coerceAtLeast(0L) / 60_000L).toInt()
        val safeInvested = investedMinutes.coerceAtLeast(0)
        return EventCheckInCompletionSummary(
            eventId = id,
            title = title,
            plannedMinutes = plannedMinutes,
            investedMinutes = safeInvested,
            checkInCount = checkInCount,
            investmentRatePercent = plannedMinutes.takeIf { it > 0 }?.let {
                ((safeInvested.toDouble() / it.toDouble()) * 100.0).roundToInt()
            },
            autoCheckedOut = autoCheckedOut
        )
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
