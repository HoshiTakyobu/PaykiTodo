using System.Text.Json;
using System.Windows;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.Wpf;

namespace PaykiTodo.Desktop;

// Only the existing account verification challenge uses a browser. All work
// areas, data editing and navigation are native WPF controls.
internal static class NativeVerification
{
    public static async Task<(bool Canceled, string? Token)> GetTokenAsync(Window owner, string server)
    {
        var result = new TaskCompletionSource<(bool, string?)>();
        var window = NativeUi.Dialog(owner, "账号安全验证", 420, 320);
        var browser = new WebView2(); window.Content = browser;
        var challenge = server.TrimEnd('/') + "/v2/auth/challenge";
        window.Closed += (_, _) => { result.TrySetResult((true, null)); browser.Dispose(); };
        window.Loaded += async (_, _) => {
            try {
                var folder = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PaykiTodo", "account-verification");
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
}
