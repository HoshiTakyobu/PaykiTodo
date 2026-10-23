package com.example.todoalarm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todoalarm.data.*
import kotlinx.coroutines.delay
import java.util.UUID

internal data class WorkspacePreferences(
    val settings: AppSettings = AppSettings(),
    val calendars: List<PlannerCalendar> = listOf(PlannerCalendar()),
    val saveCalendar: (PlannerCalendar) -> Unit = {},
    val deleteCalendar: (String) -> Unit = {},
    val display: (String, String, Int, Set<String>) -> Unit = { _, _, _, _ -> }
) {
    fun visibility(id: String, visible: Boolean) = display(settings.mainFont, settings.customFontPath, settings.countdownPastDays,
        if (visible) settings.hiddenCalendarIds - id else settings.hiddenCalendarIds + id)
}

internal val LocalWorkspacePreferences = staticCompositionLocalOf { WorkspacePreferences() }

@Composable
internal fun ProvideWorkspacePreferences(viewModel: TodoViewModel, settings: AppSettings, authenticated: Boolean, content: @Composable () -> Unit) {
    val calendars by viewModel.calendars.collectAsStateWithLifecycle()
    val countdowns by viewModel.countdownItems.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var deferred by remember { mutableStateOf(emptySet<Long>()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(15_000) } }
    val preferences = WorkspacePreferences(settings, calendars.ifEmpty { listOf(PlannerCalendar()) }, viewModel::saveCalendar, viewModel::deleteCalendar, viewModel::updateDisplayPreferences)
    CompositionLocalProvider(LocalWorkspacePreferences provides preferences) { content() }
    val pending = if (authenticated) countdowns.firstOrNull { it.id !in deferred && CountdownVisibility.needsDecision(it, now, settings.countdownPastDays) } else null
    if (pending != null) CountdownDecisionDialog(pending,
        onDismiss = { deferred = deferred + pending.id },
        onDecide = { mode -> viewModel.setCountdownPastMode(pending.id, mode); deferred = deferred + pending.id })
}

@Composable
internal fun CountdownDecisionDialog(item: TodoItem, onDismiss: () -> Unit, onDecide: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("继续保留这个日子？") },
        text = { Text("“${item.title}”已完成或结束。是否继续显示在值得期待和倒数日中，记录已经过去的时光？") },
        confirmButton = { TextButton(onClick = { onDecide("KEEP") }) { Text("保留纪念") } },
        dismissButton = { TextButton(onClick = { onDecide("HIDE") }) { Text("不再展示") } }
    )
}

@Composable
internal fun PreferenceChoice(title: String, value: String, choices: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text(choices.firstOrNull { it.first == value }?.second ?: value, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            Text("  ›", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { (key, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; onSelect(key) }) }
        }
    }
}

internal val CountdownPastChoices = listOf("AUTO" to "按设置自动隐藏", "KEEP" to "一直保留纪念", "HIDE" to "不再展示")

@Composable
internal fun CalendarManager(onDismiss: () -> Unit) {
    val workspace = LocalWorkspacePreferences.current
    var editing by remember { mutableStateOf<PlannerCalendar?>(null) }
    var deleting by remember { mutableStateOf<PlannerCalendar?>(null) }
    if (editing != null) {
        val source = editing!!
        var name by remember(source.id) { mutableStateOf(source.name) }
        var color by remember(source.id) { mutableStateOf(source.colorHex) }
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("编辑日历") }, text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("日历名称") }, singleLine = true)
                PreferenceChoice("颜色", color, listOf("#3370FF" to "蓝色", "#16A369" to "绿色", "#DB5298" to "粉色", "#BD6B17" to "橙色", "#7955CC" to "紫色", "#D94646" to "红色")) { color = it }
                Text("课程安排 · 09:00–09:30", color = Color(android.graphics.Color.parseColor(color)))
            }
        }, confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { workspace.saveCalendar(source.copy(name = name.trim(), colorHex = color)); editing = null }) { Text("完成") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("取消") } })
    } else AlertDialog(onDismissRequest = onDismiss, title = { Text("日历") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("分别安排课程表与日常日程。取消勾选仅隐藏日历，不影响提醒。", style = MaterialTheme.typography.bodySmall)
            workspace.calendars.forEach { calendar ->
                val accent = runCatching { Color(android.graphics.Color.parseColor(calendar.colorHex)) }.getOrDefault(Color(0xFF3370FF))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = calendar.id !in workspace.settings.hiddenCalendarIds, onCheckedChange = { workspace.visibility(calendar.id, it) }, modifier = Modifier.semantics { contentDescription = "显示日历：${calendar.name}" }, colors = CheckboxDefaults.colors(checkedColor = accent, uncheckedColor = accent, checkmarkColor = if (accent.luminance() > .5f) Color(0xFF182033) else Color.White))
                    Text(calendar.name, Modifier.weight(1f).clickable { editing = calendar })
                    TextButton(onClick = { editing = calendar }) { Text("编辑") }
                    if (calendar.id != DEFAULT_CALENDAR_ID) TextButton(onClick = { deleting = calendar }) { Text("删除") }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { editing = PlannerCalendar(UUID.randomUUID().toString(), "", sortOrder = workspace.calendars.size) }) { Text("新增日历") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("完成") } })
    deleting?.let { calendar -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除日历？") }, text = { Text("日程及其循环规则会移至常规日历，不会删除事项。") },
        confirmButton = { TextButton(onClick = { workspace.deleteCalendar(calendar.id); deleting = null }) { Text("删除") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }) }
}
