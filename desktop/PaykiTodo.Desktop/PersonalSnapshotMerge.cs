using System.Text.Json.Nodes;

namespace PaykiTodo.Desktop;

internal sealed partial class DesktopSnapshotDocument
{
    private static readonly HashSet<string> LifecycleFields = ["completed", "canceled", "completedAtMillis", "canceledAtMillis"];

    private JsonObject PersonalMergeJson()
    {
        var data = (JsonObject)root.DeepClone();
        StripSharedData(data);
        data.Remove("exportedAtMillis"); data.Remove("reminderChainLogs");
        if (data["settings"]?["planningAiProviders"] is JsonArray providers)
            foreach (var provider in providers.OfType<JsonObject>()) provider.Remove("apiKey");
        return data;
    }

    internal bool SameSyncContent(DesktopSnapshotDocument other)
    {
        static string Stable(JsonObject data) => string.Join("\n", data.OrderBy(p => p.Key, StringComparer.Ordinal).Select(p =>
            p.Key + ":" + (p.Value is JsonArray rows ? string.Join("\n", rows.Select(CanonicalMerge).Order(StringComparer.Ordinal)) : CanonicalMerge(p.Value))));
        return Stable(PersonalMergeJson()) == Stable(other.PersonalMergeJson());
    }

    internal static DesktopSnapshotDocument? MergePersonal(DesktopSnapshotDocument baseline, DesktopSnapshotDocument local, DesktopSnapshotDocument remote)
    {
        try {
            var b = baseline.PersonalMergeJson(); var l = local.PersonalMergeJson(); var r = remote.PersonalMergeJson();
            var result = (JsonObject)local.root.DeepClone();
            foreach (var key in MergeKeys(b, l, r)) {
                result[key] = key == "settings" ? MergeFields(b[key]!.AsObject(), l[key]!.AsObject(), r[key]!.AsObject())
                    : b[key] is JsonArray ba && l[key] is JsonArray la && r[key] is JsonArray ra ? MergeRows(key, ba, la, ra)
                    : ChooseMerge(b[key], l[key], r[key])?.DeepClone();
            }
            var merged = new DesktopSnapshotDocument(result);
            merged.ValidateMergeReferences();
            merged.PreserveLocalApiKeys(local);
            merged.PreserveSharedData(local);
            return merged;
        } catch (InvalidOperationException) { return null; }
    }

    private static JsonArray MergeRows(string table, JsonArray baseline, JsonArray local, JsonArray remote)
    {
        static Dictionary<string, JsonObject> Index(string table, JsonArray array) {
            var result = new Dictionary<string, JsonObject>();
            foreach (var row in array.OfType<JsonObject>()) {
                var id = table == "todoGroupTags" ? CanonicalMerge(row["todoId"]) + ":" + CanonicalMerge(row["groupId"]) : CanonicalMerge(row["id"]);
                if (id == "null" || !result.TryAdd(id, row)) throw new InvalidOperationException("Duplicate or missing row identity");
            }
            if (result.Count != array.Count) throw new InvalidOperationException("Invalid row");
            return result;
        }
        var b = Index(table, baseline); var l = Index(table, local); var r = Index(table, remote);
        var result = new JsonArray();
        foreach (var id in b.Keys.Concat(l.Keys).Concat(r.Keys).Distinct().Order(StringComparer.Ordinal)) {
            b.TryGetValue(id, out var bv); l.TryGetValue(id, out var lv); r.TryGetValue(id, out var rv);
            var row = table == "tasks" && bv != null && lv != null && rv != null && !EqualMerge(lv, bv) && !EqualMerge(rv, bv) && !EqualMerge(lv, rv)
                ? MergeTask(bv, lv, rv) : ChooseMerge(bv, lv, rv);
            if (row != null) result.Add(row.DeepClone());
        }
        return result;
    }

