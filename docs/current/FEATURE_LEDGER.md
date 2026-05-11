# Feature Ledger

This file tracks the product at a practical level for new coding sessions.

## Implemented And In Use

### Task / Todo System

- create, edit, delete todo items
- title / notes / group / deadline / multi-reminder fields
- no-deadline todos
- lightweight comma-based todo batch import with preview validation
- todo batch-import DDL supports same-day clock input such as `16:30` / `16：30`
- My Tasks exposes todo batch import beside the bottom-right new-todo button instead of as a top content row
- complete / cancel / restore flows
- active todo preview now uses the same bottom-sheet visual language as calendar event preview
- active todo card body opens preview; completion is isolated to the checkbox to avoid accidental completion
- recurring task support
- grouped task filtering
- three-zone home logic: overdue / today / upcoming
- board-style daily overview entry exists and can show today's todos directly

### Board / Dashboard

- dedicated daily board entry exists in the drawer and is the default home section
- board todo block includes missed active todos and today's normal todos
- board view can show today's todos and today's / tomorrow's schedule summary together
- board today's schedule hides timed events after they have ended
- board currently running events can be visually highlighted with a gold outline and subtle glow
- board greeting card supports compact collapse / expand behavior
- board background now uses separate light and dark image resources
- board schedule rows align the left color strip to the measured height of the event text block
- board surface intentionally does not expose add / batch-add buttons

### Calendar System

- timeline-style calendar foundation
- day / multi-day / month / agenda style views exist in code, with ongoing refinement
- normal events, all-day events, and recurring events
- event location / notes / color / reminder settings
- calendar reminder editing accepts the same comma-separated multi-reminder syntax as todos
- event preview keeps showing configured reminder offsets after reminder acknowledgement
- timeline pending event draft can be canceled by long-pressing blank timeline space and is cleared when opening an existing event
- text-based batch import support
- week-template and semester-generation related capabilities exist in the codebase and docs history
- current-time label is wired on the left time axis and remains visible even when today is off-screen, while the red current-time line remains in the schedule area and also stays visible

### Reminder System

- `AlarmManager` based scheduling
- todos and calendar events can store and schedule multiple configured reminder offsets
- custom snooze input can parse either minutes or a concrete future time
- notification reminder path
- full-screen reminder path
- foreground service and fallback chain work
- reboot / time change / timezone change recovery
- accessibility fallback path
- reminder diagnostics and settings-side tooling exist
- reminder and desktop-sync foreground notifications both use the dedicated `ic_stat_payki_todo` small icon resource
- calendar event acknowledgement preserves the configured reminder offsets instead of clearing the event's reminder setup

### Packaging / Identity

- adaptive launcher icon foreground is wired directly to the picture resource `ic_launcher_art`
- old vector mark launcher resources have been removed to prevent accidentally reverting to the wrong icon
- raster launcher art is also used by in-app launch / drawer surfaces
- picture launcher art is opaque pure white and scaled down inside the 512px canvas to reduce desktop mask crowding
- release-signing information template exists under `docs/PaykiTodo-Release-Signing-Template.md`

### Data / Backup / Diagnostics

- Room-based local storage
- JSON import / export
- auto-backup related support
- crash log viewing / copying
- in-app wiki assets
- in-app Wiki sidebar navigation works through local WebView JavaScript
- in-app Wiki documents current reminder, batch-import, and snooze input syntax

### Input Help

- reminder, batch-import, and custom-snooze input fields expose nearby question-mark syntax help
- shared input-help dialog explains valid examples and invalid-value behavior

### Desktop / LAN Assistance

- LAN browser-based desktop sync console exists
- phone-side HTTP serving model exists
- browser can perform limited data operations against the phone-side dataset
- desktop-sync foreground notification can be tapped to open the in-app Settings -> Desktop Sync panel
- desktop web destructive delete actions require confirmation before DELETE requests are sent
- desktop web all-day events can span multiple visible days as one continuous horizontal bar

### Destructive Action Safety

- active todo deletion asks for confirmation
- calendar event deletion asks for confirmation
- schedule-template deletion asks for confirmation
- group deletion asks for confirmation
- desktop web delete buttons ask for browser confirmation
- phone-side delete confirmations share a refined dangerous-action bottom sheet with red icon, message card, and red confirm button

## Implemented But Still Being Polished

- board / dashboard experience details and readability tuning
- calendar interaction polish
- final launcher / themed icon / notification icon surface verification on device
- default snooze picker behavior and feel
- visual consistency across settings / drawer / launch screen / board
- final device-side validation for the 1.6.30 todo preview and desktop-sync notification route

## Pending / Ongoing Direction

- stronger reminder reliability across vendor ROM differences
- more complete desktop-side operations
- cleaner release / signing / versioning process
- tighter documentation so a new session can reconstruct state without old transcripts

## Notes

This is not a perfect specification. It is a working ledger so new sessions can quickly answer:

- what the app already does
- what is partially done
- what is still under active refinement
