using System.IO;
using System.IO.Compression;
using System.Text;
using System.Text.Json.Nodes;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class NativeWorkspaceTests
{
    [Fact]
    public void ReminderOffsetsUseEventStartAndOnlyLastOffsetAllowsSnooze()
    {
        var document = DesktopSnapshotDocument.CreateEmpty();
        var start = Millis(DateTime.Today.AddHours(12));
        var id = document.SaveItem(NewItem(true, "带提醒的日程") with { StartAtMillis = start, EndAtMillis = start + 3600000,
            ReminderEnabled = true, ReminderOffsets = new[] { 30, 15, 5 } });
        var reminders = document.RemindersBetween(start - 3600000, start);
        Assert.Equal(new[] { start - 1800000, start - 900000, start - 300000 }, reminders.Select(reminder => reminder.Trigger));
        Assert.Equal(new[] { false, false, true }, reminders.Select(reminder => reminder.IsFinal));
        document.Cancel(id); Assert.Empty(document.RemindersBetween(start - 3600000, start));
    }

    [Fact]
    public void RecurrenceRoundTripDoesNotRegenerateDeletedOccurrence()
    {
        var document = DesktopSnapshotDocument.CreateEmpty();
        document.SaveItem(NewItem(true, "每日会议") with { RecurrenceType = "DAILY", RecurrenceEndEpochDay = (long)(DateTime.Today.AddDays(3) - DateTime.UnixEpoch.Date).TotalDays });
        Assert.True(document.EnsureRecurringInstances(DateTime.Today));
        Assert.Equal(4, document.Items.Count);
        var target = document.Items[1]; document.Delete(target.Id);
        Assert.False(document.EnsureRecurringInstances(DateTime.Today));
        var restored = DesktopSnapshotDocument.FromArchiveBytes(document.ToArchiveBytes());
        Assert.False(restored.EnsureRecurringInstances(DateTime.Today)); Assert.Equal(3, restored.Items.Count);
    }

    [Fact]
    public async Task LogoutLocksWorkspaceAcrossRestartAndRetainsAccountData()
    {
        var store = new DesktopSecureStore(NewDirectory()); var document = DesktopSnapshotDocument.CreateEmpty(); document.SaveItem(NewItem(false, "保留的私有待办"));
        var state = new CloudAccountState("http://127.0.0.1:1", "fixture", "fixture", "", "", 0, 0, "fixture-secret", 0, true);
        store.Save(state, document);
        var workspace = new DesktopWorkspace(store);
        Assert.Null(workspace.Document); Assert.Null(workspace.Account);
        Assert.Single(store.Load()!.Value.Document.Items);
        await workspace.LogoutAsync(); Assert.Null(new DesktopWorkspace(store).Document);
    }
    [Fact]
    public void EditingPhoneItemPreservesReminderAndCheckinFields()
    {
        var document = DesktopSnapshotDocument.CreateEmpty();
        var edit = NewItem(true, "会议") with { CheckInEnabled = true, HiddenFromBoard = true };
        var id = document.SaveItem(edit);
        document.SaveItem(document.EditFor(id)! with { Title = "调整会议" }, id);
        Assert.True(document.EditFor(id)!.CheckInEnabled);
        Assert.True(document.EditFor(id)!.HiddenFromBoard);
        document.ToggleCheckIn(id);
        Assert.True(document.CheckedIn(id));
        document.ToggleCheckIn(id);
        Assert.False(document.CheckedIn(id));
        var restored = DesktopSnapshotDocument.FromArchiveBytes(document.ToArchiveBytes());
        Assert.Equal("调整会议", restored.Items.Single().Title);
    }

    [Fact]
    public void FailedEditDoesNotMutatePersistedWorkspace()
    {
        var store = new DesktopSecureStore(NewDirectory());
        var document = DesktopSnapshotDocument.CreateEmpty(); document.SaveItem(NewItem(false, "原始待办")); store.SaveDocument(document);
        var workspace = new DesktopWorkspace(store);
        Assert.Throws<InvalidOperationException>(() => workspace.Edit(target => { target.SaveItem(NewItem(false, "不该写入")); throw new InvalidOperationException(); }));
        Assert.Single(workspace.Document!.Items); Assert.Single(store.LoadDocument()!.Items);
        workspace.Edit(target => target.SaveItem(NewItem(false, "正常新增")));
        Assert.Equal(2, store.LoadDocument()!.Items.Count);
    }

    [Fact]
    public void NativeScreensRenderAndNavigateWithoutWebView()
    {
        Exception? failure = null;
        var thread = new Thread(() => {
            try {
                var app = new System.Windows.Application();
                var folder = NewDirectory(); var store = new DesktopSecureStore(folder); var document = DesktopSnapshotDocument.CreateEmpty();
                document.SaveItem(NewItem(false, "论文实验结果整理"));
                document.SaveItem(NewItem(false, "阅读相关工作"));
                document.SaveItem(NewItem(true, "讨论实验进展"));
                document.SaveItem(NewItem(true, "阅读小组") with { StartAtMillis = Millis(DateTime.Today.AddDays(1).AddHours(10)), EndAtMillis = Millis(DateTime.Today.AddDays(1).AddHours(11)) });
                document.CreateQuickChecklist(QuickChecklistParser.Parse("本周计划\n- 数据准备\n-- 检查样本\n--# 保留原始记录"));
                store.SaveDocument(document);
                var workspace = new DesktopWorkspace(store);
                var window = new NativeWindow(workspace) { Width = 1280, Height = 850, ShowActivated = false,
                    WindowStartupLocation = WindowStartupLocation.Manual, Left = -10000, Top = -10000, ShowInTaskbar = false };
                window.Show();
                window.Measure(new System.Windows.Size(1280, 850)); window.Arrange(new Rect(0, 0, 1280, 850)); window.UpdateLayout();
                var shotFolder = Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE") ?? folder;
                Directory.CreateDirectory(shotFolder);
                Screenshot(window, Path.Combine(shotFolder, "windows-board.png"));
                Assert.Contains(Descendants<TextBlock>(window), text => text.Text == "论文实验结果整理");
                Assert.DoesNotContain(Descendants<System.Windows.DependencyObject>(window), control => control.GetType().Name.Contains("WebView"));
                foreach (var page in new[] { "待办", "日历", "规划台", "倒数日", "AI 报告", "设置" }) {
                    var button = Descendants<Button>(window).First(button => button.Content is string label && label.EndsWith("    " + page));
                    button.RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
                    window.UpdateLayout();
                    if (page == "日历") Screenshot(window, Path.Combine(shotFolder, "windows-calendar.png"));
                }
                var toggle = Descendants<Button>(window).First(button => button.Content as string == "切换深色模式");
                toggle.RaiseEvent(new RoutedEventArgs(Button.ClickEvent)); window.UpdateLayout();
                Assert.True(NativeUi.Dark);
                var board = Descendants<Button>(window).First(button => button.Content is string label && label.EndsWith("    每日看板"));
                board.RaiseEvent(new RoutedEventArgs(Button.ClickEvent)); window.UpdateLayout();
                Screenshot(window, Path.Combine(shotFolder, "windows-board-dark.png"));
                NativeUi.Dark = false;
                var formTimer = new System.Windows.Threading.DispatcherTimer { Interval = TimeSpan.FromMilliseconds(100) };
                formTimer.Tick += (_, _) => {
                    var editor = app.Windows.OfType<Window>().FirstOrDefault(candidate => candidate != window && candidate.Title == "新增待办");
                    if (editor == null) return;
                    formTimer.Stop();
                    Descendants<TextBox>(editor).First().Text = "从原生编辑器创建";
                    var save = Descendants<Button>(editor).First(button => button.Content as string == "完成");
                    save.RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
                };
                formTimer.Start();
                var edit = NativeEditors.Item(window, false, document.Groups, null, DateTime.Today);
                Assert.Equal("从原生编辑器创建", edit!.Title);
                Assert.Null(edit.DueAtMillis);
                window.Close(); app.Shutdown();
            } catch (Exception error) { failure = error; }
        });
        thread.SetApartmentState(ApartmentState.STA); thread.Start(); Assert.True(thread.Join(TimeSpan.FromSeconds(40)), "Native UI test did not finish");
        if (failure != null) throw failure;
    }
    private static void Screenshot(Window window, string path)
    {
        var image = new RenderTargetBitmap(1280, 850, 96, 96, PixelFormats.Pbgra32); image.Render(window);
        var encoder = new PngBitmapEncoder(); encoder.Frames.Add(BitmapFrame.Create(image)); using var file = File.Create(path); encoder.Save(file);
    }
    private static IEnumerable<T> Descendants<T>(DependencyObject root) where T : DependencyObject
    {
        for (var i = 0; i < VisualTreeHelper.GetChildrenCount(root); i++) {
            var child = VisualTreeHelper.GetChild(root, i); if (child is T item) yield return item;
            foreach (var descendant in Descendants<T>(child)) yield return descendant;
        }
    }
    private static string NewDirectory() => Path.Combine(Path.GetTempPath(), "PaykiTodo-tests", Guid.NewGuid().ToString("N"));
    internal static long Millis(DateTime time) => new DateTimeOffset(time).ToUnixTimeMilliseconds();
    internal static DesktopItemEdit NewItem(bool isEvent, string title) => new(isEvent, title, "", isEvent ? "研讨室" : "", null,
        isEvent ? Millis(DateTime.Today.AddHours(23)) : null, isEvent ? Millis(DateTime.Today.AddDays(1)) : null,
        false, false, new[] { 15 }, 1, "#4CB782", false, "NONE", "", null);
}
