# Feature Ledger

This file tracks the product at a practical level for new coding sessions.

## Implemented And In Use

### Task / Todo System

- create, edit, delete todo items
- title / notes / multi-group tags / deadline / multi-reminder fields
- Todo editor shows title / DDL / group first for new todos and folds notes / reminder input / reminder delivery mode / recurrence / ring / vibration into 更多选项, auto-expanding when editing existing todos that use advanced state
- Todo editor can choose reminder delivery mode between full-screen reminder and notification reminder; the selected mode is persisted for todos and recurring todo templates
- Todo editor can enable `闹钟模式（持续响铃直到操作）` for DDL-backed reminders; alarm mode persists on todos and recurring templates, future recurring instances inherit it, and timeout downgrade leaves an explicit unhandled-reminder notification plus limited retry bursts
- Todo editor can mark a DDL-backed task as `倒数日`; the task then uses its DDL date as the countdown target and appears on board / desktop / widget countdown surfaces
- Todo editor can mark a DDL-backed task as `仅提醒，不在看板/日历显示`; the task still schedules reminders and remains manageable in My Tasks, but is filtered out of the phone daily board, Android board widget, desktop board, countdown board queries, and AI daily/weekly todo statistics
- Desktop Web todo editor exposes and preserves `闹钟模式` and `仅提醒，不在看板显示`, matching the phone editor for these DDL-dependent todo fields
- Desktop Web todo editor exposes an explicit `启用提醒` switch; disabling it saves an empty reminder-offset list and disables reminder-dependent fields instead of relying on clearing reminder text
- Desktop Web todo and event editors use compact weekday multi-select chips for weekly recurrence instead of requiring manual `1,3,5` text input, and hide those chips for non-weekly recurrence rules
- Desktop Web Planning Desk recognition preview uses the same weekday multi-select chips for weekly recurrence candidates, and clears weekly-day state when the candidate is changed away from `每周`
- recurring todo templates persist `hiddenFromBoard`, and newly replenished recurring instances inherit the reminder-only visibility setting
- no-deadline todos; active no-DDL items are treated as `今日待办` across phone board, Android widget board query, desktop board, and desktop todo management
- lightweight comma-based todo batch import with preview validation
- todo batch-import DDL supports same-day clock input such as `16:30` / `16：30`, plus Planning Desk-style natural date forms such as `5.28`, `5月28日`, `明天`, and `周五`; date-only values default to `23:59`
- Todo page exposes todo batch import beside the bottom-right new-todo button instead of as a top content row
- drawer navigation exposes a single-line `待办` entry; group filtering and group management live in the todo page chip bar instead of an expandable drawer group list or standalone group page
- todos support multiple group tags through `todo_group_tags`; phone-side group filtering is multi-select intersection filtering, and the todo editor uses compact multi-select group chips while preserving the first group as the primary display color
- complete / cancel / restore flows
- active todo preview now uses the same bottom-sheet visual language as calendar event preview
- active todo card body opens preview; completion is isolated to the checkbox to avoid accidental completion
- active todo quick preview and quick action surfaces now expose visible `取消并归档` / `取消待办（归档）` actions with confirmation; the phone details preview shows an inline archive card directly below the title, a top `取消并归档` action, and a bottom fixed full-width `取消并归档` action above edit/delete, while the desktop Web preview uses the same inline archive card plus dedicated archive-action styling, keeping cancel as a history-preserving archive flow and visually distinct from hard delete
- todo quick preview is now the shared target for phone search results, Planning Desk linked todos, notification routes, and widget/deep-link todo opens, so active todos from those entry points can cancel/archive without detouring through the editor
- desktop web todo preview exposes top and bottom cancel/archive actions plus a separate hard-delete path with explicit history semantics
- built-in Wiki documents the current todo preview behavior, including completion-circle isolation, cancel/archive semantics, hard-delete semantics, and recurring-todo skip records
- recurring task support
- recurring todo/event `1.13.21` `整个循环系列` edits preserve a user-selected new series start date instead of always rebasing to the old first occurrence; time-only edits still keep the old series anchor, and stale templates are deleted before rebuilt series/templates are inserted.
- recurring todo editor weekly defaults follow the DDL date until the user manually edits weekday chips, preventing stale default weekdays after a DDL date change while preserving explicit weekday selections
- recurring todo current-instance delete writes a `recurring_instance_skips` exception and then hard-deletes the row, so the occurrence does not enter history and recurring-template replenishment does not recreate it
- recurring todo `当前及之后` / `全部` delete paths hard-delete matching instances instead of canceling them into history
- backup / restore includes `recurring_instance_skips`, so restored backups keep the user's single-instance recurring-todo deletions
- recurring event current-instance delete still uses a canceled tombstone so calendar series replenishment does not recreate an occurrence that was explicitly removed
- recurring todo/event `当前及之后` range handling uses the original recurrence anchor date where available, so moving a single instance does not corrupt future-range edit / cancel / delete behavior
- recurring Calendar series edits delete the old recurring template when an entire series is changed to non-recurring, so future replenishment cannot recreate future events that the user intentionally removed from the series
- future recurring todo instances in the `计划中` section fold into one series card with a count badge and expand/collapse control, while `已错过` and `今日待办` remain uncollapsed
- expanded future recurring todo series render only the first 30 instances plus a folded-count notice, so very long series do not flood the scrolling list
- grouped task filtering, including multi-group intersection filtering and a phone-side intersection / union switch when multiple groups are selected
- three-zone home logic: overdue / today / upcoming, with no-DDL active todos included in today rather than hidden in upcoming
- board-style daily overview entry exists and can show today's todos directly

### Board / Dashboard

