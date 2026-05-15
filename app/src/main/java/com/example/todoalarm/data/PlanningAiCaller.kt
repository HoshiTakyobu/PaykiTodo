package com.example.todoalarm.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

data class PlanningAiRequest(
    val prompt: String,
    val systemPrompt: String = ""
)

data class PlanningAiResponse(
    val content: String,
    val provider: PlanningAiProvider
)

class PlanningAiHttpException(
    val statusCode: Int,
    message: String
) : IOException(message)

object PlanningAiCaller {
    suspend fun callWithFallback(
        providers: List<PlanningAiProvider>,
        request: PlanningAiRequest
    ): PlanningAiResponse {
        var lastRetryable: Exception? = null
        val usableProviders = providers
            .map { it.normalized() }
            .filter { it.enabled && it.baseUrl.isNotBlank() && it.apiKey.isNotBlank() && it.model.isNotBlank() }
        for (provider in usableProviders) {
            try {
                return callSingle(provider, request)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!isRetryable(error)) throw error
                lastRetryable = error
            }
        }
        throw lastRetryable ?: IllegalStateException("没有可用的 AI 源，请检查 Base URL、API Key 和模型名。")
    }

    private suspend fun callSingle(
        provider: PlanningAiProvider,
        request: PlanningAiRequest
    ): PlanningAiResponse = withContext(Dispatchers.IO) {
        val base = provider.baseUrl.trimEnd('/')
        val endpoint = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        val body = JSONObject().apply {
            put("model", provider.model)
            put(
                "messages",
                org.json.JSONArray().apply {
                    if (request.systemPrompt.isNotBlank()) {
                        put(JSONObject().put("role", "system").put("content", request.systemPrompt))
                    }
                    put(JSONObject().put("role", "user").put("content", request.prompt))
                }
            )
        }.toString()

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${provider.apiKey}")
        }
        try {
            connection.outputStream.bufferedWriter().use { it.write(body) }
            val code = connection.responseCode
            val responseText = if (code in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            if (code !in 200..299) {
                throw PlanningAiHttpException(code, responseText.ifBlank { "HTTP $code" })
            }
            val content = JSONObject(responseText)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            PlanningAiResponse(content = content, provider = provider)
        } finally {
            connection.disconnect()
        }
    }

    private fun isRetryable(error: Exception): Boolean {
        return when (error) {
            is CancellationException -> false
            is SocketTimeoutException, is UnknownHostException -> true
            is PlanningAiHttpException -> error.statusCode in setOf(401, 403, 429, 500, 502, 503, 504)
            is IOException -> true
            else -> false
        }
    }
}
