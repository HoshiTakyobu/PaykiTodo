package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_task_templates",
    indices = [
        Index(value = ["seriesId"], unique = true),
        Index(value = ["multiSlotBundleId"], name = "index_recurring_task_templates_multi_slot_bundle")
    ]
)
data class RecurringTaskTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesId: String,
    val itemType: String = PlannerItemType.TODO.name,
    val title: String,
    val notes: String = "",
    val location: String = "",
    val accentColorHex: String? = null,
    val countdownEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "'default'")
    val calendarId: String = DEFAULT_CALENDAR_ID,
    @ColumnInfo(defaultValue = "'AUTO'")
    val countdownPastMode: String = "AUTO",
    @ColumnInfo(defaultValue = "0")
    val hiddenFromBoard: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val checkInEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "")
    val checkInReminderOffsetsCsv: String = "",
    val multiSlotBundleId: String? = null,
    val allDay: Boolean = false,
    @ColumnInfo(defaultValue = "''") val eventTimeZoneId: String = "",
    @ColumnInfo(defaultValue = "''") val allDayReminderRulesJson: String = "",
    @ColumnInfo(defaultValue = "'BUSY'") val eventBusyStatus: String = "BUSY",
    @ColumnInfo(defaultValue = "'DEFAULT'") val eventVisibility: String = "DEFAULT",
    @ColumnInfo(defaultValue = "'[]'") val eventParticipantsJson: String = "[]",
    val groupId: Long,
    val dueHour: Int,
    val dueMinute: Int,
    val eventDurationMinutes: Int? = null,
    val reminderOffsetMinutes: Int? = null,
    val reminderOffsetsCsv: String = "",
    val ringEnabled: Boolean,
    val vibrateEnabled: Boolean,
    @ColumnInfo(defaultValue = "0")
    val alarmMode: Boolean = false,
    val reminderDeliveryMode: String = ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION.name,
    val recurrenceType: String,
    val recurrenceWeekdays: String = "",
    @ColumnInfo(defaultValue = "1") val recurrenceInterval: Int = 1,
    @ColumnInfo(defaultValue = "'CLAMP'") val recurrenceInvalidDatePolicy: String = "CLAMP",
    @ColumnInfo(defaultValue = "0") val recurrenceNeverEnds: Boolean = false,
    val recurrenceMonthlyOrdinal: Int? = null,
    val recurrenceMonthlyWeekday: Int? = null,
    val recurrenceMonthlyDay: Int? = null,
    val recurrenceYearlyMonth: Int? = null,
    val recurrenceYearlyDay: Int? = null,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val configuredReminderOffsetsMinutes: List<Int>
        get() = decodeReminderOffsets(reminderOffsetsCsv, reminderOffsetMinutes)

    val configuredCheckInReminderOffsetsMinutes: List<Int>
        get() = decodeReminderOffsets(checkInReminderOffsetsCsv)
}
