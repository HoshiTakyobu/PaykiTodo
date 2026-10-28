# PaykiTodo Session Ledger

This file summarizes the original `【开发】PaykiTodo` session and the later continuation context in a repository-native form.

## Why This File Exists

The original PaykiTodo sessions became large and increasingly unreliable. This file converts the important project history into a stable handoff artifact.

## Latest Released Round

The released `1.15.18 / versionCode 360` round restores independent full-screen and notification reminder selections across phone/Web/Planning Desk, keeps single-channel storage values intact, limits retained post-action notifications to items that selected the notification channel, aligns Desktop Web hour labels with their solid grid lines, and fades ended events as complete cards. It preserves the `1.15.17` event-preview and snooze/acknowledgement work. Database version remains `30`; the signed APK is published in GitHub Release `v1.15.18`.

## Current Development Round

The completed local 1.17.5 / versionCode 397 round implements the original thread's 12-item Android/Windows visual and responsiveness feedback. Fixed Windows brand/navigation outlines, real font weights, colored cards, timed quick checklists, direct imported task presentation and selectable calendar colors are delivered. Phone edits are independent of background sync/reminders/backup completion; a held-network local completion subsequently persisted on Windows through an encrypted snapshot.

229 Android units, Lint, 28 distinct emulator tests, 65 Windows regressions plus isolated UIA and a cross-client bridge, and the final MSI lifecycle checks passed. Signed APK, Debug APK, MSI, portable directory and 18-image preview: `Deliverables/PaykiTodo/1.17.5`. [Validation](VALIDATION_1.17.5.md) records exact hashes, targeted reruns and boundaries. User 0 retains all 646 file hashes; user 10 business tables remain identical. New user 11 holds local fixtures; test server stopped, headless Pixel_8 restored to user 10 home. Local Chinese commit only; public release remains v1.15.18.

### Previous 1.17.3 search and collaboration

The local `1.17.3 / versionCode 395` round hides unauthorized shared search cache on both clients and fixes Android all-day search dates and dormant reminder presentation. Manual CAPTCHA/registration are complete. Actual dedicated 1.17.2 Android/Windows editor invite/accept/RSVP/bidirectional existing-event edits/revocation passed; 1.17.3 viewer access and live search revocation passed. Windows personal snapshot/revision 24, six original personal records/settings and 646 original Android user-0 file hashes remain unchanged. Test user 10 retains its identity and three revoked shared cache rows, all reminders off.

227 Android units, Lint, rebuilt signed APK verification, 57 Windows regressions, one 14-stage UIA flow and MSI install/real 1.17.2 upgrade/repair/payload/installed app/uninstall/user-data checks passed. Packages: `Deliverables/PaykiTodo/1.17.3`; [validation](VALIDATION_1.17.3.md), [native collaboration](NATIVE_COLLABORATION_ACCEPTANCE_20261025.md). Dedicated Windows host/client stopped; interactive Pixel_8 mirror remains. No push, release or deployment this round.

### Previous 1.17.2 personal/shared isolation

Windows normal shared-event saves no longer upload personal snapshots unnecessarily; personal sync preserves concurrent shared cache and continues uploading real personal edits/settings. The initial single-account build/acceptance record remains in [VALIDATION_1.17.2.md](VALIDATION_1.17.2.md). Its pending manual/two-account steps were completed in the follow-up above.

### Previous 1.17.1 rollout and participant update

The existing account server was updated on 2026-10-25 under the user's whole-task completion request. Current `/v3` calendar interfaces are live, credentials and personal snapshots are preserved, and Linux 21-test suite plus public dedicated-account read/write/conflict/authentication checks passed. No new client build or GitHub publication; see [rollout verification](PRODUCTION_CALENDAR_ROLLOUT_20261025.md).

The local `1.17.1 / versionCode 393` round completes shared-event participant selection and organizer/participant/current-response details on both clients. Edits preserve response authority, new instances/copies have independent invitation state, RSVP retains pending/in-flight local edits and current confirmed response, and source guards/all-day dates are rechecked. Room remains 33. Built APK/EXE/MSI and22 actual-control preview images exist ; 222 Android units,30 distinct emulator tests,55 Windows regressions plus one 14-stage isolated UIA flow,21 server tests and MSI lifecycle/data-preservation checks passed. See [VALIDATION_1.17.1.md](VALIDATION_1.17.1.md). That local round had no push, release or production deployment; the follow-up rollout is recorded above.

