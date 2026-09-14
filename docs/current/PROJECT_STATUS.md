# Project Status

## Current Baseline

- Repository: `https://github.com/HoshiTakyobu/PaykiTodo`
- Branch: `main`
- Package: `com.paykitodo.app`
- Supported Android: Android 8.0+ / API 26+
- Compile and target SDK: Android 14 / API 34
- License: MIT
- App version: `1.16.16 / versionCode 381` (current local validation source)
- Room database version: `31`

## Published Build

- GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.15.18`
- Release APK: `app/build/outputs/apk/release/PaykiTodo-1.15.18-release.apk`
- Public asset name: `PaykiTodo-1.15.18-release.apk`
- Current debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.16.16-debug.apk`
- Current signed release APK: `app/build/outputs/apk/release/PaykiTodo-1.16.16-release.apk`
- Current Windows self-contained EXE: `desktop/dist/PaykiTodo.exe` (rebuilt for `1.16.16`; file version `1.16.16.0`)
- Debug APK SHA-256: `C211A4B1673EE78142BEB446C30D8E9E0ED43EE6F475EFCF363883B5CF4B8876`
- Signed Release APK SHA-256: `40773D55EE02141C9D318A1E172D319950F6615CD1C2A8A331EF75F32174961F`
- Windows self-contained EXE SHA-256: `1C2425194047D8B787F2CA821563120F4D91CE5D1F8142F3228E87B9A828C253`
- Release signing material remains local and ignored by Git.

## Product Positioning

PaykiTodo is a local-first Android assistant for todos, DDLs, schedules, countdowns, planning notes, and strong reminders. It is developed through maintainer-led, AI-assisted vibe coding: the maintainer owns product decisions and acceptance, while AI tools assist implementation, testing, debugging, and documentation.

## Current Feature Baseline

- Daily board for active todos, remaining schedules, tomorrow's plan, announcements, and countdown targets.
- Todo management with optional DDL, multiple groups, recurrence, reminder offsets, archive/cancel history, and countdown marking.
- Calendar with day/week/month/list views, recurrence, locations, colors, reminders, check-in tracking, and multi-time-slot creation.
- One frequent reminder switch across phone and Desktop Web; disabled means no scheduling, enabled means full-screen plus notification-bar delivery.
- Reminder delivery falls back to a detailed notification when Android rejects a background foreground-service start, preventing `ForegroundServiceStartNotAllowedException` from crashing the app.
- Already-started calendar events may retain historical reminder settings; missed trigger times are not backfilled while future recurring instances remain schedulable.
- Planning Desk memo workflow with local-rule parsing, optional OpenAI-compatible AI recognition, preview, correction, and confirmed import.
- Local LAN Desktop Web console for board, todo, calendar, Planning Desk, AI reports, and Excel schedule import.
- Optional account-based encrypted cloud sync; the Room database remains authoritative and the server stores only encrypted migration-package bytes. Account registration/login, refresh tokens, device revocation, versioned uploads, conflict blocking, and a separate administrator user/device overview are implemented.
- Remote snapshot inspection shows export time and record counts before any restore; restore requires explicit confirmation and creates a local safety backup first.
- Windows self-contained desktop client supports an independent encrypted local data copy, account login/registration, offline todo/event/planning edits, and versioned cloud synchronization. Its phone LAN option remains a clearly separate compatibility mode using the phone's `18765`.
- Android daily-board and countdown widgets.
- Quick checklist groups with arbitrary hierarchy, visible `--#` notes, real TodoItems, parent completion, folding, backup and cross-client snapshot support.
- Local Room storage, `.paykibak` migration archive / legacy JSON restore, diagnostics, crash logs, and in-app Wiki.
- Desktop Web calendar now uses the same current-time line convention as the phone calendar, separates today's date from the selected date, and flashes the selected week column briefly.

See `README.md` for the public overview and `docs/current/FEATURE_LEDGER.md` for the detailed implementation ledger.

## v1.16.16 Validation Scope (not published)

