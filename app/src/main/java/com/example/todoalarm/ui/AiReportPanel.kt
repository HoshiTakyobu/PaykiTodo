package com.example.todoalarm.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.AiReport
import com.example.todoalarm.data.AiReportType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class AiReportFilter(val label: String) {
    ALL("全部"),
    DAILY("日报"),
    WEEKLY("周报")
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun AiReportPanel(
    reports: List<AiReport>,
    targetReportId: Long?,
    targetReportSerial: Int,
    onDeleteReport: suspend (Long) -> String?
) {
    var filter by remember { mutableStateOf(AiReportFilter.ALL) }
    var selectedReport by remember { mutableStateOf<AiReport?>(null) }
    var pendingDelete by remember { mutableStateOf<AiReport?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val filteredReports = remember(reports, filter) {
        when (filter) {
            AiReportFilter.ALL -> reports
            AiReportFilter.DAILY -> reports.filter { it.type == AiReportType.DAILY }
            AiReportFilter.WEEKLY -> reports.filter { it.type == AiReportType.WEEKLY }
        }
    }

    LaunchedEffect(targetReportSerial, reports) {
        val target = targetReportId?.let { id -> reports.firstOrNull { it.id == id } }
        if (target != null) selectedReport = target
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = "AI 报告",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiReportFilter.entries.forEach { option ->
                AiReportFilterPill(
                    label = option.label,
                    selected = filter == option,
                    onClick = { filter = option }
                )
            }
        }

        if (filteredReports.isEmpty()) {
            EmptyStateCard(
                "还没有生成过 AI 报告。可以在 设置 → AI 调用配置 → AI 日报 / 周报 中开启自动生成或立即生成。"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredReports, key = { it.id }) { report ->
                    AiReportCard(
                        report = report,
                        modifier = Modifier.combinedClickable(
                            onClick = { selectedReport = report },
                            onLongClick = { pendingDelete = report }
                        )
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    selectedReport?.let { report ->
        ModalBottomSheet(
            onDismissRequest = { selectedReport = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AiReportDetail(
                report = report,
                onDelete = { pendingDelete = report }
            )
        }
    }

    pendingDelete?.let { report ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条 AI 报告？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val message = onDeleteReport(report.id)
                            if (message == null) {
                                Toast.makeText(context, "AI 报告已删除", Toast.LENGTH_SHORT).show()
                                if (selectedReport?.id == report.id) selectedReport = null
                            } else {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                            pendingDelete = null
                        }
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun AiReportFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AiReportCard(
    report: AiReport,
    modifier: Modifier = Modifier
) {
    val purple = reportColor(report.type)
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = formatReportDate(report.generatedAtMillis),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AiReportPill(typeLabel(report.type), purple)
                    if (report.isLocalFallback) AiReportPill("本地", MaterialTheme.colorScheme.outline)
                }
            }
            Text(
                text = report.content.replace(Regex("\\s+"), " ").trim().take(80).ifBlank { "（空报告）" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "来源：${report.providerName.ifBlank { "未知来源" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AiReportDetail(
    report: AiReport,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, bottom = 26.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AiReportPill(typeLabel(report.type), reportColor(report.type))
                    if (report.isLocalFallback) AiReportPill("本地模板", MaterialTheme.colorScheme.outline)
                }
                Text(
                    text = formatReportDate(report.generatedAtMillis),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "覆盖周期：${formatReportDate(report.periodStartMillis)} - ${formatReportDate(report.periodEndMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "来源：${report.providerName.ifBlank { "未知来源" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("删除")
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        ) {
            Text(
                text = report.content.ifBlank { "（空报告）" },
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AiReportPill(label: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = tint.copy(alpha = 0.14f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

private fun typeLabel(type: AiReportType): String = when (type) {
    AiReportType.DAILY -> "日报"
    AiReportType.WEEKLY -> "周报"
}

@Composable
private fun reportColor(type: AiReportType): Color = when (type) {
    AiReportType.DAILY -> Color(0xFF8B5CF6)
    AiReportType.WEEKLY -> Color(0xFF7C3AED)
}

private fun formatReportDate(millis: Long): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEE HH:mm", Locale.CHINA))
}
