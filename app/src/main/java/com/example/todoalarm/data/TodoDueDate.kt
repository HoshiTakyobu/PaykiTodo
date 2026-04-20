package com.example.todoalarm.data

import java.time.LocalDate

val NO_DUE_DATE_MILLIS: Long = Long.MAX_VALUE
val NO_DUE_DATE_FALLBACK_DATE: LocalDate = LocalDate.of(9999, 12, 31)

fun hasDueDate(epochMillis: Long): Boolean = epochMillis != NO_DUE_DATE_MILLIS
