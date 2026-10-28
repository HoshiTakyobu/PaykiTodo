using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Net.Http.Json;
using System.Text.Json;
using System.Windows.Automation;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class LiveAndroidSyncTests
{
    private sealed class LiveFixtureFactAttribute : FactAttribute
    {
        public LiveFixtureFactAttribute()
        {
            if (Environment.GetEnvironmentVariable("PAYKI_LIVE_ANDROID_FIXTURE") != "1" ||
                string.IsNullOrEmpty(Environment.GetEnvironmentVariable("PAYKI_PACKAGED_EXE")))
                Skip = "Requires the concurrent Android loopback fixture and isolated packaged EXE.";
        }
    }

    [LiveFixtureFact]
    public async Task OpenPackagedWindowRefreshesAfterAndroidCompletionWithoutManualSync()
    {
        using var desktop = new IsolatedDesktop();
        var evidence = Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE")!;
        using var http = new HttpClient { BaseAddress = new Uri("http://127.0.0.1:19444"), Timeout = TimeSpan.FromSeconds(10) };
        string username = "";
        await Until(async () => {
            var state = await http.GetFromJsonAsync<JsonElement>("/fixture/state");
            username = state.GetProperty("liveUsername").GetString() ?? "";
            return state.GetProperty("livePhase").GetString() == "android-ready" && username.Length > 0;
        }, 75);
        var folder = Path.Combine(evidence, "live-packaged-profile-" + Guid.NewGuid().ToString("N"));
        var store = new DesktopSecureStore(folder);
        var seed = new DesktopWorkspace(store);
        await seed.LoginAsync("http://127.0.0.1:19444", username, "local-fixture-password-1234", false, null);
        Assert.False(seed.Document!.Items.Single(item => item.Title == "live completion propagation").Completed);
        var start = new ProcessStartInfo(Environment.GetEnvironmentVariable("PAYKI_PACKAGED_EXE")!) {
            UseShellExecute = false, CreateNoWindow = true, WindowStyle = ProcessWindowStyle.Hidden
        };
        start.ArgumentList.Add("--data-directory"); start.ArgumentList.Add(folder);
        using var process = Process.Start(start)!;
        try {
            await Until(() => { process.Refresh(); Assert.False(process.HasExited); return Task.FromResult(process.MainWindowHandle != IntPtr.Zero); }, 20);
            var window = AutomationElement.FromHandle(process.MainWindowHandle);
            bool Has(string title) => window.FindFirst(TreeScope.Descendants,
                new PropertyCondition(AutomationElement.NameProperty, "完成待办：" + title)) != null;
            await Until(() => Task.FromResult(Has("live completion propagation") && Has("live untouched task")), 20);
            // No workspace.SyncAsync call or UI action after this point. The
            // already-running packaged window's normal timer receives the edit.
            var elapsed = Stopwatch.StartNew();
            using (var content = new StringContent("{\"release\":true,\"livePhase\":\"windows-ready\"}", System.Text.Encoding.UTF8, "application/json"))
            using (var response = await http.PostAsync("/fixture/control", content))
                response.EnsureSuccessStatusCode();
            await Until(() => Task.FromResult(!Has("live completion propagation") && Has("live untouched task")), 20);
            var visibleMillis = elapsed.ElapsedMilliseconds;
            var restored = new DesktopWorkspace(new DesktopSecureStore(folder));
            Assert.True(restored.Document!.Items.Single(item => item.Title == "live completion propagation").Completed);
            Assert.False(restored.Document.Items.Single(item => item.Title == "live untouched task").Completed);
            Assert.False(restored.Account!.Dirty);
            using (var content = new StringContent("{\"release\":true,\"livePhase\":\"windows-completed\"}", System.Text.Encoding.UTF8, "application/json"))
            using (var response = await http.PostAsync("/fixture/control", content))
                response.EnsureSuccessStatusCode();
            Assert.True(process.CloseMainWindow());
            Assert.True(process.WaitForExit(10_000));
            Assert.Equal(0, process.ExitCode);
            File.WriteAllText(Path.Combine(evidence, "live-sync-result.json"), JsonSerializer.Serialize(new {
                passed = true, packageVersion = FileVersionInfo.GetVersionInfo(start.FileName).FileVersion,
                visibleMillis, manualSync = false, windowsWasAlreadyOpen = true,
                completedTaskRemoved = true, otherTaskRetained = true, persisted = true,
                measurement = "Windows readiness signal through Android ViewModel completion/upload and automatic Windows visible refresh"
            }, new JsonSerializerOptions { WriteIndented = true }));
        } finally {
            if (!process.HasExited) { process.Kill(true); process.WaitForExit(); }
        }
    }

    private static async Task Until(Func<Task<bool>> predicate, int seconds)
    {
        var timer = Stopwatch.StartNew();
        while (timer.Elapsed < TimeSpan.FromSeconds(seconds)) {
            if (await predicate()) return;
            await Task.Delay(100);
        }
        throw new TimeoutException("Live Android/Windows fixture did not reach the expected state.");
    }
}
