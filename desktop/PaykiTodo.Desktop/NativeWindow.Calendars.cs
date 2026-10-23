using System.Text.Json.Nodes;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using static PaykiTodo.Desktop.NativeUi;
using CheckBox = System.Windows.Controls.CheckBox;
using ComboBox = System.Windows.Controls.ComboBox;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow
{
    private static ControlTemplate? calendarToggleTemplate;
    private bool CalendarVisible(DesktopItem item) => (workspace.Document!.Settings["hiddenCalendarIds"] as JsonArray)?.Any(value => value?.GetValue<string>() == item.CalendarId) != true;

    private UIElement CalendarList()
    {
        var list = Column(SectionHeader("日历", CalendarAction("＋", () => EditCalendar(null))));
        foreach (var calendar in workspace.Document!.Calendars) {
            var hidden = (workspace.Document.Settings["hiddenCalendarIds"] as JsonArray)?.Any(value => value?.GetValue<string>() == calendar.Id) == true;
            var fill = (System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString(calendar.ColorHex);
            var tick = .2126 * fill.R + .7152 * fill.G + .0722 * fill.B > 180 ? "#182033" : "White";
            var toggle = new CheckBox { Content = calendar.Name, IsChecked = !hidden, Foreground = Ink, BorderBrush = B(calendar.ColorHex), Background = B(calendar.ColorHex), Tag = tick, Margin = new Thickness(0, 7, 0, 7) };
            toggle.Template = calendarToggleTemplate ??= (ControlTemplate)System.Windows.Markup.XamlReader.Parse("""
                <ControlTemplate xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml" TargetType="CheckBox">
                  <StackPanel Orientation="Horizontal">
                    <Border x:Name="Box" Width="18" Height="18" CornerRadius="4" BorderThickness="1.5" BorderBrush="{TemplateBinding BorderBrush}" Background="Transparent" VerticalAlignment="Center">
                      <Path x:Name="Tick" Data="M 3,8 L 6,11 L 12,4" Stroke="{Binding Tag, RelativeSource={RelativeSource TemplatedParent}}" StrokeThickness="2" Visibility="Collapsed"/>
                    </Border>
                    <ContentPresenter Margin="9,0,0,0" VerticalAlignment="Center"/>
                  </StackPanel>
                  <ControlTemplate.Triggers><Trigger Property="IsChecked" Value="True"><Setter TargetName="Box" Property="Background" Value="{Binding Background, RelativeSource={RelativeSource TemplatedParent}}"/><Setter TargetName="Tick" Property="Visibility" Value="Visible"/></Trigger></ControlTemplate.Triggers>
                </ControlTemplate>
                """);
            System.Windows.Automation.AutomationProperties.SetName(toggle, "显示日历：" + calendar.Name);
            System.Windows.Automation.AutomationProperties.SetAutomationId(toggle, "Calendar.Visible." + calendar.Id);
            void ChangeVisibility() => Change(document => {
                var ids = (document.Settings["hiddenCalendarIds"] as JsonArray)?.Select(value => value!.GetValue<string>()).ToHashSet() ?? new HashSet<string>();
                if (toggle.IsChecked == true) ids.Remove(calendar.Id); else ids.Add(calendar.Id);
                document.Settings["hiddenCalendarIds"] = new JsonArray(ids.Select(id => (JsonNode?)JsonValue.Create(id)).ToArray());
            });
            toggle.Checked += (_, _) => ChangeVisibility(); toggle.Unchecked += (_, _) => ChangeVisibility();
            var menu = new ContextMenu(); var edit = new MenuItem { Header = "编辑日历" }; edit.Click += (_, _) => EditCalendar(calendar); menu.Items.Add(edit);
            if (calendar.Id != "default") { var remove = new MenuItem { Header = "删除日历" }; remove.Click += (_, _) => {
                if (Confirm("删除此日历？其中的日程和循环规则将移至常规日历，不会删除事项。")) Change(document => document.DeleteCalendar(calendar.Id));
            }; menu.Items.Add(remove); }
            toggle.ContextMenu = menu;
            list.Children.Add(toggle);
        }
        list.Children.Add(CalendarAction("管理日历", () => {
            var dialog = Dialog(this, "管理日历", 500, 520); var entries = Column(BrandHeading("管理日历"));
            foreach (var calendar in workspace.Document.Calendars) entries.Children.Add(Action(calendar.Name + "  ·  编辑", () => { dialog.Close(); EditCalendar(calendar); }));
            dialog.Content = DialogLayout(entries, Action("新增日历", () => { dialog.Close(); EditCalendar(null); }, true), Action("关闭", () => dialog.Close())); dialog.ShowDialog();
        }));
        return list;
    }

    private void EditCalendar(DesktopCalendar? calendar)
    {
        var dialog = Dialog(this, calendar == null ? "新增日历" : "编辑日历", 500, 420);
        var name = Input(calendar?.Name ?? ""); System.Windows.Automation.AutomationProperties.SetName(name, "日历名称");
        var colors = new[] { "#3370FF", "#16A369", "#DB5298", "#BD6B17", "#7955CC", "#D94646" };
        var color = new ComboBox { ItemsSource = colors.Append(calendar?.ColorHex ?? colors[0]).Distinct().ToList(), SelectedItem = calendar?.ColorHex ?? colors[0] };
        System.Windows.Automation.AutomationProperties.SetName(color, "日历颜色");
        var preview = Text("课程安排 · 09:00–09:30", 16, B(color.SelectedItem.ToString()!));
        color.SelectionChanged += (_, _) => preview.Foreground = B(color.SelectedItem.ToString()!);
        var body = Column(BrandHeading(dialog.Title), Text("名称"), name, Text("颜色"), color, preview);
        var save = Action("完成", () => {
            if (string.IsNullOrWhiteSpace(name.Text)) return;
            Change(document => document.SaveCalendar(calendar?.Id, name.Text, color.SelectedItem.ToString()!)); dialog.Close();
        }, true);
        var cancel = Action("取消", () => dialog.Close()); cancel.IsCancel = true;
        dialog.Content = DialogLayout(body, cancel, save); dialog.ShowDialog();
    }
}
