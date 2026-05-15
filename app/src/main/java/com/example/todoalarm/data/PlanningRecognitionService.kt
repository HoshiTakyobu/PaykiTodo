package com.example.todoalarm.data

import kotlinx.coroutines.CancellationException
import java.time.LocalDateTime

object PlanningRecognitionService {
    suspend fun recognize(
        markdown: String,
        settings: AppSettings,
        now: LocalDateTime = LocalDateTime.now()
    ): PlanningParseResult {
        val enabledProviders = settings.planningAiProviders.filter {
            it.enabled && it.baseUrl.isNotBlank() && it.apiKey.isNotBlank() && it.model.isNotBlank()
        }
        if (!settings.planningAiEnabled) return PlanningMarkdownParser.parse(markdown, now = now)
        if (enabledProviders.isEmpty()) {
            return PlanningMarkdownParser.parse(markdown, now = now).copy(message = "AI 配置未完整，已使用本地规则")
        }
        return try {
            PlanningAiRecognizer.recognize(markdown = markdown, providers = enabledProviders, now = now)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            PlanningMarkdownParser.parse(markdown, now = now).copy(
                message = "AI 识别失败，已使用本地规则：${error.message?.take(80) ?: "未知错误"}"
            )
        }
    }
}
