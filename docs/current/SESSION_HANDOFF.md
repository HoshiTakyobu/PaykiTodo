# Session Handoff

## Current State

- Repository root: `G:\Workspace\Project\PaykiTodo`
- Branch: `main`
- Current code version:
  - `versionName = 1.13.44`
  - `versionCode = 292`
  - database version = `27`
- Latest debug APK target in this round:
  - `app/build/outputs/apk/debug/PaykiTodo-1.13.44-debug.apk`
- Latest signed release APK available locally:
  - `app/build/outputs/apk/release/PaykiTodo-1.13.11-release.apk`
- Latest GitHub Release:
  - `https://github.com/HoshiTakyobu/PaykiTodo/releases/tag/v1.13.11`
- Debug APK metadata inspection:
  - `versionName = 1.13.44`, `versionCode = 292`

## Active Goal

Active immediate task: continue the broader product/UX audit from the current `1.13.44 / versionCode 292` local patch baseline.

Latest status: `1.13.44` improves Desktop Web recurrence editing by replacing manual weekly-day CSV input with compact weekday multi-select chips for todo and event editors. Debug build and metadata inspection should be checked before handoff.

## What Changed In The Latest 1.13.44 Patch

1. Desktop Web todo weekly recurrence now uses visible weekday chips instead of manual `1,3,5` text input.
2. Desktop Web event weekly recurrence uses the same weekday chips, matching the phone-side selectable weekday behavior more closely.
3. Weekday chips are shown only when the recurrence type is `WEEKLY`; other recurrence types clear and hide weekly-day state.
4. The change reuses existing recurrence payload fields and does not require a database migration.
5. Version metadata moved to `1.13.44 / versionCode 292`; database version remains `27`.
6. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `git diff --check`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection confirmed `versionName = 1.13.44`, `versionCode = 292`.

## What Changed In The Latest 1.13.43 Patch

1. Phone todo quick-preview bottom fixed actions now include a visible `取消归档` button beside edit/delete.
2. The action still routes through cancel/archive semantics and sends the todo into history; hard delete remains a separate destructive path.
3. The change is UI-only and does not require a database migration.
4. Version metadata moved to `1.13.43 / versionCode 291`; database version remains `27`.
5. Verification passed: `git diff --check`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection confirmed `versionName = 1.13.43`, `versionCode = 291`.

## What Changed In The Latest 1.13.42 Patch

1. Desktop Web todo editor now has a visible `启用提醒` switch.
2. Desktop Web event editor now has a visible `启用提醒` switch.
3. Disabling reminders greys out reminder time, delivery mode, and ring/vibration fields and saves an empty reminder-offset list.
4. Desktop todo creation defaults to a 5-minute reminder; desktop event creation defaults to no reminder, matching the phone-side defaults.
5. The change reuses existing desktop sync payload fields and does not require a database migration.
6. Version metadata moved to `1.13.42 / versionCode 290`; database version remains `27`.
7. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection confirmed `versionName = 1.13.42`, `versionCode = 290`.

## What Changed In The Latest 1.13.41 Patch

1. Desktop Web event editor title input changed from a single-line input to a multi-line textarea.
2. Desktop-created or desktop-edited event titles can now contain line breaks like phone-side event titles.
3. The change reuses the existing `title` save path and does not require a database migration.
4. Version metadata moved to `1.13.41 / versionCode 289`; database version remains `27`.
5. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection confirmed `versionName = 1.13.41`, `versionCode = 289`.

## What Changed In The Latest 1.13.40 Patch

1. Phone todo quick-preview sheets now show a prominent `取消并归档` action card directly below the todo title.
2. Phone todo quick-preview top action now says `取消并归档`, avoiding confusion with a generic close/cancel action.
3. Desktop Web todo preview now shows an inline archive action card and uses `取消并归档` for the top and bottom cancel/archive actions.
4. Cancel still archives into history; delete remains a separate hard-delete path.
5. Version metadata moved to `1.13.40 / versionCode 288`; database version remains `27`.
6. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection confirmed `versionName = 1.13.40`, `versionCode = 288`.

## What Changed In The Latest 1.13.39 Patch

1. Desktop Web todo editor now exposes the `闹钟模式` checkbox.
2. Desktop Web todo editor now exposes the `仅提醒，不在看板显示` checkbox.
3. Both fields are reset when DDL is disabled, because they depend on DDL/reminder behavior.
4. Editing existing todos round-trips `alarmMode` and `hiddenFromBoard` through the existing desktop sync API.
5. Version metadata moved to `1.13.39 / versionCode 287`; database version remains `27`.
6. Verification target: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.38 Patch

1. Active todo card long-press action sheet now labels cancel as `取消待办（归档）`.
2. Active todo cancel confirmation sheet title now says `取消待办（归档）`.
3. Todo details preview cancel confirmation uses `取消并归档` as the confirm action.
4. Cancel still archives into history; delete remains a separate hard-delete action.
5. Version metadata moved to `1.13.38 / versionCode 286`; database version remains `27`.
6. Verification target: `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.37 Patch

1. Todo reminder full-screen UI now labels cancel as `取消待办（归档）`.
2. The reminder cancel button now uses archive-orange styling instead of delete-red styling.
3. Accessibility fallback reminder overlay uses the same `取消待办（归档）` wording.
4. Accessibility fallback cancel toast now says the todo was canceled and archived.
5. Version metadata moved to `1.13.37 / versionCode 285`; database version remains `27`.
6. Verification target: `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.36 Patch

