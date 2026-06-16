# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version:
  - `versionName = 1.14.10`
  - `versionCode = 330`
  - database version = `28`
- Latest debug APK target in this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.14.10-debug.apk`
- Latest signed release APK available locally:
  - `app/build/outputs/apk/release/PaykiTodo-1.13.11-release.apk`
- Latest GitHub Release:
  - `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.13.11`

## Active Goal

Fix the phone Calendar quality regression reported after the 1.14.x calendar changes, and make the Daily Board schedule rows visually tighter.

## What Changed In The Latest 1.14.10 Patch

1. **Calendar three-day view no longer starts from the wrong date window**
   - 1.14.9 restored continuous horizontal scrolling but initialized `horizontalOffsetPx` to `0f`.
   - Because `CalendarDateWindow` spans 730 days before and after the anchor date, offset `0f` made the first render focus around `anchorDate - 729 days`.
   - That first render requested and placed events for an old empty range, making the calendar look blank even when current events existed.
   - 1.14.10 uses a `Float.NaN` sentinel and derives the first effective horizontal offset from the selected date immediately, so the first `visibleDays` and `requestedEventRange` are centered on the intended date.

2. **Calendar target-date navigation still re-centers the three-day timeline**
   - The existing `LaunchedEffect` now writes the selected-date-centered offset after layout dimensions are known.
   - Date picker, section navigation, and event deep-link focus keep landing on the intended date instead of preserving a stale scroll offset.
   - Manual horizontal swipes still update the offset continuously.

3. **Daily Board schedule rows are more compact**
   - `BoardScheduleEventRow` now uses a smaller 14dp radius and tighter vertical padding.
   - The extra inner `Surface` layer was removed so normal rows behave like a compact Feishu-style list item: transparent background, left accent strip, and a dense text block.
   - Title/time/location use explicit compact font sizes and line heights; location is capped at one line.
   - In-progress rows keep the gold border and check-in status, but the border and check-in button are smaller.

4. **Version and docs**
   - Version metadata is `1.14.10 / versionCode 330`.
   - Database version remains `28`; no schema migration was added.
   - Updated `CHANGELOG.md`, `docs/current/PROJECT_STATUS.md`, `docs/current/FEATURE_LEDGER.md`, `docs/current/CURRENT_TASK.md`, and this handoff.

## Validation

- Passed:
  - `./gradlew.bat :app:compileDebugKotlin`
  - `./gradlew.bat :app:testDebugUnitTest`
  - `git diff --check`
  - `./gradlew.bat :app:assembleDebug`
  - APK metadata inspection for `versionName = 1.14.10`, `versionCode = 330`, `outputFile = PaykiTodo-1.14.10-debug.apk`

## Next Session Notes

1. If the user still sees a blank calendar, check whether events are outside the initial vertical scroll viewport rather than outside the loaded date range.
2. The current fix was structural for the first date-range calculation; physical-device or emulator UI smoke testing is still useful for confirming visual behavior.
3. The Daily Board card density is now substantially tighter, but final visual acceptance still depends on the user's real phone screenshot.
4. Do not reintroduce HorizontalPager unless there is a deliberate design decision; 1.14.9 intentionally restored continuous scrolling.
