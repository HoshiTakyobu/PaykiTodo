package com.example.todoalarm.alarm

import android.content.Context
import com.example.todoalarm.data.AiReportGenerationResult
import com.example.todoalarm.data.AiReportType

data class AiReportRuntimeState(
    val nextDailyAtMillis: Long = 0L,
    val nextDailyScheduleMode: String = "",
    val nextWeeklyAtMillis: Long = 0L,
    val nextWeeklyScheduleMode: String = "",
    val lastAttemptType: String = "",
    val lastAttemptAtMillis: Long = 0L,
    val lastSuccessType: String = "",
    val lastSuccessAtMillis: Long = 0L,
    val lastSuccessReportId: Long = 0L,
    val lastSuccessProviderName: String = "",
    val lastSuccessUsedLocalFallback: Boolean = false,
    val lastAiFailureMessage: String = "",
    val lastFailureType: String = "",
    val lastFailureAtMillis: Long = 0L,
    val lastFailureMessage: String = ""
)

class AiReportRuntimeStateStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun current(): AiReportRuntimeState {
        return AiReportRuntimeState(
            nextDailyAtMillis = preferences.getLong(KEY_NEXT_DAILY_AT, 0L),
            nextDailyScheduleMode = preferences.getString(KEY_NEXT_DAILY_MODE, "").orEmpty(),
            nextWeeklyAtMillis = preferences.getLong(KEY_NEXT_WEEKLY_AT, 0L),
            nextWeeklyScheduleMode = preferences.getString(KEY_NEXT_WEEKLY_MODE, "").orEmpty(),
            lastAttemptType = preferences.getString(KEY_LAST_ATTEMPT_TYPE, "").orEmpty(),
            lastAttemptAtMillis = preferences.getLong(KEY_LAST_ATTEMPT_AT, 0L),
            lastSuccessType = preferences.getString(KEY_LAST_SUCCESS_TYPE, "").orEmpty(),
            lastSuccessAtMillis = preferences.getLong(KEY_LAST_SUCCESS_AT, 0L),
            lastSuccessReportId = preferences.getLong(KEY_LAST_SUCCESS_REPORT_ID, 0L),
            lastSuccessProviderName = preferences.getString(KEY_LAST_SUCCESS_PROVIDER, "").orEmpty(),
            lastSuccessUsedLocalFallback = preferences.getBoolean(KEY_LAST_SUCCESS_LOCAL_FALLBACK, false),
            lastAiFailureMessage = preferences.getString(KEY_LAST_AI_FAILURE, "").orEmpty(),
            lastFailureType = preferences.getString(KEY_LAST_FAILURE_TYPE, "").orEmpty(),
            lastFailureAtMillis = preferences.getLong(KEY_LAST_FAILURE_AT, 0L),
            lastFailureMessage = preferences.getString(KEY_LAST_FAILURE_MESSAGE, "").orEmpty()
        )
    }

    fun recordScheduled(type: AiReportType, triggerAtMillis: Long, exact: Boolean) {
        val suffix = if (type == AiReportType.DAILY) "daily" else "weekly"
        preferences.edit()
            .putLong(if (suffix == "daily") KEY_NEXT_DAILY_AT else KEY_NEXT_WEEKLY_AT, triggerAtMillis)
            .putString(if (suffix == "daily") KEY_NEXT_DAILY_MODE else KEY_NEXT_WEEKLY_MODE, if (exact) "EXACT" else "INEXACT")
            .apply()
    }

    fun clearScheduled(type: AiReportType) {
        preferences.edit()
            .remove(if (type == AiReportType.DAILY) KEY_NEXT_DAILY_AT else KEY_NEXT_WEEKLY_AT)
            .remove(if (type == AiReportType.DAILY) KEY_NEXT_DAILY_MODE else KEY_NEXT_WEEKLY_MODE)
            .apply()
    }

    fun recordAttempt(type: AiReportType) {
        preferences.edit()
            .putString(KEY_LAST_ATTEMPT_TYPE, type.name)
            .putLong(KEY_LAST_ATTEMPT_AT, System.currentTimeMillis())
            .apply()
    }

    fun recordSuccess(type: AiReportType, result: AiReportGenerationResult) {
        preferences.edit()
            .putString(KEY_LAST_SUCCESS_TYPE, type.name)
            .putLong(KEY_LAST_SUCCESS_AT, System.currentTimeMillis())
            .putLong(KEY_LAST_SUCCESS_REPORT_ID, result.report.id)
            .putString(KEY_LAST_SUCCESS_PROVIDER, result.report.providerName)
            .putBoolean(KEY_LAST_SUCCESS_LOCAL_FALLBACK, result.report.isLocalFallback)
            .putString(KEY_LAST_AI_FAILURE, result.aiFailureMessage.orEmpty())
            .remove(KEY_LAST_FAILURE_TYPE)
            .remove(KEY_LAST_FAILURE_AT)
            .remove(KEY_LAST_FAILURE_MESSAGE)
            .apply()
    }

    fun recordFailure(type: AiReportType, throwable: Throwable) {
        preferences.edit()
            .putString(KEY_LAST_FAILURE_TYPE, type.name)
            .putLong(KEY_LAST_FAILURE_AT, System.currentTimeMillis())
            .putString(KEY_LAST_FAILURE_MESSAGE, sanitizeError(throwable))
            .apply()
    }

    private fun sanitizeError(throwable: Throwable): String {
        val message = throwable.message.orEmpty()
            .replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), "<服务地址>")
            .replace(Regex("(?i)(api[-_ ]?key|authorization|bearer)\\s*[:=]?\\s*\\S+")) { match ->
                "${match.groupValues[1]}=<已隐藏>"
            }
            .trim()
            .take(500)
        return if (message.isBlank()) throwable::class.java.simpleName else "${throwable::class.java.simpleName}: $message"
    }

    private companion object {
        const val PREFS_NAME = "paykitodo_ai_report_runtime"
        const val KEY_NEXT_DAILY_AT = "next_daily_at"
        const val KEY_NEXT_DAILY_MODE = "next_daily_mode"
        const val KEY_NEXT_WEEKLY_AT = "next_weekly_at"
        const val KEY_NEXT_WEEKLY_MODE = "next_weekly_mode"
        const val KEY_LAST_ATTEMPT_TYPE = "last_attempt_type"
        const val KEY_LAST_ATTEMPT_AT = "last_attempt_at"
        const val KEY_LAST_SUCCESS_TYPE = "last_success_type"
        const val KEY_LAST_SUCCESS_AT = "last_success_at"
        const val KEY_LAST_SUCCESS_REPORT_ID = "last_success_report_id"
        const val KEY_LAST_SUCCESS_PROVIDER = "last_success_provider"
        const val KEY_LAST_SUCCESS_LOCAL_FALLBACK = "last_success_local_fallback"
        const val KEY_LAST_AI_FAILURE = "last_ai_failure"
        const val KEY_LAST_FAILURE_TYPE = "last_failure_type"
        const val KEY_LAST_FAILURE_AT = "last_failure_at"
        const val KEY_LAST_FAILURE_MESSAGE = "last_failure_message"
    }
}
