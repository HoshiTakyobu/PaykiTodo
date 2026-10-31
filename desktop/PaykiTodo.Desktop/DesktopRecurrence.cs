using System.Globalization;
using System.Text.Json.Nodes;

namespace PaykiTodo.Desktop;
internal enum RecurrenceScope { Current, CurrentAndFuture, All }
internal sealed record DesktopTimeSlot(long StartAtMillis, long EndAtMillis);
internal sealed partial class DesktopSnapshotDocument
{
    private long SaveEventSlots(DesktopItemEdit edit)
    {
        if (!edit.IsEvent || edit.AllDay) throw new InvalidOperationException("多时段仅用于非全天日程");
        var drafts = new[] { edit with { AdditionalTimeSlots = Array.Empty<DesktopTimeSlot>() } }.Concat(edit.AdditionalTimeSlots.Select(slot => edit with {
            StartAtMillis = slot.StartAtMillis, EndAtMillis = slot.EndAtMillis, AdditionalTimeSlots = Array.Empty<DesktopTimeSlot>()
        })).ToList();
        foreach (var draft in drafts) ValidateItemEdit(draft);
        if (drafts.Select(d => (d.StartAtMillis, d.EndAtMillis)).Distinct().Count() != drafts.Count) throw new InvalidOperationException("请删除重复的时段");
        var before = (JsonObject)root.DeepClone(); var bundle = Guid.NewGuid().ToString(); long first = 0;
        try {
            foreach (var draft in drafts) {
                var id = SaveItemCore(draft); if (first == 0) first = id;
                var item = FindTask(id)!; item["multiSlotBundleId"] = bundle;
                var series = item["recurringSeriesId"]?.GetValue<string>();
                foreach (var template in Templates.OfType<JsonObject>().Where(t => series != null && t["seriesId"]?.GetValue<string>() == series)) template["multiSlotBundleId"] = bundle;
            }
            return first;
        } catch { root = before; throw; }
    }
    private static readonly string[] RecurrenceFields = { "recurrenceType", "recurrenceWeekdays", "recurrenceEndEpochDay", "recurrenceAnchorDueAtMillis", "recurringSeriesId", "recurrenceMonthlyOrdinal", "recurrenceMonthlyWeekday", "recurrenceMonthlyDay", "recurrenceYearlyMonth", "recurrenceYearlyDay", "recurrenceInterval", "recurrenceInvalidDatePolicy" };
    private static readonly ChineseLunisolarCalendar Lunar = new();
    private static DateTime LocalTime(long millis) => DateTimeOffset.FromUnixTimeMilliseconds(millis).LocalDateTime;
    private static long MillisAt(DateTime date) => new DateTimeOffset(date).ToUnixTimeMilliseconds();
    private static long EpochDay(DateTime date) => (long)(date.Date - DateTime.UnixEpoch.Date).TotalDays;
    private static DateTime DayFromEpoch(long day) => DateTime.SpecifyKind(DateTime.UnixEpoch.AddDays(day), DateTimeKind.Unspecified);
    private long Anchor(JsonObject item)
    {
        var anchor = item["recurrenceAnchorDueAtMillis"]?.GetValue<long?>() ?? item["startAtMillis"]?.GetValue<long?>() ?? item["dueAtMillis"]!.GetValue<long>();
        // Older Windows builds used the event end as anchor. Correct only when
        // the template time unambiguously identifies the actual start; preserve phone exceptions.
        if (item["startAtMillis"]?.GetValue<long?>() is { } start && item["endAtMillis"]?.GetValue<long?>() == anchor) {
            var series = item["recurringSeriesId"]?.GetValue<string>();
            var template = Templates.OfType<JsonObject>().FirstOrDefault(t => series != null && t["seriesId"]?.GetValue<string>() == series);
            if (template != null) {
                var time = TimeSpan.FromHours(template["dueHour"]?.GetValue<int>() ?? 0) + TimeSpan.FromMinutes(template["dueMinute"]?.GetValue<int>() ?? 0);
                if (LocalTime(start).TimeOfDay == time && LocalTime(anchor).TimeOfDay != time) return start;
            }
        }
        return anchor;
    }
    private long InstanceDay(JsonObject item) => EpochDay(DesktopCalendarTime.Wall(Anchor(item), item["eventTimeZoneId"]?.GetValue<string>()));

