package com.example.todoalarm.sync

import java.io.IOException

class CalendarServiceException(val status: Int, val currentRevision: Long, message: String) : IOException(message)
