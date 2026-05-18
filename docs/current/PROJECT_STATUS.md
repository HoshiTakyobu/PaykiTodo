# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.10.2"`
  - `versionCode = 220`

## Current Build Facts

- Latest debug APK built in this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk`
- Latest signed release APK built in this round:
  - `app/build/outputs/apk/release/PaykiTodo-1.10.2-release.apk`
- Verification completed for this `1.10.2` continuation:
  - `./gradlew.bat :app:compileDebugKotlin`
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat :app:testDebugUnitTest`
  - `./gradlew.bat :app:assembleDebug`
  - `git diff --check`
  - APK metadata inspected: `versionName = 1.10.2`, `versionCode = 220`, output `PaykiTodo-1.10.2-debug.apk`
  - Release APK metadata inspected: `versionName = 1.10.2`, `versionCode = 220`, output `PaykiTodo-1.10.2-release.apk`
  - `apksigner verify --verbose --print-certs app/build/outputs/apk/release/PaykiTodo-1.10.2-release.apk` passed with one v2 signer
  - `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk app/build/outputs/apk/release/PaykiTodo-1.10.2-release.apk` confirmed local signing material and APK outputs are ignored
- Release-signing privacy:
  - local `keystore.properties`, `release/PaykiTodo-release.jks`, APK/AAB outputs, API keys, tokens, and private Base URLs must stay out of Git
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is now being advanced from `1.10.1 / 219` to `1.10.2 / 220` for countdown formatting, visible-event counts, widget picker metadata, and focus-surface separation.

Most important current baseline facts:

- Database version remains `17`.
- `MIGRATION_16_17` still adds countdown support for todos and recurring templates.
- Active no-DDL todos are still treated as today todos across phone daily board, Android widget board query, desktop board, and desktop todo management.
- Countdown-enabled todos use their DDL time as the target; countdown-enabled events use their start time.
- Countdown targets whose exact target time has passed are filtered out before board / widget / desktop rendering.
- Phone daily board still has a countdown card for active countdown targets.
- Desktop daily board still renders countdown targets.
- Phone and desktop daily-board countdown text uses day/hour/minute decomposition and does not display seconds.
- Phone daily board, desktop daily board, and Android 今日看板 widget count only visible unfinished today events in `今日日程`.
- Phone daily board no longer displays focus stats or free-focus entry; focus has a dedicated drawer page.
- Android `今日看板` widget no longer includes a countdown section; it remains focused on announcements, greeting, today todos, and today/tomorrow schedules.
- Android `今日看板` widget section titles use the primary dark/light text color instead of the orange header color, fixing the light-mode brown section-title readability mismatch.
- Android independent `倒数日` widget shows nearest countdown targets and distinguishes todo/event semantics:
  - todo rows show a checkbox-like circle and deep-link to the exact todo;
  - event rows hide the circle and deep-link to the exact event editor.
- Android independent `倒数日` widget has no header/date/count block and schedules minute-level refresh ticks.
- Android includes a dedicated `专注` widget for focus stats and free-focus launch.
- Android widget picker metadata now gives 今日看板 / 倒数日 / 专注 separate names, descriptions, suggested sizes, and static previews.
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
- Git history scan on 181 commits did not find committed `keystore.properties`, `.jks/.keystore/.env` files, private-key blocks, common live token prefixes, or non-placeholder password/API-key literals. Broad keyword hits were reviewed as code field names, templates, generated setting keys, editor text tokens, or `https://example.com/v1` test data.

## Documentation Health

Current docs being synchronized for `1.10.2`:

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

1. Android launcher widget rendering, resizing, picker metadata, stale launcher cache behavior, light/dark readability, and date/time refresh still require physical launcher verification.
2. The independent `倒数日` widget now schedules minute-level refreshes, but OEM launchers and battery policies may still delay widget updates.
3. Browser verification is required for desktop daily-board countdown/count/focus changes and Planning Desk location / recurrence import behavior with real AI output.
4. Settings -> AI 调用配置 model discovery still needs device-side verification with real providers.
5. Strong reminder behavior still needs real-device verification for OEM notification, vibration, lock-screen, reboot, and battery policies.
6. Very large datasets may still require further profiling; desktop split endpoints and indexed board paths remain the current performance baseline.
7. Long-running chat sessions can become unreliable, so repository docs must remain the source of truth for future continuation.

## How A New Session Should Start

1. Read `AGENTS.md`
2. Read the `docs/current/` files
3. Inspect current code and `git status`
4. Only then decide the next edit
