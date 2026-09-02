# Project Status

## Current Baseline

- Repository: `https://github.com/HoshiTakyobu/PaykiTodo`
- Branch: `main`
- Package: `com.paykitodo.app`
- Supported Android: Android 8.0+ / API 26+
- Compile and target SDK: Android 14 / API 34
- License: MIT
- App version: `1.16.1 / versionCode 366` (current local development source)
- Room database version: `30`

## Published Build

- GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.15.18`
- Release APK: `app/build/outputs/apk/release/PaykiTodo-1.15.18-release.apk`
- Public asset name: `PaykiTodo-1.15.18-release.apk`
- Current debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.16.1-debug.apk` (after this round's build)
- Current signed release APK: not rebuilt in this round; release signing material remains local and ignored by Git.
- Current Windows self-contained EXE: `desktop/dist/PaykiTodo.exe` (after this round's publish)
- Release signing material remains local and ignored by Git.

## Product Positioning

PaykiTodo is a local-first Android assistant for todos, DDLs, schedules, countdowns, planning notes, and strong reminders. It is developed through maintainer-led, AI-assisted vibe coding: the maintainer owns product decisions and acceptance, while AI tools assist implementation, testing, debugging, and documentation.

## Current Feature Baseline

- Daily board for active todos, remaining schedules, tomorrow's plan, announcements, and countdown targets.
- Todo management with optional DDL, multiple groups, recurrence, reminder offsets, archive/cancel history, and countdown marking.
- Calendar with day/week/month/list views, recurrence, locations, colors, reminders, check-in tracking, and multi-time-slot creation.
- One frequent reminder switch across phone and Desktop Web; disabled means no scheduling, enabled means full-screen plus notification-bar delivery.
- Planning Desk memo workflow with local-rule parsing, optional OpenAI-compatible AI recognition, preview, correction, and confirmed import.
- Local LAN Desktop Web console for board, todo, calendar, Planning Desk, AI reports, and Excel schedule import.
- Optional account-based encrypted cloud sync; the Room database remains authoritative and the server stores only encrypted migration-package bytes. Account registration/login, refresh tokens, device revocation, versioned uploads, conflict blocking, and a separate administrator user/device overview are implemented.
- Remote snapshot inspection shows export time and record counts before any restore; restore requires explicit confirmation and creates a local safety backup first.
- Windows self-contained desktop client supports an independent encrypted local data copy, account login/registration, offline todo/event/planning edits, and versioned cloud synchronization. Its phone LAN option remains a clearly separate compatibility mode using the phone's `18765`.
- Android daily-board and countdown widgets.
- Local Room storage, `.paykibak` migration archive / legacy JSON restore, diagnostics, crash logs, and in-app Wiki.
- Desktop Web calendar now uses the same current-time line convention as the phone calendar, separates today's date from the selected date, and flashes the selected week column briefly.

See `README.md` for the public overview and `docs/current/FEATURE_LEDGER.md` for the detailed implementation ledger.

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
