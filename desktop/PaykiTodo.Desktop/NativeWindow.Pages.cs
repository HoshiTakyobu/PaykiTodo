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
        var greeting = Text(DateTime.Now.Hour < 12 ? "早上好，开启新的一天" : DateTime.Now.Hour < 18 ? "下午好，按自己的节奏来" : "晚上好，给今天一个好结尾", 28, Forest, true);
        greeting.FontFamily = new System.Windows.Media.FontFamily(new Uri("pack://application:,,,/PaykiTodo;component/"), "./Assets/#FZQingKeBenYueSongS-R-GB");
        var body = Column(Card(Column(greeting, Text(today.ToString("yyyy年M月d日  dddd", CultureInfo.GetCultureInfo("zh-CN")), 14, Muted))));
        var columns = new Grid(); columns.ColumnDefinitions.Add(new ColumnDefinition()); columns.ColumnDefinitions.Add(new ColumnDefinition());
        columns.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto }); columns.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        var left = Column(SectionHeader($"今日待办（{tasks.Count}）", Action("! ▤ 快速清单", CreateChecklist)));
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
        complete.Click += (_, _) => Change(document => { if (item.Completed) document.Restore(item.Id); else document.Complete(item.Id); });
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
        var body = Column(Row(Action("＋ 新增待办", () => EditItem(false), true), Action("! ▤ 快速清单", CreateChecklist),
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
    private UIElement CalendarPage()
    {
        var view = new ComboBox { ItemsSource = new[] { "日", "周", "月", "列表" }, SelectedItem = calendarView, Width = 90 };
        System.Windows.Automation.AutomationProperties.SetName(view, "日历视图");
        view.SelectionChanged += (_, _) => { calendarView = view.SelectedItem?.ToString() ?? "周"; Render(); };
        void Move(int direction) { selectedDay = calendarView == "月" ? selectedDay.AddMonths(direction) : selectedDay.AddDays(direction * (calendarView == "周" ? 7 : 1)); Render(); }
        var selectedDate = Text(selectedDay.ToString("yyyy年M月d日"), 18, bold: true); selectedDate.VerticalAlignment = VerticalAlignment.Center; selectedDate.Margin = new Thickness(8, 0, 16, 12);
        var bar = Row(Action("今天", () => { selectedDay = DateTime.Today; Render(); }), Action("‹", () => Move(-1)), Action("›", () => Move(1)),
            selectedDate, view, Action("＋ 创建日程", () => EditItem(true), true));
        var body = new DockPanel { Margin = new Thickness(26, 8, 26, 16) }; DockPanel.SetDock(bar, Dock.Top); body.Children.Add(bar);
        if (calendarView == "月") { body.Children.Add(MonthCalendar()); return body; }
        if (calendarView == "列表") {
            var list = Column();
            foreach (var item in Active.Where(item => item.IsEvent && Local(item.EndAtMillis) >= selectedDay.Date)) list.Children.Add(Card(EventRow(item)));
            if (list.Children.Count == 0) list.Children.Add(Text("暂无日程", 16, Muted)); body.Children.Add(Scroll(list)); return body;
        }
        var count = calendarView == "日" ? 1 : ActualWidth < 1100 ? 3 : 7;
        var sunday = workspace.Document!.Settings["weekStartMode"]?.GetValue<string>() == "SUNDAY";
        var start = count != 7 ? selectedDay.Date : selectedDay.Date.AddDays(-(((int)selectedDay.DayOfWeek + (sunday ? 0 : 6)) % 7));
        var grid = new Grid { Background = Paper, Height = 24 * 60 + 40 };
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(48) });
        for (var column = 0; column < count; column++) grid.ColumnDefinitions.Add(new ColumnDefinition());
        for (var hour = 0; hour < 24; hour++) {
            var label = Text($"{hour:00}:00", 11, Muted); label.VerticalAlignment = VerticalAlignment.Top; label.Margin = new Thickness(0, hour * 60 + 38, 4, 0); grid.Children.Add(label);
        }
        for (var column = 0; column < count; column++) {
            var day = start.AddDays(column);
            var canvas = new Canvas { Background = day == DateTime.Today ? B("#F6FBF8") : Paper, ClipToBounds = true };
            Grid.SetColumn(canvas, column + 1); grid.Children.Add(canvas);
            var header = Text(day.ToString("ddd  dd", CultureInfo.GetCultureInfo("zh-CN")), 14, day == DateTime.Today ? Forest : Muted, true);
            header.TextAlignment = TextAlignment.Center; canvas.SizeChanged += (_, _) => header.Width = Math.Max(0, canvas.ActualWidth - 16);
            Canvas.SetLeft(header, 8); Canvas.SetTop(header, 6); canvas.Children.Add(header);
            for (var hour = 0; hour < 24; hour++) {
                var line = new Border { Height = 1, Background = B("#E3EBE6") };
                line.SetBinding(FrameworkElement.WidthProperty, new System.Windows.Data.Binding("ActualWidth") { Source = canvas });
                Canvas.SetTop(line, hour * 60 + 40); canvas.Children.Add(line);
            }
            canvas.MouseLeftButtonDown += (_, e) => { if (e.ClickCount == 2) { var minute = Math.Clamp((int)((e.GetPosition(canvas).Y - 40) / 15) * 15, 0, 1425); EditItem(true, seed: day.AddMinutes(minute)); } };
            var events = EventsOn(day).OrderBy(item => item.StartAtMillis).ToList();
            var layout = CalendarEventLayout.Arrange(events, day);
            foreach (var item in events) {
                var itemStart = Local(item.StartAtMillis); var itemEnd = Local(item.EndAtMillis);
                var y = Math.Max(0, (itemStart - day).TotalMinutes); var h = Math.Max(36, Math.Min(1440, (itemEnd - day).TotalMinutes) - y);
                var position = layout[item.Id];
                var tile = new Border { Background = B(item.AccentColor), CornerRadius = new CornerRadius(5), Padding = new Thickness(6), Height = h - 2,
                    Child = Column(Text(item.Title, 12, B("#FFFFFF"), true), Text(itemStart.ToString("HH:mm"), 11, B("#FFFFFF"))), ClipToBounds = true,
                    Opacity = itemEnd < DateTime.Now ? 0.5 : 0.94, ToolTip = item.Title + "\n" + TimeLabel(item) + "\n" + item.Location };
                void Position() { tile.Width = Math.Max(20, (canvas.ActualWidth - 5) / position.Columns - 3); Canvas.SetLeft(tile, position.Lane * (canvas.ActualWidth - 5) / position.Columns + 2); }
                canvas.SizeChanged += (_, _) => Position(); Canvas.SetTop(tile, y + 40); canvas.Children.Add(tile);
                tile.MouseLeftButtonUp += (_, e) => { Preview(item); e.Handled = true; };
            }
            if (day == DateTime.Today) {
                var line = new Border { Height = 2, Background = B("#DA735E"), IsHitTestVisible = false };
                line.SetBinding(FrameworkElement.WidthProperty, new System.Windows.Data.Binding("ActualWidth") { Source = canvas });
                Canvas.SetTop(line, DateTime.Now.TimeOfDay.TotalMinutes + 40); canvas.Children.Add(line);
            }
        }
        var scroll = Scroll(grid); scroll.Loaded += (_, _) => scroll.ScrollToVerticalOffset(7 * 60); body.Children.Add(scroll); return body;
    }
    private UIElement MonthCalendar()
    {
        var first = new DateTime(selectedDay.Year, selectedDay.Month, 1); first = first.AddDays(-(((int)first.DayOfWeek + 6) % 7));
        var grid = new System.Windows.Controls.Primitives.UniformGrid { Columns = 7, Rows = 6 };
        for (var i = 0; i < 42; i++) {
            var day = first.AddDays(i); var events = EventsOn(day).ToList();
            var box = Column(Text(day.ToString("M/d"), 16, day == DateTime.Today ? Forest : Muted, true));
            foreach (var item in events.Take(3)) box.Children.Add(Text(item.Title, 12, B(item.AccentColor)));
            if (events.Count > 3) box.Children.Add(Text($"还有 {events.Count - 3} 项", 11, Muted));
            var cell = new Border { Child = box, Padding = new Thickness(10), Margin = new Thickness(2), Background = day == DateTime.Today ? B("#E2EFE6") : Paper,
                Opacity = day.Month == selectedDay.Month ? 1 : 0.5, Cursor = System.Windows.Input.Cursors.Hand };
            cell.MouseLeftButtonUp += (_, _) => { selectedDay = day; calendarView = "日"; Render(); }; grid.Children.Add(cell);
        }
        return grid;
    }
}
