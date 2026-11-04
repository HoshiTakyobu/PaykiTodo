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

internal static partial class NativeUi
{
    private const string StyleKey = "PaykiTodo.ControlStyles";
    public static FontFamily BundledFont { get; } = new(new Uri("pack://application:,,,/PaykiTodo;component/"), "./Assets/#Sarasa UI SC");
    public static FontFamily InterfaceFont { get; private set; } = BundledFont;
    public static double InterfaceFontSize { get; private set; } = DesktopFonts.DefaultSize;
    public static double TextSize(double size) => size * InterfaceFontSize / DesktopFonts.DefaultSize;
    public static void SetInterfaceFontSize(double size) => InterfaceFontSize = DesktopFonts.ValidSize(size);
    public static FontWeight EmphasisWeight { get; private set; } = FontWeights.Bold;
    public static FontFamily UtilityFont => InterfaceFont;
    public static void SetInterfaceFont(string? name)
    {
        InterfaceFont = (DesktopFonts.Find(name) ?? DesktopFonts.Choices[0]).Family;
        // Never manufacture a heavy outline for a font that ships only one face.
        EmphasisWeight = InterfaceFont.GetTypefaces()
            .Where(face => face.Style == FontStyles.Normal && face.Weight.ToOpenTypeWeight() >= 600 &&
                face.TryGetGlyphTypeface(out var glyph) && glyph.StyleSimulations == StyleSimulations.None)
            .OrderBy(face => face.Weight.ToOpenTypeWeight()).Select(face => (FontWeight?)face.Weight).FirstOrDefault() ?? FontWeights.Normal;
    }
    public static FontFamily HeadingFont { get; } = new(new Uri("pack://application:,,,/PaykiTodo;component/"), "./Assets/#FZQingKeBenYueSongS-R-GB");
    private static readonly Lazy<ResourceDictionary> FixedArtwork = new(() => new ResourceDictionary {
        Source = new Uri("pack://application:,,,/PaykiTodo;component/Assets/BrandArtwork.xaml")
    });
    public static System.Windows.Shapes.Path FixedLabel(string key, Brush color) => new() {
        Data = (Geometry)FixedArtwork.Value[key], Fill = color, Stretch = Stretch.None,
        VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(6, 0, 0, 0)
    };
    private static readonly Lazy<ImageSource> IconSource = new(() => {
        using var stream = System.Windows.Application.GetResourceStream(new Uri("pack://application:,,,/PaykiTodo;component/Assets/PaykiTodo.ico")).Stream;
        var decoder = BitmapDecoder.Create(stream, BitmapCreateOptions.PreservePixelFormat, BitmapCacheOption.OnLoad);
        var frame = decoder.Frames.OrderByDescending(value => value.PixelWidth).First();
        frame.Freeze(); return frame;
    });
    public static ImageSource AppIcon => IconSource.Value;
    public static bool Dark { get; set; }
    public static Brush Ink => B(Dark ? "#EDF1F5" : "#253448");
    public static Brush Muted => B(Dark ? "#BECAD3" : "#596B80");
    public static Brush Outline => B(Dark ? "#5E7180" : "#B8C6D5");
    public static Brush Ocean => B(Dark ? "#94BAFF" : "#326DD6");
    public static Brush Forest => B(Dark ? "#93C6AC" : "#326F59");
    public static Brush Paper => B(Dark ? "#22313E" : "#FFFFFF");
    public static Brush B(string hex)
    {
        if (Dark) hex = hex.ToUpperInvariant() switch {
            "#EEF3F4" or "#F1F5F3" => "#15232F", "#F8FAF9" => "#1B2935", "#DCEBE2" => "#354453",
            "#EAF0ED" or "#EDF3FB" => "#30404E", "#FFFAE9" => "#4C452E", "#F6FBF8" or "#F7FAFF" or "#F5F7FA" => "#1D2B38", "#E3EBE6" or "#CBD8D2" or "#E1E7F0" => "#445463", "#E2EFE6" or "#E8F0FF" => "#344553", _ => hex };
        return new SolidColorBrush((Color)System.Windows.Media.ColorConverter.ConvertFromString(hex));
    }
    public static TextBlock Text(string value, double size = 14, Brush? color = null, bool bold = false) => new()
    {
        Text = value, FontFamily = InterfaceFont,
        FontSize = TextSize(size), Foreground = color ?? Ink, TextWrapping = TextWrapping.Wrap,
        LineHeight = double.NaN, LineStackingStrategy = LineStackingStrategy.MaxHeight,
        FontWeight = bold ? EmphasisWeight : FontWeights.Normal,
        Margin = new Thickness(0, 1, 0, 3)
    };
    public static StackPanel Column(params UIElement[] elements)
    {
        var panel = new StackPanel(); foreach (var element in elements) panel.Children.Add(element); return panel;
    }
    public static Brush ColorBrush(string hex) {
        try { return new SolidColorBrush((Color)System.Windows.Media.ColorConverter.ConvertFromString(hex)); }
        catch (Exception error) when (error is FormatException or ArgumentException or NotSupportedException) { return B("#3370FF"); }
    }
    public static Brush AccentSurface(string hex) => Mix(ColorBrush(hex), Paper, Dark ? 0.80 : 0.90);
    public static Brush AccentInk(string hex)
    {
        var color = ColorBrush(hex); var surface = AccentSurface(hex);
        for (var i = 0; i < 14 && Contrast(color, surface) < 4.5; i++) color = Mix(color, Ink, 0.18);
        return color;
    }
    public static Brush EventSurface(string hex, bool classic, bool ended = false) {
        var fill = classic ? Mix(ColorBrush(hex), Paper, 0) : AccentSurface(hex);
        if (ended) fill = Mix(fill, Paper, 0.25);
        // Retain white-on-blue classic cards by a small display-only shade adjustment.
        // Very pale fills keep their hue and use dark text instead.
        if (classic && Contrast(B("#FFFFFF"), fill) is >= 3 and < 4.5)
            for (var i = 0; i < 12 && Contrast(B("#FFFFFF"), fill) < 4.5; i++) fill = Mix(fill, B("#000000"), 0.05);
        return fill;
    }
    public static Brush EventInk(string hex, bool classic, bool ended = false) {
        var fill = EventSurface(hex, classic, ended);
        var ink = classic ? B("#FFFFFF") : AccentInk(hex);
        if (classic && Contrast(ink, fill) < 4.5) ink = B("#17212B");
        for (var i = 0; i < 16 && Contrast(ink, fill) < 4.5; i++) ink = Mix(ink, Dark && !classic ? B("#FFFFFF") : B("#000000"), 0.18);
        return ink;
    }
    public static double LineHeight(double size) {
        var sample = Text("中文细笔画 Ag 14:00–17:00", size); sample.Margin = new Thickness(0);
        sample.Measure(new System.Windows.Size(double.PositiveInfinity, double.PositiveInfinity));
        return Math.Ceiling(Math.Max(sample.DesiredSize.Height, sample.FontSize * sample.FontFamily.LineSpacing)) + 1;
    }
    public static TextBlock LimitedTitle(string value, double size, Brush ink, int lines = 3) {
        var title = Text(value, size, ink); title.Margin = new Thickness(0);
        title.LineHeight = LineHeight(size); title.LineStackingStrategy = LineStackingStrategy.BlockLineHeight;
        title.MaxHeight = title.LineHeight * lines; title.TextTrimming = TextTrimming.CharacterEllipsis;
        return title;
    }
    private static Brush Mix(Brush first, Brush second, double amount)
    {
        var a = ((SolidColorBrush)first).Color; var b = ((SolidColorBrush)second).Color;
        return new SolidColorBrush(Color.FromRgb((byte)(a.R * (1 - amount) + b.R * amount),
            (byte)(a.G * (1 - amount) + b.G * amount), (byte)(a.B * (1 - amount) + b.B * amount)));
    }
    private static double Contrast(Brush a, Brush b)
    {
        double L(Brush brush) {
            var c = ((SolidColorBrush)brush).Color;
            double V(byte value) { var s = value / 255d; return s <= 0.04045 ? s / 12.92 : Math.Pow((s + 0.055) / 1.055, 2.4); }
            return 0.2126 * V(c.R) + 0.7152 * V(c.G) + 0.0722 * V(c.B);
        }
        var x = L(a); var y = L(b); return (Math.Max(x, y) + 0.05) / (Math.Min(x, y) + 0.05);
    }
    public static UIElement AlarmIcon()
    {
        var icon = new Canvas { Width = 20, Height = 22, Margin = new Thickness(0, 0, 6, 0), VerticalAlignment = VerticalAlignment.Center };
        icon.Children.Add(new System.Windows.Shapes.Path { Data = Geometry.Parse("M 4,2 L 1,5 M 16,2 L 19,5 M 5,18 L 3,21 M 15,18 L 17,21"), Stroke = B(Dark ? "#FFA8A0" : "#C23F49"), StrokeThickness = 2.5 });
        var face = new System.Windows.Shapes.Ellipse { Width = 15, Height = 15, Stroke = B(Dark ? "#FFA8A0" : "#C23F49"), StrokeThickness = 1.8, Fill = Paper };
        Canvas.SetLeft(face, 2.5); Canvas.SetTop(face, 4); icon.Children.Add(face);
        icon.Children.Add(new System.Windows.Shapes.Path { Data = Geometry.Parse("M 10,7 L 10,12 L 13,14"), Stroke = Ink, StrokeThickness = 1.5 });
        return icon;
    }
    public static CheckBox TaskToggle(bool completed, bool enabled)
    {
        var toggle = new CheckBox { IsChecked = completed, IsEnabled = enabled, VerticalAlignment = VerticalAlignment.Center,
            Margin = new Thickness(0), Background = Paper, BorderBrush = Muted, Foreground = Forest };
        toggle.Template = (ControlTemplate)XamlReader.Parse("""
            <ControlTemplate xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" TargetType="CheckBox">
              <Grid Width="30" Height="36" Background="Transparent">
                <Ellipse Width="26" Height="26" Stroke="{TemplateBinding BorderBrush}" StrokeThickness="2" Fill="{TemplateBinding Background}"/>
                <Path x:Name="check" xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml" Data="M 7,18 L 12,23 L 23,12" Stroke="{TemplateBinding Foreground}" StrokeThickness="2.5" Visibility="Collapsed"/>
              </Grid>
              <ControlTemplate.Triggers><Trigger Property="IsChecked" Value="True"><Setter TargetName="check" Property="Visibility" Value="Visible"/></Trigger>
                <Trigger Property="IsKeyboardFocused" Value="True"><Setter Property="BorderBrush" Value="{DynamicResource PaykiTodo.FocusBrush}"/></Trigger>
                <Trigger Property="IsEnabled" Value="False"><Setter Property="Opacity" Value="0.5"/></Trigger>
              </ControlTemplate.Triggers>
            </ControlTemplate>
            """);
        return toggle;
    }
    public static WrapPanel Row(params UIElement[] elements)
    {
        var panel = new WrapPanel(); foreach (var element in elements) panel.Children.Add(element); return panel;
    }
    public static Border Card(UIElement child, double padding = 20) => new()
    {
        Background = Paper, CornerRadius = new CornerRadius(22), Padding = new Thickness(padding),
        BorderBrush = B("#E1E7F0"), BorderThickness = new Thickness(1),
        Margin = new Thickness(0, 0, 0, 16), Child = child
    };
    public static Button Action(string label, Action click, bool primary = false)
    {
        var button = new Button { Content = label, FontFamily = InterfaceFont, FontSize = TextSize(14), MinHeight = 40,
            HorizontalContentAlignment = HorizontalAlignment.Center, VerticalContentAlignment = VerticalAlignment.Center,
            FontWeight = primary ? EmphasisWeight : FontWeights.Normal, Padding = new Thickness(16, 9, 16, 9),
            Margin = new Thickness(0, 0, 8, 6), Background = primary ? B("#326F59") : B("#EAF0ED"),
            Foreground = primary ? B("#FFFFFF") : Ink, BorderBrush = primary ? Forest : Outline, BorderThickness = new Thickness(1), Cursor = System.Windows.Input.Cursors.Hand };
        AutomationProperties.SetName(button, label);
        button.Click += (_, _) => click(); return button;
    }
    public static Button NavigationAction(string glyph, string label, Action click)
    {
        var button = Action(label, click);
        var row = new Grid(); row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(30) }); row.ColumnDefinitions.Add(new ColumnDefinition());
        row.Children.Add(new TextBlock { Text = glyph, FontFamily = new FontFamily("Segoe MDL2 Assets"), FontSize = 18, VerticalAlignment = VerticalAlignment.Center, Foreground = Forest });
        var text = FixedLabel(label, Ink);
        text.Margin = new Thickness(6, 0, 0, 0); text.VerticalAlignment = VerticalAlignment.Center; Grid.SetColumn(text, 1); row.Children.Add(text);
        button.Content = row; button.HorizontalContentAlignment = HorizontalAlignment.Stretch;
        button.Padding = new Thickness(16, 12, 12, 12); button.Margin = new Thickness(12, 3, 12, 3);
        button.BorderThickness = new Thickness(0); button.BorderBrush = Forest;
        AutomationProperties.SetAutomationId(button, "Navigate." + label); return button;
    }
    public static Button ChecklistAction(Action click)
    {
        var button = Action("快速清单", click);
        var icon = new Canvas { Width = 20, Height = 20, Margin = new Thickness(0, 0, 8, 0) };
        icon.Children.Add(new System.Windows.Shapes.Path { Data = Geometry.Parse("M 3,1 L 17,1 L 17,19 L 3,19 Z M 9,6 L 14,6 M 9,10 L 14,10 M 9,14 L 14,14"), Stroke = Ink, StrokeThickness = 1.4 });
        icon.Children.Add(new System.Windows.Shapes.Path { Data = Geometry.Parse("M 5,6 L 6,7 L 8,4 M 5,10 L 6,11 L 8,8 M 5,14 L 6,15 L 8,12"), Stroke = Ocean, StrokeThickness = 1.5 });
        var content = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };
        content.Children.Add(icon); content.Children.Add(Text("快速清单", 14)); button.Content = content;
        return button;
    }
    public static UIElement BrandHeading(string title, string? subtitle = null)
    {
        var row = new Grid { Margin = new Thickness(0, 0, 0, 20) };
        row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(48) }); row.ColumnDefinitions.Add(new ColumnDefinition());
        row.Children.Add(new System.Windows.Controls.Image { Source = AppIcon, Width = 32, Height = 32, HorizontalAlignment = HorizontalAlignment.Left, VerticalAlignment = VerticalAlignment.Center });
        FrameworkElement heading = title == "PaykiTodo"
            ? new System.Windows.Controls.Image { Source = new DrawingImage(new GeometryDrawing(Ink, null, (Geometry)FixedArtwork.Value["Brand"])),
                Width = 126, Height = 30, Stretch = Stretch.Uniform, HorizontalAlignment = HorizontalAlignment.Left }
            : Text(title, 22, bold: true);
        AutomationProperties.SetName(heading, title);
        var labels = Column(heading); if (!string.IsNullOrEmpty(subtitle)) labels.Children.Add(Text(subtitle, 13, Muted));
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
        Text = value, FontFamily = UtilityFont, FontSize = TextSize(14), Padding = new Thickness(12, 9, 12, 9), Margin = new Thickness(0, 4, 0, 16),
        VerticalContentAlignment = multiline ? VerticalAlignment.Top : VerticalAlignment.Center,
        MinHeight = multiline ? 110 : Math.Max(40, Math.Ceiling(TextSize(14) * Math.Max(1.4, UtilityFont.LineSpacing)) + 20), AcceptsReturn = multiline, TextWrapping = TextWrapping.Wrap,
        VerticalScrollBarVisibility = multiline ? ScrollBarVisibility.Auto : ScrollBarVisibility.Hidden,
        BorderBrush = Outline, BorderThickness = new Thickness(1), Background = Paper, Foreground = Ink
    };
    public static ScrollViewer Scroll(UIElement content) => new() { Content = content,
        VerticalScrollBarVisibility = ScrollBarVisibility.Auto, HorizontalScrollBarVisibility = ScrollBarVisibility.Disabled };
    public static Window Dialog(Window owner, string title, double width = 600, double height = 680)
    {
        var window = new Window {
        Owner = owner, Title = title, Icon = owner.Icon ?? AppIcon,
        Width = Math.Min(width, SystemParameters.WorkArea.Width * 0.95), Height = Math.Min(height, SystemParameters.WorkArea.Height * 0.95),
        MinWidth = Math.Min(420, width), MinHeight = Math.Min(320, height),
        WindowStartupLocation = WindowStartupLocation.CenterOwner, Background = B("#F1F5F3"),
        FontFamily = InterfaceFont, FontSize = TextSize(14), Foreground = Ink, UseLayoutRounding = true, SnapsToDevicePixels = true,
        ShowInTaskbar = false, Resources = owner.Resources
        };
        ConfigureRendering(window);
        return window;
    }
    public static void ConfigureRendering(DependencyObject target)
    {
        TextOptions.SetTextFormattingMode(target, TextFormattingMode.Display);
        TextOptions.SetTextRenderingMode(target, TextRenderingMode.Auto);
        TextOptions.SetTextHintingMode(target, TextHintingMode.Fixed);
    }
    public static void PositionNear(Window dialog, Window owner, System.Windows.Point screenPoint)
    {
        var transform = PresentationSource.FromVisual(owner)?.CompositionTarget?.TransformFromDevice ?? System.Windows.Media.Matrix.Identity;
        var screen = System.Windows.Forms.Screen.FromPoint(new System.Drawing.Point((int)screenPoint.X, (int)screenPoint.Y)).WorkingArea;
        var topLeft = transform.Transform(new System.Windows.Point(screen.Left, screen.Top));
        var bottomRight = transform.Transform(new System.Windows.Point(screen.Right, screen.Bottom));
        var point = transform.Transform(screenPoint);
        dialog.WindowStartupLocation = WindowStartupLocation.Manual;
        dialog.Left = Math.Clamp(point.X + 10, topLeft.X + 8, Math.Max(topLeft.X + 8, bottomRight.X - dialog.Width - 8));
        dialog.Top = Math.Clamp(point.Y - 24, topLeft.Y + 8, Math.Max(topLeft.Y + 8, bottomRight.Y - dialog.Height - 8));
    }
    public static void InstallStyles(ResourceDictionary resources)
    {
        if (resources[StyleKey] is ResourceDictionary previous) resources.MergedDictionaries.Remove(previous);
        resources[SystemColors.WindowBrushKey] = Paper;
        resources[SystemColors.WindowTextBrushKey] = Ink;
        resources[SystemColors.ControlBrushKey] = Paper;
        resources[SystemColors.ControlTextBrushKey] = Ink;
        resources[SystemColors.HighlightBrushKey] = Forest;
        resources[SystemColors.HighlightTextBrushKey] = B("#FFFFFF");
        resources["PaykiTodo.FocusBrush"] = Forest;
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
                    <Trigger Property="IsKeyboardFocused" Value="True"><Setter TargetName="edge" Property="BorderBrush" Value="{DynamicResource PaykiTodo.FocusBrush}"/></Trigger>
                  </ControlTemplate.Triggers>
                </ControlTemplate></Setter.Value></Setter>
              </Style>
              <Style TargetType="TextBox">
                <Setter Property="Template"><Setter.Value><ControlTemplate TargetType="TextBox">
                  <Border x:Name="edge" CornerRadius="6" Background="{TemplateBinding Background}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="{TemplateBinding BorderThickness}">
                    <ScrollViewer x:Name="PART_ContentHost"/>
                  </Border>
                  <ControlTemplate.Triggers><Trigger Property="IsKeyboardFocusWithin" Value="True"><Setter TargetName="edge" Property="BorderBrush" Value="{DynamicResource PaykiTodo.FocusBrush}"/></Trigger><Trigger Property="IsEnabled" Value="False"><Setter Property="Opacity" Value="0.5"/></Trigger></ControlTemplate.Triggers>
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
                  <ControlTemplate.Triggers><Trigger Property="IsChecked" Value="True"><Setter TargetName="check" Property="Visibility" Value="Visible"/><Setter TargetName="box" Property="BorderBrush" Value="{DynamicResource PaykiTodo.FocusBrush}"/></Trigger><Trigger Property="IsKeyboardFocused" Value="True"><Setter TargetName="box" Property="BorderThickness" Value="2"/></Trigger><Trigger Property="IsEnabled" Value="False"><Setter Property="Opacity" Value="0.45"/></Trigger></ControlTemplate.Triggers>
                </ControlTemplate></Setter.Value></Setter>
              </Style>
              <Style TargetType="ComboBox">
                <Setter Property="Padding" Value="10,8"/><Setter Property="MinHeight" Value="40"/><Setter Property="Margin" Value="0,4,8,16"/>
                <Setter Property="Template"><Setter.Value><ControlTemplate TargetType="ComboBox">
                  <Grid>
                    <ToggleButton Focusable="False" IsChecked="{Binding IsDropDownOpen, RelativeSource={RelativeSource TemplatedParent}, Mode=TwoWay}">
                      <ToggleButton.Template><ControlTemplate TargetType="ToggleButton">
                        <Border Background="{Binding Background, RelativeSource={RelativeSource AncestorType=ComboBox}}" BorderBrush="{Binding BorderBrush, RelativeSource={RelativeSource AncestorType=ComboBox}}" BorderThickness="{Binding BorderThickness, RelativeSource={RelativeSource AncestorType=ComboBox}}" CornerRadius="6"/>
                      </ControlTemplate></ToggleButton.Template>
                    </ToggleButton>
                    <Grid IsHitTestVisible="False"><Grid.ColumnDefinitions><ColumnDefinition Width="*"/><ColumnDefinition Width="Auto"/></Grid.ColumnDefinitions>
                      <ContentPresenter x:Name="selection" Margin="{TemplateBinding Padding}" VerticalAlignment="Center" Content="{TemplateBinding SelectionBoxItem}" ContentTemplate="{TemplateBinding SelectionBoxItemTemplate}" ContentTemplateSelector="{TemplateBinding ItemTemplateSelector}" ContentStringFormat="{TemplateBinding SelectionBoxItemStringFormat}"/>
                      <Path x:Name="arrow" Grid.Column="1" Data="M 1,1 L 5,5 L 9,1" Width="10" Height="6" Margin="2,0,8,0" VerticalAlignment="Center" Stroke="{TemplateBinding Foreground}" StrokeThickness="1.5" StrokeStartLineCap="Round" StrokeEndLineCap="Round" StrokeLineJoin="Round"/>
                    </Grid>
                    <TextBox x:Name="PART_EditableTextBox" Visibility="Collapsed" Margin="10,6,42,6" Background="{TemplateBinding Background}" Foreground="{TemplateBinding Foreground}" BorderThickness="0"/>
                    <Popup x:Name="PART_Popup" Placement="Bottom" IsOpen="{TemplateBinding IsDropDownOpen}" AllowsTransparency="True" Focusable="False">
                      <Border Background="{TemplateBinding Background}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="1" CornerRadius="6" MinWidth="{Binding ActualWidth, RelativeSource={RelativeSource TemplatedParent}}" MaxHeight="360"><ScrollViewer><ItemsPresenter KeyboardNavigation.DirectionalNavigation="Contained"/></ScrollViewer></Border>
                    </Popup>
                  </Grid><ControlTemplate.Triggers><Trigger Property="IsEditable" Value="True"><Setter TargetName="selection" Property="Visibility" Value="Collapsed"/><Setter TargetName="PART_EditableTextBox" Property="Visibility" Value="Visible"/></Trigger><Trigger Property="IsEnabled" Value="False"><Setter Property="Opacity" Value="0.45"/></Trigger></ControlTemplate.Triggers>
                </ControlTemplate></Setter.Value></Setter>
              </Style>
              <Style TargetType="p:DatePickerTextBox" BasedOn="{StaticResource {x:Type TextBox}}"><Setter Property="Padding" Value="4,4"/><Setter Property="VerticalContentAlignment" Value="Center"/><Setter Property="BorderThickness" Value="0"/></Style>
              <Style TargetType="DatePicker">
                <Setter Property="MinHeight" Value="40"/><Setter Property="Margin" Value="0,4,8,16"/>
                <Setter Property="Template"><Setter.Value><ControlTemplate TargetType="DatePicker">
                  <Border x:Name="edge" CornerRadius="6" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="{TemplateBinding BorderThickness}" Background="{TemplateBinding Background}">
                    <Grid><Grid.ColumnDefinitions><ColumnDefinition Width="*"/><ColumnDefinition Width="34"/></Grid.ColumnDefinitions>
                      <p:DatePickerTextBox x:Name="PART_TextBox" Background="{TemplateBinding Background}" Foreground="{TemplateBinding Foreground}" FontFamily="{TemplateBinding FontFamily}" FontSize="{TemplateBinding FontSize}"/>
                      <Button x:Name="PART_Button" Grid.Column="1" Background="Transparent" BorderThickness="0" Margin="2" Padding="4" Focusable="False"><TextBlock FontFamily="Segoe MDL2 Assets" Text="&#xE787;" Foreground="{TemplateBinding Foreground}" FontSize="16"/></Button>
                      <Popup x:Name="PART_Popup" Placement="Bottom" StaysOpen="False" AllowsTransparency="True"/>
                    </Grid>
                  </Border>
                  <ControlTemplate.Triggers><Trigger Property="IsEnabled" Value="False"><Setter Property="Opacity" Value="0.5"/></Trigger><Trigger Property="IsKeyboardFocusWithin" Value="True"><Setter TargetName="edge" Property="BorderBrush" Value="#609C81"/></Trigger></ControlTemplate.Triggers>
                </ControlTemplate></Setter.Value></Setter>
              </Style>
              <Style TargetType="Expander">
                <Setter Property="Template"><Setter.Value><ControlTemplate TargetType="Expander">
                  <StackPanel><ToggleButton x:Name="toggle" HorizontalContentAlignment="Left" IsChecked="{Binding IsExpanded, RelativeSource={RelativeSource TemplatedParent}, Mode=TwoWay}">
                    <ToggleButton.Template><ControlTemplate TargetType="ToggleButton">
                      <Border Background="Transparent" Padding="0,10"><StackPanel Orientation="Horizontal">
                        <Path x:Name="chevron" Data="M 1,1 L 5,5 L 9,1" Width="10" Height="6" Stroke="{DynamicResource {x:Static SystemColors.ControlTextBrushKey}}" StrokeThickness="1.5" StrokeStartLineCap="Round" StrokeEndLineCap="Round" StrokeLineJoin="Round" Margin="2,0,10,0" RenderTransformOrigin="0.5,0.5" VerticalAlignment="Center"><Path.RenderTransform><RotateTransform Angle="-90"/></Path.RenderTransform></Path>
                        <ContentPresenter Content="{Binding Header, RelativeSource={RelativeSource AncestorType=Expander}}" VerticalAlignment="Center"/>
                      </StackPanel></Border>
                      <ControlTemplate.Triggers><Trigger Property="IsChecked" Value="True"><Setter TargetName="chevron" Property="RenderTransform"><Setter.Value><RotateTransform Angle="0"/></Setter.Value></Setter></Trigger><Trigger Property="IsKeyboardFocused" Value="True"><Setter Property="Foreground" Value="{DynamicResource PaykiTodo.FocusBrush}"/></Trigger></ControlTemplate.Triggers>
                    </ControlTemplate></ToggleButton.Template>
                  </ToggleButton><ContentPresenter x:Name="body" Visibility="Collapsed"/></StackPanel>
                  <ControlTemplate.Triggers><Trigger Property="IsExpanded" Value="True"><Setter TargetName="body" Property="Visibility" Value="Visible"/></Trigger></ControlTemplate.Triggers>
                </ControlTemplate></Setter.Value></Setter>
              </Style>
              <Style TargetType="PasswordBox">
                <Setter Property="MinHeight" Value="42"/><Setter Property="Padding" Value="12,9"/><Setter Property="Margin" Value="0,4,0,16"/><Setter Property="VerticalContentAlignment" Value="Center"/>
                <Setter Property="Template"><Setter.Value><ControlTemplate TargetType="PasswordBox">
                  <Border x:Name="edge" CornerRadius="6" Background="{TemplateBinding Background}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="1"><ScrollViewer x:Name="PART_ContentHost"/></Border>
                  <ControlTemplate.Triggers><Trigger Property="IsKeyboardFocusWithin" Value="True"><Setter TargetName="edge" Property="BorderBrush" Value="#609C81"/></Trigger><Trigger Property="IsEnabled" Value="False"><Setter Property="Opacity" Value="0.5"/></Trigger></ControlTemplate.Triggers>
                </ControlTemplate></Setter.Value></Setter>
              </Style>
              <Style TargetType="ComboBoxItem"><Setter Property="Padding" Value="10,8"/><Setter Property="Template"><Setter.Value><ControlTemplate TargetType="ComboBoxItem"><Border x:Name="row" Padding="{TemplateBinding Padding}" Background="{TemplateBinding Background}" BorderThickness="2,0,0,0" BorderBrush="Transparent"><ContentPresenter Content="{TemplateBinding Content}" ContentTemplate="{TemplateBinding ContentTemplate}" ContentTemplateSelector="{TemplateBinding ContentTemplateSelector}" ContentStringFormat="{TemplateBinding ContentStringFormat}"/></Border><ControlTemplate.Triggers><Trigger Property="IsHighlighted" Value="True"><Setter TargetName="row" Property="BorderBrush" Value="{DynamicResource PaykiTodo.FocusBrush}"/></Trigger><Trigger Property="IsSelected" Value="True"><Setter TargetName="row" Property="BorderBrush" Value="{DynamicResource PaykiTodo.FocusBrush}"/></Trigger></ControlTemplate.Triggers></ControlTemplate></Setter.Value></Setter></Style>
            </ResourceDictionary>
            """);
        foreach (var type in new[] { typeof(TextBox), typeof(CheckBox), typeof(ComboBox), typeof(DatePicker), typeof(PasswordBox), typeof(ComboBoxItem) }) {
            var style = new Style(type, styles[type] as Style);
            style.Setters.Add(new Setter(Control.ForegroundProperty, Ink));
            style.Setters.Add(new Setter(Control.BackgroundProperty, Paper));
            style.Setters.Add(new Setter(Control.BorderBrushProperty, Outline));
            style.Setters.Add(new Setter(Control.FontFamilyProperty, type == typeof(TextBox) || type == typeof(PasswordBox) || type == typeof(DatePicker) ? UtilityFont : InterfaceFont));
            style.Setters.Add(new Setter(Control.FontWeightProperty, FontWeights.Normal));
            style.Setters.Add(new Setter(Control.FontSizeProperty, TextSize(14)));
            styles[type] = style;
        }
        resources[StyleKey] = styles; resources.MergedDictionaries.Add(styles);
    }
}
