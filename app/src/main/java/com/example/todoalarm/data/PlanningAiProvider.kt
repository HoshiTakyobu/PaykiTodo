package com.example.todoalarm.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PlanningAiProvider(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val enabled: Boolean = true,
    val supportsVision: Boolean = false,
    val availableModels: List<String> = emptyList()
) {
    fun normalized(): PlanningAiProvider {
        return copy(
            name = name.trim(),
            baseUrl = baseUrl.trim(),
            apiKey = apiKey.trim(),
            model = model.trim(),
            availableModels = availableModels
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
        )
    }
}

fun planningAiProvidersToJson(providers: List<PlanningAiProvider>, includeApiKey: Boolean = true): String {
    return JSONArray(
        providers.map { provider ->
            JSONObject().apply {
                put("id", provider.id)
                put("name", provider.name)
                put("baseUrl", provider.baseUrl)
                if (includeApiKey) put("apiKey", provider.apiKey)
                put("model", provider.model)
                put("enabled", provider.enabled)
                put("supportsVision", provider.supportsVision)
                put("availableModels", JSONArray(provider.availableModels))
            }
        }
    ).toString()
}

fun planningAiProvidersFromJson(raw: String?): List<PlanningAiProvider> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val provider = PlanningAiProvider(
                    id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = item.optString("name"),
                    baseUrl = item.optString("baseUrl"),
                    apiKey = item.optString("apiKey"),
                    model = item.optString("model"),
                    enabled = item.optBoolean("enabled", true),
                    supportsVision = item.optBoolean("supportsVision", false),
                    availableModels = item.optJSONArray("availableModels")?.let { models ->
                        buildList {
                            for (modelIndex in 0 until models.length()) {
                                models.optString(modelIndex).takeIf(String::isNotBlank)?.let(::add)
                            }
                        }
                    }.orEmpty()
                ).normalized()
                if (provider.name.isNotBlank() || provider.baseUrl.isNotBlank() || provider.model.isNotBlank()) {
                    add(provider)
                }
            }
        }
    }.getOrDefault(emptyList())
}
