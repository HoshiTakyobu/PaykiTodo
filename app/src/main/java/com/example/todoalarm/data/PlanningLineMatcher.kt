package com.example.todoalarm.data

import java.security.MessageDigest
import kotlin.math.min

object PlanningLineMatcher {
    fun relocateMappings(
        documentLines: List<String>,
        mappings: List<PlanningLineMapping>
    ): Map<Long, Int?> {
        val exactByFingerprint = documentLines
            .mapIndexed { index, line -> fingerprint(line) to index }
            .groupBy({ it.first }, { it.second })

        return mappings.associate { mapping ->
            val exact = exactByFingerprint[mapping.contentFingerprint]?.nearestTo(mapping.lastKnownLineNumber)
            if (exact != null) {
                mapping.id to exact
            } else {
                mapping.id to fuzzyMatchLine(documentLines, mapping)
            }
        }
    }

    fun fingerprint(lineText: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(normalizeLine(lineText).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }.take(16)
    }

    fun editDistance(a: String, b: String): Int {
        val left = normalizeLine(a)
        val right = normalizeLine(b)
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length

        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)
        for (i in 1..left.length) {
            current[0] = i
            for (j in 1..right.length) {
                val cost = if (left[i - 1] == right[j - 1]) 0 else 1
                current[j] = minOf(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + cost
                )
            }
            val temp = previous
            previous = current
            current = temp
        }
        return previous[right.length]
    }

    fun normalizeLine(lineText: String): String {
        return lineText
            .replace("\r", "")
            .replace(Regex("\\s+#imported(?=\\s|$)"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun fuzzyMatchLine(documentLines: List<String>, mapping: PlanningLineMapping): Int? {
        val source = mapping.trackedLineText.ifBlank { mapping.originalLineText }
        val normalizedSource = normalizeLine(source)
        if (normalizedSource.isBlank()) return null
        val maxDistance = (normalizedSource.length * 0.30f).toInt().coerceAtLeast(1)
        return documentLines
            .mapIndexedNotNull { index, line ->
                val distance = editDistance(normalizedSource, line)
                if (distance <= maxDistance) index to distance else null
            }
            .minWithOrNull(
                compareBy<Pair<Int, Int>> { it.second }
                    .thenBy { kotlin.math.abs((mapping.lastKnownLineNumber - 1).coerceAtLeast(0) - it.first) }
            )
            ?.first
    }

    private fun List<Int>.nearestTo(lastKnownLineNumber: Int): Int? {
        if (isEmpty()) return null
        val target = (lastKnownLineNumber - 1).coerceAtLeast(0)
        return minByOrNull { kotlin.math.abs(it - target) }
    }
}
