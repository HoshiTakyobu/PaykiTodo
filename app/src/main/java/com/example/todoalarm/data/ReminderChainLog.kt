package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminder_chain_logs",
    indices = [
        Index(value = ["createdAtMillis"]),
        Index(value = ["todoId", "createdAtMillis"]),
        Index(value = ["chainKey"])
    ]
)
data class ReminderChainLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val todoId: Long,
    val chainKey: String,
    val source: String,
    val stage: String,
    val status: String,
    val message: String? = null,
    val reminderAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

object ReminderChainStage {
    const val SCHEDULE_REQUESTED = "SCHEDULE_REQUESTED"
    const val SCHEDULED = "SCHEDULED"
    const val SCHEDULE_FAILED = "SCHEDULE_FAILED"
    const val RECEIVER_EXACT = "RECEIVER_EXACT"
    const val RECEIVER_BACKUP = "RECEIVER_BACKUP"
    const val POLL_DISPATCH = "POLL_DISPATCH"
    const val SERVICE_START = "SERVICE_START"
    const val SERVICE_ITEM_LOADED = "SERVICE_ITEM_LOADED"
    const val SERVICE_INVALID_ITEM = "SERVICE_INVALID_ITEM"
    const val NOTIFICATION_POSTED = "NOTIFICATION_POSTED"
    const val FULLSCREEN_ATTEMPT = "FULLSCREEN_ATTEMPT"
    const val FULLSCREEN_ACTIVITY_LAUNCH = "FULLSCREEN_ACTIVITY_LAUNCH"
    const val FULLSCREEN_ACTIVITY_FAILED = "FULLSCREEN_ACTIVITY_FAILED"
    const val ACCESSIBILITY_TRIGGER = "ACCESSIBILITY_TRIGGER"
    const val ACCESSIBILITY_OVERLAY = "ACCESSIBILITY_OVERLAY"
    const val REMINDER_ACTIVITY_RESUME = "REMINDER_ACTIVITY_RESUME"
    const val USER_COMPLETE = "USER_COMPLETE"
    const val USER_SNOOZE = "USER_SNOOZE"
    const val USER_CANCEL = "USER_CANCEL"
    const val TEST_CREATED = "TEST_CREATED"
}

object ReminderChainStatus {
    const val OK = "OK"
    const val INFO = "INFO"
    const val WARN = "WARN"
    const val ERROR = "ERROR"
}
