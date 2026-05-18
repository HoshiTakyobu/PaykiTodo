# PaykiTodo 1.5.0 Data Model and Migration

## Database Version

- Upgrade Room database version from `6` to `7`.

## New Entities

### `ReminderChainLog`

- Purpose: persistent reminder dispatch-chain diagnostics.
- Key fields:
  - `id`
  - `todoId`
  - `chainKey`
  - `source`
  - `stage`
  - `status`
  - `message`
  - `reminderAtMillis`
  - `createdAtMillis`

### `ScheduleTemplate`

- Purpose: reusable weekly and semester-oriented schedule templates.
- Key fields:
  - `id`
  - `name`
  - `templateType`
  - `payloadJson`
  - `accentColorHex`
  - `createdAtMillis`
  - `updatedAtMillis`

## Migration `6 -> 7`

- Create `reminder_chain_logs`.
- Create `schedule_templates`.
- Add indexes for recent reminder lookup and template ordering.

## Backup Impact

- Backup JSON shall include reminder diagnostics logs and schedule templates.
- Import shall restore them together with tasks, groups, templates, and settings.

## Compatibility Notes

- Existing user data remains intact.
- New tables are additive and do not rewrite current task or recurring template rows.
