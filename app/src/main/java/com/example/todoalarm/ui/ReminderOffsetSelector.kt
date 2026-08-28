package com.example.todoalarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal data class ReminderOffsetOption(
    val minutes: Int,
    val label: String
)

internal val StandardReminderOffsetOptions = listOf(
    ReminderOffsetOption(5, "提前 5 分钟"),
    ReminderOffsetOption(15, "提前 15 分钟"),
    ReminderOffsetOption(30, "提前 30 分钟"),
    ReminderOffsetOption(60, "提前 1 小时"),
    ReminderOffsetOption(120, "提前 2 小时"),
    ReminderOffsetOption(24 * 60, "提前 1 天"),
    ReminderOffsetOption(48 * 60, "提前 2 天")
)

private val StandardReminderOffsetValues = StandardReminderOffsetOptions.map { it.minutes }.toSet()

@Composable
internal fun ReminderOffsetSelector(
    label: String,
    rawInput: String,
    validation: ReminderInputValidation,
    onRawInputChange: (String) -> Unit,
    emptySummary: String = "未选择提醒时间"
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var customExpanded by remember { mutableStateOf(false) }
    val selectedOffsets = validation.offsetsMinutes.toSet()
    val hasCustomOffsets = validation.isValid && selectedOffsets.any { it !in StandardReminderOffsetValues }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val menuWidth = (screenWidth - 32.dp).coerceAtMost(420.dp)
    val summary = when {
        rawInput.isBlank() -> emptySummary
        validation.isValid -> validation.message
        else -> "提醒时间输入有误"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { menuExpanded = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(label, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = summary,
                            color = if (validation.isValid || rawInput.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "展开提醒时间")
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier
                    .width(menuWidth)
                    .heightIn(max = 460.dp)
            ) {
                StandardReminderOffsetOptions.forEach { option ->
                    val selected = option.minutes in selectedOffsets
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        leadingIcon = {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            val next = if (selected) {
                                selectedOffsets - option.minutes
                            } else {
                                selectedOffsets + option.minutes
                            }
                            onRawInputChange(next.sortedDescending().joinToString(","))
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(if (hasCustomOffsets) "编辑自定义提醒时间" else "自定义提醒时间") },
                    onClick = {
                        menuExpanded = false
                        customExpanded = true
                    }
                )
            }
        }

        if (customExpanded || hasCustomOffsets || !validation.isValid) {
            OutlinedTextField(
                value = rawInput,
                onValueChange = onRawInputChange,
                label = { Text("自定义提醒时间") },
                placeholder = { Text("例：20，或 16:30，或 2026-09-01 08:00") },
                supportingText = {
                    Text("可输入提前分钟或具体时间；多个值使用逗号分隔。")
                },
                isError = rawInput.isNotBlank() && !validation.isValid,
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 3
            )
            if (rawInput.isNotBlank() && !validation.isValid) {
                Text(
                    text = validation.message,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
