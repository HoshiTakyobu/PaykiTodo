# Current Task

## Active Development Focus

The current goal is PaykiTodo `1.9.0` / `versionCode 193`, focused on adding 专注模式（番茄钟） before moving on to the separate `1.9.1` AI 日报 / 周报 goal.

The prior Android `今日看板` launcher-widget visual follow-up was committed separately as `1d768ef` and should not be mixed into the remaining focus-session work.

## Completed In 1.9.0 So Far

1. Version metadata is now `1.9.0` / `versionCode 193`.
2. Room database version is now `12`; `MIGRATION_11_12` creates `focus_sessions` and indices for `startedAtMillis` and `todoId`.
3. `FocusSession` records todo binding, title, planned / actual minutes, start / end timestamps, completion state, and extension count.
4. Repository and DAO expose focus-session insert, today-range queries, completed-focus minute totals, observation, backup export/import, and clear/restore support.
5. Settings -> `专注模式` provides default duration, extension duration, keep-screen-on, and documented-only notification-suppression preferences.
6. `FocusActivity` implements a full-screen countdown with circular progress, pause / continue, early completion confirmation, abandon confirmation, zero-time vibration, extension, save-before-exit behavior, and completion feedback.
7. Active todo long-press menus now include `开始专注 · X 分钟` beside the destructive delete entry.
8. Daily board now shows a `今日已专注` card with completed minutes, total sessions, completed sessions, and a `自由专注` entry.

## Verification Completed This Round

1. `./gradlew.bat :app:compileDebugKotlin`

## Immediate Practical Next Steps

1. Finish user-facing docs for `1.9.0`.
2. Run full verification:
   - `node --check app/src/main/assets/desktop-web/app.js`
   - `./gradlew.bat testDebugUnitTest`
   - `./gradlew.bat assembleDebug`
   - `git diff --check`
3. Create a focused local commit for `1.9.0` 专注模式 using the `完成内容概要：` bullet-list body.
4. Do not push unless the user explicitly asks.
5. Then start the separate `1.9.1` AI 日报 / 周报 goal.

## Device Verification Needed After Installing 1.9.0

1. Long-press a todo and confirm `开始专注 · X 分钟` appears.
2. Start a bound focus session, pause / continue, finish early, and confirm the completion feedback page appears.
3. Start a focus session and abandon it; confirm the abandon record does not add completed minutes.
4. Let a countdown reach zero; confirm vibration, `完成 / 延续 / 放弃` choices, and extension-count behavior.
5. Start a free focus session from the daily-board focus card.
6. Confirm completed focus minutes update on the daily-board focus card after returning.
7. Confirm `设置 -> 专注模式` duration sliders affect newly started focus sessions.

## Commit Message Rule

PaykiTodo commit messages must use the `AGENTS.md` body format:

```text
完成内容概要：
- ...
```

The subject should describe the behavior change and must not append a version-bump tail.
