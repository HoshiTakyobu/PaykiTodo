using System.IO;
using System.IO.Compression;
using System.Text.Json.Nodes;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class RecurrenceScopeTests
{
    private static long Ms(DateTime time) => new DateTimeOffset(time).ToUnixTimeMilliseconds();
    private static long Day(DateTime date) => (long)(date.Date - DateTime.UnixEpoch.Date).TotalDays;
    private static DateTime Local(long? time) => DateTimeOffset.FromUnixTimeMilliseconds(time!.Value).LocalDateTime;
    private static DesktopItemEdit Event(DateTime start, int days = 5) => new(true, "循环课", "保留备注", "教室", null, Ms(start), Ms(start.AddHours(2)), false, true,
        new[] { 15 }, 1, "#3370FF", false, "DAILY", "", Day(start.AddDays(days)));
    private static JsonObject Json(DesktopSnapshotDocument doc) {
        using var zip = new ZipArchive(new MemoryStream(doc.ToArchiveBytes(false)));
        using var reader = new StreamReader(zip.GetEntry("backup.json")!.Open());
        return JsonNode.Parse(reader.ReadToEnd())!.AsObject();
    }
    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public void ReplenishedOccurrencesKeepRelativeRulesWithoutCopyingAbsoluteReminders(bool isEvent)
    {
        var start = new DateTime(2026, 11, 1, 9, 0, 0);
        var absolute = Ms(start.AddDays(30).AddHours(-1));
        var doc = DesktopSnapshotDocument.CreateEmpty();
        var draft = Event(start, 180) with { AbsoluteReminderTimes = new[] { absolute } };
        if (!isEvent) draft = draft with { IsEvent = false, DueAtMillis = Ms(start), StartAtMillis = null, EndAtMillis = null };
        var owner = doc.SaveItem(draft);

        Assert.True(doc.EnsureRecurringInstances(start, 5));
        Assert.Equal(new[] { absolute }, doc.EditFor(owner)!.AbsoluteReminderTimes);
        Assert.All(doc.Items.Where(item => item.Id != owner), item => {
            var edit = doc.EditFor(item.Id)!;
            Assert.Empty(edit.AbsoluteReminderTimes);
            Assert.True(edit.ReminderEnabled);
            var anchor = isEvent ? edit.StartAtMillis!.Value : edit.DueAtMillis!.Value;
            Assert.Equal(new[] { anchor - 15 * 60_000L }, DesktopCalendarTime.Triggers(edit));
        });

        var restored = DesktopSnapshotDocument.FromArchiveBytes(doc.ToArchiveBytes(false));
        Assert.True(restored.EnsureRecurringInstances(start.AddDays(60), 5));
        Assert.Equal(new[] { absolute }, restored.EditFor(owner)!.AbsoluteReminderTimes);
        Assert.All(restored.Items.Where(item => item.Id != owner), item => Assert.Empty(restored.EditFor(item.Id)!.AbsoluteReminderTimes));
        Assert.False(restored.EnsureRecurringInstances(start.AddDays(60), 5));
    }
    [Fact]
    public void ReplenishmentOfAbsoluteOnlyOccurrenceDoesNotEnableEmptySiblingReminders()
    {
        var start = new DateTime(2026, 11, 1, 9, 0, 0);
        var absolute = Ms(start.AddDays(30));
        var doc = DesktopSnapshotDocument.CreateEmpty();
        var owner = doc.SaveItem(Event(start) with { ReminderOffsets = Array.Empty<int>(), AbsoluteReminderTimes = new[] { absolute } });
        Assert.True(doc.EnsureRecurringInstances(start));
        Assert.Equal(new[] { absolute }, DesktopCalendarTime.Triggers(doc.EditFor(owner)!));
        Assert.All(doc.Items.Where(item => item.Id != owner), item => {
            var edit = doc.EditFor(item.Id)!;
            Assert.Empty(edit.AbsoluteReminderTimes);
            Assert.False(edit.ReminderEnabled);
            Assert.Empty(DesktopCalendarTime.Triggers(edit));
            Assert.Null(Json(doc)["tasks"]!.AsArray().Single(task => task!["id"]!.GetValue<long>() == item.Id)!["reminderAtMillis"]);
        });
        Assert.Single(doc.RemindersBetween(absolute - 1, absolute + 1));
    }
    [Theory]
    [InlineData(true, true, true)]
    [InlineData(true, true, false)]
    [InlineData(true, false, true)]
    [InlineData(true, false, false)]
    [InlineData(false, true, true)]
    [InlineData(false, true, false)]
    [InlineData(false, false, true)]
    [InlineData(false, false, false)]
    public void SharedSeriesEditsKeepEachOccurrencesOwnAbsoluteReminder(bool isEvent, bool all, bool selectOwner)
    {
        var start = new DateTime(2026, 11, 1, 9, 0, 0);
        var absolute = Ms(start.AddDays(30));
        var doc = DesktopSnapshotDocument.CreateEmpty();
        var draft = Event(start) with { AbsoluteReminderTimes = new[] { absolute } };
        if (!isEvent) draft = draft with { IsEvent = false, DueAtMillis = Ms(start), StartAtMillis = null, EndAtMillis = null };
        var owner = doc.SaveItem(draft);
        doc.EnsureRecurringInstances(start);
        var secondOwner = doc.Items[2].Id;
        var secondAbsolute = absolute + 60_000;
        doc.SaveItem(doc.EditFor(secondOwner)! with { AbsoluteReminderTimes = new[] { secondAbsolute } }, secondOwner);
        var selected = selectOwner ? owner : doc.Items[1].Id;
        doc.SaveItem(doc.EditFor(selected)! with { Title = "共同改名", Location = "新教室" }, selected,
            all ? RecurrenceScope.All : RecurrenceScope.CurrentAndFuture);

        Assert.Equal(new[] { absolute }, doc.EditFor(owner)!.AbsoluteReminderTimes);
        Assert.Equal(new[] { secondAbsolute }, doc.EditFor(secondOwner)!.AbsoluteReminderTimes);
        Assert.All(doc.Items.Where(item => item.Id != owner && item.Id != secondOwner), item => Assert.Empty(doc.EditFor(item.Id)!.AbsoluteReminderTimes));
        Assert.All(doc.Items.Where(item => all || selectOwner || item.Id != owner), item => Assert.Equal("共同改名", item.Title));
        Assert.Equal(2, doc.RemindersBetween(absolute - 1, secondAbsolute + 1).Count);
    }
    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public void RenamingAbsoluteOnlySeriesKeepsEnabledOwnerAndDisablingRemindersIsExplicit(bool selectOwner)
    {
        var start = new DateTime(2026, 11, 1, 9, 0, 0);
        var absolute = Ms(start.AddDays(30));
        var doc = DesktopSnapshotDocument.CreateEmpty();
        var owner = doc.SaveItem(Event(start) with { ReminderOffsets = Array.Empty<int>(), AbsoluteReminderTimes = new[] { absolute } });
        doc.EnsureRecurringInstances(start);
        var selected = selectOwner ? owner : doc.Items[1].Id;
        doc.SaveItem(doc.EditFor(selected)! with { Title = "只改标题" }, selected, RecurrenceScope.All);

        Assert.True(doc.EditFor(owner)!.ReminderEnabled);
        Assert.Equal(new[] { absolute }, DesktopCalendarTime.Triggers(doc.EditFor(owner)!));
        Assert.All(doc.Items.Where(item => item.Id != owner), item => Assert.False(doc.EditFor(item.Id)!.ReminderEnabled));
        doc.SaveItem(doc.EditFor(owner)! with { ReminderEnabled = false, AbsoluteReminderTimes = Array.Empty<long>() }, owner, RecurrenceScope.All);
        Assert.All(doc.Items, item => { Assert.False(doc.EditFor(item.Id)!.ReminderEnabled); Assert.Empty(doc.EditFor(item.Id)!.AbsoluteReminderTimes); });
        Assert.Empty(doc.RemindersBetween(absolute - 1, absolute + 1));
    }
    [Fact]
    public void ChangingSeriesTimeKeepsAbsoluteReminderOwnerWhileNewDatesUseOnlyRelativeRules()
    {
        var start = new DateTime(2026, 11, 1, 9, 0, 0);
        var absolute = Ms(start.AddDays(30));
        var doc = DesktopSnapshotDocument.CreateEmpty();
        var owner = doc.SaveItem(Event(start) with { AbsoluteReminderTimes = new[] { absolute } });
        doc.EnsureRecurringInstances(start);
        doc.SaveItem(doc.EditFor(owner)! with { StartAtMillis = Ms(start.AddHours(1)), EndAtMillis = Ms(start.AddHours(3)),
            RecurrenceEndEpochDay = Day(start.AddDays(8)) }, owner, RecurrenceScope.All);

        Assert.Equal(9, doc.Items.Count);
        Assert.Equal(new[] { absolute }, doc.EditFor(owner)!.AbsoluteReminderTimes);
        Assert.All(doc.Items, item => Assert.Equal(10, Local(item.StartAtMillis).Hour));
        Assert.All(doc.Items.Where(item => item.Id != owner), item => {
            var edit = doc.EditFor(item.Id)!;
            Assert.Empty(edit.AbsoluteReminderTimes);
            Assert.Equal(new[] { edit.StartAtMillis!.Value - 15 * 60_000L }, DesktopCalendarTime.Triggers(edit));
        });
        Assert.Single(doc.RemindersBetween(absolute - 1, absolute + 1));
    }
    [Fact] public void MovingOnlyCurrentDoesNotAlterSiblingsOrRegenerateOriginalDate()
    {
        var start = new DateTime(2026, 10, 23, 23, 0, 0); var doc = DesktopSnapshotDocument.CreateEmpty();
        var id = doc.SaveItem(Event(start)); doc.EnsureRecurringInstances(start);
        var siblings = doc.Items.Where(i => i.Id != id).ToArray();
        var template = Json(doc)["templates"]!.ToJsonString();
        doc.SaveItem(doc.EditFor(id)! with { Title = "本次改期", StartAtMillis = Ms(start.AddDays(2)), EndAtMillis = Ms(start.AddDays(2).AddHours(3)) }, id);
        Assert.Equal(siblings, doc.Items.Where(i => i.Id != id));
        Assert.Equal(template, Json(doc)["templates"]!.ToJsonString());
        Assert.False(doc.EnsureRecurringInstances(start));
        doc.Delete(id); Assert.False(doc.EnsureRecurringInstances(start));
        Assert.Equal(Day(start), Json(doc)["recurringInstanceSkips"]![0]!["instanceEpochDay"]!.GetValue<long>());
    }
    [Fact] public void FutureEditBranchesSeriesAndKeepsEarlierDatesAndCheckins()
    {
        var start = new DateTime(2026, 10, 23, 9, 0, 0); var doc = DesktopSnapshotDocument.CreateEmpty();
        doc.SaveItem(Event(start)); doc.EnsureRecurringInstances(start);
        var earlier = doc.Items.Take(2).ToArray(); var selected = doc.Items[2]; doc.ToggleCheckIn(selected.Id);
        doc.SaveItem(doc.EditFor(selected.Id)! with { Title = "以后改为十点", StartAtMillis = Ms(start.AddDays(2).AddHours(1)), EndAtMillis = Ms(start.AddDays(2).AddHours(2)) }, selected.Id, RecurrenceScope.CurrentAndFuture);
        Assert.Equal(earlier, doc.Items.Take(2));
        Assert.All(doc.Items.Skip(2), i => { Assert.Equal("以后改为十点", i.Title); Assert.Equal(10, Local(i.StartAtMillis).Hour); Assert.NotEqual(earlier[0].RecurringSeriesId, i.RecurringSeriesId); });
        Assert.True(doc.CheckedIn(selected.Id));
        Assert.False(doc.EnsureRecurringInstances(start));
        Assert.Equal(6, doc.Items.Count);
    }
    [Fact] public void AllEditStartsAtFirstActiveDateAndPreservesCompletedTodoHistory()
    {
        var start = new DateTime(2026, 10, 23, 9, 0, 0); var doc = DesktopSnapshotDocument.CreateEmpty();
        var id = doc.SaveItem(Event(start) with { IsEvent = false, DueAtMillis = Ms(start), StartAtMillis = null, EndAtMillis = null });
        doc.EnsureRecurringInstances(start); doc.Complete(id); var completed = doc.Items.Single(i => i.Id == id);
        var selected = doc.Items[3];
        doc.SaveItem(doc.EditFor(selected.Id)! with { Title = "全体改名", DueAtMillis = Ms(Local(selected.DueAtMillis).AddHours(1)) }, selected.Id, RecurrenceScope.All);
        Assert.Equal(completed, doc.Items.Single(i => i.Id == id));
        Assert.Equal(6, doc.Items.Count); Assert.False(doc.EnsureRecurringInstances(start));
        Assert.All(doc.Items.Where(i => !i.Completed), i => { Assert.Equal("全体改名", i.Title); Assert.Equal(10, Local(i.DueAtMillis).Hour); });
    }
    [Fact] public void DeletingLastMaterializedOccurrenceStillReplenishesTemplate()
    {
        var start = new DateTime(2026, 10, 23, 9, 0, 0); var doc = DesktopSnapshotDocument.CreateEmpty();
        var id = doc.SaveItem(Event(start)); doc.Delete(id);
        Assert.True(doc.EnsureRecurringInstances(start)); Assert.Equal(5, doc.Items.Count);
        Assert.All(doc.Items, i => Assert.True(Local(i.StartAtMillis).Date > start.Date));
        Assert.All(doc.Items, i => Assert.True(doc.EditFor(i.Id)!.ReminderEnabled));
        var selected = doc.Items[2]; doc.Delete(selected.Id, RecurrenceScope.CurrentAndFuture);
        Assert.Equal(2, doc.Items.Count); Assert.False(doc.EnsureRecurringInstances(start));
        doc.Cancel(doc.Items[0].Id, RecurrenceScope.All); Assert.All(doc.Items, i => Assert.True(i.Canceled));
        Assert.False(doc.EnsureRecurringInstances(start));
    }
    [Fact] public void MonthEndLeapYearAndNthWeekdayMatchPhoneRules()
    {
        var doc = DesktopSnapshotDocument.CreateEmpty(); var start = new DateTime(2028, 1, 31, 9, 0, 0);
        doc.SaveItem(Event(start, 90) with { RecurrenceType = "MONTHLY_DAY" }); doc.EnsureRecurringInstances(start, 90);
        Assert.Equal(new[] { "01-31", "02-29", "03-31", "04-30" }, doc.Items.Select(i => Local(i.StartAtMillis).ToString("MM-dd")));
        doc = DesktopSnapshotDocument.CreateEmpty(); start = new DateTime(2028, 2, 29, 9, 0, 0);
        doc.SaveItem(Event(start, 730) with { RecurrenceType = "YEARLY_DATE" }); doc.EnsureRecurringInstances(start, 730);
        Assert.Contains(doc.Items, i => Local(i.StartAtMillis).Date == new DateTime(2029, 2, 28));
        doc = DesktopSnapshotDocument.CreateEmpty(); start = new DateTime(2026, 10, 23, 9, 0, 0);
        doc.SaveItem(Event(start, 62) with { RecurrenceType = "MONTHLY_NTH_WEEKDAY" }); doc.EnsureRecurringInstances(start, 62);
        Assert.Equal(new[] { "10-23", "11-27" }, doc.Items.Select(i => Local(i.StartAtMillis).ToString("MM-dd")));
    }
    [Fact] public void LunarNewYearAndLeapMonthFallbackMatchPhone()
    {
        var doc = DesktopSnapshotDocument.CreateEmpty(); var start = new DateTime(2026, 2, 17, 9, 0, 0);
        doc.SaveItem(Event(start, 730) with { RecurrenceType = "YEARLY_LUNAR_DATE" }); doc.EnsureRecurringInstances(start, 730);
        Assert.Contains(doc.Items, i => Local(i.StartAtMillis).Date == new DateTime(2027, 2, 6));
        Assert.Contains(doc.Items, i => Local(i.StartAtMillis).Date == new DateTime(2028, 1, 26));
        doc = DesktopSnapshotDocument.CreateEmpty(); start = new DateTime(2023, 3, 22, 9, 0, 0); // 闰二月初一
        doc.SaveItem(Event(start, 400) with { RecurrenceType = "YEARLY_LUNAR_DATE" }); doc.EnsureRecurringInstances(start, 400);
        Assert.Contains(doc.Items, i => Local(i.StartAtMillis).Date == new DateTime(2024, 3, 10));
    }
    [Fact] public void AllDayUsesExclusiveMidnightAndRepeatsAtLocalMidnight()
    {
        var doc = DesktopSnapshotDocument.CreateEmpty(); var start = new DateTime(2026, 10, 23, 9, 0, 0);
        doc.SaveItem(Event(start, 2) with { AllDay = true }); doc.EnsureRecurringInstances(start);
        Assert.Equal(3, doc.Items.Count);
        Assert.All(doc.Items, i => { Assert.Equal(TimeSpan.Zero, Local(i.StartAtMillis).TimeOfDay); Assert.Equal(Local(i.StartAtMillis).AddDays(1), Local(i.EndAtMillis)); });
        Assert.All(doc.RemindersBetween(long.MinValue, long.MaxValue), reminder => { Assert.Equal(8, Local(reminder.Trigger).Hour); Assert.Equal(45, Local(reminder.Trigger).Minute); });
        var roundtrip = DesktopSnapshotDocument.FromArchiveBytes(doc.ToArchiveBytes(false)); Assert.False(roundtrip.EnsureRecurringInstances(start));
    }
    [Fact] public void CalendarColorsFollowDefaultsAndDraggingSnapsWithoutLosingDuration()
    {
        var start = new DateTime(2026, 10, 23, 23, 0, 0); var doc = DesktopSnapshotDocument.CreateEmpty();
        var calendar = doc.SaveCalendar(null, "课程表", "#3370FF");
        doc.SaveItem(Event(start) with { CalendarId = calendar });
        var custom = doc.SaveItem(Event(start) with { CalendarId = calendar, AccentColor = "#D94646", RecurrenceType = "NONE" });
        doc.SaveCalendar(calendar, "课程", "#16A369"); doc.EnsureRecurringInstances(start);
        Assert.Equal("#D94646", doc.EditFor(custom)!.AccentColor);
        Assert.All(doc.Items.Where(i => i.Id != custom), i => Assert.Equal("#16A369", i.AccentColor));
        var moved = NativeWindow.CalendarDragEdit(Event(start), 1, 13, false);
        Assert.Equal(start.AddDays(1).AddMinutes(15), Local(moved.StartAtMillis));
        Assert.Equal(TimeSpan.FromHours(2), Local(moved.EndAtMillis) - Local(moved.StartAtMillis));
        var resized = NativeWindow.CalendarDragEdit(Event(start), 4, -999, true);
        Assert.Equal(start, Local(resized.StartAtMillis)); Assert.Equal(start.AddMinutes(15), Local(resized.EndAtMillis));
    }
    [Fact] public void MultiSlotCreationIsAtomicAndRecurringBundleSurvivesArchive()
    {
        var start = new DateTime(2026, 10, 23, 9, 0, 0); var doc = DesktopSnapshotDocument.CreateEmpty();
        var draft = Event(start, 2) with { AdditionalTimeSlots = new[] { new DesktopTimeSlot(Ms(start.AddHours(5)), Ms(start.AddHours(6))) } };
        doc.SaveItem(draft); doc.EnsureRecurringInstances(start);
        Assert.Equal(6, doc.Items.Count);
        var json = Json(doc);
        Assert.Single(json["tasks"]!.AsArray().Select(t => t!["multiSlotBundleId"]!.GetValue<string>()).Distinct());
        Assert.Equal(2, json["templates"]!.AsArray().Count);
        var restored = DesktopSnapshotDocument.FromArchiveBytes(doc.ToArchiveBytes(false)); Assert.False(restored.EnsureRecurringInstances(start));
        var before = doc.DataFingerprint();
        Assert.Throws<InvalidOperationException>(() => doc.SaveItem(draft with { AdditionalTimeSlots = new[] { new DesktopTimeSlot(Ms(start), Ms(start.AddMinutes(-1))) } }));
        Assert.Equal(before, doc.DataFingerprint());
    }
    [Fact] public void RemovingRecurrenceCanAlsoRemoveTodoDeadline()
    {
        var start = new DateTime(2026, 10, 23, 9, 0, 0); var doc = DesktopSnapshotDocument.CreateEmpty();
        var id = doc.SaveItem(Event(start) with { IsEvent = false, StartAtMillis = null, EndAtMillis = null, DueAtMillis = Ms(start) });
        doc.EnsureRecurringInstances(start);
        doc.SaveItem(doc.EditFor(id)! with { RecurrenceType = "NONE", DueAtMillis = null }, id, RecurrenceScope.All);
        Assert.Equal(long.MaxValue, Assert.Single(doc.Items).DueAtMillis); Assert.False(doc.EnsureRecurringInstances(start));
    }
    [Fact] public void LegacyWindowsEndAnchorDoesNotDuplicateOvernightOccurrences()
    {
        var start = new DateTime(2026, 10, 23, 23, 0, 0); var doc = DesktopSnapshotDocument.CreateEmpty();
        doc.SaveItem(Event(start, 3)); doc.EnsureRecurringInstances(start);
        var json = Json(doc); foreach (var item in json["tasks"]!.AsArray()) item!["recurrenceAnchorDueAtMillis"] = item["endAtMillis"]!.DeepClone();
        using var bytes = new MemoryStream();
        using (var zip = new ZipArchive(bytes, ZipArchiveMode.Create, true)) { using var writer = new StreamWriter(zip.CreateEntry("backup.json").Open()); writer.Write(json.ToJsonString()); }
        var legacy = DesktopSnapshotDocument.FromArchiveBytes(bytes.ToArray());
        Assert.False(legacy.EnsureRecurringInstances(start));
        legacy.Delete(legacy.Items[1].Id); Assert.False(legacy.EnsureRecurringInstances(start));
    }
}