- dedicated daily board entry exists in the drawer and is the default home section
- board todo block includes missed active todos, today's normal todos, and active no-DDL todos
- board intentionally stays a read-only overview for todos, schedules, announcements, and countdown targets
- board view can show today's todos and today's / tomorrow's schedule summary together
- board can show a `倒数日` card for active countdown-enabled todos / events; todos count down to DDL times, events count down to event start times, exact-time expired targets are hidden, and tapping rows opens the corresponding todo / event preview first instead of jumping straight into an editor
- board section title / empty-card areas navigate to the corresponding task or calendar surface while concrete todo / event rows keep their detail-preview behavior
- board today's schedule hides timed events after they have ended
- board currently running events can be visually highlighted with a gold outline and subtle glow
- board greeting card supports compact collapse / expand behavior
- board announcement, countdown, today-todo, today-schedule, and tomorrow-schedule cards support collapse / expand controls with locally persisted state
- board background now uses separate light and dark image resources
- board schedule rows align the left color strip to the measured height of the event text block
- board schedule rows keep normal and in-progress color strips in one aligned column
- normal board schedule rows have no outer fill or border, while in-progress rows use a gold border with only subtle inner highlight
- board in-progress schedule rows show check-in status for check-in-enabled events and expose compact `签到` / `签退` actions
- board in-progress schedule rows route check-in-enabled events to the independent full-screen check-in surface through `去签到` / `查看`, keeping the board focused on overview rather than inline operations
- daily board shows a distinct completion message when today's schedule existed but all events have already ended
- daily board always shows the tomorrow schedule section, including `明天暂无日程` when tomorrow has no events
- daily board onboarding card is readable in dark mode, can be dismissed, and can be reset from Settings -> About -> 使用说明
- daily board floating block titles have stronger dark-theme text shadow so they stay readable over the dark wallpaper background
- board surface intentionally does not expose add / batch-add buttons

### Reminders / Notifications

- reminder alerts use a flatter high-priority surface with centered large time/title, 56dp action buttons, collapsible notes, entry animation, and a distinct alarm-mode pulse
- reminder todo cancel actions are labeled `取消待办（归档）` and use archive styling, matching quick-preview semantics that cancel enters history while delete is a hard removal
- alarm mode loops sound and vibration until the user completes, snoozes, cancels, or acknowledges; after 5 minutes it stops continuous ringing, updates the notification to `未处理提醒`, and performs three 30-second retry bursts at 2-minute intervals
- daily brief notification can be enabled from Settings with a configurable time, defaults to 08:00, summarizes today's todos/events and nearest <=7-day countdown target, and opens the daily board
- calendar events can show a low-priority ongoing notification during the event using channel `ongoing_event`; start/end alarms are restored independently of whether the original reminder time has already passed, and the ongoing schedule notification no longer depends on the event reminder being enabled

### Board Announcements

- Planning Desk supports multiple announcement lines in unarchived notes, including `#公告 5.16-7.1 内容`, `#公告 2026-05-16 2026-05-20 内容`, `> [!公告] 内容`, `> #公告 内容`, checkbox announcement lines such as `- [ ] #公告 内容`, and inline `#公告` hints
- daily board shows all active Planning Desk announcements above the greeting card when today is inside each optional date range
- announcement banner uses an orange rounded surface, campaign icon, bold deep-brown text, and marquee for long text
- desktop web announcement banner uses the same active Planning Desk announcements, stays static for short text, scrolls only when the combined text exceeds 60 characters, and pauses on hover
- announcement text strips trailing `#imported`, `#group ...`, and ordinary tail hashtags before display
- active date-scoped announcements are sorted before long-running announcements, with newer start dates first
- expired, future, archived-note, deleted-note, or removed-line announcements stay hidden
- Settings no longer exposes or stores a separate announcement editor; old app-settings announcement fields were removed and legacy SharedPreferences keys are cleaned once

### Planning Desk

