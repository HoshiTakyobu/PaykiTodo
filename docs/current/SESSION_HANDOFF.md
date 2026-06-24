# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version:
  - `versionName = 1.14.18`
  - `versionCode = 338`
  - database version = `28`
- Latest debug APK:
  - `app/build/outputs/apk/debug/PaykiTodo-1.14.18-debug.apk`
- Latest signed release APK available locally:
  - `app/build/outputs/apk/release/PaykiTodo-1.14.12-release.apk`
- Latest GitHub Release:
  - `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.14.12`

## Completed In This Round So Far

1. **Desktop Sync address mechanism audit**
   - The bad `172.19.0.1:18765` address is not a reliable same-LAN browser entry. It can come from virtual / bridge / VPN-like or low-confidence interface exposure while the phone is still on Wi-Fi.
   - The previous coordinator logic trusted active Wi-Fi `LinkProperties` as soon as it returned any IPv4, so a bad `172.*` value could prevent the real `wlan0` / hotspot address such as `192.168.0.100` from being recommended.

2. **Address selector implementation**
   - Added `DesktopSyncAddressSelector` as a pure Kotlin, unit-testable selection layer.
   - `DesktopSyncCoordinator` now collects both active Wi-Fi link candidates and all up non-loopback `NetworkInterface` candidates, then delegates ranking/filtering to the selector.
   - Trusted Wi-Fi / hotspot / Ethernet interfaces are ranked first; `192.168.*` is preferred when available.
   - Trusted `10.*` and trusted `172.*` are still allowed for real hotspot / Ethernet LAN cases.
   - VPN, virtual-interface, mobile-data, and low-confidence bridge addresses are hidden when a better LAN address exists.

3. **Docs and user-facing help**
   - Version metadata moved to `1.14.18 / versionCode 338`.
   - README, CHANGELOG, built-in Wiki, `PROJECT_STATUS.md`, and `CURRENT_TASK.md` now describe the corrected Desktop Sync address behavior.

## Validation So Far

Passed:

- `./gradlew.bat --no-daemon :app:compileDebugKotlin`
- `./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.example.todoalarm.sync.DesktopSyncAddressSelectorTest`
- `./gradlew.bat --no-daemon :app:testDebugUnitTest`
- `node --check app/src/main/assets/desktop-web/app.js`
- `git diff --check`
- `./gradlew.bat --no-daemon :app:assembleDebug`
- APK metadata inspection:
  - package: `com.paykitodo.app`
  - `versionName = 1.14.18`
  - `versionCode = 338`
  - output: `app/build/outputs/apk/debug/PaykiTodo-1.14.18-debug.apk`
- Pixel_8 emulator / ADB verification:
  - installed and launched the 1.14.18 debug APK on `emulator-5554`;
  - opened Settings -> Desktop Sync;
  - enabling Desktop Sync did not crash and showed the running/waiting-for-desktop status;
  - expanding connection addresses displayed emulator LAN URL `http://10.0.2.15:18765`;
  - `adb forward tcp:18766 tcp:18765` plus `http://127.0.0.1:18766/` returned HTTP 200 and PaykiTodo Web console HTML;
  - the ADB port forward was removed and the app was force-stopped after verification.

Note:

- An earlier parallel Gradle run caused a KSP generated-file race (`FileAlreadyExistsException` for `AppDatabase_Impl.java`). The same compile path passed when run sequentially, so this was a parallel-build artifact, not a source failure.

## Still Required Before Reporting Complete

1. Commit the completed round.

## Notes For Next Session

- Do not regress the selector into a simple “hide all 172” rule. Some real phone-hotspot LANs can use trusted `172.*` addresses.
- The intended rule is: prefer the real Wi-Fi / hotspot / Ethernet interface address; hide low-confidence or virtual-looking addresses when a better LAN candidate exists.
- Do not commit or write Desktop Sync tokens, AI keys, private Base URLs, signing files, or local diagnostic logs.
