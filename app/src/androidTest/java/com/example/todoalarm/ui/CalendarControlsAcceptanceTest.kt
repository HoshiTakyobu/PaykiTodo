package com.example.todoalarm.ui

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.RecurrenceType
import com.example.todoalarm.data.decodeAllDayReminderRules
import com.example.todoalarm.sync.AccountAuthResult
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File
import java.time.LocalDateTime

class CalendarControlsAcceptanceTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val testName = TestName()

    @After fun capture() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "ui-acceptance").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(folder, "${testName.methodName}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun serverAddressAppearsOnlyAfterSelectionAndCancelDoesNotApply() {
        var selected: String? = null
        compose.setContent {
            TodoAlarmTheme {
                AccountAuthScreen(onLogin = { _, _, _ -> AccountAuthResult("unused") }, onRegister = { _, _, _, _ -> AccountAuthResult("unused") },
                    onRecover = { _, _, _, _ -> null }, onRecoveryCode = {}, loginServerUrl = "https://old.example",
                    onSelectLoginServer = { selected = it; null })
            }
        }
        compose.onNodeWithText("https://old.example").assertDoesNotExist()
        compose.onNodeWithText("选择登录服务器").performClick()
        compose.onNode(hasText("服务器地址") and hasSetTextAction()).performTextReplacement("https://changed.example")
        compose.onNodeWithText("取消").performClick()
        assertNull(selected)
        compose.onNodeWithText("选择登录服务器").performClick()
        compose.onNode(hasText("服务器地址") and hasSetTextAction()).performTextReplacement("https://changed.example")
        compose.onNodeWithText("完成").performClick()
        compose.waitUntil(5000) { selected != null }
        assertEquals("https://changed.example", selected)
    }

    @Test fun allDayReminderAllowsAnExactClockAndDayOffset() {
        var saved = "[]"
        compose.setContent {
            TodoAlarmTheme {
                var rules by remember { mutableStateOf("[]") }
                AllDayReminderRulesEditor(rules) { rules = it; saved = it }
            }
        }
        compose.onNodeWithText("添加全天提醒").performClick()
        compose.onNode(hasText("提前天数（0为当天）") and hasSetTextAction()).performTextReplacement("1")
        compose.onNode(hasText("提醒钟点") and hasSetTextAction()).performTextReplacement("09:17")
        compose.onNodeWithText("完成").performClick()
        compose.onNodeWithText("提前1天 09:17").assertIsDisplayed()
        val rule = decodeAllDayReminderRules(saved).single()
        assertEquals(1, rule.daysBefore)
        assertEquals(557, rule.minuteOfDay)
    }

    @Test fun timedDraftSurvivesAllDayRoundTripWithIntervalAndTimeZone() {
        val start = LocalDateTime.of(2027, 1, 31, 13, 15)
        val original = CalendarEventDraft(title = "差旅", notes = "", location = "", startAt = start, endAt = start.plusMinutes(75), allDay = false,
            eventTimeZoneId = "America/New_York", accentColorHex = "#3370FF", reminderOffsetsMinutes = listOf(5, 0), ringEnabled = false,
            vibrateEnabled = true, recurrence = RecurrenceConfig(true, RecurrenceType.MONTHLY_DAY, endDate = null, interval = 2, invalidDatePolicy = "SKIP"))
        var saved: CalendarEventDraft? = null
        compose.setContent {
            TodoAlarmTheme {
                CompositionLocalProvider(LocalWorkspacePreferences provides WorkspacePreferences(settings = AppSettings())) {
                    CalendarEventEditorDialog(null, initialDraft = original, defaultRingEnabled = false, defaultVibrateEnabled = true, onDismiss = {}, onConfirm = { saved = it })
                }
            }
        }
        compose.onNodeWithTag("calendar-editor-all-day").performClick()
        compose.onNodeWithTag("calendar-editor-all-day").performClick()
        compose.onNodeWithText("完成").performClick()
        compose.waitUntil(5000) { saved != null }
        assertEquals(start, saved!!.startAt)
        assertEquals(start.plusMinutes(75), saved!!.endAt)
        assertEquals(listOf(0, 5), saved!!.normalizedReminderOffsetsMinutes.sorted())
        assertEquals("America/New_York", saved!!.eventTimeZoneId)
        assertEquals(2, saved!!.recurrence.interval)
        assertNull(saved!!.recurrence.endDate)
        assertTrue(saved!!.recurrence.invalidDatePolicy == "SKIP")
    }
}
