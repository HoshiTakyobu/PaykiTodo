package com.example.todoalarm.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.security.MessageDigest

@Entity(
    tableName = "quick_checklist_groups",
    indices = [
        Index(value = ["sourceFingerprint"], unique = true),
        Index(value = ["updatedAtMillis"])
    ]
)
data class QuickChecklistGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @ColumnInfo(defaultValue = "0")
    val collapsed: Boolean = false,
    val sourceFingerprint: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

enum class QuickChecklistDiagnosticSeverity {
    ERROR,
    WARNING
}

data class QuickChecklistDiagnostic(
    val severity: QuickChecklistDiagnosticSeverity,
    val sourceLine: Int,
    val message: String
)

data class QuickChecklistDraftNode(
    val temporaryId: Int,
    val parentTemporaryId: Int?,
    val depth: Int,
    val title: String,
    val notes: String = "",
    val sourceLine: Int,
    val sortOrder: Int
)

data class QuickChecklistDraft(
    val title: String,
    val nodes: List<QuickChecklistDraftNode>,
    val diagnostics: List<QuickChecklistDiagnostic>,
    val sourceFingerprint: String
) {
    val taskCount: Int
        get() = nodes.size

    val maxDepth: Int
        get() = nodes.maxOfOrNull { it.depth } ?: 0

    val noteCount: Int
        get() = nodes.sumOf { node -> node.notes.lines().count { it.isNotBlank() } }

    val hasErrors: Boolean
        get() = diagnostics.any { it.severity == QuickChecklistDiagnosticSeverity.ERROR }
}

object QuickChecklistParser {
    private val notePattern = Regex("^(-+)#\\s*(.*)$")
    private val taskPattern = Regex("^(-+)\\s*(.*)$")

    fun parse(rawText: String): QuickChecklistDraft {
        val preparedLines = rawText.lines().mapIndexed { index, rawLine ->
            PreparedLine(
                sourceLine = index + 1,
                content = rawLine.substringBefore("//").trim()
            )
        }
        val effectiveLines = preparedLines.filter { it.content.isNotBlank() }
        val diagnostics = mutableListOf<QuickChecklistDiagnostic>()
        val first = effectiveLines.firstOrNull()
        val hasTitle = first != null && !first.content.startsWith('-')
        val title = if (hasTitle) first?.content.orEmpty() else ""
        if (first == null) {
            diagnostics += error(1, "请先输入清单标题")
        } else if (!hasTitle) {
            diagnostics += error(first.sourceLine, "第一条有效内容必须是清单标题")
        }

        val bodyLines = if (hasTitle) {
            effectiveLines.drop(1)
        } else {
            effectiveLines
        }
        val mutableNodes = mutableListOf<QuickChecklistDraftNode>()
        val lastNodeByDepth = mutableMapOf<Int, QuickChecklistDraftNode>()

        bodyLines.forEach { line ->
            val noteMatch = notePattern.matchEntire(line.content)
            if (noteMatch != null) {
                val depth = noteMatch.groupValues[1].length
                val noteText = noteMatch.groupValues[2].trim()
                if (noteText.isBlank()) {
                    diagnostics += error(line.sourceLine, "说明内容不能为空")
                    return@forEach
                }
                val target = lastNodeByDepth[depth]
                    ?: lastNodeByDepth.entries
                        .filter { (candidateDepth, _) -> candidateDepth < depth }
                        .maxByOrNull { it.key }
                        ?.value
                if (target == null) {
                    diagnostics += error(line.sourceLine, "说明之前没有可关联的任务")
                    return@forEach
                }
                val targetIndex = mutableNodes.indexOfFirst { it.temporaryId == target.temporaryId }
                if (targetIndex >= 0) {
                    val previousNotes = mutableNodes[targetIndex].notes
                    val updated = mutableNodes[targetIndex].copy(
                        notes = listOf(previousNotes, noteText).filter { it.isNotBlank() }.joinToString("\n")
                    )
                    mutableNodes[targetIndex] = updated
                    lastNodeByDepth[target.depth] = updated
                }
                return@forEach
            }

            val taskMatch = taskPattern.matchEntire(line.content)
            if (taskMatch != null) {
                val depth = taskMatch.groupValues[1].length
                val taskTitle = taskMatch.groupValues[2].trim()
                if (taskTitle.isBlank()) {
                    val isLastPhysicalLine = line.sourceLine == preparedLines.lastOrNull()?.sourceLine
                    if (!isLastPhysicalLine) {
                        diagnostics += error(line.sourceLine, "任务内容不能为空")
                    }
                    return@forEach
                }
                val parent = if (depth == 1) {
                    null
                } else {
                    lastNodeByDepth.entries
                        .filter { (candidateDepth, _) -> candidateDepth < depth }
                        .maxByOrNull { it.key }
                        ?.value
                }
                if (depth > 1 && parent == null) {
                    diagnostics += error(line.sourceLine, "该任务缺少上级任务")
                    return@forEach
                }
                if (parent != null && parent.depth != depth - 1) {
                    diagnostics += QuickChecklistDiagnostic(
                        severity = QuickChecklistDiagnosticSeverity.WARNING,
                        sourceLine = line.sourceLine,
                        message = "层级从 ${parent.depth} 跳到 $depth，已关联到最近的上级任务"
                    )
                }
                val node = QuickChecklistDraftNode(
                    temporaryId = mutableNodes.size + 1,
                    parentTemporaryId = parent?.temporaryId,
                    depth = depth,
                    title = taskTitle,
                    sourceLine = line.sourceLine,
                    sortOrder = mutableNodes.count { it.parentTemporaryId == parent?.temporaryId }
                )
                mutableNodes += node
                lastNodeByDepth.keys.filter { it >= depth }.toList().forEach(lastNodeByDepth::remove)
                lastNodeByDepth[depth] = node
                return@forEach
            }

            diagnostics += error(line.sourceLine, "标题后的内容需要以短横线开头")
        }

        if (hasTitle && mutableNodes.isEmpty()) {
            diagnostics += error(first?.sourceLine ?: 1, "清单中至少需要一项任务")
        }
        val fingerprint = contentFingerprint(title, mutableNodes)
        return QuickChecklistDraft(
            title = title,
            nodes = mutableNodes,
            diagnostics = diagnostics,
            sourceFingerprint = fingerprint
        )
    }

