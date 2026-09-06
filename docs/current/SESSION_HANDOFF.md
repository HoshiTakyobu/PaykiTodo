# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- App version: `1.16.14 / versionCode 379` (local validation; not published)
- Room database version: `30`
- Latest public GitHub Release: `v1.15.18`
- Android outputs after final verification:
  - `app/build/outputs/apk/debug/PaykiTodo-1.16.14-debug.apk`
  - `app/build/outputs/apk/release/PaykiTodo-1.16.14-release.apk`
- Windows output after final publish: `desktop/dist/PaykiTodo.exe`
- Private cloud deployment: service enabled on the Azure host, origin loopback `127.0.0.1:18766`, public access through the privately configured Cloudflare/Nginx HTTPS hostname, and the administrator page at `/admin`.

## v1.16.14 Product Changes

- Android Turnstile WebView now allows Cloudflare's required `about:blank` and `about:srcdoc` internal documents and keeps the restrictive challenge CSP compatible with the private proxy's injected Insights script.
- One token is delivered to Compose only once, and AndroidView uses current callbacks after recomposition so a successful widget updates the active login screen.
- Android and Windows metadata is `1.16.14 / versionCode 379`.

## v1.16.14 Verification Evidence

- Server tests, Android unit tests, and Android Lint passed; Debug and Release APKs exist for `1.16.14`.
- Azure service restarted successfully; public `/healthz` returned `200`, and the challenge CSP includes `about:blank` / `about:srcdoc`.
- The emulator installed and launched `PaykiTodo-1.16.14-debug.apk` without a crash and rendered the widget. Cloudflare documents the observed 401 Private Access Token probe as non-fatal; final token acceptance remains a physical-device check because the emulator challenge was intermittent.

## v1.16.13 Product Changes

- The existing Cloudflare Turnstile widget now includes the production hostname and remains in managed/no-clearance mode with its local hosts preserved.
- The server stores the existing Secret only in `/etc/paykitodo/cloud.env` and requires `success=true`, `action=account_auth`, and the configured production hostname from Siteverify.
- Android's challenge WebView keeps the official callback and also observes the response field to handle WebViews that show Cloudflare success without invoking the parent callback.
- Android and Windows metadata is `1.16.13 / versionCode 378`.

## v1.16.13 Verification Evidence

- Server unit/integration suite: 9 tests passed.
- Android unit tests and Debug/Release assembly passed.
- Public health and challenge checks passed; dummy registration was rejected with `403 human_verification_failed`.
- The emulator disconnected before the final rebuilt APK could be installed; the earlier session showed the Cloudflare widget reaching `Success!`.

## v1.16.12 Product Changes

- Normal reminder acknowledgement, completion, notification retention, and stale-alarm rejection preserve the current reminder's dispatch claim instead of clearing it.
- Reminder dispatch state is reset only by deliberate todo/event schedule changes through Android or LAN Desktop Web, preventing the polling fallback from re-firing a past trigger.
- AI reports remain AI-first with explicit local-template fallback; model discovery and ordered fallback remain available.
- Synthetic periodic prompts intended to look like user activity are intentionally not generated.
- Android and Windows metadata is `1.16.12 / versionCode 377`.

## v1.16.12 Verification Evidence

