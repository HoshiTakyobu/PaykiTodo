using System.IO.Compression;
using System.Net;
using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace PaykiTodo.Desktop;

internal sealed record CloudAccountState(
    string ServerUrl,
    string Username,
    string DeviceId,
    string AccessToken,
    string RefreshToken,
    long AccessExpiresAt,
    long RefreshExpiresAt,
    string EncryptionSecret,
    long Revision,
    bool Dirty
) {
    public bool LocalAccessLocked { get; init; }
}

internal sealed record CloudAuthenticationResult(CloudAccountState State, long RemoteRevision);

internal sealed record CloudSyncResult(CloudAccountState State, DesktopSnapshotDocument Document, string Message);
internal sealed class CloudSessionExpiredException() : InvalidOperationException("登录已过期，请重新登录；本机数据、离线提醒和待同步修改已保留。");
internal sealed record CloudDownload(DesktopSnapshotDocument Document, long Revision);

internal sealed class CloudRevisionConflictException(long remoteRevision)
    : InvalidOperationException($"云端版本 {remoteRevision} 已更新，本机也有未上传修改。为防止覆盖，已停止同步。")
{
    public long RemoteRevision { get; } = remoteRevision;
}

internal static class CloudAccountSync
{
    private static readonly HttpClient Client = new() { Timeout = TimeSpan.FromSeconds(25) };

    public static async Task<CloudAuthenticationResult> AuthenticateAsync(
        string serverUrl,
        string username,
        string password,
        string deviceId,
        bool register,
        CancellationToken cancellationToken = default,
        string? turnstileToken = null)
    {
        var normalizedServer = NormalizeServerUrl(serverUrl);
        var normalizedUsername = username.Trim().ToLowerInvariant();
        if (normalizedUsername.Length < 3) throw new InvalidOperationException("请输入有效账号");
        if (register && password.Length < 10) throw new InvalidOperationException("注册密码至少 10 位");
        deviceId = string.IsNullOrWhiteSpace(deviceId) ? Guid.NewGuid().ToString("N") : deviceId;
        var payload = JsonSerializer.SerializeToUtf8Bytes(new
        {
            username = normalizedUsername,
            password,
            turnstileToken,
            displayName = normalizedUsername,
            deviceId,
            deviceName = Environment.MachineName,
            platform = "windows"
        });
        using var request = new HttpRequestMessage(HttpMethod.Post, normalizedServer + (register ? "/v2/auth/register" : "/v2/auth/login"));
        request.Content = new ByteArrayContent(payload);
        request.Content.Headers.ContentType = new MediaTypeHeaderValue("application/json") { CharSet = "utf-8" };
        using var response = await Client.SendAsync(request, cancellationToken);
        using var document = await ReadJsonAsync(response, cancellationToken);
        EnsureSuccess(response, document.RootElement);
        var root = document.RootElement;
        var canonicalUsername = root.GetProperty("user").GetProperty("username").GetString() ?? normalizedUsername;
        var revision = root.TryGetProperty("snapshotRevision", out var revisionValue) ? revisionValue.GetInt64() : 0L;
        var state = new CloudAccountState(
            normalizedServer,
            canonicalUsername,
            root.TryGetProperty("deviceId", out var returnedDevice) ? returnedDevice.GetString() ?? deviceId : deviceId,
            root.GetProperty("accessToken").GetString() ?? throw new InvalidOperationException("服务器未返回访问令牌"),
            root.GetProperty("refreshToken").GetString() ?? throw new InvalidOperationException("服务器未返回刷新令牌"),
            root.GetProperty("accessExpiresAt").GetInt64(),
            root.GetProperty("refreshExpiresAt").GetInt64(),
            DeriveAccountSecret(canonicalUsername, password),
            revision,
            false
        );
        return new CloudAuthenticationResult(state, revision);
    }

