# Current Task

## Active Development Focus

Active immediate task: finish `1.14.18 / versionCode 338` Desktop Sync LAN-address selection repair.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, or personal backups/logs.

## Current Round Scope

This round addresses the user-reported Desktop Sync address failure:

1. The phone is on the same LAN as the computer, but Settings -> Desktop Sync recommends `172.19.0.1:18765`.
2. The computer cannot ping or open that address.
3. The expected usable address is the Wi-Fi LAN address such as `192.168.0.100:18765`.
4. The mechanism must be understood and verified, not patched by guessing one literal address.

## Debug Conclusion

`172.19.0.1` can appear when Android exposes virtual / bridge / VPN-like or low-confidence network addresses through the active network or interface list. The previous implementation trusted the active Wi-Fi `LinkProperties` address list once it was non-empty, so a bad `172.*` address could suppress a real `wlan0` / hotspot address discovered from `NetworkInterface`.

The corrected behavior is:

- collect both active Wi-Fi link addresses and real interface enumeration candidates;
- rank trusted Wi-Fi / hotspot / Ethernet interfaces first;
- prefer `192.168.*` when available;
- keep trusted `10.*` and trusted `172.*` addresses for valid hotspot / Ethernet LAN cases;
- hide VPN, virtual-interface, mobile-data, and low-confidence bridge addresses when a better LAN address exists;
- show a status hint explaining that low-confidence addresses were hidden.

## Current Status

Implemented so far:

1. Version metadata moved to `1.14.18 / versionCode 338`; database version remains `28`.
2. Added `DesktopSyncAddressSelector`, a pure Kotlin selector for testable LAN-address ranking/filtering.
3. `DesktopSyncCoordinator` now collects active Wi-Fi candidates and `NetworkInterface` candidates, then delegates final selection to the selector.
4. Added `DesktopSyncAddressSelectorTest` coverage for:
   - wrong active `172.19.0.1` plus real `wlan0 192.168.0.100` -> recommend only `192.168.0.100`;
   - trusted `ap0 172.20.10.1` hotspot address -> keep it when it is the only LAN candidate;
   - `rmnet_data0` / `tun0` mobile-data or VPN addresses -> hide them;
   - trusted Ethernet `10.0.0.15` -> keep it.
5. README, CHANGELOG, built-in Wiki, and current docs are being updated to match the new behavior.

Validation completed so far:

1. `./gradlew.bat --no-daemon :app:compileDebugKotlin` passed after code changes.
2. `./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.example.todoalarm.sync.DesktopSyncAddressSelectorTest` passed.
3. `./gradlew.bat --no-daemon :app:testDebugUnitTest` passed.
4. `node --check app/src/main/assets/desktop-web/app.js` passed.
5. `git diff --check` passed.
6. `./gradlew.bat --no-daemon :app:assembleDebug` passed.
7. APK metadata confirms `versionName = 1.14.18`, `versionCode = 338`, output `app/build/outputs/apk/debug/PaykiTodo-1.14.18-debug.apk`.
8. Pixel_8 emulator verification:
   - installed `PaykiTodo-1.14.18-debug.apk`;
   - app launched to the daily board;
   - Settings -> Desktop Sync opened;
   - enabling Desktop Sync did not crash and showed the running/waiting-for-desktop state;
   - expanding addresses showed emulator LAN URL `http://10.0.2.15:18765`;
   - ADB forward `127.0.0.1:18766 -> emulator:18765` loaded the PaykiTodo Web console HTML with HTTP 200.

Remaining:

1. Commit the completed round with a concrete Chinese commit message.
