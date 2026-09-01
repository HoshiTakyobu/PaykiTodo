package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderDeliveryModeTest {
    @Test
    fun anyEnabledReminderUsesBothDeliverySurfaces() {
        assertEquals(
            ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION,
            ReminderDeliveryMode.fromSelections(fullscreen = true, notification = false)
        )
        assertEquals(
            ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION,
            ReminderDeliveryMode.fromSelections(fullscreen = false, notification = true)
        )
        assertEquals(
            ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION,
            ReminderDeliveryMode.fromSelections(fullscreen = true, notification = true)
        )
        assertEquals(null, ReminderDeliveryMode.fromSelections(fullscreen = false, notification = false))
    }

    @Test
    fun legacyStorageIsUpgradedToBothDeliverySurfaces() {
        ReminderDeliveryMode.entries.forEach { mode ->
            assertEquals(ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION, ReminderDeliveryMode.fromStorage(mode.name))
        }
    }
}
