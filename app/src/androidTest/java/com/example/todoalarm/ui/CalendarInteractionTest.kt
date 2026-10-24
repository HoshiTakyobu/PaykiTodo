package com.example.todoalarm.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeDown
import androidx.test.platform.app.InstrumentationRegistry
import com.example.todoalarm.data.PlannerItemType
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.WeekStartMode
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

class CalendarInteractionTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val testName = TestName()
    @Volatile private var visibleStart: LocalDate? = null
    @Volatile private var visibleEnd: LocalDate? = null

    @After
    fun screenshot() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "ui-acceptance").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(folder, "${testName.methodName}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private fun showCalendar(date: LocalDate, dark: Boolean = false, withEvent: Boolean = false) {
        val start = date.atTime(18, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val event = TodoItem(
            id = 9876, itemType = PlannerItemType.EVENT.name,
            title = "合唱团暑期集训与校庆节目排练", dueAtMillis = start + 7200000,
            startAtMillis = start, endAtMillis = start + 7200000, location = "@学生发展中心111",
            accentColorHex = "#4C8BF5", reminderAtMillis = null, reminderEnabled = false, ringEnabled = false, vibrateEnabled = false
        )
        compose.setContent {
            TodoAlarmTheme(systemDarkTheme = dark) {
                CalendarPanel(
                    events = if (withEvent) listOf(event) else emptyList(), weekStartMode = WeekStartMode.MONDAY,
                    scheduleTemplates = emptyList(), initialFocusDate = date,
                    onVisibleDateRangeChange = { first, last -> visibleStart = first; visibleEnd = last },
                    onQuickCreateEvent = { _, _ -> }, onCreateEventAt = { _, _ -> }, onEditEvent = {},
                    onGetEventById = { null }, onGetEventCheckIns = { emptyList() }, onCopyEvent = {},
                    onMoveEvent = { _, _, _, _ -> }, onCancelEvent = {}, onDeleteEvent = {}, onOpenBatchImport = {},
                    onSaveWeekAsTemplate = { _, _, _ -> null }, onApplyTemplateToWeek = { _, _ -> null },
                    onGenerateSemesterFromTemplate = { _, _, _ -> null }, onDeleteTemplate = { null }
                )
            }
        }
        compose.waitForIdle()
    }

    private fun mode(from: String, to: String) {
        compose.onNodeWithContentDescription(from).performClick()
        compose.onNodeWithText(to).performClick()
        compose.waitForIdle()
    }

    @Test
    fun weeksContinueInBothDirectionsWithoutSkipping() {
        val firstWeek = LocalDate.of(2026, 8, 24)
        showCalendar(firstWeek.plusDays(1))
        mode("三日视图", "列表视图")
        repeat(4) { index ->
            compose.onNodeWithTag("calendar-weeks").performTouchInput { swipeRight() }
            compose.waitUntil(5000) { visibleStart == firstWeek.minusWeeks(index + 1L) }
        }
        repeat(9) { index ->
            compose.onNodeWithTag("calendar-weeks").performTouchInput { swipeLeft() }
            compose.waitUntil(5000) { visibleStart == firstWeek.minusWeeks(3L).plusWeeks(index.toLong()) }
        }
        assertEquals(firstWeek.plusWeeks(5), visibleStart)
    }

    @Test
    fun monthSwipesAndDoubleTapCenterTheSelectedDay() {
        val date = LocalDate.of(2026, 6, 11)
        showCalendar(date)
        mode("三日视图", "月视图")
        compose.onNodeWithTag("calendar-months").performTouchInput { swipeUp() }
        compose.onNodeWithText("2026年7月").assertIsDisplayed()
        compose.onNodeWithTag("calendar-months").performTouchInput { swipeDown() }
        compose.onNodeWithText("2026年6月").assertIsDisplayed()
        compose.onNodeWithTag("month-day-$date").performTouchInput { doubleClick() }
        compose.waitUntil(5000) { visibleStart == date.minusDays(1) && visibleEnd == date.plusDays(2) }
        compose.onNodeWithContentDescription("三日视图").assertIsDisplayed()
    }

    @Test
    fun todaySelectsTheRealDateAndRevealsItsAgendaRow() {
        val today = LocalDate.now()
        showCalendar(today.minusMonths(3))
        mode("三日视图", "月视图")
        compose.onNodeWithText("今天").performClick()
        val matchingDays = compose.onAllNodesWithTag("month-day-$today")
        assertTrue(matchingDays.fetchSemanticsNodes().indices.any { matchingDays[it].isDisplayed() })
        mode("月视图", "三日视图")
        compose.waitUntil(5000) { visibleStart == today.minusDays(1) }
        mode("三日视图", "列表视图")
        compose.onNodeWithTag("calendar-weeks").performTouchInput { swipeRight() }
        compose.onNodeWithText("今天").performClick()
        compose.onNodeWithTag("agenda-day-$today").assertIsDisplayed()
    }

    @Test
    fun agendaAccentCoversTitleTimeAndLocation() {
        val today = LocalDate.now()
        showCalendar(today, withEvent = true)
        mode("三日视图", "列表视图")
        val accent = compose.onNodeWithTag("agenda-accent-9876", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val content = compose.onNodeWithTag("agenda-content-9876", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(content.top, accent.top, 1f)
        assertEquals(content.bottom, accent.bottom, 1f)
        compose.onNodeWithText("@学生发展中心111").assertIsDisplayed()
    }

    @Test
    fun darkAgendaRemainsReadable() {
        showCalendar(LocalDate.now(), dark = true, withEvent = true)
        mode("三日视图", "列表视图")
        compose.onNodeWithText("@学生发展中心111").assertIsDisplayed()
    }

    @Test
    fun todoButtonOnlyOpensBatchAfterEightHundredMilliseconds() {
        var single = 0
        var batch = 0
        compose.setContent {
            TodoAlarmTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize()) { TodoCreateButton(onClick = { single++ }, onBatchCreate = { batch++ }) }
                }
            }
        }
        compose.onNodeWithContentDescription("创建待办").performTouchInput { down(center); advanceEventTime(650); up() }
        compose.runOnIdle { assertEquals(1, single); assertEquals(0, batch) }
        compose.onNodeWithContentDescription("创建待办").performTouchInput { longClick(durationMillis = 900) }
        compose.runOnIdle { assertEquals(1, single); assertEquals(1, batch) }
        compose.onNodeWithContentDescription("创建待办").performClick()
        compose.runOnIdle { assertTrue(single == 2 && batch == 1) }
    }
}
