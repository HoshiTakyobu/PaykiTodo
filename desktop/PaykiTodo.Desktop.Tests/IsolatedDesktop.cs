using System.ComponentModel;
using System.Runtime.InteropServices;

namespace PaykiTodo.Desktop.Tests;

// The launcher binds the whole process before .NET/COM can create hidden HWNDs.
internal sealed class IsolatedDesktop : IDisposable
{
    public IsolatedDesktop()
    {
        var expected = Environment.GetEnvironmentVariable("PAYKI_ISOLATED_DESKTOP");
        var name = new System.Text.StringBuilder(256);
        if (!GetUserObjectInformation(GetThreadDesktop(GetCurrentThreadId()), 2, name, 512, out _))
            throw new Win32Exception(Marshal.GetLastWin32Error());
        if (string.IsNullOrEmpty(expected) || name.ToString() != expected || expected == "Default")
            throw new InvalidOperationException("UI tests require an isolated process desktop; use tools/tests/Invoke-DesktopAcceptance.ps1");
        var input = OpenInputDesktop(0, false, 1);
        if (input == IntPtr.Zero) throw new Win32Exception(Marshal.GetLastWin32Error());
        try {
            var inputName = new System.Text.StringBuilder(256);
            if (!GetUserObjectInformation(input, 2, inputName, 512, out _)) throw new Win32Exception(Marshal.GetLastWin32Error());
            if (inputName.ToString() == expected) throw new InvalidOperationException("The UI test desktop must not be the user's input desktop");
        } finally { CloseDesktop(input); }
    }
    public void Dispose() { }
    [DllImport("kernel32.dll")] private static extern uint GetCurrentThreadId();
    [DllImport("user32.dll")] private static extern IntPtr GetThreadDesktop(uint thread);
    [DllImport("user32.dll", SetLastError = true)] private static extern IntPtr OpenInputDesktop(uint flags, bool inherit, uint access);
    [DllImport("user32.dll")] private static extern bool CloseDesktop(IntPtr handle);
    [DllImport("user32.dll", EntryPoint = "GetUserObjectInformationW", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern bool GetUserObjectInformation(IntPtr handle, int index, System.Text.StringBuilder value, int length, out int needed);
}

internal sealed class IsolatedUiFactAttribute : Xunit.FactAttribute
{
    public IsolatedUiFactAttribute()
    {
        if (string.IsNullOrEmpty(Environment.GetEnvironmentVariable("PAYKI_ISOLATED_DESKTOP")))
            Skip = "Use tools/tests/Invoke-DesktopAcceptance.ps1 to render windows without taking focus";
    }
}