1. Phone todo details quick preview now exposes a top-bar `取消待办` action again, while the fixed bottom action area keeps the fuller `取消待办（归档）` explanation.
2. The top action and bottom archive action both go through the same confirmation flow, preserving the distinction between cancel/archive and hard delete.
3. Weekly recurring todo editors auto-sync default weekday selection to the DDL date when the user has not manually edited weekday chips.
4. Manual weekday chip edits are preserved, so explicit recurrence rules are not overwritten by later DDL date changes.
5. Version metadata moved to `1.13.36 / versionCode 284`; database version remains `27`.
6. Verification target: `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.35 Patch

1. DesktopSyncStatus now exposes `connected`, `lastAuthorizedAtMillis`, and `secondsUntilAutoStop`.
2. Phone Settings -> Desktop Sync now shows whether it is waiting for the access key or already connected to an authorized desktop.
3. TodoViewModel refreshes desktop sync status every 15 seconds while sync is enabled, so the Settings page can update connection state and auto-stop countdown.
4. `/api/status` remains compatible with the desktop web and adds the new status fields for future UI use.
5. Version metadata moved to `1.13.35 / versionCode 283`; database version remains `27`.
6. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.34 Patch

1. Phone todo details quick preview now keeps `取消待办（归档）` in a fixed bottom action area.
2. Preview content can scroll independently while cancel/archive, delete, edit, and restore remain visible at the bottom.
3. The preview top bar now only returns to the previous screen, reducing action clutter.
4. Cancel/archive copy still distinguishes history-preserving cancel from hard delete.
5. Version metadata moved to `1.13.34 / versionCode 282`; database version remains `27`.
6. Verification passed: `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.33 Patch

1. OngoingEventReceiver no longer clears an ongoing notification blindly when an old end alarm fires.
2. The end path now reloads the latest event state and re-schedules / re-posts the ongoing notification if the event was extended or moved and is still ongoing.
3. The notification is cleared only when the latest event is gone, no longer an event, completed/canceled, or already ended.
4. Unit coverage was added for extended events, ended events, history events, and non-event rows.
5. Version metadata moved to `1.13.33 / versionCode 281`; database version remains `27`.
6. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.32 Patch

1. ReminderForegroundService now uses a dedicated reminder delivery policy instead of forcing every todo/event into the full-screen chain.
2. Items set to `通知栏提醒` no longer actively launch the full-screen reminder surface unless `工作模式` or `闹钟模式` overrides the item setting.
3. Items set to `全屏界面提醒` continue to request full-screen delivery.
4. Unit coverage was added for notification mode, fullscreen mode, work-mode override, and alarm-mode override.
5. Version metadata moved to `1.13.32 / versionCode 280`; database version remains `27`.
6. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.31 Patch

1. Phone todo details quick preview now presents `取消待办（归档）` as an independent archive action card instead of one item in a crowded action row.
2. The archive action explains that cancel stops reminders and enters history; hard delete stays visually separate and does not enter history.
3. Desktop Web todo preview now uses a dedicated archive-action style and explanatory copy for `取消待办（归档）`.
4. Version metadata moved to `1.13.31 / versionCode 279`; database version remains `27`.
5. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.30 Patch

1. Phone Planning Desk recognition preview batch setting `全部加入倒数日` now includes selected event candidates.
2. The same batch setting still applies to DDL-backed todo candidates and remains disabled for no-DDL todos.
3. Version metadata moved to `1.13.30 / versionCode 278`; database version remains `27`.
4. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.29 Patch

1. Desktop Web Planning Desk event recognition preview now shows a `打卡追踪` checkbox beside `全天` and `倒数日`.
2. The checkbox is collected through the existing `data-planning-flag` path and sent as `checkInEnabled`, matching the phone Planning Desk preview and existing backend import support.
3. Version metadata moved to `1.13.29 / versionCode 277`; database version remains `27`.
4. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.28 Patch

1. Phone todo details quick preview now labels the top archive action as `取消待办` instead of `取消归档`.
2. Shared quick-preview entry points hide cancel/delete only for completed or canceled history todos, not for active todos opened from search, Planning Desk, notifications, widgets, or board links.
3. Desktop Web todo preview now exposes a top `取消待办` action in addition to the bottom action row.
4. Version metadata moved to `1.13.28 / versionCode 276`; database version remains `27`.
5. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection. The first concurrent unit-test attempt hit a corrupted local KSP cache; after stopping Gradle daemons and removing generated `app/build/kspCaches`, the sequential test run passed.

## What Changed In The Latest 1.13.27 Patch

1. Full-screen event reminders now clear only reminder artifacts when the user taps `我知道了`; they preserve or reschedule the ongoing-event notification path.
2. Accessibility fallback event reminders now use the same keep-ongoing behavior for `我知道了` and `签到`.
3. Ongoing event notification IDs now use the full item-ID hash request code instead of `eventId % 10000`, reducing notification collisions across many events.
4. The current feature ledger now matches the implemented snooze policy: overdue/conflicting DDLs are pushed one minute after the next reminder.
5. Version metadata moved to `1.13.27 / versionCode 275`; database version remains `27`.
6. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.26 Patch

1. Phone search results, Planning Desk linked todos, notification routes, and widget/deep-link todo opens now share the same todo details preview instead of jumping straight to the editor or depending on current list membership.
2. The shared todo details preview exposes cancel/archive, hard delete, edit, and restore where applicable.
3. The top preview action was changed in `1.13.28` to say `取消待办`; this older patch first introduced a visible top archive action.
4. Planning Desk linked events still route to Calendar preview, while linked todos open todo preview first.
5. Version metadata moved to `1.13.26 / versionCode 274`; database version remains `27`.
6. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.25 Patch

1. Built-in Wiki todo-preview instructions now say the card body opens details and the left circle only completes the todo.
2. Built-in Wiki now documents `取消待办（归档）` versus hard delete.
3. Built-in Wiki now documents recurring-todo single-instance delete as a non-history skip record.
4. Version metadata moved to `1.13.25 / versionCode 273`; database version remains `27`.
5. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.24 Patch

1. Phone todo details preview labels cancel as `取消待办（归档）`, adds confirmation, and exposes delete separately with the explicit warning that deletion does not enter history.
2. Desktop Web todo preview labels cancel as `取消待办（归档）` and confirms both cancel/archive and hard delete.
3. Desktop sync delete API now routes todo deletion to `TodoRepository.deleteTodo(item, scope)` instead of canceling recurring range deletes.
4. Recurring todo current-instance delete adds a `recurring_instance_skips` row before hard-deleting the item, so the occurrence does not enter history and is not regenerated.
5. Backup / restore includes `recurring_instance_skips`, so single-instance recurring-todo deletions survive restore.
6. Database version moved to `27`; schema `app/schemas/com.example.todoalarm.data.AppDatabase/27.json` includes `recurring_instance_skips`.
7. Version metadata moved to `1.13.24 / versionCode 272`.
8. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `git diff --check`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection.

## What Changed In The Latest 1.13.22 Patch

