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

    @Query("SELECT COUNT(*) FROM todo_items WHERE groupId = :groupId")
    suspend fun countTasksInGroup(groupId: Long): Int

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
        AND (
            contentMarkdown LIKE '%公告%'
            OR contentMarkdown LIKE '%[!announcement]%'
            OR contentMarkdown LIKE '%[! announcement]%'
        )
        ORDER BY updatedAtMillis DESC, createdAtMillis DESC
        """
    )
    fun observePlanningNotesWithAnnouncementHints(): Flow<List<PlanningNote>>

    @Query("SELECT * FROM planning_notes WHERE id = :id LIMIT 1")
    suspend fun getPlanningNote(id: Long): PlanningNote?

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
        AND (
            contentMarkdown LIKE '%公告%'
            OR contentMarkdown LIKE '%[!announcement]%'
            OR contentMarkdown LIKE '%[! announcement]%'
        )
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

    @Insert
    suspend fun insertFocusSession(session: FocusSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSessions(sessions: List<FocusSession>): List<Long>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAtMillis DESC")
    fun observeFocusSessions(): Flow<List<FocusSession>>

    @Query(
        """
        SELECT
            COUNT(*) AS totalCount,
            COALESCE(SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END), 0) AS completedCount,
            COALESCE(SUM(CASE WHEN completed = 1 THEN actualMinutes ELSE 0 END), 0) AS completedMinutes
        FROM focus_sessions
        WHERE startedAtMillis BETWEEN :startMillis AND :endMillis
        """
    )
    fun observeFocusSessionStatsInRange(startMillis: Long, endMillis: Long): Flow<FocusSessionStats>

    @Query(
        """
        SELECT
            COUNT(*) AS totalCount,
            COALESCE(SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END), 0) AS completedCount,
            COALESCE(SUM(CASE WHEN completed = 1 THEN actualMinutes ELSE 0 END), 0) AS completedMinutes
        FROM focus_sessions
        WHERE startedAtMillis BETWEEN :startMillis AND :endMillis
        """
    )
    suspend fun getFocusSessionStatsInRange(startMillis: Long, endMillis: Long): FocusSessionStats

    @Query("SELECT * FROM focus_sessions WHERE startedAtMillis BETWEEN :startMillis AND :endMillis ORDER BY startedAtMillis DESC")
    suspend fun getFocusSessionsInRange(startMillis: Long, endMillis: Long): List<FocusSession>

    @Query("SELECT COALESCE(SUM(actualMinutes), 0) FROM focus_sessions WHERE completed = 1 AND startedAtMillis BETWEEN :startMillis AND :endMillis")
    suspend fun getCompletedFocusMinutesInRange(startMillis: Long, endMillis: Long): Int

    @Query("SELECT * FROM focus_sessions ORDER BY startedAtMillis DESC")
    suspend fun getAllFocusSessions(): List<FocusSession>

    @Query("DELETE FROM focus_sessions")
    suspend fun clearFocusSessions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiReport(report: AiReport): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiReports(reports: List<AiReport>): List<Long>

    @Query("SELECT * FROM ai_reports ORDER BY generatedAtMillis DESC, id DESC LIMIT :limit")
    fun observeAiReports(limit: Int): Flow<List<AiReport>>

    @Query("SELECT * FROM ai_reports WHERE type = :type ORDER BY generatedAtMillis DESC, id DESC LIMIT :limit")
    fun observeAiReportsByType(type: AiReportType, limit: Int): Flow<List<AiReport>>

    @Query("SELECT * FROM ai_reports ORDER BY generatedAtMillis DESC, id DESC")
    suspend fun getAllAiReports(): List<AiReport>

    @Query("SELECT * FROM ai_reports WHERE type = :type ORDER BY generatedAtMillis DESC, id DESC")
    suspend fun getAiReportsByType(type: AiReportType): List<AiReport>

    @Query("SELECT * FROM ai_reports WHERE id = :id LIMIT 1")
    suspend fun getAiReportById(id: Long): AiReport?

    @Query("DELETE FROM ai_reports WHERE id = :id")
    suspend fun deleteAiReport(id: Long)

    @Query("DELETE FROM ai_reports")
    suspend fun clearAiReports()
}
