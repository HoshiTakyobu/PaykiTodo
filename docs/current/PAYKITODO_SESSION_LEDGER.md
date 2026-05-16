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
- 1.6.64 added lunar parenthesized labels to phone-side todo/event editor date rows, so picked Gregorian dates immediately show the corresponding lunar date while the dedicated lunar picker remains pending.
- 1.6.65 added desktop-web card-style date/time previews for todo DDL and event start/end inputs, moving the editor closer to the phone-side time-card interaction.
- 1.6.66 hardened desktop-web existing-event click editing by binding edit handlers directly to rendered event nodes and making timeline guide lines ignore pointer events; live access to `http://192.168.0.100:18765/` timed out from the workstation, so actual-browser verification remains pending after installing the APK.
- 1.6.67 changed the phone Settings -> Desktop Sync address area to stay collapsed by default while the sync service is running, so the LAN URL is only shown after the user taps "显示连接地址" and can be hidden again.
- 1.6.68 removed redundant explanatory copy from the phone todo editor and reminder-audio settings while preserving necessary warnings, validation messages, and syntax help.
- 1.6.69 tightened the daily-board schedule row layout so normal rows stay borderless and the left color strip sits closer to the row edge, with in-progress rows keeping the gold treatment.
- 1.6.70 split the calendar browser header into a title row and a separate action row so month titles are no longer squeezed out by compact buttons on narrow screens.
- 1.6.71 tightened the phone Settings -> Desktop Sync section further so the LAN URL stays hidden unless the user explicitly expands it.
- 1.6.72 tightened the desktop-web todo/event editor sheets into a narrower single-column layout to better resemble the phone-side bottom sheet.
- 1.6.73 added stable Compose keys around calendar timed-event cards and all-day pills to reduce needless node recreation during timeline scrolling and event updates.
- 1.6.74 tightened desktop-web todo/event preview sheets so their width, header columns, and body spacing align more closely with the editor sheets and phone-side bottom-sheet language.
- 1.6.75 changed desktop event cards back to preview-first interaction, added all-day event lunar start/end picking, softened calendar header title clipping, and tightened daily-board event-row color-strip spacing. Build verified with Android Studio bundled JBR.
- 1.6.76 added a ViewModel refresh tick for desktop-sync status after enabling/disabling sync or rotating the key, so Settings does not keep showing stale access-address state after user actions.
- 1.6.77 removed redundant all-day explanatory copy from the calendar event editor while preserving the all-day switch and lunar start/end date controls.
- 1.6.78 extended compact lunar start/end picking from all-day events to timed events, preserving the original clock time while replacing the selected date.
- Emulator QA note: SDK emulator Pixel_8 exists and was launched hidden, but no emulator/device appeared in adb devices within 90 seconds, so UI screenshot verification is still blocked.
- 1.6.79 added role/button semantics, keyboard focus, and aria labels to desktop timed event cards so keyboard users can reach the preview path.
- 1.6.80 updated in-app Wiki desktop-sync instructions to match the explicit address expansion step and current desktop preview/edit capabilities.
- Live desktop sync verification: http://192.168.0.100:18765/ served versioned 1.6.80 web assets; live app.js SHA-256 matched local asset; Node DOM simulation with live snapshot event 15 observed preview -> edit -> PUT /api/events/15.
- 1.6.81 restored direct desktop-web event-card editing after the preview-first path proved too hard to discover; local Node DOM simulation with live phone snapshot event 15 verified card click -> event editor with populated title/editingEventId and no preview modal. It also hides the Settings desktop-sync address section while sync is disabled, tightens daily-board color-strip spacing, and changes the calendar header to a flexible year/month pill plus compact actions.
- 1.6.82 restored the Settings tone panel with built-in/system notification tone choices, added compact todo lunar DDL picking via a shared LunarDatePickerDialog, gave the calendar header title a minimum width with an icon-only batch action, and loosened daily-board schedule row spacing around the left color strip.
- 1.6.83 upgraded todo lunar DDL picking to a wheel-style picker, split the calendar browser header into a dedicated month-title row plus action row to stop `2026年5月` clipping, kept the built-in/system tone panel, tightened daily-board schedule-row spacing, and synchronized README / Wiki / current docs.
- 1.7.0 implemented Planning Desk Phase 1: Room-backed multi-document Markdown planning notes, local rule-based parsing, phone-side Planning Desk editor/preview/import, desktop-web Planning Desk tab and `/api/planning/*` routes, backup/restore inclusion, and documentation/Wiki synchronization. Build verified with Android Studio bundled JBR. The feature was promoted from the provisional 1.6.84 patch version because Planning Desk is a major user-facing workflow.
- 1.7.1 implemented Planning Desk Phase 2 usability improvements: phone document search, editable phone preview cards, shared edited-candidate import models, automatic `#imported` write-back after successful import, desktop-web editable preview cards, `/api/planning/import` edited-candidate support, parser marker tests, and synchronized README / Wiki / current docs. Build verification should use Android Studio bundled JBR.
- 1.7.2 implemented phone-side Planning Desk Markdown rendering: rendered headings, task checkboxes, subtask indentation, tag pills, imported-state pills, and raw Markdown checkbox toggling from reading mode. AI planning assistant guidance was documented for future work, with local rules first and preview-first confirmation. Build verification should use Android Studio bundled JBR.
- 1.7.3 hotfixed the 1.7.2 startup crash risk by making Planning Desk always open in raw `编辑全文` mode first, keeping Markdown preview opt-in, adding render failure fallback, and replacing experimental FlowRow preview layout with simpler Row/Column composition. Build verification should use Android Studio bundled JBR.
- 1.7.4 emergency-stabilized the Planning Desk after both 1.7.2 and 1.7.3 still crashed on device launch: phone-side Markdown rendering was rolled back to the stable 1.7.1 raw editor, while Phase 2 document search, editable import preview, automatic #imported write-back, and desktop-web Planning Desk behavior were preserved. Version metadata moved to 1.7.4 / versionCode 161 and build verification should use Android Studio bundled JBR.
- 1.7.5 emergency-fixed the remaining launch crash after 1.7.4 by repairing the Planning Desk Room migration: database version moved to 10, MIGRATION_8_9 now creates a planning_notes table matching the PlanningNote entity, and MIGRATION_9_10 rebuilds mismatched 1.7.0-1.7.4 planning_notes tables. The phone-side Markdown renderer remains reverted. Build verification used Android Studio bundled JBR.
- 1.7.6 confirmed the 1.7.5 startup crash repair path and began Planning Desk phone UI polish: solid-color editor/document cards replaced semi-transparent surfaces, shortcut chips became a horizontal toolbar, the document picker became scrollable, and the recognition preview sheet was tightened. The phone-side Markdown renderer remains reverted and 1.7.x must not be pushed unless the user asks.
- 1.7.7 added an in-screen Planning Desk help entry and bottom sheet with practical workflow guidance and copyable examples for todos, subtasks, DDL, reminders, groups, and schedule ranges. It keeps the raw editor, 1.7.6 pure-color UI polish, 1.7.5 Room migration repair, and 1.7.4 Markdown-render rollback.
- 1.7.8 restored the Planning Desk Markdown preview after the 1.7.5 database migration fix made the 1.7.x line runnable: raw edit remains the startup default, preview is manual, headings/tasks/subtasks/tags/#imported render, preview checkbox taps rewrite the Markdown source only, and render failure falls back to edit mode. Do not push 1.7.x unless the user asks.
- 1.7.9 added durable commit-message rules focused on feature behavior and debug/fix reasoning, added docs/current/PLANNING_DESK_EXAMPLES.md, expanded phone Planning Desk help for heading sections like #收集箱 and #今日计划, and added a desktop-web Planning Desk 使用说明 modal. It keeps the safe Markdown preview and no-push 1.7.x policy.
- 1.7.10 fixed Planning Desk usability gaps from first real use: new/default notes are now blank with examples as placeholder/help only, `#remind` and preview reminder editing use the shared mixed reminder parser, explicit invalid reminders no longer silently fall back to defaults, shortcut task/subtask insertion is semantic, headings like #今日计划 now provide real date context, and phone/desktop Planning Desk document deletion is exposed with confirmation. Build verification should use Android Studio bundled JBR.
- 1.7.11 performed a syntax consistency pass: todo batch DDL now reuses Planning Desk date parsing for natural inputs like `5.28`, `5月28日`, `明天`, and `周五`, while reminder input and desktop-web reminder input now support AM/PM, relative dates, weekdays, dot dates, Chinese dates, and Chinese comma separators. Calendar batch remains a stricter structured import format by design.
- 1.7.12 synchronized the user-facing syntax explanations after the 1.7.11 parser changes: phone Planning Desk help, todo batch help, calendar batch help, in-app Wiki, desktop Planning Desk help, and desktop reminder placeholders now all explain the same DDL/reminder examples and the English-comma field-boundary rule.
- 1.7.13 fixed Planning Desk parser boundary issues from syntax review: inline leading dates no longer pollute event titles, heading date context resets correctly, date headings can include descriptions, natural schedules can place the time range later in the line, slash/full-width/Chinese-AMPM inputs are normalized, desktop-web reminder parsing now matches those common inputs, top-level events can parent subtasks, and unsupported semantic tags remain visible instead of being silently stripped.
- 1.7.14 fixed Planning Desk editor/import workflow issues from UI review: phone content auto-saves and saves before document switching, shortcut chips no longer double-trigger, import success immediately persists `#imported`, preview import requires a selected valid candidate, select-all/clear-all controls were added, preview/doc/help sheet heights are adaptive, middle-of-document Enter continuation works, preview edit time fields accept natural datetime inputs, and desktop-web Planning Desk now has auto-save plus Ctrl+S/Ctrl+Enter shortcuts.
- 1.7.15 fixed Planning Desk parser priority and compact date-heading boundaries: explicit `#ddl` now wins over natural schedule detection, time-range-plus-DDL rows stay todos, compact headings like `# 周五计划` and `# 5/28周末计划` provide date context, and descriptive headings like `# 我的明天计划` / `# 后天的事` remain ordinary headings.
- 1.7.16 compressed the Planning Desk phone operation area from the old three-row header into a single action row, moved secondary document actions into an overflow menu, removed manual save in favor of existing auto-save, moved the shortcut bar above the editor, raised dark-mode support-text contrast, and capped desktop-web content width.
- 1.7.17 corrected the 1.7.16 mobile operation-area density regression by making the Planning Desk action surface a fixed 56dp toolbar with only preview/edit, recognize, document list, and overflow actions, while keeping autosave state as a compact temporary label.
- 1.7.18 fixed two phone-side usability issues found in testing: calendar batch import custom syntax now defaults missing dates to today and accepts common date prefixes like 今天 / 明天 / 5.28 / 5月28日, while Planning Desk toolbar buttons clear input focus and show immediate feedback for preview/edit, recognize, and document-list actions.
- 1.7.19 fixed a follow-up calendar batch import input-normalization issue: custom-syntax event bodies now normalize Chinese commas, Chinese colons, and common full-width separators before splitting fields, so Chinese-input rows like `13:40-14:40，标题，@地点` parse correctly.
- 1.7.20 fixed Planning Desk shortcut-bar click reliability: shortcut buttons no longer use an empty Material `AssistChip.onClick` plus outer `combinedClickable`, and now use one clickable Surface path with visible `已执行：按钮名` feedback while preserving long-press help.
- 1.7.21 made the phone Planning Desk easier to approach: shortcut actions now appear as compact notes-app-style icon buttons, the toolbar has a direct tutorial entry, the help sheet is split into beginner pages, bare `ddl` text such as `任务M ddl 15:00` parses as a todo, and future DeepSeek / Qwen / OpenAI-compatible AI recognition is documented as optional, preview-first, and key-safe.
- 1.7.22 added a phone Settings -> AI 调用配置 panel for provider name, Base URL, API Key, and model name, storing the key locally while excluding it from backup JSON. It also made Planning Desk shortcuts collapsed by default with a light expand affordance and an expand/collapse toolbar button so the writing area remains primary.
- 1.7.23 added a daily-board onboarding card, a planning jump from the no-tomorrow-schedule state, desktop-sync copy buttons for token/address, shorter reminder placeholders, and a Planning Desk tutorial page for other import methods.
- 1.7.24 made onboarding resettable from Settings, expanded Planning Desk natural DDL parsing, upgraded Settings AI config to ordered multi-provider storage with key-safe backup behavior, added fallback caller scaffolding, and folded Todo editor advanced fields behind 更多选项.
- 1.7.25 wired the ordered Planning Desk AI providers into the phone-side recognition button: AI recognition uses a JSON-only prompt, converts results into the existing preview candidates, falls back through providers and then local rules, and still requires preview confirmation before import.
- 1.8.0 turned Planning Desk into a tracked upstream planning document: imported lines now create persistent mappings, preview shows mapping state pills, completed imported tasks can sync back to `- [x]`, unfinished items support refresh/postpone/undo, conflicts can be resolved both ways, desktop Planning Desk exposes the same loop, and backup/restore now includes planning mappings.
- 1.8.1 tightened Planning Desk AI trigger semantics so editing/newline and desktop import do not silently call AI, added per-provider AI connection testing, added date-ranged daily-board announcements, added the first Android today-todo widget, and shortened the launch screen to 600ms.
- 1.8.2 moved announcements from Settings into Planning Desk as multiple explicit Markdown announcement lines, upgraded the Android widget into a board-style `今日看板` surface with announcements/todos/schedules/tomorrow rows and resizable RemoteViews behavior, and fixed OpenAI-compatible AI connection failures by trying `/v1/chat/completions`, accepting `/v1` and full endpoint Base URLs, extending test timeouts, and reporting non-JSON HTML responses clearly.
- 1.8.3 cleaned up the old Settings-backed announcement fields, relaxed Planning Desk announcement syntax for checkbox/quote/inline forms, stripped imported/tag metadata from announcement text, highlighted announcements in Planning Desk preview, exposed active announcements in desktop web snapshots, and hardened the Android `今日看板` widget with dark-mode resources, board-range queries, and no duplicate update routing.
- 1.8.4 added desktop web system dark mode and length-aware announcement marquee, made Android `今日看板` widget rows deep-link to todos/events/source Planning Desk notes, collapsed the fully empty widget state to one row, and changed launch hiding to data-ready with an 800ms fallback.
- 1.8.5 added phone-side AI Provider model discovery: Settings -> AI 调用配置 can fetch OpenAI-compatible `/models` with Base URL and API Key, choose a returned model from a compact dropdown, convert common Base URL shapes to model endpoints, and still allow manual model-name fallback for providers that block model listing.
- 1.8.6 redesigned the Android `今日看板` launcher widget toward the in-app daily-board visual language: section rows, rounded empty cards, todo cards, event cards with date blocks / vertical color strips / title / time / location, and subtler dark-mode card surfaces while keeping row deep links.
- 1.8.7 refined the Android `今日看板` launcher widget after visual review: the widget now starts with a greeting card, uses a gradient board background, gives todo cards group-color strips, and combines today/tomorrow schedule content into one daily-board-like schedule card while preserving row deep links.
- 1.8.8 tightened the Android `今日看板` launcher widget further toward the in-app daily board: it adds a fixed daily-board title/date header, uses the transparent app icon as the header badge, switches to safer rounded gradient/sem-transparent card surfaces, and reduces card spacing so small widgets show more useful board content.
- 1.8.9 reworked the Android `今日看板` launcher widget after another visual review: it layers the daily-board background art with a scrim, changes the header to a circular menu button plus title, turns announcements into orange banners, and strengthens card opacity/spacing so the widget no longer reads like a generic list.
- 1.9.0 added focus mode: active todos can start bound focus sessions from the long-press sheet, the daily board can start free focus and shows today's completed focus minutes, FocusActivity provides a full-screen countdown with pause/continue, completion, extension and abandon flows, and Room / backup now preserve focus-session records.
- 1.9.3 retuned the Android `今日看板` launcher widget toward the in-app daily board after visual review: the default widget height is taller, outer padding and topbar hierarchy are closer to the phone board, card surfaces are more opaque in light/dark mode, todo strips are wider, and schedule rows use subtle inner card backgrounds rather than looking like a plain system list.
- 1.9.4 retuned the Android `今日看板` launcher widget after another visual review: the widget picker preview now uses a static daily-board layout, default target size is 4x5, todo rows include a checkbox-like marker and `DDL HH:mm` chip, and schedule rows return to transparent daily-board rows with vertical color strips.
- 1.9.5 hardened AI 日报 / 周报 scheduling: exact alarms are still used when Android allows them, but devices without exact-alarm permission now fall back to system-allowed idle scheduling instead of silently failing or risking a startup permission exception.
