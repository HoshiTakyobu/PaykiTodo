package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PlanningMarkdownParserTest {
    private val now = LocalDateTime.of(2026, 5, 14, 8, 0)

    @Test
    fun parsesNaturalScheduleWithDateContextWithoutLinkedTodoDefault() {
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
        assertFalse(event.createLinkedTodo)
        assertEquals(listOf(5), event.reminderOffsetsMinutes)
    }

    @Test
    fun parsesNaturalScheduleWithLeadingDateInlineWithoutPollutingTitle() {
        val result = PlanningMarkdownParser.parse(
            """
            - [ ] 5.28 14:00-16:00 小组讨论
            - [ ] 明天 19:30-21:00 整理保研材料
            - [ ] 周五 14:00-16:00 写论文
            """.trimIndent(),
            now = now
        )

        val events = result.candidates.filter { it.type == PlanningParsedType.EVENT }
        assertEquals(3, events.size)
        assertEquals("小组讨论", events[0].title)
        assertEquals(LocalDateTime.of(2026, 5, 28, 14, 0), events[0].startAt)
        assertEquals("整理保研材料", events[1].title)
        assertEquals(LocalDateTime.of(2026, 5, 15, 19, 30), events[1].startAt)
        assertEquals("写论文", events[2].title)
        assertEquals(LocalDateTime.of(2026, 5, 15, 14, 0), events[2].startAt)
    }

    @Test
    fun parsesSharedBareScheduleLineWithFullDateAndLocation() {
        val result = PlanningMarkdownParser.parse(
            "2030-05-21 15:00-16:00 ShareAudit @Library3",
            now = now
        )

        val event = result.candidates.single()
        assertEquals(PlanningParsedType.EVENT, event.type)
        assertEquals("ShareAudit", event.title)
        assertEquals("@Library3", event.location)
        assertEquals(LocalDateTime.of(2030, 5, 21, 15, 0), event.startAt)
        assertEquals(LocalDateTime.of(2030, 5, 21, 16, 0), event.endAt)
    }

    @Test
    fun explicitDdlTakesPrecedenceOverNaturalScheduleText() {
        val result = PlanningMarkdownParser.parse(
            "- [ ] 会议 9:00-10:00 讨论 #ddl 5.28",
            now = now
        )

        val todo = result.candidates.single()
        assertEquals(PlanningParsedType.TODO, todo.type)
        assertEquals("会议 9:00-10:00 讨论", todo.title)
        assertEquals(LocalDateTime.of(2026, 5, 28, 23, 59), todo.dueAt)
    }

    @Test
    fun parsesPlainBulletsAsNoDdlTodos() {
        val result = PlanningMarkdownParser.parse(
            """
            - 想办的事情
            * 再整理一下资料
            • 给导师发消息
            """.trimIndent(),
            now = now
        )

        val todos = result.candidates.filter { it.type == PlanningParsedType.TODO }
        assertEquals(3, todos.size)
        assertEquals(listOf("想办的事情", "再整理一下资料", "给导师发消息"), todos.map { it.title })
        assertTrue(todos.all { it.dueAt == null })
        assertTrue(todos.all { it.reminderOffsetsMinutes.isEmpty() })
        assertTrue(todos.all { it.message.contains("普通项目符号已识别为无 DDL 待办") })
    }

    @Test
    fun parsesBareDdlKeywordAsLightweightTodo() {
        val result = PlanningMarkdownParser.parse(
            "任务M ddl 15:00",
            now = now
        )

        val todo = result.candidates.single()
        assertEquals(PlanningParsedType.TODO, todo.type)
        assertEquals("任务M", todo.title)
        assertEquals(LocalDateTime.of(2026, 5, 14, 15, 0), todo.dueAt)
        assertEquals(listOf(5), todo.reminderOffsetsMinutes)
    }

    @Test
    fun parsesFuzzyDayPeriodAsDdlOnlyWithDateContext() {
        val result = PlanningMarkdownParser.parse(
            """
            # 今日计划
            - [ ] 晚上交论文
            - [ ] 上午开会
            # 收集箱
            - [ ] 晚上随便想想
            """.trimIndent(),
            now = now
        )

        val byTitle = result.candidates.associateBy { it.title }
        assertEquals(LocalDateTime.of(2026, 5, 14, 22, 0), byTitle["晚上交论文"]?.dueAt)
        assertEquals(LocalDateTime.of(2026, 5, 14, 12, 0), byTitle["上午开会"]?.dueAt)
        assertEquals(null, byTitle["晚上随便想想"]?.dueAt)
    }

    @Test
    fun parsesBeforeTimeNaturalDdl() {
        val result = PlanningMarkdownParser.parse(
            """
            - [ ] 5点前交作业
            - [ ] 明天下午3点前提交
            - [ ] 16:30之前发邮件
            """.trimIndent(),
            now = now
        )

        val todos = result.candidates.filter { it.type == PlanningParsedType.TODO }
        assertEquals(LocalDateTime.of(2026, 5, 14, 17, 0), todos[0].dueAt)
        assertEquals(LocalDateTime.of(2026, 5, 15, 15, 0), todos[1].dueAt)
        assertEquals(LocalDateTime.of(2026, 5, 14, 16, 30), todos[2].dueAt)
    }

    @Test
    fun parsesNonCheckboxDdlKeywordTodoAndRecurrenceHintMessage() {
        val result = PlanningMarkdownParser.parse(
            "交论文 截止明天 23:59，每天复盘",
            now = now
        )

        val todo = result.candidates.single()
        assertEquals(PlanningParsedType.TODO, todo.type)
        assertEquals(LocalDateTime.of(2026, 5, 15, 23, 59), todo.dueAt)
        assertTrue(todo.message.contains("根据自然文本推断"))
        assertTrue(todo.message.contains("检测到循环关键词"))
    }

    @Test
    fun headingDateContextIsExplicitAndResetsOnPlainHeading() {
        val result = PlanningMarkdownParser.parse(
            """
            # 我的明天计划
            - [ ] 描述标题任务 #ddl 09:00
            # 明天
            - [ ] 明天任务 #ddl 09:00
            # 收集箱
            - [ ] 收集箱任务 #ddl 09:00
            """.trimIndent(),
            now = now
        )

        val byTitle = result.candidates.associateBy { it.title }
        assertEquals(LocalDateTime.of(2026, 5, 14, 9, 0), byTitle["描述标题任务"]?.dueAt)
        assertEquals(LocalDateTime.of(2026, 5, 15, 9, 0), byTitle["明天任务"]?.dueAt)
        assertEquals(LocalDateTime.of(2026, 5, 14, 9, 0), byTitle["收集箱任务"]?.dueAt)
    }

    @Test
    fun parsesDateHeadingWithDescription() {
        val result = PlanningMarkdownParser.parse(
            """
            # 5/28 周末计划
            - [ ] 写论文 #ddl 23:59
            """.trimIndent(),
            now = now
        )

        val todo = result.candidates.single { it.title == "写论文" }
        assertEquals(LocalDateTime.of(2026, 5, 28, 23, 59), todo.dueAt)
    }

    @Test
    fun parsesCompactWeekdayHeadingAsDateContext() {
        val result = PlanningMarkdownParser.parse(
            """
            # 周五计划
            - [ ] 写论文 #ddl 23:59
            # 后天的事
            - [ ] 描述标题任务 #ddl 23:59
            """.trimIndent(),
            now = now
        )

        val byTitle = result.candidates.associateBy { it.title }
        assertEquals(LocalDateTime.of(2026, 5, 15, 23, 59), byTitle["写论文"]?.dueAt)
        assertEquals(LocalDateTime.of(2026, 5, 14, 23, 59), byTitle["描述标题任务"]?.dueAt)
    }

    @Test
    fun parsesNaturalScheduleWhenTimeRangeIsNotAtLineStart() {
        val result = PlanningMarkdownParser.parse("- [ ] 复习 14:00-16:00", now = now)

        val event = result.candidates.single()
        assertEquals(PlanningParsedType.EVENT, event.type)
        assertEquals("复习", event.title)
        assertEquals(LocalDateTime.of(2026, 5, 14, 14, 0), event.startAt)
        assertEquals(LocalDateTime.of(2026, 5, 14, 16, 0), event.endAt)
    }

    @Test
    fun parsesSlashDatesFullwidthSeparatorsAndChineseDayPeriods() {
        val result = PlanningMarkdownParser.parse("- [ ] 5/28 下午 2:30～下午 4:00 小组讨论", now = now)

        val event = result.candidates.single()
        assertEquals(PlanningParsedType.EVENT, event.type)
        assertEquals("小组讨论", event.title)
        assertEquals(LocalDateTime.of(2026, 5, 28, 14, 30), event.startAt)
        assertEquals(LocalDateTime.of(2026, 5, 28, 16, 0), event.endAt)
    }

    @Test
    fun parsesInlineLocationWithoutPollutingEventTitle() {
        val result = PlanningMarkdownParser.parse("- [ ] 16:05-18:00 入党表格填写 @主楼B1-412", now = now)

        val event = result.candidates.single()
        assertEquals(PlanningParsedType.EVENT, event.type)
        assertEquals("入党表格填写", event.title)
        assertEquals("@主楼B1-412", event.location)
        assertFalse(event.createLinkedTodo)
        assertEquals(LocalDateTime.of(2026, 5, 14, 16, 5), event.startAt)
        assertEquals(LocalDateTime.of(2026, 5, 14, 18, 0), event.endAt)
    }

    @Test
    fun parsesCommaSeparatedQuotedLocationWithoutPollutingEventTitle() {
        val result = PlanningMarkdownParser.parse(
            "- [ ] 10:00-12:00, 【课程】习思想，\"@主楼B1-412\"",
            now = now
        )

        val event = result.candidates.single()
        assertEquals(PlanningParsedType.EVENT, event.type)
        assertEquals("【课程】习思想", event.title)
        assertEquals("@主楼B1-412", event.location)
        assertFalse(event.createLinkedTodo)
        assertEquals("", event.groupName)
        assertEquals(LocalDateTime.of(2026, 5, 14, 10, 0), event.startAt)
        assertEquals(LocalDateTime.of(2026, 5, 14, 12, 0), event.endAt)
    }

    @Test
    fun parsesOrderedBareLocationWithoutAtPrefix() {
        val result = PlanningMarkdownParser.parse(
            "- [ ] 15:00-17:00, 写论文, 图书馆3楼",
            now = now
        )

        val event = result.candidates.single()
        assertEquals(PlanningParsedType.EVENT, event.type)
        assertEquals("写论文", event.title)
        assertEquals("图书馆3楼", event.location)
        assertEquals(LocalDateTime.of(2026, 5, 14, 15, 0), event.startAt)
        assertEquals(LocalDateTime.of(2026, 5, 14, 17, 0), event.endAt)
    }

    @Test
    fun parsesSpaceSeparatedLocationWhenLastTokenLooksLikePlace() {
        val result = PlanningMarkdownParser.parse(
            "- [ ] 15:00-17:00 写论文 图书馆3楼",
            now = now
        )

        val event = result.candidates.single()
        assertEquals(PlanningParsedType.EVENT, event.type)
        assertEquals("写论文", event.title)
        assertEquals("图书馆3楼", event.location)
    }

    @Test
    fun eventParentTitleIsAppliedToSubtasks() {
        val result = PlanningMarkdownParser.parse(
            """
            - [ ] 10:00-12:00 项目会议
              - [ ] 准备资料
            """.trimIndent(),
            now = now
        )

        val child = result.candidates.single { it.title == "准备资料" }
        assertEquals(PlanningParsedType.TODO, child.type)
        assertEquals("所属大任务：项目会议", child.notes)
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
    fun keepsUnsupportedSemanticTagsVisibleInTitle() {
        val result = PlanningMarkdownParser.parse("- [ ] 给导师发邮件 #ddl 5.18 #important #project", now = now)

        val todo = result.candidates.single()
        assertEquals("给导师发邮件 #important #project", todo.title)
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

    @Test
    fun parsesCommaBetweenDateAndTimeInDdlAndReminder() {
        val result = PlanningMarkdownParser.parse(
            "- [ ] 交材料 #ddl 5.28,23:59 #remind 5.28,22:00",
            now = now
        )

        val todo = result.candidates.single()
        assertEquals(PlanningParsedType.TODO, todo.type)
        assertEquals(LocalDateTime.of(2026, 5, 28, 23, 59), todo.dueAt)
        assertEquals(listOf(119), todo.reminderOffsetsMinutes)
    }

    @Test
    fun marksImportedLinesWithoutDuplicatingExistingTag() {
        val markdown = """
            - [ ] 第一条 #ddl 5.28
            - [ ] 第二条 #imported
            - [ ] 第三条 #ddl 5.29
        """.trimIndent()

        val updated = PlanningMarkdownParser.markImportedLines(markdown, setOf(1, 2, 3))

        assertEquals(
            """
            - [ ] 第一条 #ddl 5.28 #imported
            - [ ] 第二条 #imported
            - [ ] 第三条 #ddl 5.29 #imported
            """.trimIndent(),
            updated
        )
    }
}
