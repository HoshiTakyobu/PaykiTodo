# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Do not push to GitHub unless the user explicitly authorizes it.
- Current code version:
  - `versionName = 1.12.9`
  - `versionCode = 236`
- Latest debug APK built in this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.12.9-debug.apk`
- Debug APK metadata confirms:
  - `versionName = 1.12.9`
  - `versionCode = 236`
  - output `PaykiTodo-1.12.9-debug.apk`

## Active Goal

Active goal file:

- `docs/goals/2026-05-19-paykitodo-capture-and-outliner-goal.md`

Goal status from code audit: the quick-capture + Planning Desk Outliner deliverables are implemented in the working tree. The audit-found migrated-heading risk is fixed in `1.12.7`, the official-item delete/cancel lifecycle risk is fixed in `1.12.8`, and the desktop Web same-level drag-reorder browser compatibility risk is fixed in `1.12.9`. Do not mark the goal complete unless the final checklist audit in the active session confirms every explicit goal item has concrete evidence.

## What Changed In The Latest 1.12.9 Patch

1. Desktop Web Planning Desk Outliner same-level drag reorder no longer depends on reading `dataTransfer` during `dragover`.
2. The browser now records the current dragged node id in page state, so drop-target highlighting and placement work in browsers that restrict drag data reads before `drop`.
3. Drag reorder remains limited to nodes under the same parent; cross-level drag is blocked to avoid accidental hierarchy changes.
4. The existing up / down reorder buttons remain available as a precise fallback.
5. Database version stays `22`; no new Room schema was introduced.
6. Version metadata moved to `1.12.9 / versionCode 236`.

## What Changed In The Previous 1.12.8 Patch

1. Planning Desk Outliner linked-item deletion is now safe:
   - Deleting a formal todo / event created from an Outliner node detaches the node from official sync.
   - Startup repair will not recreate a formal item that the user intentionally deleted outside the Planning Desk.
2. Canceling a formal todo / event created from an Outliner node also detaches the node from official sync.
3. If an Outliner-created event has an optional event-end DDL linked todo, deleting or canceling the event handles that linked todo as well.
4. Phone ViewModel and desktop sync delete paths now receive the full deleted-item list so reminder artifacts are cleared for both the primary item and any linked end-DDL todo.
5. Database version stays `22`; no new Room schema was introduced.
6. Version metadata moved to `1.12.8 / versionCode 235`.

## What Changed In The Previous 1.12.7 Patch

1. Planning Desk Outliner nodes now store `syncEnabled`:
   - Ordinary nodes still create linked official todos/events.
   - Structure headings can remain in the outline without being repaired into no-DDL todos.
2. Database version moved to `22`:
   - `MIGRATION_21_22` adds `planning_nodes.syncEnabled`.
   - Common migrated headings such as `今日计划` / `收集箱` are marked as non-sync nodes.
   - If those headings were already auto-linked to no-DDL todos during `1.12.6`, only the linked auto rows are cleared.
3. Backup / restore and desktop sync preserve `syncEnabled`.
4. Desktop Web Planning Desk shows non-sync nodes as `结构标题` and exposes a compact `同步为待办/日程` checkbox.
5. Version metadata moved to `1.12.7 / versionCode 234`.

## What Changed In The Previous 1.12.6 Patch

1. Desktop Web Planning Desk Outliner time fields no longer render raw epoch milliseconds:
   - Node DDL / start / end summaries and inline inputs now prefer the backend `dueAt` / `startAt` / `endAt` strings.
   - If a string value is missing, the browser formats the millisecond value before display.
2. Desktop Web Planning Desk document switching now reloads the selected document's Outliner nodes:
   - Changing the document selector calls `/api/planning/nodes?noteId=...` before rendering.
   - This avoids showing the previous document's tree under the newly selected document title.
3. Version metadata moved to `1.12.6 / versionCode 233`.

## What Changed In The Previous 1.12.5 Patch

1. Background capture toast copy is notification-permission aware:
   - If Android notification permission is available, copy remains `正在后台识别，稍后通知`.
   - If Android 13+ notification permission is missing, share / photo / voice / Planning Desk capture now tells the user to check Planning Desk later instead of promising a notification that cannot be posted.
2. Phone Planning Desk Markdown compatibility capture is now explicit:
   - The toolbar button text is `捕获`.
   - Tapping it opens a confirmation dialog explaining that the current Markdown text will be written as new Outliner nodes and linked official todos/events.
   - This reduces accidental duplicate creation when the text was exported from the current Outliner.

## Goal Implementation Evidence Summary

### Quick Capture

- System share:
  - `app/src/main/java/com/example/todoalarm/ui/ShareReceiverActivity.kt`
  - `app/src/main/AndroidManifest.xml`
- Photo shortcut:
  - `app/src/main/java/com/example/todoalarm/ui/CaptureActivity.kt`
  - `app/src/main/res/xml/shortcuts.xml`
  - FileProvider declaration and paths in manifest / XML
- Voice shortcut:
  - `app/src/main/java/com/example/todoalarm/ui/VoiceCaptureActivity.kt`
  - `RECORD_AUDIO` permission in manifest
- Background processing:
  - `app/src/main/java/com/example/todoalarm/ui/BackgroundCaptureProcessor.kt`
  - `app/src/main/java/com/example/todoalarm/ui/CapturePlanningPipeline.kt`

### Planning Desk Outliner

- Data model / schema:
  - `app/src/main/java/com/example/todoalarm/data/PlanningNode.kt`
  - `app/src/main/java/com/example/todoalarm/data/PlanningNodeDraft.kt`
  - `app/src/main/java/com/example/todoalarm/data/AppDatabase.kt`
  - `app/src/main/java/com/example/todoalarm/data/DatabaseMigrations.kt`
  - `app/schemas/com.example.todoalarm.data.AppDatabase/21.json`
  - `app/schemas/com.example.todoalarm.data.AppDatabase/22.json`
- Repository / synchronization:
  - `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
  - `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
  - `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- Phone UI:
  - `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`
  - `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- Desktop sync / Web UI:
  - `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
  - `app/src/main/assets/desktop-web/app.js`
  - `app/src/main/assets/desktop-web/app.css`

