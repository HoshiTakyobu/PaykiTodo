package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_sessions",
    indices = [
        Index("startedAtMillis"),
        Index("todoId")
    ]
)
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val todoId: Long? = null,
    val title: String,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val completed: Boolean,
    val extensionCount: Int = 0
)

data class FocusSessionStats(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val completedMinutes: Int = 0
)
