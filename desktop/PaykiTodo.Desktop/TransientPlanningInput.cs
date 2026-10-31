using System.Globalization;
using System.Text.RegularExpressions;

namespace PaykiTodo.Desktop;

internal enum PlanningReminderMode { Omitted, Disabled, Explicit }
internal sealed record TransientPlanningSlot(DateTime StartAt, DateTime EndAt, string RecurrenceType = "NONE", int Interval = 1,
    string Weekdays = "", DateTime? EndDate = null, int? IntendedDay = null);
internal sealed record PlanningAbsoluteReminderAssignment(DateTime ReminderAt, int SlotIndex, DateTime StartAt);
internal sealed record TransientPlanningCandidate(string Id, string? ParentId, int LineNumber, int Column, string SourceLine,
    string Title, string Location, string Notes, DateTime? DueAt, IReadOnlyList<TransientPlanningSlot> Slots,
    PlanningReminderMode ReminderMode, IReadOnlyList<int> ReminderOffsets, IReadOnlyList<DateTime> AbsoluteReminders,
    string TimeExpression, string ReminderExpression, string RangeExpression)
{
    public long? GroupId { get; init; }
    public string CalendarId { get; init; } = "default";
    public bool IsEvent => Slots.Count > 0;
    public bool IsRecurring => Slots.Any(s => s.RecurrenceType != "NONE");
    public IReadOnlyList<PlanningAbsoluteReminderAssignment> AssignAbsoluteReminders()
    {
        if (!IsRecurring || ReminderMode == PlanningReminderMode.Disabled) return Array.Empty<PlanningAbsoluteReminderAssignment>();
        var result = new List<PlanningAbsoluteReminderAssignment>();
        foreach (var reminder in AbsoluteReminders.Distinct().Order()) {
            var closest = Slots.Select((slot, index) => new { Index = index, Start = DesktopSnapshotDocument.NextPlanningOccurrence(slot, reminder) })
                .Where(value => value.Start != null).OrderBy(value => value.Start).ThenBy(value => value.Index).FirstOrDefault();
            if (closest == null) throw new InvalidOperationException($"绝对提醒{reminder:yyyy-MM-dd HH:mm}之后在循环范围内没有日程，请调整提醒或循环范围");
            result.Add(new(reminder, closest.Index, closest.Start!.Value));
        }
        return result;
    }
    public string? Validate()
    {
        if (string.IsNullOrWhiteSpace(Title)) return "标题不能为空";
        if (Slots.Any(s => s.EndAt <= s.StartAt)) return "结束时间必须晚于开始时间，跨日请补结束日期";
        if (ReminderOffsets.Any(n => n < 0)) return "提醒分钟必须是非负整数";
        if (!IsEvent && DueAt == null && ReminderMode == PlanningReminderMode.Explicit && ReminderOffsets.Count > 0) return "相对提醒需要DDL，请补时间或使用()关闭提醒";
        if (Slots.Any(s => s.EndDate < s.StartAt.Date)) return "循环截止日期早于首次日程";
        try { _ = AssignAbsoluteReminders(); } catch (InvalidOperationException error) { return error.Message; }
        return null;
    }
}
internal sealed record TransientPlanningDiagnostic(int LineNumber, int Column, string Message);
internal sealed record TransientPlanningResult(IReadOnlyList<TransientPlanningCandidate> Candidates, IReadOnlyList<TransientPlanningDiagnostic> Diagnostics, string Message = "")
{ public bool HasErrors => Diagnostics.Count > 0; }

