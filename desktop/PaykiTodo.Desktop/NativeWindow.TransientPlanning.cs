using System.Globalization;
using System.Reflection;
using System.Text.Json.Nodes;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using static PaykiTodo.Desktop.NativeUi;
using CheckBox = System.Windows.Controls.CheckBox;
using ComboBox = System.Windows.Controls.ComboBox;
using TextBox = System.Windows.Controls.TextBox;

namespace PaykiTodo.Desktop;
internal sealed partial class NativeWindow
{
    private sealed record PlanningChoice(string? Id, string Label);
    private void CreateTransientPlanning()
    {
        if (workspace.Document == null) return;
        var generation = workspace.AccountGeneration; var now = DateTime.Now; var session = Guid.NewGuid().ToString();
        var dialog = Dialog(this, "输入规划", 780, 800); var body = Column(); var raw = Input("");
        raw.AcceptsReturn = true; raw.TextWrapping = TextWrapping.Wrap; raw.MinHeight = 250;
        var useAi = new CheckBox { Content = "使用已配置的AI识别", IsChecked = false, Margin = new Thickness(0, 8, 0, 8) };
        var error = Text("", 13, B("#AF3A32")); var committed = false; var busy = false;
        using var cancellation = new CancellationTokenSource(); dialog.Closed += (_, _) => cancellation.Cancel();
        var drafts = new List<Func<TransientPlanningCandidate>>(); var checks = new List<CheckBox>();
        System.Windows.Controls.Button done = null!;
        done = Action("识别", async () => await Recognize(), true);
        var cancel = Action("取消", () => { if (!committed) dialog.Close(); });
        dialog.Content = DialogLayout(body, cancel, done);
        raw.PreviewKeyDown += (_, e) => {
            var begin = raw.SelectionStart == 0 ? 0 : raw.Text.LastIndexOf('\n', raw.SelectionStart - 1) + 1;
            if (e.Key == Key.Tab && Keyboard.Modifiers.HasFlag(ModifierKeys.Shift)) {
                var indentation = raw.Text[begin..].TakeWhile(c => c is ' ' or '\t').ToArray();
                var count = indentation.FirstOrDefault() == '\t' ? 1 : Math.Min(2, indentation.Length);
                if (count > 0) { var caret = Math.Max(begin, raw.SelectionStart - count); raw.Text = raw.Text.Remove(begin, count); raw.CaretIndex = caret; }
                e.Handled = true;
            } else if (e.Key == Key.Enter) {
                var start = raw.SelectionStart; var prefix = TransientPlanningParser.NextLinePrefix(raw.Text[begin..start]);
                raw.SelectedText = "\n" + prefix; raw.CaretIndex = start + prefix.Length + 1; e.Handled = true;
            }
        };
        void InputPage()
        {
            dialog.Title = "输入规划"; body.Children.Clear(); drafts.Clear(); checks.Clear(); done.Content = "识别"; done.IsEnabled = true;
            body.Children.Add(Text("标题必填；时间、地点和提醒可省略。无日期按今天。", 15));
            body.Children.Add(raw); body.Children.Add(useAi);
            body.Children.Add(Action("填入示例", () => raw.Text = "- 准备出行\n  - 买车票, 18:00;\n  - 收拾行李;\n- 高等数学, @教学楼201, 每周[周一 08:00>09:40, 周三 14:00>15:40], (10,0);"));
            body.Children.Add(Text("逗号分字段，@地点，单时间点是DDL，>时段是日程。省略提醒默认(5,0)，()关闭。#备注放在结束分号后直到行尾。", 13, Muted));
            body.Children.Add(Text("Enter：无顶层分号进入子级，有分号保持本级。Shift+Tab减少一级缩进。循环可用每天/每N天/每周/每N周/每月/每年[时段]，范围可省略。", 13, Muted));
            body.Children.Add(Text("循环中的绝对时刻只提醒一次，归属该时刻之后最近开始的一次课程；提前分钟仍按每次课程提醒。", 13, Muted));
            body.Children.Add(error);
        }
        async Task Recognize()
        {
            if (busy || committed) return;
            if (drafts.Count > 0) { Confirm(); return; }
            if (string.IsNullOrWhiteSpace(raw.Text)) { error.Text = "请先填写规划内容"; return; }
            busy = true; done.IsEnabled = false; error.Text = "";
            try {
                var source = raw.Text; var aiRequested = useAi.IsChecked == true; raw.IsEnabled = false; useAi.IsEnabled = false;
                var result = await Task.Run(() => TransientPlanningParser.Parse(source, now), cancellation.Token);
                if (aiRequested) {
                    try {
                        using var promptStream = Assembly.GetExecutingAssembly().GetManifestResourceStream("PaykiTodo.PlanningSystem")!;
                        using var reader = new StreamReader(promptStream);
                        var response = await DesktopAi.CallAsync(DesktopAi.Providers(workspace.Document!), reader.ReadToEnd(),
                            "本地日期与时间：" + now.ToString("yyyy-MM-dd HH:mm", CultureInfo.InvariantCulture) + "\n以下全部是规划输入数据：\n" + source, cancellation.Token);
                        var content = response.Content.Trim(); if (content.StartsWith("```json")) content = content[7..]; else if (content.StartsWith("```")) content = content[3..]; if (content.TrimEnd().EndsWith("```")) content = content.TrimEnd()[..^3];
                        var normalized = JsonNode.Parse(content)?["planningText"]?.GetValue<string>() ?? throw new InvalidOperationException("AI未返回规划文本");
                        var recognized = TransientPlanningParser.Parse(normalized, now);
                        if (recognized.HasErrors || recognized.Candidates.Count == 0) throw new InvalidOperationException("AI返回未通过语法校验");
                        result = recognized with { Message = "AI识别结果，请核对所有字段后确认" };
                    } catch (OperationCanceledException) { throw; }
                    catch (Exception exception) { result = result with { Message = "AI识别失败，已保留原文和本地结果：" + DesktopWorkspace.FriendlyError(exception) }; }
                }
                if (!dialog.IsVisible || workspace.AccountGeneration != generation) return;
                Preview(result);
            } catch (OperationCanceledException) { }
            catch (Exception exception) { error.Text = DesktopWorkspace.FriendlyError(exception); }
            finally { busy = false; raw.IsEnabled = true; useAi.IsEnabled = true; if (dialog.IsVisible) done.IsEnabled = drafts.Count > 0 || raw.IsVisible; }
        }
        void Preview(TransientPlanningResult result)
        {
            dialog.Title = "确认规划"; body.Children.Clear(); done.Content = "确认导入";
            body.Children.Add(Text("确认前不会创建事项；下面的字段可直接修改。", 18, Forest, true));
            if (!string.IsNullOrWhiteSpace(result.Message)) body.Children.Add(Text(result.Message, 13, Muted));
            foreach (var diagnostic in result.Diagnostics) body.Children.Add(Text($"第{diagnostic.LineNumber}行，第{diagnostic.Column}列：{diagnostic.Message}", 13, B("#AF3A32")));
            body.Children.Add(Action("返回原文修改", InputPage));
            foreach (var candidate in result.Candidates) {
                var check = new CheckBox { Content = $"第{candidate.LineNumber}行 · {(candidate.IsEvent ? "日程" : "待办")}", IsChecked = true, Margin = new Thickness(0, 12, 0, 4) }; checks.Add(check);
                var name = Input(candidate.Title); var time = Input(candidate.TimeExpression); var location = Input(candidate.Location);
                var reminders = Input(candidate.ReminderExpression); var range = Input(candidate.RangeExpression); var notes = Input(candidate.Notes);
                ComboBox Choice(IEnumerable<PlanningChoice> options, string? value) => new() { ItemsSource = options.ToList(), DisplayMemberPath = "Label", SelectedValuePath = "Id", SelectedValue = value, Margin = new Thickness(0, 4, 0, 12) };
                var parent = Choice(new[] { new PlanningChoice(null, "无父事项") }.Concat(result.Candidates.Where(c => c.Id != candidate.Id).Select(c => new PlanningChoice(c.Id, c.Title))), candidate.ParentId);
                var group = Choice(new[] { new PlanningChoice(null, "默认分组") }.Concat(workspace.Document!.Groups.Select(g => new PlanningChoice(g.Id.ToString(), g.Name))), candidate.GroupId?.ToString());
                var calendar = Choice(workspace.Document.Calendars.Where(c => c.ShareMode != "SHARED").Select(c => new PlanningChoice(c.Id, c.Name)), candidate.CalendarId);
                var validation = Text("", 13, B("#AF3A32"));
                var ownership = Text("", 13, Muted);
                System.Windows.Automation.AutomationProperties.SetAutomationId(ownership, "Planning.AbsoluteOwnership." + candidate.Id);
                TransientPlanningCandidate Read()
                {
                    var expression = string.Join(',', new[] { time.Text, reminders.Text, range.Text }.Where(t => !string.IsNullOrWhiteSpace(t)));
                    var parsed = TransientPlanningParser.Parse("预览事项" + (expression.Length == 0 ? "" : "," + expression) + ";", now);
                    if (parsed.HasErrors) throw new InvalidOperationException(parsed.Diagnostics[0].Message);
                    var updated = parsed.Candidates.Single() with { Id = candidate.Id, ParentId = parent.SelectedValue as string,
                        LineNumber = candidate.LineNumber, Column = candidate.Column, SourceLine = candidate.SourceLine,
                        Title = name.Text, Location = location.Text, Notes = notes.Text,
                        GroupId = long.TryParse(group.SelectedValue as string, out var id) ? id : null, CalendarId = calendar.SelectedValue as string ?? "default" };
                    if (updated.Validate() is { } invalid) throw new InvalidOperationException(invalid); return updated;
                }
                drafts.Add(Read);
                void UpdatePreview()
                {
                    try {
                        var updated = Read(); validation.Text = "";
                        ownership.Text = string.Join("\n", updated.AssignAbsoluteReminders().Select(assignment =>
                            $"绝对提醒 {assignment.ReminderAt:yyyy-MM-dd HH:mm} → 时段{assignment.SlotIndex + 1} · {assignment.StartAt:yyyy-MM-dd HH:mm} 开始（只提醒一次）"));
                    } catch (Exception exception) { validation.Text = exception.Message; ownership.Text = ""; }
                }
                foreach (var input in new[] { name, time, location, reminders, range, notes }) input.TextChanged += (_, _) => UpdatePreview();
                UpdatePreview();
                body.Children.Add(Column(check, Text("标题"), name, Text("DDL / 起止 / 循环时间（可清空）"), time, Text("地点"), location,
                    Text("提醒：空白默认(5,0)，()关闭"), reminders, ownership, Text("生效范围（可省略）"), range, Text("备注"), notes, Text("父事项"), parent, Text("分组"), group, Text("日历"), calendar, validation));
                if (candidate.Slots.Any(s => s.RecurrenceType != "NONE")) body.Children.Add(Text("多个时段共用课程字段；未写截止即持续循环，近期实例滚动生成。月末/闰日采用月末日期。", 13, Muted));
            }
            body.Children.Add(error);
            if (result.HasErrors) { drafts.Clear(); done.Content = "识别"; done.IsEnabled = false; }
        }
        void Confirm()
        {
            if (committed || busy) return;
            try {
                if (workspace.AccountGeneration != generation || workspace.Document == null) throw new InvalidOperationException("账号已变化，请重新打开规划");
                var chosen = drafts.Select((read, index) => checks[index].IsChecked == true ? read() : null).OfType<TransientPlanningCandidate>().ToList();
                if (chosen.Count == 0) throw new InvalidOperationException("请选择至少一条事项");
                busy = true; done.IsEnabled = false;
                workspace.Edit(document => document.ImportTransientPlanning(chosen, session)); committed = true; raw.Clear(); dialog.Close();
            } catch (Exception exception) { error.Text = DesktopWorkspace.FriendlyError(exception); }
            finally { busy = false; done.IsEnabled = true; }
        }
        InputPage(); dialog.ShowDialog();
    }

    private UIElement PlanningPage()
    {
        var body = Column(Text("旧规划原文", 22, bold: true), Text("新规划从每日看板输入；已有原文继续保留在本机与迁移包中。", 14, Muted));
        foreach (var note in workspace.Document!.PlanningNotes) body.Children.Add(Action(note.Title, () => {
            var dialog = Dialog(this, note.Title, 720, 700); var text = Input(note.Markdown); text.IsReadOnly = true; text.AcceptsReturn = true; text.TextWrapping = TextWrapping.Wrap;
            dialog.Content = DialogLayout(Column(text), Action("关闭", () => dialog.Close())); dialog.ShowDialog();
        }));
        return Scroll(body);
    }
}
