package com.example.todoalarm.data

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime
import java.time.ZoneId

class BoardArrangementTest {
    private fun item(id: Long, event: Boolean, time: Long, allDay: Boolean = false) = TodoItem(
        id=id, itemType=if(event) PlannerItemType.EVENT.name else PlannerItemType.TODO.name,
        title="同标题", notes="", dueAtMillis=time, startAtMillis=if(event) time else null,
        endAtMillis=if(event) time+3_600_000 else null, allDay=allDay,
        reminderAtMillis=null, reminderEnabled=false, ringEnabled=false,vibrateEnabled=false,voiceEnabled=false)
    @Test fun mixedTimesAreIndependentAndStable() {
        val todos=listOf(item(1,false,13_500),item(2,false,15_000),item(3,false,17_500),item(4,false,Long.MAX_VALUE))
        val events=listOf(item(5,true,14_000),item(6,true,15_000),item(7,true,0,true))
        assertEquals(listOf(7L,1L,5L,6L,2L,3L,4L),BoardArrangement.ordered(todos,events).map{it.id})
        assertEquals(15_000L,todos[1].dueAtMillis)
        assertEquals(7,BoardArrangement.ordered(todos+todos[0],events).size)
    }
    @Test fun dailySnapshotKeepsOverdueUndatedChildrenAndTomorrowSeparate() {
        val now = LocalDateTime.of(2026, 11, 3, 12, 0)
        fun millis(value: LocalDateTime) = value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val noon = millis(now)
        val parent = item(1, false, noon - 3_600_000)
        val child = item(2, false, noon + 3_600_000).copy(parentTodoId = 1)
        val overdue = item(3, false, millis(now.minusDays(1)))
        val undated = item(4, false, Long.MAX_VALUE)
        val tomorrow = item(5, false, millis(now.plusDays(1)))
        val event = item(6, true, noon + 1_800_000)
        val allDay = item(7, true, millis(now.toLocalDate().atStartOfDay()), true).copy(endAtMillis = millis(now.toLocalDate().plusDays(1).atStartOfDay()))
        val hidden = parent.copy(id = 8, hiddenFromBoard = true)
        val completed = parent.copy(id = 9, completed = true)
        val canceled = event.copy(id = 10, canceled = true)
        val original = listOf(parent, child, overdue, undated, tomorrow, event, allDay, hidden, completed, canceled)
        val snapshot = DailyBoardSnapshotBuilder.build(original, now = now)
        assertEquals(listOf(7L, 3L, 1L, 6L, 2L, 4L), snapshot.todayArrangement.map { it.id })
        assertEquals(listOf(5L), snapshot.tomorrowArrangement.map { it.id })
        assertEquals(noon + 3_600_000, child.dueAtMillis)
        assertEquals(1L, child.parentTodoId)
    }
    @Test fun midnightEndDoesNotDuplicateAnEventInTomorrow() {
        val now = LocalDateTime.of(2026, 11, 3, 12, 0)
        fun millis(value: LocalDateTime) = value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val event = item(1, true, millis(now.toLocalDate().atTime(23, 0))).copy(endAtMillis = millis(now.toLocalDate().plusDays(1).atStartOfDay()))
        assertTrue(DailyBoardSnapshotBuilder.eventOverlapsDay(event, now.toLocalDate()))
        assertFalse(DailyBoardSnapshotBuilder.eventOverlapsDay(event, now.toLocalDate().plusDays(1)))
        assertTrue(DailyBoardSnapshotBuilder.eventOverlapsDay(event.copy(endAtMillis = event.endAtMillis!! + 1), now.toLocalDate().plusDays(1)))
    }
}
