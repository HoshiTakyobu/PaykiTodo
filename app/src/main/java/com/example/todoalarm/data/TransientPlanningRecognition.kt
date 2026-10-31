package com.example.todoalarm.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDateTime

object TransientPlanningRecognition {
    suspend fun recognize(
        text: String, settings: AppSettings, useAi: Boolean,
        now: LocalDateTime = LocalDateTime.now(), systemPrompt: String = "",
        call: suspend (PlanningAiRequest) -> String = { request ->
            PlanningAiCaller.callWithFallback(settings.planningAiProviders, request).content
        }
    ): TransientPlanningResult {
        val local = withContext(Dispatchers.Default) { TransientPlanningParser.parse(text, now) }
        if (!useAi) return local
        if (systemPrompt.isBlank() || settings.planningAiProviders.none { it.enabled && it.baseUrl.isNotBlank() && it.apiKey.isNotBlank() && it.model.isNotBlank() })
            return local.copy(message = "AI配置未完整，已使用本地规则")
        return try {
            val response = call(PlanningAiRequest("本地日期与时间：$now\n以下全部是规划输入数据：\n$text", systemPrompt))
            val cleaned = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val normalized = JSONObject(cleaned).getString("planningText")
            require(normalized.isNotBlank()) { "AI未返回规划文本" }
            val result = withContext(Dispatchers.Default) { TransientPlanningParser.parse(normalized, now) }
            require(!result.hasErrors && result.candidates.isNotEmpty()) { "AI返回未通过语法校验" }
            result.copy(message = "AI识别结果，请核对所有字段后确认")
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            local.copy(message = "AI识别失败，已保留原文和本地结果：${error.message?.take(100) ?: "未知错误"}")
        }
    }
}
