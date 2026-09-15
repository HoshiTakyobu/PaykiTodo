package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickChecklistEditingTest {
    @Test
    fun sourceLineSelectionUsesZeroBasedTextOffsetsAndSupportsCrLf() {
        val text = "标题\r\n- 第一项\r\n-- 第二项"

        assertEquals(QuickChecklistSourceSelection(0, 2), QuickChecklistEditing.sourceLineSelection(text, 1))
        assertEquals(QuickChecklistSourceSelection(4, 9), QuickChecklistEditing.sourceLineSelection(text, 2))
        assertEquals(QuickChecklistSourceSelection(11, 17), QuickChecklistEditing.sourceLineSelection(text, 3))
        assertNull(QuickChecklistEditing.sourceLineSelection(text, 4))
    }
}
