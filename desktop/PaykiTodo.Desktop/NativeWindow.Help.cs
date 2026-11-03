using System.Windows;
using System.Windows.Controls;
using System.Windows.Automation;
using static PaykiTodo.Desktop.NativeUi;
using ComboBox = System.Windows.Controls.ComboBox;
using HorizontalAlignment = System.Windows.HorizontalAlignment;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow
{
    private void ShowUsageGuide()
    {
        var resource = System.Windows.Application.GetResourceStream(new Uri("pack://application:,,,/PaykiTodo;component/Assets/WINDOWS_USER_GUIDE.md"));
        using var reader = new StreamReader(resource.Stream);
        var markdown = reader.ReadToEnd();
        var dialog = Dialog(this, "PaykiTodo 使用说明", 780, 720);
        var content = new NativeContentReader(markdown, profileDirectory, true, "PaykiTodo 使用说明");
        var layout = new DockPanel { Margin = new Thickness(24) };
        var close = Action("关闭", () => dialog.Close()); close.IsCancel = true;
        var footer = Row(close); footer.HorizontalAlignment = HorizontalAlignment.Right;
        DockPanel.SetDock(footer, Dock.Bottom); layout.Children.Add(footer);
        layout.Children.Add(content);
        dialog.Content = layout; dialog.ShowDialog();
    }
}
