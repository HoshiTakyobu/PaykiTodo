package com.example.todoalarm.data

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class PlanningAiCallerTest {
    @Test
    fun connectivityTestBodyContainsOneFixedUserMessage() {
        val body = JSONObject(
            PlanningAiCaller.buildChatCompletionBody(
                provider = testProvider("https://example.com/v1"),
                request = PlanningAiRequest(prompt = PlanningAiCaller.CONNECTIVITY_TEST_PROMPT),
                maxTokens = 32,
                temperature = 0.0
            )
        )

        val messages = body.getJSONArray("messages")
        assertEquals(1, messages.length())
        assertEquals("user", messages.getJSONObject(0).getString("role"))
        assertEquals(PlanningAiCaller.CONNECTIVITY_TEST_PROMPT, messages.getJSONObject(0).getString("content"))
        assertEquals(32, body.getInt("max_tokens"))
        assertEquals(0.0, body.getDouble("temperature"), 0.0)
    }

    @Test
    fun connectivityTestSendsOneRequestWithCodexUserAgent() = runBlocking {
        val requestCount = AtomicInteger(0)
        val receivedBody = AtomicReference("")
        val receivedUserAgent = AtomicReference("")
        SingleRequestHttpServer(
            statusCode = 200,
            responseBody = """{"choices":[{"message":{"content":"test-model"}}]}""",
            onRequest = { headers, body ->
                requestCount.incrementAndGet()
                receivedBody.set(body)
                receivedUserAgent.set(headers["user-agent"].orEmpty())
            }
        ).use { server ->
            val result = PlanningAiCaller.testProvider(
                testProvider("http://127.0.0.1:${server.port}")
            )

            assertTrue(result is PlanningAiTestResult.Success)
            assertEquals("test-model", (result as PlanningAiTestResult.Success).responseText)
            assertEquals(1, requestCount.get())
            assertEquals(CODEX_CLI_USER_AGENT, receivedUserAgent.get())
            val messages = JSONObject(receivedBody.get()).getJSONArray("messages")
            assertEquals(1, messages.length())
            assertEquals(PlanningAiCaller.CONNECTIVITY_TEST_PROMPT, messages.getJSONObject(0).getString("content"))
        }
    }

    @Test
    fun connectivityTestDoesNotRetryAfterRateLimit() = runBlocking {
        val requestCount = AtomicInteger(0)
        SingleRequestHttpServer(
            statusCode = 429,
            responseBody = """{"error":{"message":"rate limited"}}""",
            onRequest = { _, _ ->
                requestCount.incrementAndGet()
            }
        ).use { server ->
            val result = PlanningAiCaller.testProvider(
                testProvider("http://127.0.0.1:${server.port}")
            )

            assertTrue(result is PlanningAiTestResult.Failure)
            assertEquals(1, requestCount.get())
        }
    }

    @Test
    fun endpointCandidatesPreferOpenAiCompatibleV1ForRootBaseUrl() {
        assertEquals(
            listOf(
                "https://example.com/v1/chat/completions",
                "https://example.com/chat/completions"
            ),
            PlanningAiCaller.endpointCandidates("https://example.com/")
        )
    }

    @Test
    fun endpointCandidatesKeepExplicitChatCompletionUrl() {
        assertEquals(
            listOf("https://example.com/v1/chat/completions"),
            PlanningAiCaller.endpointCandidates("https://example.com/v1/chat/completions")
        )
    }

    @Test
    fun endpointCandidatesAppendChatCompletionAfterV1BaseUrl() {
        assertEquals(
            listOf("https://example.com/v1/chat/completions"),
            PlanningAiCaller.endpointCandidates("https://example.com/v1")
        )
    }

    @Test
    fun endpointCandidatesConvertModelsUrlToChatCompletionUrl() {
        assertEquals(
            listOf("https://example.com/v1/chat/completions"),
            PlanningAiCaller.endpointCandidates("https://example.com/v1/models")
        )
    }

    @Test
    fun modelEndpointCandidatesPreferOpenAiCompatibleV1ForRootBaseUrl() {
        assertEquals(
            listOf(
                "https://example.com/v1/models",
                "https://example.com/models"
            ),
            PlanningAiCaller.modelEndpointCandidates("https://example.com/")
        )
    }

    @Test
    fun modelEndpointCandidatesConvertChatCompletionUrlToModelsUrl() {
        assertEquals(
            listOf("https://example.com/v1/models"),
            PlanningAiCaller.modelEndpointCandidates("https://example.com/v1/chat/completions")
        )
    }

    @Test
    fun modelEndpointCandidatesKeepExplicitModelsUrl() {
        assertEquals(
            listOf("https://example.com/v1/models"),
            PlanningAiCaller.modelEndpointCandidates("https://example.com/v1/models")
        )
    }

    private fun testProvider(baseUrl: String): PlanningAiProvider {
        return PlanningAiProvider(
            name = "test",
            baseUrl = baseUrl,
            apiKey = "test-key",
            model = "test-model",
            enabled = true
        )
    }

    private class SingleRequestHttpServer(
        private val statusCode: Int,
        private val responseBody: String,
        private val onRequest: (Map<String, String>, String) -> Unit
    ) : AutoCloseable {
        private val serverSocket = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        private val executor = Executors.newSingleThreadExecutor()
        val port: Int = serverSocket.localPort

        init {
            executor.submit {
                serverSocket.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
                    reader.readLine()
                    val headers = linkedMapOf<String, String>()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                        val separator = line.indexOf(':')
                        if (separator > 0) {
                            headers[line.substring(0, separator).trim().lowercase(Locale.ROOT)] =
                                line.substring(separator + 1).trim()
                        }
                    }
                    val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                    val body = CharArray(contentLength).let { buffer ->
                        var offset = 0
                        while (offset < buffer.size) {
                            val read = reader.read(buffer, offset, buffer.size - offset)
                            if (read < 0) break
                            offset += read
                        }
                        String(buffer, 0, offset)
                    }
                    onRequest(headers, body)

                    val bytes = responseBody.toByteArray(Charsets.UTF_8)
                    val reason = if (statusCode == 200) "OK" else "Too Many Requests"
                    socket.getOutputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
                        writer.write("HTTP/1.1 $statusCode $reason\r\n")
                        writer.write("Content-Type: application/json\r\n")
                        writer.write("Content-Length: ${bytes.size}\r\n")
                        writer.write("Connection: close\r\n\r\n")
                        writer.write(responseBody)
                        writer.flush()
                    }
                }
            }
        }

        override fun close() {
            runCatching { serverSocket.close() }
            executor.shutdownNow()
            executor.awaitTermination(2, TimeUnit.SECONDS)
        }
    }
}
