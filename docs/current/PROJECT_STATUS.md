# Project Status

## Current Baseline

- Repository: `https://github.com/HoshiTakyobu/PaykiTodo`
- Branch: `main`
- Package: `com.paykitodo.app`
- Supported Android: Android 8.0+ / API 26+
- Compile and target SDK: Android 14 / API 34
- License: MIT
- App version: `1.15.21 / versionCode 363` (current local development source)
- Room database version: `30`

## Published Build

- GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.15.18`
- Release APK: `app/build/outputs/apk/release/PaykiTodo-1.15.18-release.apk`
- Public asset name: `PaykiTodo-1.15.18-release.apk`
- Current debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.15.21-debug.apk`
- Current signed release APK: `app/build/outputs/apk/release/PaykiTodo-1.15.21-release.apk`
- Current Windows self-contained EXE: `desktop/dist/PaykiTodo.exe`
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
- Optional encrypted remote-sync upload foundation; the Room database remains authoritative and the server stores only encrypted migration-package bytes. Account login, bidirectional device sync, conflict merge, and admin APIs are not implemented.
- Remote snapshot inspection shows export time and record counts before any restore; restore requires explicit confirmation and creates a local safety backup first.
- Windows self-contained desktop shell currently supports the phone LAN Web console plus a server-snapshot read-only mode; these are compatibility capabilities only. The target standalone EXE must own a local data copy and synchronize with the server independently of the phone's `18765`; that writable account-synced client remains unimplemented.
- Android daily-board and countdown widgets.
- Local Room storage, `.paykibak` migration archive / legacy JSON restore, diagnostics, crash logs, and in-app Wiki.

See `README.md` for the public overview and `docs/current/FEATURE_LEDGER.md` for the detailed implementation ledger.

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
- Desktop Web calendar screenshot verification using production CSS

## Remaining Real-Device Risk

- Full-screen reminder behavior depends on Android/OEM permissions, background-launch policy, exact-alarm access, and battery management.
- Launcher widget rendering and notification appearance can differ across OEM launchers and notification panels.
- Desktop Sync requires a reachable trusted LAN; campus networks may expose an address while client isolation still blocks peer access.
- Remote server sync intentionally remains snapshot-based: there is no account login, automatic pull, field-level conflict merge, or bidirectional background overwrite. The target design is recorded in `docs/current/CLOUD_SYNC_AND_ADMIN_DESIGN.md`.
- The private PaykiTodo administrator hostname is intended to sit behind Cloudflare, but its independent origin service, Nginx server block, and certificate are not deployed. Real hostname, DNS values, and certificate paths are intentionally excluded from Git.
- Cloudflare WAF/rate-limit settings and the future administrator route still require the domain owner's Cloudflare session. Do not point the hostname at the existing panel or issue an origin certificate as if the administrator backend already existed.
- The Windows EXE server mode is intentionally read-only; LAN mode still requires a running phone service and all modes require WebView2 Runtime.
- Desktop self-contained publish reports a non-fatal `WindowsBase` assembly version warning from the WebView2 package's WPF reference while compiling the WinForms shell.
- A release build signed with a different key cannot directly replace a locally installed debug build. Export a backup before switching signatures.

## Documentation Lifecycle

- Public project information: root `README.md`, `CHANGELOG.md`, `PRIVACY.md`, `SECURITY.md`, `CONTRIBUTING.md`, and related community files.
- Live maintainer truth: `docs/current/`.
- Completed goal prompts: `docs/goals/`.
- Obsolete requirements, recovery notes, and old status snapshots: `docs/archive/historical/`.
