using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class SyncReliabilityTests
{
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
        await Until(() => workspace.Account?.Revision == 1);
        Assert.True(workspace.Account!.Dirty);
        Assert.Equal(2, store.Load()!.Value.Document.Items.Count);
        Assert.Single(DesktopSnapshotDocument.FromArchiveBytes(SnapshotCrypto.Decrypt(server.Payload!, "fixture-secret")).Items);
        await workspace.SyncAsync();
        Assert.False(workspace.Account.Dirty);
        Assert.Equal(2, DesktopSnapshotDocument.FromArchiveBytes(SnapshotCrypto.Decrypt(server.Payload!, "fixture-secret")).Items.Count);
        var active = workspace.Document;
        await workspace.SyncAsync();
        Assert.Same(active, workspace.Document);
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

    private sealed class SnapshotServer : IDisposable
    {
        private readonly HttpListener listener = new();
        private readonly Task loop;
        public string Url { get; }
        public long Revision;
        public byte[]? Payload;
        public bool HoldUpload, HoldDownload;
        public TaskCompletionSource Entered { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public TaskCompletionSource Release { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
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
