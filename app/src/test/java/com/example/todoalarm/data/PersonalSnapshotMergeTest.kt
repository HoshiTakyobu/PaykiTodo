package com.example.todoalarm.data

import org.junit.Assert.*
import org.junit.Test

class PersonalSnapshotMergeTest {
    private fun base() = BackupSnapshot(0, listOf(TaskGroup(id = 1, name = "测试", colorHex = "#123456", sortOrder = 0)), emptyList(),
        listOf(TodoItem(id = 1, title = "19:30", dueAtMillis = 100000, reminderAtMillis = 90000, groupId = 1, reminderEnabled = true, ringEnabled = false, vibrateEnabled = false),
            TodoItem(id = 2, title = "另一事项", dueAtMillis = 200000, reminderAtMillis = 190000, groupId = 1, reminderEnabled = true, ringEnabled = false, vibrateEnabled = false)), settings = AppSettings())

    @Test fun independentRowsAndSettingsMergeWithoutLosingEitherEdit() {
        val b = base()
        val l = b.copy(tasks = b.tasks.map { if (it.id == 1L) it.copy(completed = true, completedAtMillis = 123L) else it },
            settings = b.settings.copy(themeMode = ThemeMode.DARK))
        val r = b.copy(tasks = b.tasks.map { if (it.id == 2L) it.copy(title = "远端修改") else it })
        val merged = PersonalSnapshotMerge.merge(b, l, r)!!
        assertTrue(merged.tasks.first { it.id == 1L }.completed)
        assertEquals("远端修改", merged.tasks.first { it.id == 2L }.title)
        assertEquals(ThemeMode.DARK, merged.settings.themeMode)
    }

    @Test fun snoozeAndPassiveMissedDoNotBlockExplicitCompletion() {
        val b = base()
        val l = b.copy(tasks = b.tasks.map { if (it.id == 1L) it.copy(reminderAtMillis = 110000, reminderOffsetsCsv = "", reminderOffsetMinutes = null,
            missed = true, missedAtMillis = 120000, reminderEnabled = false) else it })
        val r = b.copy(tasks = b.tasks.map { if (it.id == 1L) it.copy(completed = true, completedAtMillis = 115000) else it })
        for ((left, right) in listOf(l to r, r to l)) {
            val item = PersonalSnapshotMerge.merge(b, left, right)!!.tasks.first { it.id == 1L }
            assertTrue(item.completed); assertFalse(item.missed); assertFalse(item.reminderEnabled)
            assertEquals(115000L, item.completedAtMillis); assertEquals(110000L, item.reminderAtMillis)
        }
    }

    @Test fun competingEditsAndDeleteVersusEditRemainConflicts() {
        val b = base()
        val l = b.copy(tasks = b.tasks.map { it.copy(title = "本机") })
        val r = b.copy(tasks = b.tasks.map { it.copy(title = "云端") })
        assertNull(PersonalSnapshotMerge.merge(b, l, r))
        assertNull(PersonalSnapshotMerge.merge(b, l.copy(tasks = emptyList()), r))
    }

    @Test fun simultaneousNewRowsWithSameLocalIdAreNotOverwritten() {
        val b = base()
        val l = b.copy(tasks = b.tasks + b.tasks[0].copy(id = 3, title = "本机新增"))
        val r = b.copy(tasks = b.tasks + b.tasks[0].copy(id = 3, title = "云端新增"))
        assertNull(PersonalSnapshotMerge.merge(b, l, r))
    }

    @Test fun deleteAndUnrelatedEditMergeButDanglingReferencesDoNot() {
        val b = base()
        val l = b.copy(tasks = b.tasks.filter { it.id != 1L })
        val r = b.copy(tasks = b.tasks.map { if (it.id == 2L) it.copy(title = "新名字") else it })
        assertEquals(listOf("新名字"), PersonalSnapshotMerge.merge(b, l, r)!!.tasks.map { it.title })
        assertNull(PersonalSnapshotMerge.merge(b, b.copy(groups = emptyList()), r))
    }

    @Test fun equalityIgnoresTableOrderAndDiagnosticsButTracksSettings() {
        val b = base()
        assertTrue(PersonalSnapshotMerge.same(b, b.copy(exportedAtMillis = 99, tasks = b.tasks.reversed())))
        assertFalse(PersonalSnapshotMerge.same(b, b.copy(settings = b.settings.copy(themeMode = ThemeMode.DARK))))
        assertFalse(PersonalSnapshotMerge.same(b, b.copy(tasks = b.tasks.map { it.copy(completed = true) })))
    }

    @Test fun explicitOppositeHistoryActionsAreNotResolvedByWallClock() {
        val b = base()
        val l = b.copy(tasks = b.tasks.map { it.copy(completed = true, completedAtMillis = 10) })
        val r = b.copy(tasks = b.tasks.map { it.copy(canceled = true, canceledAtMillis = 20) })
        assertNull(PersonalSnapshotMerge.merge(b, l, r))
    }

    @Test fun repeatedCompletionKeepsFirstActionTime() {
        val b = base()
        val l = b.copy(tasks = b.tasks.map { it.copy(completed = true, completedAtMillis = 10) })
        val r = b.copy(tasks = b.tasks.map { it.copy(completed = true, completedAtMillis = 20) })
        assertEquals(10L, PersonalSnapshotMerge.merge(b, l, r)!!.tasks.first().completedAtMillis)
    }
}
