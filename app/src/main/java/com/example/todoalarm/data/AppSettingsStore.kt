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

enum class ReminderAudioChannel(val label: String) {
    ALARM("闹钟通道"),
    ACCESSIBILITY("无障碍辅助通道"),
    NOTIFICATION("通知通道"),
    MEDIA("媒体通道");

    companion object {
        fun fromStorage(value: String?): ReminderAudioChannel {
            return entries.firstOrNull { it.name == value } ?: ALARM
        }
    }
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
    val reminderAudioChannel: ReminderAudioChannel = ReminderAudioChannel.ALARM,
    val reminderInternalVolumePercent: Int = 80,
    val reminderBoostSystemVolume: Boolean = false,
    val reminderBoostVolumePercent: Int = 50,
    val workQuietModeEnabled: Boolean = false,
    val quoteIndex: Int = 0,
    val backupDirectoryUri: String? = null,
    val autoBackupEnabled: Boolean = false,
    val desktopSyncEnabled: Boolean = false,
    val desktopSyncToken: String = "",
    val lastOpenedPlanningNoteId: Long? = null,
    val planningAiEnabled: Boolean = false,
    val planningAiProviderName: String = "",
    val planningAiBaseUrl: String = "",
    val planningAiApiKey: String = "",
    val planningAiModel: String = ""
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

