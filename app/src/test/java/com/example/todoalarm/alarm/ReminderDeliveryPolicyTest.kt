package com.example.todoalarm.alarm

import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.TodoItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderDeliveryPolicyTest {
    @Test
    fun notificationModeDoesNotForceFullscreen() {
        val item = reminderItem(mode = ReminderDeliveryMode.NOTIFICATION)

        assertFalse(shouldUseFullscreenReminder(item, workQuietModeEnabled = false))
    }

    @Test
    fun fullscreenModeRequestsFullscreen() {
        val item = reminderItem(mode = ReminderDeliveryMode.FULLSCREEN)

        assertTrue(shouldUseFullscreenReminder(item, workQuietModeEnabled = false))
    }

    @Test
    fun workQuietModeOverridesNotificationMode() {
        val item = reminderItem(mode = ReminderDeliveryMode.NOTIFICATION)

        assertTrue(shouldUseFullscreenReminder(item, workQuietModeEnabled = true))
    }

    @Test
    fun alarmModeOverridesNotificationMode() {
        val item = reminderItem(
            mode = ReminderDeliveryMode.NOTIFICATION,
            alarmMode = true
        )

        assertTrue(shouldUseFullscreenReminder(item, workQuietModeEnabled = false))
    }

    private fun reminderItem(
        mode: ReminderDeliveryMode,
        alarmMode: Boolean = false
    ): TodoItem {
        return TodoItem(
            id = 1L,
            title = "测试提醒",
            dueAtMillis = System.currentTimeMillis() + 60_000L,
            reminderAtMillis = System.currentTimeMillis() + 30_000L,
            reminderEnabled = true,
            ringEnabled = true,
            vibrateEnabled = true,
            alarmMode = alarmMode,
            reminderDeliveryMode = mode.name
        )
    }
}
