# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version:
  - `versionName = 1.14.14`
  - `versionCode = 334`
  - database version = `28`
- Latest debug APK target in this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.14.14-debug.apk`
- Latest signed release APK available locally:
  - `app/build/outputs/apk/release/PaykiTodo-1.14.12-release.apk`
- Latest GitHub Release:
  - `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.14.12`

## Active Goal

Finish the `1.14.14 / versionCode 334` metadata rebuild after the `1.14.13` behavior fixes and the AGENTS version-bump rule hardening.

## What Changed In The 1.14.14 Patch

1. **Install-version metadata**
   - App metadata was incremented from `1.14.13 / versionCode 333` to `1.14.14 / versionCode 334`.
   - This makes the next debug APK clearly installable/testable as a distinct version after the Planning Desk / countdown / full-screen reminder fixes.

2. **Workflow rule**
   - `AGENTS.md` now explicitly requires bumping `versionName` and `versionCode` before any user-visible feature change, bug fix, reminder/sync/data behavior change, or APK build meant for installation.
   - The only exception is when the user explicitly asks for a same-version metadata rebuild.

3. **Documentation**
   - README current source version, changelog, current project status, current task, and this handoff now point at `1.14.14 / versionCode 334`.
   - Database version remains `28`; no Room schema migration was added.

## Validation

- Passed:
  - `git diff --check`
  - `./gradlew.bat --no-daemon :app:assembleDebug`
  - APK metadata inspection for `versionName = 1.14.14`, `versionCode = 334`, output `PaykiTodo-1.14.14-debug.apk`

## Next Session Notes

1. Do not report or hand off a new installable APK without confirming the APK metadata matches the current source version.
2. If the next round changes user-visible behavior or builds another installable APK, bump `versionName` and `versionCode` first.
