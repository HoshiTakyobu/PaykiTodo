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
    private readonly Button registerButton = new();
    private readonly Button disconnectButton = new();
    private readonly WebView2 browser = new();
    private readonly string preferencesPath;
    private readonly DesktopSecureStore cloudStore = new();
    private readonly SemaphoreSlim syncGate = new(1, 1);
    private readonly System.Windows.Forms.Timer cloudSyncTimer = new() { Interval = 60_000 };
    private string? pendingToken;
    private CloudAccountState? cloudState;
    private DesktopSnapshotDocument? localDocument;

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
        LoadCloudState();
        UpdateModeFields();
        cloudSyncTimer.Tick += async (_, _) =>
        {
            if (cloudState is null || localDocument is null || modeBox.SelectedIndex != 0) return;
            try
            {
                await SyncCloudAsync();
                RenderStandaloneClient();
            }
            catch (Exception error)
            {
                SetStatus("本地数据可继续使用；定时同步未完成：" + error.Message);
            }
        };
        cloudSyncTimer.Start();
        Shown += async (_, _) => await InitializeBrowserAsync();
    }

    private void BuildToolbar()
    {
        var toolbar = new TableLayoutPanel
        {
            Dock = DockStyle.Top,
            Height = 116,
            ColumnCount = 10,
            RowCount = 2,
            Padding = new Padding(10, 8, 10, 8)
        };
        var widths = new[] { 70, 180, 70, 190, 70, 180, 100, 100, 100, 100 };
        foreach (var width in widths)
            toolbar.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, width));
        toolbar.RowStyles.Add(new RowStyle(SizeType.Percent, 50));
        toolbar.RowStyles.Add(new RowStyle(SizeType.Percent, 50));

        var modeLabel = CreateLabel("连接模式");
        modeBox.Items.AddRange(new object[] { "独立云客户端", "手机 LAN（兼容）" });
        modeBox.SelectedIndex = 0;
        modeBox.Dock = DockStyle.Fill;
        modeBox.DropDownStyle = ComboBoxStyle.DropDownList;
        modeBox.SelectedIndexChanged += (_, _) => UpdateModeFields();

        endpointLabel.Text = "手机地址";
        endpointBox.PlaceholderText = "http://192.168.x.x:18765";
        tokenLabel.Text = "访问密钥";
        tokenBox.MaxLength = 4;
        tokenBox.UseSystemPasswordChar = true;

        serverLabel.Text = "云服务器";
        serverBox.PlaceholderText = "https://sync.example.com";
        deviceLabel.Text = "账号";
        deviceBox.PlaceholderText = "你的 PaykiTodo 账号";
        secretLabel.Text = "密码";
        secretBox.PlaceholderText = "登录密码（不保存明文）";
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
        registerButton.Text = "注册账号";
        registerButton.Dock = DockStyle.Fill;
        registerButton.Click += async (_, _) => await ConnectCloudAsync(register: true);
        disconnectButton.Text = "断开";
        disconnectButton.Dock = DockStyle.Fill;
        disconnectButton.Enabled = false;
        disconnectButton.Click += async (_, _) => await DisconnectAsync();

        toolbar.Controls.Add(modeLabel, 0, 0);
        toolbar.Controls.Add(modeBox, 1, 0);
        toolbar.Controls.Add(endpointLabel, 2, 0);
        toolbar.Controls.Add(endpointBox, 3, 0);
        toolbar.Controls.Add(tokenLabel, 4, 0);
        toolbar.Controls.Add(tokenBox, 5, 0);
        toolbar.Controls.Add(connectButton, 6, 0);
        toolbar.Controls.Add(registerButton, 7, 0);
        toolbar.Controls.Add(disconnectButton, 8, 0);
        toolbar.SetColumnSpan(disconnectButton, 2);

        toolbar.Controls.Add(serverLabel, 0, 1);
        toolbar.Controls.Add(serverBox, 1, 1);
        toolbar.SetColumnSpan(serverBox, 2);
        toolbar.Controls.Add(deviceLabel, 3, 1);
        toolbar.Controls.Add(deviceBox, 4, 1);
        toolbar.SetColumnSpan(deviceBox, 2);
        toolbar.Controls.Add(secretLabel, 6, 1);
        toolbar.Controls.Add(secretBox, 7, 1);
        toolbar.SetColumnSpan(secretBox, 3);
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
            else
                browser.CoreWebView2.WebMessageReceived += BrowserOnWebMessageReceived;
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
            if (cloudState is not null && localDocument is not null && modeBox.SelectedIndex == 0)
                RenderStandaloneClient();
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
            await ConnectPhoneLanAsync();
            return;
        }

        if (cloudState is not null && localDocument is not null)
        {
            try
            {
                await SyncCloudAsync();
            }
            catch (Exception error)
            {
                SetStatus("本地数据可继续使用；云同步未完成：" + error.Message);
            }
            RenderStandaloneClient();
            disconnectButton.Enabled = true;
            return;
        }
        await ConnectCloudAsync(register: false);
    }

    private async Task ConnectCloudAsync(bool register)
    {
        var serverUrl = serverBox.Text.Trim();
        var username = deviceBox.Text.Trim();
        var password = secretBox.Text;
        if (serverUrl.Length == 0 || username.Length == 0 || password.Length == 0)
        {
            SetStatus(register ? "请填写云服务器、账号和注册密码" : "请填写云服务器、账号和密码");
            return;
        }

        try
        {
            connectButton.Enabled = false;
            registerButton.Enabled = false;
            SetStatus(register ? "正在注册云账号…" : "正在登录云账号…");
            var stored = cloudStore.Load();
            var deviceId = stored?.State.Username.Equals(username, StringComparison.OrdinalIgnoreCase) == true
                ? stored!.Value.State.DeviceId
                : Guid.NewGuid().ToString("N");
            var authentication = await CloudAccountSync.AuthenticateAsync(serverUrl, username, password, deviceId, register);
            var useStored = stored is not null &&
                stored.Value.State.Username.Equals(authentication.State.Username, StringComparison.OrdinalIgnoreCase) &&
                stored.Value.State.ServerUrl.Equals(authentication.State.ServerUrl, StringComparison.OrdinalIgnoreCase);
            localDocument = useStored ? stored!.Value.Document : localDocument ?? DesktopSnapshotDocument.CreateEmpty();
            var initialState = useStored
                ? authentication.State with
                {
                    Revision = stored!.Value.State.Revision,
                    Dirty = stored.Value.State.Dirty
                }
                : authentication.State with
                {
                    Revision = 0,
                    Dirty = localDocument.HasMeaningfulData
                };
            cloudState = initialState;
            SaveCloudState();
            try
            {
                await SyncCloudAsync();
            }
            catch (CloudRevisionConflictException error)
            {
                SetStatus(error.Message);
            }
            secretBox.Clear();
            SavePreferences(endpointBox.Text, serverUrl, username);
            connectButton.Enabled = false;
            disconnectButton.Enabled = true;
            registerButton.Enabled = false;
            RenderStandaloneClient();
        }
        catch (Exception error)
        {
            SetStatus("云账号操作失败：" + error.Message);
            connectButton.Enabled = true;
            registerButton.Enabled = true;
            disconnectButton.Enabled = false;
        }
    }

    private async Task ConnectPhoneLanAsync()
    {
        var endpoint = NormalizeEndpoint(endpointBox.Text);
        var token = tokenBox.Text.Trim().ToUpperInvariant();
        if (endpoint is null || token.Length == 0)
        {
            SetStatus("请输入手机端显示的 http://地址和访问密钥");
            return;
        }

        try
        {
            await browser.EnsureCoreWebView2Async();
            pendingToken = token;
            endpointBox.Text = endpoint;
            SavePreferences(endpoint, serverBox.Text, deviceBox.Text);
            SetStatus("正在连接手机 LAN 网页（直接修改手机数据）…");
            browser.CoreWebView2.Navigate(endpoint);
            connectButton.Enabled = false;
            disconnectButton.Enabled = true;
        }
        catch (Exception error)
        {
            connectButton.Enabled = true;
            disconnectButton.Enabled = false;
            SetStatus("手机 LAN 连接失败：" + error.Message);
        }
    }

    private async Task SyncCloudAsync()
    {
        if (cloudState is null || localDocument is null) return;
        await syncGate.WaitAsync();
        try
        {
            var result = await CloudAccountSync.SyncAsync(cloudState, localDocument);
            cloudState = result.State;
            localDocument = result.Document;
            SaveCloudState();
            SetStatus(result.Message);
        }
        catch (CloudRevisionConflictException error)
        {
            cloudState = cloudState with { Dirty = true };
            SaveCloudState();
            SetStatus(error.Message);
            throw;
        }
        finally
        {
            syncGate.Release();
        }
    }

    private void RenderStandaloneClient()
    {
        if (localDocument is null || browser.CoreWebView2 is null) return;
        var displayState = cloudState ?? new CloudAccountState("", "本地未登录", "", "", "", 0, 0, "", 0, true);
        browser.NavigateToString(DesktopHtmlRenderer.Render(localDocument, displayState));
    }

    private async void BrowserOnWebMessageReceived(object? sender, CoreWebView2WebMessageReceivedEventArgs args)
    {
        if (localDocument is null || modeBox.SelectedIndex != 0) return;
        try
        {
            using var json = JsonDocument.Parse(args.WebMessageAsJson);
            var root = json.RootElement;
            var action = root.GetProperty("action").GetString() ?? "";
            var id = root.TryGetProperty("id", out var idValue) ? idValue.GetInt64() : 0L;
            switch (action)
            {
                case "addQuickChecklist":
                    SaveQuickChecklist();
                    break;
                case "addTodo":
                case "addEvent":
                    SaveEditedItem(action == "addEvent", null);
                    break;
                case "edit":
                    SaveEditedItem(localDocument.EditFor(id)?.IsEvent ?? false, id);
                    break;
                case "complete":
                    localDocument.Complete(id);
                    await SaveLocalAndSyncAsync("已完成本地待办");
                    break;
                case "cancel":
                    localDocument.Cancel(id);
                    await SaveLocalAndSyncAsync("已取消并归档本地事项");
                    break;
                case "delete":
                    localDocument.Delete(id);
                    await SaveLocalAndSyncAsync("已删除本地事项");
                    break;
                case "quickGroupToggle":
                    localDocument.ToggleQuickChecklistGroup(id);
                    await SaveLocalAndSyncAsync("清单展开状态已更新");
                    break;
                case "quickItemToggle":
                    localDocument.ToggleQuickChecklistItem(id);
                    await SaveLocalAndSyncAsync("子任务展开状态已更新");
                    break;
                case "savePlanning":
                    localDocument.SavePlanningNote(
                        id > 0 ? id : null,
                        root.TryGetProperty("title", out var title) ? title.GetString() ?? "" : "",
                        root.TryGetProperty("markdown", out var markdown) ? markdown.GetString() ?? "" : ""
                    );
                    await SaveLocalAndSyncAsync("规划已保存");
                    break;
            }
        }
        catch (Exception error)
        {
            SetStatus("桌面操作失败：" + error.Message);
        }
    }

    private void SaveEditedItem(bool isEvent, long? existingId)
    {
        if (localDocument is null) return;
        var existing = existingId.HasValue ? localDocument.EditFor(existingId.Value) : null;
        var edit = ItemEditorDialog.ShowEditor(this, isEvent, localDocument.Groups, existing);
        if (edit is null) return;
        localDocument.SaveItem(edit, existingId);
        _ = SaveLocalAndSyncAsync(existingId.HasValue ? "事项已在本地更新" : "事项已保存到本地");
    }

    private void SaveQuickChecklist()
    {
        if (localDocument is null) return;
        var draft = QuickChecklistEditorDialog.ShowEditor(this);
        if (draft is null) return;
        try
        {
            localDocument.CreateQuickChecklist(draft);
            _ = SaveLocalAndSyncAsync("已创建清单：" + draft.Title);
        }
        catch (Exception error)
        {
            MessageBox.Show(this, error.Message, "PaykiTodo", MessageBoxButtons.OK, MessageBoxIcon.Warning);
        }
    }

    private async Task SaveLocalAndSyncAsync(string localMessage)
    {
        if (localDocument is null) return;
        if (cloudState is null)
        {
            cloudStore.SaveDocument(localDocument);
            RenderStandaloneClient();
            SetStatus(localMessage + "；当前未登录，数据已保存在本机");
            return;
        }
        cloudState = cloudState with { Dirty = true };
        cloudStore.Save(cloudState, localDocument);
        RenderStandaloneClient();
        SetStatus(localMessage + "，正在同步…");
        try
        {
            await SyncCloudAsync();
            RenderStandaloneClient();
        }
        catch (Exception error)
        {
            SetStatus(localMessage + "；云同步未完成：" + error.Message);
        }
    }

    private void LoadCloudState()
    {
        var saved = cloudStore.Load();
        if (saved is null)
        {
            localDocument = cloudStore.LoadDocument();
            return;
        }
        cloudState = saved.Value.State;
        localDocument = saved.Value.Document;
        serverBox.Text = cloudState.ServerUrl;
        deviceBox.Text = cloudState.Username;
        SetStatus(cloudState.Dirty ? "已打开本地副本，存在待同步修改" : "已打开本地云端副本");
    }

    private void SaveCloudState()
    {
        if (cloudState is not null && localDocument is not null) cloudStore.Save(cloudState, localDocument);
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

    private async Task DisconnectAsync()
    {
        if (modeBox.SelectedIndex == 0)
        {
            try
            {
                if (cloudState is not null)
                    await CloudAccountSync.LogoutAsync(cloudState);
            }
            catch
            {
                // Local logout must still succeed when the cloud is unavailable.
            }
            finally
            {
                if (localDocument is not null) cloudStore.SaveDocument(localDocument);
                cloudStore.ClearAccount();
                cloudState = null;
                secretBox.Clear();
                connectButton.Enabled = true;
                registerButton.Enabled = true;
                disconnectButton.Enabled = false;
                RenderStandaloneClient();
                SetStatus("已退出云账号，本地数据仍保留并可离线编辑");
            }
            return;
        }
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
        var phoneMode = modeBox.SelectedIndex == 1;
        endpointBox.Enabled = phoneMode;
        tokenBox.Enabled = phoneMode;
        serverBox.Enabled = !phoneMode;
        deviceBox.Enabled = !phoneMode;
        secretBox.Enabled = !phoneMode;
        connectButton.Text = phoneMode ? "连接手机" : (cloudState is null ? "登录云账号" : "打开客户端");
        registerButton.Enabled = !phoneMode && cloudState is null;
        disconnectButton.Text = phoneMode ? "断开" : "退出账号";
        disconnectButton.Enabled = phoneMode ? disconnectButton.Enabled : cloudState is not null;
        if (phoneMode)
            SetStatus("兼容模式：请输入手机端电脑同步地址和访问密钥");
        else
            SetStatus(cloudState is null ? "独立客户端：登录云账号后使用本机数据" : "独立客户端：本机数据已就绪，可离线使用");
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
