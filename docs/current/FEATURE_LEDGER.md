# Feature Ledger

This ledger distinguishes completed capabilities from the active update. Historical entries describe their stated versions.

## Android and Windows 1.17.12 instance reminder protection

双端循环生成及系列修改保留对应实例自己的绝对提醒，新实例只继承相对规则；纯绝对拥有者在其他实例改名后保持提醒，显式关闭作用于所选范围。Android1.17.12/404、Room34：258单测、Lint0错误、42项数据库回归及暖启动检查通过，Debug和同证书Release交付，原模拟器数据保留，见[Android验收](VALIDATION_ANDROID_1.17.12.md)。Windows1.17.12原51项检查及MSI/便携成品保持，见[Windows验收](VALIDATION_1.17.12.md)。新循环文本的绝对时刻归属仍待选择，整体目标和最终推送未完成。

## 2026-11-01 history audit

60个未推送提交的文件与消息已脱敏并逐一复核，完整阶段历史和恢复bundle保留，最终实现/资源/工具/CI469文件与已验收1.17.11相同，见[审计](PUBLIC_HISTORY_AUDIT_20261101.md)。循环绝对提醒仍等待归属选择与相应实现，最终push尚未执行；下面“全部历史清理未做”仅描述旧阶段。

## Current update stage: 1.17.11 / 403

双端稳定Release检查和手动入口完成；手机DownloadManager状态恢复/取消、正确APK与签名/包名/版本核对、系统安装入口完成。Windows实现稳定灰度、后台签名资产校验、变化文件增量及完整回退、基础启动器/独立版本目录/nonce确认/失败回退，MSI和便携目录包含后续增量基线。验收、失败修正和真实线上/手机安装边界见[1.17.11验收](VALIDATION_1.17.11.md)。尚待循环绝对提醒选择、全部未公开历史处理及授权推送。下面1.17.10规划阶段的“更新器待完成”只描述当时状态。

## Current planning stage: 1.17.10 / 402

Android、Windows与LAN网页已从每日看板提供临时规划输入、本地/可选AI识别、预览编辑和确认事务导入。支持标题-only、日期/+简写、>日程、混合非循环绝对提醒、父子事项、同课多时段及持续循环；旧文档含归档只读保留，不创建新文档。Room34新增绝对提醒字段，贯穿备份/同步和普通编辑保留。具体已验证范围与限制见 [VALIDATION_1.17.10.md](VALIDATION_1.17.10.md)。

更新器、循环绝对提醒归属、最终公开历史审计和推送仍待完成；下面1.17.8/1.17.9是已完成历史阶段，不能用其旧“规划未实现”描述覆盖当前源码。

## 进行中：2026-10-31 双端体验更新

1.17.8阶段完成Windows设置导航与左对齐、中文字体别名、12—22px字号及真实预览、取消/保存一致性、随字号调整的日历与输入框，以及设置/F1可打开的离线使用说明。Android迁移包保留桌面字号。验证和成品见 [VALIDATION_1.17.8.md](VALIDATION_1.17.8.md)。

统一规划输入尚待语法最终收敛及实现。已确认逗号字段、>日程、可省略字段、提醒默认5和0/空括号关闭、层级、#备注、首尾引号、循环方括号、多时段课程关联和可选范围。1.17.9完成公共报告规则、上下文与168小时过期，详情见 [验收](VALIDATION_1.17.9.md)。更新机制、全部历史清理和推送仍未完成。不得用本阶段测试代替整轮验收。

## Current local fix: 1.17.7 / 399

Android and Windows persist acknowledged account/server-bound personal snapshots for conservative three-way reconciliation. Independent records and noncompeting settings merge automatically; task lifecycle actions can merge with snooze/passive missed state. Repeated identical completion/cancellation keeps the earliest action time and finished items cannot retain enabled reminders. Competing edits within a task's content/schedule section, opposite explicit actions, delete/edit, new-ID collisions and broken references remain conflicts. No baseline means no automatic choice between divergent legacy copies.

Both sides retain recovery copies before applying merged data and continue uploading remaining local changes with revision checks. No-change callbacks and Android diagnostic writes no longer alone block synchronization. Existing shared-data/account isolation remains. 237 units, 9 account device checks, a live packaged Windows bridge, 71 Windows regressions plus isolated UIA and MSI lifecycle passed within [documented scope](VALIDATION_1.17.7.md). The user's same-signature Release phone upgrade and backed-up production recovery are complete; APK/MSI/portable copies are in `Deliverables/PaykiTodo/1.17.7`.

## Previous Android fix: 1.17.6 / 398

Snapshot export reads related database tables in one transaction so concurrent replacement cannot mix old groups with new tasks. New regression reproduces the previous invalid relationship; nested sync export and checklist archive roundtrips remain valid. Automatic-backup compression/file I/O remains separate from the snapshot read.

229 units, Lint, 24 data + 6 account + 1 live Android checks and 1 existing packaged Windows compatibility check passed. New signed/Debug APKs are delivered; Windows stays 1.17.5.0. Original emulator user 0/10 files match their before-round hashes. Scope and artifacts: [validation](VALIDATION_1.17.6.md).

## Previous local update: 1.17.5 / 397

Follow-up acceptance confirms an already-open packaged Windows client automatically reflects Android completion in its visible task list and persisted store, while retaining another task. The local coordinated measurement was 5092 ms with no manual sync. Two additional targeted tests passed; app binaries remain unchanged. See [live-window validation](VALIDATION_1.17.5_LIVE_SYNC.md).

The 12-item feedback round is complete: larger white-tile icons, fixed Book Antiqua bold-italic brand and HYQiHei 60S navigation artwork, genuine font weights, larger colored task cards, timed quick checklists and direct Windows task presentation. Windows calendar defaults to pale surfaces/colored text/side stripes, with optional classic colors retained through Android backup roundtrips.

Phone completion/reminder operations run off the main thread; sync, shared refresh and widgets run independently. Application-lifetime automatic backups coalesce requests without making editing wait. Running clients poll every 3 seconds; cheap Android revision checks retain periodic/manual content audits and conflict protections. A held-upload test kept UI dispatch at 35 ms; the resulting encrypted Android snapshot was downloaded and persisted by an independent Windows client.

229 Android units, Lint, 28 distinct emulator tests, 67 distinct Windows/bridge/UIA checks, signed APK validation and complete MSI lifecycle checks passed. Rebuilt local outputs and limits are in [1.17.5 validation](VALIDATION_1.17.5.md); no public release or production deployment.

## Previous local fix: 1.17.4 / 396

Phone local edits no longer synchronously run widget IPC, sync scheduling, shared-calendar refresh scheduling, or automatic-backup file I/O from the Room change callback/completion coroutine. Automatic backups are serialized on `Dispatchers.IO`; the database edit remains local-first and cloud sync stays asynchronous. Windows `1.17.4.0` now stores an installation-scoped device ID separately from the encrypted account workspace, distinguishes expired/revoked sessions, and writes token-free diagnostics. The server keeps independent devices active when a copied ID arrives with a different machine identity. The 1.17.4 Debug APK is rebuilt; Windows 1.17.4 MSI was built for that session fix.

## Previous local update: 1.17.3 / 395

Android and Windows shared search now respects current account/server/calendar access, hiding revoked cached content while retaining recovery data. Android all-day search uses event dates and an all-day label; disabled reminders do not display dormant rules, active all-day rules exclude remembered timed offsets, and zero offset says “开始时”.

227 Android units, Lint, rebuilt signed APK verification, 57 Windows regressions, one 14-stage UIA flow and MSI lifecycle checks passed. Native production collaboration completed on 1.17.2 after the user's manual verification: editor invite/accept, RSVP, bidirectional existing-event edits and revoke. On 1.17.3 the same identities passed viewer acceptance, date/reminder presentation and live search revocation. Personal data and the original emulator user remain intact. See [validation](VALIDATION_1.17.3.md) and [native flow](NATIVE_COLLABORATION_ACCEPTANCE_20261025.md).

## Previous local update: 1.17.2 / 394

Windows ordinary shared-event saves compare personal content/settings before marking personal sync dirty. Shared-only changes during personal upload/download or explicit keep-local upload do not create redundant personal writes or false personal conflicts, and downloads preserve the latest shared cache. Existing personal edits and settings still sync. The original single-account acceptance and artifacts remain in [VALIDATION_1.17.2.md](VALIDATION_1.17.2.md); its later two-account follow-up is recorded above.

## Previous local update: 1.17.1 / 393

