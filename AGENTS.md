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

## Purpose Of The Current Docs

- `PROJECT_INTENT.md`: stable product background and design intent
- `PROJECT_STATUS.md`: current version, current branch state, active build facts, and repo health
- `FEATURE_LEDGER.md`: implemented / partial / pending feature ledger
- `CURRENT_TASK.md`: what this active development round is trying to finish next
- `SESSION_HANDOFF.md`: narrow handoff for the next coding session
- `SESSION_WORKFLOW_MANUAL.md`: how the user should operate future PaykiTodo sessions
