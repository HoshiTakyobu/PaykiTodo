using System.Globalization;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Threading;
using static PaykiTodo.Desktop.NativeUi;
using Button = System.Windows.Controls.Button;
using ComboBox = System.Windows.Controls.ComboBox;
using HorizontalAlignment = System.Windows.HorizontalAlignment;
using Point = System.Windows.Point;
using Pen = System.Windows.Media.Pen;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow
{
    private const double HourHeight = 48;
    private const double TimeInset = 12;
    private static readonly CultureInfo CalendarCulture = CultureInfo.GetCultureInfo("zh-CN");
    private DispatcherTimer? calendarClock;
    private readonly HashSet<long> hiddenCalendarGroups = new();

    private Button CalendarAction(string label, Action click, bool primary = false)
    {
        var button = Action(label, click, primary);
        button.MinHeight = 32; button.Height = 34; button.Padding = new Thickness(12, 5, 12, 5); button.Margin = new Thickness(0, 0, 6, 0);
        button.Background = primary ? B("#326DD6") : Paper; button.BorderBrush = primary ? B("#326DD6") : Outline;
        return button;
    }

    private UIElement CalendarPage()
    {
        calendarClock?.Stop();
        var count = calendarView == "日" ? 1 : ActualWidth < 1100 ? 3 : 7;
        var sunday = workspace.Document!.Settings["weekStartMode"]?.GetValue<string>() == "SUNDAY";
        var start = count != 7 ? selectedDay.Date : WeekStart(selectedDay, sunday);
        var root = new Grid { Background = Paper, Margin = new Thickness(18, 0, 18, 14) };
        root.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(ActualWidth >= 1100 ? 208 : 0) }); root.ColumnDefinitions.Add(new ColumnDefinition());
        var mini = MiniCalendar(sunday); mini.Visibility = ActualWidth >= 1100 ? Visibility.Visible : Visibility.Collapsed; root.Children.Add(mini);
        var main = new DockPanel { Margin = new Thickness(16, 12, 0, 0) }; Grid.SetColumn(main, 1); root.Children.Add(main);
        var toolbar = new Grid { Height = 46, Margin = new Thickness(0, 0, 0, 6) };
        toolbar.ColumnDefinitions.Add(new ColumnDefinition()); toolbar.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        void Move(int direction) { selectedDay = calendarView == "月" ? selectedDay.AddMonths(direction) : selectedDay.AddDays(direction * (calendarView == "周" ? 7 : 1)); Render(); }
        var range = calendarView == "月" ? selectedDay.ToString("yyyy年M月") : calendarView == "周" ? $"{start:yyyy年M月d日} – {start.AddDays(count - 1):M月d日}" : selectedDay.ToString("yyyy年M月d日");
        var rangeText = Text(range, 16, bold: true); rangeText.Margin = new Thickness(8, 0, 0, 0); rangeText.VerticalAlignment = VerticalAlignment.Center;
        var navigation = Row(CalendarAction("今天", () => { selectedDay = DateTime.Today; Render(); }), CalendarAction("‹", () => Move(-1)), CalendarAction("›", () => Move(1)), rangeText);
        navigation.VerticalAlignment = VerticalAlignment.Center; toolbar.Children.Add(navigation);
        var view = new ComboBox { ItemsSource = new[] { "日", "周", "月", "列表" }, SelectedItem = calendarView, Width = 76, Height = 34, MinHeight = 34, Padding = new Thickness(8, 4, 8, 4), Margin = new Thickness(0, 0, 8, 0) };
        System.Windows.Automation.AutomationProperties.SetName(view, "日历视图");
        view.SelectionChanged += (_, _) => { calendarView = view.SelectedItem?.ToString() ?? "周"; Render(); };
        var controls = Row(view, CalendarAction("＋ 创建日程", () => EditItem(true), true)); controls.VerticalAlignment = VerticalAlignment.Center;
        Grid.SetColumn(controls, 1); toolbar.Children.Add(controls); DockPanel.SetDock(toolbar, Dock.Top); main.Children.Add(toolbar);
        if (calendarView == "月") main.Children.Add(MonthCalendar(sunday));
        else if (calendarView == "列表") {
            var list = Column();
            foreach (var item in Active.Where(item => item.IsEvent && Local(item.EndAtMillis) >= selectedDay.Date && !hiddenCalendarGroups.Contains(item.GroupId))) list.Children.Add(Card(EventRow(item)));
            if (list.Children.Count == 0) list.Children.Add(Text("暂无日程", 16, Muted)); main.Children.Add(Scroll(list));
        } else main.Children.Add(CalendarTimeline(start, count));
        return root;
    }

    private static DateTime WeekStart(DateTime day, bool sunday) => day.Date.AddDays(-(((int)day.DayOfWeek + (sunday ? 0 : 6)) % 7));

    private UIElement MiniCalendar(bool sunday)
    {
        var panel = Column(); panel.Margin = new Thickness(12, 16, 10, 12);
        var previous = CalendarAction("‹", () => { selectedDay = selectedDay.AddMonths(-1); Render(); });
        var next = CalendarAction("›", () => { selectedDay = selectedDay.AddMonths(1); Render(); });
        foreach (var button in new[] { previous, next }) { button.Width = 26; button.Padding = new Thickness(0); button.Margin = new Thickness(0); button.BorderThickness = new Thickness(0); button.Background = B("#F5F7FA"); }
        var monthHeader = new Grid { Height = 38 }; monthHeader.ColumnDefinitions.Add(new ColumnDefinition()); monthHeader.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        var monthLabel = Text(selectedDay.ToString("yyyy年M月"), 14, bold: true); monthLabel.VerticalAlignment = VerticalAlignment.Center; monthHeader.Children.Add(monthLabel);
        var monthActions = Row(previous, next); Grid.SetColumn(monthActions, 1); monthHeader.Children.Add(monthActions); panel.Children.Add(monthHeader);
        var weekdays = new System.Windows.Controls.Primitives.UniformGrid { Columns = 7, Height = 28 };
        foreach (var name in sunday ? new[] { "日", "一", "二", "三", "四", "五", "六" } : new[] { "一", "二", "三", "四", "五", "六", "日" }) {
            var label = Text(name, 12, Muted); label.TextAlignment = TextAlignment.Center; weekdays.Children.Add(label);
        }
        panel.Children.Add(weekdays);
        var days = new System.Windows.Controls.Primitives.UniformGrid { Columns = 7, Rows = 6 };
        var first = WeekStart(new DateTime(selectedDay.Year, selectedDay.Month, 1), sunday);
        var selectedWeek = WeekStart(selectedDay, sunday);
        for (var i = 0; i < 42; i++) {
            var day = first.AddDays(i);
            var label = Text(day.Day.ToString(), 13, day == DateTime.Today ? Ocean : day.Month == selectedDay.Month ? Ink : Muted);
            label.TextAlignment = TextAlignment.Center; label.Margin = new Thickness(0); label.VerticalAlignment = VerticalAlignment.Center;
            var circle = new Border { Width = 26, Height = 26, CornerRadius = new CornerRadius(13), BorderBrush = day == DateTime.Today ? Ocean : null, BorderThickness = new Thickness(day == DateTime.Today ? 1 : 0), Child = label };
            var button = CalendarAction(day.ToString("yyyy-MM-dd"), () => { selectedDay = day; Render(); });
            button.Content = circle; button.Height = 34; button.Padding = new Thickness(0); button.Margin = new Thickness(0); button.BorderThickness = new Thickness(0);
            button.Background = calendarView == "周" && day >= selectedWeek && day < selectedWeek.AddDays(7) || day == selectedDay.Date ? B("#E8F0FF") : B("#F5F7FA");
            button.Template = (ControlTemplate)System.Windows.Markup.XamlReader.Parse("""
                <ControlTemplate xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" TargetType="Button">
                  <Border Background="{TemplateBinding Background}"><ContentPresenter HorizontalAlignment="Center" VerticalAlignment="Center"/></Border>
                </ControlTemplate>
                """);
            button.Opacity = day.Month == selectedDay.Month ? 1 : 0.65;
            System.Windows.Automation.AutomationProperties.SetAutomationId(button, "Calendar.Date." + day.ToString("yyyy-MM-dd")); days.Children.Add(button);
        }
        panel.Children.Add(days);
        panel.Children.Add(new Border { Height = 1, Background = Outline, Margin = new Thickness(0, 18, 0, 16) });
        panel.Children.Add(Text("我的日历", 14, bold: true));
        foreach (var group in workspace.Document!.Groups) {
            var toggle = new System.Windows.Controls.CheckBox { Content = group.Name, IsChecked = !hiddenCalendarGroups.Contains(group.Id), Foreground = Ink, Margin = new Thickness(0, 5, 0, 5), BorderBrush = B(group.ColorHex) };
            toggle.Checked += (_, _) => { hiddenCalendarGroups.Remove(group.Id); Render(); };
            toggle.Unchecked += (_, _) => { hiddenCalendarGroups.Add(group.Id); Render(); };
            panel.Children.Add(toggle);
        }
        return new Border { Background = B("#F5F7FA"), Child = panel, BorderBrush = B("#E1E7F0"), BorderThickness = new Thickness(0, 0, 1, 0) };
    }

    private UIElement CalendarTimeline(DateTime start, int count)
    {
        var body = new DockPanel();
        var header = new Grid { Height = 66, Margin = new Thickness(0, 0, SystemParameters.VerticalScrollBarWidth, 0) };
        header.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(56) });
        var offset = TimeZoneInfo.Local.GetUtcOffset(DateTime.Now);
        var zoneOffset = offset.Minutes == 0 ? offset.Duration().TotalHours.ToString("0", CultureInfo.InvariantCulture) : offset.Duration().ToString(@"h\:mm");
        var zone = Text($"GMT{(offset < TimeSpan.Zero ? "−" : "+")}{zoneOffset}", 10, Muted); zone.TextWrapping = TextWrapping.NoWrap; zone.VerticalAlignment = VerticalAlignment.Bottom; zone.Margin = new Thickness(0, 0, 2, 8); header.Children.Add(zone);
        for (var i = 0; i < count; i++) {
            header.ColumnDefinitions.Add(new ColumnDefinition()); var day = start.AddDays(i);
            var weekday = Text(day.ToString("ddd", CalendarCulture), 13, day == DateTime.Today ? Ocean : Muted);
            var date = Text(day.Day.ToString(), 20, day == DateTime.Today ? Ocean : Ink); date.FontWeight = FontWeights.Normal;
            var cell = new Border { BorderBrush = B("#E1E7F0"), BorderThickness = new Thickness(1, 0, 0, 1), Padding = new Thickness(10, 2, 0, 0), Child = Column(weekday, date) };
            Grid.SetColumn(cell, i + 1); header.Children.Add(cell);
        }
        DockPanel.SetDock(header, Dock.Top); body.Children.Add(header);
        var allDayEvents = Enumerable.Range(0, count).Select(i => EventsOn(start.AddDays(i)).Where(item => workspace.Document!.EditFor(item.Id)?.AllDay == true && !hiddenCalendarGroups.Contains(item.GroupId)).ToList()).ToList();
        if (allDayEvents.Any(items => items.Count > 0)) {
            var strip = new Grid { Margin = new Thickness(0, 0, SystemParameters.VerticalScrollBarWidth, 0), Background = B("#F7FAFF") };
            strip.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(56) }); strip.Children.Add(Text("全天", 11, Muted));
            for (var i = 0; i < count; i++) { strip.ColumnDefinitions.Add(new ColumnDefinition()); var stack = Column(); foreach (var item in allDayEvents[i]) {
                var action = CalendarAction(item.Title, () => Preview(item)); action.Foreground = B(item.AccentColor); stack.Children.Add(action);
            } Grid.SetColumn(stack, i + 1); strip.Children.Add(stack); }
            DockPanel.SetDock(strip, Dock.Top); body.Children.Add(strip);
        }
        var grid = new Grid { Height = TimeInset * 2 + 24 * HourHeight, Background = Paper };
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(56) });
        var axis = new Canvas { ClipToBounds = true }; grid.Children.Add(axis);
        for (var hour = 0; hour <= 24; hour++) {
            var label = Text($"{hour:00}:00", 11, Muted); label.Margin = new Thickness(0); label.Width = 50; label.TextAlignment = TextAlignment.Center; label.Height = 16;
            Canvas.SetTop(label, TimeInset + hour * HourHeight - 8); axis.Children.Add(label);
        }
        for (var column = 0; column < count; column++) {
            grid.ColumnDefinitions.Add(new ColumnDefinition()); var day = start.AddDays(column);
            var canvas = new Canvas { Background = Paper, ClipToBounds = true, Tag = day };
            Grid.SetColumn(canvas, column + 1); grid.Children.Add(canvas);
            var edge = new Border { Width = 1, Height = grid.Height, Background = B("#E1E7F0") }; canvas.Children.Add(edge);
            for (var hour = 0; hour <= 24; hour++) {
                var line = new Border { Height = 1, Background = B("#E1E7F0") }; line.SetBinding(FrameworkElement.WidthProperty, new System.Windows.Data.Binding("ActualWidth") { Source = canvas });
                Canvas.SetTop(line, TimeInset + hour * HourHeight); canvas.Children.Add(line);
            }
            canvas.MouseLeftButtonDown += (_, e) => { if (e.ClickCount == 2) { var minute = Math.Clamp((int)((e.GetPosition(canvas).Y - TimeInset) * 60 / HourHeight / 15) * 15, 0, 1425); EditItem(true, seed: day.AddMinutes(minute)); } };
            var events = EventsOn(day).Where(item => workspace.Document!.EditFor(item.Id)?.AllDay != true && !hiddenCalendarGroups.Contains(item.GroupId)).OrderBy(item => item.StartAtMillis).ToList();
            var layout = CalendarEventLayout.Arrange(events, day);
            foreach (var item in events) {
                var itemStart = Local(item.StartAtMillis); var itemEnd = Local(item.EndAtMillis);
                var minute = Math.Max(0, (itemStart - day).TotalMinutes); var duration = Math.Max(28, (Math.Min(1440, (itemEnd - day).TotalMinutes) - minute) * HourHeight / 60);
                var position = layout[item.Id];
                var tile = new Border { Background = B(item.AccentColor), CornerRadius = new CornerRadius(4), Padding = new Thickness(5, 2, 5, 2), Height = duration - 2,
                    Child = Column(Text(item.Title, 12, B("#FFFFFF"), true), Text(itemStart.ToString("HH:mm"), 11, B("#FFFFFF"))), ClipToBounds = true,
                    Opacity = itemEnd < DateTime.Now ? 0.55 : 0.94, ToolTip = item.Title + "\n" + TimeLabel(item) + "\n" + item.Location };
                void Position() { tile.Width = Math.Max(10, (canvas.ActualWidth - 4) / position.Columns - 3); Canvas.SetLeft(tile, position.Lane * (canvas.ActualWidth - 4) / position.Columns + 2); }
                canvas.SizeChanged += (_, _) => Position(); Canvas.SetTop(tile, TimeInset + minute * HourHeight / 60); canvas.Children.Add(tile);
                tile.MouseLeftButtonUp += (_, e) => { Preview(item); e.Handled = true; };
            }
        }
        var clock = new CalendarTimeLine(start, count) { IsHitTestVisible = false }; Grid.SetColumnSpan(clock, count + 1); grid.Children.Add(clock);
        calendarClock = new DispatcherTimer { Interval = TimeSpan.FromSeconds(15) }; var timer = calendarClock;
        timer.Tick += (_, _) => clock.Refresh();
        grid.Loaded += (_, _) => { clock.Refresh(); timer.Start(); }; grid.Unloaded += (_, _) => timer.Stop();
        var scroll = Scroll(grid); scroll.VerticalScrollBarVisibility = ScrollBarVisibility.Visible;
        scroll.Loaded += (_, _) => scroll.ScrollToVerticalOffset(start <= DateTime.Today && start.AddDays(count) > DateTime.Today ? Math.Max(0, TimeInset + DateTime.Now.TimeOfDay.TotalMinutes * HourHeight / 60 - scroll.ViewportHeight * 0.65) : HourHeight * 8);
        body.Children.Add(scroll); return body;
    }

    private sealed class CalendarTimeLine(DateTime start, int count) : FrameworkElement
    {
        private DateTime now = DateTime.Now;
        public void Refresh() { now = DateTime.Now; InvalidateVisual(); }
        protected override void OnRender(DrawingContext drawing)
        {
            base.OnRender(drawing);
            if (now.Date < start || now.Date >= start.AddDays(count)) return;
            var y = TimeInset + now.TimeOfDay.TotalMinutes * HourHeight / 60;
            var today = (now.Date - start).Days; var x = 56 + today * Math.Max(0, ActualWidth - 56) / count;
            var red = B(Dark ? "#FF8A91" : "#F24E59");
            drawing.DrawLine(new Pen(red, 1) { DashStyle = DashStyles.Dash }, new Point(56, y), new Point(x, y));
            drawing.DrawLine(new Pen(red, 1.4), new Point(x, y), new Point(ActualWidth, y));
            drawing.DrawEllipse(red, null, new Point(x, y), 3.5, 3.5);
            var dpi = VisualTreeHelper.GetDpi(this);
            var text = new FormattedText(now.ToString("HH:mm"), CalendarCulture, System.Windows.FlowDirection.LeftToRight, new Typeface(InterfaceFont, FontStyles.Normal, FontWeights.Normal, FontStretches.Normal), 11, red, dpi.PixelsPerDip);
            drawing.DrawRectangle(Paper, null, new Rect(0, y - 9, 54, 18)); drawing.DrawText(text, new Point(6, y - text.Height / 2));
        }
    }

    private UIElement MonthCalendar(bool sunday)
    {
        var first = WeekStart(new DateTime(selectedDay.Year, selectedDay.Month, 1), sunday);
        var grid = new System.Windows.Controls.Primitives.UniformGrid { Columns = 7, Rows = 6 };
        for (var i = 0; i < 42; i++) {
            var day = first.AddDays(i); var events = EventsOn(day).Where(item => !hiddenCalendarGroups.Contains(item.GroupId)).ToList();
            var date = Text(day.ToString("M/d"), 15, day == DateTime.Today ? Ocean : Muted, day == DateTime.Today);
            var box = Column(date);
            foreach (var item in events.Take(3)) box.Children.Add(Text(item.Title, 12, B(item.AccentColor)));
            if (events.Count > 3) box.Children.Add(Text($"还有 {events.Count - 3} 项", 11, Muted));
            var cell = new Border { Child = box, Padding = new Thickness(8), Background = day == DateTime.Today ? B("#E8F0FF") : Paper,
                BorderBrush = B("#E1E7F0"), BorderThickness = new Thickness(0, 0, 1, 1), Opacity = day.Month == selectedDay.Month ? 1 : 0.55, Cursor = System.Windows.Input.Cursors.Hand };
            cell.MouseLeftButtonUp += (_, _) => { selectedDay = day; calendarView = "日"; Render(); }; grid.Children.Add(cell);
        }
        return grid;
    }
}
