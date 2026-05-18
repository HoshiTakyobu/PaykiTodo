package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_check_ins",
    indices = [
        Index(value = ["eventId"], name = "index_event_check_ins_eventId")
    ]
)
data class EventCheckIn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val checkInAtMillis: Long,
    val checkOutAtMillis: Long? = null,
    val durationMinutes: Int = 0
)
