using System.ComponentModel;

namespace PaykiTodo.Desktop;

internal sealed class ItemEditorDialog : Form
{
    private readonly TextBox titleBox = new() { Multiline = true, Height = 66, ScrollBars = ScrollBars.Vertical };
    private readonly TextBox notesBox = new() { Multiline = true, Height = 76, ScrollBars = ScrollBars.Vertical };
    private readonly TextBox locationBox = new();
    private readonly CheckBox hasDueBox = new() { Text = "设置 DDL", Checked = true, AutoSize = true };
    private readonly DateTimePicker dueDate = new() { Format = DateTimePickerFormat.Short };
    private readonly DateTimePicker dueTime = new() { Format = DateTimePickerFormat.Time, ShowUpDown = true };
    private readonly DateTimePicker startDate = new() { Format = DateTimePickerFormat.Short };
    private readonly DateTimePicker startTime = new() { Format = DateTimePickerFormat.Time, ShowUpDown = true };
    private readonly DateTimePicker endDate = new() { Format = DateTimePickerFormat.Short };
    private readonly DateTimePicker endTime = new() { Format = DateTimePickerFormat.Time, ShowUpDown = true };
    private readonly CheckBox allDayBox = new() { Text = "全天", AutoSize = true };
    private readonly CheckBox reminderBox = new() { Text = "提醒（全屏 + 通知栏）", AutoSize = true };
    private readonly TextBox offsetsBox = new() { PlaceholderText = "例如 30,15,5" };
    private readonly CheckBox countdownBox = new() { Text = "显示为倒数日", AutoSize = true };
    private readonly ComboBox groupBox = new() { DropDownStyle = ComboBoxStyle.DropDownList };
    private readonly ComboBox colorBox = new() { DropDownStyle = ComboBoxStyle.DropDownList };
    private readonly ComboBox recurrenceBox = new() { DropDownStyle = ComboBoxStyle.DropDownList };
    private readonly TextBox weekdaysBox = new() { PlaceholderText = "周循环填写 1-7，例如 1,3,5" };
    private readonly CheckBox recurrenceEndBox = new() { Text = "设置循环结束日期", AutoSize = true };
    private readonly DateTimePicker recurrenceEndDate = new() { Format = DateTimePickerFormat.Short };
    private readonly bool isEvent;
    private readonly IReadOnlyList<DesktopGroup> groups;

    private ItemEditorDialog(bool isEvent, IReadOnlyList<DesktopGroup> groups, DesktopItemEdit? existing)
    {
        this.isEvent = isEvent;
        this.groups = groups;
        Text = existing is null ? (isEvent ? "创建日程" : "新增待办") : (isEvent ? "修改日程" : "修改待办");
        Width = 620;
        Height = 760;
        MinimumSize = new Size(560, 620);
        StartPosition = FormStartPosition.CenterParent;
        BuildLayout();
        LoadValues(existing);
    }

