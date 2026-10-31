package com.example.todoalarm.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PlanningAbsoluteAssignmentTest {
    private fun at(value: String) = LocalDateTime.parse(value)

    private fun weekly(
        start: String,
        endDate: String? = null,
        interval: Int = 1,
        durationHours: Long = 1
    ): TransientPlanningSlot {
        val startAt = at(start)
        return TransientPlanningSlot(startAt, startAt.plusHours(durationHours), RecurrenceConfig(
            true, RecurrenceType.WEEKLY, setOf(startAt.dayOfWeek), endDate?.let(LocalDate::parse), interval
        ))
    }

    private fun assertAssignments(
        slots: List<TransientPlanningSlot>,
        absolutes: List<String>,
        expected: Map<Pair<Int, String>, List<String>>
    ) {
        val actual = assignPlanningAbsoluteReminders(slots, absolutes.map(::at))
        assertEquals(expected.mapKeys { (key, _) -> key.first to at(key.second) }
            .mapValues { (_, values) -> values.map(::at).distinct().sorted() },
            actual.associate { (it.slotIndex to it.occurrenceStartAt) to it.absoluteReminderTimes })
        assertEquals(expected.size, actual.size)
        assertEquals(absolutes.distinct().size, actual.sumOf { it.absoluteReminderTimes.size })
        absolutes.distinct().forEach { absolute ->
            assertEquals("Absolute $absolute must be assigned once", 1,
                actual.count { at(absolute) in it.absoluteReminderTimes })
        }
    }

    @Test fun multipleAbsolutesUseNearestOccurrenceAcrossEntireWeeklyCourse() {
        val slots = listOf(weekly("2030-01-07T08:00", "2030-01-23"), weekly("2030-01-09T14:00", "2030-01-23"))
        assertAssignments(slots, listOf(
            "2030-01-16T15:00", "2030-01-07T08:01", "2030-01-06T07:00", "2030-01-07T08:00", "2030-01-07T08:01"
        ), mapOf(
            (0 to "2030-01-07T08:00") to listOf("2030-01-06T07:00", "2030-01-07T08:00"),
            (1 to "2030-01-09T14:00") to listOf("2030-01-07T08:01"),
            (0 to "2030-01-21T08:00") to listOf("2030-01-16T15:00")
        ))
    }

    @Test fun laterSlotOnSameDayWinsBeforeFollowingWeek() {
        val morning = weekly("2030-01-07T08:00")
        val afternoon = weekly("2030-01-07T14:00")
        assertAssignments(listOf(morning, afternoon), listOf("2030-01-07T08:01", "2030-01-07T14:00"),
            mapOf((1 to "2030-01-07T14:00") to listOf("2030-01-07T08:01", "2030-01-07T14:00")))
    }

    @Test fun equalityBelongsToCurrentOccurrenceAndTiesUseInputOrder() {
        val longSlot = weekly("2030-01-07T08:00", durationHours = 2)
        val shortSlot = weekly("2030-01-07T08:00")
        val expected = mapOf((0 to "2030-01-07T08:00") to listOf("2030-01-07T08:00"))
        assertAssignments(listOf(longSlot, shortSlot), listOf("2030-01-07T08:00"), expected)
        assertAssignments(listOf(shortSlot, longSlot), listOf("2030-01-07T08:00"), expected)
    }

    @Test fun expiredSlotDoesNotHideValidOccurrenceInAnotherSlot() {
        val slots = listOf(weekly("2030-01-07T08:00", "2030-01-14"), weekly("2030-01-09T14:00", "2030-01-23"))
        assertAssignments(slots, listOf("2030-01-15T12:00"),
            mapOf((1 to "2030-01-16T14:00") to listOf("2030-01-15T12:00")))
    }

    @Test fun finiteRangeAcceptsFinalStartAndRejectsOneMinuteAfterIt() {
        val slots = listOf(weekly("2030-01-07T08:00", "2030-01-23"), weekly("2030-01-09T14:00", "2030-01-23"))
        assertAssignments(slots, listOf("2030-01-23T14:00"),
            mapOf((1 to "2030-01-23T14:00") to listOf("2030-01-23T14:00")))
        val error = assertThrows(IllegalArgumentException::class.java) {
            assignPlanningAbsoluteReminders(slots, listOf(at("2030-01-07T08:00"), at("2030-01-23T14:01")))
        }
        assertTrue(error.message.orEmpty(), error.message.orEmpty().contains("绝对提醒"))
        assertTrue(error.message.orEmpty(), error.message.orEmpty().contains("2030-01-23 14:01"))
    }

    @Test fun farFutureAbsoluteFindsMayOwnerBeyondInitialGenerationWindow() {
        val slots = listOf(weekly("2030-01-07T08:00"), weekly("2030-01-09T14:00"))
        assertAssignments(slots, listOf("2030-04-30T12:00"),
            mapOf((1 to "2030-05-01T14:00") to listOf("2030-04-30T12:00")))
    }

    @Test fun everyTwoWeeksKeepsPhaseWhenReminderIsInInactiveWeek() {
        val slots = listOf(weekly("2030-01-07T08:00", interval = 2), weekly("2030-01-09T14:00", interval = 2))
        assertAssignments(slots, listOf("2030-01-10T12:00", "2030-01-21T08:00", "2030-01-23T14:01"), mapOf(
            (0 to "2030-01-21T08:00") to listOf("2030-01-10T12:00", "2030-01-21T08:00"),
            (0 to "2030-02-04T08:00") to listOf("2030-01-23T14:01")
        ))
    }

    @Test fun every10000WeeksFindsNextOccurrenceBeyondOneHundredYears() {
        val slots = listOf(weekly("2030-01-07T08:00", interval = 10000), weekly("2030-01-09T14:00", interval = 10000))
        assertAssignments(slots, listOf("2030-01-10T12:00", "2221-09-03T08:01", "2221-09-05T14:01"), mapOf(
            (0 to "2221-09-03T08:00") to listOf("2030-01-10T12:00"),
            (1 to "2221-09-05T14:00") to listOf("2221-09-03T08:01"),
            (0 to "2413-04-29T08:00") to listOf("2221-09-05T14:01")
        ))
    }

    @Test fun month31ClampsFebruaryWithoutChangingLaterIntendedDay() {
        val start = at("2030-01-31T08:00")
        val slot = TransientPlanningSlot(start, start.plusHours(1), RecurrenceConfig(
            true, RecurrenceType.MONTHLY_DAY, endDate = LocalDate.parse("2030-04-30"), monthlyDay = 31
        ))
        assertAssignments(listOf(slot), listOf("2030-02-28T08:00", "2030-02-28T08:01", "2030-04-01T12:00"), mapOf(
            (0 to "2030-02-28T08:00") to listOf("2030-02-28T08:00"),
            (0 to "2030-03-31T08:00") to listOf("2030-02-28T08:01"),
            (0 to "2030-04-30T08:00") to listOf("2030-04-01T12:00")
        ))
    }

    @Test fun leapDayClampsNonLeapYearAndResumesFebruary29InLeapYear() {
        val start = at("2028-02-29T08:00")
        val slot = TransientPlanningSlot(start, start.plusHours(1), RecurrenceConfig(
            true, RecurrenceType.YEARLY_DATE, endDate = LocalDate.parse("2032-03-01"), monthlyDay = 29
        ))
        assertAssignments(listOf(slot), listOf("2029-02-28T08:00", "2029-03-01T12:00", "2032-02-29T08:00"), mapOf(
            (0 to "2029-02-28T08:00") to listOf("2029-02-28T08:00"),
            (0 to "2030-02-28T08:00") to listOf("2029-03-01T12:00"),
            (0 to "2032-02-29T08:00") to listOf("2032-02-29T08:00")
        ))
    }

    @Test fun skipInvalidDatePolicyDoesNotAssignToClampedMonthOrYear() {
        val monthlyStart = at("2030-01-31T08:00")
        val monthly = TransientPlanningSlot(monthlyStart, monthlyStart.plusHours(1), RecurrenceConfig(
            true, RecurrenceType.MONTHLY_DAY, invalidDatePolicy = "SKIP", monthlyDay = 31
        ))
        assertAssignments(listOf(monthly), listOf("2030-02-01T12:00"),
            mapOf((0 to "2030-03-31T08:00") to listOf("2030-02-01T12:00")))
        val yearlyStart = at("2028-02-29T08:00")
        val yearly = TransientPlanningSlot(yearlyStart, yearlyStart.plusHours(1), RecurrenceConfig(
            true, RecurrenceType.YEARLY_DATE, invalidDatePolicy = "SKIP", monthlyDay = 29
        ))
        assertAssignments(listOf(yearly), listOf("2028-02-29T08:01"),
            mapOf((0 to "2032-02-29T08:00") to listOf("2028-02-29T08:01")))
    }

    @Test fun emptyAbsoluteListReturnsNoAssignments() {
        assertTrue(assignPlanningAbsoluteReminders(listOf(weekly("2030-01-07T08:00")), emptyList()).isEmpty())
    }
}
