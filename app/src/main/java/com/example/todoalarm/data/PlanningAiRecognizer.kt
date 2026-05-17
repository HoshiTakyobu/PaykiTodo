package com.example.todoalarm.data

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object PlanningAiRecognizer {
    suspend fun recognize(
        markdown: String,
        providers: List<PlanningAiProvider>,
        now: LocalDateTime = LocalDateTime.now()
    ): PlanningParseResult {
        val response = PlanningAiCaller.callWithFallback(
            providers = providers,
            request = PlanningAiRequest(
                systemPrompt = buildSystemPrompt(now),
                prompt = buildUserPrompt(markdown, now)
            )
        )
        return parseAiContent(
            content = response.content,
            originalMarkdown = markdown,
            providerName = response.provider.name.ifBlank { response.provider.model },
            now = now
        )
    }

    internal fun parseAiContent(
        content: String,
        originalMarkdown: String,
        providerName: String = "AI",
        now: LocalDateTime = LocalDateTime.now()
    ): PlanningParseResult {
        val jsonValue = parseJsonValue(content)
        val items = when (jsonValue) {
            is JSONObject -> jsonValue.optJSONArray("items")
                ?: JSONArray().put(jsonValue)
            is JSONArray -> jsonValue
            else -> throw IllegalArgumentException("AI 返回不是 JSON 对象或数组。")
        }
        val lines = originalMarkdown.replace("\r\n", "\n").replace('\r', '\n').lines()
        val candidates = buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                add(parseAiItem(item, index, lines, providerName, now))
            }
        }
        if (candidates.isEmpty()) {
            throw IllegalArgumentException("AI 没有返回可预览的规划条目。")
        }
        return PlanningParseResult(
            candidates = candidates,
            message = "AI 识别：$providerName；请在预览中确认"
        )
    }

    private fun parseAiItem(
        item: JSONObject,
        index: Int,
        originalLines: List<String>,
        providerName: String,
        now: LocalDateTime
    ): PlanningParsedCandidate {
        val lineNumber = item.optInt("lineNumber", index + 1).coerceAtLeast(1)
        val fallbackLine = originalLines.getOrNull(lineNumber - 1).orEmpty()
        val sourceLine = item.optString("sourceLine").ifBlank { fallbackLine }.ifBlank {
            item.optString("title").ifBlank { "AI 识别条目 ${index + 1}" }
        }
        if (sourceLine.contains("#imported")) {
            return PlanningParsedCandidate(
                id = "ai-$index",
                lineNumber = lineNumber,
                sourceLine = sourceLine,
                type = PlanningParsedType.SKIPPED,
                imported = true,
                message = "已带 #imported，默认跳过。"
            )
        }

        val typeText = item.optString("type").lowercase(Locale.ROOT)
        val type = when (typeText) {
            "todo", "task", "待办", "任务" -> PlanningParsedType.TODO
            "event", "schedule", "calendar", "日程", "事件" -> PlanningParsedType.EVENT
            "skip", "skipped", "跳过" -> PlanningParsedType.SKIPPED
            else -> PlanningParsedType.ERROR
        }
        if (type == PlanningParsedType.ERROR) {
            return PlanningParsedCandidate(
                id = "ai-$index",
                lineNumber = lineNumber,
                sourceLine = sourceLine,
                type = PlanningParsedType.ERROR,
                message = "AI 返回了无法识别的条目类型：${item.optString("type")}"
            )
        }

        val title = item.optString("title").trim()
        val notes = item.optString("notes").trim()
        val groupName = sanitizeAiGroupName(
            rawGroupName = item.optString("groupName").ifBlank { item.optString("group") },
            sourceLine = sourceLine
        )
        val dueAt = parseAiDateTime(item.optNullableString("dueAt"), defaultTime = LocalTime.of(23, 59))
        val startAt = parseAiDateTime(item.optNullableString("startAt"))
        val endAt = parseAiDateTime(item.optNullableString("endAt"))
        val reminderOffsets = parseReminderOffsets(item, type, dueAt, startAt)
        val messages = mutableListOf("AI 识别结果，建议确认")
        item.optString("message").takeIf { it.isNotBlank() }?.let { messages += it.trim() }
        providerName.takeIf { it.isNotBlank() }?.let { messages += "来源：$it" }

        if (type == PlanningParsedType.SKIPPED) {
            return PlanningParsedCandidate(
                id = "ai-$index",
                lineNumber = lineNumber,
                sourceLine = sourceLine,
                type = PlanningParsedType.SKIPPED,
                title = title,
                message = item.optString("message").ifBlank { "AI 判断该条暂不导入。" }
            )
        }

        val structuralError = when {
            title.isBlank() -> "标题不能为空。"
            type == PlanningParsedType.EVENT && startAt == null -> "日程开始时间不能为空。"
            type == PlanningParsedType.EVENT && endAt == null -> "日程结束时间不能为空。"
            type == PlanningParsedType.EVENT && startAt != null && endAt != null && !endAt.isAfter(startAt) -> "日程结束时间必须晚于开始时间。"
            else -> null
        }
        if (structuralError != null) {
            return PlanningParsedCandidate(
                id = "ai-$index",
                lineNumber = lineNumber,
                sourceLine = sourceLine,
                type = PlanningParsedType.ERROR,
                title = title,
                notes = notes,
                groupName = groupName,
                dueAt = dueAt,
                startAt = startAt,
                endAt = endAt,
                message = structuralError
            )
        }

        val importBlocked = when (type) {
            PlanningParsedType.TODO -> dueAt != null &&
                (!dueAt.isAfter(now) || reminderOffsets.any { !dueAt.minusMinutes(it.toLong()).isAfter(now) })
            PlanningParsedType.EVENT -> startAt != null &&
                reminderOffsets.any { !startAt.minusMinutes(it.toLong()).isAfter(now) }
            else -> false
        }
        if (importBlocked) messages += "提醒时间或 DDL 已经过期，请调整后再导入。"

        return PlanningParsedCandidate(
            id = "ai-$index",
            lineNumber = lineNumber,
            sourceLine = sourceLine,
            type = type,
            title = title,
            notes = notes,
            groupName = groupName,
            dueAt = if (type == PlanningParsedType.TODO) dueAt else null,
            startAt = if (type == PlanningParsedType.EVENT) startAt else null,
            endAt = if (type == PlanningParsedType.EVENT) endAt else null,
            reminderOffsetsMinutes = reminderOffsets,
            createLinkedTodo = if (type == PlanningParsedType.EVENT) item.optBoolean("createLinkedTodo", true) else false,
            importBlocked = importBlocked,
            message = messages.distinct().joinToString("；")
        )
    }

    private fun parseReminderOffsets(
        item: JSONObject,
        type: PlanningParsedType,
        dueAt: LocalDateTime?,
        startAt: LocalDateTime?
    ): List<Int> {
        val array = item.optJSONArray("reminderOffsetsMinutes")
        val parsed = if (array == null) {
            emptyList()
        } else {
            buildList {
                for (index in 0 until array.length()) {
                    val value = array.optInt(index, -1)
                    if (value >= 0) add(value)
                }
            }
        }
        if (parsed.isNotEmpty()) return parsed.distinct().sortedDescending()
        return when {
            type == PlanningParsedType.TODO && dueAt != null -> listOf(DEFAULT_PLANNING_REMINDER_MINUTES)
            type == PlanningParsedType.EVENT && startAt != null -> listOf(DEFAULT_PLANNING_REMINDER_MINUTES)
            else -> emptyList()
        }
    }

    private fun sanitizeAiGroupName(rawGroupName: String, sourceLine: String): String {
        val groupName = rawGroupName.trim()
        if (groupName.isBlank()) return ""
        val source = sourceLine.trim()
        if (Regex("""#(?:group|分组)(?=\s|$)""", RegexOption.IGNORE_CASE).containsMatchIn(source)) return groupName
        val escaped = Regex.escape(groupName)
        val explicitPattern = Regex("""(?:分组|项目|课程)\s*[:：]\s*$escaped(?:\s|$|，|,|；|;)""")
        return if (explicitPattern.containsMatchIn(source)) groupName else ""
    }

    private fun parseAiDateTime(raw: String?, defaultTime: LocalTime? = null): LocalDateTime? {
        val text = raw?.trim()?.trim('"')?.takeIf {
            it.isNotBlank() && !it.equals("null", ignoreCase = true)
        } ?: return null
        runCatching { return LocalDateTime.parse(text) }
        runCatching { return LocalDateTime.parse(text.replace(' ', 'T')) }
        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm",
            "yyyy.M.d HH:mm",
            "yyyy-M-d H:mm",
            "yyyy/M/d H:mm"
        )
        patterns.forEach { pattern ->
            runCatching {
                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern, Locale.CHINA))
            }
        }
        if (defaultTime != null) {
            val datePatterns = listOf("yyyy-MM-dd", "yyyy/M/d", "yyyy.M.d")
            datePatterns.forEach { pattern ->
                runCatching {
                    return LocalDateTime.of(LocalDate.parse(text, DateTimeFormatter.ofPattern(pattern, Locale.CHINA)), defaultTime)
                }
            }
        }
        return null
    }

    private fun parseJsonValue(raw: String): Any {
        val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
        val text = fenced ?: raw.trim()
        val start = text.indexOfFirst { it == '{' || it == '[' }
        if (start < 0) throw IllegalArgumentException("AI 返回中没有 JSON。")
        return JSONTokener(text.substring(start)).nextValue()
    }

    private fun JSONObject.optNullableString(name: String): String? {
        if (!has(name) || isNull(name)) return null
        return optString(name).takeIf { it.isNotBlank() }
    }

    private fun buildUserPrompt(markdown: String, now: LocalDateTime): String {
        return """
            当前时间：${now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}

            用户写下的规划内容如下。请把它拆成待办和日程候选：

            <planning_text>
            $markdown
            </planning_text>
        """.trimIndent()
    }

    private fun buildSystemPrompt(now: LocalDateTime): String {
        val today = now.toLocalDate()
        return """
            你是 PaykiTodo 规划台的结构化识别器。你的任务是把用户随手写的中文规划、备忘、课程安排、DDL、会议、复习计划拆成可预览候选。

            必须只输出 JSON，不要输出 Markdown、解释、寒暄或代码块。今天是 $today，当前时间是 ${now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}。

            输出格式：
            {"items":[{"type":"todo","lineNumber":1,"sourceLine":"原文片段","title":"标题","notes":"","groupName":"","dueAt":"2026-05-28T23:59:00","startAt":null,"endAt":null,"reminderOffsetsMinutes":[5],"createLinkedTodo":false,"message":"根据自然文本推断，建议确认"}]}

            字段规则：
            1. type 只能是 "todo"、"event" 或 "skip"。
            2. todo 表示待办任务；title 必填；dueAt 可以为 null。用户只写日期但没写时间时，dueAt 用当天 23:59:00。没有 DDL 就 dueAt=null 且 reminderOffsetsMinutes=[]。
            3. event 表示有开始和结束时间的日程；startAt/endAt 必填，格式为 yyyy-MM-dd'T'HH:mm:ss。event 默认 createLinkedTodo=true，表示导入时可同时创建一个 DDL 等于日程结束时间的待办。
            4. reminderOffsetsMinutes 表示提前多少分钟提醒。用户没写提醒时，有 DDL 或日程的候选默认 [5]；没有 DDL 的 todo 用 []。
            5. groupName 只在用户明确写了分组标记时填写，例如“#group 入党”“分组：课程”“项目：保研”。不要从标题里擅自截取词语当分组；例如“入党表格填写”的 groupName 必须是空字符串，标题保持“入党表格填写”。
            6. notes 用于保留上下文、父任务、地点、资料等补充信息；不要把标题重复放进 notes。
            7. 对“每天、每周、每月”等循环表达，暂时不要创建循环规则，只在 message 里写“检测到循环关键词，如需循环请导入后在待办编辑器中设置”。
            8. 已完成、已导入、明显只是标题/说明的内容可以输出 type="skip"。
            9. 不要编造用户没写的具体日期时间；如果无法确定具体时间，todo 的 dueAt 设为 null，event 不要输出或输出 skip。
            10. 相对日期要按今天 $today 解析，例如“今天/明天/后天/周五”。“晚上交材料”如有明确日期上下文，可以按 22:00；“上午”按 12:00，“下午”按 17:00，“早上”按 09:00。
        """.trimIndent()
    }
}
