package com.example.todoalarm.data

data class BackupSnapshot(
    val exportedAtMillis: Long,
    val groups: List<TaskGroup>,
    val templates: List<RecurringTaskTemplate>,
    val tasks: List<TodoItem>,
    val reminderChainLogs: List<ReminderChainLog> = emptyList(),
    val scheduleTemplates: List<ScheduleTemplate> = emptyList(),
    val settings: AppSettings
)
