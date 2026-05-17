package com.example.todoalarm.sync

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

class DesktopSyncServerTest {
    @Test
    fun readsUtf8RequestBodyByByteLength() {
        val port = freePort()
        val bodyText = JSONObject()
            .put("content", "规划台中文长文本".repeat(400))
            .toString()
        val bodyBytes = bodyText.toByteArray(StandardCharsets.UTF_8)
        val server = DesktopSyncServer(port) { method, path, body, _ ->
            DesktopSyncServer.Response.json(
                JSONObject()
                    .put("method", method)
                    .put("path", path)
                    .put("bodyLength", body.toByteArray(StandardCharsets.UTF_8).size)
                    .put("sameBody", body == bodyText)
            )
        }

        try {
            server.start()
            val response = sendRawHttp(
                port = port,
                request = buildString {
                    append("POST /api/planning/notes/1 HTTP/1.1\r\n")
                    append("Host: 127.0.0.1\r\n")
                    append("Content-Type: application/json; charset=utf-8\r\n")
                    append("Content-Length: ${bodyBytes.size}\r\n")
                    append("\r\n")
                }.toByteArray(StandardCharsets.ISO_8859_1) + bodyBytes
            )
            assertTrue(response.startsWith("HTTP/1.1 200 OK"))
            val payload = JSONObject(response.substringAfter("\r\n\r\n"))
            assertEquals("POST", payload.getString("method"))
            assertEquals("/api/planning/notes/1", payload.getString("path"))
            assertEquals(bodyBytes.size, payload.getInt("bodyLength"))
            assertTrue(payload.getBoolean("sameBody"))
        } finally {
            server.stop()
        }
    }

    @Test
    fun handlesOptionsPreflightWithoutCallingHandler() {
        val port = freePort()
        var handlerCalled = false
        val server = DesktopSyncServer(port) { _, _, _, _ ->
            handlerCalled = true
            DesktopSyncServer.Response.json(JSONObject())
        }

        try {
            server.start()
            val response = sendRawHttp(
                port = port,
                request = buildString {
                    append("OPTIONS /api/todos HTTP/1.1\r\n")
                    append("Host: 127.0.0.1\r\n")
                    append("Origin: http://127.0.0.1\r\n")
                    append("Access-Control-Request-Method: GET\r\n")
                    append("\r\n")
                }.toByteArray(StandardCharsets.ISO_8859_1)
            )
            assertTrue(response.startsWith("HTTP/1.1 204 No Content"))
            assertTrue(response.contains("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS"))
            assertEquals(false, handlerCalled)
        } finally {
            server.stop()
        }
    }

    private fun freePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }

    private fun sendRawHttp(port: Int, request: ByteArray): String {
        repeat(40) { attempt ->
            runCatching {
                Socket("127.0.0.1", port).use { socket ->
                    socket.soTimeout = 5_000
                    socket.getOutputStream().write(request)
                    socket.getOutputStream().flush()
                    socket.shutdownOutput()
                    return socket.getInputStream().readBytes().toString(StandardCharsets.UTF_8)
                }
            }
            Thread.sleep(25L + attempt)
        }
        error("DesktopSyncServer did not accept connections on port $port")
    }
}
