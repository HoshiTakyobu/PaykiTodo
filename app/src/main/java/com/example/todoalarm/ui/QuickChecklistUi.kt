package com.example.todoalarm.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.todoalarm.data.QuickChecklistDiagnosticSeverity
import com.example.todoalarm.data.QuickChecklistDraft
import com.example.todoalarm.data.QuickChecklistEditing
import com.example.todoalarm.data.QuickChecklistGroup
import com.example.todoalarm.data.QuickChecklistParser
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoItem

@Composable
internal fun QuickChecklistActionButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                contentDescription = "快速清单",
                modifier = Modifier.size(25.dp)
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("!", fontSize = 10.sp, fontWeight = FontWeight.Black, lineHeight = 10.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickChecklistEditorDialog(
    onDismiss: () -> Unit,
    onSave: suspend (String) -> String?,
    onSaved: (String) -> Unit
) {
    var value by remember { mutableStateOf(TextFieldValue("")) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val draft = remember(value.text) { QuickChecklistParser.parse(value.text) }

    fun selectSourceLine(line: Int) {
        val selection = QuickChecklistEditing.sourceLineSelection(value.text, line) ?: return
        value = value.copy(selection = TextRange(selection.start, selection.end))
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                modifier = Modifier.imePadding(),
                topBar = {
                    TopAppBar(
                        title = { Text("快速待办清单") },
                        navigationIcon = {
                            TextButton(enabled = !saving, onClick = onDismiss) { Text("取消") }
                        },
                        actions = {
                            Button(
                                enabled = !saving && !draft.hasErrors && draft.nodes.isNotEmpty(),
                                onClick = {
                                    saving = true
                                    saveError = null
                                }
                            ) { Text(if (saving) "保存中" else "确定") }
                            Spacer(Modifier.width(12.dp))
                        }
                    )
                }
            ) { padding ->
                androidx.compose.runtime.LaunchedEffect(saving) {
                    if (!saving) return@LaunchedEffect
                    val result = onSave(value.text)
                    saving = false
                    if (result == null) {
                        onSaved(draft.title.ifBlank { "快速清单" })
                    } else {
                        saveError = result
                        Regex("第\\s*(\\d+)\\s*行").find(result)?.groupValues?.getOrNull(1)?.toIntOrNull()
                            ?.let(::selectSourceLine)
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            "标题可选，可直接写 - 任务；短横线数量表示层级；--# 添加可见说明；// 后内容不会保存。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { next ->
                                value = applyQuickChecklistImeContinuation(value, next)
                                saveError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .focusRequester(focusRequester),
                            label = { Text("输入清单") },
                            placeholder = {
                                Text("0913待办\n- 准备资料\n-- 身份证复印件\n--# 请使用本学年材料")
                            },
                            maxLines = Int.MAX_VALUE
                        )
                    }
                    item { QuickChecklistPreview(draft, onDiagnosticClick = ::selectSourceLine) }
                    saveError?.let { message ->
                        item {
                            Text(
                                message,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Regex("第\\s*(\\d+)\\s*行").find(message)?.groupValues?.getOrNull(1)?.toIntOrNull()
                                            ?.let(::selectSourceLine)
                                    },
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

internal fun applyQuickChecklistImeContinuation(
    previous: TextFieldValue,
    next: TextFieldValue
): TextFieldValue {
    if (!previous.selection.collapsed || !next.selection.collapsed) return next
    if (next.text.length != previous.text.length + 1) return next
    val insertionAt = previous.selection.start
    if (insertionAt !in 0..previous.text.length || next.text.getOrNull(insertionAt) != '\n') return next
    val result = QuickChecklistEditing.insertLineBreak(previous.text, insertionAt)
    return TextFieldValue(result.text, TextRange(result.cursor))
}

@Composable
private fun QuickChecklistPreview(
    draft: QuickChecklistDraft,
    onDiagnosticClick: (Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(draft.title.ifBlank { "快速清单" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${draft.taskCount} 项任务 · ${draft.maxDepth} 层 · ${draft.noteCount} 条说明",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            draft.nodes.forEach { node ->
                Column(modifier = Modifier.padding(start = ((node.depth - 1).coerceAtLeast(0) * 18).dp)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("□", color = MaterialTheme.colorScheme.primary)
                        Text(node.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (node.notes.isNotBlank()) {
                        Text(
                            node.notes,
                            modifier = Modifier.padding(start = 26.dp, top = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            draft.diagnostics.forEach { diagnostic ->
                Text(
                    "第 ${diagnostic.sourceLine} 行：${diagnostic.message}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDiagnosticClick(diagnostic.sourceLine) },
                    color = if (diagnostic.severity == QuickChecklistDiagnosticSeverity.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color(0xFFD97706)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
internal fun QuickChecklistGroups(
    groups: List<QuickChecklistGroup>,
    items: List<TodoItem>,
    taskGroups: List<TaskGroup>,
    selectionMode: Boolean,
    selectedTodoIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onEnterSelection: (Long) -> Unit,
    onToggleGroup: (QuickChecklistGroup) -> Unit,
    onToggleTask: (TodoItem) -> Unit,
    onEdit: (TodoItem) -> Unit,
    onComplete: (TodoItem) -> Unit,
    onCancel: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit
) {
    val allByGroup = remember(items) { items.groupBy { it.quickChecklistGroupId } }
    groups.forEach { group ->
        val allGroupItems = allByGroup[group.id].orEmpty()
        val activeItems = allGroupItems.filter { it.isActive }
        if (activeItems.isEmpty()) return@forEach
        val activeIds = activeItems.mapTo(mutableSetOf()) { it.id }
        val childrenByParent = activeItems.groupBy { it.parentTodoId }
        val visibleItems = buildList {
            fun visit(item: TodoItem) {
                add(item)
                if (!item.quickChecklistCollapsed) {
                    childrenByParent[item.id].orEmpty()
                        .sortedWith(compareBy<TodoItem> { it.quickChecklistSortOrder ?: Int.MAX_VALUE }.thenBy { it.id })
                        .forEach(::visit)
                }
            }
            activeItems.filter { it.parentTodoId == null || it.parentTodoId !in activeIds }
                .sortedWith(compareBy<TodoItem> { it.quickChecklistSortOrder ?: Int.MAX_VALUE }.thenBy { it.id })
                .forEach(::visit)
        }
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.title.ifBlank { "快速清单" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val countItems = allGroupItems.filterNot { it.canceled }
                    Text(
                        "${countItems.count { it.completed }} / ${countItems.size} 已完成",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onToggleGroup(group) }) {
                    Icon(
                        if (group.collapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                        contentDescription = if (group.collapsed) "展开清单" else "收起清单"
                    )
                }
            }
            if (!group.collapsed) {
                Column(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    visibleItems.forEach { item ->
                        val hasChildren = childrenByParent[item.id].orEmpty().isNotEmpty()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Spacer(Modifier.width(((item.quickChecklistDepth ?: 1) - 1).coerceAtLeast(0).times(14).dp))
                            if (hasChildren) {
                                IconButton(
                                    onClick = { onToggleTask(item) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        if (item.quickChecklistCollapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                                        contentDescription = if (item.quickChecklistCollapsed) "展开子任务" else "收起子任务",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Spacer(Modifier.width(36.dp))
                            }
                            ActiveTodoCard(
                                item = item,
                                groups = taskGroups,
                                modifier = Modifier.weight(1f),
                                selectionMode = selectionMode,
                                selected = item.id in selectedTodoIds,
                                onSelectionToggle = { onToggleSelection(item.id) },
                                onEnterSelection = { onEnterSelection(item.id) },
                                onEdit = { onEdit(item) },
                                onComplete = { onComplete(item) },
                                onCancel = { onCancel(item) },
                                onDelete = { onDelete(item) }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
