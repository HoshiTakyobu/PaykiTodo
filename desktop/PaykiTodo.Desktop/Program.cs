using System.Windows.Forms;

namespace PaykiTodo.Desktop;

internal static class Program
{
    [STAThread]
    private static void Main(string[] args)
    {
        ApplicationConfiguration.Initialize();
        var app = new System.Windows.Application();
        string? directory = null;
        if (args.Length == 2 && args[0] == "--data-directory") directory = Path.GetFullPath(args[1]);
        else if (args.Length != 0) throw new ArgumentException("Usage: PaykiTodo.exe [--data-directory <directory>]");
        if (directory != null) DesktopDiagnosticLog.DirectoryPath = directory;
        app.Run(new NativeWindow(profileDirectory: directory));
    }
}
