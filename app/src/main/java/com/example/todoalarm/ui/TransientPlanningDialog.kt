package com.example.todoalarm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.todoalarm.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

internal data class PlanningPreviewDraft(val original: TransientPlanningCandidate,
    val title: String = original.title, val location: String = original.location, val notes: String = original.notes,
    val time: String = original.timeExpression, val reminders: String = original.reminderExpression,
    val range: String = original.rangeExpression, val parentId: String? = original.parentId,
    val groupId: Long? = original.groupId, val calendarId: String = original.calendarId
) {
    fun candidate(now: LocalDateTime): Pair<TransientPlanningCandidate?, String?> {
        val fields = listOf(time, reminders, range).filter(String::isNotBlank)
        val result = TransientPlanningParser.parse("预览事项" + fields.joinToString(prefix = if (fields.isEmpty()) "" else ",", separator = ",") + ";", now)
        val error = result.diagnostics.firstOrNull()?.message
        if (error != null) return null to error
        val parsed = result.candidates.single().copy(id = original.id, parentId = parentId,
            lineNumber = original.lineNumber, column = original.column, sourceLine = original.sourceLine,
            title = title, location = location, notes = notes, groupId = groupId, calendarId = calendarId)
        return parsed to parsed.validate()
    }
}

internal object TransientPlanningEditing {
    fun insertLinePrefix(before: TextFieldValue, after: TextFieldValue): TextFieldValue {
        val insertion = after.selection.start - 1
        if (after.text.length != before.text.length + 1 || insertion !in after.text.indices || after.text[insertion] != '\n' ||
            after.text.removeRange(insertion, insertion + 1) != before.text) return after
        val previousLine = before.text.substring(0, insertion).substringAfterLast('\n')
        val prefix = TransientPlanningParser.nextLinePrefix(previousLine)
        return after.copy(text = after.text.substring(0, insertion + 1) + prefix + after.text.substring(insertion + 1),
            selection = TextRange(insertion + 1 + prefix.length))
    }
    fun decreaseIndent(value: TextFieldValue): TextFieldValue {
        val start = if (value.selection.start == 0) 0 else value.text.lastIndexOf('\n', value.selection.start - 1).let { it + 1 }
        val count = value.text.substring(start).takeWhile { it == ' ' || it == '\t' }.let { if (it.startsWith('\t')) 1 else minOf(2, it.length) }
        if (count == 0) return value
        return value.copy(text = value.text.removeRange(start, start + count),
            selection = TextRange((value.selection.start - count).coerceAtLeast(start)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransientPlanningDialog(
    groups: List<TaskGroup>, calendars: List<PlannerCalendar>, aiAvailable: Boolean, accountGeneration: Long,
    onRecognize: suspend (String, Boolean, LocalDateTime) -> TransientPlanningResult,
    onImport: suspend (List<TransientPlanningCandidate>, String, Long) -> String?, onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var preview by remember { mutableStateOf<List<PlanningPreviewDraft>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var result by remember { mutableStateOf<TransientPlanningResult?>(null) }
    var previewVisible by remember { mutableStateOf(false) }
    var useAi by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val sessionId = remember { UUID.randomUUID().toString() }
    val account = remember { accountGeneration }
    val now = remember { LocalDateTime.now() }
    val scope = rememberCoroutineScope()
    val built = preview.map { it.candidate(now) }
    val selectedBuilt = preview.indices.filter { preview[it].original.id in selected }.map { built[it] }

    fun recognize() {
        busy = true; error = null
        scope.launch {
            try {
                val recognized = onRecognize(input.text, useAi, now)
                result = recognized
                preview = recognized.candidates.map(::PlanningPreviewDraft)
                selected = recognized.candidates.map { it.id }.toSet()
                previewVisible = true
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (exception: Exception) { error = exception.message ?: "识别失败，原文已保留" }
            finally { busy = false }
        }
    }
    Dialog(onDismissRequest = { if (!importing) onDismiss() }, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(modifier = Modifier.imePadding(), topBar = {
                TopAppBar(title = { Text(if (previewVisible) "确认规划" else "输入规划") }, navigationIcon = {
                    TextButton(enabled = !importing, onClick = onDismiss) { Text("取消") }
                }, actions = {
                    Button(enabled = !busy && if (previewVisible) selectedBuilt.isNotEmpty() &&
                        selectedBuilt.all { it.first != null && it.second == null } && result?.hasErrors != true else input.text.isNotBlank(),
                        onClick = {
                            if (!previewVisible) recognize()
                            else {
                                busy = true; importing = true; error = null
                                scope.launch {
                                    try {
                                        val imported = onImport(selectedBuilt.map { it.first!! }, sessionId, account)
                                        if (imported == null) { input = TextFieldValue(); onDismiss() } else error = imported
                                    } catch (cancelled: CancellationException) { throw cancelled }
                                    catch (exception: Exception) { error = exception.message ?: "导入失败，预览已保留" }
                                    finally { busy = false; importing = false }
                                }
                            }
                        }) { Text(if (busy) "处理中" else if (previewVisible) "确认导入" else "识别") }
                    Spacer(Modifier.width(12.dp))
                })
            }) { padding ->
                Column(Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()
                    .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (!previewVisible) {
                        Text("标题必填；时间、地点和提醒可省略。无日期按今天，不受看板所选日期影响。")
                        OutlinedTextField(value = input, onValueChange = { input = TransientPlanningEditing.insertLinePrefix(input, it) },
                            enabled = !busy, label = { Text("规划内容") }, placeholder = { Text("- 买水果, @超市, 16:30;\n- 阅读, 20:00>20:30, (); #先读第二章") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp).onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.Tab && event.isShiftPressed) {
                                    input = TransientPlanningEditing.decreaseIndent(input); true
                                } else false
                            })
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(enabled = !busy, onClick = { input = TransientPlanningEditing.decreaseIndent(input) }) { Text("减少缩进") }
                            TextButton(enabled = !busy, onClick = { input = TextFieldValue("- 准备出行\n  - 买车票, 18:00;\n  - 收拾行李;\n- 高等数学, @教学楼201, 每周[周一 08:00>09:40, 周三 14:00>15:40], (10,0);") }) { Text("填入示例") }
                        }
                        Row { Checkbox(checked = useAi, onCheckedChange = { useAi = it }, enabled = aiAvailable && !busy); Text("使用已配置的AI识别", Modifier.padding(top = 12.dp)) }
                        Text("不勾选即可离线使用。省略提醒默认提前5分钟和到点；()关闭提醒。标题没有时间时不安排默认相对提醒。", style = MaterialTheme.typography.bodySmall)
                        Text("Enter：行末无分号进入子级，有分号保持本级。Shift+Tab或减少缩进回上一级。日期与时间可用空格或+，日程起止用>。", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("确认前不会创建事项；可在下面直接修改。取消会关闭本次草稿。")
                        result?.message?.takeIf(String::isNotBlank)?.let { Text(it) }
                        result?.diagnostics?.forEach { diagnostic -> Text("第${diagnostic.lineNumber}行，第${diagnostic.column}列：${diagnostic.message}", color = MaterialTheme.colorScheme.error) }
                        TextButton(enabled = !busy, onClick = { previewVisible = false; error = null }) { Text("返回原文修改") }
                        preview.forEachIndexed { index, draft ->
                            fun update(next: PlanningPreviewDraft) { preview = preview.toMutableList().also { it[index] = next } }
                            HorizontalDivider()
                            Row { Checkbox(checked = draft.original.id in selected, onCheckedChange = { checked -> selected = if (checked) selected + draft.original.id else selected - draft.original.id }, enabled = !busy)
                                Text("第${draft.original.lineNumber}行 · ${if (built[index].first?.type == PlanningParsedType.EVENT) "日程" else "待办"}", Modifier.padding(top = 12.dp)) }
                            OutlinedTextField(draft.title, { update(draft.copy(title = it)) }, label = { Text("标题") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(draft.time, { update(draft.copy(time = it)) }, label = { Text("DDL / 起止 / 循环时间") }, supportingText = { Text("单时间点为待办，>时段或循环表达式为日程；清空成为无DDL待办") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(draft.location, { update(draft.copy(location = it)) }, label = { Text("地点") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(draft.reminders, { update(draft.copy(reminders = it)) }, label = { Text("提醒：空白默认(5,0)，()关闭") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(draft.range, { update(draft.copy(range = it)) }, label = { Text("可选生效范围：自日期至日期 / 截至日期") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(draft.notes, { update(draft.copy(notes = it)) }, label = { Text("备注") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                            PlanningPreviewChoice("父事项", draft.parentId, listOf(null to "无父事项") + preview.filter { it.original.id != draft.original.id }.map { it.original.id to it.title }, !busy) { update(draft.copy(parentId = it)) }
                            PlanningPreviewChoice("分组", draft.groupId, listOf(null to "默认分组") + groups.map { it.id to it.name }, !busy) { update(draft.copy(groupId = it)) }
                            PlanningPreviewChoice("日历", draft.calendarId, calendars.filter { it.shareMode != "SHARED" }.map { it.id to it.name }, !busy) { update(draft.copy(calendarId = it)) }
                            built[index].second?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            if (built[index].first?.slots?.any { it.recurrence.isRecurring } == true) Text("多个时段共用课程字段；未写截止即持续循环。预览只显示首次时段，后续滚动生成。月末和闰日按实际月份末日处理。", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable private fun <T> PlanningPreviewChoice(label: String, value: T, options: List<Pair<T, String>>, enabled: Boolean, onChange: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box { TextButton(enabled = enabled, onClick = { expanded = true }) { Text("$label：${options.firstOrNull { it.first == value }?.second ?: "请选择"}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { options.forEach { option ->
            DropdownMenuItem(text = { Text(option.second) }, onClick = { onChange(option.first); expanded = false })
        } }
    }
}
