using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace PaykiTodo.Desktop;

internal sealed record DesktopAiResult(string Content, string ProviderName);

internal static class DesktopAi
{
    private static readonly HttpClient Client = new() { Timeout = TimeSpan.FromSeconds(90) };
    public static IReadOnlyList<JsonObject> Providers(DesktopSnapshotDocument document) =>
        document.Settings["planningAiProviders"] is JsonArray providers ? providers.OfType<JsonObject>().ToList() : new List<JsonObject>();
    public static async Task<DesktopAiResult> CallAsync(IEnumerable<JsonObject> providers, string system, string prompt, CancellationToken cancellationToken = default)
    {
        var failures = new List<string>();
        foreach (var provider in providers.Where(provider => provider["enabled"]?.GetValue<bool>() != false)) {
            var name = provider["name"]?.GetValue<string>() ?? "AI 服务";
            var key = provider["apiKey"]?.GetValue<string>() ?? ""; var model = provider["model"]?.GetValue<string>() ?? "";
            if (key.Length == 0 || model.Length == 0) continue;
            try {
                var url = Endpoint(provider, "chat/completions");
                using var request = new HttpRequestMessage(HttpMethod.Post, url);
                request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", key);
                request.Content = new StringContent(JsonSerializer.Serialize(new { model, stream = false,
                    messages = new[] { new { role = "system", content = system }, new { role = "user", content = prompt } } }), Encoding.UTF8, "application/json");
                using var response = await Client.SendAsync(request, cancellationToken);
                if (!response.IsSuccessStatusCode) { failures.Add(name + "（HTTP " + (int)response.StatusCode + "）"); continue; }
                using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync(cancellationToken));
                var text = json.RootElement.GetProperty("choices")[0].GetProperty("message").GetProperty("content").GetString();
                if (string.IsNullOrWhiteSpace(text)) throw new InvalidOperationException("响应为空");
                return new DesktopAiResult(text, name);
            } catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested) { throw; }
            catch (Exception) { failures.Add(name + "暂时不可用"); }
        }
        throw new InvalidOperationException(failures.Count == 0 ? "请先在设置中配置可用的 AI 服务、密钥和模型。" : "AI 请求未完成：" + string.Join("；", failures));
    }
    public static async Task<string[]> ModelsAsync(JsonObject provider)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, Endpoint(provider, "models"));
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", provider["apiKey"]?.GetValue<string>() ?? "");
        using var response = await Client.SendAsync(request);
        if (!response.IsSuccessStatusCode) throw new InvalidOperationException("模型列表暂时不可用（HTTP " + (int)response.StatusCode + "）");
        using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync());
        return json.RootElement.GetProperty("data").EnumerateArray().Select(item => item.GetProperty("id").GetString() ?? "").Where(id => id.Length > 0).Distinct().Order().ToArray();
    }
    private static string Endpoint(JsonObject provider, string route)
    {
        var url = CloudAccountSync.NormalizeServerUrl(provider["baseUrl"]?.GetValue<string>() ?? "");
        if (url.EndsWith("/chat/completions", StringComparison.OrdinalIgnoreCase)) url = url[..^17];
        if (url.EndsWith("/models", StringComparison.OrdinalIgnoreCase)) url = url[..^7];
        if (!url.EndsWith("/v1", StringComparison.OrdinalIgnoreCase) && new Uri(url).AbsolutePath == "/") url += "/v1";
        return url.TrimEnd('/') + "/" + route;
    }
    public static IReadOnlyList<DesktopItemEdit> ParseCandidates(string response, long groupId)
    {
        var first = response.IndexOf('['); var last = response.LastIndexOf(']');
        if (first < 0 || last < first) throw new InvalidOperationException("识别结果不是事项列表，请重试或手动录入。");
        using var json = JsonDocument.Parse(response[first..(last + 1)]); var result = new List<DesktopItemEdit>();
        foreach (var value in json.RootElement.EnumerateArray().Take(200)) {
            string Read(string key) => value.TryGetProperty(key, out var field) && field.ValueKind == JsonValueKind.String ? field.GetString() ?? "" : "";
            long? Date(string key) => DateTimeOffset.TryParse(Read(key), out var date) ? date.ToUnixTimeMilliseconds() : null;
            var title = Read("title").Trim(); if (title.Length == 0) continue;
            if (!Read("type").Equals("event", StringComparison.OrdinalIgnoreCase) && !Read("type").Equals("todo", StringComparison.OrdinalIgnoreCase)) continue;
            var isEvent = Read("type").Equals("event", StringComparison.OrdinalIgnoreCase);
            var start = Date("start"); var end = Date("end");
            if (isEvent && (start == null || end == null || end <= start)) continue;
            result.Add(new DesktopItemEdit(isEvent, title, Read("notes"), Read("location"), Date("due"), start, end,
                false, false, Array.Empty<int>(), groupId, "#4CB782", false, "NONE", "", null));
        }
        if (result.Count == 0) throw new InvalidOperationException("没有识别出可导入的事项，请补充标题或日程起止时间。");
        return result;
    }
}

internal sealed partial class DesktopSnapshotDocument
{
    public void SaveAiProviders(IEnumerable<JsonObject> providers)
    {
        Settings["planningAiProviders"] = new JsonArray(providers.Select(provider => provider.DeepClone()).ToArray());
        Settings["planningAiEnabled"] = true;
    }
    public void AddReport(string content, string provider, bool weekly)
    {
        var reports = root["aiReports"]!.AsArray(); var now = DateTimeOffset.Now;
        reports.Add(new JsonObject { ["id"] = NextId(reports), ["type"] = weekly ? "WEEKLY" : "DAILY", ["generatedAtMillis"] = now.ToUnixTimeMilliseconds(),
            ["periodStartMillis"] = new DateTimeOffset(DateTime.Today.AddDays(weekly ? -6 : 0)).ToUnixTimeMilliseconds(),
            ["periodEndMillis"] = now.ToUnixTimeMilliseconds(), ["content"] = content, ["providerName"] = provider, ["isLocalFallback"] = false });
    }
}
