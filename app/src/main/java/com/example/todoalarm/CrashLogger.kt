package com.example.todoalarm

import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val CRASH_DIR = "crash_logs"
    private const val LAST_CRASH_FILE = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrash(appContext, thread, throwable) }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun readLastCrash(context: Context): String? {
        val file = context.getDir(CRASH_DIR, Context.MODE_PRIVATE).resolve(LAST_CRASH_FILE)
        return if (file.exists()) file.readText() else null
    }

    fun clearLastCrash(context: Context) {
        val file = context.getDir(CRASH_DIR, Context.MODE_PRIVATE).resolve(LAST_CRASH_FILE)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
        val dir = context.getDir(CRASH_DIR, Context.MODE_PRIVATE)
        val file = dir.resolve(LAST_CRASH_FILE)
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val stackTrace = StringWriter().also { writer ->
            throwable.printStackTrace(PrintWriter(writer))
        }.toString()

        val payload = buildString {
            appendLine("time=${formatter.format(Date())}")
            appendLine("thread=${thread.name}")
            appendLine("exception=${throwable::class.java.name}")
            appendLine()
            append(stackTrace)
        }
        file.writeText(payload)
    }
}
