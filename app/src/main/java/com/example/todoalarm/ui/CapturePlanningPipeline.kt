package com.example.todoalarm.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.ReminderDispatchTracker
import com.example.todoalarm.data.PlanningAiCaller
import com.example.todoalarm.data.PlanningAiVisionRequest
import com.example.todoalarm.data.PlanningImportCandidate
import com.example.todoalarm.data.PlanningNodeDraft
import com.example.todoalarm.data.PlanningNote
import com.example.todoalarm.data.PlanningParsedType
import com.example.todoalarm.data.PlanningParseResult
import com.example.todoalarm.data.PlanningRecognitionService
import com.example.todoalarm.data.toPlanningImportCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDate
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

data class CapturePlanningPreview(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val sourceMarkdown: String,
    val currentMarkdown: String,
    val activeNoteId: Long,
    val parseResult: PlanningParseResult,
    val candidates: List<PlanningImportCandidate>
) {
    val importableCount: Int
        get() = candidates.count { it.importable && it.validate() == null }
}

data class CapturePlanningInsertResult(
    val noteId: Long,
    val importedCount: Int,
    val skippedCount: Int,
    val title: String
)

object CapturePlanningStore {
    private val previews = linkedMapOf<String, CapturePlanningPreview>()

    @Synchronized
    fun put(preview: CapturePlanningPreview): CapturePlanningPreview {
        previews[preview.id] = preview
        while (previews.size > MaxPreviewCount) {
            val oldest = previews.keys.firstOrNull() ?: break
            previews.remove(oldest)
        }
        return preview
    }

    @Synchronized
    fun get(id: String): CapturePlanningPreview? = previews[id]

    @Synchronized
    fun remove(id: String) {
        previews.remove(id)
    }

    private const val MaxPreviewCount = 12
}

object CapturePlanningPipeline {
    suspend fun captureTextToNodes(
        app: TodoApplication,
        markdown: String,
        title: String = "捕获内容",
        appendToActiveNote: Boolean = true
    ): CapturePlanningInsertResult {
        val preview = recognizeText(
            app = app,
            markdown = markdown,
            title = title,
            appendToActiveNote = appendToActiveNote
        )
        return insertPreviewAsNodes(app, preview)
    }

    suspend fun capturePlanningNoteTextToNodes(
        app: TodoApplication,
        noteId: Long?,
        markdown: String,
        title: String = "规划台识别"
    ): CapturePlanningInsertResult {
        val preview = recognizePlanningNoteText(
            app = app,
            noteId = noteId,
            markdown = markdown,
            title = title
        )
        return insertPreviewAsNodes(app, preview)
    }

    suspend fun captureImagesToNodes(
        app: TodoApplication,
        uris: List<Uri>,
        title: String = "图片捕获"
    ): CapturePlanningInsertResult {
        val preview = recognizeImages(app = app, uris = uris, title = title)
        return insertPreviewAsNodes(app, preview)
    }

    suspend fun captureImageToNodes(
        app: TodoApplication,
        uri: Uri,
        title: String = "图片捕获"
    ): CapturePlanningInsertResult {
        return captureImagesToNodes(app = app, uris = listOf(uri), title = title)
    }

    suspend fun recognizeText(
        app: TodoApplication,
        markdown: String,
        title: String = "捕获内容",
        appendToActiveNote: Boolean = true
    ): CapturePlanningPreview {
        val cleanMarkdown = markdown
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()
        require(cleanMarkdown.isNotBlank()) { "没有可识别的内容" }

        val note = activePlanningNote(app)
        val currentMarkdown = if (appendToActiveNote) {
            appendPlanningMarkdown(note.contentMarkdown, cleanMarkdown)
        } else {
            cleanMarkdown
        }
        val parseMarkdown = if (appendToActiveNote) {
            offsetPlanningMarkdown(note.contentMarkdown, cleanMarkdown)
        } else {
            cleanMarkdown
        }
        val documentDate = note.documentDateEpochDay?.let(LocalDate::ofEpochDay)
        val result = PlanningRecognitionService.recognize(
            markdown = parseMarkdown,
            settings = app.settingsStore.currentSettings(),
            defaultDate = documentDate
        )
        val candidates = result.candidates.map { it.toPlanningImportCandidate() }
        return CapturePlanningPreview(
            title = title,
            sourceMarkdown = cleanMarkdown,
            currentMarkdown = currentMarkdown,
            activeNoteId = note.id,
            parseResult = result,
            candidates = candidates
        )
    }