1. Quick todo preview dialogs now expose a visible `取消待办` action so the user can archive a task from the preview surface without using hard delete.
2. Active todo cards' quick action sheet now also exposes `取消待办` directly, keeping cancel visually distinct from delete.
3. Cancel remains a history-preserving archive action; delete remains the destructive removal path.
4. Version metadata should be rebuilt to `1.13.23 / versionCode 271`; latest debug APK target should be `app/build/outputs/apk/debug/PaykiTodo-1.13.23-debug.apk` after rebuild.

## What Changed In The Latest 1.13.21 Patch

1. Desktop Web Planning Desk “大纲事项” is now the explicit primary input area instead of a passive list beside Markdown compatibility editing.
2. Empty Outliner state focuses the root input; the root input is a visible multi-line note box with examples and `Enter` / `Shift+Enter` guidance.
3. Existing Outliner nodes render as directly editable auto-height textareas, so clicking node text allows normal typing.
4. Drag reorder starts only from a dedicated drag handle, reducing input focus conflicts from row-level draggable cards.
5. Multi-line paste into the root Outliner input can create multiple draft nodes.
6. Pending Outliner edits flush before publish-all, parse, import, refresh, postpone, undo, document switching, and Markdown sync.
7. Version metadata moved to `1.13.20 / versionCode 268`; latest debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.13.20-debug.apk`.
8. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `./gradlew.bat :app:assembleDebug`, APK metadata inspection confirmed `versionName = 1.13.20`, `versionCode = 268`, and `git diff --check` passed.

## What Changed In The Latest 1.13.19 Patch

1. Future recurring todos in `计划中` keep the folded series-card model; expanded long series now render only the first 30 future instances and show a notice for the rest so one recurring series cannot flood the scrolling list.
2. Dashboard todo cards now reuse pre-resolved task-group data from a per-state group map, reducing repeated group-list scans while scrolling many todos.
3. Calendar day / three-day timeline now builds timed-event placements only for the currently visible page days instead of pre-layouting the whole loaded event window.
4. Calendar current-time axis / line refreshes once per minute, and timed-board vertical overscan is reduced from 2 hours to 1 hour.
5. Historical note: recurring todo/event repository paths were re-audited at that time; todo current-instance delete later moved to the `1.13.24` non-history skip-record design, while recurring event current-instance deletion still uses the tombstone approach.
6. Version metadata moved to `1.13.19 / versionCode 267`; latest debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.13.19-debug.apk`.
7. Verification passed: `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `./gradlew.bat :app:assembleDebug`, APK metadata inspection confirmed `versionName = 1.13.19`, `versionCode = 267`, and `git diff --check` passed before final handoff-doc edits.

## What Changed In The Latest 1.13.18 Patch

1. Desktop Web preview delete/cancel actions now send `CURRENT_AND_FUTURE` for recurring todos/events so preview actions cannot leave future recurring instances behind because of a missing scope query.
2. Database version moved to `26`; `MIGRATION_25_26` adds indexes for active todo sorting, history todo sorting, and calendar range queries.
3. Calendar day / three-day timeline precomputes timed-event placements by date, caches segment time values, and skips timed-event layout entirely in month/list views.
4. Todo list rows use a lightweight Canvas completion toggle; ordinary text bypasses strike-through layout/draw callbacks until the completion animation runs.
5. Todo sectioning compares against local-day millisecond boundaries instead of converting every item to `LocalDate`, with a unit test covering 00:00 / 23:59 / next-day boundaries.
6. Widget visual follow-up removes countdown checkbox circles, tightens countdown/event card spacing, and aligns greeting-card backgrounds with the newer widget card surface.
7. Version metadata moved to `1.13.18 / versionCode 266`; latest debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.13.18-debug.apk`.
8. Verification passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, sequential `./gradlew.bat :app:testDebugUnitTest`, `./gradlew.bat :app:assembleDebug`, `git diff --check`, and APK metadata inspection confirmed `versionName = 1.13.18`, `versionCode = 266`.

## Current Documentation / Repository Standards

The public repository documentation remains standardized:

1. `README.md` should stay concise and public-facing: app purpose, latest release, install path, feature overview, build commands, privacy/open-source/contribution links.
2. `CHANGELOG.md` should stay structured by version / milestone and should not return to a huge internal session journal.
3. Root-level public governance files now exist or are being added: `LICENSE`, `NOTICE.md`, `PRIVACY.md`, `SECURITY.md`, `CONTRIBUTING.md`.
4. Keep secret files out of git. Confirm `git check-ignore -v keystore.properties release/PaykiTodo-release.jks` before the final commit if release/signing docs are touched.
5. Public docs must distinguish the current source/debug version from the latest published GitHub Release when they differ.
6. `.github` issue templates, PR template, Android CI, `SUPPORT.md`, `CODE_OF_CONDUCT.md`, and README badges should stay usable.
7. Internal bootstrap/backlog/goal material belongs under `docs/current/`, `docs/goals/`, or `docs/archive/`, not loose in the repository root.

## What Changed In The Latest 1.13.16 Patch

1. Recurring todo/event current-instance delete now keeps a canceled tombstone instead of hard-deleting the row, preventing recurring-template replenishment from recreating the same occurrence.
2. Recurring range edit/cancel/delete and template truncation use the original recurrence anchor date where available, so moving a single occurrence no longer corrupts `当前及之后` selection.
3. Editing only the current recurring todo/event preserves the original series recurrence fields, and phone/desktop validation rejects recurrence-rule changes under `仅当前`.
4. Phone recurring-todo delete now shows the recurrence scope selector instead of silently affecting only one row.
5. Calendar day / three-day timeline renders only timed events intersecting the vertical viewport plus overscan, and buckets scroll recomputation by half-hour.
6. Todo cards use lightweight no-shadow `Surface` rows and cache each card's resolved group, reducing large-list drawing and repeated group lookup.
7. Version metadata moved to `1.13.16 / versionCode 264`; latest debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.13.16-debug.apk`.
8. Verification passed: `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:testDebugUnitTest`, `./gradlew.bat :app:assembleDebug`, `git diff --check`, and APK metadata inspection confirmed `versionName = 1.13.16`, `versionCode = 264`.

## What Changed In The Latest 1.13.15 Patch

1. Recurring Calendar series edits with scope `ALL` now delete the old recurring template when the edited series becomes non-recurring, preventing future replenishment from recreating removed future events.
2. Todo / daily-board UI state moves large-list sectioning, countdown sorting, and announcement parsing to `Dispatchers.Default` instead of doing that work on the main composition path.
3. Desktop sync status is collected through a separate state flow, so ordinary todo/event list changes no longer re-run LAN status and IP-address computation.
4. Active todo cards draw the left color strip directly and no longer use `IntrinsicSize.Min`, reducing LazyColumn measurement overhead when many items are visible.
5. Calendar day / three-day timed-event placement is computed only for the currently visible page days instead of the whole loaded event window.
6. Version metadata moved to `1.13.15 / versionCode 263`; latest debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.13.15-debug.apk`.
7. Verification passed: `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:assembleDebug`, and APK metadata inspection confirmed `versionName = 1.13.15`, `versionCode = 263`.

