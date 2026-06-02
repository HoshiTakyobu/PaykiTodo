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

    @Test
    fun backupSnapshotJsonDoesNotContainProviderOrLegacyApiKey() {
        val snapshot = BackupSnapshot(
            exportedAtMillis = 1L,
            groups = emptyList(),
            templates = emptyList(),
            tasks = emptyList(),
            settings = AppSettings(
                planningAiEnabled = true,
                planningAiApiKey = "legacy-secret-key",
                planningAiProviders = listOf(provider())
            )
        )

        val json = snapshot.toJson()
        val settings = json.getJSONObject("settings")
        val provider = settings.getJSONArray("planningAiProviders").getJSONObject(0)

        assertFalse(settings.has("planningAiApiKey"))
        assertFalse(provider.has("apiKey"))
        assertFalse(json.toString().contains("secret-key"))
        assertEquals("DeepSeek", provider.getString("name"))
        assertEquals("deepseek-v4-flash", provider.getString("model"))
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
