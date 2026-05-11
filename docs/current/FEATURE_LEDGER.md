# Feature Ledger

This file tracks the product at a practical level for new coding sessions.

## Implemented And In Use

### Task / Todo System

- create, edit, delete todo items
- title / notes / group / deadline / reminder fields
- no-deadline todos
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

### Calendar System

- timeline-style calendar foundation
- day / multi-day / month / agenda style views exist in code, with ongoing refinement
- normal events, all-day events, and recurring events
- event location / notes / color / reminder settings
- event preview keeps showing configured reminder offsets after reminder acknowledgement
- timeline pending event draft can be canceled by long-pressing blank timeline space and is cleared when opening an existing event
- text-based batch import support
- week-template and semester-generation related capabilities exist in the codebase and docs history
- current-time label is wired on the left time axis and remains visible even when today is off-screen, while the red current-time line remains in the schedule area and also stays visible

### Reminder System

- `AlarmManager` based scheduling
- notification reminder path
- full-screen reminder path
- foreground service and fallback chain work
- reboot / time change / timezone change recovery
- accessibility fallback path
- reminder diagnostics and settings-side tooling exist
- reminder and desktop-sync foreground notifications both use the dedicated `ic_stat_payki_todo` small icon resource
- calendar event acknowledgement preserves the configured reminder offsets instead of clearing the event's reminder setup

### Packaging / Identity

- adaptive launcher icon now uses a dedicated safe-zone PaykiTodo vector foreground instead of reusing the full raster art as the foreground layer
- monochrome themed launcher icon now follows the same PaykiTodo logo silhouette as the colored launcher icon
- release-signing information template exists under `docs/PaykiTodo-Release-Signing-Template.md`

### Data / Backup / Diagnostics

- Room-based local storage
- JSON import / export
- auto-backup related support
- crash log viewing / copying
- in-app wiki assets

### Desktop / LAN Assistance

- LAN browser-based desktop sync console exists
- phone-side HTTP serving model exists
- browser can perform limited data operations against the phone-side dataset
- desktop-sync foreground notification can be tapped to open the in-app Settings -> Desktop Sync panel
- desktop web destructive delete actions require confirmation before DELETE requests are sent

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
