# PaykiTodo 1.5.0 Reminder Reliability Design

## Goal

Make it easier to understand why a reminder did or did not reach fullscreen while keeping the current multi-fallback behavior.

## Reminder Chain Stages

- `SCHEDULE_REQUESTED`
- `SCHEDULED`
- `SCHEDULE_FAILED`
- `RECEIVER_EXACT`
- `RECEIVER_BACKUP`
- `POLL_DISPATCH`
- `SERVICE_START`
- `SERVICE_ITEM_LOADED`
- `SERVICE_INVALID_ITEM`
- `NOTIFICATION_POSTED`
- `FULLSCREEN_ATTEMPT`
- `FULLSCREEN_ACTIVITY_LAUNCH`
- `FULLSCREEN_ACTIVITY_FAILED`
- `ACCESSIBILITY_TRIGGER`
- `ACCESSIBILITY_OVERLAY`
- `REMINDER_ACTIVITY_RESUME`
- `USER_COMPLETE`
- `USER_SNOOZE`
- `USER_CANCEL`
- `TEST_CREATED`

## Settings-Side Diagnostics

- A new settings card exposes:
  - reminder chain test
  - recent diagnostics list
  - clear diagnostics button
- Test reminder creation uses a short, explicit delay and distinct test title.

## ROM Reliability Strategy

- Keep exact alarm + backup alarm dual-path scheduling.
- Keep foreground service dispatch fallback.
- Keep active reminder session persistence.
- Keep accessibility overlay fallback for locked or blocked activity launch scenarios.
- Add more structured chain logging around these steps.
