package com.example.todoalarm.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import com.example.todoalarm.update.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppUpdateUiTest {
    @get:Rule val compose = createComposeRule()
    private val release = AppRelease("1.17.12", GithubRelease.PAGE, "本次说明", "https://example.invalid/fixture.apk", 50, null)
    @Test fun debugExplainsSignatureBoundaryAndKeepsReleasePageAvailable() {
        var page = 0
        compose.setContent { TodoAlarmTheme { Surface { AppUpdateContent(UpdateStatus(release), true, {}, { page++ }, {}, {}, {}) } } }
        compose.onNodeWithText("下载安装包").assertIsNotEnabled()
        compose.onNodeWithText("查看发布页面").performClick()
        compose.runOnIdle { assertEquals(1, page) }
    }
    @Test fun recoveredDownloadCanCancelThenDownloadAndInstall() {
        var state by mutableStateOf(UpdateStatus(null, downloadId = 10, progress = 38)); var installs = 0
        compose.setContent { TodoAlarmTheme { Surface { AppUpdateContent(state, false, {}, {}, { installs++ }, { state = state.copy(release = release, downloadId = -1) }, { state = state.copy(downloadId = 11, ready = true) }) } } }
        compose.onNodeWithText("取消下载（38%）").performClick()
        compose.onNodeWithText("下载安装包").performClick()
        compose.onNodeWithText("安装更新").performClick()
        compose.runOnIdle { assertEquals(1, installs) }
    }
    @Test fun busyCheckDoesNotRepeatAndMissingAssetDisablesDownload() {
        compose.setContent { TodoAlarmTheme { Surface { AppUpdateContent(UpdateStatus(release.copy(apk = null), busy = true), false, {}, {}, {}, {}, {}) } } }
        compose.onNodeWithText("检查更新").assertIsNotEnabled()
        compose.onNodeWithText("下载安装包").assertIsNotEnabled()
    }
}
