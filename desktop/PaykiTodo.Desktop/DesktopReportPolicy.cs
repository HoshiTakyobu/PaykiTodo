using System.Text.Json.Nodes;

namespace PaykiTodo.Desktop;

internal static class DesktopReportPolicy
{
    internal const long DailyLifetimeMillis = 168L * 60 * 60 * 1000;
    internal static bool IsExpired(JsonObject report, long now) =>
        report["type"]?.GetValue<string>() == "DAILY" &&
        (report["generatedAtMillis"]?.GetValue<long>() ?? 0) <= now - DailyLifetimeMillis;

    internal static int RemoveExpired(JsonObject data, long now, bool includeWeekly = false)
    {
        if (data["aiReports"] is not JsonArray reports) return 0;
        var days = data["settings"]?["aiReportRetention"]?.GetValue<string>() switch {
            "DAYS_30" => 30, "DAYS_365" => 365, "FOREVER" => 0, _ => 90
        };
        var expired = reports.OfType<JsonObject>().Where(report => IsExpired(report, now) ||
            (includeWeekly && days > 0 && report["type"]?.GetValue<string>() == "WEEKLY" &&
             (report["generatedAtMillis"]?.GetValue<long>() ?? 0) <= now - days * 86_400_000L)).ToArray();
        foreach (var report in expired) reports.Remove(report);
        return expired.Length;
    }
}

internal sealed partial class DesktopSnapshotDocument
{
    internal bool ReportsPrunedOnRead { get; private set; }
    internal long? NextDailyReportExpiry => root["aiReports"]!.AsArray().OfType<JsonObject>()
        .Where(report => report["type"]?.GetValue<string>() == "DAILY")
        .Select(report => (long?)((report["generatedAtMillis"]?.GetValue<long>() ?? 0) + DesktopReportPolicy.DailyLifetimeMillis))
        .Min();

    internal int PurgeExpiredReports(long? now = null, bool includeWeekly = false) =>
        DesktopReportPolicy.RemoveExpired(root, now ?? DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(), includeWeekly);
}

internal sealed partial class DesktopWorkspace
{
    internal void ExpireReports()
    {
        if (Document == null || (!Document.ReportsPrunedOnRead &&
            !(Document.NextDailyReportExpiry <= DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()))) return;
        var candidate = DesktopSnapshotDocument.FromArchiveBytes(Document.ToArchiveBytes());
        candidate.PurgeExpiredReports();
        // Expiry is deterministic housekeeping. Keep existing pending edits and
        // let ordinary fingerprint/baseline checks propagate it without a fake edit.
        if (Account != null) store.Save(Account, candidate); else store.SaveDocument(candidate);
        Document = candidate;
        Changed?.Invoke();
    }
}
