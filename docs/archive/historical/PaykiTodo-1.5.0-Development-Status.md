# PaykiTodo 1.6.0 Development Status

## Completed In This Round

### Desktop Sync

- Added a phone-side LAN desktop sync console.
- Added a lightweight in-app HTTP server.
- Added a desktop web UI served directly by the phone app.
- Added settings-side desktop sync entry with enable switch, LAN addresses, and access token.
- Added a foreground service to keep the sync console alive when enabled.
- Added reboot recovery for the desktop sync service.
- Added first-pass desktop-side operations:
  - list todos
  - list calendar events
  - create simple todo items
  - create simple calendar events
  - complete todo items
  - cancel todo items
  - delete calendar events
- Reused the existing Room database, reminder scheduling, and auto-backup flows.

### Reminder Reliability

- Added persistent reminder chain diagnostics storage with Room.
- Added chain logging across scheduler, receiver, foreground service, accessibility fallback, overlay, and reminder activity.
- Added settings-side reminder chain test entry.
- Added settings-side recent diagnostics preview and clear action.
- Kept existing active reminder session, retry, and accessibility fallback behavior.

### Calendar Enhancements

- Added calendar multi-view mode switch skeleton:
  - timeline
  - week
  - month
  - list
- Preserved the existing detailed timeline view.
- Added simpler alternative views for quick browse and debugging.
- Added week-template save flow directly inside calendar.
- Added template-manager dialog inside calendar.
- Added apply-template-to-target-week flow.
- Added semester-level recurring schedule generation from saved week templates.
- Added reusable weekly payload schema for course schedules and duty schedules.

### Bulk Import

- Kept existing custom grammar.
- Added format selection and auto-detection.
- Added clipboard paste entry.
- Added CSV import path.
- Added TSV import path.
- Added first-pass ICS import path.
- Added custom-grammar `Group=...` support.
- Added CSV / TSV `group` column support.
- Added ICS `CATEGORIES` to group-name mapping fallback.

### Advanced Recurring Editing

- Fixed recurring edit scope `CURRENT` so it now updates only the current occurrence instead of doing nothing.
- Applied the same fix to recurring calendar events.
- Added recurring preview dialog for todo generation.
- Added recurring preview dialog for calendar-event generation.

### Engineering Artifacts

- Added requirements, architecture, migration, reminder design, import format, and test-plan documents.
- Added backup coverage for reminder diagnostics logs and schedule templates.

## Current Limitations

- Week/month/list views are functional first-pass views, not full Feishu-level polished views.
- ICS support is first-pass and focuses on common `VEVENT` fields only.
- Reminder diagnostics list is intentionally compact and currently lives inside settings.
- Schedule-template manager is functional but still lightweight, and does not yet provide deep editing of template payload rows.
- Semester generator currently uses saved weekly templates as input and does not yet provide a dedicated “guided course table wizard”.
- Recurring preview exists, but single-occurrence skip/pause and split-series visualization are not yet fully productized.

## Build Verification

- `assembleDebug` succeeds when `JAVA_HOME` points to Android Studio bundled `jbr`.
- Current debug APK output:
  - `app/build/outputs/apk/debug/PaykiTodo-1.6.0-debug.apk`

## Environment Note

- The machine default Java is JDK 25, which breaks the current Gradle Kotlin DSL toolchain.
- Use Android Studio bundled JBR for builds in this workspace.
