using System.IO;
using System.Reflection;
using System.Text.Json;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class LayoutRefinementTests
{
    private const BindingFlags Private = BindingFlags.Instance | BindingFlags.NonPublic;
    private static IEnumerable<T> Nodes<T>(DependencyObject root) where T : DependencyObject => UxRenderingTests.Descendants<T>(root);
    private static string Name(DependencyObject node) => System.Windows.Automation.AutomationProperties.GetName(node);
    private static string Id(DependencyObject node) => System.Windows.Automation.AutomationProperties.GetAutomationId(node);
    private static void Click(Window window, string caption) => Nodes<Button>(window).Single(button => Name(button) == caption).RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
    private static void Navigate(NativeWindow window, string page) {
        window.UpdateLayout();
        Nodes<Button>(window).Single(button => Id(button) == "Navigate." + page).RaiseEvent(new RoutedEventArgs(Button.ClickEvent));
        UxUiChecks.Pump(); window.UpdateLayout();
    }
    private static void Modal(Window owner, string caption, Action open, Action<Window> act) {
        Exception? failure = null; var handled = false;
        var timer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(80) };
        timer.Tick += (_, _) => {
            var dialog = owner.OwnedWindows.Cast<Window>().FirstOrDefault(w => w.Title == caption);
            if (dialog == null) return;
            timer.Stop(); handled = true;
            try { dialog.UpdateLayout(); act(dialog); }
            catch (Exception error) { failure = error; dialog.Close(); }
        };
        timer.Start(); try { open(); } finally { timer.Stop(); }
        Assert.True(handled, "Missing dialog: " + caption);
        if (failure != null) System.Runtime.ExceptionServices.ExceptionDispatchInfo.Capture(failure).Throw();
    }
    private static void CaptureElement(FrameworkElement element, string path) {
        element.UpdateLayout(); var dpi = VisualTreeHelper.GetDpi(element);
        var image = new RenderTargetBitmap((int)Math.Ceiling(element.ActualWidth * dpi.DpiScaleX), (int)Math.Ceiling(element.ActualHeight * dpi.DpiScaleY), dpi.PixelsPerInchX, dpi.PixelsPerInchY, PixelFormats.Pbgra32);
        image.Render(element); var png = new PngBitmapEncoder(); png.Frames.Add(BitmapFrame.Create(image)); using var file = File.Create(path); png.Save(file);
    }
    [IsolatedUiFact]
    public void WindowsLayoutAndEditorContracts()
    {
        Exception? failure = null;
        var thread = new Thread(() => {
            var steps = new List<string>(); var folder = Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE")!;
            Application? app = null; NativeWindow? window = null;
            try {
                using var desktop = new IsolatedDesktop(); app = new Application { ShutdownMode = ShutdownMode.OnExplicitShutdown };
                var profile = Path.Combine(folder, "layout-profile-" + Guid.NewGuid().ToString("N"));
                var store = new DesktopSecureStore(profile); var document = DesktopSnapshotDocument.CreateEmpty();
                document.Settings["calendarDisplayTimeZoneId"] = "Asia/Shanghai";
                var today = DateTime.Today;
                var todo = document.SaveItem(NativeWorkspaceTests.NewItem(false, "整理实验记录") with { DueAtMillis = NativeWorkspaceTests.Millis(today.AddHours(23)) });
                var eventId = document.SaveItem(NativeWorkspaceTests.NewItem(true, "阅读与讨论") with {
                    StartAtMillis = NativeWorkspaceTests.Millis(today.AddDays(1).AddHours(10)), EndAtMillis = NativeWorkspaceTests.Millis(today.AddDays(1).AddHours(11)), Location = "资料室", AccentColor = "#4C88FF" });
                document.AddReport("### 本期回顾\n\n今天完成了 **两项任务**，并整理了 `实验记录`。\n\n- 阅读相关工作\n- 记录下一步计划", "离线预览测试", false);
                store.SaveDocument(document); var workspace = new DesktopWorkspace(store);
                window = new NativeWindow(workspace, profile) { Width = 1280, Height = 900, ShowActivated = false };
                window.Show(); UxUiChecks.Pump(); window.UpdateLayout();
                var metrics = new List<object>();
                foreach (var dark in new[] { false, true }) {
                    workspace.Edit(d => d.Settings["themeMode"] = dark ? "DARK" : "LIGHT");
                    NativeUi.Dark = dark;
                    typeof(NativeWindow).GetMethod("BuildShell", Private)!.Invoke(window, null);
                    Navigate(window, "每日看板");
                    var cards = Nodes<Border>(window).Where(b => Id(b).StartsWith("Arrangement.")).ToArray();
                    Assert.True(cards.Length >= 1); Assert.All(cards, c => Assert.Equal(NativeUi.TextSize(84), c.Height));
                    Assert.All(cards, c => Assert.Equal(new Thickness(4, 0, 0, 0), c.BorderThickness));
                    Assert.DoesNotContain(Nodes<TextBlock>(window), t => t.Text == "按时间" || t.Text == "待办" || t.Text == "日程");
                    UxRenderingTests.Capture(window, Path.Combine(folder, $"layout-board-{dark}.png"));
                    Navigate(window, "日历");
                    Nodes<ComboBox>(window).Single(c => Name(c) == "日历视图").SelectedItem = "周"; window.UpdateLayout();
                    foreach (var width in new[] { 1280d, 1138d, window.MinWidth }) {
                        window.Width = width; UxUiChecks.Pump(); window.UpdateLayout();
                        var days = Nodes<Canvas>(window).Where(c => c.Tag is DateTime).ToArray(); Assert.Equal(7, days.Length);
                        Assert.All(days, day => Assert.True(day.ActualWidth >= NativeUi.TextSize(48) * 1.5, $"Day too narrow: {day.ActualWidth}"));
                        var create = Nodes<Button>(window).Single(b => Name(b) == "＋ 创建日程");
                        var bounds = create.TransformToAncestor(window).TransformBounds(new Rect(create.RenderSize));
                        Assert.True(bounds.Right <= window.ActualWidth && bounds.Left > 0);
                        var todayButton = Nodes<Button>(window).Single(b => Name(b) == "今天");
                        Assert.InRange(Math.Abs(todayButton.TranslatePoint(new Point(), window).Y - create.TranslatePoint(new Point(), window).Y), 0, 3);
                        metrics.Add(new { dark, window.ActualWidth, window.MinWidth, DayWidth = days[0].ActualWidth });
                        UxRenderingTests.Capture(window, Path.Combine(folder, $"layout-week-{dark}-{width:0}.png"));
                    }
                    Nodes<ComboBox>(window).Single(c => Name(c) == "日历视图").SelectedItem = "列表"; window.UpdateLayout();
                    Assert.Equal(7, Nodes<Grid>(window).Count(g => Id(g).StartsWith("Calendar.AgendaDay.")));
                    UxRenderingTests.Capture(window, Path.Combine(folder, $"layout-agenda-{dark}.png"));
                    Navigate(window, "设置");
                    Assert.Equal(0, Nodes<Grid>(window).Single(g => Id(g) == "Settings.Layout").ColumnDefinitions.Count);
                    UxRenderingTests.Capture(window, Path.Combine(folder, $"layout-settings-{dark}.png"));
                    Navigate(window, "AI 报告");
                    Assert.Contains(Nodes<TextBlock>(window), t => new System.Windows.Documents.TextRange(t.ContentStart, t.ContentEnd).Text.Contains("本期回顾"));
                    Assert.DoesNotContain(Nodes<TextBlock>(window), t => new System.Windows.Documents.TextRange(t.ContentStart, t.ContentEnd).Text.Contains("###"));
                    UxRenderingTests.Capture(window, Path.Combine(folder, $"layout-reports-{dark}.png"));
                    steps.Add("light/dark board, seven-day widths, single toolbar row, agenda, settings and Markdown: " + dark);
                    window.Width = 1280;
                    foreach (var isEvent in new[] { true, false }) {
                        var seed = NativeWorkspaceTests.NewItem(isEvent, "事务草稿") with { AccentColor = "#123ABC", EventTimeZoneId = "America/New_York" };
                        DesktopItemEdit? edit = null;
                        foreach (var commit in new[] { false, true }) {
                            Modal(window, "编辑" + (isEvent ? "日程" : "待办"), () => edit = NativeEditors.Item(window, isEvent, workspace.Document!.Groups, seed, today, preferences: workspace.Document), dialog => {
                                Assert.DoesNotContain(Nodes<ComboBox>(dialog), c => Name(c) == "日程时区");
                                var picker = Nodes<ComboBox>(dialog).Single(c => Name(c) == "事项颜色");
                                Assert.Equal("#123ABC", picker.SelectedItem);
                                Assert.DoesNotContain(Nodes<Expander>(dialog), e => Nodes<ComboBox>(e).Contains(picker));
                                var arrow = Assert.IsType<System.Windows.Shapes.Path>(picker.Template.FindName("arrow", picker));
                                Assert.Equal(10, arrow.Width); Assert.Equal(6, arrow.Height);
                                picker.BringIntoView(); UxUiChecks.Pump(); picker.IsDropDownOpen = true; UxUiChecks.Pump();
                                var popup = Assert.IsType<Popup>(picker.Template.FindName("PART_Popup", picker));
                                Assert.Equal(12, Nodes<ComboBoxItem>(popup.Child).Count(c => c.Visibility == Visibility.Visible));
                                Assert.Equal(6, Nodes<UniformGrid>(popup.Child).Single().Columns);
                                picker.SelectedItem = NativeColorChoice.Presets[6];
                                picker.IsDropDownOpen = true; UxUiChecks.Pump();
                                CaptureElement((FrameworkElement)popup.Child, Path.Combine(folder, $"layout-palette-{dark}-{isEvent}-{commit}.png"));
                                picker.IsDropDownOpen = false;
                                UxRenderingTests.Capture(dialog, Path.Combine(folder, $"layout-editor-{dark}-{isEvent}-{commit}.png"));
                                Click(dialog, commit ? "完成" : "取消");
                            });
                            if (!commit) Assert.Null(edit);
                            else { Assert.NotNull(edit); Assert.Equal(seed.EventTimeZoneId, isEvent ? edit!.EventTimeZoneId : seed.EventTimeZoneId); Assert.Equal(NativeColorChoice.Presets[6], edit!.AccentColor); Assert.Equal(seed.StartAtMillis, edit.StartAtMillis); }
                        }
                    }
                    steps.Add("editor cancel/save, preserved timezone, custom color and 2×6 palette: " + dark);
                    foreach (var id in new[] { todo, eventId }) {
                        var item = workspace.Document!.Items.Single(i => i.Id == id);
                        Modal(window, item.IsEvent ? "日程详情" : "待办详情", () => typeof(NativeWindow).GetMethod("Preview", Private)!.Invoke(window, new object[] { item }), dialog => {
                            Assert.DoesNotContain(Nodes<TextBlock>(dialog), t => t.Text == dialog.Title);
                            Assert.True(dialog.ActualHeight < 550, "Details should fit content");
                            Assert.Contains(Nodes<Button>(dialog), b => Name(b) == "更多");
                            UxRenderingTests.Capture(dialog, Path.Combine(folder, $"layout-detail-{dark}-{item.IsEvent}.png")); Click(dialog, "关闭");
                        });
                    }
                    steps.Add("content-sized details with compact actions: " + dark);
                }
                foreach (var size in new[] { 12, 18, 22 }) {
                    workspace.Edit(d => d.Settings["desktopFontSize"] = size); Navigate(window, "日历");
                    Nodes<ComboBox>(window).Single(c => Name(c) == "日历视图").SelectedItem = "周";
                    window.Width = window.MinWidth; UxUiChecks.Pump(); window.UpdateLayout();
                    Assert.Equal(7, Nodes<Canvas>(window).Count(c => c.Tag is DateTime));
                    UxRenderingTests.Capture(window, Path.Combine(folder, $"layout-font-{size}.png"));
                }
                steps.Add("font-size-aware minimum width: 12/18/22");
                File.WriteAllText(Path.Combine(folder, "layout-metrics.json"), JsonSerializer.Serialize(metrics, new JsonSerializerOptions { WriteIndented = true }));
            } catch (Exception error) { failure = error; }
            finally {
                File.WriteAllText(Path.Combine(folder, "layout-steps.json"), JsonSerializer.Serialize(new { steps, Error = failure?.ToString() }, new JsonSerializerOptions { WriteIndented = true }));
                window?.Close(); app?.Shutdown();
            }
        });
        thread.SetApartmentState(ApartmentState.STA); thread.Start(); Assert.True(thread.Join(TimeSpan.FromSeconds(120)));
        if (failure != null) System.Runtime.ExceptionServices.ExceptionDispatchInfo.Capture(failure).Throw();
    }
}
