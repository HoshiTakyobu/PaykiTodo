using System.IO.Compression;
using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace PaykiTodo.Desktop;

internal sealed record ServerSnapshotItem(
    long Id,
    bool IsEvent,
    string Title,
    string Notes,
    string Location,
    DateTime? DueAt,
    DateTime? StartAt,
    DateTime? EndAt,
    bool Completed,
    bool Canceled,
    string GroupName,
    string AccentColor
);

internal sealed record ServerSnapshotData(
    DateTime ExportedAt,
    IReadOnlyList<ServerSnapshotItem> Items,
    IReadOnlyList<string> PlanningNotes
);

internal static class ServerSnapshotReader
{
    private static readonly byte[] Magic = Encoding.ASCII.GetBytes("PKSYNC1");
    private static readonly byte[] Salt = Encoding.UTF8.GetBytes("PaykiTodo Remote Sync v1");

    public static async Task<ServerSnapshotData> DownloadAsync(
        string serverUrl,
        string deviceId,
        string secret,
        CancellationToken cancellationToken = default)
    {
        if (!serverUrl.StartsWith("https://", StringComparison.OrdinalIgnoreCase))
            throw new InvalidOperationException("服务器模式必须使用 HTTPS 地址");
        if (string.IsNullOrWhiteSpace(deviceId) || string.IsNullOrWhiteSpace(secret))
            throw new InvalidOperationException("请输入设备 ID 和远程同步密钥");

        var endpoint = serverUrl.TrimEnd('/') + "/v1/sync/" + Uri.EscapeDataString(deviceId.Trim());
        using var client = new HttpClient { Timeout = TimeSpan.FromSeconds(20) };
        using var request = new HttpRequestMessage(HttpMethod.Get, endpoint);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", secret.Trim());
        request.Headers.Add("X-Payki-Device", deviceId.Trim());
        using var response = await client.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, cancellationToken);
        if (!response.IsSuccessStatusCode)
            throw new InvalidOperationException($"服务器快照下载失败（HTTP {(int)response.StatusCode}）");

