package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlanningLineMatcherTest {
    @Test
    fun fingerprintIgnoresImportedMarkerAndWhitespaceNoise() {
        val left = "- [ ] 写论文   #ddl 5.28   #imported"
        val right = "- [ ]  写论文 #ddl 5.28"

        assertEquals(
            PlanningLineMatcher.fingerprint(left),
            PlanningLineMatcher.fingerprint(right)
        )
    }

    @Test
    fun relocateMappingsPrefersNearestExactFingerprintMatch() {
        val line = "- [ ] 09:00-10:00 写论文 #imported"
        val document = listOf(
            "# 今日计划",
            line,
            "- [ ] 10:30-11:30 开会",
            line
        )
        val mapping = PlanningLineMapping(
            id = 7,
            noteId = 1,
            contentFingerprint = PlanningLineMatcher.fingerprint(line),
            originalLineText = line,
            currentLineText = line,
            todoId = 11,
            batchId = "batch-a",
            createdAtMillis = 1,
            lastRefreshedAtMillis = 1,
            lastKnownLineNumber = 4
        )

        val relocated = PlanningLineMatcher.relocateMappings(document, listOf(mapping))

        assertEquals(3, relocated[mapping.id])
    }

    @Test
    fun relocateMappingsFallsBackToFuzzyMatchWhenLineEdited() {
        val original = "- [ ] 14:00-16:00 写论文 #group 学习 #imported"
        val edited = "- [ ] 14:30-16:30 写论文终稿 #group 学习"
        val document = listOf(
            "# 今日计划",
            edited,
            "- [ ] 17:00-18:00 散步"
        )
        val mapping = PlanningLineMapping(
            id = 9,
            noteId = 1,
            contentFingerprint = PlanningLineMatcher.fingerprint(original),
            originalLineText = original,
            currentLineText = original,
            eventId = 22,
            batchId = "batch-b",
            createdAtMillis = 1,
            lastRefreshedAtMillis = 1,
            lastKnownLineNumber = 2
        )

        val relocated = PlanningLineMatcher.relocateMappings(document, listOf(mapping))

        assertEquals(1, relocated[mapping.id])
        assertNotEquals(
            PlanningLineMatcher.fingerprint(original),
            PlanningLineMatcher.fingerprint(edited)
        )
    }
}
