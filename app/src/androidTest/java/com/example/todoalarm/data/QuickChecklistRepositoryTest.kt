package com.example.todoalarm.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickChecklistRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: TodoRepository

    @Before
    fun setUp() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TodoRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun restoringCanceledParentRestoresItsWholeSubtree() = runBlocking {
        val parent = createSampleChecklistAndFindParent()

        repository.cancelTodo(parent)
        val canceledSubtree = quickSubtree(parent)
        assertTrue(canceledSubtree.all { it.canceled && !it.completed })

        repository.restoreTodoFromHistory(parent)
        val restoredSubtree = quickSubtree(parent)

        assertTrue(restoredSubtree.all { !it.canceled && !it.completed && it.isActive })
        assertFalse(restoredSubtree.any { it.canceledAtMillis != null || it.completedAtMillis != null })
    }

    @Test
    fun restoringCompletedParentRestoresItsWholeSubtreeAndAncestorState() = runBlocking {
        val parent = createSampleChecklistAndFindParent()

        repository.setCompleted(parent.id, completed = true)
        assertTrue(quickSubtree(parent).all { it.completed })

        repository.restoreTodoFromHistory(parent)
        val restoredSubtree = quickSubtree(parent)

        assertTrue(restoredSubtree.all { it.isActive })
        assertTrue(restoredSubtree.none { it.completed || it.canceled })
    }

    @Test
    fun canceledChildDoesNotBlockParentCompletion() = runBlocking {
        val parent = createSampleChecklistAndFindParent()
        val children = directChildren(parent)

        repository.cancelTodo(children[0])
        repository.setCompleted(children[1].id, completed = true)

        assertFalse(requireNotNull(repository.getTodo(parent.id)).completed)

        repository.setCompleted(children[2].id, completed = true)

        val completedParent = requireNotNull(repository.getTodo(parent.id))
        val canceledChild = requireNotNull(repository.getTodo(children[0].id))
        assertTrue(completedParent.completed)
        assertFalse(completedParent.canceled)
        assertTrue(canceledChild.canceled)
        assertFalse(canceledChild.completed)
    }

    @Test
    fun canceledParentIsNotRevivedByRestoringAndCompletingChild() = runBlocking {
        val parent = createSampleChecklistAndFindParent()
        val child = directChildren(parent).first()

        repository.cancelTodo(parent)
        repository.restoreTodoFromHistory(child)
        repository.setCompleted(child.id, completed = true)

        val canceledParent = requireNotNull(repository.getTodo(parent.id))
        assertTrue(canceledParent.canceled)
        assertFalse(canceledParent.completed)
    }

    @Test
    fun deletingLeafKeepsRemainingAncestorStateCorrect() = runBlocking {
        val parent = createSampleChecklistAndFindParent()
        val children = directChildren(parent)

        repository.deleteTodo(children[0])

        assertNull(repository.getTodo(children[0].id))
        assertEquals(2, directChildren(parent).size)
        assertFalse(requireNotNull(repository.getTodo(parent.id)).completed)

        repository.setCompleted(children[1].id, completed = true)
        repository.setCompleted(children[2].id, completed = true)

        assertTrue(requireNotNull(repository.getTodo(parent.id)).completed)
    }

    @Test
    fun deletingOnlyChildDoesNotAutoCompleteParent() = runBlocking {
        val draft = QuickChecklistParser.parse(
            """
            单一父级
            - 父任务
            -- 唯一子任务
            """.trimIndent()
        )
        val group = repository.createQuickChecklist(draft)
        val tasks = database.todoDao().getQuickChecklistItems(group.id)
        val parent = tasks.single { it.title == "父任务" }
        val child = tasks.single { it.title == "唯一子任务" }

        repository.deleteTodo(child)

        val remainingParent = requireNotNull(repository.getTodo(parent.id))
        assertTrue(remainingParent.isActive)
        assertFalse(remainingParent.completed)
        assertTrue(database.todoDao().getQuickChecklistItems(group.id).none { it.id == child.id })
    }

    @Test
    fun titlelessChecklistCreatesOnlyTasksAndSurvivesArchiveRoundTrip() = runBlocking {
        val source = "- 想到就写\n-- 子任务\n--# 说明\n- 第二件事"
        val draft = QuickChecklistParser.parse(source)
        val group = repository.createQuickChecklist(draft)
        assertEquals("", group.title)
        val tasks = database.todoDao().getQuickChecklistItems(group.id)
        assertEquals(3, tasks.size)
        assertEquals(tasks.single { it.title == "想到就写" }.id, tasks.single { it.title == "子任务" }.parentTodoId)
        assertEquals("说明", tasks.single { it.title == "子任务" }.notes)
        val snapshot = repository.exportSnapshot(AppSettings())
        repository.importSnapshot(BackupArchiveCodec.decode(BackupArchiveCodec.encode(snapshot)))
        assertEquals(3, database.todoDao().getQuickChecklistItems(group.id).size)
        assertEquals("", repository.getAllQuickChecklistGroups().single().title)
        assertTrue(runCatching { repository.createQuickChecklist(QuickChecklistParser.parse("// 注释\n$source")) }.isFailure)
    }

    @Test
    fun quickChecklistPersistsHierarchyNotesOrderingAndRejectsDuplicate() = runBlocking {
        val draft = sampleDraft()
        val group = repository.createQuickChecklist(draft)
        val tasks = database.todoDao().getQuickChecklistItems(group.id)

        assertEquals(5, tasks.size)
        assertTrue(tasks.none { it.title == "0913待办" || it.notes.contains("//标题行") })
        assertTrue(tasks.all { it.itemType == "TODO" && !it.hasDueDate && !it.reminderEnabled })

        val parent = tasks.single { it.title == "准备浙大的资料" }
        val children = tasks
            .filter { it.parentTodoId == parent.id }
            .sortedBy { it.quickChecklistSortOrder }
        assertEquals(listOf("1. 身份证复印件", "2. 个人简历", "3. 成绩排名"), children.map { it.title })
        assertEquals(listOf(0, 1, 2), children.map { it.quickChecklistSortOrder })
        assertTrue(children.all { it.quickChecklistDepth == 2 })
        assertEquals("成绩排名是本学年的", children.last().notes)

        repository.setQuickChecklistTaskCollapsed(parent.id, true)
        assertTrue(requireNotNull(repository.getTodo(parent.id)).quickChecklistCollapsed)

        repository.setQuickChecklistGroupCollapsed(group.id, true)
        assertTrue(repository.getAllQuickChecklistGroups().single { it.id == group.id }.collapsed)

        val duplicateRejected = try {
            repository.createQuickChecklist(draft)
            false
        } catch (_: IllegalArgumentException) {
            true
        }
        assertTrue(duplicateRejected)
    }

    @Test
    fun cancelingAndDeletingQuickChecklistTaskUsesNormalLifecycle() = runBlocking {
        val draft = sampleDraft()
        val group = repository.createQuickChecklist(draft)
        val root = database.todoDao().getQuickChecklistItems(group.id)
            .single { it.title == "取快递（取件码114514）" }

        repository.cancelTodo(root)
        val canceled = requireNotNull(repository.getTodo(root.id))
        assertTrue(canceled.canceled)
        assertTrue(canceled.isHistory)

        repository.deleteTodo(canceled)
        assertNull(repository.getTodo(root.id))
    }

    private suspend fun createSampleChecklistAndFindParent(): TodoItem {
        val draft = sampleDraft()
        val group = repository.createQuickChecklist(draft)
        return database.todoDao().getQuickChecklistItems(group.id)
            .single { it.title == "准备浙大的资料" }
    }

    private fun sampleDraft(): QuickChecklistDraft {
        return QuickChecklistParser.parse(
            """
            0913待办 //标题行
            - 准备浙大的资料
            -- 1. 身份证复印件
            -- 2. 个人简历
            -- 3. 成绩排名
            --# 成绩排名是本学年的
            - 取快递（取件码114514）
            """.trimIndent()
        )
    }

    private suspend fun directChildren(parent: TodoItem): List<TodoItem> {
        val groupId = requireNotNull(parent.quickChecklistGroupId)
        return database.todoDao().getQuickChecklistItems(groupId)
            .filter { it.parentTodoId == parent.id }
            .sortedBy { it.quickChecklistSortOrder }
    }

    private suspend fun quickSubtree(parent: TodoItem): List<TodoItem> {
        val groupId = requireNotNull(parent.quickChecklistGroupId)
        val tasks = database.todoDao().getQuickChecklistItems(groupId)
        val childrenByParent = tasks.groupBy { it.parentTodoId }
        return buildList {
            fun visit(item: TodoItem) {
                add(item)
                childrenByParent[item.id].orEmpty().forEach(::visit)
            }
            visit(tasks.single { it.id == parent.id })
        }
    }
}