## What Changed In The Latest 1.13.14 Patch

1. Desktop sync foreground service now continuously monitors authorized desktop-client heartbeats instead of using only a one-shot startup timer.
2. If no desktop enters the correct access token within 5 minutes, the phone writes `desktopSyncEnabled = false`, stops the LAN server, stops the foreground service, and removes the notification.
3. If a previously connected browser tab closes, the computer sleeps, or the LAN disconnects for 5 minutes, the same auto-stop path runs.
4. Desktop Web sends an authorized `/api/status` heartbeat every 60 seconds after a successful connection.
5. Desktop sync status reads no longer start a bare HTTP server without the foreground service; when the setting is enabled and the server is absent, the foreground service is started first.
6. Version metadata moved to `1.13.14 / versionCode 262`; latest debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.13.14-debug.apk`.

## What Changed In The Latest 1.13.13 Patch

1. Desktop Web todo title editing now uses a multiline textarea instead of a single-line input, so phone-created newline titles can be edited and new newline titles can be entered from the browser.
2. Desktop todo/event list cards and detail previews now preserve title line breaks instead of collapsing them into one line.
3. Desktop Web recurring todo/event editors now expose `修改范围` with `仅当前` / `当前及之后` / `整个循环系列`.
4. Turning a recurring todo into a non-recurring todo from desktop defaults to `当前及之后`, preventing future generated instances from continuing to appear after save.
5. Desktop sync update routes accept recurrence scope and clear reminder artifacts for the full affected item set before rescheduling updates.
6. Desktop sync cancel/delete item routes accept scope query parameters; recurring todo delete with non-current scope uses repository cancellation/truncation to remove active future instances.
7. Version metadata moved to `1.13.13 / versionCode 261`; latest debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.13.13-debug.apk`.
8. Verification: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:compileDebugKotlin`, `./gradlew.bat :app:assembleDebug`, APK metadata inspection, and `git diff --check` passed.

## What Changed In The Latest 1.13.11 Patch

1. 今日看板 Widget runtime and picker preview outer content padding increased so section titles and cards no longer sit too close to the widget shell's left and right edges.
2. 倒数日 Widget runtime and picker preview outer content padding increased; countdown row internal start/end padding and accent-strip spacing were widened.
3. Database version remains `25`; no schema or user-data format change was introduced.
4. Verification passed: `git diff --check`, `./gradlew.bat :app:assembleDebug`, `./gradlew.bat :app:assembleRelease`; debug/release APK metadata confirms `1.13.11 / 259`, and release signature verification reports `Verifies`.
5. GitHub Release `v1.13.11` was published with `PaykiTodo-1.13.11-release.apk`.

## What Changed In The Latest 1.13.10 Patch

1. Version metadata moved to `1.13.10 / versionCode 258` so Android can upgrade over an installed `1.13.9` debug build.
2. This patch carries the same Widget follow-up and Calendar Pager regression-guard behavior from the current work line.
3. Database version remains `25`; no schema or user-data format change was introduced.
4. Verification passed: `git diff --check`, `./gradlew.bat :app:assembleDebug`; APK metadata confirms `1.13.10 / 258`.

## What Changed In The Latest 1.13.9 Patch

1. Version metadata moved to `1.13.9 / versionCode 257`.
2. Widget todo/event accent strips use a pure-rectangle strip drawable, with parent card/row `clipToOutline` handling rounded clipping to reduce strip-card seams.
3. Widget schedule aggregate card backgrounds now match todo soft-card backgrounds in light and dark modes.
4. Dark mode has a stronger overdue badge background; loading rows display `⏳ 加载中…`.
5. Countdown widget runtime and picker preview outer padding returned to `4dp`.
6. Calendar event long-press drag disables Pager user scrolling while dragging; three-day paging intentionally remains one day per page.
7. Database version remains `25`; no schema or user-data format change was introduced.
8. Verification so far: `./gradlew.bat :app:compileDebugKotlin` passed. Final assemble, metadata, and emulator checks pending.

## What Changed In The Latest 1.13.8 Patch

1. Version metadata moved to `1.13.8 / versionCode 256`.
2. Android 今日看板 / 倒数日 Widget runtime layouts and static previews no longer include the app wallpaper ImageView layer; both use fixed light/dark widget backgrounds and very light scrims.
3. 今日看板 Widget todo rows remove the non-functional checkbox circle, use tighter integrated card/strip spacing, and show overdue todos as `已逾期` instead of `!`.
4. Widget list/card padding and schedule row spacing are tighter; board/countdown factories return a custom `加载中…` loading RemoteViews instead of the system loading spinner.
5. Calendar day/three-day timeline paging now uses Compose `HorizontalPager`; dashboard todo LazyColumn rows add `contentType` while keeping stable item keys.
6. Database version remains `25`; no schema or user-data format change was introduced.
7. Verification passed: `./gradlew.bat :app:compileDebugKotlin`, `git diff --check`, `./gradlew.bat :app:assembleDebug`; APK metadata confirms `1.13.8 / 256`.
8. Emulator smoke test: `Pixel_8 / emulator-5554` installed `PaykiTodo-1.13.8-debug.apk`, launched `MainActivity` with `EXTRA_OPEN_CALENDAR=true`, UI dump confirmed `日历` / `三日视图`, and logcat showed no AndroidRuntime fatal crash.

## What Changed In The Latest 1.13.7 Patch

1. Version metadata moved to `1.13.7 / versionCode 255`.
2. This rebuild exists so Android can install over an already-installed `1.13.6` debug build.
3. No additional database schema, reminder behavior, Planning Desk behavior, Widget behavior, or user-data format change was intentionally introduced by this version bump.
4. Latest debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.13.7-debug.apk`.
5. Verification passed: `git diff --check`, `./gradlew.bat :app:assembleDebug`; APK metadata confirms `1.13.7 / 255`.

