# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- App version: `1.15.18 / versionCode 360`
- Database version: `30`
- GitHub Release: `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.15.18`
- Signed release APK: `app/build/outputs/apk/release/PaykiTodo-1.15.18-release.apk`
- Debug APK: `app/build/outputs/apk/debug/PaykiTodo-1.15.18-debug.apk`

## Latest Product Changes

1. Phone and Desktop Web todo/event editors and Planning Desk candidates expose independent full-screen and notification reminder selections without a separate enable switch.
2. Selecting neither channel disables scheduling; each single-channel mode remains independent; selecting both preserves both delivery surfaces.
3. Snooze/acknowledgement retains a normal detail notification only when the notification channel was selected.
4. Desktop Web hour labels align with the solid calendar grid lines.
5. Ended Desktop Web events fade as complete cards instead of only changing color.

## Repository Publication Work

1. README, changelog, support/contribution material, issue examples, privacy wording, and current maintainer docs now describe `v1.15.18`.
2. README and GitHub About position PaykiTodo as an Android 8.0+ local-first todo and schedule assistant developed through maintainer-led, AI-assisted vibe coding.
3. Old context-recovery snapshots were moved from `docs/current/` to `docs/archive/historical/`.
4. Three `Co-Authored-By: Claude ...` trailers were removed by rewriting commit messages only; source trees were preserved.
5. Repository topics describe Android, Kotlin, Compose, todo, calendar, reminder, local-first, and vibe coding.

## Verification

- JavaScript syntax, Kotlin Debug compilation, unit tests, Android Lint, Debug build, and Desktop Web calendar visual checks passed in the `1.15.18` development round.
- Signed Release assembly and APK metadata/signature checks passed in the publication round.
- `git diff --check` and ignored-secret checks passed before publication.
- GitHub Release asset and remote contributor attribution were rechecked after upload.

## Physical Device Checks Still Needed

1. Install the signed release on a clean/release-signed profile and confirm startup and data operations.
2. Test all four reminder-channel combinations on the user's physical phone.
3. Test lock-screen and unlocked-other-app full-screen reminder behavior.
4. Test both Android widgets in light/dark mode and after launcher resize.

## Safety Notes

- Never commit `keystore.properties`, `release/`, API keys, private Base URLs, Desktop Sync tokens, generated APKs/AABs, backups, logs with personal data, or local screenshots.
- The release APK belongs in GitHub Release assets, not in Git history.
