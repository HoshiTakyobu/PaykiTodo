using System.Text.Json.Nodes;
using System.Windows;
using System.Windows.Controls;
using static PaykiTodo.Desktop.NativeUi;
using ComboBox = System.Windows.Controls.ComboBox;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow
{
    private void ShowCalendarPreferences()
    {
        var document = workspace.Document!;
        var dialog = Dialog(this, "日历设置", 630, 690);
        var duration = new ComboBox { ItemsSource = new[] { 15, 30, 45, 60, 90, 120, document.CalendarDefaultDurationMinutes }.Distinct().Order().ToArray(), SelectedItem = document.CalendarDefaultDurationMinutes };
        var timed = Input(document.CalendarTimedDefaultRemindersCsv);
        var allDay = new AllDayReminderEditor(document.CalendarAllDayDefaultRemindersJson);
        var zones = new[] { "" }.Concat(TimeZoneInfo.GetSystemTimeZones().Select(DesktopCalendarTime.IanaId)).Distinct().Order().ToList();
        foreach (var value in new[] { document.CalendarDisplayTimeZoneId, document.CalendarAuxiliaryTimeZoneId }) if (!zones.Contains(value)) zones.Add(value);
        var display = new ComboBox { ItemsSource = zones, SelectedItem = document.CalendarDisplayTimeZoneId };
        var auxiliary = new ComboBox { ItemsSource = zones, SelectedItem = document.CalendarAuxiliaryTimeZoneId };
        var week = new ComboBox { ItemsSource = new[] { "星期一", "星期日" }, SelectedIndex = document.Settings["weekStartMode"]?.GetValue<string>() == "SUNDAY" ? 1 : 0 };
        System.Windows.Automation.AutomationProperties.SetName(duration, "默认日程时长");
        System.Windows.Automation.AutomationProperties.SetName(display, "日历显示时区");
        System.Windows.Automation.AutomationProperties.SetName(auxiliary, "辅助时区");
        var error = Text("", 13, B("#AF3A32"));
        var body = Column(BrandHeading("日历设置", "默认值用于新日程；已有日程保留自己的时间与提醒。"),
            SettingRow("默认时长", "新建日程使用的分钟数", duration), SettingRow("每周开始", "周视图与小月历的排列", week),
            Text("普通日程提前提醒（分钟）", 14), timed, Text("多个值用逗号分隔；留空表示默认不提醒。", 12, Muted),
            new Separator(), allDay.Control, new Separator(),
            SettingRow("显示时区", "留空时跟随设备时区", display), SettingRow("辅助时区", "留空关闭辅助时间标尺", auxiliary), error);
        dialog.Content = DialogLayout(body, Action("取消", () => dialog.Close()), Action("完成", () => {
            try {
                var offsets = timed.Text.Replace('，', ',').Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
                    .Select(value => int.TryParse(value, out var minutes) && minutes is >= 0 and <= 525600 ? minutes : throw new InvalidOperationException("提前提醒需为 0—525600 的整数分钟。")).Distinct().ToArray();
                var rules = allDay.Json; var displayId = display.SelectedItem?.ToString() ?? ""; var auxiliaryId = auxiliary.SelectedItem?.ToString() ?? "";
                DesktopCalendarTime.Zone(displayId); DesktopCalendarTime.Zone(auxiliaryId);
                workspace.Edit(value => {
                    value.Settings["calendarDefaultDurationMinutes"] = (int)duration.SelectedItem;
                    value.Settings["calendarTimedDefaultRemindersCsv"] = string.Join(',', offsets);
                    value.Settings["calendarAllDayDefaultRemindersJson"] = rules;
                    value.Settings["calendarDisplayTimeZoneId"] = displayId;
                    value.Settings["calendarAuxiliaryTimeZoneId"] = auxiliaryId;
                    value.Settings["weekStartMode"] = week.SelectedIndex == 1 ? "SUNDAY" : "MONDAY";
                });
                calendarScrollKey = null; calendarScrollOffset = null; dialog.Close(); Render();
            } catch (Exception failure) { error.Text = DesktopWorkspace.FriendlyError(failure); }
        }, true));
        dialog.ShowDialog();
    }
}
