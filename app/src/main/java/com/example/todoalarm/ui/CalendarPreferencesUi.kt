package com.example.todoalarm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.AllDayReminderRule
import com.example.todoalarm.data.decodeAllDayReminderRules
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun encodeCalendarAllDayRules(rules: List<AllDayReminderRule>): String = JSONArray().apply {
    rules.distinct().forEach { put(JSONObject().put("daysBefore", it.daysBefore).put("minuteOfDay", it.minuteOfDay)) }
}.toString()

internal fun calendarLegacyAllDayRules(offsets: List<Int>): String = encodeCalendarAllDayRules(offsets.map { offset ->
    val minutes = 540 - offset
    AllDayReminderRule(-Math.floorDiv(minutes, 1440), Math.floorMod(minutes, 1440))
})

@Composable
internal fun AllDayReminderRulesEditor(json: String, onChange: (String) -> Unit) {
    val rules = remember(json) { runCatching { decodeAllDayReminderRules(json) }.getOrDefault(emptyList()) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (rules.isEmpty()) Text("不设置全天提醒", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        rules.forEachIndexed { index, rule ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(modifier = Modifier.weight(1f), onClick = { editingIndex = index }) {
                    Text("${if (rule.daysBefore == 0) "当天" else "提前${rule.daysBefore}天"} ${"%02d:%02d".format(rule.minuteOfDay / 60, rule.minuteOfDay % 60)}", modifier = Modifier.fillMaxWidth())
                }
                IconButton(onClick = { onChange(encodeCalendarAllDayRules(rules.filterIndexed { at, _ -> at != index })) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "移除全天提醒")
                }
            }
        }
        TextButton(enabled = rules.size < 24, onClick = { editingIndex = rules.size }) { Text("添加全天提醒") }
    }
    editingIndex?.let { index ->
        val original = rules.getOrNull(index) ?: AllDayReminderRule(0, 480)
        var days by remember(index) { mutableStateOf(original.daysBefore.toString()) }
        var clock by remember(index) { mutableStateOf("%02d:%02d".format(original.minuteOfDay / 60, original.minuteOfDay % 60)) }
        val parsedClock = runCatching { LocalTime.parse(clock, DateTimeFormatter.ofPattern("H:mm")) }.getOrNull()
        val valid = days.toIntOrNull() in 0..3650 && parsedClock != null
        AlertDialog(onDismissRequest = { editingIndex = null }, title = { Text("全天提醒") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(days, { days = it.filter(Char::isDigit).take(4) }, label = { Text("提前天数（0为当天）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(clock, { clock = it }, label = { Text("提醒钟点") }, placeholder = { Text("08:00") }, singleLine = true, modifier = Modifier.fillMaxWidth(), isError = parsedClock == null)
            }
        }, confirmButton = { TextButton(enabled = valid, onClick = {
            val changed = rules.toMutableList()
            val rule = AllDayReminderRule(days.toInt(), parsedClock!!.hour * 60 + parsedClock.minute)
            if (index < changed.size) changed[index] = rule else changed.add(rule)
            onChange(encodeCalendarAllDayRules(changed)); editingIndex = null
        }) { Text("完成") } }, dismissButton = { TextButton(onClick = { editingIndex = null }) { Text("取消") } })
    }
}

@Composable
internal fun CalendarTimeZoneChoice(title: String, value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        Text(value.ifBlank { if (title == "辅助时区") "不显示" else "跟随设备" }, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (open) {
        var search by remember { mutableStateOf("") }
        val zones = remember(search) { ZoneId.getAvailableZoneIds().sorted().filter { it.contains(search.trim(), ignoreCase = true) } }
        AlertDialog(onDismissRequest = { open = false }, title = { Text(title) }, text = {
            Column {
                OutlinedTextField(search, { search = it }, placeholder = { Text("搜索城市或时区，如 Shanghai") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    item { Text(if (title == "辅助时区") "不显示辅助时区" else "跟随设备 · ${ZoneId.systemDefault().id}", Modifier.fillMaxWidth().clickable { onChange(""); open = false }.padding(vertical = 14.dp)) }
                    items(zones, key = { it }) { zone ->
                        Text(zone, Modifier.fillMaxWidth().clickable { onChange(zone); open = false }.padding(vertical = 14.dp), color = if (zone == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { open = false }) { Text("取消") } })
    }
}

@Composable
internal fun CalendarDefaultsButton() {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text("日历设置") }
    if (open) CalendarPreferencesDialog { open = false }
}

@Composable
internal fun CalendarPreferencesDialog(onDismiss: () -> Unit) {
    val app = LocalContext.current.applicationContext as TodoApplication
    val settings = LocalWorkspacePreferences.current.settings
    var duration by remember { mutableStateOf(settings.calendarDefaultDurationMinutes.toString()) }
    var timed by remember { mutableStateOf(settings.calendarTimedDefaultRemindersCsv) }
    var allDay by remember { mutableStateOf(settings.calendarAllDayDefaultRemindersJson) }
    var displayZone by remember { mutableStateOf(settings.calendarDisplayTimeZoneId) }
    var auxiliaryZone by remember { mutableStateOf(settings.calendarAuxiliaryTimeZoneId) }
    val timedValid = timed.isBlank() || timed.replace('，', ',').split(',').all { it.trim().toIntOrNull() in 0..525600 }
    SettingsSectionDialog("日历默认值与时区", onDismiss, onDone = {
        app.settingsStore.updateCalendarPreferences(duration.toInt(), timed.replace('，', ','), allDay, displayZone, auxiliaryZone)
        app.remoteSyncCoordinator.onLocalDataChanged(); onDismiss()
    }, doneEnabled = duration.toIntOrNull() in 1..1440 && timedValid, onCancel = onDismiss) {
        Text("新建日程", style = MaterialTheme.typography.titleMedium)
        PreferenceChoice("默认时长", duration, listOf(15, 30, 45, 60, 90, 120).map { it.toString() to "$it 分钟" }) { duration = it }
        OutlinedTextField(timed, { timed = it }, label = { Text("普通提醒：提前分钟") }, placeholder = { Text("15,5,0；留空表示不提醒") }, singleLine = true, isError = !timedValid, modifier = Modifier.fillMaxWidth())
        Text("全天默认提醒", style = MaterialTheme.typography.titleSmall)
        AllDayReminderRulesEditor(allDay) { allDay = it }
        Text("默认值只用于新草稿，已有日程的提醒保持不变。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        CalendarTimeZoneChoice("日历显示时区", displayZone) { displayZone = it }
        CalendarTimeZoneChoice("辅助时区", auxiliaryZone) { auxiliaryZone = it }
        Text("新日程使用这里的时区。切换设置不会改动已有日程保存的时区或时间。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
