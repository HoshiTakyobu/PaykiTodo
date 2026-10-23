using System.IO;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class ImageTextTests
{
    [Fact] public async Task WindowsOcrReadsLocalImageAndHonorsCancellation()
    {
        if (Windows.Media.Ocr.OcrEngine.AvailableRecognizerLanguages.Count == 0) throw Xunit.Sdk.SkipException.ForSkip("Windows OCR language is not installed");
        var directory = Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE") ?? Path.Combine(Path.GetTempPath(), "PaykiTodo-tests", Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(directory); var path = Path.Combine(directory, "ocr-fixture.png");
        using (var bitmap = new System.Drawing.Bitmap(1400, 240)) {
            using var graphics = System.Drawing.Graphics.FromImage(bitmap);
            graphics.Clear(System.Drawing.Color.White);
            using var font = new System.Drawing.Font("Arial", 58);
            graphics.DrawString("PaykiTodo meeting 09:30", font, System.Drawing.Brushes.Black, 25, 60);
            bitmap.Save(path, System.Drawing.Imaging.ImageFormat.Png);
        }
        var text = await DesktopImageText.ReadAsync(path);
        Assert.Contains("meeting", text, StringComparison.OrdinalIgnoreCase);
        Assert.Matches(@"09\s*[:：]\s*30", text);
        using var canceled = new CancellationTokenSource(); canceled.Cancel();
        await Assert.ThrowsAnyAsync<OperationCanceledException>(() => DesktopImageText.ReadAsync(path, canceled.Token));
    }
}
