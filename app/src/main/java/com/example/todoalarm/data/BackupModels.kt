package com.example.todoalarm.data

data class BackupSnapshot(
    val exportedAtMillis: Long,
    val groups: List<TaskGroup>,
    val templates: List<RecurringTaskTemplate>,
    val tasks: List<TodoItem>,
    val settings: AppSettings
)
