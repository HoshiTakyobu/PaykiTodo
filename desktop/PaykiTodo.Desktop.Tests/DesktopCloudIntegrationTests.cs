using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Sockets;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class DesktopCloudIntegrationTests
{
    [Fact]
    public async Task RealServerRegistrationTwoDeviceSyncProfileAndPasswordRotation()
    {
        var directory = Path.Combine(Path.GetTempPath(), "PaykiTodo-tests", Guid.NewGuid().ToString("N")); Directory.CreateDirectory(directory);
        var socket = new TcpListener(IPAddress.Loopback, 0); socket.Start(); var port = ((IPEndPoint)socket.LocalEndpoint).Port; socket.Stop();
        var script = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "../../../../../server/paykitodo_cloud_server.py"));
        var start = new ProcessStartInfo("python") { UseShellExecute = false, CreateNoWindow = true, RedirectStandardError = true, RedirectStandardOutput = true };
        start.ArgumentList.Add(script); start.Environment["PAYKITODO_CLOUD_DB"] = Path.Combine(directory, "fixture.sqlite3");
        start.Environment["PAYKITODO_CLOUD_HOST"] = "127.0.0.1"; start.Environment["PAYKITODO_CLOUD_PORT"] = port.ToString();
        start.Environment["PAYKITODO_TURNSTILE_SITE_KEY"] = ""; start.Environment["PAYKITODO_TURNSTILE_SECRET_KEY"] = "";
        start.Environment["PAYKITODO_PASSWORD_PEPPER"] = "local-test-only";
        using var server = Process.Start(start) ?? throw new InvalidOperationException("Could not start test server");
        server.BeginErrorReadLine(); server.BeginOutputReadLine();
        var url = "http://127.0.0.1:" + port;
        try {
            using var client = new HttpClient { Timeout = TimeSpan.FromSeconds(2) };
            await Until(async () => { try { return (await client.GetAsync(url + "/healthz")).IsSuccessStatusCode; } catch { return false; } });
            var username = "native_fixture"; const string password = "test-password-1234";
            var first = new DesktopWorkspace(new DesktopSecureStore(Path.Combine(directory, "first")));
            await first.LoginAsync(url, username, password, true, null);
            Assert.NotNull(first.Account); Assert.NotNull(first.Document);
            first.Edit(document => document.SaveItem(NativeWorkspaceTests.NewItem(false, "只存在于测试中的待办")));
            await Until(() => Task.FromResult(first.Account?.Dirty == false));
            var second = new DesktopWorkspace(new DesktopSecureStore(Path.Combine(directory, "second")));
            await second.LoginAsync(url, username, password, false, null);
            var item = Assert.Single(second.Document!.Items); Assert.Equal("只存在于测试中的待办", item.Title);
            second.Edit(document => document.Complete(item.Id));
            await Until(() => Task.FromResult(second.Account?.Dirty == false));
            await first.SyncAsync(); Assert.True(first.Document!.Items.Single().Completed);
            await first.AccountRequestAsync("/v2/account", HttpMethod.Post, new { displayName = "原生端验收" });
            var profile = await second.AccountRequestAsync("/v2/account", HttpMethod.Get);
            Assert.Equal("原生端验收", profile.GetProperty("user").GetProperty("displayName").GetString());
            var devices = await first.AccountRequestAsync("/v2/devices", HttpMethod.Get); Assert.Equal(2, devices.GetProperty("devices").GetArrayLength());
            await second.ChangePasswordAsync(password, "changed-test-password-5678"); Assert.Null(second.Document);
            await second.LoginAsync(url, username, "changed-test-password-5678", false, null);
            Assert.True(second.Document!.Items.Single().Completed);
            await second.LogoutAsync(); Assert.Null(second.Document);
        } finally { if (!server.HasExited) { server.Kill(entireProcessTree: true); await server.WaitForExitAsync(); } }
    }
    private static async Task Until(Func<Task<bool>> check)
    {
        for (var attempt = 0; attempt < 100; attempt++) { if (await check()) return; await Task.Delay(100); }
        throw new TimeoutException("Local integration operation did not complete");
    }
}
