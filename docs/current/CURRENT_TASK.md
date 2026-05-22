# Current Task

## Active Development Focus

Active immediate task: metadata-only rebuild to `1.13.7 / versionCode 255` so Android can install over the existing `1.13.6` debug build.

The broader widget UX goal file `docs/goals/2026-05-22-paykitodo-widget-ux-overhaul-goal.md` exists locally but is not part of this metadata-only rebuild. Do not push to GitHub unless the user explicitly asks.

## Verification Completed For 1.13.7

The `1.13.7 / versionCode 255` rebuild is intended to let Android upgrade over the installed `1.13.6` debug build.

1. Version metadata moved from `1.13.6 / versionCode 254` to `1.13.7 / versionCode 255`.
2. No additional database schema, reminder behavior, Planning Desk behavior, Widget behavior, or user-data format change was intentionally introduced by this version bump.
3. The generated debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.13.7-debug.apk`.
4. `git diff --check` passed.
5. `./gradlew.bat :app:assembleDebug` passed.
6. Debug APK metadata confirms:
   - `versionName = 1.13.7`
   - `versionCode = 255`
   - output `PaykiTodo-1.13.7-debug.apk`

## Verification Completed For 1.13.6

The `1.13.6 / versionCode 254` rebuild is intended to let Android upgrade over the installed `1.13.5` debug build.

1. Version metadata moved from `1.13.5 / versionCode 253` to `1.13.6 / versionCode 254`.
2. Database version moved to `25` for `alarmMode` on todos and recurring templates; this supersedes the UX-only goal's older "keep database 24" note.
3. The generated debug APK is `app/build/outputs/apk/debug/PaykiTodo-1.13.6-debug.apk`.
4. Implemented and compile-verified:
   - phone Planning Desk same-parent long-press drag reorder with visual lift and placement animation;
   - Planning Desk Outliner snapshot undo stack for edit/delete/merge/reorder/publish operations;
   - future recurring todo folding in My Tasks;
   - flatter high-contrast reminder page with larger title/time/actions and alarm-mode pulse;
   - ongoing event notification scheduling/cancel channel;
   - alarm mode persistence, playback loop, timeout downgrade, retry bursts, backup/sync/schema fields;
   - daily brief notification scheduling/settings/backup fields;
   - global search over todos/events/planning nodes/AI reports with result routing;
   - data health scan and safe-only confirmed cleanup.
5. `./gradlew.bat :app:compileDebugKotlin` passed after final code changes.
6. `git diff --check` passed.
7. `./gradlew.bat :app:assembleDebug` passed.
8. Debug APK metadata confirms:
   - `versionName = 1.13.6`
   - `versionCode = 254`
   - output `PaykiTodo-1.13.6-debug.apk`

## Verification Completed For 1.13.5

The `1.13.5 / versionCode 253` rebuild is intended to let Android upgrade over the installed `1.13.4` debug build.

1. Version metadata moved from `1.13.4 / versionCode 252` to `1.13.5 / versionCode 253`.
2. No additional database schema, reminder behavior, Planning Desk behavior, or user-data format change was intentionally introduced by this version bump.
3. The generated debug APK is `app/build/outputs/apk/debug/PaykiTodo-1.13.5-debug.apk`.
4. `git diff --check` passed.
5. `./gradlew.bat :app:assembleDebug` passed.
6. Debug APK metadata confirms:
   - `versionName = 1.13.5`
   - `versionCode = 253`
   - output `PaykiTodo-1.13.5-debug.apk`

## Verification Completed For 1.13.4

The `1.13.4 / versionCode 252` rebuild is intended to let Android upgrade over the installed `1.13.3` debug build.

1. Version metadata moved from `1.13.3 / versionCode 251` to `1.13.4 / versionCode 252`.
2. No additional database schema, reminder behavior, Planning Desk behavior, or user-data format change was intentionally introduced by this version bump.
3. The existing `UpcomingTodoDisplayGroup` type was made public because public `TodoUiState` exposes it; this was required for Kotlin compilation.
4. The generated debug APK is `app/build/outputs/apk/debug/PaykiTodo-1.13.4-debug.apk`.
5. `./gradlew.bat :app:assembleDebug` passed.
6. Debug APK metadata confirms:
   - `versionName = 1.13.4`
   - `versionCode = 252`
   - output `PaykiTodo-1.13.4-debug.apk`

## Verification Completed For 1.13.3

The `1.13.3 / versionCode 251` rebuild is intended to let Android upgrade over the installed `1.13.2` debug build.

1. Version metadata moved from `1.13.2 / versionCode 250` to `1.13.3 / versionCode 251`.
2. No additional database schema, reminder behavior, Planning Desk behavior, or user-data format change was intentionally introduced by this metadata-only bump.
3. The generated debug APK is `app/build/outputs/apk/debug/PaykiTodo-1.13.3-debug.apk`.
4. `./gradlew.bat :app:assembleDebug` passed.
5. Debug APK metadata confirms:
   - `versionName = 1.13.3`
   - `versionCode = 251`
   - output `PaykiTodo-1.13.3-debug.apk`

## Current Goal State

Current implementation state:

1. #7 P0 recurring reminder crash is hardened with safe alarm scheduling, SafeStartupGuard, startup recovery try/catch, and limited recurring-instance expansion.
2. #6 editor BottomSheet uses skip-partially-expanded behavior and unsaved-change confirmation.
3. #1 countdown rows and widget deep links open item previews before editing.
4. #5 board title / empty-card areas navigate to tasks or calendar, while concrete rows keep preview behavior.
5. #3 widget board/countdown layouts use tighter padding and list spacing.
6. #4 todo editor supports `hiddenFromBoard`; hidden reminder todos still schedule reminders and remain in My Tasks, but are filtered out of board/widget/desktop-board/AI-report todo statistics.
7. #2 Planning Desk supports `isNote` nodes from `// ` / `> `, note styling, note toggle actions, no publish-to-task behavior, and completion calculations that ignore note children.
8. Database version is `24`, backup/restore and desktop-sync JSON preserve `hiddenFromBoard` and `isNote`.

