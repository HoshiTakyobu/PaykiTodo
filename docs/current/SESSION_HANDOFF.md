# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.8.1` / `versionCode 184`.
- Expected debug APK path after the final build: `app/build/outputs/apk/debug/PaykiTodo-1.8.1-debug.apk`.
- Latest completed verification in this round:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat :app:compileDebugKotlin`
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`
- This round repairs the `1.8.0` Planning Desk AI-recognition UX and adds board announcements plus an Android today-todo widget.
- Do not push to GitHub unless the user explicitly asks.

## Latest Changes In 1.8.1

1. Upgraded app version metadata to `1.8.1` / `versionCode 184`.
2. Confirmed the phone Planning Desk auto-save / mapping sync paths do not call AI recognition; recognition is still triggered from the `识别` button.
3. Desktop Planning Desk import no longer calls `parsePlanningEditor()` implicitly when there is no preview result. It now asks the user to click `识别` first.
4. Phone Planning Desk unsaved-state feedback no longer uses a progress bar; it uses plain `自动保存中` text so newline/typing feedback does not look like an AI request.
5. `PlanningAiCaller` now exposes `testProvider`, a short-timeout single-provider OpenAI-compatible ping.
6. Settings -> `AI 调用配置` provider edit dialog now has `测试连接`, disabled while testing, and reports green success or red HTTP/network failure.
7. `docs/current/AI_RECOGNITION_VERIFICATION.md` records the phone and desktop AI call chains and the expected UI messages for AI success, fallback, disabled, and incomplete config.
8. Settings -> `公告设置` persists announcement text and inclusive start/end dates.
9. Daily board shows an orange `Campaign` announcement banner above the greeting card when today is inside the configured range; long text uses marquee.
10. Android launcher widget support was added:
    - `TodoWidgetProvider`
    - `TodoWidgetService`
    - `widget_todo.xml`
    - `widget_todo_item.xml`
    - `widget_todo_info.xml`
    - widget drawables
    - manifest receiver/service registration
11. `TodoRepository` accepts a mutation callback and `TodoApplication` wires it to `TodoWidgetProvider.notifyWidgetDataChanged`, so widget data refreshes after common todo create/update/delete/complete/cancel operations.
12. Launch screen delay changed from `1600ms` to `600ms`.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/todoalarm/TodoApplication.kt`
- `app/src/main/java/com/example/todoalarm/data/AppSettingsStore.kt`
- `app/src/main/java/com/example/todoalarm/data/BackupManager.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningAiCaller.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetProvider.kt`
- `app/src/main/java/com/example/todoalarm/widget/TodoWidgetService.kt`
- `app/src/main/res/layout/widget_todo.xml`
- `app/src/main/res/layout/widget_todo_item.xml`
- `app/src/main/res/xml/widget_todo_info.xml`
- `docs/current/AI_RECOGNITION_VERIFICATION.md`
- `CHANGELOG.md`
- `README.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`

## Current Verification Focus

1. Install `PaykiTodo-1.8.1-debug.apk`.
2. Verify Planning Desk editing/newline does not trigger AI; only `识别` / desktop `Ctrl+Enter` does.
3. Verify Settings AI provider `测试连接` success, HTTP error, timeout/unreachable provider, and result clearing behavior.
4. Verify announcement active/future/expired/long-text/clear behavior.
5. Verify launcher widget add/list/empty/tap/refresh behavior on a real Android launcher.
6. Re-run the existing `1.8.0` Planning Desk mapping loop device checks because `1.8.1` carries that baseline forward.

## Deferred Larger Work

- Planning Desk remains an import + tracked refresh/sync model, not a fully live bidirectional rich editor.
- Drag-and-drop planning, Gantt chart, AI auto-planning, complex project tree, and deeper desktop parity remain deferred.
- Widget deep links to a specific todo are not implemented; first version opens the app.
- Announcement uses ISO date text fields in Settings; a dedicated wheel/date picker can be added later if needed.

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
