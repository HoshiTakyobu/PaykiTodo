# Feature Ledger

This file tracks the product at a practical level for new coding sessions.

## Implemented And In Use

### Task / Todo System

- create, edit, delete todo items
- title / notes / group / deadline / multi-reminder fields
- Todo editor shows title / DDL / group first for new todos and folds notes / reminder input / recurrence / ring / vibration into 更多选项, auto-expanding when editing existing todos that use advanced state
- no-deadline todos
- lightweight comma-based todo batch import with preview validation
- todo batch-import DDL supports same-day clock input such as `16:30` / `16：30`, plus Planning Desk-style natural date forms such as `5.28`, `5月28日`, `明天`, and `周五`; date-only values default to `23:59`
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
- board schedule rows keep normal and in-progress color strips in one aligned column
- normal board schedule rows have no outer fill or border, while in-progress rows use a gold border with only subtle inner highlight
- daily board shows a distinct completion message when today's schedule existed but all events have already ended
- daily board always shows the tomorrow schedule section, including `明天暂无日程` when tomorrow has no events
- daily board onboarding card is readable in dark mode, can be dismissed, and can be reset from Settings -> About -> 使用说明
- daily board floating block titles have stronger dark-theme text shadow so they stay readable over the dark wallpaper background
- board surface intentionally does not expose add / batch-add buttons

### Planning Desk

- phone drawer has a `规划台` entry
- Planning Desk stores multiple Markdown planning documents in Room table `planning_notes`
- default startup opens the last opened planning document; if none exists, the app creates an empty `我的规划`; examples stay in help/tutorial content instead of the editor itself
- planning documents support create, open, rename, archive, and delete with confirmation on the phone UI; the phone document directory and desktop web Planning Desk both expose delete actions
- phone Planning Desk currently defaults to stable raw Markdown / natural-text editing; `1.7.8` restores a manual Markdown preview that renders headings, task checkboxes, subtask indentation, tag pills, and `#imported` state pills while keeping raw edit as the startup default
- phone editor mode remains a plain Markdown / natural-text editor with a fixed-height 56dp operation toolbar (预览 / 识别 / 文档列表 / 教程 / 快捷展开 / 更多) and a collapsible compact icon-style shortcut toolbar positioned above the editor only when needed
- phone Planning Desk secondary actions (新建/重命名/使用说明/归档/删除) are in an overflow DropdownMenu; manual save button removed in favor of auto-save
- phone Planning Desk includes a multi-page in-screen beginner tutorial explaining the workflow, natural writing, heading sections such as `# 收集箱` / `# 今日计划`, preview/import, future AI recognition, and directly usable examples
- phone Markdown preview checkbox toggles rewrite the source Markdown line only; they do not directly complete imported official todos
- phone Planning Desk editor auto-saves after a short debounce and saves before switching planning documents
- Enter continuation attempts to keep `- [ ]` task lines flowing without forcing the user to manually type Markdown every time, including when Enter is pressed in the middle of a document
- shortcut `任务` converts the current line to one checkbox task without duplicating `- [ ]`; shortcut `子任务` inserts a new indented child task line; shortcut chips avoid double-triggering one tap
- local rule parser recognizes markdown checkboxes, completed-task skip, subtask parent notes, date headings, DDL tags, lightweight bare `ddl` text such as `任务M ddl 15:00`, unified mixed reminder tags, group tags, schedule tags, and natural schedule ranges
- local rule parser also recognizes common Chinese natural DDL hints, including date-context fuzzy words such as `晚上交论文`, before-time forms such as `5点前` / `16:30之前` / `明天下午3点前`, and non-checkbox DDL keyword lines such as `交论文 截止明天 23:59`
- Planning Desk preview warns when a todo DDL is inferred from natural text and flags recurrence hint words such as `每天` / `每周` without auto-creating recurrence rules
- headings containing `今日` / `今天` / `明天` provide date context for following undated schedule lines, so `# 今日计划` has actual parser behavior
- heading date context is explicit and resets on plain headings; date headings with descriptions such as `# 5/28 周末计划` and compact headings such as `# 周五计划` work, while descriptive headings such as `# 我的明天计划` are not treated as dates
- natural schedule parsing accepts inline leading dates, time ranges later in the line, slash dates, full-width separators, Chinese AM/PM, and full-width range separators
- explicit `#ddl` takes precedence over natural schedule parsing, so a time range plus DDL stays a todo
- unsupported semantic tags such as `#today`, `#tomorrow`, `#important`, and `#project` remain visible in titles instead of being silently stripped
- natural schedule import can create both a calendar event and a linked todo whose DDL equals the event end time
- planning import is preview-first and selection-based, not immediate database writes; import is disabled until at least one valid candidate is selected
- planning preview cards are editable before import for title, group, notes, DDL/start/end times, mixed reminder input, and event linked-todo creation; preview has select-all / clear-all controls
- successful planning import appends `#imported` to imported source lines and immediately saves the active planning note to reduce duplicate imports
- imported planning lines now also create stable `planning_line_mappings` entries that link the source line to the created todo/event item
- mapping relocation uses normalized fingerprints plus fuzzy text matching, so the planning linkage is not purely a stored line number
- mapping status sync distinguishes `ACTIVE`, `COMPLETED`, `CANCELED`, `ORPHANED`, and `CONFLICT`
- phone Planning Desk preview can manually sync completed imported task lines back to source Markdown as `- [x]`, while rendered preview lines already show completion/cancel status pills
- Planning Desk refresh can update current-section or whole-document imported items from the latest Markdown, but only for unfinished active mappings
- Planning Desk batch postpone can shift unfinished imported items and the corresponding Markdown time text together
- the latest import / refresh / postpone batch can be undone
- conflicts between imported items and source Markdown can be resolved either by overwriting the item from the document or rewriting the document from the current item
- default Planning Desk import reminder is 5 minutes before, full-screen, ring + vibration
- planning notes are included in JSON backup / restore snapshots
- planning mapping records are also included in JSON backup / restore snapshots
- AI recognition for Planning Desk is now an optional Provider-based enhancement for DeepSeek / Qwen / OpenAI-compatible APIs; Settings exposes ordered multi-provider Base URL/API Key/model configuration, both phone and desktop Planning Desk recognition call enabled sources in order, local rules remain the fallback, and AI output enters preview before import
- AI Provider API Keys are stored locally in settings, deliberately excluded from backup JSON export, and preserved when importing backups without keys
- Planning Desk database migration is repaired in `1.7.5`: database version `10` includes `MIGRATION_9_10` to rebuild `planning_notes` tables created by the mismatched `1.7.0`-`1.7.4` migration
- Planning Desk database version is now `11`; `MIGRATION_10_11` creates the `planning_line_mappings` table and indices

