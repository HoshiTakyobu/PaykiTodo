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
    public void NewHistoricalRemindersNeverFireAndScheduledRemindersDoNotRepeat()
    {
        var document = DesktopSnapshotDocument.CreateEmpty();
        var now = Millis(DateTime.Now);
        var schedule = new DesktopReminderSchedule();
        document.SaveItem(NewItem(false, "刚录入的历史事项") with { DueAtMillis = now - 500, ReminderEnabled = true, ReminderOffsets = new[] { 0 } });
        var future = document.SaveItem(NewItem(false, "未来提醒") with { DueAtMillis = now + 500, ReminderEnabled = true, ReminderOffsets = new[] { 0 } });
        schedule.Update(document, now);
        Assert.Empty(schedule.TakeDue(now - 2000, now));
        Assert.Equal(future, Assert.Single(schedule.TakeDue(now, now + 600)).ItemId);
        schedule.Update(document, now + 700);
        Assert.Empty(schedule.TakeDue(now, now + 1000));
        var changed = document.SaveItem(NewItem(false, "已改期") with { DueAtMillis = now + 2000, ReminderEnabled = true, ReminderOffsets = new[] { 0 } });
        schedule.Update(document, now + 1000);
        document.SaveItem(document.EditFor(changed)! with { DueAtMillis = now + 1200 }, changed);
        schedule.Update(document, now + 1500);
        Assert.Empty(schedule.TakeDue(now + 1000, now + 2500));
    }
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

    [IsolatedUiFact]
    public void NativeScreensRenderAndNavigateWithoutWebView()
    {
        Exception? failure = null;
        IsolatedDesktop? desktop = null;
        var thread = new Thread(() => {
            try {
                desktop = new IsolatedDesktop();
                var app = new System.Windows.Application();
                var folder = NewDirectory(); var store = new DesktopSecureStore(folder); var document = DesktopSnapshotDocument.CreateEmpty();
                document.SaveItem(NewItem(false, "论文实验结果整理"));
                document.SaveItem(NewItem(false, "阅读相关工作"));
                document.SaveItem(NewItem(true, "讨论实验进展"));
                document.SaveItem(NewItem(true, "阅读小组") with { StartAtMillis = Millis(DateTime.Today.AddDays(1).AddHours(10)), EndAtMillis = Millis(DateTime.Today.AddDays(1).AddHours(11)) });
                document.CreateQuickChecklist(QuickChecklistParser.Parse("本周计划\n- 数据准备\n-- 检查样本\n--# 保留原始记录"));
                store.SaveDocument(document);
                var workspace = new DesktopWorkspace(store);
                var window = new NativeWindow(workspace, folder) { Width = 1280, Height = 850, ShowActivated = false,
                    WindowStartupLocation = WindowStartupLocation.Manual, Left = -10000, Top = -10000, ShowInTaskbar = false };
                window.Show();
                Assert.Same(NativeUi.AppIcon, window.Icon);
                Assert.True(((BitmapSource)window.Icon).PixelWidth >= 32, "Brand icon must have a high-resolution frame");
                window.Measure(new System.Windows.Size(1280, 850)); window.Arrange(new Rect(0, 0, 1280, 850)); window.UpdateLayout();
                var shotFolder = Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE") ?? folder;
                Directory.CreateDirectory(shotFolder);
                Screenshot(window, Path.Combine(shotFolder, "windows-board.png"));
                Assert.Contains(Descendants<TextBlock>(window), text => text.Text == "论文实验结果整理");
                var completion = Descendants<System.Windows.Controls.CheckBox>(window).Single(control =>
                    System.Windows.Automation.AutomationProperties.GetName(control) == "完成待办：论文实验结果整理");
                var completionPeer = System.Windows.Automation.Peers.UIElementAutomationPeer.CreatePeerForElement(completion)!;
                ((System.Windows.Automation.Provider.IToggleProvider)completionPeer.GetPattern(System.Windows.Automation.Peers.PatternInterface.Toggle)).Toggle();
                window.UpdateLayout();
                Assert.True(workspace.Document!.Items.Single(item => item.Title == "论文实验结果整理").Completed);
                Assert.True(new DesktopWorkspace(store).Document!.Items.Single(item => item.Title == "论文实验结果整理").Completed);
                Assert.DoesNotContain(Descendants<TextBlock>(window), text => text.Text == "论文实验结果整理");
                workspace.Edit(target => target.Restore(target.Items.Single(item => item.Title == "论文实验结果整理").Id));
                window.UpdateLayout();
                var typeface = new Typeface(NativeUi.BundledFont, FontStyles.Normal, FontWeights.Medium, FontStretches.Normal);
                Assert.True(typeface.TryGetGlyphTypeface(out var glyphs), "Bundled sans-serif must resolve without a system font");
                Assert.Contains("sarasa_ui_semibold.ttf", glyphs.FontUri.ToString().ToLowerInvariant());
                Assert.True(new Typeface(NativeUi.HeadingFont, FontStyles.Normal, FontWeights.Normal, FontStretches.Normal).TryGetGlyphTypeface(out var headingGlyphs));
                Assert.Contains("payki_greeting.ttf", headingGlyphs.FontUri.ToString().ToLowerInvariant());
                var searchInput = Descendants<TextBox>(window).Single(input => System.Windows.Automation.AutomationProperties.GetName(input) == "搜索事项");
                var searchHint = Descendants<TextBlock>(window).Single(text => text.Text == "搜索待办、日程…");
                Assert.Equal(Visibility.Visible, searchHint.Visibility);
                searchInput.Text = "论文实验";
                Assert.Equal(Visibility.Collapsed, searchHint.Visibility);
                Descendants<Button>(window).Single(button => System.Windows.Automation.AutomationProperties.GetName(button) == "执行搜索").RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
                window.UpdateLayout();
                Assert.Contains(Descendants<TextBlock>(window), text => text.Text == "论文实验结果整理");
                Assert.DoesNotContain(Descendants<TextBlock>(window), text => text.Text == "阅读相关工作");
                searchInput.Clear();
                Assert.Equal(Visibility.Visible, searchHint.Visibility);
                Assert.DoesNotContain(Descendants<TextBlock>(window), text => text.Text == "账号与同步");
                Assert.DoesNotContain(Descendants<System.Windows.DependencyObject>(window), control => control.GetType().Name.Contains("WebView"));
                foreach (var page in new[] { "待办", "日历", "规划台", "倒数日", "AI 报告", "设置" }) {
                    var button = Descendants<Button>(window).First(button => System.Windows.Automation.AutomationProperties.GetAutomationId(button) == "Navigate." + page);
                    button.RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
                    window.UpdateLayout();
                    if (page == "待办") {
                        var group = Descendants<ComboBox>(window).Single(control => control.ItemsSource is IEnumerable<DesktopGroup>);
                        group.ApplyTemplate(); group.UpdateLayout();
                        var labels = Descendants<TextBlock>(group).Select(text => text.Text).ToArray();
                        Assert.Contains("全部分组", labels);
                        Assert.DoesNotContain(labels, label => label.Contains("DesktopGroup"));
                        Screenshot(window, Path.Combine(shotFolder, "windows-tasks.png"));
                    }
                    if (page == "日历") {
                        var timeline = Descendants<Canvas>(window).Where(canvas => canvas.Tag is DateTime).ToList();
                        Assert.Equal(7, timeline.Count);
                        var axis = Descendants<Canvas>(window).Single(canvas => Descendants<TextBlock>(canvas).Any(text => text.Text == "01:00"));
                        var hourText = Descendants<TextBlock>(axis).Single(text => text.Text == "01:00");
                        var hourLine = timeline[0].Children.OfType<Border>().Single(line => line.Height == 1 && Canvas.GetTop(line) == 60);
                        Assert.Equal(Canvas.GetTop(hourLine), Canvas.GetTop(hourText) + hourText.Height / 2);
                        var zone = Descendants<TextBlock>(window).Single(text => text.Text.StartsWith("GMT"));
                        Assert.Equal(TextWrapping.NoWrap, zone.TextWrapping);
                        Assert.True(zone.ActualWidth > 0);
                        var todayButton = Descendants<Button>(window).Single(button => System.Windows.Automation.AutomationProperties.GetAutomationId(button) == "Calendar.Date." + DateTime.Today.ToString("yyyy-MM-dd"));
                        Assert.Equal(1, ((Border)todayButton.Content).BorderThickness.Left);
                        Screenshot(window, Path.Combine(shotFolder, "windows-calendar.png"));
                        VerifyFeedbackControls(window, workspace, store, shotFolder);
                        VerifyCalendarReaudit(window, workspace, shotFolder);
                    }
                }
                var toggle = Descendants<ComboBox>(window).First(control => System.Windows.Automation.AutomationProperties.GetName(control) == "界面主题");
                toggle.SelectedIndex = 1; window.UpdateLayout();
                Assert.True(NativeUi.Dark);
                Screenshot(window, Path.Combine(shotFolder, "windows-settings-dark.png"));
                var planning = Descendants<Button>(window).First(button => System.Windows.Automation.AutomationProperties.GetAutomationId(button) == "Navigate.规划台");
                planning.RaiseEvent(new RoutedEventArgs(Button.ClickEvent)); window.UpdateLayout();
                Descendants<Button>(window).First(button => button.Content as string == "＋ 新建文档").RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
                window.UpdateLayout();
                var draftEditor = Descendants<TextBox>(window).Single(input => input.AcceptsReturn);
                draftEditor.Text = "跨导航保留的规划草稿";
                workspace.Notify("后台同步状态更新"); window.UpdateLayout();
                Assert.Same(draftEditor, Descendants<TextBox>(window).Single(input => input.AcceptsReturn));
                var todos = Descendants<Button>(window).First(button => System.Windows.Automation.AutomationProperties.GetAutomationId(button) == "Navigate.待办");
                todos.RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
                planning.RaiseEvent(new RoutedEventArgs(Button.ClickEvent)); window.UpdateLayout();
                Descendants<Button>(window).First(button => button.Content as string == "继续新文档草稿").RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
                window.UpdateLayout();
                Assert.Equal("跨导航保留的规划草稿", Descendants<TextBox>(window).Single(input => input.AcceptsReturn).Text);
                window.Width = 920; window.UpdateLayout();
                var planningSave = Descendants<Button>(window).Single(button => button.Content as string == "完成");
                var saveBounds = planningSave.TransformToAncestor(window).TransformBounds(new Rect(planningSave.RenderSize));
                Assert.True(saveBounds.Bottom < window.ActualHeight - 24, "Planning completion must stay inside the viewport without scrolling");
                Screenshot(window, Path.Combine(shotFolder, "windows-planning-narrow-dark.png"));
                window.Width = 1280;
                var board = Descendants<Button>(window).First(button => System.Windows.Automation.AutomationProperties.GetAutomationId(button) == "Navigate.每日看板");
                board.RaiseEvent(new RoutedEventArgs(Button.ClickEvent)); window.UpdateLayout();
                Screenshot(window, Path.Combine(shotFolder, "windows-board-dark.png"));
                Descendants<Button>(window).First(button => System.Windows.Automation.AutomationProperties.GetAutomationId(button) == "Navigate.设置").RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
                window.UpdateLayout();
                Descendants<ComboBox>(window).First(control => System.Windows.Automation.AutomationProperties.GetName(control) == "界面主题").SelectedIndex = 0;
                var formTimer = new System.Windows.Threading.DispatcherTimer { Interval = TimeSpan.FromMilliseconds(100) };
                formTimer.Tick += (_, _) => {
                    var editor = app.Windows.OfType<Window>().FirstOrDefault(candidate => candidate != window && candidate.Title == "新增待办");
                    if (editor == null) return;
                    formTimer.Stop();
                    Assert.Same(window.Icon, editor.Icon);
                    foreach (var input in Descendants<TextBox>(editor).Where(input => !input.AcceptsReturn && input.Height == 42)) AssertSingleLineFits(input);
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
        desktop?.Dispose();
        if (failure != null) System.Runtime.ExceptionServices.ExceptionDispatchInfo.Capture(failure).Throw();
    }
    private static void Screenshot(Window window, string path)
    {
        var dpi = VisualTreeHelper.GetDpi(window);
        var image = new RenderTargetBitmap((int)Math.Ceiling(window.ActualWidth * dpi.DpiScaleX), (int)Math.Ceiling(window.ActualHeight * dpi.DpiScaleY), dpi.PixelsPerInchX, dpi.PixelsPerInchY, PixelFormats.Pbgra32); image.Render(window);
        var encoder = new PngBitmapEncoder(); encoder.Frames.Add(BitmapFrame.Create(image)); using var file = File.Create(path); encoder.Save(file);
    }

    private static void VerifyFeedbackControls(NativeWindow window, DesktopWorkspace workspace, DesktopSecureStore store, string folder)
    {
        void Click(string label) => Descendants<Button>(window).First(button => button.Content as string == label).RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
        void Navigate(string label) {
            Descendants<Button>(window).Single(button => System.Windows.Automation.AutomationProperties.GetAutomationId(button) == "Navigate." + label).RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
            window.UpdateLayout();
        }
        void Private(string name, params object[] args) => typeof(NativeWindow).GetMethod(name, System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!.Invoke(window, args);
        void Modal(string title, Action open, Action<Window> inspect) {
            Exception? error = null;
            var timer = new System.Windows.Threading.DispatcherTimer { Interval = TimeSpan.FromMilliseconds(80) };
            timer.Tick += (_, _) => {
                var dialog = System.Windows.Application.Current.Windows.OfType<Window>().FirstOrDefault(candidate => candidate.Title == title && candidate != window);
                if (dialog == null) return;
                timer.Stop();
                try { dialog.UpdateLayout(); inspect(dialog); } catch (Exception failure) { error = failure; dialog.Close(); }
            };
            timer.Start(); open(); timer.Stop();
            if (error != null) throw error;
            window.UpdateLayout();
        }
        void DialogClick(Window dialog, string label) => Descendants<Button>(dialog).Single(button => button.Content as string == label).RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
        void Pump() {
            var frame = new System.Windows.Threading.DispatcherFrame();
            var timer = new System.Windows.Threading.DispatcherTimer { Interval = TimeSpan.FromMilliseconds(60) };
            timer.Tick += (_, _) => { timer.Stop(); frame.Continue = false; };
            timer.Start(); System.Windows.Threading.Dispatcher.PushFrame(frame); window.UpdateLayout();
        }
        ScrollViewer TimelineScroll() => Descendants<ScrollViewer>(window).Single(scroll => scroll.Content is Grid grid && Descendants<Canvas>(grid).Any(canvas => canvas.Tag is DateTime));
        Pump();
        TimelineScroll().ScrollToVerticalOffset(300); Pump();
        Assert.Equal(300, TimelineScroll().VerticalOffset, 1);

        var count = workspace.Document!.Items.Count;
        var canvas = Descendants<Canvas>(window).First(control => control.Tag is DateTime);
        canvas.RaiseEvent(new System.Windows.Input.MouseButtonEventArgs(System.Windows.Input.Mouse.PrimaryDevice, 0, System.Windows.Input.MouseButton.Left) { RoutedEvent = UIElement.MouseLeftButtonUpEvent });
        Assert.Single(Descendants<Button>(window).Where(button => System.Windows.Automation.AutomationProperties.GetAutomationId(button) == "Calendar.Draft"));
        Assert.Empty(window.OwnedWindows.Cast<Window>());
        var seed = ((DateTime)canvas.Tag).AddHours(9).AddMinutes(15);
        Private("ShowCalendarDraft", canvas, seed);
        var draft = Descendants<Button>(window).Single(button => System.Windows.Automation.AutomationProperties.GetAutomationId(button) == "Calendar.Draft");
        Assert.Equal(24, draft.Height);
        Modal("新增日程", () => draft.RaiseEvent(new RoutedEventArgs(Button.ClickEvent)), dialog => DialogClick(dialog, "取消"));
        Assert.Equal(count, workspace.Document.Items.Count);
        Modal("新增日历", () => Private("EditCalendar", new object[] { null! }), dialog => {
            Descendants<TextBox>(dialog).Single(input => System.Windows.Automation.AutomationProperties.GetName(input) == "日历名称").Text = "课程表";
            Descendants<ComboBox>(dialog).Single().SelectedItem = "#7955CC";
            DialogClick(dialog, "完成");
        });
        var calendar = workspace.Document.Calendars.Single(item => item.Name == "课程表");
        canvas = Descendants<Canvas>(window).First(control => control.Tag is DateTime);
        Private("ShowCalendarDraft", canvas, seed);
        draft = Descendants<Button>(window).Single(button => System.Windows.Automation.AutomationProperties.GetAutomationId(button) == "Calendar.Draft");
        Modal("新增日程", () => draft.RaiseEvent(new RoutedEventArgs(Button.ClickEvent)), dialog => {
            Descendants<TextBox>(dialog).First().Text = "课程归属验收";
            var choice = Descendants<ComboBox>(dialog).Single(box => box.ItemsSource is IEnumerable<DesktopCalendar>);
            choice.SelectedItem = ((IEnumerable<DesktopCalendar>)choice.ItemsSource).Single(item => item.Id == calendar.Id);
            DialogClick(dialog, "完成");
        });
        var saved = new DesktopWorkspace(store).Document!.Items.Single(item => item.Title == "课程归属验收");
        Assert.Equal(calendar.Id, saved.CalendarId); Assert.Equal(Millis(seed), saved.StartAtMillis); Assert.Equal(Millis(seed.AddMinutes(30)), saved.EndAtMillis);
        var toggle = Descendants<System.Windows.Controls.CheckBox>(window).Single(box => System.Windows.Automation.AutomationProperties.GetAutomationId(box) == "Calendar.Visible." + calendar.Id);
        toggle.IsChecked = false; window.UpdateLayout();
        Assert.DoesNotContain(Descendants<TextBlock>(window), text => text.Text == "课程归属验收");
        Assert.Contains(calendar.Id, workspace.Document.Settings["hiddenCalendarIds"]!.ToJsonString());
        toggle = Descendants<System.Windows.Controls.CheckBox>(window).Single(box => System.Windows.Automation.AutomationProperties.GetAutomationId(box) == "Calendar.Visible." + calendar.Id);
        toggle.IsChecked = true; window.UpdateLayout();
        Pump(); Assert.Equal(300, TimelineScroll().VerticalOffset, 1);
        Assert.Contains(Descendants<TextBlock>(window), text => text.Text == "课程归属验收");
        Screenshot(window, Path.Combine(folder, "windows-calendar-ownership.png"));

        Navigate("设置");
        Modal("选择主字体", () => Click("选择字体"), dialog => {
            Descendants<ComboBox>(dialog).Single().SelectedItem = "Segoe UI"; DialogClick(dialog, "取消");
        });
        Assert.Null(workspace.Document.Settings["desktopFontFamily"]);
        Modal("选择主字体", () => Click("选择字体"), dialog => {
            Descendants<ComboBox>(dialog).Single().SelectedItem = "Segoe UI"; DialogClick(dialog, "完成");
        });
        Assert.Equal("Segoe UI", new DesktopWorkspace(store).Document!.Settings["desktopFontFamily"]!.GetValue<string>());
        Modal("选择主字体", () => Click("选择字体"), dialog => {
            Descendants<ComboBox>(dialog).Single().SelectedItem = "系统推荐"; DialogClick(dialog, "完成");
        });
        Click("数据与备份"); window.UpdateLayout(); Screenshot(window, Path.Combine(folder, "windows-backup-settings.png"));
        Click("常规");

        long memorial = 0;
        workspace.Edit(document => memorial = document.SaveItem(NewItem(false, "完成纪念验收") with { CountdownEnabled = true, DueAtMillis = Millis(DateTime.Today.AddDays(-8)) }));
        Modal("继续保留这个日子？", () => { workspace.Edit(document => document.Complete(memorial)); Private("ReviewCountdowns"); }, dialog => {
            Screenshot(dialog, Path.Combine(folder, "windows-countdown-choice.png")); DialogClick(dialog, "保留纪念");
        });
        Assert.Equal("KEEP", new DesktopWorkspace(store).Document!.Items.Single(item => item.Id == memorial).CountdownPastMode);
        workspace.Edit(document => {
            document.SaveItem(NewItem(false, "每日循环验收") with { DueAtMillis = Millis(DateTime.Today.AddDays(1).AddHours(10)), RecurrenceType = "DAILY", RecurrenceEndEpochDay = (long)(DateTime.Today.AddDays(4) - DateTime.UnixEpoch.Date).TotalDays });
            document.EnsureRecurringInstances(DateTime.Today);
        });
        Navigate("待办");
        var series = Descendants<Button>(window).Single(button => System.Windows.Automation.AutomationProperties.GetAutomationId(button).StartsWith("Todo.Series."));
        var foldedCount = Descendants<TextBlock>(window).Count(text => text.Text == "每日循环验收");
        series.RaiseEvent(new RoutedEventArgs(Button.ClickEvent)); window.UpdateLayout();
        Assert.True(Descendants<TextBlock>(window).Count(text => text.Text == "每日循环验收") > foldedCount);
        Screenshot(window, Path.Combine(folder, "windows-recurring-expanded.png"));
        Navigate("日历");
    }
    private static void VerifyCalendarReaudit(NativeWindow window, DesktopWorkspace workspace, string folder)
    {
        void Pump() {
            var frame = new System.Windows.Threading.DispatcherFrame();
            var timer = new System.Windows.Threading.DispatcherTimer { Interval = TimeSpan.FromMilliseconds(80) };
            timer.Tick += (_, _) => { timer.Stop(); frame.Continue = false; }; timer.Start();
            System.Windows.Threading.Dispatcher.PushFrame(frame); window.UpdateLayout();
        }
        void Render() { typeof(NativeWindow).GetMethod("Render", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.DeclaredOnly)!.Invoke(window, null); window.UpdateLayout(); }
        List<Canvas> Days() => Descendants<Canvas>(window).Where(c => c.Tag is DateTime).ToList();
        foreach (var width in new[] { 900d, 1100d, 1450d }) {
            window.Width = width; Pump();
            Assert.Equal(width < 1100 ? 3 : 7, Days().Count);
            foreach (var button in Descendants<Button>(window).Where(b => b.Content as string == "＋ 创建日程" || b.Content as string == "今天")) {
                var point = button.TranslatePoint(new Point(), window);
                Assert.InRange(point.X, 0, window.ActualWidth - button.ActualWidth + 1);
            }
            Screenshot(window, Path.Combine(folder, $"calendar-width-{width}.png"));
        }
        var before = Days().Select(c => (DateTime)c.Tag).ToArray();
        Descendants<Button>(window).Single(b => System.Windows.Automation.AutomationProperties.GetAutomationId(b) == "Calendar.Mini.Next").RaiseEvent(new RoutedEventArgs(Button.ClickEvent)); Pump();
        Assert.Equal(before, Days().Select(c => (DateTime)c.Tag));
        Descendants<Button>(window).Single(b => System.Windows.Automation.AutomationProperties.GetAutomationId(b) == "Calendar.Mini.Previous").RaiseEvent(new RoutedEventArgs(Button.ClickEvent)); Pump();
        var ids = new List<string>(); var busyIds = new List<long>(); var start = before[0].AddDays(21);
        workspace.Edit(doc => {
            for (var i = 0; i < 24; i++) ids.Add(doc.SaveCalendar(null, "附加课程表 " + (i + 1), "#7955CC"));
            for (var i = 0; i < 210; i++) {
                var at = start.AddDays(i % 7).AddHours(8).AddMinutes(i / 7 * 20);
                busyIds.Add(doc.SaveItem(NewItem(true, "繁忙周 " + i) with { StartAtMillis = Millis(at), EndAtMillis = Millis(at.AddMinutes(45)) }));
            }
        }); Pump();
        var side = Descendants<ScrollViewer>(window).Single(s => System.Windows.Automation.AutomationProperties.GetAutomationId(s) == "Calendar.SidebarScroll");
        Assert.True(side.ScrollableHeight > 0); side.ScrollToEnd(); Pump();
        var last = Descendants<System.Windows.Controls.CheckBox>(side).Single(c => System.Windows.Automation.AutomationProperties.GetAutomationId(c) == "Calendar.Visible." + ids.Last());
        Assert.InRange(last.TranslatePoint(new Point(), side).Y, 0, side.ViewportHeight);
        typeof(NativeWindow).GetField("selectedDay", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!.SetValue(window, start);
        var times = new List<double>();
        for (var i = 0; i < 5; i++) { var clock = System.Diagnostics.Stopwatch.StartNew(); Render(); clock.Stop(); times.Add(clock.Elapsed.TotalMilliseconds); }
        Pump(); Screenshot(window, Path.Combine(folder, "calendar-busy-210-events.png"));
        Assert.Equal(210, Descendants<Border>(window).Count(tile => System.Windows.Automation.AutomationProperties.GetAutomationId(tile).StartsWith("Calendar.Event.")));
        Assert.All(times, ms => Assert.True(ms < 3000, "Busy week blocked UI for more than 3 seconds"));
        File.WriteAllText(Path.Combine(folder, "calendar-render-timings.json"), System.Text.Json.JsonSerializer.Serialize(new { Events = 210, Calendars = workspace.Document!.Calendars.Count, RenderAndLayoutMilliseconds = times }));
        workspace.Edit(doc => { foreach (var id in ids) doc.DeleteCalendar(id); foreach (var id in busyIds) doc.Delete(id); }); Pump();
        Descendants<Button>(window).Single(b => b.Content as string == "今天").RaiseEvent(new RoutedEventArgs(Button.ClickEvent)); Pump();
        window.Width = 1280; Pump();
    }
    internal static void AssertSingleLineFits(TextBox input)
    {
        input.ApplyTemplate(); input.UpdateLayout();
        var host = Assert.IsType<ScrollViewer>(input.Template.FindName("PART_ContentHost", input));
        Assert.True(host.ViewportHeight >= input.FontSize * 1.4, $"Input '{input.Text}' has only {host.ViewportHeight}px of visible height");
        Assert.True(host.ExtentHeight <= host.ViewportHeight + 1, $"Input '{input.Text}' is vertically clipped: {host.ExtentHeight} > {host.ViewportHeight}");
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
