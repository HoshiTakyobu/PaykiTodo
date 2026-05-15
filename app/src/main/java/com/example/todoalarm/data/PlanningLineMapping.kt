package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MappingStatus {
    ACTIVE,
    COMPLETED,
    CANCELED,
    ORPHANED,
    CONFLICT
}

@Entity(
    tableName = "planning_line_mappings",
    indices = [
        Index("noteId"),
        Index("batchId"),
        Index("todoId"),
        Index("eventId")
    ]
)
data class PlanningLineMapping(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val contentFingerprint: String,
    val originalLineText: String,
    val currentLineText: String = "",
    val todoId: Long? = null,
    val eventId: Long? = null,
    val batchId: String,
    val operationType: String = "IMPORT",
    val createdAtMillis: Long,
    val lastRefreshedAtMillis: Long,
    val status: MappingStatus = MappingStatus.ACTIVE,
    val postponeOffsetMinutes: Int = 0,
    val lastKnownLineNumber: Int = 0
) {
    val itemId: Long?
        get() = todoId ?: eventId

    val trackedLineText: String
        get() = currentLineText.ifBlank { originalLineText }
}
