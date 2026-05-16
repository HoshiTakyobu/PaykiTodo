package com.example.todoalarm.data

import java.time.LocalDate
import java.time.LocalTime

data class PlanningAnnouncement(
    val text: String,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val sourceNoteId: Long = 0,
    val sourceNoteTitle: String = "",
    val lineNumber: Int = 0
) {
    fun isActiveOn(date: LocalDate): Boolean {
        val starts = startDate?.let { !date.isBefore(it) } ?: true
        val ends = endDate?.let { !date.isAfter(it) } ?: true
        return text.isNotBlank() && starts && ends
    }

    fun rangeLabel(): String {
        return when {
            startDate == null && endDate == null -> "长期"
            startDate != null && endDate != null && startDate == endDate -> "${startDate.monthValue}.${startDate.dayOfMonth}"
            startDate != null && endDate != null -> "${startDate.monthValue}.${startDate.dayOfMonth}-${endDate.monthValue}.${endDate.dayOfMonth}"
            startDate != null -> "${startDate.monthValue}.${startDate.dayOfMonth} 起"
            else -> "至 ${endDate!!.monthValue}.${endDate.dayOfMonth}"
        }
    }
}

object PlanningAnnouncementParser {
    fun activeAnnouncements(
        notes: List<PlanningNote>,
        today: LocalDate = LocalDate.now()
    ): List<PlanningAnnouncement> {
        return parse(notes, today).filter { it.isActiveOn(today) }
    }

    fun parse(
        notes: List<PlanningNote>,
        today: LocalDate = LocalDate.now()
    ): List<PlanningAnnouncement> {
        return notes
            .asSequence()
            .filter { !it.archived }
            .flatMap { note -> parseNote(note, today).asSequence() }
            .sortedWith(
                compareBy<PlanningAnnouncement> { it.startDate == null }
                    .thenByDescending { it.startDate ?: LocalDate.MIN }
                    .thenBy { it.sourceNoteId }
                    .thenBy { it.lineNumber }
            )
            .toList()
    }

    private fun parseNote(note: PlanningNote, today: LocalDate): List<PlanningAnnouncement> {
        return note.contentMarkdown
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .mapIndexedNotNull { index, line ->
                parseSingleLine(
                    rawLine = line,
                    today = today,
                    sourceNoteId = note.id,
                    sourceNoteTitle = note.title,
                    lineNumber = index + 1
                )
            }
    }

    fun parseSingleLine(
        rawLine: String,
        today: LocalDate = LocalDate.now(),
        sourceNoteId: Long = 0,
        sourceNoteTitle: String = "",
        lineNumber: Int = 0
    ): PlanningAnnouncement? {
        val content = extractAnnouncementBody(rawLine) ?: return null
        val parsed = parseRangeAndText(content, today)
        val text = cleanAnnouncementText(parsed.text)
        if (text.isBlank()) return null
        return PlanningAnnouncement(
            text = text,
            startDate = parsed.startDate,
            endDate = parsed.endDate,
            sourceNoteId = sourceNoteId,
            sourceNoteTitle = sourceNoteTitle,
            lineNumber = lineNumber
        )
    }

