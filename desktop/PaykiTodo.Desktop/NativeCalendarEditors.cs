using System.Globalization;
using System.Windows;
using System.Windows.Controls;
using static PaykiTodo.Desktop.NativeUi;
using TextBox = System.Windows.Controls.TextBox;

namespace PaykiTodo.Desktop;

internal sealed class AllDayReminderEditor
{
    private readonly StackPanel rows = Column();
    private readonly List<(TextBox Days, TextBox Clock)> values = new();
    public StackPanel Control { get; }
    public AllDayReminderEditor(string json)
    {
        var add = NativeUi.Action("添加提醒", () => Add(new AllDayReminderRule(0, 480)));
        Control = Column(Text("全天提醒 · 日期与钟点", 13, Muted), rows, add);
        SetJson(json);
    }
    public void SetJson(string json)
    {
        rows.Children.Clear(); values.Clear();
        foreach (var rule in DesktopCalendarTime.ParseRules(json)) Add(rule);
    }
    private void Add(AllDayReminderRule rule)
    {
        var days = Input(rule.DaysBefore.ToString()); days.Width = 58;
        var clock = Input(TimeSpan.FromMinutes(rule.MinuteOfDay).ToString("hh\\:mm", CultureInfo.InvariantCulture)); clock.Width = 92;
        System.Windows.Automation.AutomationProperties.SetName(days, "全天提醒提前天数");
        System.Windows.Automation.AutomationProperties.SetName(clock, "全天提醒钟点");
        var value = (Days: days, Clock: clock); values.Add(value);
        var row = Row(Text("提前", 13), days, Text("天", 13), clock);
        var remove = NativeUi.Action("移除", () => { rows.Children.Remove(row); values.Remove(value); }); remove.MinHeight = 32; remove.Padding = new Thickness(10, 4, 10, 4);
        row.Children.Add(remove); rows.Children.Add(row);
    }
    public string Json => DesktopCalendarTime.RulesJson(values.Select(value => {
        if (!int.TryParse(value.Days.Text, out var days) || days is < 0 or > 365 ||
            !TimeSpan.TryParseExact(value.Clock.Text.Trim(), new[] { "h\\:mm", "hh\\:mm" }, CultureInfo.InvariantCulture, out var clock) || clock.TotalMinutes is < 0 or >= 1440)
            throw new InvalidOperationException("全天提醒请填写 0—365 天和 HH:mm 钟点。");
        return new AllDayReminderRule(days, (int)clock.TotalMinutes);
    }));
    public static string FromLegacyOffsets(IEnumerable<int> offsets) => DesktopCalendarTime.RulesJson(offsets.Select(offset => {
        var total = 540 - offset; var before = Math.Max(0, -(int)Math.Floor(total / 1440.0));
        return new AllDayReminderRule(before, total + before * 1440);
    }));
}
