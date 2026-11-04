using System.Globalization;
using System.Windows.Data;
using System.Windows.Media;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Markup;
using ComboBox = System.Windows.Controls.ComboBox;

namespace PaykiTodo.Desktop;

// Presets are RGB values. Existing snapshot colors remain valid and are never remapped.
public sealed class NativeColorChoice : IValueConverter
{
    internal static readonly (byte R, byte G, byte B)[] Rgb = {
        (194,75,143), (240,91,85), (248,159,69), (251,203,69), (81,186,67), (23,207,181),
        (76,136,255), (80,195,240), (95,104,233), (140,86,236), (214,97,214), (117,117,117)
    };
    internal static readonly string[] Presets = Rgb.Select(c => $"#{c.R:X2}{c.G:X2}{c.B:X2}").ToArray();
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture) =>
        parameter as string == "preset" ? (Presets.Contains(value?.ToString(), StringComparer.Ordinal) ? Visibility.Visible : Visibility.Collapsed)
        : NativeUi.ColorBrush(value?.ToString() ?? "");
    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) => throw new NotSupportedException();
    internal static void Apply(ComboBox control) {
        control.ItemTemplate = (DataTemplate)XamlReader.Parse("""
            <DataTemplate xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml" xmlns:local="clr-namespace:PaykiTodo.Desktop;assembly=PaykiTodo">
              <DataTemplate.Resources><local:NativeColorChoice x:Key="ColorView"/></DataTemplate.Resources>
              <Border Width="28" Height="28" CornerRadius="6" Background="{Binding Converter={StaticResource ColorView}}" VerticalAlignment="Center">
                <Path x:Name="check" Data="M 7,14 L 12,19 L 22,9" Stroke="White" StrokeThickness="2.5" StrokeStartLineCap="Round" StrokeEndLineCap="Round" StrokeLineJoin="Round" Visibility="Collapsed"/>
              </Border>
              <DataTemplate.Triggers><DataTrigger Binding="{Binding IsSelected, RelativeSource={RelativeSource AncestorType=ComboBoxItem}}" Value="True"><Setter TargetName="check" Property="Visibility" Value="Visible"/></DataTrigger></DataTemplate.Triggers>
            </DataTemplate>
            """);
        control.ItemsPanel = (ItemsPanelTemplate)XamlReader.Parse("""
            <ItemsPanelTemplate xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"><UniformGrid Columns="6" Width="264"/></ItemsPanelTemplate>
            """);
        control.ItemContainerStyle = (Style)XamlReader.Parse("""
            <Style xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml" xmlns:local="clr-namespace:PaykiTodo.Desktop;assembly=PaykiTodo" TargetType="ComboBoxItem">
              <Style.Resources><local:NativeColorChoice x:Key="ColorView"/></Style.Resources>
              <Setter Property="Visibility" Value="{Binding Converter={StaticResource ColorView}, ConverterParameter=preset}"/>
              <Setter Property="AutomationProperties.Name" Value="{Binding}"/>
              <Setter Property="Template"><Setter.Value><ControlTemplate TargetType="ComboBoxItem">
                <Border x:Name="focus" Padding="6" Margin="1" Background="Transparent" CornerRadius="8" BorderThickness="1" BorderBrush="Transparent"><ContentPresenter HorizontalAlignment="Center" VerticalAlignment="Center"/></Border>
                <ControlTemplate.Triggers><Trigger Property="IsHighlighted" Value="True"><Setter TargetName="focus" Property="BorderBrush" Value="{DynamicResource PaykiTodo.FocusBrush}"/></Trigger></ControlTemplate.Triggers>
              </ControlTemplate></Setter.Value></Setter>
            </Style>
            """);
        System.Windows.Automation.AutomationProperties.SetName(control, "事项颜色");
    }
    internal static void ApplyLegend(ComboBox control, string colorProperty = "ColorHex") {
        control.DisplayMemberPath = "";
        control.ItemTemplate = (DataTemplate)XamlReader.Parse($$$"""
            <DataTemplate xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml" xmlns:local="clr-namespace:PaykiTodo.Desktop;assembly=PaykiTodo">
              <DataTemplate.Resources><local:NativeColorChoice x:Key="ColorView"/></DataTemplate.Resources>
              <StackPanel Orientation="Horizontal">
                <Border Width="14" Height="14" CornerRadius="4" Background="{Binding {{{colorProperty}}}, Converter={StaticResource ColorView}}" Margin="0,0,8,0" VerticalAlignment="Center"/>
                <TextBlock Text="{Binding Name}" VerticalAlignment="Center"/>
              </StackPanel>
            </DataTemplate>
            """);
        TextSearch.SetTextPath(control, "Name");
    }
}
