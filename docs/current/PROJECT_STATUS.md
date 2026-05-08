# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.6.13"`
  - `versionCode = 85`

## Current Build Facts

- Existing debug APK output seen in the repo:
  - `app/build/outputs/apk/debug/PaykiTodo-1.6.13-debug.apk`
- Minimal verification completed in the current round:
  - `./gradlew.bat assembleDebug` succeeded
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository has been consolidated to a committed `1.6.13` baseline after the calendar current-time persistence repair round.

The most important current baseline facts are:

- the version bump to `1.6.13 / 85` is already in code
- the late `1.6.9` carry-over items remain included in this baseline
- the daily board / background visual refresh remains included in this baseline
- the launcher adaptive icon foreground is no longer a full opaque raster art file
- launcher foreground and monochrome layers now use a dedicated safe-zone vector logo for mask-compatible cropping
- the calendar current-time label now stays on the left time axis even when today is off-screen
- the calendar current-time red line now remains visible across the schedule grid even when today is off-screen
- future sessions should use `git status` to detect any new local divergence instead of assuming the worktree is dirty

Recent checked-but-not-yet-fully-device-verified UI changes also include:

- daily board as the default landing section
- separate `dashboard_bg_light.jpg` and `dashboard_bg_dark.jpg` resources
- launch screen and drawer icon visual refresh
- launcher foreground resource chain was rewritten so adaptive icon cropping no longer depends on the full raster art
- launch screen and drawer now use the raster launcher art directly to avoid Compose-side drawable loading risk
- calendar current-time label / red-line visibility no longer depends on the today column staying in the current viewport

## Repository-Verified Carry-Over Status

Direct inspection of the current worktree shows the three late `1.6.9` carry-over items are already structurally present in the current `1.6.13` baseline:

- the calendar current-time text is rendered on the left time axis and now remains visible even when today is off-screen, while the red current-time line remains inside the schedule grid and is also kept visible
- the adaptive launcher icon chain is switched to the current PaykiTodo mark resources
- both reminder and desktop-sync notifications use `ic_stat_payki_todo`
- the release-signing template document exists at `docs/PaykiTodo-Release-Signing-Template.md`

The smallest remaining uncertainty in this area is no longer the resource wiring itself, but final device-side confirmation across install surface, launcher, and themed-icon surfaces.

## Documentation Health

Current repo documentation is mixed:

- `README.md`, `TODO.md`, and `CHANGELOG.md` are aligned to `1.6.13`, but should still be kept in sync as future changes land
- many files under `docs/` are historical snapshots for earlier versions such as `1.4.9`, `1.5.0`, and `1.6.1`
- older versioned docs should not be treated as the live project baseline unless explicitly referenced by the current docs

## Current Risk Areas

1. New sessions may over-trust old versioned docs and misunderstand the current scope
2. Long-lived chat sessions can become unreliable, so repository docs must carry the shared state
3. Device-side verification still matters for both the launcher icon surfaces and the calendar current-time indicator behavior

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and git status
4. Only then decide the next edit
