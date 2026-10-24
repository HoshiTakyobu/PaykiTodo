# Calendar And Account Update Contract

Version: Android `1.17.0 / versionCode 392`, Windows `1.17.0.0`, Room `33`. This document defines the implementation requirements; measured local outcomes and limitations are recorded in [VALIDATION_1.17.0.md](VALIDATION_1.17.0.md).

## Calendar behavior

| Capability | Required behavior | Verification |
| --- | --- | --- |
| Calendar body | Readable day/week/month/list views, independent mini-month browsing, today/selected-date/current-time indicators, overlap lanes and responsive date navigation | Current date/selection/scroll remain consistent through navigation, visibility and resize |
| Create and adjust | Quick draft and complete editing use explicit input state; cancellation discards changes; drag/resize and forms share time and scope rules | Save once, cancel without writes, valid start/end, keyboard/back behavior |
| Multiple calendars | Names, colors, description, order and ownership are distinct from task groups; default color and explicit event override are separate; hiding does not stop reminders | Create/edit/move/hide/delete-to-default, template/recurrence/archive roundtrip |
| Recurrence | Interval, weekday selection, ordinal/last weekday, optional cutoff and explicit valid-date policy; legacy records preserve previous semantics | Month31/leap-day/biweekly/cutoff/last-vs-fifth and current/future/all fixtures |
| All-day and reminders | All-day dates use exclusive end; timed offsets and all-day days-before/local-time are different rules; defaults and per-mode draft state are independent | Conversion/cancel/reopen, arbitrary clock values, deduplication, actual scheduling |
| Detail and copy | Detail resolves current record; copying takes the actual instance values and produces an independent nonrecurring draft | Moved instance, all-day, empty/multiple reminders, calendar/color and cancellation |
| Reminder navigation | Keep single strong-reminder window and queue; opening a reminder navigates to complete current detail | Multiple due items, earlier dismissal/later offset, hidden calendar, current detail |
| Timezones | Separate event timezone from display timezone; preserve event wall-clock anchor and absolute times; all-day uses dates | Zone conversions, DST boundaries, duration, recurrence and archive roundtrip |
| Collaboration | Calendar permissions, organizer/participant roles and response state are explicit; personal data and shared transport respect access limits | Two identities, read/write/revoke, participant update and conflict tests before claiming working sharing |

## Account and layout

- The normal login page shows no server domain. A named `选择登录服务器` action opens selection/management; Settings retains server management.
- Server selection is available when unauthenticated or after session expiry, including replacing an obsolete server address before reauthentication.
- Server changes preserve local account data and make account/server identity clear; stale sessions must not silently upload data to another server.
- Account actions use compact logical rows or wrapping groups with a clear primary operation and distinct destructive actions. Avoid a vertical stack of full-width buttons.

## Data and acceptance

Schema, repository, Android/WPF/LAN entry points, reminders, backup and encrypted snapshots must agree on new fields. Preserve old IDs, history, credentials and recoverable user data. Existing artifacts and historical test counts remain labeled by their actual version.

Acceptance must include meaningful data/migration/recurrence/reminder/timezone/collaboration checks, Android runtime screens, isolated Windows controls, actual built APK/EXE/MSI and selected screenshots. Generated outputs remain outside Git. Do not claim all-device behavior from emulator or one-machine evidence.
