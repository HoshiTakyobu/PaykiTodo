using System.Net;
using System.Text;
using System.Text.Json;

namespace PaykiTodo.Desktop;

internal static class DesktopHtmlRenderer
{
    public static string Render(DesktopSnapshotDocument document, CloudAccountState state)
    {
        var items = document.Items;
        var groups = document.Groups.ToDictionary(group => group.Id);
        var active = items.Where(item => !item.Completed && !item.Canceled).ToList();
        var now = DateTime.Now;
        var todayStart = new DateTimeOffset(now.Date).ToUnixTimeMilliseconds();
        var tomorrowStart = new DateTimeOffset(now.Date.AddDays(1)).ToUnixTimeMilliseconds();
        var nextDayStart = new DateTimeOffset(now.Date.AddDays(2)).ToUnixTimeMilliseconds();
        var todayTodos = active.Where(item => !item.IsEvent && (item.DueAtMillis == long.MaxValue || item.DueAtMillis >= todayStart && item.DueAtMillis < tomorrowStart)).ToList();
        var todayEvents = active.Where(item => item.IsEvent && item.StartAtMillis >= todayStart && item.StartAtMillis < tomorrowStart && (item.EndAtMillis ?? item.StartAtMillis) >= DateTimeOffset.Now.ToUnixTimeMilliseconds()).ToList();
        var tomorrowEvents = active.Where(item => item.IsEvent && item.StartAtMillis >= tomorrowStart && item.StartAtMillis < nextDayStart).ToList();
        var todos = active.Where(item => !item.IsEvent).ToList();
        var events = active.Where(item => item.IsEvent).ToList();
        var note = document.PlanningNotes.FirstOrDefault();

        var html = new StringBuilder("<!doctype html><html lang='zh-CN'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width'><title>PaykiTodo</title><style>");
        html.Append("*{box-sizing:border-box}body{margin:0;font-family:Segoe UI,Microsoft YaHei,sans-serif;background:#f6f8f9;color:#1f2933}header{position:sticky;top:0;z-index:3;background:#fff;border-bottom:1px solid #dde3e7;padding:0 24px;height:64px;display:flex;align-items:center;gap:18px}.brand{font-size:19px;font-weight:700}.nav{display:flex;gap:4px}.nav button,.actions button{border:0;background:transparent;padding:9px 12px;border-radius:6px;color:#4b5560;cursor:pointer}.nav button:hover{background:#eef3f4;color:#172126}.actions{margin-left:auto;display:flex;gap:8px}.actions button{background:#176b57;color:white}.meta{font-size:12px;color:#78838d;border-left:1px solid #d8dee3;padding-left:14px}main{max-width:1220px;margin:auto;padding:22px 26px}.view{display:none}.view.active{display:block}.section-title{display:flex;align-items:baseline;gap:8px;margin:4px 0 12px}.section-title h2{font-size:18px;margin:0}.count{color:#78838d;font-size:13px}.band{background:#fff;border-top:1px solid #e1e6e9;border-bottom:1px solid #e1e6e9;padding:14px 0;margin-bottom:20px}.item{display:grid;grid-template-columns:5px minmax(0,1fr) auto;gap:12px;align-items:center;padding:10px 14px;border-bottom:1px solid #edf0f2}.item:last-child{border-bottom:0}.accent{width:4px;height:100%;min-height:42px;background:var(--accent);border-radius:2px}.title{font-weight:650;white-space:pre-wrap}.sub{color:#74808a;font-size:12px;margin-top:3px}.item-actions{display:flex;gap:4px}.item-actions button{border:1px solid #d6dde1;background:#fff;border-radius:5px;padding:5px 8px;cursor:pointer;color:#4c5963}.item-actions .danger{color:#b42318}.empty{padding:20px;color:#8a949c}.planning{display:grid;grid-template-columns:240px minmax(0,1fr);gap:18px}.note-list,.editor{background:#fff;border:1px solid #dde3e7;padding:16px}.note-title{font-size:18px;font-weight:650;margin-bottom:12px}input,textarea{font:inherit;width:100%;border:1px solid #cfd7dc;border-radius:6px;padding:10px}textarea{min-height:440px;resize:vertical;line-height:1.65}.save-note{margin-top:10px;border:0;background:#176b57;color:#fff;border-radius:6px;padding:9px 16px;cursor:pointer}@media(max-width:820px){header{height:auto;flex-wrap:wrap;padding:12px 16px}.actions{margin-left:0}.meta{width:100%;border-left:0;padding-left:0}main{padding:16px}.planning{grid-template-columns:1fr}}"
        );
        html.Append("</style></head><body><header><div class='brand'>PaykiTodo</div><nav class='nav'>")
            .Append(NavButton("board", "每日看板")).Append(NavButton("todos", "待办")).Append(NavButton("calendar", "日历")).Append(NavButton("planning", "规划台"))
            .Append("</nav><div class='actions'><button onclick=\"post('addTodo')\">新增待办</button><button onclick=\"post('addEvent')\">创建日程</button></div><div class='meta'>")
            .Append(Html(state.Username)).Append(" · 本地版本 ").Append(state.Revision).Append(state.Dirty ? " · 待同步" : " · 已同步").Append("</div></header><main>");

        html.Append("<section id='board' class='view active'>")
            .Append(RenderSection("今日待办", todayTodos, groups, "今天暂无待办"))
            .Append(RenderSection("剩余日程", todayEvents, groups, "今天暂无剩余日程"))
            .Append(RenderSection("明天日程", tomorrowEvents, groups, "明天暂无日程"))
            .Append("</section>");
        html.Append("<section id='todos' class='view'>").Append(RenderSection("待办", todos, groups, "暂无未完成待办")).Append("</section>");
        html.Append("<section id='calendar' class='view'>").Append(RenderSection("日历", events, groups, "暂无未完成日程")).Append("</section>");
        html.Append("<section id='planning' class='view'><div class='planning'><aside class='note-list'><div class='note-title'>规划文档</div>")
            .Append(note is null ? "<div class='empty'>还没有规划文档</div>" : $"<div>{Html(note.Title)}</div>")
            .Append("</aside><div class='editor'><input id='noteTitle' value='").Append(HtmlAttribute(note?.Title ?? "新规划"))
            .Append("'><textarea id='noteMarkdown' placeholder='直接写下任务、日程和想法'>").Append(Html(note?.Markdown ?? ""))
            .Append("</textarea><button class='save-note' onclick='savePlanning()'>保存规划</button></div></div></section>");
        html.Append("</main><script>let current='board';function show(id){document.getElementById(current).classList.remove('active');current=id;document.getElementById(id).classList.add('active')}function post(action,id){chrome.webview.postMessage({action:action,id:id||0})}function savePlanning(){chrome.webview.postMessage({action:'savePlanning',id:")
            .Append(note?.Id ?? 0).Append(",title:document.getElementById('noteTitle').value,markdown:document.getElementById('noteMarkdown').value})}</script></body></html>");
        return html.ToString();
    }

