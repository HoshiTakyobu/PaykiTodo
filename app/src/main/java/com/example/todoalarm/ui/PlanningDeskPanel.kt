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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.PlanningNote
import com.example.todoalarm.data.PlanningParseResult
import com.example.todoalarm.data.PlanningParsedCandidate
import com.example.todoalarm.data.PlanningParsedType
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanningDeskPanel(
    notes: List<PlanningNote>,
    activeNote: PlanningNote?,
    onSelectNote: (Long) -> Unit,
    onCreateNote: suspend (String) -> String?,
    onSaveNote: suspend (Long, String) -> String?,
    onRenameNote: suspend (Long, String) -> String?,
    onDeleteNote: suspend (Long) -> String?,
    onArchiveNote: suspend (Long) -> String?,
    onParse: (String) -> PlanningParseResult,
    onImport: suspend (List<PlanningParsedCandidate>, Set<String>, Set<String>) -> String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editorValue by remember(activeNote?.id) { mutableStateOf(TextFieldValue(activeNote?.contentMarkdown.orEmpty())) }
    var parseResult by remember { mutableStateOf<PlanningParseResult?>(null) }
    var documentSheetVisible by remember { mutableStateOf(false) }
    var previewSheetVisible by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var archiveDialog by remember { mutableStateOf(false) }
    var newDialog by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateMapOf<String, Boolean>() }
    val linkedTodoIds = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(activeNote?.id, activeNote?.contentMarkdown) {
        editorValue = TextFieldValue(activeNote?.contentMarkdown.orEmpty())
        parseResult = null
        selectedIds.clear()
        linkedTodoIds.clear()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Article, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeNote?.title ?: "我的规划",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "像备忘录一样先写计划，再识别为待办 / 日程。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { documentSheetVisible = true }) { Icon(Icons.Rounded.Article, contentDescription = "文档") }
                    IconButton(onClick = { renameDialog = true }, enabled = activeNote != null) { Icon(Icons.Rounded.Edit, contentDescription = "重命名") }
                    IconButton(onClick = { archiveDialog = true }, enabled = activeNote != null) { Icon(Icons.Rounded.Archive, contentDescription = "归档") }
                    IconButton(onClick = { deleteDialog = true }, enabled = activeNote != null) { Icon(Icons.Rounded.Delete, contentDescription = "删除") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { newDialog = true }
                    ) { Text("新建") }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val noteId = activeNote?.id ?: return@OutlinedButton
                            scope.launch {
                                val message = onSaveNote(noteId, editorValue.text)
                                Toast.makeText(context, message ?: "规划已保存", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = activeNote != null
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("保存")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val result = onParse(editorValue.text)
                            parseResult = result
                            selectedIds.clear()
                            linkedTodoIds.clear()
                            result.candidates.forEach { candidate ->
                                selectedIds[candidate.id] = candidate.importable
                                if (candidate.type == PlanningParsedType.EVENT) {
                                    linkedTodoIds[candidate.id] = candidate.createLinkedTodo
                                }
                            }
                            previewSheetVisible = true
                        }
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("识别")
                    }
                }
            }
        }

        OutlinedTextField(
            value = editorValue,
            onValueChange = { editorValue = autoContinuePlanningLine(editorValue, it) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { Text("例如：\n# 明天\n- [ ] 09:00-10:30 写论文 #group 课程\n- [ ] 整理材料 #ddl 5.28") },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            shape = RoundedCornerShape(22.dp),
            minLines = 12
        )

        PlanningShortcutBar(
            onAction = { action -> editorValue = applyPlanningShortcut(editorValue, action) },
            onHelp = { token, description -> Toast.makeText(context, "$token：$description", Toast.LENGTH_LONG).show() }
        )
    }

    if (documentSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { documentSheetVisible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            PlanningDocumentSheet(
                notes = notes,
                activeNoteId = activeNote?.id,
                onSelect = {
                    onSelectNote(it)
                    documentSheetVisible = false
                }
            )
        }
    }

    if (previewSheetVisible && parseResult != null) {
        ModalBottomSheet(
            onDismissRequest = { previewSheetVisible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            PlanningPreviewSheet(
                result = requireNotNull(parseResult),
                selectedIds = selectedIds,
                linkedTodoIds = linkedTodoIds,
                onImport = {
                    scope.launch {
                        val message = onImport(
                            requireNotNull(parseResult).candidates,
                            selectedIds.filterValues { it }.keys,
                            linkedTodoIds.filterValues { it }.keys
                        )
                        Toast.makeText(context, message ?: "规划条目已导入", Toast.LENGTH_SHORT).show()
                        if (message == null) previewSheetVisible = false
                    }
                }
            )
        }
    }

    if (newDialog) {
        PlanningNameDialog(
            title = "新建规划文档",
            initial = "新的规划",
            confirmText = "新建",
            onDismiss = { newDialog = false },
            onConfirm = { title ->
                scope.launch {
                    val message = onCreateNote(title)
                    Toast.makeText(context, message ?: "已新建规划文档", Toast.LENGTH_SHORT).show()
                    newDialog = false
                }
            }
        )
    }
    if (renameDialog && activeNote != null) {
        PlanningNameDialog(
            title = "重命名规划文档",
            initial = activeNote.title,
            confirmText = "保存",
            onDismiss = { renameDialog = false },
            onConfirm = { title ->
                scope.launch {
                    val message = onRenameNote(activeNote.id, title)
                    Toast.makeText(context, message ?: "文档已重命名", Toast.LENGTH_SHORT).show()
                    renameDialog = false
                }
            }
        )
    }
    if (deleteDialog && activeNote != null) {
        ConfirmPlanningDialog(
            title = "删除规划文档？",
            message = "删除后无法恢复：${activeNote.title}",
            confirmText = "删除",
            onDismiss = { deleteDialog = false },
            onConfirm = {
                scope.launch {
                    val message = onDeleteNote(activeNote.id)
                    Toast.makeText(context, message ?: "文档已删除", Toast.LENGTH_SHORT).show()
                    deleteDialog = false
                }
            }
        )
    }
    if (archiveDialog && activeNote != null) {
        ConfirmPlanningDialog(
            title = "归档规划文档？",
            message = "归档后会从当前列表隐藏：${activeNote.title}",
            confirmText = "归档",
            onDismiss = { archiveDialog = false },
            onConfirm = {
                scope.launch {
                    val message = onArchiveNote(activeNote.id)
                    Toast.makeText(context, message ?: "文档已归档", Toast.LENGTH_SHORT).show()
                    archiveDialog = false
                }
            }
        )
    }

}

