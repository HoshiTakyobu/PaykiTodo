using Xunit;
using System.IO;

namespace PaykiTodo.Desktop.Tests;

public sealed class FeedbackDataTests
{
    [Fact] public void CalendarArchiveInteroperatesWithPhone()
    {
        var folder = Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE");
        if (string.IsNullOrEmpty(folder)) return;
        Directory.CreateDirectory(folder);
        var document = DesktopSnapshotDocument.CreateEmpty();
        document.SaveCalendar("cross-courses", "跨端课程表", "#7955CC");
        document.SaveItem(Event(DateTime.Today.AddDays(1).AddHours(9)) with { Title = "桌面跨端课程", CalendarId = "cross-courses" });
        var id = document.SaveItem(Event(DateTime.Today) with { IsEvent = false, Title = "跨端纪念", DueAtMillis = Millis(DateTime.Today.AddDays(-10)), CountdownEnabled = true });
        document.Complete(id); document.SetCountdownPastMode(id, "KEEP");
        File.WriteAllBytes(Path.Combine(folder, "calendar-desktop.paykibak"), document.ToArchiveBytes(false));
        var phonePath = Path.Combine(folder, "calendar-android.paykibak");
        if (!File.Exists(phonePath)) return;
        var phone = DesktopSnapshotDocument.FromArchiveBytes(File.ReadAllBytes(phonePath));
        Assert.Equal("cross-courses", phone.Items.Single(item => item.Title == "手机修改课程").CalendarId);
        Assert.Equal("手机修改课程表", phone.Calendars.Single(calendar => calendar.Id == "cross-courses").Name);
        Assert.Equal("KEEP", phone.Items.Single(item => item.Title == "跨端纪念").CountdownPastMode);
        Assert.True(phone.Items.Single(item => item.Title == "跨端纪念").Completed);
    }
    private static DesktopItemEdit Event(DateTime start) => new(true, "课程", "", "教室", null, Millis(start), Millis(start.AddMinutes(30)), false, false,
        Array.Empty<int>(), 1, "#7955CC", false, "NONE", "", null);
    private static long Millis(DateTime value) => new DateTimeOffset(value).ToUnixTimeMilliseconds();

    [Fact] public void CalendarOwnershipSurvivesEditArchiveAndRecurringReplenishment()
    {
        var document = DesktopSnapshotDocument.CreateEmpty();
        var calendar = document.SaveCalendar(null, "课程表", "#7955CC");
        var groupBefore = document.Groups.ToArray();
        var id = document.SaveItem(Event(DateTime.Today.AddDays(1).AddHours(9)) with { CalendarId = calendar, RecurrenceType = "DAILY", RecurrenceEndEpochDay = (long)(DateTime.Today.AddDays(5) - DateTime.UnixEpoch).TotalDays });
        document.SaveItem(document.EditFor(id)! with { Title = "高等数学" }, id);
        document.EnsureRecurringInstances(DateTime.Today);
        var restored = DesktopSnapshotDocument.FromArchiveBytes(document.ToArchiveBytes(false));
        Assert.Equal(groupBefore, restored.Groups);
        Assert.Contains(restored.Calendars, c => c.Id == calendar && c.Name == "课程表");
        Assert.All(restored.Items, item => Assert.Equal(calendar, item.CalendarId));
        restored.DeleteCalendar(calendar);
        restored.EnsureRecurringInstances(DateTime.Today.AddDays(3));
        Assert.All(restored.Items, item => Assert.Equal("default", item.CalendarId));
        Assert.Equal(document.Items.Count, restored.Items.Count);
    }

    [Fact] public void SevenDayBoundaryAndMemorialSurviveCompletedTaskRoundTrip()
    {
        var document = DesktopSnapshotDocument.CreateEmpty(); var now = DateTime.Now;
        var id = document.SaveItem(Event(now) with { IsEvent = false, StartAtMillis = null, EndAtMillis = null, DueAtMillis = Millis(now.Date.AddDays(-7)), CountdownEnabled = true });
        Assert.True(document.CountdownVisible(document.Items.Single(), now));
        document.SaveItem(document.EditFor(id)! with { DueAtMillis = Millis(now.Date.AddDays(-8)) }, id);
        Assert.False(document.CountdownVisible(document.Items.Single(), now));
        document.Complete(id);
        Assert.True(document.CountdownNeedsDecision(document.Items.Single(), DateTime.Now));
        document.SetCountdownPastMode(id, "KEEP");
        var restored = DesktopSnapshotDocument.FromArchiveBytes(document.ToArchiveBytes(false));
        Assert.True(restored.Items.Single().Completed);
        Assert.True(restored.CountdownVisible(restored.Items.Single(), now.AddYears(1)));
        Assert.False(restored.CountdownNeedsDecision(restored.Items.Single(), now));
        restored.SetCountdownPastMode(id, "HIDE");
        Assert.False(restored.CountdownVisible(restored.Items.Single(), now));
    }

    [Fact] public void CalendarSlotsUseQuarterHoursIncludingMidnightAndLastSlot()
    {
        var day = new DateTime(2026, 10, 23);
        Assert.Equal(day, NativeWindow.CalendarSlot(day, 0));
        Assert.Equal(day.AddHours(9).AddMinutes(15), NativeWindow.CalendarSlot(day, 12 + 9.4 * 48));
        Assert.Equal(day.AddHours(23).AddMinutes(45), NativeWindow.CalendarSlot(day, 2000));
    }
}
