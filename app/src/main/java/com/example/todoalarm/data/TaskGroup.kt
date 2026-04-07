package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_groups")
data class TaskGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val sortOrder: Int,
    val isDefault: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

object DefaultTaskGroups {
    val seed = listOf(
        TaskGroup(name = "重要", colorHex = "#BF7B4D", sortOrder = 0, isDefault = true),
        TaskGroup(name = "紧急", colorHex = "#FF6B4A", sortOrder = 1, isDefault = true),
        TaskGroup(name = "专注", colorHex = "#4E87E1", sortOrder = 2, isDefault = true),
        TaskGroup(name = "例行", colorHex = "#4CB782", sortOrder = 3, isDefault = true)
    )
}
