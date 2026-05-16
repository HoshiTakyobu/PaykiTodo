# Current Task

## Active Development Focus

The current round is PaykiTodo `1.8.2` / `versionCode 185`, focused on closing three follow-up issues from the `1.8.1` daily-use surfaces:

1. Android widget content should behave like a compact daily board, not a five-item today-todo list.
2. Announcements should support multiple entries and be authored in Planning Desk, not Settings.
3. Phone-side AI provider connection should handle common OpenAI-compatible Base URL shapes and report endpoint / non-JSON failures clearly.

## Completed In 1.8.2

1. `PlanningAiCaller` now resolves endpoint candidates more flexibly:
   - full `/chat/completions` URLs are used as-is
   - Base URLs ending in `/v1` append `/chat/completions`
   - root Base URLs try `/v1/chat/completions` before `/chat/completions`
2. AI provider test connection now uses 10s connect / 20s read timeouts.
3. HTML or other non-JSON responses now report a readable Base URL / OpenAI-compatible endpoint hint instead of surfacing `<!doctype` JSON conversion errors.
4. Added `PlanningAnnouncementParser`:
   - parses multiple active announcements from all unarchived Planning Desk notes
   - supports `#公告 5.16-7.1 文本`
   - supports `#公告 2026-05-16 2026-05-20 文本`
   - supports `> [!公告] 文本`
   - supports long-running announcements with no date range
5. Daily board reads active announcements from Planning Desk notes through `TodoUiState.activeAnnouncements`.
6. Settings no longer exposes `公告设置`; old app-settings announcement fields remain only as backward-compatible storage / backup fields.
7. Added `DailyBoardSnapshotBuilder` so board-style filtering can be reused outside Compose.
8. Android widget is now titled `今日看板` and displays a mixed RemoteViews list:
   - active Planning Desk announcements
   - today todo block including missed + today items
   - today schedule block including the “all events ended” message
   - tomorrow schedule block including empty state
9. Widget row layout is adaptive-height and no longer hard-limits to five todos; launcher resizing can reveal more rows.
10. Widget provider XML now declares min resize dimensions for better drag-resize compatibility.
11. Planning Desk phone tutorial, desktop-web Planning Desk help, Wiki, README, current docs, and changelog now describe Planning Desk announcements and the AI Base URL behavior.
12. Version metadata is now `1.8.2` / `versionCode 185`.

## Verification Completed This Round

1. `node --check app/src/main/assets/desktop-web/app.js` succeeded.
2. `./gradlew.bat testDebugUnitTest` succeeded after adding:
   - `PlanningAiCallerTest`
   - `PlanningAnnouncementParserTest`
3. `./gradlew.bat assembleDebug` succeeded and produced `app/build/outputs/apk/debug/PaykiTodo-1.8.2-debug.apk`.
4. `git diff --check` succeeded.

## Immediate Practical Next Steps

After installing the `1.8.2` APK on device, verify:

1. AI provider `测试连接` with:
   - root Base URL that requires `/v1/chat/completions`
   - Base URL already ending in `/v1`
   - full `/v1/chat/completions`
   - invalid root returning HTML
   - unreachable provider / timeout
2. Planning Desk announcements:
   - multiple active `#公告` lines
   - `> [!公告]` line
   - future / expired / no-date announcements
   - edit / delete / archive source note updates the board and widget
3. Daily board:
   - multiple active announcements appear above greeting
   - old Settings announcement entry is no longer present
4. Android launcher widget:
   - can be added as PaykiTodo `今日看板`
   - can be resized horizontally and vertically
   - shows announcements / today todos / today schedules / tomorrow schedules
   - uses today-ended schedule empty message correctly
   - opens the app on tap

## Commit Message Rule

PaykiTodo commit messages should describe product behavior changes and bug/debug reasoning. They should not primarily document push state, validation commands, or vague process notes. Commit subjects must not append version-bump tails such as `并升级到1.7.11`.

## Current External Dependency

The active objective came from the user request for widget board content, Planning Desk multi-announcements, and AI connection repair. Do not push unless the user explicitly asks.