## Verification Completed For 1.12.20

The `1.12.20 / versionCode 247` rebuild is intended to let Android upgrade over the installed `1.12.19` debug build.

1. Version metadata moved from `1.12.19 / versionCode 246` to `1.12.20 / versionCode 247`.
2. No database schema, reminder behavior, Planning Desk behavior, or user-data format changed in this metadata-only rebuild.
3. The generated debug APK is `app/build/outputs/apk/debug/PaykiTodo-1.12.20-debug.apk`.
4. `./gradlew.bat :app:assembleDebug` passed.
5. `git diff --check` passed.
6. Debug APK metadata confirms:
   - `versionName = 1.12.20`
   - `versionCode = 247`
   - output `PaykiTodo-1.12.20-debug.apk`

## Verification Completed For 1.12.19

The `1.12.19 / versionCode 246` rebuild status:

1. Version metadata moved from `1.12.18 / versionCode 245` to `1.12.19 / versionCode 246`.
2. The generated debug APK is `app/build/outputs/apk/debug/PaykiTodo-1.12.19-debug.apk`.
3. `./gradlew.bat :app:assembleDebug` output `BUILD SUCCESSFUL`.
4. `git diff --check`
5. Debug APK metadata confirms:
   - `versionName = 1.12.19`
   - `versionCode = 246`
   - output `PaykiTodo-1.12.19-debug.apk`

## Verification Completed For 1.12.18

The `1.12.18 / versionCode 245` rebuild status:

1. Version metadata moved from `1.12.17 / versionCode 244` to `1.12.18 / versionCode 245`.
2. The generated debug APK is `app/build/outputs/apk/debug/PaykiTodo-1.12.18-debug.apk`.
3. `./gradlew.bat :app:assembleDebug` output `BUILD SUCCESSFUL` before the shell timeout wrapper returned.
4. `git diff --check`
5. Debug APK metadata confirms:
   - `versionName = 1.12.18`
   - `versionCode = 245`
   - output `PaykiTodo-1.12.18-debug.apk`

## Verification Completed For 1.12.17

The `1.12.17 / versionCode 244` rebuild status:

1. Version metadata moved from `1.12.16 / versionCode 243` to `1.12.17 / versionCode 244`.
2. No database schema, reminder behavior, Planning Desk behavior, or user-data format changed in this metadata-only rebuild.
3. `./gradlew.bat :app:compileDebugKotlin`
4. `git diff --check`
5. `./gradlew.bat :app:assembleDebug`
6. Debug APK metadata confirms:
   - `versionName = 1.12.17`
   - `versionCode = 244`
   - output `PaykiTodo-1.12.17-debug.apk`

## Verification Completed For 1.12.16

The `1.12.16 / versionCode 243` rebuild status:

1. Version metadata moved from `1.12.15 / versionCode 242` to `1.12.16 / versionCode 243`.
2. No database schema, reminder behavior, Planning Desk behavior, or user-data format changed in this rebuild.
3. `./gradlew.bat :app:compileDebugKotlin`
4. `git diff --check`
5. `./gradlew.bat :app:assembleDebug`
6. Debug APK metadata confirms:
   - `versionName = 1.12.16`
   - `versionCode = 243`
   - output `PaykiTodo-1.12.16-debug.apk`

## Verification Completed For 1.12.15

The `1.12.15 / versionCode 242` patch has passed:

1. `./gradlew.bat :app:compileDebugKotlin`
2. `./gradlew.bat :app:testDebugUnitTest`
3. `node --check app/src/main/assets/desktop-web/app.js`
4. `git diff --check`
5. `./gradlew.bat :app:assembleDebug`
6. Debug APK metadata confirms:
   - `versionName = 1.12.15`
   - `versionCode = 242`
   - output `PaykiTodo-1.12.15-debug.apk`

