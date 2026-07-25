# Current Task

## Active Development Focus

The `1.15.4 / versionCode 346` comprehensive functional-audit round is complete. No new feature round has been selected.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, emulator screenshots, or personal backups/logs.

## Completed Scope

1. Kept the `1.15.3` dirty-editor dismissal fix and re-ran the event/todo editor regression path.
2. Audited phone board, todo/event details, recurrence scopes, history, Calendar, Search, Planning Desk, AI reports, Settings, Wiki, capture/share/voice, check-in, Desktop Sync, backup/restore, and reminder diagnostics.
3. Audited Desktop Sync APIs and desktop Web board/todo/event/planning surfaces, including unauthorized access and five-minute authorized-heartbeat shutdown.
4. Fixed narrow-screen event action wrapping, visible history status, and historical event time/location presentation.
5. Fixed stale AI report notifications, Android 13+ capture-notification permission races, and unbounded Desktop Sync CPU WakeLock ownership.
6. Ran Android Lint to completion, fixed all 5 errors, kept only reviewed warnings, and preserved RemoteViews-compatible framework tint attributes.
7. Bumped to `1.15.4 / 346`, rebuilt the debug APK, cleaned all audit records/artifacts, rotated the emulator sync token, and disabled sync.

## Validation

Completed:

1. `./gradlew.bat --no-daemon :app:testDebugUnitTest :app:assembleDebug` - passed; 22 suites / 134 tests / 0 failures or errors.
2. `./gradlew.bat --no-daemon :app:lintDebug` - passed; the previously reported 5 errors were fixed.
3. `node --check app/src/main/assets/desktop-web/app.js` and `git diff --check` - passed.
4. APK metadata - `1.15.4 / versionCode 346`, output `app/build/outputs/apk/debug/PaykiTodo-1.15.4-debug.apk`.
5. Pixel_8 emulator - locked-screen reminder opened `ReminderActivity`; unlocked/background launch produced the expected Android 14 notification fallback; completion and 10-minute snooze/reschedule both worked.
6. JSON backup - exported valid data, omitted AI keys and Desktop Sync state/token, and re-import did not duplicate matching test items.
7. AI report - local fallback generation/detail/delete worked; check-in duration was included only when positive data existed; deleting the report cleared its notification.

Not completed on physical hardware: OEM launcher widget rendering/resizing, real vibration/audio, OEM notification-shade appearance, and long-running/reboot/background reliability under the user's phone battery manager.
