using System.IO.Compression;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace PaykiTodo.Desktop;

internal sealed class DesktopSnapshotDocument
{
    private const long NoDueDateMillis = long.MaxValue;
    private JsonObject root;

    private DesktopSnapshotDocument(JsonObject root)
    {
        this.root = root;
        EnsureShape();
    }

    public static DesktopSnapshotDocument CreateEmpty()
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        return new DesktopSnapshotDocument(new JsonObject
        {
            ["exportedAtMillis"] = now,
            ["snapshotVersion"] = 1,
            ["settings"] = new JsonObject(),
            ["groups"] = new JsonArray(new JsonObject
            {
                ["id"] = 1,
                ["name"] = "默认",
                ["colorHex"] = "#4CB782",
                ["sortOrder"] = 0,
                ["isDefault"] = true,
                ["createdAtMillis"] = now
            }),
            ["templates"] = new JsonArray(),
            ["tasks"] = new JsonArray(),
            ["reminderChainLogs"] = new JsonArray(),
            ["scheduleTemplates"] = new JsonArray(),
            ["planningNotes"] = new JsonArray(),
            ["planningLineMappings"] = new JsonArray(),
            ["planningNodes"] = new JsonArray(),
            ["aiReports"] = new JsonArray(),
            ["todoGroupTags"] = new JsonArray(),
            ["eventCheckIns"] = new JsonArray(),
            ["recurringInstanceSkips"] = new JsonArray(),
            ["activityRecords"] = new JsonArray()
        });
    }

    public static DesktopSnapshotDocument FromArchiveBytes(byte[] archiveBytes)
    {
        using var input = new MemoryStream(archiveBytes, writable: false);
        using var archive = new ZipArchive(input, ZipArchiveMode.Read);
        var entry = archive.GetEntry("backup.json") ?? throw new InvalidOperationException("云端迁移包缺少 backup.json");
        using var reader = new StreamReader(entry.Open(), Encoding.UTF8);
        return new DesktopSnapshotDocument(JsonNode.Parse(reader.ReadToEnd())?.AsObject() ?? throw new InvalidOperationException("云端数据 JSON 无效"));
    }

    public byte[] ToArchiveBytes()
    {
        root["exportedAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var payload = Encoding.UTF8.GetBytes(root.ToJsonString(new JsonSerializerOptions { WriteIndented = true }));
        var manifest = new JsonObject
        {
            ["format"] = "PaykiTodoBackupArchive",
            ["archiveVersion"] = 1,
            ["snapshotVersion"] = root["snapshotVersion"]?.GetValue<int>() ?? 1,
            ["exportedAtMillis"] = root["exportedAtMillis"]?.GetValue<long>() ?? DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
            ["payload"] = "backup.json",
            ["payloadSha256"] = Convert.ToHexString(SHA256.HashData(payload)).ToLowerInvariant(),
            ["containsSecrets"] = false
        };
        using var output = new MemoryStream();
        using (var archive = new ZipArchive(output, ZipArchiveMode.Create, leaveOpen: true))
        {
            WriteEntry(archive, "manifest.json", Encoding.UTF8.GetBytes(manifest.ToJsonString(new JsonSerializerOptions { WriteIndented = true })));
            WriteEntry(archive, "backup.json", payload);
        }
        return output.ToArray();
    }

    public IReadOnlyList<DesktopItem> Items => Tasks
        .OfType<JsonObject>()
        .Select(DesktopItem.FromJson)
        .OrderBy(item => item.IsEvent ? item.StartAtMillis ?? long.MaxValue : item.DueAtMillis)
        .ToList();

    public IReadOnlyList<DesktopGroup> Groups => GroupsArray
        .OfType<JsonObject>()
        .Select(group => new DesktopGroup(
            group["id"]?.GetValue<long>() ?? 0L,
            group["name"]?.GetValue<string>() ?? "未分组",
            group["colorHex"]?.GetValue<string>() ?? "#4CB782"
        ))
        .ToList();

    public IReadOnlyList<DesktopPlanningNote> PlanningNotes => PlanningNotesArray
        .OfType<JsonObject>()
        .Select(note => new DesktopPlanningNote(
            note["id"]?.GetValue<long>() ?? 0L,
            note["title"]?.GetValue<string>() ?? "未命名规划",
            note["contentMarkdown"]?.GetValue<string>() ?? "",
            note["updatedAtMillis"]?.GetValue<long>() ?? 0L
        ))
        .OrderByDescending(note => note.UpdatedAtMillis)
        .ToList();

    public bool HasMeaningfulData => Items.Count > 0 || PlanningNotes.Count > 0;

    public long SaveItem(DesktopItemEdit edit, long? existingId = null)
    {
        JsonObject item;
        if (existingId.HasValue)
        {
            item = FindTask(existingId.Value) ?? throw new InvalidOperationException("事项不存在");
        }
        else
        {
            item = new JsonObject();
            Tasks.Add(item);
            item["id"] = NextId(Tasks);
            item["createdAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            item["completed"] = false;
            item["canceled"] = false;
            item["missed"] = false;
        }
        var previousSeriesId = item["recurringSeriesId"]?.GetValue<string>();
        var dueAt = edit.IsEvent ? edit.EndAtMillis ?? edit.StartAtMillis ?? NoDueDateMillis : edit.DueAtMillis ?? NoDueDateMillis;
        item["itemType"] = edit.IsEvent ? "EVENT" : "TODO";
        item["title"] = edit.Title.Trim();
        item["notes"] = edit.Notes.Trim();
        item["dueAtMillis"] = dueAt;
        item["startAtMillis"] = edit.IsEvent ? edit.StartAtMillis : null;
        item["endAtMillis"] = edit.IsEvent ? edit.EndAtMillis : null;
        item["allDay"] = edit.AllDay;
        item["location"] = edit.Location.Trim();
        item["accentColorHex"] = edit.AccentColor;
        item["countdownEnabled"] = edit.CountdownEnabled;
        item["hiddenFromBoard"] = false;
        item["checkInEnabled"] = false;
        item["checkInReminderOffsetsCsv"] = "";
        item["totalCheckInMinutes"] = item["totalCheckInMinutes"]?.GetValue<int>() ?? 0;
        item["reminderAtMillis"] = edit.ReminderEnabled && edit.ReminderOffsets.Count > 0 && dueAt != NoDueDateMillis
            ? dueAt - edit.ReminderOffsets.Max() * 60_000L
            : null;
        item["reminderOffsetsCsv"] = string.Join(',', edit.ReminderOffsets.OrderByDescending(value => value));
        item["reminderEnabled"] = edit.ReminderEnabled;
        item["ringEnabled"] = edit.ReminderEnabled;
        item["vibrateEnabled"] = edit.ReminderEnabled;
        item["alarmMode"] = false;
        item["voiceEnabled"] = false;
        item["reminderDeliveryMode"] = "FULLSCREEN_AND_NOTIFICATION";
        item["groupId"] = edit.GroupId;
        item["categoryKey"] = "ROUTINE";
        item["recurrenceType"] = edit.RecurrenceType;
        item["recurrenceWeekdays"] = edit.RecurrenceWeekdays;
        item["recurrenceEndEpochDay"] = edit.RecurrenceEndEpochDay;
        item["recurrenceAnchorDueAtMillis"] = edit.RecurrenceType == "NONE" ? null : dueAt;
        item["reminderOffsetMinutes"] = edit.ReminderOffsets.FirstOrDefault();
        var seriesId = edit.RecurrenceType == "NONE" ? null : previousSeriesId ?? Guid.NewGuid().ToString();
        item["recurringSeriesId"] = seriesId;
        SyncRecurringTemplate(item, edit, dueAt, previousSeriesId, seriesId);
        return item["id"]!.GetValue<long>();
    }

    public void Complete(long id)
    {
        var item = FindTask(id);
        if (item is null) return;
        if ((item["itemType"]?.GetValue<string>() ?? "TODO") == "EVENT") return;
        item["completed"] = true;
        item["completedAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
    }

    public void Cancel(long id)
    {
        var item = FindTask(id);
        if (item is null) return;
        item["canceled"] = true;
        item["canceledAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
    }

    public void Delete(long id)
    {
        var target = FindTask(id);
        if (target is not null) Tasks.Remove(target);
    }

    public DesktopItemEdit? EditFor(long id) => FindTask(id) is { } item ? DesktopItemEdit.FromJson(item) : null;

    public long SavePlanningNote(long? id, string title, string markdown)
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var note = id.HasValue
            ? PlanningNotesArray.OfType<JsonObject>().FirstOrDefault(value => value["id"]?.GetValue<long>() == id.Value)
            : null;
        if (note is null)
        {
            note = new JsonObject
            {
                ["id"] = NextId(PlanningNotesArray),
                ["createdAtMillis"] = now,
                ["archived"] = false,
                ["documentDateEpochDay"] = null
            };
            PlanningNotesArray.Add(note);
        }
        note["title"] = title.Trim().Length == 0 ? "未命名规划" : title.Trim();
        note["contentMarkdown"] = markdown;
        note["updatedAtMillis"] = now;
        return note["id"]!.GetValue<long>();
    }

    private JsonArray Tasks => root["tasks"]!.AsArray();
    private JsonArray Templates => root["templates"]!.AsArray();
    private JsonArray GroupsArray => root["groups"]!.AsArray();
    private JsonArray PlanningNotesArray => root["planningNotes"]!.AsArray();

    private JsonObject? FindTask(long id) => Tasks.OfType<JsonObject>().FirstOrDefault(item => item["id"]?.GetValue<long>() == id);

    private void SyncRecurringTemplate(JsonObject item, DesktopItemEdit edit, long dueAt, string? previousSeriesId, string? seriesId)
    {
        if (!string.IsNullOrWhiteSpace(previousSeriesId) && previousSeriesId != seriesId)
        {
            var stale = Templates.OfType<JsonObject>()
                .FirstOrDefault(template => template["seriesId"]?.GetValue<string>() == previousSeriesId);
            if (stale is not null) Templates.Remove(stale);
        }
        if (string.IsNullOrWhiteSpace(seriesId)) return;

        var template = Templates.OfType<JsonObject>()
            .FirstOrDefault(value => value["seriesId"]?.GetValue<string>() == seriesId);
        if (template is null)
        {
            template = new JsonObject
            {
                ["id"] = NextId(Templates),
                ["seriesId"] = seriesId,
                ["createdAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
            };
            Templates.Add(template);
        }
        var anchorMillis = edit.IsEvent ? edit.StartAtMillis ?? dueAt : dueAt;
        var anchor = DateTimeOffset.FromUnixTimeMilliseconds(anchorMillis).LocalDateTime;
        var startEpochDay = (long)(anchor.Date - DateTime.UnixEpoch.Date).TotalDays;
        var weekdays = edit.RecurrenceType == "WEEKLY" && string.IsNullOrWhiteSpace(edit.RecurrenceWeekdays)
            ? ((int)anchor.DayOfWeek == 0 ? 7 : (int)anchor.DayOfWeek).ToString()
            : edit.RecurrenceWeekdays;
        template["itemType"] = edit.IsEvent ? "EVENT" : "TODO";
        template["title"] = edit.Title.Trim();
        template["notes"] = edit.Notes.Trim();
        template["location"] = edit.Location.Trim();
        template["accentColorHex"] = edit.AccentColor;
        template["countdownEnabled"] = edit.CountdownEnabled;
        template["hiddenFromBoard"] = false;
        template["checkInEnabled"] = false;
        template["checkInReminderOffsetsCsv"] = "";
        template["multiSlotBundleId"] = null;
        template["allDay"] = edit.AllDay;
        template["groupId"] = edit.GroupId;
        template["dueHour"] = anchor.Hour;
        template["dueMinute"] = anchor.Minute;
        template["eventDurationMinutes"] = edit.IsEvent && edit.StartAtMillis is not null && edit.EndAtMillis is not null
            ? (int?)Math.Max(1, (edit.EndAtMillis.Value - edit.StartAtMillis.Value) / 60_000L)
            : null;
        template["reminderOffsetMinutes"] = edit.ReminderOffsets.FirstOrDefault();
        template["reminderOffsetsCsv"] = string.Join(',', edit.ReminderOffsets.OrderByDescending(value => value));
        template["ringEnabled"] = edit.ReminderEnabled;
        template["vibrateEnabled"] = edit.ReminderEnabled;
        template["alarmMode"] = false;
        template["reminderDeliveryMode"] = "FULLSCREEN_AND_NOTIFICATION";
        template["recurrenceType"] = edit.RecurrenceType;
        template["recurrenceWeekdays"] = weekdays;
        template["recurrenceMonthlyOrdinal"] = null;
        template["recurrenceMonthlyWeekday"] = null;
        template["recurrenceMonthlyDay"] = edit.RecurrenceType == "MONTHLY" ? anchor.Day : null;
        template["recurrenceYearlyMonth"] = edit.RecurrenceType == "YEARLY" ? anchor.Month : null;
        template["recurrenceYearlyDay"] = edit.RecurrenceType == "YEARLY" ? anchor.Day : null;
        template["startEpochDay"] = startEpochDay;
        template["endEpochDay"] = edit.RecurrenceEndEpochDay ?? startEpochDay + 3650L;
    }

    private void EnsureShape()
    {
        foreach (var name in new[] { "groups", "templates", "tasks", "reminderChainLogs", "scheduleTemplates", "planningNotes", "planningLineMappings", "planningNodes", "aiReports", "todoGroupTags", "eventCheckIns", "recurringInstanceSkips", "activityRecords" })
            root[name] ??= new JsonArray();
        root["settings"] ??= new JsonObject();
        root["snapshotVersion"] ??= 1;
    }

    private static long NextId(JsonArray values) => values.OfType<JsonObject>().Select(value => value["id"]?.GetValue<long>() ?? 0L).DefaultIfEmpty().Max() + 1;

    private static void WriteEntry(ZipArchive archive, string name, byte[] bytes)
    {
        var entry = archive.CreateEntry(name, CompressionLevel.Optimal);
        using var output = entry.Open();
        output.Write(bytes);
    }
}

internal sealed record DesktopGroup(long Id, string Name, string ColorHex);
internal sealed record DesktopPlanningNote(long Id, string Title, string Markdown, long UpdatedAtMillis);

internal sealed record DesktopItem(
    long Id,
    bool IsEvent,
    string Title,
    string Notes,
    string Location,
    long DueAtMillis,
    long? StartAtMillis,
    long? EndAtMillis,
    bool Completed,
    bool Canceled,
    long GroupId,
    string AccentColor,
    bool CountdownEnabled
)
{
    public static DesktopItem FromJson(JsonObject item) => new(
        item["id"]?.GetValue<long>() ?? 0L,
        string.Equals(item["itemType"]?.GetValue<string>(), "EVENT", StringComparison.OrdinalIgnoreCase),
        item["title"]?.GetValue<string>() ?? "未命名事项",
        item["notes"]?.GetValue<string>() ?? "",
        item["location"]?.GetValue<string>() ?? "",
        item["dueAtMillis"]?.GetValue<long>() ?? long.MaxValue,
        item["startAtMillis"]?.GetValue<long?>(),
        item["endAtMillis"]?.GetValue<long?>(),
        item["completed"]?.GetValue<bool>() ?? false,
        item["canceled"]?.GetValue<bool>() ?? false,
        item["groupId"]?.GetValue<long>() ?? 0L,
        item["accentColorHex"]?.GetValue<string>() ?? "#4CB782",
        item["countdownEnabled"]?.GetValue<bool>() ?? false
    );
}

internal sealed record DesktopItemEdit(
    bool IsEvent,
    string Title,
    string Notes,
    string Location,
    long? DueAtMillis,
    long? StartAtMillis,
    long? EndAtMillis,
    bool AllDay,
    bool ReminderEnabled,
    IReadOnlyList<int> ReminderOffsets,
    long GroupId,
    string AccentColor,
    bool CountdownEnabled,
    string RecurrenceType,
    string RecurrenceWeekdays,
    long? RecurrenceEndEpochDay
)
{
    public static DesktopItemEdit FromJson(JsonObject item)
    {
        var offsets = (item["reminderOffsetsCsv"]?.GetValue<string>() ?? "")
            .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(value => int.TryParse(value, out var parsed) ? parsed : -1)
            .Where(value => value >= 0)
            .ToList();
        var isEvent = string.Equals(item["itemType"]?.GetValue<string>(), "EVENT", StringComparison.OrdinalIgnoreCase);
        var due = item["dueAtMillis"]?.GetValue<long>() ?? long.MaxValue;
        return new DesktopItemEdit(
            isEvent,
            item["title"]?.GetValue<string>() ?? "",
            item["notes"]?.GetValue<string>() ?? "",
            item["location"]?.GetValue<string>() ?? "",
            !isEvent && due != long.MaxValue ? due : null,
            item["startAtMillis"]?.GetValue<long?>(),
            item["endAtMillis"]?.GetValue<long?>(),
            item["allDay"]?.GetValue<bool>() ?? false,
            item["reminderEnabled"]?.GetValue<bool>() ?? false,
            offsets,
            item["groupId"]?.GetValue<long>() ?? 0L,
            item["accentColorHex"]?.GetValue<string>() ?? "#4CB782",
            item["countdownEnabled"]?.GetValue<bool>() ?? false,
            item["recurrenceType"]?.GetValue<string>() ?? "NONE",
            item["recurrenceWeekdays"]?.GetValue<string>() ?? "",
            item["recurrenceEndEpochDay"]?.GetValue<long?>()
        );
    }
}
