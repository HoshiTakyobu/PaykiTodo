package com.example.todoalarm.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object QuoteRepository {
    suspend fun fetchRemoteQuote(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL("https://v1.hitokoto.cn/?c=d&c=i&c=k&encode=json").openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.requestMethod = "GET"
            connection.inputStream.bufferedReader().use { reader ->
                val json = JSONObject(reader.readText())
                json.optString("hitokoto").takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }
}