    public static async Task<CloudSyncResult> SyncAsync(
        CloudAccountState state,
        DesktopSnapshotDocument localDocument,
        CancellationToken cancellationToken = default)
    {
        state = await EnsureAccessTokenAsync(state, cancellationToken);
        var remoteRevision = await GetRemoteRevisionAsync(state, cancellationToken);
        if (remoteRevision > state.Revision)
        {
            if (state.Dirty) throw new CloudRevisionConflictException(remoteRevision);
            var downloaded = await DownloadAsync(state, cancellationToken);
            downloaded.Document.PreserveLocalApiKeys(localDocument);
            var nextState = state with { Revision = downloaded.Revision, Dirty = false };
            return new CloudSyncResult(nextState, downloaded.Document, $"已拉取云端版本 {downloaded.Revision}");
        }
        if (remoteRevision < state.Revision)
            throw new InvalidOperationException("云端版本低于本机同步记录，已停止同步以避免覆盖");
        if (!state.Dirty)
            return new CloudSyncResult(state, localDocument, $"已是最新版本 {state.Revision}");

        var encrypted = SnapshotCrypto.Encrypt(localDocument.ToArchiveBytes(includeSecrets: false), state.EncryptionSecret);
        using var request = AuthorizedRequest(state, HttpMethod.Put, "/v2/sync/snapshot");
        request.Headers.TryAddWithoutValidation("If-Match", $"\"{state.Revision}\"");
        request.Headers.Add("X-Payki-Operation-Id", "windows:" + Guid.NewGuid().ToString("N"));
        request.Content = new ByteArrayContent(encrypted);
        request.Content.Headers.ContentType = new MediaTypeHeaderValue("application/octet-stream");
        using var response = await Client.SendAsync(request, cancellationToken);
        using var result = await ReadJsonAsync(response, cancellationToken);
        if (response.StatusCode == HttpStatusCode.Conflict)
        {
            var conflict = result.RootElement.TryGetProperty("currentRevision", out var current) ? current.GetInt64() : state.Revision;
            throw new CloudRevisionConflictException(conflict);
        }
        EnsureSuccess(response, result.RootElement);
        var revision = result.RootElement.GetProperty("revision").GetInt64();
        var uploadedState = state with { Revision = revision, Dirty = false };
        return new CloudSyncResult(uploadedState, localDocument, $"本机修改已上传为版本 {revision}");
    }

