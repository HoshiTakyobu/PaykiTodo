using System.Windows;
using System.Windows.Controls;
using static PaykiTodo.Desktop.NativeUi;
using CheckBox = System.Windows.Controls.CheckBox;

namespace PaykiTodo.Desktop;

internal sealed class NativeParticipantEditor
{
    private readonly Window owner;
    private DesktopCalendar calendar;
    private string draft;
    private readonly Dictionary<string, string> drafts = new();
    private readonly TextBlock organizer = Text("", 13, Muted);
    private readonly TextBlock summary = Text("", 14);
    internal StackPanel Control { get; }
    internal string Json => DesktopSnapshotDocument.IsSharedCalendar(calendar.Id) ? draft : "[]";
    internal NativeParticipantEditor(Window owner, DesktopCalendar calendar, string initial)
    {
        this.owner = owner; this.calendar = calendar; draft = initial;
        var choose = NativeUi.Action("选择参与者", Choose);
        System.Windows.Automation.AutomationProperties.SetAutomationId(choose, "Event.Participants.Choose");
        Control = Column(Text("参与者", 14, bold: true), organizer, summary, choose);
        SetCalendar(calendar);
    }
    internal void SetCalendar(DesktopCalendar selected)
    {
        drafts[calendar.Id] = draft;
        calendar = selected;
        var members = DesktopEventParticipants.Members(calendar).Select(value => value.Username).ToHashSet();
        draft = DesktopEventParticipants.Json(DesktopEventParticipants.Read(drafts.GetValueOrDefault(calendar.Id, "[]")).Where(value => members.Contains(value.Username)));
        Control.Visibility = DesktopSnapshotDocument.IsSharedCalendar(calendar.Id) ? Visibility.Visible : Visibility.Collapsed;
        organizer.Text = "组织者  ·  " + calendar.OwnerAccount;
        Refresh();
    }
    private void Refresh()
    {
        var members = DesktopEventParticipants.Members(calendar).ToDictionary(value => value.Username);
        var participants = DesktopEventParticipants.Read(draft);
        summary.Text = participants.Count == 0 ? "尚未选择参与者" : string.Join("、", participants.Select(value => members.GetValueOrDefault(value.Username)?.Label ?? value.Username));
    }
    private void Choose()
    {
        var dialog = Dialog(owner, "选择参与者", 500, 530);
        var chosen = DesktopEventParticipants.Read(draft).Select(value => value.Username).ToHashSet();
        var checks = DesktopEventParticipants.Members(calendar).Select(member => {
            var check = new CheckBox { Content = member.Label, IsChecked = chosen.Contains(member.Username), Tag = member.Username, Margin = new Thickness(0, 8, 0, 8) };
            System.Windows.Automation.AutomationProperties.SetAutomationId(check, "Event.Participant." + member.Username);
            return check;
        }).ToArray();
        var complete = NativeUi.Action("完成", () => {
                var requested = DesktopEventParticipants.Json(checks.Where(check => check.IsChecked == true).Select(check => new DesktopEventParticipant((string)check.Tag, "NEEDS_ACTION")));
                draft = DesktopEventParticipants.Select(requested, draft); drafts[calendar.Id] = draft; Refresh(); dialog.Close();
            }, true);
        complete.Background = B("#3370FF"); complete.BorderBrush = B("#3370FF");
        dialog.Content = DialogLayout(Column(BrandHeading("选择参与者", "仅可选择已加入“" + calendar.Name + "”的成员。"),
            checks.Length == 0 ? Text("当前日历还没有可选成员。", 14, Muted) : Scroll(Column(checks))),
            NativeUi.Action("取消", () => dialog.Close()), complete);
        dialog.ShowDialog();
    }
}
