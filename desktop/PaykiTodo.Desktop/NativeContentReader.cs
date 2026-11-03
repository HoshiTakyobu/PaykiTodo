using System.Text.Json;
using System.Windows;
using System.Windows.Controls;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.Wpf;
using static PaykiTodo.Desktop.NativeUi;

namespace PaykiTodo.Desktop;

internal sealed class NativeContentReader : Grid, IDisposable
{
    internal const string Origin = "https://payki.content";
    private readonly string profile;
    private readonly object payload;
    private readonly string raw;
    private WebView2? browser;
    private bool disposed;
    internal CoreWebView2? Core => browser?.CoreWebView2;
    internal Task? Initialization { get; private set; }
    internal NativeContentReader(string raw, string profile, bool help = false, string title = "PaykiTodo 阅读器") {
        this.raw = raw; this.profile = profile;
        payload = new { content = raw, dark = Dark, fontSize = TextSize(16), fontFamily = InterfaceFont.Source.Contains("Sarasa") ? "PaykiBody" : InterfaceFont.Source, help, title };
        Loaded += (_, _) => { if (Initialization == null) Initialization = Initialize(); };
        Unloaded += (_, _) => Dispose();
        Children.Add(Text("正在打开离线阅读器…", 15, Muted));
    }
    private async Task Initialize() {
        using var startupTimeout = new CancellationTokenSource(TimeSpan.FromSeconds(15));
        try {
            if (disposed) return;
            browser = new WebView2(); Children.Clear(); Children.Add(browser);
            var environment = await CoreWebView2Environment.CreateAsync(null, Path.Combine(profile, "content-reader")).WaitAsync(startupTimeout.Token);
            if (disposed) return;
            await browser.EnsureCoreWebView2Async(environment).WaitAsync(startupTimeout.Token);
            if (disposed) return;
            var core = browser.CoreWebView2;
            var ready = new TaskCompletionSource<bool>();
            core.Settings.AreDevToolsEnabled = false; core.Settings.AreDefaultContextMenusEnabled = true;
            core.Settings.AreHostObjectsAllowed = false; core.Settings.IsStatusBarEnabled = false;
            core.PermissionRequested += (_, e) => e.State = CoreWebView2PermissionState.Deny;
            core.DownloadStarting += (_, e) => e.Cancel = true;
            core.NewWindowRequested += (_, e) => { e.Handled = true; };
            core.AddWebResourceRequestedFilter("*", CoreWebView2WebResourceContext.All);
            core.WebResourceRequested += (_, e) => {
                var stream = LocalResource(e.Request.Uri);
                e.Response = environment.CreateWebResourceResponse(stream, stream == null ? 403 : 200, stream == null ? "Blocked" : "OK", "Content-Type: " + Mime(e.Request.Uri) + "\r\nCache-Control: no-store");
            };
            core.NavigationStarting += (_, e) => {
                if (e.Uri == Origin + "/content-viewer/index.html" || e.Uri.StartsWith(Origin + "/content-viewer/index.html#", StringComparison.Ordinal)) return;
                e.Cancel = true;
                if (e.IsUserInitiated && IsWebLink(e.Uri)) {
                    try { System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(e.Uri) { UseShellExecute = true }); }
                    catch (Exception error) when (error is System.ComponentModel.Win32Exception or InvalidOperationException) {
                        System.Windows.MessageBox.Show(System.Windows.Window.GetWindow(this), "无法打开浏览器。请复制链接后重试。", "PaykiTodo");
                    }
                }
            };
            core.NavigationCompleted += (_, e) => { if (!e.IsSuccess) ready.TrySetException(new InvalidOperationException("Offline reader navigation failed")); };
            core.WebMessageReceived += (_, e) => {
                if (disposed || e.Source != Origin + "/content-viewer/index.html") return;
                using var message = JsonDocument.Parse(e.WebMessageAsJson);
                if (message.RootElement.TryGetProperty("type", out var type) && type.GetString() == "ready") {
                    core.PostWebMessageAsJson(JsonSerializer.Serialize(payload)); ready.TrySetResult(true);
                }
            };
            core.Navigate(Origin + "/content-viewer/index.html");
            await ready.Task.WaitAsync(startupTimeout.Token);
        } catch (Exception) {
            if (disposed) return;
            browser?.Dispose(); browser = null; Children.Clear();
            var fallback = NativeUi.Input(raw, true); fallback.IsReadOnly = true;
            Children.Add(Column(Text("阅读器未能打开。可选择并复制原文，或关闭后重试。", 15, Muted), fallback));
        }
    }
    internal static bool IsWebLink(string value) => Uri.TryCreate(value, UriKind.Absolute, out var uri) && uri.Scheme is "https" or "http" && string.IsNullOrEmpty(uri.UserInfo);
    internal static Stream? LocalResource(string url) {
        if (!Uri.TryCreate(url, UriKind.Absolute, out var uri) || uri.GetLeftPart(UriPartial.Authority) != Origin || uri.Query.Length > 0) return null;
        var path = uri.AbsolutePath.TrimStart('/');
        if (path == "content-viewer/body-font.ttf") return System.Windows.Application.GetResourceStream(new Uri("pack://application:,,,/PaykiTodo;component/Assets/sarasa_ui_regular.ttf"))?.Stream;
        var assembly = typeof(NativeContentReader).Assembly;
        var key = assembly.GetManifestResourceNames().FirstOrDefault(name => name.Replace('\\', '/') == "PaykiTodo.Content/" + path);
        return key == null ? null : assembly.GetManifestResourceStream(key);
    }
    private static string Mime(string url) => Path.GetExtension(new Uri(url).AbsolutePath) switch {
        ".html" => "text/html; charset=utf-8", ".css" => "text/css; charset=utf-8", ".js" => "application/javascript; charset=utf-8", ".ttf" => "font/ttf", _ => "text/plain; charset=utf-8"
    };
    public void Dispose() { if (disposed) return; disposed = true; browser?.Dispose(); browser = null; Children.Clear(); }
}