- phone drawer has a `规划台` entry
- Planning Desk stores multiple Markdown planning documents in Room table `planning_notes`
- default startup opens the last opened planning document; if none exists, the app creates an empty `我的规划`; examples stay in help/tutorial content instead of the editor itself
- planning documents support create, open, rename, archive, and delete with confirmation on the phone UI; the phone document directory and desktop web Planning Desk both expose delete actions
- Planning Desk Outliner nodes support note-only rows: text prefixed with `// ` or `> ` is stored as an `isNote` node, displayed with muted/italic styling, excluded from publish-to-todo/event behavior, and ignored when computing parent completion from children
- Planning Desk node overflow actions can manually switch between task and note behavior with `标记为备注 / 取消备注`
- phone Planning Desk currently defaults to a free-writing markdown/natural-text editor surface while keeping the explicit Outliner / Markdown-compatible tools available; the free-writing surface keeps the preview-first recognition flow and supports long-line inspection with horizontal scrolling
- phone editor mode remains a plain Markdown / natural-text editor with a fixed-height 56dp operation toolbar (预览 / 识别 / 文档列表 / 教程 / 快捷展开 / 更多) and a collapsible compact icon-style shortcut toolbar positioned above the editor only when needed
- phone Planning Desk secondary actions (新建/重命名/使用说明/归档/删除) are in an overflow DropdownMenu; manual save button removed in favor of auto-save
- phone Planning Desk includes a multi-page in-screen beginner tutorial aligned with the current default free-writing workflow: natural writing first, recognition/import preview second, optional Outliner draft publishing as a separate mode, AI as optional enhancement with local-rule fallback, and directly usable examples
- phone Markdown preview checkbox toggles rewrite the source Markdown line only; they do not directly complete imported official todos
- phone Markdown preview renders Planning Desk announcement lines with orange styling, a campaign icon, a `全局公告` pill, and a date-range pill; tapping the line jumps back to the source line
- phone Planning Desk editor auto-saves after a short debounce and saves before switching planning documents
- Enter continuation attempts to keep `- [ ]` task lines flowing without forcing the user to manually type Markdown every time, including when Enter is pressed in the middle of a document
- phone Planning Desk shortcut toolbar is intentionally minimal: it defaults collapsed and only exposes `子任务` plus `公告`; top-level tasks, DDL, reminders, groups, dates, and schedules remain natural-text / tag parser inputs instead of returning to a crowded button grid
- local Planning Desk parsing recognizes plain bullet lines such as `- 想办的事`, `* 整理材料`, and `• 发消息` as no-DDL todo candidates for the collection / scratchpad workflow; preview candidates explain that ordinary bullets were recognized as no-DDL todos
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
- planning recognition preview has a collapsible batch-settings area that can apply countdown to selected events / DDL-backed todos, linked-todo creation, check-in, and unified group changes to the currently selected candidates
- successful planning import appends `#imported` to imported source lines and immediately saves the active planning note to reduce duplicate imports
- imported planning lines now also create stable `planning_line_mappings` entries that link the source line to the created todo/event item
- mapping relocation uses normalized fingerprints plus fuzzy text matching, so the planning linkage is not purely a stored line number
- mapping status sync distinguishes `ACTIVE`, `COMPLETED`, `CANCELED`, `ORPHANED`, and `CONFLICT`
- phone Planning Desk preview can manually sync completed imported task lines back to source Markdown as `- [x]`, while rendered preview lines already show completion/cancel status pills
- Planning Desk refresh can update current-section or whole-document imported items from the latest Markdown, but only for unfinished active mappings
- Planning Desk batch postpone can shift unfinished imported items and the corresponding Markdown time text together
- the latest import / refresh / postpone batch can be undone
- Planning Desk Outliner supports an in-memory per-document 20-step undo stack for node edit, delete subtree, merge, same-parent reorder, single publish, and publish-all; switching documents clears the stack
- conflicts between imported items and source Markdown can be resolved either by overwriting the item from the document or rewriting the document from the current item
- default Planning Desk import reminder is 5 minutes before, full-screen, ring + vibration
- planning notes are included in JSON backup / restore snapshots
- planning mapping records are also included in JSON backup / restore snapshots
- Planning Desk supports a `今日` document shortcut; notes can carry `documentDateEpochDay`, and undated schedule lines in that note are parsed against the document date while explicit dates still win
- Planning Desk `1.13.21` fixes explicit DDL-date precedence for phone / desktop parser paths: lines such as `5.29 【DDL】...`, `5.29【DDL】...`, `5.29 【DDL】14:00 ...`, and `5.29 【紧急】【DDL】14:00 ...` use the inline date/time instead of the planning note's document date, while DDL markers and time tokens are removed from the final title
- Planning Desk `1.13.22` makes the mobile Planning Desk start as a wide free-writing surface, keeps `识别` preview-first instead of background direct-write capture, and keeps AI/local fallback understandable in the editor copy
- Planning Desk recognition first runs local rules; if explicit Markdown / DDL / schedule syntax covers every actionable line, it uses local candidates directly instead of spending an AI call or risking AI reinterpretation. Explicit local parse errors stay visible in preview instead of being swallowed by AI fallback. Mixed free-form natural text can still call enabled AI providers, and AI prompts receive the planning document date only as fallback context while inline dates remain authoritative
- AI recognition for Planning Desk is now an optional Provider-based enhancement for DeepSeek / Qwen / OpenAI-compatible APIs; Settings exposes ordered multi-provider Base URL/API Key/model configuration, single-provider model-list fetching, single-provider connection testing, both phone and desktop Planning Desk recognition call enabled sources in order, local rules remain the fallback, and AI output enters preview before import
- Planning Desk AI keeps group assignment conservative: AI `groupName` is preserved only when the source line explicitly contains a group marker such as `#group`, `分组：`, `项目：`, or `课程：`, so ordinary titles are not split into accidental groups
- Planning Desk AI / preview candidates carry event location, all-day, countdown, check-in tracking, and recurrence fields; phone and desktop previews can edit those fields before import, and imports persist them into the final todo/event drafts
- Desktop Planning Desk import accepts AI-returned preview candidates directly even when their IDs do not match local-parser `line-*` IDs, preventing the browser from showing one AI candidate but importing zero selected items
- AI source editing can fetch available models from OpenAI-compatible `/models` endpoints using only Base URL and API Key, then show a compact dropdown while preserving manual model-name entry for gateways that do not expose model discovery
- AI source editing can mark an enabled provider as `此服务支持图片识别`; phone Planning Desk uses only these vision-capable providers for screenshot / timetable image recognition
- Settings -> AI 调用配置 uses compact provider summary cards and now tries to persist valid provider add/edit/toggle/reorder/delete changes immediately; incomplete enabled providers show an in-page warning instead of silently relying on the user to remember a separate save step
- AI provider chat calls accept common OpenAI-compatible endpoint shapes: root Base URL tries `/v1/chat/completions` before `/chat/completions`, `/v1` appends `/chat/completions`, full `/chat/completions` URLs are used directly, and full `/models` URLs convert back to sibling `/chat/completions`; non-JSON HTML responses produce a Base URL hint
- Planning Desk AI recognition is explicit-only: phone calls it from the `识别` button, desktop calls it from `识别` or `Ctrl+Enter`, and desktop import without preview no longer triggers AI silently
- Phone Planning Desk overflow menu includes `从图片识别日程`: it opens the system image picker, compresses the selected image to a 1600px-long-side JPEG, sends an OpenAI-compatible vision request through vision-capable providers, appends the returned Markdown to the current planning note, auto-opens preview when candidates are parsed, and still requires preview/import confirmation before database writes
- phone Planning Desk image recognition preview shows a clickable source-image thumbnail so the user can enlarge the original image and compare it with the parsed candidate list
- Android system share can send text or images to `添加到 PaykiTodo`; shared content now uses the capture recognition pipeline to directly create Planning Desk nodes and their linked official todos/events
- launcher shortcuts expose `拍照添加` and `语音添加`; photo capture opens the system camera through FileProvider, voice capture uses Android SpeechRecognizer with zh-CN partial results, and both route results through background capture recognition
- background capture recognition uses a `capture_processing` notification channel; completed capture notifications open the matching Planning Desk document after direct node insertion, while the phone Planning Desk Markdown compatibility capture starts background recognition only after an explicit confirmation instead of blocking the editor
- AI Provider API Keys and Desktop Sync access tokens are stored locally in settings and deliberately excluded from backup JSON export; importing a backup without AI keys preserves existing local keys, while importing an old backup that contains a Desktop Sync token ignores that token so the app can generate a fresh local access key
- Planning Desk database migration is repaired in `1.7.5`: database version `10` includes `MIGRATION_9_10` to rebuild `planning_notes` tables created by the mismatched `1.7.0`-`1.7.4` migration
- Planning Desk database version is now `11`; `MIGRATION_10_11` creates the `planning_line_mappings` table and indices
- Planning Desk Outliner is user-facing in `1.12.10`: database version `22` stores `planning_nodes` with optional `linkedEndTodoId` and `syncEnabled`, migrates existing Markdown lines into tree nodes, keeps Markdown headings such as `# 今日计划` / `# 收集箱` as non-sync structure nodes instead of official todos/events, includes nodes in backup / restore, exposes repository / ViewModel CRUD plus Markdown import-export helpers, renders a phone outline editor by default, exposes desktop `/api/planning/nodes` routes, and adds desktop Web node editing plus up/down reorder and same-level drag reorder controls
- Phone Planning Desk Outliner supports same-parent long-press drag reorder in edit mode, with drag lift/alpha feedback and placement animation; preview mode blocks drag and keeps the row action menu behavior
- Phone Planning Desk Outliner `1.12.15` keeps the `1.12.13` memo-style keyboard behavior and adds draft/publish separation: edit mode shows lightweight rows with borderless root / sibling / child active input lines; empty input Backspace focuses the previous node; row-start Backspace merges into the previous same-level node; middle Enter splits a node into a new same-level node; preview mode shows per-row `⋯` actions; parent nodes with children remain structure headings; newly created/captured leaf nodes are drafts until single publish or publish-all creates official todos/events, and editing a draft preserves its publish/sync intent
- Desktop Web Planning Desk Outliner `1.12.15` carries the same Backspace / Enter / ArrowUp / ArrowDown behavior, displays draft nodes distinctly, and exposes single-node publish plus current-document publish-all through desktop sync routes
- Desktop Web Planning Desk Outliner `1.13.20` makes the Outliner the explicit primary input surface: empty documents focus the main input, root input uses a visible multi-line note box, existing nodes use directly editable auto-height textareas, multi-line paste can create multiple draft nodes, and drag reorder starts only from a dedicated handle so text editing is not blocked by row dragging.
- Planning Desk natural schedule parsing `1.12.10` accepts ordered bare location fields such as `15:00-17:00, 写论文, 图书馆3楼` and a common space-separated place-token fallback
- Desktop Web Planning Desk Outliner `1.12.6` fixes two correctness issues: node time fields display formatted DDL / start / end values rather than epoch milliseconds, and changing the active planning document reloads that document's Outliner nodes before rendering
- Planning Desk capture/share/photo/voice flows can directly write recognized candidates as planning nodes and linked official todos/events; parent-node completion clears/reschedules reminders for all affected linked child items, capture-created items disable reminder state when Android rejects scheduling, startup repairs migrated sync-enabled nodes that lack their official linked items, and official-item delete/cancel operations detach Outliner nodes so intentionally removed formal items are not recreated
- Phone Planning Desk Outliner preview `⋯` can open the linked official todo/event editor, so reminder, group, recurrence, notes, countdown, and event check-in are configured through the same editor surface as ordinary phone items; compact node actions still cover time, location, sync toggle, and delete

