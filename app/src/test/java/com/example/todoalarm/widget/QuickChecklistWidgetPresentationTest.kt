package com.example.todoalarm.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickChecklistWidgetPresentationTest {
    @Test
    fun summaryAndTaskTitleExposeChecklistState() {
        val tasks = listOf(
            QuickChecklistWidgetTask(1L, 7L, "父任务", 1, completed = true, canceled = false),
            QuickChecklistWidgetTask(2L, 7L, "子任务", 2, completed = false, canceled = false),
            QuickChecklistWidgetTask(3L, 7L, "已取消", 2, completed = false, canceled = true)
        )

        val summary = QuickChecklistWidgetPresentation.summaries(tasks).getValue(7L)

        assertEquals(1, summary.completedCount)
        assertEquals(2, summary.totalCount)
        assertEquals("清单 · 0913待办 · 1/2", QuickChecklistWidgetPresentation.groupLabel("0913待办", summary))
        assertEquals("☑ 父任务", QuickChecklistWidgetPresentation.taskTitle(tasks[0]))
        assertEquals("☐   子任务", QuickChecklistWidgetPresentation.taskTitle(tasks[1]))
    }
}
