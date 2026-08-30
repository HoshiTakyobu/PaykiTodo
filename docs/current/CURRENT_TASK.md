# Current Task

## Completed Development Focus

The `1.15.16 / versionCode 358` follow-up is complete. It synchronizes the Desktop Web Calendar week-start preference with the phone setting, simplifies the Desktop Web todo page to `待办 / 历史待办`, reuses the phone launcher icon, removes ongoing-notification chronometers, exposes automatic LAN addresses, and adds a non-destructive `我知道了` action for future todo reminders. UX findings outside these fixes are documented for review only. No push is authorized unless the user explicitly asks.

## Completed Scope

### 1.15.16 implementation scope

1. Desktop Web serves the existing phone launcher artwork in the login and workbench brand positions.
2. Desktop Web todo management removes summary statistics and the old timeline heading; active items are shown in `待办`, historical items in `历史待办`, with group filtering retained.
3. Desktop Web Calendar receives `weekStartMode` in the authenticated snapshot and persists the only two legal values through `/api/calendar-preferences`; week, month, and mini-calendar ordering use the selected mode.
4. The mini-calendar keeps the gray selected-week range behind today's blue ring.
5. Ongoing event notifications disable chronometer and timestamp display while keeping the planned event range in the content text and custom RemoteViews.
6. Future-date todo reminders expose a non-mutating acknowledgement action in both the full-screen activity and accessibility fallback overlay; later scheduled reminders for the same todo remain active.
7. Desktop Sync Settings directly shows detected connection URLs whenever the service is running, without a visibility toggle.

### Preserved 1.15.15 baseline

1. Desktop Web uses a 58px light top navigation, a 296px mini-calendar sidebar, and an unframed full-width calendar grid matching the supplied reference proportions at 125% Windows display scaling.
2. Calendar supports functional day/week/month views; mini-calendar navigation; event/location search; date navigation; and an explicit Excel import preview action. The removed sidebar group list no longer competes with the calendar hierarchy.
3. Week view uses a continuous seven-day time grid with restrained separators, compact event blocks, a fixed GMT+8 axis header, and an initial 08:00-aligned viewport.
4. The event editor is a reference-sized two-column window: editable form on the left, live day timeline preview on the right, with preserved PaykiTodo fields and transactional save/cancel behavior.
5. Corrected legacy CSS inheritance that previously expanded every top navigation item to a full viewport width, moved header actions to a second row, and stretched the event editor close button across the form.
6. Desktop Web AI Reports shares phone AI configuration, provider model retrieval, one-shot connectivity testing, report scheduling, retention, and immediate daily generation. Only the top-right completion action saves the page draft; API keys are never returned in full.
7. Phone Calendar horizontal drag work is read in layout/draw phases rather than driving ordinary per-pixel full-calendar recomposition; Room range and placement work update only when the focused day changes.
8. Database version remains `30`; app version is `1.15.16 / 358`.

## Validation

### Preserved browser and performance baseline

1. Browser verification at `1638x989` CSS pixels with `deviceScaleFactor = 1.25`, producing `2048x1236` screenshots comparable to the references.
2. Verified seven columns in week view, one column in day view, 42 cells in month view, mini-calendar selection, search filtering, blank-grid creation, existing-event editing, live editor timeline preview, Excel import controls, and the AI report workspace.
3. Browser runtime reported no page errors in the verified flows.
4. Emulator calendar swipe statistics: 441 frames, p50 `21ms`, p90 `34ms`, p95 `40ms`, p99 `81ms`, 9.98% janky frames, and 21 slow UI-thread frames. This is an improvement target, not a claim of zero jank.

### 1.15.16 verification

5. This round passed `:app:compileDebugKotlin`, `:app:testDebugUnitTest` (26 suites), `:app:lintDebug`, `:app:assembleDebug`, JavaScript syntax, APK metadata inspection, and `git diff --check`. Emulator Desktop Sync returned HTTP 200 with the embedded launcher icon and week-start control present, while the removed todo heading and summary container were absent.
6. The target APK metadata is package `com.paykitodo.app`, `versionName 1.15.16`, `versionCode 358`, min API 26, and target API 34.

## Next Product Checks

1. Review the physical desktop browser rendering at the user's normal Windows display scale and browser zoom.
2. Repeat the calendar swipe benchmark on a physical phone with a populated event dataset; the emulator still reports a measurable jank tail.
3. Continue refining only concrete mismatches against the supplied reference images; do not reintroduce dashboard cards, dark permanent sidebars, gradients, or generic admin-console composition.
4. Preserve all PaykiTodo-specific fields and backend contracts even when the visual shell follows the reference.
5. Do not push unless the user explicitly asks.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, emulator screenshots, test databases, or personal backups/logs.
