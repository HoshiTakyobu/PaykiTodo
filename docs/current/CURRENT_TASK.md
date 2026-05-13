# Current Task

## Active Development Focus

The current round implements PaykiTodo Planning Desk Phase 1 and bumps the app to `1.6.84` / `versionCode 156`.

Planning Desk is intended to cover the user's upstream workflow before final DDL/reminder execution: write rough plans like a memo or Obsidian note, locally recognize useful lines, preview them, then import selected entries into existing PaykiTodo todos and calendar events.

Completed in this round:

1. Added Room table `planning_notes` with database migration `8 -> 9`.
2. Added multi-document planning support: create, open, rename, archive, delete, and last-opened document restoration.
3. Added `PlanningMarkdownParser`, a local rule parser with no AI dependency.
4. Parser supports markdown checkbox todos, completed-task skipping, subtasks as independent todos with parent metadata, date headings, common DDL date/time formats, `#ddl`, `#remind`, `#group`, `#schedule`, and natural schedule ranges such as `10:00-12:30 作业1`.
5. Added phone-side drawer entry `规划台` and a Markdown-style planning editor.
6. Added phone-side shortcut bar for task, subtask, indent, outdent, DDL, schedule, reminder, group, today, and tomorrow.
7. Added preview-first import. Selected candidates can be imported as todos/events; natural events default to also creating a linked todo whose DDL is the event end time.
8. Planning Desk imports default to 5 minutes before, full-screen, ring + vibration.
9. Planning notes are included in JSON backup / restore snapshots.
10. Added desktop web `规划台` tab with document selector, textarea editor, save, parse preview, and selected import.
11. Added desktop sync APIs under `/api/planning/*` for notes, parse, and import.
12. Updated README, CHANGELOG, TODO, docs/current, and in-app Wiki for the new workflow.
13. `node --check app/src/main/assets/desktop-web/app.js`, `git diff --check`, and `./gradlew.bat assembleDebug` succeeded.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.84-debug.apk`
2. open drawer -> `规划台`
3. verify the default `我的规划` document appears
4. create a second document, switch between documents, rename it, and test delete confirmation
5. type examples:

```markdown
# 明天

- [ ] 09:00-10:30 写论文 #group 课程
- [ ] 整理保研材料 #ddl 5.28
10:00-12:30 作业1
```

6. tap `识别`, verify preview types, default-today labels, reminders, and linked todo checkbox
7. import selected entries and verify they appear in My Tasks / Calendar
8. enable desktop sync, open desktop web, switch to `规划台`, edit/save/parse/import the same note

## Deferred From The User's Larger Request

- No drag-and-drop planning.
- No Gantt chart.
- No AI auto-planning or paid model dependency.
- No complex project tree; subtasks are independent todos with parent metadata in notes.
- Phone editor is plain text, not Markdown highlighting or rich text.
- Desktop web Planning Desk is intentionally `textarea + preview`; phone-parity visual design can be improved later.
- Parser has build validation but not a dedicated JVM unit-test suite yet.

## Current External Dependency

No external file is needed for the current `1.6.84` verification task.
