package com.example.todoalarm.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.todoalarm.data.PlanningAiProvider
import com.example.todoalarm.data.PlanningNote
import com.example.todoalarm.data.ThemeMode
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

class WorkspaceControlsTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val name = TestName()

    @After fun screenshot() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val folder = File(inst.targetContext.getExternalFilesDir(null), "ui-acceptance").apply { mkdirs() }
        inst.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(folder, "${name.methodName}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun providerTitleIsCompleteWithoutGeneratedNumber() {
        val title = "测试AI服务源的完整名称，包含用途和线路名称的较长标题"
        compose.setContent {
            TodoAlarmTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.padding(20.dp)) {
                        PlanningAiProviderRow(
                            index = 0, total = 1,
                            provider = PlanningAiProvider(name = title, baseUrl = "https://api.example.com/v1", apiKey = "test-only-key", model = "qwen-test"),
                            onToggle = {}, onMoveUp = {}, onMoveDown = {}, onEdit = {}, onDelete = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithText("1. $title").assertDoesNotExist()
    }

    @Test fun drawerOpensAccountAndOmitsIndependentHistory() {
        var opened = false
        compose.setContent {
            TodoAlarmTheme {
                DashboardDrawer(
                    current = DashboardSection.ACTIVE, remoteSyncUsername = "test_user",
                    remoteSyncDisplayName = "测试昵称", remoteSyncUserId = 1234, onOpenAccount = { opened = true },
                    selectedThemeMode = ThemeMode.LIGHT, onSelectSection = {}, onThemeModeChange = {}
                )
            }
        }
        compose.onNodeWithText("测试昵称").performClick()
        compose.runOnIdle { assertEquals(true, opened) }
        compose.onNodeWithText("ID: 1234").assertIsDisplayed()
        compose.onNodeWithText("历史记录").assertDoesNotExist()
    }

    @Test fun documentActionsTargetTheChosenRow() {
        val first = PlanningNote(1, "文档一", "", updatedAtMillis = 2)
        val second = PlanningNote(2, "文档二", "", updatedAtMillis = 1)
        var renamed = 0L
        var archived = 0L
        var created = false
        compose.setContent {
            TodoAlarmTheme {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    PlanningDocumentSheet(
                        notes = listOf(first, second), activeNoteId = 1, onSelect = {}, onDelete = {},
                        onCreate = { created = true }, onRename = { renamed = it.id }, onArchive = { archived = it.id },
                        canImportImage = false, onImportImage = {}
                    )
                }
            }
        }
        compose.onNodeWithText("创建文档").performClick()
        compose.runOnIdle { assertEquals(true, created) }
        compose.onNodeWithContentDescription("文档操作：文档二").performClick()
        compose.onNodeWithText("重命名").performClick()
        compose.runOnIdle { assertEquals(2L, renamed) }
        compose.onNodeWithText("文档二").performTouchInput { swipeRight() }
        compose.runOnIdle { assertEquals(2L, archived) }
    }
}
