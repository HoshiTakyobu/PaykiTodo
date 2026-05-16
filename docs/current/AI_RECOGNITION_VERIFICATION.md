# AI Recognition Verification

## Scope

This document verifies the PaykiTodo `1.8.2` Planning Desk AI-recognition path. AI recognition must only run after an explicit recognition action, then still enter the preview-first import flow.

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
3. Tap `测试连接`; expect green success text.
   - Base URL may be a service root, a `/v1` URL, or a full `/v1/chat/completions` URL.
   - If the service returns HTML / non-JSON, expect a readable Base URL hint rather than a raw `<!doctype` JSON conversion error.
4. Open Planning Desk and write free-form planning text.
5. Tap `识别`.
6. Expect preview candidates and the AI success message. Import still requires selecting candidates and confirming import.

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