### AI 日报 / 周报

- Settings -> `AI 调用配置` includes an `AI 日报 / 周报` block with compact switches, HH:mm time fields, a report-retention dropdown, a save/re-schedule button, `了解 AI 日报`, and `立即生成一次日报`
- the `了解 AI 日报` guide is a centered dialog explaining what daily/weekly reports summarize, AI-source and fallback requirements, Android 12+ exact-alarm caveats, enable steps, the independent `AI 报告` archive, notification deep links, and common failure cases
- daily reports collect today's completed todos, missed todos, today's events, today's event check-in investment minutes, tomorrow events, and tomorrow DDLs
- weekly reports collect Monday-Sunday completed todos, missed todos, week events, and next-week DDLs
- report generation tries enabled Planning Desk AI providers in order through `PlanningAiCaller.callWithFallback`; if AI is disabled or fails, a local template still generates a usable report
- daily-report prompts and the local fallback report include `今日日程投入：Y 分钟`, sourced from the `event_check_ins` daily total
- reports are stored in the independent Room `ai_reports` archive rather than Planning Desk notes; the drawer exposes `AI 报告` after `规划台`
- upgraded installs migrate legacy Planning Desk `AI 日报` / `AI 周报` note entries into `ai_reports`, delete the old report notes, and clear `lastOpenedPlanningNoteId` if it pointed to a removed report note
- `AI 报告` supports keyword search, all / daily / weekly filters, all-time / recent-7 / recent-30 / recent-90-day range filters, card previews, full detail viewing, local-fallback/source pills, empty-state guidance, and delete confirmation from long press or detail view
- AI report retention can be set to 30 days / 90 days / 365 days / forever; generating a new report purges older archived reports according to the current setting
- report notifications use a low-priority `ai_report_channel`, skip posting if Android 13+ notification permission is missing, and deep-link to the matching AI report detail
- `DailyReportScheduler` schedules daily and Sunday weekly report alarms, cancels disabled schedules, and is invoked on app startup plus boot/time/timezone recovery; it uses exact alarms when allowed and safely falls back to system-allowed idle scheduling when exact-alarm permission is missing
- backup / restore snapshots include `aiReports` and the report-retention preference, so exported JSON preserves report history and cleanup policy while still excluding AI API keys

### Calendar System

