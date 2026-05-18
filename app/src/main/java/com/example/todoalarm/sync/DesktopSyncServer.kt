package com.example.todoalarm.sync

import org.json.JSONObject
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class DesktopSyncServer(
    private val port: Int,
    private val requestHandler: suspend (method: String, path: String, body: String, headers: Map<String, String>) -> Response
) {
    private val running = AtomicBoolean(false)
    private val acceptExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "PaykiTodoDesktopSyncAccept").apply { isDaemon = true }
    }
    private val clientExecutor = ThreadPoolExecutor(
        DESKTOP_SYNC_CLIENT_CORE_THREADS,
        DESKTOP_SYNC_CLIENT_MAX_THREADS,
        30L,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(DESKTOP_SYNC_CLIENT_QUEUE_SIZE)
    ) { runnable ->
        Thread(runnable, "PaykiTodoDesktopSyncClient").apply { isDaemon = true }
    }
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        acceptExecutor.execute {
            runCatching {
                val socket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                serverSocket = socket
                while (running.get()) {
                    runCatching { socket.accept() }
                        .onSuccess { client ->
                            try {
                                clientExecutor.execute { handleClient(client) }
                            } catch (_: RejectedExecutionException) {
                                runCatching { client.close() }
                            }
                        }
                        .onFailure {
                            if (running.get()) stop()
                        }
                }
            }.onFailure {
                running.set(false)
                runCatching { serverSocket?.close() }
                serverSocket = null
            }
        }
    }

    fun stop() {
        running.set(false)
        runCatching { serverSocket?.close() }
        serverSocket = null
        acceptExecutor.shutdownNow()
        clientExecutor.shutdownNow()
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 10_000
            val response = try {
                val request = readHttpRequest(client) ?: return
                if (request.method == "OPTIONS") {
                    Response.noContent()
                } else {
                    runBlocking { requestHandler(request.method, request.path, request.body, request.headers) }
                }
            } catch (error: HttpRequestException) {
                Response.json(JSONObject().put("error", error.message), error.statusCode)
            }
            writeResponse(client.getOutputStream(), response)
        }
    }

    private fun readHttpRequest(socket: Socket): HttpRequest? {
        val input = socket.getInputStream()
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(1024)
        var boundary: HeaderBoundary? = null
        while (boundary == null) {
            val read = input.read(chunk)
            if (read < 0) return null
            buffer.write(chunk, 0, read)
            if (buffer.size() > MAX_HEADER_BYTES) {
                throw HttpRequestException(413, "请求头过大")
            }
            boundary = findHeaderBoundary(buffer.toByteArray())
        }

        val raw = buffer.toByteArray()
        val headerText = raw.copyOfRange(0, boundary.start)
            .toString(StandardCharsets.ISO_8859_1)
        val lines = headerText.replace("\r\n", "\n").split('\n')
        val requestLine = lines.firstOrNull()?.trim().orEmpty()
        val parts = requestLine.split(' ')
        if (parts.size < 2) throw HttpRequestException(400, "请求行无效")

        val headers = linkedMapOf<String, String>()
        lines.drop(1).forEach { line ->
            val separator = line.indexOf(':')
            if (separator <= 0) return@forEach
            val key = line.substring(0, separator).trim()
            val value = line.substring(separator + 1).trim()
            headers[key] = value
        }

        val contentLengthHeader = headers.entries
            .firstOrNull { it.key.equals("Content-Length", ignoreCase = true) }
            ?.value
        val contentLengthLong = contentLengthHeader?.toLongOrNull()
            ?: if (contentLengthHeader == null) 0L else throw HttpRequestException(400, "Content-Length 无效")
        if (contentLengthLong < 0) throw HttpRequestException(400, "Content-Length 无效")
        if (contentLengthLong > MAX_BODY_BYTES) throw HttpRequestException(413, "请求体过大")
        val contentLength = contentLengthLong.toInt()

        val bodyBytes = ByteArray(contentLength)
        val alreadyRead = (raw.size - boundary.endExclusive).coerceAtLeast(0)
        val copyCount = minOf(alreadyRead, contentLength)
        if (copyCount > 0) {
            raw.copyInto(bodyBytes, destinationOffset = 0, startIndex = boundary.endExclusive, endIndex = boundary.endExclusive + copyCount)
        }
        var offset = copyCount
        while (offset < contentLength) {
            val read = input.read(bodyBytes, offset, contentLength - offset)
            if (read < 0) throw HttpRequestException(400, "请求体不完整")
            offset += read
        }

        return HttpRequest(
            method = parts[0].uppercase(Locale.ROOT),
            path = parts[1],
            headers = headers,
            body = bodyBytes.toString(StandardCharsets.UTF_8)
        )
    }

    private fun findHeaderBoundary(bytes: ByteArray): HeaderBoundary? {
        for (index in 0..bytes.size - 4) {
            if (
                bytes[index] == '\r'.code.toByte() &&
                bytes[index + 1] == '\n'.code.toByte() &&
                bytes[index + 2] == '\r'.code.toByte() &&
                bytes[index + 3] == '\n'.code.toByte()
            ) {
                return HeaderBoundary(start = index, endExclusive = index + 4)
            }
        }
        for (index in 0..bytes.size - 2) {
            if (bytes[index] == '\n'.code.toByte() && bytes[index + 1] == '\n'.code.toByte()) {
                return HeaderBoundary(start = index, endExclusive = index + 2)
            }
        }
        return null
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
            fun noContent() = Response(204, "No Content", "text/plain", "")
            fun json(body: JSONObject, statusCode: Int = 200): Response {
                val text = when (statusCode) {
                    200 -> "OK"
                    400 -> "Bad Request"
                    401 -> "Unauthorized"
                    404 -> "Not Found"
                    413 -> "Payload Too Large"
                    else -> "Error"
                }
                return Response(statusCode, text, "application/json", body.toString())
            }
        }
    }

    private data class HttpRequest(
        val method: String,
        val path: String,
        val headers: Map<String, String>,
        val body: String
    )

    private data class HeaderBoundary(
        val start: Int,
        val endExclusive: Int
    )

    private class HttpRequestException(
        val statusCode: Int,
        override val message: String
    ) : Exception(message)

    private companion object {
        private const val DESKTOP_SYNC_CLIENT_CORE_THREADS = 2
        private const val DESKTOP_SYNC_CLIENT_MAX_THREADS = 8
        private const val DESKTOP_SYNC_CLIENT_QUEUE_SIZE = 32
        private const val MAX_HEADER_BYTES = 32 * 1024
        private const val MAX_BODY_BYTES = 4 * 1024 * 1024
    }
}
