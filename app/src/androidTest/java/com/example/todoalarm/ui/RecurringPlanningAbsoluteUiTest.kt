package com.example.todoalarm.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.todoalarm.data.*
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

/** Exercises the production planning dialog without importing into any user database. */
class RecurringPlanningAbsoluteUiTest {
    @get:Rule val compose = createComposeRule()

    private val parseNow = LocalDateTime.of(2030, 1, 6, 12, 0)
    private val source = "数学,每周[周一 08:00>09:40,周三 14:00>15:40],(2030-01-07 08:01),自2030-01-07至2030-01-23;"
    private val initialOwner = "只提醒一次：2030-01-07 08:01 → 2030-01-09 14:00的课程"
    private val editedOwner = "只提醒一次：2030-01-09 14:01 → 2030-01-14 08:00的课程"
    private val reminderLabel = "提醒：空白默认(5,0)，()关闭"

    @Test fun ownerExplanationUpdatesAfterReminderEditAndCancelDoesNotImport() {
        var opened by mutableStateOf(true)
        var imports = 0
        var recognitions = 0
        var hideKeyboard: () -> Unit = {}
        compose.setContent {
            TodoAlarmTheme { Surface {
                val keyboard = LocalSoftwareKeyboardController.current
                SideEffect { hideKeyboard = { keyboard?.hide(); Unit } }
                if (opened) TransientPlanningDialog(
                    listOf(TaskGroup(1, "例行", "#467DAB", 0, true)),
                    listOf(PlannerCalendar("default", "常规", "#467DAB")), false, 42,
                    onRecognize = { text, _, _ ->
                        recognitions++
                        TransientPlanningParser.parse(text, parseNow)
                    },
                    onImport = { _, _, _ -> imports++; null },
                    onDismiss = { opened = false }
                )
            } }
        }

        compose.onNode(hasSetTextAction() and hasText("规划内容")).performTextInput(source)
        compose.runOnIdle { hideKeyboard() }
        compose.onNodeWithText("识别").performClick()
        compose.onNodeWithText("确认规划").assertIsDisplayed()
        compose.onNodeWithText(initialOwner).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, recognitions); assertEquals(0, imports) }

        compose.onNode(hasSetTextAction() and hasText(reminderLabel))
            .performScrollTo().performTextReplacement("(2030-01-09 14:01)")
        compose.runOnIdle { hideKeyboard() }
        compose.onNodeWithText(editedOwner).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(initialOwner).assertDoesNotExist()
        compose.onNodeWithText("确认导入").assertIsEnabled()
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { assertFalse(opened); assertEquals(1, recognitions); assertEquals(0, imports) }
    }

    @Test fun confirmationReceivesEditedUniqueOwnerAndRepeatedClickCannotImportAgain() {
        var opened by mutableStateOf(true)
        var imports = 0
        var imported = emptyList<TransientPlanningCandidate>()
        var importedSession = ""
        var originalId = ""
        var hideKeyboard: () -> Unit = {}
        val finishImport = CompletableDeferred<Unit>()
        compose.setContent {
            TodoAlarmTheme { Surface {
                val keyboard = LocalSoftwareKeyboardController.current
                SideEffect { hideKeyboard = { keyboard?.hide(); Unit } }
                if (opened) TransientPlanningDialog(
                    listOf(TaskGroup(1, "例行", "#467DAB", 0, true)),
                    listOf(PlannerCalendar("default", "常规", "#467DAB")), false, 42,
                    onRecognize = { text, _, _ ->
                        TransientPlanningParser.parse(text, parseNow).also { originalId = it.candidates.single().id }
                    },
                    onImport = { items, session, generation ->
                        assertEquals(42L, generation)
                        imports++
                        imported = items
                        importedSession = session
                        finishImport.await()
                        null
                    },
                    onDismiss = { opened = false }
                )
            } }
        }

        compose.onNode(hasSetTextAction() and hasText("规划内容")).performTextInput(source)
        compose.runOnIdle { hideKeyboard() }
        compose.onNodeWithText("识别").performClick()
        compose.onNodeWithText(initialOwner).performScrollTo().assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText(reminderLabel))
            .performScrollTo().performTextReplacement("(2030-01-09 14:01)")
        compose.runOnIdle { hideKeyboard() }
        compose.onNodeWithText(editedOwner).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("确认导入").performClick()
        compose.waitUntil(5_000) { imports == 1 }

        compose.runOnIdle {
            val candidate = imported.single()
            assertEquals(originalId, candidate.id)
            assertEquals("数学", candidate.title)
            assertEquals(PlanningReminderMode.EXPLICIT, candidate.reminderMode)
            assertTrue(candidate.reminderOffsetsMinutes.isEmpty())
            assertEquals(listOf(LocalDateTime.parse("2030-01-09T14:01")), candidate.absoluteReminderTimes)
            val owner = assignPlanningAbsoluteReminders(candidate.slots, candidate.absoluteReminderTimes).single()
            assertEquals(0, owner.slotIndex)
            assertEquals(LocalDateTime.parse("2030-01-14T08:00"), owner.occurrenceStartAt)
            assertEquals(candidate.absoluteReminderTimes, owner.absoluteReminderTimes)
            assertTrue(importedSession.isNotBlank())
        }
        compose.onNodeWithText("处理中").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, imports); assertTrue(opened); finishImport.complete(Unit) }
        compose.waitUntil(5_000) { !opened }
        compose.onNodeWithText("确认导入").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, imports); assertFalse(opened) }
    }
}
