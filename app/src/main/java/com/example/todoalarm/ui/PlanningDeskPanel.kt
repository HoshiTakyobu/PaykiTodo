package com.example.todoalarm.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FormatIndentDecrease
import androidx.compose.material.icons.rounded.FormatIndentIncrease
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Today
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.PlanningNote
import com.example.todoalarm.data.PlanningAnnouncementParser
import com.example.todoalarm.data.PlanningImportCandidate
import com.example.todoalarm.data.PlanningImportResult
import com.example.todoalarm.data.PlanningLineMapping
import com.example.todoalarm.data.MappingStatus
import com.example.todoalarm.data.PlanningOperationResult
import com.example.todoalarm.data.PlanningParseResult
import com.example.todoalarm.data.PlanningParsedType
import com.example.todoalarm.data.PlanningPostponeScope
import com.example.todoalarm.data.PlanningRefreshScope
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.RecurrenceType
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
    onParse: suspend (String) -> PlanningParseResult,
    onImport: suspend (List<PlanningImportCandidate>, Set<String>, String, Long?) -> PlanningImportResult,
    onSyncMappings: suspend (Long, String) -> List<PlanningLineMapping>,
    onGetMappings: suspend (Long) -> List<PlanningLineMapping>,
    onRefreshImportedItems: suspend (Long, String, PlanningRefreshScope, Int?) -> PlanningOperationResult,
    onPostponeImportedItems: suspend (Long, String, Long?, Int, PlanningPostponeScope) -> PlanningOperationResult,
    onUndoLastOperation: suspend (Long, String) -> PlanningOperationResult,
    onApplyConflictDocument: suspend (Long, String, Long) -> PlanningOperationResult,
    onApplyConflictItem: suspend (Long, String, Long) -> PlanningOperationResult,
    isNewUser: Boolean
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
    var shortcutBarExpanded by rememberSaveable { mutableStateOf(isNewUser) }
    var parsing by remember { mutableStateOf(false) }
    var mappingStates by remember(activeNote?.id) { mutableStateOf<List<PlanningLineMapping>>(emptyList()) }
    var refreshConfirmVisible by remember { mutableStateOf(false) }
    var postponeSheetVisible by remember { mutableStateOf(false) }
    var undoConfirmVisible by remember { mutableStateOf(false) }
    var operationRunning by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateMapOf<String, Boolean>() }
    val editableCandidates = remember { mutableStateListOf<PlanningImportCandidate>() }
    val hasUnsavedChanges = activeNote != null && editorValue.text != activeNote.contentMarkdown
    val latestUndoSummary = remember(mappingStates) { latestPlanningUndoSummary(mappingStates) }

    LaunchedEffect(activeNote?.id) {
        editorValue = TextFieldValue(activeNote?.contentMarkdown.orEmpty())
        parseResult = null
        selectedIds.clear()
        editableCandidates.clear()
        markdownEditMode = true
    }

    LaunchedEffect(activeNote?.id) {
        val note = activeNote ?: return@LaunchedEffect
        mappingStates = onSyncMappings(note.id, editorValue.text)
    }

    LaunchedEffect(activeNote?.id, editorValue.text, activeNote?.contentMarkdown) {
        val note = activeNote ?: return@LaunchedEffect
        if (editorValue.text == note.contentMarkdown) return@LaunchedEffect
        delay(2000)
        onSaveNote(note.id, editorValue.text)?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
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
                        text = "自动保存中",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    modifier = Modifier.height(40.dp),
                    onClick = {
                        focusManager.clearFocus()
                        markdownEditMode = !markdownEditMode
                    }
                ) {
                    Text(if (markdownEditMode) "预览" else "编辑")
                }
                Button(
                    modifier = Modifier.height(40.dp),
                    enabled = !parsing,
                    onClick = {
                        focusManager.clearFocus()
                        scope.launch {
                            parsing = true
                            try {
                                val result = onParse(editorValue.text)
                                parseResult = result
                                selectedIds.clear()
                                editableCandidates.clear()
                                result.candidates.forEach { candidate ->
                                    val editable = candidate.toPlanningImportCandidate()
                                    editableCandidates += editable
                                    selectedIds[editable.id] = editable.validate() == null
                                }
                                val extra = result.message.takeIf { it.isNotBlank() }?.let { "；$it" }.orEmpty()
                                Toast.makeText(context, "识别完成：${result.importableCount} 条可导入$extra", Toast.LENGTH_SHORT).show()
                                previewSheetVisible = true
                            } catch (error: Exception) {
                                Toast.makeText(context, error.message ?: "识别失败", Toast.LENGTH_SHORT).show()
                            } finally {
                                parsing = false
                            }
                        }
                    }
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (parsing) "识别中" else "识别")
                }
                IconButton(
                    modifier = Modifier.size(40.dp),
                    onClick = {
                        focusManager.clearFocus()
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
                            text = { Text(if (shortcutBarExpanded) "隐藏快捷输入栏" else "显示快捷输入栏") },
                            onClick = { overflowMenuExpanded = false; shortcutBarExpanded = !shortcutBarExpanded }
                        )
                        DropdownMenuItem(
                            text = { Text("刷新已导入项") },
                            onClick = { overflowMenuExpanded = false; refreshConfirmVisible = true },
                            enabled = activeNote != null && !operationRunning
                        )
                        DropdownMenuItem(
                            text = { Text("批量顺延") },
                            onClick = { overflowMenuExpanded = false; postponeSheetVisible = true },
                            enabled = activeNote != null && mappingStates.any { it.status == MappingStatus.ACTIVE } && !operationRunning
                        )
                        DropdownMenuItem(
                            text = { Text("撤销上次操作") },
                            onClick = { overflowMenuExpanded = false; undoConfirmVisible = true },
                            enabled = activeNote != null && latestUndoSummary != null && !operationRunning
                        )
                        DropdownMenuItem(
                            text = { Text("同步完成状态到原文") },
                            onClick = {
                                overflowMenuExpanded = false
                                val updated = syncCompletedMappingsToMarkdown(editorValue.text, mappingStates)
                                if (updated != editorValue.text) {
                                    editorValue = editorValue.copy(text = updated)
                                }
                            },
                            enabled = mappingStates.any { it.status == MappingStatus.COMPLETED }
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
            if (!shortcutBarExpanded) {
                PlanningShortcutCollapsedHint(onExpand = { shortcutBarExpanded = true })
            }
            if (shortcutBarExpanded) {
                PlanningShortcutBar(
                    onAction = { _, action ->
                        editorValue = applyPlanningShortcut(editorValue, action)
                    },
                    onHelp = { _, description -> Toast.makeText(context, description, Toast.LENGTH_LONG).show() }
                )
            }

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
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(18.dp),
                    minLines = 12
                )
            }
        } else {
            PlanningMarkdownPreview(
                markdown = editorValue.text,
                mappings = mappingStates,
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
                },
                onApplyConflictDocument = { mappingId ->
                    val noteId = activeNote?.id ?: return@PlanningMarkdownPreview
                    scope.launch {
                        val result = onApplyConflictDocument(noteId, editorValue.text, mappingId)
                        result.updatedMarkdown?.let { editorValue = TextFieldValue(it) }
                        mappingStates = onSyncMappings(noteId, editorValue.text)
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    }
                },
                onApplyConflictItem = { mappingId ->
                    val noteId = activeNote?.id ?: return@PlanningMarkdownPreview
                    scope.launch {
                        val result = onApplyConflictItem(noteId, editorValue.text, mappingId)
                        result.updatedMarkdown?.let { editorValue = TextFieldValue(it) }
                        mappingStates = onSyncMappings(noteId, editorValue.text)
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    }
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
                                activeNote?.id?.let { noteId ->
                                    onSaveNote(noteId, updated)
                                    mappingStates = onSyncMappings(noteId, updated)
                                }
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
                    message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
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
                    message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
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
                    message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
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
                    message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                    archiveDialog = false
                }
            }
        )
    }

    if (refreshConfirmVisible && activeNote != null) {
        PlanningRefreshDialog(
            running = operationRunning,
            onDismiss = { refreshConfirmVisible = false },
            onConfirm = { scopeChoice ->
                scope.launch {
                    operationRunning = true
                    try {
                        val result = onRefreshImportedItems(
                            activeNote.id,
                            editorValue.text,
                            scopeChoice,
                            currentPlanningCursorLine(editorValue)
                        )
                        result.updatedMarkdown?.let { editorValue = TextFieldValue(it) }
                        mappingStates = onGetMappings(activeNote.id)
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    } catch (error: Exception) {
                        Toast.makeText(context, error.message ?: "刷新失败", Toast.LENGTH_SHORT).show()
                    } finally {
                        operationRunning = false
                        refreshConfirmVisible = false
                    }
                }
            }
        )
    }

    if (postponeSheetVisible && activeNote != null) {
        PlanningPostponeSheet(
            mappings = mappingStates.filter { it.status == MappingStatus.ACTIVE },
            running = operationRunning,
            onDismiss = { postponeSheetVisible = false },
            onConfirm = { mappingId, offsetMinutes, scopeChoice ->
                scope.launch {
                    operationRunning = true
                    try {
                        val result = onPostponeImportedItems(activeNote.id, editorValue.text, mappingId, offsetMinutes, scopeChoice)
                        result.updatedMarkdown?.let { editorValue = TextFieldValue(it) }
                        mappingStates = onGetMappings(activeNote.id)
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    } catch (error: Exception) {
                        Toast.makeText(context, error.message ?: "顺延失败", Toast.LENGTH_SHORT).show()
                    } finally {
                        operationRunning = false
                        postponeSheetVisible = false
                    }
                }
            }
        )
    }

    if (undoConfirmVisible && activeNote != null) {
        ConfirmPlanningDialog(
            title = "撤销上次规划台操作？",
            message = latestUndoSummary?.let { "会回滚最近一次${it.label}批次，影响 ${it.affectedCount} 条事项。" }
                ?: "没有可撤销的导入、刷新或顺延批次。",
            confirmText = "撤销",
            onDismiss = { undoConfirmVisible = false },
            onConfirm = {
                scope.launch {
                    operationRunning = true
                    try {
                        val result = onUndoLastOperation(activeNote.id, editorValue.text)
                        result.updatedMarkdown?.let { editorValue = TextFieldValue(it) }
                        mappingStates = onGetMappings(activeNote.id)
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    } catch (error: Exception) {
                        Toast.makeText(context, error.message ?: "撤销失败", Toast.LENGTH_SHORT).show()
                    } finally {
                        operationRunning = false
                        undoConfirmVisible = false
                    }
                }
            }
        )
    }

}

