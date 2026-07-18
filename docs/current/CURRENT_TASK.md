# Current Task

## Active Development Focus

The `1.15.2 / versionCode 344` visual-identity restoration and activity-timer removal round is complete. The next feature round has not been selected yet.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, emulator screenshots, or personal backups/logs.

## Completed Scope

1. Restore the phone daily board's user-selected light/dark landscape backgrounds, Forest / Clay / Ocean palette, `payki_greeting` font, title shadow, circular menu surface, and greeting-card gradient.
2. Keep the `1.15.1` live `SYSTEM` theme-following fix and compact layout improvements; define all relevant Material container/outline colors so default purple does not leak into new components.
3. Remove the activity-timer UI, foreground service, notification receiver/actions, ViewModel flows, repository state machine, and AI daily/weekly report inputs.
4. Keep database version `29`, `activity_records`, `MIGRATION_28_29`, and backup serialization as an upgrade/restore compatibility layer for devices and backups that already contain historical records.
5. Bump to `1.15.2 / 344`, update public/current docs, run the full debug unit-test/build chain, and perform light/dark emulator smoke checks.

## Validation

Completed:

1. `./gradlew.bat --no-daemon :app:compileDebugKotlin` - passed.
2. `./gradlew.bat --no-daemon :app:testDebugUnitTest` - passed.
3. `./gradlew.bat --no-daemon :app:assembleDebug` - passed.
4. APK metadata - `1.15.2 / versionCode 344`, output `app/build/outputs/apk/debug/PaykiTodo-1.15.2-debug.apk`.
5. Source/merged-manifest scan - no activity-timer service, receiver, runtime action, or visible timer copy remains.
6. Emulator `emulator-5554` / AVD `Pixel_8` - installed and launched the APK in light and dark system modes; both selected background images and the custom greeting font rendered; no PaykiTodo fatal exception was logged.
7. UI hierarchy scan - `开始记录`, `今日做了什么`, and `随手补记` are absent.

Physical-device visual review remains the user's next check; emulator verification does not replace OEM-specific notification/widget/reminder testing.
