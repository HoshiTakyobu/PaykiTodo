package com.example.todoalarm.ui

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoalarm.data.CalendarEventDraft
import com.example.todoalarm.data.CalendarEventTimeSlot
import com.example.todoalarm.data.LunarCalendar
import com.example.todoalarm.data.RecurrenceConfig
import com.example.todoalarm.data.RecurrenceScope
import com.example.todoalarm.data.ReminderDeliveryMode
import com.example.todoalarm.data.RecurrencePreviewResult
import com.example.todoalarm.data.RecurrenceType
import com.example.todoalarm.data.TaskGroup
import com.example.todoalarm.data.TodoItem
import com.example.todoalarm.data.buildWeeklyMultiSlotEventDrafts
import com.example.todoalarm.data.previewCalendarRecurrence
import com.example.todoalarm.data.storageStringToWeekdays
import com.example.todoalarm.data.toEpochMillis
import com.example.todoalarm.data.calendarZone
import com.example.todoalarm.data.calendarStartDateTime
import com.example.todoalarm.data.calendarEndDateTime
import com.example.todoalarm.data.decodeReminderOffsets
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class CourseTimeSlot(
    val weekday: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime
)

private data class CourseTimeSlotTarget(
    val index: Int,
    val editingStart: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CalendarEventEditorDialog(
    initialEvent: TodoItem?,
    initialDraft: CalendarEventDraft? = null,
    groups: List<TaskGroup> = emptyList(),
    defaultRingEnabled: Boolean,
    defaultVibrateEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (CalendarEventDraft) -> Unit,
    onConfirmMultiple: ((List<CalendarEventDraft>) -> Unit)? = null,
    onConfirmMultiSlotBundleShared: ((CalendarEventDraft) -> Unit)? = null,
    editingScope: RecurrenceScope = RecurrenceScope.CURRENT,
    onChooseRecurrenceScope: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val workspace = LocalWorkspacePreferences.current
    val now = remember { LocalDateTime.now(calendarZone(workspace.settings.calendarDisplayTimeZoneId)).withSecond(0).withNano(0) }
    val seedDraft = remember(initialEvent?.id, initialDraft) { if (initialEvent == null) initialDraft else null }
    val singleInstanceContext = initialEvent?.isRecurring == true && editingScope == RecurrenceScope.CURRENT
    val defaultTimedClock = now.withMinute(0).plusMinutes(if (now.minute < 30) 30 else 60).toLocalTime()
    val writableCalendars = workspace.calendars.filter { !it.id.startsWith("shared:") ||
        (it.shareMode == "SHARED" && it.sharedRole.uppercase() in setOf("OWNER", "EDITOR")) }
    val sortedGroups = remember(groups) { groups.sortedWith(compareBy<TaskGroup> { it.sortOrder }.thenBy { it.id }) }
    val defaultGroupId = remember(sortedGroups) {
        sortedGroups.firstOrNull { it.name == "例行" }?.id ?: sortedGroups.firstOrNull()?.id ?: 0L
    }
    val validGroupIds = remember(sortedGroups) { sortedGroups.map { it.id }.toSet() }
    val initialSelectedGroupId = remember(initialEvent?.id, seedDraft, validGroupIds, defaultGroupId) {
        val preferred = initialEvent?.groupId?.takeIf { it > 0 } ?: seedDraft?.groupId?.takeIf { it > 0 } ?: defaultGroupId
        when {
            preferred in validGroupIds -> preferred
            defaultGroupId in validGroupIds -> defaultGroupId
            else -> preferred.takeIf { it > 0 } ?: 0L
        }
    }
    var title by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.title ?: seedDraft?.title.orEmpty()) }
    var location by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.location ?: seedDraft?.location.orEmpty()) }
    var notes by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.notes ?: seedDraft?.notes.orEmpty()) }
    var selectedGroupId by remember(initialEvent?.id, seedDraft, initialSelectedGroupId) {
        mutableStateOf(initialSelectedGroupId)
    }
    var allDay by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.allDay ?: (seedDraft?.allDay == true)) }
    var eventTimeZoneId by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(initialEvent?.eventTimeZoneId ?: seedDraft?.eventTimeZoneId ?: workspace.settings.calendarDisplayTimeZoneId)
    }
    var startAt by remember(initialEvent?.id) {
        mutableStateOf(initialEvent?.calendarStartDateTime() ?: seedDraft?.startAt ?: now.withMinute(0).plusMinutes(if (now.minute < 30) 30 else 60))
    }
    var endAt by remember(initialEvent?.id) {
        mutableStateOf(initialEvent?.calendarEndDateTime()?.let { if (initialEvent.allDay) it.minusDays(1) else it }
            ?: seedDraft?.endAt ?: startAt.plusMinutes(workspace.settings.calendarDefaultDurationMinutes.toLong()))
    }
    var rememberedTimedStart by remember { mutableStateOf(if (allDay) LocalDateTime.of(startAt.toLocalDate(), defaultTimedClock) else startAt) }
    var rememberedTimedEnd by remember { mutableStateOf(if (allDay) LocalDateTime.of(endAt.toLocalDate(), defaultTimedClock).plusMinutes(workspace.settings.calendarDefaultDurationMinutes.toLong()) else endAt) }
    var eventBusyStatus by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.eventBusyStatus ?: seedDraft?.eventBusyStatus ?: if (allDay) "FREE" else "BUSY") }
    var eventVisibility by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.eventVisibility ?: seedDraft?.eventVisibility ?: "DEFAULT") }
    var timedBusyStatus by remember { mutableStateOf(if (allDay) "BUSY" else eventBusyStatus) }
    var allDayBusyStatus by remember { mutableStateOf(if (allDay) eventBusyStatus else "FREE") }
    var accentColorHex by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(initialEvent?.accentColorHex ?: seedDraft?.accentColorHex ?: PaykiColorPresets.DefaultEvent)
    }
    val initialReminderEnabled = initialEvent?.reminderEnabled ?: seedDraft?.let {
        (it.normalizedAllReminderOffsetsMinutes.isNotEmpty() || it.absoluteReminderTimesMillis.isNotEmpty()) || (it.allDay && it.allDayReminderRulesJson !in listOf("", "[]"))
    } ?: workspace.settings.calendarTimedDefaultRemindersCsv.isNotBlank()
    var reminderEnabled by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(initialReminderEnabled)
    }
    var reminderInput by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(
            (initialEvent?.configuredReminderOffsetsMinutes
                ?: seedDraft?.normalizedReminderOffsetsMinutes
                ?: decodeReminderOffsets(workspace.settings.calendarTimedDefaultRemindersCsv)).joinToString(",")
        )
    }
    var allDayRulesJson by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(initialEvent?.let { it.allDayReminderRulesJson.ifBlank { calendarLegacyAllDayRules(it.configuredReminderOffsetsMinutes) } }
            ?: seedDraft?.let { it.allDayReminderRulesJson.ifBlank { if (it.allDay) calendarLegacyAllDayRules(it.normalizedReminderOffsetsMinutes) else workspace.settings.calendarAllDayDefaultRemindersJson } }
            ?: workspace.settings.calendarAllDayDefaultRemindersJson)
    }
    var timedReminderEnabled by remember { mutableStateOf(if (allDay) workspace.settings.calendarTimedDefaultRemindersCsv.isNotBlank() else reminderEnabled) }
    var allDayReminderEnabled by remember { mutableStateOf(if (allDay) reminderEnabled else allDayRulesJson != "[]") }
    var checkInReminderInput by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(
            (initialEvent?.configuredCheckInReminderOffsetsMinutes?.takeIf { it.isNotEmpty() }
                ?: seedDraft?.normalizedCheckInReminderOffsetsMinutes?.takeIf { it.isNotEmpty() })
                ?.joinToString(",")
                .orEmpty()
        )
    }
    var ringEnabled by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.ringEnabled ?: seedDraft?.ringEnabled ?: defaultRingEnabled) }
    var vibrateEnabled by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.vibrateEnabled ?: seedDraft?.vibrateEnabled ?: defaultVibrateEnabled) }
    val reminderDeliveryMode = ReminderDeliveryMode.FULLSCREEN_AND_NOTIFICATION
    var selectedCalendarId by remember(initialEvent?.id, seedDraft) { mutableStateOf((initialEvent?.calendarId ?: seedDraft?.calendarId ?: "default").takeIf { id -> writableCalendars.any { it.id == id } } ?: "default") }
    var participantsJson by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(initialEvent?.eventParticipantsJson ?: seedDraft?.eventParticipantsJson ?: "[]")
    }
    val selectedCalendar = workspace.calendars.firstOrNull { it.id == selectedCalendarId }
    var countdownPastMode by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.countdownPastMode ?: seedDraft?.countdownPastMode ?: "AUTO") }
    var countdownEnabled by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(initialEvent?.countdownEnabled == true || seedDraft?.countdownEnabled == true)
    }
    var checkInEnabled by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(initialEvent?.checkInEnabled == true || seedDraft?.checkInEnabled == true)
    }
    var recurringEnabled by remember(initialEvent?.id, seedDraft, editingScope) { mutableStateOf(!singleInstanceContext && (initialEvent?.isRecurring == true || seedDraft?.recurrence?.enabled == true)) }
    var recurrenceType by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(initialEvent?.recurrenceTypeEnum ?: seedDraft?.recurrence?.type ?: RecurrenceType.DAILY)
    }
    var weeklyDays by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(
            if (initialEvent?.isRecurring == true) {
                storageStringToWeekdays(initialEvent.recurrenceWeekdays).ifEmpty { setOf(startAt.dayOfWeek) }
            } else if (seedDraft?.recurrence?.enabled == true) {
                seedDraft.recurrence.weeklyDays.ifEmpty { setOf(startAt.dayOfWeek) }
            } else {
                setOf(startAt.dayOfWeek)
            }
        )
    }
    var recurrenceEndDate by remember(initialEvent?.id, seedDraft) {
        mutableStateOf(initialEvent?.recurrenceEndDate ?: seedDraft?.recurrence?.endDate ?: startAt.toLocalDate().plusDays(90))
    }
    var recurrenceNeverEnds by remember(initialEvent?.id, seedDraft) { mutableStateOf(
        if (initialEvent?.isRecurring == true) initialEvent.recurrenceEndDate == null else seedDraft?.recurrence?.endDate == null)
    }
    var recurrenceInterval by remember(initialEvent?.id, seedDraft) { mutableStateOf((initialEvent?.recurrenceInterval ?: seedDraft?.recurrence?.interval ?: 1).toString()) }
    var recurrenceInvalidDatePolicy by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.recurrenceInvalidDatePolicy ?: seedDraft?.recurrence?.invalidDatePolicy?.takeIf { seedDraft?.recurrence?.enabled == true } ?: "SKIP") }
    var monthlyOrdinal by remember(initialEvent?.id, seedDraft) { mutableStateOf(initialEvent?.recurrenceMonthlyOrdinal ?: seedDraft?.recurrence?.monthlyOrdinal ?: ((startAt.dayOfMonth - 1) / 7 + 1)) }
    var recurrencePreview by remember { mutableStateOf<RecurrencePreviewResult?>(null) }
    var showRecurrencePicker by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var activeDateTimeTarget by remember { mutableStateOf<CalendarDateTimeTarget?>(null) }
    var activeLunarDateTarget by remember { mutableStateOf<CalendarDateTarget?>(null) }
    var courseMultiSlotEnabled by remember(initialEvent?.id) { mutableStateOf(false) }
    var syncMultiSlotBundleSharedFields by remember(initialEvent?.id) { mutableStateOf(false) }
    var courseSlots by remember(initialEvent?.id) {
        mutableStateOf(
            listOf(
                CourseTimeSlot(
                    weekday = startAt.dayOfWeek,
                    startTime = startAt.toLocalTime(),
                    endTime = endAt.toLocalTime()
                )
            )
        )
    }
    var activeCourseSlotTarget by remember { mutableStateOf<CourseTimeSlotTarget?>(null) }
    val courseModeActive = initialEvent == null && courseMultiSlotEnabled
    val courseSlotsValid = courseSlots.isNotEmpty() && courseSlots.all { it.endTime.isAfter(it.startTime) }
    val reminderAnchor = if (allDay) LocalDateTime.of(startAt.toLocalDate(), LocalTime.of(9, 0)) else startAt
    val reminderValidation = remember(reminderInput, reminderAnchor, reminderEnabled, initialEvent?.id) {
        if (!reminderEnabled || reminderInput.isBlank()) {
            ReminderInputValidation(isValid = true)
        } else {
            parseReminderInput(
                raw = reminderInput,
                anchor = reminderAnchor,
                now = now,
                requireFuture = false
            )
        }
    }
    val checkInReminderValidation = remember(
        checkInReminderInput,
        reminderAnchor,
        reminderEnabled,
        checkInEnabled,
        initialEvent?.id
    ) {
        if (!reminderEnabled || !checkInEnabled || checkInReminderInput.isBlank()) {
            ReminderInputValidation(isValid = true)
        } else {
            parseReminderInput(
                raw = checkInReminderInput,
                anchor = reminderAnchor,
                now = now,
                requireFuture = false
            )
        }
    }
    val courseReminderValidation = remember(
        reminderInput,
        reminderEnabled,
        courseModeActive,
        courseSlots,
        startAt,
        initialEvent?.id
    ) {
        if (reminderInput.isBlank()) ReminderInputValidation(isValid = true) else validateCourseReminderInput(
            raw = reminderInput,
            baseDate = startAt.toLocalDate(),
            courseSlots = courseSlots,
            reminderEnabled = reminderEnabled && courseModeActive,
            requireFuture = false
        )
    }
    val courseCheckInReminderValidation = remember(
        checkInReminderInput,
        reminderEnabled,
        checkInEnabled,
        courseModeActive,
        courseSlots,
        startAt,
        initialEvent?.id
    ) {
        if (checkInReminderInput.isBlank()) ReminderInputValidation(isValid = true) else validateCourseReminderInput(
            raw = checkInReminderInput,
            baseDate = startAt.toLocalDate(),
            courseSlots = courseSlots,
            reminderEnabled = reminderEnabled && checkInEnabled && courseModeActive,
            requireFuture = false
        )
    }
    val effectiveReminderValidation = if (courseModeActive) courseReminderValidation else reminderValidation
    val effectiveCheckInReminderValidation = if (courseModeActive) courseCheckInReminderValidation else checkInReminderValidation
    val hasPastReminderTrigger = reminderEnabled && (
        effectiveReminderValidation.triggerTimes.any { !it.isAfter(now) } ||
            effectiveCheckInReminderValidation.triggerTimes.any { !it.isAfter(now) }
        )
    val eventEndValid = if (allDay) !endAt.toLocalDate().isBefore(startAt.toLocalDate()) else endAt.isAfter(startAt)
    val confirmEnabled = title.isNotBlank() &&
        (if (courseModeActive) courseSlotsValid else eventEndValid) &&
        (!recurringEnabled || recurrenceInterval.toIntOrNull() in 1..10000) &&
        (!recurringEnabled || recurrenceNeverEnds || !recurrenceEndDate.isBefore(startAt.toLocalDate())) &&
        (!reminderEnabled || ((allDay || effectiveReminderValidation.isValid) && effectiveCheckInReminderValidation.isValid))
    EditorBottomSheet(
        title = if (initialEvent == null) "新增日程" else "编辑日程",
        confirmLabel = "完成",
        confirmEnabled = confirmEnabled,
        onDismiss = onDismiss,
        onConfirm = {
            val normalizedOffsets = if (!allDay && reminderEnabled && reminderValidation.isValid) reminderValidation.offsetsMinutes else emptyList()
            val normalizedCheckInOffsets = if (
                reminderEnabled && checkInEnabled && checkInReminderValidation.isValid
            ) {
                checkInReminderValidation.offsetsMinutes
            } else {
                emptyList()
            }
            val selectedGroup = sortedGroups.firstOrNull { it.id == selectedGroupId }
            val baseDraft = CalendarEventDraft(
                title = title,
                notes = notes,
                location = location,
                startAt = startAt,
                endAt = endAt,
                allDay = allDay,
                eventTimeZoneId = eventTimeZoneId,
                allDayReminderRulesJson = if (allDay) { if (reminderEnabled) allDayRulesJson else "[]" } else "",
                eventBusyStatus = eventBusyStatus,
                eventVisibility = eventVisibility,
                eventParticipantsJson = selectedCalendar?.let { calendar ->
                    calendarParticipantSelection(calendar, calendarParticipants(participantsJson).map { it.username }.toSet(), participantsJson)
                } ?: "[]",
                accentColorHex = accentColorHex,
                reminderMinutesBefore = normalizedOffsets.minOrNull(),
                reminderOffsetsMinutes = normalizedOffsets,
                absoluteReminderTimesMillis = if (reminderEnabled) initialEvent?.configuredAbsoluteReminderTimesMillis ?: seedDraft?.absoluteReminderTimesMillis.orEmpty() else emptyList(),
                ringEnabled = ringEnabled,
                vibrateEnabled = vibrateEnabled,
                reminderDeliveryMode = reminderDeliveryMode,
                countdownEnabled = countdownEnabled,
                calendarId = selectedCalendarId,
                countdownPastMode = countdownPastMode,
                checkInEnabled = checkInEnabled,
                checkInReminderOffsetsMinutes = normalizedCheckInOffsets,
                recurrence = RecurrenceConfig(
                    enabled = recurringEnabled,
                    type = recurrenceType,
                    weeklyDays = weeklyDays,
                    endDate = if (recurrenceNeverEnds && !courseModeActive) null else recurrenceEndDate,
                    interval = recurrenceInterval.toIntOrNull() ?: 1,
                    invalidDatePolicy = recurrenceInvalidDatePolicy,
                    monthlyOrdinal = monthlyOrdinal,
                    monthlyDay = initialEvent?.recurrenceMonthlyDay ?: seedDraft?.recurrence?.monthlyDay
                ),
                multiSlotBundleId = initialEvent?.multiSlotBundleId ?: seedDraft?.multiSlotBundleId,
                groupId = selectedGroupId,
                groupName = selectedGroup?.name.orEmpty()
            )
            if (courseModeActive) {
                val courseDrafts = buildWeeklyMultiSlotEventDrafts(
                    baseDraft = baseDraft,
                    slots = courseSlots.map { slot ->
                        CalendarEventTimeSlot(
                            weekday = slot.weekday,
                            startTime = slot.startTime,
                            endTime = slot.endTime
                        )
                    },
                    baseDate = startAt.toLocalDate(),
                    recurrenceEndDate = recurrenceEndDate,
                    checkInReminderOffsetsForSlot = { _, slotStartAt ->
                        if (reminderEnabled && checkInEnabled && checkInReminderInput.isNotBlank()) {
                            parseReminderInput(
                                raw = checkInReminderInput,
                                anchor = slotStartAt,
                                now = now,
                                requireFuture = initialEvent == null && slotStartAt.isAfter(now)
                            ).offsetsMinutes
                        } else {
                            emptyList()
                        }
                    },
                    reminderOffsetsForSlot = { _, slotStartAt ->
                        if (reminderEnabled && reminderInput.isNotBlank()) {
                            parseReminderInput(
                                raw = reminderInput,
                                anchor = slotStartAt,
                                now = now,
                                requireFuture = initialEvent == null && slotStartAt.isAfter(now)
                            ).offsetsMinutes
                        } else {
                            emptyList()
                        }
                    }
                )
                onConfirmMultiple?.invoke(courseDrafts) ?: onConfirm(courseDrafts.first())
            } else {
                if (syncMultiSlotBundleSharedFields && initialEvent?.multiSlotBundleId?.isNotBlank() == true) {
                    onConfirmMultiSlotBundleShared?.invoke(baseDraft) ?: onConfirm(baseDraft)
                } else {
                    onConfirm(baseDraft)
                }
            }
        }
    ) {
                EditorBlock(title = "主题") {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("添加主题") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                EditorBlock(title = "日历") {
                    PreferenceChoice("所属日历", selectedCalendarId, writableCalendars.map { it.id to it.name }) { id ->
                        if (id != selectedCalendarId) participantsJson = "[]"
                        selectedCalendarId = id
                        workspace.calendars.firstOrNull { it.id == id }?.let { accentColorHex = it.colorHex }
                    }
                }
                if (selectedCalendar?.id?.startsWith("shared:") == true && selectedCalendar.shareMode == "SHARED") {
                    EditorBlock(title = "日程协作") {
                        CalendarParticipantsEditor(selectedCalendar, participantsJson) { participantsJson = it }
                    }
                }
                if (countdownEnabled) EditorBlock(title = "纪念展示") {
                    PreferenceChoice("结束后", countdownPastMode, CountdownPastChoices) { countdownPastMode = it }
                }
                EditorBlock(title = "分组") {
                    EditorSelectionRow(
                        title = "日程分组",
                        value = sortedGroups.firstOrNull { it.id == selectedGroupId }?.name ?: "默认分组",
                        onClick = { if (sortedGroups.isNotEmpty()) showGroupPicker = true }
                    )
                    Text(
                        text = "保留事项的分类信息；日历归属和显隐由上方“所属日历”独立管理。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                EditorBlock(title = "起止时间") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("全天日程", fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = allDay,
                            modifier = Modifier.testTag("calendar-editor-all-day"),
                            onCheckedChange = { target ->
                                if (target) {
                                    rememberedTimedStart = startAt; rememberedTimedEnd = endAt
                                    timedReminderEnabled = reminderEnabled; timedBusyStatus = eventBusyStatus
                                    startAt = startAt.toLocalDate().atStartOfDay(); endAt = endAt.toLocalDate().atStartOfDay()
                                    reminderEnabled = allDayReminderEnabled; eventBusyStatus = allDayBusyStatus
                                    courseMultiSlotEnabled = false
                                } else {
                                    allDayReminderEnabled = reminderEnabled; allDayBusyStatus = eventBusyStatus
                                    startAt = LocalDateTime.of(startAt.toLocalDate(), rememberedTimedStart.toLocalTime())
                                    endAt = LocalDateTime.of(endAt.toLocalDate(), rememberedTimedEnd.toLocalTime())
                                    if (!endAt.isAfter(startAt)) endAt = startAt.plusMinutes(workspace.settings.calendarDefaultDurationMinutes.toLong())
                                    reminderEnabled = timedReminderEnabled; eventBusyStatus = timedBusyStatus
                                }
                                allDay = target
                            }
                        )
                    }

                    if (allDay) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            EditorDateSelectorCard(
                                modifier = Modifier.weight(1f),
                                label = "开始日期",
                                date = startAt.toLocalDate(),
                                onClick = {
                                    showDatePicker(context, startAt.toLocalDate()) { picked ->
                                        startAt = LocalDateTime.of(picked, LocalTime.MIN)
                                        if (endAt.toLocalDate().isBefore(picked)) {
                                            endAt = LocalDateTime.of(picked, LocalTime.MIN)
                                        }
                                    }
                                }
                            )
                            EditorMiddlePill(
                                modifier = Modifier.width(86.dp),
                                label = formatAllDaySpan(startAt.toLocalDate(), endAt.toLocalDate())
                            )
                            EditorDateSelectorCard(
                                modifier = Modifier.weight(1f),
                                label = "结束日期",
                                date = endAt.toLocalDate(),
                                onClick = {
                                    showDatePicker(context, endAt.toLocalDate()) { picked ->
                                        endAt = LocalDateTime.of(picked, LocalTime.MIN)
                                    }
                                }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { activeLunarDateTarget = CalendarDateTarget.StartDate }
                            ) { Text("农历开始") }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { activeLunarDateTarget = CalendarDateTarget.EndDate }
                            ) { Text("农历结束") }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EditorDateTimeSelectorCard(
                                modifier = Modifier.weight(1f),
                                label = "开始",
                                dateTime = startAt,
                                onClick = {
                                    activeDateTimeTarget = CalendarDateTimeTarget.StartAt
                                }
                            )
                            EditorMiddlePill(
                                modifier = Modifier.width(92.dp),
                                label = formatTimedSpan(startAt, endAt)
                            )
                            EditorDateTimeSelectorCard(
                                modifier = Modifier.weight(1f),
                                label = "结束",
                                dateTime = endAt,
                                onClick = {
                                    activeDateTimeTarget = CalendarDateTimeTarget.EndAt
                                }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { activeLunarDateTarget = CalendarDateTarget.StartDate }
                            ) { Text("农历开始") }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { activeLunarDateTarget = CalendarDateTarget.EndDate }
                            ) { Text("农历结束") }
                        }
                    }
                }

                if (initialEvent == null) {
                    EditorBlock(title = "每周多时间段") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("同一日程每周多个时间段", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (courseModeActive) {
                                        "将创建 ${courseSlots.size} 条同名周循环日程，共用标题、地点、描述、提醒和颜色。"
                                    } else {
                                        "适合课程、实验、固定值班等：例如周二 10:20-11:55 + 周四 08:30-10:05。"
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = courseMultiSlotEnabled,
                                onCheckedChange = { enabled ->
                                    courseMultiSlotEnabled = enabled
                                    if (enabled) {
                                        allDay = false
                                        courseSlots = if (courseSlots.size == 1) {
                                            listOf(
                                                CourseTimeSlot(
                                                    weekday = startAt.dayOfWeek,
                                                    startTime = startAt.toLocalTime(),
                                                    endTime = endAt.toLocalTime()
                                                )
                                            )
                                        } else {
                                            courseSlots
                                        }
                                        recurringEnabled = true
                                        recurrenceType = RecurrenceType.WEEKLY
                                        weeklyDays = courseSlots.map { it.weekday }.toSet()
                                    }
                                },
                                thumbContent = null
                            )
                        }

                        if (courseModeActive) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "起始周参考：${formatEditorDateTitle(startAt.toLocalDate())}",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "如果某段周几早于这个日期，会从下一个对应周几开始生成。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            courseSlots.forEachIndexed { index, slot ->
                                CourseTimeSlotCard(
                                    index = index,
                                    slot = slot,
                                    canRemove = courseSlots.size > 1,
                                    onPickWeekday = { day ->
                                        courseSlots = courseSlots.replaceAt(index, slot.copy(weekday = day))
                                        weeklyDays = courseSlots.map { it.weekday }.toSet()
                                    },
                                    onPickStart = {
                                        activeCourseSlotTarget = CourseTimeSlotTarget(index, editingStart = true)
                                    },
                                    onPickEnd = {
                                        activeCourseSlotTarget = CourseTimeSlotTarget(index, editingStart = false)
                                    },
                                    onRemove = {
                                        courseSlots = courseSlots.removeAt(index)
                                        weeklyDays = courseSlots.map { it.weekday }.toSet()
                                    }
                                )
                            }

                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val last = courseSlots.lastOrNull()
                                        ?: CourseTimeSlot(startAt.dayOfWeek, startAt.toLocalTime(), endAt.toLocalTime())
                                    val nextDay = DayOfWeek.of(if (last.weekday.value == 7) 1 else last.weekday.value + 1)
                                    courseSlots = courseSlots + last.copy(weekday = nextDay)
                                    weeklyDays = courseSlots.map { it.weekday }.toSet()
                                }
                            ) {
                                Text("添加一个时间段")
                            }
                        }
                    }
                }

                if (initialEvent?.multiSlotBundleId?.isNotBlank() == true) {
                    EditorBlock(title = "每周多时间段组") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("同步共享字段到同组全部时间段", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "保存时同步标题、地点、描述、分组、颜色、提醒、倒数日和打卡；不会改每个时间段自己的周几和起止时间。",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = syncMultiSlotBundleSharedFields,
                                onCheckedChange = { syncMultiSlotBundleSharedFields = it },
                                thumbContent = null
                            )
                        }
                    }
                }

                EditorBlock(title = "地点") {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("添加地点") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                EditorBlock(title = "描述") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("添加描述") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                EditorBlock(title = "日程标记") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("在每日看板显示倒数日", fontWeight = FontWeight.SemiBold)
                            Text("适合期末考试、面试、报名截止等重大日程。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = countdownEnabled,
                            onCheckedChange = { countdownEnabled = it },
                            thumbContent = null
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("打卡追踪", fontWeight = FontWeight.SemiBold)
                            Text("开启后可在日程进行期间签到/签退，记录实际投入时间。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = checkInEnabled,
                            onCheckedChange = { checkInEnabled = it },
                            thumbContent = null
                        )
                    }
                }

                EditorBlock(title = "提醒") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("提醒", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (allDay) {
                                    "按所选日期和钟点提醒；开启后沿用全屏提醒和通知栏提醒。"
                                } else {
                                    "按开始时间提前提醒；开启后同时进行全屏提醒和通知栏提醒。"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Checkbox(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it }
                        )
                    }

                    if (reminderEnabled) {
                        if (allDay) AllDayReminderRulesEditor(allDayRulesJson) { allDayRulesJson = it } else ReminderOffsetSelector(
                            label = "普通提醒",
                            rawInput = reminderInput,
                            validation = effectiveReminderValidation,
                            onRawInputChange = { reminderInput = it },
                            emptySummary = "不设置普通提醒"
                        )
                        if (checkInEnabled) {
                            ReminderOffsetSelector(
                                label = "签到提醒",
                                rawInput = checkInReminderInput,
                                validation = effectiveCheckInReminderValidation,
                                onRawInputChange = { checkInReminderInput = it },
                                emptySummary = "不设置签到提醒"
                            )
                        }
                        if (hasPastReminderTrigger) {
                            Text(
                                text = "可以记录过去的日程；已经过去的提醒不会补发，后续仍未到达的提醒照常生效。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = buildReminderMetaLabel(
                                reminderDeliveryMode = reminderDeliveryMode,
                                ringEnabled = ringEnabled,
                                vibrateEnabled = vibrateEnabled
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = ringEnabled,
                                onClick = { ringEnabled = !ringEnabled },
                                label = { Text("响铃") }
                            )
                            FilterChip(
                                selected = vibrateEnabled,
                                onClick = { vibrateEnabled = !vibrateEnabled },
                                label = { Text("震动") }
                            )
                        }
                    }
                }

                EditorBlock(title = "重复") {
                    if (singleInstanceContext) {
                        Text("仅修改当前日程，循环规则和其他日期保持不变。", style = MaterialTheme.typography.bodyMedium)
                        if (onChooseRecurrenceScope != null) TextButton(onClick = onChooseRecurrenceScope) { Text("选择循环修改范围") }
                    } else if (courseModeActive) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "每周多时间段固定为每周循环",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = courseSlots.sortedWith(compareBy<CourseTimeSlot> { it.weekday.value }.thenBy { it.startTime })
                                        .joinToString("；") { "${it.weekday.shortLabel()} ${formatLocalTime(it.startTime)}-${formatLocalTime(it.endTime)}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { showDatePicker(context, recurrenceEndDate) { recurrenceEndDate = it } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("循环截止日期：$recurrenceEndDate")
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("重复", fontWeight = FontWeight.SemiBold)
                                Text(if (recurringEnabled) recurrenceType.label else "不重复", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = recurringEnabled,
                                onCheckedChange = { recurringEnabled = it },
                                thumbContent = null
                            )
                        }

                        if (recurringEnabled) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = recurrenceSummaryLabel(recurrenceType, startAt, weeklyDays),
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (recurrenceNeverEnds) "每隔 $recurrenceInterval 次 · 永不截止" else "每隔 $recurrenceInterval 次 · 循环到 $recurrenceEndDate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            EditorSelectionRow(
                                title = "重复规则",
                                value = recurrenceSummaryLabel(recurrenceType, startAt, weeklyDays),
                                onClick = { showRecurrencePicker = true }
                            )
                            OutlinedTextField(
                                value = recurrenceInterval,
                                onValueChange = { recurrenceInterval = it.filter(Char::isDigit).take(5) },
                                label = { Text("重复间隔（1–10000）") },
                                singleLine = true,
                                isError = recurrenceInterval.toIntOrNull() !in 1..10000,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (recurrenceType == RecurrenceType.MONTHLY_NTH_WEEKDAY) {
                                PreferenceChoice("每月星期位置", monthlyOrdinal.toString(), (1..5).map { it.toString() to "第${it}个" } + listOf("-1" to "最后一个")) { monthlyOrdinal = it.toInt() }
                            }
                            if (recurrenceType == RecurrenceType.MONTHLY_DAY && startAt.dayOfMonth > 28) {
                                PreferenceChoice("不存在的日期", recurrenceInvalidDatePolicy, listOf("SKIP" to "跳过该月", "CLAMP" to "使用月末")) { recurrenceInvalidDatePolicy = it }
                            }
                            if (recurrenceType == RecurrenceType.YEARLY_DATE && startAt.monthValue == 2 && startAt.dayOfMonth == 29) {
                                PreferenceChoice("非闰年", recurrenceInvalidDatePolicy, listOf("SKIP" to "跳过该年", "CLAMP" to "使用2月末")) { recurrenceInvalidDatePolicy = it }
                            }
                            PreferenceChoice("截止", if (recurrenceNeverEnds) "NEVER" else "DATE", listOf("NEVER" to "永不截止", "DATE" to "指定日期")) { recurrenceNeverEnds = it == "NEVER" }
                            if (!recurrenceNeverEnds) OutlinedButton(
                                onClick = { showDatePicker(context, recurrenceEndDate) { recurrenceEndDate = it } },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("循环截止日期：$recurrenceEndDate")
                            }
                            OutlinedButton(
                                onClick = {
                                    val normalizedOffsets = if (reminderEnabled && reminderValidation.isValid) reminderValidation.offsetsMinutes else emptyList()
                                    val normalizedCheckInOffsets = if (
                                        reminderEnabled && checkInEnabled && checkInReminderValidation.isValid
                                    ) {
                                        checkInReminderValidation.offsetsMinutes
                                    } else {
                                        emptyList()
                                    }
                                    recurrencePreview = previewCalendarRecurrence(
                                        CalendarEventDraft(
                                            title = title,
                                            notes = notes,
                                            location = location,
                                            startAt = startAt,
                                            endAt = endAt,
                                            allDay = allDay,
                                            eventTimeZoneId = eventTimeZoneId,
                                            allDayReminderRulesJson = if (allDay) { if (reminderEnabled) allDayRulesJson else "[]" } else "",
                                            eventBusyStatus = eventBusyStatus,
                                            eventVisibility = eventVisibility,
                                            accentColorHex = accentColorHex,
                                            reminderMinutesBefore = normalizedOffsets.minOrNull(),
                                            reminderOffsetsMinutes = normalizedOffsets,
                absoluteReminderTimesMillis = if (reminderEnabled) initialEvent?.configuredAbsoluteReminderTimesMillis ?: seedDraft?.absoluteReminderTimesMillis.orEmpty() else emptyList(),
                                            ringEnabled = ringEnabled,
                                            vibrateEnabled = vibrateEnabled,
                                            reminderDeliveryMode = reminderDeliveryMode,
                                            countdownEnabled = countdownEnabled,
                calendarId = selectedCalendarId,
                countdownPastMode = countdownPastMode,
                                            checkInEnabled = checkInEnabled,
                                            checkInReminderOffsetsMinutes = normalizedCheckInOffsets,
                                            recurrence = RecurrenceConfig(
                                                enabled = true,
                                                type = recurrenceType,
                                                weeklyDays = weeklyDays,
                                                endDate = if (recurrenceNeverEnds) null else recurrenceEndDate,
                                                interval = recurrenceInterval.toIntOrNull() ?: 1,
                                                invalidDatePolicy = recurrenceInvalidDatePolicy,
                                                monthlyOrdinal = monthlyOrdinal,
                    monthlyDay = initialEvent?.recurrenceMonthlyDay ?: seedDraft?.recurrence?.monthlyDay
                                            ),
                                            groupId = selectedGroupId,
                                            groupName = sortedGroups.firstOrNull { it.id == selectedGroupId }?.name.orEmpty()
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("预览循环生成")
                            }
                        }
                    }
                }

                EditorBlock(title = "时区与可见范围") {
                    CalendarTimeZoneChoice("日程时区", eventTimeZoneId) { eventTimeZoneId = it }
                    PreferenceChoice("忙闲", eventBusyStatus, listOf("BUSY" to "忙碌", "FREE" to "空闲")) { eventBusyStatus = it }
                    PreferenceChoice("可见范围", eventVisibility, listOf("DEFAULT" to "沿用日历设置", "PRIVATE" to "私密", "PUBLIC" to "公开")) { eventVisibility = it }
                    Text("个人日历的内容只在你的账号内同步；共享日历的成员访问由服务器权限控制。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                EditorBlock(title = "日程颜色") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PaykiColorPresets.Rainbow12.forEach { candidate ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(colorFromHex(candidate))
                                    .border(
                                        width = if (accentColorHex == candidate) 3.dp else 1.dp,
                                        color = if (accentColorHex == candidate) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(7.dp)
                                    )
                                    .semantics { contentDescription = "日程颜色 $candidate" }
                                    .selectable(selected = accentColorHex == candidate, role = Role.RadioButton) { accentColorHex = candidate }
                            ) {}
                        }
                    }
                }
    }

    recurrencePreview?.let { preview ->
        RecurrencePreviewDialog(
            title = "循环日程预览",
            preview = preview,
            onDismiss = { recurrencePreview = null }
        )
    }

    activeDateTimeTarget?.let { target ->
        WheelDateTimePickerDialog(
            title = when (target) {
                CalendarDateTimeTarget.StartAt -> "选择开始时间"
                CalendarDateTimeTarget.EndAt -> "选择结束时间"
            },
            initialDateTime = when (target) {
                CalendarDateTimeTarget.StartAt -> startAt
                CalendarDateTimeTarget.EndAt -> endAt
            },
            onDismiss = { activeDateTimeTarget = null },
            onConfirm = { picked ->
                when (target) {
                    CalendarDateTimeTarget.StartAt -> {
                        startAt = picked
                        if (!picked.isBefore(endAt)) {
                            endAt = picked.plusMinutes(30)
                        }
                    }

                    CalendarDateTimeTarget.EndAt -> {
                        endAt = picked
                        if (!picked.isAfter(startAt)) {
                            startAt = picked.minusMinutes(30)
                        }
                    }
                }
                activeDateTimeTarget = null
            }
        )
    }

    activeCourseSlotTarget?.let { target ->
        val slot = courseSlots.getOrNull(target.index)
        if (slot == null) {
            activeCourseSlotTarget = null
        } else {
            WheelDateTimePickerDialog(
                title = if (target.editingStart) "选择课程开始时间" else "选择课程结束时间",
                initialDateTime = LocalDateTime.of(startAt.toLocalDate(), if (target.editingStart) slot.startTime else slot.endTime),
                onDismiss = { activeCourseSlotTarget = null },
                onConfirm = { picked ->
                    val pickedTime = picked.toLocalTime()
                    courseSlots = courseSlots.replaceAt(target.index) { current ->
                        if (target.editingStart) {
                            val adjustedEnd = if (current.endTime.isAfter(pickedTime)) current.endTime else pickedTime.plusMinutes(30)
                            current.copy(startTime = pickedTime, endTime = adjustedEnd)
                        } else {
                            val adjustedStart = if (pickedTime.isAfter(current.startTime)) current.startTime else pickedTime.minusMinutes(30)
                            current.copy(startTime = adjustedStart, endTime = pickedTime)
                        }
                    }
                    activeCourseSlotTarget = null
                }
            )
        }
    }

    activeLunarDateTarget?.let { target ->
        LunarDatePickerDialog(
            title = when (target) {
                CalendarDateTarget.StartDate -> "选择农历开始日期"
                CalendarDateTarget.EndDate -> "选择农历结束日期"
            },
            initialDate = when (target) {
                CalendarDateTarget.StartDate -> startAt.toLocalDate()
                CalendarDateTarget.EndDate -> endAt.toLocalDate()
            },
            onDismiss = { activeLunarDateTarget = null },
            onConfirm = { pickedDate ->
                when (target) {
                    CalendarDateTarget.StartDate -> {
                        val pickedStart = LocalDateTime.of(pickedDate, if (allDay) LocalTime.MIN else startAt.toLocalTime())
                        startAt = pickedStart
                        if (allDay && endAt.toLocalDate().isBefore(pickedDate)) {
                            endAt = LocalDateTime.of(pickedDate, LocalTime.MIN)
                        } else if (!allDay && !pickedStart.isBefore(endAt)) {
                            endAt = pickedStart.plusMinutes(30)
                        }
                    }

                    CalendarDateTarget.EndDate -> {
                        val pickedEnd = LocalDateTime.of(pickedDate, if (allDay) LocalTime.MIN else endAt.toLocalTime())
                        endAt = pickedEnd
                        if (allDay && pickedDate.isBefore(startAt.toLocalDate())) {
                            startAt = LocalDateTime.of(pickedDate, LocalTime.MIN)
                        } else if (!allDay && !pickedEnd.isAfter(startAt)) {
                            startAt = pickedEnd.minusMinutes(30)
                        }
                    }
                }
                activeLunarDateTarget = null
            }
        )
    }
    if (showGroupPicker) {
        SingleChoiceListDialog(
            title = "选择日程分组",
            doneLabel = "完成",
            options = sortedGroups.map { group ->
                ChoiceItem(
                    key = group.id.toString(),
                    title = group.name,
                    subtitle = if (group.isDefault) "默认分组" else null
                )
            },
            selectedKey = selectedGroupId.toString(),
            onSelect = { key ->
                key.toLongOrNull()?.let { selectedGroupId = it }
            },
            onDismiss = { showGroupPicker = false },
            onDone = { showGroupPicker = false }
        )
    }

    if (showRecurrencePicker) {
        RecurrenceSelectionDialog(
            recurrenceType = recurrenceType,
            weeklyDays = weeklyDays,
            startAt = startAt,
            onDismiss = { showRecurrencePicker = false },
            onSelectType = { recurrenceType = it },
            onToggleWeekday = { day ->
                weeklyDays = if (day in weeklyDays) weeklyDays - day else weeklyDays + day
            },
            onDone = { showRecurrencePicker = false }
        )
    }
}

