package com.example.todoalarm.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.todoalarm.data.*
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FeedbackControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun completionTriggersProductionMemorialPromptBeforeNextClockTick() {
        var item by mutableStateOf(TodoItem(id = 8126, title = "完成后纪念", dueAtMillis = System.currentTimeMillis(), groupId = 1,
            countdownEnabled = true, reminderAtMillis = null, reminderEnabled = false, ringEnabled = false, vibrateEnabled = false))
        compose.setContent { TodoAlarmTheme {
            CountdownDecisionPrompt(listOf(item), 7, true) { _, mode -> item = item.copy(countdownPastMode = mode) }
        } }
        compose.onNodeWithText("继续保留这个日子？").assertDoesNotExist()
        Thread.sleep(30)
        compose.runOnIdle { item = item.copy(completed = true, completedAtMillis = System.currentTimeMillis()) }
        compose.onNodeWithText("保留纪念").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("KEEP", item.countdownPastMode); assertTrue(item.completed) }
        compose.onNodeWithText("继续保留这个日子？").assertDoesNotExist()
    }

    @Test fun calendarCreateCancelColorAndVisibilityUseIndependentOwnership() {
        var calendars by mutableStateOf(listOf(PlannerCalendar()))
        var settings by mutableStateOf(AppSettings())
        compose.setContent { TodoAlarmTheme {
            CompositionLocalProvider(LocalWorkspacePreferences provides WorkspacePreferences(settings, calendars,
                saveCalendar = { calendar -> calendars = calendars.filterNot { it.id == calendar.id } + calendar },
                display = { _, _, _, hidden -> settings = settings.copy(hiddenCalendarIds = hidden) })) {
                CalendarManager {}
            }
        } }
        compose.onNodeWithText("新增日历").performClick()
        compose.onNodeWithText("日历名称").performTextInput("不应保存")
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { assertEquals(1, calendars.size) }
        compose.onNodeWithText("新增日历").performClick()
        compose.onNodeWithText("日历名称").performTextInput("课程表")
        compose.onNodeWithText("颜色").performClick()
        compose.onNodeWithText("紫色").performClick()
        compose.onNodeWithText("完成").performClick()
        compose.runOnIdle { assertEquals("#7955CC", calendars.last().colorHex) }
        compose.onNodeWithContentDescription("显示日历：课程表").performClick()
        compose.runOnIdle { assertTrue(calendars.last().id in settings.hiddenCalendarIds); assertEquals(2, calendars.size) }
        compose.onNodeWithContentDescription("显示日历：课程表").assertIsOff()
    }

    @Test fun eventEditorSavesChosenCalendarAndItsColor() {
        val start = java.time.LocalDateTime.now().plusDays(1).withHour(9).withMinute(15).withSecond(0).withNano(0)
        val seed = CalendarEventDraft("线性代数", "", "", start, start.plusMinutes(30), false, "#3370FF", ringEnabled = false, vibrateEnabled = false)
        var result: CalendarEventDraft? = null
        compose.setContent { TodoAlarmTheme {
            CompositionLocalProvider(LocalWorkspacePreferences provides WorkspacePreferences(calendars = listOf(PlannerCalendar(), PlannerCalendar("courses", "课程表", "#7955CC")))) {
                CalendarEventEditorDialog(null, seed, defaultRingEnabled = false, defaultVibrateEnabled = false, onDismiss = {}, onConfirm = { result = it })
            }
        } }
        compose.onNodeWithText("所属日历").performScrollTo().performClick()
        compose.onNodeWithText("课程表").performClick()
        compose.onNodeWithText("完成").performClick()
        compose.runOnIdle { assertEquals("courses", result?.calendarId); assertEquals("#7955CC", result?.accentColorHex); assertEquals(start.plusMinutes(30), result?.endAt) }
    }

    @Test fun appearanceCancelDoesNotWriteAndDoneAppliesFontAndRetention() {
        var opened by mutableStateOf(true)
        var result: AppSettings? = null
        compose.setContent { TodoAlarmTheme {
            CompositionLocalProvider(LocalWorkspacePreferences provides WorkspacePreferences(display = { font, path, days, hidden ->
                result = AppSettings(mainFont = font, customFontPath = path, countdownPastDays = days, hiddenCalendarIds = hidden)
            })) { if (opened) AppearanceSettings { opened = false } }
        } }
        compose.onNodeWithText("字体").performClick()
        compose.onNodeWithText("系统等宽").performClick()
        compose.onNodeWithText("取消").performClick()
        compose.waitUntil(3_000) { !opened }
        compose.runOnIdle { assertNull(result); opened = true }
        compose.onNodeWithText("字体").performClick()
        compose.onNodeWithText("系统等宽").performClick()
        compose.onNodeWithText("历史目标保留天数（0–365）").performScrollTo().performTextReplacement("14")
        compose.onNodeWithText("完成").performClick()
        compose.runOnIdle { assertEquals("MONOSPACE", result?.mainFont); assertEquals(14, result?.countdownPastDays) }
    }

    @Test fun countdownOffersBothPersistentChoices() {
        var choice: String? = null
        compose.setContent { TodoAlarmTheme {
            CountdownDecisionDialog(TodoItem(title = "毕业纪念", dueAtMillis = 1, groupId = 1, countdownEnabled = true, reminderAtMillis = null, reminderEnabled = false, ringEnabled = false, vibrateEnabled = false), {}, { choice = it })
        } }
        compose.onNodeWithText("保留纪念").performClick()
        compose.runOnIdle { assertEquals("KEEP", choice) }
        compose.onNodeWithText("不再展示").performClick()
        compose.runOnIdle { assertEquals("HIDE", choice) }
    }

    @Test fun backupPageSeparatesDirectoryExportAndRestore() {
        var action = ""
        compose.setContent { TodoAlarmTheme {
            SettingsSectionDialog("数据与备份", {}) {
                BackupPreferencesContent(AppSettings(backupDirectoryUri = "content://private/location"), { action = "auto" }, { action = "directory" }, { action = "export" }, { action = "restore" })
            }
        } }
        compose.onNodeWithText("content://private/location").assertDoesNotExist()
        compose.onNodeWithText("备份目录").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("directory", action) }
        compose.onNodeWithText("导出迁移包").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("export", action) }
        compose.onNodeWithText("从迁移包恢复").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("restore", action) }
        val inst = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val folder = java.io.File(inst.targetContext.getExternalFilesDir(null), "ui-acceptance").apply { mkdirs() }
        inst.uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(folder, "feedback-backup.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        }
    }
}
