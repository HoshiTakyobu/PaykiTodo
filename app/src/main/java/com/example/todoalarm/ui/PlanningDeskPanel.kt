package com.example.todoalarm.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.PlanningNote
import com.example.todoalarm.data.PlanningImportCandidate
import com.example.todoalarm.data.PlanningImportResult
import com.example.todoalarm.data.PlanningParseResult
import com.example.todoalarm.data.PlanningParsedType
import com.example.todoalarm.data.toPlanningImportCandidate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanningDeskPanel(
    notes: List<PlanningNote>,
    activeNote: PlanningNote?,
    onSelectNote: (Long) -> Unit,
    onCreateNote: suspend (String) -> String?,
    onSaveNote: suspend (Long, String) -> String?,
    onRenameNote: suspend (Long, String) -> String?,
    onDeleteNote: suspend (Long) -> String?,
    onArchiveNote: suspend (Long) -> String?,
    onParse: (String) -> PlanningParseResult,
    onImport: suspend (List<PlanningImportCandidate>, Set<String>, String, Long?) -> PlanningImportResult
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var editorValue by remember(activeNote?.id) { mutableStateOf(TextFieldValue(activeNote?.contentMarkdown.orEmpty())) }
    var parseResult by remember { mutableStateOf<PlanningParseResult?>(null) }
    var documentSheetVisible by remember { mutableStateOf(false) }
    var previewSheetVisible by remember { mutableStateOf(false) }
    var helpSheetVisible by remember { mutableStateOf(false) }
    var markdownEditMode by rememberSaveable(activeNote?.id) { mutableStateOf(true) }
    var renameDialog by remember { mutableStateOf(false) }
    var pendingDeleteNote by remember { mutableStateOf<PlanningNote?>(null) }
    var archiveDialog by remember { mutableStateOf(false) }
    var newDialog by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateMapOf<String, Boolean>() }
    val editableCandidates = remember { mutableStateListOf<PlanningImportCandidate>() }
    val hasUnsavedChanges = activeNote != null && editorValue.text != activeNote.contentMarkdown

    LaunchedEffect(activeNote?.id) {
        editorValue = TextFieldValue(activeNote?.contentMarkdown.orEmpty())
        parseResult = null
        selectedIds.clear()
        editableCandidates.clear()
        markdownEditMode = true
    }

    LaunchedEffect(activeNote?.id, editorValue.text, activeNote?.contentMarkdown) {
        val note = activeNote ?: return@LaunchedEffect
        if (editorValue.text == note.contentMarkdown) return@LaunchedEffect
        delay(2000)
        onSaveNote(note.id, editorValue.text)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasUnsavedChanges) {
                    Text(
                        text = "自动保存中...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                OutlinedButton(
                    modifier = Modifier.height(40.dp),
                    onClick = {
                        focusManager.clearFocus()
                        markdownEditMode = !markdownEditMode
                        Toast.makeText(context, if (markdownEditMode) "已切换到编辑模式" else "已切换到预览模式", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (markdownEditMode) "预览" else "编辑")
                }
                Button(
                    modifier = Modifier.height(40.dp),
                    onClick = {
                        focusManager.clearFocus()
                        val result = onParse(editorValue.text)
                        parseResult = result
                        selectedIds.clear()
                        editableCandidates.clear()
                        result.candidates.forEach { candidate ->
                            val editable = candidate.toPlanningImportCandidate()
                            editableCandidates += editable
                            selectedIds[editable.id] = editable.validate() == null
                        }
                        Toast.makeText(context, "识别完成：${result.importableCount} 条可导入", Toast.LENGTH_SHORT).show()
                        previewSheetVisible = true
                    }
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("识别")
                }
                IconButton(
                    modifier = Modifier.size(40.dp),
                    onClick = {
                        focusManager.clearFocus()
                        Toast.makeText(context, "打开规划文档列表", Toast.LENGTH_SHORT).show()
                        documentSheetVisible = true
                    }
                ) {
                    Icon(Icons.Rounded.Article, contentDescription = "文档列表")
                }
                androidx.compose.foundation.layout.Box {
                    IconButton(
                        modifier = Modifier.size(40.dp),
                        onClick = {
                            focusManager.clearFocus()
                            overflowMenuExpanded = true
                        }
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "更多操作")
                    }
                    DropdownMenu(
                        expanded = overflowMenuExpanded,
                        onDismissRequest = { overflowMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("新建文档") },
                            onClick = { overflowMenuExpanded = false; newDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("重命名") },
                            onClick = { overflowMenuExpanded = false; renameDialog = true },
                            enabled = activeNote != null
                        )
                        DropdownMenuItem(
                            text = { Text("使用说明") },
                            onClick = { overflowMenuExpanded = false; helpSheetVisible = true }
                        )
                        DropdownMenuItem(
                            text = { Text("归档") },
                            onClick = { overflowMenuExpanded = false; archiveDialog = true },
                            enabled = activeNote != null
                        )
                        DropdownMenuItem(
                            text = { Text("删除文档") },
                            onClick = { overflowMenuExpanded = false; activeNote?.let { pendingDeleteNote = it } },
                            enabled = activeNote != null
                        )
                    }
                }
            }
        }

        if (markdownEditMode) {
            PlanningShortcutBar(
                onAction = { action -> editorValue = applyPlanningShortcut(editorValue, action) },
                onHelp = { token, description -> Toast.makeText(context, "$token：$description", Toast.LENGTH_LONG).show() }
            )

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                OutlinedTextField(
                    value = editorValue,
                    onValueChange = { editorValue = autoContinuePlanningLine(editorValue, it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    placeholder = {
                        Text(
                            "# 收集箱\n" +
                                "- [ ] 先把脑子里的事情写下来\n\n" +
                                "# 明天\n" +
                                "09:00-10:30 写论文 #group 课程 #remind 5\n" +
                                "- [ ] 整理材料 #ddl 5.28 23:59 #remind 30,5"
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(18.dp),
                    minLines = 12
                )
            }
        } else {
            PlanningMarkdownPreview(
                markdown = editorValue.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onToggleCheckbox = { lineNumber ->
                    val nextText = togglePlanningCheckbox(editorValue.text, lineNumber)
                    editorValue = TextFieldValue(
                        text = nextText,
                        selection = androidx.compose.ui.text.TextRange(planningLineStartOffset(nextText, lineNumber))
                    )
                },
                onRequestEdit = { lineNumber ->
                    lineNumber?.let {
                        editorValue = editorValue.copy(selection = androidx.compose.ui.text.TextRange(planningLineStartOffset(editorValue.text, it)))
                    }
                    markdownEditMode = true
                }
            )
        }
    }

    if (documentSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { documentSheetVisible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            PlanningDocumentSheet(
                notes = notes,
                activeNoteId = activeNote?.id,
                onSelect = {
                    scope.launch {
                        activeNote?.takeIf { hasUnsavedChanges }?.let { note ->
                            onSaveNote(note.id, editorValue.text)
                        }
                        onSelectNote(it)
                        documentSheetVisible = false
                    }
                },
                onDelete = { pendingDeleteNote = it }
            )
        }
    }

    if (helpSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { helpSheetVisible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            PlanningHelpSheet(onDismiss = { helpSheetVisible = false })
        }
    }

    if (previewSheetVisible && parseResult != null) {
        ModalBottomSheet(
            onDismissRequest = { previewSheetVisible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            PlanningPreviewSheet(
                result = requireNotNull(parseResult),
                candidates = editableCandidates,
                selectedIds = selectedIds,
                onCandidateChange = { changed ->
                    val index = editableCandidates.indexOfFirst { it.id == changed.id }
                    if (index >= 0) editableCandidates[index] = changed
                },
                onImport = {
                    scope.launch {
                        val result = onImport(
                            editableCandidates.toList(),
                            selectedIds.filterValues { it }.keys,
                            editorValue.text,
                            activeNote?.id
                        )
                        Toast.makeText(context, result.message ?: "已导入 ${result.importedCount} 条规划", Toast.LENGTH_SHORT).show()
                        if (result.message == null) {
                            result.updatedMarkdown?.let { updated ->
                                editorValue = TextFieldValue(updated)
                                activeNote?.id?.let { noteId -> onSaveNote(noteId, updated) }
                            }
                            parseResult = null
                            editableCandidates.clear()
                            selectedIds.clear()
                            previewSheetVisible = false
                        }
                    }
                }
            )
        }
    }

    if (newDialog) {
        PlanningNameDialog(
            title = "新建规划文档",
            initial = "新的规划",
            confirmText = "新建",
            onDismiss = { newDialog = false },
            onConfirm = { title ->
                scope.launch {
                    val message = onCreateNote(title)
                    Toast.makeText(context, message ?: "已新建规划文档", Toast.LENGTH_SHORT).show()
                    newDialog = false
                }
            }
        )
    }
    if (renameDialog && activeNote != null) {
        PlanningNameDialog(
            title = "重命名规划文档",
            initial = activeNote.title,
            confirmText = "保存",
            onDismiss = { renameDialog = false },
            onConfirm = { title ->
                scope.launch {
                    val message = onRenameNote(activeNote.id, title)
                    Toast.makeText(context, message ?: "文档已重命名", Toast.LENGTH_SHORT).show()
                    renameDialog = false
                }
            }
        )
    }
    pendingDeleteNote?.let { note ->
        ConfirmPlanningDialog(
            title = "删除规划文档？",
            message = "删除后无法恢复：${note.title}",
            confirmText = "删除",
            onDismiss = { pendingDeleteNote = null },
            onConfirm = {
                scope.launch {
                    val message = onDeleteNote(note.id)
                    Toast.makeText(context, message ?: "文档已删除", Toast.LENGTH_SHORT).show()
                    pendingDeleteNote = null
                }
            }
        )
    }
    if (archiveDialog && activeNote != null) {
        ConfirmPlanningDialog(
            title = "归档规划文档？",
            message = "归档后会从当前列表隐藏：${activeNote.title}",
            confirmText = "归档",
            onDismiss = { archiveDialog = false },
            onConfirm = {
                scope.launch {
                    val message = onArchiveNote(activeNote.id)
                    Toast.makeText(context, message ?: "文档已归档", Toast.LENGTH_SHORT).show()
                    archiveDialog = false
                }
            }
        )
    }

}

@Composable
private fun PlanningShortcutBar(
    onAction: (PlanningShortcutAction) -> Unit,
    onHelp: (String, String) -> Unit
) {
    val chips = listOf(
        PlanningShortcutSpec("任务", PlanningShortcutAction.TaskLine, "把当前行变成一条待办；同一行不会重复插入 - [ ]"),
        PlanningShortcutSpec("子任务", PlanningShortcutAction.SubtaskLine, "在当前行下面新建一条缩进子任务"),
        PlanningShortcutSpec("缩进", PlanningShortcutAction.Indent, "当前行增加一级缩进"),
        PlanningShortcutSpec("减少缩进", PlanningShortcutAction.Outdent, "当前行减少一级缩进"),
        PlanningShortcutSpec("DDL", PlanningShortcutAction.Insert(" #ddl "), "设置截止时间，例如 #ddl 5.28 23:59 或 #ddl 明天 16:30"),
        PlanningShortcutSpec("日程", PlanningShortcutAction.Insert(" #schedule "), "显式声明日程时间段"),
        PlanningShortcutSpec("提醒", PlanningShortcutAction.Insert(" #remind "), "设置提醒，例如 #remind 5,15,16:30,明天 16:30"),
        PlanningShortcutSpec("分组", PlanningShortcutAction.Insert(" #group "), "指定分组，例如 #group 课程"),
        PlanningShortcutSpec("今日标题", PlanningShortcutAction.InsertSection("# 今日计划"), "插入标题分区；下面没写日期的时间段按今天理解"),
        PlanningShortcutSpec("明日标题", PlanningShortcutAction.InsertSection("# 明天"), "插入标题分区；下面没写日期的时间段按明天理解")
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "快捷",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            chips.forEach { spec ->
                PlanningShortcutChip(spec, onAction, onHelp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlanningShortcutChip(
    spec: PlanningShortcutSpec,
    onAction: (PlanningShortcutAction) -> Unit,
    onHelp: (String, String) -> Unit
) {
    AssistChip(
        modifier = Modifier.combinedClickable(onClick = { onAction(spec.action) }, onLongClick = { onHelp(spec.label, spec.help) }),
        onClick = {},
        label = { Text(spec.label, maxLines = 1) }
    )
}

@Composable
private fun PlanningDocumentSheet(
    notes: List<PlanningNote>,
    activeNoteId: Long?,
    onSelect: (Long) -> Unit,
    onDelete: (PlanningNote) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleNotes = remember(notes, query) {
        val keyword = query.trim()
        val filtered = if (keyword.isBlank()) notes else notes.filter { note ->
            note.title.contains(keyword, ignoreCase = true) || note.contentMarkdown.contains(keyword, ignoreCase = true)
        }
        filtered.sortedByDescending { it.updatedAtMillis }
    }
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("规划文档", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索文档") },
            singleLine = true
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.62f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(visibleNotes, key = { it.id }) { note ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = if (note.id == activeNoteId) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (note.id == activeNoteId) 2.dp else 1.dp,
                    onClick = { onSelect(note.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(note.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("更新于 ${planningMillisLabel(note.updatedAtMillis)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDelete(note) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "删除文档", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            if (visibleNotes.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Text(
                            "没有匹配的规划文档",
                            modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PlanningHelpSheet(onDismiss: () -> Unit) {
    val example = """
        # 收集箱
        - [ ] 想一下保研材料还缺什么
        - [ ] 问老师课程论文格式

        # 今日计划
        - [ ] 10:00-11:30 写课程论文 #group 课程 #remind 5
        - [ ] 晚上复习操作系统 #ddl 23:59 #remind 30,5

        # 明天
        - [ ] 09:00-10:30 背英语单词 #group 学习
        - [ ] 完成实验报告 #ddl 21:30 #remind 15,5
        - [ ] 整理材料 #ddl 5.28，23:59 #remind 5月28日，22:00
        - [ ] 5/28 下午 2:30～下午 4:00 小组讨论
    """.trimIndent()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(9.dp).size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("规划台怎么用", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("先自由写计划，再识别为待办或日程。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onDismiss) { Text("知道了") }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.72f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PlanningHelpCard(
                    title = "1. 先像备忘录一样写",
                    lines = listOf(
                        "不用一开始就填完整表单，先把近期要做的事情写下来。",
                        "一行一个任务最容易识别；大标题不是必须语法，只是帮你把文档分区。",
                        "正文会在停止输入约 2 秒后自动保存，切换规划文档前也会先保存当前内容。",
                        "输入法上方的快捷栏可以快速插入任务、子任务、DDL、提醒和分组。"
                    )
                )
            }
            item {
                PlanningHelpCard(
                    title = "2. 标题是用来分区的",
                    lines = listOf(
                        "# 收集箱：只表示“先收进来”，不会自动给日期或 DDL。",
                        "# 今日计划 / # 今天：表示下面没写日期的 10:00-11:30 会按今天解析。",
                        "# 明天：表示下面没写日期的时间段会按明天解析。",
                        "# 本周计划：只是普通分区；没有具体日期时，重要任务仍建议写 #ddl。"
                    )
                )
            }
            item {
                PlanningHelpCard(
                    title = "3. 常用写法",
                    lines = listOf(
                        "待办：- [ ] 整理材料 #ddl 5.28 23:59",
                        "DDL：支持 16:30、5.28、5/28、5月28日、明天、周五；只写日期默认 23:59。",
                        "子任务：把光标放在父任务行，点“子任务”，会自动新起一行并缩进。",
                        "日程：10:00-12:30 写论文、复习 14:00-16:00，或 5/28 下午 2:30～下午 4:00 小组讨论。",
                        "分组：在行尾写 #group 课程。",
                        "提醒：#remind 5,15 表示提前分钟；也支持 #remind 16:30、#remind 下午 2:30、#remind 明天 16:30、#remind 周五 16:30、#remind 5月28日，14:30。",
                        "#today、#tomorrow、#important、#project 暂不写入正式待办属性，会保留在标题里。"
                    )
                )
            }
            item {
                PlanningHelpCard(
                    title = "4. 识别和导入",
                    lines = listOf(
                        "写完后点右侧的“识别”。",
                        "识别预览里可以先修改标题、DDL、开始结束时间、分组、备注和提醒；时间字段继续支持自然日期写法。",
                        "可以点“全选可导入项”或“全不选”快速控制候选；没有选中有效候选时不能导入。",
                        "勾选需要导入的条目，再点“导入”。",
                        "导入成功后，原文会追加 #imported 并立即保存，避免同一行重复导入。"
                    )
                )
            }
            item {
                PlanningHelpExampleCard(example)
            }
            item {
                PlanningHelpCard(
                    title = "5. 当前限制",
                    lines = listOf(
                        "手机端当前是稳定原文编辑，不是富文本 Markdown 渲染。",
                        "AI 拆解、拖拽排期、甘特图还没有接入。",
                        "如果识别不准，优先在预览页修正，再导入正式待办和日程。"
                    )
                )
            }
        }
    }
}

@Composable
private fun PlanningHelpCard(title: String, lines: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            lines.forEach { line ->
                Text("• $line", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlanningHelpExampleCard(example: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("可以直接照这个格式写", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                example,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PlanningMarkdownPreview(
    markdown: String,
    modifier: Modifier,
    onToggleCheckbox: (Int) -> Unit,
    onRequestEdit: (Int?) -> Unit
) {
    val parsedLines = remember(markdown) { runCatching { parsePlanningMarkdownLines(markdown) } }
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Markdown 预览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { onRequestEdit(null) }) { Text("编辑原文") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            if (parsedLines.isFailure) {
                PlanningPreviewFallback(onRequestEdit = { onRequestEdit(null) })
                return@Column
            }
            val lines = parsedLines.getOrDefault(emptyList())
            if (lines.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                ) {
                    Text(
                        "还没有内容，点击“编辑原文”开始写规划。",
                        modifier = Modifier.padding(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(lines, key = { it.lineNumber }) { line ->
                    when (line) {
                        is PlanningRenderedLine.Heading -> PlanningMarkdownHeading(line)
                        is PlanningRenderedLine.Task -> PlanningMarkdownTaskLine(line, onToggleCheckbox, onRequestEdit)
                        is PlanningRenderedLine.Text -> PlanningMarkdownTextLine(line, onRequestEdit)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanningPreviewFallback(onRequestEdit: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Markdown 预览失败", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            Text("已保护为可编辑模式，原文不会丢失。", color = MaterialTheme.colorScheme.onErrorContainer)
            OutlinedButton(onClick = onRequestEdit) { Text("编辑原文") }
        }
    }
}

@Composable
private fun PlanningMarkdownHeading(line: PlanningRenderedLine.Heading) {
    val style = when (line.level) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.titleLarge
        3 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.bodyLarge
    }
    Text(
        text = line.text,
        style = style,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = if (line.level <= 2) 8.dp else 4.dp)
    )
}

@Composable
private fun PlanningMarkdownTaskLine(
    line: PlanningRenderedLine.Task,
    onToggleCheckbox: (Int) -> Unit,
    onRequestEdit: (Int?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = (line.indentLevel * 18).dp),
        shape = RoundedCornerShape(18.dp),
        color = if (line.imported) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        onClick = { onRequestEdit(line.lineNumber) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = line.checked,
                onCheckedChange = { onToggleCheckbox(line.lineNumber) },
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = line.text.ifBlank { "未命名任务" },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (line.checked) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (line.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                PlanningMarkdownPills(tags = line.tags, imported = line.imported)
            }
        }
    }
}

@Composable
private fun PlanningMarkdownTextLine(
    line: PlanningRenderedLine.Text,
    onRequestEdit: (Int?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
        onClick = { onRequestEdit(line.lineNumber) }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(line.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            PlanningMarkdownPills(tags = line.tags, imported = line.imported)
        }
    }
}

@Composable
private fun PlanningMarkdownPills(tags: List<String>, imported: Boolean) {
    if (tags.isEmpty() && !imported) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        tags.forEach { tag -> PlanningMarkdownTagPill(tag) }
        if (imported) PlanningMarkdownStatePill("已导入")
    }
}

@Composable
private fun PlanningMarkdownTagPill(tag: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        tonalElevation = 1.dp
    ) {
        Text(
            text = "#$tag",
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun PlanningMarkdownStatePill(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

@Composable
private fun PlanningPreviewSheet(
    result: PlanningParseResult,
    candidates: List<PlanningImportCandidate>,
    selectedIds: MutableMap<String, Boolean>,
    onCandidateChange: (PlanningImportCandidate) -> Unit,
    onImport: () -> Unit
) {
    val invalidSelected = candidates.any { candidate -> selectedIds[candidate.id] == true && candidate.validate() != null }
    val selectedCount = candidates.count { selectedIds[it.id] == true }
    val validCandidates = candidates.filter { it.validate() == null }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(9.dp).size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("识别预览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${validCandidates.size} / ${result.candidates.size} 条可导入，已选 $selectedCount 条", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onImport, enabled = selectedCount > 0 && !invalidSelected) { Text("导入") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    validCandidates.forEach { selectedIds[it.id] = true }
                },
                enabled = validCandidates.isNotEmpty()
            ) { Text("全选可导入项") }
            TextButton(onClick = { candidates.forEach { selectedIds[it.id] = false } }, enabled = selectedCount > 0) { Text("全不选") }
        }
        LazyColumn(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.72f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(candidates, key = { it.id }) { candidate ->
                PlanningCandidateCard(
                    candidate = candidate,
                    selected = selectedIds[candidate.id] == true,
                    onSelectedChange = { selectedIds[candidate.id] = it },
                    onCandidateChange = onCandidateChange
                )
            }
        }
    }
}

@Composable
private fun PlanningCandidateCard(
    candidate: PlanningImportCandidate,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onCandidateChange: (PlanningImportCandidate) -> Unit
) {
    val validation = candidate.validate()
    val canSelect = validation == null
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selected,
                    onClick = { if (canSelect) onSelectedChange(!selected) },
                    enabled = canSelect,
                    label = { Text(if (canSelect) "导入" else "需修正") }
                )
                Text(candidate.type.label(), fontWeight = FontWeight.Bold, color = candidate.type.color())
                Text("第 ${candidate.lineNumber} 行", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
            ) {
                Text(
                    candidate.sourceLine,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (candidate.type == PlanningParsedType.TODO || candidate.type == PlanningParsedType.EVENT) {
                OutlinedTextField(
                    value = candidate.title,
                    onValueChange = { onCandidateChange(candidate.copy(title = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("标题") },
                    singleLine = true
                )
                if (candidate.type == PlanningParsedType.TODO) {
                    PlanningDateTimeField(
                        label = "DDL",
                        value = candidate.dueAt,
                        onChange = { onCandidateChange(candidate.copy(dueAt = it).revalidatePlanningReminderInput()) }
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        PlanningDateTimeField(
                            label = "开始",
                            value = candidate.startAt,
                            modifier = Modifier.weight(1f),
                            onChange = { onCandidateChange(candidate.copy(startAt = it).revalidatePlanningReminderInput()) }
                        )
                        PlanningDateTimeField(
                            label = "结束",
                            value = candidate.endAt,
                            modifier = Modifier.weight(1f),
                            onChange = { onCandidateChange(candidate.copy(endAt = it)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = candidate.groupName,
                    onValueChange = { onCandidateChange(candidate.copy(groupName = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("分组") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = candidate.notes,
                    onValueChange = { onCandidateChange(candidate.copy(notes = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注") },
                    minLines = 1,
                    maxLines = 3
                )
                OutlinedTextField(
                    value = candidate.reminderInputText.ifBlank { candidate.reminderOffsetsMinutes.joinToString(",") },
                    onValueChange = { raw -> onCandidateChange(candidate.withPlanningReminderInput(raw)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("提醒") },
                    placeholder = { Text("5,15,16:30,05-10 15:00") },
                    singleLine = true,
                    isError = candidate.reminderInputError.isNotBlank(),
                    supportingText = {
                        Text(
                            candidate.reminderInputError.ifBlank { "支持提前分钟、当天时刻、日期时间；用逗号分隔多项。" },
                            color = if (candidate.reminderInputError.isNotBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Text("提醒默认全屏 · 响铃 + 震动", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (candidate.type == PlanningParsedType.EVENT) {
                FilterChip(
                    selected = candidate.createLinkedTodo,
                    onClick = { onCandidateChange(candidate.copy(createLinkedTodo = !candidate.createLinkedTodo)) },
                    label = { Text("同时创建待办，DDL = 日程结束时间") }
                )
            }
            if (validation != null) Text(validation, color = MaterialTheme.colorScheme.error)
            if (validation == null && candidate.message.isNotBlank()) Text(candidate.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlanningDateTimeField(
    label: String,
    value: LocalDateTime?,
    modifier: Modifier = Modifier,
    onChange: (LocalDateTime?) -> Unit
) {
    var text by remember(value) { mutableStateOf(value?.let(::planningEditableDateTime).orEmpty()) }
    val parsed = remember(text) { parsePlanningEditableDateTime(text) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(parsePlanningEditableDateTime(it))
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text("05-28 14:30") },
        singleLine = true,
        isError = text.isNotBlank() && parsed == null
    )
}

@Composable
private fun PlanningNameDialog(
    title: String,
    initial: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by rememberSaveable(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("文档名称") }) },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ConfirmPlanningDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private data class PlanningShortcutSpec(
    val label: String,
    val action: PlanningShortcutAction,
    val help: String
)

private sealed interface PlanningShortcutAction {
    data class Insert(val token: String) : PlanningShortcutAction
    data class InsertSection(val heading: String) : PlanningShortcutAction
    data object TaskLine : PlanningShortcutAction
    data object SubtaskLine : PlanningShortcutAction
    data object Indent : PlanningShortcutAction
    data object Outdent : PlanningShortcutAction
}

private fun applyPlanningShortcut(value: TextFieldValue, action: PlanningShortcutAction): TextFieldValue {
    return when (action) {
        is PlanningShortcutAction.Insert -> insertPlanningToken(value, action.token)
        is PlanningShortcutAction.InsertSection -> insertPlanningSection(value, action.heading)
        PlanningShortcutAction.TaskLine -> applyPlanningTaskLine(value)
        PlanningShortcutAction.SubtaskLine -> insertPlanningSubtaskLine(value)
        PlanningShortcutAction.Indent -> indentCurrentPlanningLine(value)
        PlanningShortcutAction.Outdent -> outdentCurrentPlanningLine(value)
    }
}

private fun insertPlanningToken(value: TextFieldValue, token: String): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val nextText = value.text.replaceRange(start, end, token)
    val cursor = start + token.length
    return value.copy(text = nextText, selection = androidx.compose.ui.text.TextRange(cursor))
}

private fun insertPlanningSection(value: TextFieldValue, heading: String): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val before = value.text.substring(0, start)
    val after = value.text.substring(end)
    val prefix = when {
        before.isBlank() -> ""
        before.endsWith("\n\n") -> ""
        before.endsWith("\n") -> "\n"
        else -> "\n\n"
    }
    val suffix = if (after.startsWith("\n")) "" else "\n"
    val token = "$prefix$heading\n$suffix"
    val nextText = before + token + after
    val cursor = before.length + token.length
    return value.copy(text = nextText, selection = androidx.compose.ui.text.TextRange(cursor))
}

private fun applyPlanningTaskLine(value: TextFieldValue): TextFieldValue {
    val (lineStart, lineEnd) = currentPlanningLineBounds(value.text, value.selection.min)
    val line = value.text.substring(lineStart, lineEnd)
    if (TaskPreviewRegex.matches(line)) return value
    val indent = line.takeWhile { it == ' ' || it == '\t' }
    val content = line.trimStart()
    val nextLine = if (content.isBlank()) "${indent}- [ ] " else "${indent}- [ ] $content"
    val nextText = value.text.replaceRange(lineStart, lineEnd, nextLine)
    val cursor = lineStart + nextLine.length
    return value.copy(text = nextText, selection = androidx.compose.ui.text.TextRange(cursor))
}

private fun insertPlanningSubtaskLine(value: TextFieldValue): TextFieldValue {
    val (lineStart, lineEnd) = currentPlanningLineBounds(value.text, value.selection.min)
    val line = value.text.substring(lineStart, lineEnd)
    val parentIndent = line.takeWhile { it == ' ' || it == '\t' }.replace("\t", "  ")
    val subtask = parentIndent + "  - [ ] "
    val insertPoint: Int
    val token: String
    if (lineEnd < value.text.length && value.text[lineEnd] == '\n') {
        insertPoint = lineEnd + 1
        token = subtask
    } else {
        insertPoint = lineEnd
        token = "\n$subtask"
    }
    val nextText = value.text.replaceRange(insertPoint, insertPoint, token)
    val cursor = insertPoint + token.length
    return value.copy(text = nextText, selection = androidx.compose.ui.text.TextRange(cursor))
}

private fun currentPlanningLineBounds(text: String, cursor: Int): Pair<Int, Int> {
    val safeCursor = cursor.coerceIn(0, text.length)
    val lineStart = if (safeCursor == 0) 0 else text.lastIndexOf('\n', safeCursor - 1).let { if (it < 0) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', safeCursor).let { if (it < 0) text.length else it }
    return lineStart to lineEnd
}

private fun indentCurrentPlanningLine(value: TextFieldValue): TextFieldValue {
    val lineStart = value.text.lastIndexOf('\n', (value.selection.min - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
    val nextText = value.text.replaceRange(lineStart, lineStart, "  ")
    return value.copy(text = nextText, selection = androidx.compose.ui.text.TextRange(value.selection.min + 2, value.selection.max + 2))
}

private fun outdentCurrentPlanningLine(value: TextFieldValue): TextFieldValue {
    val lineStart = value.text.lastIndexOf('\n', (value.selection.min - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
    val removeCount = when {
        value.text.substring(lineStart).startsWith("  ") -> 2
        value.text.substring(lineStart).startsWith("\t") -> 1
        else -> 0
    }
    if (removeCount == 0) return value
    val nextText = value.text.removeRange(lineStart, lineStart + removeCount)
    return value.copy(
        text = nextText,
        selection = androidx.compose.ui.text.TextRange(
            (value.selection.min - removeCount).coerceAtLeast(lineStart),
            (value.selection.max - removeCount).coerceAtLeast(lineStart)
        )
    )
}

private fun autoContinuePlanningLine(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    if (new.text.length != old.text.length + 1 || old.selection.min != old.selection.max) return new
    val insertIndex = old.selection.min.coerceIn(0, old.text.length)
    if (new.text.getOrNull(insertIndex) != '\n') return new
    if (new.text.removeRange(insertIndex, insertIndex + 1) != old.text) return new
    val previousLine = old.text.substring(0, insertIndex).substringAfterLast('\n')
    val match = Regex("^(\\s*)- \\[ \\]\\s*(.*)$").matchEntire(previousLine) ?: return new
    if (match.groupValues[2].isBlank()) return new
    val suffix = match.groupValues[1] + "- [ ] "
    val insertAfterNewline = insertIndex + 1
    val nextText = new.text.replaceRange(insertAfterNewline, insertAfterNewline, suffix)
    return new.copy(
        text = nextText,
        selection = androidx.compose.ui.text.TextRange(insertAfterNewline + suffix.length)
    )
}

private fun planningLineStartOffset(markdown: String, lineNumber: Int): Int {
    if (lineNumber <= 1) return 0
    var offset = 0
    var currentLine = 1
    while (currentLine < lineNumber && offset < markdown.length) {
        val nextBreak = markdown.indexOf('\n', offset)
        if (nextBreak < 0) return markdown.length
        offset = nextBreak + 1
        currentLine += 1
    }
    return offset.coerceIn(0, markdown.length)
}

private sealed interface PlanningRenderedLine {
    val lineNumber: Int

    data class Heading(
        override val lineNumber: Int,
        val level: Int,
        val text: String
    ) : PlanningRenderedLine

    data class Task(
        override val lineNumber: Int,
        val indentLevel: Int,
        val checked: Boolean,
        val text: String,
        val tags: List<String>,
        val imported: Boolean
    ) : PlanningRenderedLine

    data class Text(
        override val lineNumber: Int,
        val text: String,
        val tags: List<String>,
        val imported: Boolean
    ) : PlanningRenderedLine
}

private fun parsePlanningMarkdownLines(markdown: String): List<PlanningRenderedLine> {
    return markdown.lineSequence().mapIndexedNotNull { index, rawLine ->
        val lineNumber = index + 1
        val trimmed = rawLine.trim()
        if (trimmed.isBlank()) return@mapIndexedNotNull null

        HeadingPreviewRegex.matchEntire(trimmed)?.let { match ->
            return@mapIndexedNotNull PlanningRenderedLine.Heading(
                lineNumber = lineNumber,
                level = match.groupValues[1].length.coerceIn(1, 6),
                text = match.groupValues[2].trim()
            )
        }

        TaskPreviewRegex.matchEntire(rawLine)?.let { match ->
            val content = match.groupValues[3].trim()
            val tags = planningTags(content)
            return@mapIndexedNotNull PlanningRenderedLine.Task(
                lineNumber = lineNumber,
                indentLevel = match.groupValues[1].replace("\t", "  ").length / 2,
                checked = match.groupValues[2].equals("x", ignoreCase = true),
                text = stripPlanningInlineTags(content),
                tags = tags.filterNot { it == "imported" },
                imported = tags.any { it == "imported" }
            )
        }

        val tags = planningTags(trimmed)
        PlanningRenderedLine.Text(
            lineNumber = lineNumber,
            text = stripPlanningInlineTags(trimmed),
            tags = tags.filterNot { it == "imported" },
            imported = tags.any { it == "imported" }
        )
    }.toList()
}

private fun togglePlanningCheckbox(markdown: String, lineNumber: Int): String {
    if (lineNumber <= 0) return markdown
    val hasTrailingNewline = markdown.endsWith("\n") || markdown.endsWith("\r")
    val normalized = markdown.replace("\r\n", "\n").replace('\r', '\n')
    val updated = normalized.lines().mapIndexed { index, line ->
        if (index + 1 != lineNumber) return@mapIndexed line
        TaskPreviewRegex.matchEntire(line)?.let { match ->
            val next = if (match.groupValues[2].equals("x", ignoreCase = true)) " " else "x"
            return@mapIndexed match.groupValues[1] + "- [" + next + "] " + match.groupValues[3]
        }
        line
    }.joinToString("\n")
    return if (hasTrailingNewline && !updated.endsWith("\n")) "$updated\n" else updated
}

private fun planningTags(content: String): List<String> {
    return TagPreviewRegex.findAll(content)
        .map { it.groupValues[1] }
        .distinct()
        .toList()
}

private fun stripPlanningInlineTags(content: String): String {
    return content
        .replace(TagPreviewRegex, " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private val HeadingPreviewRegex = Regex("^(#{1,6})\\s+(.+)$")
private val TaskPreviewRegex = Regex("^(\\s*)-\\s*\\[([ xX])\\]\\s*(.*)$")
private val TagPreviewRegex = Regex("(?:^|\\s)#([\\p{L}\\p{N}_-]+)(?=\\s|$)")

private fun PlanningParsedType.label(): String = when (this) {
    PlanningParsedType.TODO -> "待办"
    PlanningParsedType.EVENT -> "日程"
    PlanningParsedType.SKIPPED -> "跳过"
    PlanningParsedType.ERROR -> "错误"
}

@Composable
private fun PlanningParsedType.color() = when (this) {
    PlanningParsedType.TODO -> MaterialTheme.colorScheme.primary
    PlanningParsedType.EVENT -> MaterialTheme.colorScheme.tertiary
    PlanningParsedType.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
    PlanningParsedType.ERROR -> MaterialTheme.colorScheme.error
}

private fun planningDateTimeLabel(value: LocalDateTime?): String {
    if (value == null) return "未设置"
    return value.format(DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA))
}

private fun planningEditableDateTime(value: LocalDateTime): String {
    return value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA))
}

private fun parsePlanningEditableDateTime(raw: String): LocalDateTime? {
    val text = normalizePlanningDateTimeText(raw).trim().replace('T', ' ')
    if (text.isBlank()) return null
    runCatching { LocalDateTime.parse(text) }.getOrNull()?.let { return it }
    listOf(
        "yyyy-MM-dd HH:mm",
        "yyyy-M-d H:mm",
        "yyyy.MM.dd HH:mm",
        "yyyy.M.d H:mm",
        "yyyy/MM/dd HH:mm",
        "yyyy/M/d H:mm",
        "yyyy年M月d日 H:mm"
    ).forEach { pattern ->
        runCatching { LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern, Locale.CHINA)) }.getOrNull()?.let { return it }
    }

    val today = LocalDate.now()
    parsePlanningDateExpression(text, today)?.let { date ->
        return LocalDateTime.of(date, java.time.LocalTime.of(23, 59))
    }

    val leadingDate = parsePlanningLeadingDate(text, today)
    val date = leadingDate?.date ?: today
    val timeText = (leadingDate?.rest ?: text).trim().trimStart(',')
    val time = parsePlanningTimeToken(timeText.trim()) ?: return null
    return LocalDateTime.of(date, time)
}

private data class PlanningLeadingDate(val date: LocalDate, val rest: String)

private fun normalizePlanningDateTimeText(raw: String): String {
    return raw
        .replace('：', ':')
        .replace('，', ',')
        .replace('．', '.')
        .replace('。', '.')
        .replace('／', '/')
}

private fun parsePlanningLeadingDate(text: String, today: LocalDate): PlanningLeadingDate? {
    val match = PlanningLeadingDateRegex.find(text)?.takeIf { it.range.first == 0 } ?: return null
    val rest = text.substring(match.range.last + 1)
    val boundary = rest.firstOrNull()
    if (boundary != null && !boundary.isWhitespace() && boundary != ',' && !boundary.isDigit()) return null
    val date = parsePlanningDateExpression(match.value.trim(), today) ?: return null
    return PlanningLeadingDate(date, rest)
}

private fun parsePlanningDateExpression(raw: String, today: LocalDate): LocalDate? {
    val text = normalizePlanningDateTimeText(raw).trim()
    return when (text) {
        "今天", "今日" -> today
        "明天", "明日" -> today.plusDays(1)
        "后天" -> today.plusDays(2)
        else -> parsePlanningWeekday(text, today)
            ?: parsePlanningNumericDate(text, today)
            ?: parsePlanningChineseDate(text, today)
    }
}

private fun parsePlanningNumericDate(text: String, today: LocalDate): LocalDate? {
    Regex("^(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})$").matchEntire(text)?.let { match ->
        return safePlanningDate(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
    }
    Regex("^(\\d{1,2})[-./](\\d{1,2})$").matchEntire(text)?.let { match ->
        return safePlanningDate(today.year, match.groupValues[1].toInt(), match.groupValues[2].toInt())
    }
    return null
}

private fun parsePlanningChineseDate(text: String, today: LocalDate): LocalDate? {
    Regex("^(\\d{4})年(\\d{1,2})月(\\d{1,2})日?$").matchEntire(text)?.let { match ->
        return safePlanningDate(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
    }
    Regex("^(\\d{1,2})月(\\d{1,2})日?$").matchEntire(text)?.let { match ->
        return safePlanningDate(today.year, match.groupValues[1].toInt(), match.groupValues[2].toInt())
    }
    return null
}

private fun parsePlanningWeekday(text: String, today: LocalDate): LocalDate? {
    val weekday = when (text) {
        "周一", "星期一", "礼拜一" -> DayOfWeek.MONDAY
        "周二", "星期二", "礼拜二" -> DayOfWeek.TUESDAY
        "周三", "星期三", "礼拜三" -> DayOfWeek.WEDNESDAY
        "周四", "星期四", "礼拜四" -> DayOfWeek.THURSDAY
        "周五", "星期五", "礼拜五" -> DayOfWeek.FRIDAY
        "周六", "星期六", "礼拜六" -> DayOfWeek.SATURDAY
        "周日", "周天", "星期日", "星期天", "礼拜日", "礼拜天" -> DayOfWeek.SUNDAY
        else -> return null
    }
    val delta = (weekday.value - today.dayOfWeek.value + 7) % 7
    return today.plusDays(delta.toLong())
}

private fun parsePlanningTimeToken(raw: String): java.time.LocalTime? {
    val match = Regex("^(凌晨|早上|上午|中午|下午|晚上)?\\s*(\\d{1,2}):(\\d{2})\\s*([aApP][mM])?$").matchEntire(raw) ?: return null
    val chinesePeriod = match.groupValues[1]
    var hour = match.groupValues[2].toIntOrNull() ?: return null
    val minute = match.groupValues[3].toIntOrNull() ?: return null
    val period = match.groupValues[4].lowercase(Locale.ROOT)
    if (minute !in 0..59) return null
    if (period.isNotBlank()) {
        if (hour !in 1..12) return null
        hour = when {
            period == "pm" && hour < 12 -> hour + 12
            period == "am" && hour == 12 -> 0
            else -> hour
        }
    } else if (chinesePeriod.isNotBlank()) {
        hour = when (chinesePeriod) {
            "凌晨" -> if (hour == 12) 0 else hour
            "早上", "上午" -> if (hour == 12) 0 else hour
            "中午" -> if (hour in 1..10) hour + 12 else hour
            "下午", "晚上" -> if (hour < 12) hour + 12 else hour
            else -> return null
        }
    }
    if (hour !in 0..23) return null
    return java.time.LocalTime.of(hour, minute)
}

private fun safePlanningDate(year: Int, month: Int, day: Int): LocalDate? {
    return runCatching { LocalDate.of(year, month, day) }.getOrNull()
}

private val PlanningLeadingDateRegex = Regex("^(今天|今日|明天|明日|后天|周[一二三四五六日天]|星期[一二三四五六日天]|礼拜[一二三四五六日天]|\\d{4}[-./]\\d{1,2}[-./]\\d{1,2}|\\d{4}年\\d{1,2}月\\d{1,2}日?|\\d{1,2}[-./]\\d{1,2}|\\d{1,2}月\\d{1,2}日?)")

private fun PlanningImportCandidate.withPlanningReminderInput(raw: String): PlanningImportCandidate {
    val text = raw.trim()
    if (text.isBlank()) {
        return copy(reminderInputText = "", reminderInputError = "", reminderOffsetsMinutes = emptyList())
    }
    val anchor = when (type) {
        PlanningParsedType.TODO -> dueAt
        PlanningParsedType.EVENT -> startAt
        else -> null
    } ?: return copy(reminderInputText = raw, reminderInputError = "请先设置 DDL / 日程开始时间。")

    val parsed = parseReminderInput(raw = raw, anchor = anchor, requireFuture = false)
    return if (parsed.isValid) {
        copy(
            reminderInputText = raw,
            reminderInputError = "",
            reminderOffsetsMinutes = parsed.offsetsMinutes
        )
    } else {
        copy(reminderInputText = raw, reminderInputError = parsed.message)
    }
}

private fun PlanningImportCandidate.revalidatePlanningReminderInput(): PlanningImportCandidate {
    return if (reminderInputText.isBlank()) this else withPlanningReminderInput(reminderInputText)
}

private fun planningMillisLabel(value: Long): String {
    val dateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(value), java.time.ZoneId.systemDefault())
    return planningDateTimeLabel(dateTime)
}
