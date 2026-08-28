package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseMigrationMetadataTest {
    @Test
    fun checkInReminderMigrationTargetsVersion30() {
        assertEquals(29, DatabaseMigrations.MIGRATION_29_30.startVersion)
        assertEquals(30, DatabaseMigrations.MIGRATION_29_30.endVersion)
    }
}
