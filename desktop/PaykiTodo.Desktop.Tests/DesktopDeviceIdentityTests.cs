using System.IO;
using Xunit;

namespace PaykiTodo.Desktop.Tests;

public sealed class DesktopDeviceIdentityTests
{
    [Fact]
    public void InstallationDeviceIdIsStableForOneProfileAndDifferentForAnotherProfile()
    {
        var root = Path.Combine(Path.GetTempPath(), "PaykiTodo-tests", Guid.NewGuid().ToString("N"));
        var first = new DesktopSecureStore(Path.Combine(root, "first"));
        var second = new DesktopSecureStore(Path.Combine(root, "second"));

        var firstId = first.GetOrCreateInstallationDeviceId();
        Assert.Equal(firstId, first.GetOrCreateInstallationDeviceId());
        Assert.NotEqual(firstId, second.GetOrCreateInstallationDeviceId());
        Assert.Matches("^[0-9a-f]{32}$", firstId);
    }

    [Fact]
    public void CopiedWorkspaceDoesNotSupplyTheInstallationDeviceId()
    {
        var root = Path.Combine(Path.GetTempPath(), "PaykiTodo-tests", Guid.NewGuid().ToString("N"));
        var original = new DesktopSecureStore(Path.Combine(root, "original"));
        var copied = new DesktopSecureStore(Path.Combine(root, "copied"));
        var document = DesktopSnapshotDocument.CreateEmpty();
        var account = new CloudAccountState("https://example.invalid", "fixture", "legacy-copied-id", "access", "refresh", 1, 2, "secret", 0, false);
        original.Save(account, document);

        // Simulate copying only the old encrypted account/workspace files.
        Directory.CreateDirectory(Path.Combine(root, "copied"));
        File.Copy(Path.Combine(root, "original", "cloud-workspace.bin"), Path.Combine(root, "copied", "cloud-workspace.bin"));
        var copiedState = copied.Load()!.Value.State;

        Assert.Equal("legacy-copied-id", copiedState.DeviceId);
        Assert.NotEqual(copiedState.DeviceId, copied.GetOrCreateInstallationDeviceId());
    }
}
