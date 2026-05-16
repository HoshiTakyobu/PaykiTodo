# Current Task

## Active Development Focus

The current round is PaykiTodo `1.8.5` / `versionCode 188`, focused on making Settings -> `AI 调用配置` easier to use with OpenAI-compatible providers:

1. Let the phone-side AI source dialog fetch available models from the configured Base URL using only Base URL + API Key.
2. Support common Base URL shapes for model discovery: service root, `/v1`, full `/chat/completions`, and full `/models`.
3. Show fetched models as a compact dropdown while preserving manual model-name entry as a fallback.
4. Keep test-connection behavior intact and keep AI recognition explicit-only.

## Completed In 1.8.5

1. `PlanningAiCaller.fetchModels` now calls OpenAI-compatible `/models` endpoints with the provider API Key.
2. `PlanningAiCaller.modelEndpointCandidates` resolves:
   - service root -> `/v1/models`, then `/models`
   - `/v1` -> `/v1/models`
   - full `/chat/completions` -> sibling `/models`
   - full `/models` -> exact URL
3. Model-list parsing accepts standard `data[].id` responses and simple compatible variants such as top-level arrays / `models` arrays.
4. Model fetch errors now distinguish HTTP 401/403, 404, non-JSON HTML responses, incompatible JSON, and ordinary network failures with user-readable messages.
5. The phone AI source dialog now exposes a `获取模型` button after Base URL and API Key.
6. Fetched models appear in a compact dropdown row; fetching success defaults the model field to the first returned model when the current model is blank or not in the fetched list.
7. The model-name text field remains available for providers that do not expose `/models` or for custom model aliases.
8. Version metadata is now `1.8.5` / `versionCode 188`.

## Verification Completed This Round

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `./gradlew.bat testDebugUnitTest`
3. `./gradlew.bat assembleDebug`
4. `git diff --check`

## Immediate Practical Next Steps

Before final completion, create a focused local commit with the required `完成内容概要：` bullet-list body. Do not push unless the user explicitly asks.

After installing the `1.8.5` APK on device, verify Settings -> `AI 调用配置`:

1. Valid provider with service root Base URL can fetch models through `/v1/models`.
2. `/v1` Base URL appends `/models`.
3. Full `/chat/completions` Base URL converts to the sibling `/models` endpoint.
4. Full `/models` Base URL is used directly.
5. Invalid API Key, unsupported `/models`, HTML responses, and incompatible JSON all show readable errors.
6. A fetched model can be selected from the dropdown, then `测试连接` succeeds with the selected model.
7. If model fetching fails, the user can still type a model name manually and test/save the provider.

## Commit Message Rule

PaykiTodo commit messages must use the `AGENTS.md` body format:

```text
完成内容概要：
- ...
```

The subject should describe the behavior change and must not append a version-bump tail.

## Current External Dependency

This round does not require network verification inside the coding environment; real provider `/models` behavior should be checked on device with the user's actual Base URL and API Key.
