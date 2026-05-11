# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.30` / `versionCode 102`
- The repository has been committed and pushed to `origin/main`
- Latest code commit before this doc synchronization: `2d13b04` (`修复待办预览与同步通知跳转`)
- Latest debug APK path: `app/build/outputs/apk/debug/PaykiTodo-1.6.30-debug.apk`
- Minimal verification passed:
  - `./gradlew assembleDebug`
- The latest completed repair round addressed four user-facing issues:
  1. Daily-board todo preview now uses the unified bottom-sheet style and should not mark the todo complete when backing out of preview
  2. User-visible delete buttons now require confirmation in the phone UI paths touched this round and in the desktop web console delete paths
  3. Calendar event reminder acknowledgement preserves configured reminder offsets so previews still show reminder setup after all reminders fire
  4. The desktop-sync foreground notification can be tapped to open the in-app Settings -> Desktop Sync panel

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/ui/TodoCards.kt`
- `app/src/main/java/com/example/todoalarm/ui/CalendarPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/GroupManagementPanel.kt`
- `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
- `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncService.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncWebAssets.kt`
- `app/src/main/java/com/example/todoalarm/ui/MainActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardScreen.kt`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/ui/EditorBottomSheet.kt`

## Smallest Safe Next Step

The next session should not start by rewriting architecture. The smallest safe next step is device verification:

1. Install `PaykiTodo-1.6.30-debug.apk`
2. In daily board, tap a current todo body, close preview, verify it is not completed
3. Delete a todo / event / group / schedule template and verify confirmation appears first
4. Enable desktop sync, tap the persistent notification, verify it opens Settings -> Desktop Sync
5. Trigger or acknowledge an event reminder, reopen event preview, verify reminder offsets are still shown

## Required Reading For A New Session

1. `AGENTS.md`
2. `docs/current/PROJECT_INTENT.md`
3. `docs/current/PROJECT_STATUS.md`
4. `docs/current/FEATURE_LEDGER.md`
5. `docs/current/CURRENT_TASK.md`
6. `docs/current/PAYKITODO_SESSION_LEDGER.md`

## Recommended First Output From A New Session

Before editing code, the new session should output:

1. a 5-10 line summary of project background
2. a 5-10 line summary of current repository state
3. the smallest next coding step it plans to take

## Update Rule

If a session substantially changes project direction, active task focus, or the list of in-progress work, it should update this file and the other `docs/current/` files before ending.
