# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.8.5` / `versionCode 188`.
- Latest debug APK output after final build: `app/build/outputs/apk/debug/PaykiTodo-1.8.5-debug.apk`.
- Latest verification in this round:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
  - `git diff --check`
- This round implements phone-side AI Provider model discovery in Settings -> `AI 调用配置`.
- Do not push to GitHub unless the user explicitly asks.

## Latest Changes In 1.8.5

1. Upgraded app version metadata to `1.8.5` / `versionCode 188`.
2. `PlanningAiCaller` now exposes a model-list fetch path for OpenAI-compatible `/models` endpoints.
3. Model endpoint fallback supports service roots, `/v1`, full `/chat/completions`, and full `/models` Base URLs.
4. Model responses parse standard `data[].id` values and compatible top-level / `models` arrays.
5. Model fetch errors are user-readable for invalid keys / permissions, unsupported endpoints, HTML or non-JSON responses, and incompatible JSON formats.
6. The phone AI source dialog now has a `获取模型` action enabled after Base URL and API Key are filled.
7. Successfully fetched models appear in a compact dropdown, and the first model is selected automatically when the current model is blank or stale.
8. Manual model-name entry remains available, so providers without `/models` can still be configured and tested.
9. Existing `测试连接` behavior remains tied to the currently selected / typed model.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/data/PlanningAiCaller.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/test/java/com/example/todoalarm/data/PlanningAiCallerTest.kt`
- `CHANGELOG.md`
- `README.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`
- `docs/current/PLANNING_AI_ASSISTANT_DESIGN.md`
- `docs/current/AI_RECOGNITION_VERIFICATION.md`
- `app/src/main/assets/wiki/index.html`

## Current Verification Focus

1. Build `PaykiTodo-1.8.5-debug.apk`.
2. Device-test Settings -> `AI 调用配置` with real providers:
   - service root -> `/v1/models`
   - `/v1` -> `/v1/models`
   - full `/chat/completions` -> sibling `/models`
   - full `/models` exact endpoint
   - invalid API Key / 403
   - unsupported `/models` / 404
   - HTML or non-JSON Base URL response
   - dropdown model selection followed by `测试连接`
   - manual model entry after model fetch failure
3. Re-check the carried `1.8.4` surfaces after installing the new APK:
   - desktop web announcement marquee and system dark mode
   - widget row deep links and single empty state
   - data-ready launch hiding

## Deferred Larger Work

- Planning Desk remains an import + tracked refresh/sync model, not a fully live bidirectional rich editor.
- Drag-and-drop planning, Gantt chart, AI auto-planning, complex project tree, and deeper desktop parity remain deferred.
- Provider model discovery is implemented for OpenAI-compatible `/models`; some third-party gateways may still block or customize model-list endpoints, so manual model entry remains intentionally supported.

## Required Reading For A New Session

1. `AGENTS.md`
2. `docs/current/PROJECT_INTENT.md`
3. `docs/current/PROJECT_STATUS.md`
4. `docs/current/FEATURE_LEDGER.md`
5. `docs/current/CURRENT_TASK.md`
6. `docs/current/UI_DESIGN_RULES.md`
7. `docs/current/PLANNING_DESK_DESIGN.md`
8. `docs/current/DESKTOP_WEB_ARCHITECTURE.md`
9. `docs/current/PAYKITODO_SESSION_LEDGER.md`
10. `docs/current/AI_RECOGNITION_VERIFICATION.md`

## Update Rule

If a session substantially changes project direction, active task focus, or the list of in-progress work, it should update this file and the other `docs/current/` files before ending.
