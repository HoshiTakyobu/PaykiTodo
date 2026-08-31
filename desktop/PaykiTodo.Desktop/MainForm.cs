using System.Diagnostics;
using System.Text;
using System.Text.Json;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;

namespace PaykiTodo.Desktop;

internal sealed class MainForm : Form
{
    private readonly ComboBox modeBox = new();
    private readonly TextBox endpointBox = new();
    private readonly TextBox tokenBox = new();
    private readonly TextBox serverBox = new();
    private readonly TextBox deviceBox = new();
    private readonly TextBox secretBox = new();
    private readonly Label statusLabel = new();
    private readonly Label endpointLabel = new();
    private readonly Label tokenLabel = new();
    private readonly Label serverLabel = new();
    private readonly Label deviceLabel = new();
    private readonly Label secretLabel = new();
    private readonly Button connectButton = new();
    private readonly Button disconnectButton = new();
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
        MinimumSize = new Size(1080, 720);
        StartPosition = FormStartPosition.CenterScreen;

        BuildToolbar();
        BuildBrowser();
        LoadPreferences();
        UpdateModeFields();
        Shown += async (_, _) => await InitializeBrowserAsync();
    }

    private void BuildToolbar()
    {
        var toolbar = new TableLayoutPanel
        {
            Dock = DockStyle.Top,
            Height = 116,
            ColumnCount = 8,
            RowCount = 2,
            Padding = new Padding(10, 8, 10, 8)
        };
        var widths = new[] { 70, 180, 70, 190, 70, 180, 100, 100 };
        foreach (var width in widths)
            toolbar.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, width));
        toolbar.RowStyles.Add(new RowStyle(SizeType.Percent, 50));
        toolbar.RowStyles.Add(new RowStyle(SizeType.Percent, 50));

        var modeLabel = CreateLabel("连接模式");
        modeBox.Items.AddRange(new object[] { "手机 LAN", "服务器快照（只读）" });
        modeBox.SelectedIndex = 0;
        modeBox.Dock = DockStyle.Fill;
        modeBox.DropDownStyle = ComboBoxStyle.DropDownList;
        modeBox.SelectedIndexChanged += (_, _) => UpdateModeFields();

        endpointLabel.Text = "手机地址";
        endpointBox.PlaceholderText = "http://192.168.x.x:18765";
        tokenLabel.Text = "访问密钥";
        tokenBox.MaxLength = 4;
        tokenBox.UseSystemPasswordChar = true;

        serverLabel.Text = "服务器 URL";
        serverBox.PlaceholderText = "https://example.com/paykitodo-sync";
        deviceLabel.Text = "设备 ID";
        deviceBox.PlaceholderText = "手机端远程同步设备 ID";
        secretLabel.Text = "同步密钥";
        secretBox.UseSystemPasswordChar = true;

        foreach (var control in new Control[]
        {
            modeBox, endpointBox, tokenBox, serverBox, deviceBox, secretBox,
            endpointLabel, tokenLabel, serverLabel, deviceLabel, secretLabel
        })
        {
            control.Dock = DockStyle.Fill;
            control.Margin = new Padding(0, 3, 8, 3);
        }

        connectButton.Text = "连接手机";
        connectButton.Dock = DockStyle.Fill;
        connectButton.Click += async (_, _) => await ConnectAsync();
        disconnectButton.Text = "断开";
        disconnectButton.Dock = DockStyle.Fill;
        disconnectButton.Enabled = false;
        disconnectButton.Click += (_, _) => Disconnect();

        toolbar.Controls.Add(modeLabel, 0, 0);
        toolbar.Controls.Add(modeBox, 1, 0);
        toolbar.Controls.Add(endpointLabel, 2, 0);
        toolbar.Controls.Add(endpointBox, 3, 0);
        toolbar.Controls.Add(tokenLabel, 4, 0);
        toolbar.Controls.Add(tokenBox, 5, 0);
        toolbar.Controls.Add(connectButton, 6, 0);
        toolbar.Controls.Add(disconnectButton, 7, 0);

        toolbar.Controls.Add(serverLabel, 0, 1);
        toolbar.Controls.Add(serverBox, 1, 1);
        toolbar.SetColumnSpan(serverBox, 2);
        toolbar.Controls.Add(deviceLabel, 3, 1);
        toolbar.Controls.Add(deviceBox, 4, 1);
        toolbar.SetColumnSpan(deviceBox, 2);
        toolbar.Controls.Add(secretLabel, 6, 1);
        toolbar.Controls.Add(secretBox, 7, 1);
        Controls.Add(toolbar);

        var statusBar = new StatusStrip();
        statusLabel.Text = "请选择连接模式";
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
                SetStatus("WebView2 初始化失败：" + args.InitializationException?.Message);
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
        if (modeBox.SelectedIndex == 1)
        {
            await ConnectServerSnapshotAsync();
            return;
        }

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
            SavePreferences(endpoint, serverBox.Text, deviceBox.Text);
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

    private async Task ConnectServerSnapshotAsync()
    {
        var serverUrl = NormalizeServerUrl(serverBox.Text);
        var deviceId = deviceBox.Text.Trim();
        var secret = secretBox.Text.Trim();
        if (serverUrl is null)
        {
            SetStatus("服务器模式必须填写 HTTPS 地址");
            return;
        }
        if (deviceId.Length == 0 || secret.Length == 0)
        {
            SetStatus("服务器模式需要设备 ID 和远程同步密钥");
            return;
        }

        try
        {
            connectButton.Enabled = false;
            disconnectButton.Enabled = false;
            SetStatus("正在下载并解密服务器快照…");
            var snapshot = await ServerSnapshotReader.DownloadAsync(serverUrl, deviceId, secret);
            SavePreferences(endpointBox.Text, serverUrl, deviceId);
            await browser.EnsureCoreWebView2Async();
            browser.NavigateToString(RenderServerSnapshot(snapshot));
            connectButton.Enabled = true;
            disconnectButton.Enabled = true;
            SetStatus("已加载服务器快照（只读，不会修改手机数据）");
        }
        catch (Exception error)
        {
            connectButton.Enabled = true;
            disconnectButton.Enabled = false;
            SetStatus("服务器快照读取失败：" + error.Message);
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
            serverBox.Text = preferences?.ServerUrl ?? string.Empty;
            deviceBox.Text = preferences?.DeviceId ?? string.Empty;
        }
        catch
        {
            // A damaged preference file should not prevent the desktop client from starting.
        }
    }

    private void SavePreferences(string endpoint, string serverUrl, string deviceId)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(preferencesPath)!);
            File.WriteAllText(
                preferencesPath,
                JsonSerializer.Serialize(new DesktopPreferences(endpoint, serverUrl, deviceId))
            );
        }
        catch
        {
            // Connection must still work if convenience preferences cannot be persisted.
        }
    }

    private void UpdateModeFields()
    {
        var phoneMode = modeBox.SelectedIndex == 0;
        endpointBox.Enabled = phoneMode;
        tokenBox.Enabled = phoneMode;
        serverBox.Enabled = !phoneMode;
        deviceBox.Enabled = !phoneMode;
        secretBox.Enabled = !phoneMode;
        connectButton.Text = phoneMode ? "连接手机" : "读取快照";
        if (phoneMode)
            SetStatus("请输入手机端电脑同步地址和访问密钥");
        else
            SetStatus("服务器模式只读展示，不会修改手机数据");
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

    private static Label CreateLabel(string text)
    {
        return new Label { Text = text, Dock = DockStyle.Fill, TextAlign = ContentAlignment.MiddleLeft };
    }

    private static string? NormalizeEndpoint(string value)
    {
        var endpoint = value.Trim();
        if (endpoint.Length == 0) return null;
        if (!endpoint.StartsWith("http://", StringComparison.OrdinalIgnoreCase) &&
            !endpoint.StartsWith("https://", StringComparison.OrdinalIgnoreCase))
            endpoint = "http://" + endpoint;
        if (!Uri.TryCreate(endpoint, UriKind.Absolute, out var uri) ||
            (uri.Scheme != Uri.UriSchemeHttp && uri.Scheme != Uri.UriSchemeHttps) ||
            string.IsNullOrWhiteSpace(uri.Host) || uri.Port <= 0)
            return null;
        return uri.ToString().TrimEnd('/');
    }

    private static string? NormalizeServerUrl(string value)
    {
        var endpoint = value.Trim();
        if (!Uri.TryCreate(endpoint, UriKind.Absolute, out var uri) ||
            uri.Scheme != Uri.UriSchemeHttps || string.IsNullOrWhiteSpace(uri.Host))
            return null;
        return uri.ToString().TrimEnd('/');
    }

    private static string RenderServerSnapshot(ServerSnapshotData snapshot)
    {
        var now = DateTime.Now;
        var active = snapshot.Items.Where(item => !item.Completed && !item.Canceled).ToList();
        var todayTodos = active.Where(item => !item.IsEvent && (!item.DueAt.HasValue || item.DueAt.Value.Date == now.Date)).OrderBy(item => item.DueAt).ToList();
        var todayEvents = active.Where(item => item.IsEvent && item.StartAt?.Date == now.Date).OrderBy(item => item.StartAt).ToList();
        var tomorrowEvents = active.Where(item => item.IsEvent && item.StartAt?.Date == now.Date.AddDays(1)).OrderBy(item => item.StartAt).ToList();
        var todos = active.Where(item => !item.IsEvent).OrderBy(item => item.DueAt ?? DateTime.MaxValue).ThenBy(item => item.Title).ToList();
        var events = active.Where(item => item.IsEvent).OrderBy(item => item.StartAt ?? DateTime.MaxValue).ThenBy(item => item.Title).ToList();

        var html = new StringBuilder("<!doctype html><html lang='zh-CN'><head><meta charset='utf-8'><title>PaykiTodo 服务器快照</title><style>");
        html.Append("body{font-family:Segoe UI,Microsoft YaHei,sans-serif;background:#f5f7f9;color:#20252b;margin:0;padding:28px}header{max-width:1320px;margin:0 auto 22px}h1{font-size:28px;margin:0 0 8px}h2{font-size:18px;margin:0 0 14px}.meta{color:#68727d;font-size:13px}.grid{max-width:1320px;margin:auto;display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}.panel{background:#fff;border:1px solid #e0e5ea;border-radius:12px;padding:18px;box-shadow:0 2px 8px #1d27330d}.item{border-left:4px solid var(--accent);padding:9px 10px;margin:8px 0;background:#fafbfc}.title{font-weight:650}.time{font-size:13px;color:#64707d;margin-top:4px}.empty{color:#89939e;font-size:14px;padding:12px 0}@media(max-width:900px){.grid{grid-template-columns:1fr}}"
        );
        html.Append("</style></head><body><header><h1>PaykiTodo 服务器快照</h1><div class='meta'>导出时间：")
            .Append(Html(snapshot.ExportedAt.ToString("yyyy-MM-dd HH:mm")))
            .Append(" · 只读展示 · 不会修改手机数据</div></header><main class='grid'>");
        html.Append(RenderPanel("今日看板", RenderItems(todayTodos, "今天暂无待办"), RenderItems(todayEvents, "今天暂无日程") + RenderItems(tomorrowEvents, "明天暂无日程", "明日")));
        html.Append(RenderPanel("待办", RenderItems(todos, "暂无未完成待办")));
        html.Append(RenderPanel("日历", RenderItems(events, "暂无未完成日程")));
        html.Append("</main></body></html>");
        return html.ToString();
    }

    private static string RenderPanel(string title, string content, string suffix = "")
    {
        return "<section class='panel'><h2>" + Html(title) + "</h2>" + content + suffix + "</section>";
    }

    private static string RenderItems(IEnumerable<ServerSnapshotItem> items, string empty, string heading = "")
    {
        var list = items.ToList();
        var html = new StringBuilder();
        if (!string.IsNullOrWhiteSpace(heading)) html.Append("<div class='time'><strong>").Append(Html(heading)).Append("</strong></div>");
        if (list.Count == 0) return html.Append("<div class='empty'>").Append(Html(empty)).Append("</div>").ToString();
        foreach (var item in list)
        {
            html.Append("<article class='item' style='--accent:").Append(item.AccentColor).Append("'><div class='title'>")
                .Append(Html(item.Title)).Append("</div><div class='time'>")
                .Append(Html(FormatItemTime(item))).Append(" · ").Append(Html(item.GroupName));
            if (!string.IsNullOrWhiteSpace(item.Location)) html.Append(" · ").Append(Html(item.Location));
            html.Append("</div></article>");
        }
        return html.ToString();
    }

    private static string FormatItemTime(ServerSnapshotItem item)
    {
        if (item.IsEvent)
        {
            var start = item.StartAt?.ToString("M月d日 HH:mm") ?? "未设置开始时间";
            var end = item.EndAt?.ToString("HH:mm");
            return end is null ? start : $"{start}-{end}";
        }
        return item.DueAt?.ToString("M月d日 HH:mm") ?? "无 DDL";
    }

    private static string Html(string value)
    {
        return System.Net.WebUtility.HtmlEncode(value);
    }

    private sealed record DesktopPreferences(string Endpoint, string ServerUrl, string DeviceId);
}
