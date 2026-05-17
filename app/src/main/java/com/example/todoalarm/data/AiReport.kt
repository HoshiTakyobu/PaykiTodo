package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AiReportType {
    DAILY,
    WEEKLY
}

@Entity(
    tableName = "ai_reports",
    indices = [
        Index("generatedAtMillis"),
        Index("type"),
        Index(
            value = ["type", "generatedAtMillis", "id"],
            name = "index_ai_reports_type_generated_id"
        ),
        Index(
            value = ["generatedAtMillis", "id"],
            name = "index_ai_reports_generated_id"
        )
    ]
)
data class AiReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: AiReportType,
    val generatedAtMillis: Long,
    val periodStartMillis: Long,
    val periodEndMillis: Long,
    val content: String,
    val providerName: String = "",
    val isLocalFallback: Boolean = false
)
