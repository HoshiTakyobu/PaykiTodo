# Current Task

## Active Development Focus

The current round has produced a `1.6.33` baseline. The next work should start from repository facts rather than old chat memory.

Primary active focus areas:

1. Device-test launcher icon behavior after reconnecting adaptive foreground to the safe-zone vector mark
2. Device-test in-app Wiki sidebar navigation across multiple chapters
3. Device-test Settings -> 使用说明 and Settings -> 提示音 direct actions
4. Desktop-test multi-day all-day events in the browser console; they should render as one continuous horizontal bar
5. Continue board / dashboard and calendar polish without regressing existing flows
6. Keep version metadata and docs aligned with the actual code state

## Immediate Practical Next Steps

When a new session takes over, it should usually do these in order:

1. run `git status --short --branch`
2. verify current version number and APK naming
3. if testing on device, install `app/build/outputs/apk/debug/PaykiTodo-1.6.33-debug.apk`
4. if launcher icon still looks stale, clear launcher cache or reinstall cleanly before assuming the resource chain is wrong
5. open Wiki and click several sidebar entries besides 总览与阅读方式
6. tap Settings -> 使用说明 and Settings -> 提示音 and verify they direct-open the target screens
7. open desktop web console with a multi-day all-day event and verify it spans visible days horizontally

## Repository-Verified Notes

The current code baseline includes these specific 1.6.33 changes:

1. `WikiActivity.kt` enables JavaScript for the bundled local Wiki and disables file cross-origin access
2. `SettingsPanel.kt` routes 使用说明 directly to `onOpenWiki` and 提示音 directly to `onPickSystemReminderTone`
3. `DesktopSyncWebAssets.kt` renders all-day events as spanning cards across visible days
4. `ic_launcher_foreground.xml` points to `@drawable/ic_payki_mark` instead of `@drawable/ic_launcher_art`
5. `app/build.gradle.kts` is bumped to `1.6.33 / 105`
6. `./gradlew assembleDebug` has succeeded for this version

## What Not To Do Immediately

- do not re-plan the whole app from scratch
- do not use very old version docs as the current source of truth
- do not scan the whole workspace outside this repo
- do not revert unrelated user edits
- do not change JDK setup; use Android Studio bundled `jbr`
- do not assume device behavior is fixed until the user actually tests it

## Current External Dependency

No external file is needed for the current 1.6.33 verification task.

If future work touches icon generation or adaptation, verify the intended in-repo resource chain before reprocessing any external image.
