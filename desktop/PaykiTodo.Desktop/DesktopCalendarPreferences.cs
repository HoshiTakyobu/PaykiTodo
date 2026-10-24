using System.Globalization;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace PaykiTodo.Desktop;

internal sealed record AllDayReminderRule(int DaysBefore, int MinuteOfDay);

internal static class DesktopCalendarTime
{
    public static TimeZoneInfo Zone(string? id)
    {
        if (string.IsNullOrWhiteSpace(id)) return TimeZoneInfo.Local;
        try { return TimeZoneInfo.FindSystemTimeZoneById(id); }
        catch (TimeZoneNotFoundException) { throw new InvalidOperationException("时区不存在，请重新选择。"); }
    }
    public static string IanaId(TimeZoneInfo zone) => TimeZoneInfo.TryConvertWindowsIdToIanaId(zone.Id, out var iana) ? iana : zone.Id;
    public static DateTime Wall(long value, string? zoneId = null) => TimeZoneInfo.ConvertTime(DateTimeOffset.FromUnixTimeMilliseconds(value), Zone(zoneId)).DateTime;
    public static long Millis(DateTime wall, string? zoneId = null)
    {
        wall = DateTime.SpecifyKind(wall, DateTimeKind.Unspecified);
        var zone = Zone(zoneId);
        if (zone.IsInvalidTime(wall)) throw new InvalidOperationException("该时区在所选日期没有这个钟点，请调整开始或结束时间。");
        return new DateTimeOffset(TimeZoneInfo.ConvertTimeToUtc(wall, zone)).ToUnixTimeMilliseconds();
    }
    public static IReadOnlyList<AllDayReminderRule> ParseRules(string json)
    {
        if (string.IsNullOrWhiteSpace(json)) return Array.Empty<AllDayReminderRule>();
        try {
            using var parsed = JsonDocument.Parse(json);
            if (parsed.RootElement.ValueKind != JsonValueKind.Array) throw new FormatException();
            var rules = parsed.RootElement.EnumerateArray().Select(value => new AllDayReminderRule(
                value.GetProperty("daysBefore").GetInt32(), value.GetProperty("minuteOfDay").GetInt32())).ToArray();
            if (rules.Any(value => value.DaysBefore is < 0 or > 365 || value.MinuteOfDay is < 0 or > 1439)) throw new FormatException();
            return rules.Distinct().OrderBy(value => value.DaysBefore).ThenBy(value => value.MinuteOfDay).ToArray();
        } catch (Exception error) when (error is JsonException or FormatException or InvalidOperationException or KeyNotFoundException) {
            throw new InvalidOperationException("全天提醒格式无效，请重新设置。");
        }
    }
    public static string RulesJson(IEnumerable<AllDayReminderRule> rules) => JsonSerializer.Serialize(rules.Distinct().Select(value => new { daysBefore = value.DaysBefore, minuteOfDay = value.MinuteOfDay }));
    public static string RulesText(string json) => string.Join("；", ParseRules(json).Select(value =>
        (value.DaysBefore == 0 ? "当天" : $"提前{value.DaysBefore}天") + " " + TimeSpan.FromMinutes(value.MinuteOfDay).ToString("hh\\:mm", CultureInfo.InvariantCulture)));
    public static IReadOnlyList<long> Triggers(DesktopItemEdit edit)
    {
        if (!edit.ReminderEnabled) return Array.Empty<long>();
        var at = edit.IsEvent ? edit.StartAtMillis : edit.DueAtMillis;
        if (at is null or long.MaxValue) return Array.Empty<long>();
        if (edit.IsEvent && edit.AllDay && !string.IsNullOrWhiteSpace(edit.AllDayReminderRulesJson)) {
            var date = Wall(at.Value, edit.EventTimeZoneId).Date;
            return ParseRules(edit.AllDayReminderRulesJson).Select(rule => Millis(date.AddDays(-rule.DaysBefore).AddMinutes(rule.MinuteOfDay), edit.EventTimeZoneId)).Distinct().Order().ToArray();
        }
        var anchor = edit.IsEvent && edit.AllDay ? Millis(Wall(at.Value, edit.EventTimeZoneId).Date.AddHours(9), edit.EventTimeZoneId) : at.Value;
        return edit.ReminderOffsets.Select(offset => anchor - offset * 60000L).Distinct().Order().ToArray();
    }
}

internal sealed partial class DesktopSnapshotDocument
{
    public int CalendarDefaultDurationMinutes => Math.Clamp(Settings["calendarDefaultDurationMinutes"]?.GetValue<int>() ?? 30, 15, 1440);
    public string CalendarTimedDefaultRemindersCsv => Settings["calendarTimedDefaultRemindersCsv"]?.GetValue<string>() ?? "15";
    public string CalendarAllDayDefaultRemindersJson => Settings["calendarAllDayDefaultRemindersJson"]?.GetValue<string>() ?? "[{\"daysBefore\":0,\"minuteOfDay\":480}]";
    public string CalendarDisplayTimeZoneId => Settings["calendarDisplayTimeZoneId"]?.GetValue<string>() ?? "";
    public string CalendarAuxiliaryTimeZoneId => Settings["calendarAuxiliaryTimeZoneId"]?.GetValue<string>() ?? "";
    public DesktopItemEdit CopyDraft(long id)
    {
        var source = EditFor(id) ?? throw new InvalidOperationException("事项不存在");
        return source with { RecurrenceType = "NONE", RecurrenceWeekdays = "", RecurrenceEndEpochDay = null,
            RecurrenceInterval = 1, RecurrenceMonthlyOrdinal = null, RecurrenceMonthlyWeekday = null,
            CalendarId = IsSharedCalendar(source.CalendarId) ? "default" : source.CalendarId,
            CountdownEnabled = false, CountdownPastMode = "AUTO", AdditionalTimeSlots = Array.Empty<DesktopTimeSlot>() };
    }
}
