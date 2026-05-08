# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.6.10"`
  - `versionCode = 82`

## Current Build Facts

- Existing debug APK output seen in the repo:
  - `app/build/outputs/apk/debug/PaykiTodo-1.6.10-debug.apk`
- Minimal verification completed in the current round:
  - `./gradlew.bat assembleDebug` succeeded
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository has been consolidated to a committed `1.6.10` baseline after the icon, board, and handoff refresh round.

The most important current baseline facts are:

- the version bump to `1.6.10 / 82` is already in code
- the late `1.6.9` carry-over items are already included in this baseline
- the daily board / background visual refresh is also included in this baseline
- future sessions should use `git status` to detect any new local divergence instead of assuming the worktree is dirty

Recent checked-but-not-yet-fully-device-verified UI changes also include:

- daily board as the default landing section
- separate `dashboard_bg_light.jpg` and `dashboard_bg_dark.jpg` resources
- launch screen and drawer icon visual refresh
- launcher foreground inset and background color were adjusted again to avoid install-surface icon oversizing
- launch screen and drawer now use the raster launcher art directly to avoid Compose-side drawable loading risk

## Repository-Verified Carry-Over Status

Direct inspection of the current worktree shows the three late `1.6.9` carry-over items are already structurally present in the current `1.6.10` baseline:

- the calendar current-time text is rendered on the left time axis, while the red current-time line remains inside the schedule grid
- the adaptive launcher icon chain is switched to the current PaykiTodo mark resources
- both reminder and desktop-sync notifications use `ic_stat_payki_todo`
- the release-signing template document exists at `docs/PaykiTodo-Release-Signing-Template.md`

The smallest remaining uncertainty in this area is not missing wiring, but final device-side verification and whether `launcher_background` should stay transparent.

## Documentation Health

Current repo documentation is mixed:

- `README.md`, `TODO.md`, and `CHANGELOG.md` are aligned to `1.6.10`, but should still be kept in sync as future changes land
- many files under `docs/` are historical snapshots for earlier versions such as `1.4.9`, `1.5.0`, and `1.6.1`
- older versioned docs should not be treated as the live project baseline unless explicitly referenced by the current docs

## Current Risk Areas

1. New sessions may over-trust old versioned docs and misunderstand the current scope
2. Long-lived chat sessions can become unreliable, so repository docs must carry the shared state
3. Device-side verification still matters even when the resource and code wiring look structurally complete

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and git status
4. Only then decide the next edit
