# Current Task

## Active Development Focus

The current round is PaykiTodo `1.7.6` / `versionCode 163`, focused on making the phone-side Planning Desk visually usable after `1.7.5` fixed the startup crash.

The user confirmed `1.7.5` no longer crashes. The next immediate problem is product feel: the Planning Desk screen looked like a rough transparent debug surface, making the feature unpleasant to use. This round keeps the stable raw Markdown Planning Desk model and improves its UI hierarchy, readability, and basic interaction layout.

Completed in this round:

1. Upgraded app version to `1.7.6` / `versionCode 163`.
2. Changed the Planning Desk main surface from semi-transparent cards over the background to solid-color card surfaces.
3. Reworked the top header into a clearer document title card with a smaller subtitle and separated document actions.
4. Reworked the main editor into a solid document card with a title row, divider, and large raw Markdown text area.
5. Changed the shortcut bar from two rows of chip buttons into a single horizontal scrolling toolbar to reduce vertical clutter.
6. Changed the document picker into a scrollable solid-color document list with clearer active-note styling.
7. Tightened the recognition preview sheet with a clearer import count, primary import button, and source-line surface inside each candidate card.
8. Kept the `1.7.5` database migration repair and the `1.7.4` rollback of phone-side Markdown rendering.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.7.6-debug.apk`
2. verify the app still opens without crashing
3. open drawer -> `规划台`
4. verify the Planning Desk uses solid surfaces rather than transparent / wallpaper-noisy surfaces
5. type Markdown / natural planning text and verify the editor remains readable
6. test shortcut chips by inserting task, subtask, DDL, schedule, reminder, group, today, and tomorrow tokens
7. open the document picker and verify the document list scrolls and selection is clear
8. tap `识别` and verify the preview sheet remains usable and can import selected candidates

## Important Debugging Note

`1.7.5` fixed the likely Room startup migration problem. If `1.7.6` crashes, compare against `1.7.5` first because this round is primarily UI-side.

Do not push `1.7.x` to GitHub unless the user explicitly asks.

## Deferred From The User's Larger Request

- Phone-side rendered Markdown reading mode is still deferred.
- No rich-text inline Markdown editor yet.
- No AI auto-planning in this version.
- No drag-and-drop planning.
- No Gantt chart.
- No complex project tree; subtasks remain independent todos with parent metadata in notes.
- Full desktop UI parity with phone UI remains incomplete.
- Full calendar rendering/performance optimization remains incomplete.
- Todo lunar wheel / lunar DDL work still needs a dedicated completion pass.

## Current External Dependency

No external file or API key is needed for the current `1.7.6` verification task.