The [2026-10-25 final audit](CALENDAR_ACCEPTANCE_AUDIT_20261025.md) distinguishes implemented behavior from acceptance gaps. That audit originally left production two-account native invitation/response/revocation open; the follow-up recorded above closes that specific gap. Current participants are accepted calendar members and the organizer is the calendar owner; cross-member availability scheduling, independent subscription-source management and meeting-resource services are not complete product workflows.

Both clients now select shared-event participants from current accepted calendar members and show organizer/participant/current-account responses. Cancel stays transactional, changing calendars reselects members, existing responses are preserved, and new participants/occurrences start unanswered. PRIVATE viewers see only server-authorized projection. Confirmed RSVP remains visible despite pending local edits; in-flight edits preserve a recoverable candidate and old sync revision. Details/editors/reminders revalidate source access, and all-day notification dates use the event timezone.

The 1.17.0 calendar/timezone/recurrence/all-day/copy/account update remains. Requirements: [CALENDAR_UPDATE_CONTRACT.md](CALENDAR_UPDATE_CONTRACT.md). Participant-round validation and artifacts: [VALIDATION_1.17.1.md](VALIDATION_1.17.1.md); original1.17.0 scope: [VALIDATION_1.17.0.md](VALIDATION_1.17.0.md). The existing account server now provides `/v3` interfaces after the [2026-10-25 rollout](PRODUCTION_CALENDAR_ROLLOUT_20261025.md), with personal snapshots and credentials preserved. Other server installations also need these interfaces; all-device parity is not claimed.

## Implemented And In Use

### v1.16.26 日历作用域、拖动交互与安装包

- Windows循环编辑/取消/删除明确选择当前、当前及之后、全部；单次改期保留原锚点和系列模板，之后拆分系列，全部保留完成/取消历史。修复跨午夜、空实例补齐及旧Windows锚点，补齐月末、闰年、每月第几个星期和农历重复。
- 全天使用午夜排他结束与09:00提醒基准，跨日显示结束日期；新建日程支持多个时段并统一校验/提交。两端日历改色更新沿用旧默认色的日程/模板，保留其他自定义色。
- Windows小月历独立翻月，窗口跨宽度断点重排，窄周按3天翻页，日历列表独立滚动；紧凑日程编辑器临近草稿、底部蓝色完成固定。新增拖动改期/结束边缘调整、15分钟吸附与Esc取消。
- Windows规划文档支持本机图片OCR，识别文字进入草稿、完成才保存。Android完成倒数日后即时询问纪念选择，去除旧15秒时钟延迟。
- 中文每用户MSI包含.NET、桌面/开始菜单入口和WebView2安装入口，支持Windows 10 1809+/11 x64；升级/修复记住路径，卸载保留用户资料。安装后的实际EXE验证通过。

193 Android单测、13设备用例、38 Windows回归及1条14阶段UIA流程、跨端迁移包及安装/升级/修复/卸载通过。证据和实际环境边界见[VALIDATION_1.16.26.md](VALIDATION_1.16.26.md)。下方版本条目是历史记录，其中旧版Windows循环/OCR/多时段缺口由本节更新；不代表所有平台功能均等同。

### v1.16.25 多日历、显示规则与设置

- 两端新增独立日历：名称、颜色、日程归属与显隐；旧数据迁移到常规日历，删除日历保留事项并迁回默认，循环规则和周模板一并处理。
- Windows 网格单击按15分钟落点出现30分钟蓝色草稿，再次点击打开编辑器；取消不保存，切换显隐保持滚动位置。彩色圆角勾选框有填充色和对比清晰的勾。
- Windows 默认改为 Microsoft YaHei UI，可选择本机主字体；Android 默认系统无衬线，支持内置字体及导入 TTF/OTF。字体预览完成后才应用，原问候字体和看板图片保留。
- 今日待办加宽色条并突出 DDL；值得期待和倒数日默认隐藏超过7天的历史目标，完成后可选择长期保留纪念或隐藏，两端保存相同状态。
- Windows 未来同系列循环待办可折叠，分组筛选在右侧与操作按钮等高。两端设置按分类重排，备份使用目录、导出、恢复的列表行，手机维护工具默认收起。
- Android 应用名称恢复为 PaykiTodo；局域网旧编辑入口保留日历与纪念字段，备份往返和 Room31→32 迁移通过验证。

193 Android 单测、13个不同设备用例经针对性复验、26 Windows 回归和1条14阶段隔离 UIA 流程通过；具体范围及旧有差异见 [VALIDATION_1.16.25.md](VALIDATION_1.16.25.md)。

### v1.16.24 Typography, Native Calendar And Actual Client Acceptance

- Android uses Semibold drawer text and slightly larger body text; Android/Windows headings, usernames, settings and editors use bundled sans-serif while the greeting retains its existing display font and artwork. Windows brand uses Century Gothic/Century; greeting is blue.
- Windows Calendar adds a mini month, continuous selected-week background, today's circle, fixed weekday/date headers, local timezone, aligned hour labels and a refreshed current-time line: dashed before today, red dot at today, solid afterward. Day/week/month/list, group filtering, event creation/preview and overlapping lanes remain.
- Native search gains a visible icon/hint and Ctrl+F; account identity is the first Settings card, duplicate sync status is removed, and checklist actions use an integrated icon. Dark calendar buttons and focus colors remain readable and blue.
- Windows accessible task Toggle now saves complete/restore state; calendar group filtering also responds to checked state. Android reminder background/system bars and action text have corrected contrast.
- Verified 190 Android units/Lint/build/signature, 22 Windows regressions plus one 14-stage isolated UIA flow, real account-based phone edits/desktop completion/timed reminders and final artifacts. Original 557 emulator files restored and matched; 78 extras retained. Human verification was performed by the user. See [VALIDATION_1.16.24.md](VALIDATION_1.16.24.md); OEM and existing parity limits remain.

### v1.16.23 Sync Continuation, Reminder Recovery And Chinese Entry Points

- Android and Windows retain sync requests during transfer and continue uploading subsequent edits; content fingerprints detect missed dirty markers. Divergent legacy copies without a verified fingerprint remain blocked across repeated polls until explicit conflict resolution. Running clients poll about every 15 seconds; server-version confirmation does not prove another device has uploaded.
- Android startup/boot/time/upgrade recovery only reschedules previously registered, undelivered reminders from the last 10 minutes; dispatch identity is persisted before AlarmManager can deliver immediately. Historical todo/event/custom-reminder input can be saved without replay. Windows queues only reminders observed before their trigger and suppresses duplicates.
- Sarasa UI SC Regular/Semibold is bundled under SIL OFL for Android Compose, native WPF, LAN Web and Wiki; board artwork/greeting fonts remain. Windows group selectors show names and the redundant account navigation is removed.
- Android launcher name “派期待办”, task shortcuts and the `paykitodo://tasks` / `paykitodo://tasks/new` deep links retain the account gate and open a list/editor rather than writing a task. OEM voice vocabulary remains outside app control.
- Verified 190 Android units, 32 distinct device tests after retries, 22 Windows regressions plus one 14-stage isolated UIA flow, LAN browser/font checks and current artifacts. Original 531 emulator files match backup; 26 extra runtime files retained. See [VALIDATION_1.16.23.md](VALIDATION_1.16.23.md) and [SYNC_RECOVERY_1.16.23.md](SYNC_RECOVERY_1.16.23.md); whole-snapshot sync and OEM/Windows parity limits remain.

### v1.16.22 Native Layout And Non-interfering Acceptance

- Shared Chinese typography, original high-resolution window/tray icons, navigation glyphs/selection, light/dark fields and branded confirmations replace inconsistent native presentation. Reminder clock/actions centered; editor footers fixed; narrow boards stack; Planning Desk keeps actions visible.
- Removed double padding that clipped date/time text; UI regressions check single-line text viewport, date/time persistence and planning-button bounds. Logout shows the login heading while retained local data stays locked.
- Separate-desktop process launcher plus UIA workflows avoid user's pointer/keyboard/clipboard/focus. Optional profile directory also isolates credentials, server preference and verification browser data; it does not bypass account gates.
- Verified 19 Windows regressions (including real built EXE startup) plus 1 workflow with 14 stages, not 14 independent E2E tests. Visual samples inspected; full physical-input/real-provider/native-login/OEM coverage is not implied. See [VALIDATION_1.16.22.md](VALIDATION_1.16.22.md).

### v1.16.21 Account Expiry, Sync Recovery And Native Usability

