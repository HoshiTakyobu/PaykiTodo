namespace PaykiTodo.Desktop;

internal static class CalendarEventLayout
{
    internal sealed record Placement(int Lane, int Columns);
    public static Dictionary<long, Placement> Arrange(IEnumerable<DesktopItem> events, DateTime day)
    {
        var result = new Dictionary<long, Placement>();
        var cluster = new List<(long Id, int Lane)>();
        var ends = new List<long>();
        long clusterEnd = 0;
        void Flush() { foreach (var item in cluster) result[item.Id] = new Placement(item.Lane, ends.Count); cluster.Clear(); ends.Clear(); }
        foreach (var item in events.OrderBy(item => item.StartAtMillis).ThenBy(item => item.Id)) {
            var start = item.StartAtMillis ?? 0; var end = item.EndAtMillis ?? start + 1;
            if (cluster.Count > 0 && start >= clusterEnd) Flush();
            var lane = ends.FindIndex(value => value <= start);
            if (lane < 0) { lane = ends.Count; ends.Add(end); } else ends[lane] = end;
            cluster.Add((item.Id, lane)); clusterEnd = Math.Max(clusterEnd, end);
        }
        Flush(); return result;
    }
}
