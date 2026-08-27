package com.example.todoalarm.data

enum class ReminderDeliveryMode(val label: String) {
    NOTIFICATION("通知栏提醒"),
    FULLSCREEN("全屏界面提醒"),
    FULLSCREEN_AND_NOTIFICATION("全屏 + 通知栏");

    val usesFullscreen: Boolean
        get() = this == FULLSCREEN || this == FULLSCREEN_AND_NOTIFICATION

    val usesNotification: Boolean
        get() = this == NOTIFICATION || this == FULLSCREEN_AND_NOTIFICATION

    companion object {
        fun fromStorage(value: String?): ReminderDeliveryMode {
            return entries.firstOrNull { it.name == value } ?: FULLSCREEN
        }

        fun fromSelections(
            fullscreen: Boolean,
            notification: Boolean
        ): ReminderDeliveryMode? = when {
            fullscreen && notification -> FULLSCREEN_AND_NOTIFICATION
            fullscreen -> FULLSCREEN
            notification -> NOTIFICATION
            else -> null
        }
    }
}
