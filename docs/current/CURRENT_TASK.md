# Current Task

## Active Development Focus

The `1.15.6 / versionCode 348` light-theme and Daily Board schedule-hierarchy round is complete.

The next UI round can return to the Todo filter/action hierarchy recorded in `UI_MODERNIZATION_AUDIT.md`. Do not restore Sand beige as the global light background.

## Completed Scope

1. Traced the yellow/beige Calendar and card appearance to the global light `background`, `surface`, and `surfaceVariant` roles.
2. Replaced the global light background with cool white, surfaces with paper white, and weak containers with soft gray-green.
3. Kept Forest, Ocean, Clay, the Daily Board artwork, and `payki_greeting`; Clay is now a small-area accent rather than a page fill.
4. Rebuilt the Board schedule card into a white `今日剩余` area and a full-width tinted `明日安排` area.
5. Added counts, tomorrow's date/weekday, a Planning route for the empty state, and a Calendar route when more than two tomorrow events exist.
6. Bumped to `1.15.6 / 348` and built the debug APK without a database migration.

## Validation

- `:app:compileDebugKotlin` passed.
- `:app:assembleDebug` passed.
- `:app:testDebugUnitTest` passed: 22 suites / 134 tests / 0 failures or errors.
- `:app:lintDebug` passed.
- Pixel_8 emulator visually confirmed the cool-white Calendar and the new Board schedule split in light mode.
- Pixel_8 emulator visually confirmed the dark Board remains readable; the emulator was restored to light mode afterward.

Do not commit secrets, signing material, API keys, private Base URLs, generated APK/AAB outputs, emulator screenshots, or personal backups/logs.
