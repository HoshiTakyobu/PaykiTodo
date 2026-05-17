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

## Android Emulator Debugging Workflow

The Android Emulator is a useful middle step between "the code builds" and "the user installs the APK on a real phone". It should be used when a change needs phone-side visual or runtime confirmation but does not yet require OEM-specific behavior.

Use it for:

- checking whether the APK launches or crashes
- previewing Daily Board, Planning Desk, Settings, FocusActivity, editor dialogs, and similar phone UI
- running basic click flows before sending an APK to the user
- collecting `uiautomator dump` XML or screenshots as evidence for visible UI text and navigation state
- smoke-testing scheduled-report flows when the emulator has the relevant Android services available

Basic command flow:

```powershell
# 1. Check whether an emulator or phone is already connected.
C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe devices

# 2. Install the latest debug APK when needed.
C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\PaykiTodo-<version>-debug.apk

# 3. Start the app.
C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe shell monkey -p com.paykitodo.app 1

# 4. Dump the current UI tree when screenshots are not enough.
C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe shell uiautomator dump /sdcard/window.xml
C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe pull /sdcard/window.xml .tmp-current-ui.xml
```

If no emulator is running and this verification would be useful, the session may start an AVD from Android Studio / Android SDK. The session should explicitly tell the user that an Android Emulator window may appear, because it is a visible desktop application.

Current known useful emulator facts from the 1.9.x work:

- `emulator-5554` was used to install and run `com.paykitodo.app`.
- UI dumps confirmed the Daily Board focus card, free-focus entry, FocusActivity countdown, early-complete confirmation, and focus-session count refresh.
- The emulator is good enough to catch launch crashes and obvious UI regressions before the user installs the APK on a phone.

Do not overclaim emulator results. A real phone is still required for:

- notification shade icon sizing and color on the user's ROM
- vibration / haptics
- lock-screen / full-screen reminder behavior
- desktop launcher widget rendering and resizing
- OEM alarm, reboot, battery, and background-execution behavior

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
11. make the commit message describe the feature behavior changes and bug-fix/debug logic completed in that round, rather than vague process wording or process bookkeeping

## Commit Message Style

PaykiTodo commit messages should be useful as a product history. The core of a commit message is what changed in the app, not whether a command was run.

Good commit messages should explain:

- the module or feature that changed;
- the concrete behavior added, removed, or improved;
- the bug symptom and before/after behavior when fixing defects;
- the relevant root cause or debug conclusion when that matters;
- version changes when the app version is bumped.

Avoid commit messages whose main content is:

- `优化界面` without saying what changed;
- `修复问题` without describing the bug;
- `更新文档` without saying which behavior/status changed;
- whether the commit was pushed;
- a long list of validation commands.

Commit subjects must not append version-bump tails such as `并升级到1.7.9` or `并升级到 x.x.x`. The subject should describe only the concrete feature or bug-fix behavior. If the app version changed, put that detail in the commit body or the relevant version docs instead of the subject.

Validation commands can be mentioned briefly if useful, but final user replies are the better place for detailed verification and APK path reporting.

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
9. write the git commit message in Chinese and describe the actual user-visible feature changes or bug-fix/debug logic you completed
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
