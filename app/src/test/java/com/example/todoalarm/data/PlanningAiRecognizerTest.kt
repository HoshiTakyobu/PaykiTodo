package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class PlanningAiRecognizerTest {
    private val now = LocalDateTime.of(2026, 5, 15, 9, 0)

    @Test
    fun parsesAiJsonObjectIntoPreviewCandidates() {
        val result = PlanningAiRecognizer.parseAiContent(
            content = """
                {
                  "items": [
                    {
                      "type": "todo",
                      "lineNumber": 1,
                      "sourceLine": "分组：课程 晚上交论文，记得提前提醒我",
                      "title": "交论文",
                      "notes": "用户随手写的 DDL",
                      "groupName": "课程",
                      "dueAt": "2026-05-15T22:00:00",
                      "reminderOffsetsMinutes": [30, 5],
                      "message": "根据自然文本推断，建议确认"
                    },
                    {
                      "type": "event",
                      "lineNumber": 2,
                      "sourceLine": "明天下午开组会两点到四点",
                      "title": "组会",
                      "startAt": "2026-05-16T14:00:00",
                      "endAt": "2026-05-16T16:00:00",
                      "createLinkedTodo": true
                    }
                  ]
                }
            """.trimIndent(),
            originalMarkdown = "分组：课程 晚上交论文，记得提前提醒我\n明天下午开组会两点到四点",
            providerName = "TestAI",
            now = now
        )

        assertEquals("AI 识别：TestAI；请在预览中确认", result.message)
        assertEquals(2, result.candidates.size)

        val todo = result.candidates[0]
        assertEquals(PlanningParsedType.TODO, todo.type)
        assertEquals("交论文", todo.title)
        assertEquals("课程", todo.groupName)
        assertEquals(LocalDateTime.of(2026, 5, 15, 22, 0), todo.dueAt)
        assertEquals(listOf(30, 5), todo.reminderOffsetsMinutes)
        assertTrue(todo.message.contains("AI 识别结果"))

        val event = result.candidates[1]
        assertEquals(PlanningParsedType.EVENT, event.type)
        assertEquals("组会", event.title)
        assertEquals(LocalDateTime.of(2026, 5, 16, 14, 0), event.startAt)
        assertEquals(LocalDateTime.of(2026, 5, 16, 16, 0), event.endAt)
        assertTrue(event.createLinkedTodo)
        assertEquals(listOf(5), event.reminderOffsetsMinutes)
    }

    @Test
    fun parsesFencedAiJsonArray() {
        val result = PlanningAiRecognizer.parseAiContent(
            content = """
                ```json
                [
                  {
                    "type": "todo",
                    "title": "整理材料",
                    "dueAt": "2026-05-18",
                    "sourceLine": "下周一前整理材料"
                  }
                ]
                ```
            """.trimIndent(),
            originalMarkdown = "下周一前整理材料",
            providerName = "TestAI",
            now = now
        )

        val todo = result.candidates.single()
        assertEquals(PlanningParsedType.TODO, todo.type)
        assertEquals("整理材料", todo.title)
        assertEquals(LocalDateTime.of(2026, 5, 18, 23, 59), todo.dueAt)
        assertEquals(listOf(5), todo.reminderOffsetsMinutes)
    }

    @Test
    fun marksImportedSourceLineAsSkipped() {
        val result = PlanningAiRecognizer.parseAiContent(
            content = """{"items":[{"type":"todo","lineNumber":1,"title":"旧任务"}]}""",
            originalMarkdown = "- [ ] 旧任务 #imported",
            providerName = "TestAI",
            now = now
        )

        val candidate = result.candidates.single()
        assertEquals(PlanningParsedType.SKIPPED, candidate.type)
        assertTrue(candidate.imported)
    }

    @Test
    fun dropsAiInferredGroupNameWhenSourceLineHasNoExplicitGroupMarker() {
        val result = PlanningAiRecognizer.parseAiContent(
            content = """
                {
                  "items": [
                    {
                      "type": "event",
                      "lineNumber": 1,
                      "sourceLine": "16:05-18:00 入党表格填写",
                      "title": "入党表格填写",
                      "groupName": "入党",
                      "startAt": "2026-05-15T16:05:00",
                      "endAt": "2026-05-15T18:00:00"
                    }
                  ]
                }
            """.trimIndent(),
            originalMarkdown = "16:05-18:00 入党表格填写",
            providerName = "TestAI",
            now = now
        )

        val event = result.candidates.single()
        assertEquals("入党表格填写", event.title)
        assertEquals("", event.groupName)
        assertEquals(LocalDateTime.of(2026, 5, 15, 16, 5), event.startAt)
        assertEquals(LocalDateTime.of(2026, 5, 15, 18, 0), event.endAt)
    }

    @Test
    fun preservesAiEventLocationAndRecurrenceFields() {
        val result = PlanningAiRecognizer.parseAiContent(
            content = """
                {
                  "items": [
                    {
                      "type": "event",
                      "lineNumber": 1,
                      "sourceLine": "每周三 16:05-18:00 组会 @主楼B1-412，持续到 2026-06-30",
                      "title": "组会",
                      "location": "@主楼B1-412",
                      "startAt": "2026-05-20T16:05:00",
                      "endAt": "2026-05-20T18:00:00",
                      "allDay": false,
                      "countdownEnabled": true,
                      "recurrence": {
                        "enabled": true,
                        "type": "WEEKLY",
                        "weeklyDays": [3],
                        "endDate": "2026-06-30"
                      }
                    }
                  ]
                }
            """.trimIndent(),
            originalMarkdown = "每周三 16:05-18:00 组会 @主楼B1-412，持续到 2026-06-30",
            providerName = "TestAI",
            now = now
        )

        val event = result.candidates.single()
        assertEquals(PlanningParsedType.EVENT, event.type)
        assertEquals("@主楼B1-412", event.location)
        assertTrue(event.countdownEnabled)
        assertTrue(event.recurrence.enabled)
        assertEquals(RecurrenceType.WEEKLY, event.recurrence.type)
        assertEquals(setOf(DayOfWeek.WEDNESDAY), event.recurrence.weeklyDays)
        assertEquals(LocalDate.of(2026, 6, 30), event.recurrence.endDate)
    }
}
