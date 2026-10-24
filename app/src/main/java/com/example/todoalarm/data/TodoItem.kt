package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(
    tableName = "todo_items",
    indices = [
        Index(
            value = ["completed", "canceled", "itemType", "missed", "dueAtMillis"],
            name = "index_todo_items_board_todos"
        ),
        Index(
            value = ["completed", "canceled", "itemType", "startAtMillis", "endAtMillis"],
            name = "index_todo_items_board_events"
        ),
        Index(
            value = ["completed", "canceled", "reminderEnabled", "dueAtMillis"],
            name = "index_todo_items_active_reminders"
        ),
        Index(
            value = ["groupId", "dueAtMillis"],
            name = "index_todo_items_group_due"
        ),
        Index(
            value = ["recurringSeriesId", "dueAtMillis"],
            name = "index_todo_items_series_due"
        ),
        Index(
            value = ["multiSlotBundleId", "startAtMillis"],
            name = "index_todo_items_multi_slot_bundle"
        ),
        Index(
            value = ["itemType", "completed", "canceled", "missed", "dueAtMillis", "createdAtMillis"],
            name = "index_todo_items_desktop_todo_paging"
        ),
        Index(
            value = ["completed", "canceled", "countdownEnabled", "itemType", "dueAtMillis", "startAtMillis"],
            name = "index_todo_items_countdown"
        ),
        Index(
            value = ["itemType", "completed", "canceled", "dueAtMillis", "createdAtMillis"],
            name = "index_todo_items_active_todo_sort"
        ),
        Index(
            value = ["itemType", "completed", "canceled", "completedAtMillis", "canceledAtMillis", "missedAtMillis", "createdAtMillis"],
            name = "index_todo_items_history_todo_sort"
        ),
        Index(
            value = ["itemType", "completed", "canceled", "startAtMillis", "endAtMillis", "dueAtMillis", "createdAtMillis"],
            name = "index_todo_items_calendar_range_sort"
        ),
        Index(
            value = ["quickChecklistGroupId", "completed", "canceled", "quickChecklistSortOrder"],
            name = "index_todo_items_quick_checklist_group"
        ),
        Index(
            value = ["parentTodoId", "quickChecklistSortOrder"],
            name = "index_todo_items_quick_checklist_parent"
        )
    ]
)
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemType: String = PlannerItemType.TODO.name,
    val title: String,
    val notes: String = "",
    val dueAtMillis: Long,
    val startAtMillis: Long? = null,
    val endAtMillis: Long? = null,
    val allDay: Boolean = false,
    @ColumnInfo(defaultValue = "''") val eventTimeZoneId: String = "",
    @ColumnInfo(defaultValue = "''") val allDayReminderRulesJson: String = "",
    @ColumnInfo(defaultValue = "'BUSY'") val eventBusyStatus: String = "BUSY",
    @ColumnInfo(defaultValue = "'DEFAULT'") val eventVisibility: String = "DEFAULT",
    @ColumnInfo(defaultValue = "''") val sharedEventId: String = "",
    @ColumnInfo(defaultValue = "'[]'") val eventParticipantsJson: String = "[]",
    val location: String = "",
    val accentColorHex: String? = null,
    val countdownEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "'default'")
    val calendarId: String = DEFAULT_CALENDAR_ID,
    @ColumnInfo(defaultValue = "'AUTO'")
    val countdownPastMode: String = "AUTO",
    @ColumnInfo(defaultValue = "0")
    val hiddenFromBoard: Boolean = false,
    val checkInEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "")
    val checkInReminderOffsetsCsv: String = "",
    val totalCheckInMinutes: Int = 0,
    val reminderAtMillis: Long?,
    val reminderOffsetsCsv: String = "",
    val reminderEnabled: Boolean,
    val ringEnabled: Boolean,
    val vibrateEnabled: Boolean,
    @ColumnInfo(defaultValue = "0")
    val alarmMode: Boolean = false,
    val voiceEnabled: Boolean = false,
    val reminderDeliveryMode: String = ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION.name,
    val groupId: Long = 0,
    val categoryKey: String = TodoCategory.ROUTINE.key,
    val completed: Boolean = false,
    val completedAtMillis: Long? = null,
    val canceled: Boolean = false,
    val canceledAtMillis: Long? = null,
    val missed: Boolean = false,
    val missedAtMillis: Long? = null,
    val recurringSeriesId: String? = null,
    val multiSlotBundleId: String? = null,
    val recurrenceType: String = RecurrenceType.NONE.name,
    val recurrenceWeekdays: String = "",
    @ColumnInfo(defaultValue = "1") val recurrenceInterval: Int = 1,
    @ColumnInfo(defaultValue = "'CLAMP'") val recurrenceInvalidDatePolicy: String = "CLAMP",
    val recurrenceMonthlyOrdinal: Int? = null,
    val recurrenceMonthlyWeekday: Int? = null,
    val recurrenceMonthlyDay: Int? = null,
    val recurrenceEndEpochDay: Long? = null,
    val recurrenceAnchorDueAtMillis: Long? = null,
    val reminderOffsetMinutes: Int? = null,
    val quickChecklistGroupId: Long? = null,
    val parentTodoId: Long? = null,
    val quickChecklistDepth: Int? = null,
    val quickChecklistSortOrder: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val quickChecklistCollapsed: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val itemTypeEnum: PlannerItemType
        get() = PlannerItemType.fromStorage(itemType)

    val isTodo: Boolean
        get() = itemTypeEnum == PlannerItemType.TODO

    val isEvent: Boolean
        get() = itemTypeEnum == PlannerItemType.EVENT

    val isHistory: Boolean
        get() = completed || canceled

    val isActive: Boolean
        get() = !isHistory

    val canBeMarkedCompleted: Boolean
        get() = isTodo

    val isRecurring: Boolean
        get() = recurringSeriesId != null && recurrenceTypeEnum != RecurrenceType.NONE

    val isQuickChecklistTask: Boolean
        get() = quickChecklistGroupId != null

    val recurrenceTypeEnum: RecurrenceType
        get() = RecurrenceType.fromStorage(recurrenceType)

    val reminderDeliveryModeEnum: ReminderDeliveryMode
        get() = ReminderDeliveryMode.fromStorage(reminderDeliveryMode)

    val configuredReminderOffsetsMinutes: List<Int>
        get() = decodeReminderOffsets(reminderOffsetsCsv, reminderOffsetMinutes)

    val configuredCheckInReminderOffsetsMinutes: List<Int>
        get() = decodeReminderOffsets(checkInReminderOffsetsCsv)

    val recurrenceEndDate: LocalDate?
        get() = recurrenceEndEpochDay?.let(LocalDate::ofEpochDay)

    val hasDueDate: Boolean
        get() = hasDueDate(dueAtMillis)

    fun dueDateTimeOrNull(): LocalDateTime? {
        if (!hasDueDate) return null
        return Instant.ofEpochMilli(dueAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    fun dueDate(): LocalDate {
        return dueDateTimeOrNull()?.toLocalDate() ?: NO_DUE_DATE_FALLBACK_DATE
    }

    fun eventStartDate(): LocalDate? {
        val start = startAtMillis ?: return null
        return Instant.ofEpochMilli(start)
            .atZone(calendarZone(eventTimeZoneId))
            .toLocalDate()
    }
}
