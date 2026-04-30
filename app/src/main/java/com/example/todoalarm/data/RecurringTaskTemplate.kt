package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_task_templates",
    indices = [Index(value = ["seriesId"], unique = true)]
)
data class RecurringTaskTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesId: String,
    val itemType: String = PlannerItemType.TODO.name,
    val title: String,
    val notes: String = "",
    val location: String = "",
    val accentColorHex: String? = null,
    val allDay: Boolean = false,
    val groupId: Long,
    val dueHour: Int,
    val dueMinute: Int,
    val eventDurationMinutes: Int? = null,
    val reminderOffsetMinutes: Int? = null,
    val ringEnabled: Boolean,
    val vibrateEnabled: Boolean,
    val reminderDeliveryMode: String = ReminderDeliveryMode.FULLSCREEN.name,
    val recurrenceType: String,
    val recurrenceWeekdays: String = "",
    val recurrenceMonthlyOrdinal: Int? = null,
    val recurrenceMonthlyWeekday: Int? = null,
    val recurrenceMonthlyDay: Int? = null,
    val recurrenceYearlyMonth: Int? = null,
    val recurrenceYearlyDay: Int? = null,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val createdAtMillis: Long = System.currentTimeMillis()
)
