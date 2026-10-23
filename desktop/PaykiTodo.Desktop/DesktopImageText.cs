using Windows.Graphics.Imaging;
using Windows.Media.Ocr;
using Windows.Storage;

namespace PaykiTodo.Desktop;

internal static class DesktopImageText
{
    public static async Task<string> ReadAsync(string path, CancellationToken cancellationToken = default)
    {
        cancellationToken.ThrowIfCancellationRequested();
        var engine = OcrEngine.TryCreateFromUserProfileLanguages() ?? OcrEngine.AvailableRecognizerLanguages
            .Select(OcrEngine.TryCreateFromLanguage).FirstOrDefault(value => value != null);
        if (engine == null) throw new InvalidOperationException("请先在 Windows 设置中安装中文或英语语言的文字识别组件。");
        var file = await StorageFile.GetFileFromPathAsync(Path.GetFullPath(path));
        using var stream = await file.OpenAsync(FileAccessMode.Read);
        var decoder = await BitmapDecoder.CreateAsync(stream);
        var scale = Math.Min(1, OcrEngine.MaxImageDimension / (double)Math.Max(decoder.PixelWidth, decoder.PixelHeight));
        var transform = new BitmapTransform { ScaledWidth = (uint)Math.Max(1, decoder.PixelWidth * scale), ScaledHeight = (uint)Math.Max(1, decoder.PixelHeight * scale) };
        using var bitmap = await decoder.GetSoftwareBitmapAsync(BitmapPixelFormat.Bgra8, BitmapAlphaMode.Premultiplied, transform, ExifOrientationMode.RespectExifOrientation, ColorManagementMode.DoNotColorManage);
        cancellationToken.ThrowIfCancellationRequested();
        var result = await engine.RecognizeAsync(bitmap);
        cancellationToken.ThrowIfCancellationRequested();
        var text = string.Join(Environment.NewLine, result.Lines.Select(line => line.Text)).Trim();
        if (string.IsNullOrWhiteSpace(text)) throw new InvalidOperationException("没有识别到文字，请选择更清晰的图片。");
        return text;
    }
}
