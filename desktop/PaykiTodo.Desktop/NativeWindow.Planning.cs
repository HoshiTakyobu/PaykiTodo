using System.Text.Json.Nodes;
using System.Windows;
using System.Windows.Controls;
using static PaykiTodo.Desktop.NativeUi;
using TextBox = System.Windows.Controls.TextBox;
using HorizontalAlignment = System.Windows.HorizontalAlignment;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow
{
    private UIElement LegacyPlanningEditorPage()
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
            var importImage = Action("从图片提取文字", async () => {
                var picker = new Microsoft.Win32.OpenFileDialog { Filter = "图片|*.png;*.jpg;*.jpeg;*.bmp;*.tif;*.tiff", Title = "选择包含规划内容的图片" };
                if (picker.ShowDialog(this) != true) return;
                var account = workspace.AccountGeneration;
                error.Text = "正在本机识别图片…";
                try {
                    var text = await DesktopImageText.ReadAsync(picker.FileName);
                    if (!markdown.IsLoaded || account != workspace.AccountGeneration) return;
                    markdown.AppendText((markdown.Text.Length > 0 ? Environment.NewLine : "") + text);
                    error.Text = "已加入草稿，请检查文字后识别事项或完成保存。";
                } catch (Exception failure) { if (markdown.IsLoaded) error.Text = failure is InvalidOperationException ? failure.Message : "图片读取失败，请选择清晰的 PNG 或 JPG 图片。"; }
            });
            var buttons = Row(Action("取消", () => { planningDrafts.Remove(id); Navigate("规划台"); }), Action("识别并预览", () => RecognizePlanning(markdown.Text)), Action("完成", () => {
                if (string.IsNullOrWhiteSpace(name.Text)) { error.Text = "请填写文档标题"; return; }
                try { workspace.Edit(document => document.SavePlanningNote(note?.Id, name.Text, markdown.Text)); planningDrafts.Remove(id); editingPlanning = false; Render(); }
                catch (Exception failure) { error.Text = failure.Message; }
            }, true));
            buttons.HorizontalAlignment = HorizontalAlignment.Right;
            var layout = new DockPanel();
            var footer = new Border { Child = buttons, BorderBrush = B("#CBD8D2"), BorderThickness = new Thickness(0, 1, 0, 0), Padding = new Thickness(0, 16, 0, 0) };
            DockPanel.SetDock(footer, Dock.Bottom); layout.Children.Add(footer);
            layout.Children.Add(Scroll(Column(BrandHeading("规划文档", "跨页面保留草稿 · 点击完成才保存"), Text("文档名称", 13, Muted), name, Text("内容", 13, Muted), markdown, importImage, error)));
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
        body.Children.Add(Text("日报从生成时刻起保留 168 小时，到期自动移除。周报沿用保留设置。", 13, Muted));
        var reports = workspace.Document!.Reports.OrderByDescending(report => report["generatedAtMillis"]?.GetValue<long>() ?? 0).ToList();
        foreach (var report in reports) {
            var heading = (report["type"]?.GetValue<string>() == "WEEKLY" ? "每周回顾" : "每日回顾") + " · " + Local(report["generatedAtMillis"]?.GetValue<long>()).ToString("yyyy-MM-dd HH:mm");
            var text = report["content"]?.GetValue<string>() ?? report["contentMarkdown"]?.GetValue<string>() ?? report["reportText"]?.GetValue<string>() ?? "";
            var summary = NativeMarkdownPreview.Create(text);
            var source = report["providerName"]?.GetValue<string>() ?? "未知来源";
            var read = QuietAction("阅读报告", () => OpenReport(report, heading)); read.Foreground = Ocean;
            read.Margin = new Thickness(16, 0, 0, 0); read.VerticalAlignment = VerticalAlignment.Center;
            var row = new Grid(); row.ColumnDefinitions.Add(new ColumnDefinition()); row.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            row.Children.Add(Column(Text(heading, 18, Ink, true), Text("来源：" + source, 12, Muted), summary));
            Grid.SetColumn(read, 1); row.Children.Add(read);
            var card = Card(row, 18); card.CornerRadius = new CornerRadius(12); body.Children.Add(card);
        }
        if (reports.Count == 0) body.Children.Add(Card(Text("暂无报告。手机生成的报告会随账号数据同步到这里。", 15, Muted)));
        return Page(body);
    }
    private void OpenReport(JsonObject report, string heading) {
        var account = workspace.AccountGeneration;
        var id = report["id"]?.ToJsonString(); var generated = report["generatedAtMillis"]?.ToJsonString();
        var raw = report["content"]?.GetValue<string>() ?? report["contentMarkdown"]?.GetValue<string>() ?? report["reportText"]?.GetValue<string>() ?? "";
        var dialog = Dialog(this, heading, 860, 740);
        var content = new NativeContentReader(raw, profileDirectory);
        var layout = new DockPanel { Margin = new Thickness(16) };
        var close = Action("关闭", () => dialog.Close()); close.IsCancel = true;
        var actions = Row(Action("复制原文", () => System.Windows.Clipboard.SetText(raw)), close);
        actions.Children.Insert(1, Action("删除报告", () => {
            if (!Confirmation(dialog, "删除这份报告？删除结果会随账号数据同步。")) return;
            workspace.Edit(value => value.DeleteReport(id, generated, raw));
        }));
        DockPanel.SetDock(actions, Dock.Top); layout.Children.Add(actions); layout.Children.Add(content); dialog.Content = layout;
        var metadata = Column(Text("来源：" + (report["providerName"]?.GetValue<string>() ?? "未知来源") + (report["isLocalFallback"]?.GetValue<bool>() == true ? " · 本地模板" : ""), 13, Muted));
        if (report["periodStartMillis"]?.GetValue<long>() is { } start && report["periodEndMillis"]?.GetValue<long>() is { } end)
            metadata.Children.Add(Text($"覆盖周期：{Local(start):yyyy-MM-dd} – {Local(end):yyyy-MM-dd}", 13, Muted));
        DockPanel.SetDock(metadata, Dock.Top); layout.Children.Insert(1, metadata);
        void Changed() {
            if (workspace.AccountGeneration != account || workspace.Document?.Reports.Any(value => value["id"]?.ToJsonString() == id && value["generatedAtMillis"]?.ToJsonString() == generated && ReportContent(value) == raw) != true)
                dialog.Dispatcher.Invoke(() => dialog.Close());
        }
        workspace.Changed += Changed;
        try { dialog.ShowDialog(); } finally { workspace.Changed -= Changed; content.Dispose(); }
    }
    private static string ReportContent(JsonObject report) => report["content"]?.GetValue<string>() ?? report["contentMarkdown"]?.GetValue<string>() ?? report["reportText"]?.GetValue<string>() ?? "";
}
