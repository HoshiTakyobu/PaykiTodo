using System.Windows;
using System.Windows.Controls;
using System.Windows.Markup;
using System.Windows.Media;
using Button = System.Windows.Controls.Button;
using TextBox = System.Windows.Controls.TextBox;
using Brush = System.Windows.Media.Brush;
using Color = System.Windows.Media.Color;
using Orientation = System.Windows.Controls.Orientation;
using SystemColors = System.Windows.SystemColors;
using CheckBox = System.Windows.Controls.CheckBox;
using ComboBox = System.Windows.Controls.ComboBox;
using Control = System.Windows.Controls.Control;

namespace PaykiTodo.Desktop;

internal static class NativeUi
{
    public static bool Dark { get; set; }
    public static Brush Ink => B(Dark ? "#E8EDF3" : "#19382F");
    public static Brush Muted => B(Dark ? "#BCC8D4" : "#61736E");
    public static Brush Forest => B(Dark ? "#93C6AC" : "#326F59");
    public static Brush Paper => B(Dark ? "#22313E" : "#FFFFFF");
    public static Brush B(string hex)
    {
        if (Dark) hex = hex.ToUpperInvariant() switch {
            "#EEF3F4" or "#F1F5F3" => "#15232F", "#F8FAF9" => "#1B2935", "#DCEBE2" => "#354453",
            "#EAF0ED" => "#30404E", "#FFFAE9" => "#4C452E", "#F6FBF8" => "#1D2B38", "#E3EBE6" or "#CBD8D2" => "#445463", "#E2EFE6" => "#344553", _ => hex };
        return new SolidColorBrush((Color)System.Windows.Media.ColorConverter.ConvertFromString(hex));
    }
    public static TextBlock Text(string value, double size = 14, Brush? color = null, bool bold = false) => new()
    {
        Text = value, FontSize = size, Foreground = color ?? Ink, TextWrapping = TextWrapping.Wrap,
        FontWeight = bold ? FontWeights.SemiBold : FontWeights.Normal, Margin = new Thickness(0, 2, 0, 4)
    };
    public static StackPanel Column(params UIElement[] elements)
    {
        var panel = new StackPanel(); foreach (var element in elements) panel.Children.Add(element); return panel;
    }
    public static WrapPanel Row(params UIElement[] elements)
    {
        var panel = new WrapPanel(); foreach (var element in elements) panel.Children.Add(element); return panel;
    }
    public static Border Card(UIElement child, double padding = 20) => new()
    {
        Background = Paper, CornerRadius = new CornerRadius(10), Padding = new Thickness(padding),
        Margin = new Thickness(0, 0, 0, 14), Child = child
    };
    public static Button Action(string label, Action click, bool primary = false)
    {
        var button = new Button { Content = label, Padding = new Thickness(16, 9, 16, 9),
            Margin = new Thickness(0, 0, 8, 6), Background = primary ? B("#326F59") : B("#EAF0ED"),
            Foreground = primary ? B("#FFFFFF") : Ink, BorderThickness = new Thickness(0), Cursor = System.Windows.Input.Cursors.Hand };
        button.Click += (_, _) => click(); return button;
    }
    public static TextBox Input(string value = "", bool multiline = false) => new()
    {
        Text = value, FontSize = 14, Padding = new Thickness(10), Margin = new Thickness(0, 4, 0, 12),
        MinHeight = multiline ? 110 : 40, AcceptsReturn = multiline, TextWrapping = TextWrapping.Wrap,
        VerticalScrollBarVisibility = multiline ? ScrollBarVisibility.Auto : ScrollBarVisibility.Hidden,
        BorderBrush = B("#CBD8D2"), BorderThickness = new Thickness(1), Background = Paper, Foreground = Ink
    };
    public static ScrollViewer Scroll(UIElement content) => new() { Content = content,
        VerticalScrollBarVisibility = ScrollBarVisibility.Auto, HorizontalScrollBarVisibility = ScrollBarVisibility.Disabled };
    public static Window Dialog(Window owner, string title, double width = 600, double height = 680) => new()
    {
        Owner = owner, Title = title, Width = width, Height = height, MinWidth = 420, MinHeight = 350,
        WindowStartupLocation = WindowStartupLocation.CenterOwner, Background = B("#F1F5F3"),
        FontFamily = new System.Windows.Media.FontFamily("Microsoft YaHei UI"), FontSize = 14,
        ShowInTaskbar = false, Resources = owner.Resources
    };
    public static void InstallStyles(ResourceDictionary resources)
    {
        resources[SystemColors.WindowBrushKey] = Paper;
        resources[SystemColors.WindowTextBrushKey] = Ink;
        resources[SystemColors.ControlBrushKey] = Paper;
        resources[SystemColors.ControlTextBrushKey] = Ink;
        resources[SystemColors.HighlightBrushKey] = Forest;
        resources[SystemColors.HighlightTextBrushKey] = B("#FFFFFF");
        var styles = (ResourceDictionary)XamlReader.Parse("""
            <ResourceDictionary xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">
              <Style TargetType="Button">
                <Setter Property="HorizontalContentAlignment" Value="Center"/>
                <Setter Property="VerticalContentAlignment" Value="Center"/>
                <Setter Property="Template"><Setter.Value><ControlTemplate TargetType="Button">
                  <Border x:Name="edge" Background="{TemplateBinding Background}" CornerRadius="8" Padding="{TemplateBinding Padding}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="{TemplateBinding BorderThickness}">
                    <ContentPresenter HorizontalAlignment="{TemplateBinding HorizontalContentAlignment}" VerticalAlignment="Center"/>
                  </Border>
                  <ControlTemplate.Triggers>
                    <Trigger Property="IsMouseOver" Value="True"><Setter TargetName="edge" Property="Opacity" Value="0.8"/></Trigger>
                    <Trigger Property="IsEnabled" Value="False"><Setter TargetName="edge" Property="Opacity" Value="0.45"/></Trigger>
                    <Trigger Property="IsKeyboardFocused" Value="True"><Setter TargetName="edge" Property="BorderBrush" Value="#326F59"/><Setter TargetName="edge" Property="BorderThickness" Value="2"/></Trigger>
                  </ControlTemplate.Triggers>
                </ControlTemplate></Setter.Value></Setter>
              </Style>
              <Style TargetType="CheckBox"><Setter Property="Margin" Value="0,8,16,8"/><Setter Property="VerticalContentAlignment" Value="Center"/></Style>
              <Style TargetType="ComboBox"><Setter Property="Padding" Value="8"/><Setter Property="MinHeight" Value="36"/><Setter Property="Margin" Value="0,4,8,10"/></Style>
              <Style TargetType="DatePicker"><Setter Property="MinHeight" Value="36"/><Setter Property="Margin" Value="0,4,8,10"/></Style>
            </ResourceDictionary>
            """);
        resources.MergedDictionaries.Add(styles);
        foreach (var type in new[] { typeof(CheckBox), typeof(ComboBox), typeof(DatePicker), typeof(PasswordBox), typeof(ComboBoxItem) }) {
            var style = new Style(type, resources[type] as Style);
            style.Setters.Add(new Setter(Control.ForegroundProperty, Ink));
            style.Setters.Add(new Setter(Control.BackgroundProperty, Paper));
            style.Setters.Add(new Setter(Control.BorderBrushProperty, B("#CBD8D2")));
            resources[type] = style;
        }
    }
}
