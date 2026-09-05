package com.example.todoalarm.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONArray
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

data class PlanningAiVisionRequest(
    val prompt: String,
    val systemPrompt: String,
    val imageBase64: String,
    val imageMimeType: String
)

data class PlanningAiResponse(
    val content: String,
    val provider: PlanningAiProvider,
    val switched: Boolean = false
)

sealed class PlanningAiTestResult {
    data class Success(val providerName: String, val model: String, val responseText: String) : PlanningAiTestResult()
    data class Failure(val message: String) : PlanningAiTestResult()
}

sealed class PlanningAiModelFetchResult {
    data class Success(val providerName: String, val endpoint: String, val models: List<String>) : PlanningAiModelFetchResult()
    data class Failure(val message: String) : PlanningAiModelFetchResult()
}

class PlanningAiHttpException(
    val statusCode: Int,
    message: String
) : IOException(message)

class PlanningAiNonJsonException(message: String) : IOException(message)

object PlanningAiCaller {
    internal const val CONNECTIVITY_TEST_PROMPT =
        "Please introduce which model exactly you are, and just answer the specific model name."

    suspend fun callWithFallback(
        providers: List<PlanningAiProvider>,
        request: PlanningAiRequest,
        onSelectionChanged: suspend (PlanningAiProvider) -> Unit = {}
    ): PlanningAiResponse {
        var lastRetryable: Exception? = null
        val primary = providers
            .map { it.normalized() }
            .firstOrNull { it.enabled && it.baseUrl.isNotBlank() && it.apiKey.isNotBlank() && it.model.isNotBlank() }
        val candidates = providerCandidates(providers)
        for (provider in candidates) {
            try {
                val response = callSingle(provider, request)
                val switched = primary != null && (provider.id != primary.id || provider.model != primary.model)
                if (switched) onSelectionChanged(provider)
                return response.copy(switched = switched)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!isRetryable(error)) throw error
                lastRetryable = error
            }
        }
        throw lastRetryable ?: IllegalStateException("没有可用的 AI 源，请检查 Base URL、API Key 和模型名。")
    }

    suspend fun callVisionWithFallback(
        providers: List<PlanningAiProvider>,
        request: PlanningAiVisionRequest,
        onSelectionChanged: suspend (PlanningAiProvider) -> Unit = {}
    ): PlanningAiResponse {
        var lastRetryable: Exception? = null
        val primary = providers
            .map { it.normalized() }
            .firstOrNull {
                it.enabled &&
                    it.supportsVision &&
                    it.baseUrl.isNotBlank() &&
                    it.apiKey.isNotBlank() &&
                    it.model.isNotBlank()
            }
        val candidates = providerCandidates(providers, requireVision = true)
        for (provider in candidates) {
            try {
                val response = callVisionSingle(provider, request)
                val switched = primary != null && (provider.id != primary.id || provider.model != primary.model)
                if (switched) onSelectionChanged(provider)
                return response.copy(switched = switched)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!isRetryable(error)) throw error
                lastRetryable = error
            }
        }
        throw lastRetryable ?: IllegalStateException("请在设置中为某个 AI 源开启图片识别支持")
    }

    suspend fun testProvider(provider: PlanningAiProvider): PlanningAiTestResult {
        val normalized = provider.normalized()
        if (normalized.baseUrl.isBlank() || normalized.apiKey.isBlank() || normalized.model.isBlank()) {
            return PlanningAiTestResult.Failure("Base URL、API Key 和模型名不能为空")
        }
        return try {
            val testProvider = normalized.copy(enabled = true)
            if (endpointCandidates(testProvider.baseUrl).isEmpty()) {
                return PlanningAiTestResult.Failure("Base URL 无法生成可用的接口地址")
            }
            val response = callSingle(
                provider = testProvider,
                request = PlanningAiRequest(prompt = CONNECTIVITY_TEST_PROMPT),
                connectTimeoutMs = 10_000,
                readTimeoutMs = 20_000,
                maxTokens = 32,
                temperature = 0.0
            )
            if (response.content.isBlank()) {
                PlanningAiTestResult.Failure("响应为空")
            } else {
                PlanningAiTestResult.Success(
                    providerName = normalized.name.ifBlank { "未命名服务" },
                    model = normalized.model,
                    responseText = response.content.trim().take(120)
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: PlanningAiHttpException) {
            PlanningAiTestResult.Failure("HTTP ${error.statusCode}: ${error.message?.take(120) ?: "请求失败"}")
        } catch (error: Exception) {
            PlanningAiTestResult.Failure(error.message?.take(120) ?: "未知错误")
        }
    }

    suspend fun fetchModels(provider: PlanningAiProvider): PlanningAiModelFetchResult {
        val normalized = provider.normalized()
        if (normalized.baseUrl.isBlank() || normalized.apiKey.isBlank()) {
            return PlanningAiModelFetchResult.Failure("Base URL 和 API Key 不能为空")
        }
        return try {
            val result = fetchModelsSingle(
                provider = normalized.copy(enabled = true),
                connectTimeoutMs = 10_000,
                readTimeoutMs = 20_000
            )
            PlanningAiModelFetchResult.Success(
                providerName = normalized.name.ifBlank { "未命名服务" },
                endpoint = result.endpoint,
                models = prioritizeModelNames(result.models)
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: PlanningAiHttpException) {
            val hint = when (error.statusCode) {
                401, 403 -> "API Key 无效、无权限，或该服务不允许列出模型。"
                404 -> "Base URL 可能不支持 /models，请检查是否应填写服务根地址或 /v1。"
                else -> error.message?.take(120) ?: "请求失败"
            }
            PlanningAiModelFetchResult.Failure("HTTP ${error.statusCode}: $hint")
        } catch (error: Exception) {
            PlanningAiModelFetchResult.Failure(error.message?.take(160) ?: "未知错误")
        }
    }

    private suspend fun callSingle(
        provider: PlanningAiProvider,
        request: PlanningAiRequest,
        connectTimeoutMs: Int = 15_000,
        readTimeoutMs: Int = 45_000,
        maxTokens: Int? = null,
        temperature: Double? = null
    ): PlanningAiResponse = withContext(Dispatchers.IO) {
        val body = buildChatCompletionBody(
            provider = provider,
            request = request,
            maxTokens = maxTokens,
            temperature = temperature
        )

        var lastError: Exception? = null
        val endpoints = endpointCandidates(provider.baseUrl)
        for ((index, endpoint) in endpoints.withIndex()) {
            try {
                return@withContext postChatCompletion(
                    endpoint = endpoint,
                    provider = provider,
                    body = body,
                    connectTimeoutMs = connectTimeoutMs,
                    readTimeoutMs = readTimeoutMs
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                lastError = error
                if (index >= endpoints.lastIndex || !shouldTryNextEndpoint(error)) {
                    throw error
                }
            }
        }
        throw lastError ?: IOException("没有可用的 AI 接口地址。")
    }

    internal fun buildChatCompletionBody(
        provider: PlanningAiProvider,
        request: PlanningAiRequest,
        maxTokens: Int? = null,
        temperature: Double? = null
    ): String {
        return JSONObject().apply {
            put("model", provider.model)
            put(
                "messages",
                JSONArray().apply {
                    if (request.systemPrompt.isNotBlank()) {
                        put(JSONObject().put("role", "system").put("content", request.systemPrompt))
                    }
                    put(JSONObject().put("role", "user").put("content", request.prompt))
                }
            )
            maxTokens?.let { put("max_tokens", it) }
            temperature?.let { put("temperature", it) }
        }.toString()
    }

    private suspend fun callVisionSingle(
        provider: PlanningAiProvider,
        request: PlanningAiVisionRequest,
        connectTimeoutMs: Int = 15_000,
        readTimeoutMs: Int = 60_000
    ): PlanningAiResponse = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", provider.model)
            put(
                "messages",
                JSONArray().apply {
                    if (request.systemPrompt.isNotBlank()) {
                        put(JSONObject().put("role", "system").put("content", request.systemPrompt))
                    }
                    put(
                        JSONObject()
                            .put("role", "user")
                            .put(
                                "content",
                                JSONArray().apply {
                                    put(JSONObject().put("type", "text").put("text", request.prompt))
                                    put(
                                        JSONObject()
                                            .put("type", "image_url")
                                            .put(
                                                "image_url",
                                                JSONObject().put(
                                                    "url",
                                                    "data:${request.imageMimeType};base64,${request.imageBase64}"
                                                )
                                            )
                                    )
                                }
                            )
                    )
                }
            )
        }.toString()

        var lastError: Exception? = null
        val endpoints = endpointCandidates(provider.baseUrl)
        for ((index, endpoint) in endpoints.withIndex()) {
            try {
                return@withContext postChatCompletion(
                    endpoint = endpoint,
                    provider = provider,
                    body = body,
                    connectTimeoutMs = connectTimeoutMs,
                    readTimeoutMs = readTimeoutMs
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                lastError = error
                if (index >= endpoints.lastIndex || !shouldTryNextEndpoint(error)) {
                    throw error
                }
            }
        }
        throw lastError ?: IOException("没有可用的 AI 接口地址。")
    }

    internal fun endpointCandidates(rawBaseUrl: String): List<String> {
        val base = rawBaseUrl.trim().trimEnd('/')
        if (base.isBlank()) return emptyList()
        return when {
            base.endsWith("/chat/completions", ignoreCase = true) -> listOf(base)
            base.endsWith("/models", ignoreCase = true) -> {
                val prefix = base.replace(Regex("/models$", RegexOption.IGNORE_CASE), "")
                listOf("$prefix/chat/completions")
            }
            base.endsWith("/v1", ignoreCase = true) -> listOf("$base/chat/completions")
            else -> listOf(
                "$base/v1/chat/completions",
                "$base/chat/completions"
            )
        }.distinct()
    }

    internal fun modelEndpointCandidates(rawBaseUrl: String): List<String> {
        val base = rawBaseUrl.trim().trimEnd('/')
        if (base.isBlank()) return emptyList()
        return when {
            base.endsWith("/models", ignoreCase = true) -> listOf(base)
            base.endsWith("/chat/completions", ignoreCase = true) -> {
                val prefix = base.replace(Regex("/chat/completions$", RegexOption.IGNORE_CASE), "")
                listOf("$prefix/models")
            }
            base.endsWith("/v1", ignoreCase = true) -> listOf("$base/models")
            else -> listOf(
                "$base/v1/models",
                "$base/models"
            )
        }.distinct()
    }

    internal fun providerCandidates(
        providers: List<PlanningAiProvider>,
        requireVision: Boolean = false
    ): List<PlanningAiProvider> {
        val usable = providers
            .map { it.normalized() }
            .filter {
                it.enabled &&
                    (!requireVision || it.supportsVision) &&
                    it.baseUrl.isNotBlank() &&
                    it.apiKey.isNotBlank() &&
                    it.model.isNotBlank()
            }
        val current = usable.firstOrNull() ?: return emptyList()
        val expanded = usable.flatMap { provider ->
            (listOf(provider.model) + provider.availableModels)
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .map { model -> provider.copy(model = model) }
        }
        val preferred = expanded.filter { candidate -> isPreferredModel(candidate.model) }
        val currentProvider = expanded.filter { candidate -> candidate.id == current.id }
        val remaining = expanded.filter { candidate -> candidate.id != current.id }
        return (preferred + currentProvider + remaining)
            .distinctBy { candidate -> "${candidate.id}\u0000${candidate.model.lowercase()}" }
    }

    internal fun prioritizeModelNames(models: List<String>): List<String> {
        return models
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sortedWith(compareBy<String> { if (isPreferredModel(it)) 0 else 1 }.thenBy { it.lowercase() })
    }

    private fun isPreferredModel(model: String): Boolean {
        val normalized = model.lowercase()
        return PREFERRED_MODEL_KEYWORDS.any(normalized::contains)
    }

    private data class ModelFetchPayload(
        val endpoint: String,
        val models: List<String>
    )

    private suspend fun fetchModelsSingle(
        provider: PlanningAiProvider,
        connectTimeoutMs: Int,
        readTimeoutMs: Int
    ): ModelFetchPayload = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        val endpoints = modelEndpointCandidates(provider.baseUrl)
        for ((index, endpoint) in endpoints.withIndex()) {
            try {
                return@withContext getModels(
                    endpoint = endpoint,
                    provider = provider,
                    connectTimeoutMs = connectTimeoutMs,
                    readTimeoutMs = readTimeoutMs
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                lastError = error
                if (index >= endpoints.lastIndex || !shouldTryNextEndpoint(error)) {
                    throw error
                }
            }
        }
        throw lastError ?: IOException("没有可用的模型列表接口地址。")
    }

    private fun getModels(
        endpoint: String,
        provider: PlanningAiProvider,
        connectTimeoutMs: Int,
        readTimeoutMs: Int
    ): ModelFetchPayload {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", CODEX_CLI_USER_AGENT)
            setRequestProperty("Authorization", "Bearer ${provider.apiKey}")
        }
        try {
            val code = connection.responseCode
            val responseText = if (code in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            if (code !in 200..299) {
                throw PlanningAiHttpException(code, responseText.ifBlank { "HTTP $code" })
            }
            if (!looksLikeJson(responseText)) {
                throw PlanningAiNonJsonException(
                    "服务返回的不是 JSON，请检查 Base URL 是否填到了网页入口；模型列表通常需要 /v1/models。响应开头：${responsePreview(responseText)}"
                )
            }
            val models = try {
                parseModelIds(responseText)
            } catch (error: JSONException) {
                throw PlanningAiNonJsonException(
                    "服务返回的 JSON 无法解析，请确认该接口兼容 OpenAI /models。响应开头：${responsePreview(responseText)}"
                )
            }
            if (models.isEmpty()) {
                throw IOException("模型列表响应中没有可用的 data[].id；该 Base URL 可能不兼容 OpenAI /models。")
            }
            return ModelFetchPayload(endpoint = endpoint, models = models)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseModelIds(responseText: String): List<String> {
        val trimmed = responseText.trimStart()
        val rawModels = when {
            trimmed.startsWith("[") -> collectModelIds(JSONArray(trimmed))
            else -> {
                val json = JSONObject(responseText)
                val data = json.optJSONArray("data")
                val models = json.optJSONArray("models")
                when {
                    data != null -> collectModelIds(data)
                    models != null -> collectModelIds(models)
                    json.optString("id").isNotBlank() -> listOf(json.optString("id"))
                    else -> emptyList()
                }
            }
        }
        return rawModels
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun collectModelIds(array: JSONArray): List<String> {
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.opt(index)
                val id = when (item) {
                    is JSONObject -> item.optString("id")
                    is String -> item
                    else -> ""
                }
                if (id.isNotBlank()) add(id)
            }
        }
    }

    private fun postChatCompletion(
        endpoint: String,
        provider: PlanningAiProvider,
        body: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
        allowBlankContent: Boolean = false
    ): PlanningAiResponse {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", CODEX_CLI_USER_AGENT)
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
            if (!looksLikeJson(responseText)) {
                throw PlanningAiNonJsonException(
                    "服务返回的不是 JSON，请检查 Base URL 是否应填写到 /v1 或完整 /v1/chat/completions。响应开头：${responsePreview(responseText)}"
                )
            }
            val json = try {
                JSONObject(responseText)
            } catch (error: JSONException) {
                throw PlanningAiNonJsonException(
                    "服务返回的 JSON 无法解析，请确认该接口兼容 OpenAI chat/completions。响应开头：${responsePreview(responseText)}"
                )
            }
            val content = json
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            if (content.isBlank() && !allowBlankContent) {
                throw IOException("服务已返回 JSON，但不是 OpenAI-compatible chat/completions 响应格式。")
            }
            return PlanningAiResponse(content = content, provider = provider)
        } finally {
            connection.disconnect()
        }
    }

    private fun shouldTryNextEndpoint(error: Exception): Boolean {
        return when (error) {
            is PlanningAiNonJsonException -> true
            is PlanningAiHttpException -> error.statusCode in setOf(400, 404, 405)
            else -> false
        }
    }

    private fun looksLikeJson(text: String): Boolean {
        val trimmed = text.trimStart()
        return trimmed.startsWith("{") || trimmed.startsWith("[")
    }

    private fun responsePreview(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim().take(120).ifBlank { "空响应" }
    }

    private fun isRetryable(error: Exception): Boolean {
        return when (error) {
            is CancellationException -> false
            is SocketTimeoutException, is UnknownHostException -> true
            is PlanningAiHttpException -> error.statusCode in setOf(
                400, 401, 403, 404, 405, 408, 409, 422, 429,
                500, 501, 502, 503, 504
            )
            is IOException -> true
            else -> false
        }
    }

    private val PREFERRED_MODEL_KEYWORDS = listOf("glm", "deepseek", "gemini", "minimax", "qwen")
}
