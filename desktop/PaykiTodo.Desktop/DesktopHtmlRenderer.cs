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
        html.Append("*{box-sizing:border-box}body{margin:0;font-family:Segoe UI,Microsoft YaHei,sans-serif;background:#f6f8f9;color:#1f2933}header{position:sticky;top:0;z-index:3;background:#fff;border-bottom:1px solid #dde3e7;padding:0 24px;height:64px;display:flex;align-items:center;gap:18px}.brand{font-size:19px;font-weight:700}.nav{display:flex;gap:4px}.nav button,.actions button{border:0;background:transparent;padding:9px 12px;border-radius:6px;color:#4b5560;cursor:pointer}.nav button:hover{background:#eef3f4;color:#172126}.actions{margin-left:auto;display:flex;gap:8px}.actions button{background:#176b57;color:white}.meta{font-size:12px;color:#78838d;border-left:1px solid #d8dee3;padding-left:14px}main{max-width:1220px;margin:auto;padding:22px 26px}.view{display:none}.view.active{display:block}.section-title{display:flex;align-items:baseline;gap:8px;margin:4px 0 12px}.section-title h2{font-size:18px;margin:0}.count{color:#78838d;font-size:13px}.band{background:#fff;border-top:1px solid #e1e6e9;border-bottom:1px solid #e1e6e9;padding:14px 0;margin-bottom:20px}.item{display:grid;grid-template-columns:5px minmax(0,1fr) auto;gap:12px;align-items:center;padding:10px 14px;border-bottom:1px solid #edf0f2}.item:last-child{border-bottom:0}.accent{width:4px;height:100%;min-height:42px;background:var(--accent);border-radius:2px}.title{font-weight:650;white-space:pre-wrap}.sub{color:#74808a;font-size:12px;margin-top:3px}.item-actions{display:flex;gap:4px;flex-wrap:wrap;justify-content:flex-end}.item-actions button{border:1px solid #d6dde1;background:#fff;border-radius:5px;padding:5px 8px;cursor:pointer;color:#4c5963}.item-actions .danger{color:#b42318}.empty{padding:20px;color:#8a949c}.quick-group{background:#fff;border:1px solid #dde3e7;border-radius:8px;margin-bottom:14px;overflow:hidden}.quick-head{display:flex;align-items:center;gap:10px;padding:11px 14px;background:#f3f7f7;border-bottom:1px solid #e4e9eb}.quick-head strong{flex:1;font-size:15px}.quick-head span{color:#78838d;font-size:12px}.quick-head button{border:1px solid #d6dde1;background:#fff;border-radius:5px;padding:4px 9px;cursor:pointer}.quick-items{padding:6px 10px}.quick-item{display:grid;grid-template-columns:30px minmax(0,1fr);gap:4px;margin-left:calc((var(--depth) - 1) * 18px);align-items:start}.quick-toggle{width:30px;height:34px;border:0;background:transparent;color:#687782;font-size:18px}.quick-item .item{padding-left:8px}.quick-note{margin:2px 0 7px 24px;color:#697780;font-size:12px;white-space:pre-wrap}.planning{display:grid;grid-template-columns:240px minmax(0,1fr);gap:18px}.note-list,.editor{background:#fff;border:1px solid #dde3e7;padding:16px}.note-title{font-size:18px;font-weight:650;margin-bottom:12px}input,textarea{font:inherit;width:100%;border:1px solid #cfd7dc;border-radius:6px;padding:10px}textarea{min-height:440px;resize:vertical;line-height:1.65}.save-note{margin-top:10px;border:0;background:#176b57;color:#fff;border-radius:6px;padding:9px 16px;cursor:pointer}@media(max-width:820px){header{height:auto;flex-wrap:wrap;padding:12px 16px}.actions{margin-left:0}.meta{width:100%;border-left:0;padding-left:0}main{padding:16px}.planning{grid-template-columns:1fr}.item{grid-template-columns:5px minmax(0,1fr)}.item-actions{grid-column:2;justify-content:flex-start}}"
        );
        html.Append("</style></head><body><header><div class='brand'>PaykiTodo</div><nav class='nav'>")
            .Append(NavButton("board", "每日看板")).Append(NavButton("todos", "待办")).Append(NavButton("calendar", "日历")).Append(NavButton("planning", "规划台"))
            .Append("</nav><div class='actions'><button id='quickAction' onclick=\"post('addQuickChecklist')\" title='快速清单' aria-label='快速清单'>! ▤</button><button id='todoAction' onclick=\"post('addTodo')\">新增待办</button><button id='eventAction' onclick=\"post('addEvent')\">创建日程</button></div><div class='meta'>")
            .Append(Html(state.Username)).Append(" · 本地版本 ").Append(state.Revision).Append(state.Dirty ? " · 待同步" : " · 已同步").Append("</div></header><main>");

        html.Append("<section id='board' class='view active'>")
            .Append(RenderSection("今日待办", todayTodos.Where(item => item.QuickChecklistGroupId is null), groups, "今天暂无普通待办"))
            .Append(RenderQuickChecklistGroups(document, todayTodos))
            .Append(RenderSection("剩余日程", todayEvents, groups, "今天暂无剩余日程"))
            .Append(RenderSection("明天日程", tomorrowEvents, groups, "明天暂无日程"))
            .Append("</section>");
        html.Append("<section id='todos' class='view'>")
            .Append(RenderSection("待办", todos.Where(item => item.QuickChecklistGroupId is null), groups, "暂无普通待办"))
            .Append(RenderQuickChecklistGroups(document, todos))
            .Append("</section>");
        html.Append("<section id='calendar' class='view'>").Append(RenderSection("日历", events, groups, "暂无未完成日程")).Append("</section>");
        html.Append("<section id='planning' class='view'><div class='planning'><aside class='note-list'><div class='note-title'>规划文档</div>")
            .Append(note is null ? "<div class='empty'>还没有规划文档</div>" : $"<div>{Html(note.Title)}</div>")
            .Append("</aside><div class='editor'><input id='noteTitle' value='").Append(HtmlAttribute(note?.Title ?? "新规划"))
            .Append("'><textarea id='noteMarkdown' placeholder='直接写下任务、日程和想法'>").Append(Html(note?.Markdown ?? ""))
            .Append("</textarea><button class='save-note' onclick='savePlanning()'>保存规划</button></div></div></section>");
        html.Append("</main><script>let current='board';function syncActions(){document.getElementById('quickAction').style.display=current==='board'||current==='todos'?'':'none';document.getElementById('todoAction').style.display=current==='board'||current==='todos'?'':'none';document.getElementById('eventAction').style.display=current==='board'||current==='calendar'?'':'none'}function show(id){document.getElementById(current).classList.remove('active');current=id;document.getElementById(id).classList.add('active');syncActions()}function post(action,id){chrome.webview.postMessage({action:action,id:id||0})}function savePlanning(){chrome.webview.postMessage({action:'savePlanning',id:")
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

    private static string RenderQuickChecklistGroups(DesktopSnapshotDocument document, IEnumerable<DesktopItem> source)
    {
        var active = source.Where(item => item.QuickChecklistGroupId is not null && !item.Completed && !item.Canceled).ToList();
        var result = new StringBuilder();
        foreach (var group in document.QuickChecklistGroups)
        {
            var allItems = document.Items.Where(item => item.QuickChecklistGroupId == group.Id).ToList();
            var groupItems = active.Where(item => item.QuickChecklistGroupId == group.Id).ToList();
            if (groupItems.Count == 0) continue;
            var countItems = allItems.Where(item => !item.Canceled).ToList();
            result.Append("<section class='quick-group'><div class='quick-head'><strong>")
                .Append(Html(group.Title)).Append("</strong><span>")
                .Append(countItems.Count(item => item.Completed)).Append(" / ").Append(countItems.Count).Append(" 已完成</span><button onclick=\"post('quickGroupToggle',")
                .Append(group.Id).Append(")\">").Append(group.Collapsed ? "展开" : "收起").Append("</button></div>");
            if (!group.Collapsed)
            {
                var tree = VisibleQuickItems(groupItems);
                result.Append("<div class='quick-items'>");
                foreach (var item in tree.Ordered)
                {
                    var hasChildren = tree.ByParent.TryGetValue(item.Id, out var children) && children.Count > 0;
                    result.Append("<div class='quick-item' style='--depth:").Append(item.QuickChecklistDepth ?? 1).Append("'>")
                        .Append(hasChildren ? "<button class='quick-toggle' onclick=\"post('quickItemToggle'," + item.Id + ")\">" + (item.QuickChecklistCollapsed ? "›" : "⌄") + "</button>" : "<span></span>")
                        .Append(RenderItem(item, document.Groups.ToDictionary(value => value.Id), includeComplete: true));
                    if (!string.IsNullOrWhiteSpace(item.Notes)) result.Append("<div class='quick-note'>").Append(Html(item.Notes)).Append("</div>");
                    result.Append("</div>");
                }
                result.Append("</div>");
            }
            result.Append("</section>");
        }
        return result.ToString();
    }

    private static (IReadOnlyList<DesktopItem> Ordered, IReadOnlyDictionary<long, List<DesktopItem>> ByParent) VisibleQuickItems(IReadOnlyList<DesktopItem> items)
    {
        var byId = items.ToDictionary(item => item.Id);
        var byParent = items.GroupBy(item => item.ParentTodoId).ToDictionary(group => group.Key ?? 0L, group => group.OrderBy(item => item.QuickChecklistSortOrder ?? int.MaxValue).ThenBy(item => item.Id).ToList());
        var ordered = new List<DesktopItem>();
        void Visit(DesktopItem item)
        {
            ordered.Add(item);
            if (!item.QuickChecklistCollapsed && byParent.TryGetValue(item.Id, out var children))
                foreach (var child in children) Visit(child);
        }
        var roots = items.Where(item => item.ParentTodoId is null || !byId.ContainsKey(item.ParentTodoId.Value))
            .OrderBy(item => item.QuickChecklistSortOrder ?? int.MaxValue).ThenBy(item => item.Id);
        foreach (var root in roots) Visit(root);
        return (ordered, byParent);
    }

    private static string RenderItem(DesktopItem item, IReadOnlyDictionary<long, DesktopGroup> groups, bool includeComplete)
    {
        var group = groups.TryGetValue(item.GroupId, out var value) ? value : new DesktopGroup(0, "未分组", item.AccentColor);
        var html = new StringBuilder("<article class='item' style='--accent:").Append(SafeColor(item.AccentColor)).Append("'><div class='accent'></div><div><div class='title'>")
            .Append(Html(item.Title)).Append("</div><div class='sub'>").Append(Html(FormatTime(item))).Append(" · ").Append(Html(group.Name));
        if (!string.IsNullOrWhiteSpace(item.Location)) html.Append(" · @").Append(Html(item.Location.TrimStart('@')));
        html.Append("</div></div><div class='item-actions'><button onclick=\"post('edit',").Append(item.Id).Append(")\">修改</button>");
        if (includeComplete && !item.IsEvent) html.Append("<button onclick=\"post('complete',").Append(item.Id).Append(")\">完成</button>");
        html.Append("<button onclick=\"post('cancel',").Append(item.Id).Append(")\">取消</button><button class='danger' onclick=\"post('delete',").Append(item.Id).Append(")\">删除</button></div></article>");
        return html.ToString();
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
