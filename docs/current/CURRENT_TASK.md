# Current Task

## Active Development Focus

The current round has produced a `1.6.46` baseline. The implemented focus was UI refinement for the in-app Wiki, daily board schedule state, and drawer header icon.

Completed in this round:

1. In-app Wiki now keeps a left navigation / right article layout even on phone-sized screens.
2. Wiki responsive CSS no longer stacks all navigation buttons above the article content.
3. Daily board now distinguishes between no schedule today and today's schedule existing but already finished.
4. Daily board schedule title count uses all events that overlap today, not only events still visible after the current time.
5. Drawer header app icon is enlarged and clipped into the circular header surface to reduce the white rounded-rectangle launcher background effect.

## Immediate Practical Next Steps

When testing, use:

1. install `app/build/outputs/apk/debug/PaykiTodo-1.6.46-debug.apk`
2. open Settings -> 使用说明 and verify the Wiki shows a left menu and right article content
3. tap different Wiki sections and verify the right article changes without needing to scroll below a large button block
4. open Daily Board on a day with no schedule and verify it still says `今天暂无日程`
5. open Daily Board after all of today's events have ended and verify it says `太棒了！今天的日程都结束了~`
6. open the app drawer in dark mode and verify the header icon appears circular rather than a white rounded rectangle inside a circle

## Repository-Verified Notes

The current code baseline includes these specific `1.6.46` changes:

1. `app/build.gradle.kts` is bumped to `1.6.46 / 118`.
2. `app/src/main/assets/wiki/index.html` keeps a left navigation column in narrow layouts.
3. `DashboardChrome.kt` computes `allTodayScheduleItems` separately from currently visible schedule rows.
4. `TodayScheduleBoardCard` receives `hasTodayEvents` and changes the empty-state text accordingly.
5. The drawer header image uses `clip(CircleShape)` and `ContentScale.Crop` inside the circular surface.

## What Not To Do Immediately

- do not replace the Wiki with a separate browser launch; it should remain in-app
- do not regress the 1.6.45 desktop Web asset split back into Kotlin raw strings
- do not reintroduce stacked top button layout for Wiki on phone WebView unless there is a tested reason
- do not claim all visual polish is complete without device-side checking
- do not scan the whole workspace outside this repo
- do not revert unrelated user edits
- do not change JDK setup; use Android Studio bundled `jbr`

## Current External Dependency

No external file is needed for the current `1.6.46` verification task.
