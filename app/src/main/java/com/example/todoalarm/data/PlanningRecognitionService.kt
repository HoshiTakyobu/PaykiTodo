package com.example.todoalarm.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

object PlanningRecognitionService {
    suspend fun recognize(
        markdown: String,
        settings: AppSettings,
        now: LocalDateTime = LocalDateTime.now(),
        defaultDate: LocalDate? = null
    ): PlanningParseResult {
        val enabledProviders = settings.planningAiProviders.filter {
            it.enabled && it.baseUrl.isNotBlank() && it.apiKey.isNotBlank() && it.model.isNotBlank()
        }
        if (!settings.planningAiEnabled) return parseLocal(markdown, now, defaultDate)
        if (enabledProviders.isEmpty()) {
            return parseLocal(markdown, now, defaultDate).copy(message = "AI 配置未完整，已使用本地规则")
        }
        return try {
            PlanningAiRecognizer.recognize(markdown = markdown, providers = enabledProviders, now = now)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            parseLocal(markdown, now, defaultDate).copy(
                message = "AI 识别失败，已使用本地规则：${error.message?.take(80) ?: "未知错误"}"
            )
        }
    }

    suspend fun parseLocal(
        markdown: String,
        now: LocalDateTime = LocalDateTime.now(),
        defaultDate: LocalDate? = null
    ): PlanningParseResult {
        return withContext(Dispatchers.Default) {
            PlanningMarkdownParser.parse(markdown, now = now, documentDate = defaultDate)
        }
    }
}
