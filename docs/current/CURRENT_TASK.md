# Current Task

## Active Focus

The `v1.15.21` local development round completes the unified reminder interaction and narrow-screen reminder action layout on top of the `v1.15.20` migration / remote-snapshot foundation. It is not a GitHub Release yet; the latest public release remains `v1.15.18`.

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
2. The `1.15.21` source and local APKs are not published until the user explicitly requests a release.

## Next Validation

1. Install `1.15.21` Debug or Release on the matching signed profile and smoke-test startup, migration import, todo creation, event creation, and Desktop Sync.
2. Verify the single reminder switch and the event/intermediate/final todo action layouts on a physical phone.
3. Verify full-screen behavior while locked and while unlocked inside another app on the user's OEM ROM.
4. Verify daily-board and countdown widgets after installation, resize, date change, and dark/light theme change.
5. Physically test remote snapshot inspection and restore with a real server snapshot.
6. Physically test Windows server mode against the deployed HTTPS relay; keep it read-only until a conflict model is designed.
7. Do not present the current encrypted snapshot relay as account login or bidirectional sync; follow `docs/current/CLOUD_SYNC_AND_ADMIN_DESIGN.md` for the next architecture phase.

Do not commit signing material, AI keys, Desktop Sync tokens, private Base URLs, generated APK/AAB files, backups, screenshots with personal data, or local diagnostics.
