package com.example.todoalarm.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.TemporalAdjusters

enum class PlanningReminderMode { OMITTED, DISABLED, EXPLICIT }

data class TransientPlanningSlot(
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val recurrence: RecurrenceConfig = RecurrenceConfig()
)

data class TransientPlanningCandidate(
    val id: String,
    val parentId: String? = null,
    val lineNumber: Int,
    val column: Int,
    val sourceLine: String,
    val title: String,
    val notes: String = "",
    val location: String = "",
    val type: PlanningParsedType = PlanningParsedType.TODO,
    val dueAt: LocalDateTime? = null,
    val slots: List<TransientPlanningSlot> = emptyList(),
    val reminderMode: PlanningReminderMode = PlanningReminderMode.OMITTED,
    val reminderOffsetsMinutes: List<Int> = listOf(5, 0),
    val absoluteReminderTimes: List<LocalDateTime> = emptyList(),
    val groupId: Long? = null,
    val calendarId: String = DEFAULT_CALENDAR_ID,
    val timeExpression: String = "",
    val reminderExpression: String = "",
    val rangeExpression: String = ""
) {
    fun validate(): String? {
        if (title.isBlank()) return "标题不能为空"
        if (type !in setOf(PlanningParsedType.TODO, PlanningParsedType.EVENT)) return "请选择待办或日程"
        if (type == PlanningParsedType.EVENT && slots.isEmpty()) return "日程必须填写起止时间"
        if (slots.any { !it.endAt.isAfter(it.startAt) }) return "结束时间必须晚于开始时间，请补写跨日的结束日期"
        if (reminderOffsetsMinutes.any { it < 0 }) return "提前分钟必须为非负整数"
        if (type == PlanningParsedType.TODO && dueAt == null && reminderMode == PlanningReminderMode.EXPLICIT && reminderOffsetsMinutes.isNotEmpty()) return "相对提醒需要DDL，请补时间或使用()关闭提醒"
        if (slots.any { it.recurrence.endDate?.isBefore(it.startAt.toLocalDate()) == true }) return "循环截止日期早于首次日程"
        if (reminderMode != PlanningReminderMode.DISABLED && absoluteReminderTimes.isNotEmpty() && slots.any { it.recurrence.isRecurring }) {
            runCatching { assignPlanningAbsoluteReminders(slots, absoluteReminderTimes) }.exceptionOrNull()?.let {
                return it.message ?: "无法确定绝对提醒所属课程"
            }
        }
        return null
    }
}

data class TransientPlanningDiagnostic(val lineNumber: Int, val column: Int, val message: String)
data class TransientPlanningResult(
    val candidates: List<TransientPlanningCandidate>,
    val diagnostics: List<TransientPlanningDiagnostic>,
    val message: String = ""
) {
    val hasErrors: Boolean get() = diagnostics.isNotEmpty()
}

