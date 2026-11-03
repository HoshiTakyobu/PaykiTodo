using System.Globalization;
using System.Windows.Data;
using System.Windows.Media;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Markup;

namespace PaykiTodo.Desktop;

// Values remain the original snapshot strings; the converter only changes presentation.
public sealed class NativeColorChoice : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture) =>
        parameter as string == "name" ? Name(value?.ToString() ?? "") : NativeUi.ColorBrush(value?.ToString() ?? "");
    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) => throw new NotSupportedException();
    private static string Name(string value) => value.ToUpperInvariant() switch {
        "#4CB782" => "草绿", "#3B82F6" => "蓝色", "#EF6A67" => "珊瑚红", "#E9A23B" => "橙色", "#8B5CF6" => "紫色", "#14B8A6" => "青色", _ => "自定义颜色"
    };
    internal static void Apply(System.Windows.Controls.ComboBox control) {
        control.ItemTemplate = (DataTemplate)XamlReader.Parse("""
            <DataTemplate xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml" xmlns:local="clr-namespace:PaykiTodo.Desktop;assembly=PaykiTodo">
              <DataTemplate.Resources><local:NativeColorChoice x:Key="ColorView"/></DataTemplate.Resources>
              <StackPanel Orientation="Horizontal" ToolTip="{Binding}">
                <Border Width="20" Height="20" CornerRadius="4" BorderThickness="1" BorderBrush="#778899" Background="{Binding Converter={StaticResource ColorView}}" Margin="0,0,8,0" VerticalAlignment="Center"/>
                <TextBlock Text="{Binding Converter={StaticResource ColorView}, ConverterParameter=name}" VerticalAlignment="Center"/>
              </StackPanel>
            </DataTemplate>
            """, new ParserContext());
        System.Windows.Automation.AutomationProperties.SetName(control, "事项颜色");
    }
}
