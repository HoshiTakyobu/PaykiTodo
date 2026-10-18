package com.example.todoalarm.sync

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.alarm.ReminderDispatchTracker
import com.example.todoalarm.alarm.ReminderForegroundService
import com.example.todoalarm.alarm.ReminderNotifier
import com.example.todoalarm.data.AppSettingsStore
import com.example.todoalarm.data.ThemeMode
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Uses only synthetic account/task fixtures and restores all account preferences. */
class AccountReminderReliabilityTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private val app get() = inst.targetContext.applicationContext as TodoApplication
    private val store get() = app.settingsStore
    private lateinit var saved: List<Pair<SharedPreferences, Map<String, *>>>
    private val fixtures = mutableListOf<TodoItem>()
    private var activity: ActivityScenario<MainActivity>? = null

    @Before fun setup() {
        app.remoteSyncCoordinator.stop()
        // Read decrypted values only in memory; no credentials are exported as evidence.
        val accountPreferences = listOf("preferences", "securePreferences").map { field ->
            val member = AppSettingsStore::class.java.getDeclaredField(field).apply { isAccessible = true }
            member.get(store) as SharedPreferences
        }
        saved = (accountPreferences + app.getSharedPreferences("account_expiry", Context.MODE_PRIVATE)).map { it to it.all.toMap() }
        store.saveRemoteAccountSession("https://example.invalid", "reminder_fixture", "fixture-device", "fixture-access", "fixture-refresh",
            1, 1, "fixture-secret", 7, true)
        assertTrue(store.hasLocalAccountAccess()); assertFalse(store.hasValidRemoteSession())
    }

    @After fun restore() = runBlocking {
        activity?.close()
        fixtures.forEach { task ->
            ReminderForegroundService.dismiss(app, task.id)
            app.alarmScheduler.cancel(task.id)
            app.reminderNotifier.cancel(task.id)
            ReminderDispatchTracker.clear(app, task.id)
            app.repository.deleteTodo(task)
        }
        AccountExpiryNotifier.cancel(app)
        saved.forEach { (prefs, values) ->
            prefs.edit().clear().apply {
                values.forEach { (key, value) -> when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Long -> putLong(key, value)
                    is Int -> putInt(key, value)
                    is Float -> putFloat(key, value)
                    is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                } }
            }.commit()
        }
        val originalTheme = AppSettingsStore(app).currentSettings().themeMode
        store.updateThemeMode(originalTheme) // refreshes the live settings flow after restoration
        AccountExpiryNotifier.schedule(app)
        app.remoteSyncCoordinator.start()
    }

    @Test fun expiryNotifiesOnceRetainsDirtyEditsAndExplicitLogoutLocksAccess() {
        store.clearExpiredRemoteSessionIfNeeded()
        val manager = app.getSystemService(NotificationManager::class.java)
        val first = manager.activeNotifications.single { it.id == 19001 }
        store.clearExpiredRemoteSessionIfNeeded()
        assertEquals(first.postTime, manager.activeNotifications.single { it.id == 19001 }.postTime)
        assertTrue(store.currentSettings().remoteSyncDirty)
        assertEquals(7L, store.currentSettings().remoteSyncRevision)
        store.markRemoteSyncDirty(); val inFlight = store.remoteChangeGeneration()
        store.markRemoteSyncDirty(); store.markRemoteSyncSuccess(8, inFlight)
        assertTrue("New changes must survive completion of an older upload", store.currentSettings().remoteSyncDirty)
        store.clearRemoteAccount()
        assertFalse(store.hasLocalAccountAccess())
        assertFalse(manager.activeNotifications.any { it.id == 19001 })
    }

    @Test fun expiredAccountReceivesRealBackgroundAlarmWithoutCloudAccess() = runBlocking {
        val at = System.currentTimeMillis() + 7_000
        val task = app.repository.addTodo(TodoItem(title = "离线过期账号提醒验收", dueAtMillis = at + 60_000,
            reminderAtMillis = at, reminderEnabled = true, ringEnabled = false, vibrateEnabled = false, voiceEnabled = false))
        fixtures += task
        assertNull(app.alarmScheduler.schedule(task))
        inst.uiAutomation.executeShellCommand("input keyevent KEYCODE_HOME").close()
        val manager = app.getSystemService(NotificationManager::class.java)
        val deadline = System.currentTimeMillis() + 25_000
        while (manager.activeNotifications.none { it.id == ReminderNotifier.notificationId(task.id) } && System.currentTimeMillis() < deadline) Thread.sleep(200)
        assertTrue("AlarmManager must deliver a notification for an expired local account", manager.activeNotifications.any { it.id == ReminderNotifier.notificationId(task.id) })
        assertTrue(ReminderDispatchTracker.wasDispatched(app, task.id, at))
        screenshot("android-expired-offline-reminder.png")
    }

    @Test fun expiryNotificationOpensAccountSettingsInNeutralDarkTheme() {
        store.updateThemeMode(ThemeMode.DARK)
        activity = ActivityScenario.launch(Intent(app, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_SETTINGS_SECTION, "ACCOUNT"))
        compose.onNodeWithText("账号设置").assertIsDisplayed()
        compose.onNodeWithText("重新登录").assertIsDisplayed()
        compose.onNodeWithText("登录已过期。本机数据和离线提醒继续可用，重新登录后补传修改。").assertIsDisplayed()
        screenshot("android-account-dark.png")
        compose.onNodeWithText("完成").performClick()
        compose.waitForIdle()
        screenshot("android-settings-dark.png")
    }

    private fun screenshot(name: String) {
        val folder = File(app.getExternalFilesDir(null), "ui-acceptance").apply { mkdirs() }
        inst.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(folder, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        }
    }
}
