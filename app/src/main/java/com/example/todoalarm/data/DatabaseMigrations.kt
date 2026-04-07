package com.example.todoalarm.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE todo_items ADD COLUMN canceled INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE todo_items ADD COLUMN canceledAtMillis INTEGER")
            database.execSQL("ALTER TABLE todo_items ADD COLUMN missed INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE todo_items ADD COLUMN missedAtMillis INTEGER")
            database.execSQL("ALTER TABLE todo_items ADD COLUMN recurringSeriesId TEXT")
            database.execSQL("ALTER TABLE todo_items ADD COLUMN recurrenceType TEXT NOT NULL DEFAULT 'NONE'")
            database.execSQL("ALTER TABLE todo_items ADD COLUMN recurrenceWeekdays TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE todo_items ADD COLUMN recurrenceMonthlyOrdinal INTEGER")
            database.execSQL("ALTER TABLE todo_items ADD COLUMN recurrenceMonthlyWeekday INTEGER")
            database.execSQL("ALTER TABLE todo_items ADD COLUMN recurrenceMonthlyDay INTEGER")
            database.execSQL("ALTER TABLE todo_items ADD COLUMN recurrenceEndEpochDay INTEGER")
            database.execSQL("ALTER TABLE todo_items ADD COLUMN recurrenceAnchorDueAtMillis INTEGER")
            database.execSQL("ALTER TABLE todo_items ADD COLUMN reminderOffsetMinutes INTEGER")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE todo_items ADD COLUMN groupId INTEGER NOT NULL DEFAULT 0")

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `task_groups` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `colorHex` TEXT NOT NULL,
                    `sortOrder` INTEGER NOT NULL,
                    `isDefault` INTEGER NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recurring_task_templates` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `seriesId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `notes` TEXT NOT NULL,
                    `groupId` INTEGER NOT NULL,
                    `dueHour` INTEGER NOT NULL,
                    `dueMinute` INTEGER NOT NULL,
                    `reminderOffsetMinutes` INTEGER,
                    `ringEnabled` INTEGER NOT NULL,
                    `vibrateEnabled` INTEGER NOT NULL,
                    `recurrenceType` TEXT NOT NULL,
                    `recurrenceWeekdays` TEXT NOT NULL,
                    `recurrenceMonthlyOrdinal` INTEGER,
                    `recurrenceMonthlyWeekday` INTEGER,
                    `recurrenceMonthlyDay` INTEGER,
                    `recurrenceYearlyMonth` INTEGER,
                    `recurrenceYearlyDay` INTEGER,
                    `startEpochDay` INTEGER NOT NULL,
                    `endEpochDay` INTEGER NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_recurring_task_templates_seriesId` ON `recurring_task_templates` (`seriesId`)"
            )

            val now = System.currentTimeMillis()
            database.execSQL(
                """
                INSERT OR IGNORE INTO task_groups (id, name, colorHex, sortOrder, isDefault, createdAtMillis)
                VALUES
                (1, '重要', '#BF7B4D', 0, 1, $now),
                (2, '紧急', '#FF6B4A', 1, 1, $now),
                (3, '专注', '#4E87E1', 2, 1, $now),
                (4, '例行', '#4CB782', 3, 1, $now)
                """.trimIndent()
            )

            database.execSQL("UPDATE todo_items SET groupId = 1 WHERE categoryKey = 'important'")
            database.execSQL("UPDATE todo_items SET groupId = 2 WHERE categoryKey = 'urgent'")
            database.execSQL("UPDATE todo_items SET groupId = 3 WHERE categoryKey = 'focus'")
            database.execSQL(
                """
                UPDATE todo_items
                SET groupId = 4
                WHERE groupId = 0 OR categoryKey = 'routine'
                """.trimIndent()
            )
        }
    }
}