## What Changed In The Latest 1.13.6 Patch

1. Version metadata moved to `1.13.6 / versionCode 254`.
2. Phone Planning Desk Outliner now has same-parent long-press drag reorder with lift/alpha feedback, placement animation, preview-mode drag blocking, and snapshot-based undo for edit/delete/merge/reorder/publish.
3. My Tasks folds future recurring todo instances into one series card, while today/missed todos stay fully visible.
4. ReminderActivity uses a flatter large-time/large-title layout, larger 56dp action buttons, collapsible notes, entry animation, and alarm-mode pulse.
5. Alarm mode is persisted on todos/templates, backed by database version `25`, backup/restore and desktop sync; alert playback loops until action, then downgrades after 5 minutes to an explicit unhandled-reminder notification and 3 short retry bursts.
6. Daily brief, global search, data health cleanup, and ongoing event notifications are implemented; notification channels now use goal-matching ids `daily_brief` and `ongoing_event`.
7. Latest debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.13.6-debug.apk`.
8. Verification passed: `./gradlew.bat :app:compileDebugKotlin`, `git diff --check`, `./gradlew.bat :app:assembleDebug`; APK metadata confirms `1.13.6 / 254`.

## What Changed In The Latest 1.13.5 Patch

1. Version metadata moved to `1.13.5 / versionCode 253`.
2. This rebuild exists so Android can install over an already-installed `1.13.4` debug build.
3. No additional database schema, reminder behavior, Planning Desk behavior, or user-data format change was intentionally introduced by this version bump.
4. Latest debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.13.5-debug.apk`.
5. Verification passed: `git diff --check`, `./gradlew.bat :app:assembleDebug`; APK metadata confirms `1.13.5 / 253`.

## What Changed In The Latest 1.13.4 Patch

1. Version metadata moved to `1.13.4 / versionCode 252`.
2. This rebuild exists so Android can install over an already-installed `1.13.3` debug build.
3. No additional database schema, reminder behavior, Planning Desk behavior, or user-data format change was intentionally introduced by this version bump.
4. `UpcomingTodoDisplayGroup` was made public because public `TodoUiState` exposes it; this fixes the Kotlin compilation blocker in the current workspace.
5. Latest debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.13.4-debug.apk`.
6. Verification passed: `./gradlew.bat :app:assembleDebug`; APK metadata confirms `1.13.4 / 252`.

## What Changed In The Latest 1.13.3 Patch

1. Version metadata moved to `1.13.3 / versionCode 251`.
2. This rebuild exists so Android can install over an already-installed `1.13.2` debug build.
3. No additional database schema, reminder behavior, Planning Desk behavior, or user-data format change was intentionally introduced by this metadata-only bump.
4. Latest debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.13.3-debug.apk`.
5. Verification passed: `./gradlew.bat :app:assembleDebug`; APK metadata confirms `1.13.3 / 251`.

## What Changed In The Latest 1.13.2 Patch

1. Version metadata moved to `1.13.2 / versionCode 250`.
2. This rebuild exists so Android can install over an already-installed `1.13.1` debug build.
3. No additional database schema, reminder behavior, Planning Desk behavior, or user-data format change was intentionally introduced by this metadata-only bump.
4. Latest debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.13.2-debug.apk`.
5. Verification passed: `./gradlew.bat :app:assembleDebug`; APK metadata confirms `1.13.2 / 250`.

## What Changed In The Latest 1.12.20 Patch

1. Version metadata moved to `1.12.20 / versionCode 247`.
2. This rebuild exists so Android can install over an already-installed `1.12.19` debug build.
3. No database schema, reminder behavior, Planning Desk behavior, or user-data format changed in this patch.
4. Latest debug APK target is `app/build/outputs/apk/debug/PaykiTodo-1.12.20-debug.apk`.
5. Verification passed: `./gradlew.bat :app:assembleDebug`; `git diff --check`; APK metadata confirms `1.12.20 / 247`.

## What Changed In The Latest 1.12.19 Patch

1. Recurring reminder scheduling catches exact-alarm failures and falls back without crashing; startup reminder recovery is guarded by SafeStartupGuard and wrapped in non-fatal error handling.
2. Recurring todo creation/recovery only materializes a limited upcoming window and replenishes future instances later, avoiding a 90-day alarm burst.
3. Daily-board countdown/todo/event interactions now open detail previews first; concrete schedule rows no longer jump directly into the editor from the board.
4. Board section title / empty-card clicks navigate to My Tasks or Calendar while item rows keep their own preview behavior.
5. Editor BottomSheets skip partial expansion and protect unsaved changes through confirmation.
6. Widget board/countdown layouts use tighter padding and list spacing.
7. Todo editor supports `仅提醒，不在看板/日历显示`; recurring templates inherit it, reminders still schedule, My Tasks shows a `仅提醒` chip, and board/widget/desktop-board/AI-report todo queries filter it out.
8. Planning Desk supports note nodes with `// ` / `> `, muted/italic note styling, manual note toggle, no linked todo/event creation, and completion calculations that ignore note children.
9. Database version is `24`; backup/restore and desktop-sync JSON preserve `hiddenFromBoard` and `isNote`.
10. Version metadata moved to `1.12.19 / versionCode 246`; latest debug APK path is `app/build/outputs/apk/debug/PaykiTodo-1.12.19-debug.apk`.
11. Verification: `./gradlew.bat :app:assembleDebug` output `BUILD SUCCESSFUL`; `git diff --check` passed; APK metadata confirms `1.12.19 / 246`.

## What Changed In The Latest 1.12.18 Patch

