# PaykiTodo Agent Guide

This repository should be treated as a self-contained project workspace.

## First Read Order For Any New Session

Before making edits, read these files in order:

1. `docs/current/PROJECT_INTENT.md`
2. `docs/current/PROJECT_STATUS.md`
3. `docs/current/FEATURE_LEDGER.md`
4. `docs/current/CURRENT_TASK.md`
5. `docs/current/SESSION_HANDOFF.md`
6. `docs/current/PAYKITODO_SESSION_LEDGER.md`
7. `docs/current/SESSION_WORKFLOW_MANUAL.md`
8. `docs/current/UI_DESIGN_RULES.md`

Then read only the code files directly relevant to the current task.

## Rules

1. Do not use old chat memory as the source of truth when repository files say otherwise.
2. Do not scan the whole `G:\Workspace`; stay inside this repository unless the current task explicitly requires an external file.
3. Treat older versioned docs under `docs/` as historical reference, not the current product baseline, unless a current doc explicitly points to them.
4. Prefer updating the `docs/current/` files when project intent, status, or task scope changes.
5. If code changes affect product behavior, version status, implementation status, or current priorities, update the relevant docs in the same round rather than leaving them stale.
6. Before ending a work round, update `docs/current/SESSION_HANDOFF.md` and any other affected docs.
7. After a real feature / fix round is completed and minimally verified, create a focused git commit for that round unless the user explicitly says not to commit yet.
8. When reporting completion, tell the user the latest APK path that exists for the current version; if no new APK was built in that round, say so explicitly.
9. Git commit messages for this repository should be written in Chinese.
10. A git commit message should describe the version-relevant feature / behavior changes in that round compared with the previous state, not just generic process wording such as "finalize" or "cleanup".

## Git Commit Message Rules

Commit messages are part of the product history. They should primarily describe product behavior changes and bug-fix logic, not process bookkeeping.

Good commit messages should answer:

- Which module or feature changed?
- What user-visible behavior was added, removed, or improved?
- What bug was fixed, what was the before/after behavior, and what root cause or debug conclusion mattered?
- If relevant, what stability, data-safety, migration, reminder, sync, icon, or startup behavior changed?

Avoid making the commit subject/body primarily about:

- whether the branch was pushed;
- which validation commands were run;
- generic process notes;
- vague wording such as `优化`, `修复问题`, `更新代码`, or `完善功能` without concrete behavior.

Commit subjects must not append version-bump tails such as `并升级到1.7.9` or `并升级到 x.x.x`. The subject should describe only the concrete feature or bug-fix behavior. If the app version changed, put that detail in the commit body or the relevant version docs instead of the subject.

Required structure for every non-trivial commit body:

1. Chinese subject line that includes the affected module and concrete behavior change. Do not append version-bump tails.
2. Empty line, then the literal heading `完成内容概要：`.
3. Bullet list of concrete behavior changes, one per line, prefixed with `- `. Each bullet must be self-contained — a reader should understand the user-visible change without reading the diff.
4. The bullet list MUST include a version-bump bullet such as `- 版本升级到 1.8.4 / versionCode 187` when the app version changes.
5. Bullets must describe user-visible behavior or product surfaces, not file edits or process bookkeeping. Bad: `- 更新 SettingsPanel.kt`. Good: `- 设置 -> AI 调用配置新增「测试连接」按钮，10 秒内反馈成功字数或 HTTP/超时错误`.

Do not use paragraphs in the body. Do not let validation commands or push status dominate the message; mention them at most as one trailing bullet (e.g. `- 验证：assembleDebug、testDebugUnitTest、node --check 通过`) and only when relevant.

Concrete template:

```
{Chinese subject}

完成内容概要：
- {module + concrete behavior change}
- {module + concrete behavior change}
- 版本升级到 X.Y.Z / versionCode N
- 验证：{commands or skipped}
```

## Purpose Of The Current Docs

- `PROJECT_INTENT.md`: stable product background and design intent
- `PROJECT_STATUS.md`: current version, current branch state, active build facts, and repo health
- `FEATURE_LEDGER.md`: implemented / partial / pending feature ledger
- `CURRENT_TASK.md`: what this active development round is trying to finish next
- `SESSION_HANDOFF.md`: narrow handoff for the next coding session
- `SESSION_WORKFLOW_MANUAL.md`: how the user should operate future PaykiTodo sessions
