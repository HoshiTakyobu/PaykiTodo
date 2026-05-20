# Current Task

## Active Development Focus

Active goal: implement `docs/goals/2026-05-19-paykitodo-capture-and-outliner-goal.md`.

The working tree is now on the `1.12.9 / versionCode 236` line. The quick-capture and Planning Desk Outliner goal is implemented in code. This round fixes the desktop Web Outliner same-level drag-reorder compatibility risk by tracking the dragged node id in page state instead of relying on browser-specific `dataTransfer` reads during `dragover`. The earlier `1.12.8` patch fixed the linked-item lifecycle risk where deleting or canceling an official todo / event created from an Outliner node could leave the node sync-enabled and allow startup repair to recreate an item the user intentionally removed. The earlier `1.12.7` migration fix still keeps old Markdown headings such as `# 今日计划` / `# 收集箱` as structure nodes instead of unwanted no-DDL todos. Do not push to GitHub unless the user explicitly asks.

## Current Goal State

The goal deliverables are implemented as follows:

1. System share target accepts shared text/images and routes them through background capture into Planning Desk nodes.
2. Launcher shortcuts expose photo and voice capture.
3. Background capture writes recognized results directly into Planning Desk nodes and linked official todos/events, then opens the matching planning document through notification when notification permission is available.
4. Voice capture uses Android SpeechRecognizer with zh-CN partial results and sends the final transcript to background capture.
5. `planning_nodes` is the Outliner data model; database version is `22`, with schema export `app/schemas/com.example.todoalarm.data.AppDatabase/22.json`.
6. Existing Markdown planning notes are migrated into nodes; Markdown export/import compatibility remains available, and structure headings are preserved as non-sync outline nodes instead of becoming official todos/events.
7. Nodes create linked official todos/events immediately; no-DDL nodes create no-DDL todos, DDL nodes create DDL todos, and schedule nodes create calendar events.
8. Optional event-end DDL linked todos are controlled by the Planning Desk Outliner setting and default off.
9. Node completion, todo/event completion, parent completion, sibling event/end-DDL completion, text/time/location edits, deletion, cancellation, and document deletion synchronize across planning nodes and official items.
10. Phone Outliner UI supports tree rendering, completion styling, collapse/expand, child creation, Enter / IME Next / Tab / Shift+Tab / empty Backspace operations, long-press time/location/delete actions, and hideable `时间 | 事项 | 地点` hints.
11. Desktop sync exposes planning-node APIs and desktop Web renders a node editor with inline edits, child/delete actions, completion, collapse, up/down reorder, and same-level drag reorder.
12. Markdown compatibility capture now requires explicit confirmation before direct node creation to reduce accidental duplicate imports.
13. Background-capture toasts are notification-permission aware on Android 13+.

## Verification Completed For 1.12.9

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

No known code requirement from `2026-05-19-paykitodo-capture-and-outliner-goal.md` is intentionally left incomplete. Remaining work is QA rather than implementation:

1. Real Android device testing for share targets, launcher shortcuts, camera capture, voice recognition, Android 13+ notification-permission behavior, and OEM background behavior.
2. Real desktop-browser testing for Planning Desk node editing/reorder, same-level drag reorder, document switching, and node time-field display/editing against live phone data.
3. Real migrated-user-data testing for Markdown-to-node migration, structure-heading preservation, startup linked-item repair, and delete/cancel detachment of Outliner-created official items.
4. Optional cleanup of legacy preview/import code paths that remain reusable but are no longer the default capture path.

## Git / Release Notes

- Branch remains `main`, ahead of `origin/main`; do not push without explicit user authorization.
- `docs/goals/2026-05-19-paykitodo-capture-and-outliner-goal.md` has been checked for obvious secret patterns and is being archived in a separate goal-document commit after the feature commit.
- Local signing files, APK outputs, API keys, tokens, and private Base URLs must remain out of Git.
