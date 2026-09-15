using PaykiTodo.Desktop;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class QuickChecklistTests
{
    [Fact]
    public void ParserKeepsDeepHierarchyNotesAndSourceLineSelection()
    {
        var draft = QuickChecklistParser.Parse(
            "清单\r\n- 一级\r\n-- 二级\r\n--- 三级\r\n---- 四级\r\n----- 五级\r\n-----# 五级说明");

        Assert.False(draft.HasErrors);
        Assert.Equal(5, draft.MaxDepth);
        Assert.Equal("五级说明", draft.Nodes[^1].Notes);
        Assert.Equal(new QuickChecklistSourceSelection(4, 4), QuickChecklistParser.SourceLineSelection("清单\r\n- 一级", 2));
    }

    [Fact]
    public void CompletedAndCanceledChecklistStatesDoNotOverwriteEachOther()
    {
        var document = DesktopSnapshotDocument.CreateEmpty();
        var draft = QuickChecklistParser.Parse("清单\n- 父级\n-- 子级\n-- 另一个子级");
        var groupId = document.CreateQuickChecklist(draft);
        var items = document.Items.Where(item => item.QuickChecklistGroupId == groupId).ToList();
        var parent = items.Single(item => item.Title == "父级");
        var child = items.Single(item => item.Title == "子级");

        document.Cancel(child.Id);
        document.Complete(parent.Id);

        var canceledChild = document.Items.Single(item => item.Id == child.Id);
        Assert.True(canceledChild.Canceled);
        Assert.False(canceledChild.Completed);

        var otherDocument = DesktopSnapshotDocument.CreateEmpty();
        var otherGroupId = otherDocument.CreateQuickChecklist(draft);
        var otherItems = otherDocument.Items.Where(item => item.QuickChecklistGroupId == otherGroupId).ToList();
        var otherParent = otherItems.Single(item => item.Title == "父级");
        var completedChild = otherItems.Single(item => item.Title == "子级");

        otherDocument.Complete(completedChild.Id);
        otherDocument.Cancel(otherParent.Id);

        var preservedCompletedChild = otherDocument.Items.Single(item => item.Id == completedChild.Id);
        Assert.True(preservedCompletedChild.Completed);
        Assert.False(preservedCompletedChild.Canceled);
    }

    [Fact]
    public void ChecklistArchiveRoundTripPreservesHierarchyAndNotes()
    {
        var document = DesktopSnapshotDocument.CreateEmpty();
        document.CreateQuickChecklist(QuickChecklistParser.Parse("清单\n- 父级\n-- 子级\n--# 说明"));

        var restored = DesktopSnapshotDocument.FromArchiveBytes(document.ToArchiveBytes());

        Assert.Single(restored.QuickChecklistGroups);
        Assert.Equal(2, restored.Items.Count(item => item.QuickChecklistGroupId is not null));
        Assert.Equal("说明", restored.Items.Single(item => item.Title == "子级").Notes);
        Assert.NotNull(restored.Items.Single(item => item.Title == "子级").ParentTodoId);
    }

    [Fact]
    public void RestoringNormalTodoClearsOnlyItsTerminalState()
    {
        var document = DesktopSnapshotDocument.CreateEmpty();
        document.CreateQuickChecklist(QuickChecklistParser.Parse("清单\n- 子任务"));
        var normalId = document.SaveItem(new DesktopItemEdit(
            IsEvent: false,
            Title: "普通待办",
            Notes: "",
            Location: "",
            DueAtMillis: null,
            StartAtMillis: null,
            EndAtMillis: null,
            AllDay: false,
            ReminderEnabled: false,
            ReminderOffsets: Array.Empty<int>(),
            GroupId: 1,
            AccentColor: "#4CB782",
            CountdownEnabled: false,
            RecurrenceType: "NONE",
            RecurrenceWeekdays: "",
            RecurrenceEndEpochDay: null));
        document.Cancel(normalId);

        var otherId = document.SaveItem(new DesktopItemEdit(
            IsEvent: false,
            Title: "保持完成",
            Notes: "",
            Location: "",
            DueAtMillis: null,
            StartAtMillis: null,
            EndAtMillis: null,
            AllDay: false,
            ReminderEnabled: false,
            ReminderOffsets: Array.Empty<int>(),
            GroupId: 1,
            AccentColor: "#4CB782",
            CountdownEnabled: false,
            RecurrenceType: "NONE",
            RecurrenceWeekdays: "",
            RecurrenceEndEpochDay: null));
        document.Complete(otherId);

        document.Restore(normalId);

        Assert.False(document.Items.Single(item => item.Id == normalId).Canceled);
        Assert.False(document.Items.Single(item => item.Id == normalId).Completed);
        Assert.True(document.Items.Single(item => item.Id == otherId).Completed);
    }

    [Fact]
    public void RestoringChecklistChildRestoresItsSubtreeAndRecalculatesAncestors()
    {
        var document = DesktopSnapshotDocument.CreateEmpty();
        var groupId = document.CreateQuickChecklist(QuickChecklistParser.Parse("清单\n- 父级\n-- 子级\n--- 孙级\n-- 兄弟"));
        var items = document.Items.Where(item => item.QuickChecklistGroupId == groupId).ToList();
        var parent = items.Single(item => item.Title == "父级");
        var child = items.Single(item => item.Title == "子级");
        var grandchild = items.Single(item => item.Title == "孙级");
        var sibling = items.Single(item => item.Title == "兄弟");

        document.Cancel(sibling.Id);
        document.Complete(parent.Id);
        document.Restore(child.Id);

        var restoredItems = document.Items.Where(item => item.QuickChecklistGroupId == groupId).ToList();
        Assert.False(restoredItems.Single(item => item.Id == parent.Id).Completed);
        Assert.False(restoredItems.Single(item => item.Id == child.Id).Completed);
        Assert.False(restoredItems.Single(item => item.Id == grandchild.Id).Completed);
        Assert.True(restoredItems.Single(item => item.Id == sibling.Id).Canceled);
    }
}
