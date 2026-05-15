package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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

    private fun runRecognition(markdown: String, settings: AppSettings): PlanningParseResult =
        kotlinx.coroutines.runBlocking {
            PlanningRecognitionService.recognize(
                markdown = markdown,
                settings = settings,
                now = now
            )
        }
}