    private fun contentFingerprint(title: String, nodes: List<QuickChecklistDraftNode>): String {
        val normalized = buildString {
            append(title.trim())
            nodes.forEach { node ->
                append('\n')
                append(node.depth)
                append('|')
                append(node.parentTemporaryId ?: 0)
                append('|')
                append(node.sortOrder)
                append('|')
                append(node.title.trim())
                append('|')
                append(node.notes.trim())
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun error(line: Int, message: String): QuickChecklistDiagnostic {
        return QuickChecklistDiagnostic(
            severity = QuickChecklistDiagnosticSeverity.ERROR,
            sourceLine = line,
            message = message
        )
    }

    private data class PreparedLine(
        val sourceLine: Int,
        val content: String
    )
}

object QuickChecklistEditing {
    data class EditResult(
        val text: String,
        val cursor: Int
    )

    fun continuationPrefix(line: String): String {
        val content = line.substringBefore("//").trimStart()
        val match = Regex("^(-+)(?:#)?(?:\\s.*|$)").find(content) ?: return ""
        return "${match.groupValues[1]} "
    }

    fun insertLineBreak(
        text: String,
        selectionStart: Int,
        selectionEnd: Int = selectionStart
    ): EditResult {
        val safeStart = selectionStart.coerceIn(0, text.length)
        val safeEnd = selectionEnd.coerceIn(safeStart, text.length)
        val currentLineStart = text.lastIndexOf('\n', safeStart - 1).let { if (it < 0) 0 else it + 1 }
        val prefix = continuationPrefix(text.substring(currentLineStart, safeStart))
        val insertion = "\n$prefix"
        return EditResult(
            text = text.replaceRange(safeStart, safeEnd, insertion),
            cursor = safeStart + insertion.length
        )
    }
}
