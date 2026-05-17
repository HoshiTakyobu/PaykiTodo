package com.example.todoalarm.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todoalarm.data.AiReport
import com.example.todoalarm.data.AiReportType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val AiReportPageSize = 30

private enum class AiReportFilter(val label: String, val type: AiReportType?) {
    ALL("全部", null),
    DAILY("日报", AiReportType.DAILY),
    WEEKLY("周报", AiReportType.WEEKLY)
}

private enum class AiReportRangeFilter(val label: String, val days: Long?) {
    ALL("全部时间", null),
    LAST_7("近 7 天", 7),
    LAST_30("近 30 天", 30),
    LAST_90("近 90 天", 90)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun AiReportPanel(
    observeReports: (AiReportType?, Int, String, Long, Long) -> Flow<List<AiReport>>,
    onGetReport: suspend (Long) -> AiReport?,
    targetReportId: Long?,
    targetReportSerial: Int,
    onDeleteReport: suspend (Long) -> String?
) {
    var filter by rememberSaveable { mutableStateOf(AiReportFilter.ALL) }
    var rangeFilter by rememberSaveable { mutableStateOf(AiReportRangeFilter.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    var limit by rememberSaveable { mutableStateOf(AiReportPageSize) }
    var selectedReport by remember { mutableStateOf<AiReport?>(null) }
    var pendingDelete by remember { mutableStateOf<AiReport?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val rangeBounds = remember(rangeFilter) {
        aiReportRangeBounds(rangeFilter, zone)
    }
    val safeQuery = query.trim()
    val reportFlow = remember(filter, rangeFilter, safeQuery, limit) {
        observeReports(filter.type, limit, safeQuery, rangeBounds.first, rangeBounds.second)
    }
    val reports by reportFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(targetReportSerial, targetReportId) {
        val target = targetReportId?.let { id ->
            reports.firstOrNull { it.id == id } ?: onGetReport(id)
        }
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

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it.take(80)
                    limit = AiReportPageSize
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        TextButton(
                            onClick = {
                                query = ""
                                limit = AiReportPageSize
                            }
                        ) {
                            Text("清空")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                placeholder = { Text("搜索报告正文或来源") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AiReportDropdown(
                    label = filter.label,
                    options = AiReportFilter.entries,
                    optionLabel = { it.label },
                    onSelect = {
                        filter = it
                        limit = AiReportPageSize
                    }
                )
                AiReportDropdown(
                    label = rangeFilter.label,
                    options = AiReportRangeFilter.entries,
                    optionLabel = { it.label },
                    onSelect = {
                        rangeFilter = it
                        limit = AiReportPageSize
                    }
                )
            }
        }

        if (reports.isEmpty()) {
            EmptyStateCard(
                if (safeQuery.isBlank() && filter == AiReportFilter.ALL && rangeFilter == AiReportRangeFilter.ALL) {
                    "还没有生成过 AI 报告。可以在 设置 → AI 调用配置 → AI 日报 / 周报 中开启自动生成或立即生成。"
                } else {
                    "没有匹配的 AI 报告。可以放宽关键词、类型或时间范围。"
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reports, key = { it.id }) { report ->
                    AiReportCard(
                        report = report,
                        modifier = Modifier.combinedClickable(
                            onClick = { selectedReport = report },
                            onLongClick = { pendingDelete = report }
                        )
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "已加载 ${reports.size} 条报告 · ${filter.label} · ${rangeFilter.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (reports.size >= limit) {
                            OutlinedButton(onClick = { limit += AiReportPageSize }) {
                                Text("加载更多")
                            }
                        }
                    }
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
private fun <T> AiReportDropdown(
    label: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
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

private fun aiReportRangeBounds(filter: AiReportRangeFilter, zone: ZoneId): Pair<Long, Long> {
    val days = filter.days ?: return Long.MIN_VALUE to Long.MAX_VALUE
    val start = LocalDate.now(zone)
        .minusDays(days - 1)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
    return start to Long.MAX_VALUE
}
