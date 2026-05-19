package com.example.todoalarm.data

import java.time.LocalDateTime

enum class PlanningNodeResolvedType {
    TODO,
    EVENT
}

data class PlanningNodeDraft(
    val noteId: Long,
    val parentNodeId: Long? = null,
    val text: String,
    val sortOrder: Int? = null,
    val dueAt: LocalDateTime? = null,
    val startAt: LocalDateTime? = null,
    val endAt: LocalDateTime? = null,
    val location: String? = null,
    val collapsed: Boolean = false,
    val completed: Boolean = false
)

data class PlanningNodeEdit(
    val text: String,
    val parentNodeId: Long?,
    val sortOrder: Int,
    val dueAt: LocalDateTime?,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val location: String?,
    val collapsed: Boolean,
    val completed: Boolean
)

data class PlanningNodeChangeResult(
    val node: PlanningNode,
    val linkedItem: TodoItem?,
    val deletedLinkedItem: TodoItem? = null
)

data class PlanningNodeMarkdownImportResult(
    val created: List<PlanningNodeChangeResult>,
    val deletedLinkedItems: List<TodoItem> = emptyList()
)
