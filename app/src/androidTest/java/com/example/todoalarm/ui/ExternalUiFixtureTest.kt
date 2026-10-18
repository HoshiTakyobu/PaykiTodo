package com.example.todoalarm.ui

import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

/** Explicit fixture setup only. External ADB owns the user interaction and backup restoration. */
class ExternalUiFixtureTest {
    @Test fun prepareExpiredAccount() = runBlocking {
        val inst = InstrumentationRegistry.getInstrumentation()
        org.junit.Assume.assumeTrue("Requires a private-data backup and external restoration", InstrumentationRegistry.getArguments().getString("externalFixture") == "true")
        val app = inst.targetContext.applicationContext as TodoApplication
        app.remoteSyncCoordinator.stop()
        val original = app.repository.exportSnapshot(app.settingsStore.currentSettings())
        app.repository.importSnapshot(original.copy(tasks = emptyList(), quickChecklistGroups = emptyList(),
            planningNotes = emptyList(), planningNodes = emptyList(), planningLineMappings = emptyList()), notify = false)
        val lan = InstrumentationRegistry.getArguments().getString("lanFixture") == "true"
        val expiry = if (lan) System.currentTimeMillis() / 1000 + 3600 else 1
        app.settingsStore.saveRemoteAccountSession("https://example.invalid", "external_ui_fixture", "fixture-device", "fixture-access", "fixture-refresh", expiry, expiry, "fixture-secret", 7, true)
        app.settingsStore.updateThemeMode(ThemeMode.DARK)
        for (name in listOf("preferences", "securePreferences")) {
            val prefs = AppSettingsStore::class.java.getDeclaredField(name).apply { isAccessible = true }.get(app.settingsStore) as SharedPreferences
            if (name == "preferences" && lan) prefs.edit().putBoolean("remote_sync_enabled", false)
                .putBoolean("desktop_sync_enabled", true).putString("desktop_sync_token", "ABCD").commit()
            prefs.edit().commit()
        }
        val delay = InstrumentationRegistry.getArguments().getString("reminderDelayMs")?.toLongOrNull() ?: 600000L
        require(delay in 30000L..3600000L)
        val at = System.currentTimeMillis() + delay
        val task = app.repository.addTodo(TodoItem(title = "External offline reminder", dueAtMillis = at + 60000,
            reminderAtMillis = at, reminderEnabled = true, ringEnabled = false, vibrateEnabled = false))
        app.alarmScheduler.schedule(task)
        Unit
    }
}
