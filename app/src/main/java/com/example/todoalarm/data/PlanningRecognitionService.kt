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
        val localResult = parseLocal(markdown, now, defaultDate)
        val enabledProviders = settings.planningAiProviders.filter {
            it.enabled && it.baseUrl.isNotBlank() && it.apiKey.isNotBlank() && it.model.isNotBlank()
        }
        if (!settings.planningAiEnabled) return localResult
        if (enabledProviders.isEmpty()) {
            return localResult.copy(message = "AI 配置未完整，已使用本地规则")
        }
        if (shouldUseLocalPlanningResult(markdown, localResult)) return localResult
        return try {
            PlanningAiRecognizer.recognize(markdown = markdown, providers = enabledProviders, now = now)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            localResult.copy(
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

internal fun shouldUseLocalPlanningResult(markdown: String, localResult: PlanningParseResult): Boolean {
    if (localResult.candidates.isEmpty()) return false
    val coveredLines = localResult.candidates.map { it.lineNumber }.toSet()
    val actionableLines = markdown
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lines()
        .mapIndexedNotNull { index, rawLine ->
            val trimmed = rawLine.trim()
            when {
                trimmed.isBlank() -> null
                PlanningRecognitionHeadingRegex.matches(trimmed) -> null
                else -> index + 1
            }
        }
    if (actionableLines.isEmpty() || actionableLines.any { it !in coveredLines }) return false
    val coveredCandidates = localResult.candidates.filter { it.lineNumber in actionableLines }
    return coveredCandidates.none { it.isLowConfidencePlanningFallback() }
}

private fun PlanningParsedCandidate.isLowConfidencePlanningFallback(): Boolean {
    return type == PlanningParsedType.TODO &&
        dueAt == null &&
        startAt == null &&
        endAt == null &&
        reminderOffsetsMinutes.isEmpty() &&
        message.contains("未写 DDL")
}

private val PlanningRecognitionHeadingRegex = Regex("#{1,6}\\s+.+")