    private fun extractAnnouncementBody(rawLine: String): String? {
        val line = normalize(rawLine).trim()
        if (line.isBlank()) return null
        val candidates = listOf(line, stripLeadingMarkdownMarkers(line))
            .flatMap { value ->
                listOf(value, value.substringAfterAnnouncementHint())
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        candidates.forEach { candidate ->
            AnnouncementPrefixRegex.find(candidate)?.takeIf { it.range.first == 0 }?.let { return it.groupValues[1].trim() }
            CalloutPrefixRegex.find(candidate)?.takeIf { it.range.first == 0 }?.let { return it.groupValues[1].trim() }
            PlainPrefixRegex.find(candidate)?.takeIf { it.range.first == 0 }?.let { return it.groupValues[1].trim() }
        }
        InlineAnnouncementRegex.find(line)?.let { return it.groupValues[1].trim() }
        InlinePlainPrefixRegex.find(line)?.let { return it.groupValues[1].trim() }
        return null
    }

    private fun stripLeadingMarkdownMarkers(raw: String): String {
        var line = raw.trim()
        var changed: Boolean
        do {
            val before = line
            line = line
                .replace(LeadingQuoteRegex, "")
                .replace(LeadingTaskRegex, "")
                .replace(LeadingBulletRegex, "")
                .trimStart()
            changed = line != before
        } while (changed)
        return line
    }

    private fun String.substringAfterAnnouncementHint(): String {
        val announcementIndex = indexOf("#公告")
            .takeIf { it >= 0 }
            ?: indexOf("# 公告").takeIf { it >= 0 }
            ?: indexOf("[!公告]").takeIf { it >= 0 }
            ?: indexOf("公告:").takeIf { it >= 0 }
            ?: indexOf("公告：").takeIf { it >= 0 }
        return announcementIndex?.let { substring(it) } ?: this
    }

    private fun cleanAnnouncementText(raw: String): String {
        var text = raw.trim().trimStart(':', '：', '-', '—', '–').trim()
        do {
            val before = text
            text = text
                .replace(ImportedSuffixRegex, "")
                .replace(GroupTagSuffixRegex, "")
                .replace(TailHashtagRegex, "")
                .replace(Regex("\\s+"), " ")
                .trim()
        } while (text != before)
        return text
    }

    private fun parseRangeAndText(raw: String, today: LocalDate): ParsedAnnouncementLine {
        val text = normalize(raw).trim()
        DateRangeRegex.matchEntire(text)?.let { match ->
            val start = parseDate(match.groupValues[1], today)
            val end = parseDate(match.groupValues[2], today)
            if (start != null && end != null) {
                val orderedStart = minOf(start, end)
                val orderedEnd = maxOf(start, end)
                return ParsedAnnouncementLine(
                    startDate = orderedStart,
                    endDate = orderedEnd,
                    text = match.groupValues[3]
                )
            }
        }
        DatePairRegex.matchEntire(text)?.let { match ->
            val start = parseDate(match.groupValues[1], today)
            val end = parseDate(match.groupValues[2], today)
            if (start != null && end != null) {
                val orderedStart = minOf(start, end)
                val orderedEnd = maxOf(start, end)
                return ParsedAnnouncementLine(
                    startDate = orderedStart,
                    endDate = orderedEnd,
                    text = match.groupValues[3]
                )
            }
        }
        SingleDateRegex.matchEntire(text)?.let { match ->
            val date = parseDate(match.groupValues[1], today)
            if (date != null) {
                return ParsedAnnouncementLine(
                    startDate = date,
                    endDate = date,
                    text = match.groupValues[2]
                )
            }
        }
        return ParsedAnnouncementLine(text = text)
    }

    private fun parseDate(raw: String, today: LocalDate): LocalDate? {
        return PlanningMarkdownParser.parseDateTimeExpression(
            raw = raw,
            defaultDate = null,
            nowDate = today,
            defaultTime = LocalTime.MIN
        )?.toLocalDate()
    }

    private fun normalize(raw: String): String {
        return raw
            .replace('：', ':')
            .replace('，', ',')
            .replace('．', '.')
            .replace('。', '.')
            .replace('／', '/')
            .replace('－', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace('～', '~')
            .replace('〜', '~')
    }

    private data class ParsedAnnouncementLine(
        val startDate: LocalDate? = null,
        val endDate: LocalDate? = null,
        val text: String
    )

    private val DateToken = "(?:今天|今日|明天|明日|后天|\\d{4}[-./]\\d{1,2}[-./]\\d{1,2}|\\d{4}年\\d{1,2}月\\d{1,2}日?|\\d{1,2}[-./]\\d{1,2}|\\d{1,2}月\\d{1,2}日?)"
    private val AnnouncementPrefixRegex = Regex("^#{1,6}\\s*公告\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val CalloutPrefixRegex = Regex("^>?\\s*\\[!(?:公告|announcement)\\]\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val PlainPrefixRegex = Regex("^公告\\s*[:：]\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val InlineAnnouncementRegex = Regex("(?:^|[\\s:：，,。；;])#{1,6}\\s*公告\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val InlinePlainPrefixRegex = Regex("(?:^|[\\s:：，,。；;])公告\\s*[:：]\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val LeadingQuoteRegex = Regex("^>\\s*")
    private val LeadingTaskRegex = Regex("^[-*+]\\s*\\[[ xX]\\]\\s*")
    private val LeadingBulletRegex = Regex("^[-*+]\\s+")
    private val ImportedSuffixRegex = Regex("\\s+#imported\\s*$", RegexOption.IGNORE_CASE)
    private val GroupTagSuffixRegex = Regex("\\s+#group(?:\\s+[^#\\s]+)?\\s*$", RegexOption.IGNORE_CASE)
    private val TailHashtagRegex = Regex("\\s+#[\\p{L}\\p{N}_-]+\\s*$")
    private val DateRangeRegex = Regex("^\\s*($DateToken)\\s*(?:-|~|至|到)\\s*($DateToken)\\s+(.+)$")
    private val DatePairRegex = Regex("^\\s*($DateToken)\\s+($DateToken)\\s+(.+)$")
    private val SingleDateRegex = Regex("^\\s*($DateToken)\\s+(.+)$")
}
