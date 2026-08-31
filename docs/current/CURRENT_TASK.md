# Current Task

## Active Focus

The `v1.15.20` local development round completes the reminder acknowledgement UI, remote snapshot review/restore flow, and Windows server-snapshot read-only mode. It is not a GitHub Release yet; the latest public release remains `v1.15.18`.

## Completed `v1.15.20` Scope

1. Added signed Release and Debug build tooling using the same source with separate signing/build types.
2. Added `.paykibak` ZIP migration archives with checksums, legacy JSON import compatibility, and secret exclusion.
3. Added local-first AES-GCM remote snapshot upload scheduling and the server-side ciphertext relay.
4. Added a self-contained Windows WebView2 shell for the existing LAN Desktop Web console.
5. Fixed Android API 26 compatibility in the remote-sync URL path and verified Android/desktop builds.
6. Added `我知道了` to every todo full-screen reminder, including same-day due todos.
7. Added remote snapshot summary inspection, explicit replacement confirmation, and pre-restore safety backup.
8. Added Windows server snapshot mode with local decryption and read-only board/todo/calendar rendering.

## Latest Public Release Baseline

1. `v1.15.18` remains the latest GitHub Release and public APK.
2. The `1.15.20` source and local APKs are not published until the user explicitly requests a release.

## Next Validation

1. Install `1.15.20` Release on a release-signed profile and smoke-test startup, migration import, todo creation, event creation, and Desktop Sync.
2. Verify all four reminder-channel states on a physical phone: neither, notification only, full-screen only, and both.
3. Verify full-screen behavior while locked and while unlocked inside another app on the user's OEM ROM.
4. Verify daily-board and countdown widgets after installation, resize, date change, and dark/light theme change.
5. Physically test remote snapshot inspection and restore with a real server snapshot.
6. Physically test Windows server mode against the deployed HTTPS relay; keep it read-only until a conflict model is designed.

Do not commit signing material, AI keys, Desktop Sync tokens, private Base URLs, generated APK/AAB files, backups, screenshots with personal data, or local diagnostics.
