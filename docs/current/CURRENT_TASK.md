# Current Task

## Active Development Focus

The current round is PaykiTodo `1.8.1` / `versionCode 184`, focused on repairing the `1.8.0` Planning Desk AI-recognition experience and adding two small daily-use surfaces: board announcements and an Android today-todo widget.

## Completed In 1.8.1

1. Planning Desk AI recognition is explicit-only:
   - phone recognition remains tied to the `识别` button in `PlanningDeskPanel`
   - desktop recognition remains tied to the `识别` button or `Ctrl+Enter`
   - desktop import without an existing parse preview now asks the user to click `识别` first instead of silently calling `/api/planning/parse`
2. Planning Desk auto-save feedback no longer uses a progress bar; it shows plain `自动保存中` text so editing/newline feedback is not confused with AI/network loading.
3. Settings -> `AI 调用配置` now includes a single-provider `测试连接` action in the provider edit dialog, using the currently entered Base URL / API Key / model with short timeouts.
4. AI recognition verification is documented in `docs/current/AI_RECOGNITION_VERIFICATION.md`, including phone/desktop call chains and the four expected AI states.
5. Settings adds `公告设置`; `AppSettingsStore` persists announcement text, start date, and end date.
6. Daily board shows an orange announcement banner above the greeting card when the current date is inside the configured inclusive range; long text uses marquee.
7. Android desktop widget first version is implemented:
   - `TodoWidgetProvider`
   - `TodoWidgetService`
   - widget RemoteViews layouts and provider XML
   - manifest registration
   - repository mutation callback refreshes widget data after todo changes
8. Launch screen delay is reduced from `1600ms` to `600ms`.
9. Version metadata is now `1.8.1` / `versionCode 184`.

## Verification Completed This Round

1. `node --check app/src/main/assets/desktop-web/app.js` succeeded.
2. `./gradlew.bat :app:compileDebugKotlin` succeeded with the Android Studio bundled JBR. Kotlin incremental compilation reported a corrupted local cache, fell back to non-incremental compilation, and still finished successfully.
3. `./gradlew.bat testDebugUnitTest` succeeded.
4. `./gradlew.bat assembleDebug` succeeded and produced `app/build/outputs/apk/debug/PaykiTodo-1.8.1-debug.apk`.

## Immediate Practical Next Steps

After installing the `1.8.1` APK on device, verify:

1. Planning Desk editing/newline only auto-saves and does not call AI.
2. Desktop Planning Desk import without a parse preview asks the user to click `识别` first.
3. AI provider `测试连接` succeeds with a valid provider and reports HTTP/timeout errors clearly with invalid providers.
4. AI recognition states match `AI_RECOGNITION_VERIFICATION.md`.
5. Announcement banner appears for an active date range, hides for future/expired ranges, clears correctly, and scrolls long text.
6. Android launcher widget can be added, shows today active todos, shows `今天没有安排` when empty, opens the app on tap, and refreshes after create/complete/delete.
7. Launch screen feels shorter without exposing cold-start flicker.

## Commit Message Rule

PaykiTodo commit messages should describe product behavior changes and bug/debug reasoning. They should not primarily document push state, validation commands, or vague process notes. Commit subjects must not append version-bump tails such as `并升级到1.7.11`.

## Current External Dependency

The active objective came from `G:\Workspace\tmp_goal.md` / `G:\Workspace\goal.md`. Completion checks should rely on this file plus implemented code, synchronized docs, and verified build/test outputs. Do not push unless the user explicitly asks.
