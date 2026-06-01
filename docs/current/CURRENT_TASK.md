# Current Task

## Active Development Focus

Active immediate task: complete and commit the `1.13.25 / versionCode 273` quick-preview cancel/delete documentation parity follow-up round described in:

- `docs/goals/2026-06-01-paykitodo-reminder-ongoing-planning-ux-goal.md`

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, or personal backups/logs. The repository already ignores `keystore.properties`, `release/`, `*.apk`, `*.jks`, `.env*`, and local temp files.

## Current Round Scope

The user reported four active usability / correctness failures:

1. Todo and schedule reminders do not reliably open the full-screen reminder surface when the reminder time arrives.
2. A schedule that is currently happening should remain visible in the Android notification shade as a non-dismissible ongoing notification until the event ends or is otherwise cleared.
3. The todo reminder screen is still too incomplete: it needs a cancel-todo action, and snoozing after the DDL has already passed must push the DDL forward so the reminder loop remains usable.
4. Phone Planning Desk input still feels constrained to a narrow row even when AI providers are configured; the user needs a note-like free writing surface that supports long natural text, multi-line input, and preview-first recognition.

## Required Behavior

### Reminder Full-Screen Delivery

1. Todo reminders and schedule reminders must both create high-priority full-screen reminder delivery.
2. Reminder notifications must carry a full-screen intent.
3. The alarm receiver path should not rely only on a passive notification tap; it should make a best-effort standard Android launch of the reminder surface.
4. The reminder activity must be allowed to show over the lock screen and turn the screen on where Android permits it.

### Ongoing Schedule Notification

1. When an event reaches its start time, the app must post an ongoing notification for that event.
2. The notification must be non-dismissible in normal notification-shade swipes.
3. The notification must be canceled when the event ends, is completed, canceled, deleted, or is rescheduled away from the current time range.

### Todo Reminder Closed Loop

1. Todo reminder UI must include: complete, snooze 5 minutes, snooze 10 minutes, custom snooze, and cancel todo.
2. Canceling from the reminder screen cancels the todo and clears its reminder artifacts; it does not delete the row.
3. Snoozing computes `nextReminderAt = now + snoozeMinutes`.
4. For todos without DDL, snoozing only creates the next reminder and must not invent a DDL.
5. For todos with DDL, if the current DDL is already past or not later than `nextReminderAt`, snoozing must push DDL to `nextReminderAt + 1 minute`.
6. For todos with a DDL later than `nextReminderAt`, snoozing must not modify the DDL.

### Phone Planning Desk Input

1. Phone Planning Desk must expose a clear note-like free writing surface, not only a narrow per-row input.
2. The free writing surface must support multi-line text, long-line inspection/editing, and paste.
3. Recognition from that surface must still be preview-first and must not write official todos/events without user confirmation.
4. The UI copy should make AI/local fallback behavior understandable.

## Verification Required Before Completion

1. `./gradlew.bat :app:compileDebugKotlin`
2. `./gradlew.bat :app:testDebugUnitTest`
3. `git diff --check`
4. `./gradlew.bat :app:assembleDebug`
5. APK metadata check for the current app version.
6. Commit the completed round with a Chinese message that describes user-visible behavior, version bump, and validation.

## Current Status

The quick-preview cancel/delete semantics fix was implemented and committed in `afe0e5a` for `1.13.24 / versionCode 272`. The current follow-up updates the built-in Wiki and current handoff documents so user-facing instructions match the implemented behavior; the `1.13.25 / versionCode 273` debug build has also been verified locally.

Completed behavior so far:

1. Quick todo preview dialogs now expose a visible `取消待办（归档）` action instead of hiding cancellation behind delete.
2. The action sheet for active todo cards now also exposes `取消待办` directly, keeping cancel distinct from hard delete.
3. Cancel remains a history-preserving archive action; delete remains a destructive removal path.
4. Desktop Web todo preview confirms cancel/archive and hard delete with explicit history semantics.
5. Recurring todo range delete now uses the hard-delete path instead of cancel/archive.
6. Recurring todo current-instance delete records a `recurring_instance_skips` exception and then hard-deletes the row, so the occurrence does not enter history and does not regenerate.
7. Backup / restore includes `recurring_instance_skips`, so single-instance recurring-todo deletions survive restore.
8. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata check for `versionName = 1.13.24`, `versionCode = 272`.

Current follow-up behavior:

1. Built-in Wiki now says the todo card body opens details and the left circle only completes the todo.
2. Built-in Wiki now documents `取消待办（归档）` versus hard delete.
3. Built-in Wiki now documents recurring-todo single-instance delete as a non-history skip record.
4. Version metadata moved to `1.13.25 / versionCode 273`; database version remains `27`.
5. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata check for `versionName = 1.13.25`, `versionCode = 273`.
