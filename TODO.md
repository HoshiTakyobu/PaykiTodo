# TODO

## Current Iteration

### In Progress

- Verify version `1.6.44` on device / desktop browser after desktop sync Web UI refinement
- Keep `README.md`, `CHANGELOG.md`, and current-state docs aligned with version `1.6.44`
- Continue board / dashboard and calendar polish without regressing the current interaction model
- Improve repo-native handoff so new sessions do not depend on long chat history

### Needs Verification

- The current icon resource chain should be verified on actual device surfaces:
  - install prompt icon
  - launcher icon
  - monochrome themed icon
  - notification bar small icon
- The current time label and red-line behavior in calendar views should be verified against the user's intended behavior
- The current board / dashboard readability should be checked in both light and dark themes
- Tapping a todo in the daily board should open the new bottom-sheet preview and must not mark it completed unless the checkbox is used
- Delete actions should show confirmation before deleting on both phone UI and desktop web console
- After an event reminder fires, the event preview should still show the configured reminder offsets
- Tapping the desktop-sync foreground notification should open Settings -> Desktop Sync
- Finished daily-board events should disappear after their end time
- Currently running daily-board events should show the gold outline / glow treatment
- Daily-board schedule rows should keep the left color bars aligned across in-progress and normal events
- Normal daily-board schedule rows should show a thin same-color border instead of only a clipped side arc
- Calendar pending event draft should be cancelable by long-pressing blank timeline space, and should clear when opening an existing event
- Missed active todos should appear in the daily board todo block
- Delete confirmation sheets should show the refined dangerous-action UI on phone-side delete paths
- Wiki sidebar links should switch sections inside the in-app Wiki WebView
- Settings -> 使用说明 should open Wiki directly, and Settings -> 提示音 should open the system notification-tone picker directly
- Desktop web all-day events spanning multiple days should render as one horizontal continuous bar
- Desktop web should allow editing existing todos with DDL, reminder, group, recurrence, ring, and vibration fields
- Desktop web should expose explicit edit buttons on timed and all-day event cards
- Desktop web todo / event editors should feel close to the phone-side bottom-sheet visual language and remain usable on narrow browser windows
- Launcher icon should use the picture-based `ic_launcher_art` adaptive foreground, not the old vector mark
- Todo and calendar reminder input should accept comma-separated mixed reminder specs and block invalid values
- Todo batch import should parse lightweight comma rows, including same-day DDL like `16:30,写报告,5`
- Daily board should not show add / batch-add buttons; it is a read-only board surface
- My Tasks should show only the todo batch-import entry beside the bottom-right `+` button
- Calendar batch-import should remain available from the calendar surface
- Todo batch import should use the lightweight `DDL,任务名称,提醒时间` syntax without requiring `|` or key-value fields
- Full-screen and accessibility snooze custom input should accept minutes or a concrete future time
- Custom snooze should accept long delays beyond 180 minutes and update todo DDL when the snooze target is later than the current DDL
- Launch screen icon should use the transparent logo asset without a white square background
- Input help question-mark buttons should open the correct syntax help beside reminder, batch, and snooze fields
- In-app Wiki should describe the current 1.6.36 input syntax accurately

## Mid-Term Follow-Ups

- Continue improving reminder reliability across foreground / background / lock-screen scenarios
- Expand the LAN desktop sync console with richer operations and better field coverage
- Clean up release signing and release build workflow for future stable publishing
- Keep historical docs usable while making current docs the default entry point

## Documentation Discipline

- Update `docs/current/*` whenever active direction or known status changes
- Keep `CHANGELOG.md` focused on released or release-like milestones
- Use `docs/current/SESSION_HANDOFF.md` to describe the current narrow takeover state

## Done Recently

