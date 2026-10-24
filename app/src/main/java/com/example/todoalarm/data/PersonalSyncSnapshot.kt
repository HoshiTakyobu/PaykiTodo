package com.example.todoalarm.data

/** Account snapshots carry private records; collaborative records have a separate authority. */
internal fun BackupSnapshot.personalSyncSnapshot(): BackupSnapshot {
    val privateTasks = tasks.filterNot { it.calendarId.startsWith("shared:") }
    val privateIds = privateTasks.map { it.id }.toSet()
    val privateTemplates = templates.filterNot { it.calendarId.startsWith("shared:") }
    val sharedIds = tasks.filter { it.calendarId.startsWith("shared:") }.map { it.id }.toSet()
    val sharedSeries = templates.filter { it.calendarId.startsWith("shared:") }.map { it.seriesId }.toSet() +
        tasks.filter { it.calendarId.startsWith("shared:") }.mapNotNull { it.recurringSeriesId }
    return copy(
        tasks = privateTasks,
        templates = privateTemplates,
        calendars = calendars.filterNot { it.id.startsWith("shared:") },
        eventCheckIns = eventCheckIns.filter { it.eventId in privateIds },
        todoGroupTags = todoGroupTags.filter { it.todoId in privateIds },
        recurringInstanceSkips = recurringInstanceSkips.filterNot { it.seriesId in sharedSeries },
        reminderChainLogs = reminderChainLogs.filterNot { it.todoId in sharedIds },
        planningNodes = planningNodes.map { it.copy(
            linkedTodoId = it.linkedTodoId?.takeUnless { id -> id in sharedIds },
            linkedEndTodoId = it.linkedEndTodoId?.takeUnless { id -> id in sharedIds }) },
        planningLineMappings = planningLineMappings.map { mapping ->
            if (mapping.todoId in sharedIds || mapping.eventId in sharedIds) mapping.copy(
                todoId = mapping.todoId?.takeUnless { it in sharedIds },
                eventId = mapping.eventId?.takeUnless { it in sharedIds }, status = MappingStatus.ORPHANED)
            else mapping
        },
        settings = settings.copy(hiddenCalendarIds = settings.hiddenCalendarIds.filterNot { it.startsWith("shared:") }.toSet())
    )
}

/** Preserve cache identities and references when a private snapshot uses overlapping local IDs. */
internal fun BackupSnapshot.withSharedCacheFrom(local: BackupSnapshot): BackupSnapshot {
    val personal = personalSyncSnapshot()
    val sharedTasks = local.tasks.filter { it.calendarId.startsWith("shared:") }
    val personalSeries = personal.templates.map { it.seriesId }.toSet() + personal.tasks.mapNotNull { it.recurringSeriesId }
    val cachedSeries = local.templates.filter { it.calendarId.startsWith("shared:") }.map { it.seriesId }.toSet() +
        sharedTasks.mapNotNull { it.recurringSeriesId }
    require(personalSeries.intersect(cachedSeries).isEmpty()) { "循环日程标识与共享日历冲突，已保留本机数据，请检查日历来源" }
    val usedIds = personal.tasks.map { it.id }.toMutableSet()
    var nextId = maxOf(usedIds.maxOrNull() ?: 0, local.tasks.maxOfOrNull { it.id } ?: 0)
    val idMap = sharedTasks.associate { task -> task.id to if (task.id in usedIds) ++nextId else task.id.also { usedIds.add(it) } }
    val sharedTemplates = local.templates.filter { it.calendarId.startsWith("shared:") }
    val sharedSeries = sharedTemplates.map { it.seriesId }.toSet()
    val templateIds = personal.templates.map { it.id }.toMutableSet()
    var nextTemplateId = maxOf(templateIds.maxOrNull() ?: 0, local.templates.maxOfOrNull { it.id } ?: 0)
    val sharedCheckIns = local.eventCheckIns.filter { it.eventId in idMap }
    var nextCheckInId = maxOf(personal.eventCheckIns.maxOfOrNull { it.id } ?: 0, local.eventCheckIns.maxOfOrNull { it.id } ?: 0)
    var nextSkipId = maxOf(personal.recurringInstanceSkips.maxOfOrNull { it.id } ?: 0, local.recurringInstanceSkips.maxOfOrNull { it.id } ?: 0)
    var nextLogId = maxOf(personal.reminderChainLogs.maxOfOrNull { it.id } ?: 0, local.reminderChainLogs.maxOfOrNull { it.id } ?: 0)
    return personal.copy(
        calendars = personal.calendars + local.calendars.filter { it.id.startsWith("shared:") },
        tasks = personal.tasks + sharedTasks.map { it.copy(id = idMap.getValue(it.id), groupId = 0) },
        templates = personal.templates + sharedTemplates.map { template ->
            template.copy(id = if (template.id in templateIds) ++nextTemplateId else template.id.also { templateIds.add(it) }, groupId = 0)
        },
        eventCheckIns = personal.eventCheckIns + sharedCheckIns.map { it.copy(id = ++nextCheckInId, eventId = idMap.getValue(it.eventId)) },
        recurringInstanceSkips = personal.recurringInstanceSkips + local.recurringInstanceSkips.filter { it.seriesId in sharedSeries }.map { it.copy(id = ++nextSkipId) },
        reminderChainLogs = personal.reminderChainLogs + local.reminderChainLogs.filter { it.todoId in idMap }.map { it.copy(id = ++nextLogId, todoId = idMap.getValue(it.todoId)) },
        settings = personal.settings.copy(hiddenCalendarIds = personal.settings.hiddenCalendarIds + local.settings.hiddenCalendarIds.filter { it.startsWith("shared:") })
    )
}
