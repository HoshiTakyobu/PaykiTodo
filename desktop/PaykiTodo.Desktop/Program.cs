using System.Windows.Forms;

namespace PaykiTodo.Desktop;

internal static class Program
{
    [STAThread]
    private static void Main()
    {
        ApplicationConfiguration.Initialize();
        var app = new System.Windows.Application();
        app.Run(new NativeWindow());
    }
}