- Administrator timestamps are serialized as epoch values and formatted in the visitor's browser timezone.
- Quick checklist parsing, preview, Enter continuation, validation, and hierarchical task creation work on Android.
- LAN Desktop Web and the Windows client expose matching create/fold/complete/cancel/delete flows; Android additionally owns backup and history restoration.
- Parent completion excludes canceled children, does not revive canceled ancestors, and recurses upward. Canceled tasks enter history; deleted tasks do not.
- Room `30 -> 31` adds checklist group, parent, depth, order, note, and fold fields while preserving existing data.
- Final verification passed: 183 Android unit tests, 17 emulator instrumentation tests, Android Lint, Debug/AndroidTest/Release assembly, 12 server tests, JavaScript syntax, and Windows Release publish. Release APK v2 signature verification passed with one RSA-2048 signer.
- Known cross-client gap: LAN Web and Windows do not yet expose a dedicated history-restore entry for canceled checklist tasks.

## v1.16.15 Validation Scope (not published)

- Drawer account identity, immutable server user ID, nickname/password updates and signed-in/historical device management.
- Password changes atomically replace password hashes and re-encrypted cloud snapshots with revision checks, then revoke sessions without deleting local data.
- Calendar vertical month paging, continuous horizontal week paging, date-correct Today navigation, today agenda scrolling/highlight, compact agenda text and content-height accent bars.
- Removed calendar batch/template menus, moved history under Todo, restored Todo artwork, added a circular create button with an 800ms batch action and larger group choices.
- Planning document creation/rename/swipe archive moved into the document list; help moved to the top bar; editor guidance card removed.
- AI provider titles wrap at full width without numbering; sound preferences are merged and compacted.
- Azure account APIs are deployed; service health check passed. Public proxy accepts the encoded snapshot size needed by password rotation. Deployment credentials and rollback files remain private.
- Verification complete: 11 server tests passed; Android unit tests, Lint, Debug/Release assembly, and all 9 emulator interaction checks passed. Physical-device OEM reminder/widget checks remain.
- Emulator evidence: `emulator-5554` / `Pixel_8` installed the final Debug and instrumentation APKs; `MainActivity` launched with `versionName=1.16.15` and `versionCode=380`.
- APK evidence: Debug SHA-256 `054169AA284863F486D7EE7BAFC9D169CA63A9449985BC62BA78C45A8A3009B4`; signed Release SHA-256 `1FEF71C5367109E759E20F5FDCFD64CB4D05498CF268D82EC850C31A3DE79390`; `apksigner verify` passed the Release APK with one RSA-2048 signer.

## v1.16.14 Validation Scope (not published)

- Android WebView now allows the `about:blank` and `about:srcdoc` internal documents required by Cloudflare's mobile Turnstile flow.
- Duplicate callbacks for one Turnstile token are suppressed before they reach the Compose login state, and callbacks held by AndroidView always target the current composition.
- CSP keeps the challenge and injected Cloudflare Insights resources available while retaining a restrictive default policy.
- Raised Android and Windows application metadata to `1.16.14 / versionCode 379`.

## v1.16.13 Validation Scope (not published)

- Turnstile account authentication now requires a successful Cloudflare Siteverify response with the fixed `account_auth` action and an allowed production hostname.
- Added the production hostname allowlist to the server environment template; the real allowlist and Secret Key remain outside Git.
- Added an Android WebView fallback observer for the Turnstile response field so a successful widget can update the Compose authentication state even when a WebView omits the parent callback.
- Raised Android and Windows application metadata to `1.16.13 / versionCode 378`.

## v1.16.14 Validation Evidence

