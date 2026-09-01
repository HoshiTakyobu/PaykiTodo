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
            // Older backups may still contain one-channel values. The current
            // product contract treats every enabled reminder as both surfaces.
            return when (value?.uppercase()) {
                NOTIFICATION.name, FULLSCREEN.name, FULLSCREEN_AND_NOTIFICATION.name -> FULLSCREEN_AND_NOTIFICATION
                else -> FULLSCREEN_AND_NOTIFICATION
            }
        }

        fun fromSelections(
            fullscreen: Boolean,
            notification: Boolean
        ): ReminderDeliveryMode? = when {
            fullscreen || notification -> FULLSCREEN_AND_NOTIFICATION
            else -> null
        }
    }
}
