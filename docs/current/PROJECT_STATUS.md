# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.10.1"`
  - `versionCode = 219`

## Current Build Facts

- Latest debug APK output after this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.10.1-debug.apk`
- Latest signed release APK remains the previous release artifact unless a new release build is requested:
  - `app/build/outputs/apk/release/PaykiTodo-1.9.23-release.apk`
- Verification completed for this `1.10.1` continuation:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat :app:compileDebugKotlin`
  - `./gradlew.bat :app:testDebugUnitTest`
  - `./gradlew.bat :app:assembleDebug`
  - `git diff --check`
  - `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/debug/PaykiTodo-1.10.1-debug.apk`
  - debug `output-metadata.json` reports `versionCode=219`, `versionName=1.10.1`, `outputFile=PaykiTodo-1.10.1-debug.apk`
- Release-signing privacy:
  - local `keystore.properties`, `release/PaykiTodo-release.jks`, APK/AAB outputs, API keys, tokens, and private Base URLs must stay out of Git
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is now at `1.10.1 / 219` after the countdown/widget and desktop Planning Desk parity fix pass.

Most important current baseline facts:

- Database version remains `17`.
- `MIGRATION_16_17` still adds countdown support for todos and recurring templates.
- Active no-DDL todos are still treated as today todos across phone daily board, Android widget board query, desktop board, and desktop todo management.
- Countdown-enabled todos use their DDL date as the target; countdown-enabled events use their start time.
- Expired countdown targets are filtered out before board / widget / desktop rendering.
- Phone daily board still has a countdown card for active countdown targets.
- Desktop daily board still renders countdown targets.
- Android `今日看板` widget no longer includes a countdown section; it remains focused on announcements, greeting, today todos, and today/tomorrow schedules.
- Android independent `倒数日` widget shows nearest countdown targets and distinguishes todo/event semantics:
  - todo rows show a checkbox-like circle and open My Tasks;
  - event rows hide the circle and open Calendar.
- Countdown rows now use `Nd` plus remaining hours/minutes/seconds instead of `D-N`; event rows show full start/end time metadata.
- Planning Desk parsed/import candidates now carry location, all-day, countdown, and recurrence fields.
- Phone and desktop Planning Desk previews can edit those fields before import.
- Desktop Planning Desk AI prompt tells models to return `location` separately and not to turn normal title words into groups.
- Desktop Planning Desk import accepts AI candidate IDs directly and now persists location/all-day/countdown/recurrence fields instead of dropping them.
- Android launcher widget event locations still render saved text directly and no longer auto-prepend `@`.
- Desktop web first connection still requests `/api/snapshot?scope=board`, returning lightweight daily-board data first.
- Desktop todo management still loads through paged/searchable `/api/todos?offset=...&limit=...&q=...`.
- Desktop calendar timeline still loads only visible-range `/api/events?start=...&end=...`.
- Desktop sync enable still starts the phone-side LAN server immediately and auto-disables if no authorized client connects within 5 minutes.
- Safe release-signing explanations live under `docs/templates/`; real signing values must stay only in ignored root-level `keystore.properties`.

## Documentation Health

Current docs synchronized for `1.10.1`:

- `README.md`
- `CHANGELOG.md`
- `TODO.md`
- `app/src/main/assets/wiki/index.html`
- `docs/current/PROJECT_STATUS.md`
- `docs/current/FEATURE_LEDGER.md`
- `docs/current/CURRENT_TASK.md`
- `docs/current/SESSION_HANDOFF.md`
- `docs/current/PAYKITODO_SESSION_LEDGER.md`
- `docs/current/PLANNING_AI_ASSISTANT_DESIGN.md`

Older versioned docs under `docs/archive/historical/` remain historical references and should not be treated as the live baseline unless a current doc explicitly points to them.

## Current Risk Areas

1. Android launcher widget rendering, resizing, stale launcher cache behavior, light/dark readability, and date/time refresh still require physical launcher verification.
2. The independent `倒数日` widget displays seconds based on the latest widget refresh; Android launcher widgets are not guaranteed to tick every second.
3. Browser verification is required for desktop Planning Desk location / recurrence preview and import behavior with real AI output.
4. Settings -> AI 调用配置 model discovery still needs device-side verification with real providers.
5. Strong reminder behavior still needs real-device verification for OEM notification, vibration, lock-screen, reboot, and battery policies.
6. Very large datasets may still require further profiling; desktop split endpoints and indexed board paths remain the current performance baseline.
7. Long-running chat sessions can become unreliable, so repository docs must remain the source of truth for future continuation.

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and `git status`
4. Only then decide the next edit
