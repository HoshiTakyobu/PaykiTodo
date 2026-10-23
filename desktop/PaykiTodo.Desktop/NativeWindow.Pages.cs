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

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow
{
    private UIElement BoardPage()
    {
        var today = DateTime.Today;
        var tasks = Active.Where(item => !item.IsEvent && !workspace.Document!.HiddenOnBoard(item.Id) &&
            (item.DueAtMillis == long.MaxValue || Local(item.DueAtMillis).Date <= today)).ToList();
        var schedules = EventsOn(today).Where(item => Local(item.EndAtMillis) > DateTime.Now).ToList();
        var greeting = Text(DateTime.Now.Hour < 12 ? "早上好，开启新的一天" : DateTime.Now.Hour < 18 ? "下午好，按自己的节奏来" : "晚上好，给今天一个好结尾", 28, Ocean);
        greeting.FontFamily = HeadingFont;
        var body = Column(Card(Column(greeting, Text(today.ToString("yyyy年M月d日  dddd", CultureInfo.GetCultureInfo("zh-CN")), 14, Muted))));
        var columns = new Grid(); columns.ColumnDefinitions.Add(new ColumnDefinition()); columns.ColumnDefinitions.Add(new ColumnDefinition());
        columns.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto }); columns.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        var left = Column(SectionHeader($"今日待办（{tasks.Count}）", ChecklistAction(CreateChecklist)));
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
        scheduleBody.Children.Add(Text("明天", 18, Muted, true));
        var tomorrow = EventsOn(today.AddDays(1)).ToList();
        foreach (var item in tomorrow) scheduleBody.Children.Add(EventRow(item));
        if (tomorrow.Count == 0) scheduleBody.Children.Add(Text("明天暂无日程", 14, Muted));
        var dayGrid = new Grid(); dayGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto }); dayGrid.ColumnDefinitions.Add(new ColumnDefinition());
        dayGrid.Children.Add(date); Grid.SetColumn(scheduleBody, 1); dayGrid.Children.Add(scheduleBody);
        right.Children.Add(Card(dayGrid));
        var countdowns = Active.Where(item => item.CountdownEnabled).Take(3).ToList();
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
        var heading = Text(caption, 18, bold: true); heading.VerticalAlignment = VerticalAlignment.Center;
        grid.Children.Add(heading); ((FrameworkElement)action).VerticalAlignment = VerticalAlignment.Center;
        Grid.SetColumn(action, 1); grid.Children.Add(action); return grid;
    }
    private UIElement EventRow(DesktopItem item)
    {
        var ongoing = Local(item.StartAtMillis) <= DateTime.Now && Local(item.EndAtMillis) > DateTime.Now;
        var body = Column(Text(item.Title, 16, bold: true), Text(TimeLabel(item), 13, Muted));
        if (item.Location.Length > 0) body.Children.Add(Text("@" + item.Location.TrimStart('@'), 13, Muted));
        var row = new Border { Padding = new Thickness(14, 10, 10, 10), Margin = new Thickness(0, 0, 0, 14),
            BorderBrush = B(ongoing ? "#E8B841" : item.AccentColor), BorderThickness = new Thickness(4, ongoing ? 1 : 0, ongoing ? 1 : 0, ongoing ? 1 : 0),
            CornerRadius = new CornerRadius(6), Background = ongoing ? B("#FFFAE9") : Paper, Child = body,
            Cursor = System.Windows.Input.Cursors.Hand };
        row.MouseLeftButtonUp += (_, _) => Preview(item); return row;
    }
    private UIElement TodoRow(DesktopItem item, bool nested = false)
    {
        var group = workspace.Document!.Groups.FirstOrDefault(group => group.Id == item.GroupId);
        var grid = new Grid(); grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(36) }); grid.ColumnDefinitions.Add(new ColumnDefinition());
        var complete = new CheckBox { IsChecked = item.Completed, IsEnabled = !item.Canceled, VerticalAlignment = VerticalAlignment.Center,
            ToolTip = item.Completed ? "恢复待办" : "我已完成", Margin = new Thickness(0) };
        System.Windows.Automation.AutomationProperties.SetName(complete, (item.Completed ? "恢复待办：" : "完成待办：") + item.Title);
        complete.Checked += (_, _) => Change(document => document.Complete(item.Id));
        complete.Unchecked += (_, _) => Change(document => document.Restore(item.Id));
        grid.Children.Add(complete);
        var text = Column(Text(item.Title, 15, bold: true),
            Text(TimeLabel(item) + (group == null ? "" : "  ·  " + group.Name) +
                (item.Canceled ? "  ·  已取消" : item.Completed ? "  ·  已完成" : ""), 12, Muted));
        if (nested && item.Notes.Length > 0) text.Children.Add(Text(item.Notes, 12, Muted));
        text.Cursor = System.Windows.Input.Cursors.Hand; text.MouseLeftButtonUp += (_, e) => { Preview(item); e.Handled = true; };
        Grid.SetColumn(text, 1); grid.Children.Add(text);
        var card = Card(grid, 12); card.Margin = new Thickness(0, 0, 0, 8);
        card.BorderBrush = B(group?.ColorHex ?? item.AccentColor); card.BorderThickness = new Thickness(3, 0, 0, 0);
        if (nested) card.Margin = new Thickness(Math.Min(12, item.QuickChecklistDepth ?? 0) * 16, 0, 0, 8);
        return card;
    }
    private void AppendTodos(StackPanel host, IReadOnlyList<DesktopItem> items)
    {
        foreach (var item in items.Where(item => item.QuickChecklistGroupId == null)) host.Children.Add(TodoRow(item));
        foreach (var grouping in items.Where(item => item.QuickChecklistGroupId != null).GroupBy(item => item.QuickChecklistGroupId!.Value))
        {
            var group = workspace.Document!.QuickChecklistGroups.FirstOrDefault(group => group.Id == grouping.Key);
            if (group == null) continue;
            var all = workspace.Document.Items.Where(item => item.QuickChecklistGroupId == group.Id).ToList();
            var title = $"{(string.IsNullOrWhiteSpace(group.Title) ? "快速清单" : group.Title)}   {all.Count(item => item.Completed && !item.Canceled)}/{all.Count(item => !item.Canceled)}";
            host.Children.Add(Action((group.Collapsed ? "▸  " : "▾  ") + title, () => Change(document => document.ToggleQuickChecklistGroup(group.Id))));
            if (group.Collapsed) continue;
            var hidden = new HashSet<long>();
            foreach (var item in grouping.OrderBy(item => item.QuickChecklistSortOrder))
            {
                if (item.ParentTodoId is { } parent && hidden.Contains(parent)) { hidden.Add(item.Id); continue; }
                if (item.QuickChecklistCollapsed) hidden.Add(item.Id);
                if (all.Any(child => child.ParentTodoId == item.Id)) host.Children.Add(Action(item.QuickChecklistCollapsed ? "▸ 展开子任务" : "▾ 收起子任务",
                    () => Change(document => document.ToggleQuickChecklistItem(item.Id))));
                host.Children.Add(TodoRow(item, true));
            }
        }
    }
    private UIElement TodosPage()
    {
        var filters = new ComboBox { MinWidth = 150, DisplayMemberPath = "Name" };
        var groups = new[] { new DesktopGroup(0, "全部分组", "#326F59") }.Concat(workspace.Document!.Groups).ToList();
        filters.ItemsSource = groups; filters.SelectedItem = groups.FirstOrDefault(group => group.Id == (selectedGroup ?? 0)) ?? groups[0];
        filters.SelectionChanged += (_, _) => { selectedGroup = (filters.SelectedItem as DesktopGroup)?.Id; Render(); };
        var body = Column(Row(Action("＋ 新增待办", () => EditItem(false), true), ChecklistAction(CreateChecklist),
            Action(history ? "返回待办" : "历史记录", () => { history = !history; Render(); }), filters));
        var items = workspace.Document.Items.Where(item => !item.IsEvent && (history ? item.Completed || item.Canceled : !item.Completed && !item.Canceled))
            .Where(item => selectedGroup is null or 0 || item.GroupId == selectedGroup).ToList();
        AppendTodos(body, items);
        if (items.Count == 0) body.Children.Add(Card(Text(history ? "暂无历史记录" : "所有事情都处理好了。", 18, Muted)));
        return Page(body, true);
    }
    private UIElement SearchPage()
    {
        var query = search.Text.Trim();
        var body = Column(Text("搜索：" + query, 18, bold: true));
        var items = workspace.Document!.Items.Where(item => query.Length > 0 &&
            (item.Title + " " + item.Notes + " " + item.Location).Contains(query, StringComparison.CurrentCultureIgnoreCase)).ToList();
        foreach (var item in items) body.Children.Add(item.IsEvent ? Card(EventRow(item)) : TodoRow(item));
        if (items.Count == 0) body.Children.Add(Text("没有找到匹配的事项。", 14, Muted)); return Page(body);
    }
    private UIElement CountdownRow(DesktopItem item)
    {
        var target = item.IsEvent ? Local(item.StartAtMillis) : Local(item.DueAtMillis);
        var days = (target.Date - DateTime.Today).Days;
        var card = Card(SectionHeader(item.Title, Text(days >= 0 ? $"还有 {days} 天" : $"已过 {-days} 天", 20, Forest, true)));
        card.MouseLeftButtonUp += (_, _) => Preview(item); return card;
    }
    private UIElement CountdownPage()
    {
        var body = Column(); foreach (var item in Active.Where(item => item.CountdownEnabled)) body.Children.Add(CountdownRow(item));
        if (body.Children.Count == 0) body.Children.Add(Card(Text("编辑待办或日程时勾选“倒数日”，重要日子会出现在这里。", 16, Muted)));
        return Page(body, true);
    }
    private void Preview(DesktopItem item)
    {
        var dialog = Dialog(this, item.IsEvent ? "日程详情" : "待办详情", 580, 490);
        var body = Column(BrandHeading(dialog.Title), Text(item.Title, 24, bold: true), Text(TimeLabel(item), 16, Forest), Text(item.Location, 14, Muted), Text(item.Notes));
        var actions = new WrapPanel { Margin = new Thickness(0, 22, 0, 0) };
        void Add(string label, Action action, bool primary = false) => actions.Children.Add(Action(label, () => { dialog.Close(); action(); }, primary));
        Add("编辑", () => EditItem(item.IsEvent, item.Id), true);
        if (!item.IsEvent) Add(item.Completed || item.Canceled ? "恢复" : "我已完成", () => Change(document => {
            if (item.Completed || item.Canceled) document.Restore(item.Id); else document.Complete(item.Id);
        }));
        if (item.IsEvent && workspace.Document!.EditFor(item.Id)?.CheckInEnabled == true)
            Add(workspace.Document.CheckedIn(item.Id) ? "签退" : "签到", () => Change(document => document.ToggleCheckIn(item.Id)));
        if (!item.Completed && !item.Canceled) Add("取消事项", () => { if (Confirm("取消该事项并放入历史记录？")) Change(document => document.Cancel(item.Id)); });
        Add("删除", () => { if (Confirm("删除该事项？子任务也会一并删除。")) Change(document => document.Delete(item.Id)); });
        dialog.Content = DialogLayout(body, actions); dialog.ShowDialog();
    }
}
