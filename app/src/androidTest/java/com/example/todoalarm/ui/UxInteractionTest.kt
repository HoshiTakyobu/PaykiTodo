package com.example.todoalarm.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.todoalarm.data.*
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Actual production components and synthetic values; no account/network/Room writes. */
class UxInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val moment = LocalDateTime.of(2026, 11, 3, 12, 0)
    private fun millis(time: LocalDateTime) = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun task(id: Long, title: String, hour: Int) = TodoItem(id = id, title = title, notes = "", dueAtMillis = millis(moment.withHour(hour)),
        reminderAtMillis = null, reminderEnabled = false, ringEnabled = false, vibrateEnabled = false, voiceEnabled = false)
    private fun capture(name: String) {
        val inst = InstrumentationRegistry.getInstrumentation()
        val folder = File(inst.targetContext.getExternalFilesDir(null), "ux-reader").apply { mkdirs() }
        inst.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        }
    }
    @Test fun unifiedBoardUsesTaskDdlAndPreservesCompletionSelectionAndParentContext() {
        val before = task(1, "会前准备", 13)
        val inside = task(2, "会中提交", 15).copy(parentTodoId = 9)
        val after = task(3, "会后整理", 17)
        val tomorrow = task(4, "明天提交", 10).copy(dueAtMillis = millis(moment.plusDays(1).withHour(10)))
        val meeting = task(5, "小组讨论", 14).copy(itemType = PlannerItemType.EVENT.name, startAtMillis = millis(moment.withHour(14)),
            endAtMillis = millis(moment.withHour(17)), location = "A501")
        var opened = 0L; var completed = 0L
        compose.setContent {
            var selection by remember { mutableStateOf(false) }
            var selected by remember { mutableStateOf(emptySet<Long>()) }
            var todos by remember { mutableStateOf(listOf(before, inside, after)) }
            TodoAlarmTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
                        TodayScheduleBoardCard(moment.toLocalDate(), moment, listOf(meeting), emptyList(), false,
                            onOpenEvent = { opened = it.id }, onGetEventCheckIns = { emptyList() }, onLaunchCheckIn = {},
                            todayTodos = todos, tomorrowTodos = listOf(tomorrow), todoContext = listOf(task(9, "阶段记录清单", 10)),
                            onOpenTodo = { if (selection) selected = if (it.id in selected) selected - it.id else selected + it.id else opened = it.id },
                            onCompleteTodo = { if (selection) { selected = if (it.id in selected) selected - it.id else selected + it.id; selection = false }
                                else { completed = it.id; todos = todos.filterNot { entry -> entry.id == it.id } } },
                            selectionMode = selection, selectedTodoIds = selected,
                            onEnterTodoSelection = { selection = true; selected = setOf(it.id) })
                    }
                }
            }
        }
        val positions = listOf("会前准备", "小组讨论", "会中提交", "会后整理").map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.top }
        assertTrue(positions.zipWithNext().all { (left, right) -> left < right })
        compose.onNodeWithText("DDL 11-03 15:00").assertExists()
        compose.onNodeWithText("阶段记录清单").assertExists()
        compose.onNodeWithText("会中提交").performScrollTo().performTouchInput { longClick() }
        compose.onNodeWithContentDescription("选择待办：会中提交").assertIsOn().performClick()
        compose.onNodeWithContentDescription("完成待办：会中提交").performClick()
        compose.onNodeWithText("会中提交").assertDoesNotExist()
        assertEquals(2L, completed); assertEquals(millis(moment.withHour(15)), inside.dueAtMillis)
        compose.onNodeWithText("明天提交").performScrollTo().performClick(); assertEquals(4L, opened)
        compose.onNodeWithText("小组讨论").performScrollTo().performClick(); assertEquals(5L, opened)
        capture("unified-board-order-and-actions")
    }
    @Test fun eventColorSelectionIsDraftUntilConfirmAndKeepsTime() {
        var saved: CalendarEventDraft? = null
        val start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0)
        val original = CalendarEventDraft(title = "颜色保存样例", notes = "", location = "A501", startAt = start, endAt = start.plusHours(3), allDay = false,
            accentColorHex = "#DF58A5", ringEnabled = false, vibrateEnabled = false)
        compose.setContent { TodoAlarmTheme {
            CompositionLocalProvider(LocalWorkspacePreferences provides WorkspacePreferences(settings = AppSettings())) {
                CalendarEventEditorDialog(null, initialDraft = original, defaultRingEnabled = false, defaultVibrateEnabled = false,
                    onDismiss = {}, onConfirm = { saved = it })
            }
        } }
        compose.onNodeWithText("日程颜色").performScrollTo()
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("日程颜色")))
            .performScrollToNode(hasContentDescription("日程颜色 #51BA43"))
        compose.onNodeWithContentDescription("日程颜色 #51BA43").performScrollTo().performClick().assertIsSelected()
        assertNull(saved)
        capture("event-color-swatch-selected")
        compose.onNodeWithText("完成").performClick()
        compose.waitUntil(5_000) { saved != null }
        assertEquals("#51BA43", saved!!.accentColorHex); assertEquals(start, saved!!.startAt); assertEquals(start.plusHours(3), saved!!.endAt)
    }
}