### Previous 1.17.0 calendar and account update

The original calendar/timezone/recurrence/all-day/copy and domain-free login server/compact-account work was built and verified in 1.17.0/392. Its per-event participant UI gap was subsequently closed in 1.17.1. The original results remain in [VALIDATION_1.17.0.md](VALIDATION_1.17.0.md).

### Previous 1.16.26 implementation

The local `1.16.26 / versionCode 391` round delivered calendar scope/dragging improvements and the Chinese Windows MSI. Historical evidence and limits remain in [VALIDATION_1.16.26.md](VALIDATION_1.16.26.md).

### Previous 1.16.25 implementation

The local `1.16.25 / versionCode 390` round implements the 2026-10-23 feedback: independent calendars, Windows two-step grid creation and scroll preservation, configurable main fonts, seven-day countdown history with memorial choices, recurring-task folding, categorized settings and the PaykiTodo Android label. Room is32. See [VALIDATION_1.16.25.md](VALIDATION_1.16.25.md) for tests, local artifacts, emulator preservation and exact scope. No push, release or production deployment.

### Previous 1.16.23 implementation

The local `1.16.23 / versionCode 388` round completes automatic sync continuation and missed-change detection, persistent protection for divergent legacy copies, recent registered-reminder recovery without historical replay, bundled Chinese UI fonts, named Windows group selectors, removal of duplicate account navigation, and Chinese Android launcher/task entry points. The interrupted session was resumed against repository state; current artifacts and original emulator files were rechecked before documentation and local commit. Verification: 190 Android units, 32 distinct device tests passing after targeted retries, 22 Windows regressions plus one 14-stage isolated UIA flow, LAN browser/font checks and Release signature. See [VALIDATION_1.16.23.md](VALIDATION_1.16.23.md) for failed retries, restoration and scope. All 531 original emulator files match backup; 26 additional runtime files are retained. No push, publication or production changes; OEM voice/reminder behavior and existing Windows parity limits remain.

### Previous 1.16.22 implementation

The local `1.16.22 / versionCode 387` round completes native Windows typography, branded dialogs/icons, layout and non-interfering UIA acceptance on an isolated desktop. Real rendering checks caught clipped date/time fields and inaccessible narrow Planning Desk actions; both were fixed and regression-guarded. Built new APKs and self-contained Windows output;19 Windows regressions plus1 workflow/14 stages,186 Android units/Lint/signature and existing-Pixel_8 upgrade/startup passed. See VALIDATION_1.16.22.md. No push/publication/production changes. Desktop parity and physical/OEM limits remain explicit.

### Previous 1.16.21 implementation

The local `1.16.21 / versionCode 386` round implements expired-session notifications and continued offline reminders/editing, optional encrypted credential memory, automatic network synchronization with safe conflict recovery, title-optional checklists including archive compatibility, neutral phone Settings and native WPF draft/layout/calendar improvements. Verification: 186 Android unit tests, 23 emulator tests, 18 Windows tests, 12 server tests, Lint/build/signature and LAN browser checks. See VALIDATION_1.16.21.md. The latest public Release remains `v1.15.18`; this round is neither pushed nor published.

### 2026-10-18 Actual-operation audit

The user challenged startup/log/component tests being presented as complete UI acceptance. Follow-up uses the user's existing Pixel_8, actual ADB input, visible production WPF OS pointer/keyboard and visible Edge against the real phone service. Added3 Android account/network/concurrency/conflict tests and1 visible12-stage WPF flow; original23 Android/18 Windows/186 unit/Lint/12 server regressions rerun. Test-only changes keep1.16.21/386 and existing artifacts. See INTERACTIVE_VALIDATION_1.16.21.md for precise coverage, failed retries, backup restoration and unverified boundaries. Full OEM/Windows parity is not claimed; no push/publication/production change.


## Initial Product Request

The original request was to build a local-first Android app that:

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

