using System.IO.Compression;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;

namespace PaykiTodo.Desktop;

internal sealed record UpdateFile(string Path, long Size, string Sha256);
internal sealed record UpdateArchive(string Asset, long Size, string Sha256, string? FromVersion = null, string? BaselineSha256 = null);
internal sealed record UpdateRollout(int Percent, string Seed);
internal sealed record UpdateWindows(string EntryPoint, UpdateFile[] Files, UpdateArchive Full, UpdateArchive[] Deltas);
internal sealed record UpdateManifest(int Schema, string Version, string Tag, UpdateRollout Rollout, UpdateWindows Windows);
internal sealed record UpdateEnvelope(string Manifest, string Signature);
internal sealed record UpdateRelease(string Tag, string Url, string Notes, Dictionary<string, string> Assets);

internal static class DesktopUpdateProtocol
{
    internal const string Repository = "HoshiTakyobu/PaykiTodo";
    internal const string ManifestAsset = "PaykiTodo-update-manifest.json";
    internal const long MaxArchiveBytes = 800_000_000;
    internal static readonly JsonSerializerOptions Json = new() { PropertyNamingPolicy = JsonNamingPolicy.CamelCase, PropertyNameCaseInsensitive = true };
    internal static Version ParseVersion(string text) => Regex.IsMatch(text, @"^v?\d+\.\d+\.\d+$") && Version.TryParse(text.TrimStart('v'), out var value)
        ? value : throw new InvalidDataException("更新版本格式无效");
    internal static string Hash(byte[] bytes) => Convert.ToHexString(SHA256.HashData(bytes)).ToLowerInvariant();
    internal static string FileHash(string file) { using var stream = File.OpenRead(file); return Convert.ToHexString(SHA256.HashData(stream)).ToLowerInvariant(); }
    internal static string BaselineHash(IEnumerable<UpdateFile> files) => Hash(Encoding.UTF8.GetBytes(string.Concat(files.OrderBy(f => f.Path, StringComparer.Ordinal).Select(f => $"{f.Path}\t{f.Size}\t{f.Sha256.ToLowerInvariant()}\n"))));
    internal static string PublicKey() { using var stream = typeof(DesktopUpdateProtocol).Assembly.GetManifestResourceStream("PaykiTodo.UpdatePublicKey")!; using var reader = new StreamReader(stream); return reader.ReadToEnd(); }
    internal static UpdateManifest Verify(byte[] envelope, string publicKey)
    {
        if (envelope.Length > 4_000_000) throw new InvalidDataException("更新清单过大");
        var wire = JsonSerializer.Deserialize<UpdateEnvelope>(envelope, Json) ?? throw new InvalidDataException("缺少更新清单");
        var payload = Convert.FromBase64String(wire.Manifest);
        using var rsa = RSA.Create(); rsa.ImportFromPem(publicKey);
        if (!rsa.VerifyData(payload, Convert.FromBase64String(wire.Signature), HashAlgorithmName.SHA256, RSASignaturePadding.Pss)) throw new InvalidDataException("更新签名校验失败");
        var manifest = JsonSerializer.Deserialize<UpdateManifest>(payload, Json) ?? throw new InvalidDataException("清单无效");
        Validate(manifest); return manifest;
    }
    internal static void Validate(UpdateManifest m)
    {
        ParseVersion(m.Version);
        if (m.Schema != 1 || m.Tag != "v" + m.Version || m.Rollout is null || m.Rollout.Percent is < 0 or > 100 || string.IsNullOrWhiteSpace(m.Rollout.Seed) || m.Windows is null || m.Windows.EntryPoint != "PaykiTodo.exe" || m.Windows.Files is not { Length: > 0 and < 5000 }) throw new InvalidDataException("不支持的更新清单");
        var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase); long total = 0;
        foreach (var f in m.Windows.Files) {
            SafeRelative(f.Path); CheckHash(f.Sha256);
            if (!seen.Add(f.Path) || f.Size < 0 || f.Size > MaxArchiveBytes) throw new InvalidDataException("文件清单无效");
            total = checked(total + f.Size);
        }
        if (total > 2_000_000_000 || !seen.Contains(m.Windows.EntryPoint)) throw new InvalidDataException("更新内容无效");
        CheckArchive(m.Windows.Full);
        foreach (var delta in m.Windows.Deltas ?? []) { CheckArchive(delta); ParseVersion(delta.FromVersion ?? ""); CheckHash(delta.BaselineSha256 ?? ""); }
    }
    private static void CheckHash(string value) { if (!Regex.IsMatch(value ?? "", "^[0-9a-fA-F]{64}$")) throw new InvalidDataException("哈希格式无效"); }
    private static void CheckArchive(UpdateArchive a) { SafeRelative(a.Asset); CheckHash(a.Sha256); if (a.Asset.Contains('/') || !a.Asset.EndsWith(".zip", StringComparison.Ordinal) || a.Size is <= 0 or > MaxArchiveBytes) throw new InvalidDataException("更新压缩包无效"); }
    internal static void SafeRelative(string path)
    {
        if (string.IsNullOrEmpty(path) || path.Contains('\\') || path.Contains(':') || path.StartsWith('/') || path.Split('/').Any(p => p is "" or "." or ".." || p.EndsWith('.') || p.EndsWith(' ') || p.IndexOfAny(Path.GetInvalidFileNameChars()) >= 0 || Regex.IsMatch(p, @"^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\.|$)", RegexOptions.IgnoreCase))) throw new InvalidDataException("更新文件路径无效");
    }
    internal static string Child(string root, string relative)
    {
        SafeRelative(relative); var full = Path.GetFullPath(Path.Combine(root, relative));
        if (!full.StartsWith(Path.GetFullPath(root).TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase)) throw new InvalidDataException("路径越界");
        var current = full;
        while (current != Path.GetPathRoot(current)) { if ((File.Exists(current) || Directory.Exists(current)) && (File.GetAttributes(current) & FileAttributes.ReparsePoint) != 0) throw new InvalidDataException("更新目录包含链接"); current = Path.GetDirectoryName(current)!; }
        return full;
    }
    internal static bool InRollout(string installId, UpdateRollout rollout) => BitConverter.ToUInt32(SHA256.HashData(Encoding.UTF8.GetBytes(rollout.Seed + ":" + installId)), 0) % 100 < rollout.Percent;
    internal static UpdateRelease? ReadRelease(byte[] json)
    {
        using var doc = JsonDocument.Parse(json); UpdateRelease? best = null;
        foreach (var r in doc.RootElement.EnumerateArray()) {
            if (r.GetProperty("draft").GetBoolean() || r.GetProperty("prerelease").GetBoolean()) continue;
            var tag = r.GetProperty("tag_name").GetString()!;
            try { ParseVersion(tag); } catch (InvalidDataException) { continue; }
            var url = r.GetProperty("html_url").GetString()!;
            if (url != $"https://github.com/{Repository}/releases/tag/{tag}") continue;
            var assets = new Dictionary<string, string>(StringComparer.Ordinal);
            foreach (var a in r.GetProperty("assets").EnumerateArray()) {
                var name = a.GetProperty("name").GetString()!; var download = a.GetProperty("browser_download_url").GetString()!;
                if (download == $"https://github.com/{Repository}/releases/download/{tag}/{Uri.EscapeDataString(name)}") assets.TryAdd(name, download);
            }
            if (best == null || ParseVersion(tag) > ParseVersion(best.Tag)) best = new(tag, url, r.GetProperty("body").GetString() ?? "", assets);
        }
        return best;
    }
    internal static void VerifyDirectory(string directory, UpdateManifest m)
    {
        foreach (var f in m.Windows.Files) { var path = Child(directory, f.Path); if (!File.Exists(path) || new FileInfo(path).Length != f.Size || !FileHash(path).Equals(f.Sha256, StringComparison.OrdinalIgnoreCase)) throw new InvalidDataException("更新文件校验失败：" + f.Path); }
    }
    internal static void Extract(string zipPath, string target, UpdateManifest m)
    {
        using var zip = ZipFile.OpenRead(zipPath); var files = m.Windows.Files.ToDictionary(f => f.Path, StringComparer.OrdinalIgnoreCase); var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        foreach (var e in zip.Entries) {
            SafeRelative(e.FullName);
            if (!seen.Add(e.FullName) || !files.TryGetValue(e.FullName, out var f) || e.Length != f.Size || ((e.ExternalAttributes >> 16) & 0xF000) == 0xA000) throw new InvalidDataException("压缩包文件不在签名清单中");
            var path = Child(target, f.Path); Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            e.ExtractToFile(path, overwrite: true);
        }
    }
}