    suspend fun recognizePlanningNoteText(
        app: TodoApplication,
        noteId: Long?,
        markdown: String,
        title: String = "规划台识别"
    ): CapturePlanningPreview {
        val normalizedMarkdown = markdown
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        require(normalizedMarkdown.isNotBlank()) { "没有可识别的内容" }

        val note = noteId
            ?.let { app.repository.getPlanningNote(it) }
            ?.takeIf { !it.archived }
            ?: activePlanningNote(app)
        val documentDate = note.documentDateEpochDay?.let(LocalDate::ofEpochDay)
        val result = PlanningRecognitionService.recognize(
            markdown = normalizedMarkdown,
            settings = app.settingsStore.currentSettings(),
            defaultDate = documentDate
        )
        val candidates = result.candidates.map { it.toPlanningImportCandidate() }
        app.settingsStore.updateLastOpenedPlanningNoteId(note.id)
        return CapturePlanningPreview(
            title = title,
            sourceMarkdown = normalizedMarkdown,
            currentMarkdown = normalizedMarkdown,
            activeNoteId = note.id,
            parseResult = result,
            candidates = candidates
        )
    }

    suspend fun recognizeImages(
        app: TodoApplication,
        uris: List<Uri>,
        title: String = "图片捕获"
    ): CapturePlanningPreview {
        require(uris.isNotEmpty()) { "没有可识别的图片" }
        val recognized = uris.mapIndexed { index, uri ->
            recognizeImageMarkdown(
                context = app.applicationContext,
                app = app,
                uri = uri,
                ordinal = index + 1
            )
        }.filter { it.isNotBlank() }
        require(recognized.isNotEmpty()) { "未能从图中识别出日程" }
        return recognizeText(
            app = app,
            markdown = recognized.joinToString("\n"),
            title = title,
            appendToActiveNote = true
        )
    }

    suspend fun recognizeImage(
        app: TodoApplication,
        uri: Uri,
        title: String = "图片捕获"
    ): CapturePlanningPreview {
        return recognizeImages(app = app, uris = listOf(uri), title = title)
    }

    private suspend fun recognizeImageMarkdown(
        context: Context,
        app: TodoApplication,
        uri: Uri,
        ordinal: Int
    ): String {
        val settings = app.settingsStore.currentSettings()
        val providers = settings.planningAiProviders
            .map { it.normalized() }
            .filter { provider ->
                provider.enabled &&
                    provider.supportsVision &&
                    provider.baseUrl.isNotBlank() &&
                    provider.apiKey.isNotBlank() &&
                    provider.model.isNotBlank()
            }
        require(providers.isNotEmpty()) { "请先在 AI 设置中启用支持图片识别的来源" }
        val encodedImage = encodeCaptureImage(context, uri)
        val response = PlanningAiCaller.callVisionWithFallback(
            providers = providers,
            request = PlanningAiVisionRequest(
                systemPrompt = CaptureVisionSystemPrompt,
                prompt = if (ordinal <= 1) CaptureVisionUserPrompt else "$CaptureVisionUserPrompt\n这是第 $ordinal 张图片。",
                imageBase64 = encodedImage.base64,
                imageMimeType = encodedImage.mimeType
            )
        )
        return response.content
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("```") }
            .joinToString("\n")
            .trim()
    }

    private suspend fun activePlanningNote(app: TodoApplication): PlanningNote {
        val settings = app.settingsStore.currentSettings()
        val current = settings.lastOpenedPlanningNoteId
            ?.let { app.repository.getPlanningNote(it) }
            ?.takeIf { !it.archived }
        val note = current ?: app.repository.ensureDefaultPlanningNote()
        app.settingsStore.updateLastOpenedPlanningNoteId(note.id)
        return note
    }

    private suspend fun insertPreviewAsNodes(
        app: TodoApplication,
        preview: CapturePlanningPreview
    ): CapturePlanningInsertResult {
        val note = app.repository.getPlanningNote(preview.activeNoteId)
            ?.takeIf { !it.archived }
            ?: activePlanningNote(app)
        val importable = preview.candidates.filter { candidate ->
            candidate.importable && candidate.validate() == null
        }
        require(importable.isNotEmpty()) { "未能识别出待办或日程" }
        var inserted = 0
        importable.forEach { candidate ->
            val result = app.repository.createPlanningNode(
                candidate.toPlanningNodeDraft(note.id),
                createEventEndTodo = app.settingsStore.currentSettings().planningEventEndTodoEnabled
            ) ?: return@forEach
            result.affectedLinkedItems.forEach { linked ->
                if (linked.completed || linked.canceled) {
                    app.alarmScheduler.cancel(linked.id)
                    app.reminderNotifier.cancel(linked.id)
                    ReminderDispatchTracker.clear(app, linked.id)
                } else {
                    ReminderDispatchTracker.clear(app, linked.id)
                    val scheduleMessage = app.alarmScheduler.schedule(linked)
                    if (scheduleMessage != null) {
                        app.repository.updateTodo(linked.copy(reminderEnabled = false))
                    }
                }
            }
            inserted += 1
        }
        require(inserted > 0) { "未能识别出待办或日程" }
        app.settingsStore.updateLastOpenedPlanningNoteId(note.id)
        autoBackupIfEnabled(app)
        return CapturePlanningInsertResult(
            noteId = note.id,
            importedCount = inserted,
            skippedCount = preview.candidates.size - inserted,
            title = preview.title
        )
    }

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
                checkInEnabled = checkInEnabled
            )
            PlanningParsedType.TODO -> PlanningNodeDraft(
                noteId = noteId,
                text = title.ifBlank { fallbackText },
                notes = notes,
                groupName = groupName,
                dueAt = dueAt,
                location = location.takeIf { it.isNotBlank() },
                reminderOffsetsMinutes = normalizedReminderOffsets(),
                countdownEnabled = countdownEnabled
            )
            else -> PlanningNodeDraft(noteId = noteId, text = fallbackText)
        }
    }

    private suspend fun autoBackupIfEnabled(app: TodoApplication) {
        val settings = app.settingsStore.currentSettings()
        if (!settings.autoBackupEnabled) return
        val directoryUri = settings.backupDirectoryUri ?: return
        withContext(Dispatchers.IO) {
            val snapshot = app.repository.exportSnapshot(settings)
            app.backupManager.autoBackupToDirectory(directoryUri, snapshot)
        }
    }

    private fun appendPlanningMarkdown(current: String, appended: String): String {
        val base = current.trimEnd()
        val addition = appended.trim()
        if (base.isBlank()) return addition
        return "$base\n\n$addition"
    }

    private fun offsetPlanningMarkdown(current: String, appended: String): String {
        val addition = appended.trim()
        if (current.trimEnd().isBlank()) return addition
        val existingLineCount = current.trimEnd()
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .size
        return buildString {
            repeat(existingLineCount + 1) { append('\n') }
            append(addition)
        }
    }
}