- Android expiry notification opens account settings; expiry preserves authenticated local access, reminders and dirty data. Explicit logout locks access. Remember-account/password is opt-in and kept outside snapshots; remembered passwords use encrypted preferences or Windows DPAPI.
- Android reminder service, full-screen activity, notification clicks, ongoing/daily notifications, widgets and capture entrances use the local account gate. Exact AlarmManager scheduling, startup/boot/upgrade/time recovery and duplicate dispatch claims remain active independently of cloud authentication.
- Android cloud sync uses local-write debounce, network callbacks and persisted network-constrained JobScheduler jobs. Dirty generations and transaction-time fingerprints protect edits made during network requests. Same-account reauthentication preserves pending changes and revision.
- Both clients support cloud inspection and confirmed restore/keep-local upload with revision checks, account-bound reviews and safety copies. Restoring an operation-before copy selects a local copy for the current account, never the separately saved cloud copy.
- Android/LAN/WPF checklists accept no title while keeping dash-required tasks, nesting, notes, comments, semantic fingerprints and real TodoItems.
- Android settings use neutral blue-gray surfaces. Native WPF adds wrapped toolbars, readable dark controls, draft preservation through navigation/status updates, overlapping-event lanes and weekly weekday checkboxes.
- Windows keeps independent encrypted local storage; lunar/OCR/multi-slot creation and Android background-service permissions remain platform-specific gaps. The Windows process must be running for desktop reminders.
- Interactive acceptance now has real Pixel_8 touches, formal login/conflict component taps against a local server, physical network outages/concurrent writes, visible Edge-to-phone LAN saves and visible native WPF OS mouse/keyboard flows. Coverage and failures are separate from implementation status; see [INTERACTIVE_VALIDATION_1.16.21.md](INTERACTIVE_VALIDATION_1.16.21.md). Native Windows login E2E and OEM/long-idle/full-screen behavior are not passed merely because regression suites pass.

### v1.16.20 Cross-Client Continuation

- Android Daily Board matches the supplied agenda geometry while preserving the established artwork and greeting font; tomorrow's section keeps every visible event.
- LAN Web treats phone disconnect as a session boundary: all modal editors and pending confirmation/form promises close, drafts are discarded, stale requests are ignored, and connection failures use Chinese guidance.
- Windows launches a native WPF workspace for the primary product surfaces. WebView2 is limited to Turnstile account verification, while the phone LAN browser remains a separate compatibility mode.
- Windows encrypted cloud snapshots and exported migration packages omit AI API keys. Imported or downloaded snapshots retain the local keys by matching provider identity, and native edits preserve fields unknown to the Windows editor, including special recurrence types.

### Quick Checklist And Browser Time (1.16.19)

- The quick checklist entry is available from Daily Board and Todo on Android, the LAN Desktop Web, and the Windows client; edits render a live hierarchical preview before confirmation.
- `-`, `--`, `---`, and deeper prefixes create real TodoItems at the corresponding depth. Enter continues the previous depth, and `--#` attaches a visible note to the nearest task without becoming a task.
- Confirmed groups have a title-only container. Membership, parent ID, depth, sibling order, note, and collapsed state persist in Room `31`; existing items and todos are migrated unchanged by `30 -> 31`.
- Parent completion is derived from non-canceled children. Canceled children do not block completion, canceled parents remain canceled, deletion of a sole child does not auto-complete its parent, and recalculated state propagates to valid ancestors.
- Cancel writes history; delete does not. Android, LAN Web, and Windows restore canceled or completed ordinary todos and quick-checklist subtrees through the same history lifecycle; historical entries expose restore and delete instead of complete/cancel.
- Checklist fields survive `.paykibak` backup/import, cloud snapshots, Android-to-LAN/Windows synchronization, and Widget summary generation.
- Progress counters exclude canceled tasks. Duplicate fingerprints are rejected before a second group is written.
- Android, LAN Web, and Windows diagnostics can select the original error line. Widget rows show `☐ / ☑`, group progress, and only today's completed checklist items.
- Windows completion cannot revive canceled tasks, cancellation cannot overwrite completed tasks, and a fresh desktop snapshot uses a valid 64-bit default group ID.
- Administrator registration, sync, device-activity, and audit timestamps are emitted as epoch values and formatted with the browser's local timezone instead of the server timezone.

### Android Interaction Round (1.16.15)

- Drawer identity shows the app icon, nickname and server-issued immutable user ID, and opens account settings.
- Authenticated nickname/password changes and device-session inspection/revocation are supported. Password changes use a revision-checked atomic password-hash/snapshot-key update, revoke existing sessions and preserve local business data.
- Calendar months page vertically; agenda weeks page horizontally in both directions. Bounded internal pager indices are recentered across date windows to avoid Compose float precision errors with huge indices.
- Today preserves the actual day, recenters three-day view and scrolls/highlights the full current agenda row. Month double-tap enters a centered three-day view.
- Agenda date rails only repeat weekday/date, titles are compact, accent bars follow the full text block and text colors meet a 4.5:1 contrast target against the event surface.
- Todo shares Daily Board artwork; the circular create icon opens batch creation after an 800ms hold. Todo history is accessed from Todo; group names and dots are larger and group manager rows are compact.
- Planning document lists own creation, rename and swipe-to-confirm archive, with image recognition retained in the document sheet. The main toolbar contains only document selection and recognition, and help is in the top bar.
- AI provider names are not automatically numbered or ellipsized; sound choice and strategy share a compact settings page.

### Current Reliability Round

- reminder polling passes the concrete scheduled trigger timestamp to the same foreground-service entry used by alarm broadcasts, so one todo/trigger pair cannot be dispatched twice through the polling path
- normal reminder acknowledgement, completion, notification retention, and stale-alarm rejection preserve the dispatch claim; only deliberate rescheduling or schedule rebuilding clears it, preventing a past trigger from being redispatched
- new installs no longer schedule the legacy `+2s` backup alarm, and application startup cancels old backup PendingIntents left by earlier versions
- notification full-screen intent is the sole reminder surface when Android permits it; direct Activity and accessibility-overlay launch remain fallback paths only when the system full-screen mechanism is unavailable
- Planning Desk mappings, nodes, note creation, mapping-status refresh, and snapshot import use the repository change callback so widgets and enabled account sync observe direct Room writes
- account authentication's Turnstile WebView permits the Cloudflare challenge host and third-party cookies, blocks failed HTTP responses or a dead WebView renderer, and observes the response field as a fallback when a WebView omits the parent callback; the server requires Siteverify success, the fixed action, and an allowlisted production hostname
- account authentication's Turnstile WebView also allows Cloudflare's `about:blank` / `about:srcdoc` internal documents, suppresses duplicate token callbacks, and keeps AndroidView callbacks aligned with the current Compose state
- Turnstile token fallback polling is owned by the native WebView lifecycle, has an explicit timeout, and is stopped on reload, error, renderer loss, or release
- AI daily/weekly reports remain AI-first and fall back to a visibly local template only after a disabled, unavailable, failed, or blank real business request; no synthetic periodic anti-detection traffic is generated

### Account Boundary And Local Migration

- the Android app uses a normal PaykiTodo account login gate; after login, the local Room database remains usable offline and remains the authoritative working copy
- the unauthenticated login surface exposes no migration actions; `.paykibak` export and import are both available only after successful account login under `设置 -> 数据与备份`
- the account login/register/recovery surface uses the existing dashboard artwork and PaykiTodo launcher icon and remains vertically scrollable on narrow screens
- unauthenticated share, camera, voice, reminder-detail, widget, and phone-LAN entry points do not expose local data; an expired session stops cloud sync and the phone-LAN service
- logout stops the phone-LAN service and refreshes widgets immediately
- migration archives preserve business data and ordinary settings while excluding account tokens, cloud encryption secrets, AI API keys, Desktop Sync keys, and file-provider authorization

### Task / Todo System