@Composable
private fun EditorBlock(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

private data class ChoiceItem(
    val key: String,
    val title: String,
    val subtitle: String? = null
)

@Composable
private fun EditorSelectionRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseTimeSlotCard(
    index: Int,
    slot: CourseTimeSlot,
    canRemove: Boolean,
    onPickWeekday: (DayOfWeek) -> Unit,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("第 ${index + 1} 段", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "${slot.weekday.shortLabel()} ${formatLocalTime(slot.startTime)}-${formatLocalTime(slot.endTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canRemove) {
                    TextButton(onClick = onRemove) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        selected = slot.weekday == day,
                        onClick = { onPickWeekday(day) },
                        label = { Text(day.shortLabel()) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EditorTimeSelectorCard(
                    modifier = Modifier.weight(1f),
                    label = "开始",
                    time = slot.startTime,
                    onClick = onPickStart
                )
                EditorMiddlePill(
                    modifier = Modifier.width(84.dp),
                    label = formatTimeSlotDuration(slot)
                )
                EditorTimeSelectorCard(
                    modifier = Modifier.weight(1f),
                    label = "结束",
                    time = slot.endTime,
                    onClick = onPickEnd
                )
            }
            if (!slot.endTime.isAfter(slot.startTime)) {
                Text(
                    text = "结束时间必须晚于开始时间",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EditorTimeSelectorCard(
    modifier: Modifier = Modifier,
    label: String,
    time: LocalTime,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatLocalTime(time),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SingleChoiceListDialog(
    title: String,
    doneLabel: String,
    options: List<ChoiceItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option.key) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(option.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            option.subtitle?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (selectedKey == option.key) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDone) { Text(doneLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private enum class CalendarDateTimeTarget {
    StartAt,
    EndAt
}

private enum class CalendarDateTarget {
    StartDate,
    EndDate
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurrenceSelectionDialog(
    recurrenceType: RecurrenceType,
    weeklyDays: Set<DayOfWeek>,
    startAt: LocalDateTime,
    onDismiss: () -> Unit,
    onSelectType: (RecurrenceType) -> Unit,
    onToggleWeekday: (DayOfWeek) -> Unit,
    onDone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择重复", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                recurrenceChoiceItems(startAt).forEach { item ->
                    val selected = recurrenceType == item.type
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectType(item.type) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(item.label, modifier = Modifier.weight(1f), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (recurrenceType == RecurrenceType.WEEKLY) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DayOfWeek.entries.forEach { day ->
                                FilterChip(
                                    selected = day in weeklyDays,
                                    onClick = { onToggleWeekday(day) },
                                    label = { Text(day.shortLabel()) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDone) { Text("完成") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun EditorDateTimeSelectorCard(
    modifier: Modifier = Modifier,
    label: String,
    dateTime: LocalDateTime,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatClockTime(dateTime),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatEditorDateLine(dateTime.toLocalDate()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EditorDateSelectorCard(
    modifier: Modifier = Modifier,
    label: String,
    date: LocalDate,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatEditorDateTitle(date),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = date.dayOfWeek.shortLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditorMiddlePill(
    modifier: Modifier = Modifier,
    label: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "至",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildReminderMetaLabel(
    reminderDeliveryMode: ReminderDeliveryMode,
    ringEnabled: Boolean,
    vibrateEnabled: Boolean
): String {
    val toggles = buildList {
        if (ringEnabled) add("响铃")
        if (vibrateEnabled) add("震动")
        if (isEmpty()) add("静默")
    }
    return reminderDeliveryMode.label + " · " + toggles.joinToString(" + ")
}

private fun validateCourseReminderInput(
    raw: String,
    baseDate: LocalDate,
    courseSlots: List<CourseTimeSlot>,
    reminderEnabled: Boolean,
    requireFuture: Boolean
): ReminderInputValidation {
    if (!reminderEnabled) return ReminderInputValidation(isValid = true)
    val now = LocalDateTime.now().withSecond(0).withNano(0)
    var representative: ReminderInputValidation? = null
    courseSlots.sortedWith(compareBy<CourseTimeSlot> { it.weekday.value }.thenBy { it.startTime }).forEach { slot ->
        val slotDate = nextOrSameWeekday(baseDate, slot.weekday)
        val slotStartAt = LocalDateTime.of(slotDate, slot.startTime)
        val parsed = parseReminderInput(
            raw = raw,
            anchor = slotStartAt,
            now = now,
            requireFuture = requireFuture && slotStartAt.isAfter(now)
        )
        if (!parsed.isValid) {
            return parsed.copy(
                message = "${slot.weekday.shortLabel()} ${formatLocalTime(slot.startTime)}：${parsed.message}"
            )
        }
        if (representative == null) representative = parsed
    }
    return (representative ?: ReminderInputValidation(isValid = true)).copy(
        message = "每个时间段会按自己的开始时间解析提醒"
    )
}

private data class RecurrenceChoiceItem(
    val type: RecurrenceType,
    val label: String
)

private fun recurrenceChoiceItems(startAt: LocalDateTime): List<RecurrenceChoiceItem> {
    val nth = ((startAt.dayOfMonth - 1) / 7) + 1
    val lunar = LunarCalendar.labelFor(startAt.toLocalDate())
    return listOf(
        RecurrenceChoiceItem(RecurrenceType.DAILY, "每天"),
        RecurrenceChoiceItem(RecurrenceType.WEEKLY, "每周${startAt.dayOfWeek.shortLabel().removePrefix("周")}"),
        RecurrenceChoiceItem(RecurrenceType.MONTHLY_NTH_WEEKDAY, "每月第 ${nth} 个${startAt.dayOfWeek.shortLabel()}"),
        RecurrenceChoiceItem(RecurrenceType.MONTHLY_DAY, "每月${startAt.dayOfMonth}日"),
        RecurrenceChoiceItem(RecurrenceType.YEARLY_DATE, "每年${startAt.monthValue}月${startAt.dayOfMonth}日"),
        RecurrenceChoiceItem(RecurrenceType.YEARLY_LUNAR_DATE, "每年农历${lunar.displayText}")
    )
}

private fun recurrenceSummaryLabel(
    recurrenceType: RecurrenceType,
    startAt: LocalDateTime,
    weeklyDays: Set<DayOfWeek>
): String {
    return when (recurrenceType) {
        RecurrenceType.DAILY -> "每天重复"
        RecurrenceType.WEEKLY -> {
            val labels = weeklyDays.sortedBy { it.value }.joinToString("、") { it.shortLabel() }
            "每周 · $labels"
        }
        RecurrenceType.MONTHLY_NTH_WEEKDAY -> {
            val weekIndex = ((startAt.dayOfMonth - 1) / 7) + 1
            "每月第 ${weekIndex} 个${startAt.dayOfWeek.shortLabel()}"
        }
        RecurrenceType.MONTHLY_DAY -> "每月 ${startAt.dayOfMonth} 日"
        RecurrenceType.YEARLY_DATE -> "每年 ${startAt.monthValue} 月 ${startAt.dayOfMonth} 日"
        RecurrenceType.YEARLY_LUNAR_DATE -> "每年农历 ${LunarCalendar.labelFor(startAt.toLocalDate()).displayText}"
        RecurrenceType.NONE -> "不重复"
    }
}

private fun formatTimedSpan(startAt: LocalDateTime, endAt: LocalDateTime): String {
    val minutes = Duration.between(startAt, endAt).toMinutes().coerceAtLeast(0)
    return when {
        minutes >= 24 * 60 -> "${minutes / (24 * 60)}天"
        minutes >= 60 && minutes % 60 == 0L -> "${minutes / 60}小时"
        minutes >= 60 -> "${minutes / 60}小时${minutes % 60}分"
        else -> "${minutes}分"
    }
}

private fun formatTimeSlotDuration(slot: CourseTimeSlot): String {
    val minutes = Duration.between(slot.startTime, slot.endTime).toMinutes().coerceAtLeast(0)
    return when {
        minutes >= 60 && minutes % 60 == 0L -> "${minutes / 60}小时"
        minutes >= 60 -> "${minutes / 60}小时${minutes % 60}分"
        else -> "${minutes}分"
    }
}

private fun formatLocalTime(time: LocalTime): String {
    return time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA))
}

private fun nextOrSameWeekday(base: LocalDate, weekday: DayOfWeek): LocalDate {
    val delta = (weekday.value - base.dayOfWeek.value + 7) % 7
    return base.plusDays(delta.toLong())
}

private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> {
    if (index !in indices) return this
    return mapIndexed { currentIndex, currentValue -> if (currentIndex == index) value else currentValue }
}

private fun <T> List<T>.replaceAt(index: Int, transform: (T) -> T): List<T> {
    if (index !in indices) return this
    return mapIndexed { currentIndex, currentValue -> if (currentIndex == index) transform(currentValue) else currentValue }
}

private fun <T> List<T>.removeAt(index: Int): List<T> {
    if (index !in indices) return this
    return filterIndexed { currentIndex, _ -> currentIndex != index }
}

private fun formatAllDaySpan(startDate: LocalDate, endDate: LocalDate): String {
    val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).coerceAtLeast(0) + 1
    return if (days <= 1) "当天" else "${days}天"
}

private fun formatEditorDateLine(date: LocalDate): String {
    return formatEditorDateTitle(date) + " · " + date.dayOfWeek.shortLabel()
}

private fun formatEditorDateTitle(date: LocalDate): String {
    val nowYear = LocalDate.now().year
    val pattern = if (date.year == nowYear) "M月d日" else "yyyy年M月d日"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.CHINA)) + lunarParenthesized(date)
}

private fun lunarParenthesized(date: LocalDate): String {
    return "（农历${LunarCalendar.labelFor(date).displayText}）"
}

private fun formatClockTime(dateTime: LocalDateTime): String {
    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA))
}

private fun showDatePicker(
    context: Context,
    initialDate: LocalDate,
    onPicked: (LocalDate) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, day ->
            onPicked(LocalDate.of(year, month + 1, day))
        },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth
    ).show()
}

private fun DayOfWeek.shortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "周一"
    DayOfWeek.TUESDAY -> "周二"
    DayOfWeek.WEDNESDAY -> "周三"
    DayOfWeek.THURSDAY -> "周四"
    DayOfWeek.FRIDAY -> "周五"
    DayOfWeek.SATURDAY -> "周六"
    DayOfWeek.SUNDAY -> "周日"
}
