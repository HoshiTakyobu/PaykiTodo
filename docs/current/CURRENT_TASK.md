# Current Task

## Active Development Focus

The current round is PaykiTodo `1.8.4` / `versionCode 187`, focused on the `docs/goals/2026-05-16-paykitodo-1.8.4-goal.md` follow-up:

1. Make the desktop-web announcement banner readable for long / multiple announcements through marquee behavior.
2. Add a system-follow dark theme to the desktop-web console.
3. Make the Android `今日看板` widget rows deep-link to the corresponding todo, event, or Planning Desk note.
4. Collapse the widget's completely empty board into one clear empty-state row.
5. Hide the launch screen when first real UI state is ready, while keeping an 800ms fallback cap.

## Completed In 1.8.4

1. Desktop-web announcements now add `marquee-needed` only when the combined text is longer than 60 characters.
2. Long desktop-web announcements scroll horizontally and pause on hover; short announcements remain static.
3. Desktop-web CSS now defines light/dark theme variables and uses `@media (prefers-color-scheme: dark)` for system-follow dark mode.
4. Desktop-web timeline cards, event cards, modal sheets, summary cards, sidebar cards, tab buttons, Planning Desk surfaces, inputs, and announcement banner now use theme variables where practical.
5. Widget rows now have explicit `WidgetRowType` values plus `itemId` / `sourceNoteId`.
6. Widget `PendingIntentTemplate` is mutable on Android 12+ so RemoteViews fill-in extras can carry row targets.
7. Widget todo rows fill `MainActivity.EXTRA_OPEN_TODO_ID`; event rows fill `EXTRA_OPEN_EVENT_ID`; announcement rows fill `EXTRA_OPEN_PLANNING_NOTE_ID`.
8. `MainActivity` and `DashboardLaunchRoute` now parse todo/event/planning-note targets in addition to Settings deep links.
9. Dashboard route handling opens targeted todos in the todo editor, targeted events in Calendar detail preview, and targeted announcement notes in Planning Desk.
10. Widget completely empty state now shows only `今天没有安排，去 App 创建吧`.
11. `TodoUiState` now exposes `dataReady`; `DashboardScreen` hides the launch screen as soon as data is ready and otherwise falls back after 800ms.
12. Version metadata is now `1.8.4` / `versionCode 187`.

## Verification Completed This Round

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `./gradlew.bat testDebugUnitTest`
3. `./gradlew.bat assembleDebug`
4. `git diff --check`

## Immediate Practical Next Steps

Before final completion, create a focused local commit with the required `完成内容概要：` bullet-list body. Do not push unless the user explicitly asks.

After installing the `1.8.4` APK on device, verify:

1. Desktop web:
   - short announcements stay static;
   - long / multiple announcements scroll and pause on hover;
   - system dark mode makes the full desktop console dark and readable.
2. Android launcher widget:
   - completely empty board shows a single empty row;
   - todo row opens the matching todo;
   - event row opens Calendar and the matching event detail;
   - announcement row opens the source Planning Desk note;
   - header / empty rows return to the default daily board.
3. Startup:
   - launch screen disappears when data is ready and never waits longer than about 800ms.

## Commit Message Rule

PaykiTodo commit messages must use the `AGENTS.md` body format:

```text
完成内容概要：
- ...
```

The subject should describe the behavior change and must not append a version-bump tail.

## Current External Dependency

The active objective came from `docs/goals/2026-05-16-paykitodo-1.8.4-goal.md`. Do not push unless the user explicitly asks.