    public static async Task LogoutAsync(CloudAccountState state, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(state.AccessToken)) return;
        using var request = AuthorizedRequest(state, HttpMethod.Post, "/v2/auth/logout");
        request.Content = new StringContent("{}", Encoding.UTF8, "application/json");
        using var response = await Client.SendAsync(request, cancellationToken);
    }

    public static async Task<(CloudAccountState State, JsonElement Body)> AccountRequestAsync(CloudAccountState state,
        string path, HttpMethod method, object? payload = null)
    {
        state = await EnsureAccessTokenAsync(state, default);
        using var request = AuthorizedRequest(state, method, path);
        if (payload != null) request.Content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");
        using var response = await Client.SendAsync(request);
        using var json = await ReadJsonAsync(response, default);
        EnsureSuccess(response, json.RootElement);
        return (state, json.RootElement.Clone());
    }

    public static async Task<CloudAccountState> ChangePasswordAsync(CloudAccountState state, string currentPassword, string newPassword)
    {
        if (newPassword.Length is < 10 or > 256) throw new InvalidOperationException("新密码需为 10—256 位");
        if (DeriveAccountSecret(state.Username, currentPassword) != state.EncryptionSecret) throw new InvalidOperationException("原密码不正确");
        state = await EnsureAccessTokenAsync(state, default);
        using var request = AuthorizedRequest(state, HttpMethod.Get, "/v2/sync/snapshot");
        using var response = await Client.SendAsync(request);
        var secret = DeriveAccountSecret(state.Username, newPassword);
        long revision = 0; string? encrypted = null;
        if (response.IsSuccessStatusCode) {
            if (!response.Headers.TryGetValues("X-Payki-Revision", out var values) || !long.TryParse(values.First(), out revision))
                throw new InvalidOperationException("服务器未返回数据版本");
            var data = SnapshotCrypto.Decrypt(await response.Content.ReadAsByteArrayAsync(), state.EncryptionSecret);
            encrypted = Convert.ToBase64String(SnapshotCrypto.Encrypt(data, secret));
        } else if (response.StatusCode != HttpStatusCode.NotFound) throw new InvalidOperationException("云端数据暂时无法读取，请稍后修改密码");
        await AccountRequestAsync(state, "/v2/account/password", HttpMethod.Post,
            new { currentPassword, newPassword, snapshotRevision = revision, encryptedSnapshot = encrypted });
        return state with { EncryptionSecret = secret, AccessToken = "", RefreshToken = "", AccessExpiresAt = 0,
            RefreshExpiresAt = 0, Revision = encrypted == null ? 0 : revision + 1 };
    }

    internal static async Task<long> GetRemoteRevisionAsync(CloudAccountState state, CancellationToken cancellationToken)
    {
        using var request = AuthorizedRequest(state, HttpMethod.Get, "/v2/sync/status");
        using var response = await Client.SendAsync(request, cancellationToken);
        using var json = await ReadJsonAsync(response, cancellationToken);
        EnsureSuccess(response, json.RootElement);
        return json.RootElement.GetProperty("revision").GetInt64();
    }

    internal static async Task<CloudDownload> DownloadAsync(CloudAccountState state, CancellationToken cancellationToken)
    {
        using var request = AuthorizedRequest(state, HttpMethod.Get, "/v2/sync/snapshot");
        using var response = await Client.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, cancellationToken);
        if (response.StatusCode == HttpStatusCode.NotFound) throw new InvalidOperationException("云端尚无副本");
        if (!response.IsSuccessStatusCode) throw new InvalidOperationException($"云端数据下载失败（HTTP {(int)response.StatusCode}）");
        var encrypted = await response.Content.ReadAsByteArrayAsync(cancellationToken);
        if (!response.Headers.TryGetValues("X-Payki-Revision", out var headers) || !long.TryParse(headers.First(), out var revision))
            throw new InvalidOperationException("云端未返回数据版本，请重试");
        return new CloudDownload(DesktopSnapshotDocument.FromArchiveBytes(SnapshotCrypto.Decrypt(encrypted, state.EncryptionSecret)), revision);
    }

    internal static async Task<CloudAccountState> EnsureAccessTokenAsync(CloudAccountState state, CancellationToken cancellationToken)
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        if (state.LocalAccessLocked || state.RefreshExpiresAt <= now || string.IsNullOrWhiteSpace(state.RefreshToken)) throw new CloudSessionExpiredException();
        if (!string.IsNullOrWhiteSpace(state.AccessToken) && state.AccessExpiresAt > now + 30) return state;
        var payload = JsonSerializer.SerializeToUtf8Bytes(new { refreshToken = state.RefreshToken });
        using var request = new HttpRequestMessage(HttpMethod.Post, state.ServerUrl + "/v2/auth/refresh");
        request.Content = new ByteArrayContent(payload);
        request.Content.Headers.ContentType = new MediaTypeHeaderValue("application/json");
        using var response = await Client.SendAsync(request, cancellationToken);
        using var json = await ReadJsonAsync(response, cancellationToken);
        EnsureSuccess(response, json.RootElement);
        return state with
        {
            AccessToken = json.RootElement.GetProperty("accessToken").GetString() ?? "",
            RefreshToken = json.RootElement.GetProperty("refreshToken").GetString() ?? "",
            AccessExpiresAt = json.RootElement.GetProperty("accessExpiresAt").GetInt64(),
            RefreshExpiresAt = json.RootElement.GetProperty("refreshExpiresAt").GetInt64()
        };
    }

    private static HttpRequestMessage AuthorizedRequest(CloudAccountState state, HttpMethod method, string path)
    {
        var request = new HttpRequestMessage(method, state.ServerUrl.TrimEnd('/') + path);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", state.AccessToken);
        request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        return request;
    }

    private static async Task<JsonDocument> ReadJsonAsync(HttpResponseMessage response, CancellationToken cancellationToken)
    {
        var bytes = await response.Content.ReadAsByteArrayAsync(cancellationToken);
        return bytes.Length == 0 ? JsonDocument.Parse("{}") : JsonDocument.Parse(bytes);
    }

    private static void EnsureSuccess(HttpResponseMessage response, JsonElement body)
    {
        if (response.IsSuccessStatusCode) return;
        if (response.StatusCode == HttpStatusCode.Unauthorized) throw new CloudSessionExpiredException();
        var message = body.TryGetProperty("message", out var value) ? value.GetString() : null;
        throw new InvalidOperationException(string.IsNullOrWhiteSpace(message) ? $"云服务请求失败（HTTP {(int)response.StatusCode}）" : message);
    }

    public static string NormalizeServerUrl(string value)
    {
        var normalized = value.Trim().TrimEnd('/');
        if (!Uri.TryCreate(normalized, UriKind.Absolute, out var uri) ||
            (uri.Scheme != Uri.UriSchemeHttps && uri.Host is not ("127.0.0.1" or "localhost")))
            throw new InvalidOperationException("云同步服务器必须使用 HTTPS 地址");
        return normalized;
    }

    internal static string DeriveAccountSecret(string username, string password)
    {
        var material = Encoding.UTF8.GetBytes($"PaykiTodo Account Sync v2\0{username.Trim().ToLowerInvariant()}\0{password}");
        return Convert.ToBase64String(SHA256.HashData(material)).TrimEnd('=').Replace('+', '-').Replace('/', '_');
    }
}

