package com.example.todoalarm.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query(
        """
        SELECT * FROM todo_items
        WHERE completed = 0
        ORDER BY dueDateEpochDay ASC,
        CASE WHEN reminderAtMillis IS NULL THEN 1 ELSE 0 END ASC,
        reminderAtMillis ASC
        """
    )
    fun observeActiveTodos(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TodoItem?

    @Insert
    suspend fun insert(item: TodoItem): Long

    @Update
    suspend fun update(item: TodoItem)

    @Query(
        """
        SELECT * FROM todo_items
        WHERE completed = 0
        AND reminderEnabled = 1
        AND reminderAtMillis IS NOT NULL
        AND reminderAtMillis >= :now
        ORDER BY reminderAtMillis ASC
        """
    )
    suspend fun getFutureReminderItems(now: Long): List<TodoItem>
}