@Composable
private fun PlanningShortcutCollapsedHint(
    onExpand: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)),
        onClick = onExpand
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "展开快捷操作",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                "写作区优先",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun PlanningShortcutBar(
    onAction: (String, PlanningShortcutAction) -> Unit,
    onHelp: (String, String) -> Unit
) {
    val chips = listOf(
        PlanningShortcutSpec("任务", Icons.Rounded.CheckBox, PlanningShortcutAction.TaskLine, "把当前行变成一条待办；同一行不会重复插入 - [ ]"),
        PlanningShortcutSpec("子任务", Icons.Rounded.PlaylistAdd, PlanningShortcutAction.SubtaskLine, "在当前行下面新建一条缩进子任务"),
        PlanningShortcutSpec("缩进", Icons.Rounded.FormatIndentIncrease, PlanningShortcutAction.Indent, "当前行增加一级缩进"),
        PlanningShortcutSpec("减少缩进", Icons.Rounded.FormatIndentDecrease, PlanningShortcutAction.Outdent, "当前行减少一级缩进"),
        PlanningShortcutSpec("DDL", Icons.Rounded.Event, PlanningShortcutAction.Insert(" #ddl "), "设置截止时间，例如 #ddl 5.28 23:59 或 #ddl 明天 16:30"),
        PlanningShortcutSpec("日程", Icons.Rounded.CalendarMonth, PlanningShortcutAction.Insert(" #schedule "), "显式声明日程时间段"),
        PlanningShortcutSpec("提醒", Icons.Rounded.NotificationsNone, PlanningShortcutAction.Insert(" #remind "), "设置提醒，例如 #remind 5,15,16:30,明天 16:30"),
        PlanningShortcutSpec("分组", Icons.Rounded.Folder, PlanningShortcutAction.Insert(" #group "), "指定分组，例如 #group 课程"),
        PlanningShortcutSpec("今日", Icons.Rounded.Today, PlanningShortcutAction.InsertSection("# 今日计划"), "插入标题分区；下面没写日期的时间段按今天理解"),
        PlanningShortcutSpec("明日", Icons.Rounded.Event, PlanningShortcutAction.InsertSection("# 明天"), "插入标题分区；下面没写日期的时间段按明天理解"),
        PlanningShortcutSpec("公告", Icons.Rounded.Campaign, PlanningShortcutAction.InsertAnnouncement, "在新行插入 #公告 占位，填上日期范围和正文后会显示在每日看板顶部和桌面小组件")
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        chips.forEach { spec ->
            PlanningShortcutChip(spec, onAction, onHelp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlanningShortcutChip(
    spec: PlanningShortcutSpec,
    onAction: (String, PlanningShortcutAction) -> Unit,
    onHelp: (String, String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = Modifier.combinedClickable(
            onClick = { onAction(spec.label, spec.action) },
            onLongClick = { onHelp(spec.label, spec.help) }
        )
    ) {
        Column(
            modifier = Modifier
                .width(58.dp)
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = spec.icon,
                contentDescription = spec.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(23.dp)
            )
            Text(
                text = spec.label,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlanningRefreshDialog(
    running: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (PlanningRefreshScope) -> Unit
) {
    var wholeDocument by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("刷新已导入项") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("只会刷新未完成且未取消的映射项；已完成项会跳过，手动改过的事项会标记为冲突。")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Switch(checked = wholeDocument, onCheckedChange = { wholeDocument = it })
                    Text(if (wholeDocument) "范围：整篇文档" else "范围：当前标题分区")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !running,
                onClick = { onConfirm(if (wholeDocument) PlanningRefreshScope.WHOLE_DOCUMENT else PlanningRefreshScope.CURRENT_SECTION) }
            ) { Text("刷新") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !running) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanningPostponeSheet(
    mappings: List<PlanningLineMapping>,
    running: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long?, Int, PlanningPostponeScope) -> Unit
) {
    var selectedId by rememberSaveable(mappings) { mutableStateOf(mappings.firstOrNull()?.id) }
    var customOffset by rememberSaveable { mutableStateOf("") }
    var selectedOffset by rememberSaveable { mutableStateOf(30) }
    var selectedScope by rememberSaveable { mutableStateOf(PlanningPostponeScope.FROM_ITEM_TO_SECTION_END) }
    val offset = customOffset.toIntOrNull() ?: selectedOffset
    val previewItems: List<Pair<String, String>> = remember(mappings, selectedId, offset) {
        val selectedIndex = mappings.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: 0
        mappings.drop(selectedIndex).take(6).map { mapping ->
            val sourceLine = mapping.trackedLineText.ifBlank { "未命名条目" }
            sourceLine to shiftPlanningPreviewText(sourceLine, offset)
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("批量顺延", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("选择起始条目和偏移量后，会同步修改正式事项时间和规划台原文里的时间文本。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("起始条目", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.25f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mappings, key = { it.id }) { mapping ->
                    FilterChip(
                        selected = selectedId == mapping.id,
                        onClick = { selectedId = mapping.id },
                        label = {
                            Text(
                                mapping.trackedLineText.ifBlank { "第 ${mapping.lastKnownLineNumber} 行" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
            Text("偏移量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(15, 30, 60, 120).forEach { minutes ->
                    FilterChip(
                        selected = customOffset.isBlank() && selectedOffset == minutes,
                        onClick = {
                            customOffset = ""
                            selectedOffset = minutes
                        },
                        label = { Text("+${minutes}分") }
                    )
                }
            }
            OutlinedTextField(
                value = customOffset,
                onValueChange = { value -> customOffset = value.filter { it.isDigit() || it == '-' }.take(4) },
                label = { Text("自定义分钟，可填负数") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("影响范围", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlanningScopeChip("从此条目到分区末尾", selectedScope == PlanningPostponeScope.FROM_ITEM_TO_SECTION_END) {
                    selectedScope = PlanningPostponeScope.FROM_ITEM_TO_SECTION_END
                }
                PlanningScopeChip("从此条目到文档末尾", selectedScope == PlanningPostponeScope.FROM_ITEM_TO_DOCUMENT_END) {
                    selectedScope = PlanningPostponeScope.FROM_ITEM_TO_DOCUMENT_END
                }
                PlanningScopeChip("当前分区所有未完成项", selectedScope == PlanningPostponeScope.CURRENT_SECTION_ALL) {
                    selectedScope = PlanningPostponeScope.CURRENT_SECTION_ALL
                }
            }
            Text("预览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                previewItems.forEach { (oldLine, newLine) ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(oldLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("→ $newLine", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss, enabled = !running) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = !running && selectedId != null && offset != 0,
                    onClick = { onConfirm(selectedId, offset, selectedScope) }
                ) { Text("执行顺延") }
            }
        }
    }
}

@Composable
private fun PlanningScopeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
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
    val pages = remember { planningTutorialPages() }
    var pageIndex by rememberSaveable { mutableStateOf(0) }
    val page = pages[pageIndex]
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
                Text("规划台新手教程", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("第 ${pageIndex + 1} / ${pages.size} 页 · ${page.subtitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onDismiss) { Text("知道了") }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.58f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(page.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(page.lines) { line ->
                    Text("• $line", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                page.example?.let { example ->
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                        ) {
                            Text(
                                text = example,
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                enabled = pageIndex > 0
            ) { Text("上一页") }
            Text(
                text = pages.indices.joinToString(" ") { index -> if (index == pageIndex) "●" else "○" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = {
                    if (pageIndex < pages.lastIndex) {
                        pageIndex += 1
                    } else {
                        onDismiss()
                    }
                }
            ) { Text(if (pageIndex < pages.lastIndex) "下一页" else "开始使用") }
        }
    }
}

private data class PlanningTutorialPage(
    val title: String,
    val subtitle: String,
    val lines: List<String>,
    val example: String? = null
)

private fun planningTutorialPages(): List<PlanningTutorialPage> {
    return listOf(
        PlanningTutorialPage(
            title = "1. 你可以先随便写",
            subtitle = "像备忘录一样",
            lines = listOf(
                "不用先想复杂语法，先把脑子里的计划写下来。",
                "最自然的日程写法是：时间段 + 事情。",
                "最自然的任务写法是：事情 + ddl + 时间。",
                "如果你写得很花哨，先保留原文，再用识别预览页校正。"
            ),
            example = """
                10:00-12:00 事件1
                12:00-13:00 事件2
                任务M ddl 15:00
            """.trimIndent()
        ),
        PlanningTutorialPage(
            title = "2. 点图标可以快速变成任务",
            subtitle = "不用手敲 - [ ]",
            lines = listOf(
                "把光标放在一行文字里，点“任务”图标，这一行会变成待办。",
                "点“子任务”会在当前行下面新建缩进任务。",
                "长按任意快捷图标，可以看它的用途说明。"
            ),
            example = """
                整理材料
                点“任务”后：
                - [ ] 整理材料
            """.trimIndent()
        ),
        PlanningTutorialPage(
            title = "3. DDL 和提醒怎么写",
            subtitle = "常用就够",
            lines = listOf(
                "DDL 可以写 15:00、明天 16:30、5.28 23:59。",
                "提醒可以写 #remind 5,15，意思是提前 5 分钟和 15 分钟。",
                "你也可以先不写提醒，导入前在预览页修改。",
                "常用自然文本里写 ddl 15:00，也会尽量按今天 15:00 理解。"
            ),
            example = """
                - [ ] 任务M #ddl 15:00
                - [ ] 交材料 #ddl 5.28 23:59 #remind 30,5
            """.trimIndent()
        ),
        PlanningTutorialPage(
            title = "4. 标题只是分区",
            subtitle = "不是必须",
            lines = listOf(
                "# 今日计划 下面没写日期的时间段会按今天理解。",
                "# 明天 下面没写日期的时间段会按明天理解。",
                "# 收集箱 只是普通分区，不会自动加日期。"
            ),
            example = """
                # 今日计划
                10:00-12:00 写作业

                # 明天
                09:00-10:30 背单词
            """.trimIndent()
        ),
        PlanningTutorialPage(
            title = "5. 最后点识别",
            subtitle = "预览确认再导入",
            lines = listOf(
                "写完后点顶部“识别”。",
                "识别预览里可以改标题、时间、分组、提醒。",
                "确认没问题再导入，导入后原文会追加 #imported，避免重复导入。"
            )
        ),
        PlanningTutorialPage(
            title = "6. 关于 AI 识别",
            subtitle = "可选增强",
            lines = listOf(
                "设置里启用 AI 调用配置后，“识别”会优先调用 AI 源。",
                "DeepSeek、Qwen、OpenAI 兼容接口可以配置多个，按顺序失败兜底。",
                "AI 识别适合处理写得很随性、格式不固定的内容。",
                "AI 失败或没有完整配置时会回到本地规则；AI 只生成候选，不会直接写入待办或日程。"
            )
        ),
        PlanningTutorialPage(
            title = "7. 公告写在规划台",
            subtitle = "可以写多条",
            lines = listOf(
                "公告不在设置里单独维护，直接写进任意未归档规划文档。",
                "以 #公告 开头的行会显示在每日看板顶部和桌面小组件里。",
                "可以写日期范围；不写日期时视为长期公告。"
            ),
            example = """
                #公告 5.16-7.1 期间禁止游玩舞萌DX游戏
                #公告 2026-05-16 2026-05-20 本周推进保研材料
                > [!公告] 长期提醒：先完成今天三件大事
            """.trimIndent()
        ),
        PlanningTutorialPage(
            title = "8. 其他导入方式",
            subtitle = "已整理好的大量待办/日程",
            lines = listOf(
                "已整理好的大量待办，可以去「待办 → 右下角批量待办」一次性录入。",
                "日程批量导入在「日历 → 顶部工具栏 → 批量导入」。",
                "批量导入使用逗号分隔的结构化格式，适合从 Excel 或其他工具粘贴。"
            )
        )
    )
}

@Composable
private fun PlanningMarkdownPreview(
    markdown: String,
    mappings: List<PlanningLineMapping>,
    modifier: Modifier,
    onToggleCheckbox: (Int) -> Unit,
    onRequestEdit: (Int?) -> Unit,
    onApplyConflictDocument: (Long) -> Unit,
    onApplyConflictItem: (Long) -> Unit
) {
    val parsedLines = remember(markdown) { runCatching { parsePlanningMarkdownLines(markdown) } }
    val mappingsByLine = remember(mappings) {
        mappings
            .filter { it.lastKnownLineNumber > 0 }
            .groupBy { it.lastKnownLineNumber }
            .mapValues { (_, value) -> value.maxByOrNull { it.lastRefreshedAtMillis } }
    }
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
                    val mapping = mappingsByLine[line.lineNumber]
                    when (line) {
                        is PlanningRenderedLine.Heading -> PlanningMarkdownHeading(line)
                        is PlanningRenderedLine.Announcement -> PlanningMarkdownAnnouncementLine(line, onRequestEdit)
                        is PlanningRenderedLine.Task -> PlanningMarkdownTaskLine(line, mapping, onToggleCheckbox, onRequestEdit, onApplyConflictDocument, onApplyConflictItem)
                        is PlanningRenderedLine.Text -> PlanningMarkdownTextLine(line, mapping, onRequestEdit, onApplyConflictDocument, onApplyConflictItem)
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
private fun PlanningMarkdownAnnouncementLine(
    line: PlanningRenderedLine.Announcement,
    onRequestEdit: (Int?) -> Unit
) {
    val accent = Color(0xFFFFB347)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.82f)),
        onClick = { onRequestEdit(line.lineNumber) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Campaign,
                contentDescription = null,
                tint = Color(0xFFCC8030),
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = accent.copy(alpha = 0.20f)
                    ) {
                        Text(
                            "全局公告",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A560F),
                            maxLines = 1
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    ) {
                        Text(
                            line.rangeLabel,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
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
    mapping: PlanningLineMapping?,
    onToggleCheckbox: (Int) -> Unit,
    onRequestEdit: (Int?) -> Unit,
    onApplyConflictDocument: (Long) -> Unit,
    onApplyConflictItem: (Long) -> Unit
) {
    val status = mapping?.status
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = (line.indentLevel * 18).dp),
        shape = RoundedCornerShape(18.dp),
        color = planningLineContainerColor(status, line.imported),
        border = planningLineBorder(status),
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
                        textDecoration = if (line.checked || status == MappingStatus.COMPLETED || status == MappingStatus.CANCELED) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = planningLineTextColor(status, line.checked)
                )
                PlanningMarkdownPills(tags = line.tags, imported = line.imported, status = status)
                if (status == MappingStatus.CONFLICT && mapping != null) {
                    PlanningConflictActions(mapping.id, onApplyConflictDocument, onApplyConflictItem)
                }
            }
        }
    }
}

@Composable
private fun PlanningMarkdownTextLine(
    line: PlanningRenderedLine.Text,
    mapping: PlanningLineMapping?,
    onRequestEdit: (Int?) -> Unit,
    onApplyConflictDocument: (Long) -> Unit,
    onApplyConflictItem: (Long) -> Unit
) {
    val status = mapping?.status
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = planningLineContainerColor(status, line.imported),
        border = planningLineBorder(status),
        onClick = { onRequestEdit(line.lineNumber) }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                line.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = if (status == MappingStatus.COMPLETED || status == MappingStatus.CANCELED) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = planningLineTextColor(status, false)
            )
            PlanningMarkdownPills(tags = line.tags, imported = line.imported, status = status)
            if (status == MappingStatus.CONFLICT && mapping != null) {
                PlanningConflictActions(mapping.id, onApplyConflictDocument, onApplyConflictItem)
            }
        }
    }
}

@Composable
private fun PlanningMarkdownPills(tags: List<String>, imported: Boolean, status: MappingStatus? = null) {
    if (tags.isEmpty() && !imported && status == null) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        tags.forEach { tag -> PlanningMarkdownTagPill(tag) }
        val label = when (status) {
            MappingStatus.ACTIVE -> "已导入"
            MappingStatus.COMPLETED -> "✓ 已完成"
            MappingStatus.CANCELED -> "已取消"
            MappingStatus.ORPHANED -> "映射丢失"
            MappingStatus.CONFLICT -> "已手动修改"
            null -> if (imported) "已导入" else null
        }
        val resolvedStatus = status ?: if (imported) MappingStatus.ACTIVE else null
        if (label != null && resolvedStatus != null) PlanningMarkdownStatePill(label, resolvedStatus)
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
private fun PlanningMarkdownStatePill(label: String, status: MappingStatus = MappingStatus.ACTIVE) {
    val color = planningStatusColor(status)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun planningLineContainerColor(status: MappingStatus?, imported: Boolean): Color {
    return when (status) {
        MappingStatus.ACTIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        MappingStatus.COMPLETED -> Color(0xFF2E7D32).copy(alpha = 0.10f)
        MappingStatus.CANCELED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        MappingStatus.ORPHANED -> Color(0xFFEF6C00).copy(alpha = 0.10f)
        MappingStatus.CONFLICT -> Color(0xFFF9A825).copy(alpha = 0.13f)
        null -> if (imported) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    }
}

@Composable
private fun planningLineBorder(status: MappingStatus?): BorderStroke? {
    return when (status) {
        MappingStatus.ORPHANED -> BorderStroke(1.dp, Color(0xFFEF6C00).copy(alpha = 0.65f))
        MappingStatus.CONFLICT -> BorderStroke(1.dp, Color(0xFFF9A825).copy(alpha = 0.72f))
        else -> null
    }
}

@Composable
private fun planningLineTextColor(status: MappingStatus?, checked: Boolean): Color {
    return when {
        status == MappingStatus.COMPLETED -> Color(0xFF2E7D32)
        status == MappingStatus.CANCELED -> MaterialTheme.colorScheme.onSurfaceVariant
        checked -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
private fun planningStatusColor(status: MappingStatus): Color {
    return when (status) {
        MappingStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        MappingStatus.COMPLETED -> Color(0xFF2E7D32)
        MappingStatus.CANCELED -> MaterialTheme.colorScheme.onSurfaceVariant
        MappingStatus.ORPHANED -> Color(0xFFEF6C00)
        MappingStatus.CONFLICT -> Color(0xFFF9A825)
    }
}

@Composable
private fun PlanningConflictActions(
    mappingId: Long,
    onApplyConflictDocument: (Long) -> Unit,
    onApplyConflictItem: (Long) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onApplyConflictDocument(mappingId) }) {
            Text("以文档为准覆盖")
        }
        TextButton(onClick = { onApplyConflictItem(mappingId) }) {
            Text("以事项为准更新文档")
        }
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
                if (result.message.isNotBlank()) {
                    Text(result.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
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
                if (candidate.message.contains("AI", ignoreCase = true)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFD166).copy(alpha = 0.26f),
                        border = BorderStroke(1.dp, Color(0xFFFFB000).copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "AI",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8A5A00)
                        )
                    }
                }
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
                if (candidate.type == PlanningParsedType.EVENT) {
                    OutlinedTextField(
                        value = candidate.location,
                        onValueChange = { onCandidateChange(candidate.copy(location = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("地点") },
                        placeholder = { Text("@主楼B1-412") },
                        singleLine = true
                    )
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
                PlanningCandidateOptionRow(
                    candidate = candidate,
                    onCandidateChange = onCandidateChange
                )
                PlanningRecurrenceEditor(
                    recurrence = candidate.recurrence,
                    onChange = { onCandidateChange(candidate.copy(recurrence = it)) }
                )
            }
            if (candidate.type == PlanningParsedType.EVENT) {
                FilterChip(
                    selected = candidate.createLinkedTodo,
                    onClick = { onCandidateChange(candidate.copy(createLinkedTodo = !candidate.createLinkedTodo)) },
                    label = { Text("同步创建以日程结束时间为 DDL 的待办任务") }
                )
            }
            if (validation != null) Text(validation, color = MaterialTheme.colorScheme.error)
            if (validation == null && candidate.message.isNotBlank()) Text(candidate.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlanningCandidateOptionRow(
    candidate: PlanningImportCandidate,
    onCandidateChange: (PlanningImportCandidate) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (candidate.type == PlanningParsedType.EVENT) {
            FilterChip(
                selected = candidate.allDay,
                onClick = { onCandidateChange(candidate.copy(allDay = !candidate.allDay)) },
                label = { Text("全天") }
            )
        }
        val canCountdown = candidate.type == PlanningParsedType.EVENT || candidate.dueAt != null
        FilterChip(
            selected = candidate.countdownEnabled,
            onClick = {
                if (canCountdown) onCandidateChange(candidate.copy(countdownEnabled = !candidate.countdownEnabled))
            },
            enabled = canCountdown,
            label = { Text("倒数日") }
        )
    }
}

@Composable
private fun PlanningRecurrenceEditor(
    recurrence: RecurrenceConfig,
    onChange: (RecurrenceConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val type = if (recurrence.enabled) recurrence.type else RecurrenceType.NONE
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilterChip(
            selected = type != RecurrenceType.NONE,
            onClick = { expanded = true },
            label = { Text("重复：${type.label}") }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RecurrenceType.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onChange(
                            if (option == RecurrenceType.NONE) {
                                RecurrenceConfig()
                            } else {
                                recurrence.copy(enabled = true, type = option)
                            }
                        )
                    }
                )
            }
        }
        if (type != RecurrenceType.NONE) {
            var endDateText by remember(recurrence.endDate) { mutableStateOf(recurrence.endDate?.toString().orEmpty()) }
            OutlinedTextField(
                value = endDateText,
                onValueChange = { raw ->
                    endDateText = raw
                    onChange(recurrence.copy(endDate = runCatching { LocalDate.parse(raw.trim()) }.getOrNull()))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("循环截止日期") },
                placeholder = { Text("2026-06-30") },
                singleLine = true,
                isError = endDateText.isNotBlank() && runCatching { LocalDate.parse(endDateText.trim()) }.isFailure
            )
            if (type == RecurrenceType.WEEKLY) {
                var weekdaysText by remember(recurrence.weeklyDays) {
                    mutableStateOf(recurrence.weeklyDays.map { it.value }.sorted().joinToString(","))
                }
                OutlinedTextField(
                    value = weekdaysText,
                    onValueChange = { raw ->
                        weekdaysText = raw
                        onChange(recurrence.copy(weeklyDays = parsePlanningWeekdays(raw)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("每周循环的周几") },
                    placeholder = { Text("1,3,5") },
                    singleLine = true
                )
            }
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
    val icon: ImageVector,
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
    data object InsertAnnouncement : PlanningShortcutAction
}

private fun applyPlanningShortcut(value: TextFieldValue, action: PlanningShortcutAction): TextFieldValue {
    return when (action) {
        is PlanningShortcutAction.Insert -> insertPlanningToken(value, action.token)
        is PlanningShortcutAction.InsertSection -> insertPlanningSection(value, action.heading)
        PlanningShortcutAction.TaskLine -> applyPlanningTaskLine(value)
        PlanningShortcutAction.SubtaskLine -> insertPlanningSubtaskLine(value)
        PlanningShortcutAction.Indent -> indentCurrentPlanningLine(value)
        PlanningShortcutAction.Outdent -> outdentCurrentPlanningLine(value)
        PlanningShortcutAction.InsertAnnouncement -> insertAnnouncementAtNewLine(value)
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

private fun insertAnnouncementAtNewLine(value: TextFieldValue): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val before = value.text.substring(0, start)
    val after = value.text.substring(end)
    val lineStart = before.lastIndexOf('\n').let { if (it < 0) 0 else it + 1 }
    val prefix = if (start == lineStart) "" else "\n"
    val token = "${prefix}#公告 "
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

private fun currentPlanningCursorLine(value: TextFieldValue): Int {
    val cursor = value.selection.start.coerceIn(0, value.text.length)
    return value.text.take(cursor).count { it == '\n' } + 1
}

private fun syncCompletedMappingsToMarkdown(
    markdown: String,
    mappings: List<PlanningLineMapping>
): String {
    val completedLines = mappings
        .filter { it.status == MappingStatus.COMPLETED && it.lastKnownLineNumber > 0 }
        .map { it.lastKnownLineNumber }
        .toSet()
    if (completedLines.isEmpty()) return markdown
    val hasTrailingNewline = markdown.endsWith("\n") || markdown.endsWith("\r")
    val normalized = markdown.replace("\r\n", "\n").replace('\r', '\n')
    val updated = normalized.lines().mapIndexed { index, line ->
        if (index + 1 !in completedLines) return@mapIndexed line
        TaskPreviewRegex.matchEntire(line)?.let { match ->
            if (match.groupValues[2].equals("x", ignoreCase = true)) line else match.groupValues[1] + "- [x] " + match.groupValues[3]
        } ?: line
    }.joinToString("\n")
    return if (hasTrailingNewline && !updated.endsWith("\n")) "$updated\n" else updated
}

private fun shiftPlanningPreviewText(line: String, offsetMinutes: Int): String {
    val timeRegex = Regex("(凌晨|早上|上午|中午|下午|晚上)?\\s*(\\d{1,2})[:：](\\d{2})")
    return timeRegex.replace(line) { match ->
        val period = match.groupValues[1]
        val hour = match.groupValues[2].toIntOrNull() ?: return@replace match.value
        val minute = match.groupValues[3].toIntOrNull() ?: return@replace match.value
        val base = java.time.LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)).plusMinutes(offsetMinutes.toLong())
        if (period.isBlank()) "%02d:%02d".format(base.hour, base.minute) else "$period %02d:%02d".format(base.hour, base.minute)
    }
}

private data class PlanningUndoSummary(
    val batchId: String,
    val operationType: String,
    val affectedCount: Int
) {
    val label: String
        get() = when (operationType) {
            "IMPORT" -> "导入"
            "REFRESH" -> "刷新"
            "POSTPONE" -> "顺延"
            else -> operationType
        }
}

private fun latestPlanningUndoSummary(mappings: List<PlanningLineMapping>): PlanningUndoSummary? {
    val latest = mappings
        .filter { it.operationType == "IMPORT" || it.operationType == "REFRESH" || it.operationType == "POSTPONE" }
        .maxWithOrNull(compareBy<PlanningLineMapping> { it.lastRefreshedAtMillis }.thenBy { it.id })
        ?: return null
    val batch = mappings.filter { it.batchId == latest.batchId }
    val affectedCount = batch.mapNotNull { it.itemId }.toSet().size.takeIf { it > 0 } ?: batch.size
    return PlanningUndoSummary(
        batchId = latest.batchId,
        operationType = latest.operationType,
        affectedCount = affectedCount
    )
}

private sealed interface PlanningRenderedLine {
    val lineNumber: Int

    data class Heading(
        override val lineNumber: Int,
        val level: Int,
        val text: String
    ) : PlanningRenderedLine

    data class Announcement(
        override val lineNumber: Int,
        val text: String,
        val rangeLabel: String
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

        PlanningAnnouncementParser.parseSingleLine(rawLine, lineNumber = lineNumber)?.let { announcement ->
            return@mapIndexedNotNull PlanningRenderedLine.Announcement(
                lineNumber = lineNumber,
                text = announcement.text,
                rangeLabel = announcement.rangeLabel()
            )
        }

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

private fun parsePlanningWeekdays(raw: String): Set<DayOfWeek> {
    return raw.split(',', '，')
        .mapNotNull { token -> token.trim().toIntOrNull()?.let { runCatching { DayOfWeek.of(it) }.getOrNull() } }
        .toSet()
}

private fun planningMillisLabel(value: Long): String {
    val dateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(value), java.time.ZoneId.systemDefault())
    return planningDateTimeLabel(dateTime)
}
