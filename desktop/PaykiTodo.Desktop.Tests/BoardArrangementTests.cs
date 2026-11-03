using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class BoardArrangementTests
{
    [Fact] public void InterleavesByRealTimesWithoutChangingDeadlinesOrDroppingSameTitles() {
        var doc = DesktopSnapshotDocument.CreateEmpty(); var day = new DateTime(2026, 11, 3);
        long Add(bool eventItem, string title, int? hour, int minute = 0) => doc.SaveItem(NativeWorkspaceTests.NewItem(eventItem, title) with {
            DueAtMillis = eventItem || hour == null ? null : NativeWorkspaceTests.Millis(day.AddHours(hour.Value).AddMinutes(minute)),
            StartAtMillis = eventItem ? NativeWorkspaceTests.Millis(day.AddHours(hour!.Value)) : null,
            EndAtMillis = eventItem ? NativeWorkspaceTests.Millis(day.AddHours(hour!.Value + 3)) : null
        });
        var e1 = Add(true,"小组会",14); var t2 = Add(false,"提交记录",15); var e2 = Add(true,"同刻日程",15);
        var t1 = Add(false,"会前准备",13,30); var t3 = Add(false,"会后整理",17,30); var t4 = Add(false,"待确定",null);
        var before = doc.EditFor(t2)!.DueAtMillis;
        var ordered = BoardArrangement.Ordered(doc.Items.Where(x=>!x.IsEvent),doc.Items.Where(x=>x.IsEvent));
        Assert.Equal(new[]{t1,e1,e2,t2,t3,t4}, ordered.Select(x=>x.Id)); Assert.Equal(before,doc.EditFor(t2)!.DueAtMillis);
        var same = Add(false,"提交记录",15); Assert.Equal(7, BoardArrangement.Ordered(doc.Items.Where(x=>!x.IsEvent),doc.Items.Where(x=>x.IsEvent)).Count);
        Assert.NotEqual(t2,same);
    }
    [Theory]
    [InlineData(0, false)] [InlineData(1, true)]
    public void MidnightEndIsExclusive(int minutesAfterMidnight, bool expected) {
        var day = new DateTime(2026,11,2); var doc=DesktopSnapshotDocument.CreateEmpty();
        doc.SaveItem(NativeWorkspaceTests.NewItem(true,"跨周") with { StartAtMillis=NativeWorkspaceTests.Millis(day.AddHours(-1)),EndAtMillis=NativeWorkspaceTests.Millis(day.AddMinutes(minutesAfterMidnight)) });
        Assert.Equal(expected,BoardArrangement.OnDay(doc.Items.Single(),day));
    }
}
