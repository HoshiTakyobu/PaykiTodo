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
        AND reminderAtMillis IS NOT NULL
        AND reminderAtMillis >= :now
        ORDER BY reminderAtMillis ASC
        """
    )
    suspend fun getFutureReminderItems(now: Long): List<TodoItem>

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
}
