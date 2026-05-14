# Current Task

## Active Development Focus

The current round is PaykiTodo `1.7.10` / `versionCode 167`, focused on making Planning Desk behave like a practical blank planning notebook instead of a demo-filled syntax surface.

The user reported these Planning Desk issues:

1. New documents should not contain saved example text; examples should be grey placeholder guidance that disappears when typing.
2. Planning Desk `#remind` must align with the mixed reminder syntax already used by todo/event editing and batch import.
3. The shortcut bar should not insert repeated `- [ ]` markers into one line, and the subtask button should create a new indented subtask line.
4. Headings such as `# 今日计划`, `# 今天`, and `# 明天` were unclear and needed real behavior plus clearer explanation.
5. The planning document directory needed an obvious delete path.

Completed in this round:

1. New/default planning notes now start with empty `contentMarkdown`.
2. The editor placeholder now contains the example guidance instead of storing it as document content.
3. Added a common reminder text parser used by reminder inputs and Planning Desk `#remind`.
4. Expanded reminder parsing to support examples such as `5,15,16:30,2:30 pm,05-10 15:00,5.10 15:00,5月10日 14:30`.
5. Planning Desk `#remind` now reports invalid explicit reminder syntax instead of silently falling back to the default 5-minute reminder.
6. Planning Desk preview reminder editing now preserves raw input, shows invalid input errors, and blocks import until fixed.
7. The shortcut bar now has semantic `任务` and `子任务` actions rather than blindly inserting raw text.
8. Heading context now recognizes headings containing `今日` / `今天` / `明天`, so `# 今日计划` affects following undated schedule lines.
9. Phone Planning Desk document list now includes a per-document delete button with confirmation.
10. Desktop Web Planning Desk now includes a current-document delete button and sends raw reminder input back to the phone API for unified parsing.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.7.10-debug.apk`
2. open drawer -> `规划台`
3. create a new planning document and verify the editor is empty while placeholder text is grey
4. type on one line and tap `任务` repeatedly; verify only one `- [ ]` exists
5. place the cursor on a task and tap `子任务`; verify a new indented child task line appears
6. test `# 今日计划` followed by `10:00-11:00 写论文`; verify recognition uses today's date
7. test `#remind 5,15,16:30,5月10日 14:30` and verify preview recognizes or reports specific invalid reasons
8. open the document directory and verify a document can be deleted only after confirmation
9. enable desktop sync, open the Web Planning Desk, and verify the delete button and reminder preview import still work

## Commit Message Rule

PaykiTodo commit messages should describe product behavior changes and bug/debug reasoning. They should not primarily document push state, validation commands, or vague process notes. Commit subjects must not append version-bump tails such as `并升级到1.7.10`.

## Current External Dependency

No external file or API key is needed for the current `1.7.10` verification task.
