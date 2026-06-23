# Current Task

## Active Development Focus

Active immediate task: finish `1.14.17 / versionCode 337` hotfixes for Desktop Sync startup stability and local Codex resume-entry cleanup.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, or personal backups/logs.

## Current Round Scope

This round addresses three user-reported issues:

1. A suspicious `放心；真有问题平台早就封了 | why: ...` text fragment appeared during context recovery / log inspection.
2. The Codex resume list showed multiple PaykiTodo development entries when the user wants one unique `【开发】PaykiTodo` entry.
3. The phone app crashes immediately after tapping `启用电脑同步`.

## Required Behavior

### Context / log contamination check

- Confirm whether the suspicious text is in the PaykiTodo repository.
- If it is only in Codex history/log material from unrelated work, do not treat it as PaykiTodo source contamination.

### Codex resume entry cleanup

- Keep only the main PaykiTodo thread active:
  - `019d4e16-8f61-78e3-9028-1dccf67f1661`
  - title `【开发】PaykiTodo`
- Archive old PaykiTodo side threads and temporary recovery threads instead of deleting rollout files.
- Back up Codex state/index files before modifying local resume metadata.

### Desktop Sync startup stability

- Enabling Desktop Sync must not crash the app if network-state inspection fails.
- Desktop Sync status should declare and use network-state access safely.
- Wi-Fi / hotspot address inspection failures should degrade to a user-facing status hint.
- Foreground-service startup failures should roll back the sync switch instead of terminating the settings UI.

## Current Status

Implemented so far:

1. Version metadata moved to `1.14.17 / versionCode 337`; database version remains `28`.
2. `ACCESS_NETWORK_STATE` is declared in the manifest.
3. Desktop Sync local address inspection now catches failures and returns a safe unavailable-address hint.
4. Desktop Sync enable path now rolls back the setting if foreground-service startup fails.
5. Local Codex resume state was backed up and old PaykiTodo side entries were archived; session indexes now point only to the main `【开发】PaykiTodo` entry.
6. Suspicious `放心...平台早就封了` text was not found in active PaykiTodo source/current docs; it came from Codex history/log material.

Validation completed so far:

1. Resume metadata query confirms only one active PaykiTodo entry in local state/index.
2. `./gradlew.bat --no-daemon :app:compileDebugKotlin`
3. `./gradlew.bat --no-daemon :app:testDebugUnitTest`
4. `node --check app/src/main/assets/desktop-web/app.js`
5. `git diff --check`
6. `./gradlew.bat --no-daemon :app:assembleDebug`
7. APK metadata inspection for `versionName = 1.14.17`, `versionCode = 337`, output `PaykiTodo-1.14.17-debug.apk`

Remaining:

1. Commit the completed round.
