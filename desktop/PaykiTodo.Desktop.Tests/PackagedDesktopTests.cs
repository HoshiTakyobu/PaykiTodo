using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class PackagedDesktopTests
{
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
            Assert.True(process.CloseMainWindow());
            Assert.True(process.WaitForExit(10000), "Built EXE must close normally");
            Assert.Equal(0, process.ExitCode);
            File.WriteAllText(Path.Combine(Path.GetDirectoryName(folder)!, "packaged-exe-result.txt"),
                $"PASS: self-contained {FileVersionInfo.GetVersionInfo(executable).FileVersion} opens PaykiTodo window with icon; independent profile preserved; normal exit. Startup smoke only, not a second full UI flow.");
        } finally {
            if (!process.HasExited) { process.Kill(true); process.WaitForExit(); }
        }
    }

    [DllImport("user32.dll")] private static extern IntPtr SendMessage(IntPtr window, uint message, IntPtr wParam, IntPtr lParam);
}
