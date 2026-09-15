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
                ["id"] = 1L,
                ["name"] = "默认",
                ["colorHex"] = "#4CB782",
                ["sortOrder"] = 0,
                ["isDefault"] = true,
                ["createdAtMillis"] = now
            }),
            ["quickChecklistGroups"] = new JsonArray(),
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

    public IReadOnlyList<DesktopQuickChecklistGroup> QuickChecklistGroups => QuickChecklistGroupsArray
        .OfType<JsonObject>()
        .Select(group => new DesktopQuickChecklistGroup(
            group["id"]?.GetValue<long>() ?? 0L,
            group["title"]?.GetValue<string>() ?? "快速清单",
            group["collapsed"]?.GetValue<bool>() ?? false,
            group["sourceFingerprint"]?.GetValue<string>() ?? "",
            group["createdAtMillis"]?.GetValue<long>() ?? 0L,
            group["updatedAtMillis"]?.GetValue<long>() ?? 0L
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
        if (item["canceled"]?.GetValue<bool>() == true) return;
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        if (item["quickChecklistGroupId"]?.GetValue<long?>() is { } groupId)
        {
            foreach (var target in QuickChecklistSubtree(item, groupId).Where(target => target["canceled"]?.GetValue<bool>() != true))
            {
                target["completed"] = true;
                target["completedAtMillis"] ??= now;
            }
            UpdateQuickChecklistAncestors(groupId, item["parentTodoId"]?.GetValue<long?>(), now);
            return;
        }
        item["completed"] = true;
        item["completedAtMillis"] = now;
        item["canceled"] = false;
        item["canceledAtMillis"] = null;
    }

    public void Cancel(long id)
    {
        var item = FindTask(id);
        if (item is null) return;
        if (item["completed"]?.GetValue<bool>() == true) return;
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        if (item["quickChecklistGroupId"]?.GetValue<long?>() is { } groupId)
        {
            foreach (var target in QuickChecklistSubtree(item, groupId).Where(target =>
                         target["canceled"]?.GetValue<bool>() != true &&
                         target["completed"]?.GetValue<bool>() != true))
            {
                target["canceled"] = true;
                target["canceledAtMillis"] = now;
            }
            UpdateQuickChecklistAncestors(groupId, item["parentTodoId"]?.GetValue<long?>(), now);
            return;
        }
        item["canceled"] = true;
        item["canceledAtMillis"] = now;
        item["completed"] = false;
        item["completedAtMillis"] = null;
    }

    public void Restore(long id)
    {
        var item = FindTask(id);
        if (item is null || (item["completed"]?.GetValue<bool>() != true && item["canceled"]?.GetValue<bool>() != true)) return;
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        if (item["quickChecklistGroupId"]?.GetValue<long?>() is { } groupId)
        {
            foreach (var target in QuickChecklistSubtree(item, groupId))
            {
                target["completed"] = false;
                target["completedAtMillis"] = null;
                target["canceled"] = false;
                target["canceledAtMillis"] = null;
            }
            UpdateQuickChecklistGroupTimestamp(groupId);
            UpdateQuickChecklistAncestors(groupId, item["parentTodoId"]?.GetValue<long?>(), now);
            return;
        }
        item["completed"] = false;
        item["completedAtMillis"] = null;
        item["canceled"] = false;
        item["canceledAtMillis"] = null;
    }

    public void Delete(long id)
    {
        var target = FindTask(id);
        if (target is null) return;
        if (target["quickChecklistGroupId"]?.GetValue<long?>() is { } groupId)
        {
            var deleted = QuickChecklistSubtree(target, groupId).ToHashSet();
            foreach (var item in deleted) Tasks.Remove(item);
            if (!Tasks.OfType<JsonObject>().Any(item => item["quickChecklistGroupId"]?.GetValue<long?>() == groupId))
            {
                foreach (var node in QuickChecklistGroupsArray.OfType<JsonObject>().Where(group => group["id"]?.GetValue<long>() == groupId).ToList())
                    QuickChecklistGroupsArray.Remove(node);
            }
            UpdateQuickChecklistAncestors(groupId, target["parentTodoId"]?.GetValue<long?>(), DateTimeOffset.UtcNow.ToUnixTimeMilliseconds());
            return;
        }
        Tasks.Remove(target);
    }

    public long CreateQuickChecklist(QuickChecklistDraft draft)
    {
        if (draft.HasErrors) throw new InvalidOperationException(draft.Diagnostics.First(d => d.Severity == QuickChecklistDiagnosticSeverity.Error).Message);
        if (QuickChecklistGroupsArray.OfType<JsonObject>().Any(group => group["sourceFingerprint"]?.GetValue<string>() == draft.SourceFingerprint))
            throw new InvalidOperationException("相同内容的快速清单已经存在");
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var groupId = NextId(QuickChecklistGroupsArray);
        QuickChecklistGroupsArray.Add(new JsonObject
        {
            ["id"] = groupId,
            ["title"] = draft.Title,
            ["collapsed"] = false,
            ["sourceFingerprint"] = draft.SourceFingerprint,
            ["createdAtMillis"] = now,
            ["updatedAtMillis"] = now
        });
        var persistedIds = new Dictionary<int, long>();
        foreach (var node in draft.Nodes)
        {
            var id = NextId(Tasks);
            persistedIds[node.TemporaryId] = id;
            Tasks.Add(new JsonObject
            {
                ["id"] = id,
                ["createdAtMillis"] = now + node.SourceLine,
                ["itemType"] = "TODO",
                ["title"] = node.Title,
                ["notes"] = node.Notes,
                ["dueAtMillis"] = NoDueDateMillis,
                ["completed"] = false,
                ["canceled"] = false,
                ["missed"] = false,
                ["reminderEnabled"] = false,
                ["ringEnabled"] = false,
                ["vibrateEnabled"] = false,
                ["reminderDeliveryMode"] = "FULLSCREEN_AND_NOTIFICATION",
                ["groupId"] = DefaultGroupId(),
                ["accentColorHex"] = DefaultGroupColor(),
                ["quickChecklistGroupId"] = groupId,
                ["parentTodoId"] = node.ParentTemporaryId is { } parent ? persistedIds[parent] : null,
                ["quickChecklistDepth"] = node.Depth,
                ["quickChecklistSortOrder"] = node.SortOrder,
                ["quickChecklistCollapsed"] = false
            });
        }
        return groupId;
    }

    public void ToggleQuickChecklistGroup(long id)
    {
        var group = QuickChecklistGroupsArray.OfType<JsonObject>().FirstOrDefault(value => value["id"]?.GetValue<long>() == id);
        if (group is null) return;
        group["collapsed"] = !(group["collapsed"]?.GetValue<bool>() ?? false);
        group["updatedAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
    }

    public void ToggleQuickChecklistItem(long id)
    {
        var item = FindTask(id);
        if (item is null || item["quickChecklistGroupId"]?.GetValue<long?>() is not { } groupId) return;
        item["quickChecklistCollapsed"] = !(item["quickChecklistCollapsed"]?.GetValue<bool>() ?? false);
        UpdateQuickChecklistGroupTimestamp(groupId);
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
    private JsonArray QuickChecklistGroupsArray => root["quickChecklistGroups"]!.AsArray();
    private JsonArray Templates => root["templates"]!.AsArray();
    private JsonArray GroupsArray => root["groups"]!.AsArray();
    private JsonArray PlanningNotesArray => root["planningNotes"]!.AsArray();

    private long DefaultGroupId() => GroupsArray.OfType<JsonObject>().Select(group => group["id"]?.GetValue<long>() ?? 0L).DefaultIfEmpty(1L).Min();

    private string DefaultGroupColor() => GroupsArray.OfType<JsonObject>().FirstOrDefault(group => group["id"]?.GetValue<long>() == DefaultGroupId())?["colorHex"]?.GetValue<string>() ?? "#4CB782";

    private IEnumerable<JsonObject> QuickChecklistSubtree(JsonObject rootItem, long groupId)
    {
        var all = Tasks.OfType<JsonObject>().Where(item => item["quickChecklistGroupId"]?.GetValue<long?>() == groupId).ToList();
        var byParent = all.GroupBy(item => item["parentTodoId"]?.GetValue<long?>() ?? 0L).ToDictionary(group => group.Key, group => group.ToList());
        var result = new List<JsonObject>();
        void Visit(JsonObject item)
        {
            result.Add(item);
            if (item["id"]?.GetValue<long>() is { } childId && byParent.TryGetValue(childId, out var children))
                foreach (var child in children.OrderBy(value => value["quickChecklistSortOrder"]?.GetValue<int>() ?? 0)) Visit(child);
        }
        Visit(rootItem);
        return result;
    }

    private void UpdateQuickChecklistAncestors(long groupId, long? parentId, long now)
    {
        var all = Tasks.OfType<JsonObject>().Where(item => item["quickChecklistGroupId"]?.GetValue<long?>() == groupId).ToList();
        var byId = all.Where(item => item["id"] is not null).ToDictionary(item => item["id"]!.GetValue<long>(), item => item);
        var queue = new Queue<long>(parentId is { } parentSeed ? new[] { parentSeed } : Array.Empty<long>());
        var visited = new HashSet<long>();
        while (queue.Count > 0)
        {
            var id = queue.Dequeue();
            if (!visited.Add(id) || !byId.TryGetValue(id, out var parent)) continue;
            var children = all.Where(item => item["parentTodoId"]?.GetValue<long?>() == id).ToList();
            var effectiveChildren = children
                .Where(child => child["canceled"]?.GetValue<bool>() != true)
                .ToList();
            if (effectiveChildren.Count == 0)
            {
                if (parent["parentTodoId"]?.GetValue<long?>() is { } emptyNext) queue.Enqueue(emptyNext);
                continue;
            }
            var completed = parent["canceled"]?.GetValue<bool>() != true
                && effectiveChildren.All(child => child["completed"]?.GetValue<bool>() == true);
            parent["completed"] = completed;
            parent["completedAtMillis"] = completed ? parent["completedAtMillis"]?.GetValue<long>() ?? now : null;
            if (parent["parentTodoId"]?.GetValue<long?>() is { } next) queue.Enqueue(next);
        }
    }

    private void UpdateQuickChecklistGroupTimestamp(long groupId)
    {
        var group = QuickChecklistGroupsArray.OfType<JsonObject>().FirstOrDefault(value => value["id"]?.GetValue<long>() == groupId);
        if (group is not null) group["updatedAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
    }

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
        foreach (var name in new[] { "groups", "quickChecklistGroups", "templates", "tasks", "reminderChainLogs", "scheduleTemplates", "planningNotes", "planningLineMappings", "planningNodes", "aiReports", "todoGroupTags", "eventCheckIns", "recurringInstanceSkips", "activityRecords" })
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
internal sealed record DesktopQuickChecklistGroup(long Id, string Title, bool Collapsed, string SourceFingerprint, long CreatedAtMillis, long UpdatedAtMillis);
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
    bool CountdownEnabled,
    long? QuickChecklistGroupId,
    long? ParentTodoId,
    int? QuickChecklistDepth,
    int? QuickChecklistSortOrder,
    bool QuickChecklistCollapsed
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
        item["countdownEnabled"]?.GetValue<bool>() ?? false,
        item["quickChecklistGroupId"]?.GetValue<long?>(),
        item["parentTodoId"]?.GetValue<long?>(),
        item["quickChecklistDepth"]?.GetValue<int?>(),
        item["quickChecklistSortOrder"]?.GetValue<int?>(),
        item["quickChecklistCollapsed"]?.GetValue<bool>() ?? false
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
