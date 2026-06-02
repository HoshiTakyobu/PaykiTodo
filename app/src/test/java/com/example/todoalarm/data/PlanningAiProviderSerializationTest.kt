package com.example.todoalarm.data

import org.json.JSONArray
import org.json.JSONObject
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
                desktopSyncEnabled = true,
                desktopSyncToken = "LAN-SECRET",
                planningAiEnabled = true,
                planningAiApiKey = "legacy-secret-key",
                planningAiProviders = listOf(provider())
            )
        )

        val json = snapshot.toJson()
        val settings = json.getJSONObject("settings")
        val provider = settings.getJSONArray("planningAiProviders").getJSONObject(0)

        assertFalse(settings.has("planningAiApiKey"))
        assertFalse(settings.has("desktopSyncEnabled"))
        assertFalse(settings.has("desktopSyncToken"))
        assertFalse(provider.has("apiKey"))
        assertFalse(json.toString().contains("secret-key"))
        assertFalse(json.toString().contains("LAN-SECRET"))
        assertEquals("DeepSeek", provider.getString("name"))
        assertEquals("deepseek-v4-flash", provider.getString("model"))
    }

    @Test
    fun backupSnapshotImportDisablesDesktopSyncAndIgnoresTokenFromOldBackups() {
        val snapshot = backupSnapshotFromJson(
            JSONObject().apply {
                put("exportedAtMillis", 1L)
                put("snapshotVersion", 1)
                put("settings", JSONObject().apply {
                    put("desktopSyncEnabled", true)
                    put("desktopSyncToken", "OLD-LAN-SECRET")
                })
            }
        )

        assertFalse(snapshot.settings.desktopSyncEnabled)
        assertEquals("", snapshot.settings.desktopSyncToken)
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
