package com.example.todoalarm.data

data class BackupSnapshot(
    val exportedAtMillis: Long,
    val groups: List<TaskGroup>,
    val templates: List<RecurringTaskTemplate>,
    val tasks: List<TodoItem>,
    val pendingQuoteVersion: Int = 1,
    val reminderChainLogs: List<ReminderChainLog> = emptyList(),
    val scheduleTemplates: List<ScheduleTemplate> = emptyList(),
    val planningNotes: List<PlanningNote> = emptyList(),
    val planningLineMappings: List<PlanningLineMapping> = emptyList(),
    val aiReports: List<AiReport> = emptyList(),
    val todoGroupTags: List<TodoGroupTag> = emptyList(),
    val eventCheckIns: List<EventCheckIn> = emptyList(),
    val settings: AppSettings
)