## Verification Completed

Passed after the 1.12.9 patch:

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `git diff --check`
3. `./gradlew.bat :app:compileDebugKotlin`
4. `./gradlew.bat :app:testDebugUnitTest`
5. `./gradlew.bat :app:assembleDebug`
6. Debug APK metadata inspection: `versionName = 1.12.9`, `versionCode = 236`, output `PaykiTodo-1.12.9-debug.apk`

Known warnings are existing Kotlin / Android deprecation or unused-parameter warnings, not build failures.

## Remaining QA

No known code requirement from the active goal is intentionally left unimplemented. Remaining work is runtime QA:

1. Real-device testing for Android share targets, launcher shortcuts, camera capture, voice recognition, and Android 13+ notification-permission behavior.
2. Real-device testing for notification delivery and OEM background restrictions.
3. Real-browser testing for desktop Web Planning Desk node editing, up/down reorder, same-level drag reorder, document switching, and node time-field display/editing against live phone data.
4. Real upgraded-database testing with existing Planning Desk Markdown data, especially structure-heading preservation, old sync-enabled node linked-item repair on startup, and official-item delete/cancel detachment for Outliner-created items.

## Git State Notes

- Worktree is dirty.
- Branch is ahead of `origin/main`.
- `docs/goals/2026-05-19-paykitodo-capture-and-outliner-goal.md` is untracked and should be committed only as a separate goal-archive commit after the feature commit, assuming the final secret check finds no private material.
- Do not commit local signing material, APK outputs, API keys, tokens, or private Base URLs.
