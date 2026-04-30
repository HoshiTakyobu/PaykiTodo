# PaykiTodo 1.5.0 Requirements Specification

## Scope

This iteration targets four feature clusters:

1. Stronger reminder reliability across foreground, background, lock screen, and unlock-return paths on different Android ROMs.
2. Calendar features better suited to class schedules and duty schedules.
3. Stronger bulk import for fast entry and migration from external text sources.
4. Advanced editing for recurring todos and recurring calendar events.

## Functional Requirements

### 1. Reminder Reliability

- The app shall record the recent reminder dispatch chain for each reminder session.
- The recorded chain shall include at least scheduling, receiver wake-up, service startup, notification emission, fullscreen attempt, accessibility fallback, reminder screen resume, and user action.
- The app shall provide a settings entry for reminder chain testing.
- The test entry shall allow the user to trigger a near-term test reminder without waiting for a full manual reminder cycle.
- The app shall provide a recent reminder diagnostics view suitable for manual debugging on real devices.
- The app shall preserve existing fullscreen reminder behavior and continue retry/fallback logic for lock screen and ROM-specific delays.

### 2. Calendar Enhancements

- The calendar shall support multiple views: timeline, week, month, and list.
- The calendar shall support semester-level recurring schedule generation.
- The calendar shall support reusable weekly templates for courses and duty schedules.
- The calendar shall support fixed-week template copy, including applying a saved weekly template to a target week.
- The calendar shall support importing schedule-like content quickly without manually creating each event.

### 3. Bulk Import

- The app shall continue to support the existing custom text grammar.
- The grammar shall be extended to support color, reminder mode, reminder lead time, all-day events, and optional group mapping.
- The bulk import workflow shall support direct paste from clipboard.
- The bulk import workflow shall support more source formats, including CSV, TSV, timetable-style text, and ICS text content.
- The import flow shall provide preview and error reporting before creation.

### 4. Advanced Recurring Editing

- The app shall support one-off exception behavior for recurring items by allowing changes to only the current occurrence.
- The app shall support split-series editing from the current occurrence forward.
- The app shall provide a series preview before creating or replacing large recurring sets.
- The app shall support one-off cancellation or skip without destroying the entire series.

## Non-Functional Requirements

- The app shall remain local-first.
- All new features shall work without network dependency at runtime.
- The app shall remain compatible with existing local data through Room migration.
- The app shall provide sufficient debugging visibility for device-only testing when ADB is unavailable.

## Out of Scope

- Cloud sync is still out of scope for this iteration.
- Third-party calendar API integration is out of scope for this iteration.
- Platform-signature or OEM-privileged system-app solutions remain out of scope.
