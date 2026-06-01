package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_instance_skips",
    indices = [
        Index(value = ["seriesId", "instanceEpochDay"], unique = true)
    ]
)
data class RecurringInstanceSkip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesId: String,
    val instanceEpochDay: Long,
    val createdAtMillis: Long = System.currentTimeMillis()
)
