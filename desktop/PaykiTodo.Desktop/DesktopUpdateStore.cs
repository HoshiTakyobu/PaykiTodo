using System.Text;
using System.Text.Json;

namespace PaykiTodo.Desktop;

internal sealed record UpdateState(string InstallId, string? Active = null, string? Pending = null, string? Previous = null, string? Attempt = null, string? Nonce = null, string? Failed = null, long LastCheck = 0);
internal sealed record UpdateLaunch(string Directory, string? Version, string? Nonce);

internal sealed class DesktopUpdateStore
{
    internal string Root { get; }
    internal string Seed { get; }
    internal DesktopUpdateStore(string seed, string? updateRoot = null)
    {
        Seed = Path.GetFullPath(seed);
        Root = updateRoot ?? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PaykiTodo", "updates", DesktopUpdateProtocol.Hash(Encoding.UTF8.GetBytes(Seed.ToUpperInvariant()))[..24]);
        Directory.CreateDirectory(Root); DesktopUpdateProtocol.Child(Root, "state.json");
    }
    internal UpdateState Read() => File.Exists(StatePath) ? JsonSerializer.Deserialize<UpdateState>(File.ReadAllBytes(StatePath), DesktopUpdateProtocol.Json) ?? throw new InvalidDataException("更新状态损坏") : new(Guid.NewGuid().ToString("N"));
    private string StatePath => DesktopUpdateProtocol.Child(Root, "state.json");
    internal T Change<T>(Func<UpdateState, (UpdateState State, T Result)> change)
    {
        using var gate = new Mutex(false, "Local\\PaykiTodoUpdateState-" + DesktopUpdateProtocol.Hash(Encoding.UTF8.GetBytes(Root.ToUpperInvariant()))[..24]);
        try { try { if (!gate.WaitOne(TimeSpan.FromSeconds(10))) throw new IOException("更新状态正忙"); } catch (AbandonedMutexException) { }
            var result = change(Read()); var temporary = StatePath + "." + Guid.NewGuid().ToString("N");
            File.WriteAllBytes(temporary, JsonSerializer.SerializeToUtf8Bytes(result.State, DesktopUpdateProtocol.Json)); File.Move(temporary, StatePath, true); return result.Result;
        } finally { try { gate.ReleaseMutex(); } catch (ApplicationException) { } }
    }
    internal string VersionDirectory(string version) { DesktopUpdateProtocol.ParseVersion(version); return DesktopUpdateProtocol.Child(Root, "versions/" + version); }
    internal UpdateLaunch BeginLaunch(Version seedVersion, string publicKey)
    {
        return Change(state => {
            if (state.Attempt != null) state = state with { Failed = state.Attempt, Pending = null, Active = state.Previous, Attempt = null, Nonce = null };
            var selected = state.Pending ?? state.Active;
            if (selected == null || selected == state.Failed || DesktopUpdateProtocol.ParseVersion(selected) <= seedVersion) return (state with { Active = null, Pending = null }, new UpdateLaunch(Seed, null, null));
            try {
                var dir = VersionDirectory(selected); var m = DesktopUpdateProtocol.Verify(File.ReadAllBytes(DesktopUpdateProtocol.Child(dir, "update-manifest.json")), publicKey);
                if (m.Version != selected) throw new InvalidDataException("更新目录版本不匹配");
                DesktopUpdateProtocol.VerifyDirectory(dir, m);
                var nonce = Guid.NewGuid().ToString("N");
                return (state with { Previous = selected != state.Active ? state.Active : state.Previous, Attempt = selected, Nonce = nonce }, new UpdateLaunch(dir, selected, nonce));
            } catch (Exception e) when (e is IOException or InvalidDataException or System.Security.Cryptography.CryptographicException or FormatException or JsonException) {
                return (state with { Failed = selected, Pending = null, Active = null }, new UpdateLaunch(Seed, null, null));
            }
        });
    }
    internal void Confirm(string nonce) => Change(state => state.Nonce == nonce && state.Attempt != null ? (state with { Active = state.Attempt, Pending = null, Attempt = null, Nonce = null }, true) : (state, false));
    internal void Fail(string nonce) => Change(state => state.Nonce == nonce ? (state with { Failed = state.Attempt, Active = state.Previous, Pending = null, Attempt = null, Nonce = null }, true) : (state, false));
}
