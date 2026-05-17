# Current Task

## Active Development Focus

The current round is PaykiTodo `1.9.12` / `versionCode 206`.

Primary goal: review the `1.9.11` experience fixes, close any high-confidence behavior gaps, and record practical performance improvements / remaining performance follow-ups.

## Completed In This Round

1. Fixed a real `1.9.11` coverage gap: Android widget board-range queries now explicitly include active no-DDL todos, so widget 今日待办 matches phone daily board and desktop daily board semantics.
2. Added `todo_items` Room indices for common board/reminder/group/recurrence access paths:
   - active board todos by completed/canceled/type/missed/due
   - active board events by completed/canceled/type/start/end
   - active reminders by completed/canceled/reminder/due
   - group + DDL
   - recurring series + DDL
3. Bumped the Room database version to `14` and added `MIGRATION_13_14` to create those indices on upgraded installs.
4. Reduced desktop `/api/snapshot` overhead by reusing the already-loaded snapshot groups instead of reading groups a second time to build the group map.
5. Planning Desk plain-bullet candidates now show an explicit preview message: `普通项目符号已识别为无 DDL 待办。`
6. Settings -> `电脑同步` now states that sync will auto-close after 5 minutes without an authorized desktop connection and that opening the page without the correct token does not count as connected.
7. Settings -> `AI 调用配置` now saves valid provider changes immediately after add/edit/toggle/reorder/delete where possible, and shows an in-page warning when configuration is incomplete or has unsaved changes.
8. Version metadata moved to `1.9.12` / `versionCode 206`.
9. README / CHANGELOG / TODO / current docs are being synchronized for the `1.9.12` review pass.

## Verification Completed This Round

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat testDebugUnitTest` passed.
4. `./gradlew.bat assembleDebug` passed.
5. `git diff --check` passed.
6. `app/build/outputs/apk/debug/output-metadata.json` reports `versionCode=206`, `versionName=1.9.12`, and `outputFile=PaykiTodo-1.9.12-debug.apk`.

## Verification Still Needed Before Release

1. Device-test no-DDL todos:
   - create a normal todo with DDL disabled
   - confirm it appears under `今日待办`
   - confirm it has no reminder / recurrence behavior until DDL is added
   - confirm it appears in Android widget 今日待办 after widget refresh
2. Device-test Planning Desk:
   - write `- 想办的事`
   - run recognition
   - confirm preview explains it was recognized as a no-DDL todo
   - confirm import creates a no-DDL todo and it appears in 今日待办
3. Device-test desktop sync:
   - enable sync and connect with the correct token within 5 minutes; it should stay running
   - enable sync and do not connect with the correct token; it should auto-disable after 5 minutes
4. Device-test Settings -> AI 调用配置:
   - add/edit/toggle/reorder/delete a complete provider and leave/reopen the page; changes should persist
   - enabled incomplete providers should show the required-field warning

## Performance Findings

Completed in this round:

1. Added `todo_items` indices to reduce scans in daily board, widget board query, reminder scan, group filtering, and recurrence-series lookup.
2. Removed duplicate group reads from desktop `/api/snapshot`.

Still recommended as later performance专项:

1. Desktop `/api/snapshot` still returns all todos/events/planning notes/focus data. Split board/todos/events/planning endpoints or add range-based snapshot APIs when desktop data grows.
2. Main phone `TodoViewModel.uiState` still observes full focus-session and AI-report lists. Add today-focus aggregate Flow and paged/lazy AI report data to avoid recomputing over long histories on ordinary board/task pages.
3. Calendar timeline still deserves a separate large-data performance pass for scroll/swipe/add/edit/delete smoothness.

## Immediate Practical Next Steps

1. Commit the verified fix round locally.
2. Do not push unless the user explicitly asks.

## Commit Message Rule

PaykiTodo commit messages must use the `AGENTS.md` body format:

```text
完成内容概要：
- ...
```

The subject should describe the behavior change and must not append a version-bump tail.
