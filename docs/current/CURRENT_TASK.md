# Current Task

## Active Development Focus

The current round is PaykiTodo `1.9.9` / `versionCode 203`.

Primary goal: make the desktop Web console less like a pure management backend by giving its first tab a phone-derived daily-board surface, and fix the desktop Planning Desk AI path where a recognized AI candidate could not be imported.

## Completed In This Round

1. Desktop sync snapshot now includes a `todayBoard` payload derived from `DailyBoardSnapshotBuilder`, so the browser can reuse the phone-side board logic for today todos, visible today events, tomorrow events, and ended-event filtering.
2. Desktop Web first tab was renamed from `待办时间轴` to `每日看板`.
3. Desktop Web daily board now shows the current date, a current/next item card, today's focus minutes/session counts, today todos, today schedule, tomorrow schedule, and an empty hint when tomorrow has no events.
4. Desktop Web in-progress schedule rows use the same gold emphasis direction as the phone board; finished events are not shown as current items.
5. The full todo timeline remains below the board as the management surface.
6. Desktop Planning Desk import no longer requires edited candidates to match local-rule `line-*` IDs. AI candidates such as `ai-0` can be converted directly into import candidates.
7. Planning Desk AI group parsing is conservative: `groupName` is preserved only when the original line explicitly contains `#group`, `分组：`, `项目：`, or `课程：`.
8. The AI prompt now explicitly tells providers not to split group names out of normal titles; `16:05-18:00 入党表格填写` should keep title `入党表格填写` and empty group.
9. Added / updated unit coverage for the AI group-name guard.
10. Version metadata moved to `1.9.9` / `versionCode 203`.

## Verification Completed This Round

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `./gradlew.bat :app:compileDebugKotlin` passed.
3. `./gradlew.bat testDebugUnitTest` passed.
4. `./gradlew.bat assembleDebug` passed and produced `app/build/outputs/apk/debug/PaykiTodo-1.9.9-debug.apk`.
5. `git diff --check` passed.
6. `app/build/outputs/apk/debug/output-metadata.json` confirms `versionCode=203`, `versionName=1.9.9`, and `outputFile=PaykiTodo-1.9.9-debug.apk`.

## Immediate Practical Next Steps

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.9.9-debug.apk` on the phone.
2. Enable desktop sync and open the browser console.
3. Verify the first tab shows the daily board rather than only the old todo timeline.
4. In desktop Planning Desk, test `16:05-18:00 入党表格填写` with AI recognition: group should remain empty unless the source line explicitly writes a group marker, and importing the selected candidate should create one event rather than reporting `已导入 0 条`.
5. Do not push unless the user explicitly asks.

## Device / Browser Verification Needed After Installing 1.9.9

1. Desktop board: current/next item card, focus stats, today todos, today schedule, tomorrow schedule, ended-event filtering, and in-progress gold highlight.
2. Desktop board interactions: clicking a board todo opens todo preview; clicking a board event opens the event editor.
3. Desktop Planning Desk AI import: AI candidate IDs should import correctly after preview selection.
4. AI group guard: ordinary titles should not create accidental groups; explicit `分组：课程 ...` or `#group 课程` should still preserve group names.
5. Regression: AI report archive from `1.9.8` should still appear under drawer -> `AI 报告`.

## Local Device Verification Reality

- `adb` is available at `C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- `emulator-5554` has been used in recent rounds for pre-phone smoke tests and may still be available.
- This round has not yet run a browser connected to a live phone after installing `1.9.9`; real desktop-browser verification is still required because the Web UI depends on the phone-hosted LAN server.

## Commit Message Rule

PaykiTodo commit messages must use the `AGENTS.md` body format:

```text
完成内容概要：
- ...
```

The subject should describe the behavior change and must not append a version-bump tail.
