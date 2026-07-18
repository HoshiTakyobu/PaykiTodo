# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version: `1.15.2 / versionCode 344`
- Database version: `29` (unchanged; compatibility schema retained)
- Latest debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.15.2-debug.apk`
- Latest signed release APK available locally: `app/build/outputs/apk/release/PaykiTodo-1.14.12-release.apk`
- Latest GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.14.12`

## Completed In This Round

1. **Restored the selected phone visual identity**
   - The daily board again uses `dashboard_bg_light` and `dashboard_bg_dark`.
   - The original Forest / Clay / Ocean / Sand / Night palette and `payki_greeting` family are active again.
   - The top title shadow, circular menu backing, and greeting-card gradient/spacing are restored.
   - Material primary/secondary/tertiary containers and outlines now use matching palette colors instead of framework-default purple.
   - The `1.15.1` system-theme live-following behavior remains intact.

2. **Removed the activity-timer product surface**
   - Removed the active timer strip, `今日做了什么` card, start/manual-record dialogs, detail dialog, and all MainActivity/ViewModel callbacks.
   - Removed `ActivityTimerService`, `ActivityTimerReceiver`, their Manifest declarations, and timer action icons.
   - Removed repository/DAO runtime timer operations and activity-record inputs from AI daily/weekly reports.
   - Kept the Room entity/table/migration and backup serialization so upgrading an existing database or restoring an old backup remains safe.

3. **Version and documentation**
   - Version is `1.15.2 / 344`; database remains `29` with no new migration.
   - README, changelog, current ledgers, and UI rules describe the restored visuals and timer removal.
   - `UI_DESIGN_RULES.md` now treats the selected dashboard backgrounds, greeting font, and palette as protected assets that require explicit user approval before replacement.

## Validation Evidence

- `./gradlew.bat --no-daemon :app:compileDebugKotlin` passed.
- `./gradlew.bat --no-daemon :app:testDebugUnitTest` passed.
- `./gradlew.bat --no-daemon :app:assembleDebug` passed.
- APK output metadata reports `versionName 1.15.2`, `versionCode 344`, application ID `com.paykitodo.app`.
- Source and merged-Manifest scans found no activity-timer runtime references.
- Emulator `emulator-5554` / AVD `Pixel_8` installed and launched `PaykiTodo-1.15.2-debug.apk` in both light and dark modes.
- Light/dark screenshots confirmed the respective landscape backgrounds and custom greeting font; UI dump confirmed timer entry copy is absent.
- Post-launch log scan found no PaykiTodo fatal exception.

## Remaining Manual Checks

1. Install `PaykiTodo-1.15.2-debug.apk` on the user's physical phone and compare the restored background/font/color appearance with the intended design.
2. Confirm no stale activity-timer notification remains after upgrading from `1.15.1`; Android should remove the deleted service notification during package replacement.
3. Keep OEM-device checks for widgets, notification shade colors, vibration, lock-screen/full-screen reminders, and background alarm reliability.

## Notes For Next Session

- Do not replace `dashboard_bg_light`, `dashboard_bg_dark`, `payki_greeting`, or the established palette merely for generic “modernization.” Improve spacing and contrast around them instead.
- Do not reintroduce activity-timer UI/service/report behavior unless the user explicitly asks for a redesigned version.
- Do not drop database version below `29` or remove `MIGRATION_28_29`; the retained activity-record schema is compatibility data, not an active feature.
- Do not weaken the five-minute authorized-heartbeat shutdown rule for Desktop Sync.
- Do not commit Desktop Sync tokens, AI keys, private Base URLs, signing files, generated APKs, screenshots, or local diagnostic logs.
