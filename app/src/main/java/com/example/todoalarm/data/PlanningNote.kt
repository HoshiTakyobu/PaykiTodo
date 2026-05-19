package com.example.todoalarm.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "planning_notes",
    indices = [
        Index(
            value = ["archived", "hasAnnouncementHint", "updatedAtMillis", "createdAtMillis"],
            name = "index_planning_notes_announcement_lookup"
        )
    ]
)
data class PlanningNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val contentMarkdown: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
    @ColumnInfo(defaultValue = "NULL")
    val documentDateEpochDay: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val hasAnnouncementHint: Boolean = PlanningAnnouncementParser.mightContainAnnouncement(contentMarkdown)
)
