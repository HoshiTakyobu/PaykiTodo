package com.example.todoalarm.ui

import android.graphics.Bitmap
import android.view.Choreographer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.todoalarm.data.AppSettings
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.PlannerCalendar
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CalendarParticipantsAcceptanceTest {
    @get:Rule val compose = createComposeRule()
    private val calendar = PlannerCalendar("shared:first", "项目一", ownerAccount = "alice", shareMode = "SHARED", sharedRole = "EDITOR",
        membersJson = """[{"username":"alice","displayName":"张同学","role":"owner"},{"username":"bob","displayName":"李同学","role":"viewer"},{"username":"carol","displayName":"王同学","role":"viewer"},{"username":"eve","displayName":"已离开成员","role":"revoked"}]""")
    private val initial = """[{"username":"alice","response":"ACCEPTED"}]"""

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val ready = CountDownLatch(1)
        instrumentation.runOnMainSync { Choreographer.getInstance().postFrameCallback {
            Choreographer.getInstance().postFrameCallback { ready.countDown() }
        } }
        assertTrue(ready.await(3, TimeUnit.SECONDS))
        instrumentation.uiAutomation.waitForIdle(300, 3000)
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "ui-acceptance").apply { mkdirs() }
        File(folder, "participants-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun memberSelectionCancelAndConfirmPreserveExistingResponses() {
        var applied = initial
        compose.setContent {
            TodoAlarmTheme(systemDarkTheme = false) {
                var value by remember { mutableStateOf(initial) }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.padding(20.dp)) {
                        CalendarParticipantsEditor(calendar, value) { value = it; applied = it }
                        CalendarParticipantsDetails(calendar, value)
                    }
                }
            }
        }
        compose.onNodeWithText("参与者").performClick()
        compose.onNodeWithContentDescription("选择参与者：eve").assertDoesNotExist()
        compose.onNodeWithContentDescription("选择参与者：bob").performClick()
        compose.onNodeWithTag("calendar-participants-cancel").performClick()
        assertEquals(initial, applied)
        compose.onNodeWithText("参与者").performClick()
        compose.onNodeWithContentDescription("选择参与者：bob").performClick()
        capture("selection")
        compose.onNodeWithTag("calendar-participants-confirm").performClick()
        assertEquals(mapOf("alice" to "ACCEPTED", "bob" to "NEEDS_ACTION"), calendarParticipants(applied).associate { it.username to it.response })
        capture("detail")
    }

    private fun showEditor(calendars: List<PlannerCalendar>, onDismiss: () -> Unit, onConfirm: (CalendarEventDraft) -> Unit) {
        val start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0)
        val draft = CalendarEventDraft(title = "项目讨论", notes = "", location = "", startAt = start, endAt = start.plusMinutes(45),
            allDay = false, calendarId = calendar.id, eventParticipantsJson = initial, eventTimeZoneId = "Asia/Shanghai",
            accentColorHex = "#3370FF", ringEnabled = false, vibrateEnabled = true)
        compose.setContent {
            TodoAlarmTheme(systemDarkTheme = false) {
                CompositionLocalProvider(LocalWorkspacePreferences provides WorkspacePreferences(AppSettings(), calendars)) {
                    CalendarEventEditorDialog(null, initialDraft = draft, defaultRingEnabled = false, defaultVibrateEnabled = true,
                        onDismiss = onDismiss, onConfirm = onConfirm)
                }
            }
        }
    }

    @Test fun nestedSelectionDoesNotPublishUntilTheEventIsCompleted() {
        var saved: CalendarEventDraft? = null
        var dismissed = false
        showEditor(listOf(PlannerCalendar(), calendar), { dismissed = true }, { saved = it })
        compose.onNodeWithText("参与者").performClick()
        compose.onNodeWithContentDescription("选择参与者：bob").performClick()
        compose.onNodeWithTag("calendar-participants-confirm").performClick()
        assertNull(saved)
        capture("event-draft")
        compose.onNodeWithText("取消").performClick()
        assertTrue(dismissed)
        assertNull(saved)
    }

    @Test fun changingCalendarClearsThePreviousMemberSelection() {
        var saved: CalendarEventDraft? = null
        val other = calendar.copy(id = "shared:other", name = "项目二", ownerAccount = "carol",
            membersJson = """[{"username":"carol","displayName":"王同学","role":"owner"}]""")
        showEditor(listOf(PlannerCalendar(), calendar, other), {}, { saved = it })
        compose.onNodeWithText("所属日历").performClick()
        compose.onNodeWithText("项目二").performClick()
        compose.onNodeWithText("选择成员").assertIsDisplayed()
        compose.onNodeWithText("完成").performClick()
        compose.waitUntil(5000) { saved != null }
        assertEquals(other.id, saved!!.calendarId)
        assertEquals(emptyList<CalendarParticipant>(), calendarParticipants(saved!!.eventParticipantsJson))
    }

    @Test fun restrictedDetailsUseOnlyProjectedParticipantsAndTheCurrentResponse() {
        val projection = """[{"username":"bob","response":"DECLINED"}]"""
        var chosen: String? = null
        compose.setContent {
            TodoAlarmTheme(systemDarkTheme = false) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.padding(20.dp)) {
                        CalendarParticipantsDetails(calendar.copy(sharedRole = "VIEWER"), projection, restricted = true)
                        CalendarParticipantResponseChoice(requireNotNull(calendarParticipantResponseFor(projection, "BOB"))) { chosen = it }
                    }
                }
            }
        }
        compose.onNodeWithText("组织者 · 张同学").assertIsDisplayed()
        compose.onNodeWithText("李同学").assertIsDisplayed()
        compose.onNodeWithText("王同学").assertDoesNotExist()
        compose.onNodeWithText("已拒绝").assertIsDisplayed()
        compose.onNodeWithText("拒绝").assertIsDisplayed()
        capture("restricted-detail")
        compose.onNodeWithText("我的回应").performClick()
        compose.onNodeWithText("接受").performClick()
        assertEquals("ACCEPTED", chosen)
        assertEquals("DECLINED", calendarParticipantResponseFor(projection, "bob"))
    }
}
