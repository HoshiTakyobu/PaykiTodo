# Current Task

## Active Focus

The `v1.16.1` local development round keeps the first usable account/cloud and independent Windows client implementation from `v1.16.0`, and replaces the browser-native administrator authentication prompt with an in-page PaykiTodo login UI. The private cloud service is deployed separately from Git; it is not a GitHub Release yet, and the latest public release remains `v1.15.18`.

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

1. Install `1.16.1` Debug or Release on the matching signed profile and smoke-test startup, migration import, todo creation, event creation, and Desktop Sync.
2. Verify the single reminder switch and the event/intermediate/final todo action layouts on a physical phone.
3. Verify full-screen behavior while locked and while unlocked inside another app on the user's OEM ROM.
4. Verify daily-board and countdown widgets after installation, resize, date change, and dark/light theme change.
5. Physically test remote snapshot inspection and restore with a real server snapshot.
6. Test Android + Windows login, offline edits, upload/pull, conflict blocking, logout, and device revocation against a disposable account on the deployed service.
7. Add field-level merge or explicit conflict-resolution UI in a later round; the current encrypted snapshot contract intentionally stops before automatic merges.

Do not commit signing material, AI keys, Desktop Sync tokens, private Base URLs, generated APK/AAB files, backups, screenshots with personal data, or local diagnostics.
