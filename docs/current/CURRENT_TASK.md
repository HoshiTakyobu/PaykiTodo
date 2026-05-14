# Current Task

## Active Development Focus

The current round hotfixes the PaykiTodo `1.7.2` Planning Desk startup crash and bumps the app to `1.7.3` / `versionCode 160`.

The `1.7.2` Markdown rendering work made the phone Planning Desk feel closer to Obsidian-style task notes, but it also made app startup risky when the last opened page was Planning Desk. The `1.7.3` hotfix keeps Markdown rendering opt-in so a bad document cannot block app launch.

Completed in this round:

1. Changed Planning Desk startup behavior so it always enters `编辑全文` first instead of auto-rendering old Markdown.
2. Kept rendered Markdown preview available only after the user taps `预览`.
3. Added preview parse/render failure protection with a fallback path back to `编辑全文`.
4. Replaced the experimental `FlowRow` preview layout with simpler `Column` / `Row` layout to reduce device compatibility risk.
5. Preserved the `1.7.2` Markdown rendering features: headings, task checkboxes, subtask indentation, tag pills, imported-state pills, and checkbox-to-Markdown toggling.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.7.3-debug.apk`
2. open the app and verify it no longer crashes on launch
3. open drawer -> `规划台`; it should enter `编辑全文` first
4. write raw Markdown:

```markdown
# 20260405待办

- [ ] 【保研】研究操作系统课程 #task
- [x] 【优秀】打印并提交先进个人资料 #task #imported
  - [ ] 子任务示例 #ddl 5.28
```

5. switch to `预览`
6. verify headings, checkboxes, subtask indentation, tag pills, and imported-state pill render correctly
7. tap a checkbox in preview and verify the raw Markdown line changes between `- [ ]` and `- [x]` after returning to `编辑全文`
8. verify `识别` and import still work from the same Markdown content

## Deferred From The User's Larger Request

- No rich-text inline Markdown editor yet.
- No AI auto-planning in this version.
- No drag-and-drop planning.
- No Gantt chart.
- No complex project tree; subtasks remain independent todos with parent metadata in notes.
- Desktop web Planning Desk still uses textarea plus editable parse preview; phone Markdown rendering is implemented first.

## Current External Dependency

No external file or API key is needed for the current `1.7.3` verification task.
