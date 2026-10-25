# Calendar And Account Update Contract

Current local implementation: Android `1.17.3 / versionCode 395`, Windows `1.17.3.0`, Room `33`. Participant controls, authorized details and current-response refresh are implemented. Dedicated Android/Windows native invitation, acceptance, RSVP, bidirectional existing-event editing and revocation have passed; current viewer access and live search revocation are verified in [VALIDATION_1.17.3.md](VALIDATION_1.17.3.md) and [the native workflow](NATIVE_COLLABORATION_ACCEPTANCE_20261025.md). The original calendar/account baseline remains in [VALIDATION_1.17.0.md](VALIDATION_1.17.0.md), with the participant update in [VALIDATION_1.17.1.md](VALIDATION_1.17.1.md).

The 2026-10-26 follow-up corrects this document's stale version header only; it adds no product behavior or build. Physical-device and broader capability limits remain in [the acceptance audit](CALENDAR_ACCEPTANCE_AUDIT_20261025.md).

## Calendar behavior

| Capability | Required behavior | Verification |
| --- | --- | --- |
| Calendar body | Readable day/week/month/list views, independent mini-month browsing, today/selected-date/current-time indicators, overlap lanes and responsive date navigation | Current date/selection/scroll remain consistent through navigation, visibility and resize |
| Create and adjust | Quick draft and complete editing use explicit input state; cancellation discards changes; drag/resize and forms share time and scope rules | Save once, cancel without writes, valid start/end, keyboard/back behavior |
| Multiple calendars | Names, colors, description, order and ownership are distinct from task groups; default color and explicit event override are separate; hiding does not stop reminders | Create/edit/move/hide/delete-to-default, template/recurrence/archive roundtrip |
| Recurrence | Interval, weekday selection, ordinal/last weekday, optional cutoff and explicit valid-date policy; legacy records preserve previous semantics | Month31/leap-day/biweekly/cutoff/last-vs-fifth and current/future/all fixtures |
| All-day and reminders | All-day dates use exclusive end; timed offsets and all-day days-before/local-time are different rules; defaults and per-mode draft state are independent | Conversion/cancel/reopen, arbitrary clock values, deduplication, actual scheduling |
| Detail and copy | Detail resolves the current accessible record; copying takes actual instance values and produces an independent nonrecurring draft without the original participant invitation or response | Moved instance, all-day, empty/multiple reminders, calendar/color, permission change and cancellation |
| Reminder navigation | Keep single strong-reminder window and queue; opening a reminder navigates to complete current detail | Multiple due items, earlier dismissal/later offset, hidden calendar, current detail |
| Timezones | Separate event timezone from display timezone; preserve event wall-clock anchor and absolute times; all-day uses dates | Zone conversions, DST boundaries, duration, recurrence and archive roundtrip |
| Collaboration | Select event participants from accepted calendar members in a draft; existing responses belong to the participant and new occurrences start unanswered. Details show organizer (the calendar owner under the current server contract), participants and current-account response; PRIVATE viewers see only the server-authorized projection | Two identities, select/cancel/change-calendar, read/write/revoke, current response refresh, no forged RSVP, independent copy/occurrence and conflict tests before claiming working sharing |

## Account and layout

- The normal login page shows no server domain. A named `选择登录服务器` action opens selection/management; Settings retains server management.
- Server selection is available when unauthenticated or after session expiry, including replacing an obsolete server address before reauthentication.
- Server changes preserve local account data and make account/server identity clear; stale sessions must not silently upload data to another server.
- Account actions use compact logical rows or wrapping groups with a clear primary operation and distinct destructive actions. Avoid a vertical stack of full-width buttons.

## Data and acceptance

Schema, repository, Android/WPF/LAN entry points, reminders, backup and encrypted snapshots must agree on new fields. Preserve old IDs, history, credentials and recoverable user data. Existing artifacts and historical test counts remain labeled by their actual version.

Acceptance must include meaningful data/migration/recurrence/reminder/timezone/collaboration checks, Android runtime screens, isolated Windows controls, actual built APK/EXE/MSI and selected screenshots. Generated outputs remain outside Git. Do not claim all-device behavior from emulator or one-machine evidence.
