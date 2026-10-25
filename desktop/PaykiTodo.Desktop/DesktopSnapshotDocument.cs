using System.IO.Compression;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace PaykiTodo.Desktop;

internal sealed partial class DesktopSnapshotDocument
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
        if (entry.Length > 64 * 1024 * 1024) throw new InvalidOperationException("迁移包内容超过大小限制");
        using var stream = entry.Open(); using var payload = new MemoryStream(); stream.CopyTo(payload);
        var bytes = payload.ToArray();
        if (archive.GetEntry("manifest.json") is { } manifestEntry) {
            using var manifestReader = new StreamReader(manifestEntry.Open(), Encoding.UTF8);
            var manifest = JsonNode.Parse(manifestReader.ReadToEnd());
            var expected = manifest?["payloadSha256"]?.GetValue<string>();
            if (!string.IsNullOrEmpty(expected) && !expected.Equals(Convert.ToHexString(SHA256.HashData(bytes)), StringComparison.OrdinalIgnoreCase))
                throw new InvalidOperationException("迁移包校验失败，文件可能已损坏");
        }
        return new DesktopSnapshotDocument(JsonNode.Parse(bytes)?.AsObject() ?? throw new InvalidOperationException("云端数据 JSON 无效"));
    }

    public byte[] ToArchiveBytes(bool includeSecrets = true, bool includeShared = true)
    {
        root["exportedAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var snapshot = includeSecrets && includeShared ? root : (JsonObject)root.DeepClone();
        if (!includeShared) StripSharedData(snapshot);
        if (!includeSecrets && snapshot["settings"]?["planningAiProviders"] is JsonArray providers)
            foreach (var provider in providers.OfType<JsonObject>()) provider.Remove("apiKey");
        var payload = Encoding.UTF8.GetBytes(snapshot.ToJsonString(new JsonSerializerOptions { WriteIndented = true }));
        var manifest = new JsonObject
        {
            ["format"] = "PaykiTodoBackupArchive",
            ["archiveVersion"] = 1,
            ["snapshotVersion"] = root["snapshotVersion"]?.GetValue<int>() ?? 1,
            ["exportedAtMillis"] = root["exportedAtMillis"]?.GetValue<long>() ?? DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
            ["payload"] = "backup.json",
            ["payloadSha256"] = Convert.ToHexString(SHA256.HashData(payload)).ToLowerInvariant(),
            ["containsSecrets"] = includeSecrets && DesktopAi.Providers(this).Any(provider => !string.IsNullOrEmpty(provider["apiKey"]?.GetValue<string>()))
        };
        using var output = new MemoryStream();
        using (var archive = new ZipArchive(output, ZipArchiveMode.Create, leaveOpen: true))
        {
            WriteEntry(archive, "manifest.json", Encoding.UTF8.GetBytes(manifest.ToJsonString(new JsonSerializerOptions { WriteIndented = true })));
            WriteEntry(archive, "backup.json", payload);
        }
        return output.ToArray();
    }

    internal bool HasSamePersonalSyncContent(DesktopSnapshotDocument other) =>
        DataFingerprint() == other.DataFingerprint() && JsonNode.DeepEquals(Settings, other.Settings);

    internal string DataFingerprint()
    {
        var data = (JsonObject)root.DeepClone();
        StripSharedData(data);
        data.Remove("exportedAtMillis"); data.Remove("settings"); data.Remove("reminderChainLogs");
        static JsonNode? Canonical(JsonNode? node) => node switch {
            JsonObject obj => new JsonObject(obj.OrderBy(pair => pair.Key, StringComparer.Ordinal)
                .Select(pair => new KeyValuePair<string, JsonNode?>(pair.Key, Canonical(pair.Value)))),
            JsonArray array => new JsonArray(array.Select(Canonical).ToArray()),
            _ => node?.DeepClone()
        };
        return Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(Canonical(data)!.ToJsonString())));
    }

    public void PreserveLocalApiKeys(DesktopSnapshotDocument local)
    {
        foreach (var provider in DesktopAi.Providers(this)) {
            var saved = DesktopAi.Providers(local).FirstOrDefault(candidate =>
                candidate["id"]?.GetValue<string>() == provider["id"]?.GetValue<string>() &&
                candidate["baseUrl"]?.GetValue<string>() == provider["baseUrl"]?.GetValue<string>());
            if (saved != null) provider["apiKey"] = saved["apiKey"]?.DeepClone();
            else provider.Remove("apiKey");
        }
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

    public long SaveItem(DesktopItemEdit edit, long? existingId = null, RecurrenceScope scope = RecurrenceScope.Current)
    {
        edit = NormalizeItemEdit(edit);
        var currentInstance = scope == RecurrenceScope.Current && existingId is { } currentId &&
            !string.IsNullOrEmpty(FindTask(currentId)?["recurringSeriesId"]?.GetValue<string>());
        ValidateItemEdit(currentInstance ? edit with { RecurrenceEndEpochDay = null } : edit);
        if (!existingId.HasValue && edit.AdditionalTimeSlots.Count > 0) return SaveEventSlots(edit);
        if (existingId is { } id && FindTask(id) is { } original && !string.IsNullOrEmpty(original["recurringSeriesId"]?.GetValue<string>()))
            return SaveRecurringItem(edit, original, scope);
        return SaveItemCore(edit, existingId);
    }

    private static void ValidateItemEdit(DesktopItemEdit edit)
    {
        if (string.IsNullOrWhiteSpace(edit.Title)) throw new InvalidOperationException("请填写标题");
        if (edit.IsEvent && (edit.StartAtMillis == null || edit.EndAtMillis == null || edit.EndAtMillis <= edit.StartAtMillis))
            throw new InvalidOperationException("日程结束时间必须晚于开始时间");
        if (!edit.IsEvent && edit.RecurrenceType != "NONE" && edit.DueAtMillis == null)
            throw new InvalidOperationException("循环待办需要设置 DDL");
        if (edit.RecurrenceInterval < 1) throw new InvalidOperationException("重复间隔至少为 1");
        if (edit.IsEvent) {
            DesktopCalendarTime.Zone(edit.EventTimeZoneId);
            if (!string.IsNullOrWhiteSpace(edit.AllDayReminderRulesJson)) DesktopCalendarTime.ParseRules(edit.AllDayReminderRulesJson);
            if (edit.EventBusyStatus is not ("BUSY" or "FREE") || edit.EventVisibility is not ("DEFAULT" or "PRIVATE" or "PUBLIC"))
                throw new InvalidOperationException("日程忙闲或公开范围无效");
        }
        var at = edit.IsEvent ? edit.StartAtMillis : edit.DueAtMillis;
        if (edit.RecurrenceType != "NONE" && at is { } time && edit.RecurrenceEndEpochDay is { } end && end < EpochDay(DesktopCalendarTime.Wall(time, edit.IsEvent ? edit.EventTimeZoneId : "")))
            throw new InvalidOperationException("循环截止日期不能早于开始日期");
    }

    private long SaveItemCore(DesktopItemEdit edit, long? existingId = null, bool syncTemplate = true)
    {
        var targetCalendar = Calendars.FirstOrDefault(calendar => calendar.Id == edit.CalendarId) ?? Calendars.First(calendar => calendar.Id == "default");
        var previousParticipants = existingId is { } priorId && FindTask(priorId)?["calendarId"]?.GetValue<string>() == targetCalendar.Id
            ? FindTask(priorId)?["eventParticipantsJson"]?.GetValue<string>() ?? "[]" : "[]";
        var participants = edit.IsEvent ? DesktopEventParticipants.Reconcile(targetCalendar, edit.EventParticipantsJson, previousParticipants) : "[]";
        var previousCalendarId = existingId is { } originalId ? FindTask(originalId)?["calendarId"]?.GetValue<string>() : null;
        if (previousCalendarId != null && previousCalendarId != edit.CalendarId) MarkSharedDirty(previousCalendarId);
        MarkSharedDirty(edit.CalendarId);
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
        item["eventTimeZoneId"] = edit.EventTimeZoneId;
        item["allDayReminderRulesJson"] = edit.AllDayReminderRulesJson;
        item["eventBusyStatus"] = edit.EventBusyStatus;
        item["eventVisibility"] = edit.EventVisibility;
        item["eventParticipantsJson"] = participants;
        item["location"] = edit.Location.Trim();
        item["accentColorHex"] = edit.AccentColor;
        item["countdownEnabled"] = edit.CountdownEnabled;
        item["calendarId"] = Calendars.Any(calendar => calendar.Id == edit.CalendarId) ? edit.CalendarId : "default";
        item["countdownPastMode"] = edit.CountdownPastMode;
        item["hiddenFromBoard"] = edit.HiddenFromBoard;
        item["checkInEnabled"] = edit.CheckInEnabled;
        item["checkInReminderOffsetsCsv"] ??= "";
        item["totalCheckInMinutes"] = item["totalCheckInMinutes"]?.GetValue<int>() ?? 0;
        item["reminderAtMillis"] = DesktopCalendarTime.Triggers(edit).FirstOrDefault() is var firstTrigger && firstTrigger != 0 ? firstTrigger : null;
        item["reminderOffsetsCsv"] = string.Join(',', edit.ReminderOffsets.OrderByDescending(value => value));
        item["reminderEnabled"] = edit.ReminderEnabled;
        item["ringEnabled"] ??= true;
        item["vibrateEnabled"] ??= true;
        item["alarmMode"] ??= false;
        item["voiceEnabled"] ??= false;
        item["reminderDeliveryMode"] ??= "FULLSCREEN_AND_NOTIFICATION";
        item["groupId"] = edit.GroupId;
        item["categoryKey"] = "ROUTINE";
        item["recurrenceType"] = edit.RecurrenceType;
        item["recurrenceWeekdays"] = edit.RecurrenceWeekdays;
        item["recurrenceInterval"] = edit.RecurrenceInterval;
        item["recurrenceInvalidDatePolicy"] = edit.RecurrenceInvalidDatePolicy;
        item["recurrenceMonthlyOrdinal"] = edit.RecurrenceMonthlyOrdinal;
        item["recurrenceMonthlyWeekday"] = edit.RecurrenceMonthlyWeekday;
        item["recurrenceEndEpochDay"] = edit.RecurrenceEndEpochDay;
        item["recurrenceAnchorDueAtMillis"] = edit.RecurrenceType == "NONE" ? null : edit.IsEvent ? edit.StartAtMillis : dueAt;
        item["reminderOffsetMinutes"] = edit.ReminderOffsets.FirstOrDefault();
        var seriesId = edit.RecurrenceType == "NONE" ? null : previousSeriesId ?? Guid.NewGuid().ToString();
        item["recurringSeriesId"] = seriesId;
        if (syncTemplate) SyncRecurringTemplate(item, edit, dueAt, previousSeriesId, seriesId);
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
        MarkSharedDirty(item["calendarId"]?.GetValue<string>());
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
        MarkSharedDirty(target["calendarId"]?.GetValue<string>());
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
        SkipOccurrence(target);
        RemoveOccurrence(target);
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
        var anchor = DesktopCalendarTime.Wall(anchorMillis, edit.IsEvent ? edit.EventTimeZoneId : "");
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
        template["calendarId"] = Calendars.Any(calendar => calendar.Id == edit.CalendarId) ? edit.CalendarId : "default";
        template["countdownPastMode"] = edit.CountdownPastMode;
        template["hiddenFromBoard"] = edit.HiddenFromBoard;
        template["checkInEnabled"] = edit.CheckInEnabled;
        template["checkInReminderOffsetsCsv"] ??= "";
        template["multiSlotBundleId"] ??= item["multiSlotBundleId"]?.DeepClone();
        template["allDay"] = edit.AllDay;
        template["eventTimeZoneId"] = edit.EventTimeZoneId;
        template["allDayReminderRulesJson"] = edit.AllDayReminderRulesJson;
        template["eventBusyStatus"] = edit.EventBusyStatus;
        template["eventVisibility"] = edit.EventVisibility;
        var calendar = Calendars.FirstOrDefault(value => value.Id == edit.CalendarId);
        template["eventParticipantsJson"] = calendar == null || !IsSharedCalendar(calendar.Id) ? "[]" :
            DesktopEventParticipants.Select(DesktopEventParticipants.Reconcile(calendar, edit.EventParticipantsJson,
                item["eventParticipantsJson"]?.GetValue<string>() ?? "[]"), "[]");
        template["groupId"] = edit.GroupId;
        template["dueHour"] = anchor.Hour;
        template["dueMinute"] = anchor.Minute;
        template["eventDurationMinutes"] = edit.IsEvent && edit.StartAtMillis is not null && edit.EndAtMillis is not null
            ? (int?)Math.Max(1, (edit.EndAtMillis.Value - edit.StartAtMillis.Value) / 60_000L)
            : null;
        template["reminderOffsetMinutes"] = edit.ReminderEnabled && edit.ReminderOffsets.Count > 0 ? edit.ReminderOffsets.First() : null;
        template["reminderOffsetsCsv"] = edit.ReminderEnabled ? string.Join(',', edit.ReminderOffsets.OrderByDescending(value => value)) : "";
        template["reminderEnabled"] = edit.ReminderEnabled;
        template["ringEnabled"] = item["ringEnabled"]?.DeepClone();
        template["vibrateEnabled"] = item["vibrateEnabled"]?.DeepClone();
        template["alarmMode"] = item["alarmMode"]?.DeepClone();
        template["reminderDeliveryMode"] = item["reminderDeliveryMode"]?.DeepClone();
        template["recurrenceType"] = edit.RecurrenceType;
        template["recurrenceWeekdays"] = weekdays;
        template["recurrenceInterval"] = edit.RecurrenceInterval;
        template["recurrenceInvalidDatePolicy"] = edit.RecurrenceInvalidDatePolicy;
        template["recurrenceMonthlyOrdinal"] = edit.RecurrenceType == "MONTHLY_NTH_WEEKDAY" ? edit.RecurrenceMonthlyOrdinal ?? (anchor.Day - 1) / 7 + 1 : null;
        template["recurrenceMonthlyWeekday"] = edit.RecurrenceType == "MONTHLY_NTH_WEEKDAY" ? edit.RecurrenceMonthlyWeekday ?? ((int)anchor.DayOfWeek + 6) % 7 + 1 : null;
        template["recurrenceMonthlyDay"] = edit.RecurrenceType == "MONTHLY_DAY" ? anchor.Day : null;
        var lunar = edit.RecurrenceType == "YEARLY_LUNAR_DATE" ? LunarMonthDay(anchor) : (0, 0);
        template["recurrenceYearlyMonth"] = edit.RecurrenceType == "YEARLY_DATE" ? anchor.Month : lunar.Item1 == 0 ? null : lunar.Item1;
        template["recurrenceYearlyDay"] = edit.RecurrenceType == "YEARLY_DATE" ? anchor.Day : lunar.Item2 == 0 ? null : lunar.Item2;
        template["startEpochDay"] = startEpochDay;
        template["recurrenceNeverEnds"] = edit.RecurrenceEndEpochDay == null;
        template["endEpochDay"] = edit.RecurrenceEndEpochDay ?? startEpochDay + 36525L;
        foreach (var key in RecurrenceFields.Where(key => key != "recurrenceAnchorDueAtMillis"))
            if (template[key] != null) item[key] = template[key]!.DeepClone();
        item["recurrenceEndEpochDay"] = edit.RecurrenceEndEpochDay;
    }

    private void EnsureShape()
    {
        root["calendars"] ??= new JsonArray();
        if (!root["calendars"]!.AsArray().OfType<JsonObject>().Any(value => value["id"]?.GetValue<string>() == "default"))
            SaveCalendar("default", "常规日历", "#3370FF");
        foreach (var name in new[] { "groups", "quickChecklistGroups", "templates", "tasks", "reminderChainLogs", "scheduleTemplates", "planningNotes", "planningLineMappings", "planningNodes", "aiReports", "todoGroupTags", "eventCheckIns", "recurringInstanceSkips", "activityRecords" })
            root[name] ??= new JsonArray();
        root["settings"] ??= new JsonObject();
        root["snapshotVersion"] ??= 1;
        var calendarIds = Calendars.Select(calendar => calendar.Id).ToHashSet();
        foreach (var item in Tasks.OfType<JsonObject>().Concat(root["templates"]!.AsArray().OfType<JsonObject>()))
            if (!calendarIds.Contains(item["calendarId"]?.GetValue<string>() ?? "default")) item["calendarId"] = "default";
        RemapScheduleCalendars(id => calendarIds.Contains(id) ? id : "default");
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
    public string EventTimeZoneId { get; init; } = "";
    public string EventBusyStatus { get; init; } = "BUSY";
    public string EventVisibility { get; init; } = "DEFAULT";
    public string SharedEventId { get; init; } = "";
    public string EventParticipantsJson { get; init; } = "[]";
    public string? RecurringSeriesId { get; init; }
    public string RecurrenceType { get; init; } = "NONE";
    public string CalendarId { get; init; } = "default";
    public string CountdownPastMode { get; init; } = "AUTO";
    public long? CompletedAtMillis { get; init; }
    public bool AllDay { get; init; }
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
    ) { RecurringSeriesId = item["recurringSeriesId"]?.GetValue<string>(), RecurrenceType = item["recurrenceType"]?.GetValue<string>() ?? "NONE",
        CalendarId = item["calendarId"]?.GetValue<string>() ?? "default", CountdownPastMode = item["countdownPastMode"]?.GetValue<string>() ?? "AUTO",
        CompletedAtMillis = item["completedAtMillis"]?.GetValue<long?>(), AllDay = item["allDay"]?.GetValue<bool>() ?? false,
        EventTimeZoneId = item["eventTimeZoneId"]?.GetValue<string>() ?? "", EventBusyStatus = item["eventBusyStatus"]?.GetValue<string>() ?? "BUSY",
        EventVisibility = item["eventVisibility"]?.GetValue<string>() ?? "DEFAULT", SharedEventId = item["sharedEventId"]?.GetValue<string>() ?? "",
        EventParticipantsJson = item["eventParticipantsJson"]?.GetValue<string>() ?? "[]" };
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
    public bool HiddenFromBoard { get; init; }
    public bool CheckInEnabled { get; init; }
    public string CalendarId { get; init; } = "default";
    public string CountdownPastMode { get; init; } = "AUTO";
    public string EventTimeZoneId { get; init; } = "";
    public string AllDayReminderRulesJson { get; init; } = "";
    public string EventBusyStatus { get; init; } = "BUSY";
    public string EventVisibility { get; init; } = "DEFAULT";
    public int RecurrenceInterval { get; init; } = 1;
    public string EventParticipantsJson { get; init; } = "[]";
    public string RecurrenceInvalidDatePolicy { get; init; } = "CLAMP";
    public int? RecurrenceMonthlyOrdinal { get; init; }
    public int? RecurrenceMonthlyWeekday { get; init; }
    public IReadOnlyList<DesktopTimeSlot> AdditionalTimeSlots { get; init; } = Array.Empty<DesktopTimeSlot>();
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
        )
        {
            HiddenFromBoard = item["hiddenFromBoard"]?.GetValue<bool>() ?? false,
            CalendarId = item["calendarId"]?.GetValue<string>() ?? "default",
            CountdownPastMode = item["countdownPastMode"]?.GetValue<string>() ?? "AUTO",
            CheckInEnabled = item["checkInEnabled"]?.GetValue<bool>() ?? false,
            EventTimeZoneId = item["eventTimeZoneId"]?.GetValue<string>() ?? "",
            AllDayReminderRulesJson = item["allDayReminderRulesJson"]?.GetValue<string>() ?? "",
            EventBusyStatus = item["eventBusyStatus"]?.GetValue<string>() ?? "BUSY",
            EventVisibility = item["eventVisibility"]?.GetValue<string>() ?? "DEFAULT",
            EventParticipantsJson = item["eventParticipantsJson"]?.GetValue<string>() ?? "[]",
            RecurrenceInterval = item["recurrenceInterval"]?.GetValue<int>() ?? 1,
            RecurrenceInvalidDatePolicy = item["recurrenceInvalidDatePolicy"]?.GetValue<string>() ?? "CLAMP",
            RecurrenceMonthlyOrdinal = item["recurrenceMonthlyOrdinal"]?.GetValue<int?>(),
            RecurrenceMonthlyWeekday = item["recurrenceMonthlyWeekday"]?.GetValue<int?>()
        };
    }
}