- timeline-style calendar foundation
- day / multi-day / month / agenda style views exist in code, with ongoing refinement
- normal events, all-day events, and recurring events
- event location / notes / color / reminder settings
- phone calendar keeps a bounded loaded event window and skips redundant Room queries when the requested visible date range is already covered, reducing timeline swipe churn without changing displayed events
- Desktop Web event editor supports multi-line event titles, matching phone-side event title entry instead of forcing a single-line input
- Desktop Web event editor exposes an explicit `启用提醒` switch; disabling it saves an empty reminder-offset list and disables reminder-dependent fields, while new events default to no reminder like the phone event editor
- Desktop Web event creation supports `每周多时间段` for new events, matching the phone-side multi-slot workflow by creating one weekly recurring event per weekday/time slot through the batch event API while keeping editing of existing events single-series only
- Desktop Web 日程时间轴 supports local weekly Excel import for `review.xls` / `.xlsx`: the browser reads the user-selected file only, parses date headers plus left-side time slots, lets internal cell time ranges override row times, handles merged-cell duration inference, previews candidates, marks existing / in-file duplicates, and imports only selected confirmed events without touching the Excel file or wallpaper workflow
- Phone event creation supports `每周多时间段` for new events: multiple weekday + start/end slots share one event title, location, notes, reminder settings, color, countdown, and check-in configuration, then create one weekly recurring event series per slot without requiring a database schema change; reminder input is validated and converted per slot anchor so each period gets reminder offsets relative to its own start time
- event editor can mark important events as `倒数日`; the countdown target is the event start time and it appears on board / desktop / widget countdown surfaces
- event editor exposes an optional `打卡追踪` switch under `日程标记`; moving an event preserves both countdown and check-in marker state
- event data now supports optional check-in tracking fields and accumulated invested minutes; repository and desktop-sync APIs can create check-ins, check out active records, list event check-ins, and recompute total event investment time
- event details bottom sheet shows a `打卡追踪` card for enabled events, including total invested time, active `签到中` status, closed / active segment rows, and a `去签到` / `查看签到` jump into the full-screen check-in surface
- event details bottom sheet exposes `完成日程` for check-in-enabled events; completion marks the event complete, can automatically check out an active record, and can show a summary card with planned time, actual invested time, check-in count, and investment rate
- phone check-in operations now have an independent full-screen `CheckInActivity` with event title, live clock, large state button, sign-out confirmation, multi-segment re-check-in support, auto-backup after successful sign-in/out, and main-surface refresh after returning
- event check-in has an idle auto-checkout watchdog with a configurable threshold; app startup / resume and widget refresh can close stale active records at the event end time and send a low-priority auto-checkout notification whose collapsed title includes the event name
- check-in-enabled event reminders expose `签到` directly on the full-screen reminder page and the accessibility fallback overlay; signing in also acknowledges the current event reminder so the strong-reminder surface closes
- Settings -> `日历与提醒` exposes event check-in behavior switches for automatic checkout when completing an event and showing investment statistics after completion; both default to on and persist locally
- Settings -> `日历与提醒` also exposes `闲置自动签退阈值`, defaulting to 2 hours, with backup / restore preservation
- calendar reminder editing accepts the same comma-separated multi-reminder syntax as todos
- event preview keeps showing configured reminder offsets after reminder acknowledgement
- timeline pending event draft can be canceled by long-pressing blank timeline space and is cleared when opening an existing event
- text-based batch import support
- calendar batch import custom syntax defaults missing dates to today and accepts lightweight date prefixes such as `今天`, `明天`, `5.28`, `5/28`, and `5月28日`
- week-template and semester-generation related capabilities exist in the codebase and docs history
- current-time label is wired on the left time axis and remains visible even when today is off-screen, while the red current-time line remains in the schedule area and also stays visible
- phone Calendar top-bar title is localized as `日历`, avoiding leftover English `Schedule` in the Chinese UI
- todo editor can pick DDL through a wheel-style lunar date picker while preserving the existing time of day
- timeline headers, month cells, and agenda/list date surfaces show lunar labels using Android ICU `ChineseCalendar`

### Reminder System

- `AlarmManager` based scheduling
- reminder scheduling catches `setAlarmClock` / exact-alarm failures, records them as non-fatal diagnostics, and falls back to inexact scheduling where possible instead of crashing the app
- startup reminder recovery is wrapped in a safe recovery path with `SafeStartupGuard`, so repeated early crashes skip reminder recovery and let the user open the app to manage data
- recurring todo creation only expands an initial limited window and later replenishes future instances, reducing alarm burst size for long daily recurring tasks
- todos and calendar events can store and schedule multiple configured reminder offsets
- custom snooze input can parse either minutes or a concrete future time, has no 180-minute cap, and when the current DDL is already overdue or not later than the next reminder it will push DDL forward by one minute past the next reminder so the reminder loop keeps working
- todo reminder screens expose an explicit `DDL 推迟` action; its input accepts positive minute increments, same-date clock targets, and full date-time targets, and rejects any target that is not later than the current DDL
- notification reminder path
- full-screen reminder path
- reminder delivery policy respects the per-item `通知栏提醒` / `全屏界面提醒` choice: notification-mode items no longer actively launch the full-screen reminder surface unless work mode or alarm mode explicitly overrides them
- full-screen reminder relaunch paths now keep a short recent-surface cooldown, so ordinary app resume / accessibility window events do not repeatedly foreground the same reminder immediately after it was already shown while locked-screen forced overlay behavior remains available
- full-screen event reminders can start an event check-in when `打卡追踪` is enabled
- ongoing-event notification end broadcasts now re-check the current event state before clearing the notification, so a moved / extended event is not prematurely removed by an old end alarm
- foreground service and fallback chain work
- reboot / time change / timezone change recovery
- accessibility fallback path
- accessibility fallback event reminders can start an event check-in when `打卡追踪` is enabled
- reminder diagnostics and settings-side tooling exist
- reminder, desktop-sync foreground, capture, report, ongoing-event, and check-in watchdog notifications use the dedicated vector `ic_stat_payki_todo` small icon resource, so notification shade icons are monochrome-mask compliant instead of depending on a colored bitmap
- calendar event acknowledgement preserves the configured reminder offsets instead of clearing the event's reminder setup
- reminder playback can now select alarm, accessibility, notification, or media audio channels
- PaykiTodo now tracks an internal reminder-volume percentage for self-played alert audio
- an advanced temporary system-channel volume boost can raise the selected stream during reminder playback and restore the original volume afterward
- work mode suppresses outward reminder sound by default, forces stronger vibration even when an individual item disabled vibration, and pushes calendar reminders into the full-screen / accessibility fallback chain

### Packaging / Identity

- adaptive launcher icon foreground and round icon foreground are wired directly to `ic_launcher_art_transparent`, the transparent main-logo resource, instead of the older opaque white-background launcher art
- adaptive launcher icons provide an explicit vector monochrome drawable through `ic_stat_payki_todo`, so Android themed-icon launchers do not derive a simplified icon from colored or stale opaque art
- obsolete unreferenced launcher-art variants are removed from source resources to reduce the chance of accidentally reverting to the wrong icon
- drawer header icon uses the same transparent logo resource and is clipped into its circular header surface to avoid exposing a white rounded-rectangle background
- launch screen uses a transparent-logo variant so the mountain / sun background remains visible behind the logo
- release-signing information template exists under `docs/templates/PaykiTodo-Release-Signing-Template.md`; real signing values belong only in ignored root-level `keystore.properties`
- release builds read local `keystore.properties` and fail early with a clear error when the release keystore or required signing fields are missing, preventing unsigned or accidentally debug-signed release artifacts
- release builds enable R8 minification and Android resource shrinking; the dashboard background images are stored as WebP resources so the signed release APK can stay below the 13 MB release-size target

### Android Desktop Widget

