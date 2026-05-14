package com.example.todoalarm.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
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
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    var editorValue by remember(activeNote?.id) { mutableStateOf(TextFieldValue(activeNote?.contentMarkdown.orEmpty())) }
    var parseResult by remember { mutableStateOf<PlanningParseResult?>(null) }
    var documentSheetVisible by remember { mutableStateOf(false) }
    var previewSheetVisible by remember { mutableStateOf(false) }
    var helpSheetVisible by remember { mutableStateOf(false) }
    var markdownEditMode by rememberSaveable(activeNote?.id) { mutableStateOf(true) }
    var renameDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var archiveDialog by remember { mutableStateOf(false) }
    var newDialog by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateMapOf<String, Boolean>() }
    val editableCandidates = remember { mutableStateListOf<PlanningImportCandidate>() }

    LaunchedEffect(activeNote?.id, activeNote?.contentMarkdown) {
        editorValue = TextFieldValue(activeNote?.contentMarkdown.orEmpty())
        parseResult = null
        selectedIds.clear()
        editableCandidates.clear()
        markdownEditMode = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            Icons.Rounded.Article,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp).size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeNote?.title ?: "我的规划",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "先把想法写下来，再识别成待办和日程。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { documentSheetVisible = true }) {
                        Icon(Icons.Rounded.Article, contentDescription = "文档")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(onClick = { newDialog = true }) { Text("新建") }
                    FilledTonalButton(onClick = { renameDialog = true }, enabled = activeNote != null) { Text("重命名") }
                    FilledTonalButton(onClick = { markdownEditMode = !markdownEditMode }) { Text(if (markdownEditMode) "预览" else "编辑") }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { helpSheetVisible = true }) { Icon(Icons.Rounded.Info, contentDescription = "规划台说明") }
                    IconButton(onClick = { archiveDialog = true }, enabled = activeNote != null) { Icon(Icons.Rounded.Archive, contentDescription = "归档") }
                    IconButton(onClick = { deleteDialog = true }, enabled = activeNote != null) { Icon(Icons.Rounded.Delete, contentDescription = "删除") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val noteId = activeNote?.id ?: return@FilledTonalButton
                            scope.launch {
                                val message = onSaveNote(noteId, editorValue.text)
                                Toast.makeText(context, message ?: "规划已保存", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = activeNote != null
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("保存")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val result = onParse(editorValue.text)
                            parseResult = result
                            selectedIds.clear()
                            editableCandidates.clear()
                            result.candidates.forEach { candidate ->
                                val editable = candidate.toPlanningImportCandidate()
                                editableCandidates += editable
                                selectedIds[editable.id] = editable.validate() == null
                            }
                            previewSheetVisible = true
                        }
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("识别")
                    }
                }
            }
        }

        if (markdownEditMode) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "规划正文",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    OutlinedTextField(
                        value = editorValue,
                        onValueChange = { editorValue = autoContinuePlanningLine(editorValue, it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        placeholder = { Text("例如：\n# 明天\n- [ ] 09:00-10:30 写论文 #group 课程\n- [ ] 整理材料 #ddl 5.28") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(22.dp),
                        minLines = 12
                    )
                }
            }

            PlanningShortcutBar(
                onAction = { action -> editorValue = applyPlanningShortcut(editorValue, action) },
                onHelp = { token, description -> Toast.makeText(context, "$token：$description", Toast.LENGTH_LONG).show() }
            )
        } else {
            PlanningMarkdownPreview(
                markdown = editorValue.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onToggleCheckbox = { lineNumber -> editorValue = editorValue.copy(text = togglePlanningCheckbox(editorValue.text, lineNumber)) },
                onRequestEdit = { markdownEditMode = true }
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
                    onSelectNote(it)
                    documentSheetVisible = false
                }
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
                            result.updatedMarkdown?.let { editorValue = TextFieldValue(it) }
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
    if (deleteDialog && activeNote != null) {
        ConfirmPlanningDialog(
            title = "删除规划文档？",
            message = "删除后无法恢复：${activeNote.title}",
            confirmText = "删除",
            onDismiss = { deleteDialog = false },
            onConfirm = {
                scope.launch {
                    val message = onDeleteNote(activeNote.id)
                    Toast.makeText(context, message ?: "文档已删除", Toast.LENGTH_SHORT).show()
                    deleteDialog = false
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
        PlanningShortcutSpec("任务", PlanningShortcutAction.Insert("- [ ] "), "插入一条待办任务"),
        PlanningShortcutSpec("子任务", PlanningShortcutAction.Insert("  - [ ] "), "插入缩进后的子任务"),
        PlanningShortcutSpec("缩进", PlanningShortcutAction.Indent, "当前行增加一级缩进"),
        PlanningShortcutSpec("减少缩进", PlanningShortcutAction.Outdent, "当前行减少一级缩进"),
        PlanningShortcutSpec("DDL", PlanningShortcutAction.Insert(" #ddl "), "设置截止时间，例如 #ddl 5.28 23:59"),
        PlanningShortcutSpec("日程", PlanningShortcutAction.Insert(" #schedule "), "显式声明日程时间段"),
        PlanningShortcutSpec("提醒", PlanningShortcutAction.Insert(" #remind "), "设置提醒，例如 #remind 5,15"),
        PlanningShortcutSpec("分组", PlanningShortcutAction.Insert(" #group "), "指定分组，例如 #group 课程"),
        PlanningShortcutSpec("今天", PlanningShortcutAction.Insert("今天"), "插入今天"),
        PlanningShortcutSpec("明天", PlanningShortcutAction.Insert("明天"), "插入明天")
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
        onClick = { onAction(spec.action) },
        label = { Text(spec.label, maxLines = 1) }
    )
}

@Composable
private fun PlanningDocumentSheet(
    notes: List<PlanningNote>,
    activeNoteId: Long?,
    onSelect: (Long) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleNotes = remember(notes, query) {
        val keyword = query.trim()
        if (keyword.isBlank()) notes else notes.filter { note ->
            note.title.contains(keyword, ignoreCase = true) || note.contentMarkdown.contains(keyword, ignoreCase = true)
        }
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
            modifier = Modifier.fillMaxWidth().height(420.dp),
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
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(note.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("更新于 ${planningMillisLabel(note.updatedAtMillis)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        # 明天
        - [ ] 10:00-12:30 写课程论文 #group 课程
        - [ ] 整理保研材料 #ddl 5.28 23:59 #remind 5,15
          - [ ] 打印成绩单
          - [ ] 整理获奖证明
        明天 19:30-21:00 复习操作系统 #group 学习
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
            modifier = Modifier.fillMaxWidth().height(560.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PlanningHelpCard(
                    title = "1. 先像备忘录一样写",
                    lines = listOf(
                        "不用一开始就填完整表单，先把近期要做的事情写下来。",
                        "一行一个任务最容易识别；大标题可以写 # 明天、# 本周、# 保研材料。",
                        "输入法上方的快捷栏可以快速插入任务、子任务、DDL、提醒和分组。"
                    )
                )
            }
            item {
                PlanningHelpCard(
                    title = "2. 常用写法",
                    lines = listOf(
                        "待办：- [ ] 整理材料 #ddl 5.28 23:59",
                        "子任务：在任务下一行点“子任务”，或手写两个空格再写 - [ ]。",
                        "日程：10:00-12:30 写论文，或 明天 19:30-21:00 复习。",
                        "分组：在行尾写 #group 课程。",
                        "提醒：在行尾写 #remind 5,15，表示提前 5 分钟和 15 分钟。"
                    )
                )
            }
            item {
                PlanningHelpCard(
                    title = "3. 识别和导入",
                    lines = listOf(
                        "写完后点右侧的“识别”。",
                        "识别预览里可以先修改标题、DDL、开始结束时间、分组、备注和提醒。",
                        "勾选需要导入的条目，再点“导入”。",
                        "导入成功后，原文会追加 #imported，避免同一行重复导入。"
                    )
                )
            }
            item {
                PlanningHelpExampleCard(example)
            }
            item {
                PlanningHelpCard(
                    title = "4. 当前限制",
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
    onRequestEdit: () -> Unit
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
                TextButton(onClick = onRequestEdit) { Text("编辑原文") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            if (parsedLines.isFailure) {
                PlanningPreviewFallback(onRequestEdit = onRequestEdit)
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
                        is PlanningRenderedLine.Task -> PlanningMarkdownTaskLine(line, onToggleCheckbox)
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
    onToggleCheckbox: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = (line.indentLevel * 18).dp),
        shape = RoundedCornerShape(18.dp),
        color = if (line.imported) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
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
    onRequestEdit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
        onClick = onRequestEdit
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
                Text("${candidates.count { it.validate() == null }} / ${result.candidates.size} 条可导入", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onImport, enabled = !invalidSelected) { Text("导入") }
        }
        LazyColumn(modifier = Modifier.fillMaxWidth().height(520.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        onChange = { onCandidateChange(candidate.copy(dueAt = it)) }
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        PlanningDateTimeField(
                            label = "开始",
                            value = candidate.startAt,
                            modifier = Modifier.weight(1f),
                            onChange = { onCandidateChange(candidate.copy(startAt = it)) }
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
                    value = candidate.reminderOffsetsMinutes.joinToString(","),
                    onValueChange = { raw -> onCandidateChange(candidate.copy(reminderOffsetsMinutes = parsePlanningReminderOffsets(raw))) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("提醒分钟") },
                    placeholder = { Text("例如 5,15") },
                    singleLine = true
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
    data object Indent : PlanningShortcutAction
    data object Outdent : PlanningShortcutAction
}

private fun applyPlanningShortcut(value: TextFieldValue, action: PlanningShortcutAction): TextFieldValue {
    return when (action) {
        is PlanningShortcutAction.Insert -> insertPlanningToken(value, action.token)
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
    if (new.text.length != old.text.length + 1 || !new.text.endsWith('\n')) return new
    val previousLine = old.text.substringBeforeLast('\n', old.text).substringAfterLast('\n')
    val match = Regex("^(\\s*)- \\[ \\]\\s*(.*)$").matchEntire(previousLine) ?: return new
    if (match.groupValues[2].isBlank()) return new
    val suffix = match.groupValues[1] + "- [ ] "
    return new.copy(
        text = new.text + suffix,
        selection = androidx.compose.ui.text.TextRange(new.text.length + suffix.length)
    )
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
    val text = raw.trim().replace('：', ':').replace('T', ' ')
    if (text.isBlank()) return null
    runCatching { LocalDateTime.parse(text) }.getOrNull()?.let { return it }
    listOf("yyyy-MM-dd HH:mm", "yyyy-M-d H:mm", "yyyy.MM.dd HH:mm", "yyyy.M.d H:mm").forEach { pattern ->
        runCatching { LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern, Locale.CHINA)) }.getOrNull()?.let { return it }
    }
    val monthDayMatch = Regex("^(\\d{1,2})[-.](\\d{1,2})\\s+(\\d{1,2}):(\\d{2})$").matchEntire(text) ?: return null
    val month = monthDayMatch.groupValues[1].toIntOrNull() ?: return null
    val day = monthDayMatch.groupValues[2].toIntOrNull() ?: return null
    val hour = monthDayMatch.groupValues[3].toIntOrNull() ?: return null
    val minute = monthDayMatch.groupValues[4].toIntOrNull() ?: return null
    return runCatching { LocalDateTime.of(LocalDate.now().year, month, day, hour, minute) }.getOrNull()
}

private fun parsePlanningReminderOffsets(raw: String): List<Int> {
    return raw.split(',', '，')
        .mapNotNull { it.trim().toIntOrNull() }
        .map { it.coerceAtLeast(0) }
        .distinct()
        .sortedDescending()
}

private fun planningMillisLabel(value: Long): String {
    val dateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(value), java.time.ZoneId.systemDefault())
    return planningDateTimeLabel(dateTime)
}