1. Version metadata moved to `1.12.18 / versionCode 245`.
2. This rebuild exists so Android can install over `1.12.17`.
3. Latest debug APK path is `app/build/outputs/apk/debug/PaykiTodo-1.12.18-debug.apk`.
4. Verification: `./gradlew.bat :app:assembleDebug` output `BUILD SUCCESSFUL`; `git diff --check` passed; APK metadata confirms `1.12.18 / 245`.

## What Changed In The Latest 1.12.17 Patch

1. Version metadata moved to `1.12.17 / versionCode 244`.
2. This rebuild intentionally does not change database schema, reminder behavior, Planning Desk behavior, or user-data format.
3. Latest debug APK path is `app/build/outputs/apk/debug/PaykiTodo-1.12.17-debug.apk`.
4. Verification passed: `./gradlew.bat :app:compileDebugKotlin`, `git diff --check`, `./gradlew.bat :app:assembleDebug`; APK metadata confirms `1.12.17 / 244`.

## What Changed In The Latest 1.12.16 Patch

1. Version metadata moved to `1.12.16 / versionCode 243`.
2. This rebuild intentionally does not change database schema, reminder behavior, Planning Desk behavior, or user-data format.
3. Latest debug APK path is `app/build/outputs/apk/debug/PaykiTodo-1.12.16-debug.apk`.

## What Changed In The Latest 1.12.15 Patch

1. Calendar title and calendar action buttons share one header row while keeping the title clickable.
2. Todo group creation moved beside `全部` as a compact `+` entry.
3. Planning Desk uses a larger document button and restores `从图片识别日程` in the overflow menu.
4. Planning Desk Outliner new nodes, share capture, photo capture, voice capture, and image recognition create draft nodes first instead of immediate official todos/events.
5. Phone and desktop Web can publish one draft node or all drafts in the current document; editing a draft preserves its publish/sync intent so publication still creates the expected official item.
6. Database version moved to `23` with `planning_nodes.isDraft`; backup/restore and desktop sync preserve the draft state.
7. Phone tutorial, desktop Web help, and in-app Wiki now describe that Enter creates a draft and publish creates the official item.
8. Version metadata moved to `1.12.15 / versionCode 242`.
9. Latest debug APK path is `app/build/outputs/apk/debug/PaykiTodo-1.12.15-debug.apk`.

## What Changed In The Latest 1.12.14 Patch

1. Version metadata moved to `1.12.14 / versionCode 241`.
2. This rebuild intentionally does not change database schema, reminder behavior, Planning Desk behavior, or user-data format.
3. Latest debug APK path is `app/build/outputs/apk/debug/PaykiTodo-1.12.14-debug.apk`.

## What Changed In The Latest 1.12.13 Patch

1. Phone Planning Desk Outliner node editing now uses cursor-aware `TextFieldValue`.
2. Empty phone input rows Backspace to the previous node; row-start Backspace merges text into the previous same-level node; middle Enter splits the node into a new same-level node.
3. Phone root / sibling / child input lines are borderless `BasicTextField` rows with placeholder and subtle focus background.
4. Desktop Web Outliner implements empty-row Backspace, row-start Backspace merge, middle Enter split, ArrowUp/ArrowDown focus movement, and a borderless root input line.
5. Version metadata moved to `1.12.13 / versionCode 240`.
6. Latest debug APK path is `app/build/outputs/apk/debug/PaykiTodo-1.12.13-debug.apk`.

## What Changed In The Latest 1.12.12 Patch

1. Version metadata moved to `1.12.12 / versionCode 239` so Android can upgrade over an installed `1.12.11` debug build.
2. This rebuild intentionally does not change database schema, reminder behavior, Planning Desk behavior, or user-data format.
3. Latest debug APK path is `app/build/outputs/apk/debug/PaykiTodo-1.12.12-debug.apk`.

## What Changed In The Latest 1.12.11 Patch

1. Existing-node edit mode now treats Enter as "continue writing here": it commits the current node, opens a same-level input row directly below it, and focuses that input for the next node.
2. Same-level insertion passes an explicit sort order so the new node is inserted at the current position instead of being appended at the end.
3. Existing-node edit commit is guarded so Enter, IME Done, and focus loss do not process the same edit twice.
4. Child input expansion on nodes without children now focuses the child input and uses matching expand/collapse icon semantics.
5. Preview menus for parents with children no longer offer a misleading sync toggle; they show `有子任务时保持结构标题`.
6. Version metadata moved to `1.12.11 / versionCode 238`.

## What Changed In The Latest 1.12.10 Patch

1. Phone Planning Desk Outliner edit mode now renders existing nodes as lightweight text rows rather than per-row card-like text fields.
2. The root outline and expanded child areas expose active input lines; pressing Enter / IME Done creates a node and clears the input.
3. Preview mode exposes per-row `⋯` actions for time, location, sync toggle, delete, and opening the linked official todo/event editor, while edit mode keeps row actions hidden.
4. Adding children demotes the parent node into a structure heading and deletes its linked official item; ordinary parents can be restored as leaf synced items after the last child is removed.
5. Natural schedule parsing accepts ordered bare locations such as `15:00-17:00, 写论文, 图书馆3楼`.
6. The phone Planning Desk overflow menu is document-focused; Markdown import/export moved to Markdown compatibility mode.
7. Phone tutorial pages now match the Outliner workflow.
8. Version metadata moved to `1.12.10 / versionCode 237`.

## What Changed In The Latest 1.12.9 Patch

1. Desktop Web Planning Desk Outliner same-level drag reorder no longer depends on reading `dataTransfer` during `dragover`.
2. The browser now records the current dragged node id in page state, so drop-target highlighting and placement work in browsers that restrict drag data reads before `drop`.
3. Drag reorder remains limited to nodes under the same parent; cross-level drag is blocked to avoid accidental hierarchy changes.
4. The existing up / down reorder buttons remain available as a precise fallback.
5. Database version stays `22`; no new Room schema was introduced.
6. Version metadata moved to `1.12.9 / versionCode 236`.

## What Changed In The Previous 1.12.8 Patch

1. Planning Desk Outliner linked-item deletion is now safe:
   - Deleting a formal todo / event created from an Outliner node detaches the node from official sync.
   - Startup repair will not recreate a formal item that the user intentionally deleted outside the Planning Desk.
