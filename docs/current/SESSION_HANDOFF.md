# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Pre-round baseline commit: `e00d907`
- Current code version: `1.15.18 / versionCode 360`
- Database version: `30`
- Latest debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.15.18-debug.apk`
- Latest signed release APK available locally: `app/build/outputs/apk/release/PaykiTodo-1.14.12-release.apk`
- Latest GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.14.12`
- This round must not be pushed unless the user explicitly requests it.

## Completed In This Round

1. Removed the user-visible reminder enable switch from phone todo/event editors, phone Planning Desk preview, Desktop Web todo/event editors, and Desktop Web Planning Desk preview.
2. Added independent full-screen and notification reminder checkboxes on all those surfaces.
3. Restored exact channel persistence and routing for `FULLSCREEN`, `NOTIFICATION`, and `FULLSCREEN_AND_NOTIFICATION`; selecting neither disables scheduling.
4. Full-screen selection controls only the full-screen attempt, and notification selection controls visible notification content and retained post-action detail notifications.
5. Preserved the existing `1.15.17` behavior where snooze changes reminder time without shifting DDL and acknowledgement/snooze can leave a detail-opening normal notification when the notification channel is enabled.
6. Rebuilt the Desktop Web hour axis from 24 real rows after the actual header, eliminating the duplicated JavaScript header offset that could visually separate labels from solid grid lines.
7. Ended Desktop Web events now use parent-card opacity with no color-only saturation filter, so title, time, location, border, and background fade together.
8. Version moved to `1.15.18 / versionCode 360`; database remains `30`.

## Verification

- Passed `node --check app/src/main/assets/desktop-web/app.js`.
- Passed `:app:compileDebugKotlin`.
- Passed complete `:app:testDebugUnitTest`, including `ReminderDeliveryModeTest` four-state mapping and storage round-trip coverage.
- Passed `:app:lintDebug` with no reported Lint errors.
- Passed `:app:assembleDebug` and final `git diff --check`.
- Headless Edge loaded a local visual fixture with production `app.css`; hour labels aligned to solid rules and the ended 18:30-21:00 event faded as a complete card.
- APK metadata confirms package `com.paykitodo.app`, `versionName 1.15.18`, `versionCode 360`, min API 26, and target API 34.

## Physical Device Checks

1. Create or edit a todo and event with each of the four channel states and reopen the editor to verify exact checkbox round-trip.
2. Trigger a notification-only reminder and confirm no full-screen activity appears.
3. Trigger a full-screen-only reminder and confirm the foreground-service placeholder does not remain as a visible detail notification after acknowledgement.
4. Trigger both channels and confirm the full-screen surface appears while the full reminder remains available in the notification shade.
5. Reload Desktop Sync Calendar and confirm hour labels and ended-card opacity at the user's Windows scaling level.

## Safety Notes

- Never commit Desktop Sync tokens, AI keys, private Base URLs, signing files, generated APKs, screenshots, backups, test databases, or local diagnostic logs.
- Do not push until the user explicitly requests it.
