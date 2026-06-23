# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version:
  - `versionName = 1.14.16`
  - `versionCode = 336`
  - database version = `28`
- Latest debug APK:
  - `app/build/outputs/apk/debug/PaykiTodo-1.14.16-debug.apk`
- Latest signed release APK available locally:
  - `app/build/outputs/apk/release/PaykiTodo-1.14.12-release.apk`
- Latest GitHub Release:
  - `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.14.12`

## Completed In This Round

1. **Ongoing event notification readability**
   - `OngoingEventNotifier` now sets RemoteViews text colors through `setTextColor`.
   - The notification uses `setColorized(false)` and no longer uses `DecoratedCustomViewStyle` / custom big-content view.
   - This should reduce unreadable white/gray text on light OEM notification shades. Whether Android still shows an expand affordance is partly controlled by the system notification panel.

2. **Desktop Web navigation**
   - Sidebar now has separate `每日看板` and `待办` entries.
   - `每日看板` remains the lightweight overview.
   - `待办` opens the full todo management view and immediately starts loading the paged todo list.
   - The topbar `新增` button now opens the correct todo/event editor for the active tab.

3. **Recurring todo display semantics**
   - Live read-only Desktop Sync inspection showed the user’s `丁螺环酮I/II/III` items still keep their recurrence end dates (`2026-08-18` / `2026-08-23`) but generated instances currently extend only to the near-future window.
   - This is caused by the repository’s near-future generation strategy (`INITIAL_RECURRING_INSTANCE_LIMIT = 14`, `INITIAL_RECURRING_LOOKAHEAD_DAYS = 14`), not by the recurrence end date being lost.
   - Desktop todo cards now explain that remote instances are replenished over time and edits should use the recurrence scope selector.

4. **Desktop Web calendar timeline**
   - Timed event cards can be dragged to another time in 15-minute increments.
   - Dragging across visible day columns changes the event date.
   - Dragging a recurring event sends `scope = CURRENT` to avoid accidentally moving the whole series.
   - Already-ended timed events fade visually.

5. **Desktop Web event color picker**
   - Event color preset buttons now render as fixed 28px visible swatches.
   - The fix overrides the modal-wide button rule that previously stretched swatches into blank rows.

## Validation

Passed:

- `node --check app/src/main/assets/desktop-web/app.js`
- `./gradlew.bat --no-daemon :app:compileDebugKotlin`
- `./gradlew.bat --no-daemon :app:testDebugUnitTest`
- `git diff --check`
- `./gradlew.bat --no-daemon :app:assembleDebug`
- APK metadata inspection:
  - package: `com.paykitodo.app`
  - `versionName = 1.14.16`
  - `versionCode = 336`
  - output: `app/build/outputs/apk/debug/PaykiTodo-1.14.16-debug.apk`

## Notes For Next Session

1. Real-phone verification is still needed for OEM notification shade rendering; code now forces colors, but the final notification shell/expand affordance remains partly system-controlled.
2. The recurring todo data-layer was not changed this round. If the user wants more far-future instances visible at once, weigh increasing the lookahead window against the previously reported large-list/calendar performance issues.
3. Desktop event drag/drop has been statically and build validated, but real-browser verification on the connected phone is still useful.
4. Do not commit or write the user-provided Desktop Sync token into docs or commits.