- create, edit, delete todo items
- Shared todo/event editors persist changes only through the top-right completion action; system dismissal, sheet dismissal, and the top `取消` action silently discard the current draft without writing data or showing a second discard confirmation
- title / notes / multi-group tags / deadline / multi-reminder fields
- reminder delivery exposes one frequent `提醒` checkbox outside advanced options; disabled means unscheduled, while enabled always uses both the full-screen surface and the notification bar
- Todo editor shows title / DDL / group and reminder controls in the primary editor flow, while notes / recurrence / ring / vibration remain advanced options and auto-expand when editing existing todos that use advanced state
- legacy `FULLSCREEN`, `NOTIFICATION`, and `FULLSCREEN_AND_NOTIFICATION` values remain readable through backups, recurrence templates, batch imports, and Desktop Sync, but all enabled reminders normalize to the combined delivery contract
- Todo editor can enable `闹钟模式（持续响铃直到操作）` for DDL-backed reminders; alarm mode persists on todos and recurring templates, future recurring instances inherit it, and timeout downgrade leaves an explicit unhandled-reminder notification plus limited retry bursts
- Todo editor can mark a DDL-backed task as `倒数日`; the task then uses its DDL date as the countdown target and appears on board / desktop / widget countdown surfaces
- Todo editor can mark a DDL-backed task as `仅提醒，不在看板/日历显示`; the task still schedules reminders and remains manageable in My Tasks, but is filtered out of the phone daily board, Android board widget, desktop board, countdown board queries, and AI daily/weekly todo statistics
- Desktop Web todo editor exposes and preserves `闹钟模式` and `仅提醒，不在看板显示`, matching the phone editor for these DDL-dependent todo fields
- Desktop Web todo and event editors match the phone with one reminder checkbox; clearing it saves an empty reminder-offset list and does not schedule a reminder
- Desktop Web todo and event editors use compact weekday multi-select chips for weekly recurrence instead of requiring manual `1,3,5` text input, and hide those chips for non-weekly recurrence rules
- Desktop Web Planning Desk recognition preview uses the same weekday multi-select chips for weekly recurrence candidates, and clears weekly-day state when the candidate is changed away from `每周`
- recurring todo templates persist `hiddenFromBoard`, and newly replenished recurring instances inherit the reminder-only visibility setting
- phone todo editor recurrence type selection uses a compact single-choice row/dialog instead of a horizontal enum button group; weekly weekday chips remain only for the actual multi-select weekday set
- no-deadline todos; active no-DDL items are treated as `今日待办` across phone board, Android widget board query, desktop board, and desktop todo management
- lightweight comma-based todo batch import with preview validation
- todo batch-import DDL supports same-day clock input such as `16:30` / `16：30`, plus Planning Desk-style natural date forms such as `5.28`, `5月28日`, `明天`, and `周五`; date-only values default to `23:59`
- Todo page exposes todo batch import beside the bottom-right new-todo button instead of as a top content row
- Daily Board and Todo pages enter batch selection by long-pressing an active todo; the long-pressed card is selected immediately, and ordinary browsing has no persistent batch-operation card
- Batch selection changes the top title to show the selected count, keeps select-all / clear in the top bar, and fixes icon-free rounded complete/cancel actions at the bottom so they remain reachable while scrolling; it can toggle individual visible cards, complete through the existing completion/history/reminder path, cancel selected current todo instances into history after one confirmation, or exit without changing data
- Todo long-list rendering isolates Desktop Sync polling and Daily Board clock/derivation state from the Todo page, limits group resolution to the active section, reuses formatted colors/DDL text, and draws each card in one lightweight layer instead of nested surfaces
- drawer navigation exposes a single-line `待办` entry; group filtering and group management live in the todo page's near-full-width, height-limited dropdown instead of a horizontally scrolling chip bar, expandable drawer group list, or standalone group page
- todos support multiple group tags through `todo_group_tags`; phone-side group filtering is multi-select intersection filtering, and the todo editor uses compact multi-select group chips while preserving the first group as the primary display color
- complete / cancel / restore flows
- active todo preview now uses the same bottom-sheet visual language as calendar event preview
- active todo card body opens preview; completion is isolated to the checkbox to avoid accidental completion
- active todo quick preview and quick action surfaces expose one clear `取消待办` archive action instead of repeated cancel/archive buttons; the phone details preview keeps cancellation in the fixed bottom action area, desktop Web uses the same single cancel entry, and recurring todo cancellation asks whether to cancel only the current task, current-and-future tasks, or the whole recurring series
- todo quick preview is now the shared target for phone search results, Planning Desk linked todos, notification routes, and widget/deep-link todo opens, so active todos from those entry points can cancel/archive without detouring through the editor
- desktop web todo preview exposes a single cancel/archive action plus a separate hard-delete path with explicit history semantics; recurring todo cancel/delete actions ask for a scope instead of silently defaulting to current-and-future
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
- phone drawer uses a grouped navigation hierarchy with a product identity header, always-visible primary and review destinations, fixed Settings / appearance utilities, and a descriptive bottom-sheet theme picker; all seven destinations remain directly reachable in light and dark modes
- board todo block includes missed active todos, today's normal todos, and active no-DDL todos
- board remains an overview-first surface for schedules, announcements, and countdown targets, while its todo cards support the same long-press batch completion/cancellation mode as the Todo page; editing still routes to the dedicated todo surface
- every todo full-screen reminder exposes `我知道了`, including same-day due todos; acknowledgement only dismisses the current reminder and leaves completion, cancellation, DDL, and later reminders unchanged
- board view can show today's todos and today's / tomorrow's schedule summary together
- board can show a `倒数日` card for active countdown-enabled todos / events; todos count down to DDL times, events count down to event start times, exact-time expired targets are hidden, long target titles wrap fully, and tapping rows opens the corresponding todo / event preview first instead of jumping straight into an editor
- board section title / empty-card areas navigate to the corresponding task or calendar surface while concrete todo / event rows keep their detail-preview behavior
- board today's schedule hides timed events after they have ended
- board currently running events can be visually highlighted with a gold outline and subtle glow
- board greeting card supports compact collapse / expand behavior
- board announcement, countdown, today-todo, and the combined today/tomorrow schedule card support collapse / expand controls with locally persisted state; tomorrow itself no longer has a separate collapse button inside the schedule card
- phone daily board uses the user-selected `dashboard_bg_light` / `dashboard_bg_dark` landscape artwork, original Forest / Clay / Ocean palette, and custom `payki_greeting` font; these assets are protected visual identity rather than generic modernization targets
- activity timer / `今日做了什么` / manual actual-record controls are not part of the active board product; the board stays focused on todos, schedules, announcements, and countdown targets
- board schedule rows align the left color strip to the measured height of the event text block
- board schedule rows keep normal and in-progress color strips in one aligned column
- normal board schedule rows have no outer fill or border, while in-progress rows use a gold border with only subtle inner highlight
- daily board schedule rows use compact density with explicit small line heights, one-line locations, tighter padding, and smaller in-progress check-in actions
- board in-progress schedule rows show check-in status for check-in-enabled events and expose compact `签到` / `签退` actions
- board in-progress schedule rows route check-in-enabled events to the independent full-screen check-in surface through `去签到` / `查看`, keeping the board focused on overview rather than inline operations
- daily board uses a compact `暂无日程` state when today's visible schedule is empty, including after all of today's events have ended
- daily board always shows the tomorrow schedule section inside the expanded schedule card, including `明天暂无日程` when tomorrow has no events
- daily board schedule card separates a paper-white `今日剩余` area from a full-width tinted `明日安排` area; both show counts, tomorrow shows its full date/weekday, and overflow beyond two rows links to Calendar
- daily board onboarding card is readable in dark mode, can be dismissed, and can be reset from Settings -> About -> 使用说明
- daily board section titles use compact semibold typography without wallpaper-compensating text shadows
- light mode uses cool-white page backgrounds, paper-white surfaces, and soft gray-green weak containers instead of Sand beige as the global background; warm Clay remains an accent rather than a large-area fill
- light mode uses a distinct light blue-gray page layer, pure-white content surfaces, darker secondary text, and visible gray-green controls; Calendar separates its toolbar and time-grid surfaces while the Board tomorrow band uses a stronger blue tint
- Todo dark mode uses an explicit deep blue-gray workspace and distinct card/weak-container layers instead of a pure-black page slab; protected Daily Board artwork remains separate
- board surface intentionally does not expose add / batch-add buttons

### Reminders / Notifications

