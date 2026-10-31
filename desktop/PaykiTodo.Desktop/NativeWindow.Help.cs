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
        var chapters = reader.ReadToEnd().Replace("\r\n", "\n").Split("\n## ").Skip(1)
            .Select(chapter => chapter.Split('\n', 2)).ToArray();
        var dialog = Dialog(this, "PaykiTodo 使用说明", 780, 720);
        var selector = new ComboBox { ItemsSource = chapters.Select(chapter => chapter[0]).ToArray(), SelectedIndex = 0 };
        AutomationProperties.SetName(selector, "使用说明章节");
        var heading = Text("", 22, bold: true);
        var body = Text("", 15); body.Margin = new Thickness(0, 12, 0, 12);
        var content = Scroll(Column(heading, body));
        void ShowChapter() {
            var chapter = chapters[Math.Max(0, selector.SelectedIndex)];
            heading.Text = chapter[0]; body.Text = chapter[1].Trim(); content.ScrollToTop();
        }
        selector.SelectionChanged += (_, _) => ShowChapter(); ShowChapter();
        var layout = new DockPanel { Margin = new Thickness(24) };
        var close = Action("关闭", () => dialog.Close()); close.IsCancel = true;
        var footer = Row(close); footer.HorizontalAlignment = HorizontalAlignment.Right;
        DockPanel.SetDock(footer, Dock.Bottom); layout.Children.Add(footer);
        DockPanel.SetDock(selector, Dock.Top); layout.Children.Add(selector); layout.Children.Add(content);
        dialog.Content = layout; dialog.ShowDialog();
    }
}
