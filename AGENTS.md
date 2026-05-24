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
3. Treat older versioned docs under `docs/archive/historical/` as historical reference, not the current product baseline, unless a current doc explicitly points to them.
4. Prefer updating the `docs/current/` files when project intent, status, or task scope changes.
5. If code changes affect product behavior, version status, implementation status, or current priorities, update the relevant docs in the same round rather than leaving them stale.
6. Before ending a work round, update `docs/current/SESSION_HANDOFF.md` and any other affected docs.
7. After a real feature / fix round is completed and minimally verified, create a focused git commit for that round unless the user explicitly says not to commit yet.
8. When reporting completion, tell the user the latest APK path that exists for the current version; if no new APK was built in that round, say so explicitly.
9. Git commit messages for this repository should be written in Chinese.
10. A git commit message should describe the version-relevant feature / behavior changes in that round compared with the previous state, not just generic process wording such as "finalize" or "cleanup".
11. Goal-mode prompt files (under `docs/goals/`) that drove a completed work round must be committed into the repository as a separate archive commit after the feature work itself lands. Subject like `归档 X.Y.Z 目标文档`. Do not commit goal files that contain API keys, tokens, private Base URLs, signing material, or other secrets — redact or keep them outside git instead. See `docs/goals/README.md` for the naming convention.
12. Version numbers must always be written with standard ASCII digits and dots, such as `v1.10.3`, `1.10.3`, or `1.11.0 / versionCode 222`. Never write version numbers as Chinese numerals or mixed text such as `一一零三`, `一零三`, or `一.一零.三` in chat-facing summaries, docs, commit messages, release notes, or APK build reports.

## GitHub Public Repository Standards

Keep the repository presentable as a public Android project, not just as a local debug folder.

- The root directory should stay focused on public entry points: `README.md`, `CHANGELOG.md`, `LICENSE`, `NOTICE.md`, `PRIVACY.md`, `SECURITY.md`, `CONTRIBUTING.md`, `SUPPORT.md`, `CODE_OF_CONDUCT.md`, `.github/`, `AGENTS.md`, Gradle project files, and source directories.
- Internal session state, backlog notes, handoff prompts, and goal-mode files belong under `docs/current/`, `docs/goals/`, or `docs/archive/`; do not recreate loose root files such as `goal.md`, `TODO.md`, or temporary debug notes.
- Public docs must distinguish the current source/debug version from the latest published GitHub Release when they differ. Do not point a public APK download link at a tag that has not been created.
- When behavior changes are user-visible, update the relevant public-facing docs if the change affects how users install, understand, report issues for, or safely operate the app.
- Keep `CHANGELOG.md` as a user-facing release log. Do not turn it back into a raw internal session journal; detailed implementation history belongs in commits and `docs/archive/`.
- Keep `.github/` templates and Android CI usable. If a GitHub Actions failure is caused by repository config, formatting, or a stale workflow assumption, fix it before treating the repo as publication-ready.
- Keep `SECURITY.md` because PaykiTodo includes LAN sync, optional AI API keys, local backups, and release signing. Security and secret-handling expectations should be visible to GitHub readers.
- Never commit generated APK/AAB files, signing keystores, API keys, private Base URLs, backups, logs with personal data, or local machine diagnostics.
- Do not keep unreferenced design intermediates, old launcher icon variants, or one-off generated assets in source directories unless they are deliberately documented as maintained source assets.
- Before committing public-doc, release, signing, or repo-layout work, run `git diff --check` and verify ignored sensitive paths with `git check-ignore -v keystore.properties release/PaykiTodo-release.jks .env` or the closest relevant command.

## Release Signing And Secret Handling

Release signing secrets never belong in `docs/` or in Git history.

- Safe, commit-ready explanations and examples live under `docs/templates/`.
- The real local signing config must be the ignored root-level file `keystore.properties`, usually copied from `keystore.properties.example`.
- The generated keystore should live under the ignored root-level `release/` directory.
- Do not echo keystore passwords, API keys, tokens, or private Base URLs into chat, docs, commit messages, or logs.
- Before committing release-related work, verify `git check-ignore -v keystore.properties release/PaykiTodo-release.jks` and confirm no signing material appears in `git status --short`.
- Release APK / AAB artifacts are local distribution outputs and should not be committed unless the user explicitly asks for artifact tracking.

## Android Emulator Verification

When a change affects phone-side UI, navigation, startup stability, Planning Desk, Settings, focus mode, reminders, or other Android runtime behavior, the Android Emulator is an accepted local verification surface before asking the user to install the debug APK on a physical phone.

Recommended workflow:

1. Check whether a device is already available:
   - `C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe devices`
2. If an emulator is already running, use it rather than starting another instance.
3. If no emulator is running and UI/runtime verification is materially useful, it is acceptable to start an AVD from Android Studio or with the Android SDK emulator, then clearly tell the user that an emulator window may appear.
4. Install the current debug APK when needed:
   - `adb install -r app/build/outputs/apk/debug/PaykiTodo-<version>-debug.apk`
5. Use emulator verification for:
   - app launch / startup crash checks
   - screen navigation and basic click flows
   - Compose UI presence through screenshots or `uiautomator dump`
   - Settings / Planning Desk / Daily Board / FocusActivity smoke tests
   - notification scheduling smoke tests where Android system services are available
6. Keep physical-device verification for behavior the emulator cannot prove well:
   - notification shade icon appearance on the user's OEM ROM
   - real vibration / haptics
   - lock-screen and full-screen reminder behavior
   - launcher widget rendering, resizing, and OEM launcher quirks
   - background alarm reliability, reboot recovery, and battery-management policies

Do not treat a passing emulator smoke test as complete coverage for OEM-specific behavior. Do record emulator evidence in the final report when it materially reduces the amount of physical-phone testing needed.

If the session starts or reuses an emulator, record the device id / AVD name, installed APK path, and checked screens or flows in `docs/current/SESSION_HANDOFF.md`. This prevents later confusion when an Android Emulator window is still open on the user's desktop.

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
