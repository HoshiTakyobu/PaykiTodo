# PaykiTodo Session Workflow Manual

## Purpose

This file explains how to start and maintain a PaykiTodo coding session without depending on old chat history.

## What To Do When You Open A New PaykiTodo Session

### Step 1: Open Or Create The Session

You can use your normal Codex control panel / TUI flow.

The important part is not the exact launcher. The important part is that the session should work in:

`G:\Workspace\Project\PaykiTodo`

### Step 2: Send The Bootstrap Prompt

Immediately send the contents of:

- `START_NEW_SESSION.txt`

Do not start with casual chat. Do not start with free-form brainstorming. The first message should force the session to rebuild context from repository files.

### Step 3: Wait For The Session To Summarize

The new session should first output:

1. project background summary
2. current repo state summary
3. smallest next step

If it does not do this, stop it and tell it to follow `START_NEW_SESSION.txt` exactly.

## What Files The Session Must Read First

The repository bootstrap order is:

1. `AGENTS.md`
2. `docs/current/PROJECT_INTENT.md`
3. `docs/current/PROJECT_STATUS.md`
4. `docs/current/FEATURE_LEDGER.md`
5. `docs/current/CURRENT_TASK.md`
6. `docs/current/SESSION_HANDOFF.md`
7. `docs/current/PAYKITODO_SESSION_LEDGER.md`
8. `README.md`
9. `TODO.md`
10. `CHANGELOG.md`
11. `git status`

## What To Do During The Session

1. Keep the session focused on the current task.
2. If the task changes materially, tell the session to update:
   - `docs/current/CURRENT_TASK.md`
   - `docs/current/SESSION_HANDOFF.md`
3. If the session discovers that a current doc is stale, have it fix the doc as part of the work.
4. If the code changes user-visible behavior, versioning, feature status, or project direction, have it update the relevant docs in the same round.

## What To Do Before Ending A Session

Before you stop using a session for the day, it should leave the repository in a recoverable state.

At minimum, ask it to do these things:

1. summarize what was completed this round
2. summarize what remains unfinished
3. update `docs/current/SESSION_HANDOFF.md`
4. if project direction changed, update `docs/current/CURRENT_TASK.md`
5. if status changed materially, update `docs/current/PROJECT_STATUS.md`
6. if implementation status changed, update `docs/current/FEATURE_LEDGER.md`
7. if user-facing behavior, version references, or milestone docs became stale, update files such as `README.md`, `TODO.md`, `CHANGELOG.md`, or any relevant task-specific docs
8. after a real feature / fix is completed and minimally verified, create a focused git commit unless the user says not to commit yet
9. report the latest APK path for the current version, and clearly say whether that APK was rebuilt in the current round
10. write the git commit message in Chinese
11. make the commit message describe the version-relevant feature or behavior changes completed in that round, rather than vague process wording

## Recommended End-Of-Session Prompt

Use this before you leave a PaykiTodo session:

```text
Before ending this session:
1. summarize what you completed in this round
2. summarize what is still unfinished
3. update docs/current/SESSION_HANDOFF.md
4. update docs/current/CURRENT_TASK.md if the active focus changed
5. update docs/current/PROJECT_STATUS.md and docs/current/FEATURE_LEDGER.md if they became stale
6. update README.md / TODO.md / CHANGELOG.md or any other affected docs if your work changed what they claim
7. create a focused git commit if a real feature / fix round was completed and minimally verified
8. tell me the latest APK path for the current version and whether it was rebuilt in this round
9. write the git commit message in Chinese and describe the actual version-level changes you completed
10. do not start new feature work after the handoff update
```

## If A Session Starts Acting Stupid

Symptoms include:

- it ignores repository docs
- it starts scanning the whole workspace
- it talks as if old chat memory is more trustworthy than code
- it immediately completes without doing real work

Then do not keep arguing with that session.

Instead:

1. open a fresh session
2. send `START_NEW_SESSION.txt` again
3. continue from repository state, not from that bad session

## Main Rule

For PaykiTodo, repository files are the shared memory.
Sessions are disposable execution surfaces.