/** Parse only: no note, task, or reminder is written by this object. */
object TransientPlanningParser {
    private val clock = Regex("\\d{1,2}:\\d{2}")
    private val datetime = Regex("(?:(\\d{4}-\\d{2}-\\d{2}|\\d{2}-\\d{2})[ +])?(\\d{1,2}:\\d{2})")
    private val fullDate = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT)
    private val range = Regex("(?:自(\\d{4}-\\d{2}-\\d{2})至|截至)(\\d{4}-\\d{2}-\\d{2})")
    private val cycle = Regex("每(?:(\\d+))?(天|周|月|年)\\[(.*)]")

    fun parse(text: String, now: LocalDateTime = LocalDateTime.now()): TransientPlanningResult {
        val candidates = mutableListOf<TransientPlanningCandidate>()
        val errors = mutableListOf<TransientPlanningDiagnostic>()
        val parents = mutableMapOf<Int, String>()
        text.lineSequence().forEachIndexed lineLoop@ { index, raw ->
            if (raw.isBlank()) return@lineLoop
            val line = index + 1
            val spaces = raw.takeWhile { it == ' ' || it == '\t' }.fold(0) { total, char -> total + if (char == '\t') 2 else 1 }
            val depth = spaces / 2
            val start = raw.indexOfFirst { !it.isWhitespace() }
            var content = raw.substring(start)
            if (content.startsWith("#")) return@lineLoop
            if (content.startsWith("- ")) content = content.substring(2)
            else if (content == "-") content = ""
            val baseColumn = start + if (raw.substring(start).startsWith("- ")) 3 else 1
            var errorColumn = baseColumn
            try {
                require(content.isNotBlank()) { "标题不能为空" }
                val pieces = split(content, ';')
                var lastId: String? = null
                pieces.forEachIndexed itemLoop@ { itemIndex, piece ->
                    val value = piece.text.trim()
                    errorColumn = piece.offset + baseColumn + piece.text.takeWhile(Char::isWhitespace).length
                    if (value.isBlank()) return@itemLoop
                    if (value.startsWith("#")) {
                        require(itemIndex > 0 && lastId != null) { "备注请放在条目结束分号后" }
                        val position = candidates.indexOfLast { it.id == lastId }
                        require(position >= 0) { "备注前没有有效条目" }
                        // A note after the terminating semicolon consumes the rest of this line.
                        val note = content.substring(piece.offset).trim().removePrefix("#").trim()
                        candidates[position] = candidates[position].copy(notes = note)
                        return@itemLoop
                    }
                    if (itemIndex > 0 && pieces.take(itemIndex).any { it.text.trim().startsWith("#") }) return@itemLoop
                    val fields = split(value, ',').map { it.text.trim() }
                    require(fields.none { it.isEmpty() }) { "存在空字段，请省略整个字段或补齐内容" }
                    require(fields.isNotEmpty()) { "标题不能为空" }
                    val title = unquote(fields.first())
                    require(title.isNotBlank()) { "标题不能为空" }
                    require(!hasUnquotedHash(fields.first())) { "备注请放在结束分号后，用#开始" }
                    var location = ""
                    var time: String? = null
                    var reminders: String? = null
                    var rangeField: String? = null
                    fields.drop(1).forEach { field ->
                        when {
                            field.startsWith("@") -> {
                                require(location.isEmpty()) { "地点字段重复" }
                                location = unquote(field.substring(1).trim())
                                require(location.isNotBlank()) { "@后请填写地点" }
                            }
                            field.startsWith("(") -> { require(reminders == null) { "提醒字段重复" }; reminders = field }
                            range.matches(field) -> { require(rangeField == null) { "循环范围重复" }; rangeField = field }
                            datetime.matches(field) || field.contains('>') || field.startsWith("每") -> {
                                require(time == null) { "时间字段重复" }; time = field
                            }
                            else -> error("无法识别字段：$field")
                        }
                    }
                    val id = "$line:${piece.offset + baseColumn}"
                    require(depth == 0 || parents[depth - 1] != null) { "缺少上一级条目，请减少缩进" }
                    val candidate = parseItem(id, parents[depth - 1], line, piece.offset + baseColumn, raw,
                        title, location, time, reminders, rangeField, now.toLocalDate())
                    candidate.validate()?.let { error(it) }
                    candidates += candidate
                    lastId = id
                    parents.keys.filter { it >= depth }.toList().forEach(parents::remove)
                    parents[depth] = id
                }
            } catch (error: Exception) {
                errors += TransientPlanningDiagnostic(line, errorColumn, error.message ?: "无法识别本行")
            }
        }
        return TransientPlanningResult(candidates, errors)
    }

    private fun parseItem(id: String, parentId: String?, line: Int, column: Int, source: String,
        title: String, location: String, time: String?, reminder: String?, rangeField: String?, today: LocalDate
    ): TransientPlanningCandidate {
        var due: LocalDateTime? = null
        var slots = emptyList<TransientPlanningSlot>()
        if (time != null) {
            if (time.startsWith("每")) slots = recurringSlots(time, rangeField, today)
            else if ('>' in time) {
                require(rangeField == null) { "循环范围需要循环时间表达式" }
                slots = listOf(timeSlot(time, today))
            } else { require(rangeField == null) { "循环范围需要循环时间表达式" }; due = dateTime(time, today) }
        } else require(rangeField == null) { "循环范围需要循环时间表达式" }
        var mode = PlanningReminderMode.OMITTED
        var offsets = listOf(5, 0)
        val absolutes = mutableListOf<LocalDateTime>()
        if (reminder != null) {
            require(reminder.startsWith('(') && reminder.endsWith(')')) { "提醒需要完整半角括号" }
            val inside = reminder.substring(1, reminder.length - 1).trim()
            mode = if (inside.isEmpty()) PlanningReminderMode.DISABLED else PlanningReminderMode.EXPLICIT
            offsets = emptyList()
            if (inside.isNotEmpty()) {
                val values = inside.split(',').map(String::trim)
                require(values.none(String::isEmpty)) { "提醒列表存在空值" }
                values.forEach { value ->
                    if (value.all(Char::isDigit)) {
                        val minutes = value.toIntOrNull() ?: error("提前分钟超出整数范围")
                        offsets = offsets + minutes
                    } else absolutes += dateTime(value, today)
                }
            }
        }
        return TransientPlanningCandidate(id, parentId, line, column, source, title, location = location,
            type = if (slots.isEmpty()) PlanningParsedType.TODO else PlanningParsedType.EVENT,
            dueAt = due, slots = slots, reminderMode = mode,
            reminderOffsetsMinutes = offsets.distinct().sortedDescending(), absoluteReminderTimes = absolutes.distinct().sorted(),
            timeExpression = time.orEmpty(), reminderExpression = reminder.orEmpty(), rangeExpression = rangeField.orEmpty())
    }

    private fun recurringSlots(value: String, rangeField: String?, today: LocalDate): List<TransientPlanningSlot> {
        val match = cycle.matchEntire(value) ?: error("循环格式应为每周[周一 08:00>09:00]等")
        val interval = match.groupValues[1].ifEmpty { "1" }.toIntOrNull() ?: error("循环间隔超出范围")
        require(interval in 1..10000) { "循环间隔必须为1至10000" }
        val unit = match.groupValues[2]
        require(unit in setOf("天", "周") || interval == 1) { "当前支持每N天和每N周；月/年请使用每月或每年" }
        val bounds = rangeField?.let { range.matchEntire(it) ?: error("无法识别循环范围") }
        val begin = bounds?.groupValues?.get(1)?.takeIf(String::isNotEmpty)?.let { LocalDate.parse(it, fullDate) } ?: today
        val end = bounds?.groupValues?.get(2)?.let { LocalDate.parse(it, fullDate) }
        require(end == null || !end.isBefore(begin)) { "循环截止不能早于生效日期" }
        val bodies = match.groupValues[3].split(',').map(String::trim)
        require(bodies.isNotEmpty() && bodies.none(String::isEmpty)) { "循环至少需要一个完整时段" }
        val weekStart = begin.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return bodies.map { body ->
            var date = begin
            var times = body
            val kind: RecurrenceType
            var weekday: DayOfWeek? = null
            var intendedDay: Int? = null
            when (unit) {
                "天" -> kind = RecurrenceType.DAILY
                "周" -> {
                    val day = Regex("周([一二三四五六日天])\\s+(.+)").matchEntire(body) ?: error("每周时段请填写周几和起止时间")
                    weekday = DayOfWeek.of("一二三四五六日".indexOf(day.groupValues[1].replace('天', '日')) + 1)
                    date = weekStart.plusDays((weekday.value - 1).toLong())
                    if (date.isBefore(begin)) date = date.plusWeeks(interval.toLong())
                    times = day.groupValues[2]
                    kind = RecurrenceType.WEEKLY
                }
                "月" -> {
                    val day = Regex("(\\d{1,2})日\\s+(.+)").matchEntire(body) ?: error("每月时段请写15日 09:00>10:00")
                    val number = day.groupValues[1].toInt()
                    intendedDay = number
                    require(number in 1..31) { "每月日期必须在1至31日" }
                    var month = YearMonth.from(begin)
                    date = month.atDay(number.coerceAtMost(month.lengthOfMonth()))
                    if (date.isBefore(begin)) { month = month.plusMonths(1); date = month.atDay(number.coerceAtMost(month.lengthOfMonth())) }
                    times = day.groupValues[2]
                    kind = RecurrenceType.MONTHLY_DAY
                }
                else -> {
                    val day = Regex("(\\d{2}-\\d{2})\\s+(.+)").matchEntire(body) ?: error("每年时段请写11-01 09:00>10:00")
                    val parts = day.groupValues[1].split('-').map(String::toInt)
                    // A leap-year reference validates 02-29 while rejecting impossible month-days.
                    LocalDate.of(2000, parts[0], parts[1])
                    intendedDay = parts[1]
                    fun inYear(year: Int): LocalDate {
                        val month = YearMonth.of(year, parts[0])
                        return month.atDay(parts[1].coerceAtMost(month.lengthOfMonth()))
                    }
                    date = inYear(begin.year)
                    if (date.isBefore(begin)) date = inYear(begin.year + 1)
                    times = day.groupValues[2]
                    kind = RecurrenceType.YEARLY_DATE
                }
            }
            require(times.split('>').size == 2 && times.split('>').all { clock.matches(it.trim()) }) { "循环时段内请填写时钟，例如08:00>09:00" }
            val slot = timeSlot(times, date)
            require(end == null || !date.isAfter(end)) { "范围内没有该时段" }
            slot.copy(recurrence = RecurrenceConfig(true, kind, weekday?.let(::setOf) ?: emptySet(), end, interval, monthlyDay = intendedDay))
        }.distinct()
    }

    private fun timeSlot(value: String, today: LocalDate): TransientPlanningSlot {
        val parts = value.split('>')
        require(parts.size == 2) { "起止时间必须用一个>连接" }
        val start = dateTime(parts[0].trim(), today)
        val end = dateTime(parts[1].trim(), today)
        require(end.isAfter(start)) { "结束时间必须晚于开始时间；跨日请补结束日期" }
        return TransientPlanningSlot(start, end)
    }

    private fun dateTime(value: String, today: LocalDate): LocalDateTime {
        val match = datetime.matchEntire(value.trim()) ?: error("时间格式错误：$value")
        val dateText = match.groupValues[1]
        val date = if (dateText.isEmpty()) today else LocalDate.parse(if (dateText.length == 5) "${today.year}-$dateText" else dateText, fullDate)
        val parts = match.groupValues[2].split(':')
        return date.atTime(LocalTime.of(parts[0].toInt(), parts[1].toInt()))
    }

    private fun unquote(value: String): String {
        if (!value.startsWith('"')) { require('"' !in value && '#' !in value) { "含双引号或#的字段请用首尾双引号包裹" }; return value }
        require(value.length >= 2 && value.endsWith('"')) { "字段缺少末尾双引号" }
        return value.substring(1, value.length - 1)
    }
    private fun hasUnquotedHash(value: String): Boolean = !value.startsWith('"') && '#' in value
    private data class Piece(val text: String, val offset: Int)

    /** A closing quote is a field boundary, not every interior literal quote. */
    private fun split(value: String, delimiter: Char): List<Piece> {
        val result = mutableListOf<Piece>()
        var begin = 0
        var round = 0
        var square = 0
        var quoted = false
        var fieldStart = true
        var index = 0
        while (index < value.length) {
            val c = value[index]
            if (quoted) {
                if (c == '"') {
                    require(!(delimiter == ';' && value.substring(index + 1).trimStart().startsWith(';') &&
                        value.substring(index + 1).trimStart().substring(1).trimStart().startsWith('"'))) {
                        "引号和分号存在多种解释，请把事项拆为多行"
                    }
                    if (closingQuote(value.substring(index + 1))) quoted = false
                }
            } else when {
                c == '"' && fieldStart -> { quoted = true; fieldStart = false }
                c == '(' -> { round++; fieldStart = false }
                c == ')' -> { round--; require(round >= 0) { "多余右括号" } }
                c == '[' -> { square++; fieldStart = false }
                c == ']' -> { square--; require(square >= 0) { "多余右方括号" } }
                c == '#' && round == 0 && square == 0 -> {
                    require(delimiter == ';' && begin > 0 && value.substring(begin, index).isBlank()) { "备注请放在结束分号后" }
                    result += Piece(value.substring(begin), begin)
                    return result
                }
                c == delimiter && round == 0 && square == 0 -> { result += Piece(value.substring(begin, index), begin); begin = index + 1; fieldStart = true }
                c == ',' && round == 0 && square == 0 -> fieldStart = true
                c == '@' && fieldStart -> Unit
                !c.isWhitespace() -> fieldStart = false
            }
            index++
        }
        require(!quoted && round == 0 && square == 0) { "引号或括号没有闭合" }
        result += Piece(value.substring(begin), begin)
        return result
    }

    private fun closingQuote(tail: String): Boolean {
        val rest = tail.trimStart()
        if (rest.isEmpty() || rest.startsWith(';')) return true
        if (!rest.startsWith(',')) return false
        val next = rest.substring(1).trimStart()
        if (next.isEmpty()) return true
        return next.startsWith('@') || next.startsWith('(') || next.startsWith("每") ||
            next.startsWith("自") || next.startsWith("截至") || Regex("^(?:\\d{4}-\\d{2}-\\d{2}[ +]|\\d{2}-\\d{2}[ +])?\\d{1,2}:\\d{2}").containsMatchIn(next)
    }

    fun nextLinePrefix(line: String): String {
        val indent = line.takeWhile { it == ' ' || it == '\t' }.replace("\t", "  ")
        val completed = runCatching { split(line.trim().removePrefix("- "), ';').size > 1 }.getOrDefault(false)
        return indent + if (completed) "- " else "  - "
    }
}