    fun updateReminderAudioStrategy(
        channel: ReminderAudioChannel,
        internalVolumePercent: Int,
        boostSystemVolume: Boolean,
        boostVolumePercent: Int,
        workQuietModeEnabled: Boolean
    ) {
        preferences.edit()
            .putString(KEY_REMINDER_AUDIO_CHANNEL, channel.name)
            .putInt(KEY_REMINDER_INTERNAL_VOLUME, internalVolumePercent.coerceIn(0, 100))
            .putBoolean(KEY_REMINDER_BOOST_SYSTEM_VOLUME, boostSystemVolume)
            .putInt(KEY_REMINDER_BOOST_VOLUME_PERCENT, boostVolumePercent.coerceIn(0, 100))
            .putBoolean(KEY_WORK_QUIET_MODE, workQuietModeEnabled)
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

    fun updateLastOpenedPlanningNoteId(noteId: Long?) {
        preferences.edit().apply {
            if (noteId == null || noteId <= 0) {
                remove(KEY_LAST_OPENED_PLANNING_NOTE_ID)
            } else {
                putLong(KEY_LAST_OPENED_PLANNING_NOTE_ID, noteId)
            }
        }.apply()
        refresh()
    }

    fun updatePlanningAiConfig(
        enabled: Boolean,
        providerName: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ) {
        preferences.edit()
            .putBoolean(KEY_PLANNING_AI_ENABLED, enabled)
            .putString(KEY_PLANNING_AI_PROVIDER_NAME, providerName.trim())
            .putString(KEY_PLANNING_AI_BASE_URL, baseUrl.trim())
            .putString(KEY_PLANNING_AI_API_KEY, apiKey.trim())
            .putString(KEY_PLANNING_AI_MODEL, model.trim())
            .apply()
        refresh()
    }

    fun replaceAll(settings: AppSettings) {
        val preservedPlanningAiApiKey = settings.planningAiApiKey.ifBlank {
            preferences.getString(KEY_PLANNING_AI_API_KEY, null).orEmpty()
        }
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
            .putString(KEY_REMINDER_AUDIO_CHANNEL, settings.reminderAudioChannel.name)
            .putInt(KEY_REMINDER_INTERNAL_VOLUME, settings.reminderInternalVolumePercent.coerceIn(0, 100))
            .putBoolean(KEY_REMINDER_BOOST_SYSTEM_VOLUME, settings.reminderBoostSystemVolume)
            .putInt(KEY_REMINDER_BOOST_VOLUME_PERCENT, settings.reminderBoostVolumePercent.coerceIn(0, 100))
            .putBoolean(KEY_WORK_QUIET_MODE, settings.workQuietModeEnabled)
            .putInt(KEY_QUOTE_INDEX, settings.quoteIndex)
            .putString(KEY_BACKUP_DIR_URI, settings.backupDirectoryUri)
            .putBoolean(KEY_AUTO_BACKUP_ENABLED, settings.autoBackupEnabled)
            .putBoolean(KEY_DESKTOP_SYNC_ENABLED, settings.desktopSyncEnabled)
            .putString(KEY_DESKTOP_SYNC_TOKEN, settings.desktopSyncToken)
            .putBoolean(KEY_PLANNING_AI_ENABLED, settings.planningAiEnabled)
            .putString(KEY_PLANNING_AI_PROVIDER_NAME, settings.planningAiProviderName)
            .putString(KEY_PLANNING_AI_BASE_URL, settings.planningAiBaseUrl)
            .putString(KEY_PLANNING_AI_API_KEY, preservedPlanningAiApiKey)
            .putString(KEY_PLANNING_AI_MODEL, settings.planningAiModel)
            .apply {
                val noteId = settings.lastOpenedPlanningNoteId
                if (noteId == null || noteId <= 0) remove(KEY_LAST_OPENED_PLANNING_NOTE_ID) else putLong(KEY_LAST_OPENED_PLANNING_NOTE_ID, noteId)
            }
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
            reminderAudioChannel = ReminderAudioChannel.fromStorage(preferences.getString(KEY_REMINDER_AUDIO_CHANNEL, null)),
            reminderInternalVolumePercent = preferences.getInt(KEY_REMINDER_INTERNAL_VOLUME, 80).coerceIn(0, 100),
            reminderBoostSystemVolume = preferences.getBoolean(KEY_REMINDER_BOOST_SYSTEM_VOLUME, false),
            reminderBoostVolumePercent = preferences.getInt(KEY_REMINDER_BOOST_VOLUME_PERCENT, 50).coerceIn(0, 100),
            workQuietModeEnabled = preferences.getBoolean(KEY_WORK_QUIET_MODE, false),
            quoteIndex = preferences.getInt(KEY_QUOTE_INDEX, 0).coerceAtLeast(0),
            backupDirectoryUri = preferences.getString(KEY_BACKUP_DIR_URI, null),
            autoBackupEnabled = preferences.getBoolean(KEY_AUTO_BACKUP_ENABLED, false),
            desktopSyncEnabled = preferences.getBoolean(KEY_DESKTOP_SYNC_ENABLED, false),
            desktopSyncToken = syncToken,
            lastOpenedPlanningNoteId = preferences.getLong(KEY_LAST_OPENED_PLANNING_NOTE_ID, 0L).takeIf { it > 0 },
            planningAiEnabled = preferences.getBoolean(KEY_PLANNING_AI_ENABLED, false),
            planningAiProviderName = preferences.getString(KEY_PLANNING_AI_PROVIDER_NAME, null).orEmpty(),
            planningAiBaseUrl = preferences.getString(KEY_PLANNING_AI_BASE_URL, null).orEmpty(),
            planningAiApiKey = preferences.getString(KEY_PLANNING_AI_API_KEY, null).orEmpty(),
            planningAiModel = preferences.getString(KEY_PLANNING_AI_MODEL, null).orEmpty()
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
        private const val KEY_REMINDER_AUDIO_CHANNEL = "reminder_audio_channel"
        private const val KEY_REMINDER_INTERNAL_VOLUME = "reminder_internal_volume_percent"
        private const val KEY_REMINDER_BOOST_SYSTEM_VOLUME = "reminder_boost_system_volume"
        private const val KEY_REMINDER_BOOST_VOLUME_PERCENT = "reminder_boost_volume_percent"
        private const val KEY_WORK_QUIET_MODE = "work_quiet_mode_enabled"
        private const val KEY_QUOTE_INDEX = "quote_index"
        private const val KEY_BACKUP_DIR_URI = "backup_directory_uri"
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_DESKTOP_SYNC_ENABLED = "desktop_sync_enabled"
        private const val KEY_DESKTOP_SYNC_TOKEN = "desktop_sync_token"
        private const val KEY_LAST_OPENED_PLANNING_NOTE_ID = "last_opened_planning_note_id"
        private const val KEY_PLANNING_AI_ENABLED = "planning_ai_enabled"
        private const val KEY_PLANNING_AI_PROVIDER_NAME = "planning_ai_provider_name"
        private const val KEY_PLANNING_AI_BASE_URL = "planning_ai_base_url"
        private const val KEY_PLANNING_AI_API_KEY = "planning_ai_api_key"
        private const val KEY_PLANNING_AI_MODEL = "planning_ai_model"
    }
}
