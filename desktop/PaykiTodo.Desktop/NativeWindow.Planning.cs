using System.Text.Json.Nodes;
using System.Windows;
using System.Windows.Controls;
using static PaykiTodo.Desktop.NativeUi;
using TextBox = System.Windows.Controls.TextBox;
using HorizontalAlignment = System.Windows.HorizontalAlignment;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow
{
    private UIElement PlanningPage()
    {
        var grid = new Grid(); grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(200) }); grid.ColumnDefinitions.Add(new ColumnDefinition());
        var list = Column(); var editor = new ContentControl { Margin = new Thickness(22, 0, 0, 0) }; Grid.SetColumn(editor, 1); grid.Children.Add(editor); grid.Children.Add(Scroll(list));
        void Open(DesktopPlanningNote? note)
        {
            editingPlanning = true;
            var id = note?.Id ?? 0;
            var draft = planningDrafts.GetValueOrDefault(id, (note?.Title ?? "新规划", note?.Markdown ?? ""));
            var name = Input(draft.Item1); var markdown = Input(draft.Item2, true); markdown.Height = 320;
            name.TextChanged += (_, _) => planningDrafts[id] = (name.Text, markdown.Text);
            markdown.TextChanged += (_, _) => planningDrafts[id] = (name.Text, markdown.Text);
            var error = Text("", 13, B("#AF3A32"));
            var buttons = Row(Action("取消", () => { planningDrafts.Remove(id); Navigate("规划台"); }), Action("识别并预览", () => RecognizePlanning(markdown.Text)), Action("完成", () => {
                if (string.IsNullOrWhiteSpace(name.Text)) { error.Text = "请填写文档标题"; return; }
                try { workspace.Edit(document => document.SavePlanningNote(note?.Id, name.Text, markdown.Text)); planningDrafts.Remove(id); editingPlanning = false; Render(); }
                catch (Exception failure) { error.Text = failure.Message; }
            }, true));
            buttons.HorizontalAlignment = HorizontalAlignment.Right;
            var layout = new DockPanel();
            var footer = new Border { Child = buttons, BorderBrush = B("#CBD8D2"), BorderThickness = new Thickness(0, 1, 0, 0), Padding = new Thickness(0, 16, 0, 0) };
            DockPanel.SetDock(footer, Dock.Bottom); layout.Children.Add(footer);
            layout.Children.Add(Scroll(Column(BrandHeading("规划文档", "跨页面保留草稿 · 点击完成才保存"), Text("文档名称", 13, Muted), name, Text("内容", 13, Muted), markdown, error)));
            editor.Content = Card(layout);
        }
        list.Children.Add(Action("＋ 新建文档", () => Open(null), true));
        if (planningDrafts.ContainsKey(0)) list.Children.Add(Action("继续新文档草稿", () => Open(null)));
        var notes = workspace.Document!.PlanningNotes.Where(note => !workspace.Document.PlanningArchived(note.Id)).ToList();
        foreach (var note in notes) {
            var open = Action(note.Title + (planningDrafts.ContainsKey(note.Id) ? " · 草稿" : ""), () => Open(note));
            open.Content = Text(note.Title + (planningDrafts.ContainsKey(note.Id) ? " · 草稿" : ""), 14, bold: true); open.HorizontalContentAlignment = HorizontalAlignment.Left;
            list.Children.Add(Card(Column(open, Action("归档", () => { if (Confirm("归档这份规划文档？")) { editingPlanning = false; Change(document => document.ArchivePlanning(note.Id)); } })), 10));
        }
        editor.Content = Card(Column(Text("想法，从这里开始", 25, Forest, true), Text("选择左侧文档，或新建一份规划。完成后会保存到本机并自动同步。", 15, Muted)));
        return Page(grid, scroll: false);
    }
    private UIElement ReportsPage()
    {
        var body = Column(Row(Action("生成日报", () => GenerateReport(false), true), Action("生成周报", () => GenerateReport(true)), Action("AI 服务配置", AiSettings)));
        var reports = workspace.Document!.Reports.OrderByDescending(report => report["generatedAtMillis"]?.GetValue<long>() ?? 0).ToList();
        foreach (var report in reports) {
            var heading = (report["type"]?.GetValue<string>() == "WEEKLY" ? "每周回顾" : "每日回顾") + " · " + Local(report["generatedAtMillis"]?.GetValue<long>()).ToString("yyyy-MM-dd HH:mm");
            var text = report["content"]?.GetValue<string>() ?? report["contentMarkdown"]?.GetValue<string>() ?? report["reportText"]?.GetValue<string>() ?? "";
            body.Children.Add(Card(Column(Text(heading, 20, Forest, true), Text(text))));
        }
        if (reports.Count == 0) body.Children.Add(Card(Text("暂无报告。手机生成的报告会随账号数据同步到这里。", 15, Muted)));
        return Page(body);
    }
}
