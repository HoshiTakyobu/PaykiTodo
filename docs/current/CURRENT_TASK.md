# Current Task

## Active Focus

The `v1.16.7` local validation round removes all local-data migration actions from the unauthenticated Android account surface and keeps export/import behind successful account authentication. The private cloud service is deployed separately from Git; no GitHub Release is being created in this round.

## Completed `v1.16.7` Scope

1. Removed all migration-package actions from the unauthenticated surface; export and import remain available after login in `设置 -> 数据与备份`.
2. Updated the Debug-to-Release migration instructions so both operations require an authenticated account session.
3. Raised Android and Windows application metadata to `1.16.7 / versionCode 372`.

## Validation For `v1.16.7`

1. Passed `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and signed `assembleRelease`.
2. Verified on `emulator-5554` that the unauthenticated screen contains no migration explanation, export action, or import action; authenticated `设置 -> 数据与备份` remains the migration entry point.
3. Passed `python -m unittest server.test_paykitodo_cloud_server -v` with all 5 integration tests.
4. Passed PowerShell syntax checks for the ignored local release-build and administrator-credential helpers without rotating production credentials.
5. Passed `git diff --check` and verified `user/`, signing files, environment files, and server database patterns are ignored.

## Completed `v1.16.5` Scope

1. Caught system-level failures when Android rejects a background foreground-service start and published a normal reminder notification fallback instead of crashing the process.
2. Preserved full-screen reminder intent behavior during the notification fallback and recorded the rejection/fallback result in the reminder chain logs.
3. Allowed a newly created calendar event that has already started to keep historical reminder offsets; the alarm scheduler continues to ignore past trigger times.
4. Added an explicit editor notice for retained historical reminders and kept future calendar events subject to future reminder validation.
5. Added the existing dashboard artwork and launcher icon to the account login/register/recovery screen with a scrollable narrow-screen layout.
6. Added `-Rotate` to the ignored administrator credential helper; the default mode reads credentials and rotation mode writes a new password and restarts `paykitodo-cloud.service`.
7. Raised Android and Windows application metadata to `1.16.5 / versionCode 370`.

## Validation For `v1.16.5`

1. Run `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and signed `assembleRelease`.
2. Verify the login page on the Android emulator in light and dark themes, including migration buttons and scroll behavior.
3. Verify the reminder fallback through a controlled Android 12+ background-start denial or an injected unit-level start failure; physical OEM lock-screen and battery policies still require device testing.
4. Verify creating an already-started calendar event with a past reminder saves successfully and does not schedule the missed trigger.
5. Parse the administrator helper with PowerShell without running `-Rotate`; do not rotate the production credential during build verification.

## Completed `v1.16.4` Scope

1. Moved Android login and registration network work to `Dispatchers.IO` so the authentication action does not block the UI thread.
2. Refreshed the ViewModel authentication signal immediately after successful login or registration, so the app opens the daily board without requiring a restart or a polling tick.
3. Installed the signed Release build on the Android emulator and verified login, relaunch persistence, daily board, drawer, todos, calendar, Planning Desk, settings, and appearance navigation.
4. Added `docs/current/ANDROID_RELEASE_UI_AUDIT_1.16.3.md` with UI-tree evidence scope and residual physical-device risks.
5. Restored the final build configuration to the HTTPS cloud endpoint; the local cleartext endpoint used for isolated emulator testing is not part of the final build.

## Completed `v1.16.2` Scope

1. Added migration-package export/import actions to the unauthenticated login surface, so a user can preserve local data before uninstalling a Debug-signed build and restore it after installing a Release-signed build.
2. Stopped the phone LAN service explicitly on account logout and blocked secondary entry points, reminders, and widgets from exposing data without a valid session.
3. Made reminder acknowledgement/snooze remove stale notification and foreground-service artifacts, and kept long-title reminder actions readable on narrow screens.
4. Changed automatic backup output to one latest archive updated on data changes, with cleanup of legacy timestamped archives.
5. Raised Android and Windows application metadata to `1.16.2 / versionCode 367`.

