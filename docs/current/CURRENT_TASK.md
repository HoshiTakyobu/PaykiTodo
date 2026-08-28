# Current Task

## Active Development Focus

The `1.15.12 / versionCode 354` reminder and calendar semantics round is complete in code and verification. The remaining local action is the required focused commit; do not push unless the user explicitly asks.

## Completed Scope

1. Unified reminder dismissal now removes the matching notification, foreground reminder service, sound/vibration session, accessibility overlay, dispatch state, and full-screen handoff markers after todo completion/cancellation or stale-notification entry.
2. Calendar events are no longer completable. Phone UI, repository completion paths, Planning Desk synchronization, and Desktop Sync reject the event-completion state; event cancellation remains history-preserving.
3. Phone and Desktop Web event details can create a copy that retains editable fields but does not retain the source database ID or multi-slot bundle identity.
4. Phone and Desktop Web todo/event editors use the same multi-select reminder presets plus custom input. Check-in-enabled events have independent check-in reminder offsets.
5. Ordinary and check-in reminders are merged and deduplicated for scheduling, and persist through recurring templates, multi-slot events, weekly templates, backup/restore, and Desktop Sync.
6. Event/group color selection uses the same ordered 12-color palette on phone and desktop, with compact rounded-square swatches.
7. Database version is `30` with `MIGRATION_29_30`; app version is `1.15.12 / 354`.

## Validation

1. 26 unit-test suites / 145 tests passed with zero failures.
2. `:app:lintDebug`, `:app:assembleDebug`, `node --check`, and `git diff --check` passed.
3. APK metadata confirms `PaykiTodo-1.15.12-debug.apk`, `versionName 1.15.12`, and `versionCode 354`.
4. Room schema `30.json` contains the new todo-item and recurring-template check-in reminder columns.
5. `emulator-5554` / Pixel_8 verified accessibility enabled/bound, full-screen + notification coexistence, completion cleanup, event copy, no event completion action, reminder menus, check-in reminder control, and color swatches.

## Remaining Before Handoff

1. Create one detailed Chinese feature commit.
2. Do not push unless the user explicitly asks.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, emulator screenshots, test databases, or personal backups/logs.
