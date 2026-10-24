using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json.Nodes;

namespace PaykiTodo.Desktop;

internal sealed class SharedCalendarConflictException(long revision) : InvalidOperationException("共享日历已有其他修改，请保留本机版本并查看云端版本后再决定。")
{
    public long Revision { get; } = revision;
}

internal sealed partial class DesktopSnapshotDocument
{
    internal static bool IsSharedCalendar(string? id) => id?.StartsWith("shared:", StringComparison.Ordinal) == true;
    private static void StripSharedData(JsonObject data)
    {
        var removedIds = (data["tasks"] as JsonArray ?? new()).OfType<JsonObject>().Where(value => IsSharedCalendar(value["calendarId"]?.GetValue<string>())).Select(value => value["id"]?.GetValue<long>() ?? 0).ToHashSet();
        var sharedSeries = (data["templates"] as JsonArray ?? new()).OfType<JsonObject>().Where(value => IsSharedCalendar(value["calendarId"]?.GetValue<string>())).Select(value => value["seriesId"]?.GetValue<string>() ?? "")
            .Concat((data["tasks"] as JsonArray ?? new()).OfType<JsonObject>().Where(value => IsSharedCalendar(value["calendarId"]?.GetValue<string>())).Select(value => value["recurringSeriesId"]?.GetValue<string>() ?? "")).ToHashSet();
        foreach (var key in new[] { "tasks", "templates", "calendars" })
            if (data[key] is JsonArray rows) foreach (var row in rows.OfType<JsonObject>().Where(value => IsSharedCalendar(value[key == "calendars" ? "id" : "calendarId"]?.GetValue<string>())).ToList()) rows.Remove(row);
        foreach (var key in new[] { "eventCheckIns", "todoGroupTags", "reminderChainLogs" })
            if (data[key] is JsonArray rows) foreach (var row in rows.OfType<JsonObject>().Where(value => removedIds.Contains(value["eventId"]?.GetValue<long>() ?? value["itemId"]?.GetValue<long>() ?? value["todoId"]?.GetValue<long>() ?? 0)).ToList()) rows.Remove(row);
        if (data["recurringInstanceSkips"] is JsonArray skips) foreach (var skip in skips.OfType<JsonObject>().Where(value => sharedSeries.Contains(value["seriesId"]?.GetValue<string>() ?? "")).ToList()) skips.Remove(skip);
        foreach (var row in (data["planningNodes"] as JsonArray ?? new()).OfType<JsonObject>())
            foreach (var key in new[] { "linkedTodoId", "linkedEndTodoId" }) if (removedIds.Contains(row[key]?.GetValue<long>() ?? 0)) row[key] = null;
        foreach (var row in (data["planningLineMappings"] as JsonArray ?? new()).OfType<JsonObject>())
            foreach (var key in new[] { "todoId", "eventId" }) if (removedIds.Contains(row[key]?.GetValue<long>() ?? 0)) { row[key] = null; row["status"] = "ORPHANED"; }
    }
    public void PreserveSharedData(DesktopSnapshotDocument local)
    {
        var personalSeries = Tasks.OfType<JsonObject>().Where(value => !IsSharedCalendar(value["calendarId"]?.GetValue<string>())).Select(value => value["recurringSeriesId"]?.GetValue<string>())
            .Concat(Templates.OfType<JsonObject>().Where(value => !IsSharedCalendar(value["calendarId"]?.GetValue<string>())).Select(value => value["seriesId"]?.GetValue<string>())).Where(value => !string.IsNullOrEmpty(value)).ToHashSet();
        var localSharedSeries = local.Tasks.OfType<JsonObject>().Where(value => IsSharedCalendar(value["calendarId"]?.GetValue<string>())).Select(value => value["recurringSeriesId"]?.GetValue<string>())
            .Concat(local.Templates.OfType<JsonObject>().Where(value => IsSharedCalendar(value["calendarId"]?.GetValue<string>())).Select(value => value["seriesId"]?.GetValue<string>())).Where(value => !string.IsNullOrEmpty(value)).ToHashSet();
        if (personalSeries.Overlaps(localSharedSeries)) throw new InvalidOperationException("个人副本的循环标识与本机共享日历冲突，已保留本机数据，请检查副本来源。");
        var oldSharedIds = Tasks.OfType<JsonObject>().Where(value => IsSharedCalendar(value["calendarId"]?.GetValue<string>())).Select(value => value["id"]!.GetValue<long>()).ToHashSet();
        var remap = new Dictionary<long, long>();
        foreach (var key in new[] { "tasks", "templates", "calendars" }) {
            var rows = root[key]!.AsArray(); var idKey = key == "calendars" ? "id" : "calendarId";
            foreach (var row in rows.OfType<JsonObject>().Where(value => IsSharedCalendar(value[idKey]?.GetValue<string>())).ToList()) rows.Remove(row);
            foreach (var row in local.root[key]!.AsArray().OfType<JsonObject>().Where(value => IsSharedCalendar(value[idKey]?.GetValue<string>()))) {
                var clone = (JsonObject)row.DeepClone();
                if (key is "tasks" or "templates" && rows.OfType<JsonObject>().Any(value => value["id"]?.GetValue<long>() == clone["id"]?.GetValue<long>())) clone["id"] = NextId(rows);
                if (key == "tasks") { remap[row["id"]!.GetValue<long>()] = clone["id"]!.GetValue<long>(); clone["groupId"] = DefaultGroupId(); }
                if (key == "templates") clone["groupId"] = DefaultGroupId();
                rows.Add(clone);
            }
        }
        foreach (var (table, field) in new[] { ("eventCheckIns", "eventId"), ("todoGroupTags", "todoId"), ("reminderChainLogs", "todoId") }) {
            var rows = root[table]!.AsArray();
            foreach (var row in rows.OfType<JsonObject>().Where(value => oldSharedIds.Contains(value[field]?.GetValue<long>() ?? 0)).ToList()) rows.Remove(row);
            foreach (var row in local.root[table]!.AsArray().OfType<JsonObject>().Where(value => remap.ContainsKey(value[field]?.GetValue<long>() ?? 0))) {
                var clone = (JsonObject)row.DeepClone(); clone[field] = remap[row[field]!.GetValue<long>()];
                if (clone["id"] != null && rows.OfType<JsonObject>().Any(value => value["id"]?.GetValue<long>() == clone["id"]?.GetValue<long>())) clone["id"] = NextId(rows);
                rows.Add(clone);
            }
        }
        var series = local.Templates.OfType<JsonObject>().Where(value => IsSharedCalendar(value["calendarId"]?.GetValue<string>())).Select(value => value["seriesId"]!.GetValue<string>()).ToHashSet();
        var skips = root["recurringInstanceSkips"]!.AsArray();
        foreach (var skip in skips.OfType<JsonObject>().Where(value => series.Contains(value["seriesId"]?.GetValue<string>() ?? "")).ToList()) skips.Remove(skip);
        foreach (var skip in local.root["recurringInstanceSkips"]!.AsArray().OfType<JsonObject>().Where(value => series.Contains(value["seriesId"]?.GetValue<string>() ?? ""))) {
            var clone = (JsonObject)skip.DeepClone(); clone["id"] = NextId(skips); skips.Add(clone);
        }
    }
    internal JsonObject? SharedCalendar(string id) => root["calendars"]!.AsArray().OfType<JsonObject>().FirstOrDefault(value => value["id"]?.GetValue<string>() == id && IsSharedCalendar(id));
    internal bool SharedWritable(string id) => !IsSharedCalendar(id) || SharedCalendar(id)?["sharedRole"]?.GetValue<string>()?.ToLowerInvariant() is "owner" or "editor";
    internal void MarkSharedDirty(string? id)
    {
        if (id == null || !IsSharedCalendar(id)) return;
        if (!SharedWritable(id)) throw new InvalidOperationException("你只有此共享日历的查看权限。");
        if (SharedCalendar(id) is { } calendar) calendar["sharedDirty"] = true;
    }
    internal IReadOnlyList<string> SharedCalendarIds => root["calendars"]!.AsArray().OfType<JsonObject>().Where(value => IsSharedCalendar(value["id"]?.GetValue<string>())).Select(value => value["id"]!.GetValue<string>()).ToArray();
    internal void ImportSharedCalendar(JsonObject metadata, JsonObject events, string serverUrl, bool replaceDirty = false)
    {
        var id = "shared:" + metadata["id"]!.GetValue<string>(); var old = SharedCalendar(id);
        var dirty = old?["sharedDirty"]?.GetValue<bool>() == true;
        if (dirty && !replaceDirty) return;
        var sources = (events["events"] as JsonArray ?? new()).OfType<JsonObject>().ToList();
        if (sources.Select(value => value["id"]!.GetValue<string>()).Distinct().Count() != sources.Count) throw new InvalidOperationException("共享日程标识重复，已保留本机数据。");
        foreach (var master in sources.Select(value => value["template"]).OfType<JsonObject>())
            if (master["seriesId"]?.GetValue<string>() is { } series && Templates.OfType<JsonObject>().Any(value => value["seriesId"]?.GetValue<string>() == series && value["calendarId"]?.GetValue<string>() != id))
                throw new InvalidOperationException("循环日程标识与其他日历冲突，已保留本机数据，请检查日历来源。");
        SaveCalendar(id, metadata["name"]!.GetValue<string>(), metadata["colorHex"]?.GetValue<string>() ?? "#3370FF", metadata["description"]?.GetValue<string>() ?? "",
            metadata["ownerUsername"]?.GetValue<string>() ?? "", "SHARED", metadata["members"]?.ToJsonString() ?? "[]");
        var calendar = SharedCalendar(id)!;
        calendar["sharedRole"] = (metadata["role"]?.GetValue<string>() ?? "viewer").ToLowerInvariant();
        calendar["sharedRevision"] = events["revision"]?.DeepClone() ?? metadata["revision"]?.DeepClone() ?? JsonValue.Create(0);
        calendar["sharedServerUrl"] = serverUrl; calendar["sharedDirty"] = false; calendar["sharedConflictRevision"] = null;
        var current = Tasks.OfType<JsonObject>().Where(value => value["calendarId"]?.GetValue<string>() == id).ToList();
        var currentTemplates = Templates.OfType<JsonObject>().Where(value => value["calendarId"]?.GetValue<string>() == id).ToList(); var importedSeries = new HashSet<string>();
        var identity = current.Where(value => !string.IsNullOrWhiteSpace(value["sharedEventId"]?.GetValue<string>())).ToDictionary(value => value["sharedEventId"]!.GetValue<string>(), value => value);
        var nextId = NextId(Tasks); var present = new HashSet<string>();
        foreach (var task in current) Tasks.Remove(task);
        foreach (var template in currentTemplates) Templates.Remove(template);
        foreach (var source in sources) {
            var wireId = source["id"]!.GetValue<string>(); var task = (JsonObject)source.DeepClone();
            task.Remove("template"); task.Remove("participants");
            present.Add(wireId); identity.TryGetValue(wireId, out var prior);
            task["id"] = prior?["id"]?.DeepClone() ?? JsonValue.Create(nextId++);
            task["calendarId"] = id; task["sharedEventId"] = wireId; task["itemType"] = "EVENT";
            task.Remove("sharedMissing"); task["totalCheckInMinutes"] = prior?["totalCheckInMinutes"]?.DeepClone() ?? JsonValue.Create(0);
            task["eventParticipantsJson"] = source["participants"]?.ToJsonString() ?? "[]";
            task["groupId"] = DefaultGroupId(); Tasks.Add(task);
            if (source["template"] is JsonObject master) {
                var template = (JsonObject)master.DeepClone(); template["id"] = NextId(Templates); template["calendarId"] = id; template["groupId"] = DefaultGroupId();
                var series = template["seriesId"]?.GetValue<string>();
                if (series != null) importedSeries.Add(series);
                if (series != null && template["instanceSkips"] is JsonArray skips) {
                    foreach (var skip in root["recurringInstanceSkips"]!.AsArray().OfType<JsonObject>().Where(value => value["seriesId"]?.GetValue<string>() == series).ToList()) root["recurringInstanceSkips"]!.AsArray().Remove(skip);
                    foreach (var skip in skips) if (skip is JsonValue number && number.TryGetValue<long>(out var day)) root["recurringInstanceSkips"]!.AsArray().Add(new JsonObject { ["seriesId"] = series, ["instanceEpochDay"] = day });
                }
                if (!Templates.OfType<JsonObject>().Any(value => value["seriesId"]?.GetValue<string>() == template["seriesId"]?.GetValue<string>())) Templates.Add(template);
            }
        }
        foreach (var missing in current.Where(value => !present.Contains(value["sharedEventId"]?.GetValue<string>() ?? ""))) {
            missing["sharedMissing"] = true; missing["canceled"] = true; missing["reminderEnabled"] = false; missing["reminderAtMillis"] = null;
            missing["canceledAtMillis"] ??= JsonValue.Create(DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()); Tasks.Add(missing);
        }
        foreach (var missing in currentTemplates.Where(value => !importedSeries.Contains(value["seriesId"]?.GetValue<string>() ?? ""))) {
            missing["id"] = NextId(Templates); missing["recurrenceType"] = "NONE"; missing["recurrenceNeverEnds"] = false; missing["endEpochDay"] = (missing["startEpochDay"]?.GetValue<long>() ?? 0) - 1; Templates.Add(missing);
        }
    }
    internal JsonArray ExportSharedEvents(string calendarId)
    {
        var result = new JsonArray(); var emittedSeries = new HashSet<string>();
        foreach (var source in Tasks.OfType<JsonObject>().Where(value => value["calendarId"]?.GetValue<string>() == calendarId && value["sharedMissing"]?.GetValue<bool>() != true)) {
            if (string.IsNullOrWhiteSpace(source["sharedEventId"]?.GetValue<string>())) {
                var seriesId = source["recurringSeriesId"]?.GetValue<string>();
                source["sharedEventId"] = string.IsNullOrWhiteSpace(seriesId) ? Guid.NewGuid().ToString("N") : seriesId + ":" +
                    (source["recurrenceAnchorDueAtMillis"]?.GetValue<long>() ?? source["startAtMillis"]?.GetValue<long>() ?? source["dueAtMillis"]!.GetValue<long>()).ToString(System.Globalization.CultureInfo.InvariantCulture);
            }
            var value = (JsonObject)source.DeepClone(); value["id"] = source["sharedEventId"]!.DeepClone();
            value.Remove("calendarId"); value.Remove("sharedEventId"); value.Remove("groupId");
            try { value["participants"] = JsonNode.Parse(source["eventParticipantsJson"]?.GetValue<string>() ?? "[]"); } catch { value["participants"] = new JsonArray(); }
            value.Remove("eventParticipantsJson");
            if (source["recurringSeriesId"]?.GetValue<string>() is { } series && emittedSeries.Add(series)) {
                var template = Templates.OfType<JsonObject>().FirstOrDefault(master => master["seriesId"]?.GetValue<string>() == series);
                if (template != null) {
                    var master = (JsonObject)template.DeepClone(); foreach (var key in new[] { "id", "calendarId", "groupId" }) master.Remove(key);
                    master["instanceSkips"] = new JsonArray(root["recurringInstanceSkips"]!.AsArray().OfType<JsonObject>().Where(skip => skip["seriesId"]?.GetValue<string>() == series).Select(skip => skip["instanceEpochDay"]?.DeepClone()).ToArray());
                    value["template"] = master;
                }
            }
            result.Add(value);
        }
        return result;
    }
}