2. Canceling a formal todo / event created from an Outliner node also detaches the node from official sync.
3. If an Outliner-created event has an optional event-end DDL linked todo, deleting or canceling the event handles that linked todo as well.
4. Phone ViewModel and desktop sync delete paths now receive the full deleted-item list so reminder artifacts are cleared for both the primary item and any linked end-DDL todo.
5. Database version stays `22`; no new Room schema was introduced.
6. Version metadata moved to `1.12.8 / versionCode 235`.

## What Changed In The Previous 1.12.7 Patch

1. Planning Desk Outliner nodes now store `syncEnabled`:
   - Ordinary nodes still create linked official todos/events.
   - Structure headings can remain in the outline without being repaired into no-DDL todos.
2. Database version moved to `22`:
   - `MIGRATION_21_22` adds `planning_nodes.syncEnabled`.
   - Common migrated headings such as `今日计划` / `收集箱` are marked as non-sync nodes.
   - If those headings were already auto-linked to no-DDL todos during `1.12.6`, only the linked auto rows are cleared.
3. Backup / restore and desktop sync preserve `syncEnabled`.
4. Desktop Web Planning Desk shows non-sync nodes as `结构标题` and exposes a compact `同步为待办/日程` checkbox.
5. Version metadata moved to `1.12.7 / versionCode 234`.

## What Changed In The Previous 1.12.6 Patch

1. Desktop Web Planning Desk Outliner time fields no longer render raw epoch milliseconds:
   - Node DDL / start / end summaries and inline inputs now prefer the backend `dueAt` / `startAt` / `endAt` strings.
   - If a string value is missing, the browser formats the millisecond value before display.
2. Desktop Web Planning Desk document switching now reloads the selected document's Outliner nodes:
   - Changing the document selector calls `/api/planning/nodes?noteId=...` before rendering.
   - This avoids showing the previous document's tree under the newly selected document title.
3. Version metadata moved to `1.12.6 / versionCode 233`.

## What Changed In The Previous 1.12.5 Patch

1. Background capture toast copy is notification-permission aware:
   - If Android notification permission is available, copy remains `正在后台识别，稍后通知`.
   - If Android 13+ notification permission is missing, share / photo / voice / Planning Desk capture now tells the user to check Planning Desk later instead of promising a notification that cannot be posted.
2. Phone Planning Desk Markdown compatibility capture is now explicit:
   - The toolbar button text is `捕获`.
   - Tapping it opens a confirmation dialog explaining that the current Markdown text will be written as new Outliner nodes and linked official todos/events.
   - This reduces accidental duplicate creation when the text was exported from the current Outliner.

## Goal Implementation Evidence Summary

### Quick Capture

- System share:
  - `app/src/main/java/com/example/todoalarm/ui/ShareReceiverActivity.kt`
  - `app/src/main/AndroidManifest.xml`
- Photo shortcut:
  - `app/src/main/java/com/example/todoalarm/ui/CaptureActivity.kt`
  - `app/src/main/res/xml/shortcuts.xml`
  - FileProvider declaration and paths in manifest / XML
- Voice shortcut:
  - `app/src/main/java/com/example/todoalarm/ui/VoiceCaptureActivity.kt`
  - `RECORD_AUDIO` permission in manifest
- Background processing:
  - `app/src/main/java/com/example/todoalarm/ui/BackgroundCaptureProcessor.kt`
  - `app/src/main/java/com/example/todoalarm/ui/CapturePlanningPipeline.kt`

### Planning Desk Outliner

- Data model / schema:
  - `app/src/main/java/com/example/todoalarm/data/PlanningNode.kt`
  - `app/src/main/java/com/example/todoalarm/data/PlanningNodeDraft.kt`
  - `app/src/main/java/com/example/todoalarm/data/AppDatabase.kt`
  - `app/src/main/java/com/example/todoalarm/data/DatabaseMigrations.kt`
  - `app/schemas/com.example.todoalarm.data.AppDatabase/21.json`
  - `app/schemas/com.example.todoalarm.data.AppDatabase/22.json`
- Repository / synchronization:
  - `app/src/main/java/com/example/todoalarm/data/TodoRepository.kt`
  - `app/src/main/java/com/example/todoalarm/data/TodoDao.kt`
  - `app/src/main/java/com/example/todoalarm/ui/TodoViewModel.kt`
- Phone UI:
  - `app/src/main/java/com/example/todoalarm/ui/PlanningDeskPanel.kt`
  - `app/src/main/java/com/example/todoalarm/ui/SettingsPanel.kt`
- Desktop sync / Web UI:
  - `app/src/main/java/com/example/todoalarm/sync/DesktopSyncCoordinator.kt`
  - `app/src/main/assets/desktop-web/app.js`
  - `app/src/main/assets/desktop-web/app.css`

## Verification Completed

Passed after the 1.12.15 patch:

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `./gradlew.bat :app:compileDebugKotlin`
3. `./gradlew.bat :app:testDebugUnitTest`
4. `git diff --check`
5. `./gradlew.bat :app:assembleDebug`
6. Debug APK metadata inspection: `versionName = 1.12.15`, `versionCode = 242`, output `PaykiTodo-1.12.15-debug.apk`

Passed after the 1.12.14 metadata rebuild:

1. `./gradlew.bat :app:compileDebugKotlin`
2. `git diff --check`
3. `./gradlew.bat :app:assembleDebug`
4. Debug APK metadata inspection: `versionName = 1.12.14`, `versionCode = 241`, output `PaykiTodo-1.12.14-debug.apk`

Passed so far after the 1.12.13 keyboard patch:

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `./gradlew.bat :app:compileDebugKotlin`
3. `git diff --check`
4. `./gradlew.bat :app:assembleDebug`
5. Debug APK metadata inspection: `versionName = 1.12.13`, `versionCode = 240`, output `PaykiTodo-1.12.13-debug.apk`

Passed so far after the 1.12.12 rebuild:

1. Version metadata moved to `1.12.12 / versionCode 239`.
2. `./gradlew.bat :app:compileDebugKotlin`
3. `./gradlew.bat :app:testDebugUnitTest`
4. `git diff --check`
5. `./gradlew.bat :app:assembleDebug`
6. Debug APK metadata inspection: `versionName = 1.12.12`, `versionCode = 239`, output `PaykiTodo-1.12.12-debug.apk`

Passed so far after the 1.12.11 patch:

