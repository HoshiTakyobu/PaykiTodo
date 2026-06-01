# Current Task

## Active Development Focus

Active immediate task: continue the broader product/UX audit from the `1.13.30 / versionCode 278` local patch baseline, using the previous reminder/Planning Desk goal as the latest verified work package:

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

The quick-preview cancel/delete semantics fix was implemented and committed in `afe0e5a` for `1.13.24 / versionCode 272`. The built-in Wiki and current handoff documents were then aligned with the implemented behavior and committed in `91c9520` for `1.13.25 / versionCode 273`. The current working tree is the baseline for the next audit pass.

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

Current local follow-up patch:

1. Phone search results, Planning Desk linked todos, notification routes, and widget/deep-link todo opens now use the shared todo details preview surface instead of bypassing preview.
2. That preview exposes cancel/archive from these entry points, keeping cancel as a history-preserving archive action and delete as hard deletion.
3. The preview top action now uses an explicit `取消归档` text button instead of only a close icon.
4. Version metadata moved to `1.13.26 / versionCode 274`; database version remains `27`.
5. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata check for `versionName = 1.13.26`, `versionCode = 274`.

Current reminder/ongoing notification follow-up patch:

1. Event reminder acknowledgement and event check-in from the full-screen reminder surface now clear only event-reminder artifacts and preserve/re-schedule ongoing event notifications.
2. Accessibility fallback event reminders use the same keep-ongoing behavior for acknowledgement and check-in.
3. Ongoing event notification IDs now use the full alarm request-code hash rather than `eventId % 10000`.
4. Feature documentation now matches the implemented snooze policy for overdue/conflicting DDLs.
5. Version metadata moved to `1.13.27 / versionCode 275`; database version remains `27`.
6. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata check for `versionName = 1.13.27`, `versionCode = 275`.

Current quick-preview cancellation follow-up patch:

1. Phone todo details quick preview now says `取消待办` in the top action instead of the confusing `取消归档` wording.
2. Shared quick-preview entry points keep cancel/delete available for active todos and hide them only for completed or canceled history todos.
3. Desktop Web todo preview now has a top `取消待办` action, so cancel/archive can be used without scrolling to the bottom of the preview.
4. Version metadata moved to `1.13.28 / versionCode 276`; database version remains `27`.
5. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata check for `versionName = 1.13.28`, `versionCode = 276`.

Current Planning Desk desktop parity follow-up patch:

1. Desktop Web Planning Desk event candidates now expose `打卡追踪` in the recognition preview, matching the phone preview option.
2. The option reuses the existing `checkInEnabled` candidate field and import backend, so no data migration is needed.
3. Version metadata moved to `1.13.29 / versionCode 277`; database version remains `27`.
4. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata check for `versionName = 1.13.29`, `versionCode = 277`.

Current Planning Desk batch countdown follow-up patch:

1. Phone Planning Desk recognition preview batch setting `全部加入倒数日` now also applies to selected event candidates.
2. DDL-backed todo candidates continue to be supported; no-DDL todos remain excluded.
3. Version metadata moved to `1.13.30 / versionCode 278`; database version remains `27`.
4. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata check for `versionName = 1.13.30`, `versionCode = 278`.
