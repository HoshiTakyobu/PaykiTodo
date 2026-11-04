package com.example.todoalarm.ui

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.*
import org.junit.Test

class LayoutPreviewTest {
    @Test fun markdownPreviewKeepsFormattingWithoutRawDelimiters() {
        val source = "### 本期回顾\n\n完成 **两项任务**，记录 `结果`，详见 [笔记](https://example.invalid)。\n\n- 下一步计划\n"
        val result = reportPreviewText(source)
        assertTrue(result.text.contains("本期回顾"))
        assertTrue(result.text.contains("• 下一步计划"))
        assertFalse(result.text.contains("###"))
        assertFalse(result.text.contains("**"))
        assertFalse(result.text.contains("https://"))
        assertTrue(result.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(result.spanStyles.any { it.item.fontWeight == FontWeight.SemiBold })
        assertTrue(source.contains("**两项任务**"))
    }
    @Test fun previewDoesNotExposeHtmlOrGrowWithoutBound() {
        assertFalse(reportPreviewText("<script>alert('hidden')</script>").text.contains("alert"))
        assertTrue(reportPreviewText("长报告".repeat(10_000)).length <= 900)
        assertEquals("（空报告）", reportPreviewText("").text)
    }
    @Test fun paletteHasTwelveExactRgbValuesInTwoRows() {
        assertEquals(listOf("#C24B8F", "#F05B55", "#F89F45", "#FBCB45", "#51BA43", "#17CFB5", "#4C88FF", "#50C3F0", "#5F68E9", "#8C56EC", "#D661D6", "#757575"), PaykiColorPresets.Rainbow12)
        assertEquals(2, PaykiColorPresets.Rainbow12.chunked(6).size)
        assertEquals(12, PaykiColorPresets.Rainbow12.distinct().size)
    }
}
