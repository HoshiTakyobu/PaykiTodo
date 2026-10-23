using System.Text.Json.Nodes;

namespace PaykiTodo.Desktop;

internal sealed partial class DesktopSnapshotDocument
{
    public bool EnsureRecurringInstances(DateTime startDate, int horizonDays = 62)
    {
        var changed = false;
        var skips = root["recurringInstanceSkips"]!.AsArray().OfType<JsonObject>()
            .Select(skip => (skip["seriesId"]?.GetValue<string>(), skip["instanceEpochDay"]?.GetValue<long>() ?? -1)).ToHashSet();
        foreach (var template in Templates.OfType<JsonObject>().ToList()) {
            var series = template["seriesId"]?.GetValue<string>(); if (string.IsNullOrWhiteSpace(series)) continue;
            var source = Tasks.OfType<JsonObject>().FirstOrDefault(item => item["recurringSeriesId"]?.GetValue<string>() == series);
            if (source == null) continue;
            var occupied = Tasks.OfType<JsonObject>().Where(item => item["recurringSeriesId"]?.GetValue<string>() == series)
                .Select(item => EpochDay(DateTimeOffset.FromUnixTimeMilliseconds(item["startAtMillis"]?.GetValue<long?>() ?? item["dueAtMillis"]!.GetValue<long>()).LocalDateTime)).ToHashSet();
            var first = template["startEpochDay"]?.GetValue<long>() ?? EpochDay(startDate);
            var last = template["endEpochDay"]?.GetValue<long>() ?? first;
            var kind = template["recurrenceType"]?.GetValue<string>() ?? "NONE";
            var anchor = DateTime.UnixEpoch.AddDays(first);
            for (var date = startDate.Date; date <= startDate.Date.AddDays(horizonDays); date = date.AddDays(1)) {
                var day = EpochDay(date); if (day < first || day > last || occupied.Contains(day) || skips.Contains((series, day))) continue;
                var weekday = (int)date.DayOfWeek == 0 ? 7 : (int)date.DayOfWeek;
                var matches = kind switch {
                    "DAILY" => true,
                    "WEEKLY" => (template["recurrenceWeekdays"]?.GetValue<string>() ?? "").Split(',').Contains(weekday.ToString()),
                    "MONTHLY_DAY" or "MONTHLY" => date.Day == (template["recurrenceMonthlyDay"]?.GetValue<int?>() ?? anchor.Day),
                    "MONTHLY_NTH_WEEKDAY" => weekday == template["recurrenceMonthlyWeekday"]?.GetValue<int?>() &&
                        (template["recurrenceMonthlyOrdinal"]?.GetValue<int?>() == -1 ? date.AddDays(7).Month != date.Month : (date.Day - 1) / 7 + 1 == template["recurrenceMonthlyOrdinal"]?.GetValue<int?>()),
                    "YEARLY_DATE" or "YEARLY" => date.Month == (template["recurrenceYearlyMonth"]?.GetValue<int?>() ?? anchor.Month) && date.Day == (template["recurrenceYearlyDay"]?.GetValue<int?>() ?? anchor.Day),
                    _ => false
                };
                if (!matches) continue;
                var item = (JsonObject)source.DeepClone();
                foreach (var key in new[] { "title", "notes", "location", "accentColorHex", "countdownEnabled", "calendarId", "countdownPastMode", "hiddenFromBoard", "checkInEnabled", "checkInReminderOffsetsCsv", "allDay", "groupId", "reminderOffsetsCsv", "reminderDeliveryMode", "ringEnabled", "vibrateEnabled", "alarmMode" })
                    if (template[key] != null) item[key] = template[key]!.DeepClone();
                var time = date.AddHours(template["dueHour"]?.GetValue<int>() ?? 0).AddMinutes(template["dueMinute"]?.GetValue<int>() ?? 0);
                var millis = new DateTimeOffset(time).ToUnixTimeMilliseconds();
                var isEvent = template["itemType"]?.GetValue<string>() == "EVENT";
                item["id"] = NextId(Tasks); item["createdAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                item["completed"] = false; item["canceled"] = false; item["missed"] = false;
                item["completedAtMillis"] = null; item["canceledAtMillis"] = null; item["missedAtMillis"] = null;
                item["startAtMillis"] = isEvent ? millis : null;
                item["endAtMillis"] = isEvent ? millis + (template["eventDurationMinutes"]?.GetValue<int?>() ?? 60) * 60000L : null;
                item["dueAtMillis"] = isEvent ? item["endAtMillis"]!.DeepClone() : JsonValue.Create(millis);
                item["recurrenceAnchorDueAtMillis"] = item["dueAtMillis"]!.DeepClone(); item["totalCheckInMinutes"] = 0;
                var offsets = (item["reminderOffsetsCsv"]?.GetValue<string>() ?? "").Split(',').Select(value => int.TryParse(value, out var offset) ? offset : -1).Where(value => value >= 0).ToList();
                item["reminderAtMillis"] = offsets.Count > 0 && item["reminderEnabled"]?.GetValue<bool>() == true ? millis - offsets.Max() * 60000L : null;
                Tasks.Add(item); changed = true;
            }
        }
        return changed;
    }
    private static long EpochDay(DateTime date) => (long)(date.Date - DateTime.UnixEpoch.Date).TotalDays;
    private void SkipOccurrence(JsonObject item)
    {
        var series = item["recurringSeriesId"]?.GetValue<string>(); if (string.IsNullOrWhiteSpace(series)) return;
        var at = item["startAtMillis"]?.GetValue<long?>() ?? item["dueAtMillis"]!.GetValue<long>();
        if (at == long.MaxValue) return;
        var day = EpochDay(DateTimeOffset.FromUnixTimeMilliseconds(at).LocalDateTime);
        var skips = root["recurringInstanceSkips"]!.AsArray();
        if (skips.OfType<JsonObject>().Any(skip => skip["seriesId"]?.GetValue<string>() == series && skip["instanceEpochDay"]?.GetValue<long>() == day)) return;
        skips.Add(new JsonObject { ["id"] = NextId(skips), ["seriesId"] = series, ["instanceEpochDay"] = day, ["createdAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
    }
}