        var envelope = await response.Content.ReadAsByteArrayAsync(cancellationToken);
        return ParseSnapshot(Decrypt(envelope, secret.Trim()));
    }

    private static byte[] Decrypt(byte[] envelope, string secret)
    {
        const int ivLength = 12;
        const int tagLength = 16;
        if (envelope.Length <= Magic.Length + 1 + ivLength + tagLength ||
            !envelope.AsSpan(0, Magic.Length).SequenceEqual(Magic) ||
            envelope[Magic.Length] != 1)
            throw new InvalidOperationException("远程快照格式无效或版本过新");

        var ivOffset = Magic.Length + 1;
        var iv = envelope.AsSpan(ivOffset, ivLength);
        var encryptedLength = envelope.Length - ivOffset - ivLength - tagLength;
        var ciphertext = envelope.AsSpan(ivOffset + ivLength, encryptedLength);
        var tag = envelope.AsSpan(envelope.Length - tagLength, tagLength);
        var plaintext = new byte[encryptedLength];
        using var derive = new Rfc2898DeriveBytes(secret, Salt, 120_000, HashAlgorithmName.SHA256);
        var key = derive.GetBytes(32);
        try
        {
            using var aes = new AesGcm(key, tagLength);
            aes.Decrypt(iv, ciphertext, tag, plaintext);
            return plaintext;
        }
        catch (CryptographicException error)
        {
            throw new InvalidOperationException("远程同步密钥不匹配或快照已损坏", error);
        }
    }

    private static ServerSnapshotData ParseSnapshot(byte[] archiveBytes)
    {
        using var archiveStream = new MemoryStream(archiveBytes, writable: false);
        using var archive = new ZipArchive(archiveStream, ZipArchiveMode.Read);
        var entry = archive.GetEntry("backup.json")
            ?? throw new InvalidOperationException("远程迁移包缺少 backup.json");
        using var input = entry.Open();
        using var document = JsonDocument.Parse(input);
        var root = document.RootElement;

        var groups = new Dictionary<long, (string Name, string Color)>();
        if (root.TryGetProperty("groups", out var groupArray))
        {
            foreach (var group in groupArray.EnumerateArray())
            {
                var id = LongOrDefault(group, "id");
                groups[id] = (StringOrDefault(group, "name", "未分组"), StringOrDefault(group, "colorHex", "#4CB782"));
            }
        }

        var items = new List<ServerSnapshotItem>();
        if (root.TryGetProperty("tasks", out var taskArray))
        {
            foreach (var item in taskArray.EnumerateArray())
            {
                var groupId = LongOrDefault(item, "groupId");
                var group = groups.TryGetValue(groupId, out var foundGroup)
                    ? foundGroup
                    : (Name: "未分组", Color: "#4CB782");
                items.Add(new ServerSnapshotItem(
                    Id: LongOrDefault(item, "id"),
                    IsEvent: StringOrDefault(item, "itemType", "TODO").Equals("EVENT", StringComparison.OrdinalIgnoreCase),
                    Title: StringOrDefault(item, "title", "未命名事项"),
                    Notes: StringOrDefault(item, "notes"),
                    Location: StringOrDefault(item, "location"),
                    DueAt: EpochOrNull(item, "dueAtMillis"),
                    StartAt: EpochOrNull(item, "startAtMillis"),
                    EndAt: EpochOrNull(item, "endAtMillis"),
                    Completed: BoolOrDefault(item, "completed"),
                    Canceled: BoolOrDefault(item, "canceled"),
                    GroupName: group.Name,
                    AccentColor: SafeColor(group.Color)
                ));
            }
        }

        var planningNotes = new List<string>();
        if (root.TryGetProperty("planningNotes", out var notesArray))
        {
            foreach (var note in notesArray.EnumerateArray())
                planningNotes.Add(StringOrDefault(note, "title", "未命名规划"));
        }

        return new ServerSnapshotData(
            ExportedAt: EpochOrDefault(root, "exportedAtMillis", DateTimeOffset.UtcNow).LocalDateTime,
            Items: items,
            PlanningNotes: planningNotes
        );
    }

    private static long LongOrDefault(JsonElement element, string name)
    {
        return element.TryGetProperty(name, out var value) && value.ValueKind == JsonValueKind.Number && value.TryGetInt64(out var result)
            ? result
            : 0;
    }

    private static DateTime? EpochOrNull(JsonElement element, string name)
    {
        if (!element.TryGetProperty(name, out var value) || value.ValueKind != JsonValueKind.Number || !value.TryGetInt64(out var millis) || millis <= 0)
            return null;
        return DateTimeOffset.FromUnixTimeMilliseconds(millis).ToLocalTime().DateTime;
    }

    private static DateTimeOffset EpochOrDefault(JsonElement element, string name, DateTimeOffset fallback)
    {
        if (!element.TryGetProperty(name, out var value) || value.ValueKind != JsonValueKind.Number || !value.TryGetInt64(out var millis) || millis <= 0)
            return fallback;
        return DateTimeOffset.FromUnixTimeMilliseconds(millis).ToLocalTime();
    }

    private static string StringOrDefault(JsonElement element, string name, string fallback = "")
    {
        return element.TryGetProperty(name, out var value) && value.ValueKind == JsonValueKind.String
            ? value.GetString() ?? fallback
            : fallback;
    }

    private static bool BoolOrDefault(JsonElement element, string name)
    {
        return element.TryGetProperty(name, out var value) && value.ValueKind == JsonValueKind.True;
    }

    private static string SafeColor(string value)
    {
        return value.StartsWith('#') && value.Length is 4 or 7 or 9 && value.Skip(1).All(Uri.IsHexDigit)
            ? value
            : "#4CB782";
    }
}
