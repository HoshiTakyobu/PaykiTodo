package com.example.todoalarm.data

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanningAiProviderSerializationTest {
    @Test
    fun defaultSerializationKeepsApiKeyForLocalSettings() {
        val json = planningAiProvidersToJson(listOf(provider()))
        val item = JSONArray(json).getJSONObject(0)

        assertTrue(item.has("apiKey"))
        assertEquals("secret-key", item.getString("apiKey"))
    }

    @Test
    fun backupSerializationDropsApiKeyAndRestoresBlankKey() {
        val json = planningAiProvidersToJson(listOf(provider()), includeApiKey = false)
        val item = JSONArray(json).getJSONObject(0)

        assertFalse(item.has("apiKey"))
        val restored = planningAiProvidersFromJson(json).single()
        assertEquals("", restored.apiKey)
        assertEquals("DeepSeek", restored.name)
        assertEquals("https://api.example.com/v1", restored.baseUrl)
        assertEquals("deepseek-v4-flash", restored.model)
        assertTrue(restored.supportsVision)
    }

    private fun provider(): PlanningAiProvider {
        return PlanningAiProvider(
            id = "provider-1",
            name = "DeepSeek",
            baseUrl = "https://api.example.com/v1",
            apiKey = "secret-key",
            model = "deepseek-v4-flash",
            enabled = true,
            supportsVision = true
        )
    }
}