- reminder alerts use a flatter high-priority surface with centered large time/title, 56dp action buttons, collapsible notes, entry animation, and a distinct alarm-mode pulse
- full-screen todo reminder surfaces are intentionally minimal: they expose completion and fixed 10-minute snooze only; cancel/archive and DDL changes are handled from todo details / editors instead of the ringing surface
- alarm mode loops sound and vibration until the user completes, snoozes, cancels, or acknowledges; after 5 minutes it stops continuous ringing, updates the notification to `未处理提醒`, and performs three 30-second retry bursts at 2-minute intervals
- daily brief notification can be enabled from Settings with a configurable time, defaults to 08:00, summarizes today's todos/events and nearest <=7-day countdown target, and opens the daily board
- calendar events can show a more visible ongoing notification during the event using channel `ongoing_event_v2`; start/end alarms are restored independently of whether the original reminder time has already passed, and the ongoing schedule notification no longer depends on the event reminder being enabled
- ongoing event notifications explicitly set RemoteViews text colors in code and avoid the decorated custom big-content style, reducing unreadable white/gray text on OEM light notification panels

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
- phone Planning Desk now defaults to a memo-style natural-text editor: the visible path is document selection / help / `识别计划` / overflow actions, followed by preview-first import confirmation
- phone editor mode is intentionally plain natural writing, not a Markdown/tag tutorial surface; the old Outliner and Markdown-compatible tools remain legacy internals rather than the default user path
- phone Planning Desk secondary actions (新建/重命名/使用说明/归档/删除) are in an overflow DropdownMenu; manual save button removed in favor of auto-save
- phone Planning Desk includes a multi-page in-screen beginner tutorial aligned with the memo workflow: write naturally first, recognize/import preview second, AI as optional enhancement with local-rule fallback, and directly usable examples without hashtag tags
- phone Planning Desk editor auto-saves after a short debounce and saves before switching planning documents
- Enter in the memo editor behaves like a normal note editor; it no longer tries to force Markdown checkbox continuation on the main path
- the phone Planning Desk shortcut toolbar is removed from the default memo path so planning starts from writing, not from learning syntax buttons
- local Planning Desk parsing recognizes plain bullet lines such as `- 想办的事`, `* 整理材料`, and `• 发消息` as no-DDL todo candidates for the collection / scratchpad workflow; preview candidates explain that ordinary bullets were recognized as no-DDL todos
- local rule parser recognizes markdown checkboxes for compatibility, completed-task skip, subtask parent notes, lightweight bare `ddl` text such as `任务M ddl 15:00`, natural DDL hints, no-DDL todo lines, locations, and natural schedule ranges; hashtag planning tags are no longer the encouraged parser path
- local rule parser also recognizes common Chinese natural DDL hints, including date-context fuzzy words such as `晚上交论文`, before-time forms such as `5点前` / `16:30之前` / `明天下午3点前`, and non-checkbox DDL keyword lines such as `交论文 截止明天 23:59`
- Planning Desk preview warns when a todo DDL is inferred from natural text and flags recurrence hint words such as `每天` / `每周` without auto-creating recurrence rules
- planning document titles, document dates, and heading-style lines no longer provide hidden default dates; users should write the intended date in the actual line when it matters
- natural schedule parsing accepts inline leading dates, leading date ranges such as `06-22~06-24`, time ranges later in the line, slash dates, full-width separators, Chinese AM/PM, and full-width range separators
- natural schedule parsing extracts `备注：...` / `备注:...` into event notes while preserving original Chinese punctuation
- legacy hashtag semantic tags such as `#ddl`, `#today`, `#tomorrow`, `#important`, and `#project` are no longer part of the documented Planning Desk workflow
- natural schedule import creates calendar events by default; linked-todo creation is an explicit preview/editor choice, not automatic
- planning import is preview-first and selection-based, not immediate database writes; import is disabled until at least one valid candidate is selected
- planning preview cards are editable before import for title, group, notes, DDL/start/end times, mixed reminder input, reminder enable/disable, reminder delivery mode, and event linked-todo creation; preview has select-all / clear-all controls
- planning recognition preview has a collapsible batch-settings area that can apply countdown to selected events / DDL-backed todos, linked-todo creation, check-in, and unified group changes to the currently selected candidates
- successful planning import no longer appends visible `#imported` markers to user text; duplicate prevention relies on internal `planning_line_mappings`
- imported planning lines now also create stable `planning_line_mappings` entries that link the source line to the created todo/event item
- mapping relocation uses normalized fingerprints plus fuzzy text matching, so the planning linkage is not purely a stored line number
- mapping status sync distinguishes `ACTIVE`, `COMPLETED`, `CANCELED`, `ORPHANED`, and `CONFLICT`
- Planning Desk refresh can update current-section or whole-document imported items from the latest Markdown, but only for unfinished active mappings
- Planning Desk batch postpone can shift unfinished imported items and the corresponding Markdown time text together
- the latest import / refresh / postpone batch can be undone
- conflicts between imported items and source Markdown can be resolved either by overwriting the item from the document or rewriting the document from the current item
- default Planning Desk import reminder is 5 minutes before, full-screen, ring + vibration, but preview can explicitly disable reminders and an empty reminder input is preserved as no reminder instead of being refilled
- Planning Desk natural event lines can include tail reminder clauses such as `提醒时间：提前1h，06-24 22:10`; local parsing converts them to reminder offsets and keeps the clause out of the event title
- Planning Desk AI post-processing strips reminder clauses from imperfect AI-returned titles and can recover reminder offsets from the returned title or original source line
- Planning Desk can import already-past todo/event candidates as historical backfill when reminders are disabled; event candidates can also keep enabled reminders because already-past event reminder triggers are filtered rather than backfilled, while enabled past todo reminders still block import
- planning notes are included in JSON backup / restore snapshots
- planning mapping records are also included in JSON backup / restore snapshots
- legacy notes may still carry `documentDateEpochDay`, but recognition no longer treats the planning document date as a hidden default for user-entered lines
- Planning Desk `1.13.21` fixes explicit DDL-date precedence for phone / desktop parser paths: lines such as `5.29 【DDL】...`, `5.29【DDL】...`, `5.29 【DDL】14:00 ...`, and `5.29 【紧急】【DDL】14:00 ...` use the inline date/time instead of the planning note's document date, while DDL markers and time tokens are removed from the final title
- Planning Desk `1.13.22` makes the mobile Planning Desk start as a wide free-writing surface, keeps `识别` preview-first instead of background direct-write capture, and keeps AI/local fallback understandable in the editor copy
- Planning Desk recognition uses configured AI providers first when AI is enabled and at least one provider has Base URL / API Key / model; local rules remain the fallback when AI is disabled, provider setup is incomplete, or AI recognition fails. AI prompts no longer receive the planning document date as a hidden default
- AI recognition for Planning Desk is now an optional Provider-based enhancement for DeepSeek / Qwen / OpenAI-compatible APIs; Settings exposes ordered multi-provider Base URL/API Key/model configuration, single-provider model-list fetching, single-provider connection testing, both phone and desktop Planning Desk recognition call enabled sources in order, local rules remain the fallback, and AI output enters preview before import
- Settings -> AI 调用配置 shows an overall local status card for complete recognition providers, complete vision-capable providers, and enabled providers with missing required fields
- Planning Desk AI keeps group assignment conservative: AI `groupName` is preserved only when the source line explicitly contains a natural group field such as `分组：`, `项目：`, `类别：`, or `课程：`, so ordinary titles are not split into accidental groups
- Planning Desk AI / preview candidates carry event location, all-day, countdown, check-in tracking, and recurrence fields; phone and desktop previews can edit those fields before import, and imports persist them into the final todo/event drafts
- Desktop Planning Desk import accepts AI-returned preview candidates directly even when their IDs do not match local-parser `line-*` IDs, preventing the browser from showing one AI candidate but importing zero selected items
- AI source editing can fetch available models from OpenAI-compatible `/models` endpoints using only Base URL and API Key, then show a compact dropdown while preserving manual model-name entry for gateways that do not expose model discovery
- AI source editing can mark an enabled provider as `此服务支持图片识别`; phone Planning Desk uses only these vision-capable providers for screenshot / timetable image recognition
- Settings -> AI 调用配置 uses a page-level transactional draft: provider add/edit/toggle/reorder/delete operations, the overall AI switch, and report settings persist only when the top-right `完成` action is pressed; sheet/back dismissal discards the draft, while incomplete enabled providers show an in-page warning and disable completion
- AI provider connectivity testing sends exactly one fixed model-identification message to one endpoint with no automatic retry; HTTP 429 stops immediately, and success displays the service's returned model answer
- AI chat, model-list, vision, and remote-quote HTTP requests explicitly use the locally verified Windows Codex CLI User-Agent instead of Android's Dalvik default, preventing device model and Android build details from leaking through the request header
- AI provider chat calls accept common OpenAI-compatible endpoint shapes: root Base URL tries `/v1/chat/completions` before `/chat/completions`, `/v1` appends `/chat/completions`, full `/chat/completions` URLs are used directly, and full `/models` URLs convert back to sibling `/chat/completions`; non-JSON HTML responses produce a Base URL hint
- Planning Desk AI recognition is explicit-only: phone calls it from the `识别` button, desktop calls it from `识别` or `Ctrl+Enter`, and desktop import without preview no longer triggers AI silently
- Phone Planning Desk overflow menu includes `从图片识别日程`: it opens the system image picker, compresses the selected image to a 1600px-long-side JPEG, sends an OpenAI-compatible vision request through vision-capable providers, appends the returned Markdown to the current planning note, auto-opens preview when candidates are parsed, and still requires preview/import confirmation before database writes
- phone Planning Desk image recognition preview shows a clickable source-image thumbnail so the user can enlarge the original image and compare it with the parsed candidate list
- Android system share can send text or images to `添加到 PaykiTodo`; shared content now uses the capture recognition pipeline to directly create Planning Desk nodes and their linked official todos/events
- launcher shortcuts expose `拍照添加` and `语音添加`; photo capture opens the system camera through FileProvider, voice capture uses Android SpeechRecognizer with zh-CN partial results, and both route results through background capture recognition
- background capture recognition uses a `capture_processing` notification channel; completed capture notifications open the matching Planning Desk document after direct node insertion, while the phone Planning Desk Markdown compatibility capture starts background recognition only after an explicit confirmation instead of blocking the editor
- background capture notification delivery explicitly checks Android 13+ notification permission in the delivery function and catches permission-state races, so sharing/camera/voice capture still writes Planning Desk drafts without crashing when notifications are denied
- AI Provider API Keys and Desktop Sync access state / tokens are stored locally in settings and deliberately excluded from backup JSON export; importing a backup without AI keys preserves existing local keys, while importing an old backup that contains Desktop Sync enabled/token fields restores Desktop Sync as disabled and ignores the old token so the app can generate a fresh local access key after the user manually enables sync again
- account cloud sync uses short-lived access tokens, rotated refresh tokens, per-device revocation, PBKDF2-SHA256/AES-GCM encrypted `.paykibak` payloads, server revisions, and idempotent upload operation IDs; conflicts stop instead of silently overwriting
- Android stores cloud tokens and the account-derived snapshot secret in encrypted preferences, keeps Room locally authoritative, uploads after local writes, pulls clean remote revisions, and keeps the phone `18765` LAN service independent
- Windows cloud mode stores its account and full local archive through current-user DPAPI, supports offline todo/event/planning writes, and syncs through the account server without depending on the phone; phone LAN remains a separate compatibility mode
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

