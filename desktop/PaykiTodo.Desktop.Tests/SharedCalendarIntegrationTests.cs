using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Net;
using System.Net.Http;
using System.Net.Sockets;
using System.Text.Json.Nodes;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class SharedCalendarIntegrationTests
{
    private static JsonObject Json(DesktopSnapshotDocument document, bool shared = true)
    {
        using var bytes = new MemoryStream(document.ToArchiveBytes(false, shared)); using var zip = new ZipArchive(bytes);
        using var read = new StreamReader(zip.GetEntry("backup.json")!.Open()); return JsonNode.Parse(read.ReadToEnd())!.AsObject();
    }
    [Fact] public async Task ActualServerSharingProtectsViewerPermissionsConflictsAndPersonalSnapshots()
    {
        var folder = Path.Combine(Path.GetTempPath(), "PaykiTodo-tests", Guid.NewGuid().ToString("N")); Directory.CreateDirectory(folder);
        var socket = new TcpListener(IPAddress.Loopback, 0); socket.Start(); var port = ((IPEndPoint)socket.LocalEndpoint).Port; socket.Stop();
        var script = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "../../../../../server/paykitodo_cloud_server.py"));
        var start = new ProcessStartInfo("python") { UseShellExecute = false, CreateNoWindow = true, RedirectStandardError = true, RedirectStandardOutput = true };
        start.ArgumentList.Add(script); start.Environment["PAYKITODO_CLOUD_DB"] = Path.Combine(folder, "fixture.sqlite3");
        start.Environment["PAYKITODO_CLOUD_HOST"] = "127.0.0.1"; start.Environment["PAYKITODO_CLOUD_PORT"] = port.ToString();
        start.Environment["PAYKITODO_TURNSTILE_SITE_KEY"] = ""; start.Environment["PAYKITODO_TURNSTILE_SECRET_KEY"] = ""; start.Environment["PAYKITODO_PASSWORD_PEPPER"] = "isolated-test-only";
        using var server = Process.Start(start)!; server.BeginErrorReadLine(); server.BeginOutputReadLine();
        var url = "http://127.0.0.1:" + port;
        try {
            using var client = new HttpClient { Timeout = TimeSpan.FromSeconds(2) };
            for (var attempt = 0; ; attempt++) {
                try { if ((await client.GetAsync(url + "/healthz")).IsSuccessStatusCode) break; } catch { }
                if (attempt >= 100) throw new TimeoutException("Isolated collaboration server did not start"); await Task.Delay(100);
            }
            const string password = "test-password-1234";
            var first = new DesktopWorkspace(new DesktopSecureStore(Path.Combine(folder, "first")));
            var second = new DesktopWorkspace(new DesktopSecureStore(Path.Combine(folder, "second")));
            var viewer = new DesktopWorkspace(new DesktopSecureStore(Path.Combine(folder, "viewer")));
            await first.LoginAsync(url, "sharing_owner", password, true, null); await second.LoginAsync(url, "sharing_owner", password, false, null);
            await viewer.LoginAsync(url, "readonly_user", password, true, null);
            var created = await first.SharedRequestAsync(HttpMethod.Post, "/v3/calendars", new JsonObject { ["name"] = "共享课程", ["description"] = "合成测试资料", ["colorHex"] = "#3370FF" });
            var metadata = created["calendar"]!.AsObject(); var id = metadata["id"]!.GetValue<string>(); var local = "shared:" + id;
            var events = await first.SharedRequestAsync(HttpMethod.Get, "/v3/calendars/" + id + "/events");
            first.EditShared(data => data.ImportSharedCalendar(metadata, events, url));
            first.EditShared(data => data.SaveItem(NativeWorkspaceTests.NewItem(true, "原始共享日程") with { CalendarId = local, ReminderEnabled = true, ReminderOffsets = new[] { 0 } }));
            await first.SyncSharedCalendarsAsync();
            Assert.Single(first.Document!.Items); Assert.Empty(Json(first.Document, false)["tasks"]!.AsArray());
            var firstWire = (await first.SharedRequestAsync(HttpMethod.Get, "/v3/calendars/" + id + "/events"))["events"]!.AsArray();
            var wireId = firstWire[0]!["id"]!.GetValue<string>(); Assert.False(string.IsNullOrWhiteSpace(wireId));
            var refreshedMeta = (await second.SharedRequestAsync(HttpMethod.Get, "/v3/calendars/" + id))["calendar"]!.AsObject();
            var secondEvents = await second.SharedRequestAsync(HttpMethod.Get, "/v3/calendars/" + id + "/events");
            second.EditShared(data => data.ImportSharedCalendar(refreshedMeta, secondEvents, url));
            first.EditShared(data => data.SaveItem(data.EditFor(data.Items.Single().Id)! with { Title = "甲修改" }, data.Items.Single().Id));
            second.EditShared(data => data.SaveItem(data.EditFor(data.Items.Single().Id)! with { Title = "乙修改" }, data.Items.Single().Id));
            await first.SyncSharedCalendarsAsync(); await second.SyncSharedCalendarsAsync();
            Assert.Equal("乙修改", second.Document!.Items.Single().Title); Assert.NotNull(second.Document.SharedCalendar(local)!["sharedConflictRevision"]);
            await first.SharedRequestAsync(HttpMethod.Post, "/v3/calendars/" + id + "/invitations", new JsonObject { ["username"] = "readonly_user", ["role"] = "viewer" });
            var invitation = (await viewer.SharedRequestAsync(HttpMethod.Get, "/v3/calendar-invitations"))["invitations"]!.AsArray()[0]!["id"]!.GetValue<string>();
            await viewer.SharedRequestAsync(HttpMethod.Post, "/v3/calendar-invitations/" + invitation + "/respond", new JsonObject { ["accept"] = true });
            var viewerMeta = (await viewer.SharedRequestAsync(HttpMethod.Get, "/v3/calendars/" + id))["calendar"]!.AsObject();
            var viewerEvents = await viewer.SharedRequestAsync(HttpMethod.Get, "/v3/calendars/" + id + "/events");
            viewer.EditShared(data => data.ImportSharedCalendar(viewerMeta, viewerEvents, url));
            Assert.False(viewer.Document!.SharedWritable(local));
            Assert.Throws<InvalidOperationException>(() => viewer.EditShared(data => data.SaveItem(data.EditFor(data.Items.Single().Id)! with { Title = "不允许修改" }, data.Items.Single().Id)));
            await Assert.ThrowsAsync<InvalidOperationException>(() => viewer.SharedRequestAsync(HttpMethod.Put, "/v3/calendars/" + id + "/events", new JsonObject { ["expectedRevision"] = viewerEvents["revision"]!.DeepClone(), ["events"] = new JsonArray() }));
            await viewer.SharedRequestAsync(HttpMethod.Post, "/v3/calendars/" + id + "/events/" + Uri.EscapeDataString(wireId) + "/rsvp", new JsonObject { ["response"] = "ACCEPTED" });
            await first.SharedRequestAsync(HttpMethod.Delete, "/v3/calendars/" + id + "/members/readonly_user"); await viewer.SyncSharedCalendarsAsync();
            Assert.Equal("revoked", viewer.Document.SharedCalendar(local)!["sharedRole"]!.GetValue<string>());
            Assert.Empty(viewer.Document.RemindersBetween(long.MinValue, long.MaxValue));
            viewer.EditShared(data => data.SharedCalendar(local)!["sharedDirty"] = true);
            await first.SharedRequestAsync(HttpMethod.Post, "/v3/calendars/" + id + "/invitations", new JsonObject { ["username"] = "readonly_user", ["role"] = "editor" });
            invitation = (await viewer.SharedRequestAsync(HttpMethod.Get, "/v3/calendar-invitations"))["invitations"]!.AsArray().OfType<JsonObject>().Single(value => value["status"]!.GetValue<string>().Equals("pending", StringComparison.OrdinalIgnoreCase))["id"]!.GetValue<string>();
            await viewer.SharedRequestAsync(HttpMethod.Post, "/v3/calendar-invitations/" + invitation + "/respond", new JsonObject { ["accept"] = true });
            await viewer.SyncSharedCalendarsAsync();
            Assert.Equal("editor", viewer.Document.SharedCalendar(local)!["sharedRole"]!.GetValue<string>());
            Assert.False(viewer.Document.SharedCalendar(local)!["sharedDirty"]!.GetValue<bool>()); Assert.Equal("甲修改", viewer.Document.Items.Single().Title);
        } finally { if (!server.HasExited) { server.Kill(entireProcessTree: true); await server.WaitForExitAsync(); } }
    }
    [Fact] public void SharedCacheMergeAvoidsPersonalIdCollisionsAndRemovalNeverMigratesContent()
    {
        var cached = DesktopSnapshotDocument.CreateEmpty();
        var metadata = JsonNode.Parse("{\"id\":\"fixture\",\"name\":\"共享\",\"colorHex\":\"#3370FF\",\"role\":\"editor\",\"ownerUsername\":\"owner\"}")!.AsObject();
        var eventSource = DesktopSnapshotDocument.CreateEmpty(); eventSource.SaveItem(NativeWorkspaceTests.NewItem(true, "共享内容"));
        var wire = (JsonObject)Json(eventSource)["tasks"]!.AsArray()[0]!.DeepClone(); wire["id"] = "wire-id";
        cached.ImportSharedCalendar(metadata, new JsonObject { ["revision"] = 1L, ["events"] = new JsonArray(wire) }, "https://fixture.invalid");
        cached.ToggleCheckIn(cached.Items.Single().Id);
        var personal = DesktopSnapshotDocument.CreateEmpty(); personal.SaveItem(NativeWorkspaceTests.NewItem(false, "个人内容")); personal.PreserveSharedData(cached);
        Assert.Equal(2, personal.Items.Select(value => value.Id).Distinct().Count());
        Assert.True(personal.CheckedIn(personal.Items.Single(value => value.IsEvent).Id)); Assert.Empty(Json(personal, false)["eventCheckIns"]!.AsArray());
        Assert.Equal("个人内容", Json(personal, false)["tasks"]!.AsArray()[0]!["title"]!.GetValue<string>());
        personal.DeleteCalendar("shared:fixture"); Assert.Single(personal.Items); Assert.Equal("个人内容", personal.Items.Single().Title);
    }
    [Fact] public void SharedImportReservesExistingIdsKeepsMissingHistoryAndLocalCheckIns()
    {
        var document = DesktopSnapshotDocument.CreateEmpty(); document.SaveItem(NativeWorkspaceTests.NewItem(false, "个人事项"));
        var source = DesktopSnapshotDocument.CreateEmpty(); source.SaveItem(NativeWorkspaceTests.NewItem(true, "共享事项"));
        var metadata = JsonNode.Parse("{\"id\":\"fixture\",\"name\":\"共享\",\"role\":\"EDITOR\"}")!.AsObject();
        var first = (JsonObject)Json(source)["tasks"]!.AsArray()[0]!.DeepClone(); first["id"] = "existing";
        document.ImportSharedCalendar(metadata, new JsonObject { ["revision"] = 1, ["events"] = new JsonArray(first) }, "https://fixture.invalid");
        var existingId = document.Items.Single(value => value.IsEvent).Id; document.ToggleCheckIn(existingId);
        var second = (JsonObject)first.DeepClone(); second["id"] = "new-first";
        document.ImportSharedCalendar(metadata, new JsonObject { ["revision"] = 2, ["events"] = new JsonArray(second, first.DeepClone()) }, "https://fixture.invalid");
        Assert.True(document.SharedWritable("shared:fixture")); Assert.Equal(3, document.Items.Select(value => value.Id).Distinct().Count()); Assert.True(document.CheckedIn(existingId));
        Assert.Equal(existingId, document.Items.Single(value => value.SharedEventId == "existing").Id);
        document.ImportSharedCalendar(metadata, new JsonObject { ["revision"] = 3, ["events"] = new JsonArray(second.DeepClone()) }, "https://fixture.invalid");
        var missing = document.Items.Single(value => value.Id == existingId); Assert.True(missing.Canceled); Assert.False(document.EditFor(missing.Id)!.ReminderEnabled); Assert.True(document.CheckedIn(existingId));
        Assert.Single(document.ExportSharedEvents("shared:fixture"));
    }
    [Fact] public void PersonalRestoreRejectsSharedSeriesCollisionBeforeMutation()
    {
        var personal = DesktopSnapshotDocument.CreateEmpty(); personal.SaveItem(NativeWorkspaceTests.NewItem(true, "个人循环") with { RecurrenceType = "DAILY" });
        var wire = (JsonObject)Json(personal)["tasks"]!.AsArray()[0]!.DeepClone(); var series = wire["recurringSeriesId"]!.GetValue<string>(); wire["id"] = series + ":fixture";
        wire["template"] = Json(personal)["templates"]!.AsArray()[0]!.DeepClone();
        var metadata = JsonNode.Parse("{\"id\":\"fixture\",\"name\":\"共享\",\"role\":\"editor\"}")!.AsObject();
        var cached = DesktopSnapshotDocument.CreateEmpty(); cached.ImportSharedCalendar(metadata, new JsonObject { ["revision"] = 1, ["events"] = new JsonArray(wire) }, "https://fixture.invalid");
        var beforeTasks = Json(personal)["tasks"]!.ToJsonString(); var beforeTemplates = Json(personal)["templates"]!.ToJsonString();
        Assert.Throws<InvalidOperationException>(() => personal.PreserveSharedData(cached));
        Assert.Equal(beforeTasks, Json(personal)["tasks"]!.ToJsonString()); Assert.Equal(beforeTemplates, Json(personal)["templates"]!.ToJsonString()); Assert.Null(personal.SharedCalendar("shared:fixture"));
    }
}
