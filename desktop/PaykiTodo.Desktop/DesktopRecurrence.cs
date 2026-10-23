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
    private static readonly string[] RecurrenceFields = { "recurrenceType", "recurrenceWeekdays", "recurrenceEndEpochDay", "recurrenceAnchorDueAtMillis", "recurringSeriesId", "recurrenceMonthlyOrdinal", "recurrenceMonthlyWeekday", "recurrenceMonthlyDay", "recurrenceYearlyMonth", "recurrenceYearlyDay" };
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
    private long InstanceDay(JsonObject item) => EpochDay(LocalTime(Anchor(item)));

    internal static DesktopItemEdit NormalizeItemEdit(DesktopItemEdit edit)
    {
        if (!edit.IsEvent || !edit.AllDay || edit.StartAtMillis == null || edit.EndAtMillis == null) return edit;
        var start = LocalTime(edit.StartAtMillis.Value).Date;
        var end = LocalTime(edit.EndAtMillis.Value);
        // Storage uses an exclusive midnight end, matching Android overlap queries.
        end = end.TimeOfDay == TimeSpan.Zero ? end.Date : end.Date.AddDays(1);
        if (end <= start) end = start.AddDays(1);
        return edit with { StartAtMillis = MillisAt(start), EndAtMillis = MillisAt(end) };
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
        var editedAt = LocalTime((edit.IsEvent ? edit.StartAtMillis : edit.DueAtMillis) ?? Anchor(original));
        if (scope == RecurrenceScope.All && EpochDay(editedAt) == split)
            edit = ShiftEdit(edit, DayFromEpoch(targets.Min(InstanceDay)) + editedAt.TimeOfDay);
        ValidateItemEdit(edit);
        var firstAt = LocalTime((edit.IsEvent ? edit.StartAtMillis : edit.DueAtMillis) ?? Anchor(original));
        var oldTemplate = Templates.OfType<JsonObject>().FirstOrDefault(t => t["seriesId"]?.GetValue<string>() == series);
        var targetSeries = scope == RecurrenceScope.All ? series : Guid.NewGuid().ToString();
        var prototype = (JsonObject)original.DeepClone();
        var previousRoot = (JsonObject)root.DeepClone();
        try {
            if (oldTemplate != null) {
                if (scope == RecurrenceScope.All) Templates.Remove(oldTemplate);
                else oldTemplate["endEpochDay"] = Math.Min(oldTemplate["endEpochDay"]?.GetValue<long>() ?? split - 1, split - 1);
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
                SaveItemCore(ShiftEdit(edit, date + firstAt.TimeOfDay), itemId, false);
                if (template != null) ApplyTemplateFields(item, template);
                if (date == dates[0]) resultId = itemId;
            }
            foreach (var stale in available) RemoveOccurrence(stale);
            // An explicit series edit replaces selected dates including earlier skips.
            foreach (var skip in root["recurringInstanceSkips"]!.AsArray().OfType<JsonObject>().Where(s => s["seriesId"]?.GetValue<string>() == series &&
                         (scope == RecurrenceScope.All || s["instanceEpochDay"]?.GetValue<long>() >= split)).ToList()) root["recurringInstanceSkips"]!.AsArray().Remove(skip);
            return resultId;
        } catch { root = previousRoot; throw; }
    }
    private static DesktopItemEdit ShiftEdit(DesktopItemEdit edit, DateTime at)
    {
        if (!edit.IsEvent) return edit.DueAtMillis == null && edit.RecurrenceType == "NONE" ? edit : edit with { DueAtMillis = MillisAt(at) };
        var duration = LocalTime(edit.EndAtMillis!.Value) - LocalTime(edit.StartAtMillis!.Value);
        return edit with { StartAtMillis = MillisAt(at), EndAtMillis = MillisAt(at + duration) };
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
            else template["endEpochDay"] = Math.Min(template["endEpochDay"]?.GetValue<long>() ?? split - 1, split - 1);
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
                var item = existing.FirstOrDefault()?.DeepClone().AsObject() ?? new JsonObject();
                ApplyTemplateFields(item, template); ResetOccurrence(item);
                var time = date.AddHours(template["dueHour"]?.GetValue<int>() ?? 0).AddMinutes(template["dueMinute"]?.GetValue<int>() ?? 0);
                var millis = MillisAt(time);
                var isEvent = template["itemType"]?.GetValue<string>() == "EVENT";
                item["id"] = nextId++; item["recurringSeriesId"] = series;
                item["startAtMillis"] = isEvent ? millis : null;
                item["endAtMillis"] = isEvent ? MillisAt(time.AddMinutes(template["eventDurationMinutes"]?.GetValue<int?>() ?? 60)) : null;
                item["dueAtMillis"] = isEvent ? item["endAtMillis"]!.DeepClone() : JsonValue.Create(millis);
                item["recurrenceAnchorDueAtMillis"] = millis;
                var offsets = DesktopItemEdit.FromJson(item).ReminderOffsets;
                var reminderAnchor = isEvent && item["allDay"]?.GetValue<bool>() == true ? MillisAt(time.Date.AddHours(9)) : millis;
                item["reminderAtMillis"] = offsets.Count > 0 && item["reminderEnabled"]?.GetValue<bool>() == true ? reminderAnchor - offsets.Max() * 60000L : null;
                Tasks.Add(item); occupied.Add(day); changed = true;
            }
        }
        return changed;
    }
    private static void ApplyTemplateFields(JsonObject item, JsonObject template)
    {
        foreach (var key in new[] { "itemType", "title", "notes", "location", "accentColorHex", "countdownEnabled", "calendarId", "countdownPastMode", "hiddenFromBoard", "checkInEnabled", "checkInReminderOffsetsCsv", "multiSlotBundleId", "allDay", "groupId", "reminderOffsetsCsv", "reminderOffsetMinutes", "reminderDeliveryMode", "ringEnabled", "vibrateEnabled", "alarmMode" }.Concat(RecurrenceFields.Where(key => key != "recurrenceAnchorDueAtMillis" && key != "recurringSeriesId"))) item[key] = template[key]?.DeepClone();
        item["recurrenceEndEpochDay"] = template["endEpochDay"]?.DeepClone();
        // Android templates express enablement through configured reminder offsets.
        item["reminderEnabled"] = template["reminderEnabled"]?.GetValue<bool>() ?? !string.IsNullOrWhiteSpace(template["reminderOffsetsCsv"]?.GetValue<string>());
    }
    private static void ResetOccurrence(JsonObject item)
    {
        item["createdAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        foreach (var key in new[] { "completed", "canceled", "missed" }) item[key] = false;
        foreach (var key in new[] { "completedAtMillis", "canceledAtMillis", "missedAtMillis" }) item[key] = null;
        item["totalCheckInMinutes"] = 0;
    }
    private static IEnumerable<DateTime> RecurringDates(JsonObject template, DateTime from, DateTime to)
    {
        var first = template["startEpochDay"]?.GetValue<long>() ?? EpochDay(from);
        var last = template["endEpochDay"]?.GetValue<long>() ?? first;
        var anchor = DayFromEpoch(first);
        var kind = template["recurrenceType"]?.GetValue<string>() ?? "NONE";
        var weekdays = (template["recurrenceWeekdays"]?.GetValue<string>() ?? "").Split(',').ToHashSet();
        for (var date = from.Date > anchor ? from.Date : anchor; date <= to && EpochDay(date) <= last; date = date.AddDays(1)) {
            var weekday = ((int)date.DayOfWeek + 6) % 7 + 1;
            var matches = kind switch {
                "DAILY" => true,
                "WEEKLY" => weekdays.Contains(weekday.ToString()),
                "MONTHLY_DAY" or "MONTHLY" => date.Day == Math.Min(template["recurrenceMonthlyDay"]?.GetValue<int?>() ?? anchor.Day, DateTime.DaysInMonth(date.Year, date.Month)),
                "MONTHLY_NTH_WEEKDAY" => weekday == template["recurrenceMonthlyWeekday"]?.GetValue<int?>() &&
                    (template["recurrenceMonthlyOrdinal"]?.GetValue<int?>() == -1 ? date.AddDays(7).Month != date.Month : (date.Day - 1) / 7 + 1 == template["recurrenceMonthlyOrdinal"]?.GetValue<int?>()),
                "YEARLY_DATE" or "YEARLY" => date.Month == (template["recurrenceYearlyMonth"]?.GetValue<int?>() ?? anchor.Month) && date.Day == Math.Min(template["recurrenceYearlyDay"]?.GetValue<int?>() ?? anchor.Day, DateTime.DaysInMonth(date.Year, date.Month)),
                "YEARLY_LUNAR_DATE" => MatchesLunar(date, anchor),
                _ => false
            };
            if (matches) yield return date;
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
