using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class CalendarLayoutMathTests
{
    [Fact]
    public void LegacyHexCasingDoesNotAddAThirteenthPaletteCell()
    {
        var converter = new NativeColorChoice();
        var values = NativeColorChoice.Presets.Append("#4c88ff");
        Assert.Equal(12, values.Count(value => Equals(System.Windows.Visibility.Visible,
            converter.Convert(value, typeof(System.Windows.Visibility), "preset", System.Globalization.CultureInfo.InvariantCulture))));
    }

    [Fact]
    public void GridCreationUsesTheRenderedHourHeight()
    {
        var day = new DateTime(2027, 1, 12);
        Assert.Equal(day.AddHours(9).AddMinutes(15), NativeWindow.CalendarSlot(day, 12 + 9.25 * 96, hourHeight: 96));
        Assert.Equal(day, NativeWindow.CalendarSlot(day, 0, hourHeight: 96));
        Assert.Equal(day.AddHours(23).AddMinutes(45), NativeWindow.CalendarSlot(day, 10000, hourHeight: 96));
    }

    [Fact]
    public void DraggingUsesTheSameScaleAsItsGrid()
    {
        var start = new DateTime(2027, 1, 12, 9, 0, 0);
        var edit = NativeWorkspaceTests.NewItem(true, "缩放拖动") with {
            StartAtMillis = DesktopCalendarTime.Millis(start, "Asia/Shanghai"),
            EndAtMillis = DesktopCalendarTime.Millis(start.AddHours(1), "Asia/Shanghai"), EventTimeZoneId = "Asia/Shanghai" };
        var moved = NativeWindow.CalendarDragEdit(edit, 1, 48, false, hourHeight: 96);
        Assert.Equal(start.AddDays(1).AddMinutes(30), DesktopCalendarTime.Wall(moved.StartAtMillis!.Value, "Asia/Shanghai"));
        Assert.Equal(TimeSpan.FromHours(1).TotalMilliseconds, (double)(moved.EndAtMillis!.Value - moved.StartAtMillis.Value));
    }
}
