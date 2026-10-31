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
}
