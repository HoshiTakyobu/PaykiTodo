# AI Recognition Verification

## Scope

This document verifies the PaykiTodo `1.8.5` Planning Desk AI-recognition path. AI recognition must only run after an explicit recognition action, then still enter the preview-first import flow.

## Phone Call Chain

1. `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt:184`
   - passes `viewModel::parsePlanningMarkdown` into `DashboardScreen`.
2. `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt:329` and `:477`
   - passes `onParsePlanningMarkdown` into `DashboardBody`.
3. `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt:483`
   - passes the callback into `PlanningDeskPanel`.
4. `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt:219`
   - the visible `识别` button calls `onParse(editorValue.text)`.
5. `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt:198-199`
   - `parsePlanningMarkdown` calls `PlanningRecognitionService.recognize`.
6. `app/src/main/java/com/example/todoalarm/data/PlanningRecognitionService.kt:7-26`
   - decides AI-first or local-fallback recognition.

The phone editor auto-save effects call `onSaveNote` / `onSyncMappings` only. They do not call `onParse` or `PlanningRecognitionService.recognize`.

## Desktop Call Chain

1. `app/src/main/assets/desktop-web/app.js:900-913`
   - `parsePlanningEditor()` posts to `/api/planning/parse`.
2. `app/src/main/assets/desktop-web/app.js:1495`
   - the `识别` button calls `parsePlanningEditor()`.
3. `app/src/main/assets/desktop-web/app.js:1510-1512`
   - `Ctrl+Enter` calls `parsePlanningEditor()`.
4. `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt:123`
   - routes `POST /api/planning/parse` to `parsePlanning`.
5. `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt:355`
   - calls `PlanningRecognitionService.recognize`.

`1.8.1` removes the previous desktop import-side implicit parse: if there is no preview result, `importSelectedPlanning()` now asks the user to click `识别` first instead of calling `parsePlanningEditor()` by itself. `1.8.2` keeps that trigger rule and changes provider endpoint handling only.

## 1.8.1 Trigger Tightening

- Phone-side editing and pressing Enter never directly called AI recognition. The misleading part was the old auto-save progress indicator: it could look like a network / AI loading state. `1.8.1` replaced that with plain `自动保存中` text.
- Phone auto-save still only calls `onSaveNote`; mapping sync only calls `onSyncMappings`. Neither path calls `PlanningRecognitionService.recognize`.
- Desktop `importSelectedPlanning()` previously called `parsePlanningEditor()` when `state.planningParseResult` was absent. `1.8.1` changed this to show `请先点击“识别”生成预览，再勾选并导入`, so desktop import cannot silently trigger AI.

## 1.8.2 Base URL Compatibility

- `PlanningAiCaller.endpointCandidates` resolves provider Base URLs as follows:
  1. full `/chat/completions` URL: use directly
  2. full `/models` URL: convert to the sibling `/chat/completions`
  3. URL ending in `/v1`: append `/chat/completions`
  4. service root: try `/v1/chat/completions`, then `/chat/completions`
- `shouldTryNextEndpoint` tries the next endpoint after non-JSON responses and after HTTP `400 / 404 / 405`, which are common signs that the path shape is wrong for an OpenAI-compatible service.
- HTML responses, such as filling a site root that returns an HTML homepage, now produce a readable Base URL / endpoint hint instead of surfacing raw `<!doctype` JSON conversion errors.

## 1.8.5 Model Discovery Compatibility

- `PlanningAiCaller.modelEndpointCandidates` resolves provider Base URLs as follows:
  1. full `/models` URL: use directly
  2. full `/chat/completions` URL: convert to the sibling `/models`
  3. URL ending in `/v1`: append `/models`
  4. service root: try `/v1/models`, then `/models`
