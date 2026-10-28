package com.example.todoalarm.data

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class SnapshotConsistencyTest {
    @Test fun exportDuringAtomicReplacementNeverMixesOldGroupsWithNewTasks() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val armed = AtomicBoolean(false)
        val taskReadReached = CountDownLatch(1)
        val replacementFinished = CountDownLatch(1)
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .setQueryCallback({ sql, _ ->
                if (sql.trimStart().startsWith("SELECT", ignoreCase = true) &&
                    sql.contains("FROM todo_items", ignoreCase = true) && armed.compareAndSet(true, false)) {
                    taskReadReached.countDown()
                    // Give a concurrent atomic replacement a deterministic chance
                    // to commit between the group read and the task read. A
                    // consistent export must prevent that mixed snapshot.
                    replacementFinished.await(2, TimeUnit.SECONDS)
                }
            }, { command -> command.run() })
            .build()
        val repository = TodoRepository(database)
        suspend fun replace(id: Long) = database.withTransaction {
            val dao = database.todoDao()
            dao.clearTodos()
            dao.clearGroups()
            dao.insertGroups(listOf(TaskGroup(id = id, name = "generation $id", colorHex = "#326F59", sortOrder = 0)))
            dao.insertAll(listOf(TodoItem(id = id, title = "generation $id", groupId = id,
                dueAtMillis = Long.MAX_VALUE, reminderAtMillis = null,
                reminderEnabled = false, ringEnabled = false, vibrateEnabled = false)))
        }
        try {
            replace(1)
            withTimeout(15_000) {
                val replacement = async(Dispatchers.IO) {
                    check(taskReadReached.await(5, TimeUnit.SECONDS)) { "Export did not reach task read" }
                    try { replace(2) } finally { replacementFinished.countDown() }
                }
                armed.set(true)
                val snapshot = repository.exportSnapshot(AppSettings())
                replacement.await()
                assertEquals("All task/group rows must come from one committed database state",
                    snapshot.groups.single().id, snapshot.tasks.single().groupId)
                assertEquals(snapshot.groups.single().name, snapshot.tasks.single().title)
                assertEquals(2L, repository.getAllTodos().single().groupId)
            }
        } finally { database.close() }
    }

    @Test fun exportWithinAnExistingSyncTransactionKeepsTheSameSnapshot() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        val repository = TodoRepository(database)
        try {
            repository.createQuickChecklist(QuickChecklistParser.parse("- 准备材料\n-- 核对清单"))
            val snapshot = withTimeout(5_000) { database.withTransaction { repository.exportSnapshot(AppSettings()) } }
            val restored = BackupArchiveCodec.decode(BackupArchiveCodec.encode(snapshot))
            assertEquals(2, restored.tasks.size)
            assertEquals(1, restored.quickChecklistGroups.size)
            val parent = restored.tasks.single { it.parentTodoId == null }
            assertEquals(parent.id, restored.tasks.single { it.parentTodoId != null }.parentTodoId)
            assertTrue(restored.tasks.all { task -> restored.quickChecklistGroups.any { it.id == task.quickChecklistGroupId } })
        } finally { database.close() }
    }
}