internal sealed partial class DesktopWorkspace
{
    private static readonly HttpClient SharedClient = new() { Timeout = TimeSpan.FromSeconds(25) };
    private readonly SemaphoreSlim sharedGate = new(1, 1);
    internal void EditShared(Action<DesktopSnapshotDocument> action)
    {
        if (Document == null) throw new InvalidOperationException("请先登录账号");
        var candidate = DesktopSnapshotDocument.FromArchiveBytes(Document.ToArchiveBytes()); action(candidate);
        if (Account != null) store.Save(Account, candidate); else store.SaveDocument(candidate);
        Document = candidate; editVersion++; Notify("共享日历已更新本机副本");
    }
    internal void AcceptSharedCloud(JsonObject calendar, JsonObject events)
    {
        if (Document == null || Account == null) throw new InvalidOperationException("请先登录账号");
        store.SaveSafetyCopy(Document); EditShared(data => data.ImportSharedCalendar(calendar, events, Account.ServerUrl, true));
    }
    internal void OpenSharedCalendar(JsonObject calendar, JsonObject events)
    {
        var id = "shared:" + calendar["id"]!.GetValue<string>();
        if (Document?.SharedCalendar(id)?["sharedRole"]?.GetValue<string>()?.Equals("revoked", StringComparison.OrdinalIgnoreCase) == true) AcceptSharedCloud(calendar, events);
        else EditShared(data => data.ImportSharedCalendar(calendar, events, Account!.ServerUrl));
    }
    internal async Task KeepSharedLocalAsync(JsonObject calendar, JsonObject remote)
    {
        await sharedGate.WaitAsync();
        try {
            var id = calendar["id"]!.GetValue<string>(); var localId = "shared:" + id;
            if (Document == null || Account == null || !Document.SharedWritable(localId)) throw new InvalidOperationException("你只有此共享日历的查看权限。");
            store.SaveSafetyCopy(Document);
            var safety = DesktopSnapshotDocument.FromArchiveBytes(Document.ToArchiveBytes()); safety.ImportSharedCalendar(calendar, remote, Account.ServerUrl, true); store.SaveSafetyCopy(safety, "cloud");
            var submittedVersion = editVersion; var wire = Document.ExportSharedEvents(localId);
            var result = await SharedRequestAsync(HttpMethod.Put, "/v3/calendars/" + Uri.EscapeDataString(id) + "/events", new JsonObject { ["expectedRevision"] = remote["revision"]?.DeepClone(), ["events"] = wire });
            EditShared(data => { var local = data.SharedCalendar(localId)!; local["sharedRevision"] = result["revision"]?.DeepClone(); local["sharedConflictRevision"] = null; local["sharedDirty"] = editVersion != submittedVersion; });
        } finally { sharedGate.Release(); }
    }
    internal async Task<JsonObject> SharedRequestAsync(HttpMethod method, string path, JsonObject? payload = null)
    {
        var generation = accountGeneration;
        var state = Account ?? throw new InvalidOperationException("请先登录账号");
        state = await CloudAccountSync.EnsureAccessTokenAsync(state, default);
        if (generation != accountGeneration || Account == null) throw new InvalidOperationException("账号已切换，请重新操作。");
        Account = state with { Dirty = Account.Dirty, Revision = Account.Revision }; store.Save(Account, Document!);
        using var request = new HttpRequestMessage(method, state.ServerUrl.TrimEnd('/') + path);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", state.AccessToken);
        if (payload != null) request.Content = new StringContent(payload.ToJsonString(), Encoding.UTF8, "application/json");
        using var response = await SharedClient.SendAsync(request);
        JsonObject body;
        try { body = JsonNode.Parse(await response.Content.ReadAsStringAsync())?.AsObject() ?? new(); } catch { body = new(); }
        if (generation != accountGeneration) throw new InvalidOperationException("账号已切换，请重新操作。");
        if (response.StatusCode == HttpStatusCode.NotFound) throw new InvalidOperationException("当前服务器尚未提供共享日历，请先更新服务器或选择支持此功能的服务器。");
        if (response.StatusCode == HttpStatusCode.Conflict) throw new SharedCalendarConflictException(body["currentRevision"]?.GetValue<long>() ?? 0);
        if (response.StatusCode == HttpStatusCode.Unauthorized) throw new CloudSessionExpiredException();
        if (!response.IsSuccessStatusCode) throw new InvalidOperationException(body["message"]?.GetValue<string>() ?? $"共享日历请求失败（HTTP {(int)response.StatusCode}）");
        return body;
    }
    internal async Task SyncSharedCalendarsAsync()
    {
        if (Account == null || Document == null || Document.SharedCalendarIds.Count == 0) return;
        if (!await sharedGate.WaitAsync(0)) return;
        try {
            var server = Account.ServerUrl; var response = await SharedRequestAsync(HttpMethod.Get, "/v3/calendars");
            var metadata = (response["calendars"] as JsonArray ?? new()).OfType<JsonObject>().ToList();
            foreach (var value in metadata) {
                var id = "shared:" + value["id"]!.GetValue<string>(); var local = Document.SharedCalendar(id);
                if (local == null || local["sharedServerUrl"]?.GetValue<string>() != server) continue;
                if (local["sharedRole"]?.GetValue<string>()?.Equals("revoked", StringComparison.OrdinalIgnoreCase) == true) {
                    var rejoined = await SharedRequestAsync(HttpMethod.Get, "/v3/calendars/" + Uri.EscapeDataString(value["id"]!.GetValue<string>()) + "/events");
                    AcceptSharedCloud(value, rejoined); continue;
                }
                if (local["sharedDirty"]?.GetValue<bool>() == true) {
                    if (value["canEdit"]?.GetValue<bool>() != true) { EditShared(data => { data.SharedCalendar(id)!["sharedRole"] = "viewer"; data.SharedCalendar(id)!["sharedConflictRevision"] = value["revision"]?.DeepClone(); }); continue; }
                    try {
                        var submittedVersion = editVersion;
                        var sent = Document.ExportSharedEvents(id);
                        var saved = await SharedRequestAsync(HttpMethod.Put, "/v3/calendars/" + Uri.EscapeDataString(value["id"]!.GetValue<string>()) + "/events", new JsonObject { ["expectedRevision"] = local["sharedRevision"]?.DeepClone() ?? JsonValue.Create(0), ["events"] = sent });
                        EditShared(data => { var target = data.SharedCalendar(id)!; target["sharedRevision"] = saved["revision"]?.DeepClone(); target["sharedDirty"] = editVersion != submittedVersion; });
                    } catch (SharedCalendarConflictException error) {
                        EditShared(data => data.SharedCalendar(id)!["sharedConflictRevision"] = error.Revision); Notify(error.Message); continue;
                    }
                }
                if (Document.SharedCalendar(id)?["sharedDirty"]?.GetValue<bool>() == true) continue;
                var events = await SharedRequestAsync(HttpMethod.Get, "/v3/calendars/" + Uri.EscapeDataString(value["id"]!.GetValue<string>()) + "/events");
                EditShared(data => data.ImportSharedCalendar(value, events, server));
            }
            var accessible = metadata.Select(value => "shared:" + value["id"]!.GetValue<string>()).ToHashSet();
            foreach (var id in Document.SharedCalendarIds.Where(id => Document.SharedCalendar(id)?["sharedServerUrl"]?.GetValue<string>() == server && !accessible.Contains(id)).ToArray())
                EditShared(data => data.SharedCalendar(id)!["sharedRole"] = "revoked");
        } finally { sharedGate.Release(); }
    }
}