- The phone Settings -> `AI 调用配置` provider dialog can fetch models after Base URL and API Key are filled.
- Fetch success shows a compact model dropdown and defaults the model field to the first fetched model if the field is blank or not in the fetched list.
- Fetch failure does not block manual model entry.
- Expected model-list response is OpenAI-compatible `data[].id`; compatible top-level arrays / `models` arrays are also tolerated.

## Expected UI Messages

- AI success:
  - global preview result: `AI 识别：<provider name>；请在预览中确认`
  - candidate cards from AI include a small gold `AI` badge when the candidate message contains `AI`.
- AI failure fallback:
  - `AI 识别失败，已使用本地规则：<error>`
- AI disabled:
  - local rule parser runs directly, without AI wording.
- AI enabled but no complete provider:
  - `AI 配置未完整，已使用本地规则`

## Manual Test Steps

### 1. AI Success

1. Settings -> `AI 调用配置`.
2. Add one enabled provider with valid Base URL, API Key, and model.
3. If the provider supports model listing, tap `获取模型`; expect a green success message and a dropdown of model names. Select one model or accept the first default.
4. Tap `测试连接`; expect green success text.
   - Base URL may be a service root, a `/v1` URL, or a full `/v1/chat/completions` URL.
   - If the service returns HTML / non-JSON, expect a readable Base URL hint rather than a raw `<!doctype` JSON conversion error.
5. Open Planning Desk and write free-form planning text.
6. Tap `识别`.
7. Expect preview candidates and the AI success message. Import still requires selecting candidates and confirming import.

### 1A. Base URL Shape Checks

Use the same valid API Key and model, then test these chat-completion Base URL shapes in the provider dialog:

1. `https://api.deepseek.com`
   - Expect success through the automatic `/v1/chat/completions` candidate.
2. `https://api.deepseek.com/v1`
   - Expect success after appending `/chat/completions`.
3. `https://api.deepseek.com/v1/chat/completions`
   - Expect success with the exact endpoint.
4. `https://api.deepseek.com/v1/models`
   - Expect conversion to `https://api.deepseek.com/v1/chat/completions`.
5. `https://example.com`
   - Expect a friendly non-JSON / Base URL hint, not a raw JSON parse exception.

### 1B. Model Fetch Shape Checks

Use the same valid API Key, then test these Base URL shapes with `获取模型`:

1. `https://api.deepseek.com`
   - Expect `/v1/models` to be tried first.
2. `https://api.deepseek.com/v1`
   - Expect `/v1/models`.
3. `https://api.deepseek.com/v1/chat/completions`
   - Expect conversion to `https://api.deepseek.com/v1/models`.
4. `https://api.deepseek.com/v1/models`
   - Expect the exact endpoint.
5. A provider that does not expose `/models`
   - Expect a readable failure and manual model-name fallback to remain usable.

### 2. AI Failure Fallback

1. Keep AI enabled.
2. Use an invalid API Key or unreachable Base URL.
3. Tap `识别`.
4. Expect local-rule candidates where possible plus `AI 识别失败，已使用本地规则：...`.

### 3. AI Disabled

1. Settings -> `AI 调用配置`.
2. Disable AI recognition.
3. Tap Planning Desk `识别`.
4. Expect local-rule candidates and no AI wording.

### 4. AI Enabled But Config Incomplete

1. Enable AI recognition but leave all providers absent or incomplete.
2. Tap Planning Desk `识别`.
3. Expect local-rule candidates plus `AI 配置未完整，已使用本地规则`.

## Non-Auto-Trigger Check

- Phone newline/editing:
  - typing and pressing Enter updates editor state and may auto-save after debounce, but does not call recognition.
  - the old unsaved-state progress indicator was replaced by plain `自动保存中` text to avoid looking like an AI/network loading bar.
- Desktop newline/editing:
  - the editor `input` listener clears stale preview, renders local preview state, and marks the note dirty.
  - it does not call `parsePlanningEditor()`.
- Desktop import:
  - import without a parse result now shows `请先点击“识别”生成预览，再勾选并导入`.
