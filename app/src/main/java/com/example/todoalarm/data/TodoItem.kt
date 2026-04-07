package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val dueAtMillis: Long,
    val reminderAtMillis: Long?,
    val reminderEnabled: Boolean,
    val ringEnabled: Boolean,
    val vibrateEnabled: Boolean,
    val voiceEnabled: Boolean = false,
    val groupId: Long = 0,
    val categoryKey: String = TodoCategory.ROUTINE.key,
    val completed: Boolean = false,
    val completedAtMillis: Long? = null,
    val canceled: Boolean = false,
    val canceledAtMillis: Long? = null,
    val missed: Boolean = false,
    val missedAtMillis: Long? = null,
    val recurringSeriesId: String? = null,
    val recurrenceType: String = RecurrenceType.NONE.name,
    val recurrenceWeekdays: String = "",
    val recurrenceMonthlyOrdinal: Int? = null,
    val recurrenceMonthlyWeekday: Int? = null,
    val recurrenceMonthlyDay: Int? = null,
    val recurrenceEndEpochDay: Long? = null,
    val recurrenceAnchorDueAtMillis: Long? = null,
    val reminderOffsetMinutes: Int? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val isHistory: Boolean
        get() = completed || canceled

    val isActive: Boolean
        get() = !isHistory

    val isRecurring: Boolean
        get() = recurringSeriesId != null && recurrenceTypeEnum != RecurrenceType.NONE

    val recurrenceTypeEnum: RecurrenceType
        get() = RecurrenceType.fromStorage(recurrenceType)

    val recurrenceEndDate: LocalDate?
        get() = recurrenceEndEpochDay?.let(LocalDate::ofEpochDay)

    fun dueDate(): LocalDate {
        return Instant.ofEpochMilli(dueAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
}
