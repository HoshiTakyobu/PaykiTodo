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

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensurePlanningNodesTable(db)
            migratePlanningMarkdownToNodes(db)
        }
    }

    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensurePlanningEndTodoColumn(db)
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensurePlanningSyncEnabledColumn(db)
            markMigratedPlanningStructureNodes(db)
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

    private fun ensurePlanningNodesTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `planning_nodes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `noteId` INTEGER NOT NULL,
                `parentNodeId` INTEGER,
                `sortOrder` INTEGER NOT NULL,
                `text` TEXT NOT NULL,
                `createdAtMillis` INTEGER NOT NULL,
                `updatedAtMillis` INTEGER NOT NULL,
                `startAtMillis` INTEGER,
                `endAtMillis` INTEGER,
                `dueAtMillis` INTEGER,
                `location` TEXT,
                `linkedTodoId` INTEGER,
                `linkedEndTodoId` INTEGER,
                `syncEnabled` INTEGER NOT NULL DEFAULT 1,
                `collapsed` INTEGER NOT NULL DEFAULT 0,
                `completed` INTEGER NOT NULL DEFAULT 0,
                `completedAtMillis` INTEGER,
                FOREIGN KEY(`noteId`) REFERENCES `planning_notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`parentNodeId`) REFERENCES `planning_nodes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`linkedTodoId`) REFERENCES `todo_items`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_planning_nodes_noteId` ON `planning_nodes` (`noteId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_planning_nodes_parentNodeId` ON `planning_nodes` (`parentNodeId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_planning_nodes_linkedTodoId` ON `planning_nodes` (`linkedTodoId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_planning_nodes_linkedEndTodoId` ON `planning_nodes` (`linkedEndTodoId`)")
        ensurePlanningEndTodoColumn(db)
        ensurePlanningSyncEnabledColumn(db)
    }

    private fun ensurePlanningEndTodoColumn(db: SupportSQLiteDatabase) {
        if (!tableExists(db, "planning_nodes")) return
        if (!tableHasColumns(db, "planning_nodes", listOf("linkedEndTodoId"))) {
            db.execSQL("ALTER TABLE `planning_nodes` ADD COLUMN `linkedEndTodoId` INTEGER")
        }
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_planning_nodes_linkedEndTodoId` ON `planning_nodes` (`linkedEndTodoId`)")
    }

    private fun ensurePlanningSyncEnabledColumn(db: SupportSQLiteDatabase) {
        if (!tableExists(db, "planning_nodes")) return
        if (!tableHasColumns(db, "planning_nodes", listOf("syncEnabled"))) {
            db.execSQL("ALTER TABLE `planning_nodes` ADD COLUMN `syncEnabled` INTEGER NOT NULL DEFAULT 1")
        }
    }

    private fun markMigratedPlanningStructureNodes(db: SupportSQLiteDatabase) {
        if (!tableExists(db, "planning_nodes")) return
        val structureTitles = listOf(
            "今日计划",
            "今天计划",
            "明日计划",
            "明天计划",
            "后天计划",
            "收集箱",
            "今日",
            "今天",
            "明日",
            "明天",
            "后天"
        )
        structureTitles.forEach { title ->
            if (tableExists(db, "todo_group_tags")) {
                db.execSQL(
                    """
                    DELETE FROM `todo_group_tags`
                    WHERE `todoId` IN (
                        SELECT `linkedTodoId`
                        FROM `planning_nodes`
                        WHERE `text` = ?
                            AND `startAtMillis` IS NULL
                            AND `endAtMillis` IS NULL
                            AND `dueAtMillis` IS NULL
                            AND `linkedTodoId` IS NOT NULL
                    )
                    """.trimIndent(),
                    arrayOf(title)
                )
            }
            if (tableExists(db, "todo_items")) {
                db.execSQL(
                    """
                    DELETE FROM `todo_items`
                    WHERE `id` IN (
                        SELECT `linkedTodoId`
                        FROM `planning_nodes`
                        WHERE `text` = ?
                            AND `startAtMillis` IS NULL
                            AND `endAtMillis` IS NULL
                            AND `dueAtMillis` IS NULL
                            AND `linkedTodoId` IS NOT NULL
                    )
                    AND `itemType` = 'TODO'
                    AND `title` = ?
                    AND `dueAtMillis` = ?
                    AND (`notes` IS NULL OR `notes` = '')
                    """.trimIndent(),
                    arrayOf(title, title, Long.MAX_VALUE)
                )
            }
            db.execSQL(
                """
                UPDATE `planning_nodes`
                SET `syncEnabled` = 0,
                    `linkedTodoId` = NULL,
                    `linkedEndTodoId` = NULL
                WHERE `text` = ?
                    AND `startAtMillis` IS NULL
                    AND `endAtMillis` IS NULL
                    AND `dueAtMillis` IS NULL
                """.trimIndent(),
                arrayOf(title)
            )
        }
    }

    private fun migratePlanningMarkdownToNodes(db: SupportSQLiteDatabase) {
        if (!tableExists(db, "planning_notes") || !tableExists(db, "planning_nodes")) return
        db.query(
            """
            SELECT `id`, `contentMarkdown`, `createdAtMillis`, `updatedAtMillis`, `documentDateEpochDay`
            FROM `planning_notes`
            ORDER BY `id` ASC
            """.trimIndent()
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("id")
            val contentIndex = cursor.getColumnIndexOrThrow("contentMarkdown")
            val createdIndex = cursor.getColumnIndexOrThrow("createdAtMillis")
            val updatedIndex = cursor.getColumnIndexOrThrow("updatedAtMillis")
            val documentDateIndex = cursor.getColumnIndex("documentDateEpochDay")
            while (cursor.moveToNext()) {
                val noteId = cursor.getLong(idIndex)
                val content = cursor.getString(contentIndex).orEmpty()
                if (content.isBlank()) continue
                val createdAt = cursor.getLong(createdIndex)
                val updatedAt = cursor.getLong(updatedIndex)
                val documentDate = if (documentDateIndex >= 0 && !cursor.isNull(documentDateIndex)) {
                    LocalDate.ofEpochDay(cursor.getLong(documentDateIndex))
                } else {
                    null
                }
                migratePlanningNoteNodes(
                    db = db,
                    noteId = noteId,
                    markdown = content,
                    createdAtMillis = createdAt,
                    updatedAtMillis = updatedAt,
                    documentDate = documentDate
                )
            }
        }
    }

    private fun migratePlanningNoteNodes(
        db: SupportSQLiteDatabase,
        noteId: Long,
        markdown: String,
        createdAtMillis: Long,
        updatedAtMillis: Long,
        documentDate: LocalDate?
    ) {
        val mappings = loadPlanningMappingsByLine(db, noteId)
        val parseMarkdown = markdown.replace(Regex("\\s+#imported(?=\\s|$)"), "")
        val parsedByLine = PlanningMarkdownParser.parse(
            markdown = parseMarkdown,
            documentDate = documentDate
        ).candidates.associateBy { it.lineNumber }
        val lastNodeByDepth = mutableMapOf<Int, Long>()
        val sortOrderByParent = mutableMapOf<Long, Int>()
        markdown.replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .forEachIndexed { index, rawLine ->
                val lineNumber = index + 1
                val trimmed = rawLine.trim()
                if (trimmed.isBlank()) return@forEachIndexed
                val outlineLine = planningOutlineLine(rawLine) ?: return@forEachIndexed
                val parentId = if (outlineLine.depth > 0) {
                    ((outlineLine.depth - 1) downTo 0).firstNotNullOfOrNull { lastNodeByDepth[it] }
                } else {
                    null
                }
                val parentKey = parentId ?: 0L
                val sortOrder = sortOrderByParent.getOrDefault(parentKey, 0)
                sortOrderByParent[parentKey] = sortOrder + 1
                val parsed = parsedByLine[lineNumber]?.takeIf { it.type == PlanningParsedType.TODO || it.type == PlanningParsedType.EVENT }
                val mapping = mappings[lineNumber]
                val syncEnabled = mapping?.linkedTodoId != null || parsed != null && !outlineLine.structureOnly
                val nodeId = insertPlanningNode(
                    db = db,
                    noteId = noteId,
                    parentNodeId = parentId,
                    sortOrder = sortOrder,
                    text = parsed?.title?.ifBlank { outlineLine.text } ?: outlineLine.text,
                    createdAtMillis = createdAtMillis,
                    updatedAtMillis = updatedAtMillis,
                    startAtMillis = parsed?.startAt?.toEpochMillis(),
                    endAtMillis = parsed?.endAt?.toEpochMillis(),
                    dueAtMillis = parsed?.dueAt?.toEpochMillis(),
                    location = parsed?.location?.takeIf { it.isNotBlank() },
                    linkedTodoId = mapping?.linkedTodoId?.takeIf { planningLinkedItemExists(db, it) },
                    syncEnabled = syncEnabled,
                    completed = outlineLine.completed || mapping?.completed == true,
                    completedAtMillis = if (outlineLine.completed || mapping?.completed == true) updatedAtMillis else null
                )
                lastNodeByDepth[outlineLine.depth] = nodeId
                lastNodeByDepth.keys.filter { it > outlineLine.depth }.toList().forEach(lastNodeByDepth::remove)
            }
    }

    private fun loadPlanningMappingsByLine(
        db: SupportSQLiteDatabase,
        noteId: Long
    ): Map<Int, PlanningMigrationMapping> {
        if (!tableExists(db, "planning_line_mappings")) return emptyMap()
        val result = mutableMapOf<Int, PlanningMigrationMapping>()
        db.query(
            """
            SELECT `lastKnownLineNumber`, `todoId`, `eventId`, `status`
            FROM `planning_line_mappings`
            WHERE `noteId` = ?
            ORDER BY `id` ASC
            """.trimIndent(),
            arrayOf(noteId)
        ).use { cursor ->
            val lineIndex = cursor.getColumnIndexOrThrow("lastKnownLineNumber")
            val todoIndex = cursor.getColumnIndexOrThrow("todoId")
            val eventIndex = cursor.getColumnIndexOrThrow("eventId")
            val statusIndex = cursor.getColumnIndexOrThrow("status")
            while (cursor.moveToNext()) {
                val line = cursor.getInt(lineIndex)
                if (line <= 0) continue
                val linkedTodoId = when {
                    !cursor.isNull(todoIndex) -> cursor.getLong(todoIndex)
                    !cursor.isNull(eventIndex) -> cursor.getLong(eventIndex)
                    else -> null
                }
                val status = cursor.getString(statusIndex).orEmpty()
                result[line] = PlanningMigrationMapping(
                    linkedTodoId = linkedTodoId,
                    completed = status == MappingStatus.COMPLETED.name
                )
            }
        }
        return result
    }

    private fun insertPlanningNode(
        db: SupportSQLiteDatabase,
        noteId: Long,
        parentNodeId: Long?,
        sortOrder: Int,
        text: String,
        createdAtMillis: Long,
        updatedAtMillis: Long,
        startAtMillis: Long?,
        endAtMillis: Long?,
        dueAtMillis: Long?,
        location: String?,
        linkedTodoId: Long?,
        syncEnabled: Boolean,
        completed: Boolean,
        completedAtMillis: Long?
    ): Long {
        db.execSQL(
            """
            INSERT INTO `planning_nodes` (
                `noteId`, `parentNodeId`, `sortOrder`, `text`, `createdAtMillis`, `updatedAtMillis`,
                `startAtMillis`, `endAtMillis`, `dueAtMillis`, `location`, `linkedTodoId`,
                `syncEnabled`, `collapsed`, `completed`, `completedAtMillis`
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
            """.trimIndent(),
            arrayOf(
                noteId,
                parentNodeId,
                sortOrder,
                text,
                createdAtMillis,
                updatedAtMillis,
                startAtMillis,
                endAtMillis,
                dueAtMillis,
                location,
                linkedTodoId,
                if (syncEnabled) 1 else 0,
                if (completed) 1 else 0,
                completedAtMillis
            )
        )
        db.query("SELECT last_insert_rowid()").use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
    }

    private fun planningOutlineLine(rawLine: String): PlanningMigrationLine? {
        val leadingSpaces = rawLine.takeWhile { it == ' ' || it == '\t' }
            .fold(0) { total, char -> total + if (char == '\t') 4 else 1 }
        val trimmed = rawLine.trim()
        val heading = Regex("^#{1,6}\\s+(.+)$").matchEntire(trimmed)
        if (heading != null) {
            val depth = trimmed.takeWhile { it == '#' }.length - 1
            return PlanningMigrationLine(
                depth = depth.coerceAtLeast(0),
                text = stripPlanningNodeNoise(heading.groupValues[1]),
                structureOnly = true,
                completed = false
            )
        }
        val checkbox = Regex("^[-*+]\\s+\\[([ xX])\\]\\s+(.+)$").matchEntire(trimmed)
        if (checkbox != null) {
            return PlanningMigrationLine(
                depth = (leadingSpaces / 2).coerceAtLeast(0),
                text = stripPlanningNodeNoise(checkbox.groupValues[2]),
                structureOnly = false,
                completed = checkbox.groupValues[1].equals("x", ignoreCase = true)
            )
        }
        val bullet = Regex("^[-*+•]\\s+(.+)$").matchEntire(trimmed)
        if (bullet != null) {
            return PlanningMigrationLine(
                depth = (leadingSpaces / 2).coerceAtLeast(0),
                text = stripPlanningNodeNoise(bullet.groupValues[1]),
                structureOnly = false,
                completed = false
            )
        }
        return PlanningMigrationLine(
            depth = (leadingSpaces / 2).coerceAtLeast(0),
            text = stripPlanningNodeNoise(trimmed),
            structureOnly = false,
            completed = false
        )
    }

    private fun stripPlanningNodeNoise(text: String): String {
        return text
            .replace(Regex("\\s+#imported(?=\\s|$)"), "")
            .trim()
    }

    private fun planningLinkedItemExists(db: SupportSQLiteDatabase, itemId: Long): Boolean {
        if (itemId <= 0) return false
        db.query("SELECT 1 FROM `todo_items` WHERE `id` = ? LIMIT 1", arrayOf(itemId)).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private data class PlanningMigrationLine(
        val depth: Int,
        val text: String,
        val structureOnly: Boolean,
        val completed: Boolean
    )

    private data class PlanningMigrationMapping(
        val linkedTodoId: Long?,
        val completed: Boolean
    )
}
