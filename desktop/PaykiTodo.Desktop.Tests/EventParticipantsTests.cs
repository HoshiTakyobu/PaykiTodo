using System.IO;
using System.IO.Compression;
using System.Text.Json.Nodes;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class EventParticipantsTests
{
    internal static readonly DesktopCalendar Calendar = new("shared:fixture", "协作日历", "#3370FF") {
        ShareMode = "SHARED", SharedRole = "owner", SharedServerUrl = "https://fixture.invalid", OwnerAccount = "owner",
        MembersJson = "[{\"username\":\"owner\",\"displayName\":\"组织者\",\"role\":\"owner\"},{\"username\":\"guest\",\"displayName\":\"参与成员\",\"role\":\"viewer\"},{\"username\":\"extra\",\"displayName\":\"新增成员\",\"role\":\"editor\"}]"
    };
    internal static DesktopSnapshotDocument Fixture()
    {
        var document = DesktopSnapshotDocument.CreateEmpty();
        var source = DesktopSnapshotDocument.CreateEmpty(); source.SaveItem(NativeWorkspaceTests.NewItem(true, "协作会议") with { StartAtMillis = NativeWorkspaceTests.Millis(DateTime.Today.AddDays(2).AddHours(10)), EndAtMillis = NativeWorkspaceTests.Millis(DateTime.Today.AddDays(2).AddHours(11)) });
        var wire = (JsonObject)Json(source)["tasks"]!.AsArray()[0]!.DeepClone(); wire["id"] = "wire-fixture";
        wire["participants"] = JsonNode.Parse("[{\"username\":\"guest\",\"response\":\"TENTATIVE\"}]");
        var metadata = new JsonObject { ["id"] = "fixture", ["name"] = Calendar.Name, ["colorHex"] = Calendar.ColorHex,
            ["role"] = "owner", ["ownerUsername"] = "owner", ["members"] = JsonNode.Parse(Calendar.MembersJson) };
        document.ImportSharedCalendar(metadata, new JsonObject { ["revision"] = 1L, ["events"] = new JsonArray(wire) }, Calendar.SharedServerUrl);
        return document;
    }
    internal static JsonObject Json(DesktopSnapshotDocument document)
    {
        using var memory = new MemoryStream(document.ToArchiveBytes()); using var zip = new ZipArchive(memory);
        using var reader = new StreamReader(zip.GetEntry("backup.json")!.Open()); return JsonNode.Parse(reader.ReadToEnd())!.AsObject();
    }
    [Fact] public void EditKeepsExistingReplyResetsNewParticipantsAndRejectsUnknownMembersAtomically()
    {
        var document = Fixture(); var item = document.Items.Single();
        document.SaveItem(document.EditFor(item.Id)! with { EventParticipantsJson = "[{\"username\":\"guest\",\"response\":\"DECLINED\"},{\"username\":\"extra\",\"response\":\"ACCEPTED\"}]" }, item.Id);
        var participants = DesktopEventParticipants.Read(document.Items.Single().EventParticipantsJson);
        Assert.Equal("TENTATIVE", participants.Single(value => value.Username == "guest").Response);
        Assert.Equal("NEEDS_ACTION", participants.Single(value => value.Username == "extra").Response);
        var before = Json(document); before.Remove("exportedAtMillis");
        Assert.Throws<InvalidOperationException>(() => document.SaveItem(document.EditFor(item.Id)! with { EventParticipantsJson = "[\"outsider\"]" }, item.Id));
        var after = Json(document); after.Remove("exportedAtMillis"); Assert.Equal(before.ToJsonString(), after.ToJsonString());
        Assert.Equal(participants, DesktopEventParticipants.Read(DesktopSnapshotDocument.FromArchiveBytes(document.ToArchiveBytes()).Items.Single().EventParticipantsJson));
    }
    [Fact] public void MovingCalendarUsesTargetMembersAndMarksBothSharedSourcesDirty()
    {
        var document = Fixture(); var id = document.Items.Single().Id;
        document.SaveCalendar("shared:second", "另一日历", "#7955CC", ownerAccount: "other", shareMode: "SHARED", membersJson: "[{\"username\":\"other\",\"role\":\"owner\"}]");
        document.SharedCalendar("shared:second")!["sharedRole"] = "owner";
        document.SharedCalendar(Calendar.Id)!["sharedDirty"] = false;
        Assert.Throws<InvalidOperationException>(() => document.SaveItem(document.EditFor(id)! with { CalendarId = "shared:second" }, id));
        Assert.Equal(Calendar.Id, document.Items.Single().CalendarId);
        document.SaveItem(document.EditFor(id)! with { CalendarId = "shared:second", EventParticipantsJson = "[\"other\"]" }, id);
        Assert.Equal("other", DesktopEventParticipants.Read(document.Items.Single().EventParticipantsJson).Single().Username);
        Assert.True(document.SharedCalendar(Calendar.Id)!["sharedDirty"]!.GetValue<bool>());
        Assert.True(document.SharedCalendar("shared:second")!["sharedDirty"]!.GetValue<bool>());
        document.SaveItem(document.EditFor(id)! with { CalendarId = "default" }, id);
        Assert.Empty(DesktopEventParticipants.Read(document.Items.Single().EventParticipantsJson));
        Assert.Empty(DesktopEventParticipants.Read(document.CopyDraft(id).EventParticipantsJson));
    }
    [Fact] public void RecurringTemplateAddsParticipantsWithoutCopyingAnInstancesReply()
    {
        var document = Fixture(); var first = document.Items.Single(); var edit = document.EditFor(first.Id)!;
        document.SaveItem(edit with { RecurrenceType = "DAILY", RecurrenceEndEpochDay = (long)(DateTime.Today.AddDays(4) - DateTime.UnixEpoch).TotalDays }, first.Id);
        document.EnsureRecurringInstances(DateTime.Today, 8);
        Assert.Equal(3, document.Items.Count);
        Assert.Equal("TENTATIVE", DesktopEventParticipants.Read(document.Items.First(value => value.Id == first.Id).EventParticipantsJson).Single().Response);
        Assert.All(document.Items.Where(value => value.Id != first.Id), item => Assert.Equal("NEEDS_ACTION", DesktopEventParticipants.Read(item.EventParticipantsJson).Single().Response));
        var wire = document.ExportSharedEvents(Calendar.Id);
        Assert.Equal(3, wire.OfType<JsonObject>().Select(value => value["id"]!.GetValue<string>()).Distinct().Count());
    }
}
