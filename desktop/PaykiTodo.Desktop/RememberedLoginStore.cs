using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace PaykiTodo.Desktop;

internal sealed record RememberedLogin(string Username = "", string Password = "", bool RememberAccount = false, bool RememberPassword = false);

internal sealed class RememberedLoginStore
{
    private readonly string path;
    private static readonly byte[] Entropy = Encoding.UTF8.GetBytes("PaykiTodo remembered login v1");
    public RememberedLoginStore(string? directory = null)
    {
        directory ??= Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PaykiTodo");
        Directory.CreateDirectory(directory); path = Path.Combine(directory, "remembered-login.bin");
    }
    public RememberedLogin Load()
    {
        try { return JsonSerializer.Deserialize<RememberedLogin>(ProtectedData.Unprotect(File.ReadAllBytes(path), Entropy, DataProtectionScope.CurrentUser)) ?? new(); }
        catch { return new(); }
    }
    public void Save(string username, string password, bool account, bool secret)
    {
        var data = new RememberedLogin(account ? username.Trim() : "", account && secret ? password : "", account, account && secret);
        var temporary = path + ".tmp";
        File.WriteAllBytes(temporary, ProtectedData.Protect(JsonSerializer.SerializeToUtf8Bytes(data), Entropy, DataProtectionScope.CurrentUser));
        File.Move(temporary, path, true);
    }
    public void ForgetPassword() { var saved = Load(); Save(saved.Username, "", saved.RememberAccount, false); }
}
