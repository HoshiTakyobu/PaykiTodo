package com.example.todoalarm.data

enum class ReminderDeliveryMode(val label: String) {
    NOTIFICATION("通知栏提醒"),
    FULLSCREEN("全屏界面提醒");

    companion object {
        fun fromStorage(value: String?): ReminderDeliveryMode {
            return entries.firstOrNull { it.name == value } ?: FULLSCREEN
        }
    }
}