    [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
    public DesktopItemEdit? Result { get; private set; }

    public static DesktopItemEdit? ShowEditor(IWin32Window owner, bool isEvent, IReadOnlyList<DesktopGroup> groups, DesktopItemEdit? existing = null)
    {
        using var dialog = new ItemEditorDialog(isEvent, groups, existing);
        return dialog.ShowDialog(owner) == DialogResult.OK ? dialog.Result : null;
    }

    private void BuildLayout()
    {
        var content = new TableLayoutPanel
        {
            Dock = DockStyle.Fill,
            AutoScroll = true,
            Padding = new Padding(18),
            ColumnCount = 1,
            RowCount = 1,
            AutoSize = true
        };
        content.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
        AddField(content, "标题", titleBox);
        AddField(content, "备注", notesBox);
        if (isEvent) AddField(content, "地点", locationBox);

        if (isEvent)
        {
            AddField(content, "开始时间", TimeRow(startDate, startTime));
            AddField(content, "结束时间", TimeRow(endDate, endTime));
            content.Controls.Add(allDayBox);
        }
        else
        {
            content.Controls.Add(hasDueBox);
            AddField(content, "DDL", TimeRow(dueDate, dueTime));
            hasDueBox.CheckedChanged += (_, _) =>
            {
                dueDate.Enabled = hasDueBox.Checked;
                dueTime.Enabled = hasDueBox.Checked;
            };
        }

        content.Controls.Add(reminderBox);
        AddField(content, "提前提醒（分钟）", offsetsBox);
        content.Controls.Add(countdownBox);

        foreach (var group in groups) groupBox.Items.Add(new GroupChoice(group.Id, group.Name));
        if (groupBox.Items.Count == 0) groupBox.Items.Add(new GroupChoice(1, "默认"));
        AddField(content, "分组", groupBox);

        colorBox.Items.AddRange(new object[]
        {
            new ColorChoice("翡翠绿", "#4CB782"), new ColorChoice("湖蓝", "#3B82F6"),
            new ColorChoice("珊瑚红", "#EF6A67"), new ColorChoice("明黄", "#E9A23B"),
            new ColorChoice("紫罗兰", "#8B5CF6"), new ColorChoice("青绿", "#14B8A6")
        });
        AddField(content, "颜色", colorBox);

        recurrenceBox.Items.AddRange(new object[]
        {
            new RecurrenceChoice("不重复", "NONE"), new RecurrenceChoice("每天", "DAILY"),
            new RecurrenceChoice("每周", "WEEKLY"), new RecurrenceChoice("每月", "MONTHLY"),
            new RecurrenceChoice("每年", "YEARLY")
        });
        AddField(content, "重复", recurrenceBox);
        AddField(content, "周循环星期", weekdaysBox);
        content.Controls.Add(recurrenceEndBox);
        AddField(content, "循环结束日期", recurrenceEndDate);
        recurrenceEndBox.CheckedChanged += (_, _) => recurrenceEndDate.Enabled = recurrenceEndBox.Checked;

        var buttons = new FlowLayoutPanel { Dock = DockStyle.Fill, FlowDirection = FlowDirection.RightToLeft, AutoSize = true, Padding = new Padding(0, 12, 0, 0) };
        var save = new Button { Text = "保存", AutoSize = true, Padding = new Padding(18, 6, 18, 6) };
        var cancel = new Button { Text = "取消", AutoSize = true, Padding = new Padding(18, 6, 18, 6), DialogResult = DialogResult.Cancel };
        save.Click += (_, _) => Save();
        buttons.Controls.Add(save);
        buttons.Controls.Add(cancel);
        content.Controls.Add(buttons);
        Controls.Add(content);
        AcceptButton = save;
        CancelButton = cancel;
    }

    private void LoadValues(DesktopItemEdit? value)
    {
        var now = DateTime.Now;
        var start = value?.StartAtMillis is { } startMillis ? DateTimeOffset.FromUnixTimeMilliseconds(startMillis).LocalDateTime : now.AddMinutes(5);
        var end = value?.EndAtMillis is { } endMillis ? DateTimeOffset.FromUnixTimeMilliseconds(endMillis).LocalDateTime : start.AddHours(1);
        var due = value?.DueAtMillis is { } dueMillis ? DateTimeOffset.FromUnixTimeMilliseconds(dueMillis).LocalDateTime : now.Date.AddHours(23).AddMinutes(59);
        titleBox.Text = value?.Title ?? "";
        notesBox.Text = value?.Notes ?? "";
        locationBox.Text = value?.Location ?? "";
        hasDueBox.Checked = value?.DueAtMillis is not null || isEvent;
        SetPicker(dueDate, dueTime, due);
        SetPicker(startDate, startTime, start);
        SetPicker(endDate, endTime, end);
        allDayBox.Checked = value?.AllDay ?? false;
        reminderBox.Checked = value?.ReminderEnabled ?? false;
        offsetsBox.Text = value is null ? "" : string.Join(',', value.ReminderOffsets);
        countdownBox.Checked = value?.CountdownEnabled ?? false;
        SelectBy(groupBox, choice => choice is GroupChoice group && group.Id == (value?.GroupId ?? groups.FirstOrDefault()?.Id ?? 1));
        SelectBy(colorBox, choice => choice is ColorChoice color && color.Value == (value?.AccentColor ?? "#4CB782"));
        SelectBy(recurrenceBox, choice => choice is RecurrenceChoice recurrence && recurrence.Value == (value?.RecurrenceType ?? "NONE"));
        weekdaysBox.Text = value?.RecurrenceWeekdays ?? "";
        recurrenceEndBox.Checked = value?.RecurrenceEndEpochDay is not null;
        recurrenceEndDate.Enabled = recurrenceEndBox.Checked;
        if (value?.RecurrenceEndEpochDay is { } endDay)
            recurrenceEndDate.Value = DateTime.UnixEpoch.Date.AddDays(endDay).ToLocalTime();
    }

    private void Save()
    {
        if (string.IsNullOrWhiteSpace(titleBox.Text))
        {
            MessageBox.Show(this, "标题不能为空", "PaykiTodo", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            return;
        }
        var start = Combine(startDate, startTime);
        var end = Combine(endDate, endTime);
        if (isEvent && end <= start)
        {
            MessageBox.Show(this, "日程结束时间必须晚于开始时间", "PaykiTodo", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            return;
        }
        var offsets = offsetsBox.Text.Replace('，', ',').Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(value => int.TryParse(value, out var parsed) ? parsed : -1).Where(value => value >= 0).Distinct().ToList();
        if (reminderBox.Checked && offsets.Count == 0) offsets.Add(0);
        var group = groupBox.SelectedItem as GroupChoice ?? new GroupChoice(1, "默认");
        var color = colorBox.SelectedItem as ColorChoice ?? new ColorChoice("翡翠绿", "#4CB782");
        var recurrence = recurrenceBox.SelectedItem as RecurrenceChoice ?? new RecurrenceChoice("不重复", "NONE");
        var recurrenceEnd = recurrenceEndBox.Checked
            ? (long?)(recurrenceEndDate.Value.Date.ToUniversalTime() - DateTime.UnixEpoch).TotalDays
            : null;
        Result = new DesktopItemEdit(
            isEvent,
            titleBox.Text,
            notesBox.Text,
            locationBox.Text,
            !isEvent && hasDueBox.Checked ? ToUnixMilliseconds(Combine(dueDate, dueTime)) : null,
            isEvent ? ToUnixMilliseconds(start) : null,
            isEvent ? ToUnixMilliseconds(end) : null,
            allDayBox.Checked,
            reminderBox.Checked,
            offsets,
            group.Id,
            color.Value,
            countdownBox.Checked,
            recurrence.Value,
            NormalizeWeekdays(weekdaysBox.Text),
            recurrenceEnd
        );
        DialogResult = DialogResult.OK;
        Close();
    }

    private static void AddField(TableLayoutPanel layout, string label, Control control)
    {
        layout.Controls.Add(new Label { Text = label, AutoSize = true, Padding = new Padding(0, 8, 0, 3) });
        control.Dock = DockStyle.Top;
        layout.Controls.Add(control);
    }

    private static FlowLayoutPanel TimeRow(DateTimePicker date, DateTimePicker time)
    {
        var row = new FlowLayoutPanel { Dock = DockStyle.Top, AutoSize = true, WrapContents = false };
        date.Width = 180;
        time.Width = 140;
        row.Controls.Add(date);
        row.Controls.Add(time);
        return row;
    }

    private static void SetPicker(DateTimePicker date, DateTimePicker time, DateTime value)
    {
        date.Value = value.Date;
        time.Value = DateTime.Today.Add(value.TimeOfDay);
    }

    private static DateTime Combine(DateTimePicker date, DateTimePicker time) => date.Value.Date.Add(time.Value.TimeOfDay);
    private static long ToUnixMilliseconds(DateTime local) => new DateTimeOffset(local).ToUnixTimeMilliseconds();

    private static string NormalizeWeekdays(string value)
    {
        return string.Join(',', value.Replace('，', ',')
            .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(token => int.TryParse(token, out var day) ? day : 0)
            .Where(day => day is >= 1 and <= 7)
            .Distinct()
            .OrderBy(day => day));
    }

    private static void SelectBy(ComboBox box, Func<object?, bool> predicate)
    {
        for (var index = 0; index < box.Items.Count; index++)
        {
            if (!predicate(box.Items[index])) continue;
            box.SelectedIndex = index;
            return;
        }
        if (box.Items.Count > 0) box.SelectedIndex = 0;
    }

    private sealed record GroupChoice(long Id, string Name) { public override string ToString() => Name; }
    private sealed record ColorChoice(string Name, string Value) { public override string ToString() => $"{Name}  {Value}"; }
    private sealed record RecurrenceChoice(string Name, string Value) { public override string ToString() => Name; }
}
