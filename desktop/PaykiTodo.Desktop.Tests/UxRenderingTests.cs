using System.IO;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text.Json;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class UxRenderingTests
{
    [DllImport("user32.dll")] private static extern IntPtr GetWindowDpiAwarenessContext(IntPtr window);
    [DllImport("user32.dll")] private static extern int GetAwarenessFromDpiAwarenessContext(IntPtr context);
    [IsolatedUiFact]
    public void CalendarFontAndRenderingEvidence()
    {
        Exception? failure = null;
        var thread = new Thread(() => {
            try {
                using var desktop = new IsolatedDesktop();
                var app = new Application();
                var folder = Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE")!;
                var store = new DesktopSecureStore(Path.Combine(folder, "font-profile"));
                var document = DesktopSnapshotDocument.CreateEmpty();
                document.SaveItem(NativeWorkspaceTests.NewItem(true, "【研究】小组会：阶段进度汇报、实验结果讨论以及下一阶段安排和负责人确认") with {
                    StartAtMillis = NativeWorkspaceTests.Millis(DateTime.Today.AddHours(14)), EndAtMillis = NativeWorkspaceTests.Millis(DateTime.Today.AddHours(17)),
                    Location = "实验楼 A 座 501 会议室", AccentColor = "#3370FF" });
                store.SaveDocument(document);
                var workspace = new DesktopWorkspace(store);
                var window = new NativeWindow(workspace, Path.Combine(folder, "font-profile")) { Width = 1920, Height = 1040, ShowActivated = false };
                window.Show();
                typeof(NativeWindow).GetMethod("Navigate", BindingFlags.NonPublic | BindingFlags.Instance)!.Invoke(window, new object[] { "日历" });
                var evidence = new List<object>();
                foreach (var name in new[] { "Microsoft YaHei UI", "FZXiaoBiaoSong-B05S" }) {
                    workspace.Edit(value => value.Settings["desktopFontFamily"] = name);
                  foreach (var size in new[] { 12, 14, 18, 22 }) {
                    workspace.Edit(value => value.Settings["desktopFontSize"] = size);
                    foreach (var mode in size == 14 ? new[] { TextRenderingMode.Auto, TextRenderingMode.ClearType, TextRenderingMode.Grayscale } : new[] { TextRenderingMode.Auto }) {
                        TextOptions.SetTextRenderingMode(window, mode);
                        typeof(NativeWindow).GetMethod("Render", BindingFlags.NonPublic | BindingFlags.Instance | BindingFlags.DeclaredOnly)!.Invoke(window, null);
                        window.UpdateLayout(); UxUiChecks.Pump(); window.UpdateLayout();
                        var dpi = VisualTreeHelper.GetDpi(window);
                        var face = new Typeface(NativeUi.InterfaceFont, FontStyles.Normal, FontWeights.Normal, FontStretches.Normal);
                        face.TryGetGlyphTypeface(out var glyph);
                        var labels = Descendants<TextBlock>(window).Where(x => x.Text == "14:00" || x.Text.StartsWith("【研究】")).Select(x => new {
                            x.Text, x.FontSize, Height = x.ActualHeight, x.MaxHeight, x.LineHeight, Font = x.FontFamily.Source, Weight = x.FontWeight.ToString()
                        }).ToArray();
                        evidence.Add(new { RequestedFont = name, RequestedSize = size, ResolvedFamily = NativeUi.InterfaceFont.Source,
                            GlyphFamily = glyph?.FamilyNames.Values.FirstOrDefault(), Simulations = glyph?.StyleSimulations.ToString(),
                            LineSpacing = NativeUi.InterfaceFont.LineSpacing, Rendering = mode.ToString(), DpiX = dpi.PixelsPerInchX,
                            Awareness = GetAwarenessFromDpiAwarenessContext(GetWindowDpiAwarenessContext(new WindowInteropHelper(window).Handle)), Labels = labels });
                        Capture(window, Path.Combine(folder, $"calendar-{name}-{size}-{mode}.png"));
                    }
                  }
                }
                NativeUi.SetInterfaceFontSize(14);
                File.WriteAllText(Path.Combine(folder, "font-rendering.json"), JsonSerializer.Serialize(evidence, new JsonSerializerOptions { WriteIndented = true, NumberHandling = System.Text.Json.Serialization.JsonNumberHandling.AllowNamedFloatingPointLiterals }));
                VerifyTitleHeights(window, folder);
                window.Close(); app.Shutdown();
            } catch (Exception error) { failure = error; }
        });
        thread.SetApartmentState(ApartmentState.STA); thread.Start(); Assert.True(thread.Join(TimeSpan.FromSeconds(50)));
        if (failure != null) System.Runtime.ExceptionServices.ExceptionDispatchInfo.Capture(failure).Throw();
    }
    private static void VerifyTitleHeights(Window window, string folder) {
        var panel = new StackPanel { Background = Brushes.White };
        var data = new List<object>();
        const string raw = "【研究】小组会：阶段进度汇报、实验结果讨论以及下一阶段安排和负责人确认";
        foreach (var family in new[] { "Microsoft YaHei UI", "FZXiaoBiaoSong-B05S" }) {
          NativeUi.SetInterfaceFont(family); var row = new StackPanel { Orientation = Orientation.Horizontal }; panel.Children.Add(row);
          foreach (var extra in new[] { 0.0, 1.0, 2.0, 4.0, 8.0 }) {
            var title = NativeUi.LimitedTitle(raw, 14, Brushes.Black); title.MaxHeight += extra;
            var column = new StackPanel { Width = 79, Margin = new Thickness(10) }; column.Children.Add(new TextBlock { Text = family + " extra " + extra }); column.Children.Add(title); row.Children.Add(column);
            data.Add(new { family, extra, title.LineHeight, title.MaxHeight });
          }
        }
        window.Content = panel; window.UpdateLayout(); Capture(window, Path.Combine(folder, "title-height-diagnostics.png"));
        File.WriteAllText(Path.Combine(folder, "title-height-diagnostics.json"), JsonSerializer.Serialize(data));
    }
    internal static IEnumerable<T> Descendants<T>(DependencyObject root) where T : DependencyObject {
        for (var i = 0; i < VisualTreeHelper.GetChildrenCount(root); i++) {
            var child = VisualTreeHelper.GetChild(root, i); if (child is T item) yield return item;
            foreach (var descendant in Descendants<T>(child)) yield return descendant;
        }
    }
    internal static void Capture(Window window, string path) {
        window.UpdateLayout(); window.Dispatcher.Invoke(() => { }, System.Windows.Threading.DispatcherPriority.Render);
        var dpi = VisualTreeHelper.GetDpi(window);
        var bitmap = new RenderTargetBitmap((int)Math.Ceiling(window.ActualWidth * dpi.DpiScaleX), (int)Math.Ceiling(window.ActualHeight * dpi.DpiScaleY), dpi.PixelsPerInchX, dpi.PixelsPerInchY, PixelFormats.Pbgra32);
        bitmap.Render(window); var encoder = new PngBitmapEncoder(); encoder.Frames.Add(BitmapFrame.Create(bitmap)); using var stream = File.Create(path); encoder.Save(stream);
    }
}
