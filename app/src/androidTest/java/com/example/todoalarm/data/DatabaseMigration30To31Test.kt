package com.example.todoalarm.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigration30To31Test {
    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrationPreservesExistingRowsAndAddsChecklistColumns() {
        val databaseName = "migration-30-to-31.db"
        helper.createDatabase(databaseName, 30).use { database ->
            database.insert(
                "task_groups",
                SQLiteDatabase.CONFLICT_NONE,
                ContentValues().apply {
                    put("id", 41L)
                    put("name", "旧分组")
                    put("colorHex", "#4CB782")
                    put("sortOrder", 0)
                    put("isDefault", 1)
                    put("createdAtMillis", 100L)
                }
            )
            database.insert("todo_items", SQLiteDatabase.CONFLICT_NONE, legacyTodoValues())
        }

        helper.runMigrationsAndValidate(
            databaseName,
            31,
            true,
            DatabaseMigrations.MIGRATION_30_31
        ).use { database ->
            database.query("SELECT title, quickChecklistGroupId, quickChecklistCollapsed FROM todo_items WHERE id = 51")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("迁移前任务", cursor.getString(cursor.getColumnIndexOrThrow("title")))
                    assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("quickChecklistGroupId")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("quickChecklistCollapsed")))
                }
            database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'quick_checklist_groups'")
                .use { cursor -> assertTrue(cursor.moveToFirst()) }
            database.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_todo_items_quick_checklist_parent'")
                .use { cursor -> assertTrue(cursor.moveToFirst()) }
        }
    }

    private fun legacyTodoValues() = ContentValues().apply {
        put("id", 51L)
        put("itemType", "TODO")
        put("title", "迁移前任务")
        put("notes", "旧备注")
        put("dueAtMillis", 1_700_000_000_000L)
        putNull("startAtMillis")
        putNull("endAtMillis")
        put("allDay", 0)
        put("location", "")
        putNull("accentColorHex")
        put("countdownEnabled", 0)
        put("hiddenFromBoard", 0)
        put("checkInEnabled", 0)
        put("checkInReminderOffsetsCsv", "")
        put("totalCheckInMinutes", 0)
        putNull("reminderAtMillis")
        put("reminderOffsetsCsv", "")
        put("reminderEnabled", 0)
        put("ringEnabled", 0)
        put("vibrateEnabled", 0)
        put("alarmMode", 0)
        put("voiceEnabled", 0)
        put("reminderDeliveryMode", ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION.name)
        put("groupId", 41L)
        put("categoryKey", TodoCategory.ROUTINE.key)
        put("completed", 0)
        putNull("completedAtMillis")
        put("canceled", 0)
        putNull("canceledAtMillis")
        put("missed", 0)
        putNull("missedAtMillis")
        putNull("recurringSeriesId")
        putNull("multiSlotBundleId")
        put("recurrenceType", RecurrenceType.NONE.name)
        put("recurrenceWeekdays", "")
        putNull("recurrenceMonthlyOrdinal")
        putNull("recurrenceMonthlyWeekday")
        putNull("recurrenceMonthlyDay")
        putNull("recurrenceEndEpochDay")
        putNull("recurrenceAnchorDueAtMillis")
        putNull("reminderOffsetMinutes")
        put("createdAtMillis", 101L)
    }
}
