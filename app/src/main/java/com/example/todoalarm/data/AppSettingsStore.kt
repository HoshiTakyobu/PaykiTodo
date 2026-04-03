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

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultSnoozeMinutes: Int = 10,
    val defaultRingEnabled: Boolean = true,
    val defaultVibrateEnabled: Boolean = true,
    val defaultVoiceEnabled: Boolean = false
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

    private fun refresh() {
        _settings.value = readSettings()
    }

    private fun readSettings(): AppSettings {
        val themeName = preferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == themeName } ?: ThemeMode.SYSTEM,
            defaultSnoozeMinutes = preferences.getInt(KEY_DEFAULT_SNOOZE, 10).coerceIn(5, 60),
            defaultRingEnabled = preferences.getBoolean(KEY_DEFAULT_RING, true),
            defaultVibrateEnabled = preferences.getBoolean(KEY_DEFAULT_VIBRATE, true),
            defaultVoiceEnabled = preferences.getBoolean(KEY_DEFAULT_VOICE, false)
        )
    }

    companion object {
        private const val PREFS_NAME = "payki_todo_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DEFAULT_SNOOZE = "default_snooze_minutes"
        private const val KEY_DEFAULT_RING = "default_ring_enabled"
        private const val KEY_DEFAULT_VIBRATE = "default_vibrate_enabled"
        private const val KEY_DEFAULT_VOICE = "default_voice_enabled"
    }
}