data class CaptureImagePayload(
    val base64: String,
    val mimeType: String = "image/jpeg"
)

class CaptureImageException(message: String) : IOException(message)

suspend fun encodeCaptureImage(
    context: Context,
    uri: Uri
): CaptureImagePayload = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: throw CaptureImageException("图片过大，请裁剪后重试")
    val originalLongSide = max(bounds.outWidth, bounds.outHeight)
    if (originalLongSide <= 0) throw CaptureImageException("图片过大，请裁剪后重试")

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = captureSampleSize(originalLongSide, CaptureVisionMaxLongSide)
    }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        ?: throw CaptureImageException("图片过大，请裁剪后重试")
    val scaled = scaleCaptureBitmap(decoded)
    if (scaled !== decoded) decoded.recycle()

    val bytes = ByteArrayOutputStream().use { output ->
        val ok = scaled.compress(Bitmap.CompressFormat.JPEG, CaptureVisionJpegQuality, output)
        scaled.recycle()
        if (!ok) throw CaptureImageException("图片过大，请裁剪后重试")
        output.toByteArray()
    }
    if (bytes.isEmpty() || bytes.size > CaptureVisionMaxEncodedBytes) {
        throw CaptureImageException("图片过大，请裁剪后重试")
    }
    CaptureImagePayload(Base64.encodeToString(bytes, Base64.NO_WRAP))
}

private fun captureSampleSize(originalLongSide: Int, targetLongSide: Int): Int {
    var sample = 1
    while (originalLongSide / (sample * 2) >= targetLongSide) {
        sample *= 2
    }
    return sample.coerceAtLeast(1)
}

private fun scaleCaptureBitmap(bitmap: Bitmap): Bitmap {
    val longSide = max(bitmap.width, bitmap.height)
    if (longSide <= CaptureVisionMaxLongSide) return bitmap
    val ratio = CaptureVisionMaxLongSide.toFloat() / longSide.toFloat()
    val width = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
    val height = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
}

private const val CaptureVisionMaxLongSide = 1600
private const val CaptureVisionJpegQuality = 80
private const val CaptureVisionMaxEncodedBytes = 4 * 1024 * 1024
private const val CaptureVisionUserPrompt = "请识别图中的所有日程。"

private val CaptureVisionSystemPrompt = """
    你是一个课表/日程识图助手。用户给你一张课表、纸质计划或日程截图，请识别其中所有可进入 PaykiTodo 的待办或日程，按以下格式逐行输出 Markdown，不要任何解释，不要 code fence：

    H:MM-HH:MM, 课程名或日程名, @地点
    任务标题 ddl 5.28 23:59
    普通待办事项

    规则：
    1. 一行一个待办或日程。
    2. 时间段用 24 小时制，连字符分隔。
    3. 地点前加 @，地点不存在就省略 @ 段。
    4. 看不清的内容用 "?" 占位，不要凭空编造。
    5. 如果识别到跨多个工作日，在行尾用中文标注，例如“（每周一/三/五）”。
""".trimIndent()
