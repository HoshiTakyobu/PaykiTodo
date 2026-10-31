using System.IO.Compression;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using PaykiTodo.Desktop;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class DesktopUpdaterTests
{
    private readonly string root = Path.Combine(Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE") ?? Path.GetTempPath(), "updates-" + Guid.NewGuid().ToString("N"));
    private static byte[] Bytes(string text) => Encoding.UTF8.GetBytes(text);
    private static UpdateFile FileSpec(string name, string value) => new(name, Bytes(value).Length, DesktopUpdateProtocol.Hash(Bytes(value)));
    private static byte[] Zip(params (string Name, string Value)[] files) {
        using var memory = new MemoryStream(); using (var zip = new ZipArchive(memory, ZipArchiveMode.Create, true)) foreach (var f in files) { using var writer = new StreamWriter(zip.CreateEntry(f.Name).Open()); writer.Write(f.Value); } return memory.ToArray();
    }
    private static byte[] Sign(UpdateManifest manifest, RSA rsa) {
        var raw = JsonSerializer.SerializeToUtf8Bytes(manifest, DesktopUpdateProtocol.Json);
        return JsonSerializer.SerializeToUtf8Bytes(new UpdateEnvelope(Convert.ToBase64String(raw), Convert.ToBase64String(rsa.SignData(raw, HashAlgorithmName.SHA256, RSASignaturePadding.Pss))), DesktopUpdateProtocol.Json);
    }
    private sealed class HttpFixture(Dictionary<string, byte[]> content) : HttpMessageHandler {
        internal List<string> Requests { get; } = [];
        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken token) { var name = Uri.UnescapeDataString(request.RequestUri!.Segments.Last()); Requests.Add(name); return Task.FromResult(content.TryGetValue(name, out var bytes) ? new HttpResponseMessage(HttpStatusCode.OK) { Content = new ByteArrayContent(bytes) } : new HttpResponseMessage(HttpStatusCode.NotFound)); }
    }
    [Fact] public void VersionAndReleaseSelectionRejectPrereleasesAndExternalAssets() {
        Assert.True(DesktopUpdateProtocol.ParseVersion("v1.17.11") > DesktopUpdateProtocol.ParseVersion("1.17.9"));
        Assert.Throws<InvalidDataException>(() => DesktopUpdateProtocol.ParseVersion("1.17.11-beta"));
        var json = "[{\"draft\":false,\"prerelease\":true,\"tag_name\":\"v9.0.0\"},{\"draft\":false,\"prerelease\":false,\"tag_name\":\"v1.17.11\",\"html_url\":\"https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.17.11\",\"body\":\"说明\",\"assets\":[{\"name\":\"app.zip\",\"browser_download_url\":\"https://example.com/app.zip\"}]}]";
        var release = DesktopUpdateProtocol.ReadRelease(Bytes(json))!; Assert.Equal("v1.17.11", release.Tag); Assert.Empty(release.Assets);
    }
    [Theory] [InlineData("../outside.exe")] [InlineData("a/../../x")] [InlineData("C:/x")] [InlineData("a\\b")] [InlineData("CON.exe")] [InlineData("a/ /x")] [InlineData("a./b")]
    public void InvalidPathsReject(string name) => Assert.Throws<InvalidDataException>(() => DesktopUpdateProtocol.SafeRelative(name));
    [Theory] [InlineData(false, false)] [InlineData(true, false)] [InlineData(false, true)]
    public async Task PreparesActualDeltaOrFullFallbackAndKeepsSeedAndUserData(bool corruptDelta, bool corruptBaseline) {
        Directory.CreateDirectory(root); var seed = Path.Combine(root, "seed"); Directory.CreateDirectory(seed);
        File.WriteAllText(Path.Combine(seed, "PaykiTodo.exe"), "old exe"); File.WriteAllText(Path.Combine(seed, "unchanged.dll"), "same");
        File.WriteAllText(Path.Combine(root, "user-data.bin"), "private fixture");
        var oldFiles = new[] { FileSpec("PaykiTodo.exe", "old exe"), FileSpec("unchanged.dll", "same") };
        File.WriteAllBytes(Path.Combine(seed, "update-files.json"), JsonSerializer.SerializeToUtf8Bytes(oldFiles, DesktopUpdateProtocol.Json));
        if (corruptBaseline) File.WriteAllText(Path.Combine(seed, "unchanged.dll"), "broken baseline");
        var before = File.ReadAllBytes(Path.Combine(seed, "PaykiTodo.exe"));
        var full = Zip(("PaykiTodo.exe", "new exe"), ("unchanged.dll", "same")); var delta = Zip(("PaykiTodo.exe", "new exe"));
        var m = new UpdateManifest(1, "1.17.12", "v1.17.12", new(100, "test"), new("PaykiTodo.exe", [FileSpec("PaykiTodo.exe", "new exe"), FileSpec("unchanged.dll", "same")], new("full.zip", full.Length, DesktopUpdateProtocol.Hash(full)), [new("delta.zip", delta.Length, DesktopUpdateProtocol.Hash(delta), "1.17.11", DesktopUpdateProtocol.BaselineHash(oldFiles))]));
        using var rsa = RSA.Create(2048); var signed = Sign(m, rsa);
        var fixture = new HttpFixture(new() { [DesktopUpdateProtocol.ManifestAsset] = signed, ["full.zip"] = full, ["delta.zip"] = corruptDelta ? Bytes("damaged") : delta }); using var client = new HttpClient(fixture);
        var store = new DesktopUpdateStore(seed, Path.Combine(root, "update-cache")); var updater = new DesktopUpdater(store, client, rsa.ExportSubjectPublicKeyInfoPem());
        var release = new UpdateRelease(m.Tag, "unused", "", new() { [DesktopUpdateProtocol.ManifestAsset] = "https://github.com/test/" + DesktopUpdateProtocol.ManifestAsset, ["full.zip"] = "https://github.com/test/full.zip", ["delta.zip"] = "https://github.com/test/delta.zip" });
        var result = await updater.PrepareAsync(release, m, seed, "1.17.11", false, CancellationToken.None);
        Assert.Equal(corruptDelta || corruptBaseline, result.StartsWith("完整"));
        Assert.Equal(corruptDelta || corruptBaseline, fixture.Requests.Contains("full.zip")); Assert.Equal(!corruptBaseline, fixture.Requests.Contains("delta.zip"));
        Assert.Equal(before, File.ReadAllBytes(Path.Combine(seed, "PaykiTodo.exe"))); Assert.Equal("private fixture", File.ReadAllText(Path.Combine(root, "user-data.bin")));
        Assert.Equal("1.17.12", store.Read().Pending);
        var launch = store.BeginLaunch(new Version(1,17,11), rsa.ExportSubjectPublicKeyInfoPem()); Assert.Equal("1.17.12", launch.Version);
        store.Confirm("wrong nonce"); Assert.Equal(launch.Nonce, store.Read().Nonce);
        store.Confirm(launch.Nonce!); Assert.Equal("1.17.12", store.Read().Active); Assert.Null(store.Read().Pending);
        var next = store.BeginLaunch(new Version(1,17,11), rsa.ExportSubjectPublicKeyInfoPem()); store.Fail(next.Nonce!);
        Assert.Equal(seed, store.BeginLaunch(new Version(1,17,11), rsa.ExportSubjectPublicKeyInfoPem()).Directory);
    }
    [Fact] public void ManifestSignatureAndZipTraversalAreRejected() {
        var m = new UpdateManifest(1,"1.17.12","v1.17.12",new(50,"seed"),new("PaykiTodo.exe",[FileSpec("PaykiTodo.exe","exe")],new("full.zip",3,new string('a',64)),[]));
        using var rsa = RSA.Create(2048); var bytes = Sign(m,rsa); Assert.Equal(m.Version, DesktopUpdateProtocol.Verify(bytes,rsa.ExportSubjectPublicKeyInfoPem()).Version);
        using var wrong = RSA.Create(2048); Assert.Throws<InvalidDataException>(() => DesktopUpdateProtocol.Verify(bytes,wrong.ExportSubjectPublicKeyInfoPem()));
        Directory.CreateDirectory(root); var zipPath = Path.Combine(root,"evil.zip"); File.WriteAllBytes(zipPath,Zip(("../outside.exe","exe")));
        Assert.Throws<InvalidDataException>(() => DesktopUpdateProtocol.Extract(zipPath,Path.Combine(root,"staged"),m)); Assert.False(File.Exists(Path.Combine(root,"outside.exe")));
    }
    [Fact] public void InstallationIdentityAndRolloutAreStableAndManualCheckBypassesThrottle() {
        var store = new DesktopUpdateStore(root,Path.Combine(root,"cache")); var first = store.Change(s => (s,s.InstallId)); Assert.Equal(first,new DesktopUpdateStore(root,store.Root).Change(s => (s,s.InstallId)));
        Assert.False(DesktopUpdateProtocol.InRollout(first,new(0,"seed"))); Assert.True(DesktopUpdateProtocol.InRollout(first,new(100,"seed")));
        Assert.Equal(DesktopUpdateProtocol.InRollout(first,new(30,"seed")),DesktopUpdateProtocol.InRollout(first,new(30,"seed")));
    }
}
