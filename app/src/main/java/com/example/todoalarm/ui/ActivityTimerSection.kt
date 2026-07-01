package com.example.todoalarm.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.ActivityRecord
import com.example.todoalarm.data.ActivityRecordState
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.effectiveDurationMillis
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ActivityClockFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)

/** 看板正向计时 / 记录相关的动作集合，避免在 DashboardBody 上散落多个参数。 */
internal data class ActivityBoardActions(
    val onStartTimer: (title: String, groupId: Long?) -> Unit = { _, _ -> },
    val onPauseTimer: () -> Unit = {},
    val onResumeTimer: () -> Unit = {},
    val onFinishTimer: () -> Unit = {},
    val onAddManualRecord: (title: String, startAtMillis: Long, durationMinutes: Int, groupId: Long?, notes: String) -> Unit = { _, _, _, _, _ -> },
    val onDeleteRecord: (ActivityRecord) -> Unit = {}
)

/**
 * 看板顶部常驻计时条。只有存在进行中（RUNNING/PAUSED）记录时才显示。
 */
@Composable
internal fun ActivityTimerBar(
    record: ActivityRecord,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 每秒刷新一次走动时长。
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(record.id, record.state) {
        while (record.state == ActivityRecordState.RUNNING) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
        nowMillis = System.currentTimeMillis()
    }

    val running = record.state == ActivityRecordState.RUNNING
    val elapsed = record.effectiveDurationMillis(nowMillis)
    val title = record.title.trim().ifBlank { "未命名记录" }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )
                Text(
                    text = formatElapsed(elapsed) + if (running) "" else " · 已暂停",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(onClick = { if (running) onPause() else onResume() }) {
                Icon(
                    imageVector = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (running) "暂停" else "继续",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(onClick = onFinish) {
                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = "结束",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * 「今日做了什么」卡片：展示今日已完成记录的标题、时段、时长，附总计。
 */
@Composable
internal fun TodayActivityCard(
    records: List<ActivityRecord>,
    groupsById: Map<Long, TaskGroup>,
    timerActive: Boolean,
    onStartTimer: () -> Unit,
    onAddRecord: () -> Unit,
    onRecordClick: (ActivityRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMinutes = records.sumOf { (it.effectiveDurationMillis() / 60_000L) }.toInt()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (records.isEmpty()) "今日做了什么" else "今日做了什么 (${records.size}段·${formatMinutes(totalMinutes)})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (!timerActive) {
                    IconButton(onClick = onStartTimer) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "开始计时",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onAddRecord) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "补记一条",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (records.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "今天还没有记录，点右下角开始，或点这里补记一条。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                records.take(6).forEach { record ->
                    Spacer(Modifier.height(8.dp))
                    ActivityRecordRow(
                        record = record,
                        group = record.groupId?.let { groupsById[it] },
                        onClick = { onRecordClick(record) }
                    )
                }
                if (records.size > 6) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "其余 ${records.size - 6} 条已折叠",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityRecordRow(
    record: ActivityRecord,
    group: TaskGroup?,
    onClick: () -> Unit
) {
    val accent = group?.colorHex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
        ?: MaterialTheme.colorScheme.primary
    val minutes = (record.effectiveDurationMillis() / 60_000L).toInt()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(34.dp)
            ) {
                Surface(color = accent, shape = RoundedCornerShape(2.dp), modifier = Modifier.fillMaxWidth().height(34.dp)) {}
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.title.trim().ifBlank { "未命名记录" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = activityTimeRange(record),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatMinutes(minutes),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun activityTimeRange(record: ActivityRecord): String {
    val start = Instant.ofEpochMilli(record.startAtMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val end = record.endAtMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
    return if (end != null) {
        "${start.format(ActivityClockFormatter)}-${end.format(ActivityClockFormatter)}"
    } else {
        start.format(ActivityClockFormatter)
    }
}

private fun formatElapsed(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}