- `python -m unittest server.test_paykitodo_cloud_server -v`: 9 tests passed, including valid, wrong-hostname, incomplete-allowlist, and CSP internal-document assertions.
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease`: passed.
- Azure service restarted successfully; public `/healthz` returned `200`, and `/v2/auth/challenge` exposes the real widget without the Secret Key.
- The emulator installed and launched `PaykiTodo-1.16.14-debug.apk` without a crash and rendered the Turnstile widget. Cloudflare's documented 401 Private Access Token probe appeared; final token acceptance remains a physical-device check because the emulator challenge was intermittent.

## v1.16.13 Validation Evidence

- `python -m unittest server.test_paykitodo_cloud_server -v`: 9 tests passed, including valid, wrong-hostname, and incomplete-allowlist Turnstile responses.
- `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:assembleRelease`: passed.
- `:app:lintDebug`: passed with 0 errors after the final WebView change.
- Azure `/etc/paykitodo/cloud.env` contains the Site Key, Secret Key, and production hostname allowlist; values were only checked in redacted form.
- Public `/healthz` returned `200`; `/v2/auth/challenge` returned the real widget and did not expose the Secret Key.
- A public registration request with a dummy token returned `403 human_verification_failed`.
- The emulator previously displayed Cloudflare `Success!`; it disconnected before the rebuilt `1.16.13` APK could be installed for the final callback assertion.

## v1.16.12 Validation Scope (not published)

- Normal reminder acknowledgement, completion, notification retention, stale-alarm rejection, and service shutdown no longer clear the dispatch ledger for the current reminder time.
- A reminder dispatch is cleared only when its schedule is deliberately rebuilt or a todo/event is rescheduled through Android or LAN Desktop Web, so a past trigger cannot be redispatched by the polling fallback.
- Android uses the system notification full-screen intent as the only primary reminder surface; direct Activity and accessibility-overlay paths are reserved for devices where that system mechanism is unavailable.
- AI reports remain AI-first, model discovery remains available from provider settings, and no synthetic anti-detection traffic is generated.
- Raised Android and Windows application metadata to `1.16.12 / versionCode 377`.

## v1.16.12 Validation Evidence

- `python -m unittest server.test_paykitodo_cloud_server -v`: 8 tests passed.
- `:app:testDebugUnitTest`: passed.
- `:app:lintDebug`: passed with 0 errors and 125 non-blocking warnings.
- `:app:assembleDebug` and `:app:assembleRelease`: passed.
- ADB smoke test on `emulator-5554` / `Pixel_8`: installed and launched the Debug APK; package metadata reported `versionName=1.16.12` and `versionCode=377`; the account page rendered normally and showed the explicit “当前服务器未启用人机验证” state without a fatal crash.
- Debug APK SHA-256: `C81760BD9E887D9BFA5E49A33BED10A30158EA0D122F4D22C848D7EB79233390`.
- Release APK SHA-256: `C432619A1ECF7AFC70A80E51DA9BDBB04A6D5551A38AECC3A773A3BD6B7C4880`.
- Windows EXE SHA-256: `39BE39CA7C26E36413102AE14331E56EA822FDD8A89EB7200FB3E959302447DE`.
- Physical-device validation remains required for OEM lock-screen and full-screen reminder behavior.
- Windows publish still emits a non-fatal `WindowsBase` 4.0/5.0 WebView2/WPF assembly warning; the build completes successfully.

## v1.16.10 Validation Scope (not published)

- Reminder polling passes the concrete trigger timestamp to the foreground service, matching receiver-side dispatch deduplication.
- Removed newly scheduled `+2s` reminder backups and clean up legacy backup alarm PendingIntents at application startup.
- Planning Desk direct writes and snapshot imports notify the local widget and remote-sync coordinator through one repository callback.
- Turnstile WebView accepts the Cloudflare challenge host, third-party cookies, and reports HTTP/render-process failures as blocked states instead of allowing submission.
- Raised the application metadata to `1.16.10 / versionCode 375`.
- The public cloud environment was updated to the current server source. Its Turnstile keys are still absent, so the endpoint intentionally reports that protection is not configured until the owner supplies both keys.

## v1.16.10 Validation Evidence

- `python -m unittest server.test_paykitodo_cloud_server -v` and `:app:testDebugUnitTest` passed before the version bump.
- Public `/healthz` returned `200` and `/v2/auth/challenge` returned the current server response after deployment; the response correctly reports missing Turnstile configuration and contains no widget until keys are configured.
- Debug/Release APK assembly and final ADB smoke test remain to be run after the version bump.

## v1.16.9 Validation Scope (not published)

- Added optional Cloudflare Turnstile to Android account login, registration, and password recovery.
- Added server-side Turnstile Siteverify validation and a no-store challenge page; the server secret remains environment-only.
- Turnstile configuration now fails closed when only the public Site Key or only the server Secret Key is present.
- Kept IP rate limiting and made the feature disabled-compatible when Turnstile is not configured.
- Raised the application metadata to `1.16.9 / versionCode 374`.

## v1.16.9 Validation Evidence

- `python -m unittest server.test_paykitodo_cloud_server -v`: 7 tests passed, including missing-token rejection, mocked Siteverify success, and incomplete-key fail-closed behavior.
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease`: passed.
- ADB smoke test: `emulator-5554` / `Pixel_8` installed `PaykiTodo-1.16.9-debug.apk`, reported versionCode `374`, opened `MainActivity`, and exposed the login/register/recovery UI without a fatal crash.
- The authenticated reminder trigger was not executed on the emulator because no test account/session was configured. Full-screen launch, notification permission, exact-alarm permission, lock-screen behavior, and OEM background policy still require a controlled authenticated test and physical-device verification.

