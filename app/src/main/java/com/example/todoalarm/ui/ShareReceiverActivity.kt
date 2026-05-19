package com.example.todoalarm.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.PlanningImportCandidate
import com.example.todoalarm.data.PlanningParsedType
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

class ShareReceiverActivity : ComponentActivity() {
    private val viewModel: TodoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoAlarmTheme {
                ShareReceiverScreen(
                    intent = intent,
                    app = application as TodoApplication,
                    viewModel = viewModel,
                    onFinish = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_PREVIEW_ID = "capture_preview_id"

        fun previewIntent(context: Context, previewId: String): Intent {
            return Intent(context, ShareReceiverActivity::class.java).apply {
                action = ACTION_OPEN_PREVIEW
                putExtra(EXTRA_PREVIEW_ID, previewId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        private const val ACTION_OPEN_PREVIEW = "com.paykitodo.ACTION_OPEN_CAPTURE_PREVIEW"
    }
}

@Composable
private fun ShareReceiverScreen(
    intent: Intent?,
    app: TodoApplication,
    viewModel: TodoViewModel,
    onFinish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var importing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<CapturePlanningPreview?>(null) }
    val candidates = remember { mutableStateListOf<PlanningImportCandidate>() }
    val selectedIds = remember { mutableStateMapOf<String, Boolean>() }
    val context = LocalContext.current

    LaunchedEffect(intent) {
        loading = true
        error = null
        runCatching {
            val previewId = intent?.getStringExtra(ShareReceiverActivity.EXTRA_PREVIEW_ID)
            val resolved = if (!previewId.isNullOrBlank()) {
                CapturePlanningStore.get(previewId)
                    ?: throw IllegalArgumentException("捕获结果已失效，请重新识别")
            } else {
                recognizeSharedIntent(app, context, intent)
            }
            if (resolved.importableCount <= 0) {
                throw IllegalArgumentException("未能识别出待办或日程")
            }
            preview = resolved
            candidates.clear()
            selectedIds.clear()
            resolved.candidates.forEach { candidate ->
                candidates += candidate
                selectedIds[candidate.id] = candidate.importable && candidate.validate() == null
            }
        }.onFailure { throwable ->
            val message = throwable.message ?: "识别失败"
            error = message
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onFinish()
        }
        loading = false
    }

    CapturePreviewContent(
        loading = loading,
        importing = importing,
        error = error,
        preview = preview,
        candidates = candidates,
        selectedIds = selectedIds,
        onDismiss = onFinish,
        onToggle = { id, selected -> selectedIds[id] = selected },
        onImport = {
            val currentPreview = preview ?: return@CapturePreviewContent
            scope.launch {
                importing = true
                val result = viewModel.importCapturePreview(
                    preview = currentPreview,
                    candidates = candidates.toList(),
                    selectedIds = selectedIds.filterValues { it }.keys
                )
                importing = false
                Toast.makeText(
                    app,
                    result.message ?: "已添加 ${result.importedCount} 条",
                    Toast.LENGTH_SHORT
                ).show()
                if (result.message == null) {
                    CapturePlanningStore.remove(currentPreview.id)
                    onFinish()
                }
            }
        }
    )
}

private suspend fun recognizeSharedIntent(
    app: TodoApplication,
    context: Context,
    intent: Intent?
): CapturePlanningPreview {
    val action = intent?.action
    val type = intent?.type.orEmpty()
    if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) {
        throw IllegalArgumentException("没有可识别的内容")
    }
    val text = extractSharedText(context, intent)
    if (text.isNotBlank() && (type.isBlank() || type.startsWith("text/"))) {
        return CapturePlanningPipeline.recognizeText(
            app = app,
            markdown = text,
            title = "分享文本",
            appendToActiveNote = true
        )
    }
    val imageUris = extractSharedImageUris(intent)
    if (imageUris.isNotEmpty()) {
        return CapturePlanningPipeline.recognizeImages(
            app = app,
            uris = imageUris,
            title = "分享图片"
        )
    }
    throw IllegalArgumentException("没有可识别的内容")
}

private fun extractSharedText(context: Context, intent: Intent): String {
    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { if (it.isNotBlank()) return it }
    val clip = intent.clipData ?: return ""
    return buildString {
        for (index in 0 until clip.itemCount) {
            clip.getItemAt(index).coerceToText(context)?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let {
                if (isNotEmpty()) append('\n')
                append(it)
            }
        }
    }
}

private fun extractSharedImageUris(intent: Intent): List<Uri> {
    val uris = mutableListOf<Uri>()
    intent.parcelableExtraCompat(Intent.EXTRA_STREAM, Uri::class.java)?.let { uris += it }
    intent.parcelableArrayListExtraCompat(Intent.EXTRA_STREAM, Uri::class.java)?.let { uris += it }
    val clip = intent.clipData
    if (clip != null) {
        for (index in 0 until clip.itemCount) {
            clip.getItemAt(index).uri?.let { uris += it }
        }
    }
    return uris.distinct()
}

private fun <T : Parcelable> Intent.parcelableExtraCompat(name: String, clazz: Class<T>): T? {
    @Suppress("DEPRECATION")
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, clazz)
    } else {
        getParcelableExtra(name)
    }
}

