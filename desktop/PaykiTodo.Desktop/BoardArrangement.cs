namespace PaykiTodo.Desktop;

internal static class BoardArrangement
{
    internal static int Section(DesktopItem item) => item.IsEvent && item.AllDay ? 0 : !item.IsEvent && item.DueAtMillis == long.MaxValue ? 2 : 1;
    internal static IReadOnlyList<DesktopItem> Ordered(IEnumerable<DesktopItem> todos, IEnumerable<DesktopItem> events) =>
        todos.Concat(events).DistinctBy(item => item.Id).OrderBy(Section)
            .ThenBy(item => item.IsEvent ? item.StartAtMillis ?? long.MaxValue : item.DueAtMillis)
            .ThenBy(item => item.IsEvent ? 0 : 1).ThenBy(item => item.Id).ToArray();
    internal static bool OnDay(DesktopItem item, DateTime date) {
        if (!item.IsEvent || item.Canceled || item.Completed || item.StartAtMillis == null || item.EndAtMillis == null) return false;
        var zone = item.AllDay ? item.EventTimeZoneId : "";
        return DesktopCalendarTime.Wall(item.StartAtMillis.Value, zone) < date.Date.AddDays(1) && DesktopCalendarTime.Wall(item.EndAtMillis.Value, zone) > date.Date;
    }
}