- Settings -> `AI 调用配置` includes an `AI 日报 / 周报` block with compact switches, HH:mm time fields, a report-retention dropdown, `了解 AI 日报`, and `立即生成一次日报`; schedule changes share the page-level top-right completion transaction instead of a separate save/re-schedule button
- the `了解 AI 日报` guide is a centered dialog explaining what daily/weekly reports summarize, AI-source and fallback requirements, Android 12+ exact-alarm caveats, enable steps, the independent `AI 报告` archive, notification deep links, and common failure cases
- daily reports collect today's completed todos, missed todos, today's events, tomorrow events, and tomorrow DDLs; today's event check-in investment minutes are included only when at least one check-in minute exists
- weekly reports collect Monday-Sunday completed todos, missed todos, week events, and next-week DDLs
- report generation tries enabled Planning Desk AI providers in order through `PlanningAiCaller.callWithFallback`; if AI is disabled or fails, a local template still generates a usable report
- daily-report prompts and the local fallback report include `今日日程签到投入：Y 分钟` only when `event_check_ins` has a positive daily total; missing check-in data is treated as absent evidence, not as `0 分钟投入`
- reports are stored in the independent Room `ai_reports` archive rather than Planning Desk notes; the drawer exposes `AI 报告` after `规划台`
- upgraded installs migrate legacy Planning Desk `AI 日报` / `AI 周报` note entries into `ai_reports`, delete the old report notes, and clear `lastOpenedPlanningNoteId` if it pointed to a removed report note
- `AI 报告` supports keyword search, all / daily / weekly filters, all-time / recent-7 / recent-30 / recent-90-day range filters, card previews, full detail viewing, local-fallback/source pills, empty-state guidance, and delete confirmation from long press or detail view
- deleting an AI report conditionally cancels the daily/weekly notification only when that fixed notification currently points to the deleted report, preventing stale deep links without removing a newer report notification
- AI report retention can be set to 30 days / 90 days / 365 days / forever; generating a new report purges older archived reports according to the current setting
- report notifications use a low-priority `ai_report_channel`, skip posting if Android 13+ notification permission is missing, and deep-link to the matching AI report detail
- `DailyReportScheduler` schedules daily and Sunday weekly report alarms, cancels disabled schedules, and is invoked on app startup plus boot/time/timezone recovery; it uses exact alarms when allowed and safely falls back to system-allowed idle scheduling when exact-alarm permission is missing
- daily/weekly receivers always reschedule the next alarm in a `finally` path, so database, notification, and other non-AI failures cannot silently terminate future reports
- app startup checks the most recent missing daily/weekly period only when the prior recorded trigger is absent or overdue, avoiding a false previous-day recovery after first enabling reports; scheduled generation reuses an existing same-period report to prevent duplicates, while manual immediate generation remains explicit and force-generating
- `AiReportRuntimeStateStore` persists next trigger time and exact/inexact mode, last attempt/success/failure, provider/local-fallback status, and sanitized AI or full-chain errors
- Desktop Sync exposes authenticated read-only `/api/ai-report/status` diagnostics with schedule/settings state and recent report metadata/previews, deliberately excluding provider API keys and Base URLs
- backup / restore snapshots include `aiReports` and the report-retention preference, so exported JSON preserves report history and cleanup policy while still excluding AI API keys

### Calendar System