## Completed `v1.16.1` Scope

1. Unauthenticated `/admin` requests render a PaykiTodo-owned login page instead of returning a `WWW-Authenticate` challenge.
2. Added cookie-based administrator sessions with logout and preserved CSRF checks for state-changing actions.
3. Kept valid Basic Auth as a non-challenging compatibility path for scripts and tests.
4. Raised Android and Windows application metadata to `1.16.1 / versionCode 366`.

## Completed `v1.16.0` Scope

1. Added the account registration, login, token refresh, device, versioned encrypted snapshot, conflict, and administrator APIs on loopback port `18766`.
2. Added Android account UI and encrypted local account credentials while preserving Room as the local authority and `18765` as the separate LAN browser path.
3. Added a writable, offline-capable Windows local client with DPAPI-protected state and core todo/event/planning operations.
4. Added integration tests and public deployment/usage templates without private domain or server credentials.
5. Deployed the cloud API and administrator page behind the private HTTPS reverse proxy; the origin stays loopback-only on `18766`, with the existing phone LAN `18765` path unchanged.
6. Verified public health, account registration/authentication, administrator access, administrator CSRF protection, account disable/enable, and device revocation.

## Completed `v1.15.22` Scope

1. Split desktop todo and event creation buttons and made their visibility tab-specific.
2. Reflowed the desktop header so navigation stays left while creation and connection controls stay right without narrow-window overlap.
3. Matched the Desktop Web current-time line to the phone calendar's full-width red line and current-day marker convention.
4. Added minute-aligned indicator refresh, fixed mini-calendar selection geometry, centered week headers, and one-second selected-column feedback.

## Completed `v1.15.21` Scope

1. Added signed Release and Debug build tooling using the same source with separate signing/build types.
2. Added `.paykibak` ZIP migration archives with checksums, legacy JSON import compatibility, and secret exclusion.
3. Added local-first AES-GCM remote snapshot upload scheduling and the server-side ciphertext relay.
4. Added a self-contained Windows WebView2 shell for the existing LAN Desktop Web console.
5. Fixed Android API 26 compatibility in the remote-sync URL path and verified Android/desktop builds.
6. Added `我知道了` to every todo full-screen reminder, including same-day due todos.
7. Added remote snapshot summary inspection, explicit replacement confirmation, and pre-restore safety backup.
8. Added Windows server snapshot mode with local decryption and read-only board/todo/calendar rendering.
9. Unified the user-facing reminder control: one `提醒` checkbox now means full-screen plus notification-bar delivery; legacy channel values are migration-only.
10. Made todo reminder actions responsive: event reminders show only `我知道了`, intermediate todo reminders show completion/acknowledgement, and the final todo reminder additionally shows fixed ten-minute snooze.
11. Reflowed the accessibility overlay actions into a primary row and an optional second row, with single-line button labels and compact padding for narrow phones.
12. Revalidated retained-notification clicks so completed, canceled, or missing items only clear stale notifications instead of opening previews.

## Latest Public Release Baseline

1. `v1.15.18` remains the latest GitHub Release and public APK.
2. The `1.16.0` source and local APKs are not published until the user explicitly requests a release.

## Next Validation

1. Install `1.16.4` Release on a physical device and smoke-test startup, migration import, todo creation, event creation, and Desktop Sync.
2. Verify the single reminder switch and the event/intermediate/final todo action layouts on a physical phone.
3. Verify full-screen behavior while locked and while unlocked inside another app on the user's OEM ROM.
4. Verify daily-board and countdown widgets after installation, resize, date change, and dark/light theme change.
5. Physically test remote snapshot inspection and restore with a real server snapshot.
6. Test Android + Windows login, offline edits, upload/pull, conflict blocking, logout, and device revocation against a disposable account on the deployed service.
7. Add field-level merge or explicit conflict-resolution UI in a later round; the current encrypted snapshot contract intentionally stops before automatic merges.

Do not commit signing material, AI keys, Desktop Sync tokens, private Base URLs, generated APK/AAB files, backups, screenshots with personal data, or local diagnostics.
