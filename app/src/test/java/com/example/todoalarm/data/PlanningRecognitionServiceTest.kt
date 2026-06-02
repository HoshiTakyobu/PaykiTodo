package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PlanningRecognitionServiceTest {
    private val now = LocalDateTime.of(2026, 5, 15, 9, 0)

    @Test
    fun usesLocalParserWhenAiIsDisabled() {
        val result = runRecognition(
            markdown = "- [ ] 整理材料 #ddl 2026-05-16 23:00",
            settings = AppSettings(planningAiEnabled = false)
        )

        assertEquals("", result.message)
        assertEquals(1, result.importableCount)
        assertEquals("整理材料", result.candidates.single().title)
    }

    @Test
    fun fallsBackToLocalParserWhenNoCompleteProviderExists() {
        val result = runRecognition(
            markdown = "交论文 截止明天 23:59",
            settings = AppSettings(
                planningAiEnabled = true,
                planningAiProviders = listOf(
                    PlanningAiProvider(
                        name = "Incomplete",
                        baseUrl = "https://example.com/v1",
                        apiKey = "",
                        model = "gpt-4.1"
                    )
                )
            )
        )

        assertEquals("AI 配置未完整，已使用本地规则", result.message)
        assertEquals(1, result.importableCount)
        assertTrue(result.candidates.single().message.contains("根据自然文本推断"))
    }

    @Test
    fun prefersLocalParserWhenExplicitSyntaxCoversAllLines() {
        val local = PlanningMarkdownParser.parse(
            markdown = """
                # 今日计划
                - [ ] 整理材料 #ddl 2026-05-16 23:00
                - [ ] 10:00-12:00 写论文 @图书馆3楼
            """.trimIndent(),
            now = now,
            documentDate = LocalDate.of(2026, 5, 16)
        )

        assertTrue(shouldUseLocalPlanningResult(
            markdown = """
                # 今日计划
                - [ ] 整理材料 #ddl 2026-05-16 23:00
                - [ ] 10:00-12:00 写论文 @图书馆3楼
            """.trimIndent(),
            localResult = local
        ))
    }

    @Test
    fun doesNotPreferLocalParserWhenNaturalTextIsUncovered() {
        val markdown = """
            - [ ] 整理材料 #ddl 2026-05-16 23:00
            明天下午和同学讨论一下论文
        """.trimIndent()
        val local = PlanningMarkdownParser.parse(markdown = markdown, now = now)

        assertFalse(shouldUseLocalPlanningResult(markdown, local))
    }

    @Test
    fun localPreferencePreservesInlineDateOverDocumentDate() {
        val markdown = "5.29 【紧急】【DDL】把入党资料交到芳姐那里"
        val local = PlanningMarkdownParser.parse(
            markdown = markdown,
            now = LocalDateTime.of(2026, 5, 20, 8, 0),
            documentDate = LocalDate.of(2026, 5, 20)
        )

        assertTrue(shouldUseLocalPlanningResult(markdown, local))
        assertEquals(LocalDateTime.of(2026, 5, 29, 23, 59), local.candidates.single().dueAt)
    }

    @Test
    fun localPreferenceKeepsExplicitParseErrorsVisible() {
        val markdown = "- [ ] 整理材料 #ddl 2026-05-16 23:00 #remind abc"
        val local = PlanningMarkdownParser.parse(
            markdown = markdown,
            now = LocalDateTime.of(2026, 5, 15, 8, 0),
            documentDate = LocalDate.of(2026, 5, 16)
        )

        assertEquals(PlanningParsedType.ERROR, local.candidates.single().type)
        assertTrue(shouldUseLocalPlanningResult(markdown, local))
    }

    private fun runRecognition(markdown: String, settings: AppSettings): PlanningParseResult =
        kotlinx.coroutines.runBlocking {
            PlanningRecognitionService.recognize(
                markdown = markdown,
                settings = settings,
                now = now
            )
        }
}
