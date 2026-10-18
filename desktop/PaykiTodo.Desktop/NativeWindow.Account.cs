using System.Diagnostics;
using System.Text.Json;
using System.Windows;
using System.Windows.Controls;
using static PaykiTodo.Desktop.NativeUi;
using CheckBox = System.Windows.Controls.CheckBox;
using TextBox = System.Windows.Controls.TextBox;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow
{
    private UIElement LoginPage()
    {
        var rememberedStore = new RememberedLoginStore(); var remembered = rememberedStore.Load();
        var server = Input(ReadServerPreference()); var username = Input(remembered.Username);
        var password = new PasswordBox { Padding = new Thickness(10), Margin = new Thickness(0, 4, 0, 16), MinHeight = 42 };
        var agreed = new CheckBox { Content = "我已阅读并同意用户协议与隐私政策" };
        password.Password = remembered.Password;
        var rememberAccount = new CheckBox { Content = "记住账号", IsChecked = remembered.RememberAccount };
        var rememberPassword = new CheckBox { Content = "记住密码", IsChecked = remembered.RememberPassword };
        rememberAccount.Unchecked += (_, _) => { rememberPassword.IsChecked = false; rememberedStore.Save("", "", false, false); };
        rememberPassword.Checked += (_, _) => rememberAccount.IsChecked = true;
        rememberPassword.Unchecked += (_, _) => rememberedStore.ForgetPassword();
        var error = Text("", 13, B("#AF3A32"));
        var form = Column(Text("欢迎回来", 32, Forest, true), Text("在电脑上，接着安排你的一天。", 16, Muted),
            Text("云服务器", 13), server, Text("账号", 13), username, Text("密码", 13), password, agreed,
            Row(rememberAccount, rememberPassword), Text("密码由当前 Windows 用户加密保存，不进入备份或云同步。", 12, Muted),
            Action("查看隐私政策", () => ShowPrivacy()), error);
        async void Login(bool register)
        {
            if (string.IsNullOrWhiteSpace(username.Text) || password.Password.Length == 0) { error.Text = "请填写账号和密码"; return; }
            if (agreed.IsChecked != true) { error.Text = "请先阅读并同意用户协议与隐私政策"; return; }
            form.IsEnabled = false;
            try {
                var url = CloudAccountSync.NormalizeServerUrl(server.Text);
                var verification = await NativeVerification.GetTokenAsync(this, url);
                if (verification.Canceled) return;
                await workspace.LoginAsync(url, username.Text, password.Password, register, verification.Token);
                rememberedStore.Save(username.Text, password.Password, rememberAccount.IsChecked == true, rememberPassword.IsChecked == true);
                password.Clear();
                var folder = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PaykiTodo");
                File.WriteAllText(Path.Combine(folder, "native-server.txt"), url);
                reauthenticating = false; Navigate("每日看板");
            } catch (Exception failure) { error.Text = DesktopWorkspace.FriendlyError(failure); }
            finally { form.IsEnabled = true; }
        }
        form.Children.Add(Row(Action("登录", () => Login(false), true), Action("注册账号", () => Login(true))));
        if (reauthenticating) form.Children.Add(Action("返回离线工作区", () => { reauthenticating = false; Render(); }));
        var card = Card(form, 32); card.MaxWidth = 480; card.HorizontalAlignment = System.Windows.HorizontalAlignment.Center;
        card.Margin = new Thickness(20, 24, 20, 24); return Page(card, true);
    }
    private static string ReadServerPreference()
    {
        var folder = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PaykiTodo");
        try {
            var native = Path.Combine(folder, "native-server.txt"); if (File.Exists(native)) return File.ReadAllText(native).Trim();
            var packaged = Path.Combine(AppContext.BaseDirectory, "client-config.json");
            if (File.Exists(packaged)) return JsonDocument.Parse(File.ReadAllText(packaged)).RootElement.GetProperty("serverUrl").GetString() ?? "";
            var legacy = Path.Combine(folder, "desktop-preferences.json");
            if (File.Exists(legacy)) return JsonDocument.Parse(File.ReadAllText(legacy)).RootElement.GetProperty("ServerUrl").GetString() ?? "";
        } catch { }
        return "";
    }
    private void ShowPrivacy()
    {
        var window = Dialog(this, "用户协议与隐私政策", 650, 480);
        window.Content = new Border { Padding = new Thickness(28), Child = Scroll(Column(Text("你的数据由你掌握", 24, bold: true),
            Text("日程和待办保存在当前 Windows 用户的本机数据目录。登录后，客户端将加密的数据副本同步到你填写的服务器。账号、设备信息用于身份认证和同步管理。", 15),
            Text("提醒依赖电脑保持运行。退出程序、关机或休眠期间无法触发桌面提醒。请通过备份保存重要数据。", 15),
            Action("查看完整隐私政策", () => Process.Start(new ProcessStartInfo("https://github.com/HoshiTakyobu/PaykiTodo/blob/main/PRIVACY.md") { UseShellExecute = true })))) };
        window.ShowDialog();
    }
    private UIElement SettingsPage()
    {
        var body = Column();
        body.Children.Add(Card(Column(Text("外观", 20, bold: true), Action(Dark ? "切换浅色模式" : "切换深色模式", () => {
            Dark = !Dark; BuildShell(); Change(document => document.Settings["themeMode"] = Dark ? "DARK" : "LIGHT");
        }))));
        body.Children.Add(Card(Column(Text(workspace.Account?.Username ?? "本地账号", 24, Forest, true), Text(workspace.Status, 13, Muted),
            Text("联网后自动同步本机修改。登录过期时本地编辑和提醒继续可用。", 13, Muted),
            Row(Action("立即同步", async () => await workspace.SyncAsync(), true), Action("重新登录", () => { reauthenticating = true; Render(); }), Action("账号和设备", ShowAccountDetails), Action("退出登录", async () => {
                if (Confirm("退出当前账号？本地数据会保留，重新登录后可继续使用。")) await workspace.LogoutAsync();
            })))));
        body.Children.Add(Card(Column(Text("数据与备份", 20, bold: true), Text("迁移包可在手机与电脑间导入。导入或切换云端版本前会自动保留本机安全副本。", 13, Muted),
            Row(Action("导出备份", () => {
                var dialog = new Microsoft.Win32.SaveFileDialog { Filter = "PaykiTodo 迁移包|*.paykibak", FileName = "PaykiTodo-" + DateTime.Now.ToString("yyyyMMdd-HHmm") + ".paykibak" };
                if (dialog.ShowDialog(this) == true) { try { File.WriteAllBytes(dialog.FileName, workspace.Document!.ToArchiveBytes(includeSecrets: false)); workspace.Notify("备份已导出（不含 AI 密钥）"); } catch (Exception e) { workspace.Notify(e.Message); } }
            }), Action("导入备份", () => {
                var dialog = new Microsoft.Win32.OpenFileDialog { Filter = "PaykiTodo 迁移包|*.paykibak" };
                if (dialog.ShowDialog(this) == true && Confirm("用选中的迁移包替换本机数据？现有数据会先保留安全副本。"))
                    try { workspace.Import(File.ReadAllBytes(dialog.FileName)); } catch (Exception e) { workspace.Notify(e.Message); }
            }), Action("检查云端并恢复", () => ReviewCloud(false)), Action("保留本机上传", () => ReviewCloud(true)), Action("恢复操作前副本", () => {
                if (Confirm("恢复最近的操作前副本？当前数据也会先保存副本。")) try { workspace.RestoreSafetyCopy(); } catch (Exception error) { workspace.Notify(error.Message); }
            })))));
        var groups = Column(Text("分组管理", 20, bold: true));
        foreach (var group in workspace.Document!.Groups) {
            groups.Children.Add(Row(Text(group.Name, 16, B(group.ColorHex), true), Action("重命名", () => {
                var name = NativeEditors.Prompt(this, "分组名称", group.Name); if (name != null) Change(document => document.SaveGroup(group.Id, name, group.ColorHex));
            }), Action("删除", () => { if (Confirm("删除分组并将事项移至默认分组？")) Change(document => document.DeleteGroup(group.Id)); })));
        }
        groups.Children.Add(Action("＋ 新建分组", () => { var name = NativeEditors.Prompt(this, "分组名称"); if (name != null) Change(document => document.SaveGroup(null, name, "#4CB782")); }));
        body.Children.Add(Card(groups));
        body.Children.Add(Card(Column(Text("AI 与规划", 20, bold: true), Action("AI 服务配置", AiSettings))));
        body.Children.Add(Card(Column(Text("连接手机", 20, bold: true), Text("需要直接操作手机数据时，在手机开启“连接电脑”，然后在浏览器打开手机显示的地址。", 13, Muted),
            Action("打开手机局域网页面", () => {
                var text = NativeEditors.Prompt(this, "手机显示的连接地址", "http://192.168.");
                if (text == null) return;
                if (!Uri.TryCreate(text, UriKind.Absolute, out var uri) || uri.Scheme is not ("http" or "https")) { workspace.Notify("请输入手机显示的完整 http 或 https 地址"); return; }
                Process.Start(new ProcessStartInfo(uri.AbsoluteUri) { UseShellExecute = true });
            }))));
        body.Children.Add(Card(Column(Text("关于 PaykiTodo", 20, bold: true), Text("Windows 客户端 1.16.21", 14, Muted),
            Text("桌面提醒需要保持程序运行；手机的锁屏、通知权限与桌面小组件由 Android 系统管理。", 13, Muted), Action("隐私政策", ShowPrivacy))));
        return Page(body);
    }

    private async void ReviewCloud(bool keepLocal)
    {
        try {
            var review = await workspace.InspectCloudAsync();
            var document = review.Remote.Document;
            if (!Confirm($"云端版本 {review.Remote.Revision}：{document.Items.Count(item => !item.IsEvent)} 项待办、{document.Items.Count(item => item.IsEvent)} 项日程、{document.PlanningNotes.Count} 份规划文档。\n" +
                (keepLocal ? "确认将本机副本上传为新云端版本？" : "确认用云端副本替换本机？") + "\n操作前会保留双方恢复副本。")) return;
            await workspace.ResolveCloudAsync(review, keepLocal);
        } catch (Exception error) { workspace.Notify(DesktopWorkspace.FriendlyError(error)); }
    }
}
