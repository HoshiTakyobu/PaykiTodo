# Current Task

## Completed Development Focus

The `1.15.18 / versionCode 360` round restores independent reminder-channel selection across phone and Desktop Web, fixes Desktop Web calendar hour-axis alignment, and makes ended events fade as one complete card. The database remains version `30`. No push is authorized unless the user explicitly asks.

## Completed Scope

1. Phone todo, event, and Planning Desk candidate editors directly expose `全屏提醒` and `通知栏提醒` checkboxes without a separate enable switch.
2. Desktop Web todo, event, and Planning Desk editors use the same two-channel model and serialize the selected channel combination exactly.
3. Selecting neither channel disables reminder scheduling; selecting one channel uses only that channel; selecting both requests full-screen delivery and keeps the full notification content visible.
4. Stored single-channel values remain intact through item editing, recurring templates, backup/restore, batch import, and Desktop Sync.
5. Reminder snooze/acknowledgement retains a normal detail notification only when the notification channel was selected.
6. Desktop Web hour labels use real axis rows after the calendar header, so labels and solid hour rules share the same layout coordinate.
7. Ended Desktop Web events reduce opacity on the parent event card, fading title, time, location, border, and background together.
8. App version is `1.15.18 / 360`; database version remains `30`.

## Validation

1. `node --check app/src/main/assets/desktop-web/app.js` passed.
2. `git diff --check` passed before the final build round.
3. `:app:compileDebugKotlin` passed.
4. `:app:testDebugUnitTest` passed, including four-state reminder-mode coverage.
5. A headless Edge screenshot using the production CSS confirmed hour-label alignment and whole-card fading.
6. `:app:lintDebug` and `:app:assembleDebug` passed.
7. APK metadata confirms package `com.paykitodo.app`, `versionName 1.15.18`, `versionCode 360`, min API 26, and target API 34.

## Product Review Pending

1. Approve whether Calendar `显示待办` defaults to enabled.
2. Approve the first implementation phase: read-only todo display, complete action, and preview navigation, without drag-to-change-DDL.
3. Choose the next UX slice from `docs/current/CALENDAR_TODO_AND_UX_REVIEW_PLAN.md`.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, emulator screenshots, test databases, or personal backups/logs.
