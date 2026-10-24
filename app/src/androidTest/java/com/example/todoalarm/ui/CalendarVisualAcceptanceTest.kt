package com.example.todoalarm.ui

import android.graphics.Bitmap
import android.view.Choreographer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.PlannerCalendar
import com.example.todoalarm.data.PlannerItemType
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.RecurrenceType
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.WeekStartMode
import com.example.todoalarm.sync.AccountAuthResult
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Actual production components with in-memory display fixtures; no account or Room writes. */
class CalendarVisualAcceptanceTest {
    @get:Rule val compose = createComposeRule()
    private val zone = ZoneId.of("Asia/Shanghai")
    private val calendars = listOf(
        PlannerCalendar("default", "日常安排", "#3370FF", 0, "生活与个人日程"),
        PlannerCalendar("classes", "课程表", "#16A369", 1, "本学期课程和考试"),
        PlannerCalendar("exercise", "运动", "#DB5298", 2, "跑步与训练"),
        PlannerCalendar("shared:visual", "项目组", "#BD6B17", 3, sharedRole = "VIEWER", shareMode = "SHARED")
    )
    private val settings = AppSettings(calendarDisplayTimeZoneId = zone.id,
        calendarAuxiliaryTimeZoneId = "America/New_York", calendarDefaultDurationMinutes = 45)

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val rendered = CountDownLatch(1)
        instrumentation.runOnMainSync {
            Choreographer.getInstance().postFrameCallback {
                Choreographer.getInstance().postFrameCallback { rendered.countDown() }
            }
        }
        assertTrue("Screenshot should wait for the current scroll position to render", rendered.await(3, TimeUnit.SECONDS))
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "ui-acceptance").apply { mkdirs() }
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(folder, "visual-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun timelineAndAgendaRenderNamedCalendarsAndAllDayDates() {
        val date = LocalDate.now(zone)
        val start = LocalDateTime.now(zone).withSecond(0).withNano(0).plusHours(1)
        fun millis(value: LocalDateTime) = value.atZone(zone).toInstant().toEpochMilli()
        val events = listOf(
            TodoItem(id = 98001, itemType = PlannerItemType.EVENT.name, title = "课题讨论", calendarId = "classes",
                startAtMillis = millis(start), endAtMillis = millis(start.plusMinutes(45)), dueAtMillis = millis(start.plusMinutes(45)),
                location = "科研楼 302", eventTimeZoneId = zone.id, accentColorHex = "#16A369",
                reminderAtMillis = null, reminderEnabled = false, ringEnabled = false, vibrateEnabled = false),
            TodoItem(id = 98002, itemType = PlannerItemType.EVENT.name, title = "学期项目", calendarId = "default", allDay = true,
                startAtMillis = millis(date.atStartOfDay()), endAtMillis = millis(date.plusDays(2).atStartOfDay()),
                dueAtMillis = millis(date.plusDays(2).atStartOfDay()), eventTimeZoneId = zone.id, accentColorHex = "#3370FF",
                reminderAtMillis = null, reminderEnabled = false, ringEnabled = false, vibrateEnabled = false)
        )
        compose.setContent {
            TodoAlarmTheme(systemDarkTheme = false) {
                CompositionLocalProvider(LocalWorkspacePreferences provides WorkspacePreferences(settings, calendars)) {
                    CalendarPanel(events = events, weekStartMode = WeekStartMode.MONDAY, scheduleTemplates = emptyList(), initialFocusDate = date,
                        onQuickCreateEvent = { _, _ -> }, onCreateEventAt = { _, _ -> }, onEditEvent = {},
                        onGetEventById = { id -> events.firstOrNull { it.id == id } }, onGetEventCheckIns = { emptyList() }, onCopyEvent = {},
                        onMoveEvent = { _, _, _, _ -> }, onCancelEvent = {}, onDeleteEvent = {}, onOpenBatchImport = {},
                        onSaveWeekAsTemplate = { _, _, _ -> null }, onApplyTemplateToWeek = { _, _ -> null },
                        onGenerateSemesterFromTemplate = { _, _, _ -> null }, onDeleteTemplate = { null })
                }
            }
        }
        compose.onNodeWithText("学期项目").assertIsDisplayed()
        capture("calendar-timeline")
        compose.onNodeWithContentDescription("三日视图").performClick()
        compose.onNodeWithText("列表视图").performClick()
        compose.onNodeWithText("科研楼 302").assertIsDisplayed()
        capture("calendar-agenda")
    }

    @Test fun multipleCalendarVisibilityChangesOnlyTheInMemoryFixture() {
        var hidden = emptySet<String>()
        compose.setContent {
            TodoAlarmTheme(systemDarkTheme = false) {
                var draftSettings by remember { mutableStateOf(settings) }
                CompositionLocalProvider(LocalWorkspacePreferences provides WorkspacePreferences(draftSettings, calendars,
                    display = { _, _, _, ids -> hidden = ids; draftSettings = draftSettings.copy(hiddenCalendarIds = ids) })) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { CalendarManager {} }
                }
            }
        }
        compose.onNodeWithText("课程表").assertIsDisplayed()
        compose.onNodeWithText("共享 · 只读").assertIsDisplayed()
        compose.onNodeWithContentDescription("显示日历：运动").performClick().assertIsOff()
        assertEquals(setOf("exercise"), hidden)
        capture("multiple-calendars")
    }

    @Test fun editorShowsCalendarRecurrenceAndTimezoneControls() {
        val start = LocalDate.now(zone).plusDays(1).atTime(9, 0)
        val draft = CalendarEventDraft(title = "课程安排", notes = "准备实验材料", location = "教学楼 201", calendarId = "classes",
            startAt = start, endAt = start.plusMinutes(45), allDay = false, eventTimeZoneId = zone.id,
            accentColorHex = "#16A369", reminderOffsetsMinutes = listOf(15, 0), ringEnabled = false, vibrateEnabled = true,
            recurrence = RecurrenceConfig(true, RecurrenceType.WEEKLY, interval = 2, weeklyDays = setOf(start.dayOfWeek)))
        compose.setContent {
            TodoAlarmTheme(systemDarkTheme = false) {
                CompositionLocalProvider(LocalWorkspacePreferences provides WorkspacePreferences(settings, calendars)) {
                    CalendarEventEditorDialog(null, initialDraft = draft, defaultRingEnabled = false, defaultVibrateEnabled = true,
                        onDismiss = {}, onConfirm = {})
                }
            }
        }
        compose.onNodeWithText("所属日历").assertIsDisplayed()
        capture("event-editor")
        compose.onNodeWithText("预览循环生成").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("重复间隔（1–10000）").assertIsDisplayed()
        capture("event-recurrence")
        compose.onNodeWithText("个人日历的内容只在你的账号内同步；共享日历的成员访问由服务器权限控制。").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("时区与可见范围").assertIsDisplayed()
        capture("event-timezone")
    }

    @Test fun accountFooterUsesCompactActionsOnTheSameRow() {
        compose.setContent {
            TodoAlarmTheme(systemDarkTheme = false) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SettingsSectionDialog("账号设置", {}) {
                        AccountSettingsPanel(settings.copy(remoteSyncUsername = "calendar-preview", remoteSyncDisplayName = "日历用户",
                            remoteSyncUserId = 17, remoteSyncServerUrl = "https://calendar.example.invalid"), {}, refreshProfileOnOpen = false)
                    }
                }
            }
        }
        compose.onNodeWithText("退出账号").performScrollTo().assertIsDisplayed()
        val restore = compose.onNodeWithText("恢复操作前副本").fetchSemanticsNode().boundsInRoot
        val logout = compose.onNodeWithText("退出账号").fetchSemanticsNode().boundsInRoot
        assertTrue("Footer actions should share a row", abs(restore.center.y - logout.center.y) < 2f)
        capture("account-settings")
    }

    @Test fun loginDomainIsHiddenUntilServerSelectorOpens() {
        compose.setContent {
            TodoAlarmTheme(systemDarkTheme = false) {
                AccountAuthScreen(onLogin = { _, _, _ -> AccountAuthResult("unused") },
                    onRegister = { _, _, _, _ -> AccountAuthResult("unused") }, onRecover = { _, _, _, _ -> null }, onRecoveryCode = {},
                    loginServerUrl = "https://calendar.example.invalid", onSelectLoginServer = { null })
            }
        }
        compose.onNodeWithText("https://calendar.example.invalid").assertDoesNotExist()
        compose.onNodeWithText("选择登录服务器").assertIsDisplayed()
        capture("login")
        compose.onNodeWithText("选择登录服务器").performClick()
        compose.onNodeWithText("服务器地址").assertIsDisplayed()
        capture("login-server-selector")
    }
}
