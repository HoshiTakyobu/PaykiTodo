package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlanningAnnouncementParserTest {
    @Test
    fun announcementHintHelperMatchesSupportedEntryForms() {
        assertTrue(PlanningAnnouncementParser.mightContainAnnouncement("#公告 5.16 重要提醒"))
        assertTrue(PlanningAnnouncementParser.mightContainAnnouncement("> [!announcement] long running notice"))
        assertTrue(PlanningAnnouncementParser.mightContainAnnouncement("- [ ] 公告：长期提醒"))
        assertFalse(PlanningAnnouncementParser.mightContainAnnouncement("# 今日计划\n- [ ] 复习数据库"))
    }

    @Test
    fun planningNoteDefaultsAnnouncementHintFromContent() {
        val announcementNote = PlanningNote(
            title = "公告",
            contentMarkdown = "#公告 5.16 重要提醒"
        )
        val ordinaryNote = PlanningNote(
            title = "普通规划",
            contentMarkdown = "# 今日计划\n- [ ] 复习数据库"
        )

        assertTrue(announcementNote.hasAnnouncementHint)
        assertFalse(ordinaryNote.hasAnnouncementHint)
    }

    @Test
    fun parsesMultipleActiveAnnouncementsFromPlanningNotes() {
        val note = PlanningNote(
            id = 7,
            title = "公告文档",
            contentMarkdown = """
                # 公告 5.16-7.1 期间禁止游玩舞萌DX游戏
                > [!公告] 2026-05-16 2026-05-20 只在本周显示
                普通正文不应被识别
            """.trimIndent()
        )

        val announcements = PlanningAnnouncementParser.activeAnnouncements(
            notes = listOf(note),
            today = LocalDate.of(2026, 5, 16)
        )

        assertEquals(2, announcements.size)
        assertEquals("期间禁止游玩舞萌DX游戏", announcements[0].text)
        assertEquals(LocalDate.of(2026, 5, 16), announcements[0].startDate)
        assertEquals(LocalDate.of(2026, 7, 1), announcements[0].endDate)
        assertEquals("只在本周显示", announcements[1].text)
    }

    @Test
    fun filtersExpiredAndArchivedAnnouncements() {
        val active = PlanningNote(
            id = 1,
            title = "active",
            contentMarkdown = "#公告 5.16 今天显示"
        )
        val expired = PlanningNote(
            id = 2,
            title = "expired",
            contentMarkdown = "#公告 5.10 过期公告"
        )
        val archived = PlanningNote(
            id = 3,
            title = "archived",
            contentMarkdown = "#公告 归档公告",
            archived = true
        )

        val announcements = PlanningAnnouncementParser.activeAnnouncements(
            notes = listOf(active, expired, archived),
            today = LocalDate.of(2026, 5, 16)
        )

        assertEquals(listOf("今天显示"), announcements.map { it.text })
    }

    @Test
    fun parsesAnnouncementInsideCheckboxAndQuoteLines() {
        val note = PlanningNote(
            id = 9,
            title = "mixed",
            contentMarkdown = """
                - [ ] #公告 5.16 checkbox公告
                > #公告 5.16 引用公告
                今日提醒：#公告 5.16 行内公告
            """.trimIndent()
        )

        val announcements = PlanningAnnouncementParser.activeAnnouncements(
            notes = listOf(note),
            today = LocalDate.of(2026, 5, 16)
        )

        assertEquals(
            listOf("checkbox公告", "引用公告", "行内公告"),
            announcements.map { it.text }
        )
    }

    @Test
    fun stripsImportedAndTrailingHashtagsFromAnnouncementText() {
        val note = PlanningNote(
            id = 10,
            title = "tags",
            contentMarkdown = """
                #公告 5.16 干净公告 #imported
                #公告 5.16 分组公告 #group 自律
                公告: 长期公告 #tag
            """.trimIndent()
        )

        val announcements = PlanningAnnouncementParser.activeAnnouncements(
            notes = listOf(note),
            today = LocalDate.of(2026, 5, 16)
        )

        assertEquals(
            listOf("干净公告", "分组公告", "长期公告"),
            announcements.map { it.text }
        )
    }

    @Test
    fun dateRangedAnnouncementsSortBeforeLongRunningOnesByRecentStart() {
        val note = PlanningNote(
            id = 11,
            title = "sort",
            contentMarkdown = """
                #公告 5.16-6.30 A
                #公告 6.1-6.30 B
                #公告 长期 C
            """.trimIndent()
        )

        val announcements = PlanningAnnouncementParser.activeAnnouncements(
            notes = listOf(note),
            today = LocalDate.of(2026, 6, 1)
        )

        assertEquals(listOf("B", "A", "长期 C"), announcements.map { it.text })
    }
}
