namespace PaykiTodo.Desktop;

// The native UI and cloud transport share this local-first workspace. A network
// response must never replace edits made while that request was in flight.
internal sealed class DesktopWorkspace
{
    private readonly DesktopSecureStore store;
    private readonly SemaphoreSlim syncGate = new(1, 1);
    private long editVersion;
    private long accountGeneration;
    internal long AccountGeneration => accountGeneration;
    public DesktopSnapshotDocument? Document { get; private set; }
    public CloudAccountState? Account { get; private set; }
    public string Status { get; private set; } = "登录后，日程和待办会保存在这台电脑上。";
    public event Action? Changed;

    public DesktopWorkspace(DesktopSecureStore? store = null)
    {
        this.store = store ?? new DesktopSecureStore();
        var saved = this.store.Load();
        if (saved is not null && string.IsNullOrEmpty(saved.Value.State.AccessToken))
        {
            Status = "已退出登录，请重新登录以访问本机数据。";
            return;
        }
        Account = saved?.State;
        Document = saved?.Document ?? this.store.LoadDocument();
        if (Document != null) Status = "已打开本地数据";
    }

    public void Edit(Action<DesktopSnapshotDocument> change)
    {
        if (Document == null) throw new InvalidOperationException("请先登录账号");
        // Validate and save an isolated candidate; a failed edit cannot corrupt
        // the active document, even when disk persistence fails.
        var candidate = DesktopSnapshotDocument.FromArchiveBytes(Document.ToArchiveBytes());
        change(candidate);
        candidate.EnsureRecurringInstances(DateTime.Today);
        var next = Account is null ? null : Account with { Dirty = true };
        if (next != null) store.Save(next, candidate); else store.SaveDocument(candidate);
        Document = candidate;
        Account = next;
        editVersion++;
        Notify("已保存在本机" + (Account == null ? "" : "，等待同步"));
        if (Account != null) _ = SyncAsync();
    }

    public async Task LoginAsync(string server, string username, string password, bool register, string? token)
    {
        var result = await CloudAccountSync.AuthenticateAsync(server, username, password,
            Account?.DeviceId ?? Guid.NewGuid().ToString("N"), register, turnstileToken: token);
        var saved = store.Load();
        var same = saved != null && saved.Value.State.Username == result.State.Username &&
            saved.Value.State.ServerUrl == result.State.ServerUrl;
        // Never upload a previous user's local document into another account.
        var document = same ? saved!.Value.Document : DesktopSnapshotDocument.CreateEmpty();
        var account = result.State with { Revision = same ? saved!.Value.State.Revision : 0,
            Dirty = same && saved!.Value.State.Dirty };
        if (!same && Document?.HasMeaningfulData == true) store.SaveSafetyCopy(Document);
        store.Save(account, document);
        accountGeneration++;
        Account = account;
        Document = document;
        editVersion++;
        Notify("登录成功，正在同步…");
        await SyncAsync();
    }

    public async Task SyncAsync()
    {
        if (Account == null || Document == null || !await syncGate.WaitAsync(0)) return;
        var generation = accountGeneration;
        var version = editVersion;
        var account = Account;
        var snapshot = DesktopSnapshotDocument.FromArchiveBytes(Document.ToArchiveBytes());
        try
        {
            account = await CloudAccountSync.EnsureAccessTokenAsync(account, default);
            if (generation != accountGeneration || Account == null || Document == null) return;
            Account = account with { Dirty = Account.Dirty, Revision = Account.Revision };
            store.Save(Account, Document);
            var result = await CloudAccountSync.SyncAsync(account, snapshot);
            if (generation != accountGeneration) return;
            if (version != editVersion)
            {
                // A download based on an old local version needs conflict review.
                var downloaded = !account.Dirty && result.State.Revision > account.Revision;
                Account = downloaded ? Account : result.State with { Dirty = true };
                store.Save(Account!, Document!);
                Notify(downloaded ? "云端和本机都有新修改，请在设置中处理同步冲突。" : "新修改已保存在本机，稍后继续同步。");
                return;
            }
            store.Save(result.State, result.Document);
            Account = result.State;
            Document = result.Document;
            Notify(result.Message);
        }
        catch (Exception error)
        {
            if (generation == accountGeneration) Notify(error is CloudRevisionConflictException
                ? "云端和本机都有新修改，请在设置中处理同步冲突。"
                : "本地数据可继续使用；" + FriendlyError(error));
        }
        finally { syncGate.Release(); }
    }

