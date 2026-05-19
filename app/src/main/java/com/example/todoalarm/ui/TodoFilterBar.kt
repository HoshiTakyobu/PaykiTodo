package com.example.todoalarm.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoRepository
import kotlinx.coroutines.launch

private val TodoGroupColorPalette = listOf(
    "#BF7B4D", "#FF6B4A", "#4E87E1", "#4CB782", "#8B5CF6",
    "#E11D48", "#0F766E", "#D97706", "#2563EB", "#16A34A"
)

@Composable
internal fun TodoFilterBar(
    groups: List<TaskGroup>,
    selectedGroupIds: Set<Long>,
    groupFilterMode: TodoRepository.GroupFilterMode,
    onSelectGroup: (Long?) -> Unit,
    onToggleGroupFilterMode: () -> Unit,
    onCreateGroup: suspend (String, String) -> String?,
    onUpdateGroup: suspend (TaskGroup) -> String?,
    onDeleteGroup: suspend (Long) -> String?
) {
    var creating by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<TaskGroup?>(null) }
    val sortedGroups = remember(groups) { groups.sortedWith(compareBy<TaskGroup> { it.sortOrder }.thenBy { it.id }) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "分组筛选",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TodoGroupChip(
                label = "全部",
                color = MaterialTheme.colorScheme.primary,
                selected = selectedGroupIds.isEmpty(),
                leadingIcon = true,
                onClick = { onSelectGroup(null) }
            )
            sortedGroups.forEach { group ->
                TodoGroupChip(
                    label = group.name,
                    color = colorFromHex(group.colorHex),
                    selected = group.id in selectedGroupIds,
                    onClick = { onSelectGroup(group.id) },
                    onLongClick = { editingGroup = group }
                )
            }
            TodoGroupChip(
                label = "新建",
                color = MaterialTheme.colorScheme.primary,
                selected = false,
                leadingAddIcon = true,
                onClick = { creating = true }
            )
            if (selectedGroupIds.size >= 2) {
                FilterChip(
                    selected = true,
                    onClick = onToggleGroupFilterMode,
                    label = {
                        Text(
                            if (groupFilterMode == TodoRepository.GroupFilterMode.INTERSECTION) "∩ 交集"
                            else "∪ 并集"
                        )
                    }
                )
            }
        }
        Text(
            text = if (selectedGroupIds.size >= 2) {
                "当前多选：${if (groupFilterMode == TodoRepository.GroupFilterMode.INTERSECTION) "交集" else "并集"}；长按可管理分组。"
            } else {
                "长按可管理分组。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (creating) {
        TodoGroupEditorDialog(
            initial = null,
            onDismiss = { creating = false },
            onConfirm = onCreateGroup
        )
    }

    editingGroup?.let { group ->
        TodoGroupEditorDialog(
            initial = group,
            onDismiss = { editingGroup = null },
            onConfirm = { name, color -> onUpdateGroup(group.copy(name = name, colorHex = color)) },
            onDelete = { onDeleteGroup(group.id) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoGroupChip(
    label: String,
    color: Color,
    selected: Boolean,
    leadingIcon: Boolean = false,
    leadingAddIcon: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val container = if (selected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    val content = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        shape = RoundedCornerShape(999.dp),
        color = container,
        border = BorderStroke(1.dp, color.copy(alpha = if (selected) 0.75f else 0.36f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                leadingIcon -> Icon(
                    imageVector = Icons.Rounded.TaskAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
                leadingAddIcon -> Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
                else -> Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color, CircleShape)
                )
            }
            Text(
                text = label,
                color = content,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TodoGroupEditorDialog(
    initial: TaskGroup?,
    onDismiss: () -> Unit,
    onConfirm: suspend (String, String) -> String?,
    onDelete: (suspend () -> String?)? = null
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var colorHex by remember(initial?.id) { mutableStateOf(initial?.colorHex ?: TodoGroupColorPalette.first()) }
    var message by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建分组" else "编辑分组") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分组名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TodoGroupColorPalette.forEach { candidate ->
                        Surface(
                            modifier = Modifier
                                .size(if (candidate == colorHex) 36.dp else 30.dp)
                                .clickable { colorHex = candidate },
                            shape = CircleShape,
                            color = colorFromHex(candidate),
                            border = if (candidate == colorHex) {
                                BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f))
                            } else {
                                null
                            }
                        ) {}
                    }
                }
                message?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val result = onConfirm(name.trim(), colorHex)
                    if (result == null) {
                        onDismiss()
                    } else {
                        message = result
                    }
                }
            }) {
                Text(if (initial == null) "创建" else "保存")
            }
        },
        dismissButton = {
            Row {
                if (initial != null && onDelete != null && !initial.isDefault) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("删除")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )

    if (showDeleteConfirm && initial != null && onDelete != null) {
        PaykiDecisionBottomSheet(
            title = "删除分组",
            message = "确定删除“${initial.name}”吗？删除后无法恢复。",
            confirmLabel = "删除",
            confirmLabelColor = MaterialTheme.colorScheme.error,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                scope.launch {
                    showDeleteConfirm = false
                    val result = onDelete()
                    if (result == null) {
                        onDismiss()
                    } else {
                        message = result
                    }
                }
            }
        )
    }
}
