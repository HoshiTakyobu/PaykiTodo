package com.example.todoalarm.data

import org.junit.Assert.*
import org.junit.Test

class QuickChecklistTimeTest {
    @Test fun timedTasksKeepHierarchyNotesAndCanonicalTime() {
        val draft = QuickChecklistParser.parse("- 做作业, #16:00\n-- 阅读，#9:05\n--# 说明\n- 无时间")
        assertFalse(draft.hasErrors)
        assertEquals(listOf(960, 545, null), draft.nodes.map { it.dueMinuteOfDay })
        assertEquals("做作业", draft.nodes[0].title)
        assertEquals(1, draft.nodes[1].parentTemporaryId)
        assertEquals("说明", draft.nodes[1].notes)
        assertEquals(QuickChecklistParser.parse("- 任务, #9:05").sourceFingerprint,
            QuickChecklistParser.parse("- 任务， #09:05 // 注释").sourceFingerprint)
        assertNotEquals(QuickChecklistParser.parse("- 任务, #16:00").sourceFingerprint,
            QuickChecklistParser.parse("- 任务, #17:00").sourceFingerprint)
    }

    @Test fun invalidTimesAndEmptyTimedTitlesAreRejectedAtSourceLine() {
        listOf("24:00", "23:60", "-1:00", "9:5", "").forEach { time ->
            val draft = QuickChecklistParser.parse("标题\n- 任务, #$time")
            assertTrue(time, draft.diagnostics.any { it.sourceLine == 2 && it.severity == QuickChecklistDiagnosticSeverity.ERROR })
        }
        assertTrue(QuickChecklistParser.parse("- , #16:00").hasErrors)
    }
}
