# Project Status

## Current Baseline

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Package name: `com.paykitodo.app`
- Target platform: Android 14 / API 34
- Current version in code:
  - `versionName = "1.10.3"`
  - `versionCode = 221`

## Current Build Facts

- Latest `1.10.2` signed release APK:
  - `app/build/outputs/apk/release/PaykiTodo-1.10.2-release.apk`
  - Published as GitHub Release tag `v1.10.2`
- Latest signed release APK built in this round:
  - `app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk`
- Latest debug APK built in this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.10.3-debug.apk`
- Verification completed for this `1.10.3` continuation:
  - `node --check app/src/main/assets/desktop-web/app.js`
  - `./gradlew.bat :app:testDebugUnitTest`
  - `git diff --check`
  - `./gradlew.bat :app:assembleRelease`
  - APK metadata inspected: `versionName = 1.10.3`, `versionCode = 221`, output `PaykiTodo-1.10.3-release.apk`
  - `apksigner verify --verbose --print-certs app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk` passed with one v2 signer
  - `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/release/PaykiTodo-1.10.3-release.apk app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk` confirmed local signing material and APK outputs are ignored
  - `./gradlew.bat :app:assembleDebug`
  - Debug APK metadata inspected: `versionName = 1.10.3`, `versionCode = 221`, output `PaykiTodo-1.10.3-debug.apk`
  - Widget continuation: `./gradlew.bat :app:compileDebugKotlin`
  - Widget continuation: static search confirmed removed widget header IDs and old fixed countdown-row IDs are no longer referenced
  - Widget continuation: `git diff --check`
  - Widget continuation: `./gradlew.bat :app:assembleDebug`
- Release-signing privacy:
  - local `keystore.properties`, `release/PaykiTodo-release.jks`, APK/AAB outputs, API keys, tokens, and private Base URLs must stay out of Git
- Current build environment expectation:
  - prefer Android Studio bundled `jbr`
  - avoid random system Java overrides when building this repo

## Current Worktree Reality

The repository is implementing the larger `1.11.0 / versionCode 222` goal from the current `1.10.3 / 221` baseline. The Android widget requirements have already landed in the current baseline. The first `1.11.0` goal slice removes the former focus / pomodoro mode and prepares the database for later check-in and multi-group todo work. The second small `1.11.0` slice adds database schema export, debug-only Compose tooling, structured startup initialization, and AI-report retention cleanup. The full `1.11.0` version bump is still pending.

Most important current baseline facts:

- Database version is now `18` in the working tree. `MIGRATION_17_18` drops the old `focus_sessions` table, creates `event_check_ins`, creates `todo_group_tags`, adds `checkInEnabled` and `totalCheckInMinutes` to `todo_items`, backfills todo group tags, and merges the old default `专注` group into `例行`.
- Active no-DDL todos are still treated as today todos across phone daily board, Android widget board query, desktop board, and desktop todo management.
- Countdown-enabled todos use their DDL time as the target; countdown-enabled events use their start time.
- Countdown targets whose exact target time has passed are filtered out before board / widget / desktop rendering.
- Planning Desk event import defaults to event-only. A linked todo is created only if the user explicitly enables the preview option.
- Planning Desk linked todos no longer receive a fixed auto-generated note.
- Planning Desk local parser and AI cleanup both treat `@地点` as event location text, not a required prefix or title content.
- Desktop-web event editor now exposes compact preset color swatches matching the phone editor.
- `今日看板` Android widget no longer renders its fixed top menu/title/date header; the RemoteViews list fills the widget body.
- `今日看板` and independent `倒数日` widgets both set `updatePeriodMillis=0` and use provider-owned `AlarmManager` minute ticks to refresh RemoteViews collections.
- Independent `倒数日` widget is now a scrollable `RemoteViewsService` / `ListView` widget rather than three fixed rows, with multi-line daily-board-style rows and direct todo/event detail deep links.
- Former focus / pomodoro mode is removed from the working tree: no drawer entry, settings block, FocusActivity, focus widget, focus DAO/repository API, desktop sync focus stats, AI-report focus fields, or backup focus-session export remains.
- Existing old backups that contain `focusSessions` are ignored rather than failing import.
- Room schema export is enabled and `app/schemas/com.example.todoalarm.data.AppDatabase/18.json` records the database-18 structure.
- Compose `ui-tooling-preview` is now scoped to `debugImplementation`, so release builds do not carry the preview tooling dependency.
- AI 日报 / 周报 settings include a report-retention dropdown; generating a report purges older archived reports according to 30-day / 90-day / 365-day / forever retention.
- Backup / restore preserves the AI report retention policy while still excluding AI API Keys.
- Application startup initialization uses an application-level `SupervisorJob` scope and records non-fatal initialization failures through `CrashLogger.recordNonFatal`.
- Desktop sync keeps the 4 MB request-body limit and returns HTTP 413 for oversized requests.
- `./gradlew.bat :app:compileDebugKotlin` and `git diff --check` have passed for the schema / report-retention / startup-scope slice; no new APK has been built after that slice yet.

## Immediate Manual Verification Targets

1. Android launcher widgets:
   - Add / resize `今日看板` and confirm there is no top header row.
   - Confirm `今日看板` content updates after minute ticks and across date changes.
   - Add / resize `倒数日` and confirm rows scroll, long titles wrap, and row taps open the matching todo / event detail.
2. Planning Desk local rule import:
   - `10:00-12:00 自习 @图书馆3楼`
   - `10:00-12:00, 【课程】习思想，"@主楼B1-412"`
   - `10:00-12:00 自习 地点：图书馆3楼`
3. Planning Desk AI import:
   - verify title, location, group, and linked-todo checkbox defaults.
4. Desktop web:
   - event color preset swatches update the color input and persist after saving.
5. Regression:
   - normal event import does not create an unwanted todo;
   - manually linked todo has DDL equal to event end time and no fixed generated note.
