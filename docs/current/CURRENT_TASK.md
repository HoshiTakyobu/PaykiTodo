# Current Task

## Active Development Focus

Active goal: implement `docs/goals/2026-05-20-paykitodo-outliner-ux-fix-goal.md`.

The working tree is now on the `1.12.10 / versionCode 237` line. This round is a phone-side Planning Desk Outliner UX fix: make the editor less like a form/list manager and more like a notes app, keep only leaf nodes synced to official todos/events, add an Outliner edit/preview switch, simplify the phone overflow menu, and rewrite the phone tutorial. Do not push to GitHub unless the user explicitly asks.

## Current Goal State

Current implementation state:

1. Phone Outliner now shows lightweight text rows plus a root active input line; expanded child areas show their own active input lines.
2. Phone Outliner has an edit / preview switch; edit mode hides per-row right-side operations, while preview mode exposes row `⋯` actions for time, location, sync toggle, delete, and opening the linked official todo/event editor.
3. Adding a child demotes its parent to a structure heading and deletes the parent's linked official item; moving/deleting the last child can restore ordinary parents as synced leaf items.
4. Natural schedule parsing accepts bare ordered locations such as `时间, 事件名, 地点`.
5. Main phone overflow menu is simplified to document-level actions; Markdown import/export moved to the Markdown compatibility toolbar.
6. Phone Planning Desk tutorial now has three Outliner-focused pages.
7. Preview `⋯` reuses the existing official todo/event editor through the linked item, so reminder, group, recurrence, notes, countdown, and event check-in use the same surface as ordinary phone editing.

## Verification Completed For 1.12.10

The `1.12.10 / versionCode 237` patch has passed:

1. `./gradlew.bat :app:compileDebugKotlin`
2. `./gradlew.bat :app:testDebugUnitTest`
3. `git diff --check`
4. `./gradlew.bat :app:assembleDebug`
5. Debug APK metadata confirms:
   - `versionName = 1.12.10`
   - `versionCode = 237`
   - output `PaykiTodo-1.12.10-debug.apk`

Latest debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.12.10-debug.apk`.

## Previous Verification Completed For 1.12.9

The `1.12.9 / versionCode 236` patch has passed:

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `git diff --check`
3. `./gradlew.bat :app:compileDebugKotlin`
4. `./gradlew.bat :app:testDebugUnitTest`
5. `./gradlew.bat :app:assembleDebug`
6. Debug APK metadata confirms:
   - `versionName = 1.12.9`
   - `versionCode = 236`
   - output `PaykiTodo-1.12.9-debug.apk`

Follow-up audit on `Pixel_8 / emulator-5554` confirmed the system-share text capture path can write real data:

- Installed/running APK metadata still reports `versionName = 1.12.9`, `versionCode = 236`.
- Explicit `ACTION_SEND text/plain` to `ShareReceiverActivity` with quoted text `2030-05-21 15:00-16:00 ShareAuditQuoted-... @Library3` produced a capture notification saying `已添加 1 条到规划台`.
- Pulling `databases/todo-alarm.db` through `run-as` showed one `planning_nodes` row and one linked `todo_items` `EVENT` row with title `ShareAuditQuoted-...`, location `@Library3`, and the expected 2030-05-21 start/end timestamps.
- The earlier unquoted adb command that produced `捕获识别失败：未能识别出待办或日程` was a test-command false negative: adb shell split the text at spaces, so the app received an incomplete extra instead of the intended schedule line.
- Added a unit regression test for bare shared schedule lines such as `2030-05-21 15:00-16:00 ShareAudit @Library3`.
- Follow-up validation passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:testDebugUnitTest`, and `git diff --check`.

## Remaining Work

The `2026-05-20-paykitodo-outliner-ux-fix-goal.md` goal is not fully closed yet.

1. Phone runtime QA is still needed for active input focus behavior, IME Enter behavior, child input expansion, linked-item editor routing, and parent demotion/restoration with real data.
2. Real migrated-user-data testing is still needed for old Markdown headings and existing parent/child nodes that already have linked official items.
3. If this goal continues, the next slice should focus on emulator/manual verification rather than known code gaps.

## Git / Release Notes

- Branch remains `main`, ahead of `origin/main`; do not push without explicit user authorization.
- `docs/goals/2026-05-20-paykitodo-outliner-ux-fix-goal.md` is the active goal file and is currently untracked until the feature work is complete enough to archive.
- Local signing files, APK outputs, API keys, tokens, and private Base URLs must remain out of Git.
