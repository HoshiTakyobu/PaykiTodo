package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PlanningMarkdownParserTest {
    private val now = LocalDateTime.of(2026, 5, 14, 8, 0)

    @Test
    fun parsesNaturalScheduleWithDateContextAndLinkedTodoDefault() {
        val result = PlanningMarkdownParser.parse(
            """
            # 明天

            - [ ] 09:00-10:30 写论文 #group 课程
            """.trimIndent(),
            now = now
        )

        val event = result.candidates.single { it.type == PlanningParsedType.EVENT }
        assertEquals("写论文", event.title)
        assertEquals(LocalDateTime.of(2026, 5, 15, 9, 0), event.startAt)
        assertEquals(LocalDateTime.of(2026, 5, 15, 10, 30), event.endAt)
        assertEquals("课程", event.groupName)
        assertTrue(event.createLinkedTodo)
        assertEquals(listOf(5), event.reminderOffsetsMinutes)
    }

    @Test
    fun parsesTodoDdlWithDefaultEndOfDayAndSubtaskParentNote() {
        val result = PlanningMarkdownParser.parse(
            """
            - [ ] 保研材料准备
              - [ ] 整理成绩单 #ddl 5.28 #group 保研
            """.trimIndent(),
            now = now
        )

        val todo = result.candidates.first { it.title == "整理成绩单" }
        assertEquals(PlanningParsedType.TODO, todo.type)
        assertEquals(LocalDateTime.of(2026, 5, 28, 23, 59), todo.dueAt)
        assertEquals("保研", todo.groupName)
        assertEquals("所属大任务：保研材料准备", todo.notes)
    }

    @Test
    fun skipsCompletedAndImportedLines() {
        val result = PlanningMarkdownParser.parse(
            """
            - [x] 已完成任务
            - [ ] 已导入任务 #imported
            """.trimIndent(),
            now = now
        )

        assertEquals(2, result.candidates.size)
        assertTrue(result.candidates.all { it.type == PlanningParsedType.SKIPPED })
        assertTrue(result.candidates.any { it.completed })
        assertTrue(result.candidates.any { it.imported })
    }

    @Test
    fun blocksPastDdlAtPreviewStage() {
        val result = PlanningMarkdownParser.parse("- [ ] 过期事项 #ddl 今天 07:30", now = now)

        val todo = result.candidates.single()
        assertEquals(PlanningParsedType.TODO, todo.type)
        assertTrue(todo.importBlocked)
        assertEquals(false, todo.importable)
    }
}