- Android launcher exposes a PaykiTodo `今日看板` widget through `TodoWidgetProvider`
- widget displays active Planning Desk announcements, today todo block, and a combined today/tomorrow schedule board closer to the in-app daily board
- widget uses RemoteViews `ListView`; rows are adaptive-height, split into greeting / section / empty-card / todo-card / schedule-card / announcement-card types, and no longer limited to five todos, so resizing the launcher widget reveals more board content
- widget board and countdown layouts use tighter list/card padding for small launcher sizes while keeping readable text and row click targets
- widget provider declares horizontal / vertical resize mode plus min resize dimensions for better launcher compatibility
- widget day/night colors are resource-backed, with fixed widget-specific light/dark board backgrounds, very light scrims, and text colors for launcher readability rather than app wallpaper reuse
- widget refresh uses a board-range Room query rather than loading all historical todos, and duplicate `onReceive` update routing has been removed
- widget board-range query explicitly includes active no-DDL todos, so the launcher widget's 今日待办 block matches the phone daily board instead of dropping no-DDL tasks
- tapping the `今日待办` section title opens the in-app `待办` section, concrete todo rows open the shared todo detail preview, the `今日日程` section title / schedule aggregate card opens Calendar, concrete event rows open the corresponding event detail / calendar preview, announcement rows open the source Planning Desk note, and greeting / empty rows return to the default daily board
- widget empty states now use the same card-style visual direction as the in-app daily board rather than thin bordered rows
- widget schedule content is aggregated into one card with a left date block, today rows, tomorrow label, and tomorrow rows instead of independent event cards; todo cards use the task group's color strip
- widget root no longer includes the daily-board wallpaper image layer; the fixed menu/title/date header has been removed so the launcher widget opens directly into board content and cannot show a stale header date
- widget keeps the launcher surface focused on announcements, greeting, today todos, and today/tomorrow schedule summaries
- widget in-progress schedule rows batch-load active event check-ins and show `⏱ 签到中 Xm` in the event accent color when a check-in is currently active; the launcher widget remains display-only and does not expose sign-in / sign-out buttons
- widget greeting, empty, orange announcement, todo, and schedule cards use stronger light/dark card surfaces, lightweight elevation, larger 28dp-style rounding, wider todo color strips, tighter title/card spacing, and daily-board-like ordering with announcements before greeting for better launcher readability
- widget provider now suggests a more daily-board-like square / vertical default size instead of a shallow list-size widget, and the current card pass uses more solid rounded card surfaces plus wider todo/schedule strip spacing so desktop rendering is less like a generic system list
- widget `1.9.4` visual pass adds a static daily-board-style picker preview, targets a 4x5 vertical board by default, gives todo rows a checkbox-like marker plus `DDL HH:mm` chip, and makes ordinary schedule rows transparent with only the vertical color strip to better match the in-app daily board
- widget `1.9.6` visual pass removes the extra `轻触打开` header copy, compresses the topbar toward the in-app daily-board TopBar, shows todo group tags / notes / `⏰ DDL HH:mm`, removes heavy card strokes, and gives currently running schedule rows a gold border plus faint gold fill
- widget `1.9.22` refresh pass updates both the header and RemoteViews `ListView` rows on normal widget updates, date changes, time/timezone changes, and app replacement; the RemoteAdapter cache key includes the current date to reduce stale launcher row reuse
- widget `1.9.22` light-mode pass uses more opaque warm card surfaces plus darker primary/muted/accent colors so text remains readable over the light board background
- widget event locations display saved text directly; display code no longer prepends `@`, so user-entered `@地点` is not duplicated
- widget `1.10.0` pass registers an independent PaykiTodo `倒数日` widget that shows the nearest 3 active countdown targets; tapping a todo target opens My Tasks and tapping an event target opens Calendar
- widget `1.10.1` pass removes the countdown section from the existing `今日看板` widget; countdown targets now belong to the independent `倒数日` widget, whose rows use `Nd` plus remaining time, show a checkbox-like circle only for todos, and show full event time metadata for events
- widget `1.10.2` pass removes the independent `倒数日` widget header/date/count block, makes rows deep-link to exact todo/event details, changes countdown text to day/hour/minute without seconds, schedules minute-level refresh ticks, and counts only unfinished today events in 今日看板 widget section titles
- widget `1.10.2` adds distinct launcher-picker labels/descriptions/static previews for 今日看板 and 倒数日
- widget `1.10.3` continuation removes the 今日看板 widget top menu/title/date header, converts the independent 倒数日 widget into a scrollable RemoteViews ListView, gives countdown rows daily-board-style soft cards with dynamic accent strips and larger multi-line text, and makes both 今日看板 / 倒数日 widgets refresh RemoteViews collections through provider-owned minute ticks with `updatePeriodMillis=0`
- widget `1.13.8` pass removes the app wallpaper layer from board/countdown runtime layouts and picker previews, replaces the ambiguous todo overdue `!` with `已逾期`, removes the board-widget todo checkbox circle, returns custom `加载中…` loading rows, and further tightens board/countdown card spacing
- widget `1.13.9` follow-up makes todo/schedule accent strips rectangular and relies on parent clipping for integrated card edges, aligns schedule aggregate backgrounds with todo soft-card backgrounds, adds a stronger dark-mode overdue badge, and returns countdown widget outer padding to 4dp
- widget `1.13.11` spacing pass increases board/countdown widget outer horizontal padding and countdown row inner padding so launcher cards no longer look pressed against the widget shell edges
- planning-desk `1.10.3` pass changes event import to default event-only, keeps linked todo creation behind an explicit preview checkbox, removes the fixed generated linked-todo note, and parses `@地点` / quoted `@地点` / `地点：...` into the event location field
- desktop-web `1.10.3` pass adds compact event color preset swatches matching the phone event editor while preserving custom color input
- repository todo mutations and Planning Desk note edits / delete / archive operations notify widget data refresh through the application-level widget callback

### Data / Backup / Diagnostics

- Room-based local storage
- JSON import / export
- backup / restore includes `todoGroupTags`; old backups without explicit multi-group tags are restored by backfilling each todo's original `groupId`
- backup / restore includes `hiddenFromBoard` for todos and recurring templates plus `isNote` for Planning Desk nodes; old backups default the new fields to `false`
- backup / restore includes `eventCheckIns`, and todo/event rows preserve `checkInEnabled` plus `totalCheckInMinutes`
- backup / restore preserves event check-in behavior preferences for automatic checkout and completion statistics
- auto-backup related support
- crash log viewing / copying
- in-app wiki assets
- in-app Wiki sidebar navigation works through local WebView JavaScript
- in-app Wiki keeps a left menu / right article layout on phone-sized screens instead of stacking all section buttons above the content
- in-app Wiki documents current reminder, batch-import, Planning Desk, desktop Planning Desk, and snooze input syntax
- Settings includes a data health check surface that scans old completed todos, empty planning notes, stale draft nodes, expired AI reports, and overdue no-reminder todos; one-click cleanup only deletes safe items after confirmation

