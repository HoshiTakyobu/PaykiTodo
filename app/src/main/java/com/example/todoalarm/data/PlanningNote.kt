package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planning_notes")
data class PlanningNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val contentMarkdown: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val archived: Boolean = false
)
