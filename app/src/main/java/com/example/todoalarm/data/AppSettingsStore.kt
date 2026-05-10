package com.example.todoalarm.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色")
}

enum class WeekStartMode(val label: String) {
    MONDAY("周一开始"),
    SUNDAY("周日开始")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val weekStartMode: WeekStartMode = WeekStartMode.MONDAY,
    val defaultSnoozeMinutes: Int = 10,
    val defaultRingEnabled: Boolean = true,
    val defaultVibrateEnabled: Boolean = true,
    val defaultVoiceEnabled: Boolean = false,
    val defaultCalendarReminderMode: ReminderDeliveryMode = ReminderDeliveryMode.NOTIFICATION,
    val reminderToneUri: String? = null,
    val reminderToneName: String? = null,
    val quoteIndex: Int = 0,
    val backupDirectoryUri: String? = null,
    val autoBackupEnabled: Boolean = false,
    val desktopSyncEnabled: Boolean = false,
    val desktopSyncToken: String = ""
)

class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(readSettings())

    val settingsFlow: StateFlow<AppSettings> = _settings.asStateFlow()

    fun currentSettings(): AppSettings = _settings.value

    fun updateThemeMode(themeMode: ThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
        refresh()
    }

    fun updateWeekStartMode(weekStartMode: WeekStartMode) {
        preferences.edit().putString(KEY_WEEK_START_MODE, weekStartMode.name).apply()
        refresh()
    }

    fun updateDefaultSnooze(minutes: Int) {
        preferences.edit().putInt(KEY_DEFAULT_SNOOZE, minutes).apply()
        refresh()
    }

    fun updateReminderDefaults(ringEnabled: Boolean, vibrateEnabled: Boolean, voiceEnabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_DEFAULT_RING, ringEnabled)
            .putBoolean(KEY_DEFAULT_VIBRATE, vibrateEnabled)
            .putBoolean(KEY_DEFAULT_VOICE, voiceEnabled)
            .apply()
        refresh()
    }

    fun updateDefaultCalendarReminderMode(mode: ReminderDeliveryMode) {
        preferences.edit().putString(KEY_DEFAULT_CALENDAR_REMINDER_MODE, mode.name).apply()
        refresh()
    }

    fun updateReminderTone(uri: String, name: String?) {
        preferences.edit()
            .putString(KEY_REMINDER_TONE_URI, uri)
            .putString(KEY_REMINDER_TONE_NAME, name)
            .apply()
        refresh()
    }

    fun useBuiltInReminderTone() {
        preferences.edit()
            .remove(KEY_REMINDER_TONE_URI)
            .remove(KEY_REMINDER_TONE_NAME)
            .apply()
        refresh()
    }

    fun updateQuoteIndex(index: Int) {
        preferences.edit().putInt(KEY_QUOTE_INDEX, index.coerceAtLeast(0)).apply()
        refresh()
    }

    fun updateBackupDirectoryUri(uri: String?) {
        preferences.edit().putString(KEY_BACKUP_DIR_URI, uri).apply()
        refresh()
    }

    fun updateAutoBackupEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
        refresh()
    }

    fun updateDesktopSyncEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DESKTOP_SYNC_ENABLED, enabled).apply()
        refresh()
    }

    fun rotateDesktopSyncToken(): String {
        val token = generateDesktopSyncToken()
        preferences.edit().putString(KEY_DESKTOP_SYNC_TOKEN, token).apply()
        refresh()
        return token
    }

    fun replaceAll(settings: AppSettings) {
        preferences.edit()
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putString(KEY_WEEK_START_MODE, settings.weekStartMode.name)
            .putInt(KEY_DEFAULT_SNOOZE, settings.defaultSnoozeMinutes)
            .putBoolean(KEY_DEFAULT_RING, settings.defaultRingEnabled)
            .putBoolean(KEY_DEFAULT_VIBRATE, settings.defaultVibrateEnabled)
            .putBoolean(KEY_DEFAULT_VOICE, settings.defaultVoiceEnabled)
            .putString(KEY_DEFAULT_CALENDAR_REMINDER_MODE, settings.defaultCalendarReminderMode.name)
            .putString(KEY_REMINDER_TONE_URI, settings.reminderToneUri)
            .putString(KEY_REMINDER_TONE_NAME, settings.reminderToneName)
            .putInt(KEY_QUOTE_INDEX, settings.quoteIndex)
            .putString(KEY_BACKUP_DIR_URI, settings.backupDirectoryUri)
            .putBoolean(KEY_AUTO_BACKUP_ENABLED, settings.autoBackupEnabled)
            .putBoolean(KEY_DESKTOP_SYNC_ENABLED, settings.desktopSyncEnabled)
            .putString(KEY_DESKTOP_SYNC_TOKEN, settings.desktopSyncToken)
            .apply()
        refresh()
    }

    private fun refresh() {
        _settings.value = readSettings()
    }

    private fun readSettings(): AppSettings {
        val themeName = preferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        val weekStartName = preferences.getString(KEY_WEEK_START_MODE, WeekStartMode.MONDAY.name)
        val syncToken = preferences.getString(KEY_DESKTOP_SYNC_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
            ?: generateAndPersistDesktopSyncToken()
        return AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == themeName } ?: ThemeMode.SYSTEM,
            weekStartMode = WeekStartMode.entries.firstOrNull { it.name == weekStartName } ?: WeekStartMode.MONDAY,
            defaultSnoozeMinutes = preferences.getInt(KEY_DEFAULT_SNOOZE, 10).coerceIn(5, 60),
            defaultRingEnabled = preferences.getBoolean(KEY_DEFAULT_RING, true),
            defaultVibrateEnabled = preferences.getBoolean(KEY_DEFAULT_VIBRATE, true),
            defaultVoiceEnabled = preferences.getBoolean(KEY_DEFAULT_VOICE, false),
            defaultCalendarReminderMode = ReminderDeliveryMode.fromStorage(
                preferences.getString(KEY_DEFAULT_CALENDAR_REMINDER_MODE, ReminderDeliveryMode.NOTIFICATION.name)
            ),
            reminderToneUri = preferences.getString(KEY_REMINDER_TONE_URI, null),
            reminderToneName = preferences.getString(KEY_REMINDER_TONE_NAME, null),
            quoteIndex = preferences.getInt(KEY_QUOTE_INDEX, 0).coerceAtLeast(0),
            backupDirectoryUri = preferences.getString(KEY_BACKUP_DIR_URI, null),
            autoBackupEnabled = preferences.getBoolean(KEY_AUTO_BACKUP_ENABLED, false),
            desktopSyncEnabled = preferences.getBoolean(KEY_DESKTOP_SYNC_ENABLED, false),
            desktopSyncToken = syncToken
        )
    }

    private fun generateAndPersistDesktopSyncToken(): String {
        val token = generateDesktopSyncToken()
        preferences.edit().putString(KEY_DESKTOP_SYNC_TOKEN, token).apply()
        return token
    }

    private fun generateDesktopSyncToken(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(4) {
            repeat(4) {
                append(alphabet.random())
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "payki_todo_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_WEEK_START_MODE = "week_start_mode"
        private const val KEY_DEFAULT_SNOOZE = "default_snooze_minutes"
        private const val KEY_DEFAULT_RING = "default_ring_enabled"
        private const val KEY_DEFAULT_VIBRATE = "default_vibrate_enabled"
        private const val KEY_DEFAULT_VOICE = "default_voice_enabled"
        private const val KEY_DEFAULT_CALENDAR_REMINDER_MODE = "default_calendar_reminder_mode"
        private const val KEY_REMINDER_TONE_URI = "reminder_tone_uri"
        private const val KEY_REMINDER_TONE_NAME = "reminder_tone_name"
        private const val KEY_QUOTE_INDEX = "quote_index"
        private const val KEY_BACKUP_DIR_URI = "backup_directory_uri"
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_DESKTOP_SYNC_ENABLED = "desktop_sync_enabled"
        private const val KEY_DESKTOP_SYNC_TOKEN = "desktop_sync_token"
    }
}
