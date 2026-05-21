package com.example.todoalarm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.AiReport
import com.example.todoalarm.data.AiReportType
import com.example.todoalarm.data.GlobalSearchResult
import com.example.todoalarm.data.PlanningNodeSearchResult
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoItem
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchPanel(
    groups: List<TaskGroup>,
    onSearch: suspend (String) -> GlobalSearchResult,
    onDismiss: () -> Unit,
    onOpenTodo: (TodoItem) -> Unit,
    onOpenEvent: (TodoItem) -> Unit,
    onOpenPlanningNode: (PlanningNodeSearchResult) -> Unit,
    onOpenAiReport: (AiReport) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf(GlobalSearchResult.empty()) }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val safeQuery = query.trim()

    LaunchedEffect(safeQuery) {
        errorText = null
        if (safeQuery.isBlank()) {
            result = GlobalSearchResult.empty()
            loading = false
            return@LaunchedEffect
        }
        delay(300)
        loading = true
        result = try {
            onSearch(safeQuery)
        } catch (error: Exception) {
            errorText = error.message ?: "搜索失败"
            GlobalSearchResult.empty(safeQuery)
        } finally {
            loading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "全局搜索",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭搜索")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "清空搜索")
                        }
                    }
                },
                placeholder = { Text("搜索待办、日程、规划台、AI 报告") },
                shape = RoundedCornerShape(18.dp)
            )

            when {
                safeQuery.isBlank() -> SearchEmptyState("输入关键词后，会同时搜索待办、日程、规划台节点和 AI 报告。")
                loading -> Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                }
                errorText != null -> SearchEmptyState(errorText.orEmpty())
                result.totalCount == 0 -> SearchEmptyState("没有找到和“$safeQuery”相关的内容。")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (result.todos.isNotEmpty()) {
                        item { SearchSectionTitle("待办", result.todos.size, Color(0xFF4CB782)) }
                        items(result.todos, key = { "todo-${it.id}" }) { item ->
                            SearchTodoRow(item = item, groups = groups, onClick = { onOpenTodo(item) })
                        }
                    }
                    if (result.events.isNotEmpty()) {
                        item { SearchSectionTitle("日程", result.events.size, Color(0xFF4E87E1)) }
                        items(result.events, key = { "event-${it.id}" }) { event ->
                            SearchEventRow(item = event, onClick = { onOpenEvent(event) })
                        }
                    }
                    if (result.planningNodes.isNotEmpty()) {
                        item { SearchSectionTitle("规划台", result.planningNodes.size, Color(0xFFBF7B4D)) }
                        items(result.planningNodes, key = { "node-${it.node.id}" }) { nodeResult ->
                            SearchPlanningNodeRow(result = nodeResult, onClick = { onOpenPlanningNode(nodeResult) })
                        }
                    }
                    if (result.aiReports.isNotEmpty()) {
                        item { SearchSectionTitle("AI 报告", result.aiReports.size, Color(0xFF8B5CF6)) }
                        items(result.aiReports, key = { "report-${it.id}" }) { report ->
                            SearchAiReportRow(report = report, onClick = { onOpenAiReport(report) })
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

private val GlobalSearchResult.totalCount: Int
    get() = todos.size + events.size + planningNodes.size + aiReports.size

@Composable
private fun SearchSectionTitle(title: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.14f)) {
            Text(
                text = "$title ($count)",
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun SearchTodoRow(item: TodoItem, groups: List<TaskGroup>, onClick: () -> Unit) {
    val group = resolveTaskGroup(item, groups)
    SearchResultRow(
        icon = Icons.Rounded.TaskAlt,
        tint = colorFromHex(group.colorHex),
        title = item.title,
        subtitle = listOfNotNull(
            group.name,
            item.dueDateTimeOrNull()?.let { "DDL ${formatLocalDateTime(it)}" },
            if (item.completed) "已完成" else null,
            item.notes.takeIf { it.isNotBlank() }?.take(48)
        ).joinToString(" · "),
        onClick = onClick
    )
}

@Composable
private fun SearchEventRow(item: TodoItem, onClick: () -> Unit) {
    SearchResultRow(
        icon = Icons.Rounded.CalendarMonth,
        tint = colorFromHex(item.accentColorHex ?: "#4E87E1"),
        title = item.title,
        subtitle = listOfNotNull(
            item.startAtMillis?.let { formatLocalDateTime(reminderAtMillisToDateTime(it)) },
            item.location.takeIf { it.isNotBlank() }
        ).joinToString(" · "),
        onClick = onClick
    )
}

@Composable
private fun SearchPlanningNodeRow(result: PlanningNodeSearchResult, onClick: () -> Unit) {
    SearchResultRow(
        icon = Icons.Rounded.PostAdd,
        tint = Color(0xFFBF7B4D),
        title = result.node.text,
        subtitle = "${result.noteTitle} · 节点 #${result.node.id}",
        onClick = onClick
    )
}

@Composable
private fun SearchAiReportRow(report: AiReport, onClick: () -> Unit) {
    SearchResultRow(
        icon = Icons.Rounded.Insights,
        tint = Color(0xFF8B5CF6),
        title = "${report.type.searchLabel()} · ${formatSearchReportDate(report.generatedAtMillis)}",
        subtitle = report.content.lineSequence().firstOrNull { it.isNotBlank() }?.take(80).orEmpty(),
        onClick = onClick
    )
}

@Composable
private fun SearchResultRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = tint.copy(alpha = 0.14f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = tint
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyState(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun AiReportType.searchLabel(): String = when (this) {
    AiReportType.DAILY -> "日报"
    AiReportType.WEEKLY -> "周报"
}

private fun formatSearchReportDate(millis: Long): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA))
}
