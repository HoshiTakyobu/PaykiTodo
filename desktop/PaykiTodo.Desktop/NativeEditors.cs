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

    public static DesktopItemEdit? Item(Window owner, bool isEvent, IReadOnlyList<DesktopGroup> groups, DesktopItemEdit? value, DateTime seed, IReadOnlyList<DesktopCalendar>? calendars = null, RecurrenceScope scope = RecurrenceScope.Current, System.Windows.Point? anchorScreen = null, DesktopSnapshotDocument? preferences = null)
    {
        var dialog = Dialog(owner, (value == null ? "新增" : "编辑") + (isEvent ? "日程" : "待办"), Math.Max(600, TextSize(500) + 80), isEvent ? 760 : 710);
        dialog.Background = Paper;
        if (anchorScreen is { } anchor) PositionNear(dialog, owner, anchor);
        DesktopItemEdit? result = null;
        var title = Input(value?.Title ?? "", true); title.MaxHeight = 96;
        var notes = Input(value?.Notes ?? "", true); notes.MaxHeight = 110;
        var location = Input(value?.Location ?? "");
        var due = new TimeField(value?.DueAtMillis, seed.Date.AddHours(23).AddMinutes(59));
        var zoneId = value?.EventTimeZoneId ?? (preferences?.CalendarDisplayTimeZoneId is { Length: > 0 } displayZone ? displayZone : DesktopCalendarTime.IanaId(TimeZoneInfo.Local));
        var duration = preferences?.CalendarDefaultDurationMinutes ?? 30;
        var start = new TimeField(value?.StartAtMillis, seed, zoneId, compact: true);
        var end = new TimeField(value?.AllDay == true && value.EndAtMillis is { } endMillis
            ? DesktopCalendarTime.Millis(DesktopCalendarTime.Wall(endMillis, zoneId).AddDays(-1), zoneId) : value?.EndAtMillis, start.Value.AddMinutes(duration), zoneId, compact: true, reverse: true);
        var hasDue = new CheckBox { Content = "设置 DDL", IsChecked = value?.DueAtMillis != null };
        var allDay = new CheckBox { Content = "全天", IsChecked = value?.AllDay ?? false };
        var defaultOffsets = preferences?.CalendarTimedDefaultRemindersCsv ?? "15";
        var defaultAllDay = preferences?.CalendarAllDayDefaultRemindersJson ?? "[{\"daysBefore\":0,\"minuteOfDay\":480}]";
        var reminder = new CheckBox { Content = "提醒", IsChecked = value?.ReminderEnabled ?? (isEvent && defaultOffsets.Length > 0) };
        var offsets = Input(value == null ? defaultOffsets : string.Join(",", value.ReminderOffsets));
        var initialAllDayRules = value?.AllDay == true && string.IsNullOrWhiteSpace(value.AllDayReminderRulesJson)
            ? AllDayReminderEditor.FromLegacyOffsets(value.ReminderOffsets) : !string.IsNullOrWhiteSpace(value?.AllDayReminderRulesJson) ? value.AllDayReminderRulesJson : defaultAllDay;
        var allDayReminders = new AllDayReminderEditor(initialAllDayRules);
        // New events use the global setting; editing never reinterprets existing wall times.
        var busyChoices = new[] { new Choice("忙碌", "BUSY"), new Choice("空闲", "FREE") };
        var busy = new ComboBox { ItemsSource = busyChoices, DisplayMemberPath = "Name", SelectedItem = busyChoices.First(choice => choice.Value == (value?.EventBusyStatus ?? "BUSY")) };
        var visibilityChoices = new[] { new Choice("沿用日历设置", "DEFAULT"), new Choice("私密", "PRIVATE"), new Choice("公开", "PUBLIC") };
        var visibility = new ComboBox { ItemsSource = visibilityChoices, DisplayMemberPath = "Name", SelectedItem = visibilityChoices.First(choice => choice.Value == (value?.EventVisibility ?? "DEFAULT")) };
        var countdown = new CheckBox { Content = "倒数日", IsChecked = value?.CountdownEnabled ?? false };
        var hidden = new CheckBox { Content = "仅提醒，不在看板显示", IsChecked = value?.HiddenFromBoard ?? false };
        var checkin = new CheckBox { Content = "打卡追踪", IsChecked = value?.CheckInEnabled ?? false };
        var group = new ComboBox { ItemsSource = groups, DisplayMemberPath = "Name", SelectedItem = groups.FirstOrDefault(group => group.Id == value?.GroupId) ?? groups.FirstOrDefault() };
        calendars ??= new[] { new DesktopCalendar("default", "常规日历", "#3370FF") };
        var calendar = new ComboBox { ItemsSource = calendars, DisplayMemberPath = "Name", SelectedItem = calendars.FirstOrDefault(c => c.Id == value?.CalendarId) ?? calendars[0] };
        System.Windows.Automation.AutomationProperties.SetName(calendar, "所属日历");
        var participants = new NativeParticipantEditor(dialog, (DesktopCalendar)calendar.SelectedItem, value?.EventParticipantsJson ?? "[]");
        NativeColorChoice.ApplyLegend(group);
        NativeColorChoice.ApplyLegend(calendar);
        System.Windows.Automation.AutomationProperties.SetName(group, "事项分组");
        var colors = NativeColorChoice.Presets;
        var initialColor = value?.AccentColor ?? (isEvent ? ((DesktopCalendar)calendar.SelectedItem).ColorHex : colors[0]);
        var color = new ComboBox { ItemsSource = colors.Append(initialColor).Distinct().ToList(), SelectedItem = initialColor };
        NativeColorChoice.Apply(color);
        calendar.SelectionChanged += (_, _) => {
            if (calendar.SelectedItem is not DesktopCalendar selected) return;
            participants.SetCalendar(selected);
            color.ItemsSource = colors.Append(selected.ColorHex).Append(initialColor).Distinct().ToList(); color.SelectedItem = selected.ColorHex;
        };
        var pastModes = new[] { new Choice("按设置自动隐藏", "AUTO"), new Choice("一直保留纪念", "KEEP"), new Choice("不再展示", "HIDE") };
        var pastMode = new ComboBox { ItemsSource = pastModes, DisplayMemberPath = "Name", SelectedItem = pastModes.FirstOrDefault(mode => mode.Value == value?.CountdownPastMode) ?? pastModes[0] };
        System.Windows.Automation.AutomationProperties.SetName(pastMode, "倒数日结束后");
        var repeats = new List<Choice> { new("不重复", "NONE"), new("按天重复", "DAILY"), new("按周重复", "WEEKLY"), new("每月同日", "MONTHLY_DAY"), new("每月第几个星期几", "MONTHLY_NTH_WEEKDAY"), new("每年同月日", "YEARLY_DATE"), new("每年同农历月日", "YEARLY_LUNAR_DATE") };
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
        var interval = new ComboBox { ItemsSource = Enumerable.Range(1, 30).Concat(new[] { value?.RecurrenceInterval ?? 1 }).Distinct().Order().ToArray(), SelectedItem = value?.RecurrenceInterval ?? 1 };
        System.Windows.Automation.AutomationProperties.SetName(interval, "重复间隔");
        var policies = new[] { new Choice("跳过不存在的日期", "SKIP"), new Choice("使用该月最后一天", "CLAMP") };
        var policy = new ComboBox { ItemsSource = policies, DisplayMemberPath = "Name", SelectedItem = policies.First(choice => choice.Value == (value?.RecurrenceInvalidDatePolicy ?? (isEvent ? "SKIP" : "CLAMP"))) };
        var ordinals = new[] { new Choice("第一个", "1"), new Choice("第二个", "2"), new Choice("第三个", "3"), new Choice("第四个", "4"), new Choice("第五个", "5"), new Choice("最后一个", "-1") };
        var ordinal = new ComboBox { ItemsSource = ordinals, DisplayMemberPath = "Name", SelectedItem = ordinals.First(choice => int.Parse(choice.Value) == (value?.RecurrenceMonthlyOrdinal ?? (seed.Day - 1) / 7 + 1)) };
        var ordinalWeekday = new ComboBox { ItemsSource = Enumerable.Range(1, 7).Select(day => new Choice(new[] { "周一", "周二", "周三", "周四", "周五", "周六", "周日" }[day - 1], day.ToString())).ToArray(), DisplayMemberPath = "Name", SelectedIndex = (value?.RecurrenceMonthlyWeekday ?? ((int)seed.DayOfWeek + 6) % 7 + 1) - 1 };
        var nthRow = Pair(ordinal, ordinalWeekday);
        var repeatDetails = Column(LabeledField("间隔", interval), weeklyRow, nthRow, Text("截止日期 · 留空表示一直重复", 12, Muted), until);
        if (isEvent) repeatDetails.Children.Add(policy);
        void UpdateRepeatDetails() {
            var kind = (recurrence.SelectedItem as Choice)?.Value ?? "NONE";
            repeatDetails.Visibility = kind == "NONE" ? Visibility.Collapsed : Visibility.Visible;
            nthRow.Visibility = kind == "MONTHLY_NTH_WEEKDAY" ? Visibility.Visible : Visibility.Collapsed;
            policy.Visibility = kind is "MONTHLY_DAY" or "YEARLY_DATE" ? Visibility.Visible : Visibility.Collapsed;
        }
        recurrence.SelectionChanged += (_, _) => UpdateRepeatDetails(); UpdateRepeatDetails();
        foreach (var selection in new[] { group, calendar, recurrence, busy, visibility }) Integrated(selection);
        color.Width = 64; color.HorizontalAlignment = System.Windows.HorizontalAlignment.Left; Integrated(color);
        var fields = Column(Placeholder(title, isEvent ? "添加主题" : "添加待办", 22), Divider());
        var recurrenceRow = Column(recurrence, repeatDetails);
        if (isEvent) {
            var dash = Text("—", 14, Muted); dash.VerticalAlignment = VerticalAlignment.Center; dash.Margin = new Thickness(4, 0, 4, 0);
            fields.Children.Add(IconRow(ClockDrawing, Column(Row(start.Control, dash, end.Control), allDay, recurrenceRow)));
            var allDayHint = Text("全天结束日期包含当天", 12, Muted); fields.Children.Add(allDayHint);
            var mode = allDay.IsChecked == true;
            var memory = new Dictionary<bool, (DateTime Start, DateTime End, bool Enabled, string Offsets, string Rules, string Busy)>();
            void UpdateAllDay() {
                var next = allDay.IsChecked == true;
                if (mode != next) {
                    try {
                        memory[mode] = (start.Value, end.Value, reminder.IsChecked == true, offsets.Text, allDayReminders.Json, ((Choice)busy.SelectedItem).Value);
                        if (memory.TryGetValue(next, out var saved)) {
                            start.SetValue(saved.Start); end.SetValue(saved.End); reminder.IsChecked = saved.Enabled; offsets.Text = saved.Offsets;
                            allDayReminders.SetJson(saved.Rules); busy.SelectedItem = busyChoices.First(choice => choice.Value == saved.Busy);
                        } else if (next) {
                            var endDate = end.Value.AddTicks(-1).Date; start.SetValue(start.Value.Date); end.SetValue(endDate < start.Value.Date ? start.Value.Date : endDate);
                            allDayReminders.SetJson(defaultAllDay); reminder.IsChecked = DesktopCalendarTime.ParseRules(defaultAllDay).Count > 0; busy.SelectedItem = busyChoices[1];
                        } else {
                            var now = DesktopCalendarTime.Wall(DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(), zoneId);
                            var minutes = (Math.Floor(now.TimeOfDay.TotalMinutes / 30) + 1) * 30;
                            var begin = start.Value.Date.AddMinutes(minutes); var finish = end.Value.Date.AddMinutes(minutes + duration);
                            start.SetValue(begin); end.SetValue(finish); offsets.Text = defaultOffsets; reminder.IsChecked = defaultOffsets.Length > 0; busy.SelectedItem = busyChoices[0];
                        }
                    } catch { allDay.IsChecked = mode; return; }
                    mode = next;
                }
                start.ShowTime(!next); end.ShowTime(!next); allDayHint.Visibility = next ? Visibility.Visible : Visibility.Collapsed;
                allDayReminders.Control.Visibility = next && reminder.IsChecked == true ? Visibility.Visible : Visibility.Collapsed;
                offsets.Visibility = !next ? Visibility.Visible : Visibility.Collapsed;
            }
            allDay.Checked += (_, _) => UpdateAllDay(); allDay.Unchecked += (_, _) => UpdateAllDay(); UpdateAllDay();
            fields.Children.Add(Divider());
            fields.Children.Add(IconRow(LocationDrawing, Placeholder(location, "添加地点")));
        }
        else {
            fields.Children.Add(IconRow(ClockDrawing, Column(hasDue, due.Control, recurrenceRow)));
            void UpdateDue() => due.Control.IsEnabled = hasDue.IsChecked == true;
            hasDue.Checked += (_, _) => UpdateDue(); hasDue.Unchecked += (_, _) => UpdateDue(); UpdateDue();
        }
        fields.Children.Add(IconRow(NotesDrawing, Placeholder(notes, "添加备注")));
        Integrated(offsets); offsets.Width = 130; offsets.HorizontalAlignment = System.Windows.HorizontalAlignment.Left;
        var minuteHint = Text("提前提醒 · 分钟（可用逗号分隔）", 12, Muted);
        var reminderDetails = Column(minuteHint, offsets, allDayReminders.Control);
        fields.Children.Add(IconRow(BellDrawing, Column(reminder, reminderDetails)));
        void UpdateReminder() {
            reminderDetails.Visibility = reminder.IsChecked == true ? Visibility.Visible : Visibility.Collapsed;
            minuteHint.Visibility = offsets.Visibility = allDay.IsChecked == true && isEvent ? Visibility.Collapsed : Visibility.Visible;
            allDayReminders.Control.Visibility = isEvent && allDay.IsChecked == true ? Visibility.Visible : Visibility.Collapsed;
        }
        allDay.Checked += (_, _) => UpdateReminder(); allDay.Unchecked += (_, _) => UpdateReminder();
        reminder.Checked += (_, _) => UpdateReminder(); reminder.Unchecked += (_, _) => UpdateReminder(); UpdateReminder();
        if (isEvent) fields.Children.Add(IconRow(CalendarDrawing, calendar));
        fields.Children.Add(Pair(Column(Text("分组", 12, Muted), group), Column(Text(isEvent ? "日程颜色" : "待办颜色", 12, Muted), color)));
        if (isEvent) fields.Children.Add(Pair(Column(Text("忙闲状态", 12, Muted), busy), Column(Text("公开范围", 12, Muted), visibility)));
        var advanced = Column(Row(countdown, isEvent ? checkin : hidden));
        if (isEvent) advanced.Children.Add(participants.Control);
        var extraSlots = new List<(TimeField Start, TimeField End)>();
        if (isEvent && value == null) {
            var slotRows = Column();
            var addSlot = QuietAction("添加时段", () => {
                allDay.IsChecked = false; allDay.IsEnabled = false;
                var next = extraSlots.LastOrDefault().End?.Value.AddHours(1) ?? end.Value.AddHours(1);
                var slot = (Start: new TimeField(null, next, zoneId, compact: true), End: new TimeField(null, next.AddMinutes(duration), zoneId, compact: true, reverse: true));
                extraSlots.Add(slot);
                var separator = Text("—", 14, Muted); separator.VerticalAlignment = VerticalAlignment.Center; separator.Margin = new Thickness(4, 0, 4, 0);
                var row = Column(IconRow(ClockDrawing, Row(slot.Start.Control, separator, slot.End.Control)));
                row.Children.Add(QuietAction("移除此时段", () => { extraSlots.Remove(slot); slotRows.Children.Remove(row); allDay.IsEnabled = extraSlots.Count == 0; }));
                slotRows.Children.Add(row);
            });
            advanced.Children.Add(Text("同一事项可分多个时段安排", 13, Muted)); advanced.Children.Add(slotRows); advanced.Children.Add(addSlot);
        }
        var retention = Column(Text("倒数日结束后", 13), pastMode); advanced.Children.Add(retention);
        void UpdateCountdown() => retention.Visibility = countdown.IsChecked == true ? Visibility.Visible : Visibility.Collapsed;
        countdown.Checked += (_, _) => UpdateCountdown(); countdown.Unchecked += (_, _) => UpdateCountdown(); UpdateCountdown();
        if (value?.RecurrenceType is not (null or "NONE") && scope == RecurrenceScope.Current) {
            recurrence.IsEnabled = false; repeatDetails.IsEnabled = false;
            advanced.Children.Add(Text("仅修改当前事项，循环规则保持不变", 12, Muted));
        }
        fields.Children.Add(new Expander { Header = "更多设置", Content = advanced, IsExpanded = value?.CountdownEnabled == true || value?.CheckInEnabled == true || value?.HiddenFromBoard == true });
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
                var rulesJson = allDayReminders.Json;
                if (reminder.IsChecked == true && (value?.AbsoluteReminderTimes.Count ?? 0) == 0 && (!isEvent && hasDue.IsChecked != true || (isEvent && allDay.IsChecked == true ? DesktopCalendarTime.ParseRules(rulesJson).Count == 0 : minutes.Count == 0))) throw new InvalidOperationException("开启提醒需要设置时间和至少一条提醒");
                var repeat = (recurrence.SelectedItem as Choice)?.Value ?? "NONE";
                var weekdayCsv = string.Join(",", weekdays.Where(day => day.IsChecked == true).Select(day => day.Tag));
                if (repeat == "WEEKLY" && weekdayCsv.Length == 0) throw new InvalidOperationException("请选择至少一个重复星期");
                if (extraSlots.Any(slot => slot.End.Value <= slot.Start.Value)) throw new InvalidOperationException("每个时段的结束时间必须晚于开始时间");
                result = new DesktopItemEdit(isEvent, title.Text.Trim(), notes.Text, location.Text,
                    !isEvent && hasDue.IsChecked == true ? Millis(due.Value) : null, isEvent ? DesktopCalendarTime.Millis(startAt, zoneId) : null, isEvent ? DesktopCalendarTime.Millis(endAt, zoneId) : null,
                    allDay.IsChecked == true, reminder.IsChecked == true, minutes, (group.SelectedItem as DesktopGroup)?.Id ?? 1,
                    color.SelectedItem?.ToString() ?? colors[0], countdown.IsChecked == true, repeat, weekdayCsv,
                    until.SelectedDate is { } date ? (long)(date.Date - DateTime.UnixEpoch.Date).TotalDays : null)
                    { HiddenFromBoard = hidden.IsChecked == true, CheckInEnabled = checkin.IsChecked == true,
                        CalendarId = isEvent ? ((DesktopCalendar)calendar.SelectedItem).Id : value?.CalendarId ?? "default",
                        CountdownPastMode = ((Choice)pastMode.SelectedItem).Value,
                        EventTimeZoneId = isEvent ? zoneId : "",
                        AllDayReminderRulesJson = !isEvent ? "" : allDay.IsChecked == true && value?.AllDay == true && string.IsNullOrWhiteSpace(value.AllDayReminderRulesJson) && rulesJson == initialAllDayRules ? "" : rulesJson,
                        EventBusyStatus = ((Choice)busy.SelectedItem).Value, EventVisibility = ((Choice)visibility.SelectedItem).Value,
                        EventParticipantsJson = isEvent ? participants.Json : "[]",
                        RecurrenceInterval = (int)interval.SelectedItem, RecurrenceInvalidDatePolicy = ((Choice)policy.SelectedItem).Value,
                        RecurrenceMonthlyOrdinal = repeat == "MONTHLY_NTH_WEEKDAY" ? int.Parse(((Choice)ordinal.SelectedItem).Value) : null,
                        RecurrenceMonthlyWeekday = repeat == "MONTHLY_NTH_WEEKDAY" ? int.Parse(((Choice)ordinalWeekday.SelectedItem).Value) : null,
                        AbsoluteReminderTimes = reminder.IsChecked == true ? value?.AbsoluteReminderTimes ?? Array.Empty<long>() : Array.Empty<long>(),
                        RecurrenceMonthlyDay = value != null && repeat == value.RecurrenceType ? value.RecurrenceMonthlyDay : null,
                        AdditionalTimeSlots = extraSlots.Select(slot => new DesktopTimeSlot(DesktopCalendarTime.Millis(slot.Start.Value, zoneId), DesktopCalendarTime.Millis(slot.End.Value, zoneId))).ToList() };
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
        source.FontFamily = InterfaceFont; source.FontSize = TextSize(15);
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
            foreach (var node in draft.Nodes) preview.Children.Add(new Border { Margin = new Thickness(Math.Min(node.Depth, 12) * 14, 4, 0, 4), Child = Text("○  " + node.Title + (node.DueMinuteOfDay is { } minute ? $" · 今天 {minute / 60:00}:{minute % 60:00} 提醒" : "") + (string.IsNullOrEmpty(node.Notes) ? "" : "\n" + node.Notes)) });
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
        var fields = Column(BrandHeading("想到就记", "- 做作业, #16:00 · 设置今天的 DDL 和提醒 · Enter 自动续写"),
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
        public void SetValue(DateTime value) { date.SelectedDate = value.Date; time.Text = value.ToString("HH:mm"); }
        public TimeField(long? millis, DateTime fallback, string? zoneId = null, bool compact = false, bool reverse = false)
        {
            var value = millis is { } existing ? DesktopCalendarTime.Wall(existing, zoneId) : fallback;
            date = new DatePicker { SelectedDate = value, Width = Math.Max(200, TextSize(180)), MinHeight = 42, Margin = new Thickness(0, 4, 12, 8) };
            time = Input(value.ToString("HH:mm")); time.Width = Math.Max(100, TextSize(85)); time.MinHeight = Math.Max(42, time.MinHeight); time.TextAlignment = TextAlignment.Center;
            time.Margin = new Thickness(0, 4, 0, 8);
            if (compact) {
                Integrated(date); Integrated(time);
                date.Language = System.Windows.Markup.XmlLanguage.GetLanguage("zh-Hans"); date.SelectedDateFormat = DatePickerFormat.Short;
                date.Width = TextSize(150); time.Width = TextSize(60);
                date.Padding = new Thickness(0); time.TextWrapping = TextWrapping.NoWrap;
            }
            System.Windows.Automation.AutomationProperties.SetName(time, "时间 HH:mm"); Control = reverse ? Row(time, date) : Row(date, time);
        }
    }
}