internal static class TransientPlanningParser
{
    private static readonly Regex DateTimePattern = new(@"^(?:(\d{4}-\d{2}-\d{2}|\d{2}-\d{2})[ +])?(\d{1,2}:\d{2})$");
    private static readonly Regex RangePattern = new(@"^(?:自(\d{4}-\d{2}-\d{2})至|截至)(\d{4}-\d{2}-\d{2})$");
    private static readonly Regex CyclePattern = new(@"^每(?:(\d+))?(天|周|月|年)\[(.*)\]$");
    public static TransientPlanningResult Parse(string text, DateTime? referenceTime = null)
    {
        var today = (referenceTime ?? DateTime.Now).Date;
        var candidates = new List<TransientPlanningCandidate>(); var diagnostics = new List<TransientPlanningDiagnostic>();
        var parents = new Dictionary<int, string>();
        var lines = text.Replace("\r\n", "\n").Replace('\r', '\n').Split('\n');
        for (var row = 0; row < lines.Length; row++) {
            var raw = lines[row]; if (string.IsNullOrWhiteSpace(raw)) continue;
            var leading = raw.TakeWhile(char.IsWhiteSpace).ToArray(); var spaces = leading.Sum(c => c == '\t' ? 2 : 1); var depth = spaces / 2;
            var content = raw.TrimStart(); if (content.StartsWith('#')) continue;
            var column = leading.Length + (content.StartsWith("- ") ? 3 : 1);
            var errorColumn = column;
            if (content.StartsWith("- ")) content = content[2..]; else if (content == "-") content = "";
            try {
                Require(!string.IsNullOrWhiteSpace(content), "标题不能为空");
                var pieces = Split(content, ';'); string? lastId = null;
                for (var index = 0; index < pieces.Count; index++) {
                    var piece = pieces[index]; var value = piece.Text.Trim(); if (value.Length == 0) continue;
                    errorColumn = column + piece.Offset + piece.Text.TakeWhile(char.IsWhiteSpace).Count();
                    if (value.StartsWith('#')) {
                        Require(lastId != null, "备注请放在结束分号后");
                        var last = candidates.FindLastIndex(c => c.Id == lastId);
                        Require(last >= 0, "备注前没有有效事项");
                        candidates[last] = candidates[last] with { Notes = content[piece.Offset..].Trim().TrimStart('#').Trim() }; break;
                    }
                    var fields = Split(value, ',').Select(f => f.Text.Trim()).ToList();
                    Require(fields.Count > 0 && fields.All(f => f.Length > 0), "存在空字段，请省略整个字段或补齐内容");
                    var title = Unquote(fields[0]); Require(title.Length > 0, "标题不能为空");
                    string location = ""; string? time = null, reminder = null, range = null;
                    foreach (var field in fields.Skip(1)) {
                        if (field.StartsWith('@')) { Require(location.Length == 0, "地点字段重复"); location = Unquote(field[1..].Trim()); Require(location.Length > 0, "@后请填写地点"); }
                        else if (field.StartsWith('(')) { Require(reminder == null, "提醒字段重复"); reminder = field; }
                        else if (RangePattern.IsMatch(field)) { Require(range == null, "循环范围重复"); range = field; }
                        else if (DateTimePattern.IsMatch(field) || field.Contains('>') || field.StartsWith('每')) { Require(time == null, "时间字段重复"); time = field; }
                        else throw new InvalidOperationException("无法识别字段：" + field);
                    }
                    Require(depth == 0 || parents.ContainsKey(depth - 1), "缺少上一级事项，请减少缩进");
                    var id = $"{row + 1}:{column + piece.Offset}";
                    var candidate = Item(id, parents.GetValueOrDefault(depth - 1), row + 1, column + piece.Offset, raw, title, location, time, reminder, range, today);
                    if (candidate.Validate() is { } error) throw new InvalidOperationException(error);
                    candidates.Add(candidate); lastId = id;
                    foreach (var key in parents.Keys.Where(k => k >= depth).ToList()) parents.Remove(key);
                    parents[depth] = id;
                }
            } catch (Exception error) { diagnostics.Add(new(row + 1, errorColumn, error.Message)); }
        }
        return new(candidates, diagnostics);
    }
    private static TransientPlanningCandidate Item(string id, string? parent, int line, int column, string source,
        string title, string location, string? time, string? reminder, string? range, DateTime today)
    {
        DateTime? due = null; IReadOnlyList<TransientPlanningSlot> slots = Array.Empty<TransientPlanningSlot>();
        if (time != null) {
            if (time.StartsWith('每')) slots = Recurring(time, range, today);
            else { Require(range == null, "循环范围需要循环时间表达式"); if (time.Contains('>')) slots = new[] { Slot(time, today) }; else due = DateTimeValue(time, today); }
        } else Require(range == null, "循环范围需要循环时间表达式");
        var mode = PlanningReminderMode.Omitted; var offsets = new List<int> { 5, 0 }; var absolutes = new List<DateTime>();
        if (reminder != null) {
            Require(reminder.StartsWith('(') && reminder.EndsWith(')'), "提醒需要完整半角括号");
            var body = reminder[1..^1].Trim(); mode = body.Length == 0 ? PlanningReminderMode.Disabled : PlanningReminderMode.Explicit; offsets.Clear();
            if (body.Length > 0) foreach (var raw in body.Split(',')) {
                var value = raw.Trim(); Require(value.Length > 0, "提醒列表存在空值");
                if (value.All(char.IsAsciiDigit)) { Require(int.TryParse(value, out var minutes), "提醒分钟超出整数范围"); offsets.Add(minutes); }
                else absolutes.Add(DateTimeValue(value, today));
            }
        }
        return new(id, parent, line, column, source, title, location, "", due, slots, mode, offsets.Distinct().OrderDescending().ToList(), absolutes.Distinct().Order().ToList(), time ?? "", reminder ?? "", range ?? "");
    }
    private static IReadOnlyList<TransientPlanningSlot> Recurring(string value, string? range, DateTime today)
    {
        var match = CyclePattern.Match(value); Require(match.Success, "循环格式应为每周[周一 08:00>09:00]等");
        var interval = match.Groups[1].Value.Length == 0 ? 1 : int.Parse(match.Groups[1].Value, CultureInfo.InvariantCulture);
        Require(interval is >= 1 and <= 10000, "循环间隔必须为1至10000"); var unit = match.Groups[2].Value;
        Require(unit is "天" or "周" || interval == 1, "当前支持每N天和每N周，月/年请写每月或每年");
        var bounds = range == null ? null : RangePattern.Match(range);
        var begin = bounds?.Groups[1].Value is { Length: > 0 } first ? Date(first) : today;
        DateTime? end = bounds == null ? null : Date(bounds.Groups[2].Value); Require(end == null || end >= begin, "循环截止不能早于生效日期");
        var weekStart = begin.AddDays(-(((int)begin.DayOfWeek + 6) % 7));
        var result = new List<TransientPlanningSlot>();
        foreach (var raw in match.Groups[3].Value.Split(',')) {
            var body = raw.Trim(); Require(body.Length > 0, "循环至少需要一个完整时段");
            var date = begin; var times = body; var kind = "DAILY"; var weekdays = ""; int? intendedDay = null;
            if (unit == "周") {
                var day = Regex.Match(body, @"^周([一二三四五六日天])\s+(.+)$"); Require(day.Success, "每周时段请填写周几和起止时间");
                var dayNumber = "一二三四五六日".IndexOf(day.Groups[1].Value.Replace('天', '日'), StringComparison.Ordinal) + 1;
                date = weekStart.AddDays(dayNumber - 1); if (date < begin) date = date.AddDays(7L * interval);
                times = day.Groups[2].Value; kind = "WEEKLY"; weekdays = dayNumber.ToString(CultureInfo.InvariantCulture);
            } else if (unit == "月") {
                var day = Regex.Match(body, @"^(\d{1,2})日\s+(.+)$"); Require(day.Success, "每月时段请写15日 09:00>10:00");
                intendedDay = int.Parse(day.Groups[1].Value); Require(intendedDay is >= 1 and <= 31, "每月日期必须在1至31日");
                date = new DateTime(begin.Year, begin.Month, Math.Min(intendedDay.Value, DateTime.DaysInMonth(begin.Year, begin.Month)));
                if (date < begin) { var next = new DateTime(begin.Year, begin.Month, 1).AddMonths(1); date = new(next.Year, next.Month, Math.Min(intendedDay.Value, DateTime.DaysInMonth(next.Year, next.Month))); }
                times = day.Groups[2].Value; kind = "MONTHLY_DAY";
            } else if (unit == "年") {
                var day = Regex.Match(body, @"^(\d{2}-\d{2})\s+(.+)$"); Require(day.Success, "每年时段请写11-01 09:00>10:00");
                var parts = day.Groups[1].Value.Split('-').Select(int.Parse).ToArray(); _ = new DateTime(2000, parts[0], parts[1]); intendedDay = parts[1];
                DateTime InYear(int year) => new(year, parts[0], Math.Min(parts[1], DateTime.DaysInMonth(year, parts[0])));
                date = InYear(begin.Year); if (date < begin) date = InYear(begin.Year + 1); times = day.Groups[2].Value; kind = "YEARLY_DATE";
            }
            var clocks = times.Split('>'); Require(clocks.Length == 2 && clocks.All(c => Regex.IsMatch(c.Trim(), @"^\d{1,2}:\d{2}$")), "循环时段内请填写时钟，例如08:00>09:00");
            Require(end == null || date <= end, "范围内没有该时段");
            result.Add(Slot(times, date) with { RecurrenceType = kind, Interval = interval, Weekdays = weekdays, EndDate = end, IntendedDay = intendedDay });
        }
        return result.Distinct().ToList();
    }
    private static TransientPlanningSlot Slot(string value, DateTime today)
    {
        var parts = value.Split('>'); Require(parts.Length == 2, "起止时间必须用一个>连接");
        var start = DateTimeValue(parts[0].Trim(), today); var end = DateTimeValue(parts[1].Trim(), today);
        Require(end > start, "结束时间必须晚于开始时间，跨日请补结束日期"); return new(start, end);
    }
    private static DateTime Date(string value) => DateTime.ParseExact(value, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None);
    private static DateTime DateTimeValue(string value, DateTime today)
    {
        var match = DateTimePattern.Match(value); Require(match.Success, "时间格式错误：" + value);
        var text = match.Groups[1].Value; var date = text.Length == 0 ? today : Date(text.Length == 5 ? today.Year + "-" + text : text);
        var parts = match.Groups[2].Value.Split(':').Select(int.Parse).ToArray(); Require(parts[0] < 24 && parts[1] < 60, "时钟超出范围");
        return date.AddHours(parts[0]).AddMinutes(parts[1]);
    }
    private static string Unquote(string value)
    {
        if (!value.StartsWith('"')) { Require(!value.Contains('"') && !value.Contains('#'), "标点请用首尾双引号包裹，备注放在分号后"); return value; }
        Require(value.Length >= 2 && value.EndsWith('"'), "字段缺少末尾双引号"); return value[1..^1];
    }
    private sealed record Piece(string Text, int Offset);
    private static List<Piece> Split(string value, char delimiter)
    {
        var result = new List<Piece>(); var begin = 0; var round = 0; var square = 0; var quoted = false; var fieldStart = true;
        for (var index = 0; index < value.Length; index++) {
            var c = value[index];
            if (quoted) {
                if (c == '"') {
                    var tail = value[(index + 1)..].TrimStart();
                    Require(!(delimiter == ';' && tail.StartsWith(';') && tail[1..].TrimStart().StartsWith('"')), "引号和分号存在多种解释，请把事项拆为多行");
                    if (ClosingQuote(tail)) quoted = false;
                }
                continue;
            }
            if (c == '"' && fieldStart) { quoted = true; fieldStart = false; }
            else if (c == '(') { round++; fieldStart = false; }
            else if (c == ')') { round--; Require(round >= 0, "多余右括号"); }
            else if (c == '[') { square++; fieldStart = false; }
            else if (c == ']') { square--; Require(square >= 0, "多余右方括号"); }
            else if (c == '#' && round == 0 && square == 0) { Require(delimiter == ';' && begin > 0 && string.IsNullOrWhiteSpace(value[begin..index]), "备注请放在结束分号后"); result.Add(new(value[begin..], begin)); return result; }
            else if (c == delimiter && round == 0 && square == 0) { result.Add(new(value[begin..index], begin)); begin = index + 1; fieldStart = true; }
            else if (c == ',' && round == 0 && square == 0) fieldStart = true;
            else if (c == '@' && fieldStart) { }
            else if (!char.IsWhiteSpace(c)) fieldStart = false;
        }
        Require(!quoted && round == 0 && square == 0, "引号或括号没有闭合"); result.Add(new(value[begin..], begin)); return result;
    }
    private static bool ClosingQuote(string tail)
    {
        var rest = tail.TrimStart(); if (rest.Length == 0 || rest.StartsWith(';')) return true;
        if (!rest.StartsWith(',')) return false; var next = rest[1..].TrimStart();
        return next.Length == 0 || next.StartsWith('@') || next.StartsWith('(') || next.StartsWith('每') || next.StartsWith('自') || next.StartsWith("截至") || Regex.IsMatch(next, @"^(?:\d{4}-\d{2}-\d{2}[ +]|\d{2}-\d{2}[ +])?\d{1,2}:\d{2}");
    }
    public static string NextLinePrefix(string line)
    {
        var indent = new string(line.TakeWhile(c => c is ' ' or '\t').ToArray()).Replace("\t", "  ");
        bool done; try { done = Split(line.Trim().StartsWith("- ") ? line.Trim()[2..] : line.Trim(), ';').Count > 1; } catch { done = false; }
        return indent + (done ? "- " : "  - ");
    }
    private static void Require(bool condition, string message) { if (!condition) throw new InvalidOperationException(message); }
}
