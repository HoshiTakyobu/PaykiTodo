# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Do not push to GitHub unless the user explicitly authorizes it.
- Current code version:
  - `versionName = 1.12.11`
  - `versionCode = 238`
- Latest debug APK built in this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.12.11-debug.apk`
- Debug APK metadata confirms:
  - `versionName = 1.12.11`
  - `versionCode = 238`
  - output `PaykiTodo-1.12.11-debug.apk`

## Active Goal

Active goal file:

- `docs/goals/2026-05-20-paykitodo-outliner-ux-fix-goal.md`

Goal status from code audit: implemented in code, with emulator runtime QA partially completed. The phone Planning Desk Outliner now has lighter note-like rows, root / child active input lines, existing-row Enter sibling input, edit / preview mode, simplified overflow, ordered bare-location parsing, leaf-only linked-item synchronization, and preview `⋯` routing to the existing official todo/event editor for full field configuration.

## What Changed In The Latest 1.12.11 Patch

1. Existing-node edit mode now treats Enter as "continue writing here": it commits the current node, opens a same-level input row directly below it, and focuses that input for the next node.
2. Same-level insertion passes an explicit sort order so the new node is inserted at the current position instead of being appended at the end.
3. Existing-node edit commit is guarded so Enter, IME Done, and focus loss do not process the same edit twice.
4. Child input expansion on nodes without children now focuses the child input and uses matching expand/collapse icon semantics.
5. Preview menus for parents with children no longer offer a misleading sync toggle; they show `有子任务时保持结构标题`.
6. Version metadata moved to `1.12.11 / versionCode 238`.

## What Changed In The Latest 1.12.10 Patch

1. Phone Planning Desk Outliner edit mode now renders existing nodes as lightweight text rows rather than per-row card-like text fields.
2. The root outline and expanded child areas expose active input lines; pressing Enter / IME Done creates a node and clears the input.
3. Preview mode exposes per-row `⋯` actions for time, location, sync toggle, delete, and opening the linked official todo/event editor, while edit mode keeps row actions hidden.
4. Adding children demotes the parent node into a structure heading and deletes its linked official item; ordinary parents can be restored as leaf synced items after the last child is removed.
5. Natural schedule parsing accepts ordered bare locations such as `15:00-17:00, 写论文, 图书馆3楼`.
6. The phone Planning Desk overflow menu is document-focused; Markdown import/export moved to Markdown compatibility mode.
7. Phone tutorial pages now match the Outliner workflow.
8. Version metadata moved to `1.12.10 / versionCode 237`.

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

Passed so far after the 1.12.11 patch:

1. `./gradlew.bat :app:compileDebugKotlin`
2. `./gradlew.bat :app:testDebugUnitTest`
3. `node --check app/src/main/assets/desktop-web/app.js`
4. `git diff --check`
5. `./gradlew.bat :app:assembleDebug`
6. Debug APK metadata inspection: `versionName = 1.12.11`, `versionCode = 238`, output `PaykiTodo-1.12.11-debug.apk`
7. Note: one parallel Gradle run triggered a Kotlin incremental-cache race while another Gradle task was compiling; rerunning the same commands sequentially passed.
8. Emulator `emulator-5554` runtime audit on installed `1.12.11` confirmed the target phone Planning Desk UX:
   - package metadata reports `versionName = 1.12.11`, `versionCode = 238`.
   - drawer navigation opens `规划台`.
   - edit mode shows the note-like hint, existing rows with expand / completion controls, and the active input placeholder `继续写下一行，按回车创建`.
   - typing `GoalAudit1512` into the active input and pressing Enter creates a normal node and keeps the next active input focused.
   - the main overflow menu contains `新建文档`, `重命名`, `使用说明`, `归档`, and `删除文档`, with no image-recognition or Markdown import/export actions in that menu.
   - `使用说明` opens the three-page `规划台新手教程`.
   - preview mode changes the toolbar button to `编辑`, hides active input rows, shows preview-mode copy, and exposes per-row `节点设置`.
   - a parent row with children shows disabled `有子任务时保持结构标题` in its node menu.
9. Emulator `emulator-5554` audit before the code patch confirmed the runtime parent/leaf data behavior:
   - `ParentAudit144457` with child is `syncEnabled = 0` and has no official linked todo.
   - `ChildAudit144457` / `ChildAuditComplete237` are leaf synced official todos.
   - deleting the last child restored `ParentAudit144457` as a synced leaf todo.
   - completing `ChildAuditComplete237` propagated completion to its parent node and linked official todo.

Passed after the 1.12.10 patch:

1. `./gradlew.bat :app:compileDebugKotlin`
2. `./gradlew.bat :app:testDebugUnitTest`
3. `git diff --check`
4. `./gradlew.bat :app:assembleDebug`
5. Debug APK metadata inspection: `versionName = 1.12.10`, `versionCode = 237`, output `PaykiTodo-1.12.10-debug.apk`

Emulator smoke test on `emulator-5554`:

1. Installed `app/build/outputs/apk/debug/PaykiTodo-1.12.10-debug.apk` with `adb install -r`.
2. Launched `com.example.todoalarm.ui.MainActivity` under package `com.paykitodo.app`.
3. Navigated from drawer to `规划台`; UI dump showed the Outliner toolbar with `Markdown` and `预览`, the note-like hint text, and the active input placeholder `继续写下一行，按回车创建`.
4. Switched to preview mode; UI dump showed the toolbar button changed to `编辑`, the root input disappeared, and the row exposed `节点设置`.
5. Opened row `⋯`; UI dump showed `完整编辑`, `设置时间`, `设置地点`, `改为结构标题`, and `删除`.
6. Tapped `完整编辑`; UI dump showed the existing `编辑日程` sheet with `保存`, proving the linked official event editor opens from preview mode.

Passed after the 1.12.9 patch:

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `git diff --check`
3. `./gradlew.bat :app:compileDebugKotlin`
4. `./gradlew.bat :app:testDebugUnitTest`
5. `./gradlew.bat :app:assembleDebug`
6. Debug APK metadata inspection: `versionName = 1.12.9`, `versionCode = 236`, output `PaykiTodo-1.12.9-debug.apk`

Known warnings are existing Kotlin / Android deprecation or unused-parameter warnings, not build failures.

Follow-up audit performed on `Pixel_8 / emulator-5554`:

1. Confirmed installed package metadata on emulator: `versionName = 1.12.9`, `versionCode = 236`.
2. Reused `app/build/outputs/apk/debug/PaykiTodo-1.12.9-debug.apk`; no new APK was assembled in the audit-only pass.
3. Explicit `ACTION_SEND text/plain` to `com.example.todoalarm.ui.ShareReceiverActivity` with remote-quoted text `2030-05-21 15:00-16:00 ShareAuditQuoted-... @Library3` produced a notification title `已添加 1 条到规划台`.
4. Pulling `databases/todo-alarm.db` with `adb exec-out run-as com.paykitodo.app` showed:
   - `planning_nodes`: one node with title `ShareAuditQuoted-...`, location `@Library3`, expected start/end millis, and `linkedTodoId = 1`.
   - `todo_items`: one linked `EVENT` row with the same title/location and matching start/end timestamps.
5. The earlier `捕获识别失败：未能识别出待办或日程` observation came from an unquoted adb command that split the intended text at spaces; it was a test-command false negative, not a verified app defect.
6. Added a unit regression test for bare shared schedule text: `2030-05-21 15:00-16:00 ShareAudit @Library3`.
7. Follow-up validation passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:testDebugUnitTest`, and `git diff --check`.

## Remaining QA

No known code requirement from the active goal is intentionally left unimplemented. Remaining work is runtime QA:

1. Real-device testing for Android share targets, launcher shortcuts, camera capture, voice recognition, and Android 13+ notification-permission behavior.
2. Real-device testing for notification delivery and OEM background restrictions.
3. Real-browser testing for desktop Web Planning Desk node editing, up/down reorder, same-level drag reorder, document switching, and node time-field display/editing against live phone data.
4. Real upgraded-database testing with existing Planning Desk Markdown data, especially structure-heading preservation, old sync-enabled node linked-item repair on startup, and official-item delete/cancel detachment for Outliner-created items.

## Git State Notes

- Worktree should be clean after the 1.12.11 Outliner UX implementation and emulator-verification commits.
- Branch is ahead of `origin/main`; do not push without explicit user authorization.
- `docs/goals/2026-05-20-paykitodo-outliner-ux-fix-goal.md` is tracked in Git and was checked for common secret markers before closing the goal.
- Do not commit local signing material, APK outputs, API keys, tokens, or private Base URLs.