@Composable
private fun PlanningShortcutBar(
    onAction: (PlanningShortcutAction) -> Unit,
    onHelp: (String, String) -> Unit
) {
    val chips = listOf(
        PlanningShortcutSpec("任务", PlanningShortcutAction.Insert("- [ ] "), "插入一条待办任务"),
        PlanningShortcutSpec("子任务", PlanningShortcutAction.Insert("  - [ ] "), "插入缩进后的子任务"),
        PlanningShortcutSpec("缩进", PlanningShortcutAction.Indent, "当前行增加一级缩进"),
        PlanningShortcutSpec("减少缩进", PlanningShortcutAction.Outdent, "当前行减少一级缩进"),
        PlanningShortcutSpec("DDL", PlanningShortcutAction.Insert(" #ddl "), "设置截止时间，例如 #ddl 5.28 23:59"),
        PlanningShortcutSpec("日程", PlanningShortcutAction.Insert(" #schedule "), "显式声明日程时间段"),
        PlanningShortcutSpec("提醒", PlanningShortcutAction.Insert(" #remind "), "设置提醒，例如 #remind 5,15"),
        PlanningShortcutSpec("分组", PlanningShortcutAction.Insert(" #group "), "指定分组，例如 #group 课程"),
        PlanningShortcutSpec("今天", PlanningShortcutAction.Insert("今天"), "插入今天"),
        PlanningShortcutSpec("明天", PlanningShortcutAction.Insert("明天"), "插入明天")
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.take(5).forEach { spec ->
            PlanningShortcutChip(spec, onAction, onHelp)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.drop(5).forEach { spec ->
            PlanningShortcutChip(spec, onAction, onHelp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlanningShortcutChip(
    spec: PlanningShortcutSpec,
    onAction: (PlanningShortcutAction) -> Unit,
    onHelp: (String, String) -> Unit
) {
    AssistChip(
        modifier = Modifier.combinedClickable(onClick = { onAction(spec.action) }, onLongClick = { onHelp(spec.label, spec.help) }),
        onClick = { onAction(spec.action) },
        label = { Text(spec.label, maxLines = 1) }
    )
}

@Composable
private fun PlanningDocumentSheet(
    notes: List<PlanningNote>,
    activeNoteId: Long?,
    onSelect: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("规划文档", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        notes.forEach { note ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = if (note.id == activeNoteId) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                onClick = { onSelect(note.id) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(note.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("更新于 ${planningMillisLabel(note.updatedAtMillis)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PlanningPreviewSheet(
    result: PlanningParseResult,
    selectedIds: MutableMap<String, Boolean>,
    linkedTodoIds: MutableMap<String, Boolean>,
    onImport: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text("识别预览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("共 ${result.candidates.size} 行，${result.importableCount} 条可导入。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onImport) { Text("导入选中") }
        }
        LazyColumn(modifier = Modifier.fillMaxWidth().height(520.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(result.candidates, key = { it.id }) { candidate ->
                PlanningCandidateCard(
                    candidate = candidate,
                    selected = selectedIds[candidate.id] == true,
                    linkedTodo = linkedTodoIds[candidate.id] == true,
                    onSelectedChange = { selectedIds[candidate.id] = it },
                    onLinkedTodoChange = { linkedTodoIds[candidate.id] = it }
                )
            }
        }
    }
}

@Composable
private fun PlanningCandidateCard(
    candidate: PlanningParsedCandidate,
    selected: Boolean,
    linkedTodo: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onLinkedTodoChange: (Boolean) -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selected,
                    onClick = { if (candidate.importable) onSelectedChange(!selected) },
                    enabled = candidate.importable,
                    label = { Text(if (candidate.importable) "导入" else "不导入") }
                )
                Text(candidate.type.label(), fontWeight = FontWeight.Bold, color = candidate.type.color())
                Text("第 ${candidate.lineNumber} 行", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(candidate.sourceLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (candidate.title.isNotBlank()) Text(candidate.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(candidate.timeSummary(), style = MaterialTheme.typography.bodyMedium)
            if (candidate.groupName.isNotBlank()) Text("分组：${candidate.groupName}", style = MaterialTheme.typography.bodySmall)
            if (candidate.reminderOffsetsMinutes.isNotEmpty() && candidate.importable) {
                Text("提醒：${candidate.reminderOffsetsMinutes.joinToString("、") { "提前 ${it} 分钟" }} · 全屏 · 响铃 + 震动", style = MaterialTheme.typography.bodySmall)
            }
            if (candidate.type == PlanningParsedType.EVENT) {
                FilterChip(
                    selected = linkedTodo,
                    onClick = { onLinkedTodoChange(!linkedTodo) },
                    label = { Text("同时创建待办，DDL = 日程结束时间") }
                )
            }
            if (candidate.message.isNotBlank()) Text(candidate.message, color = if (candidate.type == PlanningParsedType.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlanningNameDialog(
    title: String,
    initial: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by rememberSaveable(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("文档名称") }) },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ConfirmPlanningDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private data class PlanningShortcutSpec(
    val label: String,
    val action: PlanningShortcutAction,
    val help: String
)

private sealed interface PlanningShortcutAction {
    data class Insert(val token: String) : PlanningShortcutAction
    data object Indent : PlanningShortcutAction
    data object Outdent : PlanningShortcutAction
}

private fun applyPlanningShortcut(value: TextFieldValue, action: PlanningShortcutAction): TextFieldValue {
    return when (action) {
        is PlanningShortcutAction.Insert -> insertPlanningToken(value, action.token)
        PlanningShortcutAction.Indent -> indentCurrentPlanningLine(value)
        PlanningShortcutAction.Outdent -> outdentCurrentPlanningLine(value)
    }
}

private fun insertPlanningToken(value: TextFieldValue, token: String): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val nextText = value.text.replaceRange(start, end, token)
    val cursor = start + token.length
    return value.copy(text = nextText, selection = androidx.compose.ui.text.TextRange(cursor))
}

private fun indentCurrentPlanningLine(value: TextFieldValue): TextFieldValue {
    val lineStart = value.text.lastIndexOf('\n', (value.selection.min - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
    val nextText = value.text.replaceRange(lineStart, lineStart, "  ")
    return value.copy(text = nextText, selection = androidx.compose.ui.text.TextRange(value.selection.min + 2, value.selection.max + 2))
}

private fun outdentCurrentPlanningLine(value: TextFieldValue): TextFieldValue {
    val lineStart = value.text.lastIndexOf('\n', (value.selection.min - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
    val removeCount = when {
        value.text.substring(lineStart).startsWith("  ") -> 2
        value.text.substring(lineStart).startsWith("\t") -> 1
        else -> 0
    }
    if (removeCount == 0) return value
    val nextText = value.text.removeRange(lineStart, lineStart + removeCount)
    return value.copy(
        text = nextText,
        selection = androidx.compose.ui.text.TextRange(
            (value.selection.min - removeCount).coerceAtLeast(lineStart),
            (value.selection.max - removeCount).coerceAtLeast(lineStart)
        )
    )
}

private fun autoContinuePlanningLine(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    if (new.text.length != old.text.length + 1 || !new.text.endsWith('\n')) return new
    val previousLine = old.text.substringBeforeLast('\n', old.text).substringAfterLast('\n')
    val match = Regex("^(\\s*)- \\[ \\]\\s*(.*)$").matchEntire(previousLine) ?: return new
    if (match.groupValues[2].isBlank()) return new
    val suffix = match.groupValues[1] + "- [ ] "
    return new.copy(
        text = new.text + suffix,
        selection = androidx.compose.ui.text.TextRange(new.text.length + suffix.length)
    )
}

private fun PlanningParsedType.label(): String = when (this) {
    PlanningParsedType.TODO -> "待办"
    PlanningParsedType.EVENT -> "日程"
    PlanningParsedType.SKIPPED -> "跳过"
    PlanningParsedType.ERROR -> "错误"
}

@Composable
private fun PlanningParsedType.color() = when (this) {
    PlanningParsedType.TODO -> MaterialTheme.colorScheme.primary
    PlanningParsedType.EVENT -> MaterialTheme.colorScheme.tertiary
    PlanningParsedType.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
    PlanningParsedType.ERROR -> MaterialTheme.colorScheme.error
}

private fun PlanningParsedCandidate.timeSummary(): String {
    return when (type) {
        PlanningParsedType.TODO -> dueAt?.let { "DDL：${planningDateTimeLabel(it)}" } ?: "无 DDL"
        PlanningParsedType.EVENT -> "${planningDateTimeLabel(startAt)} - ${planningDateTimeLabel(endAt)}" + if (defaultToday) "（默认今天）" else ""
        PlanningParsedType.SKIPPED -> message.ifBlank { "跳过" }
        PlanningParsedType.ERROR -> message.ifBlank { "无法识别" }
    }
}

private fun planningDateTimeLabel(value: LocalDateTime?): String {
    if (value == null) return "未设置"
    return value.format(DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA))
}

private fun planningMillisLabel(value: Long): String {
    val dateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(value), java.time.ZoneId.systemDefault())
    return planningDateTimeLabel(dateTime)
}
