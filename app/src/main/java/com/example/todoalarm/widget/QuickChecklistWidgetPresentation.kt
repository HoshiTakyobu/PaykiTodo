package com.example.todoalarm.widget

internal data class QuickChecklistWidgetTask(
    val id: Long,
    val groupId: Long,
    val title: String,
    val depth: Int,
    val completed: Boolean,
    val canceled: Boolean
)

internal data class QuickChecklistWidgetSummary(
    val groupId: Long,
    val completedCount: Int,
    val totalCount: Int
)

internal object QuickChecklistWidgetPresentation {
    fun summaries(tasks: List<QuickChecklistWidgetTask>): Map<Long, QuickChecklistWidgetSummary> {
        return tasks
            .filterNot { it.canceled }
            .groupBy { it.groupId }
            .mapValues { (groupId, groupTasks) ->
                QuickChecklistWidgetSummary(
                    groupId = groupId,
                    completedCount = groupTasks.count { it.completed },
                    totalCount = groupTasks.size
                )
            }
    }

    fun groupLabel(title: String, summary: QuickChecklistWidgetSummary?): String {
        return if (summary == null) {
            "清单 · $title"
        } else {
            "清单 · $title · ${summary.completedCount}/${summary.totalCount}"
        }
    }

    fun taskTitle(task: QuickChecklistWidgetTask): String {
        val marker = if (task.completed) "☑" else "☐"
        val indent = "  ".repeat((task.depth - 1).coerceAtLeast(0))
        return "$marker $indent${task.title}"
    }
}
