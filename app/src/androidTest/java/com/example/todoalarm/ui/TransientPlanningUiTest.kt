package com.example.todoalarm.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.todoalarm.data.*
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TransientPlanningUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun titleOnlyTodoWithAbsoluteReminderCanDisableItInOrdinaryEditor() {
        var draft: TodoDraft? = null
        val original = TodoItem(id = 12, title = "仅绝对提醒", notes = "", dueAtMillis = Long.MAX_VALUE,
            reminderAtMillis = 1_800_000_000_000L, reminderAbsoluteTimesCsv = "1800000000000", reminderOffsetsCsv = "",
            reminderEnabled = true, ringEnabled = true, vibrateEnabled = true, groupId = 1)
        compose.setContent { TodoAlarmTheme { Surface {
            TodoEditorDialog(original, listOf(TaskGroup(1, "例行", "#467DAB", 0, true)),
                defaultRingEnabled = true, defaultVibrateEnabled = true, onDismiss = {}, onConfirm = { draft = it })
        } } }
        compose.onNodeWithContentDescription("绝对时间提醒").performScrollTo().performClick()
        compose.onNodeWithText("完成").performClick()
        compose.runOnIdle { assertNotNull(draft); assertNull(draft!!.dueAt); assertTrue(draft!!.absoluteReminderTimesMillis.isEmpty()) }
    }
    @Test fun cancelAfterRecognitionWritesNothing() {
        var opened by mutableStateOf(true);var imports=0;var recognize=0
        var hideKeyboard:()->Unit={}
        compose.setContent { TodoAlarmTheme { Surface {
            val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            SideEffect { hideKeyboard={keyboard?.hide();Unit} }
            if(opened) TransientPlanningDialog(listOf(TaskGroup(1,"例行","#467DAB",0,true)),listOf(PlannerCalendar("default","常规","#467DAB")),false,42,
                onRecognize={text,_,now->recognize++;TransientPlanningParser.parse(text,now)},onImport={_,_,_->imports++;null},onDismiss={opened=false})
        } } }
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("- 待办,16:30;")
        compose.runOnIdle { hideKeyboard() }
        compose.onNodeWithText("识别").performClick()
        compose.onNodeWithText("确认规划").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { assertFalse(opened);assertEquals(1,recognize);assertEquals(0,imports) }
    }
    @Test fun editablePreviewConfirmsOnceWithStableIdentityAndNoDocument() {
        var opened by mutableStateOf(true);var imported=emptyList<TransientPlanningCandidate>();var calls=0;var parsedId="";var importedSession=""
        var hideKeyboard:()->Unit={}
        compose.setContent { TodoAlarmTheme { Surface {
            val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            SideEffect { hideKeyboard={keyboard?.hide();Unit} }
            if(opened) TransientPlanningDialog(listOf(TaskGroup(1,"例行","#467DAB",0,true)),listOf(PlannerCalendar("default","常规","#467DAB")),false,42,
                onRecognize={text,_,now->TransientPlanningParser.parse(text,now).also { parsedId=it.candidates.single().id }},
                onImport={items,session,generation->assertEquals(42L,generation);calls++;imported=items;importedSession=session;null},onDismiss={opened=false})
        } } }
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("仅标题;")
        compose.runOnIdle { hideKeyboard() }
        compose.onNodeWithText("识别").performClick()
        compose.onNodeWithText("确认规划").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0,calls) }
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("预览中修改的标题")
        compose.runOnIdle { hideKeyboard() }
        compose.onNodeWithText("确认导入").performClick()
        compose.waitUntil(5_000){!opened}
        compose.runOnIdle { assertEquals(1,calls);assertEquals("预览中修改的标题",imported.single().title);assertEquals(parsedId,imported.single().id);assertTrue(importedSession.isNotBlank());assertNull(imported.single().dueAt) }
    }
}
