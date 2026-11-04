package com.example.todoalarm.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
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
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Production Compose controls, isolated drafts and no account/network mutations. */
class LayoutRefinementTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        compose.waitForIdle()
        val inst = InstrumentationRegistry.getInstrumentation()
        inst.waitForIdleSync(); android.os.SystemClock.sleep(300)
        val folder = File(inst.targetContext.getExternalFilesDir(null), "layout-1.17.16").apply { mkdirs() }
        inst.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        }
    }
    @Test fun editorColorGridIsTwoRowsAndCancelLeavesDraftUncommitted() {
        val start = LocalDateTime.of(2027, 1, 12, 10, 30)
        val original = CalendarEventDraft(title = "实验研讨", notes = "先阅读相关资料", location = "资料室", startAt = start,
            endAt = start.plusHours(1), allDay = false, accentColorHex = "#123ABC", eventTimeZoneId = "America/New_York", ringEnabled = false, vibrateEnabled = false)
        var saved: CalendarEventDraft? = null; var canceled = false
        compose.setContent { TodoAlarmTheme {
            CompositionLocalProvider(LocalWorkspacePreferences provides WorkspacePreferences(settings = AppSettings(calendarDisplayTimeZoneId = "Asia/Shanghai"))) {
                CalendarEventEditorDialog(null, initialDraft = original, defaultRingEnabled = false, defaultVibrateEnabled = false,
                    onDismiss = { canceled = true }, onConfirm = { saved = it })
            }
        } }
        compose.onNodeWithText("日程时区").assertDoesNotExist()
        capture("editor-top")
        compose.onNodeWithText("日程颜色").performScrollTo()
        compose.onNodeWithContentDescription("日程颜色 #4C88FF").performScrollTo()
        val first = compose.onNodeWithContentDescription("日程颜色 #C24B8F").fetchSemanticsNode().boundsInRoot
        val sixth = compose.onNodeWithContentDescription("日程颜色 #17CFB5").fetchSemanticsNode().boundsInRoot
        val seventh = compose.onNodeWithContentDescription("日程颜色 #4C88FF").fetchSemanticsNode().boundsInRoot
        assertEquals(first.top, sixth.top, 1f); assertTrue(seventh.top > first.top)
        compose.onNodeWithContentDescription("日程颜色 #4C88FF").performClick().assertIsSelected()
        compose.onNodeWithContentDescription("已选中").assertExists()
        capture("palette-two-rows")
        compose.onNodeWithText("取消").performClick()
        assertTrue(canceled); assertNull(saved); assertEquals("#123ABC", original.accentColorHex)
    }
    @Test fun newEventUsesGlobalTimezoneAndIntegratedSubjectInput() {
        var saved: CalendarEventDraft? = null
        compose.setContent { TodoAlarmTheme {
            CompositionLocalProvider(LocalWorkspacePreferences provides WorkspacePreferences(settings = AppSettings(calendarDisplayTimeZoneId = "Asia/Tokyo"))) {
                CalendarEventEditorDialog(null, defaultRingEnabled = false, defaultVibrateEnabled = false, onDismiss = {}, onConfirm = { saved = it })
            }
        } }
        compose.onNodeWithContentDescription("添加主题").performTextReplacement("新建研讨日程")
        compose.onNodeWithText("日程时区").assertDoesNotExist()
        compose.onNodeWithText("完成").performClick(); compose.waitUntil(5000) { saved != null }
        assertEquals("Asia/Tokyo", saved!!.eventTimeZoneId); assertEquals("新建研讨日程", saved!!.title)
        capture("editor-integrated-subject")
    }
    @Test fun reportListPreviewRendersMarkupWithoutHtmlOrNetwork() {
        val raw = "### 今日回顾\n\n完成 **实验记录**，整理 `结论`。\n\n- 明天继续验证\n"
        compose.setContent { TodoAlarmTheme {
            Surface { Column(Modifier.fillMaxWidth().padding(20.dp)) { ReportMarkdownPreview(raw) } }
        } }
        compose.onNodeWithText("今日回顾", substring = true).assertIsDisplayed()
        compose.onNodeWithText("###", substring = true).assertDoesNotExist()
        compose.onNodeWithText("**", substring = true).assertDoesNotExist()
        capture("report-preview")
        assertTrue(raw.contains("**实验记录**"))
    }
    @Test fun calendarHeaderHasNamedCompactEntryAndSettings() {
        compose.setContent { TodoAlarmTheme(systemDarkTheme = true) {
            CalendarPanel(events = emptyList(), weekStartMode = WeekStartMode.MONDAY, scheduleTemplates = emptyList(),
                onQuickCreateEvent = { _, _ -> }, onCreateEventAt = { _, _ -> }, onEditEvent = {}, onGetEventById = { null }, onGetEventCheckIns = { emptyList() },
                onCopyEvent = {}, onMoveEvent = { _, _, _, _ -> }, onCancelEvent = {}, onDeleteEvent = {}, onOpenBatchImport = {},
                onSaveWeekAsTemplate = { _, _, _ -> null }, onApplyTemplateToWeek = { _, _ -> null }, onGenerateSemesterFromTemplate = { _, _, _ -> null }, onDeleteTemplate = { null })
        } }
        compose.onNodeWithText("我的日历").assertIsDisplayed()
        compose.onNodeWithText("日历设置").assertIsDisplayed()
        compose.onNodeWithText("日历 · 1").assertDoesNotExist()
        capture("calendar-header")
        compose.onNodeWithText("我的日历").performClick()
        compose.onNodeWithText("常规日历").assertExists()
        capture("calendar-manager")
    }
}
