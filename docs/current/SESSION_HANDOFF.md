# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version: `1.15.1 / versionCode 343`
- Database version: `29` (unchanged in this round)
- Latest debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.15.1-debug.apk`
- Latest signed release APK available locally: `app/build/outputs/apk/release/PaykiTodo-1.14.12-release.apk`
- Latest GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.14.12`

## Completed In This Round

1. **Campus-network Desktop Sync addresses**
   - Address selection no longer assumes every useful Wi-Fi address is RFC1918.
   - Active Wi-Fi and trusted physical WLAN/Ethernet candidates may expose campus CGNAT or campus public IPv4.
   - Mobile-data, VPN, loopback, multicast, and link-local candidates remain hidden.
   - Settings explains that campus client isolation can still block computer-to-phone access even when an address is valid.

2. **Live system-theme following**
   - `MainActivity` observes `uiMode` configuration changes and refreshes theme state immediately.
   - `onResume` recalibrates the system theme for OEMs that do not dispatch configuration changes consistently.
   - `ThemeMode.LIGHT` and `ThemeMode.DARK` still override the system value.

3. **Phone visual-system refresh**
   - Replaced beige/brown colors with neutral gray surfaces, teal primary, blue secondary, and compact Material shapes.
   - Removed the old landscape wallpaper from app content surfaces; widgets keep their separate image resources.
   - Tightened the drawer brand/navigation area, Settings menu, Settings bottom sheets, daily-board headings/cards, todo cards, empty states, countdown/schedule cards, and top app bar.
   - Recurring edit/cancel/delete scope selection is one compact grouped list with clear one-time / future / whole-series wording.

4. **Desktop Web refresh and token handling**
   - Four-character access-token validation is case-insensitive in both browser and phone server.
   - Browser input strips spaces, normalizes case, and limits length to four characters.
   - Login, sidebar, top bar, cards, timeline, Planning Desk, forms, and modals use compact neutral light/dark styling instead of aurora, purple gradients, glass panels, and oversized radii.

## Validation Evidence

- `./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.example.todoalarm.sync.DesktopSyncAddressSelectorTest` passed.
- `./gradlew.bat --no-daemon :app:testDebugUnitTest` passed.
- `./gradlew.bat --no-daemon :app:compileDebugKotlin` passed.
- `./gradlew.bat --no-daemon :app:assembleDebug` passed.
- `node --check app/src/main/assets/desktop-web/app.js` passed.
- `git diff --check` passed after the final code and documentation edits.
- Emulator `emulator-5554` / AVD `Pixel_8` installed and launched `PaykiTodo-1.15.1-debug.apk` without a fatal exception.
- Emulator smoke checks covered the daily board, todo page, drawer, Settings menu, Settings detail bottom sheet, live light/dark switching, and recurring-todo cancel-scope dialog.
- ADB forwarding `127.0.0.1:18765 -> emulator:18765` loaded the Web console. A lowercase-token API request returned HTTP 200, and headless Edge rendered the login page, light/dark daily board, event timeline, and Planning Desk.

## Remaining Manual Checks

1. Test address visibility and actual reachability on the user's campus Wi-Fi. The emulator proves filtering logic, not campus client-isolation policy.
2. Keep OEM-device checks for launcher widgets, notification shade colors, vibration, lock-screen/full-screen reminders, and background alarm reliability.
3. The broad visual refresh is complete for the requested navigation, Settings, board/todo, recurrence, and desktop surfaces; calendar/editor-specific redesigns should be separate focused rounds rather than unbounded changes in this commit.

## Notes For Next Session

- Do not reintroduce the full-screen landscape wallpaper into phone app content. Widget backgrounds are independent and may continue using their own image resources.
- Do not weaken the five-minute authorized-heartbeat shutdown rule for Desktop Sync.
- Valid campus Wi-Fi addresses can still be unreachable because of AP/client isolation; do not treat that as an address-parser failure without testing the network policy.
- Do not commit Desktop Sync tokens, AI keys, private Base URLs, signing files, generated APKs, screenshots, or local diagnostic logs.
