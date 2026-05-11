# Session Handoff

## Why This File Exists

Long-running Codex sessions can become unreliable. This file exists so a new session can resume useful work from repository facts instead of broken transcript memory.

## Current Handoff Summary

- The project is currently at code version `1.6.33` / `versionCode 105`
- Latest debug APK path: `app/build/outputs/apk/debug/PaykiTodo-1.6.33-debug.apk`
- Minimal verification passed:
  - `./gradlew assembleDebug`
- Latest repair round addressed Wiki navigation, settings direct actions, desktop all-day rendering, and launcher foreground wiring:
  1. In-app Wiki sidebar navigation now works because local WebView JavaScript is enabled
  2. Settings -> 使用说明 opens the Wiki directly
  3. Settings -> 提示音 opens the system notification-tone picker directly
  4. Desktop web all-day events spanning multiple days render as one horizontal continuous bar
  5. Adaptive launcher icon foreground points to the safe-zone vector `ic_payki_mark` instead of the full raster launcher art
- Previous `1.6.32` round remains included:
  1. Daily-board todo block includes missed active todos as well as today's normal todos
  2. Shared phone-side delete confirmation is a refined dangerous-action bottom sheet
- Previous `1.6.31` round remains included:
  1. Daily board no longer shows today's timed events after their end time
  2. Daily board highlights currently running events with a gold outline and subtle glow
  3. Calendar timeline pending new-event card can be canceled by long-pressing blank timeline space
  4. Opening an existing event clears the pending new-event card

## Files Most Relevant To The Latest Round

- `app/build.gradle.kts`
- `app/src/main/java/com/example/todoalarm/ui/WikiActivity.kt`
- `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- `app/src/main/java/com/example/todoalarm/sync/DesktopSyncWebAssets.kt`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`

## Icon Chain Note

Current launcher icon chain:

- Manifest icon: `@mipmap/ic_launcher`
- Adaptive icon foreground: `@drawable/ic_launcher_foreground`
- `ic_launcher_foreground`: inset wrapper around `@drawable/ic_payki_mark`
- In-app launch / drawer art can still use `@drawable/ic_launcher_art`

If the phone still shows an old icon after installing the latest APK, suspect launcher cache or an old installed package before changing resources again.

## Smallest Safe Next Step

The next session should device-test rather than immediately refactor:

1. Install `PaykiTodo-1.6.33-debug.apk`
2. Verify the launcher icon after reinstall / launcher cache refresh
3. Verify Wiki sidebar links switch pages in the in-app Wiki
4. Verify Settings 使用说明 and 提示音 rows directly open target screens
5. Verify desktop web multi-day all-day events render as continuous bars
6. Regression-test daily board missed todos, delete confirmation, and calendar pending-draft cancel behavior

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
