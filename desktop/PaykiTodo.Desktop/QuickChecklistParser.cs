using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;

namespace PaykiTodo.Desktop;

internal enum QuickChecklistDiagnosticSeverity
{
    Error,
    Warning
}

internal sealed record QuickChecklistDiagnostic(
    QuickChecklistDiagnosticSeverity Severity,
    int SourceLine,
    string Message
);

internal sealed record QuickChecklistDraftNode(
    int TemporaryId,
    int? ParentTemporaryId,
    int Depth,
    string Title,
    string Notes,
    int SourceLine,
    int SortOrder
);

internal sealed record QuickChecklistDraft(
    string Title,
    IReadOnlyList<QuickChecklistDraftNode> Nodes,
    IReadOnlyList<QuickChecklistDiagnostic> Diagnostics,
    string SourceFingerprint
)
{
    public int TaskCount => Nodes.Count;
    public int MaxDepth => Nodes.Count == 0 ? 0 : Nodes.Max(node => node.Depth);
    public int NoteCount => Nodes.Sum(node => node.Notes.Split('\n').Count(line => !string.IsNullOrWhiteSpace(line)));
    public bool HasErrors => Diagnostics.Any(value => value.Severity == QuickChecklistDiagnosticSeverity.Error);
}

internal static partial class QuickChecklistParser
{
    [GeneratedRegex("^(-+)#\\s*(.*)$")]
    private static partial Regex NotePattern();

    [GeneratedRegex("^(-+)\\s*(.*)$")]
    private static partial Regex TaskPattern();

    [GeneratedRegex("^(-+)(?:#)?(?:\\s.*|$)")]
    private static partial Regex ContinuationPattern();

    public static QuickChecklistDraft Parse(string rawText)
    {
        var physicalLines = rawText.Replace("\r\n", "\n").Replace('\r', '\n').Split('\n');
        var prepared = physicalLines.Select((raw, index) => new PreparedLine(index + 1, StripComment(raw).Trim()))
            .ToList();
        var effective = prepared.Where(line => line.Content.Length > 0).ToList();
        var diagnostics = new List<QuickChecklistDiagnostic>();
        var first = effective.FirstOrDefault();
        var hasTitle = first is not null && !first.Content.StartsWith('-');
        var title = hasTitle ? first!.Content : "";
        if (first is null)
            diagnostics.Add(Error(1, "请先输入清单标题"));
        else if (!hasTitle)
            diagnostics.Add(Error(first.SourceLine, "第一条有效内容必须是清单标题"));

        var nodes = new List<QuickChecklistDraftNode>();
        var lastNodeByDepth = new Dictionary<int, QuickChecklistDraftNode>();
        foreach (var line in hasTitle ? effective.Skip(1) : effective)
        {
            var noteMatch = NotePattern().Match(line.Content);
            if (noteMatch.Success)
            {
                var depth = noteMatch.Groups[1].Value.Length;
                var noteText = noteMatch.Groups[2].Value.Trim();
                if (noteText.Length == 0)
                {
                    diagnostics.Add(Error(line.SourceLine, "说明内容不能为空"));
                    continue;
                }
                var target = lastNodeByDepth.TryGetValue(depth, out var sameDepth)
                    ? sameDepth
                    : lastNodeByDepth.Where(pair => pair.Key < depth).OrderByDescending(pair => pair.Key).Select(pair => pair.Value).FirstOrDefault();
                if (target is null)
                {
                    diagnostics.Add(Error(line.SourceLine, "说明之前没有可关联的任务"));
                    continue;
                }
                var index = nodes.FindIndex(node => node.TemporaryId == target.TemporaryId);
                var updated = target with
                {
                    Notes = string.Join('\n', new[] { target.Notes, noteText }.Where(value => !string.IsNullOrWhiteSpace(value)))
                };
                if (index >= 0) nodes[index] = updated;
                lastNodeByDepth[target.Depth] = updated;
                continue;
            }

            var taskMatch = TaskPattern().Match(line.Content);
            if (taskMatch.Success)
            {
                var depth = taskMatch.Groups[1].Value.Length;
                var taskTitle = taskMatch.Groups[2].Value.Trim();
                if (taskTitle.Length == 0)
                {
                    if (line.SourceLine != prepared.LastOrDefault()?.SourceLine)
                        diagnostics.Add(Error(line.SourceLine, "任务内容不能为空"));
                    continue;
                }
                QuickChecklistDraftNode? parent = null;
                if (depth > 1)
                    parent = lastNodeByDepth.Where(pair => pair.Key < depth).OrderByDescending(pair => pair.Key).Select(pair => pair.Value).FirstOrDefault();
                if (depth > 1 && parent is null)
                {
                    diagnostics.Add(Error(line.SourceLine, "该任务缺少上级任务"));
                    continue;
                }
                if (parent is not null && parent.Depth != depth - 1)
                    diagnostics.Add(new QuickChecklistDiagnostic(QuickChecklistDiagnosticSeverity.Warning, line.SourceLine, $"层级从 {parent.Depth} 跳到 {depth}，已关联到最近的上级任务"));
                var node = new QuickChecklistDraftNode(
                    nodes.Count + 1,
                    parent?.TemporaryId,
                    depth,
                    taskTitle,
                    "",
                    line.SourceLine,
                    nodes.Count(value => value.ParentTemporaryId == parent?.TemporaryId)
                );
                nodes.Add(node);
                foreach (var stale in lastNodeByDepth.Keys.Where(value => value >= depth).ToList()) lastNodeByDepth.Remove(stale);
                lastNodeByDepth[depth] = node;
                continue;
            }

            diagnostics.Add(Error(line.SourceLine, "标题后的内容需要以短横线开头"));
        }

        if (hasTitle && nodes.Count == 0) diagnostics.Add(Error(first?.SourceLine ?? 1, "清单中至少需要一项任务"));
        return new QuickChecklistDraft(title, nodes, diagnostics, ContentFingerprint(title, nodes));
    }

    public static string ContinuationPrefix(string line)
    {
        var match = ContinuationPattern().Match(StripComment(line).TrimStart());
        return match.Success ? match.Groups[1].Value + " " : "";
    }

    private static string StripComment(string value)
    {
        var index = value.IndexOf("//", StringComparison.Ordinal);
        return index >= 0 ? value[..index] : value;
    }

    private static string ContentFingerprint(string title, IEnumerable<QuickChecklistDraftNode> nodes)
    {
        var normalized = new StringBuilder(title.Trim());
        foreach (var node in nodes)
            normalized.Append('\n').Append(node.Depth).Append('|').Append(node.ParentTemporaryId ?? 0).Append('|')
                .Append(node.SortOrder).Append('|').Append(node.Title.Trim()).Append('|').Append(node.Notes.Trim());
        return Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(normalized.ToString()))).ToLowerInvariant();
    }

    private static QuickChecklistDiagnostic Error(int line, string message) => new(QuickChecklistDiagnosticSeverity.Error, line, message);
    private sealed record PreparedLine(int SourceLine, string Content);
}
