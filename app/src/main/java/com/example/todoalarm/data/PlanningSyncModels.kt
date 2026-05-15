package com.example.todoalarm.data

data class PlanningMappingStatusSnapshot(
    val mappings: List<PlanningLineMapping>,
    val changedCount: Int
)

data class PlanningOperationResult(
    val message: String,
    val updatedMarkdown: String? = null,
    val affectedBeforeItems: List<TodoItem> = emptyList(),
    val affectedAfterItems: List<TodoItem> = emptyList(),
    val refreshedCount: Int = 0,
    val skippedCount: Int = 0,
    val orphanedCount: Int = 0,
    val conflictCount: Int = 0,
    val batchId: String? = null
)

enum class PlanningPostponeScope {
    FROM_ITEM_TO_SECTION_END,
    FROM_ITEM_TO_DOCUMENT_END,
    CURRENT_SECTION_ALL;

    companion object {
        fun fromStorage(value: String?): PlanningPostponeScope {
            return entries.firstOrNull { it.name == value } ?: FROM_ITEM_TO_SECTION_END
        }
    }
}

enum class PlanningRefreshScope {
    CURRENT_SECTION,
    WHOLE_DOCUMENT;

    companion object {
        fun fromStorage(value: String?): PlanningRefreshScope {
            return entries.firstOrNull { it.name == value } ?: CURRENT_SECTION
        }
    }
}
