using System.Text.Json.Nodes;

namespace PaykiTodo.Desktop;
internal sealed partial class DesktopSnapshotDocument
{
    public IReadOnlyList<long> ImportTransientPlanning(IReadOnlyList<TransientPlanningCandidate> candidates, string sessionId)
    {
        if (candidates.Count == 0 || string.IsNullOrWhiteSpace(sessionId)) throw new InvalidOperationException("没有可导入的事项");
        var ids = candidates.Select(c => c.Id).ToHashSet();
        if (ids.Count != candidates.Count) throw new InvalidOperationException("候选ID重复");
        foreach (var candidate in candidates) {
            if (candidate.Validate() is { } error) throw new InvalidOperationException($"第{candidate.LineNumber}行：{error}");
            if (candidate.ParentId != null && !ids.Contains(candidate.ParentId)) throw new InvalidOperationException("父事项未选中，请选择父事项或调整层级");
            if (!Groups.Any(g => g.Id == (candidate.GroupId ?? Groups.First().Id))) throw new InvalidOperationException("所选分组不存在");
            if (!Calendars.Any(c => c.Id == candidate.CalendarId)) throw new InvalidOperationException("所选日历不存在");
            if (candidate.CalendarId.StartsWith("shared:", StringComparison.Ordinal) || Calendars.Any(c => c.Id == candidate.CalendarId && c.ShareMode == "SHARED"))
                throw new InvalidOperationException("规划导入请选择个人日历，共享日程请从日历创建");
        }
        var before = (JsonObject)root.DeepClone();
        try {
            var persisted = new Dictionary<string,long>(); var trees = new Dictionary<string,long>(); var depths = new Dictionary<string,int>();
            var byId = candidates.ToDictionary(c => c.Id); var pending = candidates.ToList(); var created = new List<long>();
            while (pending.Count > 0) {
                var candidate = pending.FirstOrDefault(c => c.ParentId == null || persisted.ContainsKey(c.ParentId)) ?? throw new InvalidOperationException("父子关系存在循环");
                pending.Remove(candidate); var ancestor = candidate;
                var visited = new HashSet<string>();
                while (ancestor.ParentId != null) { if (!visited.Add(ancestor.Id)) throw new InvalidOperationException("父子关系存在循环"); ancestor = byId[ancestor.ParentId]; }
                var depth = candidate.ParentId == null ? 0 : depths[candidate.ParentId] + 1; depths[candidate.Id] = depth;
                long? checklist = null;
                if (!ancestor.IsEvent && (candidate.ParentId != null || candidates.Any(c => c.ParentId == candidate.Id))) {
                    if (!trees.TryGetValue(ancestor.Id, out var tree)) {
                        tree = NextId(QuickChecklistGroupsArray); trees[ancestor.Id] = tree;
                        QuickChecklistGroupsArray.Add(new JsonObject { ["id"] = tree, ["title"] = ancestor.Title, ["collapsed"] = false,
                            ["sourceFingerprint"] = $"planning:{sessionId}:{ancestor.Id}", ["createdAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(), ["updatedAtMillis"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
                    }
                    checklist = tree;
                }
                var offsets = candidate.ReminderMode == PlanningReminderMode.Disabled || (!candidate.IsEvent && candidate.DueAt == null) ? Array.Empty<int>() : candidate.ReminderOffsets;
                var assignments = candidate.AssignAbsoluteReminders();
                var absolute = candidate.ReminderMode == PlanningReminderMode.Disabled || candidate.IsRecurring
                    ? Array.Empty<long>() : candidate.AbsoluteReminders.Select(t => DesktopCalendarTime.Millis(t, "")).ToArray();
                var bundle = candidate.Slots.Count > 1 ? Guid.NewGuid().ToString() : null;
                IEnumerable<TransientPlanningSlot?> slots = candidate.IsEvent ? candidate.Slots.Cast<TransientPlanningSlot?>() : new TransientPlanningSlot?[] { null };
                long firstId = 0; var slotIds = new List<long>();
                foreach (var slot in slots) {
                    var edit = new DesktopItemEdit(candidate.IsEvent, candidate.Title, candidate.Notes, candidate.Location,
                        candidate.DueAt is { } due ? DesktopCalendarTime.Millis(due, "") : null,
                        slot == null ? null : DesktopCalendarTime.Millis(slot.StartAt, ""), slot == null ? null : DesktopCalendarTime.Millis(slot.EndAt, ""),
                        false, offsets.Count > 0 || absolute.Length > 0, offsets, candidate.GroupId ?? Groups.First().Id, "#467DAB", false,
                        slot?.RecurrenceType ?? "NONE", slot?.Weekdays ?? "", slot?.EndDate is { } end ? EpochDay(end) : null) {
                            CalendarId = candidate.CalendarId, RecurrenceInterval = slot?.Interval ?? 1,
                            RecurrenceMonthlyDay = slot?.IntendedDay, AbsoluteReminderTimes = absolute
                        };
                    var id = SaveItem(edit); if (firstId == 0) firstId = id;
                    var item = FindTask(id)!; item["parentTodoId"] = candidate.ParentId == null ? null : persisted[candidate.ParentId];
                    if (!candidate.IsEvent && checklist != null) {
                        item["quickChecklistGroupId"] = checklist; item["quickChecklistDepth"] = depth; item["quickChecklistSortOrder"] = candidates.ToList().IndexOf(candidate); item["quickChecklistCollapsed"] = false;
                    }
                    if (bundle != null) {
                        item["multiSlotBundleId"] = bundle; var series = item["recurringSeriesId"]?.GetValue<string>();
                        foreach (var template in Templates.OfType<JsonObject>().Where(t => t["seriesId"]?.GetValue<string>() == series)) template["multiSlotBundleId"] = bundle;
                    }
                    created.Add(id); slotIds.Add(id);
                }
                foreach (var assignment in assignments) {
                    var initial = FindTask(slotIds[assignment.SlotIndex])!;
                    var series = initial["recurringSeriesId"]?.GetValue<string>();
                    var start = DesktopCalendarTime.Millis(assignment.StartAt, "");
                    var owner = series == null ? initial : Tasks.OfType<JsonObject>().FirstOrDefault(item => item["recurringSeriesId"]?.GetValue<string>() == series && item["startAtMillis"]?.GetValue<long?>() == start);
                    if (owner == null) {
                        var template = Templates.OfType<JsonObject>().Single(t => t["seriesId"]?.GetValue<string>() == series);
                        owner = CreateRecurringOccurrence(template, initial, assignment.StartAt.Date, NextId(Tasks))
                            ?? throw new InvalidOperationException("绝对提醒所属日程的本地时间不存在");
                        Tasks.Add(owner); created.Add(owner["id"]!.GetValue<long>());
                    }
                    var times = DesktopItemEdit.FromJson(owner).AbsoluteReminderTimes.Append(DesktopCalendarTime.Millis(assignment.ReminderAt, "")).Distinct().Order().ToList();
                    owner["reminderAbsoluteTimesCsv"] = string.Join(',', times);
                    owner["reminderEnabled"] = true;
                    RefreshOccurrenceReminders(owner);
                }
                persisted[candidate.Id] = firstId;
            }
            return created;
        } catch { root = before; throw; }
    }
}
