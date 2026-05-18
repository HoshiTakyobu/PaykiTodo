# Current Task

## Active Development Focus

The current round is PaykiTodo `1.10.1` / `versionCode 219`.

Primary goal: fix the first `1.10.0` countdown/widget and desktop Planning Desk feedback:

1. desktop Planning Desk AI preview/import must carry the same key event fields as the phone-side event flow, especially location, all-day, countdown, and recurrence;
2. the existing Android `今日看板` widget must not embed countdown-day rows;
3. countdown-day targets should live in the independent `倒数日` widget and use the App daily-board visual language more closely;
4. App / desktop countdown rows should stop using `D-N` wording and show full event time metadata.

## Completed In This Round

1. Planning Desk candidate models now carry:
   - `location`
   - `allDay`
   - `countdownEnabled`
   - `recurrence`
2. Planning Desk AI prompt and parser now ask for / read `location` and `recurrence`; AI is explicitly told not to put locations into notes and not to add an extra `@`.
3. Local Planning Desk natural event parsing can extract `@地点` or `#location` / `#地点` as an event location while keeping the saved text unchanged.
4. Phone Planning Desk preview cards now expose event location, all-day, countdown, and recurrence editing before import.
5. Desktop Web Planning Desk preview now exposes event location, all-day, countdown, recurrence type, recurrence end date, and weekly-days fields, and sends them back to `/api/planning/import`.
6. Planning Desk imports now persist those fields into `TodoDraft` / `CalendarEventDraft` on both phone and desktop paths.
7. Android `今日看板` widget no longer builds or routes countdown rows.
8. Independent Android `倒数日` widget now:
   - shows todo targets with a checkbox-like circle;
   - hides the circle for event targets;
   - uses `10d` style day text plus an `xh ym zs` remaining-time subline;
   - shows full event time such as `5月28日 10:20-12:20`;
   - removes redundant item-level `倒数日` wording.
9. App daily-board countdown rows and desktop daily-board countdown rows now use the same `Nd` direction and fuller event-time metadata.
10. Version metadata moved to `1.10.1 / 219`.

## Verification Completed This Round

Completed locally:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat :app:testDebugUnitTest` passed.
4. `./gradlew.bat :app:assembleDebug` passed.
5. `git diff --check` passed.
6. `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/debug/PaykiTodo-1.10.1-debug.apk` confirms local signing material and APK output are ignored.
7. Debug `output-metadata.json` reports `versionCode=219`, `versionName=1.10.1`, and `outputFile=PaykiTodo-1.10.1-debug.apk`.

Latest debug APK:

- `app/build/outputs/apk/debug/PaykiTodo-1.10.1-debug.apk`

## Verification Still Needed On Device / Browser

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.10.1-debug.apk` on the physical phone.
2. Phone-test countdown:
   - App daily-board countdown row shows `Nd` and full event time.
   - Todo countdown opens todo editing; event countdown opens event editing.
3. Device-test widgets:
   - `今日看板` widget shows announcements, greeting, today todos, and today/tomorrow schedules, but no countdown section.
   - Independent `倒数日` widget shows nearest targets; todo rows have a circle, event rows do not.
   - Light/dark text remains readable after launcher resize.
4. Browser-test desktop Planning Desk:
   - AI or local preview of an event can edit location, all-day, countdown, and recurrence.
   - Import creates an event with location in the event location field, not notes.
   - A source like `16:05-18:00 入党表格填写` still keeps group empty unless an explicit group marker exists.

## Immediate Practical Next Steps

1. Create a focused local commit.
2. Do not push unless the user explicitly asks.
