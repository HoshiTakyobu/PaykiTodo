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
                (3, '例行', '#4CB782', 2, 1, $now)
                """.trimIndent()
            )

            db.execSQL("UPDATE todo_items SET groupId = 1 WHERE categoryKey = 'important'")
            db.execSQL("UPDATE todo_items SET groupId = 2 WHERE categoryKey = 'urgent'")
            db.execSQL("UPDATE todo_items SET groupId = 3 WHERE categoryKey = 'focus'")
            db.execSQL(
                """
                UPDATE todo_items
                SET groupId = 3
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

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            if (tableExists(db, "planning_notes")) {
                rebuildPlanningNotesTable(db)
            } else {
                createPlanningNotesTable(db, "planning_notes")
            }
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1.7.0-1.7.4 created planning_notes through a migration whose schema did not
            // exactly match the Room entity. Rebuild it so upgraded 1.6.x databases can open.
            rebuildPlanningNotesTable(db)
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `planning_line_mappings` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `noteId` INTEGER NOT NULL,
                    `contentFingerprint` TEXT NOT NULL,
                    `originalLineText` TEXT NOT NULL,
                    `currentLineText` TEXT NOT NULL,
                    `todoId` INTEGER,
                    `eventId` INTEGER,
                    `batchId` TEXT NOT NULL,
                    `operationType` TEXT NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL,
                    `lastRefreshedAtMillis` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `postponeOffsetMinutes` INTEGER NOT NULL,
                    `lastKnownLineNumber` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planning_line_mappings_noteId` ON `planning_line_mappings` (`noteId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planning_line_mappings_batchId` ON `planning_line_mappings` (`batchId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planning_line_mappings_todoId` ON `planning_line_mappings` (`todoId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planning_line_mappings_eventId` ON `planning_line_mappings` (`eventId`)")
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `focus_sessions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `todoId` INTEGER,
                    `title` TEXT NOT NULL,
                    `plannedMinutes` INTEGER NOT NULL,
                    `actualMinutes` INTEGER NOT NULL,
                    `startedAtMillis` INTEGER NOT NULL,
                    `endedAtMillis` INTEGER NOT NULL,
                    `completed` INTEGER NOT NULL,
                    `extensionCount` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_startedAtMillis` ON `focus_sessions` (`startedAtMillis`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_todoId` ON `focus_sessions` (`todoId`)")
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `ai_reports` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `type` TEXT NOT NULL,
                    `generatedAtMillis` INTEGER NOT NULL,
                    `periodStartMillis` INTEGER NOT NULL,
                    `periodEndMillis` INTEGER NOT NULL,
                    `content` TEXT NOT NULL,
                    `providerName` TEXT NOT NULL,
                    `isLocalFallback` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_reports_generatedAtMillis` ON `ai_reports` (`generatedAtMillis`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_reports_type` ON `ai_reports` (`type`)")
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createTodoItemIndexes(db)
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensurePlanningAnnouncementHintColumn(db)
        }
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createDesktopAndReportIndexes(db)
        }
    }

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensureCountdownColumns(db)
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `focus_sessions`")
            mergeDefaultFocusGroupIntoRoutine(db)
            ensureEventCheckInTable(db)
            ensureTodoGroupTagsTable(db)
            ensureCheckInColumns(db)
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensurePlanningDocumentDateColumn(db)
        }
    }

    private fun rebuildPlanningNotesTable(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `planning_notes_room_expected`")
        createPlanningNotesTable(db, "planning_notes_room_expected")
        if (tableExists(db, "planning_notes")) {
            if (tableHasColumns(db, "planning_notes", listOf("id", "title", "contentMarkdown", "createdAtMillis", "updatedAtMillis", "archived"))) {
                db.execSQL(
                    """
                    INSERT INTO `planning_notes_room_expected` (
                        `id`, `title`, `contentMarkdown`, `createdAtMillis`, `updatedAtMillis`, `archived`, `hasAnnouncementHint`
                    )
                    SELECT `id`, `title`, `contentMarkdown`, `createdAtMillis`, `updatedAtMillis`, `archived`,
                        CASE
                            WHEN `contentMarkdown` LIKE '%公告%'
                                OR `contentMarkdown` LIKE '%[!announcement]%'
                                OR `contentMarkdown` LIKE '%[! announcement]%'
                            THEN 1
                            ELSE 0
                        END
                    FROM `planning_notes`
                    """.trimIndent()
                )
            }
            db.execSQL("DROP TABLE `planning_notes`")
        }
        db.execSQL("ALTER TABLE `planning_notes_room_expected` RENAME TO `planning_notes`")
    }

    private fun createPlanningNotesTable(db: SupportSQLiteDatabase, tableName: String) {
        db.execSQL(
            """
            CREATE TABLE `$tableName` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `contentMarkdown` TEXT NOT NULL,
                `createdAtMillis` INTEGER NOT NULL,
                `updatedAtMillis` INTEGER NOT NULL,
                `archived` INTEGER NOT NULL,
                `hasAnnouncementHint` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
        db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun tableHasColumns(db: SupportSQLiteDatabase, tableName: String, columns: List<String>): Boolean {
        val existing = mutableSetOf<String>()
        db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                existing += cursor.getString(nameIndex)
            }
        }
        return existing.containsAll(columns)
    }

    private fun createTodoItemIndexes(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_items_board_todos` ON `todo_items` (`completed`, `canceled`, `itemType`, `missed`, `dueAtMillis`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_items_board_events` ON `todo_items` (`completed`, `canceled`, `itemType`, `startAtMillis`, `endAtMillis`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_items_active_reminders` ON `todo_items` (`completed`, `canceled`, `reminderEnabled`, `dueAtMillis`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_items_group_due` ON `todo_items` (`groupId`, `dueAtMillis`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_items_series_due` ON `todo_items` (`recurringSeriesId`, `dueAtMillis`)")
    }

    private fun createDesktopAndReportIndexes(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_todo_items_desktop_todo_paging`
            ON `todo_items` (`itemType`, `completed`, `canceled`, `missed`, `dueAtMillis`, `createdAtMillis`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_ai_reports_type_generated_id`
            ON `ai_reports` (`type`, `generatedAtMillis`, `id`)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_ai_reports_generated_id`
            ON `ai_reports` (`generatedAtMillis`, `id`)
            """.trimIndent()
        )
    }

    private fun ensureCountdownColumns(db: SupportSQLiteDatabase) {
        if (!tableHasColumns(db, "todo_items", listOf("countdownEnabled"))) {
            db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `countdownEnabled` INTEGER NOT NULL DEFAULT 0")
        }
        if (!tableHasColumns(db, "recurring_task_templates", listOf("countdownEnabled"))) {
            db.execSQL("ALTER TABLE `recurring_task_templates` ADD COLUMN `countdownEnabled` INTEGER NOT NULL DEFAULT 0")
        }
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_todo_items_countdown`
            ON `todo_items` (`completed`, `canceled`, `countdownEnabled`, `itemType`, `dueAtMillis`, `startAtMillis`)
            """.trimIndent()
        )
    }

    private fun ensureEventCheckInTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `event_check_ins` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `eventId` INTEGER NOT NULL,
                `checkInAtMillis` INTEGER NOT NULL,
                `checkOutAtMillis` INTEGER,
                `durationMinutes` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_event_check_ins_eventId` ON `event_check_ins` (`eventId`)")
    }

    private fun ensureTodoGroupTagsTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `todo_group_tags` (
                `todoId` INTEGER NOT NULL,
                `groupId` INTEGER NOT NULL,
                PRIMARY KEY(`todoId`, `groupId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_group_tags_todoId` ON `todo_group_tags` (`todoId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_group_tags_groupId` ON `todo_group_tags` (`groupId`)")
        db.execSQL(
            """
            INSERT OR IGNORE INTO `todo_group_tags` (`todoId`, `groupId`)
            SELECT `id`, `groupId`
            FROM `todo_items`
            WHERE `groupId` != 0 AND `itemType` = 'TODO'
            """.trimIndent()
        )
    }

    private fun ensureCheckInColumns(db: SupportSQLiteDatabase) {
        if (!tableHasColumns(db, "todo_items", listOf("checkInEnabled"))) {
            db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `checkInEnabled` INTEGER NOT NULL DEFAULT 0")
        }
        if (!tableHasColumns(db, "todo_items", listOf("totalCheckInMinutes"))) {
            db.execSQL("ALTER TABLE `todo_items` ADD COLUMN `totalCheckInMinutes` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private fun mergeDefaultFocusGroupIntoRoutine(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO `task_groups` (`name`, `colorHex`, `sortOrder`, `isDefault`, `createdAtMillis`)
            SELECT '例行', '#4CB782', 2, 1, ${System.currentTimeMillis()}
            WHERE NOT EXISTS (
                SELECT 1 FROM `task_groups` WHERE `name` = '例行'
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            UPDATE `todo_items`
            SET `groupId` = (
                SELECT `id` FROM `task_groups`
                WHERE `name` = '例行'
                ORDER BY `isDefault` DESC, `sortOrder` ASC, `id` ASC
                LIMIT 1
            )
            WHERE `groupId` IN (
                SELECT `id` FROM `task_groups`
                WHERE `name` = '专注' AND `isDefault` = 1
            )
            """.trimIndent()
        )
        db.execSQL("DELETE FROM `task_groups` WHERE `name` = '专注' AND `isDefault` = 1")
    }

    private fun ensurePlanningAnnouncementHintColumn(db: SupportSQLiteDatabase) {
        if (!tableHasColumns(db, "planning_notes", listOf("hasAnnouncementHint"))) {
            db.execSQL("ALTER TABLE `planning_notes` ADD COLUMN `hasAnnouncementHint` INTEGER NOT NULL DEFAULT 0")
        }
        db.execSQL(
            """
            UPDATE `planning_notes`
            SET `hasAnnouncementHint` = CASE
                WHEN `contentMarkdown` LIKE '%公告%'
                    OR `contentMarkdown` LIKE '%[!announcement]%'
                    OR `contentMarkdown` LIKE '%[! announcement]%'
                THEN 1
                ELSE 0
            END
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_planning_notes_announcement_lookup`
            ON `planning_notes` (`archived`, `hasAnnouncementHint`, `updatedAtMillis`, `createdAtMillis`)
            """.trimIndent()
        )
    }

    private fun ensurePlanningDocumentDateColumn(db: SupportSQLiteDatabase) {
        if (!tableHasColumns(db, "planning_notes", listOf("documentDateEpochDay"))) {
            db.execSQL("ALTER TABLE `planning_notes` ADD COLUMN `documentDateEpochDay` INTEGER DEFAULT NULL")
        }
    }
}
