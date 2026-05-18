# Current Task

## Active Development Focus

The current round is PaykiTodo `1.10.2` / `versionCode 220`.

Primary goal: fix real-device feedback after the first countdown/widget pass:

1. countdown text should use true remaining days/hours/minutes, without seconds or total-hour expansion;
2. ended daily-board events should not keep inflating the visible `今日日程` count;
3. the independent `倒数日` widget should be a compact target list, not a second titled board;
4. launcher widget picker entries should have clear names, descriptions, suggested sizes, and static previews;
5. free-focus should move out of `每日看板` into a dedicated focus surface.

## Completed In This Round

1. Phone, desktop, and widget countdown rendering now use shared day/hour/minute decomposition:
   - `>= 1 day`: `Nd` plus `xh ym`;
   - `< 1 day`: `Nh` plus `xm`;
   - `< 1 hour`: `Nm`;
   - seconds are not shown.
2. Countdown targets whose exact target time has passed are filtered out, not just targets whose target date is before today.
3. Phone daily-board countdown event rows no longer prefix the event time with `日程`.
4. Phone daily-board, desktop daily-board, and Android 今日看板 widget `今日日程` counts now use visible / unfinished today events; when all today events ended, the count is `0` and the completion message remains.
5. Android independent `倒数日` widget no longer shows title/date/count header text; rows show remaining time, color strip, target title, and a todo-only completion circle.
6. Android independent `倒数日` widget row clicks now deep-link to the exact todo/event detail instead of only opening My Tasks or Calendar.
7. Android independent `倒数日` widget schedules minute ticks so displayed minute text refreshes more frequently than the platform default widget period.
8. Android 今日看板 widget section titles now use the primary text color rather than the orange header color, fixing the light-mode brown-title mismatch.
9. Daily board no longer shows the focus stats/free-focus card, and board todo cards no longer expose the focus long-press action.
10. A new drawer section `专注` hosts the focus stats card and free-focus entry.
11. A new Android `专注` widget shows today's focus stats and starts free focus directly.
12. Android widget picker metadata now gives 今日看板 / 倒数日 / 专注 widgets distinct labels, descriptions, suggested sizes, and static preview layouts.
13. Version metadata moved to `1.10.2 / 220`.

## Verification Completed This Round

Completed locally:

1. `./gradlew.bat :app:compileDebugKotlin` passed.
2. `node --check app/src/main/assets/desktop-web/app.js` passed.
3. `./gradlew.bat :app:testDebugUnitTest` passed.
4. `./gradlew.bat :app:assembleDebug` passed and produced `app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk`.
5. `git diff --check` passed.
6. APK metadata confirms `versionName = 1.10.2`, `versionCode = 220`.
7. `git check-ignore -v keystore.properties release/PaykiTodo-release.jks app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk` confirmed signing material and APK artifacts are ignored.

## Verification Still Needed On Device / Browser

1. Install `app/build/outputs/apk/debug/PaykiTodo-1.10.2-debug.apk` on the physical phone.
2. Phone-test daily board:
   - countdown rows show day/hour/minute text without seconds;
   - event countdown metadata shows only the event time, not redundant `日程`;
   - ended today events reduce `今日日程` count to 0 while showing the completion message;
   - focus stats/free-focus no longer appear on the daily board.
3. Device-test widgets:
   - widget picker shows clear labels/descriptions/previews for 每日看板, 倒数日, and 专注;
   - 今日看板 widget section titles are readable in light mode and no focus/countdown sections appear;
   - 倒数日 widget has no header/date/count text, refreshes minute-level countdown text, and row clicks open exact todo/event details;
   - 专注 widget opens the focus page from the root and starts free focus from the action area.
4. Browser-test desktop daily board:
   - countdown rows use the same day/hour/minute format;
   - visible today-event count ignores events already ended;
   - the old focus stats block is absent from the daily-board hero.

## Immediate Practical Next Steps

1. Create a focused local commit.
2. Do not push unless the user explicitly asks.
