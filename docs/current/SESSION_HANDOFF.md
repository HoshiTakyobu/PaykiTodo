# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Pre-round baseline commit: `2edb774`
- Current code version: `1.15.16 / versionCode 358`
- Database version: `30`
- Latest debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.15.16-debug.apk`
- Latest signed release APK available locally: `app/build/outputs/apk/release/PaykiTodo-1.14.12-release.apk`
- Latest GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.14.12`
- This round must not be pushed unless the user explicitly requests it.

## Completed In This Round

1. Desktop Web brand/login surfaces now use the phone launcher artwork served from the Android resource.
2. Desktop Web todo management now renders only `待办` and `历史待办`; obsolete summary cards and the old timeline heading were removed while group filtering remains.
3. Desktop Calendar week, month, and mini-calendar calculations now use the shared phone `WeekStartMode`; the web UI persists Monday-start or Sunday-start through the authenticated calendar-preferences endpoint.
4. Today's mini-calendar cell retains the gray selected-week range background under the blue today ring.
5. Ongoing event notifications no longer display a live chronometer and retain only the planned event range.
6. Future todo reminder surfaces now include a non-mutating `我知道了` action in the activity and accessibility overlay; it clears the already-triggered alarm pair and current reminder session while preserving later reminders for the same todo.
7. Desktop Sync Settings directly displays available LAN URLs when the service is running and no longer has show/hide address buttons.
8. Database remains `30`; the app version is `1.15.16 / versionCode 358`.

## Preserved Browser Verification Evidence

- Reference inputs:
  - `E:\Temp\codex-clipboard-1T2xav.png`
  - `E:\Temp\codex-clipboard-jcrlI0.png`
  - `E:\Temp\codex-clipboard-JmKfgt.png`
- Final local screenshots, deliberately not tracked by Git:
  - `E:\Temp\payki-calendar-reference-final.png`
  - `E:\Temp\payki-editor-final-pass.png`
  - `E:\Temp\payki-calendar-month-1.15.14-final.png`
- Browser viewport: `1638x989`, device scale factor `1.25`, output `2048x1236`.
- Verified results:
  - week view: 7 day columns, correct range title, `创建日程` action
  - day view: 1 day column
  - month view: 42 date cells and visible event pills
  - search: matching title returns the event; a nonexistent query returns none
  - blank-grid click opens creation editor
  - title edits update the right-side preview live
  - existing event opens with its saved title
  - no browser page errors in the checked flows
- The browser screenshots and interaction checks above are preserved from the `1.15.15` calendar baseline.
- Emulator calendar swipe statistics: 441 frames, p50 `21ms`, p90 `34ms`, p95 `40ms`, p99 `81ms`, 9.98% janky frames, and 21 slow UI-thread frames.

## 1.15.16 Verification

- Passed `:app:compileDebugKotlin`, `:app:testDebugUnitTest` (26 suites), `:app:lintDebug`, `:app:assembleDebug`, JavaScript syntax, APK metadata inspection, and `git diff --check`.
- Lint result: 0 errors and 121 existing warnings.
- The current APK launches successfully on `emulator-5554`; `MainActivity` remains resumed and no Android runtime crash was reported in the launch smoke test.
- Emulator Desktop Sync automatically displayed its reachable LAN address. The forwarded page returned HTTP 200, contained the embedded launcher icon and week-start selector, and no longer contained the removed todo timeline heading or summary container. Desktop Sync was disabled again after verification.
- Debug APK metadata: package `com.paykitodo.app`, `versionName 1.15.16`, `versionCode 358`, min API 26, target API 34.

## Emulator Evidence

- Reused device: `emulator-5554` / AVD `Pixel_8`.
- Installed APK: `app/build/outputs/apk/debug/PaykiTodo-1.15.16-debug.apk`.
- Desktop Sync was enabled only for the runtime page smoke check and disabled again afterward.
- The emulator-only access key is intentionally not recorded here.

## Next Actions

1. Review `docs/current/UX_FOLLOW_UP_PLAN.md` with the user and select the next concrete UX slice before implementation.
2. Treat future visual feedback as reference-image delta work, not a broad redesign prompt.
3. Repeat reminder behavior on a physical phone because OEM full-screen, lock-screen, vibration, and notification behavior cannot be proven completely by the emulator.
4. Do not push until the user explicitly requests it.

## Safety Notes

- Never commit Desktop Sync tokens, AI keys, private Base URLs, signing files, generated APKs, screenshots, backups, test databases, or local diagnostic logs.
