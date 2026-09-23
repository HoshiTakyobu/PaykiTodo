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
using MessageBox = System.Windows.MessageBox;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow : Window
{
    private readonly DesktopWorkspace workspace;
    private Grid root = new();
    private ContentControl content = new();
    private StackPanel navigation = new();
    private TextBlock title = Text("每日看板", 25, bold: true);
    private TextBlock status = Text("", 12, Muted);
    private TextBox search = Input();
    private readonly DispatcherTimer timer = new() { Interval = TimeSpan.FromSeconds(30) };
    private readonly Dictionary<string, Button> navigationButtons = new();
    private string page = "每日看板";
    private DateTime selectedDay = DateTime.Today;
    private string calendarView = "周";
    private long? selectedGroup;
    private bool history;
    private bool editingPlanning;
    private DesktopReminders? reminders;

    public NativeWindow(DesktopWorkspace? workspace = null)
    {
        this.workspace = workspace ?? new DesktopWorkspace();
        Dark = this.workspace.Document?.Settings["themeMode"]?.GetValue<string>() == "DARK";
        Title = "PaykiTodo"; Width = 1280; Height = 850; MinWidth = 900; MinHeight = 620;
        Icon = BitmapFrame.Create(new Uri("pack://application:,,,/PaykiTodo;component/Assets/PaykiTodo.ico"));
        WindowStartupLocation = WindowStartupLocation.CenterScreen;
        FontFamily = new System.Windows.Media.FontFamily("Microsoft YaHei UI");
        FontSize = 14; Background = B("#EEF3F4");
        InstallStyles(Resources); BuildShell();
        this.workspace.Changed += OnWorkspaceChanged;
        timer.Tick += async (_, _) => await this.workspace.SyncAsync(); timer.Start();
        Closed += (_, _) => { timer.Stop(); reminders?.Dispose(); this.workspace.Changed -= OnWorkspaceChanged; };
        Loaded += async (_, _) => { if (workspace == null) reminders = new DesktopReminders(this, this.workspace); await this.workspace.SyncAsync(); };
        Render();
    }

    private void BuildShell()
    {
        var query = search.Text;
        root = new Grid(); content = new ContentControl(); navigation = new StackPanel();
        title = Text("每日看板", 25, bold: true); status = Text("", 12, Muted); search = Input(query);
        navigationButtons.Clear(); InputBindings.Clear(); Background = B("#EEF3F4"); Foreground = Ink;
        root.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(220) });
        root.ColumnDefinitions.Add(new ColumnDefinition()); Content = root;
        var rail = new DockPanel { Background = B("#F8FAF9"), Margin = new Thickness(0) };
        var brand = Column(Text("PaykiTodo", 26, Forest, true), Text("把今天，安排得刚刚好", 12, Muted));
        brand.Margin = new Thickness(24, 30, 16, 30); DockPanel.SetDock(brand, Dock.Top); rail.Children.Add(brand);
        var account = Action("◉  账号与同步", () => Navigate("设置"));
        account.Margin = new Thickness(16); DockPanel.SetDock(account, Dock.Bottom); rail.Children.Add(account);
        foreach (var (icon, label) in new[] { ("◈", "每日看板"), ("✓", "待办"), ("▦", "日历"), ("▤", "规划台"), ("◷", "倒数日"), ("✧", "AI 报告"), ("⚙", "设置") })
        {
            var button = Action(icon + "    " + label, () => Navigate(label));
            button.HorizontalContentAlignment = System.Windows.HorizontalAlignment.Left;
            button.Margin = new Thickness(14, 3, 14, 3); button.Padding = new Thickness(18, 13, 12, 13);
            navigationButtons[label] = button; navigation.Children.Add(button);
        }
        rail.Children.Add(navigation); root.Children.Add(rail);
        var main = new DockPanel(); Grid.SetColumn(main, 1); root.Children.Add(main);
        var footer = new Border { Padding = new Thickness(26, 8, 24, 8), Background = B("#F8FAF9"), Child = status };
        DockPanel.SetDock(footer, Dock.Bottom); main.Children.Add(footer);
        var top = new Grid { Margin = new Thickness(28, 22, 24, 14) };
        top.ColumnDefinitions.Add(new ColumnDefinition()); top.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        top.Children.Add(title); search.Width = 230; search.MinHeight = 36; search.ToolTip = "搜索标题、备注和地点，按 Enter 查看";
        search.KeyDown += (_, e) => { if (e.Key == System.Windows.Input.Key.Enter) Navigate("搜索"); };
        System.Windows.Automation.AutomationProperties.SetName(search, "搜索事项");
        Grid.SetColumn(search, 1); top.Children.Add(search); DockPanel.SetDock(top, Dock.Top); main.Children.Add(top);
        main.Children.Add(content);
        var keys = new System.Windows.Input.KeyBinding(new NativeCommand(() => EditItem(page == "日历")),
            new System.Windows.Input.KeyGesture(System.Windows.Input.Key.N, System.Windows.Input.ModifierKeys.Control));
        InputBindings.Add(keys);
    }

    private void OnWorkspaceChanged()
    {
        if (!Dispatcher.CheckAccess()) { Dispatcher.Invoke(OnWorkspaceChanged); return; }
        status.Text = workspace.Status;
        if (workspace.Document == null) {
            editingPlanning = false;
            reminders?.Clear();
            foreach (var window in OwnedWindows.Cast<Window>().ToArray()) window.Close();
        }
        // Background synchronization never throws away the planning draft.
        if (!editingPlanning) Render();
    }
    private void Navigate(string next) { editingPlanning = false; page = next; Render(); }
    private void Render()
    {
        title.Text = page; status.Text = workspace.Status;
        foreach (var pair in navigationButtons)
        { pair.Value.Background = pair.Key == page ? B("#DCEBE2") : B("#F8FAF9"); pair.Value.FontWeight = pair.Key == page ? FontWeights.Bold : FontWeights.Normal; }
        if (workspace.Document == null) { content.Content = LoginPage(); return; }
        content.Content = page switch {
            "每日看板" => BoardPage(), "待办" => TodosPage(), "日历" => CalendarPage(), "规划台" => PlanningPage(),
            "设置" => SettingsPage(), "AI 报告" => ReportsPage(), "倒数日" => CountdownPage(), "搜索" => SearchPage(), _ => BoardPage()
        };
    }
    private UIElement Page(UIElement body, bool landscape = false)
    {
        var frame = new Border { Padding = new Thickness(28, 12, 28, 24), Child = Scroll(body) };
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
    private bool Confirm(string text) => MessageBox.Show(this, text, "PaykiTodo", MessageBoxButton.OKCancel, MessageBoxImage.Question) == MessageBoxResult.OK;
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
