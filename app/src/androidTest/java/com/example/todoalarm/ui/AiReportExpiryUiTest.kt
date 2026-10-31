package com.example.todoalarm.ui

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.todoalarm.data.*
import com.example.todoalarm.ui.theme.TodoAlarmTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class AiReportExpiryUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun openDetailDisappearsAtExpiryWithoutLeavingPage() {
        val text = "独立日报正文" + "记录内容".repeat(30) + "仅详情可见"
        val report = AiReport(42, AiReportType.DAILY,
            System.currentTimeMillis() - AiReportPolicy.DAILY_LIFETIME_MILLIS + 8_000, 0, 1, text)
        compose.setContent { TodoAlarmTheme { Surface {
            AiReportPanel(
                observeReports = { _, _, _, _, _ -> flowOf(listOf(report)).withDailyExpiry() },
                onGetReport = { report.takeUnless { AiReportPolicy.isExpired(it) } },
                targetReportId = 42, targetReportSerial = 1, onDeleteReport = { null }
            )
        } } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text).assertIsDisplayed()
        compose.waitUntil(12_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }
        compose.onAllNodesWithText("独立日报正文", substring = true).assertCountEquals(0)
    }
}