- timeline-style calendar foundation
- Desktop Web Calendar `1.15.15` uses a compact workstation composition at 125% Windows scaling: 58px top navigation, 296px mini-calendar sidebar, unframed continuous calendar grid, restrained blue action/selection accents, and thin neutral separators; the misleading sidebar group list is removed and Excel import is an explicit preview action
- Desktop Web Calendar supports functional day, seven-day week, and 42-cell month views; mini-calendar month/date navigation, event/location search, per-group visibility filtering, and collapsed Excel import share the left sidebar
- Desktop Web event editing opens a reference-proportioned two-column window with the PaykiTodo event form on the left and a live single-day timeline preview on the right; title/date/time/color edits update the preview without changing save semantics
- Desktop Web calendar CSS explicitly neutralizes old full-width navigation/modal-button inheritance, preventing document-width expansion, off-screen tab scrolling, second-row header actions, and stretched close controls
- timeline quick-create clears the visible `新日程` pending placeholder as soon as its editor opens, so a successfully saved event immediately renders with its real title
- phone event details do not expose a completion action; calendar events can be canceled with their underlying record retained or deleted, but only todos can enter the completed state. The Todo history surface excludes calendar events.
- day / multi-day / month / agenda style views exist in code, with ongoing refinement
- normal events, all-day events, and recurring events
- event location / notes / color / reminder settings
- event cancellation is a first-class history-preserving action: phone and Desktop Web previews expose `取消日程`, canceled events are marked `canceled = true`, and delete remains hard removal
- phone and Desktop Web event details expose `创建副本`; the copy editor preserves editable event/reminder/check-in/recurrence fields while dropping the original database identity and multi-slot bundle identity
- phone history cards visibly distinguish `已完成` and `已取消`; historical event details show complete start/end time and location instead of treating event start as a todo DDL
- recurring event current-instance delete uses a recurring-instance skip plus hard delete, while recurring event cancel writes canceled history for the selected scope
- phone and desktop event editors can set an event's primary group; phone uses a compact single-choice row, and the selected group is persisted for normal event creation/editing plus weekly multi-slot event creation
- phone calendar keeps a bounded loaded event window and skips redundant Room queries when the requested visible date range is already covered, reducing timeline swipe churn without changing displayed events
- phone Calendar three-day continuous-scroll view initializes its first visible range around the selected date instead of the far start of the long date window, preventing an empty-looking first render when current events exist
- Desktop Web event editor supports multi-line event titles, matching phone-side event title entry instead of forcing a single-line input
- Desktop Web event editor uses the same single combined reminder checkbox as the phone editor; clearing it saves an empty reminder-offset list
- Desktop Web event creation supports `每周多时间段` for new events, matching the phone-side multi-slot workflow by creating one weekly recurring event per weekday/time slot through the batch event API; generated slots now share a persisted `multiSlotBundleId`
- Desktop Web 日程时间轴 supports local weekly Excel import for `review.xls` / `.xlsx`: the browser reads the user-selected file only, parses date headers plus left-side time slots, lets internal cell time ranges override row times, handles merged-cell duration inference, previews candidates, marks existing / in-file duplicates, and imports only selected confirmed events without touching the Excel file or wallpaper workflow
- Phone event creation supports `每周多时间段` for new events: multiple weekday + start/end slots share one event title, location, notes, reminder settings, color, countdown, and check-in configuration, then create one weekly recurring event series per slot; reminder input is validated and converted per slot anchor so each period gets reminder offsets relative to its own start time
- Weekly multi-slot events persist a shared bundle id on event items and recurring templates; phone and Desktop Web editors can synchronize shared fields across all active bundled slots while preserving each slot's own weekday and start/end time
- event editor can mark important events as `倒数日`; the countdown target is the event start time and it appears on board / desktop / widget countdown surfaces
- event editor exposes an optional `打卡追踪` switch under `日程标记`; moving an event preserves both countdown and check-in marker state
- event data now supports optional check-in tracking fields and accumulated invested minutes; repository and desktop-sync APIs can create check-ins, check out active records, list event check-ins, and recompute total event investment time
- event details bottom sheet shows a `打卡追踪` card for enabled events, including total invested time, active `签到中` status, closed / active segment rows, and a `去签到` / `查看签到` jump into the full-screen check-in surface
- repository, Planning Desk synchronization, and Desktop Sync completion paths reject attempts to mark calendar events complete, preventing hidden event-completion states from reappearing outside the phone UI
- phone check-in operations now have an independent full-screen `CheckInActivity` with event title, live clock, large state button, sign-out confirmation, multi-segment re-check-in support, auto-backup after successful sign-in/out, and main-surface refresh after returning
- event check-in has an idle auto-checkout watchdog with a configurable threshold; app startup / resume and widget refresh can close stale active records at the event end time and send a low-priority auto-checkout notification whose collapsed title includes the event name
- check-in-enabled event reminders expose `签到` directly on the full-screen reminder page and the accessibility fallback overlay; signing in also acknowledges the current event reminder so the strong-reminder surface closes
- Settings -> `日历与提醒` can automatically sign out an active check-in when the event ends; event completion statistics are no longer part of the calendar model
- Settings -> `日历与提醒` also exposes `闲置自动签退阈值`, defaulting to 2 hours, with backup / restore preservation
- todo and calendar reminder editors use a multi-select dropdown with 5/15/30-minute, 1/2-hour, and 1/2-day presets plus custom reminder input
- check-in-enabled events can store independent check-in reminder offsets; ordinary and check-in offsets are merged and deduplicated when alarms are scheduled
- recurring templates, weekly multi-slot events, weekly schedule templates, backup/restore, and Desktop Sync preserve check-in reminder offsets
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
- todo detail/edit surfaces keep the complex DDL adjustment responsibility; the full-screen reminder surface no longer exposes custom snooze, cancel/archive, or `DDL 推迟`
- notification reminder path
- full-screen reminder path
- reminder delivery policy treats every enabled reminder as combined full-screen and notification delivery; legacy notification-only or fullscreen-only storage values normalize to the combined behavior, and disabled reminders remain unscheduled
- completing a todo from the full-screen or accessibility reminder removes the notification; snoozing or acknowledging retains a silent dismissible notification whose tap opens the todo/event preview instead of reopening the reminder surface
- snoozing changes only `reminderAtMillis`, never the todo DDL; pending-miss detection skips a todo while its snoozed reminder is still in the future
- changing an existing item to no reminder immediately cancels its stored AlarmManager reminder alarms, reminder notification, dispatch markers, and full-screen handoff state; event ongoing notifications remain independent
- completing/canceling a todo or entering from a stale notification also stops the matching foreground reminder service, sound/vibration session, accessibility overlay, notification, and reminder-session markers; the service tracks its current item ID so cleanup does not stop an unrelated reminder
- full-screen reminder relaunch paths keep a short recent-surface cooldown only after `ReminderActivity` actually reaches `onResume`, so silent Android background-launch blocks do not incorrectly suppress retries; Android 14+ full-screen permission state is logged and unavailable permission triggers the accessibility fallback path
- reminder alarm notification channels use the current v10 channel IDs so old user/system channel downgrades do not keep suppressing high-priority full-screen reminder notifications
- full-screen event reminders can start an event check-in when `打卡追踪` is enabled
- ongoing-event notification end broadcasts now re-check the current event state before clearing the notification, so a moved / extended event is not prematurely removed by an old end alarm
- foreground service and fallback chain work
- reboot / time change / timezone change recovery
- accessibility fallback path
- accessibility fallback event reminders can start an event check-in when `打卡追踪` is enabled
- Settings -> reminder diagnostics now includes a strong-reminder readiness panel for notification, exact alarm, full-screen permission, battery optimization, DND bypass, and accessibility fallback, plus a short-delay full-screen test reminder and readable chain-stage logs
- reminder, desktop-sync foreground, capture, report, ongoing-event, and check-in watchdog notifications use the dedicated vector `ic_stat_payki_todo` small icon resource, so notification shade icons are monochrome-mask compliant instead of depending on a colored bitmap
- calendar event acknowledgement preserves the configured reminder offsets instead of clearing the event's reminder setup
- reminder playback can now select alarm, accessibility, notification, or media audio channels
- PaykiTodo now tracks an internal reminder-volume percentage for self-played alert audio
- an advanced temporary system-channel volume boost can raise the selected stream during reminder playback and restore the original volume afterward
- work mode suppresses outward reminder sound by default and forces stronger vibration even when an individual item disabled vibration, while respecting the item's selected full-screen / notification channels

### Packaging / Identity

- adaptive launcher icon foreground and round icon foreground are wired directly to `ic_launcher_art_transparent`, the transparent main-logo resource, instead of the older opaque white-background launcher art
- adaptive launcher icons provide an explicit vector monochrome drawable through `ic_stat_payki_todo`, so Android themed-icon launchers do not derive a simplified icon from colored or stale opaque art
- obsolete unreferenced launcher-art variants are removed from source resources to reduce the chance of accidentally reverting to the wrong icon
- drawer header icon uses the same transparent logo resource and is clipped into its circular header surface to avoid exposing a white rounded-rectangle background
- launch screen uses a transparent-logo variant so the mountain / sun background remains visible behind the logo
- release-signing information template exists under `docs/templates/PaykiTodo-Release-Signing-Template.md`; real signing values belong only in ignored `user/keystore.properties`
- release builds read `user/keystore.properties` first, retain a root-level fallback for older local setups, and fail early with a clear error when the release keystore or required signing fields are missing, preventing unsigned or accidentally debug-signed release artifacts
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
- widget `1.13.71` contrast pass makes the board widget schedule aggregate card, schedule event rows, and empty schedule prompts use dark translucent surfaces in both default and night resources, preventing white-on-light schedule text when launchers mix widget day/night resources
- planning-desk `1.10.3` pass changes event import to default event-only, keeps linked todo creation behind an explicit preview checkbox, removes the fixed generated linked-todo note, and parses `@地点` / quoted `@地点` / `地点：...` into the event location field
- desktop-web `1.10.3` pass adds compact event color preset swatches matching the phone event editor while preserving custom color input
- repository todo mutations and Planning Desk note edits / delete / archive operations notify widget data refresh through the application-level widget callback

### Data / Backup / Diagnostics

- Room-based local storage
- `.paykibak` ZIP migration archive export / restore with manifest SHA-256 validation; legacy plain JSON backups remain importable
- migration archives retain AI provider labels/Base URLs/model names without API keys, exclude backup-directory authorization URI, Desktop Sync token, and remote-sync device secret, and import preserves credentials from the current installation
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
- Debug-to-Release migration is supported through `.paykibak`; the import flow backs up current data first, restores reminders/widgets, keeps current installation credentials, and accepts legacy JSON
- optional account sync is local-first and AES-GCM encrypted; local data changes schedule a delayed upload, enabled sync has a one-minute fallback, clean clients pull newer revisions, and conflicting dirty revisions are blocked
- the generic Nginx-compatible cloud service stores only encrypted payload bytes and account/security metadata; field-level operation merge remains intentionally unimplemented

### Input Help

- reminder, batch-import, and custom-snooze input fields expose nearby question-mark syntax help
- shared input-help dialog explains valid examples, field-boundary rules, and invalid-value behavior

### Desktop / LAN Assistance