internal static class SnapshotCrypto
{
    private static readonly byte[] Magic = Encoding.ASCII.GetBytes("PKSYNC1");
    private static readonly byte[] Salt = Encoding.UTF8.GetBytes("PaykiTodo Remote Sync v1");

    public static byte[] Encrypt(byte[] payload, string secret)
    {
        var iv = RandomNumberGenerator.GetBytes(12);
        var ciphertext = new byte[payload.Length];
        var tag = new byte[16];
        using var aes = new AesGcm(DeriveKey(secret), 16);
        aes.Encrypt(iv, payload, ciphertext, tag);
        return [.. Magic, 1, .. iv, .. ciphertext, .. tag];
    }

    public static byte[] Decrypt(byte[] envelope, string secret)
    {
        if (envelope.Length <= Magic.Length + 1 + 12 + 16 ||
            !envelope.AsSpan(0, Magic.Length).SequenceEqual(Magic) || envelope[Magic.Length] != 1)
            throw new InvalidOperationException("云端快照格式无效");
        var ivOffset = Magic.Length + 1;
        var encryptedLength = envelope.Length - ivOffset - 12 - 16;
        var plaintext = new byte[encryptedLength];
        try
        {
            using var aes = new AesGcm(DeriveKey(secret), 16);
            aes.Decrypt(
                envelope.AsSpan(ivOffset, 12),
                envelope.AsSpan(ivOffset + 12, encryptedLength),
                envelope.AsSpan(envelope.Length - 16, 16),
                plaintext
            );
            return plaintext;
        }
        catch (CryptographicException error)
        {
            throw new InvalidOperationException("账号密码不匹配或云端数据已损坏", error);
        }
    }

    private static byte[] DeriveKey(string secret)
    {
        return Rfc2898DeriveBytes.Pbkdf2(secret, Salt, 120_000, HashAlgorithmName.SHA256, 32);
    }
}

internal sealed class DesktopSecureStore
{
    private static readonly byte[] Entropy = Encoding.UTF8.GetBytes("PaykiTodo Desktop local data v1");
    private readonly string statePath;
    private readonly string snapshotPath;
    private readonly string workspacePath;

    public DesktopSecureStore(string? storageDirectory = null)
    {
        var directory = storageDirectory ?? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PaykiTodo");
        Directory.CreateDirectory(directory);
        statePath = Path.Combine(directory, "cloud-account.bin");
        snapshotPath = Path.Combine(directory, "cloud-snapshot.bin");
        workspacePath = Path.Combine(directory, "cloud-workspace.bin");
    }

