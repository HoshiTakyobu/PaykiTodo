using System.IO;
using System.Runtime.CompilerServices;

namespace PaykiTodo.Desktop.Tests;

internal static class TestDiagnostics
{
    [ModuleInitializer]
    internal static void Initialize()
    {
        DesktopDiagnosticLog.DirectoryPath = Path.Combine(
            Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE") ?? Path.GetTempPath(),
            "test-diagnostics-" + Guid.NewGuid().ToString("N"));
    }
}
