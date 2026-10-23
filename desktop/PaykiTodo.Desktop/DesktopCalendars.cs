using System.Text.Json.Nodes;

namespace PaykiTodo.Desktop;

internal sealed record DesktopCalendar(string Id, string Name, string ColorHex);

internal sealed partial class DesktopSnapshotDocument
{
    public IReadOnlyList<DesktopCalendar> Calendars => root["calendars"]!.AsArray().OfType<JsonObject>()
        .OrderBy(value => value["sortOrder"]?.GetValue<int>() ?? 0)
        .Select(value => new DesktopCalendar(value["id"]!.GetValue<string>(), value["name"]?.GetValue<string>() ?? "日历", value["colorHex"]?.GetValue<string>() ?? "#3370FF")).ToList();

    public string SaveCalendar(string? id, string name, string color)
    {
        if (string.IsNullOrWhiteSpace(name)) throw new InvalidOperationException("请填写日历名称");
        if (!System.Text.RegularExpressions.Regex.IsMatch(color, "^#[0-9a-fA-F]{6}$")) throw new InvalidOperationException("请选择有效颜色");
        var calendars = root["calendars"]!.AsArray();
        var calendar = calendars.OfType<JsonObject>().FirstOrDefault(value => value["id"]?.GetValue<string>() == id);
        if (calendar == null) { calendar = new JsonObject { ["id"] = id ?? Guid.NewGuid().ToString(), ["sortOrder"] = calendars.Count }; calendars.Add(calendar); }
        calendar["name"] = name.Trim(); calendar["colorHex"] = color;
        return calendar["id"]!.GetValue<string>();
    }

    public void DeleteCalendar(string id)
    {
        if (id == "default") throw new InvalidOperationException("常规日历需要保留");
        foreach (var item in Tasks.OfType<JsonObject>().Concat(root["templates"]!.AsArray().OfType<JsonObject>()))
            if (item["calendarId"]?.GetValue<string>() == id) item["calendarId"] = "default";
        RemapScheduleCalendars(calendarId => calendarId == id ? "default" : calendarId);
        var calendar = root["calendars"]!.AsArray().OfType<JsonObject>().FirstOrDefault(value => value["id"]?.GetValue<string>() == id);
        if (calendar != null) root["calendars"]!.AsArray().Remove(calendar);
    }

    private void RemapScheduleCalendars(Func<string, string> resolve)
    {
        foreach (var template in root["scheduleTemplates"]!.AsArray().OfType<JsonObject>()) {
            if (template["payloadJson"] is not JsonValue payload || !payload.TryGetValue<string>(out var raw)) continue;
            try {
                var json = JsonNode.Parse(raw);
                if (json?["entries"] is not JsonArray entries) continue;
                foreach (var entry in entries.OfType<JsonObject>()) entry["calendarId"] = resolve(entry["calendarId"]?.GetValue<string>() ?? "default");
                template["payloadJson"] = json.ToJsonString();
            } catch (System.Text.Json.JsonException) { /* Retain unrecognized legacy payloads. */ }
        }
    }

    public int CountdownPastDays => Math.Clamp(Settings["countdownPastDays"]?.GetValue<int>() ?? 7, 0, 365);
    private static DateTime CountdownTarget(DesktopItem item) => DateTimeOffset.FromUnixTimeMilliseconds(item.IsEvent ? item.StartAtMillis ?? item.DueAtMillis : item.DueAtMillis).LocalDateTime;
    public bool CountdownVisible(DesktopItem item, DateTime now)
    {
        if (!item.CountdownEnabled || item.Canceled || HiddenOnBoard(item.Id) || item.CountdownPastMode == "HIDE" || (!item.IsEvent && item.DueAtMillis == long.MaxValue)) return false;
        return item.CountdownPastMode == "KEEP" || !item.Completed && CountdownTarget(item).Date >= now.Date.AddDays(-CountdownPastDays);
    }
    public bool CountdownNeedsDecision(DesktopItem item, DateTime now)
    {
        if (!item.CountdownEnabled || item.Canceled || item.CountdownPastMode != "AUTO") return false;
        var ended = item.IsEvent ? item.EndAtMillis : item.Completed ? item.CompletedAtMillis : null;
        return ended is { } time && DateTimeOffset.FromUnixTimeMilliseconds(time).LocalDateTime <= now &&
            DateTimeOffset.FromUnixTimeMilliseconds(time).LocalDateTime.Date >= now.Date.AddDays(-CountdownPastDays);
    }
    public void SetCountdownPastMode(long id, string mode)
    {
        if (mode is not ("AUTO" or "KEEP" or "HIDE")) throw new ArgumentException("无效的展示规则");
        if (FindTask(id) is { } item) item["countdownPastMode"] = mode;
    }
}
