# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is now being advanced to `1.9.9` / `versionCode 203`.
- Main user request: desktop Web should show the current daily board state instead of feeling like a backend-only management console; desktop Planning Desk AI recognized `16:05-18:00 入党表格填写` but produced an accidental `入党` group and then imported 0 items.
- Core behavior change: desktop Web first tab is now `每日看板`, backed by a new `/api/snapshot.todayBoard` payload derived from the same phone-side `DailyBoardSnapshotBuilder`; Planning Desk AI import accepts AI candidate IDs and keeps group names explicit-only.
- Latest debug APK target after packaging: `app/build/outputs/apk/debug/PaykiTodo-1.9.9-debug.apk`.
- Do not push to GitHub unless the user explicitly asks.

## Latest 1.9.9 Desktop Board / Planning AI Pass

1. `DesktopSyncSnapshot` now includes `todayBoard`.
2. `DesktopSyncCoordinator.buildSnapshot()` builds `DailyBoardSnapshotBuilder.build(...)` and adds today focus minutes/session counts.
3. Desktop Web first tab label changed from `待办时间轴` to `每日看板`.
4. Desktop Web daily board renders:
   - current date
   - current/next item card
   - today focus stats
   - today todo card
   - today schedule card
   - tomorrow schedule card / empty planning jump
   - in-progress gold treatment
   - old full todo timeline below the board
5. Desktop Web `jump-today` scrolls to the daily board on the todo tab.
6. `PlanningAiRecognizer` prompt and parser now preserve AI `groupName` only when the source line explicitly contains group markers such as `#group`, `分组：`, `项目：`, or `课程：`.
7. The example `16:05-18:00 入党表格填写` should remain title `入党表格填写` with empty group.
8. Desktop Planning Desk import converts AI JSON candidates directly if no fallback local-parser candidate matches the candidate id, fixing the `识别 1 条 -> 导入 0 条` path.
9. Added / updated `PlanningAiRecognizerTest` coverage for the explicit group-name guard.
10. README, TODO, CHANGELOG, Wiki, and current docs were synchronized for `1.9.9`.

## Verification Status

Completed:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat testDebugUnitTest` passed.
4. `./gradlew.bat assembleDebug` passed and produced `app/build/outputs/apk/debug/PaykiTodo-1.9.9-debug.apk`.
5. `git diff --check` passed.
6. `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=203`, `versionName=1.9.9`, and `outputFile=PaykiTodo-1.9.9-debug.apk`.

Remaining after local completion:

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.9-debug.apk` on the user's physical phone.
2. Enable desktop sync, refresh the browser console, and verify the first tab shows the daily board, not only the old todo timeline.
3. In desktop Planning Desk, test AI recognition/import for `16:05-18:00 入党表格填写`.
4. Verify `AI 报告` archive behavior from `1.9.8` did not regress.
5. Do not push unless the user explicitly asks.

## Files Most Relevant To This Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncModels.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/data/PlanningAiRecognizer.kt`
- `app/src/test/java/com/example/todoalarm/data/PlanningAiRecognizerTest.kt`
- `app/src/main/assets/desktop-web/index.html`
- `app/src/main/assets/desktop-web/app.js`
- `app/src/main/assets/desktop-web/app.css`
- `app/src/main/assets/wiki/index.html`
- `README.md`
- `TODO.md`
- `CHANGELOG.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`

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

This avoids confusion when an emulator window remains open on the user's desktop.

Latest recorded emulator use remains the `1.9.8` smoke check:

- Device id: `emulator-5554`
- Installed APK: `app/build/outputs/apk/debug/PaykiTodo-1.9.8-debug.apk`
- Checked flows: app launch, drawer navigation to `AI 报告`, report detail opening, Settings -> AI 调用配置, and `了解 AI 日报`
- Verified result: `AI 报告` archive is reachable and populated from legacy migration data; the `了解 AI 日报` help surface is centered/readable on the emulator
- Boundary: this emulator pass does not replace real-phone verification for OEM notification, vibration, lock-screen, widget, alarm, reboot, battery-management behavior, or live desktop-browser verification of `1.9.9`