private fun <T : Parcelable> Intent.parcelableArrayListExtraCompat(name: String, clazz: Class<T>): ArrayList<T>? {
    @Suppress("DEPRECATION")
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(name, clazz)
    } else {
        getParcelableArrayListExtra(name)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CapturePreviewContent(
    loading: Boolean,
    importing: Boolean,
    error: String?,
    preview: CapturePlanningPreview?,
    candidates: List<PlanningImportCandidate>,
    selectedIds: Map<String, Boolean>,
    onDismiss: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onImport: () -> Unit
) {
    val selectedCount = selectedIds.count { it.value }
    val invalidSelected = candidates.any { candidate ->
        selectedIds[candidate.id] == true && candidate.validate() != null
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(preview?.title ?: "添加到 PaykiTodo") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "关闭")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                ) { Text("取消") }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !loading && !importing && selectedCount > 0 && !invalidSelected,
                    onClick = onImport
                ) { Text(if (importing) "导入中" else "导入 $selectedCount 条") }
            }
        }
    ) { padding ->
        when {
            loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("正在识别...")
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDismiss) { Text("关闭") }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    preview?.parseResult?.message?.takeIf { it.isNotBlank() }?.let { message ->
                        item {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    items(candidates, key = { it.id }) { candidate ->
                        CaptureCandidateCard(
                            candidate = candidate,
                            selected = selectedIds[candidate.id] == true,
                            onSelectedChange = { onToggle(candidate.id, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureCandidateCard(
    candidate: PlanningImportCandidate,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    val error = candidate.validate()
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = if (candidate.importable && error == null) onSelectedChange else null
            )
            Icon(
                imageVector = if (candidate.type == PlanningParsedType.EVENT) Icons.Rounded.Event else Icons.Rounded.TaskAlt,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when (candidate.type) {
                    PlanningParsedType.EVENT -> MaterialTheme.colorScheme.tertiary
                    PlanningParsedType.TODO -> MaterialTheme.colorScheme.primary
                    PlanningParsedType.ERROR -> MaterialTheme.colorScheme.error
                    PlanningParsedType.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = candidate.title.ifBlank { candidate.sourceLine },
                    color = if (error == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = candidateMetaText(candidate),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else if (candidate.message.isNotBlank()) {
                    Text(candidate.message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun candidateMetaText(candidate: PlanningImportCandidate): String {
    val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    return when (candidate.type) {
        PlanningParsedType.TODO -> buildString {
            append("待办")
            candidate.dueAt?.let { append(" · DDL ${it.format(formatter)}") }
            candidate.groupName.takeIf { it.isNotBlank() }?.let { append(" · $it") }
        }
        PlanningParsedType.EVENT -> buildString {
            append("日程")
            if (candidate.startAt != null && candidate.endAt != null) {
                append(" · ${candidate.startAt.format(formatter)}-${candidate.endAt.toLocalTime()}")
            }
            candidate.location.takeIf { it.isNotBlank() }?.let { append(" · $it") }
        }
        PlanningParsedType.SKIPPED -> "跳过 · ${candidate.message}"
        PlanningParsedType.ERROR -> "错误 · ${candidate.message}"
    }
}
