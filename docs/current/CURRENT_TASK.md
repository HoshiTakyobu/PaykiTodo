# Current Task

## Active Development Focus

The current round is PaykiTodo `1.10.0` / `versionCode 218`.

Primary goal: add a countdown-day mode for important DDL / schedule targets, expose it on the phone daily board, desktop daily board, the existing Android 今日看板 widget, and a new independent 倒数日 widget, while keeping the existing widget visually aligned with the in-app daily board and omitting the focus card.

## Completed In This Round

1. Added countdown-day data support:
   - `TodoItem.countdownEnabled`
   - `RecurringTaskTemplate.countdownEnabled`
   - `TodoDraft.countdownEnabled`
   - `CalendarEventDraft.countdownEnabled`
2. Upgraded the Room database to version `17`:
   - `MIGRATION_16_17` adds `countdownEnabled` to `todo_items` and `recurring_task_templates`.
   - `MIGRATION_16_17` creates `index_todo_items_countdown`.
3. Added countdown persistence across repository, backup / restore, desktop sync JSON, and recurring template creation.
4. Added phone editor controls:
   - Todo editor can enable 倒数日 only for DDL-backed todos; turning off DDL clears countdown state.
   - Calendar event editor can enable 倒数日, using the event start date as the target.
5. Added phone daily-board countdown display:
   - countdown card shows active future/today targets.
   - Todo targets use DDL date; event targets use start date.
   - expired targets are hidden.
   - tapping rows opens the corresponding todo / event editor.
6. Added desktop Web support:
   - daily board renders countdown targets.
   - todo and event editors can save countdown state.
   - preview modals show whether countdown is enabled.
7. Updated Android widgets:
   - existing 今日看板 widget now includes a countdown section between greeting and today's todo block.
   - existing widget still omits the focus card / 专注一下 content.
   - new independent PaykiTodo 倒数日 widget shows the nearest 3 countdown targets.
   - countdown widget rows route todo targets to My Tasks and event targets to Calendar.
   - countdown widget provider declares resize bounds and periodic update metadata.
8. Added unit coverage in `DailyBoardSnapshotBuilderTest`:
   - future/today countdown targets are included.
   - expired targets are excluded.
   - no-DDL todos do not become countdown targets.
9. Updated product docs:
   - `README.md`
   - `CHANGELOG.md`
   - `TODO.md`
   - in-app Wiki
   - `docs/current/FEATURE_LEDGER.md`
   - current handoff / status docs

## Verification Completed This Round

Completed locally:

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat :app:testDebugUnitTest` passed.
4. `./gradlew.bat :app:assembleDebug` passed.
5. `git diff --check` passed.
6. `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/debug/PaykiTodo-1.10.0-debug.apk` confirms signing material and debug APK output are ignored.
7. Debug `output-metadata.json` reports `versionCode=218`, `versionName=1.10.0`, and `outputFile=PaykiTodo-1.10.0-debug.apk`.

Latest debug APK:

- `app/build/outputs/apk/debug/PaykiTodo-1.10.0-debug.apk`

## Verification Still Needed On Device / Browser

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.10.0-debug.apk` on the physical phone.
2. Phone-test countdown:
   - enable 倒数日 on a DDL todo.
   - enable 倒数日 on a calendar event.
   - confirm both appear on 每日看板.
   - confirm expired targets disappear after the target date.
   - confirm no-DDL todos cannot stay marked as countdown targets.
3. Device-test Android widgets:
   - 今日看板 widget should show announcements, greeting, countdown, today todos, and today/tomorrow schedule content without the focus card.
   - independent 倒数日 widget should show the nearest 3 active targets.
   - light/dark mode text should remain readable.
   - launcher resize should not break row layout.
4. Browser-test desktop Web:
   - first lightweight board snapshot should include countdown items when present.
   - desktop todo / event editors should preserve countdown state.
   - clicking countdown rows in desktop daily board should open the correct todo / event flow.

## Immediate Practical Next Steps

1. Create a focused commit for the `1.10.0` countdown/widget round.
2. Do not push unless the user explicitly asks.
3. Use the debug APK path above for phone-side verification unless the user requests a signed release build.