- `1.15.10` 收口待办批量选择的顶部/底部操作布局：选择状态移入应用栏，取消/完成固定在底部并保留滚动安全区；待办深色模式改为分层深蓝灰工作区；隔离同步轮询、看板时钟和看板专属派生计算，减少待办卡片嵌套绘制与重复计算；102 条待办模拟器快速滚动的卡顿帧从 13.00% 降至 10.93%。
- `1.15.11` 将手机端、电脑端和规划台的提醒渠道统一为全屏 / 通知栏独立勾选；清空两项会清理已有提醒闹钟、通知和全屏状态但保留日程进行中通知；电脑端整点标签与时间轴实线对齐，已完成日程仅降低透明度保留原色；`testDebugUnitTest`、`lintDebug`、`assembleDebug`、前端语法检查和差异检查均通过。
- `1.15.9` 将每日看板和待办页的待办长按批量选择、批量取消/完成及分组筛选管理入口落地，循环待办批量取消只作用于所选当前实例并写入历史。
- `1.15.8` 新增待办批量选择与统一完成，待办/日程编辑器改为仅“完成”提交、返回静默丢弃草稿；AI 配置改为页面级事务草稿，AI 测活固定单请求且 429 不重试，全部 AI/在线语录请求显式使用本机 Codex CLI UA；待办和设置页使用独立深蓝灰工作区并收紧 AI 配置层级。
- `1.15.7` 进一步提高浅色模式对比：页面底层改为可辨认的浅蓝灰，内容纯白、控件灰绿、辅助文字加深；日历拆成独立工具栏和内容面板并去掉重描边，工具按钮底色更明确，时区与初始时间轴位置收口；看板“明日安排”蓝色分区和数量层级同步加强。
- `1.15.6` 清理浅色模式大面积暖黄底色：全局背景改为冷白、卡片改为纸白、弱容器改为浅灰绿，日历时间轴不再呈现土黄色；每日看板日程卡拆成“今日剩余”和整宽浅蓝绿“明日安排”，分别显示数量，明日区域补齐日期、溢出数量及规划/日历入口。
- `1.15.5` 完成手机端 UI 分页审计并先重构抽屉导航：主要功能与回顾记录分组展示，AI 报告和历史记录不再藏在“更多”，设置与外观固定在底部，主题选择改为带说明的底部面板；保留山景、定制字体和既有配色，后续按待办、日历、规划台分轮收口。
- `1.15.4` 完成一次全功能审计并修复审计中确认的问题：日程详情操作栏不再在窄屏竖排，历史列表显示已完成/已取消且历史日程恢复完整时间地点，删除 AI 报告会清除对应通知，捕获通知补齐权限竞态保护，电脑同步 WakeLock 改为有超时且由 watchdog 续租；Android Lint 首次完整跑通并清除 5 个 error。
- `1.15.3` 修复待办 / 日程编辑器退出卡死：未保存确认改为独立对话框，关闭确认后会恢复原编辑器和输入，不再留下阻塞点击的不可见 ModalBottomSheet；同时修复点击日历“新日程”卡后临时占位未清理、保存后仍覆盖真实日程标题的问题。
- `1.14.18` 修复电脑同步地址误推荐：地址选择不再只信任 active Wi-Fi 链路返回的 IPv4，而是合并真实网卡候选并统一排序过滤；当 `172.19.0.1` 这类虚拟/桥接/VPN 低可信地址和 `wlan0 192.168.0.100` 同时存在时，会优先推荐可从电脑访问的 `192.168.*` 地址，同时保留可信热点/以太网 `10.*` / `172.*` 的合法场景。
- `1.14.17` 修复电脑同步启用闪退：Manifest 补充网络状态权限，电脑同步地址检测在 Wi-Fi/热点信息读取失败时退化为状态提示而不是崩溃，前台服务启动失败会回滚同步开关；同时清理本机 Codex resume 状态，只保留唯一 `【开发】PaykiTodo` 主入口，旧支线和临时恢复线程改为归档隐藏。
- `1.14.16` 修复进行中日程通知和电脑端交互：通知 RemoteViews 文字颜色改为代码层强制设置并减少系统装饰干扰；电脑端侧边栏拆出独立 `待办` 入口，进入后直接加载完整待办管理；日程时间轴支持 15 分钟粒度跨可见日期列拖拽，已结束日程淡化；日程颜色预设恢复为紧凑可见色块；循环待办卡片解释近未来实例生成窗口，避免误以为循环截止日期丢失。
- `1.14.15` 修复规划台自然日程提醒子句识别：`提醒时间：提前1h，06-24 22:10` 会转换为相对日程开始的多提醒偏移并从标题中剥离；AI 后处理也会兜底清洗模型返回在标题里的提醒文本；设置页新增 AI 源整体状态卡，电脑同步连接地址改为只展示当前 Wi-Fi 或疑似热点/以太网 LAN 地址，隐藏移动数据、VPN、虚拟网卡产生的普通 `10.*` / `172.*` 地址。
- `1.14.14` 递增安装版本元数据到 `versionCode 334`，用于承接 `1.14.13` 规划台/倒数日/全屏提醒修复后的可安装调试包；同时 AGENTS 明确要求未来用户可见修复或面向安装的 APK 构建必须先升级 `versionName` 和 `versionCode`，除非用户明确要求同版本元数据重建。
- `1.14.13` 修复规划台和提醒链路：规划台日期范围日程会识别为每日循环日程，`备注：...` 会进入日程备注并保留中文标点；AI 配置完整时优先走 AI 识别、本地规则作为兜底；过去/正在发生的日程可以保留默认提醒导入，已过去提醒不补发而未来循环提醒继续调度；每日看板倒数日多行标题时左侧倒计时和竖线自适配；全屏提醒只有 Activity 真正可见后才写入显示冷却，并升级提醒通知 channel 到 v10。
- `1.14.12` 收紧手机每日看板：倒数日标题完整换行、`今日日程` 去掉括号数量、明天区块取消独立折叠、今日空日程改为小号 `暂无日程`；同时让规划台在关闭提醒后可导入过去待办，桌面端取消 `启用提醒` 后会立即刷新并移除过期提醒提示。
- `1.13.68` 明确取消语义扩展到日程：手机和电脑端日程预览新增 `取消日程`，取消会进入历史记录且不同于硬删除；历史记录纳入已完成/已取消的日程；循环日程“仅删除当前”改为 skip + 硬删除，避免再用已取消 tombstone 伪装删除。
- `1.13.67` 统一手机日程详情预览的操作布局：顶部只保留返回，底部固定显示 `完成日程` / `修改` / `删除`；同时让 AI 日报只在存在正数日程签到分钟时写入投入时长，没有签到数据时不再输出或要求 AI 推断 `0 分钟投入`。
- `1.13.66` 将全屏待办提醒页收敛为 `我已完成` / `延后 10 分钟` 两个即时动作，移除取消、自定义延后和 DDL 推迟入口；无障碍兜底提醒层同步简化；手机待办详情预览把 `取消待办` 与 `修改`、`删除` 放到同一行并去掉常驻解释句。
- `1.13.65` 收敛待办预览取消入口：手机和电脑端待办详情预览只保留一个 `取消待办` 操作，循环待办取消前要求选择当前、当前及之后或全部循环任务范围，并修复桌面端循环日程预览删除在异步范围选择下可能拼出错误 API URL 的问题。
- `1.13.27` fixed an ongoing-event notification audit bug: acknowledging or signing into an event full-screen reminder now clears only the reminder artifacts and preserves/re-schedules the event's ongoing notification alarms, while ongoing event notification IDs now use the full alarm request-code hash instead of `eventId % 10000`.
- `1.13.26` unified phone-side todo preview entry points: search results, Planning Desk linked todos, notification routes, and widget/deep-link todo opens now land in the same detail preview that exposes cancel/archive, hard delete, edit, and restore where applicable. Planning Desk linked events continue routing to Calendar preview, while linked todos no longer jump straight into the editor.
- `1.13.25` updated the built-in Wiki and current handoff docs to match the implemented quick-preview behavior: todo card bodies open details, the left circle completes only, cancel is labeled as archive/history, hard delete does not enter history, and recurring-todo single-instance delete is documented as a non-history skip record.
- `1.13.24` corrected todo cancel/delete semantics in quick previews and desktop Web previews, routed recurring todo delete through hard-delete paths instead of cancel/archive, added the `recurring_instance_skips` table for single-instance recurring-todo deletion, and included those skip records in backup / restore.
- `1.13.21` fixed two correctness bugs: Planning Desk inline DDL dates such as `5.29 【DDL】...` / `5.29【DDL】...` now beat the note document date and clean DDL markers from the title, while recurring todo/event `整个循环系列` edits preserve a user-selected new start date and replace stale templates before rebuilding future instances.
- `1.13.19` re-audited recurring todo/event repository paths without finding a new data-layer recurrence bug at that time, kept the then-existing canceled-tombstone and recurrence-anchor split behavior, limited expanded future recurring todo series to the first 30 rendered instances, pre-resolved dashboard todo group data for large-list scrolling, and made the Calendar timeline compute timed-event placement only for current page days with minute-level current-time refresh. The todo single-instance delete tombstone behavior was superseded by `1.13.24`.
- `1.13.18` fixed the desktop Web preview delete/cancel recurrence-scope gap, added database 26 large-list indexes, reduced todo scrolling work with a lightweight completion toggle plus cheaper sectioning, cached Calendar timed-event placement by date, skipped timed layout outside day/three-day views, and carried the widget visual follow-up that removes countdown checkbox circles and tightens card spacing.
- `1.13.16` fixed the then-current recurring current-instance deletion path by keeping canceled tombstones, made recurring `当前及之后` range handling use original occurrence anchors, blocked current-only recurrence-rule edits, added phone recurring-todo delete scope selection, and further reduced todo / Calendar lag with lightweight todo rows plus vertical timed-event viewport culling. The todo single-instance delete tombstone behavior was superseded by `1.13.24`.
- `1.13.15` fixed a recurring Calendar template cleanup bug where changing an entire recurring event series to non-recurring could leave the old template behind, and reduced large-list / Calendar lag by moving heavy todo-state derivation off the main thread, removing intrinsic measurement from todo cards, and limiting timed-event overlap layout to visible Calendar page days.
- `1.13.14` fixed desktop sync lifecycle detection: desktop Web now sends authorized heartbeats, and the phone auto-disables the sync service plus notification after 5 minutes without a valid heartbeat.
- `1.13.13` fixed desktop Web todo multiline titles and recurring edit scope parity, including default current-and-future cleanup when a recurring todo is changed to non-recurring.
- `1.13.12` repaired the launcher icon resource chain by using the transparent main-logo resource for standard, round, drawer-header, and themed-icon paths, and removed obsolete unreferenced launcher-art PNG variants.
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
- 1.9.6 retuned the Android `今日看板` launcher widget again toward the in-app daily board: the topbar drops the `轻触打开` copy, todo cards show group tags / notes / `⏰ DDL`, heavy card strokes are removed, and in-progress schedule rows get a gold border with faint gold fill.
- 1.9.7 added a Planning Desk `公告` shortcut that inserts `#公告 ` on a new line, added a Settings -> AI 调用配置 -> AI 日报 / 周报 guide sheet for report usage, archived completed 1.9.0 / 1.9.1 goal prompts, and formalized emulator verification trace recording in the agent workflow docs.
- 1.9.8 moved AI 日报 / 周报 into an independent `AI 报告` archive backed by `ai_reports`, migrates legacy Planning Desk report notes, deep-links notifications to report detail, adds archive filters/detail/delete confirmation, includes reports in backup/restore, removes the Planning Desk auto-report hint, and changes `了解 AI 日报` into a centered readable dialog.
- 1.9.9 moved the desktop Web first tab toward the phone daily-board model with a current/next item card, focus stats, today/tomorrow schedule summaries, ended-event filtering, and in-progress gold highlighting; it also fixed desktop Planning Desk AI candidates importing 0 items and made AI group names explicit-only.
- 1.9.10 fixed Android widget section routing and reminder semantics: widget todo rows open My Tasks, widget schedule rows open Calendar, the widget no longer shows the focus card, todos can choose full-screen vs notification reminder mode on phone and desktop Web, and reminder snooze no longer changes DDL unless the explicit DDL postpone action is used.
- 1.9.11 made active no-DDL todos part of 今日待办 across phone board, widget snapshot, desktop board, and desktop todo management; Planning Desk plain bullets now import as no-DDL todos; desktop sync auto-disables after 5 minutes without an authorized desktop connection; Settings -> AI 调用配置 provider rows use summary cards and a more menu to reduce narrow-screen crowding.
- 1.9.12 reviewed the 1.9.11 behavior and fixed the remaining no-DDL widget data gap by including no-DDL tasks in the widget board-range query; it also added `todo_items` performance indices, removed a duplicate group read from desktop `/api/snapshot`, made Planning Desk plain-bullet no-DDL candidates explain themselves in preview, and made Settings -> AI 调用配置 provider edits safer to persist.
- 1.9.13 continued the performance pass by making desktop Web first load use a lightweight board snapshot, deferring full todos/events until timeline or manual load, moving phone focus stats to a today aggregate Flow, collecting AI reports only inside the AI report section, reusing one calendar event-date index across month/list/all-day surfaces, and adding a no-DDL cross-day board test.
- 1.9.14 continued the phone-side performance pass by replacing ordinary board/task full-todo observation with active-todo-only observation, limiting the daily board to today/tomorrow event range data, collecting history/templates/diagnostics only inside their owning pages, moving Planning Desk local parsing to a background dispatcher, and changing the AI report archive to paged Room queries with deep-link-by-ID support.
- 1.9.15 continued the performance pass by removing full Planning Desk note observation from ordinary board/task state, collecting complete planning notes only in the Planning Desk section, and making phone board / Android widget / desktop lightweight snapshot announcements read only announcement-candidate planning notes before strict parsing.
- 1.9.16 persisted Planning Desk announcement hints in `planning_notes.hasAnnouncementHint`, upgraded the database to version 15 with `MIGRATION_14_15`, backfilled existing planning notes, indexed the announcement lookup path, and changed phone board / Android widget / desktop lightweight snapshot announcement queries to avoid Markdown-body `LIKE` scans.
- 1.9.17 split the remaining desktop Web management snapshot hot path: initial browser connection still loads the lightweight daily board, complete todo management loads through `/api/todos`, the calendar timeline loads only the visible `/api/events?start=...&end=...` range, and todo/event/planning mutations refresh only the current page's needed data instead of reloading one complete snapshot.
- 1.9.18 finished the next desktop/widget performance cleanup: desktop todo management now pages and searches `/api/todos?offset=...&limit=...&q=...` with loaded/total UI and load-more controls, while the Android launcher widget removed the remaining focus-card runtime branch, unused focus-card resources, and picker-preview focus block.
- 1.9.19 continued the performance and stability pass: desktop sync now uses bounded client threads and byte-length UTF-8 request-body reading, AI 报告 adds keyword/type/range Room-backed filtering, calendar timeline dates use a lightweight date window, and database version 16 adds desktop todo paging plus AI-report filtering indices.
- 1.9.20 continued the user-experience/performance audit: no-DDL active todo sectioning is centralized and covered by tests so My Tasks and the board keep no-DDL items under 今日待办 every day, and the phone Calendar page now observes only the current visible event date range while preserving far-event deep-link focusing.
- 1.9.21 fixed a UI consistency issue found during emulator review: the phone Calendar page top bar now shows the Chinese title 日历 instead of the leftover English Schedule label, with version/docs synchronized to the 1.9.21 debug APK.
- 1.9.22 fixed the Android launcher widget stale-date/stale-list issue seen on a physical phone: widget updates now refresh the RemoteViews collection as well as the static header, date/time/timezone/app-replacement broadcasts refresh all widget instances, the adapter cache key includes the current date, and light-mode widget surfaces/text colors were strengthened for readability.
- 1.9.22 release-preparation continuation archived old versioned docs under `docs/archive/historical/`, moved safe signing explanations to `docs/templates/`, added a tracked `keystore.properties.example`, routed real signing values to ignored root-level `keystore.properties` plus ignored `release/`, fixed the ignore rule so the app's built-in `res/raw/remind_vocal.mp3` can be tracked, and made Gradle release tasks fail clearly when signing is incomplete.
- 1.9.23 fixed two release-blocking user issues before generating the first release APK: widget event locations no longer get an extra display `@`, and enabling desktop sync immediately starts / self-starts the LAN server so Settings can expose connection addresses without waiting for a desktop connection.
- 1.10.0 adds countdown-day mode for important DDL / schedule targets: DDL-backed todos and calendar events can be marked as countdown targets, phone and desktop daily boards show active countdown rows, and an independent 倒数日 widget shows the nearest three targets.
- 1.10.1 fixes the first countdown/widget and desktop Planning Desk feedback: the existing 今日看板 widget no longer embeds countdown rows, the independent 倒数日 widget uses `Nd` plus remaining time and hides completion circles for events, App/desktop countdown rows show full event time metadata, and Planning Desk AI preview/import preserves location, all-day, countdown, and recurrence fields instead of dropping them or putting locations into notes.
- 1.10.2 fixes follow-up daily-board/widget feedback: countdown rows use day/hour/minute text without seconds, exact-time expired countdown targets disappear, visible today-event counts ignore ended events, the independent 倒数日 widget removes its redundant header and deep-links to exact items, widget picker entries gain labels/descriptions/static previews, and focus moves from the daily board into a dedicated Focus page plus Focus widget.
- 1.10.3 fixes Planning Desk event import semantics: event candidates no longer auto-create linked todos, linked todos no longer receive a fixed generated note, local and AI recognition move `@地点` / quoted `@地点` into the event location field, and desktop-web event editing gains phone-matching preset color swatches.
- 1.12.12 increments the debug build metadata over the completed 1.12.11 Planning Desk Outliner baseline so Android devices that already installed 1.12.11 can install the next debug APK through the normal upgrade path; no database schema or product behavior changes in this rebuild.
- 1.12.13 improves Planning Desk Outliner keyboard behavior on phone and desktop Web: empty input Backspace jumps to the previous node, row-start Backspace merges into the previous same-level node, middle Enter splits a node into a new sibling, desktop ArrowUp/ArrowDown move focus across rows, and active input rows are visually borderless.
- 1.12.14 increments the debug build metadata over the completed 1.12.13 Planning Desk Outliner baseline so Android devices that already installed 1.12.13 can install the next debug APK through the normal upgrade path; no database schema or product behavior changes in this rebuild.
- 1.12.15 packages the UI recovery and Planning Desk draft/publish separation: calendar month title and actions share one row, todo group creation is fixed beside 全部, Planning Desk restores visible document and image-recognition entries, new/captured Outliner nodes stay draft until explicit single/publish-all actions, desktop Web supports the same draft publishing routes, and database version 23 adds `planning_nodes.isDraft`.
- 1.12.16 increments the debug build metadata over the completed 1.12.15 UI recovery and Planning Desk draft/publish baseline so Android devices that already installed 1.12.15 can install the next debug APK through the normal upgrade path; no database schema or product behavior changes in this rebuild.
- 1.14.21 upgrades the prompt engineering behind AI 日报/周报 and the planning recognizer without touching recurrence semantics or the database schema (still version 28): the report system/data prompts move from "short" to "comprehensive, specific, covering every supplied data dimension" with plain-text Chinese sub-sections, completion-rate / weekday / DDL / note-summary / event-location context, and analysis-then-advice structure; the planning recognizer injects today's weekday plus today/tomorrow/day-after absolute-date anchors and forces relative-date words to be resolved into absolute dates instead of being left verbatim in titles.
- 1.15.0 adds the 实际记录层 (activity timer / actual-record layer) alongside todos and schedules to answer "what did I actually do today": a new `activity_records` table (database version 28 → 29 via `MIGRATION_28_29`, entity `ActivityRecord` with RUNNING/PAUSED/FINISHED states and TIMER/MANUAL sources), a stopwatch repository state machine (single active record, pause/resume accumulation, <5s discard on finish, manual back-fill), an `ActivityTimerService` foreground service with a chronometer notification plus pause/resume/finish actions routed through `ActivityTimerReceiver`, ViewModel `activeActivityRecord` / `todayActivityRecords` flows and start/pause/resume/finish/manual/delete actions, a dashboard timer bar + 今日做了什么 card + start-record FAB with start/manual/detail dialogs, AI 日报/周报 fed the day's/week's actual records, and backup/restore extended to carry finished activity records.
- 1.15.1 broadens Desktop Sync address discovery to campus-network CGNAT/public IPv4 without exposing mobile/VPN addresses, makes the four-character Web key case-insensitive, fixes live `SYSTEM` light/dark following, replaces the app's old landscape background and oversized card hierarchy with a compact neutral phone visual system, and rebuilds the desktop login/workbench around low-radius neutral light/dark surfaces.
- 1.15.2 restores the user-selected light/dark daily-board landscape backgrounds, Forest/Clay/Ocean palette, `payki_greeting` font, title shadow, and greeting gradient while keeping the live system-theme fix; it removes the activity-timer UI, foreground service, notification actions, ViewModel/repository runtime state, and AI-report activity inputs, while retaining database version 29 plus the activity-record schema/migration/backup fields solely for safe upgrade and historical-backup compatibility.
