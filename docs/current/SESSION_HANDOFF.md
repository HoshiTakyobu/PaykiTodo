# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.32` / `versionCode 104`
- Latest debug APK path: `app/build/outputs/apk/debug/PaykiTodo-1.6.32-debug.apk`
- Minimal verification passed:
  - `./gradlew assembleDebug`
- Latest repair round addressed daily-board missed todos and delete confirmation polish:
  1. Daily-board todo block now includes missed active todos as well as today's normal todos
  2. Shared phone-side delete confirmation is now a refined dangerous-action bottom sheet with red icon, irreversible-action text, message card, red confirm button, and cancel button
- The previous `1.6.31` round remains included:
  1. Daily board no longer shows today's timed events after their end time
  2. Daily board highlights currently running events with a gold outline and subtle glow
  3. Calendar timeline pending new-event card can be canceled by long-pressing blank timeline space
  4. Opening an existing event clears the pending new-event card
- The earlier `1.6.30` round remains included:
  1. Daily-board todo preview uses unified bottom-sheet style and should not mark items complete when backing out
  2. User-visible delete buttons require confirmation in the touched phone UI paths and desktop web console delete paths
  3. Calendar event reminder acknowledgement preserves configured reminder offsets
  4. Desktop-sync foreground notification opens Settings -> Desktop Sync

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/ui/DashboardChrome.kt`
- `app/src/main/java/com/example/todoalarm/ui/EditorBottomSheet.kt`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`

## UI Consistency Note

As of this baseline, simple preview and editor surfaces are mostly aligned around the shared bottom-sheet language:

- todo preview uses `PaykiBottomSheet`
- calendar event preview uses `PaykiBottomSheet`
- todo editor uses `EditorBottomSheet`
- calendar event editor uses `EditorBottomSheet`
- delete confirmations use the refined shared `PaykiDecisionBottomSheet`

They are not byte-for-byte identical because todo and calendar event fields differ, but the main interaction and visual container language is unified. Some secondary dialogs still use `AlertDialog`, for example batch import help, recurrence previews, and wheel picker dialogs.

## Smallest Safe Next Step

The next session should device-test rather than immediately refactor:

1. Install `PaykiTodo-1.6.32-debug.apk`
2. Verify missed active todos appear in the daily-board todo block
3. Verify delete confirmation sheet appearance on todo / event / group / schedule-template deletion
4. Regression-test finished timed events disappearing from the daily board after end time
5. Regression-test currently running board events with the gold highlight
6. Regression-test calendar pending new-event cancel behavior

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
