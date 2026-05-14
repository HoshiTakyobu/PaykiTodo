# Current Task

## Active Development Focus

The current round is an emergency stabilization release for PaykiTodo `1.7.4` / `versionCode 161`.

The device still crashed on launch after the `1.7.3` Planning Desk hotfix. Because the app must open reliably before any Markdown rendering polish matters, this round rolls back the phone-side Planning Desk Markdown rendering implementation introduced in `1.7.2` / `1.7.3` and restores the stable `1.7.1` phone Planning Desk surface.

Completed in this round:

1. Restored `PlanningDeskPanel.kt` to the verified `1.7.1` implementation.
2. Removed the phone-side rendered Markdown preview path from the app startup/UI path.
3. Kept the Planning Desk core workflow from `1.7.1`: multiple documents, search, raw Markdown editing, local parsing, editable import preview, selected import, and automatic `#imported` write-back.
4. Kept desktop-web Planning Desk Phase 2 behavior; this rollback only targets the unstable phone-side Markdown renderer.
5. Upgraded version metadata to `1.7.4` / `versionCode 161`.
6. Updated README / CHANGELOG / TODO / Wiki / current docs to make the rollback explicit.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.7.4-debug.apk`
2. open the app and verify it no longer crashes on launch
3. open drawer -> `规划台`
4. verify the phone Planning Desk shows the stable raw Markdown editor, not the reverted rendered preview
5. verify document create/open/search still works
6. verify `识别` still opens editable preview cards
7. verify importing selected items still creates todos/events and appends `#imported` to source lines
8. verify existing core app areas still open: 每日看板, 我的任务, 日历, 设置

## Deferred From The User's Larger Request

- Phone-side rendered Markdown reading mode is temporarily deferred because it caused repeated startup crashes in `1.7.2` / `1.7.3`.
- No rich-text inline Markdown editor yet.
- No AI auto-planning in this version.
- No drag-and-drop planning.
- No Gantt chart.
- No complex project tree; subtasks remain independent todos with parent metadata in notes.
- Full desktop UI parity with phone UI remains incomplete.
- Full calendar rendering/performance optimization remains incomplete.
- Todo lunar wheel / lunar DDL work still needs a dedicated completion pass.

## Current External Dependency

No external file or API key is needed for the current `1.7.4` verification task.
