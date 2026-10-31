using System.Globalization;
using System.Windows.Markup;
using System.Windows.Media;
using FontFamily = System.Windows.Media.FontFamily;

namespace PaykiTodo.Desktop;

internal sealed record DesktopFontChoice(string Key, string DisplayName, FontFamily Family, IReadOnlyList<string> Aliases);

internal static class DesktopFonts
{
    public const double DefaultSize = 14;
    public const double MinimumSize = 12;
    public const double MaximumSize = 22;
    private static readonly Lazy<IReadOnlyList<DesktopFontChoice>> Installed = new(() =>
        Fonts.SystemFontFamilies.Select(font => new DesktopFontChoice(font.Source, LocalName(font), font,
            font.FamilyNames.Values.Append(font.Source).Distinct(StringComparer.OrdinalIgnoreCase).ToArray()))
        .OrderBy(font => font.DisplayName, StringComparer.Create(CultureInfo.GetCultureInfo("zh-CN"), true)).ToArray());

    public static IReadOnlyList<DesktopFontChoice> Choices => new[] {
        new DesktopFontChoice("", "系统推荐（微软雅黑 UI）", new FontFamily("Microsoft YaHei UI"), new[] { "系统推荐", "系统推荐（微软雅黑 UI）" }),
        new DesktopFontChoice("Sarasa UI SC", "更纱黑体 UI SC（内置）", NativeUi.BundledFont, new[] { "Sarasa UI SC", "更纱黑体 UI SC（内置）" })
    }.Concat(Installed.Value.Where(font => font.Key != "Sarasa UI SC")).ToArray();

    private static string LocalName(FontFamily font)
    {
        foreach (var culture in new[] { "zh-CN", "zh-Hans", "zh", CultureInfo.CurrentUICulture.IetfLanguageTag, "en-US" }.Distinct())
            if (font.FamilyNames.TryGetValue(XmlLanguage.GetLanguage(culture), out var name)) return name;
        return font.FamilyNames.FirstOrDefault(pair => pair.Key.IetfLanguageTag.StartsWith("zh", StringComparison.OrdinalIgnoreCase)).Value ?? font.Source;
    }

    public static DesktopFontChoice? Find(string? name)
    {
        if (string.IsNullOrWhiteSpace(name)) return Choices[0];
        return Choices.FirstOrDefault(font => string.Equals(font.Key, name.Trim(), StringComparison.OrdinalIgnoreCase) ||
            string.Equals(font.DisplayName, name.Trim(), StringComparison.OrdinalIgnoreCase) ||
            font.Aliases.Contains(name.Trim(), StringComparer.OrdinalIgnoreCase));
    }

    public static double ValidSize(double size) => double.IsFinite(size) ? Math.Clamp(size, MinimumSize, MaximumSize) : DefaultSize;

    public static double ReadSize(System.Text.Json.Nodes.JsonObject? settings)
    {
        if (settings?["desktopFontSize"] is not System.Text.Json.Nodes.JsonValue value) return DefaultSize;
        if (value.TryGetValue<double>(out var size)) return ValidSize(size);
        return value.TryGetValue<int>(out var integer) ? ValidSize(integer) : DefaultSize;
    }
}
