package com.example.todoalarm.data

data class DataHealthReport(
    val oldCompletedTodos: Int = 0,
    val emptyPlanningNotes: Int = 0,
    val staleDraftNodes: Int = 0,
    val expiredAiReports: Int = 0,
    val overdueTodosWithoutReminder: Int = 0
) {
    val safeCleanableCount: Int
        get() = oldCompletedTodos + emptyPlanningNotes + expiredAiReports

    val attentionCount: Int
        get() = staleDraftNodes + overdueTodosWithoutReminder

    val hasIssues: Boolean
        get() = safeCleanableCount > 0 || attentionCount > 0
}

data class DataHealthCleanResult(
    val deletedCompletedTodos: Int = 0,
    val deletedEmptyPlanningNotes: Int = 0,
    val deletedExpiredAiReports: Int = 0
) {
    val totalDeleted: Int
        get() = deletedCompletedTodos + deletedEmptyPlanningNotes + deletedExpiredAiReports

    fun message(): String {
        return if (totalDeleted == 0) {
            "没有需要清理的安全项。"
        } else {
            "已清理 ${deletedCompletedTodos} 条已完成待办、${deletedEmptyPlanningNotes} 个空规划文档、${deletedExpiredAiReports} 条过期 AI 报告。"
        }
    }
}