    private static string NavButton(string id, string label) => $"<button onclick=\"show('{id}')\">{Html(label)}</button>";

    private static string RenderSection(string title, IEnumerable<DesktopItem> source, IReadOnlyDictionary<long, DesktopGroup> groups, string empty)
    {
        var items = source.ToList();
        var html = new StringBuilder($"<div class='section-title'><h2>{Html(title)}</h2><span class='count'>{items.Count}</span></div><div class='band'>");
        if (items.Count == 0) return html.Append("<div class='empty'>").Append(Html(empty)).Append("</div></div>").ToString();
        foreach (var item in items)
        {
            var group = groups.TryGetValue(item.GroupId, out var value) ? value : new DesktopGroup(0, "未分组", item.AccentColor);
            html.Append("<article class='item' style='--accent:").Append(SafeColor(item.AccentColor)).Append("'><div class='accent'></div><div><div class='title'>")
                .Append(Html(item.Title)).Append("</div><div class='sub'>").Append(Html(FormatTime(item))).Append(" · ").Append(Html(group.Name));
            if (!string.IsNullOrWhiteSpace(item.Location)) html.Append(" · @").Append(Html(item.Location.TrimStart('@')));
            html.Append("</div></div><div class='item-actions'><button onclick=\"post('edit',").Append(item.Id).Append(")\">修改</button>");
            if (!item.IsEvent) html.Append("<button onclick=\"post('complete',").Append(item.Id).Append(")\">完成</button>");
            html.Append("<button onclick=\"post('cancel',").Append(item.Id).Append(")\">取消</button><button class='danger' onclick=\"post('delete',")
                .Append(item.Id).Append(")\">删除</button></div></article>");
        }
        return html.Append("</div>").ToString();
    }

    private static string FormatTime(DesktopItem item)
    {
        if (item.IsEvent)
        {
            var start = item.StartAtMillis is { } startMillis ? DateTimeOffset.FromUnixTimeMilliseconds(startMillis).LocalDateTime : (DateTime?)null;
            var end = item.EndAtMillis is { } endMillis ? DateTimeOffset.FromUnixTimeMilliseconds(endMillis).LocalDateTime : (DateTime?)null;
            return start is null ? "未设置时间" : end is null ? start.Value.ToString("M月d日 HH:mm") : $"{start.Value:M月d日 HH:mm}-{end.Value:HH:mm}";
        }
        return item.DueAtMillis == long.MaxValue ? "无 DDL · 今日待办" : DateTimeOffset.FromUnixTimeMilliseconds(item.DueAtMillis).LocalDateTime.ToString("M月d日 HH:mm");
    }

    private static string SafeColor(string value) => value.StartsWith('#') && value.Length is 4 or 7 or 9 && value.Skip(1).All(Uri.IsHexDigit) ? value : "#4CB782";
    private static string Html(string value) => WebUtility.HtmlEncode(value);
    private static string HtmlAttribute(string value) => WebUtility.HtmlEncode(value).Replace("'", "&#39;");
}
