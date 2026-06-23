# Current Task

## Active Development Focus

Active immediate task: finish `1.14.16 / versionCode 336` fixes for ongoing-event notification readability and Desktop Web todo/calendar interaction polish.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, or personal backups/logs.

## Current Round Scope

This round addresses five user-reported issues:

1. Ongoing event notifications are hard to read in the phone notification shade, especially on light notification panels.
2. A recurring todo whose recurrence end date is in August appears to show only a short set of generated instances, causing confusion about whether the series was truncated.
3. Desktop Web needs a dedicated sidebar `待办` entry instead of hiding full todo management behind the daily-board card.
4. Desktop Web calendar timeline should support direct event dragging with 15-minute granularity, cross visible-day columns, and fade already-ended events.
5. Desktop Web event color presets should render as compact visible swatches rather than long blank button rows.

## Required Behavior

### Notification readability

- The ongoing-event notification must explicitly set RemoteViews text colors in code, not rely only on XML colors that some OEM notification panels may tint.
- The notification should avoid unnecessary decorated big-content styling that increases the chance of OEM visual interference.
- The app can reduce but cannot fully control whether Android/OEM notification panels show an expand affordance.

### Desktop todo management

- Desktop Web sidebar contains separate `每日看板` and `待办` entries.
- `每日看板` remains the lightweight overview.
- `待办` opens the full todo management surface and starts loading the paged todo list immediately.
- Recurring todo cards explain that only near-future instances are generated while the recurrence end date remains in the series template and future instances are replenished over time.

### Desktop calendar timeline

- Timed event cards can be dragged to a new start time in 15-minute increments.
- Dragging to another visible day column changes the date.
- Dragging a recurring event updates only the current instance by default.
- Already-ended timed events are visually faded.

### Desktop event color presets

- Preset colors render as compact swatches with visible fill colors.
- The modal-wide button rule must not stretch swatches into full-width rows.

## Current Status

Implemented so far:

1. Version metadata moved to `1.14.16 / versionCode 336`; database version remains `28`.
2. Ongoing event notification now sets text colors via `RemoteViews.setTextColor`, uses `setColorized(false)`, and removes `DecoratedCustomViewStyle` / custom big content view.
3. Desktop Web sidebar now has separate `每日看板` and `待办` tabs.
4. Desktop Web `待办` tab loads full todo management directly.
5. Desktop Web event color presets now use fixed-size visible swatches.
6. Desktop Web calendar timed event cards fade after their end time.
7. Desktop Web calendar timed event cards can be dragged in 15-minute increments to another visible day; recurring event drag uses current-instance scope.
8. Desktop Web recurring todo cards now explain the near-future instance generation window.

Validation completed so far:

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `./gradlew.bat --no-daemon :app:compileDebugKotlin`
3. `./gradlew.bat --no-daemon :app:testDebugUnitTest`
4. `git diff --check`
5. `./gradlew.bat --no-daemon :app:assembleDebug`
6. APK metadata inspection for `versionName = 1.14.16`, `versionCode = 336`, output `PaykiTodo-1.14.16-debug.apk`

Remaining:

1. Commit the completed round.
2. Real-phone notification shade verification remains necessary because OEM notification rendering cannot be fully proven by local build checks.
