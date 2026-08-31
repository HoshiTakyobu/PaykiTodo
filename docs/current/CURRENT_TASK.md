# Current Task

## Active Focus

The `v1.15.19` local development round adds Debug-to-Release migration, an encrypted remote-sync foundation, and a Windows desktop shell. It is not a GitHub Release yet; the latest public release remains `v1.15.18`.

## Completed `v1.15.19` Scope

1. Added signed Release and Debug build tooling using the same source with separate signing/build types.
2. Added `.paykibak` ZIP migration archives with checksums, legacy JSON import compatibility, and secret exclusion.
3. Added local-first AES-GCM remote snapshot upload scheduling and the server-side ciphertext relay.
4. Added a self-contained Windows WebView2 shell for the existing LAN Desktop Web console.
5. Fixed Android API 26 compatibility in the remote-sync URL path and verified Android/desktop builds.

## Latest Public Release Baseline

1. `v1.15.18` remains the latest GitHub Release and public APK.
2. The `1.15.19` source and local APKs are not published until the user explicitly requests a release.

## Next Validation

1. Install `1.15.19` Release on a release-signed profile and smoke-test startup, migration import, todo creation, event creation, and Desktop Sync.
2. Verify all four reminder-channel states on a physical phone: neither, notification only, full-screen only, and both.
3. Verify full-screen behavior while locked and while unlocked inside another app on the user's OEM ROM.
4. Verify daily-board and countdown widgets after installation, resize, date change, and dark/light theme change.
5. Add remote-sync download review, conflict preview, and explicit merge/restore UX before calling server sync complete.
6. Add server-backed read-only mode to the Windows shell before advertising it as an internet-connected desktop client.

Do not commit signing material, AI keys, Desktop Sync tokens, private Base URLs, generated APK/AAB files, backups, screenshots with personal data, or local diagnostics.
