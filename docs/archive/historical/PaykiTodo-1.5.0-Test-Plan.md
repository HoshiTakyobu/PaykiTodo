# PaykiTodo 1.5.0 Test Plan

## Build Verification

- Assemble debug APK successfully.

## Reminder Diagnostics

- Create reminder chain test from settings.
- Verify recent chain logs show schedule, receiver or service dispatch, fullscreen or notification path, and user action.
- Verify logs can be cleared.

## Reminder Runtime

- Foreground reminder.
- Background reminder.
- Locked-screen reminder.
- Unlock-return reminder.
- Notification-only calendar reminder.
- Fullscreen calendar reminder.

## Calendar Views

- Timeline view render.
- Week view render.
- Month view render.
- List view render.
- Switch modes repeatedly without crash.
- Save visible week as schedule template.
- Apply saved template to a target week.
- Generate semester recurring schedule from a saved template.
- Delete schedule template.

## Bulk Import

- Custom grammar happy path.
- Clipboard paste path.
- CSV parse path.
- TSV parse path.
- Timetable text parse path.
- ICS parse path.
- Mixed invalid lines produce readable errors.
- `Group=...` custom-grammar mapping path.
- CSV / TSV `group` column path.
- ICS `CATEGORIES` mapping path.

## Recurrence Editing

- Edit only current recurring todo occurrence.
- Edit current and future occurrences.
- Edit all occurrences.
- Delete or cancel only current recurring occurrence.
- Split recurring event from current occurrence onward.
- Preview large recurring generation before confirm.
- Todo recurrence preview dialog render and count sanity.
- Calendar recurrence preview dialog render and count sanity.
