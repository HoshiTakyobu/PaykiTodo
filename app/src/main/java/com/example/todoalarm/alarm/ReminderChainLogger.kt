package com.example.todoalarm.alarm

import android.content.Context
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.ReminderChainLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object ReminderChainLogger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun log(
        context: Context,
        todoId: Long,
        source: String,
        stage: String,
        status: String,
        message: String? = null,
        reminderAtMillis: Long? = null,
        chainKey: String? = null
    ) {
        if (todoId <= 0L) return
        val app = context.applicationContext as? TodoApplication ?: return
        val key = chainKey ?: defaultChainKey(todoId, reminderAtMillis)
        scope.launch {
            app.repository.addReminderChainLog(
                ReminderChainLog(
                    todoId = todoId,
                    chainKey = key,
                    source = source,
                    stage = stage,
                    status = status,
                    message = message,
                    reminderAtMillis = reminderAtMillis
                )
            )
        }
    }

    fun defaultChainKey(todoId: Long, reminderAtMillis: Long?): String {
        return if (reminderAtMillis != null && reminderAtMillis > 0L) {
            "$todoId@$reminderAtMillis"
        } else {
            "$todoId@active"
        }
    }
}