    internal static DesktopItemEdit NormalizeItemEdit(DesktopItemEdit edit)
    {
        if (!edit.IsEvent || !edit.AllDay || edit.StartAtMillis == null || edit.EndAtMillis == null) return edit;
        var start = DesktopCalendarTime.Wall(edit.StartAtMillis.Value, edit.EventTimeZoneId).Date;
        var end = DesktopCalendarTime.Wall(edit.EndAtMillis.Value, edit.EventTimeZoneId);
        // Storage uses an exclusive midnight end, matching Android overlap queries.
        end = end.TimeOfDay == TimeSpan.Zero ? end.Date : end.Date.AddDays(1);
        if (end <= start) end = start.AddDays(1);
        return edit with { StartAtMillis = DesktopCalendarTime.Millis(start, edit.EventTimeZoneId), EndAtMillis = DesktopCalendarTime.Millis(end, edit.EventTimeZoneId) };
    }

    private long SaveRecurringItem(DesktopItemEdit edit, JsonObject original, RecurrenceScope scope)
    {
        var id = original["id"]!.GetValue<long>();
        if (scope == RecurrenceScope.Current) {
            var rule = RecurrenceFields.ToDictionary(key => key, key => original[key]?.DeepClone());
            rule["recurrenceAnchorDueAtMillis"] = JsonValue.Create(Anchor(original));
            SaveItemCore(edit, id, false);
            foreach (var field in rule) original[field.Key] = field.Value;
            return id;
        }
        var series = original["recurringSeriesId"]!.GetValue<string>();
        var split = InstanceDay(original);
        var targets = Tasks.OfType<JsonObject>().Where(item => item["recurringSeriesId"]?.GetValue<string>() == series &&
            item["canceled"]?.GetValue<bool>() != true && (edit.IsEvent || item["completed"]?.GetValue<bool>() != true) &&
            (scope == RecurrenceScope.All || InstanceDay(item) >= split)).OrderBy(InstanceDay).ToList();
        if (targets.Count == 0) return id;
        var prior = DesktopItemEdit.FromJson(original);
        if (edit.IsEvent && IsContentOnlyEdit(edit, prior)) return SaveSeriesContent(edit, original, targets, scope, split, series);
        var editedAt = DesktopCalendarTime.Wall((edit.IsEvent ? edit.StartAtMillis : edit.DueAtMillis) ?? Anchor(original), edit.IsEvent ? edit.EventTimeZoneId : "");
        if (scope == RecurrenceScope.All && EpochDay(editedAt) == split)
            edit = ShiftEdit(edit, DayFromEpoch(targets.Min(InstanceDay)) + editedAt.TimeOfDay);
        ValidateItemEdit(edit);
        var firstAt = DesktopCalendarTime.Wall((edit.IsEvent ? edit.StartAtMillis : edit.DueAtMillis) ?? Anchor(original), edit.IsEvent ? edit.EventTimeZoneId : "");
        var oldTemplate = Templates.OfType<JsonObject>().FirstOrDefault(t => t["seriesId"]?.GetValue<string>() == series);
        var targetSeries = scope == RecurrenceScope.All ? series : Guid.NewGuid().ToString();
        var prototype = (JsonObject)original.DeepClone();
        var previousRoot = (JsonObject)root.DeepClone();
        try {
            if (oldTemplate != null) {
                if (scope == RecurrenceScope.All) Templates.Remove(oldTemplate);
                else { oldTemplate["endEpochDay"] = Math.Min(oldTemplate["endEpochDay"]?.GetValue<long>() ?? split - 1, split - 1); oldTemplate["recurrenceNeverEnds"] = false; }
            }
            if (scope == RecurrenceScope.CurrentAndFuture) TruncateInstanceRules(series, split);
            prototype["recurringSeriesId"] = edit.RecurrenceType == "NONE" ? null : targetSeries;
            if (edit.RecurrenceType != "NONE") SyncRecurringTemplate(prototype, edit, edit.EndAtMillis ?? edit.DueAtMillis ?? Anchor(original), null, targetSeries);
            var template = Templates.OfType<JsonObject>().FirstOrDefault(t => t["seriesId"]?.GetValue<string>() == targetSeries);
            var until = Math.Max(targets.Max(InstanceDay), EpochDay(firstAt.AddDays(62)));
            var dates = edit.RecurrenceType == "NONE" ? new List<DateTime> { firstAt.Date } : RecurringDates(template!, firstAt.Date, DayFromEpoch(until)).ToList();
            var historicalDays = Tasks.OfType<JsonObject>().Where(t => t["recurringSeriesId"]?.GetValue<string>() == series && !targets.Contains(t)).Select(InstanceDay).ToHashSet();
            dates = dates.Where(date => !historicalDays.Contains(EpochDay(date))).ToList();
            if (dates.Count == 0) throw new InvalidOperationException("所选日期范围内没有循环日期");
            var available = targets.ToList();
            long resultId = id;
            foreach (var date in dates) {
                var item = available.FirstOrDefault(t => InstanceDay(t) == EpochDay(date)) ?? available.FirstOrDefault();
                if (item == null) { item = (JsonObject)prototype.DeepClone(); ResetOccurrence(item); item["id"] = NextId(Tasks); Tasks.Add(item); }
                else available.Remove(item);
                var itemId = item["id"]!.GetValue<long>();
                item["recurringSeriesId"] = edit.RecurrenceType == "NONE" ? null : targetSeries;
                var occurrenceEdit = WithOccurrenceReminders(ShiftEdit(edit, date + firstAt.TimeOfDay), prior, item, id);
                SaveItemCore(occurrenceEdit, itemId, false);
                if (template != null) ApplyTemplateFields(item, template);
                item["reminderEnabled"] = occurrenceEdit.ReminderEnabled;
                RefreshOccurrenceReminders(item);
                if (date == dates[0]) resultId = itemId;
            }
            foreach (var stale in available) RemoveOccurrence(stale);
            // An explicit series edit replaces selected dates including earlier skips.
            foreach (var skip in root["recurringInstanceSkips"]!.AsArray().OfType<JsonObject>().Where(s => s["seriesId"]?.GetValue<string>() == series &&
                         (scope == RecurrenceScope.All || s["instanceEpochDay"]?.GetValue<long>() >= split)).ToList()) root["recurringInstanceSkips"]!.AsArray().Remove(skip);
            return resultId;
        } catch { root = previousRoot; throw; }
    }
    private static bool IsContentOnlyEdit(DesktopItemEdit edit, DesktopItemEdit prior) =>
        edit.StartAtMillis == prior.StartAtMillis && edit.EndAtMillis == prior.EndAtMillis && edit.AllDay == prior.AllDay &&
        edit.EventTimeZoneId == prior.EventTimeZoneId && edit.RecurrenceType == prior.RecurrenceType &&
        edit.RecurrenceWeekdays == prior.RecurrenceWeekdays && edit.RecurrenceEndEpochDay == prior.RecurrenceEndEpochDay &&
        edit.RecurrenceInterval == prior.RecurrenceInterval && edit.RecurrenceInvalidDatePolicy == prior.RecurrenceInvalidDatePolicy &&
        edit.RecurrenceMonthlyOrdinal == prior.RecurrenceMonthlyOrdinal && edit.RecurrenceMonthlyWeekday == prior.RecurrenceMonthlyWeekday;
    private long SaveSeriesContent(DesktopItemEdit edit, JsonObject original, List<JsonObject> targets, RecurrenceScope scope, long split, string series)
    {
        var template = Templates.OfType<JsonObject>().FirstOrDefault(value => value["seriesId"]?.GetValue<string>() == series);
        if (template == null) return SaveItemCore(edit, original["id"]!.GetValue<long>());
        var candidate = DayFromEpoch(scope == RecurrenceScope.All ? template["startEpochDay"]!.GetValue<long>() : split)
            .AddHours(template["dueHour"]?.GetValue<int>() ?? 0).AddMinutes(template["dueMinute"]?.GetValue<int>() ?? 0);
        var duration = template["eventDurationMinutes"]?.GetValue<int>() ?? 60;
        var anchorEdit = edit with { StartAtMillis = DesktopCalendarTime.Millis(candidate, edit.EventTimeZoneId),
            EndAtMillis = DesktopCalendarTime.Millis(candidate, edit.EventTimeZoneId) + duration * 60000L };
        var targetSeries = scope == RecurrenceScope.All ? series : Guid.NewGuid().ToString();
        if (scope == RecurrenceScope.CurrentAndFuture) {
            template["endEpochDay"] = split - 1; template["recurrenceNeverEnds"] = false; TruncateInstanceRules(series, split);
            foreach (var skip in root["recurringInstanceSkips"]!.AsArray().OfType<JsonObject>().Where(value => value["seriesId"]?.GetValue<string>() == series && value["instanceEpochDay"]?.GetValue<long>() >= split))
                skip["seriesId"] = targetSeries;
        }
        var prototype = (JsonObject)original.DeepClone(); prototype["recurringSeriesId"] = targetSeries;
        SyncRecurringTemplate(prototype, anchorEdit, anchorEdit.EndAtMillis!.Value, null, targetSeries);
        var updatedTemplate = Templates.OfType<JsonObject>().First(value => value["seriesId"]?.GetValue<string>() == targetSeries);
        var selectedId = original["id"]!.GetValue<long>();
        var prior = DesktopItemEdit.FromJson(original);
        foreach (var target in targets) {
            var anchor = Anchor(target);
            var at = DesktopItemEdit.FromJson(target);
            var occurrenceEdit = WithOccurrenceReminders(edit with { StartAtMillis = at.StartAtMillis, EndAtMillis = at.EndAtMillis }, prior, target, selectedId);
            SaveItemCore(occurrenceEdit, target["id"]!.GetValue<long>(), false);
            ApplyTemplateFields(target, updatedTemplate);
            target["reminderEnabled"] = occurrenceEdit.ReminderEnabled;
            RefreshOccurrenceReminders(target);
            target["recurringSeriesId"] = targetSeries; target["recurrenceAnchorDueAtMillis"] = anchor;
        }
        return original["id"]!.GetValue<long>();
    }
    private static DesktopItemEdit WithOccurrenceReminders(DesktopItemEdit edit, DesktopItemEdit prior, JsonObject target, long selectedId)
    {
        if (prior.ReminderEnabled && !edit.ReminderEnabled)
            return edit with { AbsoluteReminderTimes = Array.Empty<long>() };
        var selected = target["id"]!.GetValue<long>() == selectedId;
        var existing = DesktopItemEdit.FromJson(target);
        var absolute = selected ? edit.AbsoluteReminderTimes : existing.AbsoluteReminderTimes;
        return edit with {
            AbsoluteReminderTimes = absolute,
            ReminderEnabled = edit.ReminderEnabled || (!selected && existing.ReminderEnabled && absolute.Count > 0)
        };
    }
    private static void RefreshOccurrenceReminders(JsonObject item)
    {
        var reminders = DesktopCalendarTime.Triggers(DesktopItemEdit.FromJson(item));
        item["reminderAtMillis"] = reminders.Count > 0 ? reminders[0] : null;
        item["reminderEnabled"] = reminders.Count > 0;
    }
    private static DesktopItemEdit ShiftEdit(DesktopItemEdit edit, DateTime at)
    {
        if (!edit.IsEvent) return edit.DueAtMillis == null && edit.RecurrenceType == "NONE" ? edit : edit with { DueAtMillis = MillisAt(at) };
        var duration = edit.EndAtMillis!.Value - edit.StartAtMillis!.Value;
        var start = DesktopCalendarTime.Millis(at, edit.EventTimeZoneId);
        return edit with { StartAtMillis = start, EndAtMillis = edit.AllDay
            ? DesktopCalendarTime.Millis(at.AddDays(Math.Max(1, (int)Math.Round(duration / 86400000.0))), edit.EventTimeZoneId) : start + duration };
    }
    public void Delete(long id, RecurrenceScope scope) => RemoveSeriesRange(id, scope, false);
    public void Cancel(long id, RecurrenceScope scope) => RemoveSeriesRange(id, scope, true);
    private void RemoveSeriesRange(long id, RecurrenceScope scope, bool cancel)
    {
        var original = FindTask(id); if (original == null) return;
        var series = original["recurringSeriesId"]?.GetValue<string>();
        if (scope == RecurrenceScope.Current || string.IsNullOrEmpty(series)) { if (cancel) Cancel(id); else Delete(id); return; }
        var split = InstanceDay(original);
        foreach (var template in Templates.OfType<JsonObject>().Where(t => t["seriesId"]?.GetValue<string>() == series).ToList()) {
            if (scope == RecurrenceScope.All) Templates.Remove(template);
            else { template["endEpochDay"] = Math.Min(template["endEpochDay"]?.GetValue<long>() ?? split - 1, split - 1); template["recurrenceNeverEnds"] = false; }
        }
        if (scope == RecurrenceScope.CurrentAndFuture) TruncateInstanceRules(series, split);
        foreach (var item in Tasks.OfType<JsonObject>().Where(t => t["recurringSeriesId"]?.GetValue<string>() == series &&
                     t["completed"]?.GetValue<bool>() != true && t["canceled"]?.GetValue<bool>() != true &&
                     (scope == RecurrenceScope.All || InstanceDay(t) >= split)).ToList()) {
            if (cancel) Cancel(item["id"]!.GetValue<long>()); else RemoveOccurrence(item);
        }
    }
    private void TruncateInstanceRules(string series, long split)
    {
        foreach (var item in Tasks.OfType<JsonObject>().Where(t => t["recurringSeriesId"]?.GetValue<string>() == series && InstanceDay(t) < split))
            item["recurrenceEndEpochDay"] = Math.Min(item["recurrenceEndEpochDay"]?.GetValue<long>() ?? split - 1, split - 1);
    }
    public bool EnsureRecurringInstances(DateTime startDate, int horizonDays = 62)
    {
        var changed = false;
        var skips = root["recurringInstanceSkips"]!.AsArray().OfType<JsonObject>()
            .Select(skip => (skip["seriesId"]?.GetValue<string>(), skip["instanceEpochDay"]?.GetValue<long>() ?? -1)).ToHashSet();
        var bySeries = Tasks.OfType<JsonObject>().Where(item => item["recurringSeriesId"] != null)
            .GroupBy(item => item["recurringSeriesId"]!.GetValue<string>()).ToDictionary(group => group.Key, group => group.ToList());
        var nextId = NextId(Tasks);
        foreach (var template in Templates.OfType<JsonObject>().ToList()) {
            var series = template["seriesId"]?.GetValue<string>(); if (string.IsNullOrWhiteSpace(series)) continue;
            var existing = bySeries.GetValueOrDefault(series) ?? new List<JsonObject>();
            var occupied = existing.Select(InstanceDay).ToHashSet();
            foreach (var date in RecurringDates(template, startDate.Date, startDate.Date.AddDays(horizonDays))) {
                var day = EpochDay(date); if (occupied.Contains(day) || skips.Contains((series, day))) continue;
                var item = CreateRecurringOccurrence(template, existing.FirstOrDefault(), date, nextId);
                if (item == null) continue;
                nextId++;
                Tasks.Add(item); occupied.Add(day); changed = true;
            }
        }
        return changed;
    }
    private static JsonObject? CreateRecurringOccurrence(JsonObject template, JsonObject? prototype, DateTime date, long id)
    {
        var item = prototype?.DeepClone().AsObject() ?? new JsonObject();
        ApplyTemplateFields(item, template); ResetOccurrence(item);
        var time = date.AddHours(template["dueHour"]?.GetValue<int>() ?? 0).AddMinutes(template["dueMinute"]?.GetValue<int>() ?? 0);
        var zoneId = template["eventTimeZoneId"]?.GetValue<string>() ?? "";
        if (DesktopCalendarTime.Zone(zoneId).IsInvalidTime(DateTime.SpecifyKind(time, DateTimeKind.Unspecified))) return null;
        var millis = DesktopCalendarTime.Millis(time, zoneId);
        var isEvent = template["itemType"]?.GetValue<string>() == "EVENT";
        item["id"] = id; item["recurringSeriesId"] = template["seriesId"]!.DeepClone();
        item["startAtMillis"] = isEvent ? millis : null;
        var duration = template["eventDurationMinutes"]?.GetValue<int?>() ?? 60;
        item["endAtMillis"] = isEvent ? item["allDay"]?.GetValue<bool>() == true
            ? DesktopCalendarTime.Millis(time.AddDays(Math.Max(1, (int)Math.Round(duration / 1440.0))), zoneId)
            : millis + duration * 60000L : null;
        item["dueAtMillis"] = isEvent ? item["endAtMillis"]!.DeepClone() : JsonValue.Create(millis);
        item["recurrenceAnchorDueAtMillis"] = millis;
        RefreshOccurrenceReminders(item);
        return item;
    }
    internal static DateTime? NextPlanningOccurrence(TransientPlanningSlot slot, DateTime reminder)
    {
        if (slot.RecurrenceType == "NONE") return slot.StartAt >= reminder ? slot.StartAt : null;
        var from = reminder.Date > slot.StartAt.Date ? reminder.Date : slot.StartAt.Date;
        // Use the same matcher as replenishment. The bounded lookahead covers two
        // full intervals, including a skipped local-clock occurrence, without
        // scanning an indefinite course all the way to DateTime.MaxValue.
        var interval = Math.Max(1, slot.Interval);
        long lookahead = slot.RecurrenceType switch {
            "DAILY" => 2L * interval + 2,
            "WEEKLY" => 14L * interval + 7,
            "MONTHLY_DAY" or "MONTHLY" => 62L * interval + 31,
            "YEARLY_DATE" or "YEARLY" => 732L * interval + 366,
            _ => throw new InvalidOperationException("规划循环类型不受支持")
        };
        var to = from.AddDays(Math.Min(lookahead, (DateTime.MaxValue.Date - from).Days));
        if (slot.EndDate is { } end && end < to) to = end.Date;
        var template = new JsonObject {
            ["startEpochDay"] = EpochDay(slot.StartAt), ["endEpochDay"] = slot.EndDate is { } last ? EpochDay(last) : null,
            ["recurrenceNeverEnds"] = slot.EndDate == null, ["recurrenceType"] = slot.RecurrenceType,
            ["recurrenceInterval"] = interval, ["recurrenceWeekdays"] = slot.Weekdays,
            ["recurrenceMonthlyDay"] = slot.IntendedDay ?? slot.StartAt.Day,
            ["recurrenceYearlyMonth"] = slot.StartAt.Month, ["recurrenceYearlyDay"] = slot.IntendedDay ?? slot.StartAt.Day
        };
        foreach (var date in RecurringDates(template, from, to)) {
            var at = date + slot.StartAt.TimeOfDay;
            if (at >= reminder && !DesktopCalendarTime.Zone("").IsInvalidTime(DateTime.SpecifyKind(at, DateTimeKind.Unspecified))) return at;
        }
        return null;
    }
    private static void ApplyTemplateFields(JsonObject item, JsonObject template)
    {
        item["eventParticipantsJson"] = DesktopEventParticipants.Select(template["eventParticipantsJson"]?.GetValue<string>() ?? "[]", item["eventParticipantsJson"]?.GetValue<string>() ?? "[]");
        foreach (var key in new[] { "itemType", "title", "notes", "location", "accentColorHex", "countdownEnabled", "calendarId", "countdownPastMode", "hiddenFromBoard", "checkInEnabled", "checkInReminderOffsetsCsv", "multiSlotBundleId", "allDay", "groupId", "reminderOffsetsCsv", "reminderOffsetMinutes", "reminderDeliveryMode", "ringEnabled", "vibrateEnabled", "alarmMode", "eventTimeZoneId", "allDayReminderRulesJson", "eventBusyStatus", "eventVisibility" }.Concat(RecurrenceFields.Where(key => key != "recurrenceAnchorDueAtMillis" && key != "recurringSeriesId"))) item[key] = template[key]?.DeepClone();
        item["recurrenceEndEpochDay"] = template["recurrenceNeverEnds"]?.GetValue<bool>() == true ? null : template["endEpochDay"]?.DeepClone();
        // Android templates express enablement through configured reminder offsets.
        item["reminderEnabled"] = template["reminderEnabled"]?.GetValue<bool>() ?? (!string.IsNullOrWhiteSpace(template["reminderOffsetsCsv"]?.GetValue<string>()) ||
            !string.IsNullOrWhiteSpace(template["allDayReminderRulesJson"]?.GetValue<string>()) && template["allDayReminderRulesJson"]?.GetValue<string>() != "[]");
    }
    private static void ResetOccurrence(JsonObject item)
    {
        item["sharedEventId"] = "";
        // Absolute reminders belong to the existing occurrence, not its template.
        item["reminderAbsoluteTimesCsv"] = "";
        item["eventParticipantsJson"] = DesktopEventParticipants.Select(item["eventParticipantsJson"]?.GetValue<string>() ?? "[]", "[]");
        item["createdAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        foreach (var key in new[] { "completed", "canceled", "missed" }) item[key] = false;
        foreach (var key in new[] { "completedAtMillis", "canceledAtMillis", "missedAtMillis" }) item[key] = null;
        item["totalCheckInMinutes"] = 0;
    }
    private static IEnumerable<DateTime> RecurringDates(JsonObject template, DateTime from, DateTime to)
    {
        var first = template["startEpochDay"]?.GetValue<long>() ?? EpochDay(from);
        var last = template["recurrenceNeverEnds"]?.GetValue<bool>() == true ? EpochDay(to) : template["endEpochDay"]?.GetValue<long>() ?? EpochDay(to);
        var anchor = DayFromEpoch(first);
        var kind = template["recurrenceType"]?.GetValue<string>() ?? "NONE";
        var weekdays = (template["recurrenceWeekdays"]?.GetValue<string>() ?? "").Split(',').ToHashSet();
        var interval = Math.Max(1, template["recurrenceInterval"]?.GetValue<int>() ?? 1);
        var skipInvalid = template["recurrenceInvalidDatePolicy"]?.GetValue<string>() == "SKIP";
        for (var date = from.Date > anchor ? from.Date : anchor; date <= to && EpochDay(date) <= last;) {
            var weekday = ((int)date.DayOfWeek + 6) % 7 + 1;
            var days = (date - anchor).Days;
            var weeks = (days + ((int)anchor.DayOfWeek + 6) % 7) / 7;
            var months = (date.Year - anchor.Year) * 12 + date.Month - anchor.Month;
            var monthDay = template["recurrenceMonthlyDay"]?.GetValue<int?>() ?? anchor.Day;
            var yearDay = template["recurrenceYearlyDay"]?.GetValue<int?>() ?? anchor.Day;
            var matches = kind switch {
                "DAILY" => days % interval == 0,
                "WEEKLY" => weeks % interval == 0 && weekdays.Contains(weekday.ToString()),
                "MONTHLY_DAY" or "MONTHLY" => months % interval == 0 && date.Day == (skipInvalid ? monthDay : Math.Min(monthDay, DateTime.DaysInMonth(date.Year, date.Month))),
                "MONTHLY_NTH_WEEKDAY" => months % interval == 0 && weekday == template["recurrenceMonthlyWeekday"]?.GetValue<int?>() &&
                    (template["recurrenceMonthlyOrdinal"]?.GetValue<int?>() == -1 ? date.AddDays(7).Month != date.Month : (date.Day - 1) / 7 + 1 == template["recurrenceMonthlyOrdinal"]?.GetValue<int?>()),
                "YEARLY_DATE" or "YEARLY" => (date.Year - anchor.Year) % interval == 0 && date.Month == (template["recurrenceYearlyMonth"]?.GetValue<int?>() ?? anchor.Month) && date.Day == (skipInvalid ? yearDay : Math.Min(yearDay, DateTime.DaysInMonth(date.Year, date.Month))),
                "YEARLY_LUNAR_DATE" => (date.Year - anchor.Year) % interval == 0 && MatchesLunar(date, anchor),
                _ => false
            };
            if (matches) yield return date;
            if (date == DateTime.MaxValue.Date) yield break;
            date = date.AddDays(1);
        }
    }
    internal static (int Month, int Day) LunarMonthDay(DateTime date)
    {
        if (date < Lunar.MinSupportedDateTime || date > Lunar.MaxSupportedDateTime) throw new InvalidOperationException("农历循环支持1901至2100年");
        var month = Lunar.GetMonth(date); var leap = Lunar.GetLeapMonth(Lunar.GetYear(date));
        return (leap > 0 && month >= leap ? month - 1 : month, Lunar.GetDayOfMonth(date));
    }
    private static bool MatchesLunar(DateTime date, DateTime anchor)
    {
        if (date < Lunar.MinSupportedDateTime || date > Lunar.MaxSupportedDateTime || anchor < Lunar.MinSupportedDateTime || anchor > Lunar.MaxSupportedDateTime) return false;
        var target = LunarMonthDay(anchor);
        var year = Lunar.GetYear(date); var leap = Lunar.GetLeapMonth(year); var month = target.Month;
        var sourceLeap = Lunar.GetLeapMonth(Lunar.GetYear(anchor));
        var wasLeap = sourceLeap > 0 && Lunar.GetMonth(anchor) == sourceLeap;
        if (leap > 0 && month >= leap) month++;
        if (wasLeap && leap == target.Month + 1) month = leap;
        return Lunar.GetMonth(date) == month && Lunar.GetDayOfMonth(date) == target.Day;
    }
    private void SkipOccurrence(JsonObject item)
    {
        var series = item["recurringSeriesId"]?.GetValue<string>(); if (string.IsNullOrWhiteSpace(series)) return;
        var day = InstanceDay(item); var skips = root["recurringInstanceSkips"]!.AsArray();
        if (skips.OfType<JsonObject>().Any(skip => skip["seriesId"]?.GetValue<string>() == series && skip["instanceEpochDay"]?.GetValue<long>() == day)) return;
        skips.Add(new JsonObject { ["id"] = NextId(skips), ["seriesId"] = series, ["instanceEpochDay"] = day, ["createdAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
    }
    private void RemoveOccurrence(JsonObject item)
    {
        var id = item["id"]!.GetValue<long>();
        foreach (var (table, key) in new[] { ("todoGroupTags", "todoId"), ("eventCheckIns", "eventId") })
            foreach (var row in root[table]!.AsArray().OfType<JsonObject>().Where(row => row[key]?.GetValue<long>() == id).ToList()) root[table]!.AsArray().Remove(row);
        foreach (var row in root["planningNodes"]!.AsArray().OfType<JsonObject>())
            foreach (var key in new[] { "linkedTodoId", "linkedEndTodoId" }) if (row[key]?.GetValue<long?>() == id) row[key] = null;
        foreach (var row in root["planningLineMappings"]!.AsArray().OfType<JsonObject>())
            foreach (var key in new[] { "todoId", "eventId" }) if (row[key]?.GetValue<long?>() == id) { row[key] = null; row["status"] = "ORPHANED"; }
        Tasks.Remove(item);
    }
}
