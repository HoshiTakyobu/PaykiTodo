# Current Task

## Active Development Focus

The current round is an emergency stabilization release for PaykiTodo `1.7.5` / `versionCode 162`.

`1.7.4` still crashed immediately on the user's phone even after the unstable phone-side Planning Desk Markdown renderer was removed. That makes the most likely startup failure a database-open problem rather than a Compose preview problem. The current fix targets the Planning Desk Room migration introduced in `1.7.0`: the `planning_notes` table created by `MIGRATION_8_9` did not exactly match the `PlanningNote` entity schema, which can make Room fail validation during app startup when upgrading from `1.6.x`.

Completed in this round:

1. Upgraded app version to `1.7.5` / `versionCode 162`.
2. Upgraded Room database version from `9` to `10`.
3. Corrected `MIGRATION_8_9` so fresh upgrades from `1.6.x` create `planning_notes` with the same schema Room expects from `PlanningNote`.
4. Added `MIGRATION_9_10` to rebuild existing `planning_notes` tables created by `1.7.0`-`1.7.4` into the expected schema.
5. Made the rebuild migration tolerate incomplete leftover `planning_notes` tables by recreating the table instead of crashing during the repair path.
6. Kept the `1.7.4` rollback of phone-side Markdown rendering; this release does not reintroduce that renderer.
7. Updated README / CHANGELOG / TODO / Wiki / current docs to document the database migration fix.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.7.5-debug.apk`
2. open the app and verify it no longer crashes on launch
3. if it opens, check that existing todos/events are still present
4. open drawer -> `规划台`
5. verify the phone Planning Desk shows the stable raw Markdown editor
6. verify document create/open/search still works
7. verify `识别` still opens editable preview cards
8. verify importing selected items still creates todos/events and appends `#imported` to source lines
9. verify existing core app areas still open: 每日看板, 我的任务, 日历, 设置

## Important Debugging Note

Installing a working `1.6.x` version to view crash logs for a crashing `1.7.x` build is not a reliable primary strategy:

- Android usually blocks versionCode downgrade installs over the same package.
- Even if forced, a `1.6.x` app may not open a database that has already been migrated to a newer Room version.
- The in-app crash log UI requires the app process to initialize far enough to show the UI; a Room startup crash can happen before that.

Use code-level startup-path inspection and migration repair first. If `1.7.5` still crashes, the next step should be direct device log capture or an even narrower startup-safe diagnostic build.

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

No external file or API key is needed for the current `1.7.5` verification task.
