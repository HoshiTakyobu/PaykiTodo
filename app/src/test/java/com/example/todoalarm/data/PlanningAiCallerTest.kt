package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlanningAiCallerTest {
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
}
