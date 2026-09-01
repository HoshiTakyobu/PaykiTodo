# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- App version: `1.15.21 / versionCode 363` (local development; not published)
- Database version: `30`
- GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.15.18`
- Latest public signed release APK: `app/build/outputs/apk/release/PaykiTodo-1.15.18-release.apk`
- Current signed release APK: `app/build/outputs/apk/release/PaykiTodo-1.15.21-release.apk`
- Current debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.15.21-debug.apk`
- Current Windows self-contained EXE: `desktop/dist/PaykiTodo.exe`

## Latest Product Changes

1. Phone and Desktop Web todo/event editors and Planning Desk candidates expose one frequent `提醒` switch; enabled means full-screen plus notification-bar delivery, disabled means no reminder scheduling.
2. Legacy `FULLSCREEN`, `NOTIFICATION`, and `FULLSCREEN_AND_NOTIFICATION` storage values are read as the unified combined contract; they are not separate user choices anymore.
3. Event reminders show only `我知道了`; todo intermediate reminders show `我已完成` + `我知道了`; only the final todo reminder adds fixed `延后10分钟`.
4. Activity and accessibility-overlay action rows are responsive on narrow phones, and retained notification clicks re-check item state before opening a detail page.
5. Desktop Web hour labels align with the solid calendar grid lines.
6. Ended Desktop Web events fade as complete cards instead of only changing color.
7. Every todo full-screen reminder now includes `我知道了`, including same-day due todos; acknowledgement only closes the current reminder.

## Current Development Changes

1. `.paykibak` ZIP migration archives include business data and ordinary settings, validate a SHA-256 payload checksum, retain provider labels/Base URLs/model names without API keys, and exclude remote-sync secrets, Desktop Sync credentials, and file authorization state.
2. Import preserves the current installation's local credentials/backup authorization, restores schedules and widgets, and accepts legacy JSON backups.
3. Remote sync is local-first: Room remains authoritative, data changes trigger a delayed encrypted upload, and enabled sync has a one-minute fallback loop. Turning it off cancels both jobs.
4. The Azure relay accepts and stores ciphertext only. Mobile inspection now shows a safe summary before explicit replacement restore, with a local safety backup first.
5. The Windows EXE is a self-contained WebView2 shell for direct LAN phone connections and now also has a server-snapshot read-only mode. It never edits server data.

## Repository Publication Work

1. README, changelog, support/contribution material, issue examples, privacy wording, and current maintainer docs now describe `v1.15.18`.
2. README and GitHub About position PaykiTodo as an Android 8.0+ local-first todo and schedule assistant developed through maintainer-led, AI-assisted vibe coding.
3. Old context-recovery snapshots were moved from `docs/current/` to `docs/archive/historical/`.
4. Three `Co-Authored-By: Claude ...` trailers were removed by rewriting commit messages only; source trees were preserved.
5. Repository topics describe Android, Kotlin, Compose, todo, calendar, reminder, local-first, and vibe coding.

## Verification

- JavaScript syntax, Kotlin compilation, unit tests, Android Lint, Debug/Release assembly, and Desktop Web calendar visual checks passed for the previous `1.15.18` round.
- `1.15.21` passed 162 Android unit tests, Kotlin Debug/Release compilation, Android Lint, Debug/Release APK assembly, APK metadata inspection, Release v2 signature verification, JavaScript syntax check, `git diff --check`, and self-contained Windows publish.
- The Windows publish retains the known non-fatal WebView2 `WindowsBase` version warning.
- The private administrator hostname was found behind Cloudflare but returning `526`; its Azure origin certificate, independent Nginx route, and administrator backend are not deployed. The real hostname is intentionally excluded from Git.
- `git diff --check` and ignored-secret checks passed before publication.
- GitHub Release asset and remote contributor attribution were rechecked after upload.

## Session Integrity

- The long-running PaykiTodo thread `019d4e16-8f61-78e3-9028-1dccf67f1661` had three live-looking rollout files under one UUID.
- Its state database previously pointed to a rollout whose final compacted summary stopped at `1.15.11`, which caused later resumes to ignore the already released `1.15.18` baseline.
- On 2026-08-31, both local Codex state stores and the active rollout were backed up. Because the running process restored the active `17-19-59` pointer, that rollout received a new structured compacted baseline for `1.15.18 / versionCode 360`, explicitly superseding the stale `1.15.11` summary.
- The thread working-directory hint now points directly to this repository. No rollout was deleted or replaced; see `docs/archive/historical/SESSION_STATE_REPAIR_2026-08-31.md` for the exact diagnosis and rollback references.

## Physical Device Checks Still Needed

1. Install the matching-signed `1.15.21` APK and confirm startup and data operations.
2. Test reminder off/on in both todo and event editors on the user's physical phone.
3. Test intermediate/final multi-reminder todo actions and the event-only `我知道了` action on narrow and ordinary phone widths.
4. Test lock-screen and unlocked-other-app full-screen reminder behavior.
5. Test both Android widgets in light/dark mode and after launcher resize.
6. Test `.paykibak` export from Debug, uninstall Debug, install Release, and import on a physical phone.
7. Test mobile remote inspection/restore and Windows server mode against a real server snapshot.
8. Account login, bidirectional cloud sync, a writable Windows local client, and the administrator panel remain future work; do not use the current snapshot relay as if those features existed.

## Safety Notes

- Never commit `keystore.properties`, `release/`, API keys, private Base URLs, Desktop Sync tokens, generated APKs/AABs, backups, logs with personal data, or local screenshots.
- The release APK belongs in GitHub Release assets, not in Git history.
- The Windows server-mode secret is session-only and must not enter desktop preferences, logs, screenshots, or documentation.
