# Current Task

## Active Development Focus

The `1.15.15 / versionCode 357` calendar follow-up is complete in code and verification. It carries the Feishu-style Desktop Web Calendar implementation forward, fixes selected-date day view behavior, clarifies past/today/future visual states, adds the shared phone AI configuration surface to Desktop Web, and reduces phone Calendar drag recomposition. No push is authorized unless the user explicitly asks.

## Completed Scope

1. Desktop Web uses a 58px light top navigation, a 296px mini-calendar sidebar, and an unframed full-width calendar grid matching the supplied reference proportions at 125% Windows display scaling.
2. Calendar supports functional day/week/month views; mini-calendar navigation; event/location search; date navigation; and an explicit Excel import preview action. The removed sidebar group list no longer competes with the calendar hierarchy.
3. Week view uses a continuous seven-day time grid with restrained separators, compact event blocks, a fixed GMT+8 axis header, and an initial 08:00-aligned viewport.
4. The event editor is a reference-sized two-column window: editable form on the left, live day timeline preview on the right, with preserved PaykiTodo fields and transactional save/cancel behavior.
5. Corrected legacy CSS inheritance that previously expanded every top navigation item to a full viewport width, moved header actions to a second row, and stretched the event editor close button across the form.
6. Desktop Web AI Reports shares phone AI configuration, provider model retrieval, one-shot connectivity testing, report scheduling, retention, and immediate daily generation. Only the top-right completion action saves the page draft; API keys are never returned in full.
7. Phone Calendar horizontal drag work is read in layout/draw phases rather than driving ordinary per-pixel full-calendar recomposition; Room range and placement work update only when the focused day changes.
8. Database version remains `30`; app version is `1.15.15 / 357`.

## Validation

1. Browser verification at `1638x989` CSS pixels with `deviceScaleFactor = 1.25`, producing `2048x1236` screenshots comparable to the references.
2. Verified seven columns in week view, one column in day view, 42 cells in month view, mini-calendar selection, search filtering, blank-grid creation, existing-event editing, live editor timeline preview, Excel import controls, and the AI report workspace.
3. Browser runtime reported no page errors in the verified flows.
4. Emulator calendar swipe statistics: 441 frames, p50 `21ms`, p90 `34ms`, p95 `40ms`, p99 `81ms`, 9.98% janky frames, and 21 slow UI-thread frames. This is an improvement target, not a claim of zero jank.
5. 27 unit-test suites / 152 tests passed with zero failures or skips; `:app:compileDebugKotlin`, `:app:lintDebug`, `:app:assembleDebug`, JavaScript syntax, and `git diff --check` passed.
6. APK metadata confirms package `com.paykitodo.app`, `versionName 1.15.15`, `versionCode 357`, min API 26, and target API 34; the APK was installed on `emulator-5554` / Pixel_8 for final verification.

## Next Product Checks

1. Review the physical desktop browser rendering at the user's normal Windows display scale and browser zoom.
2. Repeat the calendar swipe benchmark on a physical phone with a populated event dataset; the emulator still reports a measurable jank tail.
3. Continue refining only concrete mismatches against the supplied reference images; do not reintroduce dashboard cards, dark permanent sidebars, gradients, or generic admin-console composition.
4. Preserve all PaykiTodo-specific fields and backend contracts even when the visual shell follows the reference.
5. Do not push unless the user explicitly asks.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, emulator screenshots, test databases, or personal backups/logs.
