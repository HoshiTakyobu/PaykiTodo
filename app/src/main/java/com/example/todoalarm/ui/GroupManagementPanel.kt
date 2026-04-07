package com.example.todoalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.TaskGroup
import kotlinx.coroutines.launch

private val GroupColorPalette = listOf(
    "#BF7B4D", "#FF6B4A", "#4E87E1", "#4CB782", "#8B5CF6", "#E11D48", "#0F766E", "#D97706", "#2563EB", "#16A34A"
)

@Composable
fun GroupManagementPanel(
    groups: List<TaskGroup>,
    onCreateGroup: suspend (String, String) -> String?,
    onUpdateGroup: suspend (TaskGroup) -> String?,
    onDeleteGroup: suspend (Long) -> String?
) {
    var editingGroup by remember { mutableStateOf<TaskGroup?>(null) }
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ElevatedCard(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "分组管理",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "你可以新建自定义分组，并为分组选择颜色。正在被活动任务使用的分组不能直接删除。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { creating = true }) {
                    Text("新建分组")
                }
            }
        }

        groups.forEach { group ->
            ElevatedCard(shape = RoundedCornerShape(22.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(colorFromHex(group.colorHex), CircleShape)
                        )
                        Column {
                            Text(group.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = if (group.isDefault) "默认分组" else "自定义分组",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { editingGroup = group }) {
                            Text("编辑")
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        GroupEditorDialog(
            initial = null,
            onDismiss = { creating = false },
            onConfirm = onCreateGroup
        )
    }

    editingGroup?.let { group ->
        GroupEditorDialog(
            initial = group,
            onDismiss = { editingGroup = null },
            onConfirm = { name, color -> onUpdateGroup(group.copy(name = name, colorHex = color)) },
            onDelete = { onDeleteGroup(group.id) }
        )
    }
}

@Composable
private fun GroupEditorDialog(
    initial: TaskGroup?,
    onDismiss: () -> Unit,
    onConfirm: suspend (String, String) -> String?,
    onDelete: (suspend () -> String?)? = null
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var colorHex by remember(initial?.id) { mutableStateOf(initial?.colorHex ?: GroupColorPalette.first()) }
    var message by remember { mutableStateOf<String?>(null) }
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
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GroupColorPalette.forEach { candidate ->
                        Box(
                            modifier = Modifier
                                .size(if (candidate == colorHex) 36.dp else 30.dp)
                                .background(colorFromHex(candidate), CircleShape)
                                .clickable { colorHex = candidate }
                        )
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
                    TextButton(onClick = {
                        scope.launch {
                            val result = onDelete()
                            if (result == null) {
                                onDismiss()
                            } else {
                                message = result
                            }
                        }
                    }) {
                        Text("删除")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}
