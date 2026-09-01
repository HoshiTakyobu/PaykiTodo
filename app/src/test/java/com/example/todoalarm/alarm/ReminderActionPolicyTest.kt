package com.example.todoalarm.alarm

import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.TodoItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderActionPolicyTest {
    @Test
    fun intermediateTodoReminderDoesNotOfferSnooze() {
        val dueAt = 10_000_000L
        val item = reminderTodo(dueAt, "30,15,5")

        assertFalse(shouldOfferReminderSnooze(item, dueAt - 30 * 60_000L))
        assertFalse(shouldOfferReminderSnooze(item, dueAt - 15 * 60_000L))
    }

    @Test
    fun finalTodoReminderOffersSnooze() {
        val dueAt = 10_000_000L
        val item = reminderTodo(dueAt, "30,15,5")

        assertTrue(shouldOfferReminderSnooze(item, dueAt - 5 * 60_000L))
    }

    @Test
    fun multiReminderWithoutTriggerTimeDoesNotOfferSnooze() {
        val item = reminderTodo(10_000_000L, "30,15,5")

        assertFalse(shouldOfferReminderSnooze(item, null))
    }

    @Test
    fun eventReminderNeverOffersSnooze() {
        val item = reminderTodo(10_000_000L, "15").copy(itemType = "EVENT")

        assertFalse(shouldOfferReminderSnooze(item, item.reminderAtMillis))
    }

    @Test
    fun notificationOnlyOpensActiveItem() {
        val item = reminderTodo(10_000_000L, "5")

        assertTrue(shouldOpenReminderNotification(item))
        assertFalse(shouldOpenReminderNotification(item.copy(completed = true)))
        assertFalse(shouldOpenReminderNotification(item.copy(canceled = true)))
        assertFalse(shouldOpenReminderNotification(null))
    }

    private fun reminderTodo(dueAt: Long, offsets: String) = TodoItem(
        id = 1L,
        title = "测试提醒",
        dueAtMillis = dueAt,
        reminderAtMillis = null,
        reminderEnabled = true,
        ringEnabled = true,
        vibrateEnabled = true,
        reminderOffsetsCsv = offsets,
        reminderDeliveryMode = ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION.name
    )
}
