package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_templates",
    indices = [Index(value = ["templateType", "updatedAtMillis"])]
)
data class ScheduleTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val templateType: String,
    val payloadJson: String,
    val accentColorHex: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

object ScheduleTemplateType {
    const val WEEKLY = "WEEKLY"
    const val SEMESTER = "SEMESTER"
    const val DUTY_WEEK = "DUTY_WEEK"
}
