using System.IO;
using System.IO.Compression;
using System.Security.Cryptography;
using System.Text.Json.Nodes;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class AndroidUxRoundtripTests
{
    [AndroidUxFixtureFact]
    public void AndroidExportPreservesDesktopStyleColorFontAndBusinessTimes()
    {
        var source = Environment.GetEnvironmentVariable("PAYKI_ANDROID_UX_EVIDENCE")!;
        var evidence = Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE")!;
        foreach (var style in new[] { "CLASSIC", "MODERN" }) {
            var path = Path.Combine(source, $"android-roundtrip-{style}.json");
            var original = File.ReadAllBytes(path);
            using var input = new MemoryStream();
            using (var archive = new ZipArchive(input, ZipArchiveMode.Create, true)) {
                using var stream = archive.CreateEntry("backup.json").Open(); stream.Write(original);
            }
            var document = DesktopSnapshotDocument.FromArchiveBytes(input.ToArray());
            Assert.Equal(style == "CLASSIC", document.ClassicCalendarColors);
            Assert.Equal(style, document.Settings["desktopCalendarEventStyle"]!.GetValue<string>());
            Assert.Equal("方正小标宋简体", document.Settings["desktopFontFamily"]!.GetValue<string>());
            Assert.Equal(22, document.Settings["desktopFontSize"]!.GetValue<int>());
            var task = Assert.Single(document.Items);
            Assert.Equal("#DF58A5", task.AccentColor);
            Assert.Equal(1_920_000_000_000L, task.DueAtMillis);
            Assert.True(document.EditFor(task.Id)!.ReminderEnabled);
            var report = Assert.Single(document.Reports);
            Assert.Equal(JsonNode.Parse(original)!["aiReports"]![0]!["content"]!.GetValue<string>(), report["content"]!.GetValue<string>());
            var exported = document.ToArchiveBytes();
            var restored = DesktopSnapshotDocument.FromArchiveBytes(exported);
            Assert.True(document.HasSamePersonalSyncContent(restored));
            Assert.Equal(Convert.ToHexString(SHA256.HashData(original)), Convert.ToHexString(SHA256.HashData(File.ReadAllBytes(path))));
            File.WriteAllBytes(Path.Combine(evidence, $"windows-consumed-android-{style}.paykibak"), exported);
        }
    }

    private sealed class AndroidUxFixtureFactAttribute : FactAttribute
    {
        public AndroidUxFixtureFactAttribute() {
            if (string.IsNullOrEmpty(Environment.GetEnvironmentVariable("PAYKI_ANDROID_UX_EVIDENCE")))
                Skip = "Supply the isolated Android UX export evidence to verify the actual cross-client roundtrip";
        }
    }
}
