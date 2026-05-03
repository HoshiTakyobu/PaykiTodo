package com.example.todoalarm.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `todo_items_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `notes` TEXT NOT NULL,
                    `dueAtMillis` INTEGER NOT NULL,
                    `reminderAtMillis` INTEGER,
                    `reminderEnabled` INTEGER NOT NULL,
                    `ringEnabled` INTEGER NOT NULL,
                    `vibrateEnabled` INTEGER NOT NULL,
                    `voiceEnabled` INTEGER NOT NULL,
                    `categoryKey` TEXT NOT NULL,
                    `completed` INTEGER NOT NULL,
                    `completedAtMillis` INTEGER,
                    `createdAtMillis` INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.query(
                """
                SELECT id, title, notes, dueDateEpochDay, reminderAtMillis, reminderEnabled,
                       ringEnabled, vibrateEnabled, voiceEnabled, completed, createdAtMillis
                FROM todo_items
                """.trimIndent()
            ).use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow("id")
                val titleIndex = cursor.getColumnIndexOrThrow("title")
                val notesIndex = cursor.getColumnIndexOrThrow("notes")
                val dueDateEpochDayIndex = cursor.getColumnIndexOrThrow("dueDateEpochDay")
                val reminderAtMillisIndex = cursor.getColumnIndexOrThrow("reminderAtMillis")
                val reminderEnabledIndex = cursor.getColumnIndexOrThrow("reminderEnabled")
                val ringEnabledIndex = cursor.getColumnIndexOrThrow("ringEnabled")
                val vibrateEnabledIndex = cursor.getColumnIndexOrThrow("vibrateEnabled")
                val voiceEnabledIndex = cursor.getColumnIndexOrThrow("voiceEnabled")
                val completedIndex = cursor.getColumnIndexOrThrow("completed")
                val createdAtMillisIndex = cursor.getColumnIndexOrThrow("createdAtMillis")

                while (cursor.moveToNext()) {
                    val dueAtMillis = LocalDate.ofEpochDay(cursor.getLong(dueDateEpochDayIndex))
                        .atTime(LocalTime.of(23, 59))
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()

                    db.execSQL(
                        """
                        INSERT INTO `todo_items_new` (
                            `id`, `title`, `notes`, `dueAtMillis`, `reminderAtMillis`,
                            `reminderEnabled`, `ringEnabled`, `vibrateEnabled`, `voiceEnabled`,
                            `categoryKey`, `completed`, `completedAtMillis`, `createdAtMillis`
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                        arrayOf(
                            cursor.getLong(idIndex),
                            cursor.getString(titleIndex),
                            cursor.getString(notesIndex),
                            dueAtMillis,
                            if (cursor.isNull(reminderAtMillisIndex)) null else cursor.getLong(reminderAtMillisIndex),
                            cursor.getInt(reminderEnabledIndex),
                            cursor.getInt(ringEnabledIndex),
                            cursor.getInt(vibrateEnabledIndex),
                            cursor.getInt(voiceEnabledIndex),
                            TodoCategory.ROUTINE.key,
                            cursor.getInt(completedIndex),
                            null,
                            cursor.getLong(createdAtMillisIndex)
                        )
                    )
                }
            }

            db.execSQL("DROP TABLE `todo_items`")
            db.execSQL("ALTER TABLE `todo_items_new` RENAME TO `todo_items`")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE todo_items ADD COLUMN canceled INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN canceledAtMillis INTEGER")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN missed INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN missedAtMillis INTEGER")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN recurringSeriesId TEXT")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN recurrenceType TEXT NOT NULL DEFAULT 'NONE'")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN recurrenceWeekdays TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN recurrenceMonthlyOrdinal INTEGER")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN recurrenceMonthlyWeekday INTEGER")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN recurrenceMonthlyDay INTEGER")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN recurrenceEndEpochDay INTEGER")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN recurrenceAnchorDueAtMillis INTEGER")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN reminderOffsetMinutes INTEGER")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE todo_items ADD COLUMN groupId INTEGER NOT NULL DEFAULT 0")

            db.execSQL(
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

            db.execSQL(
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
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_recurring_task_templates_seriesId` ON `recurring_task_templates` (`seriesId`)"
            )

            val now = System.currentTimeMillis()
            db.execSQL(
                """
                INSERT OR IGNORE INTO task_groups (id, name, colorHex, sortOrder, isDefault, createdAtMillis)
                VALUES
                (1, '重要', '#BF7B4D', 0, 1, $now),
                (2, '紧急', '#FF6B4A', 1, 1, $now),
                (3, '专注', '#4E87E1', 2, 1, $now),
                (4, '例行', '#4CB782', 3, 1, $now)
                """.trimIndent()
            )

            db.execSQL("UPDATE todo_items SET groupId = 1 WHERE categoryKey = 'important'")
            db.execSQL("UPDATE todo_items SET groupId = 2 WHERE categoryKey = 'urgent'")
            db.execSQL("UPDATE todo_items SET groupId = 3 WHERE categoryKey = 'focus'")
            db.execSQL(
                """
                UPDATE todo_items
                SET groupId = 4
                WHERE groupId = 0 OR categoryKey = 'routine'
                """.trimIndent()
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE todo_items ADD COLUMN itemType TEXT NOT NULL DEFAULT 'TODO'")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN startAtMillis INTEGER")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN endAtMillis INTEGER")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN allDay INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN location TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE todo_items ADD COLUMN accentColorHex TEXT")

            db.execSQL("ALTER TABLE recurring_task_templates ADD COLUMN itemType TEXT NOT NULL DEFAULT 'TODO'")
            db.execSQL("ALTER TABLE recurring_task_templates ADD COLUMN location TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recurring_task_templates ADD COLUMN accentColorHex TEXT")
            db.execSQL("ALTER TABLE recurring_task_templates ADD COLUMN allDay INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE recurring_task_templates ADD COLUMN eventDurationMinutes INTEGER")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE todo_items ADD COLUMN reminderDeliveryMode TEXT NOT NULL DEFAULT 'FULLSCREEN'")
            db.execSQL("ALTER TABLE recurring_task_templates ADD COLUMN reminderDeliveryMode TEXT NOT NULL DEFAULT 'FULLSCREEN'")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reminder_chain_logs` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `todoId` INTEGER NOT NULL,
                    `chainKey` TEXT NOT NULL,
                    `source` TEXT NOT NULL,
                    `stage` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `message` TEXT,
                    `reminderAtMillis` INTEGER,
                    `createdAtMillis` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_chain_logs_createdAtMillis` ON `reminder_chain_logs` (`createdAtMillis`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_chain_logs_todoId_createdAtMillis` ON `reminder_chain_logs` (`todoId`, `createdAtMillis`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_chain_logs_chainKey` ON `reminder_chain_logs` (`chainKey`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `schedule_templates` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `templateType` TEXT NOT NULL,
                    `payloadJson` TEXT NOT NULL,
                    `accentColorHex` TEXT,
                    `createdAtMillis` INTEGER NOT NULL,
                    `updatedAtMillis` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_templates_templateType_updatedAtMillis` ON `schedule_templates` (`templateType`, `updatedAtMillis`)")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE todo_items ADD COLUMN reminderOffsetsCsv TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recurring_task_templates ADD COLUMN reminderOffsetsCsv TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE todo_items SET reminderOffsetsCsv = CAST(reminderOffsetMinutes AS TEXT) WHERE reminderOffsetMinutes IS NOT NULL AND reminderOffsetsCsv = ''")
            db.execSQL("UPDATE recurring_task_templates SET reminderOffsetsCsv = CAST(reminderOffsetMinutes AS TEXT) WHERE reminderOffsetMinutes IS NOT NULL AND reminderOffsetsCsv = ''")
        }
    }
}
