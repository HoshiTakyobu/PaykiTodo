using System.IO.Compression;
using System.Text.Json.Nodes;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class CalendarSemanticsTests
{
    private static long At(DateTime value, string zone = "Asia/Shanghai") => DesktopCalendarTime.Millis(value, zone);
    private static DesktopItemEdit Event(DateTime value) => new(true, "课程", "原始备注", "教室", null, At(value), At(value.AddMinutes(30)), false,
        true, new[] { 15 }, 1, "#3370FF", false, "NONE", "", null) { EventTimeZoneId = "Asia/Shanghai", RecurrenceInvalidDatePolicy = "SKIP" };
    private static DateTime Start(DesktopItem item) => DesktopCalendarTime.Wall(item.StartAtMillis!.Value, item.EventTimeZoneId);

    [Fact] public void NewCalendarSkipsInvalidDatesWhileLegacyRulesStillClamp()
    {
        var date = new DateTime(2028, 1, 31, 9, 0, 0); var document = DesktopSnapshotDocument.CreateEmpty();
        document.SaveItem(Event(date) with { RecurrenceType = "MONTHLY_DAY" }); document.EnsureRecurringInstances(date, 92);
        Assert.Equal(new[] { "01-31", "03-31" }, document.Items.Select(item => Start(item).ToString("MM-dd")));
        var legacy = DesktopSnapshotDocument.CreateEmpty(); legacy.SaveItem(Event(date) with { RecurrenceType = "MONTHLY_DAY", RecurrenceInvalidDatePolicy = "CLAMP" });
        legacy.EnsureRecurringInstances(date, 92); Assert.Contains(legacy.Items, item => Start(item).Date == new DateTime(2028, 2, 29));
        document = DesktopSnapshotDocument.CreateEmpty(); date = new DateTime(2028, 2, 29, 9, 0, 0);
        document.SaveItem(Event(date) with { RecurrenceType = "YEARLY_DATE" }); document.EnsureRecurringInstances(date, 1462);
        Assert.Equal(new[] { 2028, 2032 }, document.Items.Select(item => Start(item).Year));
    }
    [Fact] public void InfiniteIntervalsKeepTheirOriginalPhaseAcrossLaterWindows()
    {
        var date = new DateTime(2026, 10, 24, 9, 0, 0); var document = DesktopSnapshotDocument.CreateEmpty();
        var id = document.SaveItem(Event(date) with { RecurrenceType = "DAILY", RecurrenceInterval = 30 });
        document.EnsureRecurringInstances(date, 90); document.EnsureRecurringInstances(date.AddDays(100), 65);
        Assert.Equal(new[] { 0, 30, 60, 90, 120, 150 }, document.Items.Select(item => (Start(item).Date - date.Date).Days));
        Assert.Null(document.EditFor(id)!.RecurrenceEndEpochDay);
        var restored = DesktopSnapshotDocument.FromArchiveBytes(document.ToArchiveBytes(false));
        Assert.False(restored.EnsureRecurringInstances(date.AddDays(100), 65));
    }
    [Fact] public void LastWeekdayDiffersFromFifthAndBiweeklyCutoffIsInclusive()
    {
        var date = new DateTime(2026, 10, 31, 9, 0, 0); var document = DesktopSnapshotDocument.CreateEmpty();
        document.SaveItem(Event(date) with { RecurrenceType = "MONTHLY_NTH_WEEKDAY", RecurrenceMonthlyOrdinal = -1, RecurrenceMonthlyWeekday = 6 });
        document.EnsureRecurringInstances(date, 92);
        Assert.Contains(document.Items, item => Start(item).Date == new DateTime(2026, 11, 28));
        var fifth = DesktopSnapshotDocument.CreateEmpty(); fifth.SaveItem(Event(date) with { RecurrenceType = "MONTHLY_NTH_WEEKDAY", RecurrenceMonthlyOrdinal = 5, RecurrenceMonthlyWeekday = 6 });
        fifth.EnsureRecurringInstances(date, 92); Assert.DoesNotContain(fifth.Items, item => Start(item).Month == 11);
        date = new DateTime(2026, 10, 24, 9, 0, 0); document = DesktopSnapshotDocument.CreateEmpty();
        document.SaveItem(Event(date) with { RecurrenceType = "WEEKLY", RecurrenceInterval = 2, RecurrenceWeekdays = "1,3,6", RecurrenceEndEpochDay = (long)(new DateTime(2026, 11, 18) - DateTime.UnixEpoch.Date).TotalDays });
        document.EnsureRecurringInstances(date, 60);
        Assert.Equal(new[] { "10-24", "11-02", "11-04", "11-07", "11-16", "11-18" }, document.Items.Select(item => Start(item).ToString("MM-dd")));
    }
    [Fact] public void AllDayRulesUseTheirOwnClockAndExplicitEmptyRulesDoNotFallback()
    {
        var date = new DateTime(2026, 10, 24); var document = DesktopSnapshotDocument.CreateEmpty();
        var id = document.SaveItem(Event(date) with { AllDay = true, EndAtMillis = At(date.AddDays(2)),
            AllDayReminderRulesJson = "[{\"daysBefore\":1,\"minuteOfDay\":540},{\"daysBefore\":0,\"minuteOfDay\":497}]" });
        Assert.Equal(new[] { At(date.AddDays(-1).AddHours(9)), At(date.AddHours(8).AddMinutes(17)) }, document.RemindersBetween(long.MinValue, long.MaxValue).Select(value => value.Trigger));
        document.SaveItem(document.EditFor(id)! with { AllDayReminderRulesJson = "[]" }, id);
        Assert.Empty(document.RemindersBetween(long.MinValue, long.MaxValue));
    }
    [Fact] public void CopyUsesCurrentInstanceAndKeepsIndependentReminderFields()
    {
        var date = new DateTime(2026, 10, 24, 9, 0, 0); var document = DesktopSnapshotDocument.CreateEmpty();
        var id = document.SaveItem(Event(date) with { RecurrenceType = "DAILY", ReminderOffsets = new[] { 0, 5 }, EventBusyStatus = "FREE", EventVisibility = "PRIVATE" });
        document.SaveItem(document.EditFor(id)! with { StartAtMillis = At(date.AddDays(1)), EndAtMillis = At(date.AddDays(1).AddMinutes(30)) }, id);
        var draft = document.CopyDraft(id); Assert.Equal(At(date.AddDays(1)), draft.StartAtMillis); Assert.Equal("NONE", draft.RecurrenceType);
        Assert.Equal(new[] { 0, 5 }, draft.ReminderOffsets.Order()); Assert.Equal("PRIVATE", draft.EventVisibility); Assert.Equal("FREE", draft.EventBusyStatus);
        var copy = document.SaveItem(draft); Assert.Null(document.Items.Single(item => item.Id == copy).RecurringSeriesId);
    }
    [Fact] public void ContentUpdatesKeepMovedInstancesButTimeUpdatesReturnToRuleSlots()
    {
        var date = new DateTime(2026, 10, 26, 9, 0, 0); var document = DesktopSnapshotDocument.CreateEmpty();
        document.SaveItem(Event(date) with { RecurrenceType = "DAILY", RecurrenceEndEpochDay = (long)(date.Date.AddDays(3) - DateTime.UnixEpoch.Date).TotalDays });
        document.EnsureRecurringInstances(date, 10); var moved = document.Items[3];
        document.SaveItem(document.EditFor(moved.Id)! with { StartAtMillis = At(date.AddDays(4)), EndAtMillis = At(date.AddDays(4).AddMinutes(30)) }, moved.Id);
        var split = document.Items[2]; document.SaveItem(document.EditFor(split.Id)! with { Title = "后段", Location = "新地点" }, split.Id, RecurrenceScope.CurrentAndFuture);
        Assert.Equal(At(date.AddDays(4)), document.Items.Single(item => item.Id == moved.Id).StartAtMillis);
        Assert.Equal("新地点", document.EditFor(moved.Id)!.Location); Assert.Equal(2, document.Items.Count(item => item.Title == "课程"));
        var tail = document.Items.Single(item => item.Id == split.Id); document.SaveItem(document.EditFor(tail.Id)! with { StartAtMillis = At(date.AddDays(2).AddHours(1)), EndAtMillis = At(date.AddDays(2).AddHours(1).AddMinutes(30)) }, tail.Id, RecurrenceScope.All);
        Assert.DoesNotContain(document.Items, item => Start(item).Date == date.Date.AddDays(4));
    }
    [Fact] public void TimeZonesPreserveWallClockAcrossDstAndRejectMissingTimes()
    {
        const string zone = "America/New_York";
        Assert.Equal(new DateTimeOffset(2026, 11, 1, 6, 30, 0, TimeSpan.Zero).ToUnixTimeMilliseconds(), At(new DateTime(2026, 11, 1, 1, 30, 0), zone));
        Assert.Throws<InvalidOperationException>(() => At(new DateTime(2026, 3, 8, 2, 30, 0), zone));
        var date = new DateTime(2026, 3, 7, 2, 30, 0); var document = DesktopSnapshotDocument.CreateEmpty();
        document.SaveItem(Event(date) with { EventTimeZoneId = zone, StartAtMillis = At(date, zone), EndAtMillis = At(date.AddMinutes(30), zone), RecurrenceType = "DAILY" });
        document.EnsureRecurringInstances(date, 2);
        Assert.Equal(new[] { 7, 9 }, document.Items.Select(item => Start(item).Day));
        Assert.All(document.Items, item => Assert.Equal(2, Start(item).Hour));
    }
    [Fact] public void CalendarDragUsesDisplayedWallClockAcrossDstAndRejectsGaps()
    {
        const string zone = "America/New_York"; var date = new DateTime(2026, 3, 7, 9, 0, 0);
        var edit = Event(date) with { StartAtMillis = At(date, zone), EndAtMillis = At(date.AddMinutes(30), zone) };
        var moved = NativeWindow.CalendarDragEdit(edit, 1, 0, false, zone);
        Assert.Equal(new DateTime(2026, 3, 8, 9, 0, 0), DesktopCalendarTime.Wall(moved.StartAtMillis!.Value, zone));
        Assert.Equal(23 * 3600000L, moved.StartAtMillis!.Value - edit.StartAtMillis!.Value);
        Assert.Equal("Asia/Shanghai", moved.EventTimeZoneId);
        edit = edit with { StartAtMillis = At(date.Date.AddHours(2).AddMinutes(30), zone), EndAtMillis = At(date.Date.AddHours(3), zone) };
        Assert.Throws<InvalidOperationException>(() => NativeWindow.CalendarDragEdit(edit, 1, 0, false, zone));
    }
    [Fact] public void CalendarMetadataOrderAndUnknownEventFieldsSurviveRoundTrip()
    {
        var document = DesktopSnapshotDocument.CreateEmpty(); var first = document.SaveCalendar(null, "课程", "#3370FF", "本学期", "student");
        var second = document.SaveCalendar(null, "生活", "#16A369"); document.MoveCalendar(second, -1);
        var restored = DesktopSnapshotDocument.FromArchiveBytes(document.ToArchiveBytes(false));
        Assert.Equal(new[] { "default", second, first }, restored.Calendars.Select(value => value.Id));
        Assert.Equal("本学期", restored.Calendars.Single(value => value.Id == first).Description);
        Assert.Equal("student", restored.Calendars.Single(value => value.Id == first).OwnerAccount);
    }
}