## v1.16.8 Validation Scope (not published)

- Fixed first-login synchronization on an empty device: an existing cloud snapshot is now downloaded instead of being treated as already current.
- Kept local-data upload and explicit conflict protection unchanged for devices that already contain data.
- Raised the application metadata to `1.16.8 / versionCode 373`.

## v1.16.7 Validation Scope (not published)

- Removed all unauthenticated migration-package actions from the Android account page; export and import remain in authenticated `设置 -> 数据与备份`.
- Updated the Debug-to-Release migration instructions to require login before either operation.
- Raised the application metadata to `1.16.7 / versionCode 372`.

## v1.16.2 Development Scope (not published)

- Added migration-package export/import actions to the unauthenticated login surface so Debug-to-Release data transfer remains available before account login.
- Stopped the phone LAN service explicitly during account logout and blocked unauthenticated secondary entry points, reminder surfaces, and widgets from exposing local data.
- Made reminder dismissal remove stale notification/service artifacts and kept long-title reminder actions readable on narrow screens.
- Changed automatic backups to one latest file updated after data changes instead of accumulating timestamped files.

## v1.16.5 Validation Scope (not published)

- Added a notification fallback when Android rejects the background foreground-service start, preventing reminder-triggered process crashes while preserving notification details and the full-screen pending intent.
- Allowed reminders on already-started calendar events to remain saved without backfilling missed trigger times; future event reminders retain the future-time validation.
- Redesigned the account entry surface with the existing dashboard artwork and launcher icon, while keeping migration actions available before login.
- Added explicit `-Rotate` behavior to the ignored administrator credential helper; ordinary runs read the current credential, while rotation updates the server and restarts the cloud service.
- Raised the application metadata to `1.16.5 / versionCode 370`.

## v1.16.4 Validation Scope (not published)

- Moved account login/registration off the UI thread and refreshed the authenticated ViewModel state immediately after success.
- Completed signed Release emulator navigation and UI audit for the account gate, board, drawer, todos, calendar, Planning Desk, settings, and appearance selector.
- Recorded audit evidence and physical-device residual risks in `ANDROID_RELEASE_UI_AUDIT_1.16.3.md`.

## v1.16.1 Development Scope (not published)

- Replaced the browser-native administrator Basic Auth prompt with a PaykiTodo-owned HTML login page and cookie session flow.
- Preserved CSRF-protected administrator actions and script-compatible valid Basic Auth without issuing an authentication challenge for ordinary browser requests.

## v1.16.0 Development Scope (not published)

- Added an account-based cloud API with password hashing, short-lived access tokens, refresh-token rotation, device management, audit records, and an independently protected administrator page.
- Added Android cloud account login/registration, encrypted local token/key storage, local-first synchronization, automatic pull when clean, upload after local changes, and explicit conflict blocking.
- Added an independent Windows cloud client with DPAPI-protected local account/data files, offline editing, core todo/event/reminder/recurrence/countdown/planning fields, and the existing phone LAN compatibility mode.
- Added generic cloud server environment, systemd, Nginx, desktop usage, and integration-test materials without private hostnames, addresses, credentials, or certificates.

- Split the desktop create controls into independent todo and event actions with tab-specific visibility.
- Reordered the desktop header into left navigation and right-side creation/connection controls with responsive wrapping.
- Unified desktop calendar current-time rendering with the phone timeline and added minute-aligned refresh.
- Fixed mini-calendar selected-day geometry and added one-second selected-week-column feedback.

## v1.15.21 Development Scope (not published)

