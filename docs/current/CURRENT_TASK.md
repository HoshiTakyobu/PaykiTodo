# Current Task

## Active Development Focus

The current round implements PaykiTodo Planning Desk Markdown rendering and bumps the app to `1.7.2` / `versionCode 159`.

The goal is to make the phone Planning Desk feel closer to Obsidian-style task notes: the app still stores raw Markdown, but the default reading surface renders task syntax into visible UI instead of showing `- [ ]` / `- [x]` as plain text.

Completed in this round:

1. Added phone-side Planning Desk reading/preview mode and kept a separate `编辑全文` mode for raw Markdown editing.
2. Rendered Markdown headings as visual heading levels.
3. Rendered `- [ ]` / `- [x]` task lines as checkbox rows, including indentation for subtasks.
4. Rendered `#tag` tokens as compact pills and `#imported` as an `已导入` state pill.
5. Allowed tapping a rendered checkbox to toggle the underlying Markdown line between `- [ ]` and `- [x]`; this only edits the planning document and does not directly complete imported todos.
6. Kept the underlying Markdown storage unchanged so parsing, import, backup, and desktop sync continue to operate on the original text.
7. Added future AI planning guidance to `docs/current/PLANNING_DESK_DESIGN.md`: local rules first, optional OpenAI-compatible providers, user-supplied keys, preview-first confirmation, and no direct database mutation by AI.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.7.2-debug.apk`
2. open drawer -> `规划台`
3. write raw Markdown in `编辑全文` mode:

```markdown
# 20260405待办

- [ ] 【保研】研究操作系统课程 #task
- [x] 【优秀】打印并提交先进个人资料 #task #imported
  - [ ] 子任务示例 #ddl 5.28
```

4. switch to `预览`
5. verify headings, checkboxes, subtask indentation, tag pills, and imported-state pill render correctly
6. tap a checkbox in preview and verify the raw Markdown line changes between `- [ ]` and `- [x]` after returning to `编辑全文`
7. verify `识别` and import still work from the same Markdown content

## Deferred From The User's Larger Request

- No rich-text inline Markdown editor yet.
- No AI auto-planning in this version.
- No drag-and-drop planning.
- No Gantt chart.
- No complex project tree; subtasks remain independent todos with parent metadata in notes.
- Desktop web Planning Desk still uses textarea plus editable parse preview; phone Markdown rendering is implemented first.

## Current External Dependency

No external file or API key is needed for the current `1.7.2` verification task.
