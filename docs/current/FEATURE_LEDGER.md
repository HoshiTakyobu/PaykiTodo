# Feature Ledger

This file tracks the product at a practical level for new coding sessions.

## Implemented And In Use

### Task / Todo System

- create, edit, delete todo items
- title / notes / group / deadline / reminder fields
- no-deadline todos
- complete / cancel / restore flows
- recurring task support
- grouped task filtering
- three-zone home logic: overdue / today / upcoming
- board-style daily overview entry exists and can show today's todos directly

### Board / Dashboard

- dedicated daily board entry exists in the drawer and is the default home section
- board view can show today's todos and today's / tomorrow's schedule summary together
- board greeting card supports compact collapse / expand behavior
- board background now uses separate light and dark image resources

### Calendar System

- timeline-style calendar foundation
- day / multi-day / month / agenda style views exist in code, with ongoing refinement
- normal events, all-day events, and recurring events
- event location / notes / color / reminder settings
- text-based batch import support
- week-template and semester-generation related capabilities exist in the codebase and docs history
- current-time label is wired on the left time axis while the red current-time line remains in the schedule area

### Reminder System

- `AlarmManager` based scheduling
- notification reminder path
- full-screen reminder path
- foreground service and fallback chain work
- reboot / time change / timezone change recovery
- accessibility fallback path
- reminder diagnostics and settings-side tooling exist
- reminder and desktop-sync foreground notifications both use the dedicated `ic_stat_payki_todo` small icon resource

### Packaging / Identity

- adaptive launcher icon XML now points to the current PaykiTodo mark resources
- monochrome themed launcher icon resource exists in the current worktree
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

## Implemented But Still Being Polished

- board / dashboard experience details and readability tuning
- calendar interaction polish
- final launcher / themed icon / notification icon surface verification on device
- default snooze picker behavior and feel
- visual consistency across settings / drawer / launch screen / board

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
