using System.Text.Json.Nodes;

namespace PaykiTodo.Desktop;

internal sealed record DesktopCalendar(string Id, string Name, string ColorHex)
{
    public string Description { get; init; } = "";
    public string OwnerAccount { get; init; } = "";
    public string ShareMode { get; init; } = "PRIVATE";
    public string MembersJson { get; init; } = "[]";
    public string SharedRole { get; init; } = "";
    public long SharedRevision { get; init; }
    public string SharedServerUrl { get; init; } = "";
}

internal sealed partial class DesktopSnapshotDocument
{
    public IReadOnlyList<DesktopCalendar> Calendars => root["calendars"]!.AsArray().OfType<JsonObject>()
        .OrderBy(value => value["sortOrder"]?.GetValue<int>() ?? 0)
        .Select(value => new DesktopCalendar(value["id"]!.GetValue<string>(), value["name"]?.GetValue<string>() ?? "日历", value["colorHex"]?.GetValue<string>() ?? "#3370FF") {
            Description = value["description"]?.GetValue<string>() ?? "", OwnerAccount = value["ownerAccount"]?.GetValue<string>() ?? "",
            ShareMode = value["shareMode"]?.GetValue<string>() ?? "PRIVATE", MembersJson = value["membersJson"]?.GetValue<string>() ?? "[]",
            SharedRole = value["sharedRole"]?.GetValue<string>() ?? "", SharedRevision = value["sharedRevision"]?.GetValue<long>() ?? 0,
            SharedServerUrl = value["sharedServerUrl"]?.GetValue<string>() ?? ""
        }).ToList();

    public string SaveCalendar(string? id, string name, string color, string? description = null, string? ownerAccount = null, string? shareMode = null, string? membersJson = null)
    {
        if (string.IsNullOrWhiteSpace(name)) throw new InvalidOperationException("请填写日历名称");
        if (!System.Text.RegularExpressions.Regex.IsMatch(color, "^#[0-9a-fA-F]{6}$")) throw new InvalidOperationException("请选择有效颜色");
        var calendars = root["calendars"]!.AsArray();
        var calendar = calendars.OfType<JsonObject>().FirstOrDefault(value => value["id"]?.GetValue<string>() == id);
        var previousColor = calendar?["colorHex"]?.GetValue<string>();
        if (calendar == null) { calendar = new JsonObject { ["id"] = id ?? Guid.NewGuid().ToString(), ["sortOrder"] = calendars.Count }; calendars.Add(calendar); }
        calendar["name"] = name.Trim(); calendar["colorHex"] = color;
        if (description != null) calendar["description"] = description.Trim(); else calendar["description"] ??= "";
        if (ownerAccount != null) calendar["ownerAccount"] = ownerAccount; else calendar["ownerAccount"] ??= "";
        if (shareMode != null) calendar["shareMode"] = shareMode; else calendar["shareMode"] ??= "PRIVATE";
        if (membersJson != null) calendar["membersJson"] = membersJson; else calendar["membersJson"] ??= "[]";
        if (previousColor != null && !previousColor.Equals(color, StringComparison.OrdinalIgnoreCase)) {
            void Recolor(JsonObject entry) {
                if (entry["calendarId"]?.GetValue<string>() == id && (entry["accentColorHex"] == null || string.Equals(entry["accentColorHex"]?.GetValue<string>(), previousColor, StringComparison.OrdinalIgnoreCase))) entry["accentColorHex"] = color;
            }
            foreach (var item in (root["tasks"] as JsonArray ?? new JsonArray()).OfType<JsonObject>().Concat((root["templates"] as JsonArray ?? new JsonArray()).OfType<JsonObject>()).Where(i => i["itemType"]?.GetValue<string>() == "EVENT")) Recolor(item);
            foreach (var template in (root["scheduleTemplates"] as JsonArray ?? new JsonArray()).OfType<JsonObject>()) {
                try { var json = JsonNode.Parse(template["payloadJson"]?.GetValue<string>() ?? "{}"); if (json?["entries"] is JsonArray entries) {
                    foreach (var entry in entries.OfType<JsonObject>()) Recolor(entry); template["payloadJson"] = json.ToJsonString();
                } } catch (System.Text.Json.JsonException) { }
            }
        }
        return calendar["id"]!.GetValue<string>();
    }
    public void MoveCalendar(string id, int direction)
    {
        var sorted = root["calendars"]!.AsArray().OfType<JsonObject>().OrderBy(value => value["sortOrder"]?.GetValue<int>() ?? 0).ToList();
        var index = sorted.FindIndex(value => value["id"]?.GetValue<string>() == id); var target = index + Math.Sign(direction);
        if (index < 0 || target < 0 || target >= sorted.Count) return;
        (sorted[index], sorted[target]) = (sorted[target], sorted[index]);
        for (var i = 0; i < sorted.Count; i++) sorted[i]["sortOrder"] = i;
    }

    public void DeleteCalendar(string id)
    {
        if (id == "default") throw new InvalidOperationException("常规日历需要保留");
        if (IsSharedCalendar(id)) {
            foreach (var item in Tasks.OfType<JsonObject>().Where(value => value["calendarId"]?.GetValue<string>() == id).ToList()) Tasks.Remove(item);
            foreach (var template in Templates.OfType<JsonObject>().Where(value => value["calendarId"]?.GetValue<string>() == id).ToList()) Templates.Remove(template);
            var cached = SharedCalendar(id); if (cached != null) root["calendars"]!.AsArray().Remove(cached);
            if (Settings["hiddenCalendarIds"] is JsonArray ids) foreach (var value in ids.Where(value => value?.GetValue<string>() == id).ToList()) ids.Remove(value);
            return;
        }
        foreach (var item in Tasks.OfType<JsonObject>().Concat(root["templates"]!.AsArray().OfType<JsonObject>()))
            if (item["calendarId"]?.GetValue<string>() == id) item["calendarId"] = "default";
        RemapScheduleCalendars(calendarId => calendarId == id ? "default" : calendarId);
        var calendar = root["calendars"]!.AsArray().OfType<JsonObject>().FirstOrDefault(value => value["id"]?.GetValue<string>() == id);
        if (calendar != null) root["calendars"]!.AsArray().Remove(calendar);
        if (Settings["hiddenCalendarIds"] is JsonArray hidden)
            foreach (var value in hidden.Where(value => value?.GetValue<string>() == id).ToList()) hidden.Remove(value);
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
