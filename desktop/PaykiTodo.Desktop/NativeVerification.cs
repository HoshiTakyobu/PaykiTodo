using System.Text.Json;
using System.Windows;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.Wpf;

namespace PaykiTodo.Desktop;

// Only the existing account verification challenge uses a browser. All work
// areas, data editing and navigation are native WPF controls.
internal static class NativeVerification
{
    public static async Task<(bool Canceled, string? Token)> GetTokenAsync(Window owner, string server, string profileDirectory)
    {
        if (!RuntimeAvailable() && !await InstallRuntimeAsync(owner)) return (true, null);
        var result = new TaskCompletionSource<(bool, string?)>();
        var window = NativeUi.Dialog(owner, "账号安全验证", 420, 320);
        var browser = new WebView2(); window.Content = browser;
        var challenge = server.TrimEnd('/') + "/v2/auth/challenge";
        window.Closed += (_, _) => { result.TrySetResult((true, null)); browser.Dispose(); };
        window.Loaded += async (_, _) => {
            try {
                var folder = Path.Combine(profileDirectory, "account-verification");
                var environment = await CoreWebView2Environment.CreateAsync(null, folder);
                await browser.EnsureCoreWebView2Async(environment);
                browser.CoreWebView2.Settings.AreDevToolsEnabled = false;
                browser.CoreWebView2.Settings.AreDefaultContextMenusEnabled = false;
                await browser.CoreWebView2.AddScriptToExecuteOnDocumentCreatedAsync("""
                    window.PaykiTodoTurnstile = {
                      onToken: token => window.chrome.webview.postMessage({type:'token', token}),
                      onUnavailable: () => window.chrome.webview.postMessage({type:'unavailable'}),
                      onError: () => window.chrome.webview.postMessage({type:'error'})
                    };
                    """);
                browser.CoreWebView2.WebMessageReceived += (_, args) => {
                    if (!args.Source.StartsWith(challenge, StringComparison.Ordinal)) return;
                    using var message = JsonDocument.Parse(args.WebMessageAsJson);
                    var type = message.RootElement.GetProperty("type").GetString();
                    if (type == "token" && message.RootElement.TryGetProperty("token", out var token) && !string.IsNullOrWhiteSpace(token.GetString())) {
                        result.TrySetResult((false, token.GetString())); window.Close();
                    } else if (type == "unavailable") { result.TrySetResult((false, null)); window.Close(); }
                };
                browser.CoreWebView2.NavigationStarting += (_, args) => {
                    if (!args.Uri.StartsWith(challenge, StringComparison.Ordinal)) args.Cancel = true;
                };
                browser.CoreWebView2.Navigate(challenge);
            } catch (Exception error) { result.TrySetException(new InvalidOperationException("无法加载账号验证，请确认网络和 WebView2 已安装。", error)); window.Close(); }
        };
        window.Show();
        return await result.Task;
    }

    internal static bool RuntimeAvailable()
    {
        try { return !string.IsNullOrEmpty(CoreWebView2Environment.GetAvailableBrowserVersionString()); }
        catch (WebView2RuntimeNotFoundException) { return false; }
    }

    private static Task<bool> InstallRuntimeAsync(Window owner)
    {
        var result = new TaskCompletionSource<bool>();
        var dialog = NativeUi.Dialog(owner, "安装登录组件", 480, 360);
        var status = NativeUi.Text("此电脑需要 Microsoft Edge WebView2 才能显示账号安全验证。安装需要联网，完成后会继续登录。", 14);
        var install = NativeUi.Action("安装登录组件", () => { }, true);
        var cancel = NativeUi.Action("暂不安装", () => dialog.Close()); cancel.IsCancel = true;
        install.Click += async (_, _) => {
            install.IsEnabled = false;
            status.Text = "正在安装 Microsoft Edge WebView2，请稍候…";
            try {
                var path = Path.Combine(AppContext.BaseDirectory, "MicrosoftEdgeWebview2Setup.exe");
                if (!File.Exists(path)) throw new InvalidOperationException("未找到组件安装器，请使用完整 Windows 安装包重新安装 PaykiTodo。");
                using var process = System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(path, "/silent /install") {
                    UseShellExecute = false, CreateNoWindow = true, WindowStyle = System.Diagnostics.ProcessWindowStyle.Hidden
                }) ?? throw new InvalidOperationException("无法启动组件安装器");
                await process.WaitForExitAsync();
                if (!RuntimeAvailable()) throw new InvalidOperationException("组件尚未安装成功，请检查网络或联系办公室 IT 后重试。");
                result.TrySetResult(true); dialog.Close();
            } catch (Exception error) { status.Text = error.Message; install.IsEnabled = true; }
        };
        dialog.Content = NativeUi.DialogLayout(NativeUi.Column(NativeUi.Text("完成一次安装即可登录", 20, bold: true), status), cancel, install);
        dialog.Closed += (_, _) => result.TrySetResult(false);
        dialog.Show(); return result.Task;
    }
}
