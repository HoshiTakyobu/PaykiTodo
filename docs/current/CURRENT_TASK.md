# Current Task

## Active Development Focus

The `1.15.1 / versionCode 343` connection reliability and phone / desktop visual-system refresh is complete. No next feature round is selected yet.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, or personal backups/logs.

## Current Round Scope

1. Desktop Sync address selection: allow valid unicast IPv4 addresses reported by active Wi-Fi or trusted physical LAN interfaces, including campus CGNAT and campus public IPv4; keep mobile-data, VPN, loopback, multicast, and link-local addresses hidden.
2. Theme following: explicitly observe Android `uiMode` changes and refresh again on activity resume so `SYSTEM` follows OEM light / dark changes reliably.
3. Phone UI: replace the old beige/brown palette and oversized rounded surfaces; tighten drawer navigation, Settings menu density, Settings detail presentation, and recurring-item scope choices.
4. Desktop Web: make the 4-character token case-insensitive, normalize input, remove aurora / gradient / glass styling, use a compact workstation layout, and add system dark-mode variables.
5. Version / docs / verification: bump to `1.15.1 / 343`, update public/current docs, run focused unit tests, full debug tests, JS syntax validation, assemble the APK, and perform emulator smoke checks where available.

## Current Status

- All four behavior/UI areas are implemented and verified.
- Phone app content no longer uses the old landscape wallpaper; board/todo/chrome surfaces now match the refreshed drawer and Settings visual system.
- `DesktopSyncAddressSelectorTest` covers private Wi-Fi, hotspot, campus CGNAT, campus public IPv4, mobile data, and VPN filtering.
- Database version remains `29`; no migration is required.

## Validation

Completed:

1. `node --check app/src/main/assets/desktop-web/app.js` — passed.
2. `git diff --check` — passed after code and final documentation updates.
3. `./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.example.todoalarm.sync.DesktopSyncAddressSelectorTest` — passed.
4. `./gradlew.bat --no-daemon :app:testDebugUnitTest` — passed.
5. `./gradlew.bat --no-daemon :app:compileDebugKotlin` and `:app:assembleDebug` — passed.
6. APK metadata — `1.15.1 / versionCode 343`, output `app/build/outputs/apk/debug/PaykiTodo-1.15.1-debug.apk`.
7. Emulator — launch, board, todo, drawer, Settings bottom sheet, live light/dark switching, and recurring-scope selection passed without a fatal exception.
8. ADB-forwarded Web — lowercase token authorization returned HTTP 200; light/dark board, event timeline, and Planning Desk rendered successfully.

Physical campus Wi-Fi reachability and OEM-specific notification/widget behavior remain manual device checks, not unfinished code in this round.