- Added a `.paykibak` ZIP migration archive containing business data, planning data, history, and ordinary settings while excluding AI API keys, remote-sync secrets, Desktop Sync credentials, and file-provider authorization state; provider labels, Base URLs, and model names are retained without keys.
- Import protects the current installation's local credentials and backup authorization, refreshes reminder scheduling and widgets, and remains compatible with legacy JSON backups.
- Added a local-first encrypted remote-sync coordinator: data changes schedule a delayed upload, enabled sync performs a one-minute fallback cycle, and disabled sync owns no background upload jobs.
- Added the Azure/Nginx-compatible ciphertext relay service and deployment materials without uploading user data or secrets.
- Added a Windows `win-x64` self-contained shell around the existing LAN Desktop Web console and a repeatable build script.
- Upgraded the desktop WebView2 package to `1.0.4191.47`.
- Fixed Android API 33-only URL encoding in the new sync path so the API 26 minimum remains lint-clean.
- Added `我知道了` to every todo full-screen reminder, including todos due today; it only acknowledges the current reminder and does not complete, cancel, or move the task.
- Added remote snapshot inspection with local/remote counts and export time, explicit replacement confirmation, and automatic pre-restore local backup while preserving local credentials.
- Added Windows server-snapshot read-only mode with local PBKDF2-SHA256/AES-GCM decryption and `.paykibak` parsing; the server secret is not persisted by the desktop client.
- Unified the user-facing reminder control into one `提醒` checkbox: enabled means full-screen plus notification-bar delivery, disabled means no reminder scheduling.
- Reflowed full-screen and accessibility reminder actions for narrow phones: events show only `我知道了`, intermediate todo reminders show completion/acknowledgement, and only the final todo reminder offers fixed ten-minute snooze.
- Retained notification clicks now re-check the item state and clear stale notifications without opening completed, canceled, or missing items.

## v1.15.18 Release Scope

- The published build used direct full-screen/notification channel selection; current development supersedes this user-facing choice with one unified `提醒` switch while preserving all three stored values for migration compatibility.
- Desktop Web calendar hour labels share the grid coordinate system and align with solid hour rules.
- Ended Desktop Web events fade as complete cards, including title, time, location, border, and background.
- Database version remains `30`; no migration is required.

## Verification Baseline

- `node --check app/src/main/assets/desktop-web/app.js`
- `:app:compileDebugKotlin`
- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- `:app:assembleRelease`
- `dotnet publish desktop/PaykiTodo.Desktop/PaykiTodo.Desktop.csproj --configuration Release --runtime win-x64 --self-contained false`
- `tools/Build-PaykiTodoDesktop.ps1` (self-contained `win-x64` publish)
- APK metadata inspection for package/version/min/target SDK
- Release APK v2 signature verification with `apksigner`
- `git diff --check`
- `python -m unittest server.test_paykitodo_cloud_server -v`
- 公网云服务健康检查、账号注册/鉴权和管理员页访问检查
- `dotnet build desktop/PaykiTodo.Desktop/PaykiTodo.Desktop.csproj --configuration Release`
- Desktop Web calendar screenshot verification using production CSS

## Remaining Real-Device Risk

- Full-screen reminder behavior depends on Android/OEM permissions, background-launch policy, exact-alarm access, and battery management.
- Launcher widget rendering and notification appearance can differ across OEM launchers and notification panels.
- Desktop Sync requires a reachable trusted LAN; campus networks may expose an address while client isolation still blocks peer access.
- Remote sync is currently encrypted whole-snapshot synchronization rather than field-level operation merge. Conflicts are blocked and must be resolved by explicit local/remote restore; there is no automatic field-level merge.
- The administrator service is deployed behind the owner's private Cloudflare hostname through an independent Nginx server block; the real hostname, DNS values, Cloudflare settings, certificate paths, administrator password, and server pepper remain outside Git.
- The origin certificate is issued specifically for the private PaykiTodo hostname and Certbot renewal is enabled. The public route exposes only HTTPS `443`; the cloud process remains loopback-only on `18766` and is not the 1Panel service.
- Cloudflare WAF/rate-limit policy still needs periodic review in the owner's Cloudflare account. The current application-level registration/login rate limits and administrator web-session login are active, but administrator MFA is not implemented yet.
- The Windows cloud mode is writable against its local copy; LAN mode still requires a running phone service and all modes require WebView2 Runtime.
- Desktop self-contained publish reports a non-fatal `WindowsBase` assembly version warning from the WebView2 package's WPF reference while compiling the WinForms shell.
- A release build signed with a different key cannot directly replace a locally installed debug build. Export a backup before switching signatures.

## Documentation Lifecycle

- Public project information: root `README.md`, `CHANGELOG.md`, `PRIVACY.md`, `SECURITY.md`, `CONTRIBUTING.md`, and related community files.
- Live maintainer truth: `docs/current/`.
- Completed goal prompts: `docs/goals/`.
- Obsolete requirements, recovery notes, and old status snapshots: `docs/archive/historical/`.
