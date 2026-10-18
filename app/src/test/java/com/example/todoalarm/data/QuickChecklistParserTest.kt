package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickChecklistParserTest {
    @Test
    fun titlelessInputPreservesNotesHierarchyAndSemanticFingerprint() {
        val source = "- 想到就写\n-- 子任务\n--# 说明\n- 第二件事"
        val draft = QuickChecklistParser.parse(source)
        assertFalse(draft.hasErrors)
        assertEquals("", draft.title)
        assertEquals(3, draft.taskCount)
        assertEquals(draft.nodes.first().temporaryId, draft.nodes[1].parentTemporaryId)
        assertEquals("说明", draft.nodes[1].notes)
        assertEquals(draft.sourceFingerprint, QuickChecklistParser.parse("// 注释\n$source // 注释").sourceFingerprint)
        assertTrue(QuickChecklistParser.parse("只有标题").hasErrors)
        assertTrue(QuickChecklistParser.parse("- 任务\n没有短横线的正文").hasErrors)
    }
    @Test
    fun parsesSpecificationExample() {
        val draft = QuickChecklistParser.parse(
            """
            0913待办 //标题行
            - 准备浙大的资料
            -- 1. 身份证复印件
            -- 2. 个人简历
            -- 3. 成绩排名
            --# 成绩排名是本学年的
            - 取快递（取件码114514）
            """.trimIndent()
        )

        assertEquals("0913待办", draft.title)
        assertEquals(5, draft.taskCount)
        assertEquals(2, draft.maxDepth)
        assertEquals(1, draft.noteCount)
        assertFalse(draft.hasErrors)
        assertFalse(draft.nodes.any { it.title.contains("标题行") })

        val parent = draft.nodes.first()
        val children = draft.nodes.filter { it.parentTemporaryId == parent.temporaryId }
        assertEquals(
            listOf("1. 身份证复印件", "2. 个人简历", "3. 成绩排名"),
            children.map { it.title }
        )
        assertEquals("成绩排名是本学年的", children.last().notes)
        assertNull(draft.nodes.last().parentTemporaryId)
    }

    @Test
    fun preservesMultipleNotesAndIgnoresBlankAndCommentLines() {
        val draft = QuickChecklistParser.parse(
            """
            清单
            // 不保存

            - 任务 // 行内注释
            -# 第一条说明
            -# 第二条说明
            """.trimIndent()
        )

        assertFalse(draft.hasErrors)
        assertEquals("任务", draft.nodes.single().title)
        assertEquals("第一条说明\n第二条说明", draft.nodes.single().notes)
    }

    @Test
    fun allowsOptionalTitleButRejectsEmptyTasksOrphanNotesAndPlainBodyLines() {
        val missingTitle = QuickChecklistParser.parse("- 任务")
        assertFalse(missingTitle.hasErrors)
        assertEquals("", missingTitle.title)
        assertEquals(1, missingTitle.taskCount)

        val emptyTask = QuickChecklistParser.parse("标题\n-\n- 后续")
        assertTrue(emptyTask.diagnostics.any { it.sourceLine == 2 && it.message.contains("不能为空") })

        val orphanNote = QuickChecklistParser.parse("标题\n--# 无归属\n- 任务")
        assertTrue(orphanNote.diagnostics.any { it.sourceLine == 2 && it.message.contains("没有可关联") })

        val plainLine = QuickChecklistParser.parse("标题\n普通正文\n- 任务")
        assertTrue(plainLine.diagnostics.any { it.sourceLine == 2 && it.message.contains("短横线") })
    }

    @Test
    fun allowsHierarchyJumpWithWarningAndStableParent() {
        val draft = QuickChecklistParser.parse("标题\n- 父级\n--- 跳级子任务")

        assertFalse(draft.hasErrors)
        assertTrue(draft.diagnostics.any { it.severity == QuickChecklistDiagnosticSeverity.WARNING })
        assertEquals(draft.nodes.first().temporaryId, draft.nodes.last().parentTemporaryId)
    }

    @Test
    fun ignoresOnlyTrailingGeneratedEmptyTask() {
        val trailing = QuickChecklistParser.parse("标题\n- 任务\n-- ")
        assertFalse(trailing.hasErrors)
        assertEquals(1, trailing.taskCount)

        val middle = QuickChecklistParser.parse("标题\n- \n- 任务")
        assertTrue(middle.hasErrors)
    }

    @Test
    fun fingerprintIgnoresCommentsAndBlankLinesButDetectsSemanticChanges() {
        val first = QuickChecklistParser.parse("标题\n- 任务\n-- 子任务")
        val equivalent = QuickChecklistParser.parse("标题 // 注释\n\n- 任务 // 注释\n-- 子任务")
        val changed = QuickChecklistParser.parse("标题\n- 任务\n-- 另一个子任务")

        assertEquals(first.sourceFingerprint, equivalent.sourceFingerprint)
        assertNotEquals(first.sourceFingerprint, changed.sourceFingerprint)
    }

    @Test
    fun enterContinuationUsesCurrentTaskOrNoteDepth() {
        assertEquals("- ", QuickChecklistEditing.continuationPrefix("- task"))
        assertEquals("-- ", QuickChecklistEditing.continuationPrefix("-- task"))
        assertEquals("-- ", QuickChecklistEditing.continuationPrefix("--# note"))
        assertEquals("", QuickChecklistEditing.continuationPrefix("标题"))

        val result = QuickChecklistEditing.insertLineBreak("标题\n-- task", 10)
        assertEquals("标题\n-- task\n-- ", result.text)
        assertEquals(result.text.length, result.cursor)
    }
}
