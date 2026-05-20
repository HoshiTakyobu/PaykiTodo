package com.example.todoalarm

import android.content.Context

object SafeStartupGuard {
    private const val PREFS_NAME = "paykitodo_safe_startup_guard"
    private const val KEY_LAST_START_AT = "last_start_at"
    private const val KEY_LAST_SUCCESS_AT = "last_success_at"
    private const val KEY_CRASH_STREAK = "crash_streak"
    private const val CRASH_WINDOW_MILLIS = 10_000L
    private const val SAFE_MODE_THRESHOLD = 2

    fun enterStartup(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastStartAt = prefs.getLong(KEY_LAST_START_AT, 0L)
        val lastSuccessAt = prefs.getLong(KEY_LAST_SUCCESS_AT, 0L)
        val previousLooksCrashed = lastStartAt > lastSuccessAt && now - lastStartAt <= CRASH_WINDOW_MILLIS
        val streak = if (previousLooksCrashed) {
            prefs.getInt(KEY_CRASH_STREAK, 0) + 1
        } else {
            0
        }
        prefs.edit()
            .putLong(KEY_LAST_START_AT, now)
            .putInt(KEY_CRASH_STREAK, streak)
            .apply()
        return streak >= SAFE_MODE_THRESHOLD
    }

    fun markStartupSuccessful(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SUCCESS_AT, System.currentTimeMillis())
            .putInt(KEY_CRASH_STREAK, 0)
            .apply()
    }
}