- LAN browser-based desktop sync console exists
- Windows `win-x64` self-contained WebView2 client builds to `desktop/dist/PaykiTodo.exe`, owns an encrypted local working copy in cloud mode, and retains direct phone LAN access only as a separate compatibility mode
- phone-side HTTP serving model exists
- browser can perform limited data operations against the phone-side dataset
- desktop web can edit existing todos with title, notes, DDL, the combined reminder switch, multi-group tags, recurrence, ring, and vibration fields
- desktop web todo title editing uses a multiline textarea and preserves newline display in todo/event cards and previews
- desktop web recurring todo/event editing exposes the same scope model as the phone side (`仅当前` / `当前及之后` / `整个循环系列`); turning a recurring todo into non-recurring defaults to `当前及之后` so future generated instances do not keep appearing
- desktop web todo management has compact multi-select group filter chips with intersection semantics, and todo cards / previews / board rows display all group names instead of collapsing to one group
- desktop web todo/event reminder inputs accept AM/PM, Chinese AM/PM, relative-date, weekday, dot/slash-date, Chinese-date, full-width separator, and Chinese-comma reminder syntax in addition to the existing minute and ISO-like forms; placeholders now show these examples
- desktop web todo cards open a detail preview first; event cards open the editor directly, while destructive actions still require confirmation
- desktop web todo and event reminder editors accept mixed reminder syntax matching the phone-side examples, including minutes, same-day time, current-year date-time, and full date-time
- desktop sync API accepts todo `reminderOffsetsMinutes`, allowing desktop-created / edited todos to persist multiple reminders
- desktop sync API exposes and accepts todo `groupIds`, so desktop-created / edited todos preserve phone-side multi-group relationships
- desktop sync API exposes event `checkInEnabled`, `checkInReminderOffsetsMinutes`, and `totalCheckInMinutes`, provides event check-in endpoints for listing records / checking in / checking out, and rejects event completion requests
- desktop Web event editor exposes `打卡追踪`; event preview renders a check-in card for enabled events, lists records, shows total invested time / active segment state, and can call `签到` / `签退`
- desktop Web todo/event editors use the same multi-select reminder presets and custom input as phone; enabled event check-in adds a separate check-in reminder selector
- desktop web todo / event editors use a bottom-sheet-like visual structure with cancel / centered title / save actions
- Desktop Web uses a light top-navigation workstation with a compact upper-right connection indicator instead of the older dark admin sidebar
- Desktop Web Calendar keeps a continuous 7-day desktop grid and responds to narrower widths with 3-day or 1-day columns; Excel import is collapsed by default to preserve calendar focus
- Desktop Web event editors use a centered flat dialog and phone-aligned field order: title, start/end, location, reminders/check-in, notes, recurrence, group, and color
- Desktop Web includes an `AI 报告` tab for report scheduling health, sanitized failure diagnosis, and recent report previews
- Desktop Web `AI 报告` also edits the phone's AI provider list and report settings, retrieves provider models, sends one fixed single-request connectivity test, and saves the whole page only through `完成`; API keys remain masked in browser responses
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
- desktop sync status filters connection addresses through a shared selector that merges active Wi-Fi link candidates with real network-interface candidates; it accepts campus CGNAT / campus public IPv4 reported by active Wi-Fi or trusted physical LAN interfaces while still hiding mobile-data, VPN, loopback, multicast, link-local, and low-confidence virtual addresses
- desktop sync address/status inspection is permission- and ROM-failure safe: network-state read failures degrade to a status hint and service-start failures roll the setting back instead of crashing Settings
- desktop sync can keep WiFi and a partial wake lock while the foreground service is running; the default-on `桌面同步期间保持网络唤醒` setting is surfaced in Settings and reflected in the foreground notification copy
- desktop web has a `规划台` tab with a memo-style textarea editor, document selector, auto-save, `Ctrl+S` save, `Ctrl+Enter` parse, editable parse preview, selected import, and a help modal aligned with the phone-side natural-writing workflow
- desktop web sidebar separates `每日看板` from an independent `待办` management tab; the board stays a lightweight overview while the todo tab directly loads the paged searchable todo list
- desktop web calendar timeline supports dragging timed event cards in 15-minute increments across visible day columns; recurring-event drags update the current instance by default, and already-ended event cards fade visually
- desktop web event color presets match the phone's ordered 12-color palette and render as compact rounded-square swatches rather than full-width modal buttons
- desktop web recurring todo cards explain that generated instances are intentionally limited to a near-future window while the recurrence template keeps the real end date and replenishes future instances over time
- desktop web Planning Desk uses phone-local `/api/planning/*` routes, edits the same Room planning notes as the phone UI, saves before switching documents, blocks empty selected imports, keeps imported state in internal mappings instead of writing visible hashtag markers, and reuses the same AI recognition / local fallback path as the phone Planning Desk
- desktop web Planning Desk now shows the current note title and mapping status preview; legacy refresh/postpone/undo internals remain available behind the sync API but are not the default visible planning entry
- desktop web Planning Desk create-note, refresh-imported, postpone-imported, and child-node creation flows use PaykiTodo in-app form dialogs instead of browser-native `prompt` / `confirm`; postponement exposes mapped item and scope as dropdowns rather than manual row-number / `1/2/3` input
- desktop web in-app form dialogs support keyboard confirmation: Enter confirms text / number / select fields, while textarea fields keep Enter for line breaks and use Ctrl+Enter / Cmd+Enter to confirm
- desktop web `/api/snapshot` includes active Planning Desk announcements and the browser console renders them as a top orange announcement banner
- desktop web `/api/snapshot` includes a phone-derived `todayBoard` payload, and the browser first tab is now a daily-board view with current/next item, countdown targets, today todos, today schedule, tomorrow schedule, ended-event filtering, and in-progress gold highlighting above the full todo timeline; focus stats are kept out of the daily-board surface
- desktop web `/api/snapshot` reuses the groups already loaded in the snapshot JSON path instead of issuing a second Room group read for group-name/color mapping
- desktop sync server uses bounded client handling and byte-length request-body parsing, so abnormal LAN clients cannot grow threads without bound and Chinese long Planning Desk saves are not truncated by UTF-8 character/byte mismatch
- desktop web first connection keeps using the lightweight `/api/snapshot?scope=board` daily-board payload, while todo management now loads through paged/searchable `/api/todos?offset=...&limit=...&q=...` only when requested
- desktop web calendar timeline now loads only the visible date range through `/api/events?start=...&end=...`, and stale event-range responses are ignored when the user switches dates quickly
- desktop web todo / event mutations and Planning Desk import / refresh / undo paths refresh the current page's needed data instead of reloading one complete snapshot after every operation
- desktop web follows system dark mode through CSS variables for timeline cards, event cards, modal sheets, summary cards, tab buttons, sidebar cards, Planning Desk, inputs, and announcement surfaces
- desktop Web access-token validation is case-insensitive in both browser normalization and the phone HTTP server
- desktop Web login, sidebar, top bar, cards, timeline, Planning Desk, and modal surfaces use a compact neutral workstation visual system instead of aurora / purple gradient / glass styling

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
- Database version is `30`; migrations through `MIGRATION_26_27` cover the existing planning, reminder, performance, and recurring-skip schema, `MIGRATION_27_28` adds multi-slot bundle identity, `MIGRATION_28_29` preserves the activity-record compatibility table, and `MIGRATION_29_30` adds event check-in reminder offsets to items and recurring templates.
- Room schema export is enabled and `app/schemas/com.example.todoalarm.data.AppDatabase/31.json` is committed as the current database-31 reference schema; `MIGRATION_30_31` adds quick-checklist group, parent, depth, order, note, and folded fields.
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
- Phone Calendar horizontal drag offset reads are handled in layout/draw phases instead of ordinary per-pixel Compose state propagation; only a focused-day change reloads the visible Room range and recalculates event placement. The latest emulator swipe run measured 441 frames with p50 `21ms`, p90 `34ms`, p95 `40ms`, and 9.98% janky frames.
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
- remote-sync field-level conflict merge remains disabled; automatic pull is allowed only when the local copy is clean, while concurrent dirty revisions stop for explicit resolution
- tighter documentation so a new session can reconstruct state without old transcripts

## Notes

This is not a perfect specification. It is a working ledger so new sessions can quickly answer:

- what the app already does
- what is partially done
- what is still under active refinement

- desktop web event cards use string-compatible ID lookup for card-click editing, preventing silent failures when snapshot IDs are not the same JavaScript type

- desktop web all-day events are exposed through compact per-day pills that reuse the same event editor click path as timed events
- Daily-board normal schedule rows no longer show an outer border; in-progress rows retain the gold highlight
