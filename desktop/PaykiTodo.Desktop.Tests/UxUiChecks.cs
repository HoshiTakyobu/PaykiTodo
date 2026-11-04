using System.IO;
using System.Reflection;
using System.Text.Json;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Windows.Media;
using System.Windows.Threading;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

internal static class UxUiChecks
{
    private static readonly BindingFlags InstancePrivate = BindingFlags.Instance | BindingFlags.NonPublic;
    private static string Name(DependencyObject item) => System.Windows.Automation.AutomationProperties.GetName(item);
    private static void Navigate(NativeWindow window, string page) { typeof(NativeWindow).GetMethod("Navigate", InstancePrivate)!.Invoke(window, new object[] { page }); window.UpdateLayout(); Pump(); }
    private static void Click(Window dialog, string label) => UxRenderingTests.Descendants<Button>(dialog).Single(x => x.Content as string == label).RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
    private static void InDialog(Window owner, string title, Action open, Action<Window> inspect) {
        Exception? failure = null; var handled = false; var deadline = DateTime.UtcNow.AddSeconds(20);
        var timer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(50) };
        timer.Tick += (_, _) => {
            var dialog = Application.Current.Windows.Cast<Window>().FirstOrDefault(x => x != owner && x.Title == title);
            if (dialog == null && DateTime.UtcNow < deadline) return;
            timer.Stop(); handled = true;
            try { Assert.NotNull(dialog); dialog.UpdateLayout(); inspect(dialog!); }
            catch (Exception error) { failure = error; dialog?.Close(); foreach (var owned in owner.OwnedWindows.Cast<Window>().ToArray()) owned.Close(); }
        };
        timer.Start(); try { open(); } finally { timer.Stop(); }
        Assert.True(handled, "Expected modal did not open: " + title);
        if (failure != null) System.Runtime.ExceptionServices.ExceptionDispatchInfo.Capture(failure).Throw();
    }
    internal static T Await<T>(Task<T> task, int seconds = 15) {
        var deadline = DateTime.UtcNow.AddSeconds(seconds);
        while (!task.IsCompleted && DateTime.UtcNow < deadline) Pump();
        Assert.True(task.IsCompleted, "Reader operation timed out"); return task.GetAwaiter().GetResult();
    }
    internal static void Pump() {
        var frame = new DispatcherFrame(); var timer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(40) };
        timer.Tick += (_, _) => { timer.Stop(); frame.Continue = false; }; timer.Start(); Dispatcher.PushFrame(frame);
    }
    internal static string Evaluate(NativeContentReader reader, string script) => JsonSerializer.Deserialize<string>(Await(reader.Core!.ExecuteScriptAsync(script)))!;
    internal static void Ready(NativeContentReader reader, string expected) {
        var deadline = DateTime.UtcNow.AddSeconds(15);
        while (DateTime.UtcNow < deadline) {
            Pump();
            if (reader.Core != null && Evaluate(reader, "document.body.innerText").Contains(expected)) return;
        }
        Assert.Fail("Offline HTML reader did not render expected content: " + expected);
    }
    internal static void VerifyGuide(Window dialog, string folder) {
        var reader = UxRenderingTests.Descendants<NativeContentReader>(dialog).Single(); Ready(reader, "首次打开时选择登录服务器");
        var text = Evaluate(reader, "document.body.innerText");
        Assert.Contains("12—22 DIP", text); Assert.Contains("今日安排", text); Assert.Contains("托盘", text);
        Assert.Equal("true", Evaluate(reader, "String(document.querySelectorAll('h2').length > 4 && document.querySelector('#toc nav').children.length > 4)"));
        using var stream = File.Create(Path.Combine(folder, "windows-guide-html.png"));
        var capture = reader.Core!.CapturePreviewAsync(Microsoft.Web.WebView2.Core.CoreWebView2CapturePreviewImageFormat.Png, stream);
        Await(capture.ContinueWith(_ => true)); capture.GetAwaiter().GetResult();
    }
    internal static void VerifyCalendarAndLifecycle(NativeWindow window, DesktopWorkspace workspace, string folder) {
        void Navigate(string page) { typeof(NativeWindow).GetMethod("Navigate", BindingFlags.Instance | BindingFlags.NonPublic)!.Invoke(window, new object[] { page }); window.UpdateLayout(); }
        var before = workspace.Document!.ToArchiveBytes();
        var day = DateTime.Today;
        workspace.Edit(doc => {
            doc.ReplaceWith(DesktopSnapshotDocument.CreateEmpty());
            doc.SaveItem(NativeWorkspaceTests.NewItem(true, "【研究】小组会：阶段进度汇报、实验结果讨论以及下一阶段安排和负责人确认") with { StartAtMillis = NativeWorkspaceTests.Millis(day.AddHours(14)), EndAtMillis = NativeWorkspaceTests.Millis(day.AddHours(17)), Location = "实验楼 A 座 501 会议室" });
            doc.SaveItem(NativeWorkspaceTests.NewItem(true, "周外不得出现") with { StartAtMillis = NativeWorkspaceTests.Millis(day.AddDays(14)), EndAtMillis = NativeWorkspaceTests.Millis(day.AddDays(14).AddHours(1)) });
        });
        typeof(NativeWindow).GetField("selectedDay", BindingFlags.NonPublic | BindingFlags.Instance)!.SetValue(window, day);
        window.Width = 1920; Navigate("日历"); Pump(); window.UpdateLayout();
        var title = UxRenderingTests.Descendants<TextBlock>(window).Single(x => x.Text.StartsWith("【研究】"));
        File.WriteAllText(Path.Combine(folder, "ux-calendar-title-metrics.json"), JsonSerializer.Serialize(new { title.FontSize, title.LineHeight, title.MaxHeight, title.ActualHeight, title.ActualWidth, Family = title.FontFamily.Source, NativeUi.InterfaceFontSize }));
        Assert.Equal(TextWrapping.Wrap, title.TextWrapping); Assert.True(title.ActualHeight > title.LineHeight * 2.5); Assert.True(title.ActualHeight <= title.LineHeight * 3 + 1);
        Assert.Contains(UxRenderingTests.Descendants<TextBlock>(window), x => x.Text == "实验楼 A 座 501 会议室");
        UxRenderingTests.Capture(window, Path.Combine(folder, "ux-calendar-three-lines.png"));
        UxRenderingTests.Descendants<ComboBox>(window).Single(x => System.Windows.Automation.AutomationProperties.GetName(x) == "日历视图").SelectedItem = "列表";
        window.UpdateLayout();
        Assert.DoesNotContain(UxRenderingTests.Descendants<TextBlock>(window), x => x.Text == "周外不得出现");
        var selected = typeof(NativeWindow).GetField("selectedDay", BindingFlags.NonPublic | BindingFlags.Instance)!;
        var initial = (DateTime)selected.GetValue(window)!;
        UxRenderingTests.Descendants<Button>(window).Single(x => System.Windows.Automation.AutomationProperties.GetName(x) == "下一周").RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
        Assert.Equal(initial.AddDays(7), selected.GetValue(window));
        window.KeepRunningOnClose = true;
        for (var i = 0; i < 10; i++) { window.Close(); Assert.False(window.IsVisible); Assert.False(window.ExitRequested); window.RestoreMainWindow(); Assert.True(window.IsVisible); }
        Assert.Equal(initial.AddDays(7), selected.GetValue(window));
        window.WindowState = WindowState.Maximized; window.Close(); window.RestoreMainWindow(); Assert.Equal(WindowState.Maximized, window.WindowState);
        window.WindowState = WindowState.Normal; window.KeepRunningOnClose = false;
        workspace.Edit(doc => doc.ReplaceWith(DesktopSnapshotDocument.FromArchiveBytes(before)));
    }
    internal static void VerifyReaderFailure(Window owner, string folder) {
        var blocked = Path.Combine(folder, "reader-profile-is-a-file"); File.WriteAllText(blocked, "synthetic blocked directory");
        const string raw = "# 阅读器回退\n原文仍可选择并复制";
        var reader = new NativeContentReader(raw, blocked);
        var dialog = NativeUi.Dialog(owner, "阅读器失败验证", 650, 460); dialog.Content = reader;
        try {
            dialog.Show(); Pump();
            var deadline = DateTime.UtcNow.AddSeconds(20);
            while (!UxRenderingTests.Descendants<TextBox>(reader).Any(x => x.IsReadOnly && x.Text == raw) && DateTime.UtcNow < deadline) { Pump(); dialog.UpdateLayout(); }
            Assert.Contains(UxRenderingTests.Descendants<TextBox>(reader), x => x.IsReadOnly && x.Text == raw);
            Assert.Contains(UxRenderingTests.Descendants<TextBlock>(reader), x => x.Text.Contains("关闭后重试"));
            UxRenderingTests.Capture(dialog, Path.Combine(folder, "windows-reader-fallback.png"));
        } finally { dialog.Close(); reader.Dispose(); }
    }
    internal static void VerifyColorsAndStyle(NativeWindow window, DesktopWorkspace workspace, string folder) {
        var original = DesktopSnapshotDocument.FromArchiveBytes(workspace.Document!.ToArchiveBytes());
        try {
            foreach (var isEvent in new[] { false, true }) {
                var calendars = new[] { new DesktopCalendar("default", "常规日历", "#3370FF"), new DesktopCalendar("other", "选色测试日历", "#E9A23B") };
                var seed = NativeWorkspaceTests.NewItem(isEvent, isEvent ? "色块日程" : "色块待办") with { AccentColor = "#123ABC" };
                DesktopItemEdit? result = null;
                void Exercise(bool commit, DesktopItemEdit value) {
                    InDialog(window, "编辑" + (isEvent ? "日程" : "待办"), () => result = NativeEditors.Item(window, isEvent, workspace.Document!.Groups, value, DateTime.Today, calendars, preferences: workspace.Document), dialog => {
                        // Color is a first-class field, not part of the advanced expander.
                        dialog.UpdateLayout();
                        var control = UxRenderingTests.Descendants<ComboBox>(dialog).Single(x => Name(x) == "事项颜色");
                        control.BringIntoView(); Pump(); control.ApplyTemplate(); control.UpdateLayout();
                        Assert.Equal(value.AccentColor, control.SelectedItem);
                        Assert.Contains(UxRenderingTests.Descendants<Border>(control), x => x.Width == 28 && x.Height == 28 && x.Background is SolidColorBrush b && b.Color.ToString() == "#FF123ABC");
                        Assert.DoesNotContain(UxRenderingTests.Descendants<TextBlock>(control), x => x.Text.StartsWith('#'));
                        var arrow = Assert.IsType<System.Windows.Shapes.Path>(control.Template.FindName("arrow", control));
                        Assert.Equal(6, arrow.Height); Assert.Equal(10, arrow.Width); Assert.Equal(1.5, arrow.StrokeThickness);
                        var peer = System.Windows.Automation.Peers.UIElementAutomationPeer.CreatePeerForElement(control)!;
                        ((System.Windows.Automation.Provider.IExpandCollapseProvider)peer.GetPattern(System.Windows.Automation.Peers.PatternInterface.ExpandCollapse)).Expand(); Pump();
                        var popup = Assert.IsType<Popup>(control.Template.FindName("PART_Popup", control));
                        Assert.True(popup.IsOpen);
                        Assert.Equal(12, UxRenderingTests.Descendants<ComboBoxItem>(popup.Child).Count(x => x.Visibility == Visibility.Visible));
                        Assert.DoesNotContain(UxRenderingTests.Descendants<TextBlock>(popup.Child), x => x.Text.StartsWith('#'));
                        if (!commit) UxRenderingTests.Capture(dialog, Path.Combine(folder, isEvent ? "event-color-popup.png" : "todo-color-popup.png"));
                        control.IsDropDownOpen = false;
                        if (isEvent) {
                            UxRenderingTests.Descendants<ComboBox>(dialog).Single(x => Name(x) == "所属日历").SelectedIndex = 1;
                            Assert.Equal("#E9A23B", control.SelectedItem);
                        }
                        control.SelectedItem = "#17CFB5";
                        Click(dialog, commit ? "完成" : "取消");
                    });
                }
                Exercise(false, seed); Assert.Null(result); Assert.True(workspace.Document!.HasSamePersonalSyncContent(original));
                Exercise(true, seed); Assert.NotNull(result); Assert.Equal("#17CFB5", result!.AccentColor);
                long id = 0; workspace.Edit(doc => id = doc.SaveItem(result));
                Assert.Equal("#17CFB5", workspace.Document!.EditFor(id)!.AccentColor);
                InDialog(window, "编辑" + (isEvent ? "日程" : "待办"), () => result = NativeEditors.Item(window, isEvent, workspace.Document!.Groups, workspace.Document.EditFor(id), DateTime.Today, calendars), dialog => {
                    dialog.UpdateLayout();
                    Assert.Equal("#17CFB5", UxRenderingTests.Descendants<ComboBox>(dialog).Single(x => Name(x) == "事项颜色").SelectedItem); Click(dialog, "取消");
                });
                workspace.Edit(doc => doc.ReplaceWith(original));
            }
            Navigate(window, "日历");
            void Preferences(bool commit) {
                InDialog(window, "日历设置", () => typeof(NativeWindow).GetMethod("ShowCalendarPreferences", InstancePrivate)!.Invoke(window, null), dialog => {
                    var colors = UxRenderingTests.Descendants<ComboBox>(dialog).Single(x => Name(x) == "日程颜色样式");
                    var previewTitle = UxRenderingTests.Descendants<TextBlock>(dialog).Single(x => x.Text == "小组会：阶段进度与下一阶段安排");
                    DependencyObject parent = previewTitle; while (parent is not Border) parent = VisualTreeHelper.GetParent(parent);
                    var preview = (Border)parent; colors.SelectedIndex = 0; var modern = ((SolidColorBrush)preview.Background).Color;
                    colors.SelectedIndex = 1; var classic = ((SolidColorBrush)preview.Background).Color;
                    Assert.NotEqual(modern, classic); Assert.Equal((byte)255, classic.A);
                    UxRenderingTests.Capture(dialog, Path.Combine(folder, commit ? "calendar-style-classic.png" : "calendar-style-draft.png"));
                    Click(dialog, commit ? "完成" : "取消");
                });
            }
            Preferences(false); Assert.True(workspace.Document!.HasSamePersonalSyncContent(original));
            Preferences(true); Assert.True(workspace.Document!.ClassicCalendarColors);
            var restored = DesktopSnapshotDocument.FromArchiveBytes(workspace.Document.ToArchiveBytes());
            Assert.True(restored.ClassicCalendarColors); Assert.Equal("CLASSIC", restored.Settings["desktopCalendarEventStyle"]!.GetValue<string>());
        } finally { workspace.Edit(doc => doc.ReplaceWith(original)); }
    }
    internal static void VerifyWeeklyList(NativeWindow window, DesktopWorkspace workspace, string folder) {
        var original = DesktopSnapshotDocument.FromArchiveBytes(workspace.Document!.ToArchiveBytes());
        var selected = typeof(NativeWindow).GetField("selectedDay", InstancePrivate)!;
        var initial = (DateTime)selected.GetValue(window)!; var width = window.Width;
        var fixture = DesktopSnapshotDocument.CreateEmpty(); fixture.Settings["calendarDisplayTimeZoneId"] = "Asia/Shanghai"; fixture.Settings["weekStartMode"] = "MONDAY";
        long Add(string name, DateTime start, DateTime end, bool allDay = false) => fixture.SaveItem(NativeWorkspaceTests.NewItem(true, name) with {
            StartAtMillis = DesktopCalendarTime.Millis(start, "Asia/Shanghai"), EndAtMillis = DesktopCalendarTime.Millis(end, "Asia/Shanghai"), AllDay = allDay, EventTimeZoneId = "Asia/Shanghai" });
        var monday = new DateTime(2026, 11, 2);
        Add("周外前", monday.AddDays(-1).AddHours(10), monday.AddDays(-1).AddHours(11));
        Add("周一", monday.AddHours(10), monday.AddHours(11)); Add("周日", monday.AddDays(6).AddHours(10), monday.AddDays(6).AddHours(11));
        Add("周外后", monday.AddDays(7).AddHours(10), monday.AddDays(7).AddHours(11));
        Add("跨周延续", monday.AddHours(-1), monday.AddHours(1)); Add("午夜结束排除", monday.AddHours(-1), monday);
        Add("周日延续", monday.AddDays(6).AddHours(23), monday.AddDays(7).AddHours(1));
        Add("跨周全天", monday.AddDays(-1), monday.AddDays(8), true);
        var canceled = Add("已取消不出现", monday.AddHours(14), monday.AddHours(15)); fixture.Cancel(canceled);
        try {
            workspace.Edit(doc => doc.ReplaceWith(fixture)); selected.SetValue(window, monday.AddDays(1)); Navigate(window, "日历");
            UxRenderingTests.Descendants<ComboBox>(window).Single(x => Name(x) == "日历视图").SelectedItem = "列表"; window.UpdateLayout();
            var titles = UxRenderingTests.Descendants<TextBlock>(window).Select(x => x.Text).ToArray();
            foreach (var name in new[] { "周一", "周日", "跨周延续", "周日延续" }) Assert.Contains(name, titles);
            foreach (var name in new[] { "周外前", "周外后", "午夜结束排除", "已取消不出现" }) Assert.DoesNotContain(name, titles);
            Assert.Equal(7, titles.Count(x => x == "跨周全天")); Assert.Contains("2026年11月2日 – 2026年11月8日", titles);
            Assert.True(workspace.Document!.HasSamePersonalSyncContent(fixture)); UxRenderingTests.Capture(window, Path.Combine(folder, "weekly-list-boundaries.png"));
            window.Width = window.MinWidth; window.UpdateLayout(); Pump();
            var day = (DateTime)selected.GetValue(window)!; UxRenderingTests.Descendants<Button>(window).Single(x => Name(x) == "下一周").RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
            window.UpdateLayout(); Pump(); Assert.Equal(day.AddDays(7), selected.GetValue(window));
            UxRenderingTests.Descendants<Button>(window).Single(x => Name(x) == "上一周").RaiseEvent(new RoutedEventArgs(Button.ClickEvent)); window.UpdateLayout(); Pump(); Assert.Equal(day, selected.GetValue(window));
            workspace.Edit(doc => doc.Settings["weekStartMode"] = "SUNDAY"); window.UpdateLayout();
            Assert.Contains(UxRenderingTests.Descendants<TextBlock>(window), x => x.ToolTip as string == "2026年11月1日 – 2026年11月7日");
            selected.SetValue(window, new DateTime(2026, 12, 31)); Navigate(window, "日历");
            Assert.Contains(UxRenderingTests.Descendants<TextBlock>(window), x => x.ToolTip as string == "2026年12月27日 – 2027年1月2日");
            workspace.Edit(doc => { doc.Settings["weekStartMode"] = "MONDAY"; doc.Settings["calendarDisplayTimeZoneId"] = "America/New_York"; });
            selected.SetValue(window, new DateTime(2026, 11, 3)); Navigate(window, "日历");
            Assert.Contains(UxRenderingTests.Descendants<TextBlock>(window), x => x.ToolTip as string == "2026年11月2日 – 2026年11月8日");
            Assert.DoesNotContain(UxRenderingTests.Descendants<TextBlock>(window), x => x.Text == "跨周延续");
        } finally { window.Width = width; selected.SetValue(window, initial); workspace.Edit(doc => doc.ReplaceWith(original)); }
    }
    internal static void VerifyProductionTrayCallbacks(string folder) {
        var profile = Path.Combine(folder, "tray-profile");
        var store = new DesktopSecureStore(profile); store.SaveDocument(DesktopSnapshotDocument.CreateEmpty());
        var window = new NativeWindow(profileDirectory: profile) { ShowActivated = false, ShowInTaskbar = false, Width = 1280, Height = 850 };
        window.Show(); Pump();
        var workspace = (DesktopWorkspace)typeof(NativeWindow).GetField("workspace", InstancePrivate)!.GetValue(window)!;
        var reminders = Assert.IsType<DesktopReminders>(typeof(NativeWindow).GetField("reminders", InstancePrivate)!.GetValue(window));
        var tray = (System.Windows.Forms.NotifyIcon)typeof(DesktopReminders).GetField("tray", InstancePrivate)!.GetValue(reminders)!;
        var timer = (DispatcherTimer)typeof(DesktopReminders).GetField("timer", InstancePrivate)!.GetValue(reminders)!;
        var selected = typeof(NativeWindow).GetField("selectedDay", InstancePrivate)!; var chosen = DateTime.Today.AddDays(7);
        selected.SetValue(window, chosen); Navigate(window, "日历");
        var scroll = Assert.IsType<ScrollViewer>(typeof(NativeWindow).GetField("activeCalendarScroll", InstancePrivate)!.GetValue(window));
        scroll.ScrollToVerticalOffset(500); window.UpdateLayout(); Pump(); var offset = scroll.VerticalOffset;
        try {
            Assert.True(tray.Visible); Assert.True(timer.IsEnabled);
            for (var i = 0; i < 10; i++) {
                window.Close(); Assert.False(window.IsVisible); Assert.True(timer.IsEnabled);
                if (i % 2 == 0) tray.ContextMenuStrip!.Items[0].PerformClick();
                else typeof(System.Windows.Forms.NotifyIcon).GetMethod("OnDoubleClick", InstancePrivate)!.Invoke(tray, new[] { EventArgs.Empty });
                window.UpdateLayout(); Pump(); Assert.True(window.IsVisible); Assert.Equal(chosen, selected.GetValue(window));
                Assert.Equal(offset, scroll.VerticalOffset, 1);
                Assert.Same(reminders, typeof(NativeWindow).GetField("reminders", InstancePrivate)!.GetValue(window));
            }
            window.WindowState = WindowState.Minimized; window.Close(); tray.ContextMenuStrip!.Items[0].PerformClick(); Assert.Equal(WindowState.Normal, window.WindowState);
            window.WindowState = WindowState.Maximized; window.Close(); tray.ContextMenuStrip.Items[0].PerformClick(); Assert.Equal(WindowState.Maximized, window.WindowState);
            window.WindowState = WindowState.Normal;
            long id = 0; var trigger = DateTimeOffset.UtcNow.AddSeconds(2).ToUnixTimeMilliseconds();
            workspace.Edit(doc => id = doc.SaveItem(NativeWorkspaceTests.NewItem(false, "后台提醒独立验证") with { DueAtMillis = trigger, ReminderEnabled = true, ReminderOffsets = new[] { 0 } }));
            window.Close(); workspace.Edit(doc => doc.SaveItem(doc.EditFor(id)! with { Notes = "后台收到的新内容" }, id));
            var deadline = DateTime.UtcNow.AddSeconds(7); Window? alert = null;
            while (DateTime.UtcNow < deadline) {
                Pump(); alert = typeof(DesktopReminders).GetField("alert", InstancePrivate)!.GetValue(reminders) as Window;
                if (alert != null) break;
            }
            Assert.NotNull(alert); Assert.False(window.IsVisible);
            Assert.Contains(UxRenderingTests.Descendants<TextBlock>(alert!), x => x.Text == "后台收到的新内容");
            UxRenderingTests.Capture(alert!, Path.Combine(folder, "background-reminder.png"));
            InDialog(window, "待办详情", () => Click(alert!, "查看详情"), dialog => {
                Assert.True(window.IsVisible); Assert.Contains(UxRenderingTests.Descendants<TextBlock>(dialog), x => x.Text == "后台收到的新内容"); dialog.Close();
            });
            typeof(DesktopReminders).GetMethod("Tick", InstancePrivate)!.Invoke(reminders, null);
            Assert.Null(typeof(DesktopReminders).GetField("alert", InstancePrivate)!.GetValue(reminders));
            Assert.Equal("后台收到的新内容", new DesktopWorkspace(store).Document!.EditFor(id)!.Notes);
            tray.ContextMenuStrip.Items[1].PerformClick(); Assert.True(window.ExitRequested); Assert.False(window.IsVisible); Assert.False(timer.IsEnabled);
            Assert.Null(tray.Icon);
            File.WriteAllText(Path.Combine(folder, "tray-callbacks.txt"), "PASS: production NotifyIcon menu/double-click callbacks, ten hide/restore cycles, same window/service/page/date/scroll, minimized/maximized restore, real 2-second reminder while hidden, background local refresh, details, no replay, explicit menu exit stops timer/disposes icon. Independent desktop has no Explorer shell: no physical tray-pointer hit test or real server synchronization.");
        } finally { window.RequestExit(); }
    }
    internal static void VerifyReport(NativeWindow window, DesktopWorkspace workspace, string folder) {
        using var resource = typeof(UxUiChecks).Assembly.GetManifestResourceStream("PaykiTodo.Tests.Markdown")!;
        using var input = new StreamReader(resource); var raw = input.ReadToEnd();
        workspace.Edit(doc => doc.AddReport(raw, "离线语法测试", false));
        typeof(NativeWindow).GetMethod("Navigate", BindingFlags.NonPublic | BindingFlags.Instance)!.Invoke(window, new object[] { "AI 报告" }); window.UpdateLayout();
        Exception? failure = null;
        var timer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(100) };
        timer.Tick += (_, _) => {
            var dialog = Application.Current.Windows.Cast<Window>().FirstOrDefault(x => x != window && x.Title.StartsWith("每日回顾"));
            if (dialog == null) return; timer.Stop();
            try {
                var reader = UxRenderingTests.Descendants<NativeContentReader>(dialog).Single(); Ready(reader, "今日回顾");
                Assert.Equal("true", Evaluate(reader, "String(document.querySelectorAll('table').length===1 && document.querySelectorAll('h6').length===1 && document.querySelectorAll('pre').length>=1 && document.querySelectorAll('input:disabled').length===2)"));
                var dimensions = Evaluate(reader, "JSON.stringify({bad:[...document.querySelectorAll('img,iframe,script:not([src]),a[href^=javascript],a[href^=file]')].map(x=>x.outerHTML),width:document.documentElement.scrollWidth,inner:innerWidth,content:document.getElementById('content').outerHTML})");
                File.WriteAllText(Path.Combine(folder, "windows-report-dom.json"), dimensions);
                Assert.Equal("true", Evaluate(reader, "String(document.querySelectorAll('img,iframe,script:not([src]),a[href^=javascript],a[href^=file]').length===0)"));
                Assert.Equal("true", Evaluate(reader, "String(document.documentElement.scrollWidth<=document.documentElement.clientWidth+1)"));
                using var stream = File.Create(Path.Combine(folder, "windows-report-html.png"));
                Await(reader.Core!.CapturePreviewAsync(Microsoft.Web.WebView2.Core.CoreWebView2CapturePreviewImageFormat.Png, stream).ContinueWith(_ => true));
                Assert.Equal(raw, workspace.Document!.Reports.Last()["content"]!.GetValue<string>());
                workspace.Edit(doc => doc.Reports.Last()["generatedAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - 604_800_001);
                workspace.ExpireReports(); Assert.False(dialog.IsVisible);
            } catch (Exception error) { failure = error; dialog.Close(); }
        };
        timer.Start(); UxRenderingTests.Descendants<Button>(window).Last(x => x.Content as string == "阅读报告").RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
        timer.Stop(); if (failure != null) System.Runtime.ExceptionServices.ExceptionDispatchInfo.Capture(failure).Throw();
    }
}
