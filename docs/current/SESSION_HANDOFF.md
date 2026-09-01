# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- App version: `1.16.0 / versionCode 365` (local development; not published)
- Room database version: `30`
- Latest public GitHub Release: `v1.15.18`
- Android outputs after final verification:
  - `app/build/outputs/apk/debug/PaykiTodo-1.16.0-debug.apk`
  - `app/build/outputs/apk/release/PaykiTodo-1.16.0-release.apk`
- Windows output after final publish: `desktop/dist/PaykiTodo.exe`

## v1.16.0 Product Changes

1. `server/paykitodo_cloud_server.py` implements user registration/login, short access tokens, rotating refresh tokens, devices, encrypted snapshot revisions, idempotent uploads, audit records, and a Basic-Auth administrator page with CSRF-protected state changes.
2. The cloud origin defaults to `127.0.0.1:18766`; public clients use Cloudflare/Nginx HTTPS `443`. Real hostnames, IPs, certificates, environment values, cloud databases, and administrator credentials are not in Git.
3. Android Settings -> Data and Backup exposes cloud registration/login, account state, revision, pending/conflict/error state, manual sync, cloud inspection, and logout.
4. Android keeps Room authoritative, stores tokens/snapshot keys in encrypted preferences, uploads after local writes, pulls only when the local copy is clean, and blocks concurrent dirty revisions instead of silently overwriting.
5. Windows cloud mode owns a DPAPI-protected local `.paykibak` working copy, works offline, and can add/edit/complete/cancel/delete todos, add/edit/cancel/delete events, configure core reminder/recurrence/countdown fields, and edit a Planning Desk memo.
6. Windows phone-LAN compatibility mode still embeds the phone's `18765` page and directly changes phone data. It is not the cloud client and does not require the cloud account server.
7. The legacy `/v1/sync/{deviceId}` encrypted relay remains available for old Android settings.

## Verification

- Python cloud integration tests cover register/login, wrong password, token refresh, device list, versioned upload, idempotency, conflict rejection, encrypted snapshot download, and administrator authentication.
- Android cloud tests cover stable cross-client account-key derivation and AES-GCM wrong-key rejection.
- Android compile and full unit tests, Debug/Release APK assembly, Lint, Windows Release build/publish, and `git diff --check` must be rerun after the final documentation commit.
- The existing non-fatal WebView2 `WindowsBase` assembly warning remains during Windows build.

## Deployment Still Required

1. Create a private server environment file from `server/paykitodo-cloud.env.example` with random administrator/password-pepper secrets.
2. Install the service as a dedicated `paykitodo` user, keep the SQLite database under `/var/lib/paykitodo`, and proxy the private hostname to `127.0.0.1:18766`.
3. Configure a valid origin certificate and Cloudflare SSL/TLS Full (strict), WAF, login/registration rate limits, and origin firewall rules.
4. Smoke-test with a disposable account before migrating real phone data.

## Known Limitations

- Synchronization is versioned whole-snapshot sync, not field-level operation merge. Conflicts stop for explicit resolution.
- The Windows client covers core writable todo/event/planning fields but is not yet feature-identical to every Android-only surface such as alarm delivery, widgets, AI reports, check-in runtime, and advanced recurrence scope operations.
- The administrator page currently uses deployment-level Basic Auth rather than MFA; it is suitable only behind HTTPS and Cloudflare access controls until MFA is added.

## Safety

- Never commit private domains, Azure IPs, Cloudflare settings, SSH keys, environment files, SQLite user databases, keystores, passwords, tokens, API keys, real backups, or user screenshots.
- Do not push or create a GitHub Release until the user explicitly requests it.
