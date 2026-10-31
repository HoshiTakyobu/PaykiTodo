using System.Windows.Forms;

namespace PaykiTodo.Desktop;

internal static class Program
{
    [STAThread]
    private static void Main(string[] args)
    {
        var launcher = Path.Combine(AppContext.BaseDirectory, "PaykiTodo.Launcher.exe");
        if (args.Length == 0 && string.IsNullOrEmpty(Environment.GetEnvironmentVariable("PAYKI_UPDATE_SEED")) && File.Exists(launcher)) {
            System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(launcher) { UseShellExecute = false, CreateNoWindow = true });
            return;
        }
        ApplicationConfiguration.Initialize();
        var app = new System.Windows.Application();
        string? directory = null;
        if (args.Length == 2 && args[0] == "--data-directory") directory = Path.GetFullPath(args[1]);
        else if (args.Length != 0) throw new ArgumentException("Usage: PaykiTodo.exe [--data-directory <directory>]");
        if (directory != null) DesktopDiagnosticLog.DirectoryPath = directory;
        app.Run(new NativeWindow(profileDirectory: directory));
    }
}
