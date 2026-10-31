using System.Text.Json.Nodes;

namespace PaykiTodo.Desktop;

internal sealed partial class DesktopSnapshotDocument
{
    public void ReplaceWith(DesktopSnapshotDocument document) => root = (JsonObject)document.root.DeepClone();
    public JsonObject Settings => root["settings"]!.AsObject();
    public IReadOnlyList<JsonObject> Reports => root["aiReports"]!.AsArray().OfType<JsonObject>().Where(report => !DesktopReportPolicy.IsExpired(report, DateTimeOffset.UtcNow.ToUnixTimeMilliseconds())).ToList();
    public bool HiddenOnBoard(long id) => FindTask(id)?["hiddenFromBoard"]?.GetValue<bool>() == true;
    public long SaveGroup(long? id, string name, string color)
    {
        if (string.IsNullOrWhiteSpace(name)) throw new InvalidOperationException("请填写分组名称");
        var group = id == null ? new JsonObject { ["id"] = NextId(GroupsArray), ["sortOrder"] = GroupsArray.Count,
            ["isDefault"] = false, ["createdAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() }
            : GroupsArray.OfType<JsonObject>().First(item => item["id"]!.GetValue<long>() == id);
        group["name"] = name.Trim(); group["colorHex"] = color;
        if (id == null) GroupsArray.Add(group);
        return group["id"]!.GetValue<long>();
    }
    public void DeleteGroup(long id)
    {
        var group = GroupsArray.OfType<JsonObject>().FirstOrDefault(item => item["id"]!.GetValue<long>() == id);
        if (group == null) return;
        if (group["isDefault"]?.GetValue<bool>() == true || GroupsArray.Count <= 1)
            throw new InvalidOperationException("默认分组需要保留");
        foreach (var item in Tasks.OfType<JsonObject>().Where(item => item["groupId"]?.GetValue<long>() == id))
            item["groupId"] = DefaultGroupId();
        GroupsArray.Remove(group);
    }
    public void ArchivePlanning(long id)
    {
        var note = PlanningNotesArray.OfType<JsonObject>().FirstOrDefault(note => note["id"]?.GetValue<long>() == id);
        if (note != null) { note["archived"] = true; note["updatedAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(); }
    }
    public bool PlanningArchived(long id) => PlanningNotesArray.OfType<JsonObject>()
        .FirstOrDefault(note => note["id"]?.GetValue<long>() == id)?["archived"]?.GetValue<bool>() == true;

    public bool CheckedIn(long id) => root["eventCheckIns"]!.AsArray().OfType<JsonObject>()
        .Any(check => check["eventId"]?.GetValue<long>() == id && check["checkOutAtMillis"] == null);
    public void ToggleCheckIn(long id)
    {
        var task = FindTask(id) ?? throw new InvalidOperationException("日程不存在");
        var checks = root["eventCheckIns"]!.AsArray();
        var active = checks.OfType<JsonObject>().FirstOrDefault(check => check["eventId"]?.GetValue<long>() == id && check["checkOutAtMillis"] == null);
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        if (active == null) checks.Add(new JsonObject { ["id"] = NextId(checks), ["eventId"] = id,
            ["checkInAtMillis"] = now, ["checkOutAtMillis"] = null, ["durationMinutes"] = 0 });
        else {
            active["checkOutAtMillis"] = now;
            active["durationMinutes"] = (int)Math.Max(0, (now - active["checkInAtMillis"]!.GetValue<long>()) / 60000);
            task["totalCheckInMinutes"] = (task["totalCheckInMinutes"]?.GetValue<int>() ?? 0) +
                (int)Math.Max(0, (now - active["checkInAtMillis"]!.GetValue<long>()) / 60000);
        }
    }
}
