using System.Text.Json.Nodes;

namespace PaykiTodo.Desktop;

internal sealed record DesktopCalendarMember(string Username, string DisplayName, string Role)
{
    public string Label => string.IsNullOrWhiteSpace(DisplayName) || DisplayName == Username ? Username : DisplayName + " · " + Username;
}
internal sealed record DesktopEventParticipant(string Username, string Response);

internal static class DesktopEventParticipants
{
    internal static IReadOnlyList<DesktopCalendarMember> Members(DesktopCalendar calendar)
    {
        try {
            return (JsonNode.Parse(calendar.MembersJson) as JsonArray ?? new()).OfType<JsonObject>()
                .Select(value => new DesktopCalendarMember(value["username"]?.GetValue<string>()?.Trim().ToLowerInvariant() ?? "",
                    value["displayName"]?.GetValue<string>() ?? "", value["role"]?.GetValue<string>() ?? "viewer"))
                .Where(value => value.Username.Length > 0).DistinctBy(value => value.Username).ToArray();
        } catch (System.Text.Json.JsonException) { throw new InvalidOperationException("日历成员数据无效，请刷新共享日历。"); }
    }
    internal static IReadOnlyList<DesktopEventParticipant> Read(string json)
    {
        try {
            if (JsonNode.Parse(string.IsNullOrWhiteSpace(json) ? "[]" : json) is not JsonArray values || values.Count > 128)
                throw new InvalidOperationException("日程参与者数据无效。");
            return values.Select(value => {
                var username = value is JsonObject entry ? entry["username"]?.GetValue<string>() : value?.GetValue<string>();
                var response = value is JsonObject row ? row["response"]?.GetValue<string>() ?? "NEEDS_ACTION" : "NEEDS_ACTION";
                if (string.IsNullOrWhiteSpace(username) || response is not ("NEEDS_ACTION" or "ACCEPTED" or "TENTATIVE" or "DECLINED"))
                    throw new InvalidOperationException("日程参与者或回应无效。");
                return new DesktopEventParticipant(username.Trim().ToLowerInvariant(), response);
            }).DistinctBy(value => value.Username).ToArray();
        } catch (System.Text.Json.JsonException) { throw new InvalidOperationException("日程参与者数据无效。"); }
    }
    internal static string Json(IEnumerable<DesktopEventParticipant> participants) => new JsonArray(participants.Select(value => (JsonNode)new JsonObject {
        ["username"] = value.Username, ["response"] = value.Response }).ToArray()).ToJsonString();
    internal static string Select(string requested, string existing)
    {
        var previous = Read(existing).ToDictionary(value => value.Username);
        return Json(Read(requested).Select(value => new DesktopEventParticipant(value.Username, previous.GetValueOrDefault(value.Username)?.Response ?? "NEEDS_ACTION")));
    }
    internal static string Reconcile(DesktopCalendar calendar, string requested, string existing)
    {
        if (!DesktopSnapshotDocument.IsSharedCalendar(calendar.Id)) return "[]";
        var members = Members(calendar).Select(value => value.Username).ToHashSet();
        var previous = Read(existing).ToDictionary(value => value.Username);
        var selected = Read(requested);
        if (selected.Any(value => !members.Contains(value.Username) && !previous.ContainsKey(value.Username)))
            throw new InvalidOperationException("参与者须先加入当前共享日历，请重新选择参与者。");
        return Json(selected.Where(value => members.Contains(value.Username)).Select(value =>
            new DesktopEventParticipant(value.Username, previous.GetValueOrDefault(value.Username)?.Response ?? "NEEDS_ACTION")));
    }
    internal static string ResponseLabel(string response) => response switch {
        "ACCEPTED" => "接受", "TENTATIVE" => "暂定", "DECLINED" => "拒绝", _ => "未回应"
    };
}
