using System.IO;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class AndroidSnapshotCrossClientTests
{
    [AndroidFixtureFact]
    public async Task AndroidCompletionDownloadsIntoIndependentWindowsWorkspace()
    {
        var username = Environment.GetEnvironmentVariable("PAYKI_ANDROID_FIXTURE_ACCOUNT")!;
        var folder = Path.Combine(Environment.GetEnvironmentVariable("PAYKI_UI_EVIDENCE") ?? Path.GetTempPath(), "android-bridge-" + Guid.NewGuid().ToString("N"));
        var store = new DesktopSecureStore(folder);
        var workspace = new DesktopWorkspace(store);
        await workspace.LoginAsync("http://127.0.0.1:19444", username, "local-fixture-password-1234", false, null);
        Assert.NotNull(workspace.Document);
        Assert.True(workspace.Document.Items.Single(item => item.Title == "responsive completion").Completed);
        Assert.False(workspace.Document.Items.Single(item => item.Title == "held network write").Completed);
        Assert.True(new DesktopWorkspace(store).Document!.Items.Single(item => item.Title == "responsive completion").Completed);
        Assert.False(workspace.Account!.Dirty);
        File.WriteAllText(Path.Combine(folder, "result.txt"), "Android local completion -> isolated server -> Windows local persisted completion: PASS");
    }
}

public sealed class AndroidFixtureFactAttribute : FactAttribute
{
    public AndroidFixtureFactAttribute()
    {
        if (string.IsNullOrEmpty(Environment.GetEnvironmentVariable("PAYKI_ANDROID_FIXTURE_ACCOUNT")))
            Skip = "Requires the completed Android localhost sync fixture; no production account is used.";
    }
}
