using System.Text.Json;
using System.Windows;
using System.Windows.Controls;
using static PaykiTodo.Desktop.NativeUi;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow
{
    private async void ShowAccountDetails()
    {
        if (workspace.Account == null) { workspace.Notify("请先登录账号"); return; }
        var dialog = Dialog(this, "账号和设备", 720, 740);
        var body = Column(Text("正在读取账号信息…", 18, Muted));
        dialog.Content = new Border { Padding = new Thickness(26), Child = Scroll(body) }; dialog.Show();
        async Task Refresh()
        {
            try {
                var account = await workspace.AccountRequestAsync("/v2/account", HttpMethod.Get);
                var devices = await workspace.AccountRequestAsync("/v2/devices", HttpMethod.Get);
                if (!dialog.IsVisible) return;
                var user = account.GetProperty("user");
                string Read(JsonElement value, string key) => value.TryGetProperty(key, out var node) ? node.ToString() : "";
                body.Children.Clear();
                body.Children.Add(Text(Read(user, "displayName"), 24, Forest, true));
                body.Children.Add(Text("账号：" + Read(user, "username") + "    ID：" + Read(user, "id"), 13, Muted));
                body.Children.Add(Row(Action("修改昵称", async () => {
                    var name = NativeEditors.Prompt(dialog, "昵称", Read(user, "displayName")); if (name == null) return;
                    try { await workspace.AccountRequestAsync("/v2/account", HttpMethod.Post, new { displayName = name }); await Refresh(); }
                    catch (Exception error) { workspace.Notify(DesktopWorkspace.FriendlyError(error)); }
                }), Action("修改密码", () => ChangePasswordDialog(dialog))));
                body.Children.Add(Text("登录设备", 20, bold: true));
                foreach (var device in devices.GetProperty("devices").EnumerateArray()) {
                    var id = Read(device, "id"); var current = device.GetProperty("current").GetBoolean(); var revoked = device.GetProperty("revoked").GetBoolean();
                    var row = Column(Text(Read(device, "name") + (current ? " · 当前设备" : ""), 16, bold: true),
                        Text(Read(device, "platform") + " · " + (revoked ? "已撤销" : device.GetProperty("active").GetBoolean() ? "已登录" : "离线") +
                            " · 最近活动 " + DateTimeOffset.FromUnixTimeSeconds(device.GetProperty("lastSeenAt").GetInt64()).LocalDateTime.ToString("g"), 12, Muted));
                    if (!current && !revoked) row.Children.Add(Action("退出此设备", async () => {
                        if (!Confirm("撤销此设备的登录？")) return;
                        try { await workspace.AccountRequestAsync("/v2/devices/" + Uri.EscapeDataString(id), HttpMethod.Delete); await Refresh(); }
                        catch (Exception error) { workspace.Notify(DesktopWorkspace.FriendlyError(error)); }
                    }));
                    body.Children.Add(Card(row));
                }
            } catch (Exception error) { body.Children.Clear(); body.Children.Add(Text(DesktopWorkspace.FriendlyError(error), 16, Muted)); }
        }
        await Refresh();
    }
    private void ChangePasswordDialog(Window owner)
    {
        var dialog = Dialog(owner, "修改密码", 490, 480);
        PasswordBox InputPassword() => new() { Padding = new Thickness(10), Margin = new Thickness(0, 5, 0, 16) };
        var current = InputPassword(); var next = InputPassword(); var repeat = InputPassword(); var error = Text("", 13, B("#AF3A32"));
        var body = Column(Text("原密码"), current, Text("新密码（10—256 位）"), next, Text("再次输入新密码"), repeat, error);
        body.Children.Add(Action("完成", async () => {
            if (next.Password != repeat.Password) { error.Text = "两次新密码不一致"; return; }
            body.IsEnabled = false;
            try { await workspace.ChangePasswordAsync(current.Password, next.Password); dialog.Close(); owner.Close(); }
            catch (Exception failure) { error.Text = DesktopWorkspace.FriendlyError(failure); }
            finally { body.IsEnabled = true; }
        }, true));
        dialog.Content = new Border { Padding = new Thickness(26), Child = Scroll(body) }; dialog.ShowDialog();
    }
}
