package com.example.todoalarm.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DailyBoardSnapshotBuilderTest {
    private val now = LocalDateTime.of(2026, 5, 14, 8, 0)

    @Test
    fun noDdlTodosAreShownAsTodayTodos() {
        val todayWithDdl = todo(
            id = 1,
            title = "今天有 DDL",
            dueAt = LocalDateTime.of(2026, 5, 14, 18, 0)
        )
        val noDdl = todo(
            id = 2,
            title = "随手想办的事",
            dueAt = null
        )
        val future = todo(
            id = 3,
            title = "明天再做",
            dueAt = LocalDateTime.of(2026, 5, 15, 12, 0)
        )

        val snapshot = DailyBoardSnapshotBuilder.build(
            items = listOf(todayWithDdl, noDdl, future),
            now = now
        )

        assertEquals(listOf("今天有 DDL", "随手想办的事"), snapshot.todoItems.map { it.title })
    }

    @Test
    fun noDdlTodosStayInTodayTodosOnLaterDates() {
        val noDdl = todo(
            id = 2,
            title = "随手想办的事",
            dueAt = null
        )

        val firstDay = DailyBoardSnapshotBuilder.build(
            items = listOf(noDdl),
            now = LocalDateTime.of(2026, 5, 14, 8, 0)
        )
        val nextDay = DailyBoardSnapshotBuilder.build(
            items = listOf(noDdl),
            now = LocalDateTime.of(2026, 5, 15, 8, 0)
        )

        assertEquals(listOf("随手想办的事"), firstDay.todoItems.map { it.title })
        assertEquals(listOf("随手想办的事"), nextDay.todoItems.map { it.title })
    }

    @Test
    fun countdownItemsOnlyIncludeActiveFutureTargets() {
        val futureExam = todo(
            id = 10,
            title = "期末考试",
            dueAt = LocalDateTime.of(2026, 5, 20, 9, 0),
            countdownEnabled = true
        )
        val todayEvent = event(
            id = 11,
            title = "答辩",
            startAt = LocalDateTime.of(2026, 5, 14, 15, 0),
            countdownEnabled = true
        )
        val expired = todo(
            id = 12,
            title = "已经过去的目标",
            dueAt = LocalDateTime.of(2026, 5, 13, 22, 0),
            countdownEnabled = true
        )
        val noDdl = todo(
            id = 13,
            title = "无 DDL 不进入倒数日",
            dueAt = null,
            countdownEnabled = true
        )

        val snapshot = DailyBoardSnapshotBuilder.build(
            items = listOf(futureExam, todayEvent, expired, noDdl),
            now = now
        )

        assertEquals(listOf("答辩", "期末考试"), snapshot.countdownItems.map { it.title })
        assertEquals(0L, DailyBoardSnapshotBuilder.countdownDays(todayEvent, now.toLocalDate()))
        assertEquals(6L, DailyBoardSnapshotBuilder.countdownDays(futureExam, now.toLocalDate()))
    }

    @Test
    fun countdownItemsExcludeTargetsEarlierToday() {
        val pastEvent = event(
            id = 20,
            title = "上午考试",
            startAt = LocalDateTime.of(2026, 5, 14, 7, 0),
            countdownEnabled = true
        )
        val futureEvent = event(
            id = 21,
            title = "下午考试",
            startAt = LocalDateTime.of(2026, 5, 14, 15, 0),
            countdownEnabled = true
        )

        val snapshot = DailyBoardSnapshotBuilder.build(
            items = listOf(pastEvent, futureEvent),
            now = now
        )

        assertEquals(listOf("下午考试"), snapshot.countdownItems.map { it.title })
    }

    @Test
    fun countdownDisplayUsesDaysHoursAndMinutesWithoutSeconds() {
        val target = LocalDateTime.of(2026, 5, 20, 10, 36)
        val display = DailyBoardSnapshotBuilder.countdownRemainingDisplay(
            target = target,
            now = now
        )
        val hourDisplay = DailyBoardSnapshotBuilder.countdownRemainingDisplay(
            target = LocalDateTime.of(2026, 5, 14, 23, 23),
            now = now
        )
        val minuteDisplay = DailyBoardSnapshotBuilder.countdownRemainingDisplay(
            target = LocalDateTime.of(2026, 5, 14, 8, 15),
            now = now
        )

        assertEquals("6d", display.primary)
        assertEquals("2h 36m", display.secondary)
        assertEquals("15h", hourDisplay.primary)
        assertEquals("23m", hourDisplay.secondary)
        assertEquals("15m", minuteDisplay.primary)
        assertEquals("", minuteDisplay.secondary)
    }

    @Test
    fun visibleTodayEventsExcludeTimedEventsAfterTheyEnd() {
        val ended = event(
            id = 30,
            title = "上午已结束",
            startAt = LocalDateTime.of(2026, 5, 14, 7, 0)
        )
        val running = event(
            id = 31,
            title = "正在进行",
            startAt = LocalDateTime.of(2026, 5, 14, 7, 30)
        )
        val upcoming = event(
            id = 32,
            title = "下午会议",
            startAt = LocalDateTime.of(2026, 5, 14, 15, 0)
        )

        val snapshot = DailyBoardSnapshotBuilder.build(
            items = listOf(ended, running, upcoming),
            now = now
        )

        assertEquals(listOf("上午已结束", "正在进行", "下午会议"), snapshot.allTodayEvents.map { it.title })
        assertEquals(listOf("正在进行", "下午会议"), snapshot.visibleTodayEvents.map { it.title })
    }

    private fun todo(
        id: Long,
        title: String,
        dueAt: LocalDateTime?,
        countdownEnabled: Boolean = false
    ): TodoItem {
        return TodoItem(
            id = id,
            title = title,
            countdownEnabled = countdownEnabled,
            dueAtMillis = dueAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                ?: NO_DUE_DATE_MILLIS,
            reminderAtMillis = null,
            reminderEnabled = false,
            ringEnabled = true,
            vibrateEnabled = true
        )
    }

    private fun event(
        id: Long,
        title: String,
        startAt: LocalDateTime,
        countdownEnabled: Boolean = false
    ): TodoItem {
        return TodoItem(
            id = id,
            itemType = PlannerItemType.EVENT.name,
            title = title,
            countdownEnabled = countdownEnabled,
            dueAtMillis = startAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            startAtMillis = startAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            endAtMillis = startAt.plusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            reminderAtMillis = null,
            reminderEnabled = false,
            ringEnabled = true,
            vibrateEnabled = true
        )
    }
}
