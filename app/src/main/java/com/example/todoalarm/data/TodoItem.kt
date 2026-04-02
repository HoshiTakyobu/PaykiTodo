package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val dueDateEpochDay: Long,
    val reminderAtMillis: Long?,
    val reminderEnabled: Boolean,
    val ringEnabled: Boolean,
    val vibrateEnabled: Boolean,
    val voiceEnabled: Boolean,
    val completed: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

