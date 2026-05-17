# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.9.11` / `versionCode 205`.
- Main user request: support no-DDL tasks as today tasks, stop desktop sync automatically if no desktop connects within 5 minutes, explain non-`192.168.*` sync addresses, and make the AI provider configuration screen less cramped.
- Core behavior change: active no-DDL todos are part of `今日待办`; Planning Desk plain bullets can become no-DDL todos; desktop sync auto-disables after 5 minutes without an authorized API connection; AI provider rows use summary cards with a more menu.
- Latest debug APK target after packaging: `app/build/outputs/apk/debug/PaykiTodo-1.9.11-debug.apk`.
- Do not push to GitHub unless the user explicitly asks.

## Latest 1.9.11 No-DDL / Desktop Sync / AI Config Pass

1. `TodoViewModel` now puts active no-DDL todos into `todayItems` and excludes them from `upcomingItems`.
2. `DailyBoardSnapshotBuilder` includes active no-DDL todos in `todayTodos`, so phone board, widget snapshot, and desktop daily board share the same behavior.
3. Desktop Web todo management places no-DDL active todos under `今日待办` and removes the separate `未设置 DDL` section.
4. `PlanningMarkdownParser` recognizes plain bullet lines such as `- 想办的事`, `* 整理材料`, and `• 发消息` as no-DDL todo candidates.
5. Parser tests now cover plain-bullet no-DDL todos.
6. `DailyBoardSnapshotBuilderTest` covers no-DDL todos appearing in the daily-board todo list.
7. `DesktopSyncCoordinator` records the first authorized API request as a real desktop connection.
8. `DesktopSyncService` starts a 5-minute no-client timer when sync starts; if no authorized client connects, it disables desktop sync and stops the service / server.
9. Desktop sync IP addresses are sorted with likely Wi-Fi `192.168.*` addresses first.
10. Settings -> `电脑同步` now explains that `10.*` / `172.*` entries can come from VPN, hotspot, virtual adapters, or other network interfaces; `localhost` is `127.0.0.1`, not those addresses.
11. Settings -> `AI 调用配置` provider rows now show name, Base URL, model, completion status, enable switch, and a compact more menu for edit / reorder / delete.
12. Wiki / README / TODO / CHANGELOG / current docs were synchronized for `1.9.11`.

## Verification Status

Completed so far in this round:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat testDebugUnitTest` passed.
4. `./gradlew.bat assembleDebug` passed and produced `app/build/outputs/apk/debug/PaykiTodo-1.9.11-debug.apk`.
5. `git diff --check` passed.
6. `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=205`, `versionName=1.9.11`, and `outputFile=PaykiTodo-1.9.11-debug.apk`.

Remaining after local completion:

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.11-debug.apk` on the user's physical phone.
2. Verify a normal no-DDL todo appears under `今日待办`, does not enable reminders, and does not enable recurrence.
3. Verify Planning Desk plain bullets import as no-DDL todos and appear under `今日待办`.
4. Verify Android widget and desktop daily board also show no-DDL todos in today's todo block.
5. Verify desktop sync auto-disables after 5 minutes if no authorized desktop connection occurs.
6. Verify desktop sync stays enabled if the desktop browser connects with the correct token within 5 minutes.
7. Verify Settings -> AI 调用配置 provider cards are readable on a narrow phone screen and the more menu exposes edit / up / down / delete.
8. Do not push unless the user explicitly asks.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `app/src/main/java/com/example/todoalarm/data/DailyBoardSnapshot.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningMarkdownParser.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncService.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/assets/wiki/index.html`
- `app/src/test/java/com/example/todoalarm/data/PlanningMarkdownParserTest.kt`
- `app/src/test/java/com/example/todoalarm/data/DailyBoardSnapshotBuilderTest.kt`
- `README.md`
- `TODO.md`
- `CHANGELOG.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/PLANNING_AI_ASSISTANT_DESIGN.md`

## Known Worktree Notes

- Branch is `main`; local branch is far ahead of origin. Do not push without user authorization.
- Existing untracked temp UI dumps such as `.tmp-*.xml` were present before this round and should not be committed unless intentionally needed.
- An untracked user note named `当前使用中存在的问题.md` may exist in the repo root; do not commit or modify it unless the user asks.
- Goal prompt files under `docs/goals/` should remain separate archive commits after secret scanning.

## Android Emulator Verification Rule

If an Android Emulator is started or reused, record:

- device id / AVD name
- installed APK path
- checked screens or flows
- what was actually verified

This avoids confusion when an emulator window remains on the user's desktop.

Latest recorded emulator use remains the `1.9.8` smoke check:

- Device id: `emulator-5554`
- Installed APK: `app/build/outputs/apk/debug/PaykiTodo-1.9.8-debug.apk`
- Checked flows: app launch, drawer navigation to `AI 报告`, report detail opening, Settings -> AI 调用配置, and `了解 AI 日报`
- Verified result: `AI 报告` archive is reachable and populated from legacy migration data; the `了解 AI 日报` help surface is centered/readable on the emulator
- Boundary: this emulator pass does not replace real-phone verification for OEM notification, vibration, lock-screen, widget, alarm, reboot, battery-management behavior, or live desktop-browser verification of `1.9.11`
