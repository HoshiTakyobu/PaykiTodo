using System.ComponentModel;
using System.Text;

namespace PaykiTodo.Desktop;

internal sealed class QuickChecklistEditorDialog : Form
{
    private readonly TextBox input = new()
    {
        Multiline = true,
        AcceptsReturn = true,
        AcceptsTab = true,
        ScrollBars = ScrollBars.Both,
        WordWrap = false,
        Font = new Font("Cascadia Mono", 10F),
        Dock = DockStyle.Fill
    };
    private readonly RichTextBox preview = new()
    {
        ReadOnly = true,
        BorderStyle = BorderStyle.None,
        BackColor = Color.White,
        Dock = DockStyle.Fill,
        Font = new Font("Microsoft YaHei UI", 10F)
    };
    private readonly Label summary = new() { AutoSize = true, ForeColor = Color.DimGray };
    private readonly Button save = new() { Text = "确定", AutoSize = true, Padding = new Padding(18, 6, 18, 6), Enabled = false };
    private QuickChecklistDraft? draft;

    private QuickChecklistEditorDialog()
    {
        Text = "快速待办清单";
        Width = 980;
        Height = 700;
        MinimumSize = new Size(700, 520);
        StartPosition = FormStartPosition.CenterParent;
        BuildLayout();
        input.TextChanged += (_, _) => RenderPreview();
        input.KeyDown += InputOnKeyDown;
        save.Click += (_, _) => Save();
        Shown += (_, _) => input.Focus();
        RenderPreview();
    }

    [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
    public QuickChecklistDraft? Result { get; private set; }

    public static QuickChecklistDraft? ShowEditor(IWin32Window owner)
    {
        using var dialog = new QuickChecklistEditorDialog();
        return dialog.ShowDialog(owner) == DialogResult.OK ? dialog.Result : null;
    }

    private void BuildLayout()
    {
        var root = new TableLayoutPanel { Dock = DockStyle.Fill, Padding = new Padding(18), ColumnCount = 1, RowCount = 4 };
        root.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        root.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        root.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
        root.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        root.Controls.Add(new Label
        {
            Text = "短横线数量表示层级；--# 添加可见说明；// 后的内容不会保存。",
            AutoSize = true,
            ForeColor = Color.DimGray,
            Padding = new Padding(0, 0, 0, 8)
        });
        root.Controls.Add(summary);
        var split = new SplitContainer { Dock = DockStyle.Fill, SplitterDistance = 440, Panel1MinSize = 260, Panel2MinSize = 260 };
        split.Panel1.Padding = new Padding(0, 10, 8, 0);
        split.Panel2.Padding = new Padding(8, 10, 0, 0);
        split.Panel1.Controls.Add(input);
        split.Panel2.Controls.Add(preview);
        root.Controls.Add(split);
        var buttons = new FlowLayoutPanel { Dock = DockStyle.Fill, FlowDirection = FlowDirection.RightToLeft, AutoSize = true, Padding = new Padding(0, 12, 0, 0) };
        var cancel = new Button { Text = "取消", AutoSize = true, Padding = new Padding(18, 6, 18, 6), DialogResult = DialogResult.Cancel };
        buttons.Controls.Add(save);
        buttons.Controls.Add(cancel);
        root.Controls.Add(buttons);
        Controls.Add(root);
        CancelButton = cancel;
    }

    private void InputOnKeyDown(object? sender, KeyEventArgs eventArgs)
    {
        if (eventArgs.KeyCode != Keys.Enter || eventArgs.Control || eventArgs.Alt) return;
        eventArgs.SuppressKeyPress = true;
        var start = input.SelectionStart;
        var end = start + input.SelectionLength;
        var lineStart = input.Text.LastIndexOf('\n', Math.Max(0, start - 1)) + 1;
        var prefix = QuickChecklistParser.ContinuationPrefix(input.Text[lineStart..start]);
        input.Text = input.Text[..start] + "\r\n" + prefix + input.Text[end..];
        input.SelectionStart = start + 2 + prefix.Length;
        input.SelectionLength = 0;
    }

    private void RenderPreview()
    {
        draft = QuickChecklistParser.Parse(input.Text);
        summary.Text = $"{draft.TaskCount} 项任务 · {draft.MaxDepth} 层 · {draft.NoteCount} 条说明";
        save.Enabled = draft.Title.Length > 0 && draft.TaskCount > 0;
        var text = new StringBuilder();
        if (draft.Title.Length > 0) text.AppendLine(draft.Title).AppendLine();
        foreach (var node in draft.Nodes)
        {
            text.Append(' ', Math.Max(0, node.Depth - 1) * 4).Append("☐ ").AppendLine(node.Title);
            if (node.Notes.Length > 0)
                foreach (var line in node.Notes.Split('\n')) text.Append(' ', node.Depth * 4 + 2).AppendLine(line);
        }
        if (draft.Diagnostics.Count > 0)
        {
            text.AppendLine();
            foreach (var diagnostic in draft.Diagnostics)
                text.Append(diagnostic.Severity == QuickChecklistDiagnosticSeverity.Error ? "错误" : "警告")
                    .Append(" · 第 ").Append(diagnostic.SourceLine).Append(" 行：").AppendLine(diagnostic.Message);
        }
        preview.Text = text.ToString();
    }

    private void Save()
    {
        var reparsed = QuickChecklistParser.Parse(input.Text);
        if (reparsed.HasErrors)
        {
            var error = reparsed.Diagnostics.First(value => value.Severity == QuickChecklistDiagnosticSeverity.Error);
            if (QuickChecklistParser.SourceLineSelection(input.Text, error.SourceLine) is { } selection)
            {
                input.Focus();
                input.Select(selection.Start, selection.Length);
                input.ScrollToCaret();
            }
            MessageBox.Show(this, $"第 {error.SourceLine} 行：{error.Message}", "PaykiTodo", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            return;
        }
        Result = reparsed;
        DialogResult = DialogResult.OK;
        Close();
    }
}
