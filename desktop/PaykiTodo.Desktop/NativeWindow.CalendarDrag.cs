using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using static PaykiTodo.Desktop.NativeUi;
using Point = System.Windows.Point;
using Cursors = System.Windows.Input.Cursors;

namespace PaykiTodo.Desktop;

internal sealed partial class NativeWindow
{
    internal static DesktopItemEdit CalendarDragEdit(DesktopItemEdit edit, int days, double verticalPixels, bool resize, string? displayZoneId = null)
    {
        var minutes = (int)Math.Round(verticalPixels * 60 / HourHeight / 15, MidpointRounding.AwayFromZero) * 15;
        var zone = displayZoneId ?? edit.EventTimeZoneId;
        var start = DesktopCalendarTime.Wall(edit.StartAtMillis!.Value, zone); var end = DesktopCalendarTime.Wall(edit.EndAtMillis!.Value, zone);
        if (resize) end = new[] { start.AddMinutes(15), end.AddMinutes(minutes) }.Max();
        else { start = start.AddDays(days).AddMinutes(minutes); end = end.AddDays(days).AddMinutes(minutes); }
        return edit with { StartAtMillis = DesktopCalendarTime.Millis(start, zone), EndAtMillis = DesktopCalendarTime.Millis(end, zone) };
    }

    private void AttachCalendarDrag(Border tile, DesktopItem item, Canvas dayCanvas, Grid grid, DateTime visibleStart, int count)
    {
        if (workspace.Document?.SharedWritable(item.CalendarId) != true) { tile.MouseLeftButtonUp += (_, e) => { Preview(item); e.Handled = true; }; return; }
        Point? origin = null; bool dragging = false, resizing = false;
        Canvas? overlay = null; DesktopItemEdit? candidate = null;
        var originalOpacity = tile.Opacity;
        void Reset() {
            origin = null; dragging = false; candidate = null; tile.Opacity = originalOpacity;
            if (overlay != null) grid.Children.Remove(overlay); overlay = null;
            if (tile.IsMouseCaptured) tile.ReleaseMouseCapture();
        }
        tile.Focusable = true;
        System.Windows.Automation.AutomationProperties.SetAutomationId(tile, "Calendar.Event." + item.Id);
        System.Windows.Automation.AutomationProperties.SetHelpText(tile, "拖动日程改期，拖动下边缘调整结束时间；按 Esc 取消");
        tile.MouseLeftButtonDown += (_, e) => {
            ClearCalendarDraft(); origin = e.GetPosition(grid); resizing = e.GetPosition(tile).Y >= tile.ActualHeight - 7;
            tile.Focus(); tile.CaptureMouse(); e.Handled = true;
        };
        tile.MouseMove += (_, e) => {
            tile.Cursor = e.GetPosition(tile).Y >= tile.ActualHeight - 7 ? Cursors.SizeNS : Cursors.Hand;
            if (origin is not { } point || e.LeftButton != MouseButtonState.Pressed) return;
            var current = e.GetPosition(grid);
            if (!dragging && (current - point).Length < 5) return;
            dragging = true;
            var columnWidth = Math.Max(1, (grid.ActualWidth - TimeAxisWidth) / count);
            var oldColumn = ((DateTime)dayCanvas.Tag - visibleStart).Days;
            var newColumn = Math.Clamp((int)((current.X - TimeAxisWidth) / columnWidth), 0, count - 1);
            var edit = workspace.Document?.EditFor(item.Id); if (edit == null) { Reset(); return; }
            try { candidate = CalendarDragEdit(edit, newColumn - oldColumn, current.Y - point.Y, resizing, workspace.Document?.CalendarDisplayTimeZoneId); }
            catch (InvalidOperationException) { candidate = null; overlay?.Children.Clear(); return; }
            if (overlay == null) { overlay = new Canvas { IsHitTestVisible = false }; Grid.SetColumnSpan(overlay, count + 1); System.Windows.Controls.Panel.SetZIndex(overlay, 25); grid.Children.Add(overlay); }
            overlay.Children.Clear(); tile.Opacity = .3;
            var at = CalendarLocal(item, candidate.StartAtMillis); var end = CalendarLocal(item, candidate.EndAtMillis);
            var column = Math.Clamp((at.Date - visibleStart).Days, 0, count - 1);
            var ghost = new Border { Width = columnWidth - 5, Height = Math.Max(24, Math.Min(24 * HourHeight, (end - at).TotalMinutes * HourHeight / 60)),
                Background = B("#3370FF"), CornerRadius = new CornerRadius(4), Padding = new Thickness(5), Opacity = .85,
                Child = Column(Text(item.Title, 12, B("#FFFFFF"), true), Text($"{at:MM-dd HH:mm} – {end:MM-dd HH:mm}", 11, B("#FFFFFF"))) };
            Canvas.SetLeft(ghost, TimeAxisWidth + columnWidth * column + 2); Canvas.SetTop(ghost, TimeInset + at.TimeOfDay.TotalMinutes * HourHeight / 60); overlay.Children.Add(ghost);
            e.Handled = true;
        };
        tile.MouseLeftButtonUp += (_, e) => {
            var save = dragging ? candidate : null; var wasDragging = dragging; Reset(); e.Handled = true;
            if (!wasDragging) { Preview(item); return; }
            if (save == null || save.StartAtMillis == item.StartAtMillis && save.EndAtMillis == item.EndAtMillis) return;
            var scope = item.RecurringSeriesId != null ? NativeEditors.ChooseRecurrenceScope(this, "调整循环日程时间") : RecurrenceScope.Current;
            if (scope != null) Change(document => document.SaveItem(save, item.Id, scope.Value));
        };
        tile.LostMouseCapture += (_, _) => { if (origin != null) Reset(); };
        tile.PreviewKeyDown += (_, e) => { if (e.Key == Key.Escape && origin != null) { Reset(); e.Handled = true; } };
        tile.Unloaded += (_, _) => Reset();
    }
}