    public async Task UseCloudAsync()
    {
        if (Document == null || Account == null) return;
        if (syncGate.CurrentCount == 0) { Notify("同步正在进行，请稍后再处理冲突。"); return; }
        store.SaveSafetyCopy(Document);
        accountGeneration++;
        Account = Account with { Revision = 0, Dirty = false };
        store.Save(Account, Document);
        await SyncAsync();
    }

    public async Task LogoutAsync()
    {
        var account = Account;
        accountGeneration++;
        // Keep the encrypted account/workspace pair for the same user's next login.
        // Local access is locked until authentication succeeds again.
        if (account != null && Document != null) store.Save(account with { AccessToken = "", AccessExpiresAt = 0 }, Document);
        Account = null;
        Document = null;
        Notify("已退出登录，本地数据已保留。");
        if (account != null) try { await CloudAccountSync.LogoutAsync(account); } catch { }
    }

    public async Task<System.Text.Json.JsonElement> AccountRequestAsync(string path, HttpMethod method, object? payload = null)
    {
        await syncGate.WaitAsync();
        var generation = accountGeneration;
        try {
            if (Account == null || Document == null) throw new InvalidOperationException("请先登录账号");
            var refreshed = await CloudAccountSync.EnsureAccessTokenAsync(Account, default);
            if (generation != accountGeneration || Account == null || Document == null) throw new InvalidOperationException("账号已退出");
            Account = refreshed with { Dirty = Account.Dirty, Revision = Account.Revision };
            store.Save(Account, Document);
            var result = await CloudAccountSync.AccountRequestAsync(Account, path, method, payload);
            if (generation != accountGeneration || Account == null || Document == null) throw new InvalidOperationException("账号已退出");
            Account = result.State with { Dirty = Account.Dirty, Revision = Account.Revision };
            store.Save(Account, Document); return result.Body;
        } finally { syncGate.Release(); }
    }

    public async Task ChangePasswordAsync(string currentPassword, string newPassword)
    {
        await syncGate.WaitAsync();
        try {
            if (Account == null || Document == null) throw new InvalidOperationException("请先登录账号");
            var generation = accountGeneration;
            var refreshed = await CloudAccountSync.EnsureAccessTokenAsync(Account, default);
            if (generation != accountGeneration || Account == null || Document == null) return;
            Account = refreshed with { Dirty = Account.Dirty, Revision = Account.Revision };
            store.Save(Account, Document);
            store.SaveSafetyCopy(Document);
            var next = await CloudAccountSync.ChangePasswordAsync(Account, currentPassword, newPassword);
            if (generation != accountGeneration || Account == null || Document == null) return;
            store.Save(next with { Dirty = Account.Dirty }, Document);
            accountGeneration++; Account = null; Document = null;
            Notify("密码已修改，请使用新密码重新登录。其他设备也需要重新登录。");
        } finally { syncGate.Release(); }
    }

    public void Import(byte[] bytes)
    {
        var document = DesktopSnapshotDocument.FromArchiveBytes(bytes);
        if (Document == null) throw new InvalidOperationException("请先登录账号");
        document.PreserveLocalApiKeys(Document);
        store.SaveSafetyCopy(Document);
        Edit(target => target.ReplaceWith(document));
    }

    public void Notify(string message) { Status = message; Changed?.Invoke(); }
    public static string FriendlyError(Exception error) => error switch
    {
        HttpRequestException => "暂时无法连接服务器，请检查网络后重试。",
        TaskCanceledException => "连接超时，请稍后重试。",
        _ => error.Message
    };
}
