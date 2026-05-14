# Current Task

## Active Development Focus

The current round implements PaykiTodo Planning Desk Phase 2 and bumps the app to `1.7.1` / `versionCode 158`.

Phase 2 keeps the Planning Desk scope practical: no AI dependency, no drag/drop planning, no Gantt chart, and no complex project tree. The goal is to make the existing memo-like planning workflow safer and more usable for daily work.

Completed in this round:

1. Added document search in the phone Planning Desk document sheet. Search matches title and Markdown content.
2. Added editable phone-side recognition preview cards. Before import, the user can adjust title, group, notes, todo DDL, event start/end, reminder offsets, and event linked-todo creation.
3. Added shared `PlanningImportCandidate` / `PlanningImportResult` models so edited preview data can flow through the same import path.
4. Added automatic `#imported` write-back after a successful import. The current planning document is updated immediately to reduce duplicate imports.
5. Added `PlanningMarkdownParser.markImportedLines` with unit-test coverage for appending the marker without duplicating an existing one.
6. Updated desktop web Planning Desk preview cards to support inline editing before import.
7. Updated `/api/planning/import` to accept edited candidates, validate them, import selected candidates, write back `#imported`, and return `updatedMarkdown`.
8. Kept Planning Desk import defaults at 5 minutes before, full-screen, ring + vibration.
9. Updated README, CHANGELOG, TODO, docs/current, and in-app Wiki for `1.7.1`.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.7.1-debug.apk`
2. open drawer -> `规划台`
3. verify document search filters by title and content
4. type examples:

```markdown
# 明天

- [ ] 09:00-10:30 写论文 #group 课程
- [ ] 整理保研材料 #ddl 5.28
10:00-12:30 作业1
```

5. tap `识别`, edit title/group/notes/time/reminder fields in preview, then import selected entries
6. verify imported todos/events appear in My Tasks / Calendar
7. verify imported source lines are automatically appended with `#imported`
8. enable desktop sync, open desktop web, switch to `规划台`, edit preview fields, import, and verify the editor text receives `#imported`

## Deferred From The User's Larger Request

- No drag-and-drop planning.
- No Gantt chart.
- No AI auto-planning or paid model dependency.
- No complex project tree; subtasks remain independent todos with parent metadata in notes.
- Phone editor remains plain text, not Markdown highlighting or rich text.
- Desktop web Planning Desk remains a textarea plus preview layout, now with editable preview fields.

## Current External Dependency

No external file is needed for the current `1.7.1` verification task.
