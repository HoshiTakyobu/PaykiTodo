package com.example.todoalarm.sync

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class DesktopSyncServer(
    private val port: Int,
    private val requestHandler: (method: String, path: String, body: String, headers: Map<String, String>) -> Response
) {
    private val running = AtomicBoolean(false)
    private val executor = Executors.newCachedThreadPool()
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        executor.execute {
            val socket = ServerSocket(port)
            serverSocket = socket
            while (running.get()) {
                runCatching { socket.accept() }
                    .onSuccess { client -> executor.execute { handleClient(client) } }
                    .onFailure {
                        if (running.get()) {
                            stop()
                        }
                    }
            }
        }
    }

    fun stop() {
        running.set(false)
        runCatching { serverSocket?.close() }
        serverSocket = null
        executor.shutdownNow()
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 10_000
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(' ')
            if (parts.size < 2) return
            val method = parts[0].uppercase(Locale.ROOT)
            val path = parts[1].substringBefore('?')
            val headers = linkedMapOf<String, String>()
            var contentLength = 0
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) break
                val separator = line.indexOf(':')
                if (separator <= 0) continue
                val key = line.substring(0, separator).trim()
                val value = line.substring(separator + 1).trim()
                headers[key] = value
                if (key.equals("Content-Length", ignoreCase = true)) {
                    contentLength = value.toIntOrNull() ?: 0
                }
            }
            val body = if (contentLength > 0) {
                CharArray(contentLength).also { reader.read(it, 0, contentLength) }.concatToString()
            } else {
                ""
            }
            val response = requestHandler(method, path, body, headers)
            writeResponse(client.getOutputStream(), response)
        }
    }

    private fun writeResponse(output: OutputStream, response: Response) {
        val payload = response.body.toByteArray(StandardCharsets.UTF_8)
        val header = buildString {
            append("HTTP/1.1 ${response.statusCode} ${response.statusText}\r\n")
            append("Content-Type: ${response.contentType}; charset=utf-8\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Access-Control-Allow-Headers: Content-Type, Authorization, X-Payki-Token\r\n")
            append("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n")
            append("Cache-Control: no-store, no-cache, must-revalidate, max-age=0\r\n")
            append("Pragma: no-cache\r\n")
            append("Content-Length: ${payload.size}\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(StandardCharsets.UTF_8)
        output.write(header)
        output.write(payload)
        output.flush()
    }

    data class Response(
        val statusCode: Int,
        val statusText: String,
        val contentType: String,
        val body: String
    ) {
        companion object {
            fun html(body: String) = Response(200, "OK", "text/html", body)
            fun js(body: String) = Response(200, "OK", "application/javascript", body)
            fun css(body: String) = Response(200, "OK", "text/css", body)
            fun json(body: org.json.JSONObject, statusCode: Int = 200): Response {
                val text = when (statusCode) {
                    200 -> "OK"
                    401 -> "Unauthorized"
                    404 -> "Not Found"
                    else -> "Error"
                }
                return Response(statusCode, text, "application/json", body.toString())
            }
        }
    }
}