    private static JsonObject MergeTask(JsonObject baseline, JsonObject local, JsonObject remote)
    {
        static JsonObject Section(JsonObject row, IEnumerable<string> fields) => new(fields.Where(row.ContainsKey)
            .Order(StringComparer.Ordinal).Select(k => new KeyValuePair<string, JsonNode?>(k, row[k]?.DeepClone())));
        var content = MergeKeys(baseline, local, remote).Except(LifecycleFields).Except(["reminderEnabled", "missed", "missedAtMillis"]);
        var result = (JsonObject)ChooseMerge(Section(baseline, content), Section(local, content), Section(remote, content))!.DeepClone();
        var sameTerminal = (local["completed"]?.GetValue<bool>() == true || local["canceled"]?.GetValue<bool>() == true) &&
            (local["completed"]?.GetValue<bool>() ?? false) == (remote["completed"]?.GetValue<bool>() ?? false) &&
            (local["canceled"]?.GetValue<bool>() ?? false) == (remote["canceled"]?.GetValue<bool>() ?? false);
        var state = sameTerminal ? Section(local, LifecycleFields) : ChooseMerge(Section(baseline, LifecycleFields), Section(local, LifecycleFields), Section(remote, LifecycleFields))!.AsObject();
        if (sameTerminal) foreach (var key in new[] { "completedAtMillis", "canceledAtMillis" }) {
            var times = new[] { local[key]?.GetValue<long>() ?? 0, remote[key]?.GetValue<long>() ?? 0 }.Where(t => t > 0).ToArray();
            if (times.Length > 0) state[key] = times.Min();
        }
        foreach (var pair in state) result[pair.Key] = pair.Value?.DeepClone();
        var finished = result["completed"]?.GetValue<bool>() == true || result["canceled"]?.GetValue<bool>() == true;
        result["reminderEnabled"] = finished ? JsonValue.Create(false) : ChooseMerge(baseline["reminderEnabled"], local["reminderEnabled"], remote["reminderEnabled"])?.DeepClone();
        result["missed"] = finished ? JsonValue.Create(false) : ChooseMerge(baseline["missed"], local["missed"], remote["missed"])?.DeepClone();
        if (!finished && result["missed"]?.GetValue<bool>() == true)
            result["missedAtMillis"] = Math.Max(local["missedAtMillis"]?.GetValue<long>() ?? 0, remote["missedAtMillis"]?.GetValue<long>() ?? 0);
        else result.Remove("missedAtMillis");
        return result;
    }

    private static JsonObject MergeFields(JsonObject baseline, JsonObject local, JsonObject remote) => new(
        MergeKeys(baseline, local, remote).Select(k => new KeyValuePair<string, JsonNode?>(k, ChooseMerge(baseline[k], local[k], remote[k])?.DeepClone())));
    private static IEnumerable<string> MergeKeys(params JsonObject[] objects) => objects.SelectMany(o => o.Select(p => p.Key)).Distinct().Order(StringComparer.Ordinal);
    private static JsonNode? ChooseMerge(JsonNode? baseline, JsonNode? local, JsonNode? remote) => EqualMerge(local, remote) ? local
        : EqualMerge(local, baseline) ? remote : EqualMerge(remote, baseline) ? local : throw new InvalidOperationException("Both copies changed the same value");
    private static bool EqualMerge(JsonNode? a, JsonNode? b) => CanonicalMerge(a) == CanonicalMerge(b);
    private static string CanonicalMerge(JsonNode? node) => node switch {
        JsonObject obj => "{" + string.Join(",", obj.OrderBy(p => p.Key, StringComparer.Ordinal).Select(p => System.Text.Json.JsonSerializer.Serialize(p.Key) + ":" + CanonicalMerge(p.Value))) + "}",
        JsonArray array => "[" + string.Join(",", array.Select(CanonicalMerge)) + "]",
        _ => node?.ToJsonString() ?? "null"
    };

    private void ValidateMergeReferences()
    {
        HashSet<string> Ids(string table) => root[table]!.AsArray().OfType<JsonObject>().Select(r => CanonicalMerge(r["id"])).ToHashSet();
        bool Ref(JsonObject row, string field, HashSet<string> ids) => row[field] == null || CanonicalMerge(row[field]) == "0" || ids.Contains(CanonicalMerge(row[field]));
        var tasks = Ids("tasks"); var groups = Ids("groups"); var calendars = Ids("calendars"); var lists = Ids("quickChecklistGroups");
        foreach (var task in Tasks.OfType<JsonObject>())
            if (!Ref(task,"groupId",groups) || !Ref(task,"calendarId",calendars) || !Ref(task,"parentTodoId",tasks) || !Ref(task,"quickChecklistGroupId",lists)) throw new InvalidOperationException("Dangling task reference");
        foreach (var template in root["templates"]!.AsArray().OfType<JsonObject>())
            if (!Ref(template,"groupId",groups) || !Ref(template,"calendarId",calendars)) throw new InvalidOperationException("Dangling template reference");
        foreach (var tag in root["todoGroupTags"]!.AsArray().OfType<JsonObject>())
            if (!Ref(tag,"todoId",tasks) || !Ref(tag,"groupId",groups)) throw new InvalidOperationException("Dangling group tag");
        foreach (var check in root["eventCheckIns"]!.AsArray().OfType<JsonObject>())
            if (!Ref(check,"eventId",tasks)) throw new InvalidOperationException("Dangling check-in");
        var notes = Ids("planningNotes"); var nodes = Ids("planningNodes");
        foreach (var node in root["planningNodes"]!.AsArray().OfType<JsonObject>())
            if (!Ref(node,"noteId",notes) || !Ref(node,"parentNodeId",nodes) || !Ref(node,"linkedTodoId",tasks) || !Ref(node,"linkedEndTodoId",tasks)) throw new InvalidOperationException("Dangling planning reference");
    }
}
