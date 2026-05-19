package com.example.todoalarm.data

data class EventCheckInCompletionSummary(
    val eventId: Long,
    val title: String,
    val plannedMinutes: Int,
    val investedMinutes: Int,
    val checkInCount: Int,
    val investmentRatePercent: Int?,
    val autoCheckedOut: Boolean
)

data class CompletedItemResult(
    val item: TodoItem,
    val affectedItems: List<TodoItem> = listOf(item),
    val eventCheckInSummary: EventCheckInCompletionSummary? = null
)
