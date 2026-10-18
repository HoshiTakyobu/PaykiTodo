using System.Globalization;
using System.Windows;
using System.Windows.Controls;
using static PaykiTodo.Desktop.NativeUi;
using CheckBox = System.Windows.Controls.CheckBox;
using ComboBox = System.Windows.Controls.ComboBox;
using TextBox = System.Windows.Controls.TextBox;

namespace PaykiTodo.Desktop;

internal static class NativeEditors
{
    public static DesktopItemEdit? Item(Window owner, bool isEvent, IReadOnlyList<DesktopGroup> groups, DesktopItemEdit? value, DateTime seed)
    {
        var dialog = Dialog(owner, (value == null ? "新增" : "编辑") + (isEvent ? "日程" : "待办"), 650, 800);
        DesktopItemEdit? result = null;
        var title = Input(value?.Title ?? "", true); title.MinHeight = 60;
        var notes = Input(value?.Notes ?? "", true); var location = Input(value?.Location ?? "");
        var due = new TimeField(value?.DueAtMillis, seed.Date.AddHours(23).AddMinutes(59));
        var start = new TimeField(value?.StartAtMillis, seed.TimeOfDay == TimeSpan.Zero ? seed.AddHours(9) : seed);
        var end = new TimeField(value?.EndAtMillis, start.Value.AddHours(1));
        var hasDue = new CheckBox { Content = "设置 DDL", IsChecked = value?.DueAtMillis != null };
        var allDay = new CheckBox { Content = "全天", IsChecked = value?.AllDay ?? false };
        var reminder = new CheckBox { Content = "提醒", IsChecked = value?.ReminderEnabled ?? false };
        var offsets = Input(value == null ? "15" : string.Join(",", value.ReminderOffsets));
        var countdown = new CheckBox { Content = "倒数日", IsChecked = value?.CountdownEnabled ?? false };
        var hidden = new CheckBox { Content = "仅提醒，不在看板显示", IsChecked = value?.HiddenFromBoard ?? false };
        var checkin = new CheckBox { Content = "打卡追踪", IsChecked = value?.CheckInEnabled ?? false };
        var group = new ComboBox { ItemsSource = groups, DisplayMemberPath = "Name", SelectedItem = groups.FirstOrDefault(group => group.Id == value?.GroupId) ?? groups.FirstOrDefault() };
        var colors = new[] { "#4CB782", "#3B82F6", "#EF6A67", "#E9A23B", "#8B5CF6", "#14B8A6" };
        var color = new ComboBox { ItemsSource = colors.Append(value?.AccentColor ?? colors[0]).Distinct().ToList(), SelectedItem = value?.AccentColor ?? colors[0] };
        var repeats = new List<Choice> { new("不重复", "NONE"), new("每天", "DAILY"), new("每周", "WEEKLY"), new("每月", "MONTHLY_DAY"), new("每年", "YEARLY_DATE") };
        if (value != null && repeats.All(item => item.Value != value.RecurrenceType))
            repeats.Add(new Choice(value.RecurrenceType switch {
                "MONTHLY_NTH_WEEKDAY" => "保留每月第几个星期的规则", "YEARLY_LUNAR_DATE" => "保留农历循环规则", _ => "保留已有循环规则"
            }, value.RecurrenceType));
        var recurrence = new ComboBox { ItemsSource = repeats, DisplayMemberPath = "Name", SelectedItem = repeats.FirstOrDefault(item => item.Value == value?.RecurrenceType) ?? repeats[0] };
        var selectedDays = (value?.RecurrenceWeekdays ?? "").Split(',').ToHashSet();
        var weekdays = Enumerable.Range(1, 7).Select(day => new CheckBox {
            Content = new[] { "周一", "周二", "周三", "周四", "周五", "周六", "周日" }[day - 1],
            IsChecked = selectedDays.Contains(day.ToString()), Tag = day
        }).ToArray();
        if (value == null) weekdays[((int)seed.DayOfWeek + 6) % 7].IsChecked = true;
        var weeklyRow = Row(weekdays);
        void UpdateWeekly() => weeklyRow.Visibility = (recurrence.SelectedItem as Choice)?.Value == "WEEKLY" ? Visibility.Visible : Visibility.Collapsed;
        recurrence.SelectionChanged += (_, _) => UpdateWeekly(); UpdateWeekly();
        var until = new DatePicker { SelectedDate = value?.RecurrenceEndEpochDay is { } day ? DateTime.UnixEpoch.AddDays(day) : null };
        var fields = Column(Text("标题", 14, bold: true), title);
        if (isEvent) { fields.Children.Add(Text("开始时间")); fields.Children.Add(start.Control); fields.Children.Add(Text("结束时间")); fields.Children.Add(end.Control); fields.Children.Add(allDay); fields.Children.Add(Text("地点")); fields.Children.Add(location); }
        else { fields.Children.Add(hasDue); fields.Children.Add(due.Control); }
        fields.Children.Add(reminder); fields.Children.Add(Text("提前提醒（分钟，可用逗号分隔，例如 30,15,5）", 12, Muted)); fields.Children.Add(offsets);
        fields.Children.Add(Text("备注")); fields.Children.Add(notes); fields.Children.Add(Row(countdown, isEvent ? checkin : hidden));
        fields.Children.Add(Text("重复")); fields.Children.Add(recurrence); fields.Children.Add(weeklyRow);
        fields.Children.Add(Text("循环截止日期（可留空）", 12, Muted)); fields.Children.Add(until);
        fields.Children.Add(Text("分组")); fields.Children.Add(group); fields.Children.Add(Text("颜色")); fields.Children.Add(color);
        var error = Text("", 13, B("#AF3A32"));
        void Save()
        {
            try {
                if (string.IsNullOrWhiteSpace(title.Text)) throw new InvalidOperationException("请填写标题");
                var minutes = offsets.Text.Replace('，', ',').Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
                    .Select(text => int.TryParse(text, out var number) && number >= 0 && number <= 525600 ? number : throw new InvalidOperationException("提醒时间应为 0—525600 的整数分钟")).Distinct().ToList();
                if (isEvent && end.Value <= start.Value) throw new InvalidOperationException("结束时间必须晚于开始时间");
                if (reminder.IsChecked == true && (!isEvent && hasDue.IsChecked != true || minutes.Count == 0)) throw new InvalidOperationException("开启提醒需要设置时间和提前提醒分钟数");
                var repeat = (recurrence.SelectedItem as Choice)?.Value ?? "NONE";
                var weekdayCsv = string.Join(",", weekdays.Where(day => day.IsChecked == true).Select(day => day.Tag));
                if (repeat == "WEEKLY" && weekdayCsv.Length == 0) throw new InvalidOperationException("请选择至少一个重复星期");
                result = new DesktopItemEdit(isEvent, title.Text.Trim(), notes.Text, location.Text,
                    !isEvent && hasDue.IsChecked == true ? Millis(due.Value) : null, isEvent ? Millis(start.Value) : null, isEvent ? Millis(end.Value) : null,
                    allDay.IsChecked == true, reminder.IsChecked == true, minutes, (group.SelectedItem as DesktopGroup)?.Id ?? 1,
                    color.SelectedItem?.ToString() ?? colors[0], countdown.IsChecked == true, repeat, weekdayCsv,
                    until.SelectedDate is { } date ? (long)(date.Date - DateTime.UnixEpoch.Date).TotalDays : null)
                    { HiddenFromBoard = hidden.IsChecked == true, CheckInEnabled = checkin.IsChecked == true };
                dialog.DialogResult = true;
            } catch (Exception problem) { error.Text = problem.Message; }
        }
        dialog.Content = EditorLayout(fields, error, () => dialog.Close(), Save);
        dialog.ShowDialog(); return result;
    }
    public static QuickChecklistDraft? Checklist(Window owner)
    {
        var dialog = Dialog(owner, "快速清单", 960, 720); QuickChecklistDraft? result = null;
        var source = Input("", true); source.MinHeight = 450;
        var preview = new StackPanel(); var error = Text("", 13, B("#AF3A32"));
        var grid = new Grid(); grid.ColumnDefinitions.Add(new ColumnDefinition()); grid.ColumnDefinitions.Add(new ColumnDefinition());
        grid.Children.Add(source); Grid.SetColumn(preview, 1); preview.Margin = new Thickness(20, 0, 0, 0); grid.Children.Add(preview);
        void Refresh()
        {
            preview.Children.Clear(); var draft = QuickChecklistParser.Parse(source.Text);
            preview.Children.Add(Text(string.IsNullOrWhiteSpace(draft.Title) ? "快速清单" : draft.Title, 22, bold: true));
            foreach (var node in draft.Nodes) preview.Children.Add(new Border { Margin = new Thickness(Math.Min(node.Depth, 12) * 14, 4, 0, 4), Child = Text("○  " + node.Title + (string.IsNullOrEmpty(node.Notes) ? "" : "\n" + node.Notes)) });
            error.Text = string.Join("\n", draft.Diagnostics.Select(problem => $"第 {problem.SourceLine} 行：{problem.Message}"));
        }
        source.TextChanged += (_, _) => Refresh();
        source.PreviewKeyDown += (_, e) => {
            if (e.Key != System.Windows.Input.Key.Enter) return;
            var lineStart = source.CaretIndex == 0 ? -1 : source.Text.LastIndexOf('\n', source.CaretIndex - 1);
            var line = source.Text[(lineStart + 1)..source.CaretIndex];
            var prefix = QuickChecklistParser.ContinuationPrefix(line).TrimEnd();
            var insert = "\n" + prefix + (prefix.Length > 0 ? " " : ""); var position = source.SelectionStart; source.SelectedText = insert; source.CaretIndex = position + insert.Length; e.Handled = true;
        };
        var fields = Column(Text("标题可选，可直接写 - 任务；短横线表示层级，--# 添加说明，// 是不保存的注释。", 13, Muted), grid);
        dialog.Content = EditorLayout(fields, error, () => dialog.Close(), () => {
            var draft = QuickChecklistParser.Parse(source.Text);
            if (draft.HasErrors) {
                var problem = draft.Diagnostics.First(d => d.Severity == QuickChecklistDiagnosticSeverity.Error);
                error.Text = $"第 {problem.SourceLine} 行：{problem.Message}";
                if (QuickChecklistParser.SourceLineSelection(source.Text, problem.SourceLine) is { } selection) {
                    source.Focus(); source.Select(selection.Start, selection.Length);
                }
                return;
            }
            result = draft; dialog.DialogResult = true;
        });
        dialog.ShowDialog(); return result;
    }
    public static string? Prompt(Window owner, string caption, string value = "")
    {
        var dialog = Dialog(owner, caption, 490, 290); var input = Input(value); string? result = null;
        dialog.Content = EditorLayout(Column(Text(caption, 18, bold: true), input), Text(""), () => dialog.Close(), () => {
            if (string.IsNullOrWhiteSpace(input.Text)) return; result = input.Text.Trim(); dialog.DialogResult = true;
        }); dialog.ShowDialog(); return result;
    }
    private static UIElement EditorLayout(UIElement fields, TextBlock error, Action cancel, Action save)
    {
        var root = new DockPanel { Margin = new Thickness(24) };
        var actions = Column(error, Row(Action("取消", cancel), Action("完成", save, true))); DockPanel.SetDock(actions, Dock.Bottom); root.Children.Add(actions);
        root.Children.Add(Scroll(fields)); return root;
    }
    private static long Millis(DateTime date) => new DateTimeOffset(date).ToUnixTimeMilliseconds();
    private sealed record Choice(string Name, string Value);
    private sealed class TimeField
    {
        private readonly DatePicker date;
        private readonly TextBox time;
        public UIElement Control { get; }
        public DateTime Value => (date.SelectedDate ?? throw new InvalidOperationException("请选择日期")).Date +
            (TimeSpan.TryParseExact(time.Text.Trim(), new[] { "h\\:mm", "hh\\:mm" }, CultureInfo.InvariantCulture, out var parsed) && parsed.TotalHours < 24
                ? parsed : throw new InvalidOperationException("时间请填写 HH:mm，例如 09:30"));
        public TimeField(long? millis, DateTime fallback)
        {
            var value = millis is { } existing ? DateTimeOffset.FromUnixTimeMilliseconds(existing).LocalDateTime : fallback;
            date = new DatePicker { SelectedDate = value, Width = 200 }; time = Input(value.ToString("HH:mm")); time.Width = 100; Control = Row(date, time);
        }
    }
}
