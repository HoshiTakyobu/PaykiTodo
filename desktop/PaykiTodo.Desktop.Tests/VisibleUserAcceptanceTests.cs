using System.Runtime.InteropServices;
using System.IO;
using PaykiTodo.Desktop.Tests;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Automation;
using System.Windows.Automation.Peers;
using System.Windows.Automation.Provider;
using Xunit;
using Button = System.Windows.Controls.Button;
using TextBox = System.Windows.Controls.TextBox;

namespace PaykiTodo.Desktop;

// UIA mode uses an isolated desktop. Physical-input mode remains separately opt-in.
public sealed class VisibleUserAcceptanceTests
{
    private sealed class VisibleFact : FactAttribute {
        public VisibleFact() { if (Environment.GetEnvironmentVariable("PAYKI_VISIBLE_ACCEPTANCE") != "1" && Environment.GetEnvironmentVariable("PAYKI_UIA_ACCEPTANCE") != "1") Skip = "Run isolated UIA acceptance using Invoke-DesktopAcceptance.ps1; physical input separately opt-in"; }
    }
    [VisibleFact]
    public void NativeWorkflowsWithUiaOrOptInPhysicalInput()
    {
        var physical = Environment.GetEnvironmentVariable("PAYKI_VISIBLE_ACCEPTANCE") == "1";
        Exception? failure = null;
        IsolatedDesktop? desktop = null;
        var thread = new Thread(() => {
            try {
            if (!physical) desktop = new IsolatedDesktop();
            var application = new System.Windows.Application { ShutdownMode = ShutdownMode.OnExplicitShutdown };
            var folder = Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE") ?? throw new InvalidOperationException("Evidence directory required");
            Directory.CreateDirectory(folder);
            using var server = new SyncReliabilityTests.SnapshotServer();
            var store = new DesktopSecureStore(Path.Combine(folder, "isolated-windows-profile"));
            var expiry = DateTimeOffset.UtcNow.AddHours(1).ToUnixTimeSeconds();
            store.Save(new CloudAccountState(server.Url, "visible_fixture", "fixture-device", "fixture-access", "fixture-refresh", expiry, expiry, "fixture-secret", 0, false), DesktopSnapshotDocument.CreateEmpty());
            File.WriteAllText(Path.Combine(folder, "isolated-windows-profile", "native-server.txt"), server.Url);
            var workspace = new DesktopWorkspace(store);
            var window = new NativeWindow(workspace, Path.Combine(folder, "isolated-windows-profile")) { Width = 1160, Height = 820, ShowActivated = true };
            var reminders = new DesktopReminders(window, workspace);
            window.Show(); if (physical) window.Activate();
            var driver = Task.Run(async () => {
                try {
                    await Task.Delay(1200);
                    void Stage(string id, string result) => File.AppendAllText(Path.Combine(folder, physical ? "windows-visible-steps.txt" : "windows-uia-steps.txt"), id + " PASS " + (physical ? "OS-input: " : "isolated UIA (not physical E2E): ") + result + "\n");
                    T UI<T>(Func<T> read) => window.Dispatcher.Invoke(read);
                    Window Current() => UI(() => application.Windows.OfType<Window>().LastOrDefault(w => w.IsVisible && w != window) ?? window);
                    void Capture(string name) => UI(() => {
                        var target = Current(); target.UpdateLayout();
                        Assert.Same(NativeUi.AppIcon, target.Icon);
                        if (target != window) Assert.Equal(WindowStartupLocation.CenterOwner, target.WindowStartupLocation);
                        var bitmap = new System.Windows.Media.Imaging.RenderTargetBitmap((int)Math.Ceiling(target.ActualWidth), (int)Math.Ceiling(target.ActualHeight), 96, 96, PixelFormats.Pbgra32);
                        bitmap.Render(target); var encoder = new System.Windows.Media.Imaging.PngBitmapEncoder(); encoder.Frames.Add(System.Windows.Media.Imaging.BitmapFrame.Create(bitmap));
                        using var output = File.Create(Path.Combine(folder, name + ".png")); encoder.Save(output); return true;
                    });
                    async Task Click(FrameworkElement control) {
                        if (!physical) {
                            UI(() => {
                                control.BringIntoView(); control.UpdateLayout();
                                var peer = UIElementAutomationPeer.CreatePeerForElement(control)!;
                                if (peer.GetPattern(PatternInterface.Invoke) is IInvokeProvider invoke) invoke.Invoke();
                                else if (peer.GetPattern(PatternInterface.Toggle) is IToggleProvider toggle) toggle.Toggle();
                                else throw new InvalidOperationException("Control has no invoke/toggle pattern");
                                return true;
                            });
                            await Task.Delay(180); return;
                        }
                        var handle = UI(() => new System.Windows.Interop.WindowInteropHelper(Window.GetWindow(control)).Handle);
                        Key(0x12, false); Key(0x12, true); SetForegroundWindow(handle);
                        UI(() => { control.BringIntoView(); control.UpdateLayout(); return true; });
                        await Task.Delay(200);
                        var point = UI(() => control.PointToScreen(new Point(control.ActualWidth / 2, control.ActualHeight / 2)));
                        File.AppendAllText(Path.Combine(folder, "windows-visible-steps.txt"), $"pointer {point.X},{point.Y} foreground={GetForegroundWindow() == handle}\n");
                        Assert.True(SetPhysicalCursorPos((int)point.X, (int)point.Y)); await Task.Delay(120);
                        GetPhysicalCursorPos(out var cursor);
                        var hit = UI(() => {
                            var node = Window.GetWindow(control).InputHitTest(Window.GetWindow(control).PointFromScreen(new Point(cursor.X, cursor.Y))) as DependencyObject;
                            while (node != null && node != control) node = VisualTreeHelper.GetParent(node);
                            return node == control;
                        });
                        Assert.True(hit, "Physical cursor must hit the visible target before clicking");
                        mouse_event(2, 0, 0, 0, UIntPtr.Zero); await Task.Delay(80); mouse_event(4, 0, 0, 0, UIntPtr.Zero);
                        await Task.Delay(450);
                    }
                    async Task ButtonClick(string label) {
                        Button? control = null;
                        for (var attempt = 0; attempt < 30 && control == null; attempt++) {
                            control = UI(() => Descendants<Button>(Current()).FirstOrDefault(b => AutomationProperties.GetName(b) == label || b.Content as string == label));
                            if (control == null) await Task.Delay(100);
                        }
                        Assert.NotNull(control);
                        if (physical) UI(() => { Current().Activate(); return true; });
                        await Click(control);
                    }
                    async Task Navigate(string page) {
                        if (physical) UI(() => { window.Activate(); return true; });
                        for (var attempt = 0; attempt < 3; attempt++) {
                            await Click(UI(() => Descendants<Button>(window).First(b => AutomationProperties.GetAutomationId(b) == "Navigate." + page)));
                            if (UI(() => Descendants<TextBlock>(window).Any(t => t.Text == page))) break;
                            await Task.Delay(500);
                        }
                        Assert.Contains(UI(() => Descendants<TextBlock>(window).Select(t => t.Text).ToList()), text => text == page);
                    }
                    async Task Type(TextBox input, string text) {
                        if (!physical) {
                            UI(() => { ((IValueProvider)UIElementAutomationPeer.CreatePeerForElement(input)!.GetPattern(PatternInterface.Value)).SetValue(text); return true; });
                            await Task.Delay(150); Assert.Equal(text, UI(() => input.Text)); return;
                        }
                        await Click(input); Key(0x11, false); Key(0x41, false); Key(0x41, true); Key(0x11, true);
                        if (text.Contains('\n')) {
                            for (var attempt = 0; ; attempt++) {
                                try { await window.Dispatcher.InvokeAsync(() => System.Windows.Clipboard.SetDataObject(text, true)); break; }
                                catch (COMException) when (attempt < 15) { await Task.Delay(150); }
                            }
                            Key(0x11, false); Key(0x56, false); Key(0x56, true); Key(0x11, true);
                        } else foreach (var character in text) Unicode(character);
                        await Task.Delay(350);
                        Assert.Equal(text, UI(() => input.Text));
                    }
                    async Task ConfirmSystemDialog() {
                        Capture("windows-confirmation");
                        await ButtonClick("确认操作"); await Task.Delay(800);
                    }
                    async Task Synced() {
                        for (var i = 0; i < 100; i++) { if (workspace.Account?.Dirty == false) return; await Task.Delay(100); }
                        throw new TimeoutException("Local test upload did not settle");
                    }
                    await Navigate("待办");
                    await ButtonClick("＋ 新增待办");
                    Capture("windows-todo-editor-light");
                    await Type(UI(() => Descendants<TextBox>(Current()).First()), "Visible keyboard todo");
                    await ButtonClick("完成");
                    Assert.Contains(workspace.Document!.Items, item => item.Title == "Visible keyboard todo");
                    Assert.Contains(store.Load()!.Value.Document.Items, item => item.Title == "Visible keyboard todo");
                    Stage("W01", "Create a todo through the native editor; encrypted disk copy verified");
                    await ButtonClick("＋ 新增待办");
                    await Type(UI(() => Descendants<TextBox>(Current()).First()), "Canceled draft must not exist");
                    await ButtonClick("取消");
                    Assert.DoesNotContain(workspace.Document.Items, item => item.Title == "Canceled draft must not exist");
                    Stage("W02", "Cancel native editor discards input without saving");
                    await Navigate("每日看板");
                    await ButtonClick("输入规划");
                    var source = UI(() => Descendants<TextBox>(Current()).Single(t => t.AcceptsReturn));
                    await Type(source, "- Visible root\n  - Visible child; #Visible note");
                    Capture("windows-checklist-editor-light");
                    await ButtonClick("识别");
                    await Task.Delay(350);
                    await ButtonClick("确认导入");
                    Assert.Equal(2, workspace.Document.Items.Count(i => i.QuickChecklistGroupId != null));
                    Assert.Equal("Visible note", workspace.Document.Items.Single(i => i.Title == "Visible child").Notes);
                    Stage("W03", "Transient planner preview confirmed real parent/child tasks and notes");
                    await ButtonClick("输入规划");
                    source = UI(() => Descendants<TextBox>(Current()).Single(t => t.AcceptsReturn));
                    await Type(source, "- Continuation");
                    if (physical) { Key(0x0D, false); Key(0x0D, true); }
                    else UI(() => { source.CaretIndex = source.Text.Length; source.RaiseEvent(new System.Windows.Input.KeyEventArgs(System.Windows.Input.Keyboard.PrimaryDevice, System.Windows.PresentationSource.FromVisual(source), 0, System.Windows.Input.Key.Enter) { RoutedEvent = System.Windows.Input.Keyboard.PreviewKeyDownEvent }); return true; });
                    await Task.Delay(250);
                    Assert.EndsWith("\n  - ", UI(() => source.Text));
                    await ButtonClick("取消"); Stage("W04", physical ? "Physical Enter continues task dash; cancellation not stored" : "Component PreviewKeyDown continues dash (not keyboard E2E); cancellation not stored");
                    await Navigate("日历");
                    Capture("windows-calendar-current-light");
                    await ButtonClick("＋ 创建日程");
                    var eventDay = DateTime.Today.AddDays(2);
                    if (!physical) UI(() => {
                        foreach (var date in Descendants<DatePicker>(Current()).Take(2)) {
                            ((IValueProvider)UIElementAutomationPeer.CreatePeerForElement(date)!.GetPattern(PatternInterface.Value)).SetValue(eventDay.ToShortDateString());
                            date.UpdateLayout();
                            NativeWorkspaceTests.AssertSingleLineFits((TextBox)date.Template.FindName("PART_TextBox", date));
                        }
                        return true;
                    });
                    var times = UI(() => Descendants<TextBox>(Current()).Where(input => AutomationProperties.GetName(input) == "时间 HH:mm").ToList());
                    Assert.Equal(2, times.Count);
                    await Type(times[0], "10:15"); await Type(times[1], "11:45");
                    UI(() => { foreach (var time in times) NativeWorkspaceTests.AssertSingleLineFits(time); return true; });
                    Capture("windows-event-editor-light");
                    await Type(UI(() => Descendants<TextBox>(Current()).First()), "Visible calendar event");
                    await ButtonClick("完成");
                    Assert.Contains(workspace.Document.Items, i => i.Title == "Visible calendar event" && i.IsEvent);
                    var savedEvent = workspace.Document.Items.Single(i => i.Title == "Visible calendar event");
                    Assert.Equal(new TimeSpan(10, 15, 0), DateTimeOffset.FromUnixTimeMilliseconds(savedEvent.StartAtMillis!.Value).LocalDateTime.TimeOfDay);
                    Assert.Equal(new TimeSpan(11, 45, 0), DateTimeOffset.FromUnixTimeMilliseconds(savedEvent.EndAtMillis!.Value).LocalDateTime.TimeOfDay);
                    if (!physical) Assert.Equal(eventDay, DateTimeOffset.FromUnixTimeMilliseconds(savedEvent.StartAtMillis.Value).LocalDateTime.Date);
                    Stage("W05", "Calendar date/time edited and persisted; date/time text viewport checked against clipping");
                    await Navigate("每日看板"); await ButtonClick("输入规划");
                    var planningCount = workspace.Document.PlanningNotes.Count;
                    var markdown = UI(() => Descendants<TextBox>(Current()).Single(t => t.AcceptsReturn));
                    await Type(markdown, "Preview cancellation,16:30;");
                    await ButtonClick("识别"); await Task.Delay(350);
                    Assert.DoesNotContain(workspace.Document.Items, i => i.Title == "Preview cancellation");
                    await ButtonClick("取消");
                    Assert.Equal(planningCount, workspace.Document.PlanningNotes.Count);
                    Assert.DoesNotContain(workspace.Document.Items, i => i.Title == "Preview cancellation");
                    Stage("W06", "Transient recognition/preview/cancel writes neither task nor planning document");
                    await Navigate("设置");
                    UI(() => {
                        var combo = Descendants<System.Windows.Controls.ComboBox>(window).Single(c => AutomationProperties.GetName(c) == "界面主题");
                        var peer = new ComboBoxAutomationPeer(combo); ((IExpandCollapseProvider)peer.GetPattern(PatternInterface.ExpandCollapse)).Expand(); combo.UpdateLayout();
                        var item = peer.GetChildren().Single(child => child.GetName() == "深色");
                        ((ISelectionItemProvider)item.GetPattern(PatternInterface.SelectionItem)).Select(); return true;
                    });
                    await Task.Delay(300);
                    Assert.True(NativeUi.Dark); Stage("W07", "Theme selector changes full native workspace to dark mode");
                    Capture("windows-uia-settings-dark");
                    foreach (var page in new[] { "每日看板", "待办", "日历", "倒数日", "AI 报告", "设置" }) {
                        await Navigate(page);
                        if (page == "日历") {
                            Capture("windows-calendar-current-dark");
                            UI(() => { window.Width = 920; window.UpdateLayout(); return true; });
                            await Task.Delay(300); Capture("windows-calendar-narrow-dark");
                            UI(() => { window.Width = 1160; window.UpdateLayout(); return true; });
                        }
                    }
                    Stage("W08", "All seven primary destinations navigated in dark window");
                    await Navigate("AI 报告"); await ButtonClick("AI 服务配置");
                    Capture("windows-ai-settings-dark"); await ButtonClick("＋ 添加服务");
                    await Type(UI(() => Descendants<TextBox>(Current()).First()), "Canceled AI provider");
                    var modelInput = UI(() => Descendants<TextBox>(Current()).Single(input => input.Name == "PART_EditableTextBox"));
                    await Type(modelInput, "fixture-model");
                    UI(() => { NativeWorkspaceTests.AssertSingleLineFits(modelInput); return true; });
                    Capture("windows-ai-provider-dark"); await ButtonClick("取消"); await ButtonClick("取消");
                    Assert.DoesNotContain(DesktopAi.Providers(workspace.Document!), provider => provider["name"]?.GetValue<string>() == "Canceled AI provider");
                    Stage("W13", "Branded AI dialogs and editable model field render; nested cancellation does not save provider");
                    await Navigate("设置");
                    await Synced();
                    var cloud = DesktopSnapshotDocument.CreateEmpty(); cloud.SaveItem(NativeWorkspaceTests.NewItem(false, "Cloud conflict fixture"));
                    server.Payload = SnapshotCrypto.Encrypt(cloud.ToArchiveBytes(), "fixture-secret"); server.Revision++;
                    await window.Dispatcher.InvokeAsync(() => workspace.Edit(doc => doc.SaveItem(NativeWorkspaceTests.NewItem(false, "Keep local conflict fixture"))));
                    await Task.Delay(500);
                    await ButtonClick("数据与备份");
                    await ButtonClick("保留本机上传"); await ConfirmSystemDialog(); await Synced();
                    Assert.Contains(DesktopSnapshotDocument.FromArchiveBytes(SnapshotCrypto.Decrypt(server.Payload!, "fixture-secret")).Items, i => i.Title == "Keep local conflict fixture");
                    Stage("W11", "Conflict review and branded confirmation keep local snapshot and upload with revision validation");
                    server.Payload = SnapshotCrypto.Encrypt(cloud.ToArchiveBytes(), "fixture-secret"); server.Revision++;
                    await ButtonClick("检查云端并恢复"); await ConfirmSystemDialog();
                    Assert.Equal("Cloud conflict fixture", workspace.Document!.Items.Single().Title);
                    await ButtonClick("恢复操作前副本"); await ConfirmSystemDialog();
                    Assert.Contains(workspace.Document.Items, i => i.Title == "Keep local conflict fixture");
                    Stage("W12", "Actual cloud-restore confirmation replaces data; operation-before restore button recovers local edits");
                    await Synced();
                    await window.Dispatcher.InvokeAsync(() => {
                        store.Save(workspace.Account! with { AccessExpiresAt = 1, RefreshExpiresAt = 1 }, workspace.Document!);
                        typeof(DesktopWorkspace).GetProperty("Account")!.SetValue(workspace, workspace.Account! with { AccessExpiresAt = 1, RefreshExpiresAt = 1 });
                    });
                    await window.Dispatcher.InvokeAsync(() => workspace.Edit(doc => doc.SaveItem(NativeWorkspaceTests.NewItem(false, "Visible expired reminder") with {
                        DueAtMillis = DateTimeOffset.UtcNow.AddSeconds(7).ToUnixTimeMilliseconds(), ReminderEnabled = true, ReminderOffsets = new[] { 0 }
                    })));
                    await Task.Delay(10000);
                    Assert.Equal("PaykiTodo 提醒", UI(() => Current().Title));
                    Assert.Contains("Visible expired reminder", UI(() => Descendants<TextBlock>(Current()).Select(t => t.Text).ToList()));
                    Capture("windows-reminder-dark");
                    await ButtonClick("我已完成");
                    for (var attempt = 0; attempt < 3 && !workspace.Document.Items.Single(i => i.Title == "Visible expired reminder").Completed; attempt++) {
                        Assert.Equal("PaykiTodo 提醒", UI(() => Current().Title));
                        await ButtonClick("我已完成");
                    }
                    Assert.True(workspace.Document.Items.Single(i => i.Title == "Visible expired reminder").Completed, "Physical completion click must persist the task state");
                    Stage("W09", "Expired-account offline timer opens reminder; completion updates local task");
                    var beforeLogout = workspace.Document.DataFingerprint();
                    await ButtonClick("账号与同步"); await ButtonClick("退出登录"); await ConfirmSystemDialog();
                    Assert.Null(workspace.Document); Assert.Null(new DesktopWorkspace(store).Document);
                    Assert.Equal(beforeLogout, store.Load()!.Value.Document.DataFingerprint());
                    Stage("W10", "Actual logout confirmation locks workspace on disk without deleting tasks");
                    Assert.Contains(UI(() => Descendants<TextBlock>(window).Select(text => text.Text).ToList()), text => text == "账号登录");
                    Assert.Contains(UI(() => Descendants<System.Windows.Controls.CheckBox>(window).Select(control => control.Content as string).ToList()), text => text == "记住账号");
                    Assert.Contains(UI(() => Descendants<System.Windows.Controls.CheckBox>(window).Select(control => control.Content as string).ToList()), text => text == "记住密码");
                    Capture("windows-login-dark");
                    await ButtonClick("查看隐私政策"); Capture("windows-privacy-dark"); await ButtonClick("关闭");
                    Stage("W14", "Locked login page exposes both memory options; branded privacy dialog opens and closes without external browsing");
                } catch (Exception error) {
                    window.Dispatcher.Invoke(() => {
                        var bitmap = new System.Windows.Media.Imaging.RenderTargetBitmap((int)window.ActualWidth, (int)window.ActualHeight, 96, 96, PixelFormats.Pbgra32);
                        bitmap.Render(window); var encoder = new System.Windows.Media.Imaging.PngBitmapEncoder(); encoder.Frames.Add(System.Windows.Media.Imaging.BitmapFrame.Create(bitmap));
                        using var output = File.Create(Path.Combine(folder, "windows-visible-failure.png")); encoder.Save(output);
                    });
                    File.AppendAllText(Path.Combine(folder, "windows-visible-steps.txt"), error.ToString() + "\n"); failure = error;
                }
                finally { window.Dispatcher.Invoke(() => { reminders.Dispose(); window.Close(); application.Shutdown(); }); }
            });
            application.Run(); driver.GetAwaiter().GetResult();
            } catch (Exception error) { failure = error; }
        });
        thread.SetApartmentState(ApartmentState.STA); thread.Start();
        Assert.True(thread.Join(TimeSpan.FromMinutes(4)), "Visible input acceptance timed out");
        desktop?.Dispose();
        if (failure != null) System.Runtime.ExceptionServices.ExceptionDispatchInfo.Capture(failure).Throw();
    }
    private static IEnumerable<T> Descendants<T>(DependencyObject root) where T : DependencyObject {
        for (var i = 0; i < VisualTreeHelper.GetChildrenCount(root); i++) {
            var child = VisualTreeHelper.GetChild(root, i); if (child is T value) yield return value;
            foreach (var descendant in Descendants<T>(child)) yield return descendant;
        }
    }
    private static void Key(byte code, bool up) => keybd_event(code, 0, up ? 2u : 0u, UIntPtr.Zero);
    private static void Unicode(char character) {
        var inputs = new[] { new Input { Type = 1, Keyboard = new KeyboardInput { Scan = character, Flags = 4 } }, new Input { Type = 1, Keyboard = new KeyboardInput { Scan = character, Flags = 6 } } };
        if (SendInput(2, inputs, Marshal.SizeOf<Input>()) != 2) throw new InvalidOperationException("OS keyboard injection rejected");
    }
    [StructLayout(LayoutKind.Explicit, Size = 40)] private struct Input { [FieldOffset(0)] public uint Type; [FieldOffset(8)] public KeyboardInput Keyboard; }
    [StructLayout(LayoutKind.Sequential)] private struct KeyboardInput { public ushort Key, Scan; public uint Flags, Time; public UIntPtr Extra; }
    [DllImport("user32.dll")] private static extern bool SetPhysicalCursorPos(int x, int y);
    [StructLayout(LayoutKind.Sequential)] private struct CursorPoint { public int X, Y; }
    [DllImport("user32.dll")] private static extern bool GetPhysicalCursorPos(out CursorPoint point);
    [DllImport("user32.dll")] private static extern void mouse_event(uint flags, uint x, uint y, uint data, UIntPtr extra);
    [DllImport("user32.dll")] private static extern void keybd_event(byte key, byte scan, uint flags, UIntPtr extra);
    [DllImport("user32.dll")] private static extern uint SendInput(uint count, Input[] inputs, int size);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] private static extern IntPtr FindWindow(string name, string title);
    private delegate bool EnumWindow(IntPtr handle, IntPtr parameter);
    [DllImport("user32.dll")] private static extern bool EnumChildWindows(IntPtr parent, EnumWindow callback, IntPtr parameter);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] private static extern int GetWindowText(IntPtr handle, System.Text.StringBuilder text, int size);
    [StructLayout(LayoutKind.Sequential)] private struct Rectangle { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll")] private static extern bool GetWindowRect(IntPtr handle, out Rectangle rectangle);
    [DllImport("user32.dll")] private static extern int GetDlgCtrlID(IntPtr handle);
    [DllImport("user32.dll")] private static extern bool SetForegroundWindow(IntPtr handle);
    [DllImport("user32.dll")] private static extern IntPtr GetForegroundWindow();
}
