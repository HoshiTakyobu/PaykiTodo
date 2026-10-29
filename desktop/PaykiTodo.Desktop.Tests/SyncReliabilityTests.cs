using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class SyncReliabilityTests
{
    private static System.Text.Json.Nodes.JsonArray Rows(DesktopSnapshotDocument document) =>
        (System.Text.Json.Nodes.JsonArray)typeof(DesktopSnapshotDocument).GetProperty("Tasks", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)!.GetValue(document)!;

    [Fact]
    public async Task RestartedOfflineCopiesAutomaticallyMergeIndependentEditsAndConverge()
    {
        using var server = new SnapshotServer();
        var firstStore = Store(server.Url); var first = new DesktopWorkspace(firstStore);
        first.Edit(d => { d.SaveItem(NativeWorkspaceTests.NewItem(false, "A")); d.SaveItem(NativeWorkspaceTests.NewItem(false, "B")); });
        await first.SyncAsync();
        var secondStore = Store(server.Url); var second = new DesktopWorkspace(secondStore); await second.SyncAsync();
        var offline = secondStore.Load()!.Value;
        offline.Document.Complete(offline.Document.Items.Single(i => i.Title == "B").Id);
        secondStore.Save(offline.State with { Dirty = true }, offline.Document);
        first.Edit(d => d.Complete(d.Items.Single(i => i.Title == "A").Id)); await first.SyncAsync();
        var restarted = new DesktopWorkspace(secondStore); await restarted.SyncAsync(); await first.SyncAsync();
        Assert.All(first.Document!.Items, i => Assert.True(i.Completed));
        Assert.All(secondStore.Load()!.Value.Document.Items, i => Assert.True(i.Completed));
        Assert.False(restarted.Account!.Dirty); Assert.Equal(server.Revision, first.Account!.Revision);
        Assert.NotNull(secondStore.Load()!.Value.State.SyncBaselineArchive);
    }

    [Fact]
    public async Task DelayedDirtyFlagDoesNotTurnCloudCompletionIntoConflictOrRedundantUpload()
    {
        using var server = new SnapshotServer();
        var store = Store(server.Url); var first = new DesktopWorkspace(store);
        first.Edit(d => d.SaveItem(NativeWorkspaceTests.NewItem(false, "完成"))); await first.SyncAsync();
        var second = new DesktopWorkspace(Store(server.Url)); await second.SyncAsync();
        var saved = store.Load()!.Value;
        store.Save(saved.State with { Dirty = true }, saved.Document);
        second.Edit(d => d.Complete(d.Items.Single().Id)); await second.SyncAsync();
        var revision = server.Revision;
        var reopened = new DesktopWorkspace(store); await reopened.SyncAsync();
        Assert.True(reopened.Document!.Items.Single().Completed); Assert.False(reopened.Account!.Dirty);
        Assert.Equal(revision, server.Revision);
    }

    [Fact]
    public void PassiveMissedAndSnoozeMergeWithExplicitCompletionInBothDirections()
    {
        var baseline = DesktopSnapshotDocument.CreateEmpty();
        baseline.SaveItem(NativeWorkspaceTests.NewItem(false, "19:30"));
        var local = DesktopSnapshotDocument.FromArchiveBytes(baseline.ToArchiveBytes());
        Rows(local)[0]!["missed"] = true; Rows(local)[0]!["missedAtMillis"] = 120000L;
        Rows(local)[0]!["reminderAtMillis"] = 110000L; Rows(local)[0]!["reminderEnabled"] = false;
        var remote = DesktopSnapshotDocument.FromArchiveBytes(baseline.ToArchiveBytes()); remote.Complete(remote.Items.Single().Id);
        foreach (var (a, b) in new[] { (local, remote), (remote, local) }) {
            var merged = DesktopSnapshotDocument.MergePersonal(baseline, a, b)!;
            Assert.NotNull(merged); Assert.True(merged.Items.Single().Completed);
            Assert.False(Rows(merged)[0]!["missed"]!.GetValue<bool>());
            Assert.False(Rows(merged)[0]!["reminderEnabled"]!.GetValue<bool>());
            Assert.Equal(110000L, Rows(merged)[0]!["reminderAtMillis"]!.GetValue<long>());
        }
    }

    [Fact]
    public async Task RealSameFieldConflictPreservesBothCopiesAcrossRepeatedPolls()
    {
        using var server = new SnapshotServer();
        var aStore = Store(server.Url); var a = new DesktopWorkspace(aStore);
        a.Edit(d => d.SaveItem(NativeWorkspaceTests.NewItem(false, "原始"))); await a.SyncAsync();
        var b = new DesktopWorkspace(Store(server.Url)); await b.SyncAsync();
        var saved = aStore.Load()!.Value; Rows(saved.Document)[0]!["title"] = "本机修改";
        aStore.Save(saved.State with { Dirty = true }, saved.Document);
        b.Edit(d => Rows(d)[0]!["title"] = "云端修改"); await b.SyncAsync();
        var revision = server.Revision;
        a = new DesktopWorkspace(aStore); await a.SyncAsync(); await a.SyncAsync();
        Assert.Equal("本机修改", a.Document!.Items.Single().Title); Assert.True(a.Account!.Dirty);
        Assert.Equal(revision, server.Revision);
        Assert.Equal("云端修改", DesktopSnapshotDocument.FromArchiveBytes(SnapshotCrypto.Decrypt(server.Payload!, "fixture-secret")).Items.Single().Title);
    }

    [Fact]
    public void CollidingNewIdsAndDeleteEditRemainConflicts()
    {
        var baseline = DesktopSnapshotDocument.CreateEmpty();
        var a = DesktopSnapshotDocument.FromArchiveBytes(baseline.ToArchiveBytes());
        var b = DesktopSnapshotDocument.FromArchiveBytes(baseline.ToArchiveBytes());
        a.SaveItem(NativeWorkspaceTests.NewItem(false, "A")); b.SaveItem(NativeWorkspaceTests.NewItem(false, "B"));
        // Force the same database-local identity regardless of clock-based allocation.
        Rows(b)[0]!["id"] = Rows(a)[0]!["id"]!.DeepClone();
        Assert.Null(DesktopSnapshotDocument.MergePersonal(baseline, a, b));
        baseline = DesktopSnapshotDocument.FromArchiveBytes(a.ToArchiveBytes());
        Rows(a).Clear(); b = DesktopSnapshotDocument.FromArchiveBytes(baseline.ToArchiveBytes()); Rows(b)[0]!["title"] = "edited";
        Assert.Null(DesktopSnapshotDocument.MergePersonal(baseline, a, b));
    }

    [Fact]
    public void RepeatedCompletionRetainsFirstTimestamp()
    {
        var baseline = DesktopSnapshotDocument.CreateEmpty(); baseline.SaveItem(NativeWorkspaceTests.NewItem(false, "same action"));
        var a = DesktopSnapshotDocument.FromArchiveBytes(baseline.ToArchiveBytes());
        var b = DesktopSnapshotDocument.FromArchiveBytes(baseline.ToArchiveBytes());
        a.Complete(a.Items.Single().Id); b.Complete(b.Items.Single().Id);
        Rows(a)[0]!["completedAtMillis"] = 10L; Rows(b)[0]!["completedAtMillis"] = 20L;
        var merged = DesktopSnapshotDocument.MergePersonal(baseline, a, b)!;
        Assert.Equal(10L, Rows(merged)[0]!["completedAtMillis"]!.GetValue<long>());
    }

    [Fact]
    public async Task SharedEventEditKeepsPersonalSnapshotWhilePersonalSettingsStillSync()
    {
        using var server = new SnapshotServer();
        var store = Store(server.Url);
        var saved = store.Load()!.Value;
        var document = EventParticipantsTests.Fixture();
        document.SharedCalendar(EventParticipantsTests.Calendar.Id)!["sharedServerUrl"] = server.Url;
        store.Save(saved.State, document);
        var workspace = new DesktopWorkspace(store);
        workspace.Edit(data => data.SaveItem(NativeWorkspaceTests.NewItem(false, "保留个人待办")));
        await workspace.SyncAsync();
        var revision = server.Revision;
        var payload = server.Payload!.ToArray();
        var shared = workspace.Document!.Items.Single(item => item.IsEvent);

        // The native editor uses Edit, unlike the transport-only EditShared path.
        workspace.Edit(data => data.SaveItem(data.EditFor(shared.Id)! with { Title = "只修改共享日程" }, shared.Id));
        await workspace.SyncAsync();
        Assert.Equal(revision, server.Revision);
        Assert.Equal(payload, server.Payload);
        Assert.Equal("只修改共享日程", store.Load()!.Value.Document.Items.Single(item => item.Id == shared.Id).Title);
        Assert.True(workspace.Document.SharedCalendar(shared.CalendarId)!["sharedDirty"]!.GetValue<bool>());
        Assert.False(workspace.Account!.Dirty);

        workspace.Edit(data => data.SaveItem(NativeWorkspaceTests.NewItem(true, "新建共享日程") with { CalendarId = shared.CalendarId }));
        await workspace.SyncAsync();
        Assert.Equal(revision, server.Revision);
        Assert.Equal(payload, server.Payload);

        workspace.Edit(data => data.Settings["themeMode"] = "DARK");
        await workspace.SyncAsync();
        Assert.Equal(revision + 1, server.Revision);
        var personal = DesktopSnapshotDocument.FromArchiveBytes(SnapshotCrypto.Decrypt(server.Payload!, "fixture-secret"));
        Assert.Equal("DARK", personal.Settings["themeMode"]!.GetValue<string>());
        Assert.Single(personal.Items);
        Assert.Equal("保留个人待办", personal.Items.Single().Title);
    }

    [Fact]
    public async Task SharedEditsDuringPersonalUploadAndDownloadPreserveBothChannels()
    {
        using var server = new SnapshotServer();
        var store = Store(server.Url);
        var document = EventParticipantsTests.Fixture();
        document.SharedCalendar(EventParticipantsTests.Calendar.Id)!["sharedServerUrl"] = server.Url;
        store.Save(store.Load()!.Value.State, document);
        var workspace = new DesktopWorkspace(store);
        var shared = document.Items.Single();
        server.HoldUpload = true;
        workspace.Edit(data => data.SaveItem(NativeWorkspaceTests.NewItem(false, "上传个人待办")));
        await server.Entered.Task.WaitAsync(TimeSpan.FromSeconds(10));
        workspace.Edit(data => data.SaveItem(data.EditFor(shared.Id)! with { Title = "上传期间修改共享日程" }, shared.Id));
        server.Release.TrySetResult();
        await workspace.SyncAsync();
        Assert.Equal(1, server.Revision);
        Assert.False(workspace.Account!.Dirty);
        Assert.Equal("上传期间修改共享日程", workspace.Document!.Items.Single(item => item.Id == shared.Id).Title);

        var remote = DesktopSnapshotDocument.FromArchiveBytes(SnapshotCrypto.Decrypt(server.Payload!, "fixture-secret"));
        var personal = remote.Items.Single();
        remote.SaveItem(remote.EditFor(personal.Id)! with { Title = "另一设备修改个人待办" }, personal.Id);
        server.Payload = SnapshotCrypto.Encrypt(remote.ToArchiveBytes(false, false), "fixture-secret");
        server.Revision = 2;
        server.ResetBarriers();
        server.HoldDownload = true;
        var downloading = workspace.SyncAsync();
        await server.Entered.Task.WaitAsync(TimeSpan.FromSeconds(10));
        workspace.EditShared(data => data.SaveItem(data.EditFor(shared.Id)! with { Title = "下载期间修改共享日程" }, shared.Id));
        server.Release.TrySetResult();
        await downloading;
        var persisted = store.Load()!.Value;
        Assert.Equal(2, persisted.State.Revision);
        Assert.False(persisted.State.Dirty);
        Assert.Equal("另一设备修改个人待办", persisted.Document.Items.Single(item => !item.IsEvent).Title);
        Assert.Equal("下载期间修改共享日程", persisted.Document.Items.Single(item => item.Id == shared.Id).Title);
        Assert.True(persisted.Document.SharedCalendar(shared.CalendarId)!["sharedDirty"]!.GetValue<bool>());
    }

    [Fact]
    public void RememberPasswordIsOptionalEncryptedAndSeparateFromMigrationArchive()
    {
        var folder = Path.Combine(Path.GetTempPath(), "PaykiTodo-tests", Guid.NewGuid().ToString("N"));
        var credentials = new RememberedLoginStore(folder);
        credentials.Save("fixture-user", "fixture-password-1234", true, true);
        Assert.Equal("fixture-password-1234", new RememberedLoginStore(folder).Load().Password);
        Assert.DoesNotContain("fixture-password-1234", Encoding.UTF8.GetString(File.ReadAllBytes(Path.Combine(folder, "remembered-login.bin"))));
        credentials.ForgetPassword(); Assert.Empty(credentials.Load().Password); Assert.True(credentials.Load().RememberAccount);
        credentials.Save("fixture-user", "fixture-password-1234", false, true);
        Assert.Empty(credentials.Load().Username); Assert.Empty(credentials.Load().Password);
    }
    [Fact]
    public async Task UploadKeepsEditsMadeWhileRequestIsInFlightAndIdleSyncKeepsDocument()
    {
        using var server = new SnapshotServer();
        var store = Store(server.Url);
        var workspace = new DesktopWorkspace(store);
        server.HoldUpload = true;
        workspace.Edit(document => document.SaveItem(NativeWorkspaceTests.NewItem(false, "第一次修改")));
        await server.Entered.Task.WaitAsync(TimeSpan.FromSeconds(10));
        workspace.Edit(document => document.SaveItem(NativeWorkspaceTests.NewItem(false, "上传途中修改")));
        server.Release.TrySetResult();
        await Until(() => workspace.Account?.Revision == 2 && !workspace.Account.Dirty);
        Assert.Equal(2, store.Load()!.Value.Document.Items.Count);
        Assert.False(workspace.Account!.Dirty);
        Assert.Equal(2, DesktopSnapshotDocument.FromArchiveBytes(SnapshotCrypto.Decrypt(server.Payload!, "fixture-secret")).Items.Count);
        var active = workspace.Document;
        await workspace.SyncAsync();
        Assert.Same(active, workspace.Document);
    }

    [Fact]
    public async Task VerifiedCopyDetectsMissedDirtyFlagAndPropagatesCompletionToAnotherClient()
    {
        using var server = new SnapshotServer();
        var firstStore = Store(server.Url);
        var first = new DesktopWorkspace(firstStore);
        first.Edit(document => document.SaveItem(NativeWorkspaceTests.NewItem(false, "跨端完成")));
        await first.SyncAsync();
        var second = new DesktopWorkspace(Store(server.Url));
        await second.SyncAsync();
        var saved = firstStore.Load()!.Value;
        saved.Document.Complete(saved.Document.Items.Single().Id);
        firstStore.Save(saved.State with { Dirty = false }, saved.Document);
        var reopened = new DesktopWorkspace(firstStore);
        await reopened.SyncAsync();
        await second.SyncAsync();
        Assert.True(second.Document!.Items.Single().Completed);
        Assert.False(reopened.Account!.Dirty);
        Assert.Equal(reopened.Account.Revision, second.Account!.Revision);
    }

    [Fact]
    public async Task LegacyCleanButDifferentCopiesRequireResolutionAcrossRepeatedPolls()
    {
        using var server = new SnapshotServer();
        var cloud = DesktopSnapshotDocument.CreateEmpty(); cloud.SaveItem(NativeWorkspaceTests.NewItem(false, "旧云端"));
        server.Payload = SnapshotCrypto.Encrypt(cloud.ToArchiveBytes(), "fixture-secret"); server.Revision = 4;
        var store = Store(server.Url); var saved = store.Load()!.Value;
        saved.Document.SaveItem(NativeWorkspaceTests.NewItem(false, "新本机"));
        store.Save(saved.State with { Revision = 4, Dirty = false }, saved.Document);
        var workspace = new DesktopWorkspace(store);
        await workspace.SyncAsync(); await workspace.SyncAsync();
        Assert.Equal(4, server.Revision);
        Assert.Equal("新本机", workspace.Document!.Items.Single().Title);
        Assert.Contains("未覆盖", workspace.Status);
        await workspace.ResolveCloudAsync(await workspace.InspectCloudAsync(), true);
        Assert.Equal("新本机", DesktopSnapshotDocument.FromArchiveBytes(SnapshotCrypto.Decrypt(server.Payload!, "fixture-secret")).Items.Single().Title);
    }

    [Fact]
    public async Task DownloadNeverOverwritesNewEditsAndConflictRestoreKeepsLocalSafetyCopy()
    {
        using var server = new SnapshotServer();
        var cloud = DesktopSnapshotDocument.CreateEmpty(); cloud.SaveItem(NativeWorkspaceTests.NewItem(false, "云端副本"));
        server.Payload = SnapshotCrypto.Encrypt(cloud.ToArchiveBytes(), "fixture-secret"); server.Revision = 1; server.HoldDownload = true;
        var store = Store(server.Url); var workspace = new DesktopWorkspace(store);
        var sync = workspace.SyncAsync();
        await server.Entered.Task.WaitAsync(TimeSpan.FromSeconds(10));
        workspace.Edit(document => document.SaveItem(NativeWorkspaceTests.NewItem(false, "下载途中修改")));
        server.Release.TrySetResult(); await sync;
        Assert.True(workspace.Account!.Dirty);
        Assert.Contains(workspace.Document!.Items, item => item.Title == "下载途中修改");
        Assert.DoesNotContain(workspace.Document.Items, item => item.Title == "云端副本");
        var stale = await workspace.InspectCloudAsync();
        workspace.Edit(document => document.SaveItem(NativeWorkspaceTests.NewItem(false, "检查后修改")));
        await Assert.ThrowsAsync<InvalidOperationException>(() => workspace.ResolveCloudAsync(stale, false));
        var review = await workspace.InspectCloudAsync();
        await workspace.ResolveCloudAsync(review, false);
        Assert.Equal("云端副本", Assert.Single(workspace.Document.Items).Title);
        var safety = DesktopSnapshotDocument.FromArchiveBytes(store.LatestSafetyCopy());
        Assert.Contains(safety.Items, item => item.Title == "下载途中修改");
        Assert.Contains(safety.Items, item => item.Title == "检查后修改");
        Assert.DoesNotContain(safety.Items, item => item.Title == "云端副本");
    }

    [Fact]
    public async Task ExpiredSessionRetainsLocalEditingAndNotifiesOnceUntilExplicitLogout()
    {
        var store = Store("http://127.0.0.1:1", expired: true);
        var workspace = new DesktopWorkspace(store); var notifications = 0;
        workspace.LoginRequired += () => notifications++;
        await workspace.SyncAsync(); await workspace.SyncAsync();
        Assert.Equal(1, notifications); Assert.True(workspace.SessionExpired); Assert.NotNull(workspace.Document);
        workspace.Edit(document => document.SaveItem(NativeWorkspaceTests.NewItem(false, "离线修改")));
        Assert.True(store.Load()!.Value.State.Dirty);
        Assert.Contains(new DesktopWorkspace(store).Document!.Items, item => item.Title == "离线修改");
        await workspace.LogoutAsync();
        Assert.Null(new DesktopWorkspace(store).Document);
        Assert.Contains(store.Load()!.Value.Document.Items, item => item.Title == "离线修改");
    }

    [Fact]
    public void OverlappingEventsUseSeparateLanesAndAdjacentEventsReuseFullWidth()
    {
        var document = DesktopSnapshotDocument.CreateEmpty(); var start = DateTimeOffset.Now.ToUnixTimeMilliseconds();
        var a = document.SaveItem(NativeWorkspaceTests.NewItem(true, "A") with { StartAtMillis = start, EndAtMillis = start + 100 });
        var b = document.SaveItem(NativeWorkspaceTests.NewItem(true, "B") with { StartAtMillis = start + 50, EndAtMillis = start + 200 });
        var c = document.SaveItem(NativeWorkspaceTests.NewItem(true, "C") with { StartAtMillis = start + 100, EndAtMillis = start + 150 });
        var d = document.SaveItem(NativeWorkspaceTests.NewItem(true, "D") with { StartAtMillis = start + 200, EndAtMillis = start + 300 });
        var lanes = CalendarEventLayout.Arrange(document.Items, DateTime.Today);
        Assert.Equal(2, lanes[a].Columns); Assert.NotEqual(lanes[a].Lane, lanes[b].Lane);
        Assert.Equal(lanes[a].Lane, lanes[c].Lane); Assert.Equal(1, lanes[d].Columns);
    }

    private static DesktopSecureStore Store(string server, bool expired = false)
    {
        var store = new DesktopSecureStore(Path.Combine(Path.GetTempPath(), "PaykiTodo-tests", Guid.NewGuid().ToString("N")));
        var expiry = expired ? 1 : DateTimeOffset.UtcNow.ToUnixTimeSeconds() + 3600;
        store.Save(new CloudAccountState(server, "fixture", "fixture-device", "fixture-access", "fixture-refresh", expiry, expiry, "fixture-secret", 0, false), DesktopSnapshotDocument.CreateEmpty());
        return store;
    }

    private static async Task Until(Func<bool> check)
    {
        for (var i = 0; i < 200; i++) { if (check()) { await Task.Delay(50); return; } await Task.Delay(25); }
        throw new TimeoutException("Synchronization did not complete");
    }

    internal sealed class SnapshotServer : IDisposable
    {
        private readonly HttpListener listener = new();
        private readonly Task loop;
        public string Url { get; }
        public long Revision;
        public byte[]? Payload;
        public bool HoldUpload, HoldDownload;
        public TaskCompletionSource Entered { get; private set; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public TaskCompletionSource Release { get; private set; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public void ResetBarriers()
        {
            Entered = new(TaskCreationOptions.RunContinuationsAsynchronously);
            Release = new(TaskCreationOptions.RunContinuationsAsynchronously);
        }
        public SnapshotServer()
        {
            var socket = new TcpListener(IPAddress.Loopback, 0); socket.Start(); var port = ((IPEndPoint)socket.LocalEndpoint).Port; socket.Stop();
            Url = "http://127.0.0.1:" + port;
            listener.Prefixes.Add(Url + "/"); listener.Start(); loop = Run();
        }
        private async Task Run()
        {
            try { while (listener.IsListening) { var context = await listener.GetContextAsync(); _ = Handle(context); } }
            catch (Exception error) when (error is HttpListenerException or ObjectDisposedException) { }
        }
        private async Task Handle(HttpListenerContext context)
        {
            try {
                var request = context.Request; var response = context.Response; byte[] body;
                if (request.Url!.AbsolutePath.EndsWith("/status")) body = Encoding.UTF8.GetBytes("{\"revision\":" + Revision + "}");
                else if (request.HttpMethod == "PUT") {
                    using var stream = new MemoryStream(); await request.InputStream.CopyToAsync(stream);
                    if (HoldUpload) { Entered.TrySetResult(); await Release.Task.WaitAsync(TimeSpan.FromSeconds(15)); HoldUpload = false; }
                    if (request.Headers["If-Match"] != "\"" + Revision + "\"") { response.StatusCode = 409; body = Encoding.UTF8.GetBytes("{\"currentRevision\":" + Revision + "}"); }
                    else { Payload = stream.ToArray(); Revision++; body = Encoding.UTF8.GetBytes("{\"revision\":" + Revision + "}"); }
                } else {
                    if (HoldDownload) { Entered.TrySetResult(); await Release.Task.WaitAsync(TimeSpan.FromSeconds(15)); HoldDownload = false; }
                    body = Payload ?? Array.Empty<byte>(); response.Headers["X-Payki-Revision"] = Revision.ToString();
                }
                response.ContentLength64 = body.Length; await response.OutputStream.WriteAsync(body); response.Close();
            } catch { context.Response.Abort(); }
        }
        public void Dispose() { Release.TrySetResult(); listener.Close(); loop.GetAwaiter().GetResult(); }
    }
}