## Verification Completed For 1.12.14

The `1.12.14 / versionCode 241` rebuild has passed:

1. `./gradlew.bat :app:compileDebugKotlin`
2. `git diff --check`
3. `./gradlew.bat :app:assembleDebug`
4. Debug APK metadata confirms:
   - `versionName = 1.12.14`
   - `versionCode = 241`
   - output `PaykiTodo-1.12.14-debug.apk`

## Verification Completed For 1.12.13

The `1.12.13 / versionCode 240` patch has passed so far:

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `./gradlew.bat :app:compileDebugKotlin`
3. `git diff --check`
4. `./gradlew.bat :app:assembleDebug`
5. Debug APK metadata confirms:
   - `versionName = 1.12.13`
   - `versionCode = 240`
   - output `PaykiTodo-1.12.13-debug.apk`

## Verification Completed For 1.12.12

The `1.12.12 / versionCode 239` rebuild has passed so far:

1. Version metadata moved from `1.12.11 / versionCode 238` to `1.12.12 / versionCode 239`.
2. No database schema, reminder behavior, Planning Desk behavior, or user-data format changed in this rebuild.
3. `./gradlew.bat :app:compileDebugKotlin`
4. `./gradlew.bat :app:testDebugUnitTest`
5. `git diff --check`
6. `./gradlew.bat :app:assembleDebug`
7. Debug APK metadata confirms:
   - `versionName = 1.12.12`
   - `versionCode = 239`
   - output `PaykiTodo-1.12.12-debug.apk`
8. Latest debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.12.12-debug.apk`.

## Verification Completed For 1.12.11

The `1.12.11 / versionCode 238` patch has passed so far:

1. Emulator `emulator-5554` runtime audit on installed `1.12.10` before the code patch confirmed the existing parent/leaf data behavior:
   - `ParentAudit144457` became `syncEnabled = 0` with no linked official item after adding a child.
   - `ChildAudit144457` / `ChildAuditComplete237` remained leaf synced official todos.
   - deleting the last child restored `ParentAudit144457` to a leaf synced official todo.
   - completing the child propagated completion to the parent node and linked official todo.
2. `./gradlew.bat :app:compileDebugKotlin`
3. `./gradlew.bat :app:testDebugUnitTest`
4. `node --check app/src/main/assets/desktop-web/app.js`
5. `git diff --check`
6. `./gradlew.bat :app:assembleDebug`
7. Debug APK metadata confirms:
   - `versionName = 1.12.11`
   - `versionCode = 238`
   - output `PaykiTodo-1.12.11-debug.apk`
8. Emulator `emulator-5554` runtime audit on installed `1.12.11` confirmed the target phone Planning Desk UX:
   - app package metadata reports `versionName = 1.12.11`, `versionCode = 238`.
   - opening drawer -> `规划台` shows the Outliner toolbar with `今日`, `Markdown`, `预览`, document list, and `更多操作`.
   - edit mode shows the note-like hint `像备忘录一样写：输入一行后按回车...`, existing rows with expand / completion controls, and the active input placeholder `继续写下一行，按回车创建`.
   - typing `GoalAudit1512` into the active input and pressing Enter created a normal outline node, while a new active input row stayed focused.
   - the main overflow menu contains only `新建文档`, `重命名`, `使用说明`, `归档`, and `删除文档`; Markdown import/export and image recognition are not in the main menu.
   - `使用说明` opens the three-page `规划台新手教程`; page 1 is `像备忘录一样一行一行写` and explains `输入 → 回车 → 变成节点`.
   - tapping `预览` switches the toolbar button to `编辑`, hides the active input row, shows preview-mode copy, and exposes per-row `节点设置`.
   - opening a parent row's `节点设置` menu shows `有子任务时保持结构标题` as a disabled item rather than `同步为待办/日程`.

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

Latest debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.12.15-debug.apk`.

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

## Remaining QA

No known code requirement from `docs/goals/2026-05-20-paykitodo-outliner-ux-fix-goal.md` is intentionally left unimplemented. Remaining work is runtime QA beyond this goal's local verification scope:

1. Phone runtime QA is still needed on a physical device for active input focus behavior, IME Enter behavior, child input expansion, linked-item editor routing, and parent demotion/restoration with real data.
2. Real migrated-user-data testing is still needed for old Markdown headings and existing parent/child nodes that already have linked official items.
3. Real-browser testing is still needed for desktop Web Planning Desk node editing, up/down reorder, same-level drag reorder, document switching, and node time-field display/editing against live phone data.

## Git / Release Notes

- Branch remains `main`, ahead of `origin/main`; do not push without explicit user authorization.
- `docs/goals/2026-05-20-paykitodo-outliner-ux-fix-goal.md` is tracked in Git and was checked for common secret markers before closing the goal.
- Local signing files, APK outputs, API keys, tokens, and private Base URLs must remain out of Git.
