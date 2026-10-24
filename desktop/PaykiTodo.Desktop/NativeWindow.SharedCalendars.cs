using System.Net;
using System.Text.Json.Nodes;
using System.Windows;
using System.Windows.Controls;
using static PaykiTodo.Desktop.NativeUi;
using ComboBox = System.Windows.Controls.ComboBox;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow
{
    private bool SharedDisclosure() => Confirm("共享日历的日程内容由登录服务器保存，并按成员权限提供访问；它与个人加密同步副本分开。是否继续？");
    private async void EditSharedCalendarMetadata(DesktopCalendar current)
    {
        var id = current.Id[7..];
        try {
            var response = await workspace.SharedRequestAsync(HttpMethod.Get, "/v3/calendars/" + Uri.EscapeDataString(id));
            var metadata = response["calendar"]!.AsObject();
            if (metadata["canManage"]?.GetValue<bool>() != true) { ShowSharedMembers(metadata); return; }
            var dialog = Dialog(this, "编辑共享日历", 560, 530);
            var name = Input(metadata["name"]!.GetValue<string>()); var description = Input(metadata["description"]?.GetValue<string>() ?? "", true); description.MinHeight = 70;
            var color = new ComboBox { ItemsSource = new[] { current.ColorHex, "#3370FF", "#16A369", "#DB5298", "#7955CC", "#D94646" }.Distinct().ToArray(), SelectedItem = current.ColorHex };
            var error = Text("", 13, B("#AF3A32"));
            dialog.Content = DialogLayout(Column(BrandHeading("共享日历"), Text("名称", 13), name, Text("说明", 13), description, Text("颜色", 13), color, error),
                Action("成员", () => ShowSharedMembers(metadata)), Action("取消", () => dialog.Close()), Action("完成", async () => {
                    if (string.IsNullOrWhiteSpace(name.Text)) { error.Text = "请填写日历名称"; return; }
                    try {
                        var updated = await workspace.SharedRequestAsync(HttpMethod.Patch, "/v3/calendars/" + Uri.EscapeDataString(id), new JsonObject { ["expectedRevision"] = metadata["revision"]?.DeepClone(),
                            ["name"] = name.Text.Trim(), ["description"] = description.Text.Trim(), ["colorHex"] = color.SelectedItem.ToString() });
                        workspace.EditShared(data => {
                            data.SaveCalendar(current.Id, name.Text, color.SelectedItem.ToString()!, description.Text);
                            data.SharedCalendar(current.Id)!["sharedRevision"] = updated["calendar"]?["revision"]?.DeepClone();
                        }); dialog.Close();
                    } catch (Exception failure) { error.Text = DesktopWorkspace.FriendlyError(failure); }
                }, true)); dialog.ShowDialog();
        } catch (Exception error) { workspace.Notify(DesktopWorkspace.FriendlyError(error)); }
    }
    private void ShowSharedCalendars()
    {
        var dialog = Dialog(this, "共享日历与邀请", 690, 690);
        var rows = Column(); var status = Text("正在获取共享日历…", 13, Muted);
        async Task Refresh()
        {
            rows.IsEnabled = false;
            try {
                var response = await workspace.SharedRequestAsync(HttpMethod.Get, "/v3/calendars");
                var invitations = await workspace.SharedRequestAsync(HttpMethod.Get, "/v3/calendar-invitations");
                rows.Children.Clear();
                foreach (var calendar in (response["calendars"] as JsonArray ?? new()).OfType<JsonObject>()) {
                    var id = calendar["id"]!.GetValue<string>(); var localId = "shared:" + id;
                    var role = (calendar["role"]?.GetValue<string>() ?? "viewer").ToLowerInvariant();
                    var actions = new System.Windows.Controls.WrapPanel();
                    actions.Children.Add(Action("打开", async () => {
                        try {
                            var events = await workspace.SharedRequestAsync(HttpMethod.Get, "/v3/calendars/" + Uri.EscapeDataString(id) + "/events");
                            workspace.OpenSharedCalendar(calendar, events);
                            dialog.Close(); Navigate("日历");
                        } catch (Exception error) { status.Text = DesktopWorkspace.FriendlyError(error); }
                    }, true));
                    actions.Children.Add(Action("成员", () => ShowSharedMembers(calendar)));
                    if (workspace.Document?.SharedCalendar(localId)?["sharedConflictRevision"] != null)
                        actions.Children.Add(Action("处理冲突", () => ReviewSharedConflict(calendar)));
                    rows.Children.Add(Card(Column(Text(calendar["name"]!.GetValue<string>(), 18, bold: true), Text((role == "owner" ? "所有者" : role == "editor" ? "可编辑" : "只读") + " · " + (calendar["description"]?.GetValue<string>() ?? ""), 13, Muted), actions)));
                }
                var pending = (invitations["invitations"] as JsonArray ?? new()).OfType<JsonObject>().Where(value => value["status"]?.GetValue<string>()?.Equals("pending", StringComparison.OrdinalIgnoreCase) == true).ToArray();
                if (pending.Length > 0) rows.Children.Add(Text("待处理邀请", 17, bold: true));
                foreach (var invitation in pending) {
                    async void Respond(bool accept) {
                        if (accept && !SharedDisclosure()) return;
                        try {
                            await workspace.SharedRequestAsync(HttpMethod.Post, "/v3/calendar-invitations/" + Uri.EscapeDataString(invitation["id"]!.GetValue<string>()) + "/respond", new JsonObject { ["accept"] = accept });
                            await Refresh();
                        } catch (Exception error) { status.Text = DesktopWorkspace.FriendlyError(error); }
                    }
                    rows.Children.Add(Card(Column(Text(invitation["calendarName"]?.GetValue<string>() ?? "共享日历", 16, bold: true),
                        Text("来自 " + invitation["ownerUsername"]?.GetValue<string>(), 13, Muted), Row(Action("接受", () => Respond(true), true), Action("拒绝", () => Respond(false))))));
                }
                status.Text = rows.Children.Count == 0 ? "还没有共享日历或待处理邀请。" : "已获取当前账号可访问的共享日历。";
            } catch (Exception error) { status.Text = DesktopWorkspace.FriendlyError(error); }
            finally { rows.IsEnabled = true; }
        }
        dialog.Content = DialogLayout(Column(BrandHeading("共享日历与邀请", "共享内容与个人加密副本分别管理。"), status, rows),
            Action("新建共享日历", () => CreateSharedCalendar(async () => await Refresh()), true), Action("刷新", async () => await Refresh()), Action("关闭", () => dialog.Close()));
        dialog.Loaded += async (_, _) => await Refresh(); dialog.ShowDialog();
    }
    private void CreateSharedCalendar(Action finished)
    {
        var dialog = Dialog(this, "新建共享日历", 550, 470); var name = Input(); var description = Input("", true); description.MinHeight = 75;
        var color = new ComboBox { ItemsSource = new[] { "#3370FF", "#16A369", "#DB5298", "#7955CC", "#D94646" }, SelectedIndex = 0 };
        var error = Text("", 13, B("#AF3A32"));
        dialog.Content = DialogLayout(Column(BrandHeading("新建共享日历"), Text("名称", 13), name, Text("说明", 13), description, Text("颜色", 13), color, error),
            Action("取消", () => dialog.Close()), Action("完成", async () => {
                if (string.IsNullOrWhiteSpace(name.Text)) { error.Text = "请填写日历名称"; return; }
                if (!SharedDisclosure()) return;
                try {
                    var response = await workspace.SharedRequestAsync(HttpMethod.Post, "/v3/calendars", new JsonObject { ["name"] = name.Text.Trim(), ["description"] = description.Text.Trim(), ["colorHex"] = color.SelectedItem.ToString() });
                    var calendar = response["calendar"]!.AsObject(); var id = calendar["id"]!.GetValue<string>();
                    var events = await workspace.SharedRequestAsync(HttpMethod.Get, "/v3/calendars/" + Uri.EscapeDataString(id) + "/events");
                    workspace.EditShared(data => data.ImportSharedCalendar(calendar, events, workspace.Account!.ServerUrl));
                    dialog.Close(); finished();
                } catch (Exception failure) { error.Text = DesktopWorkspace.FriendlyError(failure); }
            }, true)); dialog.ShowDialog();
    }
    private void ShowSharedMembers(JsonObject calendar)
    {
        var id = calendar["id"]!.GetValue<string>(); var dialog = Dialog(this, "日历成员", 620, 580);
        var body = Column(BrandHeading(calendar["name"]!.GetValue<string>())); var error = Text("", 13, B("#AF3A32"));
        foreach (var member in (calendar["members"] as JsonArray ?? new()).OfType<JsonObject>()) {
            var name = member["username"]!.GetValue<string>(); var role = (member["role"]?.GetValue<string>() ?? "viewer").ToLowerInvariant();
            var row = Row(Text(name + " · " + (role == "owner" ? "所有者" : role == "editor" ? "可编辑" : "只读"), 14));
            if (calendar["canManage"]?.GetValue<bool>() == true && role != "owner") row.Children.Add(Action("移除", async () => {
                if (!Confirm("移除 " + name + " 的日历访问权限？")) return;
                try { await workspace.SharedRequestAsync(HttpMethod.Delete, "/v3/calendars/" + Uri.EscapeDataString(id) + "/members/" + Uri.EscapeDataString(name)); dialog.Close(); }
                catch (Exception failure) { error.Text = DesktopWorkspace.FriendlyError(failure); }
            }));
            body.Children.Add(row);
        }
        if (calendar["canManage"]?.GetValue<bool>() == true) {
            var username = Input(); var role = new ComboBox { ItemsSource = new[] { "可编辑", "只读" }, SelectedIndex = 1 };
            body.Children.Add(Text("邀请成员", 17, bold: true)); body.Children.Add(Text("账号", 13)); body.Children.Add(username); body.Children.Add(role);
            body.Children.Add(Action("发送邀请", async () => {
                if (string.IsNullOrWhiteSpace(username.Text)) { error.Text = "请填写成员账号"; return; }
                if (!Confirm("向 " + username.Text.Trim() + " 发送此日历的访问邀请？")) return;
                try {
                    await workspace.SharedRequestAsync(HttpMethod.Post, "/v3/calendars/" + Uri.EscapeDataString(id) + "/invitations", new JsonObject { ["username"] = username.Text.Trim(), ["role"] = role.SelectedIndex == 0 ? "editor" : "viewer" });
                    error.Text = "邀请已发送，等待对方接受。";
                } catch (Exception failure) { error.Text = DesktopWorkspace.FriendlyError(failure); }
            }, true));
        }
        body.Children.Add(error); dialog.Content = DialogLayout(body, Action("关闭", () => dialog.Close())); dialog.ShowDialog();
    }
    private async void ReviewSharedConflict(JsonObject calendar)
    {
        var id = calendar["id"]!.GetValue<string>(); var localId = "shared:" + id;
        try {
            var remote = await workspace.SharedRequestAsync(HttpMethod.Get, "/v3/calendars/" + Uri.EscapeDataString(id) + "/events");
            var dialog = Dialog(this, "共享日历版本冲突", 630, 570); var body = Column(BrandHeading("云端日程", "本机修改已保留；选择前请检查云端内容。"));
            foreach (var value in (remote["events"] as JsonArray ?? new()).OfType<JsonObject>()) body.Children.Add(Text(value["title"]?.GetValue<string>() ?? "日程", 14));
            var error = Text("", 13, B("#AF3A32")); body.Children.Add(error);
            dialog.Content = DialogLayout(body, Action("稍后处理", () => dialog.Close()), Action("使用云端版本", () => {
                if (!Confirm("使用已查看的云端版本？本机当前内容会先保留安全副本。")) return;
                workspace.AcceptSharedCloud(calendar, remote); dialog.Close();
            }), Action("保留本机版本", async () => {
                if (!Confirm("用本机版本替换已查看的云端版本？如果云端再次变化，将继续停止覆盖。")) return;
                try {
                    await workspace.KeepSharedLocalAsync(calendar, remote); dialog.Close();
                } catch (Exception failure) { error.Text = DesktopWorkspace.FriendlyError(failure); }
            }, true)); dialog.ShowDialog();
        } catch (Exception failure) { workspace.Notify(DesktopWorkspace.FriendlyError(failure)); }
    }
}
