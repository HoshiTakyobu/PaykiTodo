package com.example.todoalarm.sync

import android.content.Context
import java.nio.charset.StandardCharsets

object DesktopSyncWebAssets {
    private const val BASE_PATH = "desktop-web"

    fun indexHtml(context: Context): String = readAsset(context, "index.html")

    fun appCss(context: Context): String = readAsset(context, "app.css")

    fun appJs(context: Context): String = readAsset(context, "app.js")

    private fun readAsset(context: Context, fileName: String): String {
        return runCatching {
            context.assets.open("$BASE_PATH/$fileName").use { input ->
                input.readBytes().toString(StandardCharsets.UTF_8)
            }
        }.getOrElse { throwable ->
            fallbackAsset(fileName, throwable)
        }
    }

    private fun fallbackAsset(fileName: String, throwable: Throwable): String {
        val message = "PaykiTodo desktop asset missing: $fileName (${throwable.javaClass.simpleName})"
        return when (fileName) {
            "index.html" -> """
                <!doctype html>
                <html lang="zh-CN">
                <head><meta charset="utf-8" /><title>PaykiTodo Desktop Sync</title></head>
                <body><h1>PaykiTodo 电脑端资源加载失败</h1><p>$message</p></body>
                </html>
            """.trimIndent()
            "app.css" -> "body{font-family:sans-serif;padding:24px;}"
            "app.js" -> "console.error(${message.toJsStringLiteral()});"
            else -> message
        }
    }

    private fun String.toJsStringLiteral(): String {
        return "'" + replace("\\", "\\\\").replace("'", "\\'") + "'"
    }
}
