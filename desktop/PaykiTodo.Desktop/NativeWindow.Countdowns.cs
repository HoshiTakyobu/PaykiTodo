using System.Windows;
using static PaykiTodo.Desktop.NativeUi;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow
{
    private bool reviewingCountdown;
    private readonly HashSet<long> deferredCountdowns = new();

    private void ReviewCountdowns()
    {
        if (exiting || !IsVisible || !IsLoaded || reviewingCountdown || reauthenticating || workspace.Document == null || OwnedWindows.Count > 0) return;
        var item = workspace.Document.Items.FirstOrDefault(item => !deferredCountdowns.Contains(item.Id) && workspace.Document.CountdownNeedsDecision(item, DateTime.Now));
        if (item == null) return;
        reviewingCountdown = true;
        var dialog = Dialog(this, "继续保留这个日子？", 500, 340);
        void Decide(string mode) { dialog.Close(); Change(document => document.SetCountdownPastMode(item.Id, mode)); }
        dialog.Content = DialogLayout(Column(BrandHeading("继续保留这个日子？"), Text(item.Title, 19, bold: true),
            Text("这项待办已完成或日程已结束。保留后，它会继续显示在“值得期待”和倒数日中，记录已经过去的时光。", 14, Muted)),
            Action("不再展示", () => Decide("HIDE")), Action("保留纪念", () => Decide("KEEP"), true));
        try { dialog.ShowDialog(); } finally { deferredCountdowns.Add(item.Id); reviewingCountdown = false; }
        Dispatcher.BeginInvoke(new Action(ReviewCountdowns));
    }
}
