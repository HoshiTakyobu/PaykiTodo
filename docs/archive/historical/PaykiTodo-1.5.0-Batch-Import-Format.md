# PaykiTodo 1.5.0 Batch Import Format

## Supported Input Families

- Custom text grammar
- CSV
- TSV
- Timetable-style weekly text
- ICS text

## Custom Grammar Extensions

- `Color=#RRGGBB`
- `Mode=Notification|Fullscreen`
- `Remind=15m|1h|2d|off`
- `Ring=On|Off`
- `Vibrate=On|Off`
- `Group=组名`
- `Note=备注`
- `全天` as time token for all-day events

## CSV / TSV Expected Columns

- `date`
- `start`
- `end`
- `title`
- `location`
- `notes`
- `allDay`
- `color`
- `remind`
- `mode`
- `ring`
- `vibrate`
- `recurrence`
- `recurrenceEnd`
- `group`

## Timetable Text Style

- One line per recurring weekly slot.
- Recommended format:
  - `Mon, 08:00-09:35, 【课】高代, @A305, 2026-03-02, 2026-06-30, Color=#4E87E1, Remind=10m`

## ICS Scope

- First implementation supports core `VEVENT` fields:
  - `SUMMARY`
  - `DESCRIPTION`
  - `LOCATION`
  - `DTSTART`
  - `DTEND`
  - `RRULE`
- `CATEGORIES` is used as a best-effort group-name mapping source when present.
- Unsupported ICS constructs are ignored instead of crashing import.

## Current Mapping Notes

- `Group=组名` will resolve an existing group by name, case-insensitively.
- If the target group does not exist yet, the app will create it automatically with a default accent color.
- CSV / TSV imports use the `group` column for the same behavior.
- ICS imports currently use `CATEGORIES` as a coarse fallback and do not yet support complex multi-category resolution.
