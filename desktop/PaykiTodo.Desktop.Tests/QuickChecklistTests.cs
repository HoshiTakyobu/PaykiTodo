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
}