### Calendar System

- timeline-style calendar foundation
- day / multi-day / month / agenda style views exist in code, with ongoing refinement
- normal events, all-day events, and recurring events
- event location / notes / color / reminder settings
- calendar reminder editing accepts the same comma-separated multi-reminder syntax as todos
- event preview keeps showing configured reminder offsets after reminder acknowledgement
- timeline pending event draft can be canceled by long-pressing blank timeline space and is cleared when opening an existing event
- text-based batch import support
- calendar batch import custom syntax defaults missing dates to today and accepts lightweight date prefixes such as `今天`, `明天`, `5.28`, `5/28`, and `5月28日`
- week-template and semester-generation related capabilities exist in the codebase and docs history
- current-time label is wired on the left time axis and remains visible even when today is off-screen, while the red current-time line remains in the schedule area and also stays visible
- todo editor can pick DDL through a wheel-style lunar date picker while preserving the existing time of day
- timeline headers, month cells, and agenda/list date surfaces show lunar labels using Android ICU `ChineseCalendar`

### Reminder System

- `AlarmManager` based scheduling
- todos and calendar events can store and schedule multiple configured reminder offsets
- custom snooze input can parse either minutes or a concrete future time
- custom snooze has no 180-minute cap and moves a todo's DDL when the snooze target is later than the current DDL
- notification reminder path
- full-screen reminder path
- foreground service and fallback chain work
- reboot / time change / timezone change recovery
- accessibility fallback path
- reminder diagnostics and settings-side tooling exist
- reminder and desktop-sync foreground notifications both use the dedicated `ic_stat_payki_todo` small icon resource
- calendar event acknowledgement preserves the configured reminder offsets instead of clearing the event's reminder setup
- reminder playback can now select alarm, accessibility, notification, or media audio channels
- PaykiTodo now tracks an internal reminder-volume percentage for self-played alert audio
- an advanced temporary system-channel volume boost can raise the selected stream during reminder playback and restore the original volume afterward
- work mode suppresses outward reminder sound by default, forces stronger vibration even when an individual item disabled vibration, and pushes calendar reminders into the full-screen / accessibility fallback chain

### Packaging / Identity

- adaptive launcher icon foreground is wired directly to the picture resource `ic_launcher_art`
- old vector mark launcher resources have been removed to prevent accidentally reverting to the wrong icon
- raster launcher art is also used by in-app launch / drawer surfaces
- drawer header icon is clipped into its circular header surface to avoid exposing the launcher icon's white rounded-rectangle background
- launch screen uses a transparent-logo variant so the mountain / sun background remains visible behind the logo
- picture launcher art is opaque pure white and scaled down inside the 512px canvas to reduce desktop mask crowding
- release-signing information template exists under `docs/PaykiTodo-Release-Signing-Template.md`

