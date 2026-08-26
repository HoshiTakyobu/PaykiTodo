package com.example.todoalarm.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    var expanded by remember { mutableStateOf(false) }
    var showManager by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<TaskGroup?>(null) }
    val sortedGroups = remember(groups) {
        groups.sortedWith(compareBy<TaskGroup> { it.sortOrder }.thenBy { it.id })
    }
    val selectedGroups = remember(sortedGroups, selectedGroupIds) {
        sortedGroups.filter { it.id in selectedGroupIds }
    }
    val summary = when {
        selectedGroups.isEmpty() -> "全部分组"
        selectedGroups.size == 1 -> selectedGroups.single().name
        else -> "已选 ${selectedGroups.size} 个分组（${if (groupFilterMode == TodoRepository.GroupFilterMode.INTERSECTION) "交集" else "并集"}）"
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "分组筛选",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TaskAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedGroupIds.isEmpty()) "显示所有待办" else "点击展开并调整分组范围",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = "展开分组筛选")
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(maxWidth)
                .heightIn(max = 380.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Text("管理分组", fontWeight = FontWeight.SemiBold)
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Settings, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    showManager = true
                }
            )
            HorizontalDivider()
            GroupFilterMenuItem(
                label = "全部",
                color = MaterialTheme.colorScheme.primary,
                selected = selectedGroupIds.isEmpty(),
                onClick = { onSelectGroup(null); expanded = false }
            )
            sortedGroups.forEach { group ->
                GroupFilterMenuItem(
                    label = group.name,
                    color = colorFromHex(group.colorHex),
                    selected = group.id in selectedGroupIds,
                    onClick = { onSelectGroup(group.id) }
                )
            }
            if (selectedGroupIds.size >= 2) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = {
                        Text(
                            if (groupFilterMode == TodoRepository.GroupFilterMode.INTERSECTION) {
                                "当前为交集：切换为并集"
                            } else {
                                "当前为并集：切换为交集"
                            }
                        )
                    },
                    onClick = onToggleGroupFilterMode
                )
            }
        }
    }

    if (showManager) {
        GroupManagerDialog(
            groups = sortedGroups,
            onDismiss = { showManager = false },
            onCreate = { showManager = false; creating = true },
            onEdit = { showManager = false; editingGroup = it }
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

@Composable
private fun GroupFilterMenuItem(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
        },
        trailingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = "已选择", tint = MaterialTheme.colorScheme.primary) }
        } else {
            null
        },
        onClick = onClick
    )
}

@Composable
private fun GroupManagerDialog(
    groups: List<TaskGroup>,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (TaskGroup) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理分组") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groups.forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(group) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(colorFromHex(group.colorHex), CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(group.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onEdit(group) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "编辑分组")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreate) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("新建分组")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
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
                message?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val result = onConfirm(name.trim(), colorHex)
                    if (result == null) onDismiss() else message = result
                }
            }) { Text(if (initial == null) "创建" else "完成") }
        },
        dismissButton = {
            Row {
                if (initial != null && onDelete != null && !initial.isDefault) {
                    TextButton(onClick = { showDeleteConfirm = true }) { Text("删除") }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
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
                    if (result == null) onDismiss() else message = result
                }
            }
        )
    }
}