- Bumped the app to `1.6.44` / `versionCode 116`
- Refined the desktop web todo / event editor into a bottom-sheet-like layout with cancel/title/save header
- Card-styled desktop web editor fields and lightened timeline/event action buttons
- Fixed hidden destructive buttons in desktop web create mode
- Bumped the app to `1.6.43` / `versionCode 115`
- Added desktop web editing for existing todos, including DDL, reminder, group, recurrence, ring, and vibration fields
- Added a desktop sync `PUT /api/todos/{id}` endpoint and reminder rescheduling after todo edits
- Added explicit desktop web edit buttons for timed and all-day events
- Bumped the app to `1.6.42` / `versionCode 114`
- Added a transparent launch-screen logo asset and switched only the launch screen to it
- Removed the 180-minute cap from custom snooze input
- Updated todo snooze so a snooze target later than the current DDL also moves the todo DDL to that target
- Bumped the app to `1.6.41` / `versionCode 113`
- Unified daily-board schedule row left color-bar alignment across in-progress and normal events
- Added thin same-color borders to normal daily-board schedule rows
- Reduced in-progress schedule highlight to a border plus subtle inner glow instead of a large yellow overlay
- Bumped the app to `1.6.40` / `versionCode 112`
- Fixed todo batch-import DDL parsing so `16:30` means today 16:30
- Made todo batch-import DDL parsing accept Chinese colon input such as `16：30`
- Updated todo batch-import help text and Wiki examples for same-day DDL input
- Bumped the app to `1.6.39` / `versionCode 111`
- Removed batch-add buttons from the daily board so the board stays read-only
- Moved My Tasks batch todo import to the bottom-right FAB area beside the new-todo button
- Matched daily-board schedule event color-strip height to the right-side text block height
- Bumped the app to `1.6.38` / `versionCode 110`
- Reprocessed picture-based launcher icon art with pure-white opaque background and smaller centered content
- Changed launcher adaptive background color to pure white
- Bumped the app to `1.6.37` / `versionCode 109`
- Restored launcher adaptive icon foreground to the picture-based `ic_launcher_art`
- Removed old vector mark launcher resources so they cannot be reused accidentally
- Bumped the app to `1.6.36` / `versionCode 108`
- Added question-mark syntax help buttons beside key reminder, batch-import, and custom-snooze inputs
- Updated in-app Wiki for unified reminder syntax, lightweight todo batch syntax, calendar batch `Remind=`, and custom snooze input
- Bumped the app to `1.6.35` / `versionCode 107`
- Simplified todo batch import to comma-ordered `DDL,任务名称,提醒时间`
- Reused the shared reminder parser for calendar batch `Remind=` fields
- Reused the shared time parser for custom snooze input on full-screen and accessibility reminder surfaces
- Bumped the app to `1.6.34` / `versionCode 106`
- Unified todo and calendar reminder editing around one comma-separated text input syntax
- Connected normal todos to the multi-reminder offset storage and scheduling path
- Added todo batch import with line-based syntax and preview validation
- Exposed batch-add buttons instead of hiding them only inside folded menus
- Bumped the app to `1.6.33` / `versionCode 105`
- Fixed in-app Wiki navigation by enabling JavaScript only for the local asset page
- Changed Settings help and tone rows to direct actions instead of nested subpanels
- Rendered multi-day all-day events as continuous horizontal bars in the desktop web console
- Historical note: launcher foreground was once reconnected to a safe-zone vector mark, but this was reverted in 1.6.37; current launcher foreground uses picture resource `ic_launcher_art`
- Bumped the app to `1.6.32` / `versionCode 104`
- Added missed active todos back into the daily board todo block
- Reworked the shared delete confirmation bottom sheet into a refined dangerous-action UI
- Bumped the app to `1.6.31` / `versionCode 103`
- Fixed daily-board schedule filtering so finished timed events are hidden
- Added gold outline and glow treatment for currently running events on the daily board
- Added clear/cancel behavior for calendar timeline pending event drafts
- Bumped the app to `1.6.30` / `versionCode 102`
- Reworked active todo preview into the shared bottom-sheet visual language and split preview click from completion checkbox
- Added delete confirmation coverage for schedule templates, group deletion, calendar event deletion, active todo deletion, and desktop web delete buttons
- Preserved calendar event reminder configuration after a reminder has fired
- Added foreground desktop-sync notification click routing into the in-app desktop sync settings section
- Switched the adaptive launcher icon chain to the current PaykiTodo mark resources
- Replaced the launcher foreground with a safe-zone vector logo so adaptive masks no longer crop the full raster composition
- Fixed the calendar current-time label and red line so they remain visible even when the current day is off-screen
- Switched reminder and desktop-sync notifications to `ic_stat_payki_todo`
- Added the daily board default entry and the today / tomorrow schedule summary block
- Split dashboard background art into light and dark resources and refreshed launch / drawer visuals
- Added the release-signing template document under `docs/`
- Added repo-native new-session bootstrap docs under `docs/current/`
- Added repository-level `AGENTS.md` so new sessions know what to read first
- Preserved historical docs while marking them as non-baseline through current entry files
