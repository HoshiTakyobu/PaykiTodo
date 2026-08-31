package com.example.todoalarm.sync

import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.BackupSnapshot
import com.example.todoalarm.data.PlannerItemType
import com.example.todoalarm.data.PlanningNote
import com.example.todoalarm.data.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteBackupModelsTest {
    @Test
    fun diffAgainstReportsAddedRemovedAndChangedItems() {
        val local = snapshot(
            tasks = listOf(
                todo(id = 1, title = "旧标题"),
                todo(id = 3, title = "本地任务"),
                event(id = 7, title = "原日程")
            ),
            notes = listOf(PlanningNote(id = 20, title = "计划", contentMarkdown = "旧内容"))
        )
        val remote = snapshot(
            tasks = listOf(
                todo(id = 1, title = "新标题"),
                todo(id = 2, title = "服务器任务"),
                event(id = 7, title = "更新日程"),
                event(id = 8, title = "新增日程")
            ),
            notes = listOf(
                PlanningNote(id = 20, title = "计划", contentMarkdown = "新内容"),
                PlanningNote(id = 21, title = "新计划", contentMarkdown = "内容")
            )
        )

        val diff = remote.diffAgainst(local)

        assertEquals(1, diff.addedTodos)
        assertEquals(1, diff.removedTodos)
        assertEquals(1, diff.changedTodos)
        assertEquals(1, diff.addedEvents)
        assertEquals(0, diff.removedEvents)
        assertEquals(1, diff.changedEvents)
        assertEquals(1, diff.addedPlanningNotes)
        assertEquals(0, diff.removedPlanningNotes)
        assertEquals(1, diff.changedPlanningNotes)
        assertTrue(diff.hasChanges)
    }

    private fun snapshot(tasks: List<TodoItem>, notes: List<PlanningNote>): BackupSnapshot {
        return BackupSnapshot(
            exportedAtMillis = 1L,
            groups = emptyList(),
            templates = emptyList(),
            tasks = tasks,
            planningNotes = notes,
            settings = AppSettings()
        )
    }

    private fun todo(id: Long, title: String): TodoItem {
        return TodoItem(id = id, title = title, dueAtMillis = 1L, reminderAtMillis = null, reminderEnabled = false, ringEnabled = true, vibrateEnabled = true)
    }

    private fun event(id: Long, title: String): TodoItem {
        return todo(id, title).copy(
            itemType = PlannerItemType.EVENT.name,
            startAtMillis = 1L,
            endAtMillis = 2L
        )
    }
}
