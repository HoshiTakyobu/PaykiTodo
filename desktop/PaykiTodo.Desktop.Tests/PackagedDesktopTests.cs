using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.Reflection;
using System.Resources;
using System.Security.Cryptography;
using System.Text.Json;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class PackagedDesktopTests
{
    [PackagedFact]
    public void PackagedReaderAndGuideResourcesMatchTheCurrentBuild()
    {
        var executable = Environment.GetEnvironmentVariable("PAYKI_PACKAGED_EXE")!;
        var packaged = Assembly.LoadFile(Path.Combine(Path.GetDirectoryName(executable)!, "PaykiTodo.dll"));
        var source = typeof(DesktopWorkspace).Assembly;
        var evidence = new List<object>();
        foreach (var name in source.GetManifestResourceNames().Where(x => x.StartsWith("PaykiTodo.Content/", StringComparison.Ordinal))) {
            using var expected = source.GetManifestResourceStream(name)!;
            using var actual = packaged.GetManifestResourceStream(name)!;
            Assert.NotNull(actual);
            var hash = Convert.ToHexString(SHA256.HashData(expected));
            Assert.Equal(hash, Convert.ToHexString(SHA256.HashData(actual)));
            evidence.Add(new { Resource = name, SHA256 = hash });
        }
        using var expectedResources = new ResourceReader(source.GetManifestResourceStream("PaykiTodo.g.resources")!);
        using var actualResources = new ResourceReader(packaged.GetManifestResourceStream("PaykiTodo.g.resources")!);
        expectedResources.GetResourceData("assets/windows_user_guide.md", out var expectedType, out var expectedGuide);
        actualResources.GetResourceData("assets/windows_user_guide.md", out var actualType, out var actualGuide);
        Assert.Equal(expectedType, actualType); Assert.Equal(expectedGuide, actualGuide);
        evidence.Add(new { Resource = "assets/windows_user_guide.md", SHA256 = Convert.ToHexString(SHA256.HashData(actualGuide)) });
        File.WriteAllText(Path.Combine(Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE")!, "packaged-content-resources.json"), JsonSerializer.Serialize(evidence, new JsonSerializerOptions { WriteIndented = true }));
    }
    [PackagedFact]
    public void UpdatedExecutableConfirmsNonceBeforeNetworkWithIsolatedProfile()
    {
        using var isolated = new IsolatedDesktop();
        var executable = Environment.GetEnvironmentVariable("PAYKI_PACKAGED_EXE")!;
        var folder = Path.Combine(Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE")!, "updated-profile");
        var store = new DesktopSecureStore(folder); var document = DesktopSnapshotDocument.CreateEmpty();
        document.SaveItem(NativeWorkspaceTests.NewItem(false, "更新启动保留的独立任务")); store.SaveDocument(document);
        var updates = new DesktopUpdateStore(Path.GetDirectoryName(executable)!, Path.Combine(folder, "updates"));
        var nonce = Guid.NewGuid().ToString("N"); var version = typeof(DesktopWorkspace).Assembly.GetName().Version!.ToString(3);
        updates.Change(s => (s with { Pending = version, Attempt = version, Nonce = nonce }, true));
        var start = new ProcessStartInfo(executable) { UseShellExecute = false, CreateNoWindow = true, WindowStyle = ProcessWindowStyle.Hidden };
        start.ArgumentList.Add("--data-directory"); start.ArgumentList.Add(folder);
        start.Environment["PAYKI_UPDATE_SEED"] = Path.GetDirectoryName(executable)!;
        start.Environment["PAYKI_UPDATE_ROOT"] = updates.Root; start.Environment["PAYKI_UPDATE_NONCE"] = nonce;
        using var process = Process.Start(start)!;
        try {
            var deadline = DateTime.UtcNow.AddSeconds(20);
            while (!process.HasExited && updates.Read().Nonce == nonce && DateTime.UtcNow < deadline) Thread.Sleep(100);
            Assert.False(process.HasExited); Assert.Null(updates.Read().Nonce); Assert.Equal(version, updates.Read().Active);
            Assert.Equal("更新启动保留的独立任务", new DesktopWorkspace(store).Document!.Items.Single().Title);
            CloseToTrayThenEndTestSession(process);
        } finally { if (!process.HasExited) { process.Kill(true); process.WaitForExit(); } }
    }

    private sealed class PackagedFactAttribute : FactAttribute
    {
        public PackagedFactAttribute()
        {
            if (string.IsNullOrEmpty(Environment.GetEnvironmentVariable("PAYKI_PACKAGED_EXE")))
                Skip = "Supply -PackagedExe to Invoke-DesktopAcceptance.ps1 after building the distribution";
        }
    }

    [PackagedFact]
    public void BuiltExecutableOpensBrandedWindowWithIndependentData()
    {
        using var isolated = new IsolatedDesktop();
        var executable = Environment.GetEnvironmentVariable("PAYKI_PACKAGED_EXE")!;
        Assert.Equal(typeof(DesktopWorkspace).Assembly.GetName().Version!.ToString(), FileVersionInfo.GetVersionInfo(executable).FileVersion);
        var folder = Path.Combine(Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE")!, "packaged-profile");
        var store = new DesktopSecureStore(folder);
        var document = DesktopSnapshotDocument.CreateEmpty();
        document.SaveItem(NativeWorkspaceTests.NewItem(false, "成品 EXE 的独立测试任务"));
        store.SaveDocument(document);
        var start = new ProcessStartInfo(executable) { UseShellExecute = false, CreateNoWindow = true, WindowStyle = ProcessWindowStyle.Hidden };
        start.ArgumentList.Add("--data-directory"); start.ArgumentList.Add(folder);
        using var process = Process.Start(start)!;
        try {
            var deadline = DateTime.UtcNow.AddSeconds(20);
            while (process.MainWindowHandle == IntPtr.Zero && !process.HasExited && DateTime.UtcNow < deadline) {
                Thread.Sleep(100); process.Refresh();
            }
            Assert.False(process.HasExited, "Built EXE must remain running");
            Assert.NotEqual(IntPtr.Zero, process.MainWindowHandle);
            Assert.Equal("PaykiTodo", process.MainWindowTitle);
            Assert.NotEqual(IntPtr.Zero, SendMessage(process.MainWindowHandle, 0x007F, new IntPtr(1), IntPtr.Zero));
            Assert.Equal("成品 EXE 的独立测试任务", new DesktopWorkspace(store).Document!.Items.Single().Title);
            WriteDpiEvidence(process.MainWindowHandle, executable, "packaged-dpi.json");
            CloseToTrayThenEndTestSession(process);
            Assert.Equal(0, process.ExitCode);
            File.WriteAllText(Path.Combine(Path.GetDirectoryName(folder)!, "packaged-exe-result.txt"),
                $"PASS: self-contained {FileVersionInfo.GetVersionInfo(executable).FileVersion} opens PaykiTodo window with icon; independent profile preserved; main X hides with live process; scoped test-window session-end exits with code 0. No actual system logout or MSI installation.");
        } finally {
            if (!process.HasExited) { process.Kill(true); process.WaitForExit(); }
        }
    }

    private static void CloseToTrayThenEndTestSession(Process process)
    {
        process.Refresh(); var window = process.MainWindowHandle;
        Assert.NotEqual(IntPtr.Zero, window);
        Assert.NotEqual(IntPtr.Zero, SendMessage(window, 0x0011, IntPtr.Zero, IntPtr.Zero));
        SendMessage(window, 0x0016, IntPtr.Zero, IntPtr.Zero);
        Assert.True(process.CloseMainWindow());
        var deadline = DateTime.UtcNow.AddSeconds(5);
        while (!process.HasExited && IsWindowVisible(window) && DateTime.UtcNow < deadline) Thread.Sleep(50);
        Assert.False(process.HasExited, "Main X must keep the process and services alive");
        Assert.False(IsWindowVisible(window), "Main X must hide the same HWND");
        // These messages target only the isolated test application's window. They do not log off Windows.
        Assert.NotEqual(IntPtr.Zero, SendMessage(window, 0x0011, IntPtr.Zero, IntPtr.Zero));
        SendMessage(window, 0x0016, new IntPtr(1), IntPtr.Zero);
        Assert.True(process.WaitForExit(10000), "A controlled session end must bypass close-to-tray interception");
        Assert.Equal(0, process.ExitCode);
    }

    private static void WriteDpiEvidence(IntPtr window, string executable, string filename)
    {
        var context = GetWindowDpiAwarenessContext(window);
        var details = new { Executable = executable, Version = FileVersionInfo.GetVersionInfo(executable).FileVersion,
            WindowDpi = GetDpiForWindow(window), Awareness = GetAwarenessFromDpiAwarenessContext(GetWindowDpiAwarenessContext(window)),
            PerMonitorV2 = AreDpiAwarenessContextsEqual(context, new IntPtr(-4)),
            Scope = "Actual EXE window on the isolated Win32 desktop; no physical monitor movement" };
        File.WriteAllText(Path.Combine(Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE")!, filename),
            JsonSerializer.Serialize(details, new JsonSerializerOptions { WriteIndented = true }));
    }

    [BaselineFact]
    public void PreviousExecutableDpiEvidenceUsesItsOwnIsolatedProfile()
    {
        var executable = Environment.GetEnvironmentVariable("PAYKI_BASELINE_EXE")!;
        using var isolated = new IsolatedDesktop();
        var folder = Path.Combine(Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE")!, "baseline-exe-profile");
        new DesktopSecureStore(folder).SaveDocument(DesktopSnapshotDocument.CreateEmpty());
        var start = new ProcessStartInfo(executable) { UseShellExecute = false, CreateNoWindow = true, WindowStyle = ProcessWindowStyle.Hidden };
        start.ArgumentList.Add("--data-directory"); start.ArgumentList.Add(folder);
        using var process = Process.Start(start)!;
        try {
            var deadline = DateTime.UtcNow.AddSeconds(20);
            while (process.MainWindowHandle == IntPtr.Zero && !process.HasExited && DateTime.UtcNow < deadline) { Thread.Sleep(100); process.Refresh(); }
            Assert.False(process.HasExited); Assert.NotEqual(IntPtr.Zero, process.MainWindowHandle);
            WriteDpiEvidence(process.MainWindowHandle, executable, "baseline-exe-dpi.json");
            Assert.True(process.CloseMainWindow());
            // Older baselines exit on X; newer baselines intentionally stay in their tray.
            // This case measures DPI only, so end just this isolated fixture process if needed.
            if (!process.WaitForExit(1500)) { process.Kill(true); process.WaitForExit(); }
        } finally { if (!process.HasExited) { process.Kill(true); process.WaitForExit(); } }
    }

    private sealed class BaselineFactAttribute : FactAttribute
    {
        public BaselineFactAttribute() { if (string.IsNullOrEmpty(Environment.GetEnvironmentVariable("PAYKI_BASELINE_EXE"))) Skip = "Supply -BaselineExe for actual previous-EXE DPI evidence"; }
    }

    [DllImport("user32.dll")] private static extern IntPtr SendMessage(IntPtr window, uint message, IntPtr wParam, IntPtr lParam);
    [DllImport("user32.dll")] private static extern bool IsWindowVisible(IntPtr window);
    [DllImport("user32.dll")] private static extern uint GetDpiForWindow(IntPtr window);
    [DllImport("user32.dll")] private static extern IntPtr GetWindowDpiAwarenessContext(IntPtr window);
    [DllImport("user32.dll")] private static extern int GetAwarenessFromDpiAwarenessContext(IntPtr context);
    [DllImport("user32.dll")] private static extern bool AreDpiAwarenessContextsEqual(IntPtr first, IntPtr second);
}
