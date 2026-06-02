package com.example.todoalarm.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.PlanningAiCaller
import com.example.todoalarm.data.PlanningAiProvider
import com.example.todoalarm.data.PlanningAiVisionRequest
import com.example.todoalarm.data.PlanningNote
import com.example.todoalarm.data.PlanningAnnouncementParser
import com.example.todoalarm.data.PlanningImportCandidate
import com.example.todoalarm.data.PlanningImportResult
import com.example.todoalarm.data.PlanningLineMapping
import com.example.todoalarm.data.PlanningNode
import com.example.todoalarm.data.PlanningNodeDraft
import com.example.todoalarm.data.PlanningNodeEdit
import com.example.todoalarm.data.PlanningNodeSnapshot
import com.example.todoalarm.data.MappingStatus
import com.example.todoalarm.data.PlanningOperationResult
import com.example.todoalarm.data.PlanningParseResult
import com.example.todoalarm.data.PlanningParsedType
import com.example.todoalarm.data.PlanningPostponeScope
import com.example.todoalarm.data.PlanningRefreshScope
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.RecurrenceType
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.toPlanningImportCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanningDeskPanel(
    notes: List<PlanningNote>,
    activeNote: PlanningNote?,
    nodes: List<PlanningNode>,
    groups: List<TaskGroup>,
    planningAiProviders: List<PlanningAiProvider>,
    outlineHintVisible: Boolean,
    onSelectNote: (Long) -> Unit,
    onCreateNote: suspend (String) -> String?,
    onSaveNote: suspend (Long, String) -> String?,
    onRenameNote: suspend (Long, String) -> String?,
    onDeleteNote: suspend (Long) -> String?,
    onArchiveNote: suspend (Long) -> String?,
    onOpenTodayNote: suspend () -> Long,
    onCreateNode: suspend (PlanningNodeDraft) -> String?,
    onUpdateNode: suspend (PlanningNode, PlanningNodeEdit) -> String?,
    onToggleNode: suspend (PlanningNode) -> String?,
    onPublishNode: suspend (PlanningNode) -> String?,
    onPublishAllDrafts: suspend (Long) -> String?,
    onDeleteNode: suspend (PlanningNode) -> String?,
    onReorderNodes: suspend (Long, Long?, List<Long>) -> String?,
    onCreateNodeSnapshot: suspend (Long) -> PlanningNodeSnapshot,
    onRestoreNodeSnapshot: suspend (PlanningNodeSnapshot) -> String?,
    onOpenLinkedItem: (Long) -> Unit,
    onExportNodesMarkdown: suspend (Long) -> String,
    onReplaceNodesFromMarkdown: suspend (Long, String) -> String?,
    onParse: suspend (String, Long?) -> PlanningParseResult,
    onImport: suspend (List<PlanningImportCandidate>, Set<String>, String, Long?) -> PlanningImportResult,
    onSyncMappings: suspend (Long, String) -> List<PlanningLineMapping>,
    onGetMappings: suspend (Long) -> List<PlanningLineMapping>,
    onRefreshImportedItems: suspend (Long, String, PlanningRefreshScope, Int?) -> PlanningOperationResult,
    onPostponeImportedItems: suspend (Long, String, Long?, Int, PlanningPostponeScope) -> PlanningOperationResult,
    onUndoLastOperation: suspend (Long, String) -> PlanningOperationResult,
    onApplyConflictDocument: suspend (Long, String, Long) -> PlanningOperationResult,
    onApplyConflictItem: suspend (Long, String, Long) -> PlanningOperationResult,
    highlightedPlanningNodeId: Long? = null,
    highlightedPlanningNodeSerial: Int = 0,
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
    var markdownCompatMode by rememberSaveable(activeNote?.id) { mutableStateOf(true) }
    var markdownEditMode by rememberSaveable(activeNote?.id) { mutableStateOf(true) }
    var outlinePreviewMode by rememberSaveable(activeNote?.id) { mutableStateOf(false) }
    var markdownImportVisible by remember { mutableStateOf(false) }
    var markdownImportText by remember(activeNote?.id) { mutableStateOf("") }
    var renameDialog by remember { mutableStateOf(false) }
    var pendingDeleteNote by remember { mutableStateOf<PlanningNote?>(null) }
    var pendingDeleteNode by remember { mutableStateOf<PlanningNode?>(null) }
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
    var visionRecognizing by remember { mutableStateOf(false) }
    var latestVisionImageUri by remember { mutableStateOf<Uri?>(null) }
    val selectedIds = remember { mutableStateMapOf<String, Boolean>() }
    val editableCandidates = remember { mutableStateListOf<PlanningImportCandidate>() }
    val outlineUndoStack = remember(activeNote?.id) { mutableStateListOf<PlanningNodeUndoEntry>() }
    var outlineUndoRunning by remember(activeNote?.id) { mutableStateOf(false) }
    val hasUnsavedChanges = activeNote != null && editorValue.text != activeNote.contentMarkdown
    val draftNodeCount = remember(nodes) { nodes.count { it.isDraft } }
    val latestUndoSummary = remember(mappingStates) { latestPlanningUndoSummary(mappingStates) }
    val visionProviders = remember(planningAiProviders) {
        planningAiProviders
            .map { it.normalized() }
            .filter { it.enabled && it.supportsVision && it.baseUrl.isNotBlank() && it.apiKey.isNotBlank() && it.model.isNotBlank() }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            visionRecognizing = true
            try {
                val encodedImage = compressPlanningVisionImage(context, uri)
                val response = PlanningAiCaller.callVisionWithFallback(
                    providers = planningAiProviders,
                    request = PlanningAiVisionRequest(
                        systemPrompt = PlanningVisionSystemPrompt,
                        prompt = PlanningVisionUserPrompt,
                        imageBase64 = encodedImage.base64,
                        imageMimeType = encodedImage.mimeType
                    )
                )
                val recognizedMarkdown = response.content
                    .replace("\r\n", "\n")
                    .replace('\r', '\n')
                    .lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.startsWith("```") }
                    .joinToString("\n")
                    .trim()
                if (recognizedMarkdown.isBlank()) {
                    Toast.makeText(context, "未能从图中识别出日程", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                latestVisionImageUri = uri
                val noteId = activeNote?.id
                val result = onParse(recognizedMarkdown, noteId)
                val drafts = result.candidates
                    .map { it.toPlanningImportCandidate() }
                    .filter { candidate -> candidate.importable && candidate.validate() == null }
                if (noteId != null && drafts.isNotEmpty()) {
                    var added = 0
                    drafts.forEach { candidate ->
                        if (onCreateNode(candidate.toPlanningNodeDraft(noteId)) == null) {
                            added += 1
                        }
                    }
                    Toast.makeText(context, "图片识别完成，已添加 $added 条草稿。", Toast.LENGTH_SHORT).show()
                } else {
                    val previousMarkdown = editorValue.text
                    val updated = appendPlanningMarkdown(previousMarkdown, recognizedMarkdown)
                    editorValue = TextFieldValue(text = updated, selection = TextRange(updated.length))
                    markdownEditMode = true
                    val lineCount = recognizedMarkdown.lines().count { it.isNotBlank() }
                    Toast.makeText(context, "已追加 $lineCount 条识别结果，请检查格式后手动整理。", Toast.LENGTH_LONG).show()
                }
            } catch (error: PlanningVisionImageException) {
                Toast.makeText(context, error.message ?: "图片过大，请裁剪后重试", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(context, "图片识别失败：${error.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
            } finally {
                visionRecognizing = false
            }
        }
    }

    LaunchedEffect(activeNote?.id) {
        editorValue = TextFieldValue(activeNote?.contentMarkdown.orEmpty())
        parseResult = null
        selectedIds.clear()
        editableCandidates.clear()
        outlineUndoStack.clear()
        markdownCompatMode = true
        markdownEditMode = true
    }

    fun pushOutlineUndo(entry: PlanningNodeUndoEntry?) {
        if (entry == null || entry.snapshot.nodes.isEmpty()) return
        outlineUndoStack.add(entry)
        while (outlineUndoStack.size > PlanningNodeUndoMaxSize) {
            outlineUndoStack.removeAt(0)
        }
    }

    suspend fun captureOutlineUndo(description: String): PlanningNodeUndoEntry? {
        val noteId = activeNote?.id ?: return null
        return runCatching {
            PlanningNodeUndoEntry(description = description, snapshot = onCreateNodeSnapshot(noteId))
        }.getOrElse { error ->
            Toast.makeText(context, error.message ?: "撤销快照创建失败", Toast.LENGTH_SHORT).show()
            null
        }
    }

    suspend fun restoreLastOutlineUndo() {
        if (outlineUndoRunning || outlineUndoStack.isEmpty()) return
        val index = outlineUndoStack.lastIndex
        val entry = outlineUndoStack.removeAt(index)
        outlineUndoRunning = true
        try {
            val message = onRestoreNodeSnapshot(entry.snapshot)
            message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            Toast.makeText(context, "已撤销：${entry.description}", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            outlineUndoStack.add(entry)
            Toast.makeText(context, error.message ?: "撤销失败", Toast.LENGTH_SHORT).show()
        } finally {
            outlineUndoRunning = false
        }
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
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.height(40.dp),
                    onClick = {
                        focusManager.clearFocus()
                        scope.launch {
                            val noteId = onOpenTodayNote()
                            onSelectNote(noteId)
                        }
                    }
                ) {
                    Text("今日")
                }
                OutlinedButton(
                    modifier = Modifier.height(40.dp),
                    onClick = {
                        focusManager.clearFocus()
                        documentSheetVisible = true
                    }
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = activeNote?.title?.take(8)?.let { "文档:$it" } ?: "文档",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!markdownCompatMode) {
                    OutlinedButton(
                        modifier = Modifier.height(40.dp),
                        onClick = {
                            focusManager.clearFocus()
                            outlinePreviewMode = !outlinePreviewMode
                        }
                    ) {
                        Text(if (outlinePreviewMode) "编辑" else "预览")
                    }
                    if (draftNodeCount > 0) {
                        Button(
                            modifier = Modifier.height(40.dp),
                            onClick = {
                                focusManager.clearFocus()
                                val noteId = activeNote?.id ?: return@Button
                                scope.launch {
                                    val undoEntry = captureOutlineUndo("批量发布草稿")
                                    val message = onPublishAllDrafts(noteId)
                                    message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                    if (message?.startsWith("已发布 0 条") != true) {
                                        pushOutlineUndo(undoEntry)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("发布${draftNodeCount}条")
                        }
                    }
                } else {
                    OutlinedButton(
                        modifier = Modifier.height(40.dp),
                        onClick = {
                            focusManager.clearFocus()
                            markdownCompatMode = false
                        }
                    ) {
                        Text("大纲")
                    }
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
                            val markdown = editorValue.text
                            if (markdown.isBlank()) {
                                Toast.makeText(context, "没有可识别的内容", Toast.LENGTH_SHORT).show()
                            } else {
                                scope.launch {
                                    parsing = true
                                    try {
                                        val result = onParse(markdown, activeNote?.id)
                                        parseResult = result
                                        editableCandidates.clear()
                                        editableCandidates.addAll(result.candidates.map { it.toPlanningImportCandidate() })
                                        selectedIds.clear()
                                        editableCandidates.forEach { candidate ->
                                            selectedIds[candidate.id] = candidate.importable && candidate.validate() == null
                                        }
                                        previewSheetVisible = true
                                    } catch (error: Exception) {
                                        Toast.makeText(context, error.message ?: "识别失败", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        parsing = false
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (parsing) "识别中" else "识别")
                    }
                }
                if (hasUnsavedChanges) {
                    Text(
                        text = "自动保存中",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!markdownCompatMode) {
                    IconButton(
                        modifier = Modifier.size(40.dp),
                        enabled = outlineUndoStack.isNotEmpty() && !outlineUndoRunning,
                        onClick = {
                            focusManager.clearFocus()
                            scope.launch { restoreLastOutlineUndo() }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = outlineUndoStack.lastOrNull()?.let { "撤销：${it.description}" } ?: "撤销"
                        )
                    }
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
                            text = {
                                Column {
                                    Text("从图片识别日程")
                                    if (visionProviders.isEmpty()) {
                                        Text(
                                            "请先在设置中标记支持图片识别的 AI 源",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                overflowMenuExpanded = false
                                imagePicker.launch("image/*")
                            },
                            enabled = activeNote != null && visionProviders.isNotEmpty() && !visionRecognizing,
                            leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null) }
                        )
                        DropdownMenuItem(
                                text = { Text(if (markdownCompatMode) "切换到大纲模式" else "自由书写模式") },
                            onClick = {
                                overflowMenuExpanded = false
                                focusManager.clearFocus()
                                markdownCompatMode = !markdownCompatMode
                            }
                        )
                        if (markdownCompatMode) {
                            DropdownMenuItem(
                                text = { Text("导出到 Markdown 兼容区") },
                                onClick = {
                                    overflowMenuExpanded = false
                                    focusManager.clearFocus()
                                    val noteId = activeNote?.id ?: return@DropdownMenuItem
                                    scope.launch {
                                        val markdown = onExportNodesMarkdown(noteId)
                                        editorValue = TextFieldValue(markdown, selection = TextRange(markdown.length))
                                        markdownEditMode = true
                                        Toast.makeText(context, "已导出到 Markdown 兼容区。", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = activeNote != null
                            )
                            DropdownMenuItem(
                                text = { Text("从 Markdown 导入大纲") },
                                onClick = {
                                    overflowMenuExpanded = false
                                    focusManager.clearFocus()
                                    val noteId = activeNote?.id ?: return@DropdownMenuItem
                                    scope.launch {
                                        markdownImportText = onExportNodesMarkdown(noteId)
                                        markdownImportVisible = true
                                    }
                                },
                                enabled = activeNote != null
                            )
                        }
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

        if (!markdownCompatMode) {
            PlanningOutlineEditor(
                activeNote = activeNote,
                nodes = nodes,
                outlineHintVisible = outlineHintVisible,
                previewMode = outlinePreviewMode,
                highlightedPlanningNodeId = highlightedPlanningNodeId,
                highlightedPlanningNodeSerial = highlightedPlanningNodeSerial,
                onCreateNode = { draft ->
                    scope.launch {
                        val message = onCreateNode(draft)
                        message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                    }
                },
                onUpdateNode = { node, edit ->
                    scope.launch {
                        val undoEntry = captureOutlineUndo("编辑节点：${node.text.compactUndoLabel()}")
                        val message = onUpdateNode(node, edit)
                        message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        if (message == null) pushOutlineUndo(undoEntry)
                    }
                },
                onToggleNode = { node ->
                    scope.launch {
                        val message = onToggleNode(node)
                        message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                    }
                },
                onPublishNode = { node ->
                    scope.launch {
                        val undoEntry = captureOutlineUndo("发布草稿：${node.text.compactUndoLabel()}")
                        val message = onPublishNode(node)
                        message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        if (message?.startsWith("草稿发布失败") != true) {
                            pushOutlineUndo(undoEntry)
                        }
                    }
                },
                onDeleteNodeNow = { node ->
                    scope.launch {
                        val undoEntry = captureOutlineUndo("删除节点：${node.text.compactUndoLabel()}")
                        val message = onDeleteNode(node)
                        message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        if (message == null) pushOutlineUndo(undoEntry)
                    }
                },
                onDeleteNode = { node ->
                    pendingDeleteNode = node
                },
                onReorderNodes = { noteId, parentNodeId, orderedNodeIds ->
                    scope.launch {
                        val undoEntry = captureOutlineUndo("调整节点顺序")
                        val message = onReorderNodes(noteId, parentNodeId, orderedNodeIds)
                        message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        if (message == null) pushOutlineUndo(undoEntry)
                    }
                },
                onMergeNodeIntoPrevious = { node, previous, edit ->
                    scope.launch {
                        val undoEntry = captureOutlineUndo("合并节点：${node.text.compactUndoLabel()}")
                        val updateMessage = onUpdateNode(previous, edit)
                        updateMessage?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        val deleteMessage = if (updateMessage == null) onDeleteNode(node) else null
                        deleteMessage?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        if (updateMessage == null) pushOutlineUndo(undoEntry)
                    }
                },
                onOpenLinkedItem = onOpenLinkedItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else if (markdownEditMode) {
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
                    ) {
                        Text(
                            text = if (planningAiProviders.any { it.normalized().enabled }) {
                                "自由书写：像备忘录一样写一整段，点“识别”后优先使用已启用 AI，失败时回到本地规则；结果先进入预览。"
                            } else {
                                "自由书写：像备忘录一样写一整段，点“识别”后用本地规则解析；结果先进入预览，不会直接入库。"
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = editorValue,
                            onValueChange = { editorValue = autoContinuePlanningLine(editorValue, it) },
                            modifier = Modifier
                                .widthIn(min = 720.dp)
                                .fillMaxHeight(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            shape = RoundedCornerShape(18.dp),
                            placeholder = {
                                Text(
                                    text = "可以直接这样写：\n\n" +
                                        "10:00-12:00 写论文 @图书馆3楼\n" +
                                        "12:00-13:00 吃饭\n" +
                                        "任务M ddl 15:00\n" +
                                        "明天 16:30 交材料\n" +
                                        "- [ ] 整理保研材料\n" +
                                        "  - [ ] 打印成绩单",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            minLines = 16
                        )
                    }
                }
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
                },
                groups = groups,
                latestVisionImageUri = latestVisionImageUri,
                onBackToEditor = {
                    previewSheetVisible = false
                    markdownEditMode = true
                    editorValue = editorValue.copy(selection = TextRange(editorValue.text.length))
                }
            )
        }
    }

    if (markdownImportVisible && activeNote != null) {
        AlertDialog(
            onDismissRequest = { markdownImportVisible = false },
            title = { Text("从 Markdown 导入") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("会用当前文本替换此文档的大纲节点。原节点关联的待办 / 日程会同步删除后重建。")
                    OutlinedTextField(
                        value = markdownImportText,
                        onValueChange = { markdownImportText = it },
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        placeholder = { Text("- [ ] 入党资料要写完\n  - [ ] 填写个人信息表\n- [ ] 数据库复习 ddl 5.28 23:59") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = markdownImportText.isNotBlank(),
                    onClick = {
                        val noteId = activeNote.id
                        scope.launch {
                            val message = onReplaceNodesFromMarkdown(noteId, markdownImportText)
                            message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                            markdownImportVisible = false
                        }
                    }
                ) { Text("导入并替换") }
            },
            dismissButton = { TextButton(onClick = { markdownImportVisible = false }) { Text("取消") } }
        )
    }

    if (visionRecognizing) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("AI 识别中") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Text(
                        text = "AI 识别中…可能需要 10-30 秒",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {}
        )
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
    pendingDeleteNode?.let { node ->
        ConfirmPlanningDialog(
            title = "删除大纲事项？",
            message = "会同时删除它和子项关联的待办 / 日程：${node.text}",
            confirmText = "删除",
            onDismiss = { pendingDeleteNode = null },
            onConfirm = {
                scope.launch {
                    val undoEntry = captureOutlineUndo("删除节点：${node.text.compactUndoLabel()}")
                    val message = onDeleteNode(node)
                    message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                    if (message == null) pushOutlineUndo(undoEntry)
                    pendingDeleteNode = null
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlanningOutlineEditor(
    activeNote: PlanningNote?,
    nodes: List<PlanningNode>,
    outlineHintVisible: Boolean,
    previewMode: Boolean,
    highlightedPlanningNodeId: Long?,
    highlightedPlanningNodeSerial: Int,
    onCreateNode: (PlanningNodeDraft) -> Unit,
    onUpdateNode: (PlanningNode, PlanningNodeEdit) -> Unit,
    onToggleNode: (PlanningNode) -> Unit,
    onPublishNode: (PlanningNode) -> Unit,
    onDeleteNodeNow: (PlanningNode) -> Unit,
    onDeleteNode: (PlanningNode) -> Unit,
    onReorderNodes: (Long, Long?, List<Long>) -> Unit,
    onMergeNodeIntoPrevious: (PlanningNode, PlanningNode, PlanningNodeEdit) -> Unit,
    onOpenLinkedItem: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var rootInput by remember(activeNote?.id) { mutableStateOf(TextFieldValue("")) }
    var editingTarget by remember(activeNote?.id) { mutableStateOf<PlanningOutlineEditingTarget?>(null) }
    var pendingCreatedFocus by remember(activeNote?.id) { mutableStateOf<PlanningOutlinePendingNodeFocus?>(null) }
    var siblingInputAfterNodeId by remember(activeNote?.id) { mutableStateOf<Long?>(null) }
    var inputFocusTarget by remember(activeNote?.id) { mutableStateOf<String?>(null) }
    val childInputs = remember(activeNote?.id) { mutableStateMapOf<Long, TextFieldValue>() }
    val siblingInputs = remember(activeNote?.id) { mutableStateMapOf<Long, TextFieldValue>() }
    val expandedChildInputs = remember(activeNote?.id) { mutableStateMapOf<Long, Boolean>() }
    var timeDialogNode by remember { mutableStateOf<PlanningNode?>(null) }
    var locationDialogNode by remember { mutableStateOf<PlanningNode?>(null) }
    val flattened = remember(nodes) { flattenPlanningNodes(nodes) }
    val childrenByParent = remember(nodes) { nodes.groupBy { it.parentNodeId } }
    val listState = rememberLazyListState()
    var activeHighlightNodeId by remember(activeNote?.id) { mutableStateOf<Long?>(null) }
    var draggingNodeId by remember(activeNote?.id) { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember(activeNote?.id) { mutableStateOf(0f) }
    val reorderStepPx = with(LocalDensity.current) { 52.dp.toPx() }

    fun focusNode(node: PlanningNode?, cursor: Int? = null) {
        if (node == null) return
        editingTarget = PlanningOutlineEditingTarget(node.id, cursor ?: node.text.length)
        siblingInputAfterNodeId = null
        inputFocusTarget = null
    }

    fun siblingNodes(parentId: Long?): List<PlanningNode> {
        return nodes
            .filter { it.parentNodeId == parentId }
            .sortedWith(compareBy<PlanningNode> { it.sortOrder }.thenBy { it.id })
    }

    fun previousSibling(node: PlanningNode): PlanningNode? {
        val siblings = siblingNodes(node.parentNodeId)
        val index = siblings.indexOfFirst { it.id == node.id }
        return siblings.getOrNull(index - 1)
    }

    fun nextVisibleNode(node: PlanningNode): PlanningNode? {
        val index = flattened.indexOfFirst { it.node.id == node.id }
        return flattened.getOrNull(index + 1)?.node
    }

    fun previousVisibleNode(node: PlanningNode): PlanningNode? {
        val index = flattened.indexOfFirst { it.node.id == node.id }
        return flattened.getOrNull(index - 1)?.node
    }

    fun mergeTextIntoPrevious(previous: PlanningNode?, raw: String, onMerged: () -> Unit = {}) {
        val clean = raw.trimStart()
        if (previous == null || clean.isBlank()) return
        val cursor = previous.text.length
        onUpdateNode(previous, previous.toPlanningNodeEdit(text = previous.text + clean))
        onMerged()
        focusNode(previous, cursor)
    }

    fun mergeNodeIntoPrevious(node: PlanningNode, raw: String) {
        val previous = previousSibling(node) ?: return
        val clean = raw.trimStart()
        if (clean.isBlank()) return
        val cursor = previous.text.length
        onMergeNodeIntoPrevious(node, previous, previous.toPlanningNodeEdit(text = previous.text + clean))
        focusNode(previous, cursor)
    }

    fun focusRootInput(cursor: Int = rootInput.text.length) {
        editingTarget = null
        rootInput = rootInput.copy(selection = TextRange(cursor.coerceIn(0, rootInput.text.length)))
        inputFocusTarget = "root-${System.nanoTime()}"
    }

    fun moveNodeWithinSiblings(node: PlanningNode, direction: Int): Boolean {
        val note = activeNote ?: return false
        val siblings = siblingNodes(node.parentNodeId)
        val index = siblings.indexOfFirst { it.id == node.id }
        val targetIndex = (index + direction).coerceIn(0, siblings.lastIndex)
        if (index < 0 || index == targetIndex) return false
        val reordered = siblings.toMutableList().apply {
            add(targetIndex, removeAt(index))
        }
        onReorderNodes(note.id, node.parentNodeId, reordered.map { it.id })
        return true
    }

    LaunchedEffect(nodes, pendingCreatedFocus) {
        val pending = pendingCreatedFocus ?: return@LaunchedEffect
        val target = nodes
            .filter { candidate ->
                candidate.parentNodeId == pending.parentNodeId &&
                    candidate.text.trim() == pending.text.trim()
            }
            .let { candidates ->
                pending.sortOrder?.let { order ->
                    candidates.firstOrNull { it.sortOrder == order } ?: candidates.firstOrNull()
                } ?: candidates.firstOrNull()
            }
        if (target != null) {
            pendingCreatedFocus = null
            focusNode(target, pending.cursor)
        }
    }

    LaunchedEffect(highlightedPlanningNodeId, highlightedPlanningNodeSerial, nodes, activeNote?.id) {
        val targetId = highlightedPlanningNodeId ?: return@LaunchedEffect
        val target = nodes.firstOrNull { it.id == targetId } ?: return@LaunchedEffect
        val nodesById = nodes.associateBy { it.id }
        var parentId = target.parentNodeId
        var expandedAnyAncestor = false
        while (parentId != null) {
            val parent = nodesById[parentId] ?: break
            if (parent.collapsed) {
                expandedAnyAncestor = true
                onUpdateNode(parent, parent.toPlanningNodeEdit(collapsed = false))
            }
            parentId = parent.parentNodeId
        }
        if (expandedAnyAncestor) return@LaunchedEffect
        val targetIndex = flattened.indexOfFirst { it.node.id == targetId }
        if (targetIndex >= 0) {
            activeHighlightNodeId = targetId
            listState.animateScrollToItem(targetIndex)
            delay(3_000L)
            if (activeHighlightNodeId == targetId) {
                activeHighlightNodeId = null
            }
        }
    }

    fun createNode(parentId: Long?, raw: String, sortOrder: Int? = null) {
        val note = activeNote ?: return
        val text = raw.trim()
        if (text.isBlank()) return
        onCreateNode(
            PlanningNodeDraft(
                noteId = note.id,
                parentNodeId = parentId,
                text = text,
                sortOrder = sortOrder
            )
        )
    }

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (previewMode) {
                    "预览模式：草稿点纸飞机发布，正式节点可点 ⋯ 补充时间、地点或删除。"
                } else {
                    "像备忘录一样写：回车只生成草稿节点；点纸飞机或顶部发布按钮后才进入待办/日历。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeNote == null) {
                    item {
                        PlanningOutlineEmptyCard("请先新建或打开一个规划文档。")
                    }
                } else if (flattened.isEmpty()) {
                    item {
                        PlanningOutlineEmptyCard("写下第一件事，比如：入党资料要写完，或 10:00-12:00 写论文 图书馆3楼。")
                    }
                } else {
                    flattened.forEach { outline ->
                        val node = outline.node
                        val hasChildren = childrenByParent[node.id].orEmpty().isNotEmpty()
                        val childInputExpanded = expandedChildInputs[node.id] == true
                        val childInputVisible = !previewMode && (childInputExpanded || (hasChildren && !node.collapsed))
                        val dragging = draggingNodeId == node.id
                        item(key = node.id) {
                            Box(modifier = Modifier.animateItemPlacement()) {
                                PlanningOutlineRow(
                            item = outline,
                            hasChildren = hasChildren,
                            childInputVisible = childInputVisible,
                            outlineHintVisible = outlineHintVisible,
                            previewMode = previewMode,
                            highlighted = activeHighlightNodeId == node.id,
                            editing = editingTarget?.nodeId == node.id,
                            editingCursor = editingTarget?.takeIf { it.nodeId == node.id }?.cursor,
                            dragEnabled = !previewMode && editingTarget?.nodeId != node.id && siblingNodes(node.parentNodeId).size > 1,
                            dragging = dragging,
                            dragOffsetY = if (dragging) dragOffsetY else 0f,
                            onDragStart = {
                                focusManager.clearFocus()
                                editingTarget = null
                                draggingNodeId = node.id
                                dragOffsetY = 0f
                            },
                            onDragDelta = { deltaY ->
                                if (draggingNodeId != node.id) return@PlanningOutlineRow
                                dragOffsetY += deltaY
                                if (abs(dragOffsetY) >= reorderStepPx) {
                                    val direction = if (dragOffsetY > 0f) 1 else -1
                                    if (moveNodeWithinSiblings(node, direction)) {
                                        dragOffsetY = 0f
                                    } else {
                                        dragOffsetY = dragOffsetY.coerceIn(-reorderStepPx, reorderStepPx)
                                    }
                                }
                            },
                            onDragEnd = {
                                if (draggingNodeId == node.id) {
                                    draggingNodeId = null
                                    dragOffsetY = 0f
                                }
                            },
                            onStartEdit = { focusNode(node, node.text.length) },
                            onStopEdit = {
                                if (editingTarget?.nodeId == node.id) editingTarget = null
                            },
                            onToggle = { onToggleNode(node) },
                            onPublish = { onPublishNode(node) },
                            onToggleCollapse = {
                                if (hasChildren) {
                                    onUpdateNode(
                                        node,
                                        node.toPlanningNodeEdit(collapsed = !node.collapsed)
                                    )
                                } else {
                                    val nextExpanded = !childInputExpanded
                                    expandedChildInputs[node.id] = nextExpanded
                                    if (nextExpanded) inputFocusTarget = "child-${node.id}-${System.nanoTime()}"
                                }
                            },
                            onTextCommit = { text ->
                                if (text.isBlank()) {
                                    onDeleteNodeNow(node)
                                } else if (text.trim() != node.text.trim()) {
                                    onUpdateNode(node, node.toPlanningNodeEdit(text = text.trim()))
                                }
                            },
                            onDeleteAndFocusPrevious = {
                                val previous = previousVisibleNode(node)
                                onDeleteNodeNow(node)
                                focusNode(previous, previous?.text?.length)
                                true
                            },
                            onMergeWithPrevious = { text ->
                                if (previousSibling(node) == null) {
                                    false
                                } else {
                                    mergeNodeIntoPrevious(node, text)
                                    true
                                }
                            },
                            onCreateSibling = {
                                editingTarget = null
                                siblingInputAfterNodeId = node.id
                                siblingInputs[node.id] = TextFieldValue("")
                                inputFocusTarget = "sibling-${node.id}-${System.nanoTime()}"
                            },
                            onCreateSiblingWithText = { before, after ->
                                val cleanBefore = before.trim()
                                val cleanAfter = after.trim()
                                if (cleanBefore.isBlank()) {
                                    false
                                } else {
                                    if (cleanBefore != node.text.trim()) {
                                        onUpdateNode(node, node.toPlanningNodeEdit(text = cleanBefore))
                                    }
                                    editingTarget = null
                                    if (cleanAfter.isNotBlank()) {
                                        val nextSortOrder = node.sortOrder + 1
                                        pendingCreatedFocus = PlanningOutlinePendingNodeFocus(
                                            parentNodeId = node.parentNodeId,
                                            text = cleanAfter,
                                            sortOrder = nextSortOrder,
                                            cursor = 0
                                        )
                                        createNode(node.parentNodeId, cleanAfter, sortOrder = nextSortOrder)
                                    } else {
                                        siblingInputAfterNodeId = node.id
                                        siblingInputs[node.id] = TextFieldValue("")
                                        inputFocusTarget = "sibling-${node.id}-${System.nanoTime()}"
                                    }
                                    true
                                }
                            },
                            onFocusPrevious = {
                                val previous = previousVisibleNode(node)
                                if (previous != null) {
                                    focusNode(previous, previous.text.length)
                                    true
                                } else {
                                    false
                                }
                            },
                            onFocusNext = {
                                val next = nextVisibleNode(node)
                                if (next != null) {
                                    focusNode(next, next.text.length)
                                    true
                                } else {
                                    focusRootInput()
                                    true
                                }
                            },
                            onIndent = {
                                val index = flattened.indexOfFirst { it.node.id == node.id }
                                val previous = flattened.getOrNull(index - 1)?.node
                                if (previous != null) {
                                    onUpdateNode(node, node.toPlanningNodeEdit(parentNodeId = previous.id))
                                }
                            },
                            onOutdent = {
                                val parent = nodes.firstOrNull { it.id == node.parentNodeId }
                                if (node.parentNodeId != null) {
                                    onUpdateNode(node, node.toPlanningNodeEdit(parentNodeId = parent?.parentNodeId))
                                }
                            },
                            onToggleSync = {
                                onUpdateNode(node, node.toPlanningNodeEdit(syncEnabled = !node.syncEnabled))
                            },
                            onToggleNote = {
                                onUpdateNode(
                                    node,
                                    node.toPlanningNodeEdit(
                                        isNote = !node.isNote,
                                        syncEnabled = node.isNote
                                    )
                                )
                            },
                            onOpenLinkedItem = {
                                node.linkedTodoId?.let(onOpenLinkedItem)
                            },
                            onRequestTime = { timeDialogNode = node },
                            onRequestLocation = { locationDialogNode = node },
                                    onDelete = { onDeleteNode(node) }
                                )
                            }
                        }
                        if (!previewMode && siblingInputAfterNodeId == node.id) {
                            item(key = "sibling-input-${node.id}") {
                                PlanningOutlineInputLine(
                                    depth = outline.depth,
                                    value = siblingInputs[node.id] ?: TextFieldValue(""),
                                    placeholder = "继续写同级事项",
                                    enabled = true,
                                    autoFocusKey = inputFocusTarget.takeIf { it?.startsWith("sibling-${node.id}-") == true },
                                    onValueChange = { siblingInputs[node.id] = it },
                                    onCommit = {
                                        val raw = siblingInputs[node.id]?.text.orEmpty()
                                        if (raw.isBlank()) {
                                            siblingInputAfterNodeId = null
                                            siblingInputs.remove(node.id)
                                        } else {
                                            createNode(node.parentNodeId, raw, sortOrder = node.sortOrder + 1)
                                            siblingInputAfterNodeId = null
                                            siblingInputs.remove(node.id)
                                        }
                                    },
                                    onBackspaceEmpty = {
                                        siblingInputAfterNodeId = null
                                        siblingInputs.remove(node.id)
                                        focusNode(node, node.text.length)
                                        true
                                    },
                                    onBackspaceAtStart = { text ->
                                        mergeTextIntoPrevious(node, text) {
                                            siblingInputAfterNodeId = null
                                            siblingInputs.remove(node.id)
                                        }
                                        true
                                    },
                                    onArrowUp = {
                                        focusNode(node, node.text.length)
                                        true
                                    }
                                )
                            }
                        }
                        if (childInputVisible) {
                            item(key = "child-input-${node.id}") {
                                PlanningOutlineInputLine(
                                    depth = outline.depth + 1,
                                    value = childInputs[node.id] ?: TextFieldValue(""),
                                    placeholder = "输入 ${node.text} 的子任务",
                                    enabled = true,
                                    autoFocusKey = inputFocusTarget.takeIf { it?.startsWith("child-${node.id}-") == true },
                                    onValueChange = { childInputs[node.id] = it },
                                    onCommit = {
                                        val raw = childInputs[node.id]?.text.orEmpty()
                                        createNode(node.id, raw)
                                        childInputs[node.id] = TextFieldValue("")
                                    },
                                    onBackspaceEmpty = {
                                        childInputs.remove(node.id)
                                        if (!hasChildren) expandedChildInputs[node.id] = false
                                        focusNode(node, node.text.length)
                                        true
                                    },
                                    onBackspaceAtStart = { text ->
                                        mergeTextIntoPrevious(node, text) {
                                            childInputs.remove(node.id)
                                            if (!hasChildren) expandedChildInputs[node.id] = false
                                        }
                                        true
                                    },
                                    onArrowUp = {
                                        focusNode(node, node.text.length)
                                        true
                                    }
                                )
                            }
                        }
                    }
                }
                if (!previewMode) {
                    item(key = "root-input") {
                        PlanningOutlineInputLine(
                            depth = 0,
                            value = rootInput,
                            placeholder = "继续写下一行，按回车创建",
                            enabled = activeNote != null,
                            autoFocusKey = inputFocusTarget.takeIf { it?.startsWith("root-") == true },
                            onValueChange = { rootInput = it },
                            onCommit = {
                                createNode(null, rootInput.text)
                                rootInput = TextFieldValue("")
                            },
                            onBackspaceEmpty = {
                                val previous = flattened.lastOrNull()?.node
                                if (previous != null) {
                                    focusNode(previous, previous.text.length)
                                    true
                                } else {
                                    false
                                }
                            },
                            onBackspaceAtStart = { text ->
                                val previous = flattened.lastOrNull()?.node
                                mergeTextIntoPrevious(previous, text) {
                                    rootInput = TextFieldValue("")
                                }
                                previous != null
                            },
                            onArrowUp = {
                                val previous = flattened.lastOrNull()?.node
                                if (previous != null) {
                                    focusNode(previous, previous.text.length)
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    timeDialogNode?.let { node ->
        PlanningNodeTimeDialog(
            node = node,
            onDismiss = { timeDialogNode = null },
            onConfirm = { dueAt, startAt, endAt ->
                onUpdateNode(node, node.toPlanningNodeEdit(dueAt = dueAt, startAt = startAt, endAt = endAt))
                timeDialogNode = null
            }
        )
    }
    locationDialogNode?.let { node ->
        PlanningNodeLocationDialog(
            node = node,
            onDismiss = { locationDialogNode = null },
            onConfirm = { location ->
                onUpdateNode(node, node.toPlanningNodeEdit(location = location))
                locationDialogNode = null
            }
        )
    }
}

@Composable
private fun PlanningOutlineEmptyCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class PlanningOutlineEditingTarget(
    val nodeId: Long,
    val cursor: Int? = null
)

private data class PlanningOutlinePendingNodeFocus(
    val parentNodeId: Long?,
    val text: String,
    val sortOrder: Int?,
    val cursor: Int
)

@Composable
private fun PlanningOutlineInputLine(
    depth: Int,
    value: TextFieldValue,
    placeholder: String,
    enabled: Boolean,
    autoFocusKey: Any? = null,
    onValueChange: (TextFieldValue) -> Unit,
    onCommit: () -> Unit,
    onBackspaceEmpty: () -> Boolean = { false },
    onBackspaceAtStart: (String) -> Boolean = { false },
    onArrowUp: () -> Boolean = { false }
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(autoFocusKey) {
        if (autoFocusKey != null) {
            delay(80)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 22).dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(22.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val cursorAtStart = value.selection.start == 0 && value.selection.end == 0
                    when (event.key) {
                        Key.Enter -> {
                            onCommit()
                            true
                        }
                        Key.Backspace -> {
                            when {
                                value.text.isBlank() -> onBackspaceEmpty()
                                cursorAtStart -> onBackspaceAtStart(value.text)
                                else -> false
                            }
                        }
                        Key.DirectionUp -> onArrowUp()
                        else -> false
                    }
                },
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onCommit() }),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (focused) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun PlanningOutlineRow(
    item: PlanningOutlineItem,
    hasChildren: Boolean,
    childInputVisible: Boolean,
    outlineHintVisible: Boolean,
    previewMode: Boolean,
    highlighted: Boolean,
    editing: Boolean,
    editingCursor: Int?,
    dragEnabled: Boolean,
    dragging: Boolean,
    dragOffsetY: Float,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onStartEdit: () -> Unit,
    onStopEdit: () -> Unit,
    onToggle: () -> Unit,
    onPublish: () -> Unit,
    onToggleCollapse: () -> Unit,
    onTextCommit: (String) -> Unit,
    onDeleteAndFocusPrevious: () -> Boolean,
    onMergeWithPrevious: (String) -> Boolean,
    onCreateSibling: () -> Unit,
    onCreateSiblingWithText: (String, String) -> Boolean,
    onFocusPrevious: () -> Boolean,
    onFocusNext: () -> Boolean,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onToggleSync: () -> Unit,
    onToggleNote: () -> Unit,
    onOpenLinkedItem: () -> Unit,
    onRequestTime: () -> Unit,
    onRequestLocation: () -> Unit,
    onDelete: () -> Unit
) {
    val node = item.node
    val initialCursor = editingCursor?.coerceIn(0, node.text.length) ?: node.text.length
    var textValue by remember(node.id, node.text, editingCursor) {
        mutableStateOf(TextFieldValue(node.text, TextRange(initialCursor)))
    }
    var actionMenuExpanded by remember { mutableStateOf(false) }
    val editFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var editHadFocus by remember(node.id, editing) { mutableStateOf(false) }
    var editCommitHandled by remember(node.id, editing) { mutableStateOf(false) }
    val chipText = remember(node) { planningNodeMetaText(node) }
    val childToggleExpanded = if (hasChildren) !node.collapsed else childInputVisible
    val textColor = when {
        node.isNote -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
        node.completed -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
    }
    LaunchedEffect(editing, previewMode) {
        if (editing && !previewMode) {
            delay(80)
            editFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    fun commit() {
        if (editCommitHandled) return
        editCommitHandled = true
        val clean = textValue.text.trim()
        if (clean.isBlank()) {
            onDelete()
        } else if (clean != node.text.trim()) {
            onTextCommit(clean)
        }
        onStopEdit()
    }

    val rowContainerColor = if (highlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
    } else {
        Color.Transparent
    }
    val rowBorder = if (highlighted) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
    } else {
        null
    }
    val dragModifier = if (dragEnabled) {
        Modifier.pointerInput(node.id, dragEnabled) {
            detectDragGesturesAfterLongPress(
                onDragStart = { onDragStart() },
                onDragEnd = onDragEnd,
                onDragCancel = onDragEnd,
                onDrag = { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.y)
                }
            )
        }
    } else {
        Modifier
    }

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (outlineHintVisible && editing && !previewMode) {
            PlanningOutlineHeaderHint(textValue.text)
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = if (dragging) dragOffsetY else 0f
                    alpha = if (dragging) 0.82f else 1f
                    shadowElevation = if (dragging) 12f else 0f
                }
                .then(dragModifier),
            shape = RoundedCornerShape(16.dp),
            color = rowContainerColor,
            border = rowBorder
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Spacer(Modifier.width((item.depth * 22).dp))
                IconButton(
                    modifier = Modifier.size(30.dp),
                    onClick = onToggleCollapse
                ) {
                    Icon(
                        imageVector = if (childToggleExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (childToggleExpanded) "折叠子任务" else "展开子任务",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                if (node.isNote) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "//",
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = onToggle
                    ) {
                        Icon(
                            imageVector = if (node.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = if (node.completed) "标记未完成" else "完成",
                            tint = when {
                                node.completed -> MaterialTheme.colorScheme.onSurface
                                node.isDraft -> MaterialTheme.colorScheme.outline.copy(alpha = 0.48f)
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                    }
                }
                val textColumnModifier = if (!previewMode && !editing) {
                    Modifier
                        .weight(1f)
                        .clickable { onStartEdit() }
                } else {
                    Modifier.weight(1f)
                }
                Column(
                    modifier = textColumnModifier,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (editing && !previewMode) {
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(editFocusRequester)
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    val cursorStart = minOf(textValue.selection.start, textValue.selection.end)
                                    val cursorEnd = maxOf(textValue.selection.start, textValue.selection.end)
                                    val cursorAtStart = cursorStart == 0 && cursorEnd == 0
                                    val cursorAtEnd = cursorStart == textValue.text.length && cursorEnd == textValue.text.length
                                    when (event.key) {
                                        Key.Enter -> {
                                            val before = textValue.text.substring(0, cursorStart)
                                            val after = textValue.text.substring(cursorEnd)
                                            if (cursorStart in 1 until textValue.text.length || after.isNotBlank()) {
                                                if (onCreateSiblingWithText(before, after)) {
                                                    editCommitHandled = true
                                                    onStopEdit()
                                                    true
                                                } else {
                                                    false
                                                }
                                            } else {
                                                commit()
                                                onCreateSibling()
                                                true
                                            }
                                        }
                                        Key.Tab -> {
                                            if (event.isShiftPressed) onOutdent() else onIndent()
                                            true
                                        }
                                        Key.Backspace -> {
                                            when {
                                                textValue.text.isBlank() -> onDeleteAndFocusPrevious()
                                                cursorAtStart -> onMergeWithPrevious(textValue.text)
                                                else -> false
                                            }
                                        }
                                        Key.DirectionUp -> if (cursorAtStart) onFocusPrevious() else false
                                        Key.DirectionDown -> if (cursorAtEnd) onFocusNext() else false
                                        else -> false
                                    }
                                }
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        editHadFocus = true
                                    } else if (editing && editHadFocus) {
                                        commit()
                                    }
                                },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = textColor,
                                fontStyle = if (node.isNote) FontStyle.Italic else FontStyle.Normal,
                                textDecoration = if (node.completed) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { commit() }),
                            singleLine = true,
                            placeholder = { Text("写下事项、DDL 或日程") }
                        )
                    } else {
                        Text(
                            text = node.text,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = textColor,
                                fontStyle = if (node.isNote) FontStyle.Italic else FontStyle.Normal,
                                textDecoration = if (node.completed) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (chipText.isNotBlank()) {
                        Text(
                            text = chipText,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (node.isDraft) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (node.isDraft && !node.isNote) {
                    IconButton(
                        modifier = Modifier.size(34.dp),
                        onClick = onPublish
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "发布为正式事项",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (previewMode) {
                    androidx.compose.foundation.layout.Box {
                        IconButton(modifier = Modifier.size(34.dp), onClick = { actionMenuExpanded = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "节点设置")
                        }
                        DropdownMenu(
                            expanded = actionMenuExpanded,
                            onDismissRequest = { actionMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("完整编辑") },
                                onClick = {
                                    actionMenuExpanded = false
                                    onOpenLinkedItem()
                                },
                                enabled = node.linkedTodoId != null
                            )
                            DropdownMenuItem(
                                text = { Text("设置时间") },
                                onClick = {
                                    actionMenuExpanded = false
                                    onRequestTime()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("设置地点") },
                                onClick = {
                                    actionMenuExpanded = false
                                    onRequestLocation()
                                }
                            )
                            if (hasChildren) {
                                DropdownMenuItem(
                                    text = { Text("有子任务时保持结构标题") },
                                    onClick = {},
                                    enabled = false
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(if (node.syncEnabled) "改为结构标题" else "同步为待办/日程") },
                                    onClick = {
                                        actionMenuExpanded = false
                                        onToggleSync()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (node.isNote) "取消备注" else "标记为备注") },
                                onClick = {
                                    actionMenuExpanded = false
                                    onToggleNote()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("删除") },
                                onClick = {
                                    actionMenuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanningOutlineHeaderHint(text: String) {
    val hasTime = Regex("\\d{1,2}[:：]\\d{2}|ddl|截止|今天|明天|后天").containsMatchIn(text)
    val hasLocation = Regex("@\\S+|地点[:：]").containsMatchIn(text)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlanningOutlineHintToken("时间", active = hasTime)
        Text("|", color = MaterialTheme.colorScheme.outline)
        PlanningOutlineHintToken("事项", active = text.isNotBlank())
        Text("|", color = MaterialTheme.colorScheme.outline)
        PlanningOutlineHintToken("地点", active = hasLocation)
    }
}

@Composable
private fun PlanningOutlineHintToken(label: String, active: Boolean) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
private fun PlanningNodeTimeDialog(
    node: PlanningNode,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime?, LocalDateTime?, LocalDateTime?) -> Unit
) {
    var dueText by rememberSaveable(node.id, node.dueAtMillis) {
        mutableStateOf(node.dueAtMillis?.toPlanningNodeLocalDateTime()?.let(::planningEditableDateTime).orEmpty())
    }
    var startText by rememberSaveable(node.id, node.startAtMillis) {
        mutableStateOf(node.startAtMillis?.toPlanningNodeLocalDateTime()?.let(::planningEditableDateTime).orEmpty())
    }
    var endText by rememberSaveable(node.id, node.endAtMillis) {
        mutableStateOf(node.endAtMillis?.toPlanningNodeLocalDateTime()?.let(::planningEditableDateTime).orEmpty())
    }
    val dueAt = parsePlanningEditableDateTime(dueText)
    val startAt = parsePlanningEditableDateTime(startText)
    val endAt = parsePlanningEditableDateTime(endText)
    val dueInvalid = dueText.isNotBlank() && dueAt == null
    val startInvalid = startText.isNotBlank() && startAt == null
    val endInvalid = endText.isNotBlank() && endAt == null
    val eventRangeInvalid = (startAt == null) xor (endAt == null) ||
        (startAt != null && endAt != null && !endAt.isAfter(startAt))
    val valid = !dueInvalid && !startInvalid && !endInvalid && !eventRangeInvalid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置时间") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "只填 DDL 表示待办；填开始和结束表示日程。时间支持 16:30、明天 16:30、5.28 14:30。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = dueText,
                    onValueChange = { dueText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("DDL") },
                    placeholder = { Text("明天 16:30") },
                    singleLine = true,
                    isError = dueInvalid
                )
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("开始") },
                    placeholder = { Text("10:00") },
                    singleLine = true,
                    isError = startInvalid || eventRangeInvalid
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("结束") },
                    placeholder = { Text("12:00") },
                    singleLine = true,
                    isError = endInvalid || eventRangeInvalid
                )
                if (dueInvalid || startInvalid || endInvalid) {
                    Text("时间格式无法识别。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else if (eventRangeInvalid) {
                    Text("日程必须同时设置开始和结束，且结束晚于开始。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val normalizedDue = dueAt.takeIf { startAt == null && endAt == null }
                    onConfirm(normalizedDue, startAt, endAt)
                }
            ) { Text("保存") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onConfirm(null, null, null) }) { Text("清除时间") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

@Composable
private fun PlanningNodeLocationDialog(
    node: PlanningNode,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var value by rememberSaveable(node.id, node.location) { mutableStateOf(node.location.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置地点") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("地点") },
                placeholder = { Text("@图书馆3楼") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim().ifBlank { null }) }) { Text("保存") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onConfirm(null) }) { Text("清除地点") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
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
        PlanningShortcutSpec("子任务", Icons.AutoMirrored.Rounded.PlaylistAdd, PlanningShortcutAction.SubtaskLine, "在当前行下面新建一条缩进子任务"),
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
            title = "1. 像备忘录一样一行一行写",
            subtitle = "自由书写 → 识别 → 预览确认",
            lines = listOf(
                "规划台默认是自由书写区，不需要一开始就选日期、分组或提醒。",
                "你可以像备忘录一样整段写：课程、DDL、想办的事、子任务都先放进同一份文档。",
                "长行可以横向检查；手机粘贴多行文本也可以先放进这里再整理。",
                "点顶部“识别”后才会解析，结果先进入预览，不会直接写入待办或日历。",
                "没有 DDL 的普通事项导入后会成为无 DDL 待办，统一显示在今日待办。"
            ),
            example = """
                入党资料要写完
                数据库复习
                给导师发消息
            """.trimIndent()
        ),
        PlanningTutorialPage(
            title = "2. 时间、地点和子任务",
            subtitle = "按自然顺序写",
            lines = listOf(
                "日程推荐写法：时间, 事件名, 地点；逗号、分号或明显地点词都可以辅助识别。",
                "DDL 待办可以写：任务M ddl 15:00、交材料 截止明天 23:59。",
                "大任务下面的小任务可以用缩进表达；识别预览会保留父任务线索。",
                "如果你想用更结构化的节点编辑，可以点顶部“大纲”切换到草稿节点模式。"
            ),
            example = """
                10:00-12:00, 写论文, 图书馆3楼
                任务M ddl 15:00
                保研材料
                  扫描获奖证明
                  汇总课程成绩
            """.trimIndent()
        ),
        PlanningTutorialPage(
            title = "3. 预览配置和 Markdown 兼容",
            subtitle = "粗写之后再精修",
            lines = listOf(
                "自由书写区的“预览”只负责查看 Markdown 渲染；真正导入前还要点“识别”。",
                "识别预览里可以勾选、编辑标题、DDL、日程时间、地点、分组、提醒和重复规则。",
                "如果配置了 AI，识别会优先尝试 AI；失败或未配置时会回到本地规则解析。",
                "公告可写 #公告；图片、语音、系统分享会先进入规划台，仍然需要你检查后再发布。",
                "大纲模式适合逐条草稿管理：节点右侧纸飞机发布，顶部“发布N条”可批量发布。"
            ),
            example = """
                #公告 5.16-7.1 期间禁止游玩舞萌DX游戏
                - [ ] 交材料 #ddl 5.28 23:59 #remind 30,5
            """.trimIndent()
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
    val conflictMapping = mapping?.takeIf { it.status == MappingStatus.CONFLICT }
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
                conflictMapping?.let {
                    PlanningConflictActions(it.id, onApplyConflictDocument, onApplyConflictItem)
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
    val conflictMapping = mapping?.takeIf { it.status == MappingStatus.CONFLICT }
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
            conflictMapping?.let {
                PlanningConflictActions(it.id, onApplyConflictDocument, onApplyConflictItem)
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
    onImport: () -> Unit,
    groups: List<TaskGroup>,
    latestVisionImageUri: Uri?,
    onBackToEditor: () -> Unit
) {
    val context = LocalContext.current
    val invalidSelected = candidates.any { candidate -> selectedIds[candidate.id] == true && candidate.validate() != null }
    val selectedCount = candidates.count { selectedIds[it.id] == true }
    val validCandidates = candidates.filter { it.validate() == null }
    var batchExpanded by remember { mutableStateOf(false) }
    var imagePreviewDialogVisible by remember { mutableStateOf(false) }
    val previewBitmap = remember(latestVisionImageUri, context) { latestVisionImageUri?.loadPreviewBitmap(context) }
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
        if (latestVisionImageUri != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                onClick = { if (previewBitmap != null) imagePreviewDialogVisible = true }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "已保留本次识图来源，点缩略图可放大核对。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    previewBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "本次识图原图缩略图",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
        if (imagePreviewDialogVisible && previewBitmap != null) {
            AlertDialog(
                onDismissRequest = { imagePreviewDialogVisible = false },
                title = { Text("识图原图") },
                text = {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "本次识图原图放大预览",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        contentScale = ContentScale.Fit
                    )
                },
                confirmButton = {
                    TextButton(onClick = { imagePreviewDialogVisible = false }) {
                        Text("关闭")
                    }
                }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    validCandidates.forEach { selectedIds[it.id] = true }
                },
                enabled = validCandidates.isNotEmpty()
            ) { Text("全选可导入项") }
            TextButton(onClick = { candidates.forEach { selectedIds[it.id] = false } }, enabled = selectedCount > 0) { Text("全不选") }
            TextButton(onClick = onBackToEditor) { Text("返回编辑器修改") }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
            onClick = { batchExpanded = !batchExpanded }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("批量设置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = if (batchExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (batchExpanded) {
                    val selectedCandidates = candidates.filter { selectedIds[it.id] == true }
                    val selectedCountdownCandidates = selectedCandidates.filter { candidate ->
                        candidate.type == PlanningParsedType.EVENT ||
                            (candidate.type == PlanningParsedType.TODO && candidate.dueAt != null)
                    }
                    val selectedEventCandidates = selectedCandidates.filter { it.type == PlanningParsedType.EVENT }
                    PlanningBatchSwitchRow(
                        title = "全部加入倒数日",
                        summary = "影响已勾选的日程，以及带 DDL 的待办候选",
                        checked = selectedCountdownCandidates.isNotEmpty() && selectedCountdownCandidates.all { it.countdownEnabled },
                        enabled = selectedCountdownCandidates.isNotEmpty(),
                        onCheckedChange = { checked ->
                            selectedCountdownCandidates.forEach { candidate ->
                                onCandidateChange(candidate.copy(countdownEnabled = checked))
                            }
                        }
                    )
                    PlanningBatchSwitchRow(
                        title = "全部创建待办",
                        summary = "只影响已勾选的日程候选",
                        checked = selectedEventCandidates.isNotEmpty() && selectedEventCandidates.all { it.createLinkedTodo },
                        enabled = selectedEventCandidates.isNotEmpty(),
                        onCheckedChange = { checked ->
                            selectedEventCandidates.forEach { candidate ->
                                onCandidateChange(candidate.copy(createLinkedTodo = checked))
                            }
                        }
                    )
                    PlanningBatchSwitchRow(
                        title = "全部启用打卡",
                        summary = "只影响已勾选的日程候选",
                        checked = selectedEventCandidates.isNotEmpty() && selectedEventCandidates.all { it.checkInEnabled },
                        enabled = selectedEventCandidates.isNotEmpty(),
                        onCheckedChange = { checked ->
                            selectedEventCandidates.forEach { candidate ->
                                onCandidateChange(candidate.copy(checkInEnabled = checked))
                            }
                        }
                    )
                    if (groups.isNotEmpty()) {
                        Text(
                            text = "统一分组",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            groups.forEach { group ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        selectedCandidates.forEach { candidate ->
                                            onCandidateChange(candidate.copy(groupName = group.name))
                                        }
                                    },
                                    label = { Text(group.name) }
                                )
                            }
                        }
                    }
                }
            }
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
private fun PlanningBatchSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
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
        if (candidate.type == PlanningParsedType.EVENT) {
            FilterChip(
                selected = candidate.checkInEnabled,
                onClick = { onCandidateChange(candidate.copy(checkInEnabled = !candidate.checkInEnabled)) },
                label = { Text("打卡追踪") }
            )
        }
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

private fun PlanningImportCandidate.toPlanningNodeDraft(noteId: Long): PlanningNodeDraft {
    val fallbackText = sourceLine.ifBlank { title }
    return when (type) {
        PlanningParsedType.EVENT -> PlanningNodeDraft(
            noteId = noteId,
            text = title.ifBlank { fallbackText },
            notes = notes,
            groupName = groupName,
            startAt = startAt,
            endAt = endAt,
            location = location.takeIf { it.isNotBlank() },
            reminderOffsetsMinutes = normalizedReminderOffsets(),
            allDay = allDay,
            countdownEnabled = countdownEnabled,
            checkInEnabled = checkInEnabled,
            isDraft = true
        )
        PlanningParsedType.TODO -> PlanningNodeDraft(
            noteId = noteId,
            text = title.ifBlank { fallbackText },
            notes = notes,
            groupName = groupName,
            dueAt = dueAt,
            location = location.takeIf { it.isNotBlank() },
            reminderOffsetsMinutes = normalizedReminderOffsets(),
            countdownEnabled = countdownEnabled,
            isDraft = true
        )
        else -> PlanningNodeDraft(noteId = noteId, text = fallbackText, isDraft = true)
    }
}

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

private const val PlanningNodeUndoMaxSize = 20

private data class PlanningNodeUndoEntry(
    val description: String,
    val snapshot: PlanningNodeSnapshot
)

private fun String.compactUndoLabel(maxLength: Int = 12): String {
    val clean = trim().replace(Regex("\\s+"), " ")
    return if (clean.length <= maxLength) clean else clean.take(maxLength) + "…"
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

private data class PlanningVisionImagePayload(
    val base64: String,
    val mimeType: String = "image/jpeg"
)

private class PlanningVisionImageException(message: String) : IOException(message)

private fun Uri.loadPreviewBitmap(context: Context): Bitmap? {
    return runCatching {
        context.contentResolver.openInputStream(this)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }.getOrNull()
}

private suspend fun compressPlanningVisionImage(
    context: Context,
    uri: Uri
): PlanningVisionImagePayload = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: throw PlanningVisionImageException("图片过大，请裁剪后重试")
    val originalLongSide = max(bounds.outWidth, bounds.outHeight)
    if (originalLongSide <= 0) throw PlanningVisionImageException("图片过大，请裁剪后重试")

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = calculatePlanningVisionSampleSize(originalLongSide, PlanningVisionMaxLongSide)
    }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        ?: throw PlanningVisionImageException("图片过大，请裁剪后重试")
    val scaled = scalePlanningVisionBitmap(decoded)
    if (scaled !== decoded) decoded.recycle()

    val bytes = ByteArrayOutputStream().use { output ->
        val ok = scaled.compress(Bitmap.CompressFormat.JPEG, PlanningVisionJpegQuality, output)
        scaled.recycle()
        if (!ok) throw PlanningVisionImageException("图片过大，请裁剪后重试")
        output.toByteArray()
    }
    if (bytes.isEmpty() || bytes.size > PlanningVisionMaxEncodedBytes) {
        throw PlanningVisionImageException("图片过大，请裁剪后重试")
    }
    PlanningVisionImagePayload(Base64.encodeToString(bytes, Base64.NO_WRAP))
}

private fun calculatePlanningVisionSampleSize(originalLongSide: Int, targetLongSide: Int): Int {
    var sample = 1
    while (originalLongSide / (sample * 2) >= targetLongSide) {
        sample *= 2
    }
    return sample.coerceAtLeast(1)
}

private fun scalePlanningVisionBitmap(bitmap: Bitmap): Bitmap {
    val longSide = max(bitmap.width, bitmap.height)
    if (longSide <= PlanningVisionMaxLongSide) return bitmap
    val ratio = PlanningVisionMaxLongSide.toFloat() / longSide.toFloat()
    val width = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
    val height = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
}

private fun appendPlanningMarkdown(current: String, appended: String): String {
    val base = current.trimEnd()
    val addition = appended.trim()
    if (base.isBlank()) return addition
    return "$base\n\n$addition"
}

private const val PlanningVisionMaxLongSide = 1600
private const val PlanningVisionJpegQuality = 80
private const val PlanningVisionMaxEncodedBytes = 4 * 1024 * 1024

private const val PlanningVisionUserPrompt = "请识别图中的所有日程。"

private val PlanningVisionSystemPrompt = """
    你是一个课表/日程识图助手。用户给你一张课表或日程截图，请你识别其中所有的课程或日程，按以下格式逐行输出 Markdown，不要任何解释，不要 code fence：

    H:MM-HH:MM, 课程名, @地点
    H:MM-HH:MM, 课程名, @地点

    规则：
    1. 一行一个日程
    2. 时间段用 24 小时制，连字符分隔
    3. 地点前加 @，地点不存在就省略 @ 段
    4. 课程名保持简洁，去掉教师名等冗余信息
    5. 如果识别到的日程跨多个工作日（例如每周一三五），在每行末尾加 "（每周X）" 或 "（周一/三/五）" 等中文标注
    6. 看不清的内容用 "?" 占位，不要凭空编造
""".trimIndent()

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

private data class PlanningOutlineItem(
    val node: PlanningNode,
    val depth: Int
)

private fun flattenPlanningNodes(nodes: List<PlanningNode>): List<PlanningOutlineItem> {
    if (nodes.isEmpty()) return emptyList()
    val childrenByParent = nodes
        .groupBy { it.parentNodeId }
        .mapValues { (_, children) -> children.sortedWith(compareBy<PlanningNode> { it.sortOrder }.thenBy { it.id }) }
    val result = mutableListOf<PlanningOutlineItem>()
    val visiting = mutableSetOf<Long>()

    fun append(node: PlanningNode, depth: Int) {
        if (!visiting.add(node.id)) return
        result += PlanningOutlineItem(node = node, depth = depth.coerceAtMost(8))
        if (!node.collapsed) {
            childrenByParent[node.id].orEmpty().forEach { child -> append(child, depth + 1) }
        }
        visiting.remove(node.id)
    }

    childrenByParent[null].orEmpty().forEach { append(it, 0) }
    return result
}

private fun PlanningNode.toPlanningNodeEdit(
    text: String = this.text,
    parentNodeId: Long? = this.parentNodeId,
    sortOrder: Int = this.sortOrder,
    dueAt: LocalDateTime? = dueAtMillis?.toPlanningNodeLocalDateTime(),
    startAt: LocalDateTime? = startAtMillis?.toPlanningNodeLocalDateTime(),
    endAt: LocalDateTime? = endAtMillis?.toPlanningNodeLocalDateTime(),
    location: String? = this.location,
    isNote: Boolean = this.isNote,
    syncEnabled: Boolean = this.syncEnabled,
    collapsed: Boolean = this.collapsed,
    completed: Boolean = this.completed
): PlanningNodeEdit {
    return PlanningNodeEdit(
        text = text,
        parentNodeId = parentNodeId,
        sortOrder = sortOrder,
        dueAt = dueAt,
        startAt = startAt,
        endAt = endAt,
        location = location,
        isNote = isNote,
        syncEnabled = syncEnabled,
        collapsed = collapsed,
        completed = completed
    )
}

private fun planningNodeMetaText(node: PlanningNode): String {
    val location = node.location?.trim().orEmpty()
    val draft = "草稿".takeIf { node.isDraft }
    if (node.isNote) {
        return listOf(draft, "备注", location.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" · ")
    }
    if (!node.syncEnabled) {
        return listOf(draft, "结构标题", location.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" · ")
    }
    val time = when {
        node.startAtMillis != null && node.endAtMillis != null -> {
            val start = node.startAtMillis.toPlanningNodeLocalDateTime()
            val end = node.endAtMillis.toPlanningNodeLocalDateTime()
            if (start.toLocalDate() == end.toLocalDate()) {
                "日程 ${start.format(DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA))}-${end.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA))}"
            } else {
                "日程 ${start.format(DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA))} → ${end.format(DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA))}"
            }
        }
        node.dueAtMillis != null -> {
            "DDL ${node.dueAtMillis.toPlanningNodeLocalDateTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA))}"
        }
        else -> ""
    }
    return listOf(draft, time, location.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" · ")
}

private fun Long.toPlanningNodeLocalDateTime(): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
}
