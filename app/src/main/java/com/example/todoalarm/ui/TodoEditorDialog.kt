package com.example.todoalarm.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.TodoCategory
import com.example.todoalarm.data.TodoItem
import java.time.LocalDateTime

@Composable
fun TodoEditorDialog(
    initialTodo: TodoItem?,
    defaultRingEnabled: Boolean,
    defaultVibrateEnabled: Boolean,
    defaultVoiceEnabled: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: (String, String, LocalDateTime, LocalDateTime?, TodoCategory, Boolean, Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    val now = remember { LocalDateTime.now().withSecond(0).withNano(0) }
    val isHistory = initialTodo?.completed == true
    var title by remember(initialTodo?.id) { mutableStateOf(initialTodo?.title.orEmpty()) }
    var notes by remember(initialTodo?.id) { mutableStateOf(initialTodo?.notes.orEmpty()) }
    var category by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.let { TodoCategory.fromKey(it.categoryKey) } ?: TodoCategory.ROUTINE)
    }
    var dueAt by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.let { reminderAtMillisToDateTime(it.dueAtMillis) } ?: now.plusHours(2))
    }
    var reminderEnabled by remember(initialTodo?.id) {
        mutableStateOf(if (isHistory) false else initialTodo?.reminderEnabled ?: true)
    }
    var reminderAt by remember(initialTodo?.id) {
        mutableStateOf(initialTodo?.reminderAtMillis?.let(::reminderAtMillisToDateTime) ?: now.plusMinutes(30))
    }
    var ringEnabled by remember(initialTodo?.id) { mutableStateOf(initialTodo?.ringEnabled ?: defaultRingEnabled) }
    var vibrateEnabled by remember(initialTodo?.id) { mutableStateOf(initialTodo?.vibrateEnabled ?: defaultVibrateEnabled) }
    remember(initialTodo?.id) { defaultVoiceEnabled }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTodo == null) "新增任务" else "编辑任务",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
                Text(
                    text = "任务分类",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TodoCategory.entries.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item.label) }
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showDateTimePicker(context, dueAt) { dueAt = it } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("DDL：${formatLocalDateTime(dueAt)}")
                }
                if (isHistory) {
                    Text(
                        text = "历史任务允许修改标题、备注、分类和 DDL，但不会重新启用提醒。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("启用提醒", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("建议比 DDL 提前一段时间。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it }
                        )
                    }
                    if (reminderEnabled) {
                        OutlinedButton(
                            onClick = { showDateTimePicker(context, reminderAt) { reminderAt = it } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("提醒时间：${formatLocalDateTime(reminderAt)}")
                        }
                    }
                    Text("提醒方式", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(selected = ringEnabled, onClick = { ringEnabled = !ringEnabled }, label = { Text("响铃") })
                        FilterChip(selected = vibrateEnabled, onClick = { vibrateEnabled = !vibrateEnabled }, label = { Text("震动") })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title,
                        notes,
                        dueAt,
                        if (isHistory || !reminderEnabled) null else reminderAt,
                        category,
                        ringEnabled,
                        vibrateEnabled,
                        false
                    )
                }
            ) {
                Text(if (initialTodo == null) "创建任务" else "保存修改")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialTodo != null) {
                    TextButton(onClick = onDelete) {
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

private fun showDateTimePicker(
    context: Context,
    initialDateTime: LocalDateTime,
    onPicked: (LocalDateTime) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    onPicked(LocalDateTime.of(year, month + 1, day, hour, minute))
                },
                initialDateTime.hour,
                initialDateTime.minute,
                true
            ).show()
        },
        initialDateTime.year,
        initialDateTime.monthValue - 1,
        initialDateTime.dayOfMonth
    ).show()
}
