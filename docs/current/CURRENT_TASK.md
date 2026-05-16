# Current Task

## Active Development Focus

The current round is PaykiTodo `1.8.3` / `versionCode 186`, focused on cleanup and parity after `1.8.2`:

1. Remove the old Settings-backed announcement storage now that announcements live in Planning Desk.
2. Make Planning Desk announcements easier to write, preview, sort, and see from desktop web.
3. Make the Android `今日看板` widget more robust: dark-mode colors, narrower data loading, no duplicate update path, and confirmed adaptive row height.
4. Update AI-recognition verification docs for the `1.8.1` trigger tightening and `1.8.2` Base URL endpoint fallback behavior.

## Completed In 1.8.3

1. Removed legacy `announcementText / announcementStartDate / announcementEndDate` fields from `AppSettings`.
2. Removed old announcement backup serialization / deserialization; old backup JSON fields are ignored without error.
3. Added one-time cleanup for legacy announcement SharedPreferences keys.
4. Removed `TodoWidgetProvider.onReceive` duplicate refresh handling; default `AppWidgetProvider` routing now handles update broadcasts once.
5. Confirmed widget rows remain adaptive-height (`wrap_content`, `maxLines=2`) and resizable metadata remains present.
6. Relaxed Planning Desk announcement parsing:
   - `#公告 ...`
   - `- [ ] #公告 ...`
   - `> #公告 ...`
   - inline `今日提醒：#公告 ...`
   - `> [!公告] ...`
   - `公告: ...`
7. Announcement display text strips trailing `#imported`, `#group ...`, and ordinary tail hashtags.
8. Announcement sorting now prefers date-scoped active announcements, newest start date first, with long-running announcements last.
9. Planning Desk Markdown preview renders announcement lines with orange border, campaign icon, `全局公告` pill, and date range pill; tapping the row jumps back to the source line.
10. Android widget colors moved into `res/values/colors.xml` and `res/values-night/colors.xml`, with matching night drawables.
11. Widget `TodoWidgetService` reloads colors on every `onDataSetChanged` and uses `ContextCompat.getColor`.
12. Added a board-range DAO query for widget refresh so it no longer pulls every historical todo/event.
13. Desktop web `/api/snapshot` now includes active announcements; the web UI renders a top orange announcement banner.
14. `docs/current/AI_RECOGNITION_VERIFICATION.md` now documents:
    - `1.8.1` trigger tightening
    - desktop import no longer auto-parses
    - `1.8.2` endpoint candidates and next-endpoint conditions
    - Base URL manual test cases
15. Version metadata is now `1.8.3` / `versionCode 186`.

## Verification Completed This Round

1. `node --check app/src/main/assets/desktop-web/app.js` succeeded.
2. `./gradlew.bat testDebugUnitTest` succeeded after parser / DAO / resource changes.
3. `./gradlew.bat assembleDebug` succeeded and produced `app/build/outputs/apk/debug/PaykiTodo-1.8.3-debug.apk`.
4. `git diff --check` succeeded.

## Immediate Practical Next Steps

Before final completion, create a focused local commit with a concrete Chinese message; do not push unless the user asks.

After installing the `1.8.3` APK on device, verify:

1. Planning Desk announcements:
   - `- [ ] #公告 ...`
   - `> #公告 ...`
   - inline `#公告 ...`
   - tail `#imported` cleanup
   - newest date-range announcement before older / long-running announcements
   - preview orange announcement card jumps back to source line
2. Desktop web:
   - active announcement appears below the topbar after browser refresh
3. Android launcher widget:
   - system dark mode readability
   - resize behavior
   - today/tomorrow board rows still correct
   - refresh after todo/event/planning-note changes

## Commit Message Rule

PaykiTodo commit messages should describe product behavior changes and bug/debug reasoning. They should not primarily document push state, validation commands, or vague process notes. Commit subjects must not append version-bump tails such as `并升级到1.7.11`.

## Current External Dependency

The active objective came from `docs/goals/2026-05-16-paykitodo-1.8.3-goal.md`. Do not push unless the user explicitly asks.
