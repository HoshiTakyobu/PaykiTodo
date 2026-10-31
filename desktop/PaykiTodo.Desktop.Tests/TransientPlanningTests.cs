using PaykiTodo.Desktop;
using Xunit;

namespace PaykiTodo.Desktop.Tests;
public sealed class TransientPlanningTests
{
    private static readonly DateTime Now = new(2026,11,1,12,0,0);
    private static TransientPlanningResult Parse(string text) => TransientPlanningParser.Parse(text, Now);
    [Fact] public void OptionalFieldsMixedRemindersAndPastTimesPersistAcrossArchive()
    {
        var parsed = Parse("- 仅标题;\n- 混合, @超市, 16:30, (5,0,09:00,2027-07-03+15:00); #说明;内文");
        Assert.False(parsed.HasErrors); Assert.Null(parsed.Candidates[0].DueAt);
        Assert.Equal("说明;内文",parsed.Candidates[1].Notes);
        var document=DesktopSnapshotDocument.CreateEmpty();document.ImportTransientPlanning(parsed.Candidates,"optional");
        var restored=DesktopSnapshotDocument.FromArchiveBytes(document.ToArchiveBytes());
        var plain=restored.Items.Single(i=>i.Title=="仅标题");Assert.False(restored.EditFor(plain.Id)!.ReminderEnabled);
        var mixed=restored.Items.Single(i=>i.Title=="混合");var edit=restored.EditFor(mixed.Id)!;
        Assert.Equal(2,edit.AbsoluteReminderTimes.Count);Assert.Equal(4,DesktopCalendarTime.Triggers(edit).Count);
        Assert.Equal("超市",mixed.Location);
        Assert.False(Parse("历史,2020-01-01 09:00,(5,0);").HasErrors);
        Assert.True(Parse("无DDL,(5);").HasErrors);
    }
    [Fact] public void ParentIdsAndBundlesAreRealAndImportRollsBackOnInvalidParent()
    {
        var parsed=Parse("- 出行\n  - 车票,18:00;\n  - 行李;\n- 数学,每2周[周一 08:00>09:40,周三 14:00>15:40];");
        Assert.False(parsed.HasErrors);
        var document=DesktopSnapshotDocument.CreateEmpty();document.ImportTransientPlanning(parsed.Candidates,"tree");
        document.EnsureRecurringInstances(Now.Date);
        var root=document.Items.Single(i=>i.Title=="出行");var ticket=document.Items.Single(i=>i.Title=="车票");
        Assert.Equal(root.Id,ticket.ParentTodoId);
        var course=document.Items.Where(i=>i.Title=="数学").Select(i=>document.EditFor(i.Id)!).ToList();
        Assert.True(course.Count>2);Assert.All(course,e=>Assert.Null(e.RecurrenceEndEpochDay));Assert.All(course,e=>Assert.Equal(2,e.RecurrenceInterval));
        var before=document.ToArchiveBytes();
        Assert.Throws<InvalidOperationException>(()=>document.ImportTransientPlanning(new[]{parsed.Candidates[1] with { ParentId="missing" }},"bad"));
        Assert.True(document.HasSamePersonalSyncContent(DesktopSnapshotDocument.FromArchiveBytes(before)));
    }
    [Fact] public void CyclicParentsRollBackEarlierWrites()
    {
        var parsed=Parse("先导入; A; B;");Assert.False(parsed.HasErrors);
        var a=parsed.Candidates[1] with { ParentId=parsed.Candidates[2].Id };var b=parsed.Candidates[2] with { ParentId=parsed.Candidates[1].Id };
        var document=DesktopSnapshotDocument.CreateEmpty();var before=document.ToArchiveBytes();
        Assert.Throws<InvalidOperationException>(()=>document.ImportTransientPlanning(new[]{parsed.Candidates[0],a,b},"cycle"));
        Assert.True(document.HasSamePersonalSyncContent(DesktopSnapshotDocument.FromArchiveBytes(before)));
    }
    [Theory]
    [InlineData("跨日,23:00>01:00;")]
    [InlineData("错误日期,2026-02-29 09:00;")]
    [InlineData("错误时间,24:00;")]
    [InlineData("空字段,,16:00;")]
    [InlineData("负提醒,16:00,(-1);")]
    [InlineData("重复,16:00,17:00;")]
    [InlineData("重复,@超市,@公园;")]
    [InlineData("循环,每0周[周一 09:00>10:00];")]
    [InlineData("\"A\";\"B\";")]
    public void MalformedInputProducesPositionalDiagnostics(string text) { var parsed=Parse(text);Assert.True(parsed.HasErrors);Assert.Equal(1,parsed.Diagnostics[0].LineNumber); }
    [Fact] public void QuotedInteriorPunctuationAndExplicitCrossDayAreKept()
    {
        var parsed=Parse("- \"阅读\"重点\",并整理\",@\"教学楼,201\",2026-11-01 23:00>2026-11-02 01:00,(); #原文\";");
        Assert.False(parsed.HasErrors);var item=Assert.Single(parsed.Candidates);
        Assert.Equal("阅读\"重点\",并整理",item.Title);Assert.Equal("教学楼,201",item.Location);
        Assert.Equal(PlanningReminderMode.Disabled,item.ReminderMode);Assert.Empty(item.ReminderOffsets);
        Assert.Equal("原文\";",item.Notes);
    }
    [Fact] public void MonthEndAndLeapDayStayConfiguredAfterPersistence()
    {
        var parsed=Parse("月末,每月[31日 09:00>10:00]; 闰日,每年[02-29 09:00>10:00];");Assert.False(parsed.HasErrors);
        var document=DesktopSnapshotDocument.CreateEmpty();document.ImportTransientPlanning(parsed.Candidates,"clamp");document.EnsureRecurringInstances(Now.Date,120);
        Assert.Equal(31,document.EditFor(document.Items.First(i=>i.Title=="月末").Id)!.RecurrenceMonthlyDay);
        Assert.Equal(29,document.EditFor(document.Items.First(i=>i.Title=="闰日").Id)!.RecurrenceMonthlyDay);
        Assert.Contains(document.Items,i=>i.Title=="月末" && DateTimeOffset.FromUnixTimeMilliseconds(i.StartAtMillis!.Value).LocalDateTime.Date==new DateTime(2026,12,31));
    }
    [Fact] public void EnterRespectsQuotedSemicolonsAndExistingIndent()
    {
        Assert.Equal("  - ",TransientPlanningParser.NextLinePrefix("- 父事项"));
        Assert.Equal("  - ",TransientPlanningParser.NextLinePrefix("  - 子事项; #备注;"));
        Assert.Equal("  - ",TransientPlanningParser.NextLinePrefix("- \"内部;标点\""));
    }
    [Fact] public void RecurringAbsoluteRemindersChooseNearestStartAcrossAllSlotsAndKeepRelativeRules()
    {
        var parsed = Parse("课程,每周[周一 08:00>09:00,周三 14:00>15:00],(10,0,2026-11-02 08:00,2026-11-02 09:00,2026-11-04 14:00,2026-11-04 15:00,2026-11-02 09:00);");
        Assert.False(parsed.HasErrors);
        var candidate = Assert.Single(parsed.Candidates);
        Assert.Equal(new[] { new DateTime(2026,11,2,8,0,0), new DateTime(2026,11,4,14,0,0), new DateTime(2026,11,4,14,0,0), new DateTime(2026,11,9,8,0,0) },
            candidate.AssignAbsoluteReminders().Select(a => a.StartAt));
        Assert.Equal(new[] { 0, 1, 1, 0 }, candidate.AssignAbsoluteReminders().Select(a => a.SlotIndex));
        var document = DesktopSnapshotDocument.CreateEmpty(); document.ImportTransientPlanning(parsed.Candidates, "nearest");
        document.EnsureRecurringInstances(Now.Date);
        foreach (var reminder in candidate.AbsoluteReminders) {
            var owner = Assert.Single(document.Items.Where(item => document.EditFor(item.Id)!.AbsoluteReminderTimes.Contains(Ms(reminder))));
            Assert.Equal(candidate.AssignAbsoluteReminders().Single(a => a.ReminderAt == reminder).StartAt, Local(owner.StartAtMillis));
        }
        Assert.All(document.Items, item => Assert.Equal(new[] { 10, 0 }, document.EditFor(item.Id)!.ReminderOffsets));
    }
    [Fact] public void EqualCourseStartsUseInputSlotOrderAndNeverDuplicateAnAbsoluteReminder()
    {
        var parsed = Parse("并列,每天[09:00>10:00,09:00>11:00],(08:00);"); Assert.False(parsed.HasErrors);
        var assignment = Assert.Single(parsed.Candidates.Single().AssignAbsoluteReminders()); Assert.Equal(0, assignment.SlotIndex);
        var document = DesktopSnapshotDocument.CreateEmpty(); document.ImportTransientPlanning(parsed.Candidates, "tie");
        document.EnsureRecurringInstances(Now.Date, 5);
        var owner = Assert.Single(document.Items.Where(item => document.EditFor(item.Id)!.AbsoluteReminderTimes.Count > 0));
        Assert.Equal(new DateTime(2026,11,1,10,0,0), Local(owner.EndAtMillis));
        Assert.Single(document.RemindersBetween(Ms(Now.Date.AddHours(8)) - 1, Ms(Now.Date.AddHours(8))));
    }
    [Fact] public void MixedRecurringAndSingleSlotsDoNotAttachRemindersToAnExistingUnrelatedEvent()
    {
        var old = Parse("已有日程,09:00>10:00,();").Candidates;
        var document = DesktopSnapshotDocument.CreateEmpty(); var oldId = Assert.Single(document.ImportTransientPlanning(old, "old"));
        var candidate = Parse("新课程,每天[15:00>16:00],(08:00);").Candidates.Single();
        candidate = candidate with { Slots = new[] { new TransientPlanningSlot(Now.Date.AddHours(9), Now.Date.AddHours(10)), candidate.Slots[0] } };
        Assert.Null(candidate.Validate());
        document.ImportTransientPlanning(new[] { candidate }, "mixed");
        Assert.Empty(document.EditFor(oldId)!.AbsoluteReminderTimes);
        var owner = Assert.Single(document.Items, item => document.EditFor(item.Id)!.AbsoluteReminderTimes.Count > 0);
        Assert.Equal("新课程", owner.Title); Assert.Equal(Now.Date.AddHours(9), Local(owner.StartAtMillis)); Assert.Null(owner.RecurringSeriesId);
    }
    [Theory]
    [InlineData("课程,每周[周一 08:00>09:00,周三 14:00>15:00],(2026-11-04 14:01),自2026-11-01至2026-11-04;")]
    [InlineData("课程,每天[09:00>10:00],(9999-12-31 09:01),自9999-12-30至9999-12-31;")]
    public void AbsoluteReminderWithoutAnEligibleOccurrenceHasAPositionalError(string source)
    {
        var parsed = Parse(source); Assert.True(parsed.HasErrors);
        var error = Assert.Single(parsed.Diagnostics); Assert.Equal(1, error.LineNumber); Assert.True(error.Column > 0);
        Assert.Contains("之后在循环范围内没有日程", error.Message); Assert.Empty(parsed.Candidates);
    }
    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public void FarFutureOwnersPersistWithoutSkippingIntermediateReplenishmentOrSpreadingOnCourseEdits(bool relative)
    {
        var parsed = Parse("远期课程,每天[09:00>10:00,15:00>16:00],(" + (relative ? "5,0," : "") + "2030-01-01 08:00,2030-01-01 10:00);");
        Assert.False(parsed.HasErrors);
        var document = DesktopSnapshotDocument.CreateEmpty(); var created = document.ImportTransientPlanning(parsed.Candidates, "far");
        Assert.Equal(4, created.Count);
        Assert.Equal(2, document.Items.Count(item => Local(item.StartAtMillis).Date == new DateTime(2030,1,1)));
        Assert.True(document.EnsureRecurringInstances(Now.Date, 5));
        Assert.Equal(12, document.Items.Count(item => Local(item.StartAtMillis).Date < Now.Date.AddDays(6)));
        var restored = DesktopSnapshotDocument.FromArchiveBytes(document.ToArchiveBytes());
        foreach (var series in restored.Items.Select(item => item.RecurringSeriesId).Distinct()) {
            var selected = restored.Items.First(item => item.RecurringSeriesId == series && Local(item.StartAtMillis).Date == Now.Date);
            restored.SaveItem(restored.EditFor(selected.Id)! with { Title = "共同改名", Location = "新教室" }, selected.Id, RecurrenceScope.All);
        }
        Assert.True(restored.EnsureRecurringInstances(new DateTime(2027,1,1), 5));
        Assert.Equal(12, restored.Items.Count(item => Local(item.StartAtMillis).Date >= new DateTime(2027,1,1) && Local(item.StartAtMillis).Date <= new DateTime(2027,1,6)));
        var owners = restored.Items.Where(item => restored.EditFor(item.Id)!.AbsoluteReminderTimes.Count > 0).ToList(); Assert.Equal(2, owners.Count);
        Assert.All(owners, item => { Assert.Equal(new DateTime(2030,1,1), Local(item.StartAtMillis).Date); Assert.True(restored.EditFor(item.Id)!.ReminderEnabled); });
        Assert.All(restored.Items.Where(item => !owners.Contains(item)), item => { Assert.Empty(restored.EditFor(item.Id)!.AbsoluteReminderTimes); Assert.Equal(relative, restored.EditFor(item.Id)!.ReminderEnabled); });
        Assert.All(restored.Items, item => { Assert.Equal("共同改名", item.Title); Assert.Equal("新教室", item.Location); });
        Assert.False(restored.EnsureRecurringInstances(new DateTime(2027,1,1), 5));
        Assert.Equal(relative ? 4 : 2, restored.RemindersBetween(Ms(new DateTime(2030,1,1,7,0,0)), Ms(new DateTime(2030,1,1,11,0,0))).Count);
    }
    [Theory]
    [InlineData("月末,每月[31日 09:00>10:00],(2026-11-30 10:00);", "2026-12-31 09:00")]
    [InlineData("闰日,每年[02-29 09:00>10:00],(2027-02-28 10:00);", "2028-02-29 09:00")]
    [InlineData("闰日,每年[02-29 09:00>10:00],(2027-02-28 09:00);", "2027-02-28 09:00")]
    public void AbsoluteAssignmentUsesTheSameMonthEndAndLeapDayDatesAsReplenishment(string source, string expected)
    {
        var parsed = Parse(source); Assert.False(parsed.HasErrors);
        var expectedStart = DateTime.ParseExact(expected, "yyyy-MM-dd HH:mm", System.Globalization.CultureInfo.InvariantCulture);
        Assert.Equal(expectedStart, Assert.Single(parsed.Candidates.Single().AssignAbsoluteReminders()).StartAt);
        var document = DesktopSnapshotDocument.CreateEmpty(); document.ImportTransientPlanning(parsed.Candidates, "calendar");
        document.EnsureRecurringInstances(expectedStart.Date.AddDays(-1), 2);
        Assert.Single(document.Items.Where(item => Local(item.StartAtMillis) == expectedStart));
    }
    [Fact] public void VeryLargeWeeklyIntervalHasAnOwnerBeyondOneHundredYearsAndKeepsItsOriginalAnchor()
    {
        var parsed = Parse("远期周课,每10000周[周一 09:00>10:00],(2026-11-02 10:00),自2026-11-02至2300-01-01;"); Assert.False(parsed.HasErrors);
        var expected = new DateTime(2026,11,2,9,0,0).AddDays(70_000);
        Assert.Equal(expected, Assert.Single(parsed.Candidates.Single().AssignAbsoluteReminders()).StartAt);
        var document = DesktopSnapshotDocument.CreateEmpty(); document.ImportTransientPlanning(parsed.Candidates, "large");
        Assert.Equal(2, document.Items.Count);
        Assert.False(document.EnsureRecurringInstances(expected.Date, 0));
        Assert.False(document.EnsureRecurringInstances(Now.Date, 62));
        Assert.Single(document.Items.Where(item => Local(item.StartAtMillis) == expected && document.EditFor(item.Id)!.AbsoluteReminderTimes.Count == 1));
    }
    [Fact] public void ImportedHistoricalAbsoluteReminderIsRetainedWithoutBackfillAndFutureReminderFiresOnce()
    {
        var parsed = Parse("课程,每天[09:00>10:00],(08:00,2026-11-02 08:00);"); Assert.False(parsed.HasErrors);
        var document = DesktopSnapshotDocument.CreateEmpty(); document.ImportTransientPlanning(parsed.Candidates, "past");
        document.EnsureRecurringInstances(Now.Date, 5);
        Assert.Equal(2, document.Items.Count(item => document.EditFor(item.Id)!.AbsoluteReminderTimes.Count > 0));
        var schedule = new DesktopReminderSchedule(); schedule.Update(document, Ms(Now));
        Assert.Empty(schedule.TakeDue(Ms(Now.Date), Ms(Now)));
        var next = Ms(new DateTime(2026,11,2,8,0,0));
        Assert.Single(schedule.TakeDue(Ms(Now), next)); schedule.Update(document, next + 1);
        Assert.Empty(schedule.TakeDue(Ms(Now), next + 2));
    }
    [Fact] public void AFailedConfirmationRollsBackAlreadyMaterializedAbsoluteOwners()
    {
        var parsed = Parse("先导入,每天[09:00>10:00],(2030-01-01 08:00); A; B;"); Assert.False(parsed.HasErrors);
        var candidates = new[] { parsed.Candidates[0], parsed.Candidates[1] with { ParentId = parsed.Candidates[2].Id }, parsed.Candidates[2] with { ParentId = parsed.Candidates[1].Id } };
        var document = DesktopSnapshotDocument.CreateEmpty(); var before = DesktopSnapshotDocument.FromArchiveBytes(document.ToArchiveBytes());
        Assert.Throws<InvalidOperationException>(() => document.ImportTransientPlanning(candidates, "rollback"));
        Assert.True(document.HasSamePersonalSyncContent(before)); Assert.Empty(document.Items);
        Assert.Equal(2, document.ImportTransientPlanning(new[] { parsed.Candidates[0] }, "rollback").Count);
    }
    private static long Ms(DateTime time) => DesktopCalendarTime.Millis(time, "");
    private static DateTime Local(long? time) => DesktopCalendarTime.Wall(time!.Value, "");
}
