package com.example.todoalarm.data

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream

class BackupArchiveCodecTest {
    @Test
    fun archiveRoundTripPreservesSnapshotAndSettings() {
        val source = sampleSnapshot()

        val restored = BackupArchiveCodec.decode(BackupArchiveCodec.encode(source))

        assertEquals(source.exportedAtMillis, restored.exportedAtMillis)
        assertEquals(source.snapshotVersion, restored.snapshotVersion)
        assertEquals(ThemeMode.DARK, restored.settings.themeMode)
        assertEquals("迁移测试", restored.groups.single().name)
        assertEquals(source.quickChecklistGroups, restored.quickChecklistGroups)
        assertEquals(source.tasks, restored.tasks)
    }

    @Test
    fun legacyJsonBackupRemainsImportable() {
        val source = sampleSnapshot()
        val legacyPayload = source.toJson().apply {
            remove("quickChecklistGroups")
            val legacyTasks = getJSONArray("tasks")
            for (index in 0 until legacyTasks.length()) {
                val task = legacyTasks.getJSONObject(index)
                task.remove("quickChecklistGroupId")
                task.remove("parentTodoId")
                task.remove("quickChecklistDepth")
                task.remove("quickChecklistSortOrder")
                task.remove("quickChecklistCollapsed")
            }
        }
        val legacyJson = legacyPayload.toString().toByteArray(StandardCharsets.UTF_8)

        val restored = BackupArchiveCodec.decode(legacyJson)

        assertEquals(source.exportedAtMillis, restored.exportedAtMillis)
        assertEquals("迁移测试", restored.groups.single().name)
        assertTrue(restored.quickChecklistGroups.isEmpty())
        assertTrue(
            restored.tasks.all {
                it.quickChecklistGroupId == null &&
                    it.parentTodoId == null &&
                    it.quickChecklistDepth == null &&
                    it.quickChecklistSortOrder == null &&
                    !it.quickChecklistCollapsed
            }
        )
    }

    @Test
    fun archiveRejectsPayloadWithWrongChecksum() {
        val payload = sampleSnapshot().toJson().toString().toByteArray(StandardCharsets.UTF_8)
        val archive = ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(
                    JSONObject()
                        .put("format", "PaykiTodoBackupArchive")
                        .put("archiveVersion", 1)
                        .put("payload", "backup.json")
                        .put("payloadSha256", "invalid")
                        .toString()
                        .toByteArray(StandardCharsets.UTF_8)
                )
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("backup.json"))
                zip.write(payload)
                zip.closeEntry()
            }
            output.toByteArray()
        }

        assertThrows(IllegalArgumentException::class.java) {
            BackupArchiveCodec.decode(archive)
        }
    }

    @Test
    fun contentFingerprintIgnoresExportTimestampButDetectsDataChanges() {
        val source = sampleSnapshot()

        assertEquals(
            BackupArchiveCodec.contentFingerprint(source),
            BackupArchiveCodec.contentFingerprint(source.copy(exportedAtMillis = 999_999_999L))
        )
        assertFalse(
            BackupArchiveCodec.contentFingerprint(source) ==
                BackupArchiveCodec.contentFingerprint(source.copy(settings = AppSettings(themeMode = ThemeMode.LIGHT)))
        )
    }

    @Test
    fun archiveExcludesLocalCredentialsAndFileAuthorization() {
        val source = sampleSnapshot().copy(
            settings = AppSettings(
                backupDirectoryUri = "content://com.android.externalstorage.documents/tree/private",
                remoteSyncSecret = "remote-secret",
                desktopSyncToken = "desktop-token",
                planningAiBaseUrl = "https://private.example.invalid/v1",
                planningAiApiKey = "ai-key",
                planningAiProviders = listOf(
                    PlanningAiProvider(
                        name = "private-ai",
                        baseUrl = "https://private.example.invalid/v1",
                        apiKey = "ai-key",
                        model = "model"
                    )
                )
            )
        )

        val payload = readArchiveEntry(BackupArchiveCodec.encode(source), "backup.json")

        val settings = payload.getJSONObject("settings")
        assertFalse(settings.has("backupDirectoryUri"))
        assertFalse(settings.has("remoteSyncSecret"))
        assertFalse(settings.has("desktopSyncToken"))
        assertFalse(settings.has("planningAiBaseUrl"))
        assertFalse(settings.has("planningAiApiKey"))
        val providers = settings.getJSONArray("planningAiProviders")
        assertTrue(providers.length() == 1)
        assertFalse(providers.getJSONObject(0).has("apiKey"))
        assertFalse(payload.toString().contains("ai-key"))
    }

    private fun readArchiveEntry(bytes: ByteArray, name: String): JSONObject {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == name) {
                    return JSONObject(zip.readBytes().toString(StandardCharsets.UTF_8))
                }
            }
        }
        error("archive entry missing: $name")
    }

    private fun sampleSnapshot(): BackupSnapshot {
        return BackupSnapshot(
            exportedAtMillis = 123_456_789L,
            groups = listOf(
                TaskGroup(
                    id = 7L,
                    name = "迁移测试",
                    colorHex = "#336699",
                    sortOrder = 0,
                    isDefault = false,
                    createdAtMillis = 100L
                )
            ),
            templates = emptyList(),
            tasks = listOf(
                TodoItem(
                    id = 21L,
                    title = "父任务",
                    notes = "说明",
                    dueAtMillis = NO_DUE_DATE_MILLIS,
                    reminderAtMillis = null,
                    reminderEnabled = false,
                    ringEnabled = false,
                    vibrateEnabled = false,
                    groupId = 7L,
                    quickChecklistGroupId = 11L,
                    quickChecklistDepth = 1,
                    quickChecklistSortOrder = 0,
                    quickChecklistCollapsed = true,
                    createdAtMillis = 101L
                ),
                TodoItem(
                    id = 22L,
                    title = "1. 子任务",
                    dueAtMillis = NO_DUE_DATE_MILLIS,
                    reminderAtMillis = null,
                    reminderEnabled = false,
                    ringEnabled = false,
                    vibrateEnabled = false,
                    groupId = 7L,
                    quickChecklistGroupId = 11L,
                    parentTodoId = 21L,
                    quickChecklistDepth = 2,
                    quickChecklistSortOrder = 0,
                    createdAtMillis = 102L
                )
            ),
            quickChecklistGroups = listOf(
                QuickChecklistGroup(
                    id = 11L,
                    title = "0913待办",
                    collapsed = false,
                    sourceFingerprint = "fingerprint",
                    createdAtMillis = 99L,
                    updatedAtMillis = 100L
                )
            ),
            settings = AppSettings(themeMode = ThemeMode.DARK)
        )
    }
}
