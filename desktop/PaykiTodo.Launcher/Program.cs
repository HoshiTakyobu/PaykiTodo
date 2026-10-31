using System.Diagnostics;
using System.Text;
using PaykiTodo.Desktop;

var seed = AppContext.BaseDirectory.TrimEnd(Path.DirectorySeparatorChar);
var isolatedRoot = args.Length == 2 && args[0] == "--data-directory" ? Path.Combine(Path.GetFullPath(args[1]), "updates") : null;
var store = new DesktopUpdateStore(seed, isolatedRoot);
using var instance = new Mutex(false, "Local\\PaykiTodoLaunch-" + DesktopUpdateProtocol.Hash(Encoding.UTF8.GetBytes(store.Root)));
try { if (!instance.WaitOne(0)) return; } catch (AbandonedMutexException) { }
try {
    var seedExe = Path.Combine(seed, "PaykiTodo.exe");
    var seedVersion = DesktopUpdateProtocol.ParseVersion(FileVersionInfo.GetVersionInfo(seedExe).FileVersion is { } version ? string.Join('.', version.Split('.').Take(3)) : "0.0.0");
    var launch = store.BeginLaunch(seedVersion, DesktopUpdateProtocol.PublicKey());
    Process Start(UpdateLaunch selection) {
        var info = new ProcessStartInfo(Path.Combine(selection.Directory, "PaykiTodo.exe")) { UseShellExecute = false, WorkingDirectory = seed, CreateNoWindow = true };
        foreach (var arg in args) info.ArgumentList.Add(arg);
        info.Environment["PAYKI_UPDATE_SEED"] = seed;
        info.Environment["PAYKI_UPDATE_ROOT"] = store.Root;
        info.Environment["PAYKI_UPDATE_NONCE"] = selection.Nonce ?? "";
        return Process.Start(info) ?? throw new IOException("无法启动 PaykiTodo");
    }
    Process? process = null;
    try {
        process = Start(launch);
        if (launch.Nonce != null) {
            var deadline = DateTime.UtcNow.AddSeconds(45);
            while (!process.HasExited && DateTime.UtcNow < deadline && store.Read().Nonce == launch.Nonce) Thread.Sleep(200);
            if (store.Read().Nonce == launch.Nonce) {
                if (!process.HasExited) process.Kill(entireProcessTree: true);
                store.Fail(launch.Nonce); process.Dispose(); process = Start(store.BeginLaunch(seedVersion, DesktopUpdateProtocol.PublicKey()));
            }
        }
        process.WaitForExit();
    } catch (Exception) when (launch.Nonce != null) {
        if (process is { HasExited: false }) process.Kill(entireProcessTree: true);
        store.Fail(launch.Nonce);
        using var fallback = Start(new UpdateLaunch(seed, null, null)); fallback.WaitForExit();
    } finally { process?.Dispose(); }
} catch (Exception error) {
    // Keep diagnostics separate from accounts and business records.
    File.AppendAllText(Path.Combine(store.Root, "launcher-errors.log"), DateTimeOffset.UtcNow.ToString("O") + " " + error.GetType().Name + Environment.NewLine);
} finally { instance.ReleaseMutex(); }
