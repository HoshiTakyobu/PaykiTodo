package com.example.todoalarm.data

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class UxDataPreservationTest {
    @Test fun archiveRoundTripPreservesDesktopStyleCustomColorsAndReportSource() {
        val raw = "# 旧报告\n<script>原文保留</script>\n| 时间 | 安排 |\n| --- | --- |\n| 15:00 | 提交记录 |"
        val task = TodoItem(id = 1, title = "原时间任务", notes = "", dueAtMillis = 1_920_000_000_000L,
            accentColorHex = "#DF58A5", reminderAtMillis = 1_919_999_700_000L, reminderEnabled = true,
            ringEnabled = false, vibrateEnabled = false, voiceEnabled = false)
        for (style in listOf("CLASSIC", "MODERN")) {
            val source = BackupSnapshot(0, emptyList(), emptyList(), listOf(task),
                aiReports = listOf(AiReport(42, AiReportType.WEEKLY, 1, 0, 1, raw)),
                settings = AppSettings(desktopCalendarEventStyle = style, desktopFontFamily = "方正小标宋简体", desktopFontSize = 22))
            val decoded = BackupArchiveCodec.decode(BackupArchiveCodec.encode(source))
            assertEquals(style, decoded.settings.desktopCalendarEventStyle)
            assertEquals("方正小标宋简体", decoded.settings.desktopFontFamily)
            assertEquals(22, decoded.settings.desktopFontSize)
            assertEquals(task.accentColorHex, decoded.tasks.single().accentColorHex)
            assertEquals(task.dueAtMillis, decoded.tasks.single().dueAtMillis)
            assertEquals(task.reminderAtMillis, decoded.tasks.single().reminderAtMillis)
            assertEquals(raw, decoded.aiReports.single().content)
            val roundTrip = backupSnapshotFromJson(decoded.toJson())
            assertEquals(style, roundTrip.settings.desktopCalendarEventStyle)
            assertEquals(raw, roundTrip.aiReports.single().content)
            val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "ux-reader").apply { mkdirs() }
            File(folder, "android-roundtrip-$style.json").writeText(roundTrip.toJson().toString())
        }
    }
}
