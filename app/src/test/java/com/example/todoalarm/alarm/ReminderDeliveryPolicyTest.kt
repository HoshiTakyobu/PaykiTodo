package com.example.todoalarm.alarm

import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.TodoItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderDeliveryPolicyTest {
    @Test
    fun notificationModeDoesNotRequestFullscreen() {
        val item = reminderItem(mode = ReminderDeliveryMode.NOTIFICATION)

        assertFalse(shouldUseFullscreenReminder(item))
        assertTrue(item.reminderDeliveryModeEnum.usesNotification)
    }

    @Test
    fun fullscreenModeRequestsFullscreenWithoutNotificationSemantics() {
        val item = reminderItem(mode = ReminderDeliveryMode.FULLSCREEN)

        assertTrue(shouldUseFullscreenReminder(item))
        assertFalse(item.reminderDeliveryModeEnum.usesNotification)
    }

    @Test
    fun combinedModeRequestsFullscreenAndKeepsNotificationSemantics() {
        val item = reminderItem(mode = ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION)

        assertTrue(shouldUseFullscreenReminder(item))
        assertTrue(item.reminderDeliveryModeEnum.usesNotification)
    }

    @Test
    fun alarmModeDoesNotOverrideNotificationOnlySelection() {
        val item = reminderItem(
            mode = ReminderDeliveryMode.NOTIFICATION,
            alarmMode = true
        )

        assertFalse(shouldUseFullscreenReminder(item))
    }

    @Test
    fun disabledReminderDoesNotRequestFullscreenEvenWithStoredFullscreenMode() {
        val item = reminderItem(mode = ReminderDeliveryMode.FULLSCREEN).copy(reminderEnabled = false)

        assertFalse(shouldUseFullscreenReminder(item))
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
