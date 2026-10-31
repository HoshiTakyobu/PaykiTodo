using System.Globalization;
using System.Text.Json.Nodes;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using static PaykiTodo.Desktop.NativeUi;
using Button = System.Windows.Controls.Button;
using CheckBox = System.Windows.Controls.CheckBox;
using ComboBox = System.Windows.Controls.ComboBox;
using Orientation = System.Windows.Controls.Orientation;
using TextBox = System.Windows.Controls.TextBox;
using HorizontalAlignment = System.Windows.HorizontalAlignment;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow
{
    private readonly HashSet<string> expandedTodoSeries = new();
    private UIElement BoardPage()
    {
        var today = DateTime.Today;
        var tasks = Active.Where(item => !item.IsEvent && !workspace.Document!.HiddenOnBoard(item.Id) &&
            (item.DueAtMillis == long.MaxValue || Local(item.DueAtMillis).Date <= today)).ToList();
        var schedules = EventsOn(today).Where(item => Local(item.EndAtMillis) > DateTime.Now).ToList();
        var greeting = Text(DateTime.Now.Hour < 12 ? "早上好，开启新的一天" : DateTime.Now.Hour < 18 ? "下午好，按自己的节奏来" : "晚上好，给今天一个好结尾", 28, Ocean);
        greeting.FontFamily = HeadingFont; greeting.FontSize = 28;
        var greetingCard = Card(Column(greeting, Text(today.ToString("yyyy年M月d日  dddd", CultureInfo.GetCultureInfo("zh-CN")), 14, Muted)), 24);
        greetingCard.CornerRadius = new CornerRadius(8, 8, 28, 28);
        var body = Column(greetingCard);
        var columns = new Grid(); columns.ColumnDefinitions.Add(new ColumnDefinition()); columns.ColumnDefinitions.Add(new ColumnDefinition());
        columns.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto }); columns.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        var left = Column(SectionHeader($"今日待办（{tasks.Count}）", Action("输入规划", CreateTransientPlanning)));
        left.Margin = new Thickness(0, 0, 18, 0);
        AppendTodos(left, tasks);
        if (tasks.Count == 0) left.Children.Add(Card(Text("今天还没有安排任务。", 14, Muted)));
        left.Children.Add(Action("＋ 新增待办", () => EditItem(false), true));
        var right = Column(SectionHeader($"今日日程（{schedules.Count}）", Action("查看日历", () => Navigate("日历"))));
        var date = Column(Text(today.ToString("M月"), 14, Muted, true), Text(today.ToString("ddd", CultureInfo.GetCultureInfo("zh-CN")), 16, B("#568BC5"), true), Text(today.Day.ToString(), 40, bold: true));
        date.Width = 64;
        foreach (var label in date.Children.OfType<TextBlock>()) label.TextAlignment = TextAlignment.Center;
        date.Margin = new Thickness(0, 0, 12, 0);
        var scheduleBody = Column();
        foreach (var item in schedules) scheduleBody.Children.Add(EventRow(item));
        if (schedules.Count == 0) scheduleBody.Children.Add(Text("暂无日程", 14, Muted));
        var tomorrow = EventsOn(today.AddDays(1)).ToList();
        scheduleBody.Children.Add(Text($"明天（{tomorrow.Count}）", 19, Muted, true));
        foreach (var item in tomorrow) scheduleBody.Children.Add(EventRow(item));
        if (tomorrow.Count == 0) scheduleBody.Children.Add(Text("明天暂无日程", 16, Muted));
        var dayGrid = new Grid(); dayGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto }); dayGrid.ColumnDefinitions.Add(new ColumnDefinition());
        dayGrid.Children.Add(date); Grid.SetColumn(scheduleBody, 1); dayGrid.Children.Add(scheduleBody);
        right.Children.Add(Card(dayGrid));
        var countdowns = workspace.Document!.Items.Where(item => workspace.Document.CountdownVisible(item, DateTime.Now)).Take(3).ToList();
        if (countdowns.Count > 0) {
            right.Children.Add(Text("值得期待", 20, bold: true));
            foreach (var item in countdowns) right.Children.Add(CountdownRow(item));
        }
        columns.Children.Add(left); Grid.SetColumn(right, 1); columns.Children.Add(right);
        columns.SizeChanged += (_, _) => {
            var compact = columns.ActualWidth < 760;
            columns.ColumnDefinitions[1].Width = compact ? new GridLength(0) : new GridLength(1, GridUnitType.Star);
            Grid.SetColumn(right, compact ? 0 : 1); Grid.SetRow(right, compact ? 1 : 0);
            left.Margin = compact ? new Thickness(0, 0, 0, 12) : new Thickness(0, 0, 18, 0);
        };
        body.Children.Add(columns);
        return Page(body, true);
    }
    private static Grid SectionHeader(string caption, UIElement action)
    {
        var grid = new Grid { Margin = new Thickness(0, 8, 0, 8) };
        grid.ColumnDefinitions.Add(new ColumnDefinition()); grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        var heading = Text(caption, 21, bold: true); heading.VerticalAlignment = VerticalAlignment.Center;
        grid.Children.Add(heading); ((FrameworkElement)action).VerticalAlignment = VerticalAlignment.Center;
        Grid.SetColumn(action, 1); grid.Children.Add(action); return grid;
    }
    private UIElement EventRow(DesktopItem item)
    {
        var ongoing = Local(item.StartAtMillis) <= DateTime.Now && Local(item.EndAtMillis) > DateTime.Now;
        var body = Column(Text(item.Title, 19, bold: true), Text(TimeLabel(item), 15, Muted));
        if (item.Location.Length > 0) body.Children.Add(Text("@" + item.Location.TrimStart('@'), 15, Muted));
        var row = new Border { Padding = new Thickness(14, 10, 10, 10), Margin = new Thickness(0, 0, 0, 14),
            BorderBrush = B(ongoing ? "#E8B841" : item.AccentColor), BorderThickness = new Thickness(4, ongoing ? 1 : 0, ongoing ? 1 : 0, ongoing ? 1 : 0),
            CornerRadius = new CornerRadius(6), Background = ongoing ? B("#FFFAE9") : Paper, Child = body,
            Cursor = System.Windows.Input.Cursors.Hand };
        row.MouseLeftButtonUp += (_, _) => Preview(item); return row;
    }
    private UIElement TodoRow(DesktopItem item, bool nested = false)
    {
        var groups = workspace.Document!.GroupsForItem(item);
        var group = groups.FirstOrDefault();
        var overdue = item.DueAtMillis != long.MaxValue && Local(item.DueAtMillis) < DateTime.Now && !item.Completed && !item.Canceled;
        var ink = overdue ? B(Dark ? "#FFA8A0" : "#AC352D") : Ink;
        var grid = new Grid(); grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(46) }); grid.ColumnDefinitions.Add(new ColumnDefinition());
        var complete = TaskToggle(item.Completed, !item.Canceled);
        complete.ToolTip = item.Completed ? "恢复待办" : "我已完成";
        System.Windows.Automation.AutomationProperties.SetName(complete, (item.Completed ? "恢复待办：" : "完成待办：") + item.Title);
        complete.Checked += (_, _) => Change(document => document.Complete(item.Id));
        complete.Unchecked += (_, _) => Change(document => document.Restore(item.Id));
        grid.Children.Add(complete);
        var heading = new WrapPanel { VerticalAlignment = VerticalAlignment.Center };
        foreach (var tag in groups) {
            var label = Text(tag.Name, 13, AccentInk(tag.ColorHex)); label.Margin = new Thickness(0);
            heading.Children.Add(new Border { Background = AccentSurface(tag.ColorHex), CornerRadius = new CornerRadius(12),
                Padding = new Thickness(8, 3, 8, 3), Margin = new Thickness(0, 3, 8, 3), Child = label });
        }
        var titleText = Text(item.Title, 19, ink, true); titleText.Margin = new Thickness(0, 2, 0, 5);
        heading.Children.Add(titleText);
        var text = Column(heading);
        if (item.DueAtMillis != long.MaxValue) {
            var deadline = Text(TimeLabel(item), 16, overdue ? ink : Forest, true); deadline.Margin = new Thickness(0);
            var deadlineRow = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(0, 4, 0, 4) };
            deadlineRow.Children.Add(AlarmIcon()); deadlineRow.Children.Add(deadline); text.Children.Add(deadlineRow);
        }
        var details = new List<string>();
        if (item.RecurrenceType != "NONE") details.Add("循环任务");
        if (item.Canceled) details.Add("已取消"); else if (item.Completed) details.Add("已完成");
        if (details.Count > 0) text.Children.Add(Text(string.Join(" · ", details), 14, Muted));
        if (item.Notes.Length > 0) text.Children.Add(Text(item.Notes, 14, Muted));
        text.Cursor = System.Windows.Input.Cursors.Hand; text.MouseLeftButtonUp += (_, e) => { Preview(item); e.Handled = true; };
        Grid.SetColumn(text, 1); grid.Children.Add(text);
        if (nested && workspace.Document.Items.Any(child => child.ParentTodoId == item.Id)) {
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            var fold = Action(item.QuickChecklistCollapsed ? "▸" : "▾", () => Change(document => document.ToggleQuickChecklistItem(item.Id)));
            fold.ToolTip = item.QuickChecklistCollapsed ? "展开子任务" : "收起子任务";
            System.Windows.Automation.AutomationProperties.SetName(fold, fold.ToolTip.ToString());
            fold.Width = 28; fold.MinHeight = 28; fold.Padding = new Thickness(3); fold.Margin = new Thickness(8, 0, 0, 0);
            fold.Background = System.Windows.Media.Brushes.Transparent; fold.BorderThickness = new Thickness(0);
            Grid.SetColumn(fold, 2); grid.Children.Add(fold);
        }
        var layout = new Grid();
        var stripe = new Border { Width = 5, Background = B(group?.ColorHex ?? item.AccentColor), CornerRadius = new CornerRadius(3),
            HorizontalAlignment = HorizontalAlignment.Left, Margin = new Thickness(0, 14, 0, 14) };
        layout.Children.Add(stripe);
        grid.Margin = new Thickness(18, 18, 18, 18); layout.Children.Add(grid);
        var card = Card(layout, 0); card.Margin = new Thickness(nested ? Math.Max(0, Math.Min(12, item.QuickChecklistDepth ?? 1) - 1) * 16 : 0, 0, 0, 12);
        System.Windows.Automation.AutomationProperties.SetAutomationId(card, "Todo.Card." + item.Id);
        return card;
    }
    private void AppendTodos(StackPanel host, IReadOnlyList<DesktopItem> items, bool foldRecurring = false)
    {
        var ordinary = items.Where(item => item.QuickChecklistGroupId == null).ToList();
        var series = ordinary.Where(item => foldRecurring && item.RecurrenceType != "NONE" && !string.IsNullOrEmpty(item.RecurringSeriesId)
                && Local(item.DueAtMillis).Date > DateTime.Today)
            .GroupBy(item => item.RecurringSeriesId!).ToDictionary(group => group.Key, group => group.OrderBy(item => item.DueAtMillis).ToList());
        var emitted = new HashSet<string>();
        foreach (var item in ordinary) {
            if (item.RecurringSeriesId is { } key && series.TryGetValue(key, out var instances) && instances.Count > 1 && instances.Contains(item)) {
                if (!emitted.Add(key)) continue;
                var expanded = expandedTodoSeries.Contains(key);
                var toggle = Action($"{(expanded ? "▾" : "▸")}  {instances[0].Title} · {instances.Count} 次循环", () => {
                    if (!expandedTodoSeries.Add(key)) expandedTodoSeries.Remove(key); Render();
                });
                System.Windows.Automation.AutomationProperties.SetAutomationId(toggle, "Todo.Series." + key);
                host.Children.Add(toggle);
                if (expanded) foreach (var instance in instances) host.Children.Add(TodoRow(instance));
                else host.Children.Add(TodoRow(instances[0]));
            } else host.Children.Add(TodoRow(item));
        }
        foreach (var grouping in items.Where(item => item.QuickChecklistGroupId != null).GroupBy(item => item.QuickChecklistGroupId!.Value))
        {
            var group = workspace.Document!.QuickChecklistGroups.FirstOrDefault(group => group.Id == grouping.Key);
            if (group == null) continue;
            var all = workspace.Document.Items.Where(item => item.QuickChecklistGroupId == group.Id).ToList();
            // Import provenance remains stored, but tasks have no extra group container.
            var hidden = new HashSet<long>();
            foreach (var item in grouping.OrderBy(item => item.Id))
            {
                if (item.ParentTodoId is { } parent && hidden.Contains(parent)) { hidden.Add(item.Id); continue; }
                if (item.QuickChecklistCollapsed) hidden.Add(item.Id);
                host.Children.Add(TodoRow(item, true));
            }
        }
    }
    private UIElement TodosPage()
    {
        var filters = new ComboBox { MinWidth = 150, MaxWidth = 230, Height = 40, MinHeight = 40, Margin = new Thickness(12, 0, 0, 6), DisplayMemberPath = "Name", VerticalAlignment = VerticalAlignment.Top };
        System.Windows.Automation.AutomationProperties.SetName(filters, "待办分组");
        var groups = new[] { new DesktopGroup(0, "全部分组", "#326F59") }.Concat(workspace.Document!.Groups).ToList();
        filters.ItemsSource = groups; filters.SelectedItem = groups.FirstOrDefault(group => group.Id == (selectedGroup ?? 0)) ?? groups[0];
        filters.SelectionChanged += (_, _) => { selectedGroup = (filters.SelectedItem as DesktopGroup)?.Id; Render(); };
        var toolbar = new Grid(); toolbar.ColumnDefinitions.Add(new ColumnDefinition()); toolbar.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        var actions = Row(Action("＋ 新增待办", () => EditItem(false), true),
            Action(history ? "返回待办" : "历史记录", () => { history = !history; Render(); }));
        foreach (var button in actions.Children.OfType<Button>()) { button.Height = 40; button.Padding = new Thickness(14, 6, 14, 6); }
        toolbar.Children.Add(actions);
        Grid.SetColumn(filters, 1); toolbar.Children.Add(filters);
        var body = Column(toolbar);
        var items = workspace.Document.Items.Where(item => !item.IsEvent && (history ? item.Completed || item.Canceled : !item.Completed && !item.Canceled))
            .Where(item => selectedGroup is null or 0 || item.GroupId == selectedGroup).ToList();
        AppendTodos(body, items, foldRecurring: !history);
        if (items.Count == 0) body.Children.Add(Card(Text(history ? "暂无历史记录" : "所有事情都处理好了。", 18, Muted)));
        return Page(body, true);
    }
    private UIElement SearchPage()
    {
        var query = search.Text.Trim();
        var body = Column(Text("搜索：" + query, 18, bold: true));
        var items = workspace.Document!.Items.Where(item => query.Length > 0 && CanReadItemDetails(item) &&
            (item.Title + " " + item.Notes + " " + item.Location).Contains(query, StringComparison.CurrentCultureIgnoreCase)).ToList();
        foreach (var item in items) body.Children.Add(item.IsEvent ? Card(EventRow(item)) : TodoRow(item));
        if (items.Count == 0) body.Children.Add(Text("没有找到匹配的事项。", 14, Muted)); return Page(body);
    }
    private UIElement CountdownRow(DesktopItem item)
    {
        var target = item.IsEvent ? Local(item.StartAtMillis) : Local(item.DueAtMillis);
        var days = (target.Date - DateTime.Today).Days;
        var card = Card(SectionHeader(item.Title, Text(days >= 0 ? $"还有 {days} 天" : $"已过 {-days} 天", 20, Forest, true)));
        var groupColor = workspace.Document!.Groups.FirstOrDefault(group => group.Id == item.GroupId)?.ColorHex;
        card.BorderBrush = B(item.IsEvent ? item.AccentColor : groupColor ?? item.AccentColor);
        card.BorderThickness = new Thickness(8, 0, 0, 0);
        card.MouseLeftButtonUp += (_, _) => Preview(item); return card;
    }
    private UIElement CountdownPage()
    {
        var body = Column(); foreach (var item in workspace.Document!.Items.Where(item => workspace.Document.CountdownVisible(item, DateTime.Now))) body.Children.Add(CountdownRow(item));
        if (body.Children.Count == 0) body.Children.Add(Card(Text("编辑待办或日程时勾选“倒数日”，重要日子会出现在这里。", 16, Muted)));
        return Page(body, true);
    }
    private bool CanReadItemDetails(DesktopItem? item)
    {
        if (item == null || workspace.Document == null) return false;
        if (!DesktopSnapshotDocument.IsSharedCalendar(item.CalendarId)) return true;
        var calendar = workspace.Document.Calendars.FirstOrDefault(value => value.Id == item.CalendarId);
        return workspace.Account != null && calendar?.ShareMode == "SHARED" &&
            calendar.SharedRole.ToLowerInvariant() is "owner" or "editor" or "viewer" &&
            string.Equals(calendar.SharedServerUrl, workspace.Account.ServerUrl, StringComparison.OrdinalIgnoreCase);
    }
    private void Preview(DesktopItem initial)
    {
        var id = initial.Id; var calendarId = initial.CalendarId; var generation = workspace.AccountGeneration;
        var current = workspace.Document?.Items.FirstOrDefault(value => value.Id == id);
        if (!CanReadItemDetails(current)) { workspace.Notify("这条事项当前不可用或已无访问权限。"); return; }
        var dialog = Dialog(this, initial.IsEvent ? "日程详情" : "待办详情", 620, 650);
        var closed = false; var replyStatus = "";
        bool Accessible() => generation == workspace.AccountGeneration && CanReadItemDetails(workspace.Document?.Items.FirstOrDefault(value => value.Id == id)) &&
            workspace.Document?.Items.FirstOrDefault(value => value.Id == id)?.CalendarId == calendarId;
        void Refresh()
        {
            if (closed) return;
            if (!Accessible()) { dialog.Content = null; dialog.Close(); return; }
            var item = workspace.Document!.Items.First(value => value.Id == id);
            var edit = workspace.Document.EditFor(id)!;
            var body = Column(BrandHeading(dialog.Title), Text(item.Title, 24, bold: true), Text(TimeLabel(item), 16, item.IsEvent ? Ocean : Forest));
            if (item.IsEvent) {
                var calendar = workspace.Document.Calendars.FirstOrDefault(value => value.Id == item.CalendarId);
                body.Children.Add(Text("日历  ·  " + (calendar?.Name ?? "常规日历"), 14, B(calendar?.ColorHex ?? item.AccentColor)));
                if (item.RecurringSeriesId != null) body.Children.Add(Text("循环日程 · 可选择当前、当前及之后或全部进行编辑", 13, Muted));
                body.Children.Add(Text(edit.ReminderEnabled ? "提醒  ·  " + (item.AllDay && !string.IsNullOrWhiteSpace(edit.AllDayReminderRulesJson)
                    ? DesktopCalendarTime.RulesText(edit.AllDayReminderRulesJson) : string.Join("、", edit.ReminderOffsets.Select(value => value == 0 ? "开始时" : $"提前 {value} 分钟"))) : "提醒  ·  无", 14, Muted));
                body.Children.Add(Text("时区  ·  " + (string.IsNullOrWhiteSpace(item.EventTimeZoneId) ? "跟随设备" : item.EventTimeZoneId), 13, Muted));
                body.Children.Add(Text((item.EventBusyStatus == "FREE" ? "空闲" : "忙碌") + " · " + (item.EventVisibility == "PRIVATE" ? "私密" : item.EventVisibility == "PUBLIC" ? "公开" : "沿用日历设置"), 13, Muted));
                if (calendar != null && DesktopSnapshotDocument.IsSharedCalendar(item.CalendarId)) {
                    var members = DesktopEventParticipants.Members(calendar).ToDictionary(value => value.Username);
                    var organizer = members.GetValueOrDefault(calendar.OwnerAccount.ToLowerInvariant())?.Label ?? calendar.OwnerAccount;
                    body.Children.Add(Text("组织者  ·  " + organizer, 14));
                    if (item.EventVisibility == "PRIVATE" && calendar.SharedRole.Equals("viewer", StringComparison.OrdinalIgnoreCase))
                        body.Children.Add(Text("私密日程 · 仅显示你的回应", 13, Muted));
                    body.Children.Add(Text("参与者", 15, bold: true));
                    var participants = DesktopEventParticipants.Read(item.EventParticipantsJson);
                    if (participants.Count == 0) body.Children.Add(Text(item.EventVisibility == "PRIVATE" && calendar.SharedRole.Equals("viewer", StringComparison.OrdinalIgnoreCase)
                        ? "参与者名单受限；你尚未回应。" : "尚无参与者", 13, Muted));
                    foreach (var participant in participants) body.Children.Add(Text((members.GetValueOrDefault(participant.Username)?.Label ?? participant.Username) + "  ·  " + DesktopEventParticipants.ResponseLabel(participant.Response), 14));
                    if (!string.IsNullOrWhiteSpace(item.SharedEventId)) {
                        var responses = new[] { "NEEDS_ACTION", "ACCEPTED", "TENTATIVE", "DECLINED" };
                        var own = participants.FirstOrDefault(value => value.Username == workspace.Account!.Username.ToLowerInvariant())?.Response ?? "NEEDS_ACTION";
                        var response = new ComboBox { ItemsSource = responses.Select(DesktopEventParticipants.ResponseLabel).ToArray(), SelectedIndex = Array.IndexOf(responses, own), MinWidth = 110 };
                        System.Windows.Automation.AutomationProperties.SetAutomationId(response, "Event.Rsvp.Response");
                        var status = Text(replyStatus, 13, Muted);
                        Button? reply = null;
                        reply = Action("回复日程", async () => {
                            if (!Accessible()) { Refresh(); return; }
                            if (response.SelectedIndex <= 0) { status.Text = "请选择接受、暂定或拒绝。"; return; }
                            response.IsEnabled = false;
                            reply!.IsEnabled = false;
                            try {
                                await workspace.RespondToSharedEventAsync(item.CalendarId, item.SharedEventId, responses[response.SelectedIndex]);
                                if (!Accessible()) { Refresh(); return; }
                                try {
                                    await workspace.SyncSharedCalendarsAsync(waitForGate: true);
                                    replyStatus = workspace.Document?.SharedCalendar(item.CalendarId)?["sharedConflictRevision"] != null
                                        ? "回应已发送；日历有版本冲突，请处理同步。" : "回应已更新。";
                                } catch (Exception failure) { replyStatus = "回应已发送，共享日历暂未同步：" + DesktopWorkspace.FriendlyError(failure); }
                                Refresh();
                            } catch (Exception failure) { if (Accessible() && !closed) replyStatus = DesktopWorkspace.FriendlyError(failure); Refresh(); }
                        });
                        body.Children.Add(Row(Text("我的回应", 13), response, reply)); body.Children.Add(status);
                    }
                }
            }
            if (!string.IsNullOrWhiteSpace(item.Location)) body.Children.Add(Text("地点  ·  " + item.Location, 14, Muted));
            if (!string.IsNullOrWhiteSpace(item.Notes)) body.Children.Add(Text(item.Notes, 15));
            var actions = new WrapPanel { Margin = new Thickness(0, 22, 0, 0) };
            void Add(string label, Action action, bool primary = false) {
                var button = Action(label, () => { if (!Accessible()) { Refresh(); return; } dialog.Close(); action(); }, primary);
                if (item.IsEvent && primary) { button.Background = B("#3370FF"); button.BorderBrush = B("#3370FF"); }
                actions.Children.Add(button);
            }
            var writable = workspace.Document.SharedWritable(item.CalendarId);
            if (writable) Add("编辑", () => EditItem(item.IsEvent, item.Id), true);
            Add("复制", () => CopyItem(item.Id));
            if (!item.IsEvent) Add(item.Completed || item.Canceled ? "恢复" : "我已完成", () => Change(document => { if (item.Completed || item.Canceled) document.Restore(item.Id); else document.Complete(item.Id); }));
            if (item.IsEvent && edit.CheckInEnabled) Add(workspace.Document.CheckedIn(item.Id) ? "签退" : "签到", () => Change(document => document.ToggleCheckIn(item.Id)));
            void Remove(bool cancel) {
                var scope = item.RecurringSeriesId != null ? NativeEditors.ChooseRecurrenceScope(this, cancel ? "取消循环事项" : "删除循环事项") : RecurrenceScope.Current;
                if (scope == null || !Confirm(cancel ? "取消所选事项并放入历史记录？" : "删除所选事项？子任务也会一并删除。")) return;
                Change(document => { if (cancel) document.Cancel(item.Id, scope.Value); else document.Delete(item.Id, scope.Value); });
            }
            if (writable && !item.Completed && !item.Canceled) Add("取消事项", () => Remove(true));
            if (writable) Add("删除", () => Remove(false));
            actions.Children.Add(Action("关闭", () => dialog.Close()));
            dialog.Content = DialogLayout(body, actions); dialog.UpdateLayout();
        }
        void Changed() => dialog.Dispatcher.Invoke(Refresh);
        workspace.Changed += Changed;
        dialog.Closed += (_, _) => { closed = true; workspace.Changed -= Changed; dialog.Content = null; };
        Refresh(); dialog.ShowDialog();
    }
}
