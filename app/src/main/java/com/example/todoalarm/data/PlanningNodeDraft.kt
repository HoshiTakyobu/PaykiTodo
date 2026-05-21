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
    val notes: String = "",
    val groupId: Long = 0,
    val groupName: String = "",
    val sortOrder: Int? = null,
    val dueAt: LocalDateTime? = null,
    val startAt: LocalDateTime? = null,
    val endAt: LocalDateTime? = null,
    val location: String? = null,
    val reminderOffsetsMinutes: List<Int>? = null,
    val allDay: Boolean = false,
    val countdownEnabled: Boolean = false,
    val checkInEnabled: Boolean = false,
    val isDraft: Boolean = true,
    val isNote: Boolean = false,
    val syncEnabled: Boolean = true,
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
    val isNote: Boolean,
    val syncEnabled: Boolean,
    val collapsed: Boolean,
    val completed: Boolean
)

data class PlanningNodeChangeResult(
    val node: PlanningNode,
    val linkedItem: TodoItem?,
    val deletedLinkedItem: TodoItem? = null,
    val deletedLinkedItems: List<TodoItem> = deletedLinkedItem?.let { listOf(it) }.orEmpty(),
    val affectedLinkedItems: List<TodoItem> = linkedItem?.let { listOf(it) }.orEmpty()
)

data class PlanningNodePublishBatchResult(
    val published: List<PlanningNodeChangeResult>,
    val failedCount: Int
)

data class PlanningNodeMarkdownImportResult(
    val created: List<PlanningNodeChangeResult>,
    val deletedLinkedItems: List<TodoItem> = emptyList()
)

data class PlanningNodeDeleteResult(
    val deletedLinkedItems: List<TodoItem> = emptyList(),
    val affectedLinkedItems: List<TodoItem> = emptyList()
)

data class PlanningNodeSnapshot(
    val noteId: Long,
    val nodes: List<PlanningNode>,
    val linkedItems: List<TodoItem> = emptyList(),
    val todoGroupTags: List<TodoGroupTag> = emptyList(),
    val eventCheckIns: List<EventCheckIn> = emptyList()
)

data class PlanningNodeRestoreResult(
    val affectedBeforeItems: List<TodoItem> = emptyList(),
    val affectedAfterItems: List<TodoItem> = emptyList()
)
