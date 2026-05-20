package com.example.todoalarm.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "planning_nodes",
    foreignKeys = [
        ForeignKey(
            entity = PlanningNote::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlanningNode::class,
            parentColumns = ["id"],
            childColumns = ["parentNodeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TodoItem::class,
            parentColumns = ["id"],
            childColumns = ["linkedTodoId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("noteId"),
        Index("parentNodeId"),
        Index("linkedTodoId"),
        Index("linkedEndTodoId")
    ]
)
data class PlanningNode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val parentNodeId: Long? = null,
    val sortOrder: Int,
    val text: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val startAtMillis: Long? = null,
    val endAtMillis: Long? = null,
    val dueAtMillis: Long? = null,
    val location: String? = null,
    val linkedTodoId: Long? = null,
    val linkedEndTodoId: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val isDraft: Boolean = false,
    @ColumnInfo(defaultValue = "1")
    val syncEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val collapsed: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val completed: Boolean = false,
    val completedAtMillis: Long? = null
)
