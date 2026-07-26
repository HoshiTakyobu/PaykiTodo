# Current Task

## Active Development Focus

The `1.15.5 / versionCode 347` phone UI audit and drawer-modernization round is complete.

The next UI round should address Todo page hierarchy first. Do not start a one-shot rewrite of all phone screens, and do not replace the protected Daily Board artwork, custom greeting font, or established palettes.

## Completed Scope

1. Audited the navigation drawer, Daily Board, Todo, Calendar, Planning Desk, and Settings on the Pixel_8 emulator in light and dark themes.
2. Rebuilt the drawer around a product identity header and explicit `主要功能` / `回顾与记录` groups.
3. Removed the discoverability problem caused by hiding AI Reports and History behind `更多`.
4. Fixed Settings and appearance controls to the drawer bottom and unified menu row/icon/selection dimensions.
5. Replaced the floating appearance dropdown with a descriptive bottom sheet and selected checkmark.
6. Recorded remaining page-specific findings in `docs/current/UI_MODERNIZATION_AUDIT.md`.
7. Bumped to `1.15.5 / 347` and built the debug APK without a database migration.

## Next Recommended Scope

1. Fix the Todo group-filter strip ending in a visibly clipped chip and add a clear horizontal-scroll affordance.
2. Clarify the primary add action versus batch-import/group-management actions.
3. Reduce empty-section card/heading weight and align Todo spacing with the Daily Board.
4. Then handle Calendar toolbar discoverability and Planning Desk writing-first layout as separate rounds.

## Validation

- `:app:compileDebugKotlin` passed.
- `:app:assembleDebug` passed.
- `:app:testDebugUnitTest` passed: 22 suites / 134 tests / 0 failures or errors.
- `:app:lintDebug` passed.
- APK metadata reports `1.15.5 / versionCode 347`, package `com.paykitodo.app`.
- Pixel_8 emulator installed and launched `PaykiTodo-1.15.5-debug.apk`.
- Drawer routes passed for Daily Board, Todo, Calendar, Planning Desk, AI Reports, History, and Settings.
- Light mode, dark mode, and appearance bottom-sheet rendering were visually checked.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, emulator screenshots, or personal backups/logs.
