# Current Task

## Active Development Focus

The current round is PaykiTodo `1.7.8` / `versionCode 165`, focused on safely restoring the phone-side Planning Desk Markdown preview that was originally attempted in `1.7.2` / `1.7.3` but could not be validated because the `1.7.x` line crashed before opening.

The user confirmed `1.7.5` fixed the startup crash and asked to add Markdown rendering plus the functionality from the earlier non-running versions. This round restores the practical `1.7.2` / `1.7.3` Markdown preview features, but keeps the safety constraints learned from the crash cycle: the Planning Desk always opens in raw edit mode, and rendering only runs after the user taps `预览`.

Completed in this round:

1. Upgraded app version to `1.7.8` / `versionCode 165`.
2. Restored a safe `编辑 / 预览` switch in the Planning Desk top action area.
3. Planning Desk still defaults to raw Markdown editing when the screen opens or the active note changes.
4. Markdown preview renders headings, task checkboxes, subtask indentation, tag pills, and `#imported` state pills.
5. Tapping a task checkbox in preview rewrites the raw Markdown line between `- [ ]` and `- [x]`.
6. Checkbox toggling does not directly complete imported official todos, avoiding ambiguous state synchronization.
7. Preview rendering is wrapped in a failure-protected path with a visible fallback and a return-to-edit action.
8. Kept `1.7.7` in-screen Planning Desk help, `1.7.6` pure-color UI polish, `1.7.5` database migration repair, and `1.7.4` startup-safety lessons.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.7.8-debug.apk`
2. verify the app still opens without crashing
3. open drawer -> `规划台`
4. verify it opens in raw edit mode, not preview mode
5. type headings and tasks such as `# 明天`, `- [ ] 整理材料 #ddl 5.28 #group 课程`, and indented subtasks
6. tap `预览` and verify headings, checkboxes, indentation, tags, and `#imported` pills render
7. tap a preview checkbox and return to edit mode to verify raw Markdown changed between `- [ ]` and `- [x]`
8. verify `识别` and import still work after toggling preview
9. verify the help sheet still opens from the Info button

## Safety Rule For Future Markdown Work

Do not make rendered Markdown the default startup view unless there is explicit crash-safe isolation and test coverage. The safe default remains raw edit mode.

Do not push `1.7.x` to GitHub unless the user explicitly asks.

## Deferred From The User's Larger Request

- No rich-text inline Markdown editor yet.
- No AI auto-planning in this version.
- No drag-and-drop planning.
- No Gantt chart.
- No complex project tree; subtasks remain independent todos with parent metadata in notes.
- Full desktop UI parity with phone UI remains incomplete.
- Full calendar rendering/performance optimization remains incomplete.
- Todo lunar wheel / lunar DDL work still needs a dedicated completion pass.

## Current External Dependency

No external file or API key is needed for the current `1.7.8` verification task.
