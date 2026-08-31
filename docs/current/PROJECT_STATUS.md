# Project Status

## Current Baseline

- Repository: `https://github.com/HoshiTakyobu/PaykiTodo`
- Branch: `main`
- Package: `com.paykitodo.app`
- Supported Android: Android 8.0+ / API 26+
- Compile and target SDK: Android 14 / API 34
- License: MIT
- App version: `1.15.19 / versionCode 361` (current local development source)
- Room database version: `30`

## Published Build

- GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.15.18`
- Release APK: `app/build/outputs/apk/release/PaykiTodo-1.15.18-release.apk`
- Public asset name: `PaykiTodo-1.15.18-release.apk`
- Current debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.15.19-debug.apk`
- Current signed release APK: `app/build/outputs/apk/release/PaykiTodo-1.15.19-release.apk`
- Current Windows self-contained EXE: `desktop/dist/PaykiTodo.exe`
- Release signing material remains local and ignored by Git.

## Product Positioning

PaykiTodo is a local-first Android assistant for todos, DDLs, schedules, countdowns, planning notes, and strong reminders. It is developed through maintainer-led, AI-assisted vibe coding: the maintainer owns product decisions and acceptance, while AI tools assist implementation, testing, debugging, and documentation.

## Current Feature Baseline

- Daily board for active todos, remaining schedules, tomorrow's plan, announcements, and countdown targets.
- Todo management with optional DDL, multiple groups, recurrence, reminder offsets, archive/cancel history, and countdown marking.
- Calendar with day/week/month/list views, recurrence, locations, colors, reminders, check-in tracking, and multi-time-slot creation.
- Independent full-screen and notification reminder channels; selecting neither disables reminder scheduling.
- Planning Desk memo workflow with local-rule parsing, optional OpenAI-compatible AI recognition, preview, correction, and confirmed import.
- Local LAN Desktop Web console for board, todo, calendar, Planning Desk, AI reports, and Excel schedule import.
- Optional encrypted remote-sync upload foundation; the Room database remains authoritative and the server stores only encrypted migration-package bytes.
- Windows self-contained desktop shell for the existing LAN Web console; it currently connects directly to a running phone service.
- Android daily-board and countdown widgets.
- Local Room storage, `.paykibak` migration archive / legacy JSON restore, diagnostics, crash logs, and in-app Wiki.

See `README.md` for the public overview and `docs/current/FEATURE_LEDGER.md` for the detailed implementation ledger.

## v1.15.19 Development Scope (not published)

- Added a `.paykibak` ZIP migration archive containing business data, planning data, history, and ordinary settings while excluding AI API keys, remote-sync secrets, Desktop Sync credentials, and file-provider authorization state; provider labels, Base URLs, and model names are retained without keys.
- Import protects the current installation's local credentials and backup authorization, refreshes reminder scheduling and widgets, and remains compatible with legacy JSON backups.
- Added a local-first encrypted remote-sync coordinator: data changes schedule a delayed upload, enabled sync performs a one-minute fallback cycle, and disabled sync owns no background upload jobs.
- Added the Azure/Nginx-compatible ciphertext relay service and deployment materials without uploading user data or secrets.
- Added a Windows `win-x64` self-contained shell around the existing LAN Desktop Web console and a repeatable build script.
- Upgraded the desktop WebView2 package to `1.0.4191.47`.
- Fixed Android API 33-only URL encoding in the new sync path so the API 26 minimum remains lint-clean.

## v1.15.18 Release Scope

- Phone and Desktop Web todo/event editors and Planning Desk candidates use direct full-screen/notification channel selection without a separate reminder-enable switch.
- All three stored reminder modes remain round-trip compatible through recurrence, backup/restore, batch import, and Desktop Sync.
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
- Remote server sync currently uploads encrypted snapshots and exposes a download primitive, but does not yet provide automatic pull, conflict preview, or merge UI.
- The Windows EXE currently embeds the LAN Web console and requires a running phone service plus WebView2 Runtime; server-backed read/write desktop operation is not complete.
- Desktop self-contained publish reports a non-fatal `WindowsBase` assembly version warning from the WebView2 package's WPF reference while compiling the WinForms shell.
- A release build signed with a different key cannot directly replace a locally installed debug build. Export a backup before switching signatures.

## Documentation Lifecycle

- Public project information: root `README.md`, `CHANGELOG.md`, `PRIVACY.md`, `SECURITY.md`, `CONTRIBUTING.md`, and related community files.
- Live maintainer truth: `docs/current/`.
- Completed goal prompts: `docs/goals/`.
- Obsolete requirements, recovery notes, and old status snapshots: `docs/archive/historical/`.
