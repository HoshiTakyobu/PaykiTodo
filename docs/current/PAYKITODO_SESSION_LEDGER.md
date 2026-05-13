# PaykiTodo Session Ledger

This file summarizes the original `【开发】PaykiTodo` session and the later continuation context in a repository-native form.

## Why This File Exists

The original PaykiTodo sessions became large and increasingly unreliable. This file converts the important project history into a stable handoff artifact.

## Initial Product Request

The original request was to build an Android 14 local single-device app that:

- opens directly to today-oriented task information
- supports todo deadlines
- supports reminder times for specific tasks
- can escalate reminders in a way closer to an alarm than a weak notification
- supports ringtone / vibration / voice style reminder delivery
- allows snooze-style handling instead of casual dismissal

The user explicitly preferred:

- local single-device first
- minimal UI initially
- strong reminder behavior over shallow polish

## Major Product Directions Added During Iteration

Over time, the user pushed the project beyond a simple todo reminder app into:

- stronger reminder reliability across Android scenarios
- task + calendar integrated workflow
- recurring tasks and recurring events
- batch import and schedule generation
- board / dashboard style home experience
- LAN desktop-side browser control for phone data
- better icon / visual identity / launch surface quality

## Implemented And Accepted Themes

The following themes were clearly implemented and repeatedly refined across the session history:

1. Strong reminder chain as a first-class capability
2. Task + calendar integration rather than separate products
3. LAN browser console instead of a heavyweight desktop client
4. Ongoing visual refinement of dashboard, calendar, launch screen, drawer, and icons
5. Local-first data management with export / import / diagnostics

## Implemented But Not Fully Settled

These areas were implemented and then repeatedly revised based on user feedback, so they should be treated as live refinement zones rather than finished design:

- app icon / launcher icon / notification icon
- launch screen and wallpaper readability
- dashboard / board visual hierarchy
- calendar view behavior and visual completion
- current-time label placement in calendar

## User-Requested But Still Worth Re-Checking

The original session shows repeated user focus on:

- Feishu-like calendar polish and completion level
- removing redundant calendar header controls
- better themed wallpapers for light and dark contexts
- clearer text contrast in dashboard, settings, and launch screens
- icon design that feels distinctive rather than generic

These should not be assumed fully closed just because related code exists.

## Last Meaningful Known Progress In The Original Main Session

Near the end of the original `【开发】PaykiTodo` session, the active focus had narrowed to:

1. move current-time label behavior back to the left time axis rather than drawing it inside the schedule area
2. replace both launcher and notification icon paths with the intended icon direction
3. add a release-signing template document

The session log shows the agent explicitly stating it had:

- identified that the current-time text was still being drawn in the schedule area
- identified that the icon resource chain was not fully switched to the intended resources
- planned to patch those two areas and add the signing template

That session then became unstable and stopped being a trustworthy execution surface.

## What This Means For Current Work

Current sessions should not assume the old main session completed those final steps cleanly. Instead they should verify the repository directly.

## Practical Rule

When reconstructing PaykiTodo state, use this priority order:

1. current code and git status
2. `docs/current/*`
3. this session ledger
4. older versioned docs only as historical reference

## Recent Repository-Native Updates

- `1.6.48` refined Settings reminder-audio UI by replacing large option button groups with compact dropdown rows.
- `1.6.48` changed percent controls for reminder audio from `±10%` buttons to a 0-100 slider plus numeric input.
- `1.6.49` corrected the `1.6.48` work-mode interpretation: 工作模式 is a quiet strong-reminder mode that suppresses outward sound by default, strengthens vibration, and keeps calendar reminders on the full-screen / accessibility fallback chain.
- `1.6.50` closed a quiet-mode edge case: 工作模式 now forces the strong vibration pattern even if the individual todo / event had vibration disabled, avoiding silent no-vibration reminders.
- `1.6.51` fixed daily-board tomorrow copy and desktop web event timeline UI: tomorrow now has an explicit no-event message, event cards are clicked to edit, inline edit/delete buttons are removed, group colors are preferred, and the separate all-day strip is hidden.
- `docs/current/UI_DESIGN_RULES.md` now records the project rule against using button-group option UIs for ordinary enum settings.

- 1.6.52 fixed desktop web event-card click editing by comparing event IDs as strings and stopping card-click propagation into blank timeline creation.

- 1.6.53 fixed desktop web cache/click/all-day edit paths, hid stale desktop-sync addresses when the service is not running, and polished daily-board/calendar header naming and layout.

- 1.6.54 fixed the desktop event editor hidden-modal failure by normalizing recurrenceWeekdays values before filling the editor; Edge CDP click simulation passed against real phone snapshot data.

- 1.6.55 removed generic editor/settings helper copy and added lightweight desktop web motion with reduced-motion fallback.

- 1.6.56 added desktop web no-DDL todo editing and removed desktop modal helper subtitles.

- 1.6.57 replaced desktop browser confirm delete prompts with an in-app dangerous-action modal.

- 1.6.59 added runtime desktop-web resource versioning, desktop-sync disabled-state self-stop protection, a Node simulated click check for desktop event editing, a smaller calendar current-time recomposition pass, daily-board normal-row cleanup, and a tighter calendar header layout.

- 1.6.60 moved desktop web todo/event interactions toward the phone-side preview model: cards open preview sheets first, and edit/delete/complete/cancel actions are launched from preview instead of inline card buttons.

- 1.6.61 aligned desktop todo/event reminder editing with the phone-side mixed reminder syntax and added todo `reminderOffsetsMinutes` support to the desktop sync API.

- 1.6.62 added display-only lunar labels to calendar timeline headers, month cells, and agenda/list date surfaces using Android ICU `ChineseCalendar`; lunar date picking and lunar recurrence remain pending.

- 1.6.63 restored direct desktop event-card editing, hardened disabled desktop-sync status addresses, tightened daily-board/calendar header UI, and added minimal YEARLY_LUNAR_DATE recurrence support while leaving the dedicated lunar date picker pending.