### Input Help

- reminder, batch-import, and custom-snooze input fields expose nearby question-mark syntax help
- shared input-help dialog explains valid examples, field-boundary rules, and invalid-value behavior

### Desktop / LAN Assistance

- LAN browser-based desktop sync console exists
- phone-side HTTP serving model exists
- browser can perform limited data operations against the phone-side dataset
- desktop web can edit existing todos with title, notes, DDL, reminder, reminder delivery mode, multi-group tags, recurrence, ring, and vibration fields
- desktop web todo title editing uses a multiline textarea and preserves newline display in todo/event cards and previews
- desktop web recurring todo/event editing exposes the same scope model as the phone side (`仅当前` / `当前及之后` / `整个循环系列`); turning a recurring todo into non-recurring defaults to `当前及之后` so future generated instances do not keep appearing
- desktop web todo management has compact multi-select group filter chips with intersection semantics, and todo cards / previews / board rows display all group names instead of collapsing to one group
- desktop web todo/event reminder inputs accept AM/PM, Chinese AM/PM, relative-date, weekday, dot/slash-date, Chinese-date, full-width separator, and Chinese-comma reminder syntax in addition to the existing minute and ISO-like forms; placeholders now show these examples
- desktop web todo cards open a detail preview first; event cards open the editor directly, while destructive actions still require confirmation
- desktop web todo and event reminder editors accept mixed reminder syntax matching the phone-side examples, including minutes, same-day time, current-year date-time, and full date-time
- desktop sync API accepts todo `reminderOffsetsMinutes`, allowing desktop-created / edited todos to persist multiple reminders
- desktop sync API exposes and accepts todo `groupIds`, so desktop-created / edited todos preserve phone-side multi-group relationships
- desktop sync API exposes event `checkInEnabled` / `totalCheckInMinutes`, provides event check-in endpoints for listing records / checking in / checking out, and returns `eventCheckInSummary` from item completion when event completion statistics are enabled
- desktop Web event editor exposes `打卡追踪`; event preview renders a check-in card for enabled events, lists records, shows total invested time / active segment state, and can call `签到` / `签退`
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
- desktop sync enable immediately starts the phone-side foreground service / LAN server; status reads start the foreground service when needed instead of creating a hidden server without notification
- desktop sync service auto-disables itself after 5 minutes without an authorized desktop heartbeat, covering both "电脑未输入密钥" and "已连接后断开" cases; desktop Web sends a 60-second authorized heartbeat after connection
- desktop sync status now exposes authorized-heartbeat freshness and auto-stop countdown to the phone Settings panel, which distinguishes waiting-for-key from connected and refreshes while sync is enabled
- desktop sync can keep WiFi and a partial wake lock while the foreground service is running; the default-on `桌面同步期间保持网络唤醒` setting is surfaced in Settings and reflected in the foreground notification copy
- desktop web has a `规划台` tab with textarea editor, document selector, auto-save, `Ctrl+S` save, `Ctrl+Enter` parse, editable parse preview, selected import, and a help modal that explains the same DDL/reminder syntax as the phone-side Planning Desk help
- desktop web Planning Desk uses phone-local `/api/planning/*` routes, edits the same Room planning notes as the phone UI, saves before switching documents, blocks empty selected imports, writes back `#imported` markers after import, and reuses the same AI recognition / local fallback path as the phone Planning Desk
- desktop web Planning Desk now also shows the current note title, mapping status preview, refresh/postpone/undo controls, and conflict resolution actions for imported planning lines
- desktop web `/api/snapshot` includes active Planning Desk announcements and the browser console renders them as a top orange announcement banner
- desktop web `/api/snapshot` includes a phone-derived `todayBoard` payload, and the browser first tab is now a daily-board view with current/next item, countdown targets, today todos, today schedule, tomorrow schedule, ended-event filtering, and in-progress gold highlighting above the full todo timeline; focus stats are kept out of the daily-board surface
- desktop web `/api/snapshot` reuses the groups already loaded in the snapshot JSON path instead of issuing a second Room group read for group-name/color mapping
- desktop sync server uses bounded client handling and byte-length request-body parsing, so abnormal LAN clients cannot grow threads without bound and Chinese long Planning Desk saves are not truncated by UTF-8 character/byte mismatch
- desktop web first connection keeps using the lightweight `/api/snapshot?scope=board` daily-board payload, while todo management now loads through paged/searchable `/api/todos?offset=...&limit=...&q=...` only when requested
- desktop web calendar timeline now loads only the visible date range through `/api/events?start=...&end=...`, and stale event-range responses are ignored when the user switches dates quickly
- desktop web todo / event mutations and Planning Desk import / refresh / undo paths refresh the current page's needed data instead of reloading one complete snapshot after every operation
- desktop web follows system dark mode through CSS variables for timeline cards, event cards, modal sheets, summary cards, tab buttons, sidebar cards, Planning Desk, inputs, and announcement surfaces

### Search

- phone daily board exposes a global search entry; search covers todo titles/notes, event titles/locations, Planning Desk node text, and AI report content/provider names with 300ms debounce and per-category limits
- search result taps route to the corresponding todo preview, calendar event detail, Planning Desk document/node highlight, or AI report detail

### Destructive Action Safety

- active todo deletion asks for confirmation
- calendar event deletion asks for confirmation
- schedule-template deletion asks for confirmation
- group deletion asks for confirmation
- desktop web delete buttons use the PaykiTodo in-app dangerous-action confirmation modal
- phone-side delete confirmations share a refined dangerous-action bottom sheet with red icon, message card, and red confirm button

### Data / Performance

