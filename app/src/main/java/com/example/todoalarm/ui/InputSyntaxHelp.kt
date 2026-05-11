package com.example.todoalarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal enum class InputSyntaxHelpTopic {
    Reminder,
    TodoBatch,
    CalendarBatch,
    Snooze
}

@Composable
internal fun InputSyntaxHelpIconButton(
    topic: InputSyntaxHelpTopic,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                contentDescription = inputSyntaxHelpTitle(topic),
                modifier = Modifier.padding(5.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
internal fun InputSyntaxHelpTitleRow(
    title: String,
    topic: InputSyntaxHelpTopic,
    onHelp: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        InputSyntaxHelpIconButton(topic = topic, onClick = onHelp)
    }
}

@Composable
internal fun InputSyntaxHelpDialog(
    topic: InputSyntaxHelpTopic,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(inputSyntaxHelpTitle(topic), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                inputSyntaxHelpLines(topic).forEach { line ->
                    Text(line, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                ) {
                    Text(
                        text = inputSyntaxHelpExample(topic),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } }
    )
}

internal fun inputSyntaxHelpTitle(topic: InputSyntaxHelpTopic): String {
    return when (topic) {
        InputSyntaxHelpTopic.Reminder -> "提醒时间怎么填"
        InputSyntaxHelpTopic.TodoBatch -> "待办批量添加怎么填"
        InputSyntaxHelpTopic.CalendarBatch -> "日程批量导入怎么填"
        InputSyntaxHelpTopic.Snooze -> "自定义延后怎么填"
    }
}

internal fun inputSyntaxHelpLines(topic: InputSyntaxHelpTopic): List<String> {
    return when (topic) {
        InputSyntaxHelpTopic.Reminder -> listOf(
            "多个提醒用英文逗号分隔。",
            "纯数字表示提前多少分钟。",
            "HH:mm 表示 DDL / 日程开始当天的具体时刻。",
            "MM-DD HH:mm 表示当年的日期时间；YYYY-MM-DD HH:mm 表示完整日期时间。",
            "任一提醒晚于 DDL / 日程开始，或新建时已经过去，都会被判定为非法。"
        )
        InputSyntaxHelpTopic.TodoBatch -> listOf(
            "每行一条待办，字段顺序固定。",
            "格式是：DDL时间,任务名称,提醒时间。",
            "DDL 可写 HH:mm，表示今天的对应时刻；中文冒号会自动按英文冒号处理。",
            "提醒时间可省略；默认使用默认分组、响铃和震动设置。",
            "待办批量添加为了好手输，一行只支持一个提醒时间。"
        )
        InputSyntaxHelpTopic.CalendarBatch -> listOf(
            "首条日程需要写日期，同一天后续日程可以省略日期。",
            "条目之间可以用分号或换行分隔。",
            "Remind= 后面的提醒时间支持和编辑界面相同的写法。",
            "地点里的 @ 会被当作普通文字保存。"
        )
        InputSyntaxHelpTopic.Snooze -> listOf(
            "可以输入延后分钟数，也可以直接输入未来时刻。",
            "HH:mm 表示今天的具体时刻。",
            "MM-DD HH:mm 表示当年的日期时间；YYYY-MM-DD HH:mm 表示完整日期时间。",
            "目标时间必须晚于当前时间，且最多延后 180 分钟。"
        )
    }
}

internal fun inputSyntaxHelpExample(topic: InputSyntaxHelpTopic): String {
    return when (topic) {
        InputSyntaxHelpTopic.Reminder -> "5,15,16:30,05-10 15:00,2026-05-10 14:30"
        InputSyntaxHelpTopic.TodoBatch -> "16:30,写报告,5\n05-13 09:30,给老师发消息,09:00\n无DDL,整理 Obsidian 待办"
        InputSyntaxHelpTopic.CalendarBatch -> "2026-04-27: 10:20-11:55, 辅导员助理值班, @MB-B1-412, Remind=5;\n12:30-14:00, 午休"
        InputSyntaxHelpTopic.Snooze -> "5\n16:30\n05-10 15:00"
    }
}
