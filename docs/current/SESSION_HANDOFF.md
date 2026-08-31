# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- App version: `1.15.20 / versionCode 362` (local development; not published)
- Database version: `30`
- GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.15.18`
- Latest public signed release APK: `app/build/outputs/apk/release/PaykiTodo-1.15.18-release.apk`
- Current signed release APK: `app/build/outputs/apk/release/PaykiTodo-1.15.20-release.apk`
- Current debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.15.20-debug.apk`
- Current Windows self-contained EXE: `desktop/dist/PaykiTodo.exe`

## Latest Product Changes

1. Phone and Desktop Web todo/event editors and Planning Desk candidates expose independent full-screen and notification reminder selections without a separate enable switch.
2. Selecting neither channel disables scheduling; each single-channel mode remains independent; selecting both preserves both delivery surfaces.
3. Snooze/acknowledgement retains a normal detail notification only when the notification channel was selected.
4. Desktop Web hour labels align with the solid calendar grid lines.
5. Ended Desktop Web events fade as complete cards instead of only changing color.
6. Every todo full-screen reminder now includes `我知道了`, including same-day due todos; acknowledgement only closes the current reminder.

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
- `1.15.20` Kotlin compilation, clean Android unit tests, Android Lint, Debug/Release assembly, and APK metadata/signing checks passed for the source round.
- `1.15.20` WebView2 Windows build passed with the existing non-fatal `WindowsBase` reference warning.
- `git diff --check` and ignored-secret checks passed before publication.
- GitHub Release asset and remote contributor attribution were rechecked after upload.

## Session Integrity

- The long-running PaykiTodo thread `019d4e16-8f61-78e3-9028-1dccf67f1661` had three live-looking rollout files under one UUID.
- Its state database previously pointed to a rollout whose final compacted summary stopped at `1.15.11`, which caused later resumes to ignore the already released `1.15.18` baseline.
- On 2026-08-31, both local Codex state stores and the active rollout were backed up. Because the running process restored the active `17-19-59` pointer, that rollout received a new structured compacted baseline for `1.15.18 / versionCode 360`, explicitly superseding the stale `1.15.11` summary.
- The thread working-directory hint now points directly to this repository. No rollout was deleted or replaced; see `docs/archive/historical/SESSION_STATE_REPAIR_2026-08-31.md` for the exact diagnosis and rollback references.

## Physical Device Checks Still Needed

1. Install the signed release on a clean/release-signed profile and confirm startup and data operations.
2. Test all four reminder-channel combinations on the user's physical phone.
3. Test the todo `我知道了` action for a same-day due reminder.
4. Test lock-screen and unlocked-other-app full-screen reminder behavior.
5. Test both Android widgets in light/dark mode and after launcher resize.
6. Test `.paykibak` export from Debug, uninstall Debug, install Release, and import on a physical phone.
7. Test mobile remote inspection/restore and Windows server mode against a real server snapshot.

## Safety Notes

- Never commit `keystore.properties`, `release/`, API keys, private Base URLs, Desktop Sync tokens, generated APKs/AABs, backups, logs with personal data, or local screenshots.
- The release APK belongs in GitHub Release assets, not in Git history.
- The Windows server-mode secret is session-only and must not enter desktop preferences, logs, screenshots, or documentation.