    public void Save(CloudAccountState state, DesktopSnapshotDocument document)
    {
        AtomicWrite(workspacePath, Protect(JsonSerializer.SerializeToUtf8Bytes(new PersistedWorkspace(state, document.ToArchiveBytes()))));
    }

    public (CloudAccountState State, DesktopSnapshotDocument Document)? Load()
    {
        try
        {
            if (File.Exists(workspacePath))
            {
                var saved = JsonSerializer.Deserialize<PersistedWorkspace>(Unprotect(File.ReadAllBytes(workspacePath)))
                    ?? throw new InvalidOperationException("本地账号数据无效");
                return (saved.State, DesktopSnapshotDocument.FromArchiveBytes(saved.Archive));
            }
            if (!File.Exists(statePath) || !File.Exists(snapshotPath)) return null;
            var state = JsonSerializer.Deserialize<CloudAccountState>(Unprotect(File.ReadAllBytes(statePath)));
            if (state is null) return null;
            var document = DesktopSnapshotDocument.FromArchiveBytes(Unprotect(File.ReadAllBytes(snapshotPath)));
            return (state, document);
        }
        catch
        {
            return null;
        }
    }

    public DesktopSnapshotDocument? LoadDocument()
    {
        try
        {
            if (File.Exists(workspacePath)) return Load()?.Document;
            return File.Exists(snapshotPath)
                ? DesktopSnapshotDocument.FromArchiveBytes(Unprotect(File.ReadAllBytes(snapshotPath)))
                : null;
        }
        catch
        {
            return null;
        }
    }

    public void SaveDocument(DesktopSnapshotDocument document)
    {
        AtomicWrite(snapshotPath, Protect(document.ToArchiveBytes()));
    }

    private string SafetyDirectory()
    {
        var username = Load()?.State.Username ?? "local";
        var identity = Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(username)));
        return Path.Combine(Path.GetDirectoryName(snapshotPath)!, "safety-copies", identity);
    }

    public void SaveSafetyCopy(DesktopSnapshotDocument document, string source = "local")
    {
        var folder = SafetyDirectory();
        Directory.CreateDirectory(folder);
        AtomicWrite(Path.Combine(folder, DateTime.UtcNow.ToString("yyyyMMdd-HHmmss-fffffff") + "-" + source + "-" + Guid.NewGuid().ToString("N") + ".bin"), Protect(document.ToArchiveBytes()));
    }

    public byte[] LatestSafetyCopy()
    {
        var folder = SafetyDirectory();
        var path = Directory.Exists(folder) ? Directory.GetFiles(folder, "*-local-*.bin").OrderByDescending(File.GetLastWriteTimeUtc).FirstOrDefault() : null;
        return path == null ? throw new InvalidOperationException("尚无操作前恢复副本") : Unprotect(File.ReadAllBytes(path));
    }

    private static void AtomicWrite(string path, byte[] bytes)
    {
        var temporary = path + ".tmp";
        File.WriteAllBytes(temporary, bytes);
        File.Move(temporary, path, overwrite: true);
    }

    public void ClearAccount()
    {
        var saved = Load();
        if (saved != null) SaveDocument(saved.Value.Document);
        if (File.Exists(workspacePath)) File.Delete(workspacePath);
        if (File.Exists(statePath)) File.Delete(statePath);
    }

    public void Clear()
    {
        if (File.Exists(workspacePath)) File.Delete(workspacePath);
        if (File.Exists(statePath)) File.Delete(statePath);
        if (File.Exists(snapshotPath)) File.Delete(snapshotPath);
    }

    private static byte[] Protect(byte[] value) => ProtectedData.Protect(value, Entropy, DataProtectionScope.CurrentUser);
    private static byte[] Unprotect(byte[] value) => ProtectedData.Unprotect(value, Entropy, DataProtectionScope.CurrentUser);
    private sealed record PersistedWorkspace(CloudAccountState State, byte[] Archive);
}
