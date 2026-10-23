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
    public static RecurrenceScope? ChooseRecurrenceScope(Window owner, string title)
    {
        var dialog = Dialog(owner, title, 430, 330); RecurrenceScope? result = null;
        var choices = new[] { "仅当前", "当前及之后", "全部" };
        var scope = new ComboBox { ItemsSource = choices, SelectedIndex = 0 };
        System.Windows.Automation.AutomationProperties.SetName(scope, "循环作用范围");
        var cancel = Action("取消", () => dialog.Close()); cancel.IsCancel = true;
        dialog.Content = DialogLayout(Column(Text(title, 20, bold: true), Text("选择本次操作影响的循环日期", 13, Muted), scope), cancel,
            Action("继续", () => { result = (RecurrenceScope)scope.SelectedIndex; dialog.DialogResult = true; }, true));
        dialog.ShowDialog(); return result;
    }

    public static DesktopItemEdit? Item(Window owner, bool isEvent, IReadOnlyList<DesktopGroup> groups, DesktopItemEdit? value, DateTime seed, IReadOnlyList<DesktopCalendar>? calendars = null, RecurrenceScope scope = RecurrenceScope.Current, System.Windows.Point? anchorScreen = null)
    {
        var dialog = Dialog(owner, (value == null ? "新增" : "编辑") + (isEvent ? "日程" : "待办"), 560, isEvent ? 640 : 690);
        if (isEvent) dialog.Background = Paper;
        if (anchorScreen is { } anchor) PositionNear(dialog, owner, anchor);
        DesktopItemEdit? result = null;
        var title = Input(value?.Title ?? "", true); title.MinHeight = 54; title.MaxHeight = 100; title.FontSize = 18;
        var notes = Input(value?.Notes ?? "", true); notes.MinHeight = 54; notes.MaxHeight = 100;
        var location = Input(value?.Location ?? "");
        var due = new TimeField(value?.DueAtMillis, seed.Date.AddHours(23).AddMinutes(59));
        var start = new TimeField(value?.StartAtMillis, seed);
        var end = new TimeField(value?.AllDay == true && value.EndAtMillis is { } endMillis ? Millis(DateTimeOffset.FromUnixTimeMilliseconds(endMillis).LocalDateTime.AddDays(-1)) : value?.EndAtMillis, start.Value.AddMinutes(30));
        var hasDue = new CheckBox { Content = "设置 DDL", IsChecked = value?.DueAtMillis != null };
        var allDay = new CheckBox { Content = "全天", IsChecked = value?.AllDay ?? false };
        var reminder = new CheckBox { Content = "提醒", IsChecked = value?.ReminderEnabled ?? false };
        var offsets = Input(value == null ? "15" : string.Join(",", value.ReminderOffsets));
        var countdown = new CheckBox { Content = "倒数日", IsChecked = value?.CountdownEnabled ?? false };
        var hidden = new CheckBox { Content = "仅提醒，不在看板显示", IsChecked = value?.HiddenFromBoard ?? false };
        var checkin = new CheckBox { Content = "打卡追踪", IsChecked = value?.CheckInEnabled ?? false };
        var group = new ComboBox { ItemsSource = groups, DisplayMemberPath = "Name", SelectedItem = groups.FirstOrDefault(group => group.Id == value?.GroupId) ?? groups.FirstOrDefault() };
        calendars ??= new[] { new DesktopCalendar("default", "常规日历", "#3370FF") };
        var calendar = new ComboBox { ItemsSource = calendars, DisplayMemberPath = "Name", SelectedItem = calendars.FirstOrDefault(c => c.Id == value?.CalendarId) ?? calendars[0] };
        System.Windows.Automation.AutomationProperties.SetName(calendar, "所属日历");
        var colors = new[] { "#4CB782", "#3B82F6", "#EF6A67", "#E9A23B", "#8B5CF6", "#14B8A6" };
        var initialColor = value?.AccentColor ?? (isEvent ? ((DesktopCalendar)calendar.SelectedItem).ColorHex : colors[0]);
        var color = new ComboBox { ItemsSource = colors.Append(initialColor).Distinct().ToList(), SelectedItem = initialColor };
        calendar.SelectionChanged += (_, _) => {
            if (calendar.SelectedItem is not DesktopCalendar selected) return;
            color.ItemsSource = colors.Append(selected.ColorHex).Append(initialColor).Distinct().ToList(); color.SelectedItem = selected.ColorHex;
        };
        var pastModes = new[] { new Choice("按设置自动隐藏", "AUTO"), new Choice("一直保留纪念", "KEEP"), new Choice("不再展示", "HIDE") };
        var pastMode = new ComboBox { ItemsSource = pastModes, DisplayMemberPath = "Name", SelectedItem = pastModes.FirstOrDefault(mode => mode.Value == value?.CountdownPastMode) ?? pastModes[0] };
        System.Windows.Automation.AutomationProperties.SetName(pastMode, "倒数日结束后");
        var repeats = new List<Choice> { new("不重复", "NONE"), new("每天", "DAILY"), new("每周", "WEEKLY"), new("每月同日（月末自动适配）", "MONTHLY_DAY"), new("每月第几个星期几", "MONTHLY_NTH_WEEKDAY"), new("每年同月日", "YEARLY_DATE"), new("每年同农历月日", "YEARLY_LUNAR_DATE") };
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
        if (isEvent) {
            fields.Children.Add(allDay); fields.Children.Add(LabeledField("开始", start.Control));
            fields.Children.Add(LabeledField("结束", end.Control));
            var allDayHint = Text("全天结束日期包含当天", 12, Muted); fields.Children.Add(allDayHint);
            void UpdateAllDay() { start.ShowTime(allDay.IsChecked != true); end.ShowTime(allDay.IsChecked != true); allDayHint.Visibility = allDay.IsChecked == true ? Visibility.Visible : Visibility.Collapsed; }
            allDay.Checked += (_, _) => UpdateAllDay(); allDay.Unchecked += (_, _) => UpdateAllDay(); UpdateAllDay();
            fields.Children.Add(LabeledField("日历", calendar)); fields.Children.Add(LabeledField("地点", location));
        }
        else {
            fields.Children.Add(hasDue); fields.Children.Add(due.Control);
            void UpdateDue() => due.Control.IsEnabled = hasDue.IsChecked == true;
            hasDue.Checked += (_, _) => UpdateDue(); hasDue.Unchecked += (_, _) => UpdateDue(); UpdateDue();
        }
        fields.Children.Add(reminder);
        var reminderDetails = Column(Text("提前提醒（分钟，例如 30,15,5）", 12, Muted), offsets); fields.Children.Add(reminderDetails);
        void UpdateReminder() => reminderDetails.Visibility = reminder.IsChecked == true ? Visibility.Visible : Visibility.Collapsed;
        reminder.Checked += (_, _) => UpdateReminder(); reminder.Unchecked += (_, _) => UpdateReminder(); UpdateReminder();
        fields.Children.Add(isEvent ? LabeledField("备注", notes) : Column(Text("备注"), notes));
        var advanced = Column(Row(countdown, isEvent ? checkin : hidden));
        var extraSlots = new List<(TimeField Start, TimeField End)>();
        if (isEvent && value == null) {
            var slotRows = Column();
            var addSlot = Action("添加时段", () => {
                allDay.IsChecked = false; allDay.IsEnabled = false;
                var next = extraSlots.LastOrDefault().End?.Value.AddHours(1) ?? end.Value.AddHours(1);
                var slot = (Start: new TimeField(null, next), End: new TimeField(null, next.AddMinutes(30)));
                extraSlots.Add(slot);
                var row = Column(LabeledField("开始", slot.Start.Control), LabeledField("结束", slot.End.Control));
                row.Children.Add(Action("移除此时段", () => { extraSlots.Remove(slot); slotRows.Children.Remove(row); allDay.IsEnabled = extraSlots.Count == 0; }));
                slotRows.Children.Add(row);
            });
            advanced.Children.Add(Text("同一事项可分多个时段安排", 13, Muted)); advanced.Children.Add(slotRows); advanced.Children.Add(addSlot);
        }
        var retention = Column(Text("倒数日结束后", 13), pastMode); advanced.Children.Add(retention);
        void UpdateCountdown() => retention.Visibility = countdown.IsChecked == true ? Visibility.Visible : Visibility.Collapsed;
        countdown.Checked += (_, _) => UpdateCountdown(); countdown.Unchecked += (_, _) => UpdateCountdown(); UpdateCountdown();
        advanced.Children.Add(Text("循环安排", 16, bold: true)); advanced.Children.Add(recurrence); advanced.Children.Add(weeklyRow);
        advanced.Children.Add(Text("循环截止日期（可留空）", 12, Muted)); advanced.Children.Add(until);
        if (value?.RecurrenceType is not (null or "NONE") && scope == RecurrenceScope.Current) {
            recurrence.IsEnabled = false; weeklyRow.IsEnabled = false; until.IsEnabled = false;
            advanced.Children.Add(Text("仅修改当前事项，循环规则保持不变", 12, Muted));
        }
        advanced.Children.Add(Pair(Column(Text("分组"), group), Column(Text("强调色"), color)));
        advanced.Children.Add(Text("过去的提醒不会补发，未来提醒照常生效。", 12, Muted));
        fields.Children.Add(new Expander { Header = "更多选项 · 循环、倒数日与颜色", Content = advanced, IsExpanded = value?.RecurrenceType is not (null or "NONE") });
        var error = Text("", 13, B("#AF3A32"));
        void Save()
        {
            try {
                if (string.IsNullOrWhiteSpace(title.Text)) throw new InvalidOperationException("请填写标题");
                var minutes = offsets.Text.Replace('，', ',').Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
                    .Select(text => int.TryParse(text, out var number) && number >= 0 && number <= 525600 ? number : throw new InvalidOperationException("提醒时间应为 0—525600 的整数分钟")).Distinct().ToList();
                var startAt = allDay.IsChecked == true ? start.Value.Date : start.Value;
                var endAt = allDay.IsChecked == true ? end.Value.Date.AddDays(1) : end.Value;
                if (isEvent && endAt <= startAt) throw new InvalidOperationException("结束时间必须晚于开始时间");
                if (reminder.IsChecked == true && (!isEvent && hasDue.IsChecked != true || minutes.Count == 0)) throw new InvalidOperationException("开启提醒需要设置时间和提前提醒分钟数");
                var repeat = (recurrence.SelectedItem as Choice)?.Value ?? "NONE";
                var weekdayCsv = string.Join(",", weekdays.Where(day => day.IsChecked == true).Select(day => day.Tag));
                if (repeat == "WEEKLY" && weekdayCsv.Length == 0) throw new InvalidOperationException("请选择至少一个重复星期");
                if (extraSlots.Any(slot => slot.End.Value <= slot.Start.Value)) throw new InvalidOperationException("每个时段的结束时间必须晚于开始时间");
                result = new DesktopItemEdit(isEvent, title.Text.Trim(), notes.Text, location.Text,
                    !isEvent && hasDue.IsChecked == true ? Millis(due.Value) : null, isEvent ? Millis(startAt) : null, isEvent ? Millis(endAt) : null,
                    allDay.IsChecked == true, reminder.IsChecked == true, minutes, (group.SelectedItem as DesktopGroup)?.Id ?? 1,
                    color.SelectedItem?.ToString() ?? colors[0], countdown.IsChecked == true, repeat, weekdayCsv,
                    until.SelectedDate is { } date ? (long)(date.Date - DateTime.UnixEpoch.Date).TotalDays : null)
                    { HiddenFromBoard = hidden.IsChecked == true, CheckInEnabled = checkin.IsChecked == true,
                        CalendarId = isEvent ? ((DesktopCalendar)calendar.SelectedItem).Id : value?.CalendarId ?? "default",
                        CountdownPastMode = ((Choice)pastMode.SelectedItem).Value,
                        AdditionalTimeSlots = extraSlots.Select(slot => new DesktopTimeSlot(Millis(slot.Start.Value), Millis(slot.End.Value))).ToList() };
                dialog.DialogResult = true;
            } catch (Exception problem) { error.Text = problem.Message; }
        }
        dialog.Content = EditorLayout(fields, error, () => dialog.Close(), Save, isEvent);
        dialog.ShowDialog(); return result;
    }
    public static QuickChecklistDraft? Checklist(Window owner)
    {
        var dialog = Dialog(owner, "快速清单", 960, 720); QuickChecklistDraft? result = null;
        var source = Input("", true); source.MinHeight = 330; source.Height = 380;
        source.FontFamily = InterfaceFont; source.FontSize = 15;
        System.Windows.Automation.AutomationProperties.SetName(source, "快速清单源码");
        var preview = new StackPanel(); var error = Text("", 13, B("#AF3A32"));
        var grid = new Grid(); grid.ColumnDefinitions.Add(new ColumnDefinition()); grid.ColumnDefinitions.Add(new ColumnDefinition());
        grid.Children.Add(Column(Text("快速记录", 15, bold: true), source));
        var previewPane = Column(Text("实际待办预览", 15, bold: true), Scroll(preview)); previewPane.Margin = new Thickness(20, 0, 0, 0);
        ((ScrollViewer)previewPane.Children[1]).MaxHeight = 380; Grid.SetColumn(previewPane, 1); grid.Children.Add(previewPane);
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
        var fields = Column(BrandHeading("想到就记", "标题可选 · 短横线定义任务 · Enter 自动续写"),
            Text("- 任务　-- 子任务　--# 说明　// 不保存的注释", 13, Muted), grid);
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
        var dialog = Dialog(owner, caption, 520, 340); var input = Input(value); string? result = null;
        dialog.Content = EditorLayout(Column(BrandHeading(caption), input), Text(""), () => dialog.Close(), () => {
            if (string.IsNullOrWhiteSpace(input.Text)) return; result = input.Text.Trim(); dialog.DialogResult = true;
        }); dialog.ShowDialog(); return result;
    }
    private static UIElement EditorLayout(UIElement fields, TextBlock error, Action cancel, Action save, bool calendar = false)
    {
        var cancelButton = NativeUi.Action("取消", cancel); cancelButton.IsCancel = true;
        var saveButton = NativeUi.Action("完成", save, true);
        if (calendar) { saveButton.Background = B("#3370FF"); saveButton.BorderBrush = B("#3370FF"); }
        return DialogLayout(Column(fields, error), cancelButton, saveButton);
    }
    private static Grid Pair(UIElement left, UIElement right)
    {
        var grid = new Grid(); grid.ColumnDefinitions.Add(new ColumnDefinition()); grid.ColumnDefinitions.Add(new ColumnDefinition());
        grid.Children.Add(left); ((FrameworkElement)right).Margin = new Thickness(16, 0, 0, 0); Grid.SetColumn(right, 1); grid.Children.Add(right); return grid;
    }
    private static Grid LabeledField(string label, UIElement field)
    {
        var row = new Grid(); row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(52) }); row.ColumnDefinitions.Add(new ColumnDefinition());
        var text = Text(label, 13); text.Margin = new Thickness(0, 12, 0, 0); row.Children.Add(text);
        Grid.SetColumn(field, 1); row.Children.Add(field); return row;
    }
    private static long Millis(DateTime date) => new DateTimeOffset(date).ToUnixTimeMilliseconds();
    private sealed record Choice(string Name, string Value);
    private sealed class TimeField
    {
        private readonly DatePicker date;
        private readonly TextBox time;
        public UIElement Control { get; }
        public void ShowTime(bool visible) => time.Visibility = visible ? Visibility.Visible : Visibility.Collapsed;
        public DateTime Value => (date.SelectedDate ?? throw new InvalidOperationException("请选择日期")).Date + (time.Visibility == Visibility.Collapsed ? TimeSpan.Zero :
            (TimeSpan.TryParseExact(time.Text.Trim(), new[] { "h\\:mm", "hh\\:mm" }, CultureInfo.InvariantCulture, out var parsed) && parsed.TotalHours < 24
                ? parsed : throw new InvalidOperationException("时间请填写 HH:mm，例如 09:30")));
        public TimeField(long? millis, DateTime fallback)
        {
            var value = millis is { } existing ? DateTimeOffset.FromUnixTimeMilliseconds(existing).LocalDateTime : fallback;
            date = new DatePicker { SelectedDate = value, Width = 200, Height = 42, Margin = new Thickness(0, 4, 12, 8) };
            time = Input(value.ToString("HH:mm")); time.Width = 100; time.Height = 42; time.TextAlignment = TextAlignment.Center;
            time.Margin = new Thickness(0, 4, 0, 8);
            System.Windows.Automation.AutomationProperties.SetName(time, "时间 HH:mm"); Control = Row(date, time);
        }
    }
}