1. `./gradlew.bat :app:compileDebugKotlin`
2. `./gradlew.bat :app:testDebugUnitTest`
3. `node --check app/src/main/assets/desktop-web/app.js`
4. `git diff --check`
5. `./gradlew.bat :app:assembleDebug`
6. Debug APK metadata inspection: `versionName = 1.12.11`, `versionCode = 238`, output `PaykiTodo-1.12.11-debug.apk`
7. Note: one parallel Gradle run triggered a Kotlin incremental-cache race while another Gradle task was compiling; rerunning the same commands sequentially passed.
8. Emulator `emulator-5554` runtime audit on installed `1.12.11` confirmed the target phone Planning Desk UX:
   - package metadata reports `versionName = 1.12.11`, `versionCode = 238`.
   - drawer navigation opens `规划台`.
   - edit mode shows the note-like hint, existing rows with expand / completion controls, and the active input placeholder `继续写下一行，按回车创建`.
   - typing `GoalAudit1512` into the active input and pressing Enter creates a normal node and keeps the next active input focused.
   - the main overflow menu contains `新建文档`, `重命名`, `使用说明`, `归档`, and `删除文档`, with no image-recognition or Markdown import/export actions in that menu.
   - `使用说明` opens the three-page `规划台新手教程`.
   - preview mode changes the toolbar button to `编辑`, hides active input rows, shows preview-mode copy, and exposes per-row `节点设置`.
   - a parent row with children shows disabled `有子任务时保持结构标题` in its node menu.
9. Emulator `emulator-5554` audit before the code patch confirmed the runtime parent/leaf data behavior:
   - `ParentAudit144457` with child is `syncEnabled = 0` and has no official linked todo.
   - `ChildAudit144457` / `ChildAuditComplete237` are leaf synced official todos.
   - deleting the last child restored `ParentAudit144457` as a synced leaf todo.
   - completing `ChildAuditComplete237` propagated completion to its parent node and linked official todo.

Passed after the 1.12.10 patch:

1. `./gradlew.bat :app:compileDebugKotlin`
2. `./gradlew.bat :app:testDebugUnitTest`
3. `git diff --check`
4. `./gradlew.bat :app:assembleDebug`
5. Debug APK metadata inspection: `versionName = 1.12.10`, `versionCode = 237`, output `PaykiTodo-1.12.10-debug.apk`

Emulator smoke test on `emulator-5554`:

1. Installed `app/build/outputs/apk/debug/PaykiTodo-1.12.10-debug.apk` with `adb install -r`.
2. Launched `com.example.todoalarm.ui.MainActivity` under package `com.paykitodo.app`.
3. Navigated from drawer to `规划台`; UI dump showed the Outliner toolbar with `Markdown` and `预览`, the note-like hint text, and the active input placeholder `继续写下一行，按回车创建`.
4. Switched to preview mode; UI dump showed the toolbar button changed to `编辑`, the root input disappeared, and the row exposed `节点设置`.
5. Opened row `⋯`; UI dump showed `完整编辑`, `设置时间`, `设置地点`, `改为结构标题`, and `删除`.
6. Tapped `完整编辑`; UI dump showed the existing `编辑日程` sheet with `保存`, proving the linked official event editor opens from preview mode.

Passed after the 1.12.9 patch:

1. `node --check app/src/main/assets/desktop-web/app.js`
2. `git diff --check`
3. `./gradlew.bat :app:compileDebugKotlin`
4. `./gradlew.bat :app:testDebugUnitTest`
5. `./gradlew.bat :app:assembleDebug`
6. Debug APK metadata inspection: `versionName = 1.12.9`, `versionCode = 236`, output `PaykiTodo-1.12.9-debug.apk`

Known warnings are existing Kotlin / Android deprecation or unused-parameter warnings, not build failures.

Follow-up audit performed on `Pixel_8 / emulator-5554`:

1. Confirmed installed package metadata on emulator: `versionName = 1.12.9`, `versionCode = 236`.
2. Reused `app/build/outputs/apk/debug/PaykiTodo-1.12.9-debug.apk`; no new APK was assembled in the audit-only pass.
3. Explicit `ACTION_SEND text/plain` to `com.example.todoalarm.ui.ShareReceiverActivity` with remote-quoted text `2030-05-21 15:00-16:00 ShareAuditQuoted-... @Library3` produced a notification title `已添加 1 条到规划台`.
4. Pulling `databases/todo-alarm.db` with `adb exec-out run-as com.paykitodo.app` showed:
   - `planning_nodes`: one node with title `ShareAuditQuoted-...`, location `@Library3`, expected start/end millis, and `linkedTodoId = 1`.
   - `todo_items`: one linked `EVENT` row with the same title/location and matching start/end timestamps.
5. The earlier `捕获识别失败：未能识别出待办或日程` observation came from an unquoted adb command that split the intended text at spaces; it was a test-command false negative, not a verified app defect.
6. Added a unit regression test for bare shared schedule text: `2030-05-21 15:00-16:00 ShareAudit @Library3`.
7. Follow-up validation passed: `node --check app/src/main/assets/desktop-web/app.js`, `./gradlew.bat :app:testDebugUnitTest`, and `git diff --check`.

## Remaining QA

No known code requirement from the active goal is intentionally left unimplemented. Remaining work is runtime QA:

1. Real-device testing for Android share targets, launcher shortcuts, camera capture, voice recognition, and Android 13+ notification-permission behavior.
2. Real-device testing for notification delivery and OEM background restrictions.
3. Real-browser testing for desktop Web Planning Desk node editing, up/down reorder, same-level drag reorder, document switching, and node time-field display/editing against live phone data.
4. Real upgraded-database testing with existing Planning Desk Markdown data, especially structure-heading preservation, old sync-enabled node linked-item repair on startup, and official-item delete/cancel detachment for Outliner-created items.

## Git State Notes

- Worktree should be clean after the 1.12.13 keyboard-fix implementation and goal-archive commits.
- Branch is ahead of `origin/main`; do not push without explicit user authorization.
- `docs/goals/2026-05-20-paykitodo-outliner-ux-fix-goal.md` is tracked in Git and was checked for common secret markers before closing the goal.
- Do not commit local signing material, APK outputs, API keys, tokens, or private Base URLs.
