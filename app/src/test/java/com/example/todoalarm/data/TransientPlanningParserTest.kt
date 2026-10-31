package com.example.todoalarm.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TransientPlanningParserTest {
    private val now = LocalDateTime.of(2026, 11, 1, 12, 0)
    private fun parse(text: String) = TransientPlanningParser.parse(text, now)
    private fun one(text: String): TransientPlanningCandidate {
        val result = parse(text)
        assertEquals(result.diagnostics.toString(), emptyList<TransientPlanningDiagnostic>(), result.diagnostics)
        return result.candidates.single()
    }
    @Test fun titleOnlyOptionalFieldsAndTodayDefaults() {
        val simple = one("- 整理书桌;")
        assertNull(simple.dueAt); assertNull(simple.validate())
        assertEquals(listOf(5, 0), simple.reminderOffsetsMinutes)
        assertEquals(now.toLocalDate().atTime(16, 30), one("交作业,16:30;").dueAt)
        assertEquals("超市", one("买水果,@超市;").location)
    }
    @Test fun explicitEmptyAndMixedRemindersRemainDistinct() {
        assertEquals(PlanningReminderMode.DISABLED, one("阅读,20:00,();").reminderMode)
        val mixed = one("买水果,16:30,(5,15,120,11-02+12:00,2027-07-03 15:00,09:00);")
        assertEquals(listOf(120, 15, 5), mixed.reminderOffsetsMinutes)
        assertEquals(listOf(now.toLocalDate().atTime(9, 0), LocalDateTime.of(2026,11,2,12,0), LocalDateTime.of(2027,7,3,15,0)), mixed.absoluteReminderTimes)
        assertTrue(parse("无DDL,(5);").hasErrors)
        assertNull(one("仅绝对提醒,(15:00);").validate())
    }
    @Test fun datesAreStrictAndPastDatesDoNotAdvanceToNextYear() {
        assertEquals(LocalDateTime.of(2026,1,1,2,25), one("过去任务,01-01+02:25;").dueAt)
        assertFalse(parse("历史任务,2020-01-01 09:00,(5,0);").hasErrors)
        assertTrue(parse("错误日期,2026-02-29 09:00;").hasErrors)
        assertTrue(parse("错误时钟,24:00;").hasErrors)
    }
    @Test fun eventRangesRequireExplicitCrossMidnightEndDate() {
        val normal = one("讨论,@会议室,14:00>15:00;")
        assertEquals(PlanningParsedType.EVENT, normal.type)
        assertTrue(parse("跨日,23:00>01:00;").hasErrors)
        val cross = one("值班,2026-11-01 23:00>2026-11-02 01:00;")
        assertEquals(LocalDateTime.of(2026,11,2,1,0),cross.slots.single().endAt)
        assertTrue(parse("旧连接,16:30-18:00;").hasErrors)
    }
    @Test fun notesAfterSemicolonConsumeAllRemainingPunctuation() {
        val result = parse("- 阅读,16:30; #先读第二章; 别跳过, \"重点\" (附录)\n- 写作;")
        assertFalse(result.diagnostics.toString(), result.hasErrors)
        assertEquals(2,result.candidates.size)
        assertEquals("先读第二章; 别跳过, \"重点\" (附录)", result.candidates.first().notes)
        assertTrue(parse("阅读,16:30 #先读第二章;").hasErrors)
    }
    @Test fun quotedPunctuationAndInteriorQuotesArePreserved() {
        val item = one("- \"阅读\"重点\",并整理\", @\"教学楼,201\", 14:00>15:00;")
        assertEquals("阅读\"重点\",并整理", item.title)
        assertEquals("教学楼,201", item.location)
        assertEquals("讨论;整理", one("\"讨论;整理\",16:30,();").title)
        assertTrue(parse("\"缺少引号,16:30;").hasErrors)
    }
    @Test fun hierarchyUsesIdsAndSameLineItemsStayPeers() {
        val result = parse("- 准备出行\n  - 买车票,18:00; 收拾行李;\n- 阅读;")
        assertFalse(result.diagnostics.toString(),result.hasErrors)
        val items=result.candidates
        assertEquals(items[0].id,items[1].parentId)
        assertEquals(items[0].id,items[2].parentId)
        assertNull(items[3].parentId)
        assertEquals(items.map { it.id }.distinct().size,items.size)
        assertTrue(parse("  - 缺少父事项;").hasErrors)
    }
    @Test fun enterUnderstandsTopLevelSeparators() {
        assertEquals("  - ",TransientPlanningParser.nextLinePrefix("- 父事项"))
        assertEquals("  - ",TransientPlanningParser.nextLinePrefix("  - 子事项; #说明;更多"))
        assertEquals("  - ",TransientPlanningParser.nextLinePrefix("- \"内部;标点\""))
    }
    @Test fun allConfirmedRecurrenceFormsAndOptionalRange() {
        val forms = listOf("每天[08:00>08:30]","每2天[08:00>08:30]","每周[周一 08:00>09:40,周三 14:00>15:40]","每2周[周一 08:00>09:40,周三 14:00>15:40]","每月[15日 09:00>10:00]","每年[11-01 09:00>10:00]")
        forms.forEach { expression ->
            val candidate=one("课程,$expression;")
            assertTrue(candidate.slots.all { it.recurrence.isRecurring && it.recurrence.endDate == null })
        }
        val finite=one("数学,每周[周一 08:00>09:40,周三 14:00>15:40],自2026-11-01至2027-01-15;")
        assertTrue(finite.slots.all { it.recurrence.endDate==LocalDate.of(2027,1,15) })
        assertTrue(parse("倒置,每天[08:00>09:00],自2027-01-15至2026-11-01;").hasErrors)
        assertTrue(parse("错误循环,每0周[周一 08:00>09:00];").hasErrors)
    }
    @Test fun weeklySlotsShareIntervalPhaseAcrossStartMidWeek() {
        val candidate=one("课程,每2周[周一 08:00>09:00,周三 14:00>15:00],自2026-11-04至2026-12-01;")
        val monday=candidate.slots[0];val wednesday=candidate.slots[1]
        val mondays=calendarRecurrenceDates(monday.startAt.toLocalDate(),monday.recurrence)
        val wednesdays=calendarRecurrenceDates(wednesday.startAt.toLocalDate(),wednesday.recurrence)
        assertEquals(listOf(LocalDate.of(2026,11,16),LocalDate.of(2026,11,30)),mondays)
        assertEquals(listOf(LocalDate.of(2026,11,4),LocalDate.of(2026,11,18)),wednesdays)
    }
    @Test fun monthEndAndLeapDayKeepIntendedDayAfterClamping() {
        val monthly=one("月末,每月[31日 09:00>10:00];").slots.single()
        assertEquals(31,monthly.recurrence.monthlyDay)
        assertEquals(listOf(LocalDate.of(2026,11,30),LocalDate.of(2026,12,31),LocalDate.of(2027,1,31)),
            calendarRecurrenceDates(monthly.startAt.toLocalDate(),monthly.recurrence,rangeEnd=LocalDate.of(2027,1,31)))
        val leap=one("闰日,每年[02-29 09:00>10:00];").slots.single()
        assertEquals(listOf(LocalDate.of(2027,2,28),LocalDate.of(2028,2,29)),
            calendarRecurrenceDates(leap.startAt.toLocalDate(),leap.recurrence,rangeEnd=LocalDate.of(2028,3,1)))
    }
    @Test fun invalidInputNeverBecomesAValidSilentlyTruncatedTask() {
        listOf("标题,,16:30;","标题,(5,,0);","标题,(-1);","标题,(999999999999);","标题,@超市,@公园;","标题,16:00,17:00;","标题,每周[周八 09:00>10:00];").forEach {
            val result=parse(it);assertTrue(it,result.hasErrors);assertEquals(1,result.diagnostics.single().lineNumber)
        }
    }
}
