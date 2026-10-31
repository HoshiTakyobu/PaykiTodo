using System.Diagnostics;
using System.Windows;
using System.Windows.Controls;
using static PaykiTodo.Desktop.NativeUi;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow
{
    private CancellationTokenSource? updateCancellation;
    private TextBlock? updateStatus;
    private UpdateRelease? availableUpdate;
    private UpdateManifest? availableManifest;
    private string updateMessage = "自动检查稳定版本；更新准备完成后下次启动生效。";
    private bool updateBusy;
    private System.Windows.Controls.Button? updateNotice;
    private FrameworkElement UpdateFooter()
    {
        updateNotice = Action("查看更新", () => { settingsCategory = 6; Navigate("设置"); });
        updateNotice.Visibility = availableUpdate == null ? Visibility.Collapsed : Visibility.Visible;
        return Row(status, updateNotice);
    }
    private DesktopUpdateStore UpdateStore => new(Environment.GetEnvironmentVariable("PAYKI_UPDATE_SEED") ?? AppContext.BaseDirectory, isolatedProfile ? Path.Combine(profileDirectory, "updates") : null);
    private FrameworkElement UpdateControls()
    {
        updateStatus = Text(updateMessage, 13, Muted);
        return Column(updateStatus, Row(Action("检查更新", async () => await CheckUpdate(true)), Action("查看发布页面", () => Process.Start(new ProcessStartInfo(availableUpdate?.Url ?? "https://github.com/HoshiTakyobu/PaykiTodo/releases/latest") { UseShellExecute = true })), Action("下载并准备更新", async () => {
            if (availableUpdate == null || availableManifest == null) { SetUpdateMessage("请先检查更新；较旧发布可通过发布页面安装。"); return; }
            await PrepareUpdate(true);
        }), Action("取消下载", () => updateCancellation?.Cancel())));
    }
    private void SetUpdateMessage(string message) { updateMessage = message; if (updateStatus != null) updateStatus.Text = message; if (updateNotice != null) updateNotice.Visibility = availableUpdate == null ? Visibility.Collapsed : Visibility.Visible; }
    private async Task CheckUpdate(bool manual)
    {
        if (updateBusy) return; updateBusy = true; updateCancellation = new CancellationTokenSource();
        try {
            using var http = new HttpClient { Timeout = TimeSpan.FromMinutes(15) };
            var updater = new DesktopUpdater(UpdateStore, http, DesktopUpdateProtocol.PublicKey());
            var result = await Task.Run(() => updater.CheckAsync(DesktopUpdateProtocol.ParseVersion(typeof(NativeWindow).Assembly.GetName().Version!.ToString(3)), manual, updateCancellation.Token));
            if (result.Release == null) { if (manual) SetUpdateMessage("当前没有更新的稳定版本。"); return; }
            availableUpdate = result.Release; availableManifest = result.Manifest;
            SetUpdateMessage("发现 " + result.Release.Tag + "。" + (result.Manifest == null ? "此发布暂无应用内更新包，请打开发布页面。" : "可后台准备更新。") + "\n" + result.Release.Notes[..Math.Min(3000, result.Release.Notes.Length)]);
        } catch (OperationCanceledException) { SetUpdateMessage("更新检查已取消，可重新检查。"); }
        catch (Exception error) { SetUpdateMessage("更新检查失败：" + (error is System.Net.Http.HttpRequestException ? "网络暂不可用，请稍后重试。" : error.Message)); }
        finally { updateBusy = false; updateCancellation?.Dispose(); updateCancellation = null; }
        if (!manual && availableManifest != null) await PrepareUpdate(false);
    }
    private async Task PrepareUpdate(bool manual)
    {
        if (updateBusy || availableUpdate == null || availableManifest == null) return;
        updateBusy = true; updateCancellation = new CancellationTokenSource(); SetUpdateMessage("正在后台下载并校验更新，当前程序可继续使用…");
        try {
            using var http = new HttpClient { Timeout = TimeSpan.FromMinutes(15) };
            var updater = new DesktopUpdater(UpdateStore, http, DesktopUpdateProtocol.PublicKey());
            SetUpdateMessage(await Task.Run(() => updater.PrepareAsync(availableUpdate, availableManifest, AppContext.BaseDirectory, typeof(NativeWindow).Assembly.GetName().Version!.ToString(3), manual, updateCancellation.Token)));
        } catch (OperationCanceledException) { SetUpdateMessage("下载已取消，当前版本继续可用。"); }
        catch (Exception error) { SetUpdateMessage("更新未应用：" + (error is System.Net.Http.HttpRequestException ? "下载失败，请重试或查看发布页面。" : error.Message)); }
        finally { updateBusy = false; updateCancellation?.Dispose(); updateCancellation = null; }
    }
    private void UpdateStartup(bool production)
    {
        var nonce = Environment.GetEnvironmentVariable("PAYKI_UPDATE_NONCE");
        if (!string.IsNullOrEmpty(nonce)) UpdateStore.Confirm(nonce);
        if (production) _ = CheckUpdate(false);
    }
}
