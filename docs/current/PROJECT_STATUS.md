# Project Status

## Current Baseline

- Repository: `https://github.com/HoshiTakyobu/PaykiTodo`
- Branch: `main`
- Package: `com.paykitodo.app`
- Supported Android: Android 8.0+ / API 26+
- Compile and target SDK: Android 14 / API 34
- License: MIT
- App version: `1.15.18 / versionCode 360`
- Room database version: `30`

## Published Build

- GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.15.18`
- Release APK: `app/build/outputs/apk/release/PaykiTodo-1.15.18-release.apk`
- Public asset name: `PaykiTodo-1.15.18-release.apk`
- Latest debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.15.18-debug.apk`
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
- Android daily-board and countdown widgets.
- Local Room storage, JSON backup/restore, diagnostics, crash logs, and in-app Wiki.

See `README.md` for the public overview and `docs/current/FEATURE_LEDGER.md` for the detailed implementation ledger.

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
- APK metadata inspection for package/version/min/target SDK
- `git diff --check`
- Desktop Web calendar screenshot verification using production CSS

## Remaining Real-Device Risk

- Full-screen reminder behavior depends on Android/OEM permissions, background-launch policy, exact-alarm access, and battery management.
- Launcher widget rendering and notification appearance can differ across OEM launchers and notification panels.
- Desktop Sync requires a reachable trusted LAN; campus networks may expose an address while client isolation still blocks peer access.
- A release build signed with a different key cannot directly replace a locally installed debug build. Export a backup before switching signatures.

## Documentation Lifecycle

- Public project information: root `README.md`, `CHANGELOG.md`, `PRIVACY.md`, `SECURITY.md`, `CONTRIBUTING.md`, and related community files.
- Live maintainer truth: `docs/current/`.
- Completed goal prompts: `docs/goals/`.
- Obsolete requirements, recovery notes, and old status snapshots: `docs/archive/historical/`.