### Data / Backup / Diagnostics

- Room-based local storage
- JSON import / export
- auto-backup related support
- crash log viewing / copying
- in-app wiki assets
- in-app Wiki sidebar navigation works through local WebView JavaScript
- in-app Wiki keeps a left menu / right article layout on phone-sized screens instead of stacking all section buttons above the content
- in-app Wiki documents current reminder, batch-import, Planning Desk, desktop Planning Desk, and snooze input syntax

### Input Help

- reminder, batch-import, and custom-snooze input fields expose nearby question-mark syntax help
- shared input-help dialog explains valid examples, field-boundary rules, and invalid-value behavior

### Desktop / LAN Assistance

- LAN browser-based desktop sync console exists
- phone-side HTTP serving model exists
- browser can perform limited data operations against the phone-side dataset
- desktop web can edit existing todos with title, notes, DDL, reminder, group, recurrence, ring, and vibration fields
- desktop web todo/event reminder inputs accept AM/PM, Chinese AM/PM, relative-date, weekday, dot/slash-date, Chinese-date, full-width separator, and Chinese-comma reminder syntax in addition to the existing minute and ISO-like forms; placeholders now show these examples
- desktop web todo cards open a detail preview first; event cards open the editor directly, while destructive actions still require confirmation
- desktop web todo and event reminder editors accept mixed reminder syntax matching the phone-side examples, including minutes, same-day time, current-year date-time, and full date-time
- desktop sync API accepts todo `reminderOffsetsMinutes`, allowing desktop-created / edited todos to persist multiple reminders
- desktop web todo / event editors use a bottom-sheet-like visual structure with cancel / centered title / save actions
- desktop web editor fields are card-styled, and timeline / event card buttons are lighter and less form-like
- desktop Web UI resources are separated under `app/src/main/assets/desktop-web/`, while Android sync service code stays in `sync/`
- desktop Web packaging / future extraction guidance is documented in `docs/current/DESKTOP_WEB_ARCHITECTURE.md`
- Settings is split into common settings and advanced settings; compact dropdown rows replace large button groups for enum-like choices
- desktop-sync foreground notification can be tapped to open the in-app Settings -> Desktop Sync panel
- desktop web destructive delete actions require confirmation before DELETE requests are sent
- desktop web uses an in-app dangerous-action modal for delete confirmation instead of browser-native confirm
- desktop web event timeline cards open the editor directly, no longer expose inline edit/delete buttons, and prefer group color for display
- desktop web event timeline no longer shows the separate all-day strip above the timeline
- desktop web shows the installed APK version in the brand block and uses runtime versioned CSS / JS URLs
- desktop sync service self-stops if Android restarts it while desktop sync is disabled in Settings
- desktop web has a `规划台` tab with textarea editor, document selector, auto-save, `Ctrl+S` save, `Ctrl+Enter` parse, editable parse preview, selected import, and a help modal that explains the same DDL/reminder syntax as the phone-side Planning Desk help
- desktop web Planning Desk uses phone-local `/api/planning/*` routes, edits the same Room planning notes as the phone UI, saves before switching documents, blocks empty selected imports, writes back `#imported` markers after import, and reuses the same AI recognition / local fallback path as the phone Planning Desk
- desktop web Planning Desk now also shows the current note title, mapping status preview, refresh/postpone/undo controls, and conflict resolution actions for imported planning lines

### Destructive Action Safety

- active todo deletion asks for confirmation
- calendar event deletion asks for confirmation
- schedule-template deletion asks for confirmation
- group deletion asks for confirmation
- desktop web delete buttons use the PaykiTodo in-app dangerous-action confirmation modal
- phone-side delete confirmations share a refined dangerous-action bottom sheet with red icon, message card, and red confirm button

## Implemented But Still Being Polished

- board / dashboard experience details and readability tuning
- calendar interaction polish
- dedicated full lunar wheel remains pending; lunar work now includes display labels, minimal yearly same-lunar-date recurrence, event lunar start/end picking, and todo lunar DDL picking
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

- desktop web event cards use string-compatible ID lookup for card-click editing, preventing silent failures when snapshot IDs are not the same JavaScript type

- desktop web all-day events are exposed through compact per-day pills that reuse the same event editor click path as timed events
- Daily-board normal schedule rows no longer show an outer border; in-progress rows retain the gold highlight
