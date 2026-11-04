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
    private static double HourHeight => TextSize(48);
    // Seven one-hour tiles: width >= 1.5 × height, plus tile gutters, axis, rails and window frame.
    internal static double MinimumCalendarWindowWidth => Math.Ceiling(204 + 36 + 16 + TimeAxisWidth + SystemParameters.VerticalScrollBarWidth + 7 * (1.5 * HourHeight + 7) + 16);
    private static double MiniCalendarBreakpoint => MinimumCalendarWindowWidth + TextSize(208) + 28;
    private const double TimeInset = 12;
    private static double TimeAxisWidth => TextSize(56);
    private static readonly CultureInfo CalendarCulture = CultureInfo.GetCultureInfo("zh-CN");
    private DispatcherTimer? calendarClock;
    private Button? calendarDraft;
    private Canvas? calendarDraftCanvas;
    private double? calendarScrollOffset;
    private string? calendarScrollKey;
    private ScrollViewer? activeCalendarScroll;
    private DateTime? miniMonth;
    private static ControlTemplate? miniDateTemplate;

    private void ClearCalendarDraft()
    {
        if (calendarDraft != null) calendarDraftCanvas?.Children.Remove(calendarDraft);
        calendarDraft = null; calendarDraftCanvas = null;
    }

    internal static DateTime CalendarSlot(DateTime day, double y, double hourHeight = 48) => day.Date.AddMinutes(
        Math.Clamp((int)Math.Floor((y - TimeInset) * 60 / hourHeight / 15) * 15, 0, 1425));

    private void ShowCalendarDraft(Canvas canvas, DateTime seed)
    {
        ClearCalendarDraft();
        var duration = workspace.Document!.CalendarDefaultDurationMinutes;
        var draft = CalendarAction("添加日程", () => {
            var anchor = calendarDraft?.PointToScreen(new Point(calendarDraft.ActualWidth, 0));
            ClearCalendarDraft(); EditItem(true, seed: seed, anchorScreen: anchor);
        });
        draft.Content = Text($"添加日程  {seed:HH:mm}–{seed.AddMinutes(duration):HH:mm}", 11, B("#FFFFFF"), true);
        draft.Background = B("#3370FF"); draft.BorderBrush = B("#245BDB"); draft.Foreground = B("#FFFFFF");
        draft.MinHeight = 0; draft.Height = Math.Max(12, Math.Min(duration, 1440 - seed.TimeOfDay.TotalMinutes) * HourHeight / 60); draft.Margin = new Thickness(0);
        draft.Padding = new Thickness(4, 0, 4, 0); draft.ClipToBounds = true;
        draft.HorizontalContentAlignment = HorizontalAlignment.Left;
        draft.SetBinding(FrameworkElement.WidthProperty, new System.Windows.Data.Binding("ActualWidth") { Source = canvas });
        System.Windows.Automation.AutomationProperties.SetAutomationId(draft, "Calendar.Draft");
        System.Windows.Automation.AutomationProperties.SetHelpText(draft, $"{seed:yyyy-MM-dd HH:mm}–{seed.AddMinutes(duration):HH:mm}，再次点击添加");
        Canvas.SetTop(draft, TimeInset + seed.TimeOfDay.TotalMinutes * HourHeight / 60);
        Canvas.SetZIndex(draft, 10); canvas.Children.Add(draft); calendarDraft = draft; calendarDraftCanvas = canvas;
    }

    private Button CalendarAction(string label, Action click, bool primary = false)
    {
        var button = Action(label, click, primary);
        button.MinHeight = Math.Max(34, TextSize(24) + 10); button.Padding = new Thickness(12, 5, 12, 5); button.Margin = new Thickness(0, 0, 6, 0);
        button.Background = primary ? B("#326DD6") : Paper; button.BorderBrush = primary ? B("#326DD6") : Outline;
        return button;
    }

    private UIElement CalendarPage()
    {
        calendarClock?.Stop();
        ClearCalendarDraft();
        var count = calendarView == "日" ? 1 : 7;
        var sunday = workspace.Document!.Settings["weekStartMode"]?.GetValue<string>() == "SUNDAY";
        var start = count != 7 ? selectedDay.Date : WeekStart(selectedDay, sunday);
        var root = new Grid { Background = Paper, Margin = new Thickness(18, 0, 18, 14) };
        root.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(ActualWidth >= MiniCalendarBreakpoint ? TextSize(208) : 0) }); root.ColumnDefinitions.Add(new ColumnDefinition());
        var mini = MiniCalendar(sunday); mini.Visibility = ActualWidth >= MiniCalendarBreakpoint ? Visibility.Visible : Visibility.Collapsed; root.Children.Add(mini);
        var main = new DockPanel { Margin = new Thickness(16, 12, 0, 0) }; Grid.SetColumn(main, 1); root.Children.Add(main);
        var toolbar = new Grid { Margin = new Thickness(0, 0, 0, 12) };
        toolbar.ColumnDefinitions.Add(new ColumnDefinition()); toolbar.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        void Move(int direction) { selectedDay = calendarView == "月" ? selectedDay.AddMonths(direction) : selectedDay.AddDays(direction * (calendarView is "周" or "列表" ? count : 1)); miniMonth = selectedDay; Render(); }
        var range = calendarView == "月" ? selectedDay.ToString("yyyy年M月") : calendarView is "周" or "列表" ? $"{start:yyyy年M月d日} – {start.AddDays(count - 1):yyyy年M月d日}" : selectedDay.ToString("yyyy年M月d日");
        var rangeText = Text(range, 15, bold: true); rangeText.Margin = new Thickness(6, 0, 6, 0); rangeText.VerticalAlignment = VerticalAlignment.Center;
        rangeText.TextWrapping = TextWrapping.NoWrap; rangeText.TextTrimming = TextTrimming.CharacterEllipsis; rangeText.ToolTip = range;
        var previous = ChevronAction("left", "上一页", () => Move(-1)); var next = ChevronAction("right", "下一页", () => Move(1));
        System.Windows.Automation.AutomationProperties.SetName(previous, calendarView == "列表" ? "上一周" : "上一页");
        System.Windows.Automation.AutomationProperties.SetName(next, calendarView == "列表" ? "下一周" : "下一页");
        var navigation = new DockPanel { VerticalAlignment = VerticalAlignment.Center };
        var dateActions = Row(CalendarAction("今天", () => { selectedDay = CalendarNow.Date; miniMonth = selectedDay; calendarScrollKey = null; calendarScrollOffset = null; Render(); }), previous, next);
        DockPanel.SetDock(dateActions, Dock.Left); navigation.Children.Add(dateActions); navigation.Children.Add(rangeText); toolbar.Children.Add(navigation);
        var view = new ComboBox { ItemsSource = new[] { "日", "周", "月", "列表" }, SelectedItem = calendarView, Width = TextSize(36) + 26, MinHeight = Math.Max(34, TextSize(24) + 10), Padding = new Thickness(5, 4, 3, 4), Margin = new Thickness(0, 0, 4, 0) };
        System.Windows.Automation.AutomationProperties.SetName(view, "日历视图");
        view.SelectionChanged += (_, _) => { calendarView = view.SelectedItem?.ToString() ?? "周"; Render(); };
        var controls = Row(CalendarAction("日历", () => { var dialog = Dialog(this, "日历", 440, 530); dialog.Content = DialogLayout(CalendarList(), Action("关闭", () => dialog.Close())); dialog.ShowDialog(); }), view, CalendarAction("日历设置", ShowCalendarPreferences), CalendarAction("＋ 创建日程", () => EditItem(true), true)); controls.VerticalAlignment = VerticalAlignment.Center;
        Grid.SetColumn(controls, 1);
        foreach (var button in controls.Children.OfType<Button>()) { button.Padding = new Thickness(8, 5, 8, 5); button.Margin = new Thickness(0, 0, 4, 0); }
        toolbar.SizeChanged += (_, _) => {
            rangeText.Text = toolbar.ActualWidth < TextSize(850) && calendarView is "周" or "列表"
                ? start.Year == start.AddDays(6).Year ? $"{start:M月d日} – {start.AddDays(6):M月d日}" : $"{start:yyyy/M/d} – {start.AddDays(6):yyyy/M/d}" : range;
        };
        toolbar.Children.Add(controls); DockPanel.SetDock(toolbar, Dock.Top); main.Children.Add(toolbar);
        if (calendarView == "月") main.Children.Add(MonthCalendar(sunday));
        else if (calendarView == "列表") {
            var list = Column();
            var total = 0;
            for (var i = 0; i < 7; i++) {
                var day = start.AddDays(i);
                var dateRail = Column(Text(day.ToString("ddd", CalendarCulture), 13, day == CalendarNow.Date ? Ocean : Muted), Text(day.Day.ToString(), 25, day == CalendarNow.Date ? Ocean : Ink, true));
                dateRail.Margin = new Thickness(8, 10, 14, 0);
                var entries = Column();
                var items = EventsOn(day).Where(CalendarVisible).OrderByDescending(item => item.AllDay).ThenBy(item => item.StartAtMillis).ThenBy(item => item.Id).ToList();
                total += items.Count;
                foreach (var item in items) entries.Children.Add(EventRow(item));
                if (items.Count == 0) { var empty = Text("本日暂无日程", 14, Muted); empty.Margin = new Thickness(14, 26, 0, 0); entries.Children.Add(empty); }
                var dayRow = new Grid { MinHeight = TextSize(88), Margin = new Thickness(0, 6, 0, 6) };
                dayRow.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(TextSize(76)) }); dayRow.ColumnDefinitions.Add(new ColumnDefinition());
                dayRow.Children.Add(dateRail); Grid.SetColumn(entries, 1); dayRow.Children.Add(entries);
                System.Windows.Automation.AutomationProperties.SetAutomationId(dayRow, "Calendar.AgendaDay." + day.ToString("yyyy-MM-dd"));
                list.Children.Add(new Border { BorderBrush = B("#E1E7F0"), BorderThickness = new Thickness(0, 0, 0, 1), Child = dayRow });
            }
            if (total == 0) list.Children.Insert(0, Text("本周暂无日程", 16, Muted)); main.Children.Add(Scroll(list));
        } else main.Children.Add(CalendarTimeline(start, count));
        return root;
    }

    private static DateTime WeekStart(DateTime day, bool sunday) => day.Date.AddDays(-(((int)day.DayOfWeek + (sunday ? 0 : 6)) % 7));

    private UIElement MiniCalendar(bool sunday)
    {
        var panel = Column(); panel.Margin = new Thickness(12, 16, 10, 12);
        var month = miniMonth ?? selectedDay;
        var previous = ChevronAction("left", "上个月", () => { miniMonth = month.AddMonths(-1); Render(); });
        var next = ChevronAction("right", "下个月", () => { miniMonth = month.AddMonths(1); Render(); });
        System.Windows.Automation.AutomationProperties.SetAutomationId(previous, "Calendar.Mini.Previous");
        System.Windows.Automation.AutomationProperties.SetAutomationId(next, "Calendar.Mini.Next");
        foreach (var button in new[] { previous, next }) { button.Width = 26; button.Padding = new Thickness(0); button.Margin = new Thickness(0); button.BorderThickness = new Thickness(0); button.Background = B("#F5F7FA"); }
        var monthHeader = new Grid { MinHeight = TextSize(38) }; monthHeader.ColumnDefinitions.Add(new ColumnDefinition()); monthHeader.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        var monthLabel = Text(month.ToString("yyyy年M月"), 14, bold: true); monthLabel.VerticalAlignment = VerticalAlignment.Center; monthHeader.Children.Add(monthLabel);
        var monthActions = Row(previous, next); Grid.SetColumn(monthActions, 1); monthHeader.Children.Add(monthActions); panel.Children.Add(monthHeader);
        var weekdays = new System.Windows.Controls.Primitives.UniformGrid { Columns = 7, Height = TextSize(28) };
        foreach (var name in sunday ? new[] { "日", "一", "二", "三", "四", "五", "六" } : new[] { "一", "二", "三", "四", "五", "六", "日" }) {
            var label = Text(name, 12, Muted); label.TextAlignment = TextAlignment.Center; weekdays.Children.Add(label);
        }
        panel.Children.Add(weekdays);
        var days = new System.Windows.Controls.Primitives.UniformGrid { Columns = 7, Rows = 6 };
        var first = WeekStart(new DateTime(month.Year, month.Month, 1), sunday);
        var selectedWeek = WeekStart(selectedDay, sunday);
        for (var i = 0; i < 42; i++) {
            var day = first.AddDays(i);
            var label = Text(day.Day.ToString(), 13, day == CalendarNow.Date ? Ocean : day.Month == month.Month ? Ink : Muted);
            label.TextAlignment = TextAlignment.Center; label.Margin = new Thickness(0); label.VerticalAlignment = VerticalAlignment.Center;
            var circle = new Border { Width = TextSize(26), Height = TextSize(26), CornerRadius = new CornerRadius(TextSize(13)), BorderBrush = day == CalendarNow.Date ? Ocean : null, BorderThickness = new Thickness(day == CalendarNow.Date ? 1 : 0), Child = label };
            var button = CalendarAction(day.ToString("yyyy-MM-dd"), () => { selectedDay = day; miniMonth = day; Render(); });
            button.Content = circle; button.MinHeight = TextSize(34); button.Padding = new Thickness(0); button.Margin = new Thickness(0); button.BorderThickness = new Thickness(0);
            button.Background = calendarView is "周" or "列表" && day >= selectedWeek && day < selectedWeek.AddDays(7) || day == selectedDay.Date ? B("#E8F0FF") : B("#F5F7FA");
            button.Template = miniDateTemplate ??= (ControlTemplate)System.Windows.Markup.XamlReader.Parse("""
                <ControlTemplate xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" TargetType="Button">
                  <Border Background="{TemplateBinding Background}"><ContentPresenter HorizontalAlignment="Center" VerticalAlignment="Center"/></Border>
                </ControlTemplate>
                """);
            button.Opacity = day.Month == month.Month ? 1 : 0.65;
            System.Windows.Automation.AutomationProperties.SetAutomationId(button, "Calendar.Date." + day.ToString("yyyy-MM-dd")); days.Children.Add(button);
        }
        panel.Children.Add(days);
        panel.Children.Add(new Border { Height = 1, Background = Outline, Margin = new Thickness(0, 18, 0, 16) });
        var sidebar = new DockPanel(); DockPanel.SetDock(panel, Dock.Top); sidebar.Children.Add(panel);
        var calendars = Scroll(CalendarList()); calendars.Margin = new Thickness(12, 0, 10, 12);
        System.Windows.Automation.AutomationProperties.SetAutomationId(calendars, "Calendar.SidebarScroll"); sidebar.Children.Add(calendars);
        return new Border { Background = B("#F5F7FA"), Child = sidebar, BorderBrush = B("#E1E7F0"), BorderThickness = new Thickness(0, 0, 1, 0) };
    }

    private UIElement CalendarTimeline(DateTime start, int count)
    {
        var body = new DockPanel();
        var header = new Grid { Height = TextSize(66), Margin = new Thickness(0, 0, SystemParameters.VerticalScrollBarWidth, 0) };
        header.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(TimeAxisWidth) });
        var zoneId = workspace.Document!.CalendarDisplayTimeZoneId;
        var auxiliaryZoneId = workspace.Document.CalendarAuxiliaryTimeZoneId;
        var offset = DesktopCalendarTime.Zone(zoneId).GetUtcOffset(DateTimeOffset.UtcNow);
        var zoneOffset = offset.Minutes == 0 ? offset.Duration().TotalHours.ToString("0", CultureInfo.InvariantCulture) : offset.Duration().ToString(@"h\:mm");
        var zone = Text($"GMT{(offset < TimeSpan.Zero ? "−" : "+")}{zoneOffset}", 10, Muted); zone.TextWrapping = TextWrapping.NoWrap;
        var zones = Column(zone); zones.VerticalAlignment = VerticalAlignment.Bottom; zones.Margin = new Thickness(0, 0, 2, 8); zones.ToolTip = DesktopCalendarTime.Zone(zoneId).Id;
        if (!string.IsNullOrEmpty(auxiliaryZoneId)) {
            var secondaryOffset = DesktopCalendarTime.Zone(auxiliaryZoneId).GetUtcOffset(DateTimeOffset.UtcNow);
            var hours = secondaryOffset.Duration().TotalHours.ToString("0.##", CultureInfo.InvariantCulture);
            var secondary = Text($"GMT{(secondaryOffset < TimeSpan.Zero ? "−" : "+")}{hours}", 9, Ocean); secondary.TextWrapping = TextWrapping.NoWrap; secondary.ToolTip = auxiliaryZoneId; zones.Children.Add(secondary);
        }
        header.Children.Add(zones);
        for (var i = 0; i < count; i++) {
            header.ColumnDefinitions.Add(new ColumnDefinition()); var day = start.AddDays(i);
            var weekday = Text(day.ToString("ddd", CalendarCulture), 13, day == CalendarNow.Date ? Ocean : Muted);
            var date = Text(day.Day.ToString(), 20, day == CalendarNow.Date ? Ocean : Ink); date.FontWeight = FontWeights.Normal;
            var cell = new Border { BorderBrush = B("#E1E7F0"), BorderThickness = new Thickness(1, 0, 0, 1), Padding = new Thickness(10, 2, 0, 0), Child = Column(weekday, date) };
            Grid.SetColumn(cell, i + 1); header.Children.Add(cell);
        }
        DockPanel.SetDock(header, Dock.Top); body.Children.Add(header);
        var allDayEvents = Enumerable.Range(0, count).Select(i => EventsOn(start.AddDays(i)).Where(item => item.AllDay && CalendarVisible(item)).ToList()).ToList();
        if (allDayEvents.Any(items => items.Count > 0)) {
            var strip = new Grid { Margin = new Thickness(0, 0, SystemParameters.VerticalScrollBarWidth, 0), Background = B("#F7FAFF") };
            strip.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(TimeAxisWidth) }); strip.Children.Add(Text("全天", 11, Muted));
            for (var i = 0; i < count; i++) { strip.ColumnDefinitions.Add(new ColumnDefinition()); var stack = Column(); foreach (var item in allDayEvents[i]) {
                var action = CalendarAction(item.Title, () => Preview(item)); action.Foreground = EventInk(item.AccentColor, workspace.Document!.ClassicCalendarColors); action.Background = EventSurface(item.AccentColor, workspace.Document.ClassicCalendarColors); stack.Children.Add(action);
            } Grid.SetColumn(stack, i + 1); strip.Children.Add(stack); }
            DockPanel.SetDock(strip, Dock.Top); body.Children.Add(strip);
        }
        var grid = new Grid { Height = TimeInset * 2 + 24 * HourHeight, Background = Paper };
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(TimeAxisWidth) });
        var axis = new Canvas { ClipToBounds = true }; grid.Children.Add(axis);
        for (var hour = 0; hour <= 24; hour++) {
            var label = Text($"{hour:00}:00", 11, Muted); label.Margin = new Thickness(0); label.Width = TextSize(50); label.TextAlignment = TextAlignment.Center; label.Height = LineHeight(11);
            Canvas.SetTop(label, TimeInset + hour * HourHeight - label.Height / 2); axis.Children.Add(label);
            if (!string.IsNullOrEmpty(auxiliaryZoneId)) {
                var wall = start.AddHours(hour); DateTime? auxiliary = null;
                try { auxiliary = DesktopCalendarTime.Wall(DesktopCalendarTime.Millis(wall, zoneId), auxiliaryZoneId); } catch (InvalidOperationException) { }
                var secondary = Text(auxiliary?.ToString("HH:mm") ?? "—", 9, Ocean); secondary.Width = TextSize(50); secondary.TextAlignment = TextAlignment.Center; secondary.Margin = new Thickness(0); secondary.Height = TextSize(14);
                secondary.ToolTip = auxiliary == null ? "此日期没有这个钟点" : $"{auxiliary:yyyy-MM-dd HH:mm} · {auxiliaryZoneId}";
                System.Windows.Automation.AutomationProperties.SetAutomationId(secondary, "Calendar.AuxiliaryHour." + hour);
                Canvas.SetTop(secondary, TimeInset + hour * HourHeight + 7); axis.Children.Add(secondary);
            }
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
            canvas.MouseLeftButtonUp += (_, e) => {
                if (e.Handled) return;
                ShowCalendarDraft(canvas, CalendarSlot(day, e.GetPosition(canvas).Y, HourHeight)); e.Handled = true;
            };
            var events = EventsOn(day).Where(item => !item.AllDay && CalendarVisible(item)).OrderBy(item => item.StartAtMillis).ToList();
            var layout = CalendarEventLayout.Arrange(events, day);
            foreach (var item in events) {
                var itemStart = CalendarLocal(item, item.StartAtMillis); var itemEnd = CalendarLocal(item, item.EndAtMillis);
                var minute = Math.Max(0, (itemStart - day).TotalMinutes); var duration = Math.Max(12, (Math.Min(1440, (itemEnd - day).TotalMinutes) - minute) * HourHeight / 60);
                var position = layout[item.Id];
                var classic = workspace.Document!.ClassicCalendarColors;
                var ended = itemEnd < CalendarNow;
                var ink = EventInk(item.AccentColor, classic, ended);
                var titleLine = LineHeight(14); var detailLine = LineHeight(12);
                var topPadding = duration >= titleLine + 5 ? 3 : 0;
                var room = Math.Max(0, duration - 2 - topPadding);
                var showTitle = room >= titleLine;
                var showTime = room >= titleLine + detailLine + 2;
                var showLocation = item.Location.Length > 0 && room >= titleLine + detailLine * 2 + 4;
                var lines = Math.Clamp((int)Math.Floor((room - (showTime ? detailLine + 2 : 0) - (showLocation ? detailLine + 2 : 0)) / titleLine), 1, 3);
                var eventText = Column();
                if (showTitle) eventText.Children.Add(LimitedTitle(item.Title, 14, ink, lines));
                else eventText.Children.Add(new System.Windows.Shapes.Path { Data = Geometry.Parse("M 1,3 L 1,3 M 6,3 L 6,3 M 11,3 L 11,3"), Stroke = ink, StrokeThickness = 3, StrokeStartLineCap = PenLineCap.Round, StrokeEndLineCap = PenLineCap.Round, Width = 14, Height = 6, HorizontalAlignment = HorizontalAlignment.Left, VerticalAlignment = VerticalAlignment.Center });
                void Detail(string value) { var text = Text(value, 12, ink); text.Margin = new Thickness(0, 2, 0, 0); text.TextWrapping = TextWrapping.NoWrap; text.TextTrimming = TextTrimming.CharacterEllipsis; eventText.Children.Add(text); }
                if (showTime) Detail(itemStart.ToString("HH:mm") + "–" + itemEnd.ToString("HH:mm"));
                if (showLocation) Detail(item.Location);
                var tile = new Border { Background = EventSurface(item.AccentColor, classic, ended),
                    BorderBrush = ColorBrush(item.AccentColor), BorderThickness = new Thickness(4, 0, 0, 0),
                    CornerRadius = new CornerRadius(5), Padding = new Thickness(6, topPadding, 4, 0), Height = duration - 2,
                    Child = eventText, ClipToBounds = true, ToolTip = item.Title + "\n" + TimeLabel(item) + "\n" + item.Location };
                System.Windows.Automation.AutomationProperties.SetAutomationId(tile, "Calendar.Event." + item.Id);
                void Position() { tile.Width = Math.Max(10, (canvas.ActualWidth - 4) / position.Columns - 3); Canvas.SetLeft(tile, position.Lane * (canvas.ActualWidth - 4) / position.Columns + 2); }
                canvas.SizeChanged += (_, _) => Position(); Canvas.SetTop(tile, TimeInset + minute * HourHeight / 60); canvas.Children.Add(tile);
                AttachCalendarDrag(tile, item, canvas, grid, start, count);
                System.Windows.Automation.AutomationProperties.SetName(tile, item.Title);
                System.Windows.Automation.AutomationProperties.SetHelpText(tile, (showTitle ? "" : "短时段，点击查看完整内容。\n") + TimeLabel(item) + "\n" + item.Location + "\n拖动改期，拖动下边缘调整结束，Esc 取消。");
            }
        }
        var clock = new CalendarTimeLine(start, count, zoneId) { IsHitTestVisible = false }; Grid.SetColumnSpan(clock, count + 1); grid.Children.Add(clock);
        calendarClock = new DispatcherTimer { Interval = TimeSpan.FromSeconds(15) }; var timer = calendarClock;
        timer.Tick += (_, _) => clock.Refresh();
        grid.Loaded += (_, _) => { clock.Refresh(); timer.Start(); }; grid.Unloaded += (_, _) => timer.Stop();
        var scroll = Scroll(grid); scroll.VerticalScrollBarVisibility = ScrollBarVisibility.Visible;
        activeCalendarScroll = scroll;
        var scrollKey = $"{start:yyyy-MM-dd}/{count}";
        var restoreOffset = calendarScrollKey == scrollKey ? calendarScrollOffset : null;
        var ready = false;
        scroll.Loaded += (_, _) => {
            scroll.ScrollToVerticalOffset(restoreOffset ?? (start <= CalendarNow.Date && start.AddDays(count) > CalendarNow.Date ? Math.Max(0, TimeInset + CalendarNow.TimeOfDay.TotalMinutes * HourHeight / 60 - scroll.ViewportHeight * 0.65) : HourHeight * 8));
            ready = true;
        };
        scroll.ScrollChanged += (_, e) => {
            if (!ready || !scroll.IsLoaded || !ReferenceEquals(scroll, activeCalendarScroll) || e.VerticalChange == 0) return;
            calendarScrollKey = scrollKey; calendarScrollOffset = e.VerticalOffset;
        };
        grid.PreviewKeyDown += (_, e) => { if (e.Key == System.Windows.Input.Key.Escape) { ClearCalendarDraft(); e.Handled = true; } };
        body.Children.Add(scroll); return body;
    }

    private sealed class CalendarTimeLine(DateTime start, int count, string zoneId) : FrameworkElement
    {
        private DateTime now = DesktopCalendarTime.Wall(DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(), zoneId);
        public void Refresh() { now = DesktopCalendarTime.Wall(DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(), zoneId); InvalidateVisual(); }
        protected override void OnRender(DrawingContext drawing)
        {
            base.OnRender(drawing);
            if (now.Date < start || now.Date >= start.AddDays(count)) return;
            var y = TimeInset + now.TimeOfDay.TotalMinutes * HourHeight / 60;
            var today = (now.Date - start).Days; var x = TimeAxisWidth + today * Math.Max(0, ActualWidth - TimeAxisWidth) / count;
            var red = B(Dark ? "#FF8A91" : "#F24E59");
            drawing.DrawLine(new Pen(red, 1) { DashStyle = DashStyles.Dash }, new Point(TimeAxisWidth, y), new Point(x, y));
            drawing.DrawLine(new Pen(red, 1.4), new Point(x, y), new Point(ActualWidth, y));
            drawing.DrawEllipse(red, null, new Point(x, y), 3.5, 3.5);
            var dpi = VisualTreeHelper.GetDpi(this);
            var text = new FormattedText(now.ToString("HH:mm"), CalendarCulture, System.Windows.FlowDirection.LeftToRight, new Typeface(InterfaceFont, FontStyles.Normal, FontWeights.Normal, FontStretches.Normal), TextSize(11), red, dpi.PixelsPerDip);
            drawing.DrawRectangle(Paper, null, new Rect(0, y - 9, 54, 18)); drawing.DrawText(text, new Point(6, y - text.Height / 2));
        }
    }

    private UIElement MonthCalendar(bool sunday)
    {
        var first = WeekStart(new DateTime(selectedDay.Year, selectedDay.Month, 1), sunday);
        var grid = new System.Windows.Controls.Primitives.UniformGrid { Columns = 7, Rows = 6 };
        for (var i = 0; i < 42; i++) {
            var day = first.AddDays(i); var events = EventsOn(day).Where(item => CalendarVisible(item)).ToList();
            var date = Text(day.ToString("M/d"), 15, day == CalendarNow.Date ? Ocean : Muted, day == CalendarNow.Date);
            var box = Column(date);
            foreach (var item in events.Take(3)) box.Children.Add(new Border { Background = EventSurface(item.AccentColor, workspace.Document!.ClassicCalendarColors), Padding = new Thickness(3), Margin = new Thickness(0, 2, 0, 0), Child = LimitedTitle(item.Title, 12, EventInk(item.AccentColor, workspace.Document.ClassicCalendarColors), 1) });
            if (events.Count > 3) box.Children.Add(Text($"还有 {events.Count - 3} 项", 11, Muted));
            var cell = new Border { Child = box, Padding = new Thickness(8), Background = day == CalendarNow.Date ? B("#E8F0FF") : Paper,
                BorderBrush = B("#E1E7F0"), BorderThickness = new Thickness(0, 0, 1, 1), Opacity = day.Month == selectedDay.Month ? 1 : 0.55, Cursor = System.Windows.Input.Cursors.Hand };
            cell.MouseLeftButtonUp += (_, _) => { selectedDay = day; calendarView = "日"; Render(); }; grid.Children.Add(cell);
        }
        return grid;
    }
}
