package com.example.todoalarm.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ActivityRecordState { RUNNING, PAUSED, FINISHED }

enum class ActivityRecordSource { TIMER, MANUAL }

@Entity(
    tableName = "activity_records",
    indices = [
        Index(value = ["state", "createdAtMillis"], name = "index_activity_records_state_created"),
        Index(value = ["groupId"], name = "index_activity_records_groupId")
    ]
)
data class ActivityRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val groupId: Long? = null,
    val startAtMillis: Long,
    val accumulatedMillis: Long = 0,
    val lastResumeAtMillis: Long? = null,
    val state: ActivityRecordState,
    val endAtMillis: Long? = null,
    val source: ActivityRecordSource,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

/**
 * 有效用时（毫秒）= 已累计跑动毫秒 +（运行中则加上当前段已跑时间）。
 * 暂停/结束时 lastResumeAtMillis 为 null，只算累计值。
 */
fun ActivityRecord.effectiveDurationMillis(nowMillis: Long = System.currentTimeMillis()): Long {
    val running = if (state == ActivityRecordState.RUNNING && lastResumeAtMillis != null) {
        (nowMillis - lastResumeAtMillis).coerceAtLeast(0L)
    } else {
        0L
    }
    return accumulatedMillis + running
}

fun ActivityRecord.effectiveDurationMinutes(nowMillis: Long = System.currentTimeMillis()): Int {
    return (effectiveDurationMillis(nowMillis) / 60_000L).toInt()
}
