# Current Task

## Active Development Focus

The `1.15.14 / versionCode 356` Desktop Web Calendar reference-reproduction round is complete in code and browser verification. The current round replaces the generic `1.15.13` calendar workspace with a screenshot-matched implementation based on the user's three Feishu Calendar references. No push is authorized unless the user explicitly asks.

## Completed Scope

1. Desktop Web uses a 58px light top navigation, a 296px mini-calendar sidebar, and an unframed full-width calendar grid matching the supplied reference proportions at 125% Windows display scaling.
2. Calendar supports functional day, week, and month views; mini-calendar navigation; event/location search; group visibility filtering; date navigation; and collapsed Excel import.
3. Week view uses a continuous seven-day time grid with restrained separators, compact event blocks, a fixed GMT+8 axis header, and an initial 08:00-aligned viewport.
4. The event editor is a reference-sized two-column window: editable form on the left, live day timeline preview on the right, with preserved PaykiTodo fields and transactional save/cancel behavior.
5. Corrected legacy CSS inheritance that previously expanded every top navigation item to a full viewport width, moved header actions to a second row, and stretched the event editor close button across the form.
6. Database version remains `30`; app version is `1.15.14 / 356`.

## Validation

1. Browser verification at `1638x989` CSS pixels with `deviceScaleFactor = 1.25`, producing `2048x1236` screenshots comparable to the references.
2. Verified seven columns in week view, one column in day view, 42 cells in month view, mini-calendar selection, search filtering, blank-grid creation, existing-event editing, and live editor timeline preview.
3. Browser runtime reported no page errors in the verified flows.
4. 27 unit-test suites / 152 tests passed with zero failures or skips; `:app:lintDebug`, `:app:assembleDebug`, JavaScript syntax, and `git diff --check` passed.
5. APK metadata confirms package `com.paykitodo.app`, `versionName 1.15.14`, `versionCode 356`, min API 26, and target API 34; the APK was installed on `emulator-5554` / Pixel_8 for Desktop Sync verification.

## Next Product Checks

1. Review the physical desktop browser rendering at the user's normal Windows display scale and browser zoom.
2. Continue refining only concrete mismatches against the supplied reference images; do not reintroduce dashboard cards, dark permanent sidebars, gradients, or generic admin-console composition.
3. Preserve all PaykiTodo-specific fields and backend contracts even when the visual shell follows the reference.
4. Do not push unless the user explicitly asks.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, emulator screenshots, test databases, or personal backups/logs.
