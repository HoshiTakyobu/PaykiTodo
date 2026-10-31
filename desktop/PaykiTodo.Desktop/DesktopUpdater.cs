using System.Net;

namespace PaykiTodo.Desktop;

internal sealed class DesktopUpdater(DesktopUpdateStore store, HttpClient client, string publicKey)
{
    internal async Task<(UpdateRelease? Release, UpdateManifest? Manifest)> CheckAsync(Version current, bool manual, CancellationToken token)
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        var due = store.Change(state => (!manual && now - state.LastCheck < 43200) ? (state, false) : (state with { LastCheck = now }, true));
        if (!due) return (null, null);
        var release = DesktopUpdateProtocol.ReadRelease(await ReadBytes("https://api.github.com/repos/" + DesktopUpdateProtocol.Repository + "/releases?per_page=30", 4_000_000, token));
        if (release == null || DesktopUpdateProtocol.ParseVersion(release.Tag) <= current) return (null, null);
        if (!release.Assets.TryGetValue(DesktopUpdateProtocol.ManifestAsset, out var url)) return (release, null);
        var manifest = DesktopUpdateProtocol.Verify(await ReadBytes(url, 4_000_000, token), publicKey);
        if (manifest.Tag != release.Tag) throw new InvalidDataException("发布版本与签名清单不一致");
        return (release, manifest);
    }
    private async Task<byte[]> ReadBytes(string url, long limit, CancellationToken token)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, url); request.Headers.UserAgent.ParseAdd("PaykiTodo-Updater/1");
        using var response = await client.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, token);
        if (response.StatusCode is HttpStatusCode.Forbidden or HttpStatusCode.TooManyRequests) throw new IOException("GitHub 暂时限制请求，请稍后重试");
        response.EnsureSuccessStatusCode(); if (response.Content.Headers.ContentLength > limit) throw new InvalidDataException("下载内容过大");
        using var stream = await response.Content.ReadAsStreamAsync(token); using var memory = new MemoryStream(); var buffer = new byte[65536]; int count;
        while ((count = await stream.ReadAsync(buffer, token)) > 0) { if (memory.Length + count > limit) throw new InvalidDataException("下载内容过大"); await memory.WriteAsync(buffer.AsMemory(0, count), token); }
        return memory.ToArray();
    }
    internal async Task<string> PrepareAsync(UpdateRelease release, UpdateManifest manifest, string currentDirectory, string currentVersion, bool manual, CancellationToken token)
    {
        DesktopUpdateProtocol.Validate(manifest);
        if (release.Tag != manifest.Tag || (!manual && !DesktopUpdateProtocol.InRollout(store.Change(s => (s, s.InstallId)), manifest.Rollout))) return "发现新版本，尚未轮到本机自动更新；可手动下载安装";
        if (store.Read().Failed == manifest.Version) return "此版本曾启动失败，已回退；请使用发布页面安装包或等待下一版";
        var target = store.VersionDirectory(manifest.Version);
        if (Directory.Exists(target)) {
            // Never mutate a previously staged/active version directory.
            DesktopUpdateProtocol.VerifyDirectory(target, manifest);
            store.Change(s => (s with { Pending = manifest.Version }, true)); return "新版本已准备，下次启动生效";
        }
        var staging = DesktopUpdateProtocol.Child(store.Root, "staging/" + Guid.NewGuid().ToString("N")); Directory.CreateDirectory(staging);
        var baseline = manifest.Windows.Deltas?.FirstOrDefault(d => d.FromVersion == currentVersion);
        var usedDelta = false;
        if (baseline != null) {
            try {
                var baselineFile = DesktopUpdateProtocol.Child(currentDirectory, "update-files.json");
                var old = System.Text.Json.JsonSerializer.Deserialize<UpdateFile[]>(File.ReadAllBytes(baselineFile), DesktopUpdateProtocol.Json)!;
                if (DesktopUpdateProtocol.BaselineHash(old) != baseline.BaselineSha256) throw new InvalidDataException("增量基线不匹配");
                foreach (var f in manifest.Windows.Files) {
                    var previous = old.FirstOrDefault(o => o.Path == f.Path && o.Size == f.Size && o.Sha256 == f.Sha256);
                    if (previous == null) continue;
                    var source = DesktopUpdateProtocol.Child(currentDirectory, f.Path);
                    if (!File.Exists(source) || new FileInfo(source).Length != f.Size || DesktopUpdateProtocol.FileHash(source) != f.Sha256) throw new InvalidDataException("本机基线已变化");
                    var dest = DesktopUpdateProtocol.Child(staging, f.Path); Directory.CreateDirectory(Path.GetDirectoryName(dest)!); File.Copy(source, dest);
                }
                await ApplyArchive(release, manifest, baseline, staging, token); DesktopUpdateProtocol.VerifyDirectory(staging, manifest); usedDelta = true;
            } catch (Exception e) when (e is IOException or InvalidDataException or System.Net.Http.HttpRequestException or InvalidOperationException or System.Text.Json.JsonException) { token.ThrowIfCancellationRequested(); }
        }
        if (!usedDelta) {
            // A fresh directory keeps files from the failed candidate isolated.
            staging = DesktopUpdateProtocol.Child(store.Root, "staging/" + Guid.NewGuid().ToString("N")); Directory.CreateDirectory(staging);
            await ApplyArchive(release, manifest, manifest.Windows.Full, staging, token); DesktopUpdateProtocol.VerifyDirectory(staging, manifest);
        }
        var rawEnvelope = await ReadBytes(release.Assets[DesktopUpdateProtocol.ManifestAsset], 4_000_000, token);
        var checkedManifest = DesktopUpdateProtocol.Verify(rawEnvelope, publicKey);
        if (System.Text.Json.JsonSerializer.Serialize(checkedManifest, DesktopUpdateProtocol.Json) != System.Text.Json.JsonSerializer.Serialize(manifest, DesktopUpdateProtocol.Json)) throw new InvalidDataException("下载期间发布清单已变化，请重新检查");
        File.WriteAllBytes(Path.Combine(staging, "update-manifest.json"), rawEnvelope);
        File.WriteAllBytes(Path.Combine(staging, "update-files.json"), System.Text.Json.JsonSerializer.SerializeToUtf8Bytes(manifest.Windows.Files, DesktopUpdateProtocol.Json));
        Directory.CreateDirectory(Path.GetDirectoryName(target)!); Directory.Move(staging, target);
        store.Change(s => (s with { Pending = manifest.Version }, true));
        return (usedDelta ? "增量更新" : "完整更新") + "已准备，下次启动生效";
    }
    private async Task ApplyArchive(UpdateRelease release, UpdateManifest m, UpdateArchive archive, string staging, CancellationToken token)
    {
        if (!release.Assets.TryGetValue(archive.Asset, out var url)) throw new InvalidDataException("发布缺少安装资产");
        var bytes = await ReadBytes(url, archive.Size, token);
        if (bytes.LongLength != archive.Size || DesktopUpdateProtocol.Hash(bytes) != archive.Sha256) throw new InvalidDataException("下载压缩包校验失败");
        var cache = DesktopUpdateProtocol.Child(store.Root, "downloads/" + Guid.NewGuid().ToString("N") + ".zip"); Directory.CreateDirectory(Path.GetDirectoryName(cache)!); await File.WriteAllBytesAsync(cache, bytes, token);
        DesktopUpdateProtocol.Extract(cache, staging, m);
    }
}