- `todo_items` has Room indices for board todo queries, board event range queries, active reminders, group+DDL sorting, recurring-series lookup, desktop todo paging / sorting, and active countdown lookup.
- Database version is `27`; `MIGRATION_13_14` creates the initial `todo_items` performance indices on upgraded installs, `MIGRATION_14_15` adds / backfills `planning_notes.hasAnnouncementHint` plus the indexed announcement lookup path, `MIGRATION_15_16` adds desktop todo paging plus AI-report generated-time/type indices, `MIGRATION_16_17` adds countdown fields / indices for todos and recurring templates, `MIGRATION_17_18` removes `focus_sessions` while adding `event_check_ins`, `todo_group_tags`, todo check-in fields, and a backfill from existing todo `groupId` values into the multi-group join table, `MIGRATION_18_19` adds `planning_notes.documentDateEpochDay` for today-note date context, `MIGRATION_19_20` creates `planning_nodes` plus migrates Planning Desk Markdown lines into node rows, `MIGRATION_20_21` adds optional event-end linked todo IDs, `MIGRATION_21_22` adds `syncEnabled` for structure headings, `MIGRATION_22_23` adds `planning_nodes.isDraft` for explicit Planning Desk publish, `MIGRATION_23_24` adds reminder-only board visibility / note-node fields, `MIGRATION_24_25` adds `alarmMode` to todos and recurring templates, `MIGRATION_25_26` adds large-list runtime indices, and `MIGRATION_26_27` creates `recurring_instance_skips` for non-history single-instance recurring-todo hard delete.
- Room schema export is enabled and `app/schemas/com.example.todoalarm.data.AppDatabase/27.json` is committed as the current database-27 reference schema.
- Desktop Web first connection now uses a lightweight board snapshot (`/api/snapshot?scope=board`); todo management uses paged/searchable `/api/todos?offset=...&limit=...&q=...`, and the event timeline uses visible-range `/api/events?start=...&end=...` instead of sharing one full snapshot for every management tab.
- Main phone board/task UI uses active-todo-only observation and today/tomorrow event range observation instead of merging all todos and full active events into ordinary board/task state.
- Main phone board/task UI uses the shared active-todo section classifier for missed / today / upcoming lists, so no-DDL active todos keep the same "today every day" behavior in My Tasks and the board.
- Main phone board/task UI now computes large-list sectioning, countdown filtering/sorting, and announcement parsing off the main thread, and desktop-sync status recomputation is split out of ordinary todo/event list updates.
- Active todo cards draw their left color strip directly instead of using intrinsic-height measurement, use lightweight no-shadow `Surface` rows, and cache per-card group resolution, reducing per-row LazyColumn measurement / drawing / lookup cost when many items are visible.
- Main phone board/task UI no longer carries the full Planning Desk note list; complete planning notes are collected only while the Planning Desk section is open.
- Daily-board announcements on phone, Android widget, and desktop lightweight snapshot use the indexed `planning_notes.hasAnnouncementHint` candidate query before strict parsing instead of reading every planning document or scanning Markdown bodies with `LIKE`.
- Planning-note backup / restore recomputes `hasAnnouncementHint` from Markdown content, so imported data does not preserve stale announcement-candidate state.
- `AI 报告` uses paged Room queries by type, keyword, time range, and limit; the archive no longer observes the full report history just to render or filter the first page.
- AI daily/weekly report generation uses range-limited DAO queries for completed todos, missed todos, active DDL-backed todos, and overlapping active events instead of loading the full todo table before filtering.
- Desktop sync rejects request bodies larger than 4 MB with HTTP 413, preventing abnormal LAN clients from forcing unbounded request-body reads.
- Desktop sync request handling uses a suspend handler from `DesktopSyncServer` into `DesktopSyncCoordinator`; repository-backed business routes call suspend APIs directly, and the only remaining `runBlocking` is the per-client socket response boundary.
- Application startup initialization uses an application-level `SupervisorJob` scope and records non-fatal initialization failures through `CrashLogger.recordNonFatal`.
- Calendar month/list/all-day surfaces reuse one top-level event-by-date index instead of rebuilding date buckets independently in each view; the timeline date span is represented by a lightweight date window instead of allocating a full long-range date list.
- Phone Calendar subscribes only to active events overlapping the current padded visible date range, while notification / deep-link navigation to a far event expands that query range before focusing the target date.
- Calendar day / three-day timed-event overlap placement is now computed only for the currently visible page days, and timed-event card rendering is vertically culled to the viewport plus overscan, reducing timeline paging and scrolling work when many events are present.
- Calendar day / three-day timed-event placement now runs only for the currently rendered page days, current-time markers refresh once per minute, and vertical overscan is narrowed to reduce repeated scroll recomposition.
- Dashboard todo cards reuse pre-resolved group data from a per-state group map, avoiding repeated group-list scans across visible cards while scrolling many todos.
- Countdown-enabled todos / events are included in phone board, independent countdown widget, and desktop board data without requiring a full historical scan; exact-time past countdown targets are filtered out before rendering.
- Schedule-template saving reads active events overlapping the selected week, and desktop Planning Desk note update / mapping refresh paths read single notes by ID instead of scanning all planning notes.
- Future large-history work can still add real FTS for report content search and real-device calendar profiling; the biggest desktop full-snapshot coupling has been split, and desktop todo management now pages/searches `/api/todos` instead of returning the complete todo list by design.

## Implemented But Still Being Polished

- board / dashboard experience details and readability tuning
- calendar interaction polish
- lunar date UX is still open to polish, but the dedicated wheel-style lunar picker is already implemented for todo DDL and event start/end date picking; lunar labels and yearly same-lunar-date recurrence are also present
- final launcher / themed icon / notification icon surface verification on device
- default snooze picker behavior and feel
- visual consistency across settings / drawer / launch screen / board
- final device-side validation for the 1.6.30 todo preview and desktop-sync notification route

## Pending / Ongoing Direction

- stronger reminder reliability across vendor ROM differences
- more complete desktop-side operations
- GitHub Release publishing / public distribution remains a manual step and requires explicit user authorization
- tighter documentation so a new session can reconstruct state without old transcripts

## Notes

This is not a perfect specification. It is a working ledger so new sessions can quickly answer:

- what the app already does
- what is partially done
- what is still under active refinement

- desktop web event cards use string-compatible ID lookup for card-click editing, preventing silent failures when snapshot IDs are not the same JavaScript type

- desktop web all-day events are exposed through compact per-day pills that reuse the same event editor click path as timed events
- Daily-board normal schedule rows no longer show an outer border; in-progress rows retain the gold highlight
