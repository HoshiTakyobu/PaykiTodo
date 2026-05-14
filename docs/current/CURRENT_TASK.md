# Current Task

## Active Development Focus

The current round is PaykiTodo `1.7.7` / `versionCode 164`, focused on making the phone-side Planning Desk easier to learn after the pure-color UI polish.

The user confirmed the app no longer crashes and then reported that the Planning Desk is not obvious to use. This round keeps the stable raw Markdown Planning Desk model and adds a direct in-screen help entry with practical examples.

Completed in this round:

1. Upgraded app version to `1.7.7` / `versionCode 164`.
2. Added a Planning Desk help button in the top action area.
3. Added an in-app Planning Desk help bottom sheet that explains the real workflow: write freely, use shortcuts, recognize, edit preview, and import.
4. Added directly usable examples covering todo lines, subtasks, DDL, reminders, groups, and schedule ranges.
5. Documented current limitations in the help sheet: stable raw editor, no rich renderer yet, no AI split, no drag scheduling.
6. Kept the `1.7.6` pure-color Planning Desk UI polish, `1.7.5` database repair, and `1.7.4` Markdown-render rollback.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.7.7-debug.apk`
2. verify the app still opens without crashing
3. open drawer -> `规划台`
4. tap the Planning Desk说明/Info button and verify the help sheet opens
5. type Markdown / natural planning text and verify the editor remains readable
6. read the examples and test shortcut chips by inserting task, subtask, DDL, schedule, reminder, group, today, and tomorrow tokens
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

No external file or API key is needed for the current `1.7.7` verification task.
