using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using Brush = System.Windows.Media.Brush;
using Brushes = System.Windows.Media.Brushes;
using Button = System.Windows.Controls.Button;
using TextBox = System.Windows.Controls.TextBox;
using Control = System.Windows.Controls.Control;
using HorizontalAlignment = System.Windows.HorizontalAlignment;
using Orientation = System.Windows.Controls.Orientation;

namespace PaykiTodo.Desktop;

internal static partial class NativeUi
{
    internal static FrameworkElement Chevron(string direction = "down", Brush? ink = null) => new System.Windows.Shapes.Path {
        Data = Geometry.Parse(direction switch {
            "left" => "M 6,1 L 2,5 L 6,9", "right" => "M 2,1 L 6,5 L 2,9",
            "up" => "M 1,5 L 5,1 L 9,5", _ => "M 1,1 L 5,5 L 9,1" }),
        Width = direction is "left" or "right" ? 8 : 10, Height = direction is "left" or "right" ? 10 : 6,
        Stroke = ink ?? Muted, StrokeThickness = 1.5, StrokeStartLineCap = PenLineCap.Round,
        StrokeEndLineCap = PenLineCap.Round, StrokeLineJoin = PenLineJoin.Round, VerticalAlignment = VerticalAlignment.Center
    };
    internal static Button ChevronAction(string direction, string label, Action click) {
        var button = QuietAction(label, click); button.Content = Chevron(direction); button.Width = 30;
        button.ToolTip = label; return button;
    }
    internal static Button QuietAction(string label, Action click, bool danger = false) {
        var button = Action(label, click); button.Background = Brushes.Transparent; button.BorderThickness = new Thickness(0);
        button.Foreground = danger ? B(Dark ? "#FFB4AB" : "#AF3A32") : Ink;
        button.MinHeight = 32; button.Padding = new Thickness(10, 6, 10, 6); button.Margin = new Thickness(0, 0, 4, 0);
        return button;
    }
    internal static FrameworkElement Glyph(string geometry) => new System.Windows.Shapes.Path {
        Data = Geometry.Parse(geometry), Width = 20, Height = 20, Stroke = Muted, StrokeThickness = 1.4,
        StrokeStartLineCap = PenLineCap.Round, StrokeEndLineCap = PenLineCap.Round, StrokeLineJoin = PenLineJoin.Round,
        Margin = new Thickness(0, 7, 12, 0), VerticalAlignment = VerticalAlignment.Top
    };
    internal const string ClockDrawing = "M 10,2 A 8,8 0 1 1 9.99,2 M 10,5 L 10,10 L 14,12";
    internal const string LocationDrawing = "M 10,19 C 7,15 3,12 3,8 A 7,7 0 1 1 17,8 C 17,12 13,16 10,19 Z M 13,8 A 3,3 0 1 1 7,8 A 3,3 0 1 1 13,8";
    internal const string NotesDrawing = "M 3,4 L 17,4 M 3,8 L 17,8 M 3,12 L 17,12 M 3,16 L 12,16";
    internal const string BellDrawing = "M 5,8 A 5,5 0 0 1 15,8 L 15,13 L 17,15 L 3,15 L 5,13 Z M 8,18 L 12,18 M 10,1 L 10,3";
    internal const string CalendarDrawing = "M 3,4 L 17,4 L 17,18 L 3,18 Z M 3,8 L 17,8 M 6,2 L 6,5 M 14,2 L 14,5";
    internal static Grid IconRow(string geometry, UIElement content) {
        var grid = new Grid { Margin = new Thickness(0, 3, 0, 3) };
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(32) }); grid.ColumnDefinitions.Add(new ColumnDefinition());
        grid.Children.Add(Glyph(geometry)); Grid.SetColumn(content, 1); grid.Children.Add(content); return grid;
    }
    internal static Border Divider() => new() { Height = 1, Background = B("#E1E7F0"), Margin = new Thickness(0, 10, 0, 10) };
    internal static void Integrated(Control control) {
        control.BorderThickness = new Thickness(0); control.Background = Brushes.Transparent;
        control.Padding = new Thickness(4, 6, 4, 6);
        control.MinHeight = Math.Max(34, Math.Ceiling(TextSize(14) * Math.Max(1.4, InterfaceFont.LineSpacing)) + 14);
        control.Margin = new Thickness(0);
    }
    internal static Grid Placeholder(TextBox input, string placeholder, double size = 14) {
        Integrated(input); input.FontSize = TextSize(size);
        input.MinHeight = Math.Max(input.MinHeight, Math.Ceiling(input.FontSize * Math.Max(1.4, InterfaceFont.LineSpacing)) + 14);
        System.Windows.Automation.AutomationProperties.SetName(input, placeholder);
        var hint = Text(placeholder, size, Muted); hint.Margin = new Thickness(4, 6, 4, 6); hint.IsHitTestVisible = false;
        var grid = new Grid(); grid.Children.Add(input); grid.Children.Add(hint);
        void Update() => hint.Visibility = input.Text.Length == 0 ? Visibility.Visible : Visibility.Collapsed;
        input.TextChanged += (_, _) => Update(); Update(); return grid;
    }
}
