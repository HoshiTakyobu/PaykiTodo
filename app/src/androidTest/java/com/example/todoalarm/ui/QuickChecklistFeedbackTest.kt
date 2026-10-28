package com.example.todoalarm.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QuickChecklistFeedbackTest {
    @get:Rule val compose = createComposeRule()

    @Test fun checklistIconOpensEditorAndTimePreviewMatchesSavedSource() {
        var opened by mutableStateOf(false)
        var saved = ""
        var hideKeyboard: () -> Unit = {}
        compose.setContent { TodoAlarmTheme {
            val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            SideEffect { hideKeyboard = { keyboard?.hide(); Unit } }
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(24.dp)) {
                    Text("待办", style = MaterialTheme.typography.headlineMedium)
                    QuickChecklistActionButton { opened = true }
                }
                if (opened) QuickChecklistEditorDialog(onDismiss = { opened = false },
                    onSave = { saved = it; null }, onSaved = { opened = false })
            }
        } }
        compose.onNodeWithContentDescription("快速清单").assertIsDisplayed()
        capture("android-checklist-icon.png")
        compose.onNodeWithContentDescription("快速清单").performClick()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("- 做作业, #16:00")
        compose.runOnIdle { hideKeyboard() }
        compose.onNodeWithText("做作业 · 今天 16:00 提醒").performScrollTo().assertIsDisplayed()
        capture("android-checklist-time.png")
        compose.onNodeWithText("确定").performClick()
        compose.waitUntil(3_000) { !opened }
        assertEquals("- 做作业, #16:00", saved)
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val inst = InstrumentationRegistry.getInstrumentation()
        val folder = File(inst.targetContext.filesDir, "feedback-ui").apply { mkdirs() }
        val bitmap = requireNotNull(inst.uiAutomation.takeScreenshot())
        File(folder, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
