package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderDeliveryModeTest {
    @Test
    fun selectionsPreserveAllFourReminderStates() {
        assertNull(ReminderDeliveryMode.fromSelections(fullscreen = false, notification = false))
        assertEquals(
            ReminderDeliveryMode.FULLSCREEN,
            ReminderDeliveryMode.fromSelections(fullscreen = true, notification = false)
        )
        assertEquals(
            ReminderDeliveryMode.NOTIFICATION,
            ReminderDeliveryMode.fromSelections(fullscreen = false, notification = true)
        )
        assertEquals(
            ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION,
            ReminderDeliveryMode.fromSelections(fullscreen = true, notification = true)
        )
    }

    @Test
    fun storageRoundTripKeepsSingleChannelSelections() {
        ReminderDeliveryMode.entries.forEach { mode ->
            assertEquals(mode, ReminderDeliveryMode.fromStorage(mode.name))
        }
    }
}
