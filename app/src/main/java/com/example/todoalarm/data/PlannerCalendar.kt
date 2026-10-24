package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/** Calendar ownership is independent of task group tags. */
@Entity(tableName = "planner_calendars")
data class PlannerCalendar(
    @PrimaryKey val id: String = DEFAULT_CALENDAR_ID,
    val name: String = "常规日历",
    val colorHex: String = "#3370FF",
    val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "''") val description: String = "",
    @ColumnInfo(defaultValue = "''") val ownerAccount: String = "",
    @ColumnInfo(defaultValue = "'PRIVATE'") val shareMode: String = "PRIVATE",
    @ColumnInfo(defaultValue = "'[]'") val membersJson: String = "[]",
    @ColumnInfo(defaultValue = "'OWNER'") val sharedRole: String = "OWNER",
    @ColumnInfo(defaultValue = "0") val sharedRevision: Long = 0,
    @ColumnInfo(defaultValue = "''") val sharedServerUrl: String = ""
)

const val DEFAULT_CALENDAR_ID = "default"

object CountdownVisibility {
    fun visible(item: TodoItem, nowMillis: Long, pastDays: Int = 7): Boolean {
        if (!item.countdownEnabled || item.canceled || item.hiddenFromBoard || item.countdownPastMode == "HIDE") return false
        val target = DailyBoardSnapshotBuilder.countdownTargetMillis(item) ?: return false
        if (item.countdownPastMode == "KEEP") return true
        return !item.completed && target >= cutoff(nowMillis, pastDays)
    }

    fun needsDecision(item: TodoItem, nowMillis: Long, pastDays: Int = 7): Boolean {
        if (!item.countdownEnabled || item.canceled || item.countdownPastMode != "AUTO") return false
        return if (item.isTodo) item.completed && item.completedAtMillis?.let { it in cutoff(nowMillis, pastDays)..nowMillis } == true
        else (item.endAtMillis ?: Long.MAX_VALUE) <= nowMillis &&
            (item.endAtMillis ?: 0) >= cutoff(nowMillis, pastDays)
    }

    private fun cutoff(nowMillis: Long, pastDays: Int): Long = java.time.Instant.ofEpochMilli(nowMillis)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate().minusDays(pastDays.coerceIn(0, 365).toLong())
        .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
}
