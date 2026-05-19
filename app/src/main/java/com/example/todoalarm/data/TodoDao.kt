package com.example.todoalarm.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query(
        """
        SELECT * FROM todo_items
        ORDER BY dueAtMillis ASC, createdAtMillis DESC
        """
    )
    fun observeTodos(): Flow<List<TodoItem>>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE completed = 0
        AND canceled = 0
        AND itemType = 'TODO'
        AND (:groupId IS NULL OR groupId = :groupId)
        ORDER BY dueAtMillis ASC, createdAtMillis ASC
        """
    )
    fun observeActiveTodoItems(groupId: Long?): Flow<List<TodoItem>>

    @Query(
        """
        SELECT t.* FROM todo_items t
        INNER JOIN todo_group_tags g ON t.id = g.todoId
        WHERE t.completed = 0
        AND t.canceled = 0
        AND t.itemType = 'TODO'
        AND g.groupId IN (:groupIds)
        GROUP BY t.id
        HAVING COUNT(DISTINCT g.groupId) = :requiredCount
        ORDER BY t.dueAtMillis ASC, t.createdAtMillis ASC
        """
    )
    fun observeActiveTodoItemsByGroupIntersection(groupIds: List<Long>, requiredCount: Int): Flow<List<TodoItem>>

    @Query(
        """
        SELECT DISTINCT t.* FROM todo_items t
        INNER JOIN todo_group_tags g ON t.id = g.todoId
        WHERE t.completed = 0
        AND t.canceled = 0
        AND t.itemType = 'TODO'
        AND g.groupId IN (:groupIds)
        ORDER BY t.dueAtMillis ASC, t.createdAtMillis ASC
        """
    )
    fun observeActiveTodoItemsByGroupUnion(groupIds: List<Long>): Flow<List<TodoItem>>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE itemType = 'TODO'
        AND (:groupId IS NULL OR groupId = :groupId)
        AND (completed = 1 OR canceled = 1)
        ORDER BY
            COALESCE(completedAtMillis, canceledAtMillis, missedAtMillis, createdAtMillis) DESC,
            createdAtMillis DESC
        """
    )
    fun observeHistoryTodoItems(groupId: Long?): Flow<List<TodoItem>>

    @Query(
        """
        SELECT t.* FROM todo_items t
        INNER JOIN todo_group_tags g ON t.id = g.todoId
        WHERE t.itemType = 'TODO'
        AND (t.completed = 1 OR t.canceled = 1)
        AND g.groupId IN (:groupIds)
        GROUP BY t.id
        HAVING COUNT(DISTINCT g.groupId) = :requiredCount
        ORDER BY
            COALESCE(t.completedAtMillis, t.canceledAtMillis, t.missedAtMillis, t.createdAtMillis) DESC,
            t.createdAtMillis DESC
        """
    )
    fun observeHistoryTodoItemsByGroupIntersection(groupIds: List<Long>, requiredCount: Int): Flow<List<TodoItem>>

    @Query(
        """
        SELECT DISTINCT t.* FROM todo_items t
        INNER JOIN todo_group_tags g ON t.id = g.todoId
        WHERE t.itemType = 'TODO'
        AND (t.completed = 1 OR t.canceled = 1)
        AND g.groupId IN (:groupIds)
        ORDER BY
            COALESCE(t.completedAtMillis, t.canceledAtMillis, t.missedAtMillis, t.createdAtMillis) DESC,
            t.createdAtMillis DESC
        """
    )
    fun observeHistoryTodoItemsByGroupUnion(groupIds: List<Long>): Flow<List<TodoItem>>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE completed = 0
        AND canceled = 0
        AND itemType = 'EVENT'
        ORDER BY COALESCE(startAtMillis, dueAtMillis) ASC, createdAtMillis ASC
        """
    )
    fun observeActiveCalendarEvents(): Flow<List<TodoItem>>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE completed = 0
        AND canceled = 0
        AND itemType = 'EVENT'
        AND startAtMillis IS NOT NULL
        AND startAtMillis < :rangeEndMillis
        AND COALESCE(endAtMillis, startAtMillis) >= :rangeStartMillis
        ORDER BY COALESCE(startAtMillis, dueAtMillis) ASC, createdAtMillis ASC
        """
    )
    fun observeActiveCalendarEventsInRange(rangeStartMillis: Long, rangeEndMillis: Long): Flow<List<TodoItem>>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE completed = 0
        AND canceled = 0
        AND countdownEnabled = 1
        AND (
            (itemType = 'TODO' AND dueAtMillis != :noDueDateMillis AND dueAtMillis >= :minTargetMillis)
            OR (itemType = 'EVENT' AND startAtMillis IS NOT NULL AND startAtMillis >= :minTargetMillis)
        )
        ORDER BY COALESCE(startAtMillis, dueAtMillis) ASC, createdAtMillis ASC
        """
    )
    fun observeActiveCountdownItems(noDueDateMillis: Long, minTargetMillis: Long): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TodoItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TodoItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TodoItem>): List<Long>

    @Update
    suspend fun update(item: TodoItem)

    @Update
    suspend fun updateAll(items: List<TodoItem>)

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM todo_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM todo_items WHERE recurringSeriesId = :seriesId ORDER BY dueAtMillis ASC")
    suspend fun getBySeriesId(seriesId: String): List<TodoItem>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE recurringSeriesId = :seriesId
        AND completed = 0
        AND canceled = 0
        ORDER BY dueAtMillis ASC
        """
    )
    suspend fun getActiveBySeriesId(seriesId: String): List<TodoItem>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE completed = 0
        AND canceled = 0
        AND reminderEnabled = 1
        AND (
            reminderAtMillis IS NOT NULL
            OR reminderOffsetsCsv != ''
        )
        ORDER BY dueAtMillis ASC, createdAtMillis ASC
        """
    )
    suspend fun getActiveReminderItems(): List<TodoItem>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE completed = 0
        AND canceled = 0
        AND itemType = 'TODO'
        AND missed = 0
        AND dueAtMillis < :missBefore
        ORDER BY dueAtMillis ASC
        """
    )
    suspend fun getPendingMissCandidates(missBefore: Long): List<TodoItem>

    @Query("SELECT * FROM todo_items")
    suspend fun getAllTodos(): List<TodoItem>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE itemType = 'TODO'
        AND completed = 1
        AND completedAtMillis IS NOT NULL
        AND completedAtMillis >= :startMillis
        AND completedAtMillis < :endMillis
        ORDER BY completedAtMillis ASC, dueAtMillis ASC, createdAtMillis ASC
        """
    )
    suspend fun getCompletedTodosInRange(startMillis: Long, endMillis: Long): List<TodoItem>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE itemType = 'TODO'
        AND missed = 1
        AND dueAtMillis >= :startMillis
        AND dueAtMillis < :endMillis
        ORDER BY dueAtMillis ASC, createdAtMillis ASC
        """
    )
    suspend fun getMissedTodosDueInRange(startMillis: Long, endMillis: Long): List<TodoItem>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE itemType = 'TODO'
        AND completed = 0
        AND canceled = 0
        AND dueAtMillis != :noDueDateMillis
        AND dueAtMillis >= :startMillis
        AND dueAtMillis < :endMillis
        ORDER BY dueAtMillis ASC, createdAtMillis ASC
        """
    )
    suspend fun getActiveTodosDueInRange(
        startMillis: Long,
        endMillis: Long,
        noDueDateMillis: Long
    ): List<TodoItem>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE itemType = 'EVENT'
        AND completed = 0
        AND canceled = 0
        AND startAtMillis IS NOT NULL
        AND startAtMillis < :rangeEndMillis
        AND COALESCE(endAtMillis, startAtMillis) >= :rangeStartMillis
        ORDER BY COALESCE(startAtMillis, dueAtMillis) ASC, createdAtMillis ASC
        """
    )
    suspend fun getActiveEventsOverlappingRange(rangeStartMillis: Long, rangeEndMillis: Long): List<TodoItem>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE itemType = 'TODO'
        ORDER BY dueAtMillis ASC, createdAtMillis ASC
        """
    )
    suspend fun getDesktopTodoItems(): List<TodoItem>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE itemType = 'TODO'
        AND (
            :query = ''
            OR title LIKE '%' || :query || '%'
            OR notes LIKE '%' || :query || '%'
            OR location LIKE '%' || :query || '%'
        )
        ORDER BY
            CASE
                WHEN completed = 0 AND canceled = 0 AND (missed = 1 OR (dueAtMillis != :noDueDateMillis AND dueAtMillis < :todayStartMillis)) THEN 0
                WHEN completed = 0 AND canceled = 0 AND (dueAtMillis = :noDueDateMillis OR (dueAtMillis >= :todayStartMillis AND dueAtMillis < :todayEndMillisExclusive)) THEN 1
                WHEN completed = 0 AND canceled = 0 THEN 2
                ELSE 3
            END ASC,
            CASE WHEN dueAtMillis = :noDueDateMillis THEN 0 ELSE 1 END ASC,
            dueAtMillis ASC,
            createdAtMillis ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getDesktopTodoItemsPaged(
        query: String,
        todayStartMillis: Long,
        todayEndMillisExclusive: Long,
        noDueDateMillis: Long,
        limit: Int,
        offset: Int
    ): List<TodoItem>

    @Query(
        """
        SELECT COUNT(*) FROM todo_items
        WHERE itemType = 'TODO'
        AND (
            :query = ''
            OR title LIKE '%' || :query || '%'
            OR notes LIKE '%' || :query || '%'
            OR location LIKE '%' || :query || '%'
        )
        """
    )
    suspend fun countDesktopTodoItems(query: String): Int

    @Query(
        """
        SELECT * FROM todo_items
        WHERE completed = 0
        AND canceled = 0
        AND itemType = 'EVENT'
        AND startAtMillis IS NOT NULL
        AND startAtMillis < :rangeEndMillis
        AND COALESCE(endAtMillis, startAtMillis) >= :rangeStartMillis
        ORDER BY COALESCE(startAtMillis, dueAtMillis) ASC, createdAtMillis ASC
        """
    )
    suspend fun getActiveCalendarEventsInRangeOnce(rangeStartMillis: Long, rangeEndMillis: Long): List<TodoItem>

    @Query(
        """
        SELECT * FROM todo_items
        WHERE completed = 0
        AND canceled = 0
        AND (
            (
                itemType = 'TODO'
                AND (
                    missed = 1
                    OR dueAtMillis = :noDueDateMillis
                    OR dueAtMillis BETWEEN :todoStartMillis AND :boardEndMillis
                )
            )
            OR (
                itemType = 'EVENT'
                AND startAtMillis IS NOT NULL
                AND startAtMillis < :boardEndMillis
                AND COALESCE(endAtMillis, startAtMillis) >= :boardStartMillis
            )
            OR (
                countdownEnabled = 1
                AND (
                    (itemType = 'TODO' AND dueAtMillis != :noDueDateMillis AND dueAtMillis >= :boardStartMillis)
                    OR (itemType = 'EVENT' AND startAtMillis IS NOT NULL AND startAtMillis >= :boardStartMillis)
                )
            )
        )
        ORDER BY dueAtMillis ASC, createdAtMillis ASC
        """
    )
    suspend fun getActiveItemsForBoardRange(
        todoStartMillis: Long,
        boardStartMillis: Long,
        boardEndMillis: Long,
        noDueDateMillis: Long
    ): List<TodoItem>

    @Query("DELETE FROM todo_items")
    suspend fun clearTodos()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: EventCheckIn): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIns(checkIns: List<EventCheckIn>): List<Long>

    @Query(
        """
        UPDATE event_check_ins
        SET checkOutAtMillis = :checkOutAtMillis,
            durationMinutes = :durationMinutes
        WHERE id = :id
        """
    )
    suspend fun checkOutEvent(id: Long, checkOutAtMillis: Long, durationMinutes: Int)

    @Query("SELECT * FROM event_check_ins WHERE eventId = :eventId ORDER BY checkInAtMillis ASC")
    suspend fun getCheckInsForEvent(eventId: Long): List<EventCheckIn>

    @Query("SELECT * FROM event_check_ins WHERE eventId = :eventId AND checkOutAtMillis IS NULL ORDER BY checkInAtMillis DESC LIMIT 1")
    suspend fun getActiveCheckIn(eventId: Long): EventCheckIn?

    @Query("SELECT * FROM event_check_ins WHERE eventId IN (:eventIds) AND checkOutAtMillis IS NULL ORDER BY checkInAtMillis DESC")
    suspend fun getActiveCheckInsForEvents(eventIds: List<Long>): List<EventCheckIn>

    @Query("SELECT * FROM event_check_ins ORDER BY eventId ASC, checkInAtMillis ASC")
    suspend fun getAllEventCheckIns(): List<EventCheckIn>

    @Query("SELECT * FROM event_check_ins WHERE checkOutAtMillis IS NULL ORDER BY checkInAtMillis ASC")
    suspend fun getAllActiveEventCheckIns(): List<EventCheckIn>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM event_check_ins WHERE eventId = :eventId")
    suspend fun getTotalCheckInMinutesForEvent(eventId: Long): Int

    @Query("UPDATE todo_items SET totalCheckInMinutes = :minutes WHERE id = :id")
    suspend fun updateTotalCheckInMinutes(id: Long, minutes: Int)

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM event_check_ins WHERE checkInAtMillis >= :startMillis AND checkInAtMillis < :endMillis")
    suspend fun getTotalCheckInMinutesInRange(startMillis: Long, endMillis: Long): Int

    @Query("DELETE FROM event_check_ins WHERE eventId = :eventId")
    suspend fun clearCheckInsForEvent(eventId: Long)

    @Query("DELETE FROM event_check_ins WHERE eventId IN (:eventIds)")
    suspend fun clearCheckInsForEvents(eventIds: List<Long>)

    @Query("DELETE FROM event_check_ins")
    suspend fun clearEventCheckIns()

    @Query("SELECT * FROM task_groups ORDER BY sortOrder ASC, createdAtMillis ASC")
    fun observeGroups(): Flow<List<TaskGroup>>

    @Query("SELECT * FROM task_groups ORDER BY sortOrder ASC, createdAtMillis ASC")
    suspend fun getAllGroups(): List<TaskGroup>

    @Query("SELECT * FROM task_groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: Long): TaskGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: TaskGroup): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<TaskGroup>): List<Long>

    @Update
    suspend fun updateGroup(group: TaskGroup)

    @Query("DELETE FROM task_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Long)

    @Query(
        """
        SELECT COUNT(DISTINCT todoId)
        FROM todo_group_tags
        WHERE groupId = :groupId
        """
    )
    suspend fun countTasksInGroup(groupId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodoGroupTags(tags: List<TodoGroupTag>)

    @Query("SELECT * FROM todo_group_tags ORDER BY todoId ASC, groupId ASC")
    suspend fun getAllTodoGroupTags(): List<TodoGroupTag>

    @Query("SELECT groupId FROM todo_group_tags WHERE todoId = :todoId ORDER BY groupId ASC")
    suspend fun getGroupIdsForTodo(todoId: Long): List<Long>

    @Query("DELETE FROM todo_group_tags WHERE todoId = :todoId")
    suspend fun clearTodoGroupTags(todoId: Long)

    @Query("DELETE FROM todo_group_tags WHERE todoId IN (:todoIds)")
    suspend fun clearTodoGroupTagsForTodos(todoIds: List<Long>)

    @Query("DELETE FROM todo_group_tags WHERE groupId = :groupId")
    suspend fun clearTagsByGroup(groupId: Long)

    @Query("DELETE FROM todo_group_tags")
    suspend fun clearTodoGroupTags()

    @Query("SELECT * FROM recurring_task_templates ORDER BY createdAtMillis ASC")
    suspend fun getAllRecurringTemplates(): List<RecurringTaskTemplate>

    @Query("SELECT * FROM recurring_task_templates WHERE seriesId = :seriesId LIMIT 1")
    suspend fun getTemplateBySeriesId(seriesId: String): RecurringTaskTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: RecurringTaskTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<RecurringTaskTemplate>): List<Long>

    @Update
    suspend fun updateTemplate(template: RecurringTaskTemplate)

    @Query("DELETE FROM recurring_task_templates WHERE seriesId = :seriesId")
    suspend fun deleteTemplateBySeriesId(seriesId: String)

    @Query("DELETE FROM recurring_task_templates")
    suspend fun clearTemplates()

    @Query("DELETE FROM task_groups")
    suspend fun clearGroups()

    @Query(
        """
        SELECT * FROM reminder_chain_logs
        ORDER BY createdAtMillis DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentReminderChainLogs(limit: Int): List<ReminderChainLog>

    @Query(
        """
        SELECT * FROM reminder_chain_logs
        ORDER BY createdAtMillis DESC
        LIMIT :limit
        """
    )
    fun observeRecentReminderChainLogs(limit: Int): Flow<List<ReminderChainLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderChainLog(log: ReminderChainLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderChainLogs(logs: List<ReminderChainLog>): List<Long>

    @Query("DELETE FROM reminder_chain_logs")
    suspend fun clearReminderChainLogs()

    @Query(
        """
        DELETE FROM reminder_chain_logs
        WHERE id NOT IN (
            SELECT id FROM reminder_chain_logs ORDER BY createdAtMillis DESC LIMIT :keepCount
        )
        """
    )
    suspend fun trimReminderChainLogs(keepCount: Int)

    @Query("SELECT * FROM schedule_templates ORDER BY updatedAtMillis DESC, createdAtMillis DESC")
    suspend fun getScheduleTemplates(): List<ScheduleTemplate>

    @Query("SELECT * FROM schedule_templates ORDER BY updatedAtMillis DESC, createdAtMillis DESC")
    fun observeScheduleTemplates(): Flow<List<ScheduleTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleTemplate(template: ScheduleTemplate): Long

    @Update
    suspend fun updateScheduleTemplate(template: ScheduleTemplate)

    @Query("DELETE FROM schedule_templates WHERE id = :templateId")
    suspend fun deleteScheduleTemplate(templateId: Long)

    @Query("DELETE FROM schedule_templates")
    suspend fun clearScheduleTemplates()

    @Query(
        """
        SELECT * FROM planning_notes
        WHERE archived = 0
        ORDER BY updatedAtMillis DESC, createdAtMillis DESC
        """
    )
    fun observePlanningNotes(): Flow<List<PlanningNote>>

    @Query(
        """
        SELECT * FROM planning_notes
        WHERE archived = 0
        AND hasAnnouncementHint = 1
        ORDER BY updatedAtMillis DESC, createdAtMillis DESC
        """
    )
    fun observePlanningNotesWithAnnouncementHints(): Flow<List<PlanningNote>>

    @Query("SELECT * FROM planning_notes WHERE id = :id LIMIT 1")
    suspend fun getPlanningNote(id: Long): PlanningNote?

    @Query("SELECT * FROM planning_notes WHERE archived = 0 AND documentDateEpochDay = :epochDay ORDER BY updatedAtMillis DESC LIMIT 1")
    suspend fun getActivePlanningNoteByDocumentDate(epochDay: Long): PlanningNote?

    @Query("SELECT * FROM planning_notes ORDER BY updatedAtMillis DESC, createdAtMillis DESC")
    suspend fun getAllPlanningNotes(): List<PlanningNote>

    @Query(
        """
        SELECT * FROM planning_notes
        WHERE archived = 0
        ORDER BY updatedAtMillis DESC, createdAtMillis DESC
        """
    )
    suspend fun getActivePlanningNotes(): List<PlanningNote>

    @Query(
        """
        SELECT * FROM planning_notes
        WHERE archived = 0
        AND hasAnnouncementHint = 1
        ORDER BY updatedAtMillis DESC, createdAtMillis DESC
        """
    )
    suspend fun getPlanningNotesWithAnnouncementHints(): List<PlanningNote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanningNote(note: PlanningNote): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanningNotes(notes: List<PlanningNote>): List<Long>

    @Update
    suspend fun updatePlanningNote(note: PlanningNote)

    @Query("DELETE FROM planning_notes WHERE id = :id")
    suspend fun deletePlanningNote(id: Long)

    @Query("DELETE FROM planning_notes WHERE id IN (:ids)")
    suspend fun deletePlanningNotesByIds(ids: List<Long>)

    @Query("DELETE FROM planning_notes")
    suspend fun clearPlanningNotes()

    @Query("SELECT * FROM planning_line_mappings WHERE noteId = :noteId ORDER BY id ASC")
    suspend fun getMappingsForNote(noteId: Long): List<PlanningLineMapping>

    @Query("SELECT * FROM planning_line_mappings WHERE noteId = :noteId AND status = 'ACTIVE' ORDER BY id ASC")
    suspend fun getActiveMappingsForNote(noteId: Long): List<PlanningLineMapping>

    @Query("SELECT * FROM planning_line_mappings WHERE batchId = :batchId ORDER BY id ASC")
    suspend fun getMappingsByBatch(batchId: String): List<PlanningLineMapping>

    @Query("SELECT * FROM planning_line_mappings WHERE operationType IN ('IMPORT', 'REFRESH', 'POSTPONE') ORDER BY lastRefreshedAtMillis DESC, id DESC LIMIT 1")
    suspend fun getLatestPlanningMapping(): PlanningLineMapping?

    @Query("SELECT * FROM planning_line_mappings WHERE noteId = :noteId AND operationType IN ('IMPORT', 'REFRESH', 'POSTPONE') ORDER BY lastRefreshedAtMillis DESC, id DESC LIMIT 1")
    suspend fun getLatestPlanningMappingForNote(noteId: Long): PlanningLineMapping?

    @Query("SELECT * FROM planning_line_mappings WHERE (todoId = :todoId AND :todoId IS NOT NULL) OR (eventId = :eventId AND :eventId IS NOT NULL) LIMIT 1")
    suspend fun getMappingForItem(todoId: Long? = null, eventId: Long? = null): PlanningLineMapping?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanningMappings(mappings: List<PlanningLineMapping>): List<Long>

    @Update
    suspend fun updatePlanningMappings(mappings: List<PlanningLineMapping>)

    @Query("DELETE FROM planning_line_mappings WHERE batchId = :batchId")
    suspend fun deletePlanningMappingBatch(batchId: String)

    @Query("DELETE FROM planning_line_mappings WHERE noteId = :noteId")
    suspend fun deletePlanningMappingsForNote(noteId: Long)

    @Query("UPDATE planning_line_mappings SET status = :status WHERE id = :id")
    suspend fun updatePlanningMappingStatus(id: Long, status: MappingStatus)

    @Query("DELETE FROM planning_line_mappings")
    suspend fun clearPlanningMappings()

    @Query("SELECT * FROM planning_nodes WHERE noteId = :noteId ORDER BY parentNodeId ASC, sortOrder ASC, id ASC")
    fun observePlanningNodesForNote(noteId: Long): Flow<List<PlanningNode>>

    @Query("SELECT * FROM planning_nodes WHERE noteId = :noteId ORDER BY parentNodeId ASC, sortOrder ASC, id ASC")
    suspend fun getPlanningNodesForNote(noteId: Long): List<PlanningNode>

    @Query("SELECT * FROM planning_nodes ORDER BY noteId ASC, parentNodeId ASC, sortOrder ASC, id ASC")
    suspend fun getAllPlanningNodes(): List<PlanningNode>

    @Query("SELECT * FROM planning_nodes WHERE id = :nodeId LIMIT 1")
    suspend fun getPlanningNode(nodeId: Long): PlanningNode?

    @Query("SELECT * FROM planning_nodes WHERE linkedTodoId = :linkedTodoId ORDER BY id ASC")
    suspend fun getPlanningNodesByLinkedTodo(linkedTodoId: Long): List<PlanningNode>

    @Query(
        """
        SELECT * FROM planning_nodes
        WHERE linkedTodoId = :linkedTodoId OR linkedEndTodoId = :linkedTodoId
        ORDER BY id ASC
        """
    )
    suspend fun getPlanningNodesByAnyLinkedTodo(linkedTodoId: Long): List<PlanningNode>

    @Query(
        """
        SELECT * FROM planning_nodes
        WHERE linkedTodoId IN (:linkedTodoIds) OR linkedEndTodoId IN (:linkedTodoIds)
        ORDER BY noteId ASC, parentNodeId ASC, sortOrder ASC, id ASC
        """
    )
    suspend fun getPlanningNodesByAnyLinkedTodos(linkedTodoIds: List<Long>): List<PlanningNode>

    @Query(
        """
        SELECT COALESCE(MAX(sortOrder), -1)
        FROM planning_nodes
        WHERE noteId = :noteId
        AND (
            (:parentNodeId IS NULL AND parentNodeId IS NULL)
            OR parentNodeId = :parentNodeId
        )
        """
    )
    suspend fun getMaxPlanningNodeSortOrder(noteId: Long, parentNodeId: Long?): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanningNode(node: PlanningNode): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanningNodes(nodes: List<PlanningNode>): List<Long>

    @Update
    suspend fun updatePlanningNode(node: PlanningNode)

    @Update
    suspend fun updatePlanningNodes(nodes: List<PlanningNode>)

    @Query("DELETE FROM planning_nodes WHERE id = :nodeId")
    suspend fun deletePlanningNode(nodeId: Long)

    @Query("DELETE FROM planning_nodes WHERE noteId = :noteId")
    suspend fun deletePlanningNodesForNote(noteId: Long)

    @Query("DELETE FROM planning_nodes")
    suspend fun clearPlanningNodes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiReport(report: AiReport): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiReports(reports: List<AiReport>): List<Long>

    @Query("SELECT * FROM ai_reports ORDER BY generatedAtMillis DESC, id DESC LIMIT :limit")
    fun observeAiReports(limit: Int): Flow<List<AiReport>>

    @Query("SELECT * FROM ai_reports WHERE type = :type ORDER BY generatedAtMillis DESC, id DESC LIMIT :limit")
    fun observeAiReportsByType(type: AiReportType, limit: Int): Flow<List<AiReport>>

    @Query(
        """
        SELECT * FROM ai_reports
        WHERE generatedAtMillis BETWEEN :startMillis AND :endMillis
        AND (
            :query = ''
            OR content LIKE '%' || :query || '%'
            OR providerName LIKE '%' || :query || '%'
        )
        ORDER BY generatedAtMillis DESC, id DESC
        LIMIT :limit
        """
    )
    fun observeAiReportsFiltered(
        query: String,
        startMillis: Long,
        endMillis: Long,
        limit: Int
    ): Flow<List<AiReport>>

    @Query(
        """
        SELECT * FROM ai_reports
        WHERE type = :type
        AND generatedAtMillis BETWEEN :startMillis AND :endMillis
        AND (
            :query = ''
            OR content LIKE '%' || :query || '%'
            OR providerName LIKE '%' || :query || '%'
        )
        ORDER BY generatedAtMillis DESC, id DESC
        LIMIT :limit
        """
    )
    fun observeAiReportsFilteredByType(
        type: AiReportType,
        query: String,
        startMillis: Long,
        endMillis: Long,
        limit: Int
    ): Flow<List<AiReport>>

    @Query("SELECT * FROM ai_reports ORDER BY generatedAtMillis DESC, id DESC")
    suspend fun getAllAiReports(): List<AiReport>

    @Query("SELECT * FROM ai_reports WHERE type = :type ORDER BY generatedAtMillis DESC, id DESC")
    suspend fun getAiReportsByType(type: AiReportType): List<AiReport>

    @Query("SELECT * FROM ai_reports WHERE id = :id LIMIT 1")
    suspend fun getAiReportById(id: Long): AiReport?

    @Query("DELETE FROM ai_reports WHERE id = :id")
    suspend fun deleteAiReport(id: Long)

    @Query("DELETE FROM ai_reports WHERE generatedAtMillis < :cutoffMillis")
    suspend fun purgeAiReportsBefore(cutoffMillis: Long): Int

    @Query("DELETE FROM ai_reports")
    suspend fun clearAiReports()
}
