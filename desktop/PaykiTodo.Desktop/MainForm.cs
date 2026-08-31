using System.Diagnostics;
using System.Text.Json;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;

namespace PaykiTodo.Desktop;

internal sealed class MainForm : Form
{
    private readonly TextBox endpointBox = new();
    private readonly TextBox tokenBox = new();
    private readonly Button connectButton = new();
    private readonly Button disconnectButton = new();
    private readonly Label statusLabel = new();
    private readonly WebView2 browser = new();
    private readonly string preferencesPath;
    private string? pendingToken;

    public MainForm()
    {
        preferencesPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "PaykiTodo",
            "desktop-preferences.json"
        );

        Text = "PaykiTodo 电脑端";
        MinimumSize = new Size(960, 640);
        StartPosition = FormStartPosition.CenterScreen;

        BuildToolbar();
        BuildBrowser();
        LoadPreferences();
        Shown += async (_, _) => await InitializeBrowserAsync();
    }

    private void BuildToolbar()
    {
        var toolbar = new TableLayoutPanel
        {
            Dock = DockStyle.Top,
            Height = 58,
            ColumnCount = 5,
            RowCount = 1,
            Padding = new Padding(10, 8, 10, 8)
        };
        toolbar.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 58));
        toolbar.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
        toolbar.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 84));
        toolbar.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 120));
        toolbar.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 120));

        var endpointLabel = new Label
        {
            Text = "手机地址",
            Dock = DockStyle.Fill,
            TextAlign = ContentAlignment.MiddleLeft
        };
        endpointBox.Dock = DockStyle.Fill;
        endpointBox.PlaceholderText = "http://192.168.x.x:18765";
        endpointBox.Margin = new Padding(0, 4, 8, 4);

        var tokenLabel = new Label
        {
            Text = "访问密钥",
            Dock = DockStyle.Fill,
            TextAlign = ContentAlignment.MiddleLeft
        };
        tokenBox.Dock = DockStyle.Fill;
        tokenBox.MaxLength = 4;
        tokenBox.UseSystemPasswordChar = true;
        tokenBox.Margin = new Padding(0, 4, 8, 4);

        connectButton.Text = "连接手机";
        connectButton.Dock = DockStyle.Fill;
        connectButton.Click += async (_, _) => await ConnectAsync();
        disconnectButton.Text = "断开";
        disconnectButton.Dock = DockStyle.Fill;
        disconnectButton.Enabled = false;
        disconnectButton.Click += (_, _) => Disconnect();

        toolbar.Controls.Add(endpointLabel, 0, 0);
        toolbar.Controls.Add(endpointBox, 1, 0);
        toolbar.Controls.Add(tokenLabel, 2, 0);
        toolbar.Controls.Add(tokenBox, 3, 0);
        toolbar.Controls.Add(connectButton, 4, 0);
        Controls.Add(toolbar);

        var statusBar = new StatusStrip();
        statusLabel.Text = "请输入手机端电脑同步地址和访问密钥";
        statusBar.Items.Add(new ToolStripControlHost(statusLabel));
        Controls.Add(statusBar);
    }

    private void BuildBrowser()
    {
        browser.Dock = DockStyle.Fill;
        browser.CreationProperties = new CoreWebView2CreationProperties
        {
            UserDataFolder = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "PaykiTodo",
                "webview2"
            )
        };
        browser.NavigationCompleted += BrowserOnNavigationCompleted;
        browser.CoreWebView2InitializationCompleted += (_, args) =>
        {
            if (!args.IsSuccess)
            {
                SetStatus("WebView2 初始化失败：" + args.InitializationException?.Message);
            }
        };
        Controls.Add(browser);
    }

    private async Task InitializeBrowserAsync()
    {
        try
        {
            await browser.EnsureCoreWebView2Async();
            browser.CoreWebView2.Settings.AreDefaultContextMenusEnabled = true;
            browser.CoreWebView2.Settings.IsZoomControlEnabled = true;
            browser.CoreWebView2.Settings.AreDevToolsEnabled = Debugger.IsAttached;
        }
        catch (WebView2RuntimeNotFoundException)
        {
            SetStatus("未安装 Microsoft Edge WebView2 Runtime，请先安装后重试");
        }
        catch (Exception error)
        {
            SetStatus("电脑端初始化失败：" + error.Message);
        }
    }

    private async Task ConnectAsync()
    {
        var endpoint = NormalizeEndpoint(endpointBox.Text);
        var token = tokenBox.Text.Trim().ToUpperInvariant();
        if (endpoint is null)
        {
            SetStatus("请输入形如 http://192.168.0.100:18765 的手机地址");
            return;
        }
        if (token.Length == 0)
        {
            SetStatus("请输入手机端显示的访问密钥");
            return;
        }

        try
        {
            await browser.EnsureCoreWebView2Async();
            pendingToken = token;
            endpointBox.Text = endpoint;
            SavePreferences(endpoint);
            SetStatus("正在连接手机…");
            browser.CoreWebView2.Navigate(endpoint);
            connectButton.Enabled = false;
            disconnectButton.Enabled = true;
        }
        catch (Exception error)
        {
            SetStatus("连接失败：" + error.Message);
            connectButton.Enabled = true;
            disconnectButton.Enabled = false;
        }
    }

    private async void BrowserOnNavigationCompleted(object? sender, CoreWebView2NavigationCompletedEventArgs args)
    {
        if (!args.IsSuccess)
        {
            SetStatus("手机端页面加载失败：" + args.WebErrorStatus);
            connectButton.Enabled = true;
            disconnectButton.Enabled = false;
            return;
        }

        if (string.IsNullOrWhiteSpace(pendingToken)) return;
        var token = JsonSerializer.Serialize(pendingToken);
        pendingToken = null;
        try
        {
            await browser.ExecuteScriptAsync($"(() => {{ const input = document.getElementById('token'); const button = document.getElementById('connect'); if (!input || !button) return false; input.value = {token}; input.dispatchEvent(new Event('input', {{ bubbles: true }})); button.click(); return true; }})()");
            SetStatus("已打开手机端工作台");
        }
        catch (Exception error)
        {
            SetStatus("已打开手机页面，但自动填充访问密钥失败：" + error.Message);
        }
    }

    private void Disconnect()
    {
        pendingToken = null;
        browser.CoreWebView2?.Navigate("about:blank");
        connectButton.Enabled = true;
        disconnectButton.Enabled = false;
        SetStatus("已断开");
    }

    private void LoadPreferences()
    {
        try
        {
            if (!File.Exists(preferencesPath)) return;
            var preferences = JsonSerializer.Deserialize<DesktopPreferences>(File.ReadAllText(preferencesPath));
            endpointBox.Text = preferences?.Endpoint ?? string.Empty;
        }
        catch
        {
            // A damaged preference file should not prevent the desktop client from starting.
        }
    }

    private void SavePreferences(string endpoint)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(preferencesPath)!);
            File.WriteAllText(preferencesPath, JsonSerializer.Serialize(new DesktopPreferences(endpoint)));
        }
        catch
        {
            // The endpoint is a convenience; connection must still work if it cannot be persisted.
        }
    }

    private void SetStatus(string message)
    {
        if (InvokeRequired)
        {
            BeginInvoke(() => SetStatus(message));
            return;
        }
        statusLabel.Text = message;
    }

    private static string? NormalizeEndpoint(string value)
    {
        var endpoint = value.Trim();
        if (endpoint.Length == 0) return null;
        if (!endpoint.StartsWith("http://", StringComparison.OrdinalIgnoreCase) &&
            !endpoint.StartsWith("https://", StringComparison.OrdinalIgnoreCase))
        {
            endpoint = "http://" + endpoint;
        }
        if (!Uri.TryCreate(endpoint, UriKind.Absolute, out var uri) ||
            (uri.Scheme != Uri.UriSchemeHttp && uri.Scheme != Uri.UriSchemeHttps) ||
            string.IsNullOrWhiteSpace(uri.Host) || uri.Port <= 0)
        {
            return null;
        }
        return uri.ToString().TrimEnd('/');
    }

    private sealed record DesktopPreferences(string Endpoint);
}
