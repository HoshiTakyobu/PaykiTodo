using System.Diagnostics;
using System.Text.Json;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using static PaykiTodo.Desktop.NativeUi;
using Button = System.Windows.Controls.Button;
using TextBox = System.Windows.Controls.TextBox;
using Orientation = System.Windows.Controls.Orientation;
using FontFamily = System.Windows.Media.FontFamily;
using Brushes = System.Windows.Media.Brushes;
using HorizontalAlignment = System.Windows.HorizontalAlignment;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow : Window
{
    private readonly DesktopWorkspace workspace;
    private readonly string profileDirectory;
    private readonly bool isolatedProfile;
    private Grid root = new();
    private ContentControl content = new();
    private StackPanel navigation = new();
    private TextBlock title = Text("每日看板", 25, bold: true);
    private TextBlock status = Text("", 12, Muted);
    private TextBox search = Input();
    private readonly DispatcherTimer timer = new() { Interval = TimeSpan.FromSeconds(3) };
    private readonly DispatcherTimer reportExpiryTimer = new();
    private bool syncTickRunning;
    private readonly Dictionary<string, Button> navigationButtons = new();
    private string page = "每日看板";
    private DateTime selectedDay = DateTime.Today;
    private string calendarView = "周";
    private long? selectedGroup;
    private bool history;
    private bool editingPlanning;
    private DesktopReminders? reminders;
    private bool reauthenticating;
    private DesktopSnapshotDocument? renderedDocument;
    private IReadOnlyList<DesktopItem> renderedItems = Array.Empty<DesktopItem>();
    private readonly Dictionary<long, (string Title, string Markdown)> planningDrafts = new();

    public NativeWindow(DesktopWorkspace? workspace = null, string? profileDirectory = null)
    {
        isolatedProfile = workspace != null || profileDirectory != null;
        this.profileDirectory = profileDirectory ?? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PaykiTodo");
        this.workspace = workspace ?? new DesktopWorkspace(new DesktopSecureStore(this.profileDirectory));
        Dark = this.workspace.Document?.Settings["themeMode"]?.GetValue<string>() == "DARK";
        SetInterfaceFont(this.workspace.Document?.Settings["desktopFontFamily"]?.GetValue<string>());
        SetInterfaceFontSize(DesktopFonts.ReadSize(this.workspace.Document?.Settings));
        Title = "PaykiTodo"; Width = 1280; Height = 850; MinWidth = 900; MinHeight = 620;
        Icon = AppIcon; UseLayoutRounding = true; SnapsToDevicePixels = true;
        WindowStartupLocation = WindowStartupLocation.CenterScreen;
        FontFamily = InterfaceFont;
        ConfigureRendering(this);
        FontSize = TextSize(14); Background = B("#EEF3F4");
        BuildShell();
        SizeChanged += (_, e) => {
            if (page == "日历" && ((e.PreviousSize.Width < TextSize(1100)) != (e.NewSize.Width < TextSize(1100)) || (e.PreviousSize.Width < TextSize(1350)) != (e.NewSize.Width < TextSize(1350))))
                Dispatcher.BeginInvoke(new Action(() => { if (page == "日历") Render(); }));
        };
        PreviewKeyDown += (_, e) => { if (e.Key == System.Windows.Input.Key.Escape && calendarDraft != null) { ClearCalendarDraft(); e.Handled = true; } };
        this.workspace.Changed += OnWorkspaceChanged;
        reportExpiryTimer.Tick += (_, _) => {
            reportExpiryTimer.Stop();
            try { this.workspace.ExpireReports(); }
            catch (Exception error) { this.workspace.Notify(DesktopWorkspace.FriendlyError(error)); }
            if (page == "AI 报告") Render();
            ScheduleReportExpiry();
        };
        ScheduleReportExpiry();
        timer.Tick += async (_, _) => {
            if (syncTickRunning) return;
            syncTickRunning = true;
            try { await this.workspace.SyncAsync(); await this.workspace.SyncSharedCalendarsAsync(); }
            catch (Exception error) { this.workspace.Notify(DesktopWorkspace.FriendlyError(error)); }
            finally { syncTickRunning = false; }
            ReviewCountdowns();
        }; timer.Start();
        Closed += (_, _) => { updateCancellation?.Cancel(); timer.Stop(); reportExpiryTimer.Stop(); calendarClock?.Stop(); reminders?.Dispose(); this.workspace.Changed -= OnWorkspaceChanged; };
        Loaded += async (_, _) => { UpdateStartup(workspace == null && profileDirectory == null); if (workspace == null) reminders = new DesktopReminders(this, this.workspace, OpenItemDetails); await this.workspace.SyncAsync(); };
        Render();
    }

    private void BuildShell()
    {
        var query = search.Text;
        root = new Grid(); content = new ContentControl(); navigation = new StackPanel();
        title = Text("每日看板", 25, bold: true); status = Text("", 12, Muted); search = Input(query);
        navigationButtons.Clear(); InputBindings.Clear(); InstallStyles(Resources); Background = B("#EEF3F4"); Foreground = Ink;
        root.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(204) });
        root.ColumnDefinitions.Add(new ColumnDefinition()); Content = root;
        var rail = new DockPanel { Background = B("#F8FAF9"), Margin = new Thickness(0) };
        var brand = Column(BrandHeading("PaykiTodo"), Text("把今天，安排得刚刚好", 12, Muted));
        brand.Margin = new Thickness(18, 24, 12, 24); DockPanel.SetDock(brand, Dock.Top); rail.Children.Add(brand);
        foreach (var (icon, label) in new[] { ("\uE80F", "每日看板"), ("\uE73E", "待办"), ("\uE787", "日历"), ("\uE823", "倒数日"), ("\uE9D9", "AI 报告"), ("\uE713", "设置") })
        {
            var button = NavigationAction(icon, label, () => Navigate(label));
            navigationButtons[label] = button; navigation.Children.Add(button);
        }
        rail.Children.Add(navigation); root.Children.Add(rail);
        var main = new DockPanel(); Grid.SetColumn(main, 1); root.Children.Add(main);
        var footer = new Border { Padding = new Thickness(26, 8, 24, 8), Background = B("#F8FAF9"), Child = UpdateFooter() };
        DockPanel.SetDock(footer, Dock.Bottom); main.Children.Add(footer);
        var top = new Grid { Margin = new Thickness(28, 22, 24, 14) };
        top.ColumnDefinitions.Add(new ColumnDefinition()); top.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        title.VerticalAlignment = VerticalAlignment.Center; top.Children.Add(title);
        search.MinHeight = Math.Max(42, search.MinHeight); search.Margin = new Thickness(0); search.Padding = new Thickness(38, 9, 42, 9);
        search.TextWrapping = TextWrapping.NoWrap; search.ToolTip = "搜索标题、备注和地点 · Ctrl+F 定位 · Enter 查看";
        search.KeyDown += (_, e) => { if (e.Key == System.Windows.Input.Key.Enter) Navigate("搜索"); };
        System.Windows.Automation.AutomationProperties.SetName(search, "搜索事项");
        var searchBox = new Grid { Width = 260, Margin = new Thickness(12, 0, 0, 0) };
        searchBox.Children.Add(search);
        var searchButton = Action("执行搜索", () => Navigate("搜索"));
        searchButton.Content = new TextBlock { Text = "\uE721", FontFamily = new FontFamily("Segoe MDL2 Assets"), FontSize = 16, Foreground = Muted };
        searchButton.Width = 34; searchButton.MinHeight = 0; searchButton.Padding = new Thickness(6); searchButton.Margin = new Thickness(2);
        searchButton.Background = Brushes.Transparent; searchButton.BorderThickness = new Thickness(0); searchButton.HorizontalAlignment = HorizontalAlignment.Left;
        searchBox.Children.Add(searchButton);
        var hint = Text("搜索待办、日程…", 13, Muted); hint.FontFamily = UtilityFont;
        hint.IsHitTestVisible = false; hint.Margin = new Thickness(38, 0, 40, 0); hint.VerticalAlignment = VerticalAlignment.Center;
        hint.Visibility = string.IsNullOrEmpty(search.Text) ? Visibility.Visible : Visibility.Collapsed;
        search.TextChanged += (_, _) => hint.Visibility = string.IsNullOrEmpty(search.Text) ? Visibility.Visible : Visibility.Collapsed;
        searchBox.Children.Add(hint);
        var enter = Text("Enter", 10, Muted); enter.IsHitTestVisible = false; enter.HorizontalAlignment = HorizontalAlignment.Right;
        enter.VerticalAlignment = VerticalAlignment.Center; enter.Margin = new Thickness(0, 0, 9, 0); searchBox.Children.Add(enter);
        Grid.SetColumn(searchBox, 1); top.Children.Add(searchBox); DockPanel.SetDock(top, Dock.Top); main.Children.Add(top);
        main.Children.Add(content);
        var keys = new System.Windows.Input.KeyBinding(new NativeCommand(() => EditItem(page == "日历")),
            new System.Windows.Input.KeyGesture(System.Windows.Input.Key.N, System.Windows.Input.ModifierKeys.Control));
        InputBindings.Add(keys);
        InputBindings.Add(new System.Windows.Input.KeyBinding(new NativeCommand(() => { search.Focus(); search.SelectAll(); }),
            new System.Windows.Input.KeyGesture(System.Windows.Input.Key.F, System.Windows.Input.ModifierKeys.Control)));
        InputBindings.Add(new System.Windows.Input.KeyBinding(new NativeCommand(ShowUsageGuide), new System.Windows.Input.KeyGesture(System.Windows.Input.Key.F1)));
    }

    private void ScheduleReportExpiry()
    {
        reportExpiryTimer.Stop();
        if (workspace.Document?.NextDailyReportExpiry is not { } expiry) return;
        reportExpiryTimer.Interval = TimeSpan.FromMilliseconds(Math.Clamp(expiry - DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(), 1, 60_000));
        reportExpiryTimer.Start();
    }

    private void OnWorkspaceChanged()
    {
        if (!Dispatcher.CheckAccess()) { Dispatcher.Invoke(OnWorkspaceChanged); return; }
        ScheduleReportExpiry();
        status.Text = workspace.Status;
        if (workspace.Document == null) {
            editingPlanning = false;
            planningDrafts.Clear(); reauthenticating = false;
            reminders?.Clear();
            foreach (var window in OwnedWindows.Cast<Window>().ToArray()) window.Close();
        }
        // Background synchronization never throws away the planning draft.
        if (!editingPlanning && (workspace.Document != renderedDocument || workspace.Document == null)) Render();
    }
    private void Navigate(string next) { editingPlanning = false; ClearCalendarDraft(); page = next; Render(); }
    private void Render()
    {
        var settings = workspace.Document?.Settings;
        var font = DesktopFonts.Find(settings?["desktopFontFamily"]?.GetValue<string>()) ?? DesktopFonts.Choices[0];
        var size = DesktopFonts.ReadSize(settings);
        if (InterfaceFont.Source != font.Family.Source || InterfaceFontSize != size) {
            SetInterfaceFont(font.Key); SetInterfaceFontSize(size); FontFamily = InterfaceFont; FontSize = TextSize(14); BuildShell();
        }
        calendarClock?.Stop();
        Resources["PaykiTodo.FocusBrush"] = page == "日历" ? Ocean : Forest;
        title.Text = workspace.Document == null ? "账号登录" : reauthenticating ? "重新登录" : page; status.Text = workspace.Status;
        foreach (var pair in navigationButtons)
        {
            var selected = workspace.Document != null && !reauthenticating && pair.Key == page;
            pair.Value.Background = selected ? B(page == "日历" ? "#E8F0FF" : "#DCEBE2") : B("#F8FAF9");
            pair.Value.BorderThickness = new Thickness(selected ? 3 : 0, 0, 0, 0);
            pair.Value.BorderBrush = selected && page == "日历" ? Ocean : Forest;
            if (pair.Value.Content is Grid labels && labels.Children[1] is System.Windows.Shapes.Path label)
                label.Fill = selected ? (page == "日历" ? Ocean : Forest) : Ink;
        }
        renderedDocument = workspace.Document;
        renderedItems = workspace.Document?.Items ?? Array.Empty<DesktopItem>();
        if (workspace.Document == null || reauthenticating) { content.Content = LoginPage(); return; }
        content.Content = page switch {
            "每日看板" => BoardPage(), "待办" => TodosPage(), "日历" => CalendarPage(), "规划台" => PlanningPage(),
            "设置" => SettingsPage(), "AI 报告" => ReportsPage(), "倒数日" => CountdownPage(), "搜索" => SearchPage(), _ => BoardPage()
        };
        Dispatcher.BeginInvoke(new Action(ReviewCountdowns));
    }
    private UIElement Page(UIElement body, bool landscape = false, bool scroll = true)
    {
        var frame = new Border { Padding = new Thickness(28, 12, 28, 24), Child = scroll ? Scroll(body) : body };
        if (landscape) frame.Background = new ImageBrush(new BitmapImage(new Uri("pack://application:,,,/PaykiTodo;component/Assets/dashboard-" + (Dark ? "dark" : "light") + ".png"))) { Stretch = Stretch.UniformToFill, Opacity = 0.75 };
        return frame;
    }
    private void Change(Action<DesktopSnapshotDocument> action)
    {
        try { workspace.Edit(action); _ = SyncSharedChanges(); } catch (Exception error) { workspace.Notify(DesktopWorkspace.FriendlyError(error)); }
    }
    private async Task SyncSharedChanges() { try { await workspace.SyncSharedCalendarsAsync(); } catch (Exception error) { workspace.Notify(DesktopWorkspace.FriendlyError(error)); } }
    private void EditItem(bool isEvent, long? id = null, DateTime? seed = null, System.Windows.Point? anchorScreen = null)
    {
        if (workspace.Document == null) return;
        var existing = id.HasValue ? workspace.Document.EditFor(id.Value) : null;
        var scope = existing?.RecurrenceType is not (null or "NONE") ? NativeEditors.ChooseRecurrenceScope(this, "编辑循环事项") : RecurrenceScope.Current;
        if (scope == null) return;
        if (existing != null && !workspace.Document.SharedWritable(existing.CalendarId)) { workspace.Notify("你只有此共享日历的查看权限。"); return; }
        var calendars = workspace.Document.Calendars.Where(c => workspace.Document.SharedWritable(c.Id)).OrderBy(c => (workspace.Document.Settings["hiddenCalendarIds"] as System.Text.Json.Nodes.JsonArray)?.Any(v => v?.GetValue<string>() == c.Id) == true).ToList();
        var edit = GuardItemEditor(calendars, id, () => NativeEditors.Item(this, isEvent, workspace.Document!.Groups, existing, seed ?? selectedDay, calendars, scope.Value, anchorScreen, workspace.Document));
        if (edit != null) Change(document => document.SaveItem(edit, id, scope.Value));
    }
    private DesktopItemEdit? GuardItemEditor(IReadOnlyList<DesktopCalendar> calendars, long? sourceId, Func<DesktopItemEdit?> open)
    {
        var generation = workspace.AccountGeneration;
        bool Accessible() => generation == workspace.AccountGeneration && workspace.Document != null &&
            (sourceId == null || CanReadItemDetails(workspace.Document.Items.FirstOrDefault(item => item.Id == sourceId))) &&
            calendars.Where(c => DesktopSnapshotDocument.IsSharedCalendar(c.Id)).All(c => workspace.Document.Calendars.Any(current => current.Id == c.Id &&
                current.ShareMode == "SHARED" && current.SharedRole.ToLowerInvariant() is "owner" or "editor" &&
                string.Equals(current.SharedServerUrl, workspace.Account?.ServerUrl, StringComparison.OrdinalIgnoreCase)));
        void CheckEditorAccess() {
            if (!Dispatcher.CheckAccess()) { Dispatcher.Invoke(CheckEditorAccess); return; }
            if (Accessible()) return;
            foreach (var dialog in OwnedWindows.OfType<Window>().Where(value => value.Title is "新增日程" or "编辑日程" or "新增待办" or "编辑待办").ToArray()) {
                foreach (var nested in dialog.OwnedWindows.OfType<Window>().ToArray()) { nested.Content = null; nested.Close(); }
                dialog.Content = null; dialog.Close();
            }
        }
        if (!Accessible()) return null;
        workspace.Changed += CheckEditorAccess;
        try { var edit = open(); return Accessible() ? edit : null; }
        finally { workspace.Changed -= CheckEditorAccess; }
    }
    private void CreateChecklist()
    {
        var draft = NativeEditors.Checklist(this);
        if (draft != null) Change(document => document.CreateQuickChecklist(draft));
    }
    internal void OpenItemDetails(long id)
    {
        Show(); WindowState = WindowState.Normal; Activate();
        var item = workspace.Document?.Items.FirstOrDefault(value => value.Id == id);
        if (item == null) { workspace.Notify("这条事项已删除或当前不可用。"); return; }
        if (workspace.Document!.SharedCalendar(item.CalendarId)?["sharedRole"]?.GetValue<string>() == "revoked") { workspace.Notify("此日历的访问权限已撤销。"); return; }
        reauthenticating = false;
        if (item.IsEvent) {
            selectedDay = CalendarLocal(item, item.StartAtMillis).Date; miniMonth = selectedDay;
            if (workspace.Document!.Settings["hiddenCalendarIds"] is System.Text.Json.Nodes.JsonArray hidden && hidden.Any(value => value?.GetValue<string>() == item.CalendarId))
                Change(document => {
                    if (document.Settings["hiddenCalendarIds"] is System.Text.Json.Nodes.JsonArray values)
                        foreach (var value in values.Where(value => value?.GetValue<string>() == item.CalendarId).ToList()) values.Remove(value);
                });
        }
        Navigate(item.IsEvent ? "日历" : "待办"); Preview(item);
    }
    private void CopyItem(long id)
    {
        if (workspace.Document?.EditFor(id) is not { } source) return;
        var draft = workspace.Document.CopyDraft(id);
        var calendars = workspace.Document.Calendars.Where(calendar => workspace.Document.SharedWritable(calendar.Id)).ToList();
        var edit = GuardItemEditor(calendars, id, () => NativeEditors.Item(this, source.IsEvent, workspace.Document!.Groups, draft,
            DesktopCalendarTime.Wall(source.StartAtMillis ?? source.DueAtMillis ?? DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(), source.EventTimeZoneId), calendars, preferences: workspace.Document));
        if (edit != null) Change(document => document.SaveItem(edit));
    }
    private bool Confirm(string text) => Confirmation(this, text);
    private string TimeLabel(DesktopItem item) => item.IsEvent
        ? item.AllDay ? CalendarLocal(item, item.StartAtMillis).ToString("yyyy-MM-dd") + (CalendarLocal(item, item.EndAtMillis).AddTicks(-1).Date > CalendarLocal(item, item.StartAtMillis).Date ? " – " + CalendarLocal(item, item.EndAtMillis).AddTicks(-1).ToString("yyyy-MM-dd") : "") + " 全天"
        : CalendarLocal(item, item.StartAtMillis).ToString("MM-dd HH:mm") + " – " + CalendarLocal(item, item.EndAtMillis).ToString(CalendarLocal(item, item.EndAtMillis).Date == CalendarLocal(item, item.StartAtMillis).Date ? "HH:mm" : "MM-dd HH:mm")
        : item.DueAtMillis == long.MaxValue ? "今天想做的事" : "DDL  " + Local(item.DueAtMillis).ToString("yyyy-MM-dd HH:mm");
    private static DateTime Local(long? value) => value is null or long.MaxValue ? DateTime.Today : DateTimeOffset.FromUnixTimeMilliseconds(value.Value).LocalDateTime;
    private IEnumerable<DesktopItem> Active => renderedItems.Where(item => !item.Canceled && !item.Completed);
    private DateTime CalendarNow => DesktopCalendarTime.Wall(DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(), workspace.Document?.CalendarDisplayTimeZoneId);
    private DateTime CalendarLocal(DesktopItem item, long? millis) => millis is null or long.MaxValue ? CalendarNow.Date
        : DesktopCalendarTime.Wall(millis.Value, item.AllDay ? item.EventTimeZoneId : workspace.Document?.CalendarDisplayTimeZoneId);
    private IEnumerable<DesktopItem> EventsOn(DateTime date) => Active.Where(item => item.IsEvent && CalendarLocal(item, item.StartAtMillis) < date.Date.AddDays(1) && CalendarLocal(item, item.EndAtMillis) > date.Date);
    private sealed class NativeCommand(Action action) : System.Windows.Input.ICommand
    {
        public event EventHandler? CanExecuteChanged { add { } remove { } }
        public bool CanExecute(object? parameter) => true;
        public void Execute(object? parameter) => action();
    }
}
