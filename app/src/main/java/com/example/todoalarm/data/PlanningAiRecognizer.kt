package com.example.todoalarm.data

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.DayOfWeek
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
                message = "这一行已经导入过，默认跳过。"
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

        val rawTitle = item.optString("title").trim()
        val notes = item.optString("notes").trim()
        val rawLocation = item.optString("location").trim()
        val location = if (type == PlanningParsedType.EVENT) {
            normalizeAiLocation(rawLocation.ifBlank { extractAiInlineLocation(sourceLine).ifBlank { extractAiInlineLocation(rawTitle) } })
        } else {
            ""
        }
        val groupName = sanitizeAiGroupName(
            rawGroupName = item.optString("groupName").ifBlank { item.optString("group") },
            sourceLine = sourceLine
        )
        val dueAt = parseAiDateTime(item.optNullableString("dueAt"), defaultTime = LocalTime.of(23, 59))
        val startAt = parseAiDateTime(item.optNullableString("startAt"))
        val endAt = parseAiDateTime(item.optNullableString("endAt"))
        val rawCleanedTitle = if (type == PlanningParsedType.EVENT) {
            cleanAiEventTitle(rawTitle, location)
        } else {
            rawTitle
        }
        val reminderAnchor = if (type == PlanningParsedType.EVENT) startAt else dueAt
        val titleReminder = parseAiReminderClause(rawCleanedTitle, reminderAnchor, now)
        val sourceReminder = parseAiReminderClause(sourceLine, reminderAnchor, now)
        val title = titleReminder.title.ifBlank { rawCleanedTitle }.trim()
        val explicitReminderOffsets = titleReminder.reminderOffsetsMinutes.ifEmpty { sourceReminder.reminderOffsetsMinutes }
        val reminderOffsets = explicitReminderOffsets.ifEmpty { parseReminderOffsets(item, type, dueAt, startAt) }
        val recurrence = parseAiRecurrence(item.optJSONObject("recurrence"), if (type == PlanningParsedType.TODO) dueAt else startAt)
        val allDay = item.optBoolean("allDay", false)
        val countdownEnabled = item.optBoolean("countdownEnabled", false)
        val checkInEnabled = item.optBoolean("checkInEnabled", false)
        val messages = mutableListOf("AI 识别结果，建议确认")
        item.optString("message").takeIf { it.isNotBlank() }?.let { messages += it.trim() }
        titleReminder.message.takeIf { it.isNotBlank() }?.let { messages += it }
        if (titleReminder.message.isBlank()) {
            sourceReminder.message.takeIf { it.isNotBlank() }?.let { messages += it }
        }
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
                location = location,
                groupName = groupName,
                dueAt = dueAt,
                startAt = startAt,
                endAt = endAt,
                allDay = allDay,
                countdownEnabled = countdownEnabled,
                checkInEnabled = checkInEnabled,
                recurrence = recurrence,
                message = structuralError
            )
        }

        val importBlocked = when (type) {
            PlanningParsedType.TODO -> dueAt != null &&
                (!dueAt.isAfter(now) || reminderOffsets.any { !dueAt.minusMinutes(it.toLong()).isAfter(now) })
            else -> false
        }
        val pastEventReminder = type == PlanningParsedType.EVENT && startAt != null &&
            reminderOffsets.any { !startAt.minusMinutes(it.toLong()).isAfter(now) }
        if (importBlocked) messages += "提醒时间或 DDL 已经过期，请调整后再导入。"
        if (pastEventReminder) messages += "已过去的提醒不会补发；导入后只安排未来提醒。"

        return PlanningParsedCandidate(
            id = "ai-$index",
            lineNumber = lineNumber,
            sourceLine = sourceLine,
            type = type,
            title = title,
            notes = notes,
            location = if (type == PlanningParsedType.EVENT) location else "",
            groupName = groupName,
            dueAt = if (type == PlanningParsedType.TODO) dueAt else null,
            startAt = if (type == PlanningParsedType.EVENT) startAt else null,
            endAt = if (type == PlanningParsedType.EVENT) endAt else null,
            allDay = if (type == PlanningParsedType.EVENT) allDay else false,
            countdownEnabled = countdownEnabled && (type == PlanningParsedType.EVENT || dueAt != null),
            checkInEnabled = if (type == PlanningParsedType.EVENT) checkInEnabled else false,
            reminderOffsetsMinutes = reminderOffsets,
            recurrence = recurrence,
            createLinkedTodo = if (type == PlanningParsedType.EVENT) item.optBoolean("createLinkedTodo", false) else false,
            importBlocked = importBlocked,
            message = messages.distinct().joinToString("；")
        )
    }

    private fun extractAiInlineLocation(text: String): String {
        AiQuotedLocationRegex.find(text)?.groupValues?.getOrNull(1)?.let { return normalizeAiLocation(it) }
        return AiInlineLocationRegex.find(text)?.groupValues?.getOrNull(1)?.let(::normalizeAiLocation).orEmpty()
    }

    private fun cleanAiEventTitle(title: String, location: String): String {
        if (location.isBlank()) return title.trim()
        return title
            .replace(AiQuotedLocationRegex, " ")
            .replace(AiInlineLocationRegex, " ")
            .replace(location, " ")
            .trim()
            .trim(',', '，', '；', ';')
            .replace(Regex("\\s+"), " ")
    }

    private fun normalizeAiLocation(raw: String): String {
        return raw.trim()
            .trim('"', '\'', '“', '”', '‘', '’', ',', '，', '；', ';')
            .replace(Regex("^@{2,}"), "@")
    }

    private fun parseAiReminderClause(text: String, anchor: LocalDateTime?, now: LocalDateTime): AiReminderClause {
        val match = AiReminderClauseRegex.find(text) ?: return AiReminderClause(title = text)
        val title = text.substring(0, match.range.first)
            .trim()
            .trim(',', '，', '；', ';')
        val reminderRaw = match.groupValues[1].trim()
        if (anchor == null || reminderRaw.isBlank()) {
            return AiReminderClause(title = title)
        }
        val parsed = parseReminderTextInput(
            raw = reminderRaw,
            anchor = anchor,
            now = now,
            requireFuture = false
        )
        return if (parsed.isValid) {
            AiReminderClause(
                title = title,
                reminderOffsetsMinutes = parsed.offsetsMinutes,
                message = "已从原文识别提醒：${parsed.message}"
            )
        } else {
            AiReminderClause(title = title)
        }
    }

    private fun parseAiRecurrence(json: JSONObject?, anchor: LocalDateTime?): RecurrenceConfig {
        if (json == null || !json.optBoolean("enabled", false)) return RecurrenceConfig()
        val type = RecurrenceType.fromStorage(json.optString("type"))
        if (type == RecurrenceType.NONE) return RecurrenceConfig()
        val weekdays = json.optJSONArray("weeklyDays")?.let { array ->
            buildSet {
                for (index in 0 until array.length()) {
                    runCatching { DayOfWeek.of(array.optInt(index)) }.getOrNull()?.let(::add)
                }
            }
        }.orEmpty()
        val endDate = json.optNullableString("endDate")?.let { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        }
        return RecurrenceConfig(
            enabled = true,
            type = type,
            weeklyDays = if (type == RecurrenceType.WEEKLY && weekdays.isEmpty()) {
                anchor?.dayOfWeek?.let { setOf(it) }.orEmpty()
            } else {
                weekdays
            },
            endDate = endDate
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
        val escaped = Regex.escape(groupName)
        val explicitPattern = Regex("""(?:分组|项目|类别|课程)\s*[:：]\s*$escaped(?:\s|$|，|,|；|;)""")
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

    private val AiQuotedLocationRegex = Regex("[\"“”'‘’](@[^\"“”'‘’#]+)[\"“”'‘’]")
    private val AiInlineLocationRegex = Regex("(?:^|[\\s,，；;])(@[^\\s#\"'“”‘’，,；;]+)")
    private val AiReminderClauseRegex = Regex("(?:^|[\\s,，；;])(?:提醒(?:时间)?|remind(?:er)?)\\s*[:：]?\\s*([\\s\\S]+)$", RegexOption.IGNORE_CASE)

    private data class AiReminderClause(
        val title: String,
        val reminderOffsetsMinutes: List<Int> = emptyList(),
        val message: String = ""
    )

    private fun JSONObject.optNullableString(name: String): String? {
        if (!has(name) || isNull(name)) return null
        return optString(name).takeIf { it.isNotBlank() }
    }

    private fun buildUserPrompt(markdown: String, now: LocalDateTime): String {
        return """
            当前时间：${now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}（${weekdayLabel(now.toLocalDate())}）
            参照日期锚点：今天=${now.toLocalDate()}（${weekdayLabel(now.toLocalDate())}），明天=${now.toLocalDate().plusDays(1)}（${weekdayLabel(now.toLocalDate().plusDays(1))}），后天=${now.toLocalDate().plusDays(2)}（${weekdayLabel(now.toLocalDate().plusDays(2))}）。
            规划文档名称或日期不作为默认日期；只按用户正文里写出的“今天/明天/后天/周五/下周三/5.29/具体日期”等信息，相对今天这个锚点来推算真实日期。

            用户写下的规划内容如下。请把它拆成待办和日程候选：

            <planning_text>
            $markdown
            </planning_text>
        """.trimIndent()
    }

    private fun weekdayLabel(date: LocalDate): String {
        return when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "周一"
            DayOfWeek.TUESDAY -> "周二"
            DayOfWeek.WEDNESDAY -> "周三"
            DayOfWeek.THURSDAY -> "周四"
            DayOfWeek.FRIDAY -> "周五"
            DayOfWeek.SATURDAY -> "周六"
            DayOfWeek.SUNDAY -> "周日"
        }
    }

    private fun buildSystemPrompt(now: LocalDateTime): String {
        val today = now.toLocalDate()
        val todayWeekday = weekdayLabel(today)
        val documentDateRule = "规划文档名称或日期不提供默认日期。没有显式日期时，日程可按今天 $today 预览；待办没有 DDL 就 dueAt=null。"
        return """
            你是 PaykiTodo 规划台的结构化识别器。你的任务是把用户随手写的中文规划、备忘、课程安排、DDL、会议、复习计划拆成可预览候选。

            必须只输出 JSON，不要输出 Markdown、解释、寒暄或代码块。今天是 $today（$todayWeekday），当前时间是 ${now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}。
            $documentDateRule

            输出格式：
            {"items":[{"type":"todo","lineNumber":1,"sourceLine":"原文片段","title":"标题","notes":"","location":"","groupName":"","dueAt":"2026-05-28T23:59:00","startAt":null,"endAt":null,"allDay":false,"countdownEnabled":false,"reminderOffsetsMinutes":[5],"recurrence":{"enabled":false,"type":"NONE","weeklyDays":[],"endDate":null},"createLinkedTodo":false,"message":"根据自然文本推断，建议确认"}]}

            字段规则：
            1. type 只能是 "todo"、"event" 或 "skip"。
            2. todo 表示待办任务；title 必填；dueAt 可以为 null。用户只写日期但没写时间时，dueAt 用当天 23:59:00。没有 DDL 就 dueAt=null 且 reminderOffsetsMinutes=[]。
            3. event 表示有开始和结束时间的日程；startAt/endAt 必填，格式为 yyyy-MM-dd'T'HH:mm:ss。event 默认 createLinkedTodo=false；只有用户明确写了“同步待办 / 同时创建待办 / 也建一个DDL任务 / 日程结束作为DDL”这类意图时才设为 true。地点要放在 location 字段，不要塞进 notes；如果原文写了“@主楼B1-412”或“"@主楼B1-412"”，location 保留原文的 @，不要额外补 @。
            4. reminderOffsetsMinutes 表示提前多少分钟提醒。用户没写提醒时，有 DDL 或日程的候选默认 [5]；没有 DDL 的 todo 用 []。
               如果用户写了“提醒时间：提前1h，06-24 22:10”“提醒：提前30分钟”“提前 2 小时提醒”等自然提醒表达，要把它们换算成相对目标时间的分钟数，例如日程 06-25 14:00 的 “06-24 22:10” 是提前 950 分钟；这些提醒文字不要放进 title。
            5. groupName 只在用户明确写了分组字段时填写，例如“分组：课程”“项目：保研”“类别：入党”。不要从标题里擅自截取词语当分组；例如“入党表格填写”的 groupName 必须是空字符串，标题保持“入党表格填写”。
            6. notes 用于保留上下文、父任务、资料等补充信息；不要把标题、时间、地点重复放进 notes。原文出现“备注：...”或“备注:...”时，把冒号后面的内容逐字放进 notes，保留中文标点、全角符号和原文措辞，不要把“，”改成“,”。
            7. recurrence 用于循环规则：不循环输出 {"enabled":false,"type":"NONE","weeklyDays":[],"endDate":null}；明确写了每天/每周/每月/每年且能确定截止日期时才设置 enabled=true，type 可用 DAILY、WEEKLY、MONTHLY_NTH_WEEKDAY、MONTHLY_DAY、YEARLY_DATE、YEARLY_LUNAR_DATE；每周循环 weeklyDays 用 1-7 表示周一到周日；没有截止日期时不要启用循环，只在 message 提醒用户导入后设置。日期范围要识别为循环：例如“06-22~06-24 19:00-21:00 【合唱】省赛排练 @学生发展中心111”表示 2026-06-22、06-23、06-24 每天 19:00-21:00，输出一个 event，startAt 为 2026-06-22T19:00:00，endAt 为 2026-06-22T21:00:00，recurrence={"enabled":true,"type":"DAILY","weeklyDays":[],"endDate":"2026-06-24"}，title 不要包含“06-22~06-24”。
            8. 已完成、已导入、明显只是标题/说明的内容可以输出 type="skip"。
            9. 不要编造用户没写的具体日期时间；如果无法确定具体时间，todo 的 dueAt 设为 null，event 不要输出或输出 skip。
            10. 相对日期必须结合今天 $today（$todayWeekday）逐字推理出绝对日期，并写进 dueAt / startAt / endAt，绝不要把相对词原样留在标题里：
                - “今天”=$today；“明天”=今天+1 天；“后天”=今天+2 天；“大后天”=今天+3 天；“昨天”=今天-1 天。
                - 单独的星期（“周五 / 星期五 / 周日”）指“从今天起、未来最近的那个星期几”；如果那个星期几就是今天，按今天处理，除非用户明确说“下周”。
                - “这周X / 本周X”指今天所在周（周一为一周开始）的那个星期几，可能已经过去；“下周X”指下一周的那个星期几；“下下周X”再加一周。
                - “X 天后 / X 周后 / 下个月今天”等也要换算成绝对日期。
                - 一天内的时段词按：早上=09:00，上午=12:00（截止类）或上午时段，中午=12:00，下午=17:00，傍晚=18:00，晚上=22:00；用户给了具体时刻以具体时刻为准。
                - 推理示例（假设今天是 $today $todayWeekday）：用户写“明天下午3点开组会”，输出 event，startAt=今天+1天的 15:00。用户写“周五前交报告”，把“周五”解析为未来最近的周五日期，dueAt 设为该日 23:59:00。无法可靠确定具体日期时，todo 的 dueAt 设为 null 并在 message 说明，不要瞎猜一个日期。
            11. 逗号分隔的轻量日程要按“时间段，标题，地点”理解，例如“10:00-12:00, 【课程】习思想，"@主楼B1-412"”应输出 event，title="【课程】习思想"，location="@主楼B1-412"，groupName=""，createLinkedTodo=false。不要把“【课程】”自动截成分组，除非原文明确写了“分组：课程”。
        """.trimIndent()
    }
}
