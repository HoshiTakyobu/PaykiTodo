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
    Snooze,
    DdlPostpone
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
        InputSyntaxHelpTopic.DdlPostpone -> "DDL 推迟怎么填"
    }
}

internal fun inputSyntaxHelpLines(topic: InputSyntaxHelpTopic): List<String> {
    return when (topic) {
        InputSyntaxHelpTopic.Reminder -> listOf(
            "多个提醒用英文或中文逗号分隔。",
            "纯数字表示提前多少分钟。",
            "HH:mm、2:30 pm 或 下午 2:30 表示 DDL / 日程开始当天的具体时刻。",
            "明天 16:30、周五 16:30 表示相对日期或本周对应星期的具体时刻。",
            "MM-DD HH:mm、M.D HH:mm、M/D HH:mm、M月D日 HH:mm 表示当年的日期时间；YYYY-MM-DD 或 YYYY/MM/DD 表示完整日期。",
            "全角句点、全角波浪线、全角破折号等常见中文输入法符号会先归一化。",
            "如果习惯写 5.28,14:30 或 5月28日，14:30，也会按一条具体提醒时间识别。",
            "任一提醒晚于 DDL / 日程开始，或新建时已经过去，都会被判定为非法。"
        )
        InputSyntaxHelpTopic.TodoBatch -> listOf(
            "每行一条待办，字段顺序固定。",
            "格式是：DDL时间,任务名称,提醒时间。",
            "字段分隔用英文逗号；字段内部如果要连接日期和时间，推荐用空格或中文逗号。",
            "DDL 可写 HH:mm 表示今天，也可写 5.28、5/28、5月28日、明天、周五；只写日期默认 23:59。",
            "DDL 和提醒都支持中文冒号、中文 AM/PM、斜杠日期和常见全角分隔符。",
            "提醒时间可省略；默认使用默认分组、响铃和震动设置。",
            "待办批量添加为了好手输，一行只支持一个提醒时间。"
        )
        InputSyntaxHelpTopic.CalendarBatch -> listOf(
            "不写日期时默认今天；写了日期后，同一天后续日程可以省略日期。",
            "日期可写 今天、明天、5.28、5/28、5月28日、2026-05-28。",
            "条目之间可以用分号或换行分隔。",
            "Remind= / #remind 后面的提醒时间支持和编辑界面相同的写法。",
            "地点里的 @ 会被当作普通文字保存。"
        )
        InputSyntaxHelpTopic.Snooze -> listOf(
            "可以输入延后分钟数，也可以直接输入未来时刻。",
            "HH:mm 或 2:30 pm 表示今天的具体时刻。",
            "明天 16:30、周五 16:30 表示相对日期或本周对应星期的具体时刻。",
            "MM-DD HH:mm、M.D HH:mm、M/D HH:mm、M月D日 HH:mm 表示当年的日期时间；YYYY-MM-DD 或 YYYY/MM/DD 表示完整日期时间，日期和时间之间也可以用逗号。",
            "目标时间必须晚于当前时间；这里只改下一次提醒，不修改 DDL。"
        )
        InputSyntaxHelpTopic.DdlPostpone -> listOf(
            "纯数字、XX分钟、往后推XX分钟都表示在当前 DDL 基础上继续往后推。分钟数必须大于 0。",
            "HH:mm、2:30 pm 或 下午 2:30 表示当前 DDL 所在日期的目标时刻。",
            "MM-DD HH:mm、M.D HH:mm、M/D HH:mm、M月D日 HH:mm 表示当年的日期时间；YYYY-MM-DD 或 YYYY/MM/DD 表示完整日期时间。",
            "新的 DDL 必须严格晚于当前 DDL，否则不会允许确认。"
        )
    }
}

internal fun inputSyntaxHelpExample(topic: InputSyntaxHelpTopic): String {
    return when (topic) {
        InputSyntaxHelpTopic.Reminder -> "5,15,16:30,2:30 pm,下午 2:30,明天 16:30,周五 16:30,5/10,14:30"
        InputSyntaxHelpTopic.TodoBatch -> "明天 16:30,写报告,5\n周五 09:30,给老师发消息,09:00\n后天 23:59,整理保研材料,5\n无DDL,整理 Obsidian 待办"
        InputSyntaxHelpTopic.CalendarBatch -> "13:40-14:40, 学院立德树人优秀教师推荐学生座谈会, @MB-B1-403\n明天: 10:20-11:55, 辅导员助理值班, @MB-B1-412, Remind=5"
        InputSyntaxHelpTopic.Snooze -> "5\n16:30\n2:30 pm\n下午 2:30\n明天 16:30\n周五 16:30"
        InputSyntaxHelpTopic.DdlPostpone -> "30\n30分钟\n往后推45分钟\n16:30\n2026-05-22 16:30"
    }
}