- `python -m unittest server.test_paykitodo_cloud_server -v`: 8 tests passed.
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease` passed; Lint reported 0 errors and 125 non-blocking warnings.
- ADB device `emulator-5554` / AVD `Pixel_8` installed and launched `PaykiTodo-1.16.12-debug.apk`; package metadata reported `versionName=1.16.12` and `versionCode=377`, and the account page rendered without a fatal crash.
- Debug APK SHA-256: `C81760BD9E887D9BFA5E49A33BED10A30158EA0D122F4D22C848D7EB79233390`.
- Release APK SHA-256: `C432619A1ECF7AFC70A80E51DA9BDBB04A6D5551A38AECC3A773A3BD6B7C4880`.
- Windows EXE SHA-256: `39BE39CA7C26E36413102AE14331E56EA822FDD8A89EB7200FB3E959302447DE`.
- Physical-device reminder behavior still requires an OEM test for lock-screen and full-screen delivery.
- Windows publish completed with one existing non-fatal `WindowsBase` 4.0/5.0 WebView2/WPF assembly warning.

## Operational Notes

- When Android has permission to use notification full-screen intents, the system notification is the only reminder surface. The accessibility overlay and direct Activity launch are fallback paths for devices where Android rejects or cannot use that mechanism, reducing the chance of two different reminder UIs appearing for one trigger.
- The production authentication challenge is now backed by the existing Turnstile widget. The Secret and hostname allowlist remain in the private Azure environment and are never included in Android or Git.
- AI provider failover is limited to real planning/report/vision calls. No periodic prompts are generated to disguise automated traffic or evade provider safeguards.

## v1.16.10 Product Changes

- Reminder polling passes the concrete trigger timestamp into the foreground service, matching alarm-receiver deduplication.
- New schedules omit the legacy `+2s` backup alarm; application startup cancels old backup PendingIntents.
- Planning Desk direct Room writes and snapshot import notify the shared widget/remote-sync callback.
- Turnstile WebView accepts the Cloudflare challenge host and third-party cookies and blocks HTTP/render-process failures.
- AI reports remain AI-first with an explicit local-template fallback; synthetic anti-detection traffic is not generated.
- Android and Windows metadata is `1.16.10 / versionCode 375`.

## v1.16.10 Verification Evidence

- Server integration tests and Android unit tests passed before the version bump.
- The cloud source and Turnstile environment were redeployed on 2026-09-06; `/healthz` returned `200`, `/v2/auth/challenge` returned the real widget, and a dummy registration token was rejected.
- Final Android Lint, Debug/Release assembly, and emulator smoke verification remains after the version bump.

## v1.16.9 Product Changes

- Added optional Cloudflare Turnstile to Android login, registration, and password recovery.
- Authentication requests are accepted only after server-side Siteverify when a Turnstile Secret Key is configured; the secret remains outside Android and Git.
- Partial Turnstile configuration fails closed instead of leaving the authentication endpoint unprotected.
- Kept IP rate limiting as a second layer and preserved compatibility when the feature is not configured.

## v1.16.9 Verification Evidence

- `python -m unittest server.test_paykitodo_cloud_server -v`: 7 tests passed.
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease`: passed.
- ADB device `emulator-5554` / AVD `Pixel_8` installed `app/build/outputs/apk/debug/PaykiTodo-1.16.9-debug.apk`; package inspection reported `versionName=1.16.9` and `versionCode=374`.
- The app launched `MainActivity` and the UI tree exposed the account login, registration, and password recovery controls. No fatal crash was observed during launch.
- No authenticated reminder smoke test was run on the emulator. Because the app had no test account/session, the scheduled reminder could not be created through the UI; full-screen/notification behavior remains subject to the separate permission and OEM checks documented below.

## v1.16.8 Product Changes

- Fixed first-login cloud snapshot download for an empty device by initializing its local sync revision at zero.
- Preserved local-first upload behavior and explicit conflict protection when both local and cloud data exist.

## v1.16.7 Product Changes

- The unauthenticated account page no longer exposes local migration export or import; both operations remain under authenticated `设置 -> 数据与备份`.
- Debug-to-Release migration instructions now require logging in before exporting from the old installation and before importing into the new installation.
- Version metadata is `1.16.7 / versionCode 372`.

## v1.16.7 Verification Evidence

