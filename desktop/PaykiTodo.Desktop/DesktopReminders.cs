using System.Text.Json.Nodes;
using System.Windows;
using System.Windows.Threading;
using static PaykiTodo.Desktop.NativeUi;
using HorizontalAlignment = System.Windows.HorizontalAlignment;

namespace PaykiTodo.Desktop;

internal sealed record DesktopReminder(long ItemId, long Trigger, bool IsFinal);

internal sealed partial class DesktopSnapshotDocument
{
    public IReadOnlyList<DesktopReminder> RemindersBetween(long after, long until)
    {
        var result = new List<DesktopReminder>();
        foreach (var item in Items.Where(item => !item.Completed && !item.Canceled)) {
            var raw = FindTask(item.Id)!;
            if (raw["reminderEnabled"]?.GetValue<bool>() != true) continue;
            var offsets = (raw["reminderOffsetsCsv"]?.GetValue<string>() ?? "").Split(',', StringSplitOptions.RemoveEmptyEntries)
                .Select(value => int.TryParse(value, out var offset) ? offset : -1).Where(value => value >= 0).Distinct().ToList();
            var anchor = item.IsEvent ? item.StartAtMillis ?? item.DueAtMillis : item.DueAtMillis;
            var triggers = offsets.Count > 0 && anchor != long.MaxValue ? offsets.Select(offset => anchor - offset * 60000L).Order().ToList()
                : raw["reminderAtMillis"]?.GetValue<long?>() is { } single ? new List<long> { single } : new List<long>();
            foreach (var trigger in triggers.Where(trigger => trigger > after && trigger <= until))
                result.Add(new DesktopReminder(item.Id, trigger, trigger == triggers.Last()));
        }
        return result.OrderBy(reminder => reminder.Trigger).ToList();
    }
    public void Snooze(long id, long nextReminder)
    {
        var item = FindTask(id); if (item == null || item["completed"]?.GetValue<bool>() == true || item["canceled"]?.GetValue<bool>() == true) return;
        item["reminderAtMillis"] = nextReminder; item["reminderOffsetsCsv"] = ""; item["reminderOffsetMinutes"] = null;
        item["reminderEnabled"] = true; item["missed"] = false; item["missedAtMillis"] = null;
    }
}

internal sealed class DesktopReminders : IDisposable
{
    private readonly Window owner;
    private readonly DesktopWorkspace workspace;
    private readonly System.Windows.Forms.NotifyIcon tray;
    private readonly System.Drawing.Icon trayIcon;
    private readonly DispatcherTimer timer = new() { Interval = TimeSpan.FromSeconds(2) };
    private readonly Queue<DesktopReminder> pending = new();
    private long checkedAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
    private Window? alert;
    private long? notifiedItem;
    public DesktopReminders(Window owner, DesktopWorkspace workspace)
    {
        this.owner = owner; this.workspace = workspace;
        workspace.LoginRequired += OnLoginRequired;
        using (var stream = System.Windows.Application.GetResourceStream(new Uri("pack://application:,,,/PaykiTodo;component/Assets/PaykiTodo.ico")).Stream)
            trayIcon = new System.Drawing.Icon(stream);
        tray = new System.Windows.Forms.NotifyIcon { Icon = trayIcon, Text = "PaykiTodo", Visible = true };
        tray.DoubleClick += (_, _) => { owner.Show(); owner.WindowState = WindowState.Normal; owner.Activate(); };
        tray.BalloonTipClicked += (_, _) => {
            if (workspace.SessionExpired) { owner.Show(); owner.WindowState = WindowState.Normal; owner.Activate(); return; }
            var item = workspace.Document?.Items.FirstOrDefault(item => item.Id == notifiedItem);
            if (item != null && !item.Completed && !item.Canceled) { owner.Show(); owner.WindowState = WindowState.Normal; owner.Activate(); }
        };
        var menu = new System.Windows.Forms.ContextMenuStrip();
        menu.Items.Add("打开 PaykiTodo", null, (_, _) => { owner.Show(); owner.WindowState = WindowState.Normal; owner.Activate(); });
        menu.Items.Add("退出", null, (_, _) => owner.Close()); tray.ContextMenuStrip = menu;
        timer.Tick += (_, _) => Tick(); timer.Start();
    }
    private void OnLoginRequired() => owner.Dispatcher.Invoke(() => tray.ShowBalloonTip(10000, "PaykiTodo 登录已过期", "打开账号重新登录。本机数据和离线提醒保留。", System.Windows.Forms.ToolTipIcon.Info));
    private void Tick()
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        // Do not replay a night's worth of reminders after sleep/resume.
        var start = Math.Max(checkedAt, now - 60000);
        if (workspace.Document != null) foreach (var reminder in workspace.Document.RemindersBetween(start, now)) pending.Enqueue(reminder);
        checkedAt = now;
        if (alert != null || pending.Count == 0) return;
        var current = pending.Dequeue();
        var item = workspace.Document?.Items.FirstOrDefault(item => item.Id == current.ItemId && !item.Completed && !item.Canceled);
        if (item == null) return;
        notifiedItem = item.Id;
        tray.ShowBalloonTip(10000, item.IsEvent ? "日程提醒" : "待办提醒", item.Title, System.Windows.Forms.ToolTipIcon.Info);
        alert = Dialog(owner, "PaykiTodo 提醒", 680, 480); alert.Topmost = true;
        var window = alert;
        var clock = Text(DateTime.Now.ToString("HH:mm"), 48, Forest, true); clock.TextAlignment = TextAlignment.Center;
        var heading = Text(item.Title, 26, bold: true); heading.TextAlignment = TextAlignment.Center;
        var body = Column(BrandHeading(item.IsEvent ? "日程提醒" : "待办提醒"), clock, heading, Text(item.Notes, 15, Muted));
        var actions = new System.Windows.Controls.WrapPanel { HorizontalAlignment = HorizontalAlignment.Center };
        if (!item.IsEvent) actions.Children.Add(Action("我已完成", () => { workspace.Edit(document => document.Complete(item.Id)); window.Close(); }, true));
        if (!item.IsEvent && current.IsFinal) actions.Children.Add(Action("延后 10 分钟", () => {
            workspace.Edit(document => document.Snooze(item.Id, DateTimeOffset.UtcNow.AddMinutes(10).ToUnixTimeMilliseconds())); window.Close();
        }));
        actions.Children.Add(Action("我知道了", () => window.Close()));
        window.Content = DialogLayout(body, HorizontalAlignment.Center, actions);
        window.Closed += (_, _) => alert = null;
        System.Media.SystemSounds.Exclamation.Play(); window.Show();
    }
    public void Clear() { pending.Clear(); notifiedItem = null; alert?.Close(); checkedAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(); }
    public void Dispose() { workspace.LoginRequired -= OnLoginRequired; timer.Stop(); Clear(); tray.Dispose(); trayIcon.Dispose(); }
}
