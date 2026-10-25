namespace PaykiTodo.Desktop;

// The native UI and cloud transport share this local-first workspace. A network
// response must never replace edits made while that request was in flight.
internal sealed partial class DesktopWorkspace
{
    private readonly DesktopSecureStore store;
    private readonly SemaphoreSlim syncGate = new(1, 1);
    private long editVersion;
    private long accountGeneration;
    private bool syncRequested;
    private readonly object syncRequestLock = new();
    private Task? syncTask;
    internal long AccountGeneration => accountGeneration;
    public DesktopSnapshotDocument? Document { get; private set; }
    public CloudAccountState? Account { get; private set; }
    public string Status { get; private set; } = "登录后，日程和待办会保存在这台电脑上。";
    public event Action? Changed;
    public bool SessionExpired { get; private set; }
    public event Action? LoginRequired;
    internal sealed record CloudReview(CloudDownload Remote, long EditVersion, long AccountGeneration);

    public DesktopWorkspace(DesktopSecureStore? store = null)
    {
        this.store = store ?? new DesktopSecureStore();
        var saved = this.store.Load();
        if (saved is not null && (saved.Value.State.LocalAccessLocked || (string.IsNullOrEmpty(saved.Value.State.AccessToken) && saved.Value.State.RefreshExpiresAt == 0)))
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
        // Shared event edits use this same native entry point. They must not
        // publish a new personal snapshot; retain any already pending writes.
        var personalChanged = !Document.HasSamePersonalSyncContent(candidate);
        var next = Account is null ? null : Account with { Dirty = Account.Dirty || personalChanged };
        if (next != null) store.Save(next, candidate); else store.SaveDocument(candidate);
        Document = candidate;
        Account = next;
        editVersion++;
        Notify("已保存在本机" + (Account?.Dirty == true ? "，等待同步" : ""));
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
            Dirty = same && saved!.Value.State.Dirty,
            VerifiedDataFingerprint = same ? saved!.Value.State.VerifiedDataFingerprint : "" };
        if (!same && Document?.HasMeaningfulData == true) store.SaveSafetyCopy(Document);
        store.Save(account, document);
        accountGeneration++;
        Account = account;
        SessionExpired = false;
        Document = document;
        editVersion++;
        Notify("登录成功，正在同步…");
        await SyncAsync();
    }

    public Task SyncAsync()
    {
        lock (syncRequestLock) {
            if (Account == null || Document == null) return Task.CompletedTask;
            syncRequested = true;
            return syncTask ??= DrainSyncAsync();
        }
    }

    private async Task DrainSyncAsync()
    {
        // Yield before publishing the task, including with no UI context.
        await Task.Yield();
        try {
            while (true) {
                lock (syncRequestLock) syncRequested = false;
                await syncGate.WaitAsync();
                try { if (Account != null && Document != null) await SyncOnceAsync(); }
                finally { syncGate.Release(); }
                lock (syncRequestLock) {
                    if (syncRequested && Account != null && Document != null) continue;
                    syncTask = null;
                    return;
                }
            }
        } catch {
            lock (syncRequestLock) syncTask = null;
            throw;
        }
    }

    private async Task SyncOnceAsync()
    {
        var generation = accountGeneration;
        var version = editVersion;
        var account = Account;
        var local = Document;
        if (account == null || local == null) return;
        var snapshot = DesktopSnapshotDocument.FromArchiveBytes(local.ToArchiveBytes());
        try
        {
            account = await CloudAccountSync.EnsureAccessTokenAsync(account, default);
            if (generation != accountGeneration || Account == null || Document == null) return;
            Account = account with { Dirty = Account.Dirty, Revision = Account.Revision };
            store.Save(Account, Document);
            var result = await CloudAccountSync.SyncAsync(account, snapshot);
            if (generation != accountGeneration) return;
            if (version != editVersion && !snapshot.HasSamePersonalSyncContent(Document!))
            {
                // A download based on an old local version needs conflict review.
                var downloaded = result.Document != snapshot;
                Account = downloaded ? Account : result.State with { Dirty = true };
                store.Save(Account!, Document!);
                Notify(downloaded ? "云端和本机都有新修改，请在设置中处理同步冲突。" : "新修改已保存在本机，稍后继续同步。");
                return;
            }
            var downloadedDocument = result.Document != snapshot;
            if (downloadedDocument) {
                // Shared updates made during a personal download belong to the
                // current local cache, not the copy captured before the request.
                result.Document.PreserveSharedData(Document!);
                store.SaveSafetyCopy(Document!);
            }
            var document = downloadedDocument ? result.Document : Document!;
            store.Save(result.State, document);
            Account = result.State;
            Document = document;
            SessionExpired = false;
            Notify(result.Message);
        }
        catch (Exception error)
        {
            if (error is CloudSessionExpiredException && generation == accountGeneration && !SessionExpired) { SessionExpired = true; LoginRequired?.Invoke(); }
            if (generation == accountGeneration) Notify(error is CloudRevisionConflictException
                ? "本机与云端副本不同，未覆盖数据。打开设置→检查云端；较新内容在手机时，请在手机确认保留本机上传，然后电脑恢复云端。"
                : "本地数据可继续使用；" + FriendlyError(error));
        }
    }

    public async Task UseCloudAsync()
    {
        var review = await InspectCloudAsync();
        await ResolveCloudAsync(review, keepLocal: false);
    }

    public async Task LogoutAsync()
    {
        var account = Account;
        accountGeneration++;
        // Keep the encrypted account/workspace pair for the same user's next login.
        // Local access is locked until authentication succeeds again.
        if (account != null && Document != null) store.Save(account with { AccessToken = "", AccessExpiresAt = 0, LocalAccessLocked = true }, Document);
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

    public async Task<CloudReview> InspectCloudAsync()
    {
        await syncGate.WaitAsync();
        try {
            if (Account == null || Document == null) throw new InvalidOperationException("请先登录账号");
            var generation = accountGeneration;
            var refreshed = await CloudAccountSync.EnsureAccessTokenAsync(Account, default);
            if (generation != accountGeneration || Account == null || Document == null) throw new InvalidOperationException("账号已退出");
            Account = refreshed with { Revision = Account.Revision, Dirty = Account.Dirty };
            store.Save(Account, Document);
            var remote = await CloudAccountSync.DownloadAsync(Account, default);
            if (generation != accountGeneration || Account == null || Document == null) throw new InvalidOperationException("账号已退出");
            return new CloudReview(remote, editVersion, generation);
        } finally { syncGate.Release(); }
    }

    public async Task ResolveCloudAsync(CloudReview review, bool keepLocal)
    {
        await syncGate.WaitAsync();
        try {
            if (Account == null || Document == null || review.AccountGeneration != accountGeneration) throw new InvalidOperationException("账号已改变，请重新检查");
            if (review.EditVersion != editVersion) throw new InvalidOperationException("本机已有新修改，请重新检查");
            var refreshed = await CloudAccountSync.EnsureAccessTokenAsync(Account, default);
            if (Account == null || Document == null || review.AccountGeneration != accountGeneration) throw new InvalidOperationException("账号已改变，请重新检查");
            Account = refreshed with { Revision = Account.Revision, Dirty = Account.Dirty };
            store.Save(Account, Document);
            var revision = await CloudAccountSync.GetRemoteRevisionAsync(Account, default);
            if (revision != review.Remote.Revision) throw new InvalidOperationException("云端又有更新，请重新检查");
            if (Account == null || Document == null || review.AccountGeneration != accountGeneration || review.EditVersion != editVersion) throw new InvalidOperationException("本机或账号已有变化，请重新检查");
            store.SaveSafetyCopy(Document); store.SaveSafetyCopy(review.Remote.Document, "cloud");
            if (!keepLocal) {
                var document = DesktopSnapshotDocument.FromArchiveBytes(review.Remote.Document.ToArchiveBytes());
                document.PreserveLocalApiKeys(Document);
                document.PreserveSharedData(Document);
                Account = Account with { Revision = revision, Dirty = false, VerifiedDataFingerprint = document.DataFingerprint() }; Document = document; editVersion++;
                store.Save(Account, Document); Notify("已恢复云端副本；双方操作前副本已保留。");
            } else {
                var version = editVersion; var generation = accountGeneration;
                var snapshot = DesktopSnapshotDocument.FromArchiveBytes(Document.ToArchiveBytes());
                var result = await CloudAccountSync.SyncAsync(Account with { Revision = revision, Dirty = true }, snapshot);
                if (generation != accountGeneration || Account == null || Document == null) return;
                Account = result.State with { Dirty = version != editVersion && !snapshot.HasSamePersonalSyncContent(Document) };
                store.Save(Account, Document); Notify("本机副本已上传；双方操作前副本已保留。");
            }
        } finally { syncGate.Release(); }
    }

    public void RestoreSafetyCopy() => Import(store.LatestSafetyCopy());

    public void Notify(string message) { Status = message; Changed?.Invoke(); }
    public static string FriendlyError(Exception error) => error switch
    {
        HttpRequestException => "暂时无法连接服务器，请检查网络后重试。",
        TaskCanceledException => "连接超时，请稍后重试。",
        _ => error.Message
    };
}
