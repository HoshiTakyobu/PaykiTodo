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
    private Grid root = new();
    private ContentControl content = new();
    private StackPanel navigation = new();
    private TextBlock title = Text("每日看板", 25, bold: true);
    private TextBlock status = Text("", 12, Muted);
    private TextBox search = Input();
    private readonly DispatcherTimer timer = new() { Interval = TimeSpan.FromSeconds(15) };
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
    private readonly Dictionary<long, (string Title, string Markdown)> planningDrafts = new();

    public NativeWindow(DesktopWorkspace? workspace = null, string? profileDirectory = null)
    {
        this.profileDirectory = profileDirectory ?? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PaykiTodo");
        this.workspace = workspace ?? new DesktopWorkspace(new DesktopSecureStore(this.profileDirectory));
        Dark = this.workspace.Document?.Settings["themeMode"]?.GetValue<string>() == "DARK";
        Title = "PaykiTodo"; Width = 1280; Height = 850; MinWidth = 900; MinHeight = 620;
        Icon = AppIcon; UseLayoutRounding = true; SnapsToDevicePixels = true;
        WindowStartupLocation = WindowStartupLocation.CenterScreen;
        FontFamily = InterfaceFont;
        ConfigureRendering(this);
        FontSize = 14; Background = B("#EEF3F4");
        BuildShell();
        this.workspace.Changed += OnWorkspaceChanged;
        timer.Tick += async (_, _) => await this.workspace.SyncAsync(); timer.Start();
        Closed += (_, _) => { timer.Stop(); calendarClock?.Stop(); reminders?.Dispose(); this.workspace.Changed -= OnWorkspaceChanged; };
        Loaded += async (_, _) => { if (workspace == null) reminders = new DesktopReminders(this, this.workspace); await this.workspace.SyncAsync(); };
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
        foreach (var (icon, label) in new[] { ("\uE80F", "每日看板"), ("\uE73E", "待办"), ("\uE787", "日历"), ("\uE70B", "规划台"), ("\uE823", "倒数日"), ("\uE9D9", "AI 报告"), ("\uE713", "设置") })
        {
            var button = NavigationAction(icon, label, () => Navigate(label));
            navigationButtons[label] = button; navigation.Children.Add(button);
        }
        rail.Children.Add(navigation); root.Children.Add(rail);
        var main = new DockPanel(); Grid.SetColumn(main, 1); root.Children.Add(main);
        var footer = new Border { Padding = new Thickness(26, 8, 24, 8), Background = B("#F8FAF9"), Child = status };
        DockPanel.SetDock(footer, Dock.Bottom); main.Children.Add(footer);
        var top = new Grid { Margin = new Thickness(28, 22, 24, 14) };
        top.ColumnDefinitions.Add(new ColumnDefinition()); top.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        title.VerticalAlignment = VerticalAlignment.Center; top.Children.Add(title);
        search.MinHeight = 42; search.Margin = new Thickness(0); search.Padding = new Thickness(38, 9, 42, 9);
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
    }

    private void OnWorkspaceChanged()
    {
        if (!Dispatcher.CheckAccess()) { Dispatcher.Invoke(OnWorkspaceChanged); return; }
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
    private void Navigate(string next) { editingPlanning = false; page = next; Render(); }
    private void Render()
    {
        calendarClock?.Stop();
        Resources["PaykiTodo.FocusBrush"] = page == "日历" ? Ocean : Forest;
        title.Text = workspace.Document == null ? "账号登录" : reauthenticating ? "重新登录" : page; status.Text = workspace.Status;
        foreach (var pair in navigationButtons)
        {
            var selected = workspace.Document != null && !reauthenticating && pair.Key == page;
            pair.Value.Background = selected ? B(page == "日历" ? "#E8F0FF" : "#DCEBE2") : B("#F8FAF9");
            pair.Value.BorderThickness = new Thickness(selected ? 3 : 0, 0, 0, 0);
            pair.Value.BorderBrush = selected && page == "日历" ? Ocean : Forest;
            if (pair.Value.Content is Grid labels && labels.Children[1] is TextBlock label)
                label.Foreground = selected ? (page == "日历" ? Ocean : Forest) : Ink;
        }
        renderedDocument = workspace.Document;
        if (workspace.Document == null || reauthenticating) { content.Content = LoginPage(); return; }
        content.Content = page switch {
            "每日看板" => BoardPage(), "待办" => TodosPage(), "日历" => CalendarPage(), "规划台" => PlanningPage(),
            "设置" => SettingsPage(), "AI 报告" => ReportsPage(), "倒数日" => CountdownPage(), "搜索" => SearchPage(), _ => BoardPage()
        };
    }
    private UIElement Page(UIElement body, bool landscape = false, bool scroll = true)
    {
        var frame = new Border { Padding = new Thickness(28, 12, 28, 24), Child = scroll ? Scroll(body) : body };
        if (landscape) frame.Background = new ImageBrush(new BitmapImage(new Uri("pack://application:,,,/PaykiTodo;component/Assets/dashboard-" + (Dark ? "dark" : "light") + ".png"))) { Stretch = Stretch.UniformToFill, Opacity = 0.75 };
        return frame;
    }
    private void Change(Action<DesktopSnapshotDocument> action)
    {
        try { workspace.Edit(action); } catch (Exception error) { workspace.Notify(DesktopWorkspace.FriendlyError(error)); }
    }
    private void EditItem(bool isEvent, long? id = null, DateTime? seed = null)
    {
        if (workspace.Document == null) return;
        var existing = id.HasValue ? workspace.Document.EditFor(id.Value) : null;
        var edit = NativeEditors.Item(this, isEvent, workspace.Document.Groups, existing, seed ?? selectedDay);
        if (edit != null) Change(document => document.SaveItem(edit, id));
    }
    private void CreateChecklist()
    {
        var draft = NativeEditors.Checklist(this);
        if (draft != null) Change(document => document.CreateQuickChecklist(draft));
    }
    private bool Confirm(string text) => Confirmation(this, text);
    private static string TimeLabel(DesktopItem item) => item.IsEvent
        ? Local(item.StartAtMillis).ToString("MM-dd HH:mm") + " – " + Local(item.EndAtMillis).ToString("HH:mm")
        : item.DueAtMillis == long.MaxValue ? "今天想做的事" : "DDL  " + Local(item.DueAtMillis).ToString("yyyy-MM-dd HH:mm");
    private static DateTime Local(long? value) => value is null or long.MaxValue ? DateTime.Today : DateTimeOffset.FromUnixTimeMilliseconds(value.Value).LocalDateTime;
    private IEnumerable<DesktopItem> Active => workspace.Document!.Items.Where(item => !item.Canceled && !item.Completed);
    private IEnumerable<DesktopItem> EventsOn(DateTime date) => Active.Where(item => item.IsEvent && Local(item.StartAtMillis) < date.Date.AddDays(1) && Local(item.EndAtMillis) > date.Date);
    private sealed class NativeCommand(Action action) : System.Windows.Input.ICommand
    {
        public event EventHandler? CanExecuteChanged { add { } remove { } }
        public bool CanExecute(object? parameter) => true;
        public void Execute(object? parameter) => action();
    }
}
