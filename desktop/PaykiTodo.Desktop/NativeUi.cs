using System.Windows;
using System.Windows.Controls;
using System.Windows.Markup;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Automation;
using Button = System.Windows.Controls.Button;
using TextBox = System.Windows.Controls.TextBox;
using Brush = System.Windows.Media.Brush;
using Color = System.Windows.Media.Color;
using Orientation = System.Windows.Controls.Orientation;
using SystemColors = System.Windows.SystemColors;
using CheckBox = System.Windows.Controls.CheckBox;
using ComboBox = System.Windows.Controls.ComboBox;
using Control = System.Windows.Controls.Control;
using FontFamily = System.Windows.Media.FontFamily;
using HorizontalAlignment = System.Windows.HorizontalAlignment;

namespace PaykiTodo.Desktop;

internal static class NativeUi
{
    private const string StyleKey = "PaykiTodo.ControlStyles";
    public static FontFamily InterfaceFont { get; } = new("Microsoft YaHei UI, Segoe UI");
    private static readonly Lazy<ImageSource> IconSource = new(() => {
        using var stream = System.Windows.Application.GetResourceStream(new Uri("pack://application:,,,/PaykiTodo;component/Assets/PaykiTodo.ico")).Stream;
        var decoder = BitmapDecoder.Create(stream, BitmapCreateOptions.PreservePixelFormat, BitmapCacheOption.OnLoad);
        var frame = decoder.Frames.OrderByDescending(value => value.PixelWidth).First();
        frame.Freeze(); return frame;
    });
    public static ImageSource AppIcon => IconSource.Value;
    public static bool Dark { get; set; }
    public static Brush Ink => B(Dark ? "#E8EDF3" : "#243746");
    public static Brush Muted => B(Dark ? "#AFBDCA" : "#637482");
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
        Text = value, FontFamily = InterfaceFont, FontSize = size, Foreground = color ?? Ink, TextWrapping = TextWrapping.Wrap,
        LineHeight = Math.Ceiling(size * 1.5), LineStackingStrategy = LineStackingStrategy.BlockLineHeight,
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
        Background = Paper, CornerRadius = new CornerRadius(12), Padding = new Thickness(padding),
        BorderBrush = B(Dark ? "#334452" : "#E0E7EC"), BorderThickness = new Thickness(1),
        Margin = new Thickness(0, 0, 0, 16), Child = child
    };
    public static Button Action(string label, Action click, bool primary = false)
    {
        var button = new Button { Content = label, FontFamily = InterfaceFont, FontSize = 14, MinHeight = 40,
            HorizontalContentAlignment = HorizontalAlignment.Center, VerticalContentAlignment = VerticalAlignment.Center,
            FontWeight = primary ? FontWeights.SemiBold : FontWeights.Normal, Padding = new Thickness(16, 9, 16, 9),
            Margin = new Thickness(0, 0, 8, 6), Background = primary ? B("#326F59") : B("#EAF0ED"),
            Foreground = primary ? B("#FFFFFF") : Ink, BorderBrush = primary ? Forest : B("#CBD8D2"), BorderThickness = new Thickness(1), Cursor = System.Windows.Input.Cursors.Hand };
        AutomationProperties.SetName(button, label);
        button.Click += (_, _) => click(); return button;
    }
    public static Button NavigationAction(string glyph, string label, Action click)
    {
        var button = Action(label, click);
        var row = new Grid(); row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(30) }); row.ColumnDefinitions.Add(new ColumnDefinition());
        row.Children.Add(new TextBlock { Text = glyph, FontFamily = new FontFamily("Segoe MDL2 Assets"), FontSize = 18, VerticalAlignment = VerticalAlignment.Center, Foreground = Forest });
        var text = Text(label); text.Margin = new Thickness(6, 0, 0, 0); text.VerticalAlignment = VerticalAlignment.Center; Grid.SetColumn(text, 1); row.Children.Add(text);
        button.Content = row; button.HorizontalContentAlignment = HorizontalAlignment.Stretch;
        button.Padding = new Thickness(16, 12, 12, 12); button.Margin = new Thickness(12, 3, 12, 3);
        button.BorderThickness = new Thickness(0); button.BorderBrush = Forest;
        AutomationProperties.SetAutomationId(button, "Navigate." + label); return button;
    }
    public static UIElement BrandHeading(string title, string? subtitle = null)
    {
        var row = new Grid { Margin = new Thickness(0, 0, 0, 20) };
        row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(48) }); row.ColumnDefinitions.Add(new ColumnDefinition());
        row.Children.Add(new System.Windows.Controls.Image { Source = AppIcon, Width = 32, Height = 32, HorizontalAlignment = HorizontalAlignment.Left, VerticalAlignment = VerticalAlignment.Center });
        var labels = Column(Text(title, 22, bold: true)); if (!string.IsNullOrEmpty(subtitle)) labels.Children.Add(Text(subtitle, 12, Muted));
        Grid.SetColumn(labels, 1); row.Children.Add(labels); return row;
    }
    public static UIElement DialogLayout(UIElement body, params UIElement[] actions)
        => DialogLayout(body, HorizontalAlignment.Right, actions);
    public static UIElement DialogLayout(UIElement body, HorizontalAlignment alignment, params UIElement[] actions)
    {
        var layout = new DockPanel { Margin = new Thickness(24) };
        var footer = new Border { BorderBrush = B("#CBD8D2"), BorderThickness = new Thickness(0, 1, 0, 0), Padding = new Thickness(0, 18, 0, 0), Margin = new Thickness(0, 18, 0, 0) };
        var row = Row(actions); row.HorizontalAlignment = alignment; footer.Child = row;
        DockPanel.SetDock(footer, Dock.Bottom); layout.Children.Add(footer); layout.Children.Add(Scroll(body)); return layout;
    }
    public static bool Confirmation(Window owner, string text)
    {
        var dialog = Dialog(owner, "PaykiTodo · 操作确认", 520, 360);
        var cancel = Action("取消", () => dialog.Close()); cancel.IsCancel = true;
        var confirm = Action("确认操作", () => dialog.DialogResult = true, true);
        // Destructive/replacement operations require an explicit choice, not an accidental Enter.
        cancel.IsDefault = true;
        dialog.Content = DialogLayout(Column(BrandHeading("确认此操作", "请检查后再继续"), Text(text, 15)), cancel, confirm);
        return dialog.ShowDialog() == true;
    }
    public static TextBox Input(string value = "", bool multiline = false) => new()
    {
        Text = value, FontFamily = InterfaceFont, FontSize = 14, Padding = new Thickness(12, 9, 12, 9), Margin = new Thickness(0, 4, 0, 16),
        VerticalContentAlignment = multiline ? VerticalAlignment.Top : VerticalAlignment.Center,
        MinHeight = multiline ? 110 : 40, AcceptsReturn = multiline, TextWrapping = TextWrapping.Wrap,
        VerticalScrollBarVisibility = multiline ? ScrollBarVisibility.Auto : ScrollBarVisibility.Hidden,
        BorderBrush = B("#CBD8D2"), BorderThickness = new Thickness(1), Background = Paper, Foreground = Ink
    };
    public static ScrollViewer Scroll(UIElement content) => new() { Content = content,
        VerticalScrollBarVisibility = ScrollBarVisibility.Auto, HorizontalScrollBarVisibility = ScrollBarVisibility.Disabled };
    public static Window Dialog(Window owner, string title, double width = 600, double height = 680) => new()
    {
        Owner = owner, Title = title, Icon = owner.Icon ?? AppIcon,
        Width = Math.Min(width, SystemParameters.WorkArea.Width * 0.95), Height = Math.Min(height, SystemParameters.WorkArea.Height * 0.95),
        MinWidth = Math.Min(420, width), MinHeight = Math.Min(320, height),
        WindowStartupLocation = WindowStartupLocation.CenterOwner, Background = B("#F1F5F3"),
        FontFamily = InterfaceFont, FontSize = 14, Foreground = Ink, UseLayoutRounding = true, SnapsToDevicePixels = true,
        ShowInTaskbar = false, Resources = owner.Resources
    };
    public static void InstallStyles(ResourceDictionary resources)
    {
        if (resources[StyleKey] is ResourceDictionary previous) resources.MergedDictionaries.Remove(previous);
        resources[SystemColors.WindowBrushKey] = Paper;
        resources[SystemColors.WindowTextBrushKey] = Ink;
        resources[SystemColors.ControlBrushKey] = Paper;
        resources[SystemColors.ControlTextBrushKey] = Ink;
        resources[SystemColors.HighlightBrushKey] = Forest;
        resources[SystemColors.HighlightTextBrushKey] = B("#FFFFFF");
        var styles = (ResourceDictionary)XamlReader.Parse("""
            <ResourceDictionary xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml" xmlns:p="clr-namespace:System.Windows.Controls.Primitives;assembly=PresentationFramework">
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
                    <Trigger Property="IsPressed" Value="True"><Setter TargetName="edge" Property="Opacity" Value="0.65"/></Trigger>
                    <Trigger Property="IsKeyboardFocused" Value="True"><Setter TargetName="edge" Property="BorderBrush" Value="#93C6AC"/></Trigger>
                  </ControlTemplate.Triggers>
                </ControlTemplate></Setter.Value></Setter>
              </Style>
              <Style TargetType="TextBox">
                <Setter Property="Template"><Setter.Value><ControlTemplate TargetType="TextBox">
                  <Border x:Name="edge" CornerRadius="6" Background="{TemplateBinding Background}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="{TemplateBinding BorderThickness}">
                    <ScrollViewer x:Name="PART_ContentHost"/>
                  </Border>
                  <ControlTemplate.Triggers><Trigger Property="IsKeyboardFocusWithin" Value="True"><Setter TargetName="edge" Property="BorderBrush" Value="#609C81"/></Trigger><Trigger Property="IsEnabled" Value="False"><Setter Property="Opacity" Value="0.5"/></Trigger></ControlTemplate.Triggers>
                </ControlTemplate></Setter.Value></Setter>
              </Style>
              <Style TargetType="CheckBox">
                <Setter Property="Margin" Value="0,8,16,8"/><Setter Property="MinHeight" Value="28"/><Setter Property="VerticalContentAlignment" Value="Center"/>
                <Setter Property="Template"><Setter.Value><ControlTemplate TargetType="CheckBox">
                  <Grid Background="Transparent"><Grid.ColumnDefinitions><ColumnDefinition Width="Auto"/><ColumnDefinition Width="*"/></Grid.ColumnDefinitions>
                    <Border x:Name="box" Width="18" Height="18" CornerRadius="4" Background="{TemplateBinding Background}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="1" VerticalAlignment="Center">
                      <Path x:Name="check" Data="M 3,8 L 7,12 L 14,4" Stroke="{TemplateBinding Foreground}" StrokeThickness="2" Visibility="Collapsed"/>
                    </Border>
                    <ContentPresenter Grid.Column="1" Margin="8,0,0,0" VerticalAlignment="Center" RecognizesAccessKey="True"/>
                  </Grid>
                  <ControlTemplate.Triggers><Trigger Property="IsChecked" Value="True"><Setter TargetName="check" Property="Visibility" Value="Visible"/><Setter TargetName="box" Property="BorderBrush" Value="#609C81"/></Trigger><Trigger Property="IsKeyboardFocused" Value="True"><Setter TargetName="box" Property="BorderThickness" Value="2"/></Trigger><Trigger Property="IsEnabled" Value="False"><Setter Property="Opacity" Value="0.45"/></Trigger></ControlTemplate.Triggers>
                </ControlTemplate></Setter.Value></Setter>
              </Style>
              <Style TargetType="ComboBox">
                <Setter Property="Padding" Value="10,8"/><Setter Property="MinHeight" Value="40"/><Setter Property="Margin" Value="0,4,8,16"/>
                <Setter Property="Template"><Setter.Value><ControlTemplate TargetType="ComboBox">
                  <Grid>
                    <ToggleButton Focusable="False" IsChecked="{Binding IsDropDownOpen, RelativeSource={RelativeSource TemplatedParent}, Mode=TwoWay}">
                      <ToggleButton.Template><ControlTemplate TargetType="ToggleButton">
                        <Border Background="{Binding Background, RelativeSource={RelativeSource AncestorType=ComboBox}}" BorderBrush="{Binding BorderBrush, RelativeSource={RelativeSource AncestorType=ComboBox}}" BorderThickness="1" CornerRadius="6"><TextBlock Text="⌄" HorizontalAlignment="Right" VerticalAlignment="Center" Margin="0,0,12,0" Foreground="{Binding Foreground, RelativeSource={RelativeSource AncestorType=ComboBox}}"/></Border>
                      </ControlTemplate></ToggleButton.Template>
                    </ToggleButton>
                    <ContentPresenter x:Name="selection" Margin="10,8,32,8" VerticalAlignment="Center" IsHitTestVisible="False" Content="{TemplateBinding SelectionBoxItem}" ContentTemplate="{TemplateBinding SelectionBoxItemTemplate}"/>
                    <TextBox x:Name="PART_EditableTextBox" Visibility="Collapsed" Margin="10,6,32,6" Background="{TemplateBinding Background}" Foreground="{TemplateBinding Foreground}" BorderThickness="0"/>
                    <Popup x:Name="PART_Popup" Placement="Bottom" IsOpen="{TemplateBinding IsDropDownOpen}" AllowsTransparency="True" Focusable="False">
                      <Border Background="{TemplateBinding Background}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="1" CornerRadius="6" MinWidth="{Binding ActualWidth, RelativeSource={RelativeSource TemplatedParent}}" MaxHeight="360"><ScrollViewer><ItemsPresenter KeyboardNavigation.DirectionalNavigation="Contained"/></ScrollViewer></Border>
                    </Popup>
                  </Grid><ControlTemplate.Triggers><Trigger Property="IsEditable" Value="True"><Setter TargetName="selection" Property="Visibility" Value="Collapsed"/><Setter TargetName="PART_EditableTextBox" Property="Visibility" Value="Visible"/></Trigger><Trigger Property="IsEnabled" Value="False"><Setter Property="Opacity" Value="0.45"/></Trigger></ControlTemplate.Triggers>
                </ControlTemplate></Setter.Value></Setter>
              </Style>
              <Style TargetType="p:DatePickerTextBox" BasedOn="{StaticResource {x:Type TextBox}}"><Setter Property="Padding" Value="10,8"/><Setter Property="VerticalContentAlignment" Value="Center"/><Setter Property="BorderThickness" Value="0"/></Style>
              <Style TargetType="DatePicker">
                <Setter Property="MinHeight" Value="40"/><Setter Property="Margin" Value="0,4,8,16"/>
                <Setter Property="Template"><Setter.Value><ControlTemplate TargetType="DatePicker">
                  <Border x:Name="edge" CornerRadius="6" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="1" Background="{TemplateBinding Background}">
                    <Grid><Grid.ColumnDefinitions><ColumnDefinition Width="*"/><ColumnDefinition Width="34"/></Grid.ColumnDefinitions>
                      <p:DatePickerTextBox x:Name="PART_TextBox" Background="{TemplateBinding Background}" Foreground="{TemplateBinding Foreground}" FontFamily="{TemplateBinding FontFamily}" FontSize="{TemplateBinding FontSize}"/>
                      <Button x:Name="PART_Button" Grid.Column="1" Background="Transparent" BorderThickness="0" Margin="2" Padding="4" Focusable="False"><TextBlock FontFamily="Segoe MDL2 Assets" Text="&#xE787;" Foreground="{TemplateBinding Foreground}" FontSize="16"/></Button>
                      <Popup x:Name="PART_Popup" Placement="Bottom" StaysOpen="False" AllowsTransparency="True"/>
                    </Grid>
                  </Border>
                  <ControlTemplate.Triggers><Trigger Property="IsEnabled" Value="False"><Setter Property="Opacity" Value="0.5"/></Trigger><Trigger Property="IsKeyboardFocusWithin" Value="True"><Setter TargetName="edge" Property="BorderBrush" Value="#609C81"/></Trigger></ControlTemplate.Triggers>
                </ControlTemplate></Setter.Value></Setter>
              </Style>
              <Style TargetType="PasswordBox">
                <Setter Property="MinHeight" Value="42"/><Setter Property="Padding" Value="12,9"/><Setter Property="Margin" Value="0,4,0,16"/><Setter Property="VerticalContentAlignment" Value="Center"/>
                <Setter Property="Template"><Setter.Value><ControlTemplate TargetType="PasswordBox">
                  <Border x:Name="edge" CornerRadius="6" Background="{TemplateBinding Background}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="1"><ScrollViewer x:Name="PART_ContentHost"/></Border>
                  <ControlTemplate.Triggers><Trigger Property="IsKeyboardFocusWithin" Value="True"><Setter TargetName="edge" Property="BorderBrush" Value="#609C81"/></Trigger><Trigger Property="IsEnabled" Value="False"><Setter Property="Opacity" Value="0.5"/></Trigger></ControlTemplate.Triggers>
                </ControlTemplate></Setter.Value></Setter>
              </Style>
              <Style TargetType="ComboBoxItem"><Setter Property="Padding" Value="10,8"/><Setter Property="Template"><Setter.Value><ControlTemplate TargetType="ComboBoxItem"><Border x:Name="row" Padding="{TemplateBinding Padding}" Background="{TemplateBinding Background}"><ContentPresenter/></Border><ControlTemplate.Triggers><Trigger Property="IsHighlighted" Value="True"><Setter TargetName="row" Property="Opacity" Value="0.65"/></Trigger><Trigger Property="IsSelected" Value="True"><Setter Property="FontWeight" Value="SemiBold"/></Trigger></ControlTemplate.Triggers></ControlTemplate></Setter.Value></Setter></Style>
            </ResourceDictionary>
            """);
        foreach (var type in new[] { typeof(TextBox), typeof(CheckBox), typeof(ComboBox), typeof(DatePicker), typeof(PasswordBox), typeof(ComboBoxItem) }) {
            var style = new Style(type, styles[type] as Style);
            style.Setters.Add(new Setter(Control.ForegroundProperty, Ink));
            style.Setters.Add(new Setter(Control.BackgroundProperty, Paper));
            style.Setters.Add(new Setter(Control.BorderBrushProperty, B("#CBD8D2")));
            style.Setters.Add(new Setter(Control.FontFamilyProperty, InterfaceFont));
            style.Setters.Add(new Setter(Control.FontSizeProperty, 14d));
            styles[type] = style;
        }
        resources[StyleKey] = styles; resources.MergedDictionaries.Add(styles);
    }
}
