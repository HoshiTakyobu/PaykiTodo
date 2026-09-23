package com.example.todoalarm.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.todoalarm.data.PlannerItemType
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DailyBoardReferenceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun agendaListsEveryTomorrowEventAndOpensItsDetails() {
        val today = LocalDate.now()
        fun event(id: Long, title: String, day: LocalDate, hour: Int): TodoItem {
            val start = day.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return TodoItem(id = id, title = title, notes = "", dueAtMillis = start + 3600000,
                startAtMillis = start, endAtMillis = start + 3600000, itemType = PlannerItemType.EVENT.name,
                location = "主楼 B1-412", accentColorHex = "#568BC5", reminderAtMillis = null,
                reminderEnabled = false, ringEnabled = false, vibrateEnabled = false, voiceEnabled = false)
        }
        var opened = 0L
        val tomorrow = (1..4).map { event(it.toLong(), "明日事项 $it", today.plusDays(1), 8 + it) }
        compose.setContent {
            TodoAlarmTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                        TodayScheduleBoardCard(today, today.atTime(9, 30), listOf(event(5, "学生科值班", today, 9)),
                            tomorrow, false, onOpenEvent = { opened = it.id }, onGetEventCheckIns = { emptyList() }, onLaunchCheckIn = {})
                    }
                }
            }
        }
        compose.onNodeWithText("学生科值班").assertIsDisplayed()
        compose.onNodeWithText("今日剩余").assertDoesNotExist()
        compose.onNodeWithText("明日事项 4").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(4L, opened) }
        compose.onNodeWithText("学生科值班").performScrollTo()
        val inst = InstrumentationRegistry.getInstrumentation()
        val folder = File(inst.targetContext.getExternalFilesDir(null), "ui-acceptance").apply { mkdirs() }
        inst.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(folder, "daily-board-reference.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