- Android `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and signed `assembleRelease` passed for `1.16.8`.
- `emulator-5554` was previously updated with `PaykiTodo-1.16.7-debug.apk`; the `1.16.8` build was verified by unit tests and signed APK assembly. The unauthenticated account page remains free of local migration actions.
- `python -m unittest server.test_paykitodo_cloud_server -v` passed all 5 integration tests.
- PowerShell parser checks, `git diff --check`, and ignored sensitive-path checks passed. No production credential rotation was run.

## v1.16.6 Product Changes

1. The account login/register/recovery card is vertically centered and remains scrollable on compact screens.
2. Unauthenticated users can export local data, but migration import is only exposed after successful login in `设置 -> 数据与备份`.
3. Normal account and recovery passwords use salted `scrypt` hashes with server pepper; the separate administrator deployment credential remains outside Git in the protected server environment.
4. Version metadata is `1.16.6 / versionCode 371`.

## v1.16.5 Product Changes

1. Reminder delivery catches Android background foreground-service start rejection and falls back to a detailed notification, preserving the full-screen pending intent when configured.
2. Already-started calendar events may retain past reminder settings; the scheduler ignores missed trigger times instead of treating the event as invalid.
3. The account screen uses the existing dashboard background and launcher icon and remains scrollable for small devices.
4. The administrator credential helper reads by default and rotates only with explicit `-Rotate`, including service restart and post-rotation verification.
5. Version metadata is `1.16.5 / versionCode 370`.

## v1.16.2 Product Changes

1. The unauthenticated Android login surface now exposes local migration-package export/import actions, so Debug-to-Release migration does not require a cloud login first.
2. Secondary entry points, reminder surfaces, widgets, and phone LAN access require a valid account session; session expiry stops cloud sync and phone LAN service.
3. Logout explicitly stops the phone LAN service and refreshes widget data.
4. Reminder acknowledgement/snooze removes stale notification and foreground-service artifacts; long-title reminder actions remain readable on narrow screens.
5. Automatic backup writes one latest archive and cleans up legacy timestamped archives.

## v1.16.1 Product Changes

1. The administrator page now renders an in-page PaykiTodo login form for unauthenticated browser requests; it no longer relies on a native Basic Auth dialog.
2. Successful login creates an HttpOnly, Secure, SameSite cookie session; logout clears it. CSRF protection remains required for administrator state changes.
3. Valid Basic Auth remains available for non-browser scripts without sending an authentication challenge for missing or invalid credentials.

## v1.16.0 Product Changes

1. `server/paykitodo_cloud_server.py` implements user registration/login, short access tokens, rotating refresh tokens, devices, encrypted snapshot revisions, idempotent uploads, audit records, and a Basic-Auth administrator page with CSRF-protected state changes.
2. The cloud origin defaults to `127.0.0.1:18766`; public clients use Cloudflare/Nginx HTTPS `443`. Real hostnames, IPs, certificates, environment values, cloud databases, and administrator credentials are not in Git.
3. Android Settings -> Data and Backup exposes cloud registration/login, account state, revision, pending/conflict/error state, manual sync, cloud inspection, and logout.
4. Android keeps Room authoritative, stores tokens/snapshot keys in encrypted preferences, uploads after local writes, pulls only when the local copy is clean, and blocks concurrent dirty revisions instead of silently overwriting.
5. Windows cloud mode owns a DPAPI-protected local `.paykibak` working copy, works offline, and can add/edit/complete/cancel/delete todos, add/edit/cancel/delete events, configure core reminder/recurrence/countdown fields, and edit a Planning Desk memo.
6. Windows phone-LAN compatibility mode still embeds the phone's `18765` page and directly changes phone data. It is not the cloud client and does not require the cloud account server.
7. The legacy `/v1/sync/{deviceId}` encrypted relay remains available for old Android settings.

## Verification

- Python cloud integration tests cover register/login, wrong password, token refresh, device list, versioned upload, idempotency, conflict rejection, encrypted snapshot download, administrator authentication, CSRF rejection, user disable/enable, and device revocation.
- Public deployment smoke checks cover HTTPS health, account registration/authentication, administrator page access, and cleanup of disposable deployment test accounts.
- Android cloud tests cover stable cross-client account-key derivation and AES-GCM wrong-key rejection.
- Android compile and full unit tests, Debug/Release APK assembly, Lint, Windows Release build/publish, and `git diff --check` must be rerun after the final documentation commit.
- The existing non-fatal WebView2 `WindowsBase` assembly warning remains during Windows build.

## Deployment Completed / Follow-Up

1. The private server environment is installed outside Git with random administrator/password-pepper secrets; the service runs as the dedicated `paykitodo` user and stores SQLite under `/var/lib/paykitodo`.
2. Nginx proxies the private HTTPS hostname to `127.0.0.1:18766`; the hostname-specific origin certificate is installed and Certbot renewal is enabled. The old `paykitodo-sync.service` remains unchanged.
3. Cloudflare proxy and HTTPS routing are working. WAF/rate-limit policy should be reviewed in the Cloudflare console before adding real users.
4. Android and Windows real-client login/offline edit/conflict testing remains the next device-level acceptance step; do not migrate important data until that smoke test passes.

5. Android Release acceptance on 2026-09-02: device `emulator-5554` installed signed `PaykiTodo-1.16.3-release.apk`, then the authentication refresh fix was verified and the current source was advanced to `1.16.4`. Login, relaunch persistence, daily board, drawer, todos, calendar, Planning Desk, settings, and appearance navigation were checked. Physical Android reminder behavior and real Android/Windows account conflict testing remain outstanding.
6. The account transition fix runs login/registration on `Dispatchers.IO` and explicitly signals the ViewModel after success; without this, a successful server response could leave the login screen visible until a later lifecycle refresh.
7. UI audit evidence is recorded in `docs/current/ANDROID_RELEASE_UI_AUDIT_1.16.3.md`; screenshots and XML dumps remain only in ignored `.tmp/audit-1.16.3/`.
8. The cloud service was redeployed from the current source on 2026-09-02; its HTTPS reverse-proxy `/admin` check returned the in-page login UI without `WWW-Authenticate`.
9. The local `user/Build-PaykiTodoRelease.ps1 -SkipDebugBuild` wrapper remains the local Release build entry point; signing material, output APKs, and checksums remain ignored local artifacts.

10. On 2026-09-02, `PaykiTodo-1.16.5-release.apk` was installed on `emulator-5554` after clearing the emulator app data. The unauthenticated account screen was visually checked in its default light theme: dashboard background, launcher artwork, login form, agreement control, and local migration actions were all visible and the page remained scrollable. Dark-theme and physical-device OEM reminder behavior remain follow-up checks.

## Known Limitations

- Synchronization is versioned whole-snapshot sync, not field-level operation merge. Conflicts stop for explicit resolution.
- The Windows client covers core writable todo/event/planning fields but is not yet feature-identical to every Android-only surface such as alarm delivery, widgets, AI reports, check-in runtime, and advanced recurrence scope operations.
- The administrator page now uses an HTTPS-only HttpOnly web session rather than a browser Basic Auth challenge; administrator MFA is not implemented yet, so the private hostname and Cloudflare access controls remain required.

## Safety

- Never commit private domains, Azure IPs, Cloudflare settings, SSH keys, environment files, SQLite user databases, keystores, passwords, tokens, API keys, real backups, or user screenshots.
- Do not push or create a GitHub Release until the user explicitly requests it.
